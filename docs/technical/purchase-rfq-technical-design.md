# 采购询价/报价管理系统 - 技术设计文档

## 📋 文档信息

| 项目 | 内容 |
|------|------|
| **版本** | 1.0.0 |
| **创建日期** | 2026-04-28 |
| **作者** | 产品分析师 |
| **项目** | AI-Ready Sprint 28 |
| **状态** | 技术设计完成 |

---

## 🏗️ 系统架构设计

### 1. 整体架构

```
┌─────────────────────────────────────────────────────────────┐
│                    前端应用层                                │
│  ├── 采购管理门户                                           │
│  ├── 供应商协同门户                                         │
│  └── 移动端应用                                           │
└─────────────────────────────────────────────────────────────┘
                              │ HTTP/HTTPS
┌─────────────────────────────────────────────────────────────┐
│                    API网关层                                 │
│  ├── 认证授权                                               │
│  ├── 请求路由                                               │
│  ├── 限流熔断                                               │
│  └── 日志监控                                               │
└─────────────────────────────────────────────────────────────┘
                              │ RESTful API
┌─────────────────────────────────────────────────────────────┐
│                    业务服务层                                │
│  ├── 采购询价服务 (purchase-inquiry-service)                │
│  ├── 供应商报价服务 (supplier-quote-service)                │
│  ├── 比价分析服务 (comparison-analysis-service)             │
│  └── 采购决策服务 (purchase-decision-service)               │
└─────────────────────────────────────────────────────────────┘
                              │ 数据库访问
┌─────────────────────────────────────────────────────────────┐
│                    数据持久层                                │
│  ├── 关系型数据库 (MySQL)                                    │
│  ├── 缓存 (Redis)                                           │
│  └── 文件存储 (MinIO)                                      │
└─────────────────────────────────────────────────────────────┘
```

### 2. 微服务划分

#### 2.1 采购询价服务 (purchase-inquiry-service)
- **职责**: 询价单管理、询价明细管理、状态流转
- **技术栈**: Spring Boot, JPA, MySQL
- **端口**: 8081

#### 2.2 供应商报价服务 (supplier-quote-service)
- **职责**: 报价单管理、报价明细管理、报价接收
- **技术栈**: Spring Boot, JPA, MySQL
- **端口**: 8082

#### 2.3 比价分析服务 (comparison-analysis-service)
- **职责**: 报价比较、评分计算、分析报告
- **技术栈**: Spring Boot, 规则引擎, Redis
- **端口**: 8083

#### 2.4 采购决策服务 (purchase-decision-service)
- **职责**: 决策规则管理、审批流程、订单生成
- **技术栈**: Spring Boot, 工作流引擎, JPA
- **端口**: 8084

---

## 📡 API 设计规范

### 1. RESTful API 设计原则

#### 1.1 URL 设计
```
GET    /api/v1/inquiries           # 查询询价单列表
POST   /api/v1/inquiries           # 创建询价单
GET    /api/v1/inquiries/{id}      # 查询询价单详情
PUT    /api/v1/inquiries/{id}      # 更新询价单
DELETE /api/v1/inquiries/{id}      # 删除询价单

# 状态操作
POST   /api/v1/inquiries/{id}/publish  # 发布询价单
POST   /api/v1/inquiries/{id}/close    # 关闭询价单
POST   /api/v1/inquiries/{id}/cancel   # 取消询价单
```

#### 1.2 请求/响应格式
```json
// 请求示例
POST /api/v1/inquiries
{
  "title": "2026年Q1服务器采购询价",
  "inquiryType": "EQUIPMENT",
  "requirementDesc": "需要采购高性能服务器...",
  "deadlineDate": "2026-04-30T23:59:59",
  "urgencyLevel": "HIGH",
  "purchaserId": 1001,
  "invitedSupplierIds": [2001, 2002, 2003]
}

// 响应示例
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 1,
    "inquiryNo": "INQ202604280001",
    "title": "2026年Q1服务器采购询价",
    "status": "DRAFT",
    "createdAt": "2026-04-28T15:30:00"
  },
  "timestamp": "2026-04-28T15:30:00"
}
```

