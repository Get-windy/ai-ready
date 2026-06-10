"""
数据映射转换器

负责将源系统数据格式转换为目标系统格式。
支持字段映射、类型转换、默认值、自定义转换函数。
"""
from __future__ import annotations

from copy import deepcopy
from datetime import datetime
from typing import Any, Callable, Dict, List, Optional, Tuple

TransformFunc = Callable[[Any], Any]


class FieldMapping:
    """
    单个字段映射定义

    将源字段 src 映射到目标字段 dst，可选转换。

    Examples:
        >>> # 简单映射：源字段 -> 目标字段（同名）
        >>> FieldMapping("order_no")

        >>> # 重命名字段
        >>> FieldMapping("sale_id", "bill_id")

        >>> # 带类型转换
        >>> FieldMapping("create_time", "gmt_create", transform=str)

        >>> # 固定默认值
        >>> FieldMapping("status", default="pending")

        >>> # 自定义转换函数
        >>> FieldMapping("items", "line_items", transform=parse_items)
    """

    def __init__(
        self,
        src: str,
        dst: Optional[str] = None,
        *,
        transform: Optional[TransformFunc] = None,
        default: Any = None,
        required: bool = False,
    ):
        self.src = src
        self.dst = dst or src
        self.transform = transform
        self.default = default
        self.required = required


class DataMapper:
    """
    数据映射器

    管理一组 FieldMapping，将源记录列表转换为目标记录列表。

    Examples:
        >>> mapper = DataMapper([
        ...     FieldMapping("sale_no", "bill_no"),
        ...     FieldMapping("total_amount", "amount", transform=float),
        ...     FieldMapping("status", default="draft"),
        ... ])
        >>> mapper.map_one({"sale_no": "SO001", "total_amount": "100.50"})
        {'bill_no': 'SO001', 'amount': 100.5, 'status': 'draft'}
    """

    def __init__(self, mappings: Optional[List[FieldMapping]] = None):
        self._mappings: Dict[str, FieldMapping] = {}
        if mappings:
            for m in mappings:
                self._mappings[m.src] = m

    def add_mapping(self, mapping: FieldMapping) -> None:
        """添加单个字段映射"""
        self._mappings[mapping.src] = mapping

    def add_mappings(self, mappings: List[FieldMapping]) -> None:
        """批量添加字段映射"""
        for m in mappings:
            self._mappings[m.src] = m

    def map_one(self, record: Dict[str, Any]) -> Dict[str, Any]:
        """
        将单条源记录映射为目标记录

        Args:
            record: 源数据记录

        Returns:
            目标格式记录

        Raises:
            ValueError: 缺少 required 字段
        """
        result: Dict[str, Any] = {}

        for src_field, mapping in self._mappings.items():
            raw_value = record.get(src_field)

            if raw_value is None:
                if mapping.required:
                    raise ValueError(f"缺少必需字段: {src_field}")
                if mapping.default is not None:
                    value = mapping.default
                else:
                    continue
            else:
                value = raw_value

            if mapping.transform:
                try:
                    value = mapping.transform(value)
                except (ValueError, TypeError) as e:
                    raise ValueError(
                        f"字段 {src_field} -> {mapping.dst} 转换失败: {e}"
                    )

            result[mapping.dst] = value

        return result

    def map_many(
        self, records: List[Dict[str, Any]]
    ) -> Tuple[List[Dict[str, Any]], List[Tuple[int, str]]]:
        """
        批量映射记录

        Args:
            records: 源数据记录列表

        Returns:
            (成功记录列表, [(索引, 错误消息), ...])
        """
        success: List[Dict[str, Any]] = []
        errors: List[Tuple[int, str]] = []

        for idx, record in enumerate(records):
            try:
                mapped = self.map_one(record)
                success.append(mapped)
            except ValueError as e:
                errors.append((idx, str(e)))

        return success, errors

    def map_bill_type(
        self,
        bill_type: str,
        records: List[Dict[str, Any]],
    ) -> Tuple[List[Dict[str, Any]], List[Tuple[int, str]]]:
        """
        按单据类型映射（可被子类重写以支持不同单据类型的映射规则）

        默认行为：对所有记录使用统一映射规则。
        子类可以基于 bill_type 返回不同的映射逻辑。

        Args:
            bill_type: 单据类型标识
            records: 源数据记录列表

        Returns:
            (成功记录列表, [(索引, 错误消息), ...])
        """
        return self.map_many(records)


def default_mapper() -> DataMapper:
    """
    创建默认的数据映射器（空映射，透传数据）

    当源和目标字段名一致时使用。
    """
    return DataMapper([])
