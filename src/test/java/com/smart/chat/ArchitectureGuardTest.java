package com.smart.chat;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DDD 架构守卫（见 docs/adr/0004 与 docs/ddd/02-layering.md 第五节）。
 * <p>
 * 不引 ArchUnit：本机 Maven 离线仓库里没有它，而所有验证都在 mvn -o 下跑。这里直接扫源码，
 * 判四件事，任何一条破口都让构建红：
 * <ol>
 *   <li>上下文之间不许有循环依赖（DFS 找环）；</li>
 *   <li>{@code domain} 层保持纯净——不 import 框架，也不 import 同上下文的其它层，还不带 Spring 注解；</li>
 *   <li>跨上下文只能经 {@code domain}（端口/发布语言）、sharedkernel 或 bootstrap.properties；</li>
 *   <li>{@code application}/{@code domain} 不得直接摸本上下文的 PO 与 Mapper（战术改造收口进度，见 ADR-0008 第 3 条）；</li>
 *   <li>扫描面非空——路径写错时守卫必须当场失效，而不是静默恒绿。</li>
 * </ol>
 */
class ArchitectureGuardTest {

    private static final Path ROOT = Path.of("src/main/java/com/smart/chat");
    private static final Set<String> CONTEXTS = Set.of("couple", "messaging", "identity", "platform");
    private static final Set<String> LAYERS = Set.of("api", "application", "domain", "infrastructure");
    private static final Pattern IMPORT = Pattern.compile("^import\\s+(?:static\\s+)?([\\w.]+);", Pattern.MULTILINE);
    private static final List<String> FORBIDDEN_IN_DOMAIN = List.of(
            "org.springframework", "com.baomidou", "org.apache.ibatis", "jakarta.servlet", "com.alibaba.fastjson");
    /** 领域层里出现这些注解（含全限定写法）就说明容器/ORM 语义渗进了业务模型 */
    private static final Pattern FORBIDDEN_ANNOTATION_IN_DOMAIN = Pattern.compile(
            "@(?:[\\w.]+\\.)?(?:Component|Service|Repository|Autowired|Resource|TableName|TableId|TableField)\\b");

    /** com.smart.chat.〈上下文〉.〈层〉.… 里的上下文名；非本仓 import 或太短的返回 null */
    private static String contextOf(String fqn) {
        if (!fqn.startsWith("com.smart.chat.")) {
            return null;
        }
        String[] parts = fqn.split("\\.");
        return parts.length >= 4 ? parts[3] : null;
    }

    private record Src(String file, String context, String layer, List<String> imports, String text) {
    }