### 2. 核心API设计

#### 2.1 询价单管理API

| API | 方法 | 路径 | 功能描述 | 权限 |
|-----|------|------|----------|------|
| 创建询价单 | POST | `/api/v1/inquiries` | 创建新的询价单 | purchase:inquiry:create |
| 查询列表 | GET | `/api/v1/inquiries` | 查询询价单列表 | purchase:inquiry:view |
| 查询详情 | GET | `/api/v1/inquiries/{id}` | 查询询价单详情 | purchase:inquiry:view |
| 更新询价单 | PUT | `/api/v1/inquiries/{id}` | 更新询价单信息 | purchase:inquiry:update |
| 删除询价单 | DELETE | `/api/v1/inquiries/{id}` | 删除询价单 | purchase:inquiry:delete |
| 发布询价单 | POST | `/api/v1/inquiries/{id}/publish` | 发布询价单 | purchase:inquiry:publish |
| 关闭询价单 | POST | `/api/v1/inquiries/{id}/close` | 关闭询价单 | purchase:inquiry:close |
| 取消询价单 | POST | `/api/v1/inquiries/{id}/cancel` | 取消询价单 | purchase:inquiry:cancel |

#### 2.2 报价单管理API

| API | 方法 | 路径 | 功能描述 | 权限 |
|-----|------|------|----------|------|
| 提交报价 | POST | `/api/v1/quotes` | 供应商提交报价 | supplier:quote:submit |
| 查询列表 | GET | `/api/v1/quotes` | 查询报价单列表 | purchase:quote:view |
| 查询详情 | GET | `/api/v1/quotes/{id}` | 查询报价单详情 | purchase:quote:view |
| 更新报价 | PUT | `/api/v1/quotes/{id}` | 更新报价信息 | supplier:quote:update |
| 审查报价 | POST | `/api/v1/quotes/{id}/review` | 审查报价 | purchase:quote:review |
| 接受报价 | POST | `/api/v1/quotes/{id}/accept` | 接受报价 | purchase:quote:accept |
| 拒绝报价 | POST | `/api/v1/quotes/{id}/reject` | 拒绝报价 | purchase:quote:reject |
| 撤回报价 | POST | `/api/v1/quotes/{id}/withdraw` | 供应商撤回报价 | supplier:quote:withdraw |

#### 2.3 比价分析API

| API | 方法 | 路径 | 功能描述 | 权限 |
|-----|------|------|----------|------|
| 比价分析 | GET | `/api/v1/inquiries/{id}/comparison` | 询价单比价分析 | purchase:quote:compare |
| 报价对比 | GET | `/api/v1/quotes/comparison` | 多报价对比 | purchase:quote:compare |
| 评分计算 | POST | `/api/v1/quotes/{id}/score` | 计算报价评分 | purchase:quote:score |
| 分析报告 | GET | `/api/v1/inquiries/{id}/analysis-report` | 生成分析报告 | purchase:quote:report |

#### 2.4 采购决策API

| API | 方法 | 路径 | 功能描述 | 权限 |
|-----|------|------|----------|------|
| 决策推荐 | GET | `/api/v1/inquiries/{id}/recommendation` | 获取决策推荐 | purchase:decision:recommend |
| 确认中标 | POST | `/api/v1/quotes/{id}/win` | 确认中标报价 | purchase:decision:confirm |
| 生成订单 | POST | `/api/v1/quotes/{id}/generate-order` | 生成采购订单 | purchase:order:generate |
| 审批流程 | POST | `/api/v1/decisions/{id}/approve` | 审批决策 | purchase:decision:approve |

---

## 🗄️ 数据库设计

### 1. 核心表结构

