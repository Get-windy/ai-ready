# 价格策略模块架构设计文档

## 1. 概述

价格策略模块是企智连ERP系统的核心定价引擎，负责处理复杂的多维度定价场景。该模块提供了灵活的定价策略配置、实时价格计算和定价策略管理功能。

## 2. 架构设计

### 2.1 整体架构图

```
┌─────────────────────────────────────────────┐
│             价格策略模块架构                 │
├─────────────────────────────────────────────┤
│  ┌──────────────────────────────────────┐  │
│  │          API接口层                    │  │
│  │  ┌─────────────┐ ┌─────────────┐    │  │
│  │  │ PricingCtrl │ │  PriceAPI   │    │  │
│  │  └─────────────┘ └─────────────┘    │  │
│  └──────────────────────────────────────┘  │
│                    ↓                        │
│  ┌──────────────────────────────────────┐  │
│  │          业务逻辑层                    │  │
│  │  ┌─────────────┐ ┌─────────────┐    │  │
│  │  │ 策略管理服务 │ │ 价格计算服务 │    │  │
│  │  └─────────────┘ └─────────────┘    │  │
│  └──────────────────────────────────────┘  │
│                    ↓                        │
│  ┌──────────────────────────────────────┐  │
│  │          数据访问层                    │  │
│  │  ┌─────────────┐ ┌─────────────┐    │  │
│  │  │ PriceStrategy│ │  PriceRule  │    │  │
│  │  │    Mapper    │ │   Mapper    │    │  │
│  │  └─────────────┘ └─────────────┘    │  │
│  └──────────────────────────────────────┘  │
│                    ↓                        │
│  ┌──────────────────────────────────────┐  │
│  │          数据存储层                    │  │
│  │  ┌─────────────┐ ┌─────────────┐    │  │
│  │  │ erp_pricing_│ │ erp_pricing_│    │  │
│  │  │   strategy  │ │     rule    │    │  │
│  │  └─────────────┘ └─────────────┘    │  │
│  └──────────────────────────────────────┘  │
└─────────────────────────────────────────────┘
```

### 2.2 核心组件说明

#### 2.2.1 API接口层
- **PricingController**: 价格策略管理控制器，提供完整的CRUD操作
- **PriceAPI**: Swagger API文档生成接口定义

#### 2.2.2 业务逻辑层
- **IPriceStrategyService**: 价格策略管理服务接口
- **IPriceCalculationService**: 价格计算服务接口
- **PriceStrategyServiceImpl**: 策略管理服务实现
- **PriceCalculationServiceImpl**: 价格计算服务实现

#### 2.2.3 数据访问层
- **PriceStrategyMapper**: 价格策略数据访问接口
- **PriceRuleMapper**: 价格规则数据访问接口

#### 2.2.4 数据模型层
- **PriceStrategy**: 价格策略实体类
- **PriceRule**: 价格规则实体类
- **PriceStrategyDTO**: 价格策略数据传输对象
- **PriceRuleDTO**: 价格规则数据传输对象
- **PriceCalculationRequest**: 价格计算请求对象
- **PriceCalculationResult**: 价格计算结果对象

## 3. 数据库设计

### 3.1 核心表结构

#### 3.1.1 价格策略表 (erp_pricing_strategy)

```sql
CREATE TABLE erp_pricing_strategy (
    id BIGINT PRIMARY KEY COMMENT '策略ID',
    tenant_id BIGINT COMMENT '租户ID',
    name VARCHAR(100) NOT NULL COMMENT '策略名称',
    description VARCHAR(500) COMMENT '策略描述',
    strategy_type VARCHAR(50) COMMENT '策略类型',
    customer_level VARCHAR(50) COMMENT '适用客户等级',
    region_code VARCHAR(50) COMMENT '适用区域编码',
    product_category_id BIGINT COMMENT '适用产品类别ID',
    base_price DECIMAL(20,6) COMMENT '基础价格',
    price_factor DECIMAL(10,4) COMMENT '价格系数',
    discount_rate DECIMAL(10,4) COMMENT '折扣率',
    discount_amount DECIMAL(20,6) COMMENT '折扣金额',
    min_quantity INT COMMENT '最小数量门槛',
    formula_config JSON COMMENT '公式配置(JSON)',
    effective_start_time DATETIME COMMENT '生效开始时间',
    effective_end_time DATETIME COMMENT '生效结束时间',
    priority INT DEFAULT 0 COMMENT '优先级',
    status VARCHAR(20) DEFAULT 'draft' COMMENT '状态',
    deleted TINYINT DEFAULT 0 COMMENT '是否删除',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by BIGINT COMMENT '创建人ID',
    update_by BIGINT COMMENT '更新人ID',
    INDEX idx_tenant_status (tenant_id, status),
    INDEX idx_effective_time (effective_start_time, effective_end_time),
    INDEX idx_customer_level (customer_level),
    INDEX idx_product_category (product_category_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='价格策略表';
```

