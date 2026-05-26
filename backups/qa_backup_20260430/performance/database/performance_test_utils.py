"""
数据库性能测试工具类
用于Sprint 27+1测试环境数据库性能优化与压力测试
"""

import os
import time
import json
import psycopg2
import redis
import logging
from datetime import datetime
from dataclasses import dataclass
from typing import Dict, List, Any, Optional, Tuple
from pathlib import Path

# 配置日志
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

@dataclass
class DatabaseConfig:
    """数据库配置"""
    name: str
    host: str
    port: int
    database: str
    username: str
    password: str

@dataclass
class PerformanceMetrics:
    """性能指标"""
    query_time: Dict[str, float]  # 查询类型 -> 时间(ms)
    throughput: float  # 查询/秒
    latency_p95: float  # P95延迟(ms)
    latency_p99: float  # P99延迟(ms)
    error_rate: float  # 错误率
    connection_usage: float  # 连接池使用率
    cpu_usage: float  # CPU使用率
    memory_usage: float  # 内存使用率

@dataclass
class StressTestResult:
    """压力测试结果"""
    concurrent_users: int
    total_requests: int
    successful_requests: int
    failed_requests: int
    avg_response_time: float
    max_response_time: float
    min_response_time: float
    throughput: float
    error_rate: float

class DatabaseAnalyzer:
    """数据库分析器"""
    
    def __init__(self):
        self.connection = None
        self.cursor = None
    
    def connect(self, config: DatabaseConfig) -> bool:
        """连接到数据库"""
        try:
            self.connection = psycopg2.connect(
                host=config.host,
                port=config.port,
                database=config.database,
                user=config.username,
                password=config.password,
                connect_timeout=10
            )
            self.cursor = self.connection.cursor()
            logger.info(f"成功连接到数据库: {config.name}")
            return True
        except Exception as e:
            logger.error(f"连接数据库失败 {config.name}: {str(e)}")
            return False
    
    def disconnect(self):
        """断开数据库连接"""
        if self.cursor:
            self.cursor.close()
        if self.connection:
            self.connection.close()
    
    def analyze_performance(self, config: DatabaseConfig) -> Dict[str, Any]:
        """分析数据库性能"""
        results = {
            "database": config.name,
            "timestamp": datetime.now().isoformat(),
            "configuration": {},
            "performance_metrics": {},
            "bottlenecks": [],
            "recommendations": []
        }
        
        try:
            # 1. 分析数据库配置
            config_analysis = self._analyze_configuration()
            results["configuration"] = config_analysis
            
            # 2. 分析性能指标
            performance_metrics = self._analyze_performance_metrics()
            results["performance_metrics"] = performance_metrics
            
            # 3. 识别瓶颈
            bottlenecks = self._identify_bottlenecks(config_analysis, performance_metrics)
            results["bottlenecks"] = bottlenecks
            
            # 4. 生成优化建议
            recommendations = self._generate_recommendations(bottlenecks)
            results["recommendations"] = recommendations
            
            # 5. 计算性能分数
            performance_score = self._calculate_performance_score(performance_metrics, bottlenecks)
            results["performance_score"] = performance_score
            
        except Exception as e:
            logger.error(f"数据库分析失败 {config.name}: {str(e)}")
            results["error"] = str(e)
        
        return results
    
    def _analyze_configuration(self) -> Dict[str, Any]:
        """分析数据库配置"""
        config = {}
        
        try:
            # 获取数据库版本
            self.cursor.execute("SELECT version();")
            config["version"] = self.cursor.fetchone()[0]
            
            # 获取配置参数
            queries = [
                ("max_connections", "SHOW max_connections;"),
                ("shared_buffers", "SHOW shared_buffers;"),
                ("effective_cache_size", "SHOW effective_cache_size;"),
                ("work_mem", "SHOW work_mem;"),
                ("maintenance_work_mem", "SHOW maintenance_work_mem;"),
                ("checkpoint_completion_target", "SHOW checkpoint_completion_target;"),
                ("wal_buffers", "SHOW wal_buffers;"),
                ("default_statistics_target", "SHOW default_statistics_target;"),
                ("random_page_cost", "SHOW random_page_cost;"),
                ("effective_io_concurrency", "SHOW effective_io_concurrency;"),
                ("max_wal_size", "SHOW max_wal_size;"),
                ("min_wal_size", "SHOW min_wal_size;")
            ]
            
            for param_name, query in queries:
                try:
                    self.cursor.execute(query)
                    result = self.cursor.fetchone()
                    config[param_name] = result[0] if result else "N/A"
                except:
                    config[param_name] = "N/A"
            
        except Exception as e:
            logger.warning(f"配置分析失败: {str(e)}")
        
        return config
    
    def _analyze_performance_metrics(self) -> Dict[str, Any]:
        """分析性能指标"""
        metrics = {}
        
        try:
            # 获取连接统计
            self.cursor.execute("""
                SELECT 
                    COUNT(*) as total_connections,
                    COUNT(*) FILTER (WHERE state = 'active') as active_connections,
                    COUNT(*) FILTER (WHERE state = 'idle') as idle_connections
                FROM pg_stat_activity 
                WHERE datname = current_database();
            """)
            conn_stats = self.cursor.fetchone()
            metrics["connections"] = {
                "total": conn_stats[0],
                "active": conn_stats[1],
                "idle": conn_stats[2]
            }
            
            # 获取缓存命中率
            self.cursor.execute("""
                SELECT 
                    sum(heap_blks_hit) / (sum(heap_blks_hit) + sum(heap_blks_read)) as heap_hit_ratio,
                    sum(idx_blks_hit) / (sum(idx_blks_hit) + sum(idx_blks_read)) as idx_hit_ratio
                FROM pg_statio_user_tables;
            """)
            cache_stats = self.cursor.fetchone()
            metrics["cache_hit_ratio"] = {
                "heap": float(cache_stats[0]) if cache_stats[0] else 0,
                "index": float(cache_stats[1]) if cache_stats[1] else 0
            }
            
            # 获取慢查询统计
            self.cursor.execute("""
                SELECT 
                    COUNT(*) as total_queries,
                    COUNT(*) FILTER (WHERE total_time > 1000) as slow_queries,
                    AVG(total_time) as avg_query_time,
                    MAX(total_time) as max_query_time
                FROM pg_stat_statements 
                WHERE query NOT LIKE '%%pg_stat_%%';
            """)
            query_stats = self.cursor.fetchone()
            metrics["query_statistics"] = {
                "total_queries": query_stats[0],
                "slow_queries": query_stats[1],
                "avg_query_time_ms": float(query_stats[2]) if query_stats[2] else 0,
                "max_query_time_ms": float(query_stats[3]) if query_stats[3] else 0
            }
            
            # 获取索引使用情况
            self.cursor.execute("""
                SELECT 
                    schemaname,
                    relname,
                    seq_scan,
                    idx_scan,
                    CASE 
                        WHEN seq_scan + idx_scan > 0 
                        THEN idx_scan::float / (seq_scan + idx_scan) 
                        ELSE 0 
                    END as idx_usage_ratio
                FROM pg_stat_user_tables
                WHERE schemaname NOT LIKE 'pg_%%'
                ORDER BY idx_usage_ratio ASC
                LIMIT 10;
            """)
            index_usage = self.cursor.fetchall()
            metrics["index_usage"] = [
                {
                    "schema": row[0],
                    "table": row[1],
                    "seq_scan": row[2],
                    "idx_scan": row[3],
                    "idx_usage_ratio": float(row[4])
                }
                for row in index_usage
            ]
            
        except Exception as e:
            logger.warning(f"性能指标分析失败: {str(e)}")
        
        return metrics
    
    def _identify_bottlenecks(self, config: Dict[str, Any], metrics: Dict[str, Any]) -> List[Dict[str, Any]]:
        """识别性能瓶颈"""
        bottlenecks = []
        
        try:
            # 检查连接数
            if "connections" in metrics:
                conn = metrics["connections"]
                max_conn = int(config.get("max_connections", 0))
                if max_conn > 0:
                    usage = conn["total"] / max_conn
                    if usage > 0.8:
                        bottlenecks.append({
                            "type": "connection_pool",
                            "severity": "high",
                            "description": f"连接池使用率过高: {usage:.1%}",
                            "recommendation": "增加max_connections或优化连接管理"
                        })
            
            # 检查缓存命中率
            if "cache_hit_ratio" in metrics:
                cache = metrics["cache_hit_ratio"]
                if cache.get("heap", 0) < 0.9:
                    bottlenecks.append({
                        "type": "cache",
                        "severity": "medium",
                        "description": f"堆缓存命中率低: {cache['heap']:.1%}",
                        "recommendation": "增加shared_buffers或优化查询模式"
                    })
                if cache.get("index", 0) < 0.95:
                    bottlenecks.append({
                        "type": "index_cache",
                        "severity": "medium",
                        "description": f"索引缓存命中率低: {cache['index']:.1%}",
                        "recommendation": "检查索引使用情况，优化索引策略"
                    })
            
            # 检查慢查询
            if "query_statistics" in metrics:
                query = metrics["query_statistics"]
                if query.get("slow_queries", 0) > 0:
                    bottlenecks.append({
                        "type": "slow_query",
                        "severity": "high",
                        "description": f"存在 {query['slow_queries']} 个慢查询",
                        "recommendation": "分析慢查询，优化SQL语句和索引"
                    })
            
            # 检查索引使用
            if "index_usage" in metrics:
                for idx in metrics["index_usage"]:
                    if idx["idx_usage_ratio"] < 0.1:
                        bottlenecks.append({
                            "type": "index_usage",
                            "severity": "low",
                            "description": f"表 {idx['table']} 索引使用率低: {idx['idx_usage_ratio']:.1%}",
                            "recommendation": f"检查表 {idx['table']} 的索引策略"
                        })
            
        except Exception as e:
            logger.warning(f"瓶颈识别失败: {str(e)}")
        
        return bottlenecks
    
    def _generate_recommendations(self, bottlenecks: List[Dict[str, Any]]) -> List[str]:
        """生成优化建议"""
        recommendations = []
        
        for bottleneck in bottlenecks:
            if "recommendation" in bottleneck:
                recommendations.append(bottleneck["recommendation"])
        
        # 添加通用建议
        if not recommendations:
            recommendations.append("数据库性能良好，建议定期监控")
        
        recommendations.append("定期执行VACUUM ANALYZE维护")
        recommendations.append("监控慢查询日志")
        recommendations.append("定期备份数据库")
        
        return recommendations
    
    def _calculate_performance_score(self, metrics: Dict[str, Any], bottlenecks: List[Dict[str, Any]]) -> float:
        """计算性能分数 (0-100)"""
        score = 100.0
        
        try:
            # 根据瓶颈扣分
            for bottleneck in bottlenecks:
                severity = bottleneck.get("severity", "low")
                if severity == "high":
                    score -= 20
                elif severity == "medium":
                    score -= 10
                elif severity == "low":
                    score -= 5
            
            # 根据指标调整分数
            if "cache_hit_ratio" in metrics:
                cache = metrics["cache_hit_ratio"]
                heap_ratio = cache.get("heap", 0)
                idx_ratio = cache.get("index", 0)
                
                if heap_ratio < 0.8:
                    score -= (0.8 - heap_ratio) * 50
                if idx_ratio < 0.9:
                    score -= (0.9 - idx_ratio) * 30
            
            # 确保分数在0-100之间
            score = max(0, min(100, score))
            
        except Exception as e:
            logger.warning(f"性能分数计算失败: {str(e)}")
            score = 0
        
        return score

