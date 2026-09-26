package com.smart.chat.im;

import com.smart.chat.common.BusinessException;
import com.smart.chat.config.ModerationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;

/**
 * 86 敏感词过滤：发送文本时按词库处理。
 * - mode=censor（默认）：命中词替换为等长 ＊，消息照发
 * - mode=block：命中直接 400 拦截
 * 词库来自 arechat.moderation.sensitive-words 配置（yml 数组或逗号分隔环境变量），
 * 运维改配置重启即生效；空词库时完全放行。
 */
@Service
public class ModerationService {

    private static final Logger log = LoggerFactory.getLogger(ModerationService.class);
    private static final char MASK = '＊';

    private final ModerationProperties properties;
    /** 预编译的命中模式（大小写不敏感） */
    private final List<Pattern> patterns = new CopyOnWriteArrayList<>();

    public ModerationService(ModerationProperties properties) {
        this.properties = properties;
        rebuild();
    }

    /** 配置变化后重建词库（Spring 绑定时机完成后调用一次；测试可重复调用） */
    public synchronized void rebuild() {
        patterns.clear();
        if (!properties.enabled()) {
            return;
        }
        for (String word : properties.sensitiveWords()) {
            String clean = word == null ? "" : word.trim();
            if (clean.isEmpty()) {
                continue;
            }
            patterns.add(Pattern.compile(Pattern.quote(clean), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE));
        }
        if (!patterns.isEmpty()) {
            log.info("敏感词过滤已启用：{} 个词，mode={}", patterns.size(), properties.mode());
        }
    }

    /** 过滤入口：文本消息发送前调用，返回处理后的文本 */
    public String clean(String username, String text) {
        if (patterns.isEmpty() || text == null || text.isEmpty()) {
            return text;
        }
        for (Pattern pattern : patterns) {
            var matcher = pattern.matcher(text);
            if (!matcher.find()) {
                continue;
            }
            if (properties.blockMode()) {
                throw new BusinessException(400, "消息包含不允许的内容，请修改后重试");
            }
            text = matcher.replaceAll(match -> String.valueOf(MASK).repeat(match.group().length()));
        }
        return text;
    }

    /** 词库是否生效（测试/健康展示用） */
    public boolean active() {
        return properties.enabled() && !patterns.isEmpty();
    }
}
