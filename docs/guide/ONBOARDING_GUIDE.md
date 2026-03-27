# 智企连·AI-Ready 新人入门指南

**版本**: v1.0  
**日期**: 2026-03-27  
**项目**: 智企连·AI-Ready

---

## 欢迎加入智企连团队！

本指南将帮助您快速上手智企连·AI-Ready 项目的开发工作。

---

## 1. 第一天：环境准备

### 1.1 必备软件安装

| 软件 | 版本要求 | 下载地址 |
|------|----------|----------|
| JDK | 17+ | https://adoptium.net/ |
| Maven | 3.9+ | https://maven.apache.org/ |
| Node.js | 18+ | https://nodejs.org/ |
| Git | 最新版 | https://git-scm.com/ |
| IDEA | 最新版 | https://www.jetbrains.com/idea/ |
| VS Code | 最新版 | https://code.visualstudio.com/ |

### 1.2 开发工具配置

#### IDEA 插件推荐

- **Lombok** - 简化 POJO 编写
- **MyBatisX** - MyBatis 增强插件
- **Spring Boot Assistant** - Spring Boot 支持
- **SonarLint** - 代码质量检查
- **GitToolBox** - Git 增强

#### VS Code 插件推荐

- **Vue - Official** - Vue 3 语法支持
- **TypeScript Vue Plugin** - TypeScript 支持
- **ESLint** - 代码规范检查
- **Prettier** - 代码格式化
- **Volar** - Vue Language Features

### 1.3 环境验证

```powershell
# 检查 Java 版本
java -version
# 输出: java version "17.0.x"

# 检查 Maven 版本
mvn -version
# 输出: Apache Maven 3.9.x

# 检查 Node.js 版本
node --version
# 输出: v18.x.x 或更高

# 检查 Git 版本
git --version
# 输出: git version 2.x.x
```

---

## 2. 第二天：项目熟悉

### 2.1 获取项目代码

```bash
# 克隆项目
git clone https://github.com/aiedge/ai-ready.git

# 进入项目目录
cd ai-ready

# 查看项目结构
tree -L 2
```

### 2.2 项目结构概览

```
ai-ready/
├── core-base/           # 基础模块 - 用户、权限、配置
├── core-common/         # 公共工具类
├── core-api/            # API 接口定义
├── core-workflow/       # 工作流模块
├── core-report/         # 报表模块
├── core-agent/          # Agent 调用层
├── erp/                 # ERP 模块群
│   ├── erp-purchase/    # 采购管理
│   ├── erp-sale/        # 销售管理
│   ├── erp-stock/       # 库存管理
│   └── erp-account/     # 财务管理
├── crm/                 # CRM 模块群
│   ├── crm-lead/        # 线索管理
│   ├── crm-opportunity/ # 商机管理
│   └── crm-customer/    # 客户管理
├── smart-admin-web/     # 前端项目 (Vue3)
├── erp-mobile/          # 移动端项目 (UniApp)
├── docs/                # 文档目录
└── docker/              # Docker 配置
```

### 2.3 技术栈理解

#### 后端技术栈

