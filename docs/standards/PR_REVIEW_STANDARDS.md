# 智企连·AI-Ready PR 审核规范

**版本**: v1.0  
**日期**: 2026-03-27  
**项目**: 智企连·AI-Ready

---

## 1. PR 流程概览

```
开发者提交 PR → 自动化检查 → 代码审核 → 审核通过 → 合并到主分支
                      ↓
                   检查失败 → 修复问题 → 重新提交
```

---

## 2. PR 提交前检查

### 2.1 开发者自查清单

#### 代码质量
- [ ] 代码符合命名规范
- [ ] 无重复代码
- [ ] 无硬编码值
- [ ] 注释清晰完整
- [ ] 无无用代码（注释掉的代码、调试代码）

#### 功能正确性
- [ ] 功能符合需求描述
- [ ] 边界条件处理完整
- [ ] 异常情况处理完整
- [ ] 无安全漏洞

#### 测试覆盖
- [ ] 单元测试已编写
- [ ] 测试用例覆盖核心逻辑
- [ ] 测试全部通过
- [ ] 无测试代码警告

#### 文档更新
- [ ] README 已更新（如需要）
- [ ] API 文档已更新（如需要）
- [ ] CHANGELOG 已更新（如需要）

---

## 3. 自动化检查项

### 3.1 CI 检查

| 检查项 | 工具 | 要求 |
|--------|------|------|
| 代码编译 | Maven/Gradle | 必须通过 |
| 单元测试 | JUnit | 覆盖率 ≥ 60% |
| 代码风格 | Checkstyle/ESLint | 无严重问题 |
| 静态分析 | SonarQube | 无阻断/严重问题 |
| 安全扫描 | Dependency Check | 无已知漏洞 |

### 3.2 GitHub Actions 配置

```yaml
# .github/workflows/pr-check.yml
name: PR Check

on:
  pull_request:
    branches: [develop, main]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      
      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
          
      - name: Build with Maven
        run: mvn clean install -DskipTests
        
      - name: Run tests
        run: mvn test
        
      - name: Checkstyle
        run: mvn checkstyle:check
        
      - name: SonarQube Scan
        uses: sonarsource/sonarqube-scan-action@master
        env:
          SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
```

---

## 4. 代码审核要点

### 4.1 后端代码审核

#### 架构设计
- [ ] 分层是否正确（Controller → Service → Repository）
- [ ] 模块职责是否单一
- [ ] 依赖注入是否正确使用
- [ ] 是否遵循 SOLID 原则

#### 安全性
- [ ] 输入参数是否校验
- [ ] 是否存在 SQL 注入风险
- [ ] 敏感数据是否加密
- [ ] 权限控制是否完善
- [ ] 是否存在 XSS/CSRF 风险

#### 性能
- [ ] 数据库查询是否优化
- [ ] 是否存在 N+1 问题
- [ ] 缓存使用是否合理
- [ ] 是否避免大对象频繁创建

#### 异常处理
- [ ] 异常是否正确捕获和处理
- [ ] 错误信息是否友好
- [ ] 是否记录必要日志

#### 代码示例

```java
// ❌ 问题代码
@GetMapping("/user/{id}")
public User getUser(@PathVariable Long id) {
    return userRepository.findById(id).orElse(null);
}

// ✅ 审核意见：需要添加权限校验和异常处理
@GetMapping("/user/{id}")
@SaCheckPermission("user:view")
public Result<UserVO> getUser(@PathVariable Long id) {
    User user = userRepository.findById(id)
        .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "用户不存在"));
    return Result.success(toVO(user));
}
```

### 4.2 前端代码审核

#### 组件设计
- [ ] 组件职责是否单一
- [ ] Props 定义是否完整
- [ ] 是否正确使用组合式 API
- [ ] 是否避免过度渲染

#### 状态管理
- [ ] 状态是否合理划分
- [ ] 是否正确使用 Pinia
- [ ] 是否避免状态冗余

#### 性能
- [ ] 是否使用懒加载
- [ ] 是否避免不必要的重渲染
- [ ] 是否正确使用 computed/watch

#### 代码示例

```vue
<!-- ❌ 问题代码 -->
<script setup>
const data = ref([])
const search = () => {
  fetch('/api/users').then(r => r.json()).then(d => data.value = d)
}
</script>

<!-- ✅ 审核意见：需要封装 API、添加类型、处理异常 -->
<script setup lang="ts">
import type { User } from '@/types/user'
import { userApi } from '@/api/user'

const loading = ref(false)
const data = ref<User[]>([])

const search = async () => {
  loading.value = true
  try {
    data.value = await userApi.getList({})
  } catch (e) {
    message.error('获取用户列表失败')
  } finally {
    loading.value = false
  }
}
</script>
```

---

## 5. 审核反馈规范

### 5.1 审核意见等级

