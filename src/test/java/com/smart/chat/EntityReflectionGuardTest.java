package com.smart.chat;

import com.baomidou.mybatisplus.annotation.TableName;
import org.apache.ibatis.reflection.DefaultReflectorFactory;
import org.apache.ibatis.reflection.ReflectionException;
import org.apache.ibatis.reflection.Reflector;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 实体反射守卫：MyBatis 给同一属性建 Reflector 时，若同时存在 {@code getX()}（Integer/Long 等非 Boolean）
 * 与手写的 {@code isX()}（boolean），会按方法枚举顺序随机抛
 * ReflectionException「ambiguous type for property」——实测已让 /api/couple/world/world 与
 * /api/couple/legacy/vault 整页 500（两家与朋友、传世系统两批功能对每个用户都不可用）。
 * 这里把所有 @TableName 实体过一遍 Reflector，把「靠运气的方法顺序」变成构建期确定性失败。
 */
class EntityReflectionGuardTest {

    private static final String BASE_PACKAGE = "com.smart.chat";

    @Test
    void everyEntityClassHasUnambiguousGetters() throws Exception {
        List<Class<?>> entities = scanEntities();
        assertThat(entities).isNotEmpty();

        List<String> broken = new ArrayList<>();
        DefaultReflectorFactory factory = new DefaultReflectorFactory();
        for (Class<?> clazz : entities) {
            Object instance;
            try {
                instance = clazz.getDeclaredConstructor().newInstance();
            } catch (ReflectiveOperationException ex) {
                broken.add(clazz.getSimpleName() + " → 无法实例化：" + ex);
                continue;
            }
            // 只建 Reflector 不会抛：歧义 getter 会被包成 AmbiguousMethodInvoker，
            // 真正读取属性值（MyBatis 给 #{et.xxx} 取值时就是这步）才抛——所以必须逐个 invoke
            Reflector reflector = factory.findForClass(clazz);
            for (java.lang.reflect.Field field : clazz.getDeclaredFields()) {
                if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                try {
                    reflector.getGetInvoker(field.getName()).invoke(instance, new Object[0]);
                } catch (ReflectionException ex) {
                    broken.add(clazz.getSimpleName() + "." + field.getName() + " → "
                            + String.valueOf(ex.getMessage()).replaceAll("\\s+", " ").trim());
                } catch (ReflectiveOperationException | RuntimeException ex) {
                    broken.add(clazz.getSimpleName() + "." + field.getName() + " → 读取失败 " + ex);
                }
            }
        }
        assertThat(broken)
                .as("实体里 Integer/Long 字段配 isXxx() 布尔 getter 会让 MyBatis 取值时抛歧义（改成 xxxFlag() 命名）")
                .isEmpty();
    }

    /** 用 Spring 的 classpath 扫描器找实体，避免依赖具体容器或数据库。 */
    private List<Class<?>> scanEntities() throws Exception {
        var resolver = new PathMatchingResourcePatternResolver();
        var readerFactory = new CachingMetadataReaderFactory(resolver);
        var resources = resolver.getResources(
                "classpath*:" + BASE_PACKAGE.replace('.', '/') + "/**/*.class");
        List<Class<?>> out = new ArrayList<>();
        for (var resource : resources) {
            MetadataReader reader = readerFactory.getMetadataReader(resource);
            String className = reader.getClassMetadata().getClassName();
            if (className.contains("$")) {
                continue;
            }
            Class<?> clazz = Class.forName(className, false, getClass().getClassLoader());
            if (clazz.isAnnotationPresent(TableName.class)) {
                out.add(clazz);
            }
        }
        return out;
    }
}
