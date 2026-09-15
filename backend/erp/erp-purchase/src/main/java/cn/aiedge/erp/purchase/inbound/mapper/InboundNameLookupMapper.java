package cn.aiedge.erp.purchase.inbound.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 采购入库单名称解析（只读）
 *
 * <p>由采购订单生成入库单时，需要把单据头的名称快照带过来（供应商/仓库/经手人/部门）。
 * 取值口径与《采购单据查询》一致：</p>
 * <ul>
 *   <li>供应商名称 → 订单伙伴快照表 {@code erp_purchase_order_partner_snapshot}（单据口径，非供应商主数据）；</li>
 *   <li>仓库 → {@code erp_warehouse}；经手人 → {@code sys_user.nickname}；部门 → {@code sys_dept}。</li>
 * </ul>
 *
 * @author AI-Ready Team
 */
@Mapper
public interface InboundNameLookupMapper {

    /** 订单伙伴快照的供应商名称（该表无 deleted 列，与《采购单据查询》JOIN 口径一致） */
    @Select("SELECT supplier_name FROM erp_purchase_order_partner_snapshot WHERE order_id = #{orderId} LIMIT 1")
    String findSupplierNameByOrder(@Param("orderId") Long orderId);

    @Select("SELECT warehouse_name FROM erp_warehouse WHERE id = #{id} AND deleted = 0")
    String findWarehouseName(@Param("id") Long id);

    @Select("SELECT nickname FROM sys_user WHERE id = #{id} AND deleted = 0")
    String findUserName(@Param("id") Long id);

    @Select("SELECT dept_name FROM sys_dept WHERE id = #{id} AND deleted = 0")
    String findDeptName(@Param("id") Long id);
}
