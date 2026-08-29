package cn.aiedge.erp.purchase.mapper;

import cn.aiedge.erp.purchase.dto.SupplierSnapshotRow;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 供应商快照查询 Mapper（跨模块只读 erp_supplier）
 *
 * <p>采购入库单仅在主表冗余 supplier_id/supplier_name，
 * 未冗余供应商编号/联系人/电话/地址/备注。为补齐采购单据查询的
 * 「供应商编号/联系人/联系电话/联系地址/供应商备注」列，
 * 从 erp_supplier 档案表按 id 批量读取（只读，不介入写链路）。
 *
 * <p>注意：erp_supplier.tenant_id 为 character varying，与 MyBatis-Plus
 * 多租户插件自动注入的整型 `tenant_id = 1` 类型不匹配，故本查询忽略租户行拦截，
 * 改按主键精确过滤（id 全局唯一，安全）。
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Mapper
public interface SupplierSnapshotMapper {

    @InterceptorIgnore(tenantLine = "true")
    @Select("<script>" +
            "SELECT id, supplier_code AS supplierCode, contact_person AS contactName, " +
            "       contact_phone AS contactPhone, company_address AS contactAddress, remark AS supplierRemark " +
            "FROM erp_supplier " +
            "WHERE deleted = 0 AND id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>" +
            "</script>")
    List<SupplierSnapshotRow> selectByIds(@Param("ids") List<Long> ids);
}
