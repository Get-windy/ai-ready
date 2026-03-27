# 智企连·AI-Ready 常见问题 FAQ

**版本**: v1.0  
**日期**: 2026-03-27  
**项目**: 智企连·AI-Ready

---

## 目录

- [环境搭建](#1-环境搭建)
- [后端开发](#2-后端开发)
- [前端开发](#3-前端开发)
- [数据库](#4-数据库)
- [部署运维](#5-部署运维)
- [Git 操作](#6-git-操作)
- [API 调用](#7-api-调用)

---

## 1. 环境搭建

### Q1.1: JDK 应该安装哪个版本？

**A**: 项目要求 JDK 17 或更高版本。推荐使用：
- **Adoptium Temurin**: https://adoptium.net/
- **Oracle JDK**: https://www.oracle.com/java/technologies/downloads/

验证安装：
```bash
java -version
# 输出: java version "17.0.x"
```

### Q1.2: Maven 配置阿里云镜像？

**A**: 编辑 `~/.m2/settings.xml`：

```xml
<settings>
    <mirrors>
        <mirror>
            <id>aliyun</id>
            <mirrorOf>central</mirrorOf>
            <name>Aliyun Maven Mirror</name>
            <url>https://maven.aliyun.com/repository/public</url>
        </mirror>
    </mirrors>
</settings>
```

### Q1.3: Node.js 版本要求？

**A**: Node.js 18+ 或 20+（推荐 LTS 版本）。可以使用 nvm 管理：

```bash
# 安装 nvm
# Windows: https://github.com/coreybutler/nvm-windows

# 安装 Node.js
nvm install 20

# 切换版本
nvm use 20
```

### Q1.4: IDEA 推荐配置？

**A**: 推荐以下配置：
1. **File → Settings → Editor → Code Style**: 设置缩进为 4 空格
2. **File → Settings → Build → Compiler**: 勾选 "Build project automatically"
3. **File → Settings → Plugins**: 安装 Lombok、MyBatisX、SonarLint

---

## 2. 后端开发

### Q2.1: 如何创建新的 API 接口？

**A**: 按以下步骤：

```java
// 1. 定义 DTO
public record CreateUserRequest(
    @NotBlank String username,
    @NotBlank @Email String email
) {}

// 2. 定义 Controller
@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "用户管理")
public class UserController {
    
    @PostMapping
    @Operation(summary = "创建用户")
    public Result<UserVO> create(@Valid @RequestBody CreateUserRequest request) {
        return Result.success(userService.create(request));
    }
}

// 3. 定义 Service
@Service
public class UserServiceImpl implements UserService {
    public UserVO create(CreateUserRequest request) {
        // 业务逻辑
    }
}
```

### Q2.2: 如何使用 Sa-Token 进行权限控制？

**A**: 使用注解方式：

```java
// 需要登录
@SaCheckLogin
public Result<UserVO> getCurrentUser() { }

// 需要角色
@SaCheckRole("admin")
public Result<Void> deleteUser(Long id) { }

// 需要权限
@SaCheckPermission("user:delete")
public Result<Void> deleteUser(Long id) { }
```

### Q2.3: 如何使用 MyBatis-Plus 进行分页查询？

**A**: 使用 Page 对象：

```java
// Service 层
public Page<UserVO> listUsers(int page, int size) {
    Page<User> pageParam = new Page<>(page, size);
    LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(User::getStatus, "active");
    
    Page<User> result = userMapper.selectPage(pageParam, wrapper);
    
    return result.convert(this::toVO);
}

// Controller 层
@GetMapping
public Result<Page<UserVO>> list(
    @RequestParam(defaultValue = "1") int page,
    @RequestParam(defaultValue = "20") int size
) {
    return Result.success(userService.listUsers(page, size));
}
```

### Q2.4: 如何统一处理异常？

**A**: 使用 `@RestControllerAdvice`：

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        return Result.fail(e.getCode(), e.getMessage());
    }
    
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return Result.fail("SYSTEM_ERROR", "系统繁忙");
    }
}
```

### Q2.5: 如何使用 Redis 缓存？

**A**: 使用 Spring Cache 注解：

```java
// 查询缓存
@Cacheable(value = "user", key = "#id")
public User getUserById(Long id) {
    return userMapper.selectById(id);
}

// 更新缓存
@CachePut(value = "user", key = "#user.id")
public User updateUser(User user) {
    userMapper.updateById(user);
    return user;
}

// 删除缓存
@CacheEvict(value = "user", key = "#id")
public void deleteUser(Long id) {
    userMapper.deleteById(id);
}
```

---

## 3. 前端开发

### Q3.1: 如何创建新页面？

**A**: 按以下步骤：

```typescript
// 1. 创建页面组件 src/views/system/user/index.vue
<template>
  <div class="user-list">
    <a-table :dataSource="data" :columns="columns" />
  </div>
</template>

<script setup lang="ts">
const data = ref([])
const columns = [
  { title: '用户名', dataIndex: 'username' },
  { title: '邮箱', dataIndex: 'email' }
]

onMounted(() => {
  loadData()
})
</script>

// 2. 添加路由 src/router/modules/system.ts
{
  path: 'user',
  name: 'SystemUser',
  component: () => import('@/views/system/user/index.vue'),
  meta: { title: '用户管理' }
}
```

### Q3.2: 如何封装 API 请求？

**A**: 创建 API 模块：

```typescript
// src/api/modules/user.ts
import request from '@/utils/request'
import type { User, UserQuery } from '@/types/user'

export const userApi = {
  getList: (params: UserQuery) => 
    request.get('/api/v1/users', { params }),
  
  getDetail: (id: number) => 
    request.get(`/api/v1/users/${id}`),
  
  create: (data: UserForm) => 
    request.post('/api/v1/users', data),
    
  update: (id: number, data: Partial<UserForm>) => 
    request.put(`/api/v1/users/${id}`, data),
    
  delete: (id: number) => 
    request.delete(`/api/v1/users/${id}`)
}
```

### Q3.3: 如何使用 Pinia 状态管理？

**A**: 创建 Store：

```typescript
// src/store/modules/user.ts
import { defineStore } from 'pinia'

export const useUserStore = defineStore('user', () => {
  const userInfo = ref<User | null>(null)
  const token = ref('')
  
  const isLoggedIn = computed(() => !!token.value)
  
  async function login(username: string, password: string) {
    const res = await authApi.login({ username, password })
    token.value = res.token
    userInfo.value = res.user
  }
  
  function logout() {
    token.value = ''
    userInfo.value = null
  }
  
  return { userInfo, token, isLoggedIn, login, logout }
})
```

### Q3.4: 如何实现表格分页？

**A**: 使用 Ant Design Vue 表格：

```vue
<template>
  <a-table
    :dataSource="data"
    :columns="columns"
    :pagination="pagination"
    :loading="loading"
    @change="handleTableChange"
  />
</template>

<script setup lang="ts">
import { useTable } from '@/composables/useTable'

const { data, loading, pagination, loadData, handleTableChange } = useTable({
  fetchData: userApi.getList
})

onMounted(() => {
  loadData()
})
</script>
```

### Q3.5: 如何处理表单验证？

**A**: 使用 Ant Design Vue 表单：

```vue
<template>
  <a-form :model="form" :rules="rules" ref="formRef">
    <a-form-item label="用户名" name="username">
      <a-input v-model:value="form.username" />
    </a-form-item>
  </a-form>
</template>

<script setup lang="ts">
const form = reactive({
  username: ''
})

const rules = {
  username: [
    { required: true, message: '请输入用户名' },
    { min: 3, max: 20, message: '用户名长度3-20位' }
  ]
}

const formRef = ref()

const handleSubmit = async () => {
  await formRef.value.validate()
  // 提交表单
}
</script>
```

---

## 4. 数据库

### Q4.1: PostgreSQL 如何创建数据库？

**A**: 使用 Docker 或命令行：

```bash
# Docker 方式
docker exec -it ai-ready-postgres psql -U devuser -c "CREATE DATABASE newdb;"

# 命令行方式
psql -U devuser -c "CREATE DATABASE newdb;"
```

### Q4.2: 如何执行数据库迁移？

**A**: 使用 Flyway 或手动执行 SQL：

```bash
# 手动执行 SQL 文件
psql -U devuser -d devdb -f docs/database/init-base-tables.sql
```

### Q4.3: 如何查看 SQL 执行计划？

**A**: 使用 EXPLAIN：

```sql
EXPLAIN ANALYZE SELECT * FROM users WHERE status = 'active';
```

### Q4.4: 常用 PostgreSQL 命令？

**A**: 常用命令：

```sql
-- 查看所有数据库
\l

-- 切换数据库
\c devdb

-- 查看所有表
\dt

-- 查看表结构
\d users

-- 查看索引
\di
```

---

## 5. 部署运维

### Q5.1: 如何打包项目？

**A**: 使用 Maven 打包：

```bash
# 后端打包
mvn clean package -DskipTests

# 生成的 jar 包在 target/ 目录下
```

### Q5.2: 如何使用 Docker 部署？

**A**: 使用 Dockerfile：

```dockerfile
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app
COPY target/ai-ready.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
```

```bash
# 构建镜像
docker build -t ai-ready:latest .

# 运行容器
docker run -d -p 8080:8080 ai-ready:latest
```

### Q5.3: 如何查看日志？

**A**: 查看日志方式：

```bash
# Docker 容器日志
docker logs -f ai-ready-app

# 应用日志文件
tail -f logs/application.log

# 实时查看
tail -f logs/application.log | grep ERROR
```

---

## 6. Git 操作

### Q6.1: 如何撤销最近的提交？

**A**: 使用 reset：

```bash
# 保留变更，撤销提交
git reset --soft HEAD~1

# 丢弃变更，撤销提交（危险操作）
git reset --hard HEAD~1
```

### Q6.2: 如何修改提交信息？

**A**: 使用 amend：

```bash
# 修改最后一次提交信息
git commit --amend -m "新的提交信息"
```

### Q6.3: 如何解决合并冲突？

**A**: 解决步骤：

```bash
# 1. 查看冲突文件
git status

# 2. 手动编辑冲突文件，解决冲突标记
<<<<<<< HEAD
当前分支内容
=======
合并分支内容
>>>>>>> feature/xxx

# 3. 标记已解决
git add .

# 4. 完成合并
git commit -m "chore: 解决合并冲突"
```

### Q6.4: 如何暂存当前修改？

**A**: 使用 stash：

```bash
# 暂存修改
git stash

# 查看暂存列表
git stash list

# 恢复最近的暂存
git stash pop

# 恢复指定暂存
git stash apply stash@{0}
```

---

## 7. API 调用

### Q7.1: 如何获取 Token？

**A**: 调用登录接口：

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

# 响应
{
  "code": 200,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "..."
  }
}
```

### Q7.2: 如何刷新 Token？

**A**: 调用刷新接口：

```bash
curl -X POST http://localhost:8080/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"xxx"}'
```

### Q7.3: 如何调试 API？

**A**: 推荐工具：
- **Postman**: 图形化 API 调试工具
- **curl**: 命令行工具
- **Knife4j**: http://localhost:8080/doc.html

### Q7.4: 常见 HTTP 状态码？

| 状态码 | 说明 | 处理方式 |
|--------|------|----------|
| 200 | 成功 | 正常处理 |
| 400 | 参数错误 | 检查请求参数 |
| 401 | 未认证 | 重新登录 |
| 403 | 无权限 | 检查权限配置 |
| 404 | 未找到 | 检查 URL 路径 |
| 500 | 服务器错误 | 查看日志排查 |

---

## 更多问题？

如果您的问题未在此列表中，请：

1. 查阅项目文档: `docs/` 目录
2. 在项目群聊中提问
3. 联系技术导师

---

**文档更新**: 2026-03-27  
**维护者**: doc-writer