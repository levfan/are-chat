import {execFileSync} from 'node:child_process';
import fs from 'node:fs';

const [rev, scope] = process.argv.slice(2);
const RE = {
  strings: /"[^"\n]*[\u4e00-\u9fff][^"\n]*"/g,
  routes: /@(?:Get|Post|Put|Delete|Request)Mapping\([^)]*\)/g,
  events: /pushCouple[A-Za-z]*\(\s*"[^"]+"/g,
};

const list = rev === 'WORK' ? walk(scope) : execFileSync('git', ['ls-tree', '-r', '--name-only', rev, '--', scope],
  { encoding: 'utf8' }).split('\n').filter(Boolean);

function walk(dir, out = []) {
  for (const e of fs.readdirSync(dir, { withFileTypes: true })) {
    const p = `${dir}/${e.name}`;
    if (e.isDirectory()) walk(p, out);
    else if (e.name.endsWith('.java')) out.push(p);
  }
  return out;
}

const out = {};
for (const [key, re] of Object.entries(RE)) {
  const counts = new Map();
  for (const f of list) {
    const txt = rev === 'WORK' ? fs.readFileSync(f, 'utf8')
      : execFileSync('git', ['show', `${rev}:${f}`], { encoding: 'utf8', maxBuffer: 64e6 });
    for (const m of txt.match(re) ?? []) counts.set(m, (counts.get(m) ?? 0) + 1);
  }
  out[key] = [...counts.entries()].sort((a, b) => a[0].localeCompare(b[0], 'zh'));
}
console.log(JSON.stringify(out));
