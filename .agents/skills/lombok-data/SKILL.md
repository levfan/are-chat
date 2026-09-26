---
name: lombok-data
description: are-chat POJO 规范：实体/DTO/VO 等数据类一律用 Lombok @Data（或 @Getter/@Setter），禁止手写成堆的 getter/setter；含注解选型与 MyBatis-Plus/fastjson2/equals 注意事项
whenToUse: 新建或修改 Java 实体、DTO、VO、配置属性类时使用
---

# Lombok 数据类规范（lombok-data）

## 硬性规则

- 新建实体、DTO、VO、配置属性等数据类：类上加 Lombok 注解，**禁止手写 getter/setter 模板代码**
- 默认 `@Data`（= @Getter + @Setter + @ToString + @EqualsAndHashCode + @RequiredArgsConstructor）；只需访问器、不需要 equals/hashCode/toString 的类用 `@Getter` + `@Setter`
- 某个字段需要特殊逻辑时，只手写那一个方法——Lombok 遇到同名已存在方法会自动跳过
- 改到旧类时：属于本次任务范围的，可顺手把手写 getter/setter 换成注解并删除；不做与任务无关的全库无差别重构
- 依赖已就绪：pom.xml 已有 `org.projectlombok:lombok`（optional，版本由 Spring Boot 父 POM 管理），不要重复添加

## 注解选型

- `@Data`：默认选择。MyBatis-Plus 实体、普通 DTO/VO
- `@Getter` / `@Setter`：只要访问器的类
- `@RequiredArgsConstructor` + `final` 字段：Spring 组件构造注入（Service/Controller 用这种，不用 @Autowired 字段注入）
- `@Slf4j`：替代手写 Logger 声明
- `@Builder`：多字段构建场景，可与 @Data 同用

## 本项目注意事项

1. equals/hashCode：@Data 默认基于全部字段生成。实体要放进 Set/Map 或做身份比较时，显式写 `@EqualsAndHashCode(of = "id")`；有继承的类加 `@EqualsAndHashCode(callSuper = true)`
2. MyBatis-Plus：`@TableName`/`@TableId`/`@TableField` 照常标注在类/字段上，与 @Data 互不冲突
3. fastjson2：序列化走 getter，@Data 生成的 getter 正常工作；原始 `boolean` 字段 Lombok 生成的是 `isXxx()`，若对前端字段名敏感建议用包装类型 `Boolean`
4. Spring 配置属性类（@ConfigurationProperties，如 FileStorageProperties）：用 `@Data` 即可

## 示例

手写 6 组 getter/setter 的旧风格（见 im/UserProfile.java）等价改造为：

```java
import lombok.Data;

@Data
@TableName("user_profile")
public class UserProfile {

    @TableId(value = "username", type = IdType.INPUT)
    private String username;
    private String nickname;
    private String signature;
    private String avatar;
    private String presenceStatus;
    private Long updatedAt;
}
```

需要覆写个别方法时再单独手写那一个，其余全部由 Lombok 生成。
