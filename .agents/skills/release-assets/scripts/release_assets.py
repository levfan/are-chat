"""Build are-chat release assets and sync them to the GitHub Release.

Run from anywhere; paths are resolved from this script location. The GitHub
token is read from GITHUB_TOKEN / GH_TOKEN or git credential fill and never
printed. Requires tracked files to be clean unless --allow-dirty is given.
Only major versions are released unless --force is given.
"""

import argparse
import datetime
import hashlib
import http.client
import json
import os
import re
import shutil
import subprocess
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path

HERE = Path(__file__).resolve()
ROOT = HERE.parents[4]
OWNER = "levfan"
REPO = "are-chat"
API = "/repos/%s/%s" % (OWNER, REPO)
DEFAULT_JAVA_HOME = r"C:\Program Files\Java\jdk-25.0.3"
ASSETS = (
    ("deploy/app.jar", "are-chat-{v}-app.jar"),
    ("deploy/are-chat-{v}.tar", "are-chat-{v}-image.tar"),
)


def log(msg):
    print(msg, flush=True)


def pom_version():
    text = (ROOT / "pom.xml").read_text(encoding="utf-8")
    m = re.search(r"<artifactId>are-chat</artifactId>\s*<version>([^<]+)</version>", text)
    if not m:
        raise RuntimeError("pom.xml 里找不到 are-chat 的 <version>")
    return m.group(1).strip()


def check_version_alignment(ver):
    for rel in ("deploy/build.sh", "deploy/build-base.sh", "deploy/build-business.sh",
                "deploy/compose/opt/.env"):
        path = ROOT / rel
        if not path.exists():
            raise RuntimeError("缺版本对齐文件：%s" % rel)
        lines = [l for l in path.read_text(encoding="utf-8", errors="replace").splitlines()
                 if re.search(r"\bVERSION\s*=", l)]
        if not any(ver in l for l in lines):
            raise RuntimeError("%s 的 VERSION 与 pom.xml 的 %s 不一致" % (rel, ver))


def is_major(ver):
    m = re.match(r"^(\d+)\.(\d+)\.(\d+)", ver)
    return bool(m) and m.group(2) == "0" and m.group(3) == "0"


def git(*args):
    r = subprocess.run(["git"] + list(args), cwd=str(ROOT), text=True,
                       encoding="utf-8", errors="replace", capture_output=True)
    if r.returncode:
        raise RuntimeError("git %s 失败：%s" % (" ".join(args), r.stderr.strip()[:300]))
    return r.stdout.strip()


def token():
    for key in ("GITHUB_TOKEN", "GH_TOKEN"):
        if os.environ.get(key):
            return os.environ[key]
    r = subprocess.run(["git", "credential", "fill"], cwd=str(ROOT),
                       input="protocol=https\nhost=github.com\n\n",
                       text=True, encoding="utf-8", errors="replace", capture_output=True)
    got = dict(l.strip().split("=", 1) for l in r.stdout.splitlines() if "=" in l).get("password", "")
    if not got:
        raise RuntimeError("找不到 GitHub 令牌：设 GITHUB_TOKEN 或配置 git credential")
    return got


TOKEN = None
HEADERS = None


def init_auth():
    global TOKEN, HEADERS
    TOKEN = token()
    HEADERS = {
        "Authorization": "Bearer " + TOKEN,
        "Accept": "application/vnd.github+json",
        "X-GitHub-Api-Version": "2022-11-28",
        "User-Agent": "are-chat-release-assets",
    }


def api(method, path, payload=None):
    data = json.dumps(payload).encode("utf-8") if payload is not None else None
    headers = dict(HEADERS)
    if data is not None:
        headers["Content-Type"] = "application/json"
    req = urllib.request.Request("https://api.github.com" + path, data=data,
                                 headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=60) as r:
            return r.status, json.loads(r.read().decode("utf-8") or "{}")
    except urllib.error.HTTPError as e:
        raw = e.read().decode("utf-8", "replace")
        try:
            return e.code, json.loads(raw)
        except ValueError:
            return e.code, raw[:300]


def api_retry(method, path, payload=None, tries=4, label=""):
    last = None
    for attempt in range(1, tries + 1):
        try:
            st, data = api(method, path, payload)
        except Exception as e:
            st, data = 0, "%s %s" % (type(e).__name__, str(e)[:160])
        if 200 <= st < 300:
            return st, data
        last = (st, data)
        log("RETRY %s HTTP %s %s" % (label or method, st, str(data)[:160]))
        if attempt < tries:
            time.sleep(2 * attempt)
    return last


