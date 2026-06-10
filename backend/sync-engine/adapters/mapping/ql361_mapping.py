"""
ql361 ERP → AI-Ready 数据字段映射配置

定义 ql361.com 系统 19 种单据类型到 AI-Ready ERP 业务表的完整字段映射。
所有 ql361 单据共享相同的 97 个字段结构，通过 billtype 区分业务类型。

映射规则说明:
  - billcode      → *_no           (单据编号)
  - billdate      → *_date         (单据日期)
  - dfullname     → customer_name / supplier_name  (对方单位名称)
  - totalmoney    → total_amount   (总金额)
  - status  (10=草稿, 20=已记账)  → status
  - soutfullname  → warehouse_name / from_warehouse  (出库/调出仓库)
  - sinfullname   → warehouse_name / to_warehouse    (入库/调入仓库)
  - taxedmoney    → total_amount_with_tax  (含税金额)
  - discountmoney1→ discount_amount  (折扣金额)
  - afullname1    → bank_account      (银行账户, 资金单据)
"""

from typing import Any, Dict, List, Optional, Tuple


# ========================================================================
# 字段值转换函数
# ========================================================================

def _to_float(value: Any) -> Optional[float]:
    """转换为浮点数；无法转换或 None 时返回 None"""
    if value is None:
        return None
    try:
        return float(str(value).replace(",", ""))
    except (ValueError, TypeError):
        return None


def _to_int(value: Any) -> Optional[int]:
    """转换为整数；无法转换或 None 时返回 None"""
    if value is None:
        return None
    try:
        return int(float(str(value)))
    except (ValueError, TypeError):
        return None


def _to_str(value: Any) -> Optional[str]:
    """转换为字符串；None 输入返回 None"""
    return str(value) if value is not None else None


def _to_date(value: Any) -> Optional[str]:
    """转换为日期字符串 YYYY-MM-DD；处理 ql361 常见的 '2026-05-23 14:30:00' 格式"""
    if value is None:
        return None
    s = str(value).strip()
    if not s:
        return None
    return s[:10]  # 截取前 10 位得到日期部分


def _upper(value: Any) -> Optional[str]:
    """转换为大写"""
    if value is None:
        return None
    return str(value).upper()


def _lower(value: Any) -> Optional[str]:
    """转换为小写"""
    if value is None:
        return None
    return str(value).lower()


def _none_if_empty(value: Any) -> Optional[Any]:
    """空字符串转换为 None，其余保持原值"""
    if value is None:
        return None
    if isinstance(value, str) and value.strip() == "":
        return None
    return value


# 转换函数注册表，供 transform_value() 按名查找
_TRANSFORM_MAP: Dict[str, Any] = {
    "to_float": _to_float,
    "to_int": _to_int,
    "to_str": _to_str,
    "to_date": _to_date,
    "upper": _upper,
    "lower": _lower,
    "none_if_empty": _none_if_empty,
}


# ========================================================================
# 单据类型 → AI-Ready 目标表映射
# 每个条目 = [主表, ...明细表]
# map_ql361_to_ai_ready() 返回主表映射
# ========================================================================

BILL_TYPE_TABLE_MAPPING: Dict[str, List[str]] = {
    # ── 采购模块 ──
    "501": ["erp_purchase_inbound", "erp_purchase_inbound_item"],   # 采购入库单
    "502": ["erp_purchase_return"],                                  # 采购退货单
    "504": ["erp_purchase_order"],                                   # 采购订单
    # ── 销售模块 ──
    "601": ["erp_sale_outbound", "erp_sale_outbound_item"],          # 销售出库单
    "602": ["erp_sale_outbound"],                                    # 销售退货单
    "603": ["erp_sale_outbound"],                                    # 销售换货单
    "604": ["erp_sale_order", "erp_sale_order_item"],                # 销售订单
    "605": ["erp_sale_outbound"],                                    # 销售退货申请单
    "607": ["erp_sale_order"],                                       # 销售单/POS
    # ── 库存模块 ──
    "702": ["erp_stock_transfer"],                                   # 调拨单
    "901": ["erp_stock_transfer"],                                   # 库存中转单
    "903": ["erp_stock_transfer"],                                   # 报损单
    "904": ["erp_sale_outbound"],                                    # 领料出库单
    # ── 资金模块 ──
    "801": ["erp_receipt"],                                          # 收款单
    "802": ["erp_payment"],                                          # 付款单
    "803": ["erp_payment"],                                          # 应付核销单
    "804": ["erp_receipt"],                                          # 应收核销单
    "805": ["erp_payment"],                                          # 应付冲抵单
    # ── 人事模块 ──
    "905": ["erp_purchase_order"],                                   # 计工凭证（通用映射）
}