    private List<Src> scan() {
        if (!Files.isDirectory(ROOT)) {
            throw new IllegalStateException("找不到源码目录 " + ROOT.toAbsolutePath() + "，扫描面为空等于没有守卫");
        }
        List<Src> out = new ArrayList<>();
        try (Stream<Path> stream = Files.walk(ROOT)) {
            stream.filter(p -> p.toString().endsWith(".java")).forEach(p -> {
                String rel = ROOT.relativize(p).toString().replace('\\', '/');
                String[] seg = rel.split("/");
                String context = seg[0];
                String layer = seg.length > 2 ? seg[1] : "";
                String text;
                try {
                    text = Files.readString(p);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
                List<String> imports = new ArrayList<>();
                Matcher m = IMPORT.matcher(text);
                while (m.find()) {
                    imports.add(m.group(1));
                }
                out.add(new Src(rel, context, layer, imports, text));
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return out;
    }

    /** 守卫自己先得被证明不是空转：扫描面必须覆盖全部主源码 */
    @Test
    void scanCoversWholeMainSource() {
        assertThat(scan())
                .as("主源码文件数（写错路径或过滤条件时这里就会红）")
                .hasSizeGreaterThanOrEqualTo(160);
    }

    @Test
    void contextsHaveNoCyclicDependencies() {
        Map<String, Set<String>> edges = new HashMap<>();
        for (Src s : scan()) {
            for (String imp : s.imports()) {
                String to = contextOf(imp);
                if (to == null || !CONTEXTS.contains(to) || to.equals(s.context())) {
                    continue;
                }
                edges.computeIfAbsent(s.context(), k -> new HashSet<>()).add(to);
            }
        }
        List<String> cycles = new ArrayList<>();
        Map<String, Integer> state = new HashMap<>();
        Deque<String> stack = new ArrayDeque<>();
        for (String node : CONTEXTS) {
            dfs(node, edges, state, stack, cycles);
        }
        assertThat(cycles)
                .as("上下文之间的循环依赖（拆法见 docs/ddd/01-context-map.md 第二节）")
                .isEmpty();
    }

    private void dfs(String node, Map<String, Set<String>> edges, Map<String, Integer> state,
                     Deque<String> stack, List<String> cycles) {
        state.put(node, 1);
        stack.push(node);
        for (String next : edges.getOrDefault(node, Set.of())) {
            Integer mark = state.get(next);
            if (mark == null) {
                dfs(next, edges, state, stack, cycles);
            } else if (mark == 1) {
                List<String> path = new ArrayList<>(stack);
                java.util.Collections.reverse(path);
                int at = path.indexOf(next);
                cycles.add(String.join(" → ", path.subList(at, path.size())) + " → " + next);
            }
        }
        stack.pop();
        state.put(node, 2);
    }

    @Test
    void domainLayerStaysPure() {
        List<String> bad = new ArrayList<>();
        for (Src s : scan()) {
            if (!"domain".equals(s.layer())) {
                continue;
            }
            for (String imp : s.imports()) {
                for (String prefix : FORBIDDEN_IN_DOMAIN) {
                    if (imp.startsWith(prefix)) {
                        bad.add(s.file() + " → " + imp);
                    }
                }
                if (imp.startsWith("com.smart.chat.") && !imp.contains(".domain.") && !imp.contains(".sharedkernel.")) {
                    bad.add(s.file() + " → " + imp + "（domain 不得依赖别的层）");
                }
            }
            Matcher annotation = FORBIDDEN_ANNOTATION_IN_DOMAIN.matcher(s.text());
            while (annotation.find()) {
                bad.add(s.file() + " → " + annotation.group() + "（domain 不得带容器/ORM 注解）");
            }
        }
        assertThat(bad).as("domain 层的框架依赖、跨层依赖与注解").isEmpty();
    }

    /**
     * 战术改造的收口账（ADR-0008 第 3 条）：application/domain 只能通过仓储端口取数，PO 与 Mapper 不许外泄。
     * <p>
     * 账本不是"永久豁免名单"，而是<b>由实测违规集合反推出来的</b>：哪个上下文还在摸 PO，它就出现在账本里；
     * 一旦收口干净就必须从 {@link #TACTICAL_PENDING} 删掉——否则这条测试红。
     * 两个方向都拦：新代码不许把 PO 递出 infrastructure，记账也不许停在过期状态假装还在改造。
     */
    private static final Set<String> TACTICAL_PENDING = Set.of("couple", "messaging", "platform", "filestorage");

    @Test
    void persistenceTypesStayBehindRepositoryPorts() {
        Map<String, List<String>> leaks = poLeaks();
        List<String> bad = new ArrayList<>();
        for (Map.Entry<String, List<String>> e : leaks.entrySet()) {
            if (TACTICAL_PENDING.contains(e.getKey())) {
                continue;
            }
            e.getValue().forEach(v -> bad.add(e.getKey() + " / " + v));
        }
        assertThat(bad)
                .as("已收口上下文的 application/domain 直接 import 本上下文 PO/Mapper。"
                        + "手法见 docs/ddd/05-tactical-playbook.md 第二节")
                .isEmpty();
    }

    /** 收口账必须与实测一致：既不能漏记（第 4 条红），也不能多记（这里红） */
    @Test
    void tacticalLedgerMatchesReality() {
        assertThat(poLeaks().keySet())
                .as("TACTICAL_PENDING 与实测违规的上下文不一致：改造收口后要同步删账（见 ADR-0008 第 3 条）")
                .containsExactlyInAnyOrder(TACTICAL_PENDING.toArray(new String[0]));
    }

    private Map<String, List<String>> poLeaks() {
        Map<String, List<String>> out = new HashMap<>();
        for (Src s : scan()) {
            if (!"application".equals(s.layer()) && !"domain".equals(s.layer())) {
                continue;
            }
            String own = "com.smart.chat." + s.context() + ".infrastructure.persistence";
            for (String imp : s.imports()) {
                if (imp.startsWith(own)) {
                    out.computeIfAbsent(s.context(), k -> new ArrayList<>()).add(s.file() + " → " + imp);
                }
            }
        }
        return out;
    }

    @Test
    void crossContextImportsGoThroughPorts() {
        List<String> bad = new ArrayList<>();
        for (Src s : scan()) {
            for (String imp : s.imports()) {
                String[] parts = imp.split("\\.");
                if (parts.length < 5) {
                    continue;
                }
                String to = parts[3];
                if (to.equals(s.context()) || to.equals("sharedkernel")) {
                    continue;
                }
                if (to.equals("bootstrap")) {
                    // 只放行 @ConfigurationProperties 值绑定；装配类被业务 import 就是方向反了
                    if (!imp.startsWith("com.smart.chat.bootstrap.properties.")) {
                        bad.add(s.file() + " → " + imp + "（业务不该 import 装配层）");
                    }
                    continue;
                }
                if (!CONTEXTS.contains(to)) {
                    continue;
                }
                if (!imp.startsWith("com.smart.chat." + to + ".domain.")) {
                    bad.add(s.file() + " → " + imp + "（跨上下文只能经 " + to + ".domain 的端口/发布语言）");
                }
            }
        }
        assertThat(bad)
                .as("跨上下文越界。历史基线：裁剪轮实测 24 处，Phase C 后为 0——只许减少，不许新增")
                .isEmpty();
    }
}