class PerformanceTester:
    """性能测试器"""
    
    def __init__(self):
        self.connection = None
        self.cursor = None
    
    def connect(self, config: DatabaseConfig) -> bool:
        """连接到数据库"""
        try:
            self.connection = psycopg2.connect(
                host=config.host,
                port=config.port,
                database=config.database,
                user=config.username,
                password=config.password,
                connect_timeout=10
            )
            self.cursor = self.connection.cursor()
            return True
        except Exception as e:
            logger.error(f"连接数据库失败 {config.name}: {str(e)}")
            return False
    
    def run_tests(self, config: DatabaseConfig, bottlenecks: List[Dict[str, Any]]) -> Dict[str, Any]:
        """运行性能测试"""
        results = {
            "database": config.name,
            "timestamp": datetime.now().isoformat(),
            "tests": {},
            "summary": {}
        }
        
        try:
            # 1. 基本查询测试
            basic_tests = self._run_basic_query_tests()
            results["tests"]["basic_queries"] = basic_tests
            
            # 2. 连接池测试
            connection_tests = self._run_connection_pool_tests()
            results["tests"]["connection_pool"] = connection_tests
            
            # 3. 索引测试
            index_tests = self._run_index_tests()
            results["tests"]["index_performance"] = index_tests
            
            # 4. 事务测试
            transaction_tests = self._run_transaction_tests()
            results["tests"]["transaction_performance"] = transaction_tests
            
            # 生成测试摘要
            summary = self._generate_test_summary(results["tests"])
            results["summary"] = summary
            
        except Exception as e:
            logger.error(f"性能测试失败 {config.name}: {str(e)}")
            results["error"] = str(e)
        
        return results
    
    def _run_basic_query_tests(self) -> Dict[str, Any]:
        """运行基本查询测试"""
        tests = {}
        
        try:
            # 简单查询
            start_time = time.time()
            self.cursor.execute("SELECT 1")
            simple_query_time = (time.time() - start_time) * 1000
            tests["simple_query_ms"] = simple_query_time
            
            # 复杂查询
            start_time = time.time()
            self.cursor.execute("""
                SELECT schemaname, tablename, tableowner 
                FROM pg_tables 
                WHERE schemaname NOT IN ('pg_catalog', 'information_schema')
                LIMIT 100
            """)
            complex_query_time = (time.time() - start_time) * 1000
            tests["complex_query_ms"] = complex_query_time
            
            # 聚合查询
            start_time = time.time()
            self.cursor.execute("""
                SELECT COUNT(*) as table_count 
                FROM pg_tables 
                WHERE schemaname NOT IN ('pg_catalog', 'information_schema')
            """)
            aggregate_query_time = (time.time() - start_time) * 1000
            tests["aggregate_query_ms"] = aggregate_query_time
            
        except Exception as e:
            logger.warning(f"基本查询测试失败: {str(e)}")
        
        return tests
    
    def _run_connection_pool_tests(self) -> Dict[str, Any]:
        """运行连接池测试"""
        tests = {}
        
        try:
            # 测试连接建立时间
            connection_times = []
            for i in range(10):
                start_time = time.time()
                conn = psycopg2.connect(
                    host=self.connection.info.host,
                    port=self.connection.info.port,
                    database=self.connection.info.dbname,
                    user=self.connection.info.user,
                    password="",  # 实际中应该从配置获取
                    connect_timeout=5
                )
                conn.close()
                connection_times.append((time.time() - start_time) * 1000)
            
            tests["connection_times_ms"] = {
                "min": min(connection_times),
                "max": max(connection_times),
                "avg": sum(connection_times) / len(connection_times),
                "p95": sorted(connection_times)[int(len(connection_times) * 0.95)]
            }
            
        except Exception as e:
            logger.warning(f"连接池测试失败: {str(e)}")
        
        return tests
    
    def _run_index_tests(self) -> Dict[str, Any]:
        """运行索引测试"""
        tests = {}
        
        try:
            # 创建测试表
            self.cursor.execute("""
                CREATE TABLE IF NOT EXISTS performance_test_index (
                    id SERIAL PRIMARY KEY,
                    name VARCHAR(100),
                    value INTEGER,
                    created_at TIMESTAMP DEFAULT NOW()
                )
            """)
            
            # 插入测试数据
            self.cursor.execute("DELETE FROM performance_test_index")
            for i in range(1000):
                self.cursor.execute(
                    "INSERT INTO performance_test_index (name, value) VALUES (%s, %s)",
                    (f"test_{i}", i)
                )
            self.connection.commit()
            
            # 无索引查询
            start_time = time.time()
            self.cursor.execute("SELECT * FROM performance_test_index WHERE value = 500")
            no_index_time = (time.time() - start_time) * 1000
            tests["no_index_query_ms"] = no_index_time
            
            # 创建索引
            self.cursor.execute("CREATE INDEX IF NOT EXISTS idx_performance_test_value ON performance_test_index(value)")
            
            # 有索引查询
            start_time = time.time()
            self.cursor.execute("SELECT * FROM performance_test_index WHERE value = 500")
            with_index_time = (time.time() - start_time) * 1000
            tests["with_index_query_ms"] = with_index_time
            
            # 计算性能提升
            if no_index_time > 0:
                improvement = (no_index_time - with_index_time) / no_index_time * 100
                tests["index_improvement_percent"] = improvement
            
            # 清理
            self.cursor.execute("DROP TABLE IF EXISTS performance_test_index")
            self.connection.commit()
            
        except Exception as e:
            logger.warning(f"索引测试失败: {str(e)}")
            try:
                self.cursor.execute("DROP TABLE IF EXISTS performance_test_index")
                self.connection.commit()
            except:
                pass
        
        return tests
    
    def _run_transaction_tests(self) -> Dict[str, Any]:
        """运行事务测试"""
        tests = {}
        
        try:
            # 创建测试表
            self.cursor.execute("""
                CREATE TABLE IF NOT EXISTS performance_test_transaction (
                    id SERIAL PRIMARY KEY,
                    account_id INTEGER,
                    amount DECIMAL(10,2),
                    transaction_time TIMESTAMP DEFAULT NOW()
                )
            """)
            
            # 测试事务性能
            transaction_times = []
            for i in range(100):
                start_time = time.time()
                try:
                    self.cursor.execute("BEGIN")
                    for j in range(10):
                        self.cursor.execute(
                            "INSERT INTO performance_test_transaction (account_id, amount) VALUES (%s, %s)",
                            (i, 100.0 + j)
                        )
                    self.cursor.execute("COMMIT")
                    transaction_times.append((time.time() - start_time) * 1000)
                except:
                    self.cursor.execute("ROLLBACK")
            
            if transaction_times:
                tests["transaction_times_ms"] = {
                    "min": min(transaction_times),
                    "max": max(transaction_times),
                    "avg": sum(transaction_times) / len(transaction_times),
                    "p95": sorted(transaction_times)[int(len(transaction_times) * 0.95)]
                }
            
            # 清理
            self.cursor.execute("DROP TABLE IF EXISTS performance_test_transaction")
            self.connection.commit()
            
        except Exception as e:
            logger.warning(f"事务测试失败: {str(e)}")
            try:
                self.cursor.execute("DROP TABLE IF EXISTS performance_test_transaction")
                self.connection.commit()
            except:
                pass
        
        return tests
    
    def _generate_test_summary(self, test_results: Dict[str, Any]) -> Dict[str, Any]:
        """生成测试摘要"""
        summary = {
            "performance_score": 100,
            "recommendations": [],
            "issues_found": []
        }
        
        try:
            # 评估查询性能
            if "basic_queries" in test_results:
                basic = test_results["basic_queries"]
                
                if basic.get("simple_query_ms", 100) > 10:
                    summary["issues_found"].append("简单查询响应时间过长")
                    summary["performance_score"] -= 10
                
                if basic.get("complex_query_ms", 1000) > 100:
                    summary["issues_found"].append("复杂查询响应时间过长")
                    summary["performance_score"] -= 15
            
            # 评估连接性能
            if "connection_pool" in test_results:
                conn = test_results["connection_pool"]
                if "connection_times_ms" in conn:
                    avg_time = conn["connection_times_ms"].get("avg", 0)
                    if avg_time > 100:
                        summary["issues_found"].append("连接建立时间过长")
                        summary["performance_score"] -= 10
            
            # 评估索引性能
            if "index_performance" in test_results:
                idx = test_results["index_performance"]
                improvement = idx.get("index_improvement_percent", 0)
                if improvement < 50:
                    summary["issues_found"].append("索引性能提升不足")
                    summary["performance_score"] -= 5
                    summary["recommendations"].append("优化索引设计")
            
            # 评估事务性能
            if "transaction_performance" in test_results:
                trans = test_results["transaction_performance"]
                if "transaction_times_ms" in trans:
                    avg_time = trans["transaction_times_ms"].get("avg", 0)
                    if avg_time > 500:
                        summary["issues_found"].append("事务处理时间过长")
                        summary["performance_score"] -= 15
                        summary["recommendations"].append("优化事务逻辑")
            
            # 确保分数在0-100之间
            summary["performance_score"] = max(0, min(100, summary["performance_score"]))
            
            # 根据性能分数给出总体建议
            if summary["performance_score"] >= 90:
                summary["overall_assessment"] = "优秀"
                summary["recommendations"].append("继续保持当前配置，定期监控")
            elif summary["performance_score"] >= 70:
                summary["overall_assessment"] = "良好"
                summary["recommendations"].append("存在优化空间，建议实施上述改进")
            elif summary["performance_score"] >= 50:
                summary["overall_assessment"] = "一般"
                summary["recommendations"].append("需要显著优化，优先处理高优先级问题")
            else:
                summary["overall_assessment"] = "较差"
                summary["recommendations"].append("急需全面优化，建议进行架构评审")
            
        except Exception as e:
            logger.warning(f"测试摘要生成失败: {str(e)}")
        
        return summary