#### 1.1 purchase_inquiry (询价单主表)
```sql
CREATE TABLE purchase_inquiry (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    inquiry_no VARCHAR(32) NOT NULL UNIQUE,
    title VARCHAR(200) NOT NULL,
    inquiry_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    requirement_desc TEXT,
    urgency_level VARCHAR(10) DEFAULT 'NORMAL',
    deadline_date DATETIME NOT NULL,
    publish_date DATETIME,
    close_date DATETIME,
    department_id BIGINT,
    requester_id BIGINT,
    purchaser_id BIGINT NOT NULL,
    invited_supplier_ids TEXT,
    quote_count INT DEFAULT 0,
    approval_status VARCHAR(20) DEFAULT 'PENDING',
    approved_by BIGINT,
    approval_date DATETIME,
    approval_comment TEXT,
    related_order_id BIGINT,
    created_by BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_at DATETIME,
    deleted TINYINT(1) DEFAULT 0,
    
    INDEX idx_inquiry_no (inquiry_no),
    INDEX idx_status (status),
    INDEX idx_deadline (deadline_date),
    INDEX idx_purchaser (purchaser_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### 1.2 purchase_inquiry_item (询价明细表)
```sql
CREATE TABLE purchase_inquiry_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    inquiry_id BIGINT NOT NULL,
    item_seq INT NOT NULL,
    material_id BIGINT,
    material_code VARCHAR(50),
    material_name VARCHAR(200) NOT NULL,
    specification TEXT,
    unit VARCHAR(20) NOT NULL,
    quantity DECIMAL(18,4) NOT NULL,
    min_quantity DECIMAL(18,4),
    quality_requirement TEXT,
    delivery_requirement TEXT,
    brand_requirement VARCHAR(200),
    estimated_price DECIMAL(18,2),
    estimated_amount DECIMAL(18,2),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME,
    
    INDEX idx_inquiry_id (inquiry_id),
    INDEX idx_material (material_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### 1.3 purchase_supplier_quote (供应商报价表)
```sql
CREATE TABLE purchase_supplier_quote (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    quote_no VARCHAR(32) NOT NULL UNIQUE,
    inquiry_id BIGINT NOT NULL,
    supplier_id BIGINT NOT NULL,
    quote_status VARCHAR(20) NOT NULL DEFAULT 'SUBMITTED',
    quote_date DATETIME NOT NULL,
    valid_until DATETIME NOT NULL,
    total_amount DECIMAL(18,2) NOT NULL,
    tax_rate DECIMAL(5,2) DEFAULT 13.00,
    tax_amount DECIMAL(18,2),
    payment_terms TEXT,
    delivery_terms TEXT,
    warranty_terms TEXT,
    supplier_note TEXT,
    competitive_advantage TEXT,
    attachment_urls TEXT,
    price_score DECIMAL(5,2),
    quality_score DECIMAL(5,2),
    service_score DECIMAL(5,2),
    total_score DECIMAL(5,2),
    is_recommended TINYINT(1) DEFAULT 0,
    recommend_reason TEXT,
    review_status VARCHAR(20) DEFAULT 'PENDING',
    reviewed_by BIGINT,
    review_date DATETIME,
    review_comment TEXT,
    created_by BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME,
    
    INDEX idx_quote_no (quote_no),
    INDEX idx_inquiry (inquiry_id),
    INDEX idx_supplier (supplier_id),
    INDEX idx_status (quote_status),
    INDEX idx_score (total_score)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 2. 索引优化策略

#### 2.1 查询优化索引
```sql
-- 高频查询优化
CREATE INDEX idx_inquiry_composite ON purchase_inquiry (purchaser_id, status, deadline_date);
CREATE INDEX idx_quote_composite ON purchase_supplier_quote (inquiry_id, quote_status, total_score);
CREATE INDEX idx_inquiry_item_composite ON purchase_inquiry_item (inquiry_id, item_seq);

-- 统计查询优化
CREATE INDEX idx_inquiry_date_status ON purchase_inquiry (created_at, status);
CREATE INDEX idx_quote_date_status ON purchase_supplier_quote (quote_date, quote_status);
```

#### 2.2 分区策略
```sql
-- 按时间分区（每月）
ALTER TABLE purchase_inquiry PARTITION BY RANGE (YEAR(created_at) * 100 + MONTH(created_at)) (
    PARTITION p202604 VALUES LESS THAN (202605),
    PARTITION p202605 VALUES LESS THAN (202606),
    PARTITION p202606 VALUES LESS THAN (202607),
    PARTITION p_future VALUES LESS THAN MAXVALUE
);
```

---

## 🔧 业务逻辑设计

### 1. 询价单状态机

```java
public enum InquiryStatus {
    DRAFT("草稿"),
    PENDING_APPROVAL("待审批"),
    PUBLISHED("已发布"),
    QUOTING("报价中"),
    CLOSED("已关闭"),
    CANCELLED("已取消");
    
    private final String description;
    
    InquiryStatus(String description) {
        this.description = description;
    }
    
    // 状态流转规则
    public static Map<InquiryStatus, List<InquiryStatus>> TRANSITION_RULES = Map.of(
        DRAFT, List.of(PENDING_APPROVAL, CANCELLED),
        PENDING_APPROVAL, List.of(PUBLISHED, REJECTED, CANCELLED),
        PUBLISHED, List.of(QUOTING, CLOSED, CANCELLED),
        QUOTING, List.of(CLOSED, CANCELLED),
        CLOSED, List.of(),
        CANCELLED, List.of()
    );
}
```

### 2. 报价单状态机

```java
public enum QuoteStatus {
    DRAFT("草稿"),
    SUBMITTED("已提交"),
    REVIEWED("已审查"),
    ACCEPTED("已接受"),
    REJECTED("已拒绝"),
    WITHDRAWN("已撤回"),
    WON("已中标");
    
    private final String description;
    
    // 状态流转规则
    public static boolean canTransition(QuoteStatus from, QuoteStatus to) {
        Map<QuoteStatus, List<QuoteStatus>> rules = Map.of(
            DRAFT, List.of(SUBMITTED),
            SUBMITTED, List.of(REVIEWED, WITHDRAWN),
            REVIEWED, List.of(ACCEPTED, REJECTED, WON),
            ACCEPTED, List.of(WON, REJECTED),
            WON, List.of(),
            REJECTED, List.of(),
            WITHDRAWN, List.of()
        );
        return rules.getOrDefault(from, List.of()).contains(to);
    }
}
```

### 3. 比价分析算法

#### 3.1 加权评分算法
```java
public class WeightedScoringAlgorithm {
    private Map<String, Double> weightMap; // 维度权重
    
    public QuoteScore calculateScore(SupplierQuote quote) {
        double priceScore = calculatePriceScore(quote);
        double qualityScore = calculateQualityScore(quote);
        double serviceScore = calculateServiceScore(quote);
        double deliveryScore = calculateDeliveryScore(quote);
        
        double totalScore = 
            priceScore * weightMap.get("price") +
            qualityScore * weightMap.get("quality") +
            serviceScore * weightMap.get("service") +
            deliveryScore * weightMap.get("delivery");
        
        return new QuoteScore(priceScore, qualityScore, serviceScore, deliveryScore, totalScore);
    }
    
    private double calculatePriceScore(SupplierQuote quote) {
        // 价格评分算法：价格越低得分越高
        double minPrice = getMinPriceInMarket();
        double price = quote.getTotalAmount();
        return Math.max(0, 100 - (price - minPrice) / minPrice * 100);
    }
}
```

#### 3.2 决策规则引擎
```java
public class DecisionRuleEngine {
    private List<DecisionRule> rules;
    
    public DecisionResult evaluate(QuoteComparison comparison) {
        DecisionResult result = new DecisionResult();
        
        for (DecisionRule rule : rules) {
            if (rule.evaluate(comparison)) {
                result.addRecommendation(rule.getRecommendation());
                result.addReason(rule.getReason());
            }
        }
        
        return result;
    }
}

// 决策规则示例
public class LowestPriceRule implements DecisionRule {
    @Override
    public boolean evaluate(QuoteComparison comparison) {
        SupplierQuote lowestQuote = comparison.getLowestPriceQuote();
        double priceDiffPercent = comparison.getPriceDifferencePercent();
        
        // 如果最低价与平均价差异超过10%，触发推荐
        return priceDiffPercent > 10.0;
    }
    
    @Override
    public String getRecommendation() {
        return "推荐选择最低价报价";
    }
}
```

---

## 🔐 安全设计

### 1. 认证与授权

#### 1.1 JWT Token认证
```java
@Component
public class JwtTokenProvider {
    private final String secretKey = "your-secret-key";
    private final long validityInMilliseconds = 3600000; // 1小时
    
    public String createToken(String username, List<String> roles) {
        Claims claims = Jwts.claims().setSubject(username);
        claims.put("roles", roles);
        
        Date now = new Date();
        Date validity = new Date(now.getTime() + validityInMilliseconds);
        
        return Jwts.builder()
            .setClaims(claims)
            .setIssuedAt(now)
            .setExpiration(validity)
            .signWith(SignatureAlgorithm.HS256, secretKey)
            .compact();
    }
}
```

#### 1.2 权限控制
```java
@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig extends WebSecurityConfigurerAdapter {
    
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .authorizeRequests()
                // 询价单相关权限
                .antMatchers(HttpMethod.POST, "/api/v1/inquiries").hasAuthority("purchase:inquiry:create")
                .antMatchers(HttpMethod.GET, "/api/v1/inquiries").hasAuthority("purchase:inquiry:view")
                .antMatchers(HttpMethod.PUT, "/api/v1/inquiries/*").hasAuthority("purchase:inquiry:update")
                
                // 报价单相关权限
                .antMatchers(HttpMethod.POST, "/api/v1/quotes").hasAnyAuthority("purchase:quote:create", "supplier:quote:submit")
                .antMatchers(HttpMethod.GET, "/api/v1/quotes/*").hasAuthority("purchase:quote:view")
                
                // 比价分析权限
                .antMatchers(HttpMethod.GET, "/api/v1/inquiries/*/comparison").hasAuthority("purchase:quote:compare")
                
                // 采购决策权限
                .antMatchers(HttpMethod.POST, "/api/v1/quotes/*/win").hasAuthority("purchase:decision:confirm")
            .anyRequest().authenticated();
    }
}
```

### 2. 数据权限控制

#### 2.1 行级权限控制
```java
@Service
public class DataPermissionService {
    
