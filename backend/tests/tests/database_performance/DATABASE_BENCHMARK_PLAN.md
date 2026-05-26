# AI-Ready 数据库性能基准测试计划

## 测试目标
1. 评估 MySQL、PostgreSQL 和 Redis 在不同工作负载下的性能表现
2. 提供数据库性能优化建议
3. 建立性能基准线用于后续版本对比

## 测试范围

### 1. MySQL 性能测试
- **读取性能**: 简单查询、复杂查询、分页查询
- **写入性能**: 单条插入、批量插入、更新、删除
- **并发性能**: 多线程并发读写
- **连接池性能**: 不同连接池大小的性能影响

### 2. PostgreSQL 性能测试
- **读取性能**: 简单查询、复杂查询（JOIN、子查询）、JSON查询
- **写入性能**: 单条插入、批量插入、更新、删除
- **并发性能**: 多线程并发读写
- **索引性能**: B-tree、GIN、GiST 索引性能对比

### 3. Redis 性能测试
- **基本操作**: SET/GET、HSET/HGET、LPUSH/LPOP
- **批量操作**: MSET/MGET、Pipeline
- **数据结构性能**: String、Hash、List、Set、Sorted Set
- **并发性能**: 多客户端并发访问

## 测试指标
- **响应时间**: 平均响应时间、P95、P99
- **吞吐量**: QPS (Queries Per Second)、TPS (Transactions Per Second)
- **资源利用率**: CPU、内存、磁盘 I/O
- **稳定性**: 长时间运行的性能波动

## 测试环境配置
- **MySQL**: localhost:3306, ai_ready_test database
- **PostgreSQL**: localhost:5432, ai_ready_test database  
- **Redis**: localhost:6379, database 0

## 测试数据准备
- 每个数据库创建 10,000 条测试记录
- 包含各种数据类型：字符串、数字、日期、JSON
- 创建适当的索引以模拟生产环境

## 验收标准
- ✅ 测试方案覆盖读写性能、并发性能
- ✅ 测试数据准备充分
- ✅ 性能指标采集完整
- ✅ 提供性能优化建议