class StressTester:
    """压力测试器"""
    
    def __init__(self):
        self.connection = None
        self.cursor = None
    
    def connect(self, config: DatabaseConfig) -> bool:
        """连接到数据库"""
        try:
            self.connection = psycopg2.connect(
                host=config.host,
                port=config.port,
                database=config.database,
                user=config.username,
                password=config.password,
                connect_timeout=10
            )
            self.cursor = self.connection.cursor()
            return True
        except Exception as e:
            logger.error(f"连接数据库失败 {config.name}: {str(e)}")
            return False
    
    def run_stress_test(self, config: DatabaseConfig) -> Dict[str, Any]:
        """运行压力测试"""
        results = {
            "database": config.name,
            "timestamp": datetime.now().isoformat(),
            "test_scenarios": [],
            "summary": {}
        }
        
        try:
            # 准备测试数据
            self._prepare_test_data()
            
            # 运行不同并发级别的测试
            concurrency_levels = [10, 50, 100, 200]
            
            for concurrency in concurrency_levels:
                scenario_result = self._run_scenario(concurrency, 1000)
                results["test_scenarios"].append(scenario_result)
            
            # 生成压力测试摘要
            summary = self._generate_stress_summary(results["test_scenarios"])
            results["summary"] = summary
            
            # 清理测试数据
            self._cleanup_test_data()
            
        except Exception as e:
            logger.error(f"压力测试失败 {config.name}: {str(e)}")
            results["error"] = str(e)
            try:
                self._cleanup_test_data()
            except:
                pass
        
        return results
    
    def _prepare_test_data(self):
        """准备测试数据"""
        try:
            # 创建压力测试表
            self.cursor.execute("""
                CREATE TABLE IF NOT EXISTS stress_test_data (
                    id SERIAL PRIMARY KEY,
                    user_id INTEGER,
                    account_number VARCHAR(50),
                    balance DECIMAL(15,2),
                    last_transaction TIMESTAMP,
                    status VARCHAR(20),
                    metadata JSONB,
                    created_at TIMESTAMP DEFAULT NOW(),
                    updated_at TIMESTAMP DEFAULT NOW()
                )
            """)
            
            # 创建索引
            self.cursor.execute("CREATE INDEX IF NOT EXISTS idx_stress_user_id ON stress_test_data(user_id)")
            self.cursor.execute("CREATE INDEX IF NOT EXISTS idx_stress_account ON stress_test_data(account_number)")
            self.cursor.execute("CREATE INDEX IF NOT EXISTS idx_stress_status ON stress_test_data(status)")
            
            # 插入测试数据
            self.cursor.execute("DELETE FROM stress_test_data")
            batch_size = 1000
            total_records = 10000
            
            for batch_start in range(0, total_records, batch_size):
                batch_end = min(batch_start + batch_size, total_records)
                values = []
                for i in range(batch_start, batch_end):
                    values.append((
                        i % 1000,  # user_id
                        f"ACC{str(i).zfill(8)}",  # account_number
                        1000.0 + (i % 100),  # balance
                        datetime.now(),  # last_transaction
                        "ACTIVE" if i % 10 != 0 else "INACTIVE",  # status
                        json.dumps({"test": True, "iteration": i})  # metadata
                    ))
                
                # 批量插入
                args = ','.join(self.cursor.mogrify("(%s,%s,%s,%s,%s,%s)", v).decode('utf-8') for v in values)
                self.cursor.execute(f"INSERT INTO stress_test_data (user_id, account_number, balance, last_transaction, status, metadata) VALUES {args}")
            
            self.connection.commit()
            logger.info(f"准备测试数据完成: {total_records} 条记录")
            
        except Exception as e:
            logger.error(f"准备测试数据失败: {str(e)}")
            raise
    
    def _run_scenario(self, concurrency: int, requests_per_user: int) -> Dict[str, Any]:
        """运行单个测试场景"""
        import threading
        import queue
        
        scenario = {
            "concurrency": concurrency,
            "requests_per_user": requests_per_user,
            "start_time": datetime.now().isoformat(),
            "results": []
        }
        
        try:
            # 创建线程池和结果队列
            result_queue = queue.Queue()
            threads = []
            
            # 定义工作线程
            def worker(worker_id: int):
                worker_results = {
                    "worker_id": worker_id,
                    "successful_requests": 0,
                    "failed_requests": 0,
                    "response_times": []
                }
                
                try:
                    # 为每个工作线程创建独立的数据库连接
                    conn = psycopg2.connect(
                        host=self.connection.info.host,
                        port=self.connection.info.port,
                        database=self.connection.info.dbname,
                        user=self.connection.info.user,
                        password="",  # 实际中应该从配置获取
                        connect_timeout=5
                    )
                    cursor = conn.cursor()
                    
                    for i in range(requests_per_user):
                        try:
                            # 随机选择测试类型
                            test_type = i % 4
                            start_time = time.time()
                            
                            if test_type == 0:
                                # 简单查询
                                cursor.execute("SELECT COUNT(*) FROM stress_test_data")
                                cursor.fetchone()
                            elif test_type == 1:
                                # 条件查询
                                user_id = worker_id % 1000
                                cursor.execute(
                                    "SELECT * FROM stress_test_data WHERE user_id = %s LIMIT 10",
                                    (user_id,)
                                )
                                cursor.fetchall()
                            elif test_type == 2:
                                # 更新操作
                                account_num = f"ACC{str(worker_id).zfill(8)}"
                                cursor.execute(
                                    "UPDATE stress_test_data SET balance = balance + 1.0 WHERE account_number = %s",
                                    (account_num,)
                                )
                            elif test_type == 3:
                                # 事务操作
                                cursor.execute("BEGIN")
                                cursor.execute(
                                    "INSERT INTO stress_test_data (user_id, account_number, balance, status) VALUES (%s, %s, %s, %s)",
                                    (worker_id, f"NEW_ACC{worker_id}", 100.0, "ACTIVE")
                                )
                                cursor.execute("COMMIT")
                            
                            response_time = (time.time() - start_time) * 1000
                            worker_results["response_times"].append(response_time)
                            worker_results["successful_requests"] += 1
                            
                        except Exception as e:
                            worker_results["failed_requests"] += 1
                            logger.debug(f"工作线程 {worker_id} 请求失败: {str(e)}")
                    
                    cursor.close()
                    conn.close()
                    
                except Exception as e:
                    worker_results["error"] = str(e)
                    logger.error(f"工作线程 {worker_id} 执行失败: {str(e)}")
                
                result_queue.put(worker_results)
            
            # 启动工作线程
            for i in range(concurrency):
                thread = threading.Thread(target=worker, args=(i,))
                thread.start()
                threads.append(thread)
            
            # 等待所有线程完成
            for thread in threads:
                thread.join()
            
            # 收集结果
            all_results = []
            while not result_queue.empty():
                all_results.append(result_queue.get())
            
            scenario["results"] = all_results
            scenario["end_time"] = datetime.now().isoformat()
            
            # 计算场景统计
            total_requests = 0
            successful_requests = 0
            failed_requests = 0
            all_response_times = []
            
            for result in all_results:
                total_requests += result["successful_requests"] + result["failed_requests"]
                successful_requests += result["successful_requests"]
                failed_requests += result["failed_requests"]
                all_response_times.extend(result.get("response_times", []))
            
            scenario["statistics"] = {
                "total_requests": total_requests,
                "successful_requests": successful_requests,
                "failed_requests": failed_requests,
                "success_rate": successful_requests / total_requests if total_requests > 0 else 0,
                "avg_response_time_ms": sum(all_response_times) / len(all_response_times) if all_response_times else 0,
                "min_response_time_ms": min(all_response_times) if all_response_times else 0,
                "max_response_time_ms": max(all_response_times) if all_response_times else 0,
                "p95_response_time_ms": sorted(all_response_times)[int(len(all_response_times) * 0.95)] if all_response_times else 0,
                "p99_response_time_ms": sorted(all_response_times)[int(len(all_response_times) * 0.99)] if all_response_times else 0,
                "throughput_rps": successful_requests / ((datetime.fromisoformat(scenario["end_time"]) - datetime.fromisoformat(scenario["start_time"])).total_seconds()) if successful_requests > 0 else 0
            }
            
        except Exception as e:
            scenario["error"] = str(e)
            logger.error(f"测试场景执行失败 (并发={concurrency}): {str(e)}")
        
        return scenario
    
    def _generate_stress_summary(self, scenarios: List[Dict[str, Any]]) -> Dict[str, Any]:
        """生成压力测试摘要"""
        summary = {
            "max_concurrency_tested": 0,
            "max_throughput_rps": 0,
            "max_response_time_ms": 0,
            "bottleneck_concurrency": 0,
            "recommendations": []
        }
        
        try:
            # 分析所有场景
            for scenario in scenarios:
                if "statistics" in scenario:
                    stats = scenario["statistics"]
                    concurrency = scenario["concurrency"]
                    
                    # 更新最大值
                    summary["max_concurrency_tested"] = max(summary["max_concurrency_tested"], concurrency)
                    summary["max_throughput_rps"] = max(summary["max_throughput_rps"], stats.get("throughput_rps", 0))
                    summary["max_response_time_ms"] = max(summary["max_response_time_ms"], stats.get("max_response_time_ms", 0))
                    
                    # 识别瓶颈
                    if stats.get("success_rate", 1) < 0.95:
                        summary["bottleneck_concurrency"] = concurrency
                        summary["recommendations"].append(
                            f"在 {concurrency} 并发下出现性能瓶颈 (成功率: {stats['success_rate']:.1%})"
                        )
            
            # 生成总体建议
            if summary["bottleneck_concurrency"] > 0:
                summary["overall_assessment"] = "存在性能瓶颈"
                summary["recommendations"].append(
                    f"建议最大并发数限制在 {summary['bottleneck_concurrency'] // 2} 以内"
                )
                summary["recommendations"].append("考虑优化数据库配置和查询性能")
            else:
                summary["overall_assessment"] = "性能良好"
                summary["recommendations"].append("当前配置可以支持高并发场景")
                summary["recommendations"].append("建议持续监控性能指标")
            
            # 添加通用建议
            summary["recommendations"].append("定期进行压力测试")
            summary["recommendations"].append("监控慢查询和连接池使用情况")
            summary["recommendations"].append("考虑读写分离和分库分表策略")
            
        except Exception as e:
            logger.warning(f"压力测试摘要生成失败: {str(e)}")
            summary["error"] = str(e)
        
        return summary
    
    def _cleanup_test_data(self):
        """清理测试数据"""
        try:
            self.cursor.execute("DROP TABLE IF EXISTS stress_test_data")
            self.connection.commit()
            logger.info("测试数据清理完成")
        except Exception as e:
            logger.warning(f"清理测试数据失败: {str(e)}")