package com.smart.chat.config;

import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.support.config.FastJsonConfig;
import com.alibaba.fastjson2.support.spring6.http.converter.FastJsonHttpMessageConverter;
import org.springframework.boot.http.converter.autoconfigure.ServerHttpMessageConvertersCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 用 fastjson2 接管全部 application/json 的序列化与反序列化。
 * Spring Boot 4 的推荐方式：通过 ServerHttpMessageConvertersCustomizer 把默认 JSON 转换器
 * 替换为 FastJsonHttpMessageConverter（替代 Framework 7 已废弃的 extendMessageConverters）。
 * 实现 WebMvcConfigurer 是为了让 @WebMvcTest 切片也能纳入本配置。
 */
@Configuration
public class FastJsonWebConfig implements WebMvcConfigurer {

    @Bean
    public ServerHttpMessageConvertersCustomizer fastJsonHttpMessageConvertersCustomizer() {
        return builder -> builder.withJsonConverter(fastJsonHttpMessageConverter());
    }

    private FastJsonHttpMessageConverter fastJsonHttpMessageConverter() {
        FastJsonHttpMessageConverter converter = new FastJsonHttpMessageConverter();
        FastJsonConfig config = new FastJsonConfig();
        config.setCharset(StandardCharsets.UTF_8);
        config.setDateFormat("yyyy-MM-dd HH:mm:ss");
        config.setReaderFeatures(JSONReader.Feature.SupportSmartMatch);
        config.setWriterFeatures(
                JSONWriter.Feature.WriteMapNullValue,
                JSONWriter.Feature.WriteNullListAsEmpty
        );
        converter.setFastJsonConfig(config);
        // MediaTypes 携带 UTF-8：保证响应 Content-Type 为 application/json;charset=UTF-8，中文不乱码
        converter.setSupportedMediaTypes(List.of(
                new MediaType("application", "json", StandardCharsets.UTF_8),
                new MediaType("application", "*+json", StandardCharsets.UTF_8)
        ));
        return converter;
    }
}
