#!/usr/bin/env python3
"""
测试环境智能配置推荐系统原型
基于协同过滤和内容推荐的混合推荐算法
"""

import numpy as np
import pandas as pd
from typing import Dict, List, Tuple, Optional
from dataclasses import dataclass
from enum import Enum
import json
import random
from datetime import datetime
from collections import defaultdict

class TestType(Enum):
    """测试类型枚举"""
    UNIT = "unit_test"
    INTEGRATION = "integration_test"
    PERFORMANCE = "performance_test"
    E2E = "e2e_test"

class ResourceLevel(Enum):
    """资源级别枚举"""
    LOW = "low"
    MEDIUM = "medium"
    HIGH = "high"
    EXTREME = "extreme"

@dataclass
class TestEnvironmentConfig:
    """测试环境配置数据类"""
    config_id: str
    test_type: TestType
    resource_level: ResourceLevel
    cpu_cores: int
    memory_gb: int
    storage_gb: int
    network_bandwidth: str
    database_type: str
    cache_enabled: bool
    load_balancer: bool
    success_rate: float
    execution_time_minutes: float
    cost_score: float
    
    def to_feature_vector(self) -> List[float]:
        """将配置转换为特征向量"""
        return [
            list(TestType).index(self.test_type),
            list(ResourceLevel).index(self.resource_level),
            self.cpu_cores / 32.0,  # 归一化到0-1
            self.memory_gb / 128.0,
            self.storage_gb / 1000.0,
            float(self.cache_enabled),
            float(self.load_balancer),
            self.success_rate,
            self.execution_time_minutes / 120.0,
            self.cost_score
        ]

class CollaborativeFilteringRecommender:
    """协同过滤推荐器"""
    
    def __init__(self):
        self.config_similarity_matrix = None
        self.config_ratings = None
        self.config_ids = None
        
    def fit(self, configs: List[TestEnvironmentConfig], ratings: Dict[str, float]):
        """训练协同过滤模型"""
        self.config_ids = [c.config_id for c in configs]
        
        # 构建配置特征矩阵
        feature_matrix = np.array([c.to_feature_vector() for c in configs])
        
        # 计算配置之间的余弦相似度
        from sklearn.metrics.pairwise import cosine_similarity
        self.config_similarity_matrix = cosine_similarity(feature_matrix)
        
        # 存储评分数据
        self.config_ratings = ratings
        
    def recommend(self, target_config_id: str, top_k: int = 3) -> List[Tuple[str, float]]:
        """为目标配置推荐相似配置"""
        if target_config_id not in self.config_ids:
            return []
            
        target_idx = self.config_ids.index(target_config_id)
        
        # 获取相似度分数
        similarities = self.config_similarity_matrix[target_idx]
        
        # 排除自己
        similarities[target_idx] = -1
        
        # 获取最相似的配置
        similar_indices = np.argsort(similarities)[-top_k:][::-1]
        
        recommendations = []
        for idx in similar_indices:
            config_id = self.config_ids[idx]
            similarity_score = similarities[idx]
            
            # 如果有评分，加权计算推荐分数
            if config_id in self.config_ratings:
                rating = self.config_ratings[config_id]
                recommendation_score = similarity_score * (1 + rating) / 2
            else:
                recommendation_score = similarity_score
                
            recommendations.append((config_id, recommendation_score))
            
        return recommendations

class ContentBasedRecommender:
    """基于内容的推荐器"""
    
    def __init__(self):
        from sklearn.neighbors import NearestNeighbors
        self.knn_model = NearestNeighbors(n_neighbors=5, metric='cosine')
        self.configs = None
        self.config_ids = None
        
    def fit(self, configs: List[TestEnvironmentConfig]):
        """训练基于内容的推荐模型"""
        self.configs = configs
        self.config_ids = [c.config_id for c in configs]
        
        # 构建特征矩阵
        feature_matrix = np.array([c.to_feature_vector() for c in configs])
        
        # 训练KNN模型
        self.knn_model.fit(feature_matrix)
        
    def recommend(self, target_config: TestEnvironmentConfig, top_k: int = 3) -> List[Tuple[str, float]]:
        """基于内容推荐相似配置"""
        if self.configs is None:
            return []
            
        # 将目标配置转换为特征向量
        target_features = np.array([target_config.to_feature_vector()])
        
        # 查找最近邻
        distances, indices = self.knn_model.kneighbors(target_features, n_neighbors=top_k+1)
        
        recommendations = []
        for i, idx in enumerate(indices[0]):
            # 跳过自己（如果是训练集中的配置）
            if i == 0 and target_config.config_id in self.config_ids:
                continue
                
            config_id = self.config_ids[idx]
            distance = distances[0][i]
            
            # 将距离转换为相似度分数
            similarity_score = 1 - distance
            
            recommendations.append((config_id, similarity_score))
            
        return recommendations[:top_k]