    public boolean canViewInquiry(Long inquiryId, Long userId) {
        PurchaseInquiry inquiry = inquiryRepository.findById(inquiryId);
        
        // 创建人可以查看
        if (inquiry.getCreatedBy().equals(userId)) {
            return true;
        }
        
        // 采购员可以查看
        if (inquiry.getPurchaserId().equals(userId)) {
            return true;
        }
        
        // 部门经理可以查看本部门的询价单
        if (isDepartmentManager(userId, inquiry.getDepartmentId())) {
            return true;
        }
        
        return false;
    }
}
```

---

## 📊 性能优化设计

### 1. 缓存策略

#### 1.1 Redis缓存配置
```yaml
# application-redis.yml
spring:
  redis:
    host: localhost
    port: 6379
    password: 
    database: 0
    timeout: 2000ms
    lettuce:
      pool:
        max-active: 8
        max-idle: 8
        min-idle: 0
```

#### 1.2 缓存注解使用
```java
@Service
public class InquiryServiceImpl implements InquiryService {
    
    @Cacheable(value = "inquiry", key = "#id")
    @Override
    public PurchaseInquiry getInquiryById(Long id) {
        return inquiryRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("询价单不存在"));
    }
    
    @CachePut(value = "inquiry", key = "#inquiry.id")
    @Override
    public PurchaseInquiry updateInquiry(PurchaseInquiry inquiry) {
        return inquiryRepository.save(inquiry);
    }
    
    @CacheEvict(value = "inquiry", key = "#id")
    @Override
    public void deleteInquiry(Long id) {
        inquiryRepository.deleteById(id);
    }
}
```

### 2. 数据库优化

#### 2.1 读写分离
```java
@Configuration
public class DataSourceConfig {
    
