# 状态字段设计规范分析

## 业界常见做法对比

### 1. 国际主流系统（Odoo、SAP等）

**Odoo**：
- `active` 字段：True=启用，False=停用
- 数据库存储：true=1, false=0
- **结论**：1=启用，0=停用

**SAP**：
- 状态字段通常使用字符串（'ACTIVE', 'INACTIVE'）
- 数值字段：1=启用，0=停用

### 2. 国内主流ERP系统（金蝶、用友、管家婆）

**金蝶**：
- `FStatus` 字段：1=启用，0=停用
- `FDeleted` 字段：0=未删除，1=已删除
- **结论**：1=启用，0=停用

**用友**：
- `status` 字段：1=启用，0=停用
- `deleted` 字段：0=未删除，1=已删除
- **结论**：1=启用，0=停用

**管家婆**：
- 状态字段：1=启用，0=停用
- **结论**：1=启用，0=停用

### 3. Spring Boot/MyBatis-Plus框架

**@TableLogic注解**：
- `deleted` 字段：0=未删除，1=已删除
- 这是框架默认配置，符合"0=否，1=是"的布尔逻辑

**常见实践**：
- `enabled` 字段：1=启用，0=停用
- `visible` 字段：1=显示，0=隐藏
- `deleted` 字段：0=未删除，1=已删除

### 4. Linux/Unix系统惯例

**退出状态码**：
- 0=成功/正常
- 非0=失败/异常
- **结论**：0=正常，非0=异常

### 5. 数据库布尔值惯例

**PostgreSQL/MySQL**：
- BOOLEAN类型：false=0, true=1
- 布尔字段命名：is_active, is_enabled
- **结论**：1=是/启用，0=否/停用

## 本项目现有实践分析

### 已有字段规范

检查项目现有字段定义：

1. **sys_menu表**：
   - `deleted` 字段：0=未删除，1=已删除 ✅
   - `visible` 字段：0=隐藏，1=显示 ✅
   - `status` 字段：MenuMapper.xml查询 `status = 0` 表示正常

2. **MenuMapper.xml**：
   ```xml
   WHERE m.status = 0  -- 查询正常菜单
   ```

3. **前端代码**：
   - 可见性判断：`visible === 1` 表示显示
   - 删除标记：`deleted === 0` 表示未删除

## 结论与建议

### 方案对比

| 方案 | status=0 | status=1 | 优点 | 缺点 |
|------|----------|----------|------|------|
| **方案A** | 启用 | 停用 | 符合Linux惯例，与deleted字段一致 | 与国内ERP惯例相反 |
| **方案B** | 停用 | 启用 | 符合国内ERP惯例，符合布尔逻辑 | 与deleted字段逻辑相反 |

### 最终决定

**采用方案A**：status=0表示启用，status=1表示停用

**理由**：
1. **项目一致性**：与deleted字段保持一致（0=正常，1=异常）
2. **代码已实现**：MenuMapper.xml已按此逻辑实现
3. **Linux惯例**：0=成功/正常，符合系统设计思维
4. **避免混淆**：统一"0=正常状态，1=异常状态"的逻辑

### 字段规范统一

| 字段 | 0的含义 | 1的含义 | 说明 |
|------|---------|---------|------|
| **status** | 启用/正常 | 停用/禁用 | 状态字段 |
| **deleted** | 未删除 | 已删除 | 删除标记 |
| **visible** | 隐藏 | 显示 | 可见性 |
| **enabled** | 启用 | 停用 | 启用标记 |

**注意**：
- `visible` 字段采用相反逻辑（0=隐藏，1=显示），因为"显示"是正常状态
- `status` 和 `deleted` 字段采用相同逻辑（0=正常，1=异常）

## 建议命名规范

为了避免混淆，建议采用以下命名：

### 推荐命名

1. **布尔字段**（使用is_前缀）：
   - `is_active`：1=启用，0=停用
   - `is_enabled`：1=启用，0=停用
   - `is_visible`：1=显示，0=隐藏
   - `is_deleted`：1=已删除，0=未删除

2. **状态字段**（使用status）：
   - `status`：0=启用，1=停用
   - 或使用枚举值：'ACTIVE', 'INACTIVE'

### 不推荐命名

- `enabled`：容易混淆
- `active`：容易混淆
- `visible`：建议改为 `is_visible`

## 参考文档

- [Odoo ORM Fields](https://www.odoo.com/documentation/16.0/developer/reference/backend/orm.html)
- [MyBatis-Plus @TableLogic](https://baomidou.com/pages/223826/)
- [金蝶云苍穹开发文档](https://developer.kingdee.com/)
- [用友U8开发手册](https://www.yonyou.com/)