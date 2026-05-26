"""
资源成本计算器
计算基础设施、应用和容器的资源成本
"""

import logging
from typing import Dict, List, Optional, Any, Union, Tuple
from datetime import datetime, timedelta
from dataclasses import dataclass
from enum import Enum
import pandas as pd
import numpy as np
from decimal import Decimal, ROUND_HALF_UP

logger = logging.getLogger(__name__)


class ResourceType(Enum):
    """资源类型枚举"""
    CPU = "cpu"
    MEMORY = "memory"
    STORAGE = "storage"
    NETWORK = "network"
    INSTANCE = "instance"  # 虚拟机/容器实例


class PricingModel(Enum):
    """定价模型枚举"""
    ON_DEMAND = "on_demand"  # 按需计费
    RESERVED = "reserved"    # 预留实例
    SPOT = "spot"           # 竞价实例
    SAVINGS_PLAN = "savings_plan"  # 节省计划


@dataclass
class ResourceSpec:
    """资源规格定义"""
    resource_type: ResourceType
    quantity: float
    unit: str
    region: str = "us-east-1"
    instance_type: Optional[str] = None
    storage_type: Optional[str] = None  # hdd, ssd, nvme等
    network_tier: Optional[str] = None  # standard, premium等


@dataclass
class CostItem:
    """成本项"""
    resource_type: ResourceType
    resource_spec: ResourceSpec
    usage_hours: float
    unit_price: Decimal
    total_cost: Decimal
    pricing_model: PricingModel
    period_start: datetime
    period_end: datetime
    tags: Dict[str, str] = None


@dataclass
class CostSummary:
    """成本汇总"""
    total_cost: Decimal
    cost_by_resource_type: Dict[ResourceType, Decimal]
    cost_by_service: Dict[str, Decimal]
    cost_by_region: Dict[str, Decimal]
    cost_by_pricing_model: Dict[PricingModel, Decimal]
    period_start: datetime
    period_end: datetime
    currency: str = "USD"