    @Primary
    @Bean(name = "masterDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.master")
    public DataSource masterDataSource() {
        return DataSourceBuilder.create().build();
    }
    
    @Bean(name = "slaveDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.slave")
    public DataSource slaveDataSource() {
        return DataSourceBuilder.create().build();
    }
    
    @Bean
    public DataSource routingDataSource(
            @Qualifier("masterDataSource") DataSource masterDataSource,
            @Qualifier("slaveDataSource") DataSource slaveDataSource) {
        
        Map<Object, Object> targetDataSources = new HashMap<>();
        targetDataSources.put("master", masterDataSource);
        targetDataSources.put("slave", slaveDataSource);
        
        RoutingDataSource routingDataSource = new RoutingDataSource();
        routingDataSource.setDefaultTargetDataSource(masterDataSource);
        routingDataSource.setTargetDataSources(targetDataSources);
        
        return routingDataSource;
    }
}
```

#### 2.2 分页查询优化
```java
@Repository
public interface InquiryRepository extends JpaRepository<PurchaseInquiry, Long> {
    
    @Query(value = "SELECT * FROM purchase_inquiry WHERE purchaser_id = ?1 ORDER BY created_at DESC LIMIT ?2 OFFSET ?3",
           nativeQuery = true)
    List<PurchaseInquiry> findByPurchaserIdWithPagination(Long purchaserId, int limit, int offset);
    
