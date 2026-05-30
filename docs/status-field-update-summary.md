# 状态字段规范化更新总结

## 更新总结

### 已完成更新

✅ **所有核心表的status字段已成功更新为方案A标准**

| 表名 | 更新记录数 | 更新前status | 更新后status | 状态 |
|------|-----------|-------------|-------------|------|
| sys_menu | 34条 | 0 | 1 | ✅ 完成 |
| sys_permission | 73条 | 0 | 1 | ✅ 完成 |
| sys_user | 1条 | 0 | 1 | ✅ 完成 |
| sys_role | 3条 | 0 | 1 | ✅ 完成 |

### 验证结果

所有表的status字段已统一为：
- **status=1**：启用/正常状态 ✅
- **status=0**：停用/禁用状态

**遵循国内ERP行业标准**：金蝶、用友、管家婆均采用此规范。

## 更新方案

**方案A**：遵循国内ERP惯例（金蝶、用友、管家婆）
- **status=1**：启用/正常状态
- **status=0**：停用/禁用状态

## 更新内容

### 1. 代码文件更新

#### MenuMapper.xml
- 文件路径：`backend/core/web-admin/src/main/resources/mapper/menu/MenuMapper.xml`
- 更新内容：
  - `selectMenusByUserId`：`status = 0` → `status = 1`
  - `selectMenusByRoleId`：`status = 0` → `status = 1`
  - `selectPermissionsByUserId`：`status = 0` → `status = 1`

#### SysPermissionMapper.java
- 文件路径：`backend/core/base/core-base/src/main/java/cn/aiedge/base/mapper/SysPermissionMapper.java`
- 更新内容：
  - `selectPermissionsByUserId`：`status = 0` → `status = 1`
  - `selectPermissionsByRoleId`：`status = 0` → `status = 1`

#### DataPermissionMapper.xml
- 文件路径：`backend/core/base/core-base/src/main/resources/mapper/DataPermissionMapper.xml`
- 更新内容：
  - `selectDataPermissionsByRoleIds`：`status = 0` → `status = 1`

### 2. 数据库数据更新

#### sys_menu表
- 更新所有菜单的status字段：`status = 0` → `status = 1`
- 更新记录数：34条菜单记录
- 更新时间：2026-05-29

#### sys_permission表
- 更新所有权限的status字段：`status = 0` → `status = 1`
- 更新记录数：73条权限记录
- 更新时间：2026-05-29

#### sys_user表
- 更新所有用户的status字段：`status = 0` → `status = 1`
- 更新记录数：1条用户记录
- 更新时间：2026-05-29

#### sys_role表
- 更新所有角色的status字段：`status = 0` → `status = 1`
- 更新记录数：3条角色记录
- 更新时间：2026-05-29

### 3. 文档更新

#### AGENTS.md（根目录）
- 文件路径：`AGENTS.md`
- 更新内容：添加"状态字段规范（遵循国内ERP惯例）"章节

#### backend/AGENTS.md
- 文件路径：`backend/AGENTS.md`
- 更新内容：更新"状态字段规范"章节，明确status=1=启用

#### backend/sql/AGENTS.md
- 文件路径：`backend/sql/AGENTS.md`
- 更新内容：更新"菜单状态说明"章节，明确status=1=启用

## 状态字段规范总结

| 字段类型 | 0的含义 | 1的含义 | 说明 |
|---------|---------|---------|------|
| **status** | 停用/禁用 | 启用/正常 ✅ | 状态字段 |
| **deleted** | 未删除 | 已删除 | 删除标记 |
| **visible** | 隐藏 | 显示 | 可见性 |

**核心原则**：
- **status=1**：启用/正常状态（查询条件：`WHERE status = 1`）
- **status=0**：停用/禁用状态
- **遵循国内ERP行业标准**：金蝶、用友、管家婆均采用此规范
- **符合布尔逻辑**：1=是/启用，0=否/停用

## 后续建议

### 1. 重启后端服务

更新完成后，需要重启后端服务以应用所有更改：

```bash
cd backend
mvn clean install -DskipTests
java -jar core/api/core-api/target/core-api-1.0.0-SNAPSHOT.jar --spring.profiles.active=dev
```

### 2. 刷新浏览器

刷新浏览器页面（Ctrl+F5）或重新登录，验证菜单显示。

### 3. 检查其他业务表

建议检查其他业务模块的表是否还有status字段需要更新：
- 采购订单表（purchase_order）
- 销售订单表（sale_order）
- 库存表（stock）
- 其他业务单据表

**注意**：业务单据表的status字段含义不同（0=草稿，1=待审批等），不需要更新。

## 参考文档

- [状态字段分析文档](docs/status-field-analysis.md)
- [AGENTS.md](AGENTS.md)
- [backend/AGENTS.md](backend/AGENTS.md)
- [backend/sql/AGENTS.md](backend/sql/AGENTS.md)