| 技术 | 作用 | 学习资源 |
|------|------|----------|
| Spring Boot 3.2 | 核心框架 | [官方文档](https://spring.io/projects/spring-boot) |
| Sa-Token | 权限认证 | [官方文档](https://sa-token.cc/) |
| MyBatis-Plus | ORM 框架 | [官方文档](https://baomidou.com/) |
| PostgreSQL | 主数据库 | [官方文档](https://www.postgresql.org/docs/) |
| Redis | 缓存 | [官方文档](https://redis.io/docs/) |

#### 前端技术栈

| 技术 | 作用 | 学习资源 |
|------|------|----------|
| Vue 3 | 前端框架 | [官方文档](https://vuejs.org/) |
| Ant Design Vue | UI 组件库 | [官方文档](https://antdv.com/) |
| Pinia | 状态管理 | [官方文档](https://pinia.vuejs.org/) |
| TypeScript | 类型系统 | [官方文档](https://www.typescriptlang.org/) |

---

## 3. 第三天：本地开发环境

### 3.1 启动数据库

```bash
# 使用 Docker 启动 PostgreSQL
docker run -d \
  --name ai-ready-postgres \
  -e POSTGRES_USER=devuser \
  -e POSTGRES_PASSWORD="Dev@2026#Local" \
  -e POSTGRES_DB=devdb \
  -p 5432:5432 \
  postgres:14

# 使用 Docker 启动 Redis
docker run -d \
  --name ai-ready-redis \
  -p 6379:6379 \
  redis:7-alpine
```

### 3.2 配置项目

```yaml
# core-base/src/main/resources/application-dev.yml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/devdb
    username: devuser
    password: Dev@2026#Local
  redis:
    host: localhost
    port: 6379
```

### 3.3 启动后端

```bash
# 编译项目
mvn clean install -DskipTests

# 启动应用
cd core-base
mvn spring-boot:run
```

### 3.4 启动前端

```bash
# 进入前端目录
cd smart-admin-web

# 安装依赖
npm install

# 启动开发服务器
npm run dev
```

### 3.5 访问应用

- 前端界面: http://localhost:5173
- API 文档: http://localhost:8080/doc.html
- 默认账号: admin / admin123

---

## 4. 第四天：代码规范学习

### 4.1 必读文档

| 文档 | 路径 | 说明 |
|------|------|------|
| Java 代码规范 | docs/standards/JAVA_CODE_STANDARDS.md | 后端编码规范 |
| 前端代码规范 | docs/standards/FRONTEND_CODE_STANDARDS.md | 前端编码规范 |
| Git 提交规范 | docs/standards/GIT_COMMIT_STANDARDS.md | 版本控制规范 |
| API 规范 | docs/api/API_STANDARDS.md | 接口设计规范 |

### 4.2 关键规范速记

#### Java 规范要点

```java
// 1. 使用 Records 定义 DTO
public record UserDTO(Long id, String username, String email) {}

// 2. 使用 Sealed 限制继承
public sealed interface Result<T> permits Success, Failure {}

// 3. 使用 Pattern Matching
if (obj instanceof String s) {
    System.out.println(s);
}

// 4. 使用 Text Blocks
String sql = """
    SELECT * FROM users
    WHERE status = 'active'
    """;
```

#### 前端规范要点

```typescript
// 1. 使用 Composition API
<script setup lang="ts">
const count = ref(0)
</script>

// 2. 类型定义
interface User {
  id: number
  name: string
}

// 3. 组合式函数
function useCounter() {
  const count = ref(0)
  const increment = () => count.value++
  return { count, increment }
}
```

---

## 5. 第五天：开发流程

### 5.1 开发流程图

```
领取任务 → 创建分支 → 编写代码 → 单元测试 → 提交代码 → 代码审核 → 合并分支
```

### 5.2 分支管理

```bash
# 从 develop 创建功能分支
git checkout develop
git pull origin develop
git checkout -b feature/user-login

# 开发完成后提交
git add .
git commit -m "feat(user): 添加用户登录功能"

# 推送到远程
git push origin feature/user-login

# 创建 Pull Request
# 在 GitHub/GitLab 上创建 PR，目标分支为 develop
```

### 5.3 提交信息规范

```
feat(user): 添加用户登录功能

- 新增登录接口
- 新增 Token 验证
- 新增登录页面

Closes #123
```

---

## 6. 常用命令速查

### 6.1 后端常用命令

```bash
# 编译项目
mvn clean install

# 跳过测试编译
mvn clean install -DskipTests

# 运行测试
mvn test

# 运行单个测试类
mvn test -Dtest=UserServiceTest

# 启动应用
mvn spring-boot:run

# 打包
mvn clean package
```

### 6.2 前端常用命令

```bash
# 安装依赖
npm install

# 启动开发服务器
npm run dev

# 构建生产版本
npm run build

# 代码检查
npm run lint

# 代码格式化
npm run format
```

### 6.3 Git 常用命令

```bash
# 查看状态
git status

# 查看日志
git log --oneline -10

# 拉取最新代码
git pull origin develop

# 切换分支
git checkout feature/xxx

# 创建并切换分支
git checkout -b feature/xxx

# 暂存代码
git stash

# 恢复暂存
git stash pop
```

---

## 7. 学习资源

### 7.1 官方文档

- [Spring Boot 官方文档](https://spring.io/projects/spring-boot)
- [Vue 3 官方文档](https://vuejs.org/)
- [MyBatis-Plus 官方文档](https://baomidou.com/)
- [Sa-Token 官方文档](https://sa-token.cc/)

### 7.2 推荐书籍

- 《Spring Boot 实战》
- 《Vue.js 设计与实现》
- 《深入浅出 Spring Boot》
- 《Java 并发编程实战》

### 7.3 视频教程

- [Spring Boot 教程](https://www.bilibili.com/video/BV1xxx)
- [Vue 3 教程](https://www.bilibili.com/video/BV1xxx)

---

## 8. 常见问题

### Q1: 数据库连接失败？

```bash
# 检查 PostgreSQL 是否启动
docker ps | grep postgres

# 如果未启动，启动容器
docker start ai-ready-postgres
```

### Q2: Redis 连接失败？

```bash
# 检查 Redis 是否启动
docker ps | grep redis

# 如果未启动，启动容器
docker start ai-ready-redis
```

### Q3: 前端依赖安装失败？

```bash
# 清除缓存
npm cache clean --force

# 删除 node_modules
rm -rf node_modules

# 重新安装
npm install
```

### Q4: Maven 依赖下载慢？

```xml
<!-- 配置阿里云镜像 -->
<mirror>
    <id>aliyun</id>
    <mirrorOf>central</mirrorOf>
    <name>Aliyun Maven</name>
    <url>https://maven.aliyun.com/repository/public</url>
</mirror>
```

---

## 9. 联系方式

遇到问题不要慌，及时沟通：

| 场景 | 联系人 | 方式 |
|------|--------|------|
| 技术问题 | 技术导师 | 项目群聊 |
| 环境问题 | DevOps | 项目群聊 |
| 业务问题 | 产品经理 | 项目群聊 |

---

## 10. 检查清单

完成以下内容，即可正式开始开发：

- [ ] JDK 17 已安装
- [ ] Maven 已安装
- [ ] Node.js 已安装
- [ ] Git 已安装
- [ ] IDEA 已安装并配置
- [ ] VS Code 已安装并配置
- [ ] PostgreSQL 已启动
- [ ] Redis 已启动
- [ ] 项目代码已克隆
- [ ] 后端项目已能启动
- [ ] 前端项目已能启动
- [ ] 代码规范已阅读
- [ ] Git 提交规范已了解

---

**欢迎加入！祝工作顺利！**

---

**文档更新**: 2026-03-27  
**维护者**: doc-writer