    @Query(value = "SELECT COUNT(*) FROM purchase_inquiry WHERE purchaser_id = ?1",
           nativeQuery = true)
    long countByPurchaserId(Long purchaserId);
}
```

---

## 📈 监控与日志

### 1. 监控指标

#### 1.1 业务监控指标
```java
@Component
public class BusinessMetrics {
    
    private final MeterRegistry meterRegistry;
    
    // 询价单创建次数
    private final Counter inquiryCreationCounter;
    
    // 报价提交次数
    private final Counter quoteSubmissionCounter;
    
    // 比价分析耗时
    private final Timer comparisonTimer;
    
    public BusinessMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        this.inquiryCreationCounter = Counter.builder("purchase.inquiry.creation.count")
            .description("询价单创建次数")
            .register(meterRegistry);
            
        this.quoteSubmissionCounter = Counter.builder("purchase.quote.submission.count")
            .description("报价提交次数")
            .register(meterRegistry);
            
        this.comparisonTimer = Timer.builder("purchase.comparison.duration")
            .description("比价分析耗时")
            .register(meterRegistry);
    }
}
```

#### 1.2 性能监控指标
- **API响应时间**: P50 < 100ms, P95 < 500ms, P99 < 1000ms
- **数据库查询时间**: < 50ms
- **缓存命中率**: > 90%
- **系统可用性**: > 99.9%

### 2. 日志规范

#### 2.1 结构化日志
```java
@Slf4j
@Service
public class InquiryServiceImpl implements InquiryService {
    
    @Override
    public PurchaseInquiry createInquiry(PurchaseInquiry inquiry) {
        log.info("开始创建询价单", 
            kv("title", inquiry.getTitle()),
            kv("type", inquiry.getInquiryType()),
            kv("purchaserId", inquiry.getPurchaserId()));
        
        try {
            PurchaseInquiry created = inquiryRepository.save(inquiry);
            
            log.info("询价单创建成功",
                kv("inquiryId", created.getId()),
                kv("inquiryNo", created.getInquiryNo()),
                kv("duration", System.currentTimeMillis() - startTime));
            
            return created;
        } catch (Exception e) {
            log.error("询价单创建失败",
                kv("error", e.getMessage()),
                kv("stackTrace", ExceptionUtils.getStackTrace(e)));
            throw new BusinessException("询价单创建失败", e);
        }
    }
}
```

#### 2.2 审计日志
```java
@Aspect
@Component
public class AuditLogAspect {
    
