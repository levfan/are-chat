package com.smart.chat.config;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.spring.MybatisSqlSessionFactoryBean;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * 手工装配 MyBatis-Plus：MP 官方 starter 的自动配置面向 Spring Boot 3，
 * 在 Boot 4 下不生效，这里显式提供 SqlSessionFactory，Mapper 由 @MapperScan 扫描注册。
 * 本类是普通 @Configuration，不会进入 @WebMvcTest 切片上下文。
 */
@Configuration
@MapperScan(basePackages = "com.smart.chat", annotationClass = Mapper.class)
public class MybatisPlusConfig {

    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
        MybatisSqlSessionFactoryBean factoryBean = new MybatisSqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        MybatisConfiguration configuration = new MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true);
        factoryBean.setConfiguration(configuration);
        return factoryBean.getObject();
    }
}