class ResourceCostCalculator:
    """资源成本计算器"""
    
    def __init__(
        self,
        pricing_config: Optional[Dict[str, Any]] = None,
        currency: str = "USD",
        default_region: str = "us-east-1"
    ):
        """
        初始化资源成本计算器
        
        Args:
            pricing_config: 定价配置
            currency: 货币单位
            default_region: 默认区域
        """
        self.currency = currency
        self.default_region = default_region
        self.pricing_config = pricing_config or self._get_default_pricing()
        
    def _get_default_pricing(self) -> Dict[str, Any]:
        """获取默认定价配置"""
        return {
            # AWS定价 (美元/小时)
            "aws": {
                "ec2": {
                    "t3.micro": Decimal("0.0104"),
                    "t3.small": Decimal("0.0208"),
                    "t3.medium": Decimal("0.0416"),
                    "t3.large": Decimal("0.0832"),
                    "t3.xlarge": Decimal("0.1664"),
                    "t3.2xlarge": Decimal("0.3328"),
                    "m5.large": Decimal("0.096"),
                    "m5.xlarge": Decimal("0.192"),
                    "m5.2xlarge": Decimal("0.384"),
                    "c5.large": Decimal("0.085"),
                    "c5.xlarge": Decimal("0.17"),
                    "c5.2xlarge": Decimal("0.34"),
                },
                "rds": {
                    "db.t3.micro": Decimal("0.017"),
                    "db.t3.small": Decimal("0.034"),
                    "db.t3.medium": Decimal("0.068"),
                    "db.m5.large": Decimal("0.171"),
                    "db.m5.xlarge": Decimal("0.342"),
                },
                "elasticache": {
                    "cache.t3.micro": Decimal("0.018"),
                    "cache.t3.small": Decimal("0.036"),
                    "cache.t3.medium": Decimal("0.072"),
                    "cache.m5.large": Decimal("0.175"),
                },
                "ebs": {
                    "gp2": Decimal("0.10"),  # 美元/GB/月
                    "gp3": Decimal("0.08"),
                    "io1": Decimal("0.125"),
                    "st1": Decimal("0.045"),
                    "sc1": Decimal("0.025"),
                },
                "s3": {
                    "standard": Decimal("0.023"),  # 美元/GB/月
                    "intelligent_tiering": Decimal("0.023"),
                    "standard_ia": Decimal("0.0125"),
                    "one_zone_ia": Decimal("0.01"),
                    "glacier": Decimal("0.004"),
                    "deep_archive": Decimal("0.00099"),
                },
                "data_transfer": {
                    "inbound": Decimal("0.00"),  # 免费入站
                    "outbound": {
                        "first_10_tb": Decimal("0.09"),  # 美元/GB
                        "next_40_tb": Decimal("0.085"),
                        "next_100_tb": Decimal("0.07"),
                        "over_150_tb": Decimal("0.05"),
                    }
                }
            },
            
            # Azure定价 (美元/小时)
            "azure": {
                "vm": {
                    "B1s": Decimal("0.0125"),
                    "B1ms": Decimal("0.025"),
                    "B2s": Decimal("0.05"),
                    "B2ms": Decimal("0.10"),
                    "D2s_v3": Decimal("0.096"),
                    "D4s_v3": Decimal("0.192"),
                    "D8s_v3": Decimal("0.384"),
                },
                "sql": {
                    "basic": Decimal("0.015"),
                    "standard": Decimal("0.041"),
                    "premium": Decimal("0.465"),
                },
                "storage": {
                    "standard_hdd": Decimal("0.04"),  # 美元/GB/月
                    "standard_ssd": Decimal("0.08"),
                    "premium_ssd": Decimal("0.15"),
                }
            },
            
            # GCP定价 (美元/小时)
            "gcp": {
                "compute": {
                    "e2-micro": Decimal("0.0085"),
                    "e2-small": Decimal("0.017"),
                    "e2-medium": Decimal("0.034"),
                    "n2-standard-2": Decimal("0.097"),
                    "n2-standard-4": Decimal("0.194"),
                    "n2-standard-8": Decimal("0.388"),
                },
                "cloud_sql": {
                    "db-f1-micro": Decimal("0.018"),
                    "db-g1-small": Decimal("0.05"),
                    "db-n1-standard-1": Decimal("0.096"),
                },
                "storage": {
                    "standard": Decimal("0.026"),  # 美元/GB/月
                    "nearline": Decimal("0.01"),
                    "coldline": Decimal("0.004"),
                    "archive": Decimal("0.0012"),
                }
            },
            
            # 内部数据中心定价 (美元/月)
            "internal": {
                "server": {
                    "physical": Decimal("1000"),  # 物理服务器
                    "virtual": Decimal("200"),    # 虚拟机
                    "container": Decimal("50"),   # 容器实例
                },
                "storage": {
                    "hdd": Decimal("0.03"),  # 美元/GB/月
                    "ssd": Decimal("0.10"),
                    "nvme": Decimal("0.15"),
                },
                "network": {
                    "bandwidth": Decimal("0.05"),  # 美元/GB
                    "public_ip": Decimal("3.00"),  # 美元/月
                }
            }
        }
    
    def calculate_instance_cost(
        self,
        instance_type: str,
        usage_hours: float,
        provider: str = "aws",
        region: str = None,
        pricing_model: PricingModel = PricingModel.ON_DEMAND,
        reserved_term: Optional[int] = None  # 预留期限（月）
    ) -> Decimal:
        """
        计算实例成本
        
        Args:
            instance_type: 实例类型
            usage_hours: 使用小时数
            provider: 云服务商 (aws, azure, gcp, internal)
            region: 区域
            pricing_model: 定价模型
            reserved_term: 预留期限
            
        Returns:
            总成本
        """
        region = region or self.default_region
        
        # 获取基础价格
        base_price = self._get_instance_price(instance_type, provider, region)
        if base_price is None:
            logger.warning(f"未找到实例类型 {instance_type} 的价格，使用默认价格")
            base_price = Decimal("0.10")  # 默认价格
        
        # 根据定价模型调整价格
        adjusted_price = self._adjust_price_by_model(
            base_price, pricing_model, reserved_term
        )
        
        # 计算总成本
        total_cost = adjusted_price * Decimal(str(usage_hours))
        
        return total_cost.quantize(Decimal("0.0001"), rounding=ROUND_HALF_UP)
    
    def _get_instance_price(
        self,
        instance_type: str,
        provider: str,
        region: str
    ) -> Optional[Decimal]:
        """获取实例价格"""
        provider_config = self.pricing_config.get(provider, {})
        
        # 尝试从EC2/VM/Compute配置中获取价格
        for service_key in ["ec2", "vm", "compute", "server"]:
            if service_key in provider_config:
                service_config = provider_config[service_key]
                if instance_type in service_config:
                    return Decimal(str(service_config[instance_type]))
        
        # 如果找不到，尝试模糊匹配
        for service_key in ["ec2", "vm", "compute", "server"]:
            if service_key in provider_config:
                service_config = provider_config[service_key]
                for key, price in service_config.items():
                    if instance_type.lower() in key.lower() or key.lower() in instance_type.lower():
                        return Decimal(str(price))
        
        return None
    
    def _adjust_price_by_model(
        self,
        base_price: Decimal,
        pricing_model: PricingModel,
        reserved_term: Optional[int] = None
    ) -> Decimal:
        """根据定价模型调整价格"""
        if pricing_model == PricingModel.ON_DEMAND:
            return base_price
        
        elif pricing_model == PricingModel.RESERVED:
            # 预留实例通常有30-60%的折扣
            if reserved_term == 1:  # 1年
                discount = Decimal("0.40")  # 40%折扣
            elif reserved_term == 3:  # 3年
                discount = Decimal("0.60")  # 60%折扣
            else:
                discount = Decimal("0.50")  # 默认50%折扣
            
            return base_price * (Decimal("1") - discount)
        
        elif pricing_model == PricingModel.SPOT:
            # 竞价实例通常有70-90%的折扣
            discount = Decimal("0.80")  # 80%折扣
            return base_price * (Decimal("1") - discount)
        
        elif pricing_model == PricingModel.SAVINGS_PLAN:
            # 节省计划通常有30-50%的折扣
            discount = Decimal("0.40")  # 40%折扣
            return base_price * (Decimal("1") - discount)
        
        else:
            return base_price
    
    def calculate_storage_cost(
        self,
        storage_gb: float,
        storage_type: str = "standard",
        provider: str = "aws",
        region: str = None,
        duration_days: float = 30  # 默认一个月
    ) -> Decimal:
        """
        计算存储成本
        
        Args:
            storage_gb: 存储容量 (GB)
            storage_type: 存储类型
            provider: 云服务商
            region: 区域
            duration_days: 存储天数
            
        Returns:
            总成本
        """
        region = region or self.default_region
        
        # 获取存储价格 (美元/GB/月)
        monthly_price = self._get_storage_price(storage_type, provider, region)
        if monthly_price is None:
            logger.warning(f"未找到存储类型 {storage_type} 的价格，使用默认价格")
            monthly_price = Decimal("0.10")  # 默认价格
        
        # 计算月成本
        monthly_cost = monthly_price * Decimal(str(storage_gb))
        
        # 根据天数调整成本
        daily_cost = monthly_cost / Decimal("30")
        total_cost = daily_cost * Decimal(str(duration_days))
        
        return total_cost.quantize(Decimal("0.0001"), rounding=ROUND_HALF_UP)
    
    def _get_storage_price(
        self,
        storage_type: str,
        provider: str,
        region: str
    ) -> Optional[Decimal]:
        """获取存储价格"""
        provider_config = self.pricing_config.get(provider, {})
        
        # 尝试从存储配置中获取价格
        for service_key in ["storage", "ebs", "s3"]:
            if service_key in provider_config:
                service_config = provider_config[service_key]
                if storage_type in service_config:
                    return Decimal(str(service_config[storage_type]))
        
        # 如果找不到，尝试模糊匹配
        for service_key in ["storage", "ebs", "s3"]:
            if service_key in provider_config:
                service_config = provider_config[service_key]
                for key, price in service_config.items():
                    if storage_type.lower() in key.lower() or key.lower() in storage_type.lower():
                        return Decimal(str(price))
        
        return None
    
    def calculate_network_cost(
        self,
        data_transfer_gb: float,
        direction: str = "outbound",  # inbound, outbound
        provider: str = "aws",
        region: str = None
    ) -> Decimal:
        """
        计算网络传输成本
        
        Args:
            data_transfer_gb: 数据传输量 (GB)
            direction: 传输方向
            provider: 云服务商
            region: 区域
            
        Returns:
            总成本
        """
        region = region or self.default_region
        
        # 获取网络价格
        unit_price = self._get_network_price(direction, provider, region, data_transfer_gb)
        
        # 计算总成本
        total_cost = unit_price * Decimal(str(data_transfer_gb))
        
        return total_cost.quantize(Decimal("0.0001"), rounding=ROUND_HALF_UP)
    
    def _get_network_price(
        self,
        direction: str,
        provider: str,
        region: str,
        data_transfer_gb: float
    ) -> Decimal:
        """获取网络价格"""
        provider_config = self.pricing_config.get(provider, {})
        
        # AWS数据传出定价 (分层定价)
        if provider == "aws" and direction == "outbound":
            data_transfer_config = provider_config.get("data_transfer", {})
            outbound_config = data_transfer_config.get("outbound", {})
            
            if isinstance(outbound_config, dict):
                # 分层定价
                if data_transfer_gb <= 10000:  # 前10TB
                    return Decimal(str(outbound_config.get("first_10_tb", "0.09")))
                elif data_transfer_gb <= 50000:  # 下一个40TB
                    return Decimal(str(outbound_config.get("next_40_tb", "0.085")))
                elif data_transfer_gb <= 150000:  # 下一个100TB
                    return Decimal(str(outbound_config.get("next_100_tb", "0.07")))
                else:  # 超过150TB
                    return Decimal(str(outbound_config.get("over_150_tb", "0.05")))
            else:
                return Decimal(str(outbound_config))
        
        # 其他情况
        data_transfer_config = provider_config.get("data_transfer", {})
        if direction in data_transfer_config:
            return Decimal(str(data_transfer_config[direction]))
        
        # 默认价格
        if direction == "inbound":
            return Decimal("0.00")  # 入站通常免费
        else:
            return Decimal("0.05")  # 出站默认0.05美元/GB
    
    def calculate_resource_cost(
        self,
        resource_spec: ResourceSpec,
        usage_hours: float,
        provider: str = "aws",
        pricing_model: PricingModel = PricingModel.ON_DEMAND
    ) -> CostItem:
        """
        计算资源成本
        
        Args:
            resource_spec: 资源规格
            usage_hours: 使用小时数
            provider: 云服务商
            pricing_model: 定价模型
            
        Returns:
            成本项
        """
        total_cost = Decimal("0")
        unit_price = Decimal("0")
        
        if resource_spec.resource_type == ResourceType.INSTANCE:
            if resource_spec.instance_type:
                total_cost = self.calculate_instance_cost(
                    instance_type=resource_spec.instance_type,
                    usage_hours=usage_hours,
                    provider=provider,
                    region=resource_spec.region,
                    pricing_model=pricing_model
                )
                unit_price = total_cost / Decimal(str(usage_hours)) if usage_hours > 0 else Decimal("0")
        
        elif resource_spec.resource_type == ResourceType.STORAGE:
            # 将小时转换为天数 (用于存储成本计算)
            storage_days = usage_hours / 24
            total_cost = self.calculate_storage_cost(
                storage_gb=resource_spec.quantity,
                storage_type=resource_spec.storage_type or "standard",
                provider=provider,
                region=resource_spec.region,
                duration_days=storage_days
            )
            unit_price = total_cost / Decimal(str(resource_spec.quantity)) if resource_spec.quantity > 0 else Decimal("0")
        
        elif resource_spec.resource_type == ResourceType.NETWORK:
            total_cost = self.calculate_network_cost(
                data_transfer_gb=resource_spec.quantity,
                direction="outbound",  # 默认计算出站成本
                provider=provider,
                region=resource_spec.region
            )
            unit_price = total_cost / Decimal(str(resource_spec.quantity)) if resource_spec.quantity > 0 else Decimal("0")
        
        # 计算时间段
        period_end = datetime.now()
        period_start = period_end - timedelta(hours=usage_hours)
        
        return CostItem(
            resource_type=resource_spec.resource_type,
            resource_spec=resource_spec,
            usage_hours=usage_hours,
            unit_price=unit_price,
            total_cost=total_cost,
            pricing_model=pricing_model,
            period_start=period_start,
            period_end=period_end
        )
    
    def calculate_costs_from_metrics(
        self,
        metrics_data: Dict[str, pd.DataFrame],
        period_hours: float = 24
    ) -> List[CostItem]:
        """
        从监控指标计算成本
        
        Args:
            metrics_data: 监控指标数据
            period_hours: 时间段 (小时)
            
        Returns:
            成本项列表
        """
        cost_items = []
        
        # 从CPU使用率计算实例成本
        if "node_cpu_usage" in metrics_data:
            cpu_df = metrics_data["node_cpu_usage"]
            if not cpu_df.empty:
                # 计算平均CPU使用率
                avg_cpu_usage = cpu_df["value"].mean()
                
                # 假设使用t3.medium实例
                instance_type = "t3.medium"
                instance_cost = self.calculate_instance_cost(
                    instance_type=instance_type,
                    usage_hours=period_hours,
                    provider="aws"
                )
                
                # 根据实际使用率调整成本
                adjusted_cost = instance_cost * Decimal(str(avg_cpu_usage / 100))
                
                resource_spec = ResourceSpec(
                    resource_type=ResourceType.INSTANCE,
                    quantity=1,
                    unit="instance",
                    instance_type=instance_type
                )
                
                cost_items.append(CostItem(
                    resource_type=ResourceType.INSTANCE,
                    resource_spec=resource_spec,
                    usage_hours=period_hours,
                    unit_price=instance_cost / Decimal(str(period_hours)),
                    total_cost=adjusted_cost,
                    pricing_model=PricingModel.ON_DEMAND,
                    period_start=datetime.now() - timedelta(hours=period_hours),
                    period_end=datetime.now(),
                    tags={"metric": "node_cpu_usage", "adjustment": "usage_based"}
                ))
        
        # 从内存使用计算成本
        if "node_memory_usage" in metrics_data:
            memory_df = metrics_data["node_memory_usage"]
            if not memory_df.empty:
                # 计算平均内存使用率
                avg_memory_usage = memory_df["value"].mean()
                
                # 假设16GB内存的实例
                memory_gb = 16
                memory_cost = self.calculate_storage_cost(
                    storage_gb=memory_gb,
                    storage_type="ssd",  # 内存成本类似SSD存储
                    provider="aws",
                    duration_days=period_hours / 24
                )
                
                # 根据实际使用率调整成本
                adjusted_cost = memory_cost * Decimal(str(avg_memory_usage / 100))
                
                resource_spec = ResourceSpec(
                    resource_type=ResourceType.MEMORY,
                    quantity=memory_gb,
                    unit="GB",
                    storage_type="memory"
                )
                
                cost_items.append(CostItem(
                    resource_type=ResourceType.MEMORY,
                    resource_spec=resource_spec,
                    usage_hours=period_hours,
                    unit_price=memory_cost / Decimal(str(memory_gb * period_hours / 24)),
                    total_cost=adjusted_cost,
                    pricing_model=PricingModel.ON_DEMAND,
                    period_start=datetime.now() - timedelta(hours=period_hours),
                    period_end=datetime.now(),
                    tags={"metric": "node_memory_usage", "adjustment": "usage_based"}
                ))
        
        # 从磁盘使用计算成本
        if "node_disk_usage" in metrics_data:
            disk_df = metrics_data["node_disk_usage"]
            if not disk_df.empty:
                # 计算平均磁盘使用率
                avg_disk_usage = disk_df["value"].mean()
                
                # 假设100GB磁盘
                disk_gb = 100
                disk_cost = self.calculate_storage_cost(
                    storage_gb=disk_gb,
                    storage_type="gp2",
                    provider="aws",
                    duration_days=period_hours / 24
                )
                
                # 根据实际使用率调整成本
                adjusted_cost = disk_cost * Decimal(str(avg_disk_usage / 100))
                
                resource_spec = ResourceSpec(
                    resource_type=ResourceType.STORAGE,
                    quantity=disk_gb,
                    unit="GB",
                    storage_type="gp2"
                )
                
                cost_items.append(CostItem(
                    resource_type=ResourceType.STORAGE,
                    resource_spec=resource_spec,
                    usage_hours=period_hours,
                    unit_price=disk_cost / Decimal(str(disk_gb * period_hours / 24)),
                    total_cost=adjusted_cost,
                    pricing_model=PricingModel.ON_DEMAND,
                    period_start=datetime.now() - timedelta(hours=period_hours),
                    period_end=datetime.now(),
                    tags={"metric": "node_disk_usage", "adjustment": "usage_based"}
                ))
        
        # 从网络使用计算成本
        if "node_network_transmit" in metrics_data:
            network_df = metrics_data["node_network_transmit"]
            if not network_df.empty:
                # 计算总网络传输量 (bytes -> GB)
                total_bytes = network_df["value"].sum() * 300  # 假设5分钟间隔
                total_gb = total_bytes / (1024 ** 3)
                
                network_cost = self.calculate_network_cost(
                    data_transfer_gb=total_gb,
                    direction="outbound",
                    provider="aws"
                )
                
                resource_spec = ResourceSpec(
                    resource_type=ResourceType.NETWORK,
                    quantity=total_gb,
                    unit="GB"
                )
                
                cost_items.append(CostItem(
                    resource_type=ResourceType.NETWORK,
                    resource_spec=resource_spec,
                    usage_hours=period_hours,
                    unit_price=network_cost / Decimal(str(total_gb)) if total_gb > 0 else Decimal("0"),
                    total_cost=network_cost,
                    pricing_model=PricingModel.ON_DEMAND,
                    period_start=datetime.now() - timedelta(hours=period_hours),
                    period_end=datetime.now(),
                    tags={"metric": "node_network_transmit"}
                ))
        
        return cost_items
    
    def summarize_costs(
        self,
        cost_items: List[CostItem],
        currency: str = "USD"
    ) -> CostSummary:
        """
        汇总成本
        
        Args:
            cost_items: 成本项列表
            currency: 货币单位
            
        Returns:
            成本汇总
        """
        if not cost_items:
            # 返回空的汇总
            return CostSummary(
                total_cost=Decimal("0"),
                cost_by_resource_type={},
                cost_by_service={},
                cost_by_region={},
                cost_by_pricing_model={},
                period_start=datetime.now(),
                period_end=datetime.now(),
                currency=currency
            )
        
        # 计算总成本
        total_cost = sum(item.total_cost for item in cost_items)
        
        # 按资源类型汇总
        cost_by_resource_type = {}
        for item in cost_items:
            resource_type = item.resource_type
            cost_by_resource_type[resource_type] = cost_by_resource_type.get(resource_type, Decimal("0")) + item.total_cost
        
        # 按服务汇总 (从实例类型推断)
        cost_by_service = {}
        for item in cost_items:
            if item.resource_spec.instance_type:
                service = "ec2" if "t3" in item.resource_spec.instance_type.lower() else "compute"
            elif item.resource_type == ResourceType.STORAGE:
                service = "storage"
            elif item.resource_type == ResourceType.NETWORK:
                service = "network"
            else:
                service = "other"
            
            cost_by_service[service] = cost_by_service.get(service, Decimal("0")) + item.total_cost
        
        # 按区域汇总
        cost_by_region = {}
        for item in cost_items:
            region = item.resource_spec.region or self.default_region
            cost_by_region[region] = cost_by_region.get(region, Decimal("0")) + item.total_cost
        
        # 按定价模型汇总
        cost_by_pricing_model = {}
        for item in cost_items:
            pricing_model = item.pricing_model
            cost_by_pricing_model[pricing_model] = cost_by_pricing_model.get(pricing_model, Decimal("0")) + item.total_cost
        
        # 确定时间段
        period_start = min(item.period_start for item in cost_items)
        period_end = max(item.period_end for item in cost_items)
        
        return CostSummary(
            total_cost=total_cost,
            cost_by_resource_type=cost_by_resource_type,
            cost_by_service=cost_by_service,
            cost_by_region=cost_by_region,
            cost_by_pricing_model=cost_by_pricing_model,
            period_start=period_start,
            period_end=period_end,
            currency=currency
        )