class HybridRecommender:
    """混合推荐器"""
    
    def __init__(self, cf_weight: float = 0.4, cb_weight: float = 0.6):
        self.cf_recommender = CollaborativeFilteringRecommender()
        self.cb_recommender = ContentBasedRecommender()
        self.cf_weight = cf_weight
        self.cb_weight = cb_weight
        
    def fit(self, configs: List[TestEnvironmentConfig], ratings: Dict[str, float]):
        """训练混合推荐模型"""
        self.cf_recommender.fit(configs, ratings)
        self.cb_recommender.fit(configs)
        
    def recommend(self, target_config: TestEnvironmentConfig, target_config_id: str, top_k: int = 3) -> List[Tuple[str, float, Dict]]:
        """混合推荐"""
        # 获取协同过滤推荐
        cf_recommendations = self.cf_recommender.recommend(target_config_id, top_k*2)
        
        # 获取基于内容的推荐
        cb_recommendations = self.cb_recommender.recommend(target_config, top_k*2)
        
        # 合并推荐结果
        all_recommendations = {}
        
        # 处理协同过滤推荐
        for config_id, cf_score in cf_recommendations:
            if config_id not in all_recommendations:
                all_recommendations[config_id] = {'cf_score': 0, 'cb_score': 0}
            all_recommendations[config_id]['cf_score'] = cf_score
            
        # 处理基于内容的推荐
        for config_id, cb_score in cb_recommendations:
            if config_id not in all_recommendations:
                all_recommendations[config_id] = {'cf_score': 0, 'cb_score': 0}
            all_recommendations[config_id]['cb_score'] = cb_score
            
        # 计算混合分数
        final_recommendations = []
        for config_id, scores in all_recommendations.items():
            hybrid_score = (scores['cf_score'] * self.cf_weight + 
                          scores['cb_score'] * self.cb_weight)
            final_recommendations.append((config_id, hybrid_score, scores))
            
        # 按混合分数排序
        final_recommendations.sort(key=lambda x: x[1], reverse=True)
        
        return final_recommendations[:top_k]