| 等级 | 标记 | 说明 | 处理方式 |
|------|------|------|----------|
| 必须修改 | `🚫 MUST` | 阻塞问题，必须修复 | 阻止合并 |
| 建议修改 | `💡 SUGGEST` | 改进建议，强烈建议采纳 | 可讨论 |
| 问题讨论 | `❓ QUESTION` | 需要澄清的问题 | 需回复 |
| 赞同 | `👍 APPROVE` | 同意当前实现 | - |

### 5.2 审核意见格式

```markdown
**[MUST]** 这是一个安全问题，必须修复

这段代码存在 SQL 注入风险，请使用参数化查询：

```java
// 建议修改为
@Select("SELECT * FROM users WHERE username = #{username}")
User findByUsername(@Param("username") String username);
```
```

### 5.3 审核意见示例

```markdown
## 代码审核报告

### 🚫 必须修改

1. **UserController.java:45**
   - 问题：缺少权限校验
   - 建议：添加 `@SaCheckPermission("user:create")` 注解

2. **OrderService.java:128**
   - 问题：未处理空指针异常
   - 建议：使用 Optional 或添加 null 检查

### 💡 建议修改

1. **UserDTO.java**
   - 建议：使用 Java 17 record 简化代码
   ```java
   public record UserDTO(Long id, String username, String email) {}
   ```

### ❓ 问题讨论

1. **支付模块设计**
   - 是否考虑了支付超时的情况？
   - 退款流程如何处理？

### 👍 优点

1. 测试覆盖率高，核心逻辑都有测试
2. 代码结构清晰，易于维护
```

---

## 6. 审核流程

### 6.1 审核时间要求

| PR 类型 | 审核时间 | 审核人数 |
|---------|----------|----------|
| 功能开发 | 2 个工作日内 | ≥ 1 人 |
| Bug 修复 | 1 个工作日内 | ≥ 1 人 |
| 紧急修复 | 4 小时内 | ≥ 1 人 |
| 架构变更 | 3 个工作日内 | ≥ 2 人 |

### 6.2 审核流程图

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│  提交 PR    │────→│  CI 检查    │────→│  代码审核   │
└─────────────┘     └─────────────┘     └─────────────┘
                           │                   │
                           ↓                   ↓
                    ┌─────────────┐     ┌─────────────┐
                    │  检查失败   │     │  审核通过   │
                    └─────────────┘     └─────────────┘
                           │                   │
                           ↓                   ↓
                    ┌─────────────┐     ┌─────────────┐
                    │  修复问题   │     │  合并代码   │
                    └─────────────┘     └─────────────┘
```

### 6.3 合并条件

- [ ] CI 构建通过
- [ ] 至少 1 人审核通过
- [ ] 无未解决的 MUST 意见
- [ ] 所有讨论已解决
- [ ] 分支已同步最新代码

---

## 7. 审核者职责

### 7.1 审核者责任

1. **保证代码质量**
   - 审核代码逻辑正确性
   - 检查代码规范遵守情况
   - 评估代码可维护性

2. **提供建设性反馈**
   - 明确指出问题
   - 提供改进建议
   - 尊重开发者

3. **及时响应**
   - 在规定时间内完成审核
   - 及时回复开发者的提问

### 7.2 审核者礼仪

- ✅ 使用礼貌用语
- ✅ 针对代码而非个人
- ✅ 解释原因和建议
- ✅ 肯定好的代码
- ❌ 使用攻击性语言
- ❌ 只批评不提建议
- ❌ 延迟审核不说明原因

---

## 8. 开发者职责

### 8.1 响应审核意见

1. **及时响应**
   - 在 1 个工作日内回复
   - 说明修改计划

2. **认真对待**
   - 理解审核意见
   - 按建议修改或说明原因

3. **保持沟通**
   - 有疑问及时讨论
   - 避免误解

### 8.2 回复模板

```markdown
感谢审核意见，我会尽快修改。

> **[MUST]** 需要添加权限校验

已修改，请查看 commit abc123。

> **[SUGGEST]** 建议使用 record

这是一个好建议，我在下一个 PR 中优化。

> **[QUESTION]** 是否考虑了超时情况？

是的，已经添加了 30 秒超时配置。
```

---

## 9. 审核工具

### 9.1 GitHub PR Review

- 使用 "Request changes" 标记必须修改
- 使用 "Comment" 标记建议
- 使用 "Approve" 表示通过

### 9.2 辅助工具

| 工具 | 用途 |
|------|------|
| SonarQube | 代码质量分析 |
| Codecov | 测试覆盖率 |
| Dependabot | 依赖安全检查 |
| Reviewable | 代码审核增强 |

---

## 10. 参考资料

- [Google Engineering Practices](https://google.github.io/eng-practices/review/)
- [Thoughtbot Code Review Guide](https://github.com/thoughtbot/guides/tree/main/code-review)

---

**文档更新**: 2026-03-27  
**维护者**: doc-writer