# ========================================================================
# 字段映射定义（按单据类型）
# 格式: (ql361字段名, AI-Ready列名, 转换函数名或None)
# ========================================================================

TABLE_FIELD_MAPPINGS: Dict[str, List[Tuple[str, str, Optional[str]]]] = {

    # ══════════════════════════════════════════════════════════════════════
    # 501 采购入库单 → erp_purchase_inbound
    # 目标列: inbound_no, order_no, supplier_name, warehouse_name,
    #         inbound_date, status, total_amount, remark
    # ══════════════════════════════════════════════════════════════════════
    "501": [
        ("billcode",          "inbound_no",     "to_str"),        # 入库单号
        ("billdate",          "inbound_date",   "to_date"),       # 入库日期
        ("dfullname",         "supplier_name",  "to_str"),        # 供应商名称
        ("totalmoney",        "total_amount",   "to_float"),      # 总金额
        ("sinfullname",       "warehouse_name", "to_str"),        # 入库仓库
        ("status",            "status",         "to_int"),        # 状态(10草稿/20已记账)
        ("remark",            "remark",         "none_if_empty"), # 备注
        ("orderbillcode",     "order_no",       "to_str"),        # 关联采购订单号
    ],

    # ══════════════════════════════════════════════════════════════════════
    # 502 采购退货单 → erp_purchase_return
    # 目标列: return_no, order_no, supplier_name, return_date,
    #         return_reason, status, total_amount, remark
    # ══════════════════════════════════════════════════════════════════════
    "502": [
        ("billcode",          "return_no",      "to_str"),        # 退货单号
        ("billdate",          "return_date",    "to_date"),       # 退货日期
        ("dfullname",         "supplier_name",  "to_str"),        # 供应商名称
        ("totalmoney",        "total_amount",   "to_float"),      # 退货金额
        ("status",            "status",         "to_int"),        # 状态
        ("remark",            "remark",         "none_if_empty"), # 备注
        ("dmemo",             "return_reason",  "to_str"),        # 退货原因
        ("orderbillcode",     "order_no",       "to_str"),        # 关联订单号
    ],

    # ══════════════════════════════════════════════════════════════════════
    # 504 采购订单 → erp_purchase_order
    # 目标列: order_no, supplier_name, order_date, total_amount,
    #         tax_amount, discount_amount, paid_amount, status,
    #         warehouse_name, payment_method, remark
    # ══════════════════════════════════════════════════════════════════════
    "504": [
        ("billcode",          "order_no",        "to_str"),        # 订单编号
        ("billdate",          "order_date",      "to_date"),       # 订单日期
        ("dfullname",         "supplier_name",   "to_str"),        # 供应商名称
        ("totalmoney",        "total_amount",    "to_float"),      # 订单总金额
        ("taxmoney",          "tax_amount",      "to_float"),      # 税额
        ("discountmoney1",    "discount_amount", "to_float"),      # 折扣金额
        ("premoney",          "paid_amount",     "to_float"),      # 已付款金额
        ("status",            "status",          "to_int"),        # 状态
        ("soutfullname",      "warehouse_name",  "to_str"),        # 仓库
        ("paytype",           "payment_method",  "to_str"),        # 付款方式
        ("remark",            "remark",          "none_if_empty"), # 备注
    ],

    # ══════════════════════════════════════════════════════════════════════
    # 601 销售出库单 → erp_sale_outbound
    # 目标列: outbound_no, order_no, customer_name, warehouse_name,
    #         outbound_date, status, total_amount, remark
    # ══════════════════════════════════════════════════════════════════════
    "601": [
        ("billcode",          "outbound_no",    "to_str"),         # 出库单号
        ("billdate",          "outbound_date",  "to_date"),        # 出库日期
        ("dfullname",         "customer_name",  "to_str"),         # 客户名称
        ("totalmoney",        "total_amount",   "to_float"),       # 总金额
        ("soutfullname",      "warehouse_name", "to_str"),         # 出库仓库
        ("status",            "status",         "to_int"),         # 状态
        ("remark",            "remark",         "none_if_empty"),  # 备注
        ("orderbillcode",     "order_no",       "to_str"),         # 关联销售订单号
    ],

    # ══════════════════════════════════════════════════════════════════════
    # 602 销售退货单 → erp_sale_outbound
    # 客户退货回仓库，使用 sinfullname 作为入库仓库
    # ══════════════════════════════════════════════════════════════════════
    "602": [
        ("billcode",          "outbound_no",    "to_str"),         # 退货单号
        ("billdate",          "outbound_date",  "to_date"),        # 退货日期
        ("dfullname",         "customer_name",  "to_str"),         # 客户名称
        ("totalmoney",        "total_amount",   "to_float"),       # 退货金额
        ("sinfullname",       "warehouse_name", "to_str"),         # 退货入库仓库
        ("status",            "status",         "to_int"),         # 状态
        ("remark",            "remark",         "none_if_empty"),  # 备注
        ("orderbillcode",     "order_no",       "to_str"),         # 关联订单号
    ],

    # ══════════════════════════════════════════════════════════════════════
    # 603 销售换货单 → erp_sale_outbound
    # 换货出库，与正常出库结构一致
    # ══════════════════════════════════════════════════════════════════════
    "603": [
        ("billcode",          "outbound_no",    "to_str"),         # 换货单号
        ("billdate",          "outbound_date",  "to_date"),        # 换货日期
        ("dfullname",         "customer_name",  "to_str"),         # 客户名称
        ("totalmoney",        "total_amount",   "to_float"),       # 换货金额
        ("soutfullname",      "warehouse_name", "to_str"),         # 出库仓库
        ("status",            "status",         "to_int"),         # 状态
        ("remark",            "remark",         "none_if_empty"),  # 备注
        ("orderbillcode",     "order_no",       "to_str"),         # 关联订单号
    ],

    # ══════════════════════════════════════════════════════════════════════
    # 604 销售订单 → erp_sale_order
    # 目标列: order_no, customer_name, order_date, total_amount,
    #         total_amount_with_tax, discount_amount, received_amount,
    #         status, warehouse_name, salesman_name, remark,
    #         shipping_address, receiver_name, receiver_phone, payment_method
    # ══════════════════════════════════════════════════════════════════════
    "604": [
        ("billcode",          "order_no",                "to_str"),        # 订单编号
        ("billdate",          "order_date",              "to_date"),       # 订单日期
        ("dfullname",         "customer_name",           "to_str"),        # 客户名称
        ("totalmoney",        "total_amount",            "to_float"),      # 订单总金额
        ("taxedmoney",        "total_amount_with_tax",   "to_float"),      # 含税金额
        ("taxmoney",          "tax_amount",              "to_float"),      # 税额
        ("discountmoney1",    "discount_amount",         "to_float"),      # 折扣金额
        ("premoney",          "received_amount",         "to_float"),      # 已收款
        ("status",            "status",                  "to_int"),        # 状态
        ("soutfullname",      "warehouse_name",          "to_str"),        # 仓库名称
        ("bfullname",         "salesman_name",           "to_str"),        # 销售员
        ("remark",            "remark",                  "none_if_empty"), # 备注
        ("address",           "shipping_address",        "to_str"),        # 收货地址
        ("contactor",         "receiver_name",           "to_str"),        # 收货人
        ("dtel",              "receiver_phone",          "to_str"),        # 联系电话
        ("paytype",           "payment_method",          "to_str"),        # 付款方式
    ],

    # ══════════════════════════════════════════════════════════════════════
    # 605 销售退货申请单 → erp_sale_outbound
    # 退货申请尚未实际出库，使用 sinfullname 作为目标仓库
    # ══════════════════════════════════════════════════════════════════════
    "605": [
        ("billcode",          "outbound_no",    "to_str"),         # 退货申请单号
        ("billdate",          "outbound_date",  "to_date"),        # 申请日期
        ("dfullname",         "customer_name",  "to_str"),         # 客户名称
        ("totalmoney",        "total_amount",   "to_float"),       # 金额
        ("sinfullname",       "warehouse_name", "to_str"),         # 退货仓库
        ("status",            "status",         "to_int"),         # 状态
        ("remark",            "remark",         "none_if_empty"),  # 备注
        ("orderbillcode",     "order_no",       "to_str"),         # 关联订单号
    ],

    # ══════════════════════════════════════════════════════════════════════
    # 607 销售单/POS → erp_sale_order
    # POS 销售即时开单，包含订单+收款+出库
    # ══════════════════════════════════════════════════════════════════════
    "607": [
        ("billcode",          "order_no",                "to_str"),        # POS单号
        ("billdate",          "order_date",              "to_date"),       # 开单日期
        ("dfullname",         "customer_name",           "to_str"),        # 客户名称
        ("totalmoney",        "total_amount",            "to_float"),      # 总金额
        ("taxedmoney",        "total_amount_with_tax",   "to_float"),      # 含税金额
        ("discountmoney1",    "discount_amount",         "to_float"),      # 折扣金额
        ("paymoney",          "received_amount",         "to_float"),      # 实收金额
        ("status",            "status",                  "to_int"),        # 状态
        ("soutfullname",      "warehouse_name",          "to_str"),        # 仓库
        ("bfullname",         "salesman_name",           "to_str"),        # 收银员/销售员
        ("remark",            "remark",                  "none_if_empty"), # 备注
        ("paytype",           "payment_method",          "to_str"),        # 支付方式
    ],

    # ══════════════════════════════════════════════════════════════════════
    # 702 调拨单 → erp_stock_transfer
    # 目标列: transfer_no, transfer_date, from_warehouse, to_warehouse,
    #         status, remark
    # soutfullname = 调出仓库, sinfullname = 调入仓库
    # ══════════════════════════════════════════════════════════════════════
    "702": [
        ("billcode",          "transfer_no",    "to_str"),         # 调拨单号
        ("billdate",          "transfer_date",  "to_date"),        # 调拨日期
        ("soutfullname",      "from_warehouse", "to_str"),         # 调出仓库
        ("sinfullname",       "to_warehouse",   "to_str"),         # 调入仓库
        ("status",            "status",         "to_int"),         # 状态
        ("remark",            "remark",         "none_if_empty"),  # 备注
    ],

    # ══════════════════════════════════════════════════════════════════════
    # 801 收款单 → erp_receipt
    # 目标列: receipt_no, customer_name, receipt_date, total_amount,
    #         receipt_method, status, bank_account, remark
    # ══════════════════════════════════════════════════════════════════════
    "801": [
        ("billcode",          "receipt_no",     "to_str"),         # 收款单号
        ("billdate",          "receipt_date",   "to_date"),        # 收款日期
        ("dfullname",         "customer_name",  "to_str"),         # 付款方(客户名称)
        ("totalmoney",        "total_amount",   "to_float"),       # 收款金额
        ("paytype",           "receipt_method", "to_str"),         # 收款方式
        ("status",            "status",         "to_int"),         # 状态
        ("afullname1",        "bank_account",   "to_str"),         # 收款银行账户
        ("remark",            "remark",         "none_if_empty"),  # 备注
    ],

    # ══════════════════════════════════════════════════════════════════════
    # 802 付款单 → erp_payment
    # 目标列: payment_no, supplier_name, payment_date, total_amount,
    #         payment_method, status, bank_account, remark
    # ══════════════════════════════════════════════════════════════════════
    "802": [
        ("billcode",          "payment_no",     "to_str"),         # 付款单号
        ("billdate",          "payment_date",   "to_date"),        # 付款日期
        ("dfullname",         "supplier_name",  "to_str"),         # 收款方(供应商名称)
        ("totalmoney",        "total_amount",   "to_float"),       # 付款金额
        ("paytype",           "payment_method", "to_str"),         # 付款方式
        ("status",            "status",         "to_int"),         # 状态
        ("afullname1",        "bank_account",   "to_str"),         # 付款银行账户
        ("remark",            "remark",         "none_if_empty"),  # 备注
    ],

    # ══════════════════════════════════════════════════════════════════════
    # 803 应付核销单 → erp_payment
    # 应付核销 = 供应商付款核销，与付款单结构一致
    # ══════════════════════════════════════════════════════════════════════
    "803": [
        ("billcode",          "payment_no",     "to_str"),         # 核销单号
        ("billdate",          "payment_date",   "to_date"),        # 核销日期
        ("dfullname",         "supplier_name",  "to_str"),         # 供应商
        ("totalmoney",        "total_amount",   "to_float"),       # 核销金额
        ("status",            "status",         "to_int"),         # 状态
        ("afullname1",        "bank_account",   "to_str"),         # 银行账户
        ("remark",            "remark",         "none_if_empty"),  # 备注
        ("orderbillcode",     "order_no",       "to_str"),         # 关联单据号
    ],

    # ══════════════════════════════════════════════════════════════════════
    # 804 应收核销单 → erp_receipt
    # 应收核销 = 客户收款核销，与收款单结构一致
    # ══════════════════════════════════════════════════════════════════════
    "804": [
        ("billcode",          "receipt_no",     "to_str"),         # 核销单号
        ("billdate",          "receipt_date",   "to_date"),        # 核销日期
        ("dfullname",         "customer_name",  "to_str"),         # 客户
        ("totalmoney",        "total_amount",   "to_float"),       # 核销金额
        ("status",            "status",         "to_int"),         # 状态
        ("afullname1",        "bank_account",   "to_str"),         # 银行账户
        ("remark",            "remark",         "none_if_empty"),  # 备注
        ("orderbillcode",     "order_no",       "to_str"),         # 关联单据号
    ],

    # ══════════════════════════════════════════════════════════════════════
    # 805 应付冲抵单 → erp_payment
    # 应付冲抵 = 供应商应付账款互相冲抵
    # ══════════════════════════════════════════════════════════════════════
    "805": [
        ("billcode",          "payment_no",     "to_str"),         # 冲抵单号
        ("billdate",          "payment_date",   "to_date"),        # 冲抵日期
        ("dfullname",         "supplier_name",  "to_str"),         # 供应商
        ("totalmoney",        "total_amount",   "to_float"),       # 冲抵金额
        ("status",            "status",         "to_int"),         # 状态
        ("remark",            "remark",         "none_if_empty"),  # 备注
        ("orderbillcode",     "order_no",       "to_str"),         # 关联单据号
    ],

    # ══════════════════════════════════════════════════════════════════════
    # 901 库存中转单 → erp_stock_transfer
    # 库存中转 = 货物经中转仓库转运，类似调拨
    # ══════════════════════════════════════════════════════════════════════
    "901": [
        ("billcode",          "transfer_no",    "to_str"),         # 中转单号
        ("billdate",          "transfer_date",  "to_date"),        # 中转日期
        ("soutfullname",      "from_warehouse", "to_str"),         # 调出仓库
        ("sinfullname",       "to_warehouse",   "to_str"),         # 调入仓库
        ("status",            "status",         "to_int"),         # 状态
        ("remark",            "remark",         "none_if_empty"),  # 备注
    ],

    # ══════════════════════════════════════════════════════════════════════
    # 903 报损单 → erp_stock_transfer
    # 报损 = 库存损毁出库，只有调出方，无调入方
    # ══════════════════════════════════════════════════════════════════════
    "903": [
        ("billcode",          "transfer_no",    "to_str"),         # 报损单号
        ("billdate",          "transfer_date",  "to_date"),        # 报损日期
        ("soutfullname",      "from_warehouse", "to_str"),         # 报损仓库
        ("status",            "status",         "to_int"),         # 状态
        ("remark",            "remark",         "none_if_empty"),  # 备注
    ],

    # ══════════════════════════════════════════════════════════════════════
    # 904 领料出库单 → erp_sale_outbound
    # 生产领料 = 从仓库领出物料用于生产
    # dfullname 在此场景为领料部门/人员
    # ══════════════════════════════════════════════════════════════════════
    "904": [
        ("billcode",          "outbound_no",    "to_str"),         # 领料单号
        ("billdate",          "outbound_date",  "to_date"),        # 领料日期
        ("dfullname",         "customer_name",  "to_str"),         # 领料部门/人员
        ("totalmoney",        "total_amount",   "to_float"),       # 领料金额
        ("soutfullname",      "warehouse_name", "to_str"),         # 出库仓库
        ("status",            "status",         "to_int"),         # 状态
        ("remark",            "remark",         "none_if_empty"),  # 备注
    ],

    # ══════════════════════════════════════════════════════════════════════
    # 905 计工凭证 → erp_purchase_order（通用映射）
    # 计工凭证 = 员工计件/计时工资凭证
    # 目前无专用表，暂用 erp_purchase_order 承载公共字段
    # ✅ dfullname → supplier_name（实际为员工姓名）
    # ✅ totalmoney → total_amount
    # ✅ qty → 计工数量
    # ══════════════════════════════════════════════════════════════════════
    "905": [
        ("billcode",          "order_no",        "to_str"),         # 凭证编号
        ("billdate",          "order_date",      "to_date"),        # 计工日期
        ("dfullname",         "supplier_name",   "to_str"),         # 员工姓名
        ("totalmoney",        "total_amount",    "to_float"),       # 工资总额
        ("qty",               "total_qty",       "to_float"),       # 计工数量
        ("status",            "status",          "to_int"),         # 状态
        ("remark",            "remark",          "none_if_empty"),  # 备注
    ],
}


