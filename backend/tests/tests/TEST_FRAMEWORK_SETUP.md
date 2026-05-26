# 单元测试框架配置文档

**版本**: v1.0  
**创建日期**: 2026-04-13  
**作者**: qa-lead  

---

## 1. 测试框架概述

本项目使用以下测试技术栈：
- **JUnit 5**: 单元测试框架
- **Mockito**: Mock框架
- **JaCoCo**: 代码覆盖率工具
- **H2**: 集成测试内存数据库
- **Spring Boot Test**: Spring测试支持

---

## 2. Maven依赖配置

在 `smart-admin-web-java/pom.xml` 中添加以下依赖：

```xml
<!-- ==================== 测试依赖 ==================== -->
<!-- JUnit 5 -->
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>

<!-- Mockito -->
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-core</artifactId>
    <scope>test</scope>
</dependency>

<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>

<!-- Spring Boot Test -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
    <exclusions>
        <exclusion>
            <groupId>org.junit.vintage</groupId>
            <artifactId>junit-vintage-engine</artifactId>
        </exclusion>
    </exclusions>
</dependency>

<!-- H2 Database for Testing -->
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>test</scope>
</dependency>

<!-- TestContainers for Integration Testing -->
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>

<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <scope>test</scope>
</dependency>
```

---

## 3. JaCoCo配置

在 `smart-admin-web-java/pom.xml` 的 `<build><plugins>` 中添加：

```xml
<!-- JaCoCo 代码覆盖率插件 -->
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.11</version>
    <configuration>
        <excludes>
            <!-- 排除不需要测试的类 -->
            <exclude>**/entity/**</exclude>
            <exclude>**/dto/**</exclude>
            <exclude>**/vo/**</exclude>
            <exclude>**/config/**</exclude>
            <exclude>**/aspect/**</exclude>
        </excludes>
    </configuration>
    <executions>
        <execution>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals>
                <goal>report</goal>
            </goals>
            <configuration>
                <outputDirectory>${project.basedir}/../tests/report/jacoco</outputDirectory>
            </configuration>
        </execution>
        <execution>
            <id>check</id>
            <goals>
                <goal>check</goal>
            </goals>
            <configuration>
                <rules>
                    <rule>
                        <element>PACKAGE</element>
                        <limits>
                            <limit>
                                <counter>LINE</counter>
                                <value>COVEREDRATIO</value>
                                <minimum>0.80</minimum>
                            </limit>
                            <limit>
                                <counter>BRANCH</counter>
                                <value>COVEREDRATIO</value>
                                <minimum>0.70</minimum>
                            </limit>
                        </limits>
                    </rule>
                </rules>
            </configuration>
        </execution>
    </executions>
</plugin>
```

---

## 4. 测试类结构

```
src/test/java/com/aiready/
├── AbstractTest.java                    # 单元测试基类
├── AbstractIntegrationTest.java         # 集成测试基类
└── system/
    └── service/
        ├── DepartmentServiceTest.java   # 部门服务测试
        └── PositionServiceTest.java     # 岗位服务测试
```

---

## 5. 测试执行命令

### 5.1 执行所有测试
```bash
cd I:\AI-Ready\smart-admin-web-java
mvn test
```

### 5.2 执行特定测试类
```bash
mvn test -Dtest=DepartmentServiceTest
```

### 5.3 生成覆盖率报告
```bash
mvn jacoco:report
```

### 5.4 执行测试并检查覆盖率
```bash
mvn clean test jacoco:check
```

---

## 6. 测试报告位置

- **测试报告**: `I:\AI-Ready\smart-admin-web-java\target\surefire-reports`
- **覆盖率报告**: `I:\AI-Ready\tests\report\jacoco\index.html`

---

## 7. 测试覆盖率要求

| 模块 | 行覆盖率 | 分支覆盖率 |
|------|----------|------------|
| DepartmentService | ≥80% | ≥70% |
| PositionService | ≥80% | ≥70% |
| UserService | ≥80% | ≥70% |

---

## 8. 注意事项

1. 测试类必须继承 `AbstractTest` 或 `AbstractIntegrationTest`
2. 使用 `@DisplayName` 注解描述测试用例
3. 使用 `@Nested` 组织相关测试
4. 每个测试方法应包含 Given-When-Then 注释
5. 覆盖率报告生成后，检查是否达到80%要求