def run_cmd(cmd, cwd, env=None, shell=False, label="构建"):
    log("RUN %s" % (cmd if isinstance(cmd, str) else " ".join(cmd)))
    p = subprocess.run(cmd, cwd=str(cwd), env=env, shell=shell, text=True,
                       encoding="utf-8", errors="replace",
                       stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    for line in (p.stdout or "").splitlines():
        log("  | " + line.rstrip())
    if p.returncode:
        raise RuntimeError("%s 失败（退出码 %d）" % (label, p.returncode))


def stage_old_artifacts(ver):
    backup = ROOT / ".workbuddy" / ("release-backup-v" + ver + "-" +
                                    datetime.datetime.now().strftime("%Y%m%d-%H%M%S"))
    targets = [ROOT / "deploy" / "app.jar", ROOT / "deploy" / ("are-chat-%s.tar" % ver)]
    moved = []
    for src in targets:
        if src.exists():
            dst = backup / src.name
            dst.parent.mkdir(parents=True, exist_ok=True)
            shutil.move(str(src), str(dst))
            moved.append(src.name)
    if moved:
        log("STAGE old artifacts -> %s | %s" % (backup.relative_to(ROOT), ", ".join(moved)))


def build_backend(ver):
    env = dict(os.environ)
    default_jdk = Path(DEFAULT_JAVA_HOME)
    if not env.get("JAVA_HOME") and default_jdk.exists():
        env["JAVA_HOME"] = DEFAULT_JAVA_HOME
    run_cmd("mvn -q -DskipTests package", ROOT, env=env, shell=True, label="后端构建")
    jar = ROOT / "target" / ("are-chat-%s.jar" % ver)
    if not jar.exists():
        raise RuntimeError("后端 jar 未生成：%s" % jar)
    shutil.copy2(str(jar), str(ROOT / "deploy" / "app.jar"))
    log("BUILD OK backend jar | %s" % jar.name)


def wsl_path(path):
    s = str(path).replace("\\", "/")
    m = re.match(r"^([A-Za-z]):/(.*)$", s)
    return "/mnt/%s/%s" % (m.group(1).lower(), m.group(2)) if m else s


def build_image(ver):
    env = dict(os.environ)
    env["VERSION"] = ver
    wsl = shutil.which("wsl")
    bash = shutil.which("bash")
    if wsl:
        cmd = ["wsl", "bash", "-lc",
               "cd '%s' && VERSION=%s bash deploy/build.sh --skip-base" % (wsl_path(ROOT), ver)]
    elif bash:
        cmd = [bash, "deploy/build.sh", "--skip-base"]
    else:
        raise RuntimeError("找不到 WSL 或 Git Bash，无法构建镜像 tar")
    run_cmd(cmd, ROOT, env=env, label="镜像构建")
    tar = ROOT / "deploy" / ("are-chat-%s.tar" % ver)
    if not tar.exists():
        raise RuntimeError("镜像 tar 未生成：%s" % tar)
    log("BUILD OK image tar | %s" % tar.name)


def sha256(path):
    h = hashlib.sha256()
    with open(str(path), "rb") as f:
        for chunk in iter(lambda: f.read(1 << 20), b""):
            h.update(chunk)
    return h.hexdigest()


def stage_assets(ver):
    out = ROOT / "target" / "release-assets" / ("v" + ver)
    out.mkdir(parents=True, exist_ok=True)
    paths, hashes = {}, {}
    for rel, tmpl in ASSETS:
        src = ROOT / rel.format(v=ver)
        if not src.exists():
            raise RuntimeError("缺资产源文件：%s" % src)
        name = tmpl.format(v=ver)
        dst = out / name
        shutil.copy2(str(src), str(dst))
        paths[name] = dst
        hashes[name] = sha256(dst)
        log("ASSET READY %s | %.1f MB | sha256=%s" %
            (name, dst.stat().st_size / 1048576, hashes[name]))
    return paths, hashes


def make_body(ver, hashes, commit):
    lines = [
        "## are-chat %s" % ver,
        "",
        "后端 jar 与业务镜像 tar 同版本构建；私有化部署加载镜像后按 `deploy/compose/opt` 下的 compose 启动。",
        "数据库口令不随资产分发，凭据由运维单独注入。",
        "",
        "构建提交：`%s`。发布由 `release-assets` 技能同步。" % commit,
        "",
        "### SHA-256",
        "",
    ]
    lines += ["- `%s`: `%s`" % (n, h) for n, h in hashes.items()]
    return "\n".join(lines) + "\n"


def ensure_release(ver, body, prerelease):
    tag = "v" + ver
    st, rel = api("GET", API + "/releases/tags/" + urllib.parse.quote(tag))
    if st == 200:
        log("RELEASE found %s | id=%s" % (tag, rel["id"]))
        st, _ = api_retry("PATCH", API + "/releases/%s" % rel["id"],
                          {"body": body, "prerelease": prerelease}, label="PATCH body")
        if st < 200 or st >= 300:
            raise RuntimeError("更新发布说明失败：HTTP %s" % st)
        log("PATCH body HTTP %s" % st)
        return rel
    if st != 404:
        raise RuntimeError("查询 Release 失败：HTTP %s %s" % (st, str(rel)[:200]))
    st, rel = api_retry("POST", API + "/releases", {
        "tag_name": tag, "name": tag, "body": "are-chat " + ver,
        "draft": False, "prerelease": prerelease, "generate_release_notes": False}, label="POST release")
    if st < 200 or st >= 300:
        raise RuntimeError("创建 Release 失败：HTTP %s %s" % (st, str(rel)[:200]))
    log("RELEASE created %s | id=%s | %s" % (tag, rel["id"], rel.get("html_url", "")))
    st, _ = api_retry("PATCH", API + "/releases/%s" % rel["id"], {"body": body}, label="PATCH body")
    if st < 200 or st >= 300:
        raise RuntimeError("写入发布说明失败：HTTP %s" % st)
    return rel


def asset_urls(rel):
    return {a["name"]: a for a in rel.get("assets", [])}


def body_hashes(rel):
    return dict(re.findall(r"- `([^`]+)`: `([0-9a-f]{64})`", rel.get("body", "")))


def upload(path, name, rel_id):
    size = path.stat().st_size
    url = "%s/releases/%s/assets?name=%s" % (API, rel_id, urllib.parse.quote(name))
    for attempt in range(1, 4):
        try:
            conn = http.client.HTTPSConnection("uploads.github.com", timeout=900)
            conn.putrequest("POST", url)
            for k, v in HEADERS.items():
                conn.putheader(k, v)
            conn.putheader("Content-Type", "application/octet-stream")
            conn.putheader("Content-Length", str(size))
            conn.endheaders()
            sent, mark, t0 = 0, 50 * 1024 * 1024, time.time()
            with open(str(path), "rb") as f:
                while True:
                    chunk = f.read(1 << 20)
                    if not chunk:
                        break
                    conn.send(chunk)
                    sent += len(chunk)
                    if sent >= mark:
                        log("SEND %s %.0f%% | %.0fs" % (name, sent * 100.0 / size, time.time() - t0))
                        mark += 50 * 1024 * 1024
            r = conn.getresponse()
            raw = r.read()
            conn.close()
            if r.status in (200, 201):
                a = json.loads(raw.decode("utf-8"))
                log("UPLOADED %s | %s" % (name, a.get("browser_download_url", "")))
                return
            log("RETRY %s HTTP %s %s" % (name, r.status, raw[:160].decode("utf-8", "replace")))
        except Exception as e:
            log("RETRY %s %s %s" % (name, type(e).__name__, str(e)[:160]))
        if attempt < 3:
            time.sleep(3 * attempt)
    raise RuntimeError("上传失败：" + name)


def sync(ver, hashes, paths):
    init_auth()
    prerelease = bool(re.search(r"-(alpha|beta|rc)\.", ver))
    commit = git("rev-parse", "--short", "HEAD")
    rel = ensure_release(ver, make_body(ver, hashes, commit), prerelease)
    have, recorded = asset_urls(rel), body_hashes(rel)
    for name, path in paths.items():
        if (name in have and have[name]["size"] == path.stat().st_size
                and recorded.get(name) == hashes[name]):
            log("SKIP %s | 资产与 SHA-256 已同步" % name)
            continue
        if name in have:
            st, _ = api("DELETE", API + "/releases/assets/%s" % have[name]["id"])
            if st != 204:
                raise RuntimeError("删除旧资产失败：%s HTTP %s" % (name, st))
            log("REPLACE %s" % name)
        upload(path, name, rel["id"])
    st, final = api("GET", API + "/releases/tags/" + urllib.parse.quote("v" + ver))
    if st != 200:
        raise RuntimeError("复核 Release 失败：HTTP %s" % st)
    ok = set(paths) <= set(asset_urls(final))
    log("FINAL %s | %s" % ("OK" if ok else "MISSING ASSET", final.get("html_url", "")))
    for a in final.get("assets", []):
        log("ASSET %s | %d" % (a["name"], a["size"]))
    if not ok:
        raise RuntimeError("资产清单不齐")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--skip-build", action="store_true", help="复用现有 jar 与镜像 tar")
    ap.add_argument("--allow-dirty", action="store_true", help="允许工作区有已跟踪改动")
    ap.add_argument("--no-upload", action="store_true", help="只构建与打包，不碰 GitHub")
    ap.add_argument("--force", action="store_true", help="非 major 版本也执行，需用户明确要求")
    args = ap.parse_args()
    ver = pom_version()
    if not args.force and not is_major(ver):
        raise RuntimeError("版本 %s 不是 major（X.0.0），发版链路未触发；确需发布加 --force" % ver)
    check_version_alignment(ver)
    if not args.allow_dirty and git("status", "--porcelain", "--untracked-files=no"):
        raise RuntimeError("工作区有已跟踪改动：先提交再发布")
    log("VERSION %s | major=%s" % (ver, is_major(ver)))
    if not args.skip_build:
        stage_old_artifacts(ver)
        build_backend(ver)
        build_image(ver)
    paths, hashes = stage_assets(ver)
    if args.no_upload:
        log("NO-UPLOAD | 本地资产与 SHA-256 已就绪")
        return
    sync(ver, hashes, paths)


if __name__ == "__main__":
    try:
        main()
    except Exception as exc:
        log("FAILED " + str(exc))
        sys.exit(1)