# 使用示例
def example_usage():
    """使用示例"""
    calculator = ResourceCostCalculator()
    
    # 计算实例成本
    instance_cost = calculator.calculate_instance_cost(
        instance_type="t3.medium",
        usage_hours=720,  # 30天
        provider="aws"
    )
    print(f"t3.medium实例30天成本: ${instance_cost}")
    
    # 计算存储成本
    storage_cost = calculator.calculate_storage_cost(
        storage_gb=100,
        storage_type="gp2",
        provider="aws",
        duration_days=30
    )
    print(f"100GB gp2存储30天成本: ${storage_cost}")
    
    # 计算网络成本
    network_cost = calculator.calculate_network_cost(
        data_transfer_gb=1000,
        direction="outbound",
        provider="aws"
    )
    print(f"1000GB出站数据传输成本: ${network_cost}")
    
    # 创建资源规格
    resource_spec = ResourceSpec(
        resource_type=ResourceType.INSTANCE,
        quantity=1,
        unit="instance",
        instance_type="t3.medium",
        region="us-east-1"
    )
    
    # 计算资源成本
    cost_item = calculator.calculate_resource_cost(
        resource_spec=resource_spec,
        usage_hours=24,
        provider="aws",
        pricing_model=PricingModel.ON_DEMAND
    )
    
    print(f"\n资源成本详情:")
    print(f"  资源类型: {cost_item.resource_type}")
    print(f"  实例类型: {cost_item.resource_spec.instance_type}")
    print(f"  使用小时: {cost_item.usage_hours}")
    print(f"  单价: ${cost_item.unit_price}/小时")
    print(f"  总成本: ${cost_item.total_cost}")
    print(f"  定价模型: {cost_item.pricing_model.value}")


if __name__ == "__main__":
    example_usage()