#### 3.1.2 价格规则表 (erp_pricing_rule)

```sql
CREATE TABLE erp_pricing_rule (
    id BIGINT PRIMARY KEY COMMENT '规则ID',
    strategy_id BIGINT NOT NULL COMMENT '策略ID',
    rule_name VARCHAR(100) COMMENT '规则名称',
    rule_type VARCHAR(50) COMMENT '规则类型',
    condition_type VARCHAR(50) COMMENT '条件类型',
    condition_value VARCHAR(500) COMMENT '条件值',
    calculation_type VARCHAR(50) COMMENT '计算类型',
    price_factor DECIMAL(10,4) COMMENT '价格系数',
    discount_rate DECIMAL(10,4) COMMENT '折扣率',
    discount_amount DECIMAL(20,6) COMMENT '折扣金额',
    min_quantity INT COMMENT '最小数量',
    max_quantity INT COMMENT '最大数量',
    min_amount DECIMAL(20,6) COMMENT '最小金额',
    max_amount DECIMAL(20,6) COMMENT '最大金额',
    priority INT DEFAULT 0 COMMENT '规则优先级',
    status VARCHAR(20) DEFAULT 'active' COMMENT '状态',
    deleted TINYINT DEFAULT 0 COMMENT '是否删除',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_strategy_id (strategy_id),
    INDEX idx_rule_type (rule_type),
    FOREIGN KEY (strategy_id) REFERENCES erp_pricing_strategy(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='价格规则表';
```

### 3.2 表关系说明

```
erp_pricing_strategy (1) : (n) erp_pricing_rule
    ↑                           ↑
    |                           |
   策略主表                    策略规则表
   包含基础配置              包含详细计算规则
```

## 4. 核心业务逻辑设计

### 4.1 策略优先级系统

价格策略模块实现了多层级的优先级机制：

1. **策略优先级**: 每个价格策略都有优先级字段，数字越小优先级越高
2. **规则优先级**: 策略内的规则也有优先级，控制规则的执行顺序
3. **冲突解决**: 当多个策略同时生效时，按优先级执行，高优先级覆盖低优先级

### 4.2 价格计算流程

```java
// 简化后的价格计算流程
public PriceCalculationResult calculatePrice(PriceCalculationRequest request) {
    // 1. 获取有效的价格策略
    List<PriceStrategy> activeStrategies = getActiveStrategies();
    
    // 2. 根据条件过滤适用的策略
    List<PriceStrategy> applicableStrategies = filterStrategies(activeStrategies, request);
    
    // 3. 按优先级排序
    applicableStrategies.sort(Comparator.comparingInt(PriceStrategy::getPriority));
    
    // 4. 应用策略计算价格
    BigDecimal finalPrice = basePrice;
    List<AppliedStrategyInfo> appliedStrategies = new ArrayList<>();
    
    for (PriceStrategy strategy : applicableStrategies) {
        // 应用策略规则
        finalPrice = applyStrategy(strategy, finalPrice);
        appliedStrategies.add(recordStrategyApplication(strategy));
    }
    
    // 5. 返回计算结果
    return buildResult(finalPrice, appliedStrategies);
}
```

### 4.3 策略匹配逻辑