class OptimizationAlgorithm:
    """配置优化算法"""
    
    def __init__(self):
        pass
        
    def optimize_resources(self, config: TestEnvironmentConfig, 
                          target_performance: float,
                          max_cost: float) -> TestEnvironmentConfig:
        """优化资源配置"""
        optimized_config = TestEnvironmentConfig(
            config_id=f"{config.config_id}_optimized",
            test_type=config.test_type,
            resource_level=self._determine_resource_level(config),
            cpu_cores=self._optimize_cpu(config.cpu_cores, target_performance),
            memory_gb=self._optimize_memory(config.memory_gb, target_performance),
            storage_gb=self._optimize_storage(config.storage_gb, target_performance),
            network_bandwidth=config.network_bandwidth,
            database_type=config.database_type,
            cache_enabled=self._optimize_cache(config.cache_enabled, target_performance),
            load_balancer=self._optimize_load_balancer(config.load_balancer, target_performance),
            success_rate=min(config.success_rate * 1.1, 1.0),  # 提升10%，最大为1.0
            execution_time_minutes=max(config.execution_time_minutes * 0.9, 1.0),  # 减少10%，最小为1分钟
            cost_score=min(config.cost_score * 0.8, max_cost)  # 减少20%，不超过最大成本
        )
        
        return optimized_config
        
    def _determine_resource_level(self, config: TestEnvironmentConfig) -> ResourceLevel:
        """确定资源级别"""
        total_score = (config.cpu_cores / 32.0 + 
                      config.memory_gb / 128.0 + 
                      config.storage_gb / 1000.0) / 3.0
                      
        if total_score < 0.25:
            return ResourceLevel.LOW
        elif total_score < 0.5:
            return ResourceLevel.MEDIUM
        elif total_score < 0.75:
            return ResourceLevel.HIGH
        else:
            return ResourceLevel.EXTREME
            
    def _optimize_cpu(self, current_cpu: int, target_performance: float) -> int:
        """优化CPU配置"""
        # 根据目标性能调整CPU核心数
        optimized_cpu = int(current_cpu * target_performance)
        
        # 确保在合理范围内
        optimized_cpu = max(1, min(optimized_cpu, 32))
        
        # 调整为2的幂次或常见配置
        common_configs = [1, 2, 4, 8, 16, 32]
        return min(common_configs, key=lambda x: abs(x - optimized_cpu))
        
    def _optimize_memory(self, current_memory: int, target_performance: float) -> int:
        """优化内存配置"""
        optimized_memory = int(current_memory * target_performance)
        
        # 确保在合理范围内
        optimized_memory = max(1, min(optimized_memory, 128))
        
        # 调整为常见配置
        common_configs = [1, 2, 4, 8, 16, 32, 64, 128]
        return min(common_configs, key=lambda x: abs(x - optimized_memory))
        
    def _optimize_storage(self, current_storage: int, target_performance: float) -> int:
        """优化存储配置"""
        optimized_storage = int(current_storage * target_performance)
        
        # 确保在合理范围内
        optimized_storage = max(10, min(optimized_storage, 1000))
        
        # 调整为50GB的倍数
        return (optimized_storage // 50) * 50
        
    def _optimize_cache(self, current_cache: bool, target_performance: float) -> bool:
        """优化缓存配置"""
        # 如果目标性能高，启用缓存
        return target_performance > 0.7 or current_cache
        
    def _optimize_load_balancer(self, current_lb: bool, target_performance: float) -> bool:
        """优化负载均衡器配置"""
        # 如果目标性能高且需要高可用性，启用负载均衡
        return target_performance > 0.8 or current_lb

def generate_sample_configs(num_configs: int = 100) -> List[TestEnvironmentConfig]:
    """生成样本配置数据"""
    configs = []
    
    test_types = list(TestType)
    resource_levels = list(ResourceLevel)
    
    for i in range(num_configs):
        test_type = random.choice(test_types)
        resource_level = random.choice(resource_levels)
        
        # 根据测试类型和资源级别生成配置
        if resource_level == ResourceLevel.LOW:
            cpu_cores = random.randint(1, 4)
            memory_gb = random.randint(1, 8)
            storage_gb = random.randint(10, 100)
        elif resource_level == ResourceLevel.MEDIUM:
            cpu_cores = random.randint(4, 8)
            memory_gb = random.randint(8, 32)
            storage_gb = random.randint(100, 500)
        elif resource_level == ResourceLevel.HIGH:
            cpu_cores = random.randint(8, 16)
            memory_gb = random.randint(32, 64)
            storage_gb = random.randint(500, 800)
        else:  # EXTREME
            cpu_cores = random.randint(16, 32)
            memory_gb = random.randint(64, 128)
            storage_gb = random.randint(800, 1000)
            
        config = TestEnvironmentConfig(
            config_id=f"config_{i:03d}",
            test_type=test_type,
            resource_level=resource_level,
            cpu_cores=cpu_cores,
            memory_gb=memory_gb,
            storage_gb=storage_gb,
            network_bandwidth=random.choice(["100Mbps", "1Gbps", "10Gbps"]),
            database_type=random.choice(["MySQL", "PostgreSQL", "MongoDB", "Redis"]),
            cache_enabled=random.random() > 0.3,
            load_balancer=random.random() > 0.7,
            success_rate=random.uniform(0.7, 1.0),
            execution_time_minutes=random.uniform(1.0, 120.0),
            cost_score=random.uniform(0.1, 1.0)
        )
        
        configs.append(config)
        
    return configs

def generate_sample_ratings(configs: List[TestEnvironmentConfig]) -> Dict[str, float]:
    """生成样本评分数据"""
    ratings = {}
    
    for config in configs:
        # 基于配置质量生成评分
        base_score = config.success_rate * 0.4 + (1 - config.cost_score) * 0.3 + (1 - config.execution_time_minutes / 120.0) * 0.3
        
        # 添加一些随机噪声
        noise = random.uniform(-0.1, 0.1)
        final_score = max(0.1, min(1.0, base_score + noise))
        
        ratings[config.config_id] = final_score
        
    return ratings

def main():
    """主函数：演示智能配置推荐系统"""
    print("=" * 60)
    print("测试环境智能配置推荐系统原型")
    print("=" * 60)
    
    # 1. 生成样本数据
    print("\n1. 生成样本配置数据...")
    configs = generate_sample_configs(50)
    ratings = generate_sample_ratings(configs)
    
    print(f"   生成 {len(configs)} 个配置样本")
    print(f"   生成 {len(ratings)} 个评分样本")
    
    # 2. 创建目标配置（用于推荐）
    print("\n2. 创建目标配置...")
    target_config = TestEnvironmentConfig(
        config_id="target_config_001",
        test_type=TestType.PERFORMANCE,
        resource_level=ResourceLevel.HIGH,
        cpu_cores=12,
        memory_gb=48,
        storage_gb=600,
        network_bandwidth="10Gbps",
        database_type="MySQL",
        cache_enabled=True,
        load_balancer=True,
        success_rate=0.85,
        execution_time_minutes=45.0,
        cost_score=0.7
    )
    
    print(f"   目标配置: {target_config.test_type.value}, {target_config.resource_level.value}")
    print(f"   资源: {target_config.cpu_cores} cores, {target_config.memory_gb}GB RAM, {target_config.storage_gb}GB storage")
    
    # 3. 训练混合推荐模型
    print("\n3. 训练混合推荐模型...")
    hybrid_recommender = HybridRecommender(cf_weight=0.4, cb_weight=0.6)
    hybrid_recommender.fit(configs, ratings)
    
    # 4. 获取推荐
    print("\n4. 获取配置推荐...")
    recommendations = hybrid_recommender.recommend(
        target_config=target_config,
        target_config_id="config_025",  # 使用一个已有的配置ID
        top_k=5
    )
    
    print(f"   为 {target_config.config_id} 推荐以下配置:")
    for i, (config_id, hybrid_score, scores) in enumerate(recommendations, 1):
        print(f"   {i}. {config_id}:")
        print(f"      混合分数: {hybrid_score:.4f}")
        print(f"      协同过滤分数: {scores['cf_score']:.4f}")
        print(f"      内容推荐分数: {scores['cb_score']:.4f}")
        
    # 5. 配置优化
    print("\n5. 配置优化演示...")
    optimizer = OptimizationAlgorithm()
    
    # 选择一个配置进行优化
    sample_config = configs[10]
    print(f"   原始配置: {sample_config.config_id}")
    print(f"   CPU: {sample_config.cpu_cores} cores, 内存: {sample_config.memory_gb}GB")
    print(f"   成功率: {sample_config.success_rate:.2%}, 执行时间: {sample_config.execution_time_minutes:.1f}分钟")
    print(f"   成本分数: {sample_config.cost_score:.2f}")
    
    # 优化配置（目标性能提升20%，最大成本0.8）
    optimized_config = optimizer.optimize_resources(
        sample_config,
        target_performance=1.2,
        max_cost=0.8
    )
    
    print(f"\n   优化后配置: {optimized_config.config_id}")
    print(f"   CPU: {optimized_config.cpu_cores} cores, 内存: {optimized_config.memory_gb}GB")
    print(f"   成功率: {optimized_config.success_rate:.2%} (+{optimized_config.success_rate - sample_config.success_rate:+.2%})")
    print(f"   执行时间: {optimized_config.execution_time_minutes:.1f}分钟 (-{sample_config.execution_time_minutes - optimized_config.execution_time_minutes:.1f}分钟)")
    print(f"   成本分数: {optimized_config.cost_score:.2f} (-{sample_config.cost_score - optimized_config.cost_score:.2f})")
    
    # 6. 性能评估
    print("\n6. 性能评估...")
    
    # 计算推荐准确性（模拟）
    print("   推荐系统性能指标:")
    print("   - 平均推荐分数: {:.4f}".format(np.mean([score for _, score, _ in recommendations])))
    print("   - 推荐多样性: {:.4f}".format(len(set([cid for cid, _, _ in recommendations])) / len(recommendations)))
    
    # 计算优化效果
    print("\n   优化算法效果:")
    performance_improvement = (sample_config.success_rate - optimized_config.success_rate) / sample_config.success_rate * 100
    cost_reduction = (sample_config.cost_score - optimized_config.cost_score) / sample_config.cost_score * 100
    time_reduction = (sample_config.execution_time_minutes - optimized_config.execution_time_minutes) / sample_config.execution_time_minutes * 100
    
    print(f"   - 成功率提升: {performance_improvement:+.1f}%")
    print(f"   - 成本降低: {cost_reduction:+.1f}%")
    print(f"   - 时间减少: {time_reduction:+.1f}%")
    
    print("\n" + "=" * 60)
    print("原型演示完成!")
    print("=" * 60)

if __name__ == "__main__":
    main()