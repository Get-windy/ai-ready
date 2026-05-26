# AI-Ready 搜索服务模块文档

## 概述

本模块提供全局搜索能力，支持客户、产品、订单等多类型数据的全文搜索，并提供搜索建议、搜索历史、热门搜索等功能。

## 模块结构

```
cn.aiedge.search
├── config/
│   └── SearchConfig.java         # 搜索配置
├── controller/
│   └── SearchController.java     # 搜索接口
├── model/
│   ├── SearchRequest.java        # 搜索请求
│   ├── SearchResponse.java       # 搜索响应
│   ├── SearchResult.java         # 搜索结果
│   ├── SearchSuggestion.java     # 搜索建议
│   └── SearchHistory.java        # 搜索历史
└── service/
    ├── SearchService.java        # 搜索服务接口
    └── impl/
        └── SearchServiceImpl.java # 搜索服务实现
```

## 功能特性

### 1. 全局搜索
- 支持多类型数据同时搜索（客户、产品、订单）
- 支持分页、排序
- 支持关键词高亮显示
- 按匹配度智能排序

### 2. 搜索建议
- 基于用户历史的建议
- 基于热门搜索的建议
- 基于索引的建议

### 3. 搜索历史
- 用户级别的历史记录
- 支持清空、删除单条
- 自动过期清理

### 4. 热门搜索
- 按时间周期统计
- 实时更新热度
- 支持租户隔离

## 快速开始

### 1. 全局搜索

```bash
# POST 方式
POST /api/search
Content-Type: application/json

{
  "keyword": "客户名称",
  "type": "all",
  "page": 1,
  "pageSize": 10,
  "highlight": true
}

# GET 方式（快速搜索）
GET /api/search?keyword=客户名称&type=all&page=1&pageSize=10
```

### 2. 单类型搜索

```bash
# 搜索客户
GET /api/search/customer?keyword=张三&page=1&pageSize=10

# 搜索产品
GET /api/search/product?keyword=手机&page=1&pageSize=10

# 搜索订单
GET /api/search/order?keyword=ORD001&page=1&pageSize=10
```

### 3. 搜索建议

```bash
GET /api/search/suggestions?prefix=张&limit=10
```

响应示例：
```json
[
  { "text": "张三公司", "type": "history", "score": 1.0 },
  { "text": "张四科技", "type": "hot", "count": 128 },
  { "text": "张五集团", "type": "keyword", "score": 0.5 }
]
```

### 4. 热门搜索词

```bash
GET /api/search/hot?limit=10
```

响应示例：
```json
{
  "hotSearches": ["手机", "客户", "订单", "产品"],
  "count": 4
}
```

### 5. 搜索历史

```bash
# 获取历史
GET /api/search/history?limit=20

# 清空历史
DELETE /api/search/history

# 删除单条历史
DELETE /api/search/history/1
```

## 索引管理

### 创建索引

```bash
# 索引客户
POST /api/search/index/customer/1
Content-Type: application/json

{
  "name": "张三公司",
  "code": "C001",
  "contact": "张三",
  "phone": "13800138000",
  "email": "zhangsan@example.com"
}

# 索引产品
POST /api/search/index/product/1

{
  "name": "智能手机",
  "code": "P001",
  "category": "电子产品",
  "description": "高性能智能手机"
}

# 索引订单
POST /api/search/index/order/1

{
  "orderNo": "ORD202604040001",
  "customerName": "张三公司",
  "productName": "智能手机",
  "status": "已完成"
}
```

### 删除索引

```bash
DELETE /api/search/index/customer/1
DELETE /api/search/index/product/1
DELETE /api/search/index/order/1
```

### 重建索引

```bash
# 重建单个类型索引
POST /api/search/index/rebuild/customer

# 重建所有索引
POST /api/search/index/rebuild/all
```

## 配置说明

### application.yml

