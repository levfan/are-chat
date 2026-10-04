package com.smart.chat.bootstrap.config;

import com.smart.chat.bootstrap.properties.RequestLogProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * 接口访问日志的装配。
 * 单独成类且刻意不实现 WebMvcConfigurer：@WebMvcTest 切片会纳入 WebMvcConfigurer 但不会纳入普通
 * @Configuration，注册放这里切片才不会因为缺 RequestLogProperties 而起不来（同 MybatisPlusConfig 的口径）。
 */
@Configuration
public class RequestLogConfig {

    @Bean
    public FilterRegistrationBean<RequestLogFilter> requestLogFilterRegistration(RequestLogProperties props) {
        FilterRegistrationBean<RequestLogFilter> registration =
                new FilterRegistrationBean<>(new RequestLogFilter(props));
        registration.setName("requestLogFilter");
        registration.addUrlPatterns("/api/*");
        // 排在字符集过滤器（HIGHEST_PRECEDENCE）之后：先定好编码再读请求体，中文入参才不会解错
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        return registration;
    }
}