策略匹配基于以下维度：
1. **客户等级匹配**: customer_level字段
2. **区域匹配**: region_code字段
3. **产品类别匹配**: product_category_id字段
4. **时间有效性**: effective_start_time和effective_end_time字段
5. **数量门槛**: min_quantity字段

## 5. 技术选型说明

### 5.1 后端技术栈
- **框架**: Spring Boot 3.x
- **数据库**: MySQL 8.0
- **ORM**: MyBatis-Plus
- **权限认证**: Sa-Token
- **API文档**: SpringDoc OpenAPI 3.0

### 5.2 设计决策

#### 5.2.1 策略-规则分离设计
**决策**: 将价格策略和价格规则分离为两张表
**原因**:
1. 策略是配置，规则是具体计算逻辑，关注点分离
2. 便于策略复用，一个策略可以包含多个规则
3. 提高扩展性，未来可以增加更多规则类型

#### 5.2.2 维度化的策略设计
**决策**: 支持多维度价格策略配置
**原因**:
1. 满足复杂业务场景：客户等级、区域、产品类别、时间等维度
2. 提高定价的精准性和灵活性
3. 支持精细化运营

#### 5.2.3 优先级冲突解决机制
**决策**: 使用数字优先级字段解决策略冲突
**原因**:
1. 简单直观，易于理解和配置
2. 性能高效，排序计算简单
3. 业务场景覆盖全面

## 6. 性能设计

### 6.1 缓存策略
- **策略缓存**: 将活跃策略缓存到Redis，减少数据库查询
- **计算结果缓存**: 对常用价格计算进行缓存，提高响应速度

### 6.2 数据库优化
- **索引设计**: 对常用查询字段建立复合索引
- **分页查询**: 大数据量时使用分页查询
- **软删除**: 使用逻辑删除而非物理删除

### 6.3 并发处理
- **乐观锁**: 使用version字段实现乐观锁
- **数据库事务**: 保证数据一致性
- **幂等设计**: 关键操作实现幂等性

## 7. 扩展性设计

### 7.1 插件化架构
- **策略类型插件**: 支持自定义策略类型
- **规则计算插件**: 支持自定义价格计算规则
- **条件判断插件**: 支持自定义策略适用条件

### 7.2 配置化设计
- **公式配置**: 通过JSON配置价格计算公式
- **规则配置**: 可视化规则配置界面
- **策略模板**: 支持策略模板功能

### 7.3 监控与告警
- **价格计算监控**: 监控价格计算性能和准确率
- **策略生效监控**: 监控策略生效状态
- **异常告警**: 价格计算异常实时告警

## 8. 安全设计

### 8.1 权限控制
- **接口权限**: 基于Sa-Token的接口权限控制
- **数据权限**: 租户级别的数据隔离
- **操作日志**: 记录所有价格策略操作

### 8.2 数据安全
- **输入验证**: 严格验证所有输入参数
- **参数绑定**: 防止SQL注入和XSS攻击
- **审计日志**: 完整的价格计算审计日志

## 9. 部署架构

### 9.1 高可用部署
```
┌─────────────────────────────────────────┐
│              负载均衡器                  │
└─────────────────────────────────────────┘
            ↓           ↓
┌─────────────────┐ ┌─────────────────┐
│  应用服务器1     │ │  应用服务器2     │
│  - Pricing服务  │ │  - Pricing服务  │
└─────────────────┘ └─────────────────┘
            ↓           ↓
┌─────────────────────────────────────────┐
│             MySQL主从集群                │
└─────────────────────────────────────────┘
            ↓           ↓
┌─────────────────────────────────────────┐
│              Redis集群                  │
└─────────────────────────────────────────┘
```

### 9.2 监控告警
- **应用监控**: Spring Boot Actuator + Prometheus
- **日志收集**: ELK日志收集分析
- **性能监控**: Grafana监控面板

## 10. 总结

价格策略模块采用了分层架构设计，实现了高度可配置、高性能、高可用的定价引擎。通过策略-规则分离、维度化配置、优先级机制等设计，满足了企业级ERP系统的复杂定价需求。模块具备良好的扩展性和维护性，为业务发展提供了坚实的技术基础。