# ========================================================================
# 公共辅助函数
# ========================================================================

def transform_value(value: Any, transform_name: Optional[str] = None) -> Any:
    """
    对字段值应用转换函数。

    Args:
        value:          原始值
        transform_name: 转换函数名称（见 _TRANSFORM_MAP 键）
                        None 表示不做转换，直接返回原值

    Returns:
        转换后的值
    """
    if transform_name is None:
        return value
    func = _TRANSFORM_MAP.get(transform_name)
    if func is None:
        return value
    return func(value)


def map_ql361_to_ai_ready(bill_type: str, record: Dict[str, Any]) -> Optional[Dict[str, Any]]:
    """
    将一条 ql361 单据记录映射为 AI-Ready 目标表数据。

    Args:
        bill_type: 单据类型代码（如 "601", "702"）
        record:    ql361 API 返回的原始记录字典

    Returns:
        {
            "table": "erp_sale_outbound",  # 目标表名
            "data": {
                "outbound_no": "XSCKD-20260523-018",
                "customer_name": "某某客户",
                ...
            }
        }
        如果 bill_type 无对应映射，返回 None。

    Notes:
        - 结果只包含可映射的字段（无对应映射的 ql361 字段被跳过）
        - status: ql361 使用 10=草稿, 20=已记账，直接透传整数值
        - 目标表中 customer_id / supplier_id / warehouse_id 等 ID 字段
          需要通过名称在 AI-Ready 系统中查找解析
        - tenant_id 需由调用层注入
    """
    tables = BILL_TYPE_TABLE_MAPPING.get(bill_type)
    if not tables:
        return None

    target_table = tables[0]  # 第一个是主表
    field_list = TABLE_FIELD_MAPPINGS.get(bill_type, [])

    data: Dict[str, Any] = {}
    for ql361_field, ai_ready_col, transform_name in field_list:
        value = record.get(ql361_field)
        data[ai_ready_col] = transform_value(value, transform_name)

    return {
        "table": target_table,
        "data": data,
    }
