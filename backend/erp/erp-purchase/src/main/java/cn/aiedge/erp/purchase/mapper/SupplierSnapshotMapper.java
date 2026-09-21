package cn.aiedge.erp.purchase.mapper;

import cn.aiedge.erp.purchase.dto.SupplierSnapshotRow;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 供应商快照查询 Mapper（跨模块只读供应商档案）
 *
 * <p>采购入库单仅在主表冗余 supplier_id/supplier_name，
 * 未冗余供应商编号/联系人/电话/地址/备注。为补齐采购单据查询的
 * 「供应商编号/联系人/联系电话/联系地址/供应商备注」列，
 * 按 id 批量读取档案（只读，不介入写链路）。</p>
 *
 * <p><b>2026-09-21 修复（PUR-BREAK-03）：档案表由 {@code erp_supplier} 改为 {@code biz_party}。</b>
 * 原写法读的是 {@code erp_supplier}，而该表**实测 0 行** —— 它是个没人写、没人维护的重复档案表
 * （建表语句在 {@code DatabaseInitializer}，消费方只有本 Mapper 与
 * {@code PurchaseAnalysisReportServiceImpl}）；系统真正在用的供应商主数据是
 * {@code biz_party}（152 行、按 {@code party_type} 区分往来单位角色），
 * 供应商管理页 {@code views/md/supplier} 走的也是 biz_party（{@code partnerApi}）。
 * 于是这五个「供应商编号/联系人/电话/地址/备注」列此前**恒为空**。</p>
 *
 * <p>列映射：供应商编号←{@code party_code}、联系人←{@code default_handler_name}（默认经手人，
 * 空则退 {@code legal_person}）、电话←{@code phone}（空则退 {@code legal_person_phone}）、
 * 地址←{@code address}、备注←{@code remark}。</p>
 *
 * <p>注意：{@code biz_party.tenant_id} 为 bigint，但本查询仍忽略租户行拦截、改按主键精确过滤
 * （id 全局唯一，安全）—— 与项目其它「跨模块只读主数据」的 Mapper 口径一致。</p>
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Mapper
public interface SupplierSnapshotMapper {

    @InterceptorIgnore(tenantLine = "true")
    @Select("<script>" +
            "SELECT id, party_code AS supplierCode, " +
            "       COALESCE(NULLIF(default_handler_name, ''), legal_person) AS contactName, " +
            "       COALESCE(NULLIF(phone, ''), legal_person_phone) AS contactPhone, " +
            "       address AS contactAddress, remark AS supplierRemark " +
            "FROM biz_party " +
            "WHERE deleted = 0 AND id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>" +
            "</script>")
    List<SupplierSnapshotRow> selectByIds(@Param("ids") List<Long> ids);
}