```yaml
search:
  # 是否启用搜索服务
  enabled: true
  
  # 分页配置
  default-page-size: 10
  max-page-size: 100
  
  # 搜索历史配置
  history-retention-days: 30
  
  # 热门搜索配置
  hot-search-period: 7      # 统计周期（天）
  hot-search-count: 10      # 显示数量
  
  # 搜索建议配置
  suggestion-min-chars: 1   # 最小字符数
  suggestion-max-count: 10  # 最大建议数
  
  # 索引配置
  indices:
    customer:
      enabled: true
      name: customer
      search-fields:
        - name
        - code
        - contact
        - phone
        - email
      field-weights:
        name: 3.0
        code: 2.5
        contact: 1.5
      result-type: customer
    
    product:
      enabled: true
      name: product
      search-fields:
        - name
        - code
        - category
        - description
      field-weights:
        name: 3.0
        code: 2.5
      result-type: product
    
    order:
      enabled: true
      name: order
      search-fields:
        - orderNo
        - customerName
        - productName
        - status
      field-weights:
        orderNo: 3.0
        customerName: 2.0
      result-type: order
```

## 技术实现

### 存储架构

本模块基于 Redis 实现搜索功能：

```
搜索索引: search:index:{type}:{tenantId}
  └── Hash类型，存储各类型数据的搜索索引

搜索历史: search:history:{userId}
  └── List类型，存储用户搜索历史

热门搜索: search:hot:{yyyyMMdd}
  └── ZSet类型，按热度排序的搜索词

搜索建议: search:suggest:{prefix}
  └── Set类型，存储以某字符开头的搜索词
```

### 匹配算法

1. **完全匹配**: 分数 3.0 × 字段权重
2. **前缀匹配**: 分数 2.0 × 字段权重
3. **包含匹配**: 分数 1.0 × 字段权重

结果按匹配分数降序排列。

### 高亮实现

使用正则替换实现关键词高亮：

```java
text.replaceAll("(?i)(" + keyword + ")", "<em>$1</em>");
```

## 最佳实践

### 1. 索引更新

建议在数据变更时同步更新索引：

```java
@Service
public class CustomerService {
    
    @Autowired
    private SearchService searchService;
    
    public void saveCustomer(Customer customer) {
        // 保存客户
        customerRepository.save(customer);
        
        // 更新搜索索引
        Map<String, Object> indexData = new HashMap<>();
        indexData.put("name", customer.getName());
        indexData.put("code", customer.getCode());
        indexData.put("contact", customer.getContact());
        searchService.indexCustomer(customer.getId(), indexData, customer.getTenantId());
    }
}
```

### 2. 批量索引

对于大量数据，建议使用批量索引：

```java
@Scheduled(cron = "0 0 2 * * ?") // 每天凌晨2点
public void rebuildSearchIndex() {
    searchService.rebuildIndex("customer", tenantId);
    searchService.rebuildIndex("product", tenantId);
    searchService.rebuildIndex("order", tenantId);
}
```

### 3. 搜索性能优化

- 使用 Redis 缓存热门搜索结果
- 对长关键词进行分词搜索
- 限制搜索结果数量
- 使用异步方式更新索引

## 扩展说明

### 添加新的搜索类型

1. 在 `SearchConfig` 中添加索引配置：

```java
IndexConfig newIndex = new IndexConfig();
newIndex.setName("newType");
newIndex.setSearchFields(Arrays.asList("field1", "field2"));
newIndex.setFieldWeights(Map.of("field1", 3.0, "field2", 1.0));
configs.put("newType", newIndex);
```

2. 在 `SearchServiceImpl` 中添加搜索逻辑：

```java
case "newType":
    return SearchResult.fromNewType(id, data, score);
```

3. 在 `SearchController` 中添加接口：

```java
@GetMapping("/newType")
public ResponseEntity<List<SearchResult>> searchNewType(...) {
    // 实现搜索逻辑
}
```

### 集成 Elasticsearch

如需更强大的搜索能力，可集成 Elasticsearch：

1. 添加依赖：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-elasticsearch</artifactId>
</dependency>
```

2. 创建 Elasticsearch 实现：

```java
@Service
@ConditionalOnProperty(name = "search.engine", havingValue = "elasticsearch")
public class ElasticsearchSearchServiceImpl implements SearchService {
    // 使用 Elasticsearch 实现
}
```

## 版本历史

| 版本 | 日期 | 说明 |
|------|------|------|
| 1.0.0 | 2026-04-04 | 初始版本，支持基础搜索功能 |

---

*AI-Ready Team © 2026*
