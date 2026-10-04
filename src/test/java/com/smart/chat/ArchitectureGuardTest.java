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
 *   <li>{@code domain} 层保持纯净——不 import 框架，也不 import 同上下文的其它层；</li>
 *   <li>跨上下文只能经 {@code domain}（端口/发布语言）、sharedkernel 或 bootstrap.properties；</li>
 *   <li>扫描面非空——路径写错时守卫必须当场失效，而不是静默恒绿。</li>
 * </ol>
 */
class ArchitectureGuardTest {

    private static final Path ROOT = Path.of("src/main/java/com/smart/chat");
    private static final Set<String> CONTEXTS = Set.of("couple", "messaging", "identity", "platform", "filestorage");
    private static final Set<String> LAYERS = Set.of("api", "application", "domain", "infrastructure");
    private static final Pattern IMPORT = Pattern.compile("^import\\s+(?:static\\s+)?([\\w.]+);", Pattern.MULTILINE);
    private static final List<String> FORBIDDEN_IN_DOMAIN = List.of(
            "org.springframework", "com.baomidou", "org.apache.ibatis", "jakarta.servlet", "com.alibaba.fastjson");

    /** com.smart.chat.〈上下文〉.〈层〉.… 里的上下文名；非本仓 import 或太短的返回 null */
    private static String contextOf(String fqn) {
        if (!fqn.startsWith("com.smart.chat.")) {
            return null;
        }
        String[] parts = fqn.split("\\.");
        return parts.length >= 4 ? parts[3] : null;
    }

    private record Src(String file, String context, String layer, List<String> imports) {
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
                out.add(new Src(rel, context, layer, imports));
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
        }
        assertThat(bad).as("domain 层的框架/跨层依赖").isEmpty();
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