    @AfterReturning(pointcut = "@annotation(auditLog)", returning = "result")
    public void logAfterReturning(JoinPoint joinPoint, AuditLog auditLog, Object result) {
        String operation = auditLog.operation();
        String module = auditLog.module();
        Long userId = SecurityUtils.getCurrentUserId();
        
        AuditLogEntity logEntity = new AuditLogEntity();
        logEntity.setModule(module);
        logEntity.setOperation(operation);
        logEntity.setUserId(userId);
        logEntity.setRequestParams(JsonUtils.toJson(joinPoint.getArgs()));
        logEntity.setResponseResult(JsonUtils.toJson(result));
        logEntity.setOperationTime(new Date());
        
        auditLogRepository.save(logEntity);
    }
}
```

---

## 🚀 部署架构

### 1. 容器化部署

#### 1.1 Docker配置
```dockerfile
# Dockerfile
FROM openjdk:11-jre-slim
WORKDIR /app
COPY target/purchase-inquiry-service.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
```

#### 1.2 Docker Compose配置
```yaml
# docker-compose.yml
version: '3.8'
services:
  purchase-inquiry-service:
    build: ./purchase-inquiry-service
    ports:
      - "8081:8081"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - DB_HOST=mysql
      - REDIS_HOST=redis
    depends_on:
      - mysql
      - redis
  
  mysql:
    image: mysql:8.0
    ports:
      - "3306:3306"
    environment:
      - MYSQL_ROOT_PASSWORD=root
      - MYSQL_DATABASE=purchase_db
  
  redis:
    image: redis:6.2
    ports:
      - "6379:6379"
```

### 2. 高可用架构

#### 2.1 负载均衡配置
```nginx
# nginx.conf
upstream purchase_inquiry_service {
    server purchase-inquiry-service-1:8081;
    server purchase-inquiry-service-2:8081;
    server purchase-inquiry-service-3:8081;
}

server {
    listen 80;
    server_name purchase-api.example.com;
    
    location /api/v1/inquiries {
        proxy_pass http://purchase_inquiry_service;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

#### 2.2 服务注册与发现
```yaml
# application-eureka.yml
spring:
  application:
    name: purchase-inquiry-service
  
eureka:
  client:
    service-url:
      defaultZone: http://eureka-server:8761/eureka/
  instance:
    prefer-ip-address: true
    instance-id: ${spring.cloud.client.ip-address}:${server.port}
```

---

## 📋 测试策略

### 1. 单元测试
```java
@SpringBootTest
public class InquiryServiceTest {
    
    @MockBean
    private InquiryRepository inquiryRepository;
    
    @Autowired
    private InquiryService inquiryService;
    
    @Test
    public void testCreateInquiry() {
        // 准备测试数据
        PurchaseInquiry inquiry = new PurchaseInquiry();
        inquiry.setTitle("测试询价单");
        inquiry.setInquiryType("TEST");
        
        // 模拟Repository行为
        when(inquiryRepository.save(any(PurchaseInquiry.class)))
            .thenAnswer(invocation -> {
                PurchaseInquiry saved = invocation.getArgument(0);
                saved.setId(1L);
                saved.setInquiryNo("INQ202604280001");
                return saved;
            });
        
        // 执行测试
        PurchaseInquiry created = inquiryService.createInquiry(inquiry);
        
        // 验证结果
        assertNotNull(created.getId());
        assertEquals("INQ202604280001", created.getInquiryNo());
        assertEquals(InquiryStatus.DRAFT, created.getStatus());
    }
}
```

### 2. 集成测试
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public class InquiryControllerIntegrationTest {
    
    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");
    
    @Test
    public void testCreateInquiryApi() {
        // 准备请求数据
        InquiryCreateRequest request = new InquiryCreateRequest();
        request.setTitle("集成测试询价单");
        request.setInquiryType("INTEGRATION_TEST");
        
        // 发送请求
        ResponseEntity<InquiryResponse> response = restTemplate.postForEntity(
            "/api/v1/inquiries",
            request,
            InquiryResponse.class);
        
        // 验证响应
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody().getData().getId());
        assertEquals("DRAFT", response.getBody().getData().getStatus());
    }
}
```

### 3. 性能测试
```yaml
# purchase-inquiry-performance-test.jmx
<?xml version="1.0" encoding="UTF-8"?>
<jmeterTestPlan version="1.2" properties="5.0" jmeter="5.4.1">
  <hashTree>
    <TestPlan guiclass="TestPlanGui" testclass="TestPlan" testname="采购询价性能测试">
      <elementProp name="TestPlan.user_defined_variables" elementType="Arguments"/>
      <stringProp name="TestPlan.comments">采购询价API性能测试</stringProp>
      <boolProp name="TestPlan.functional_mode">false</boolProp>
      <boolProp name="TestPlan.serialize_threadgroups">false</boolProp>
    </TestPlan>
    <hashTree>
      <ThreadGroup guiclass="ThreadGroupGui" testclass="ThreadGroup" testname="并发用户组">
        <intProp name="ThreadGroup.num_threads">100</intProp>
        <intProp name="ThreadGroup.ramp_time">60</intProp>
        <longProp name="ThreadGroup.duration">300</longProp>
      </ThreadGroup>
    </hashTree>
  </hashTree>
</jmeterTestPlan>
```

---

## 📝 开发规范

### 1. 代码规范

#### 1.1 命名规范
- **类名**: 大驼峰，如 `PurchaseInquiryService`
- **方法名**: 小驼峰，如 `createInquiry`
- **变量名**: 小驼峰，如 `inquiryNo`
- **常量名**: 大写+下划线，如 `MAX_INQUIRY_COUNT`

#### 1.2 代码结构
```
src/main/java/cn/aiedge/erp/purchase/rfq/
├── controller/          # 控制器层
│   ├── InquiryController.java
│   └── QuoteController.java
├── service/            # 服务层接口
│   ├── InquiryService.java
│   └── QuoteService.java
├── service/impl/       # 服务层实现
│   ├── InquiryServiceImpl.java
│   └── QuoteServiceImpl.java
├── repository/         # 数据访问层
│   ├── InquiryRepository.java
│   └── QuoteRepository.java
├── entity/             # 实体类
│   ├── PurchaseInquiry.java
│   └── PurchaseSupplierQuote.java
├── dto/                # 数据传输对象
│   ├── InquiryDTO.java
│   └── QuoteDTO.java
├── enums/              # 枚举类
│   ├── InquiryStatus.java
│   └── QuoteStatus.java
└── config/             # 配置类
    ├── RedisConfig.java
    └── SecurityConfig.java
```

### 2. API文档规范

#### 2.1 Swagger配置
```java
@Configuration
@EnableSwagger2
public class SwaggerConfig {
    
    @Bean
    public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2)
            .select()
            .apis(RequestHandlerSelectors.basePackage("cn.aiedge.erp.purchase.rfq.controller"))
            .paths(PathSelectors.any())
            .build()
            .apiInfo(apiInfo())
            .securitySchemes(Arrays.asList(apiKey()));
    }
    
    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
            .title("采购询价/报价管理系统API文档")
            .description("采购询价、报价管理、比价分析、采购决策等API接口")
            .version("1.0.0")
            .build();
    }
}
```

---

## 🔧 运维手册

### 1. 部署步骤

#### 1.1 环境准备
```bash
# 1. 安装Docker和Docker Compose
sudo apt-get update
sudo apt-get install docker docker-compose

# 2. 创建项目目录
mkdir -p /opt/purchase-rfq
cd /opt/purchase-rfq

# 3. 创建配置文件
cp config/application-prod.yml /opt/purchase-rfq/
```

#### 1.2 启动服务
```bash
# 1. 启动服务
docker-compose up -d

# 2. 查看服务状态
docker-compose ps

# 3. 查看日志
docker-compose logs -f purchase-inquiry-service
```

### 2. 监控与告警

#### 2.1 Prometheus监控配置
```yaml
# prometheus.yml
scrape_configs:
  - job_name: 'purchase-inquiry-service'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['purchase-inquiry-service:8081