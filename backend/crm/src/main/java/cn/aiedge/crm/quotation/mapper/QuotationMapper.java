package cn.aiedge.crm.quotation.mapper;

import cn.aiedge.crm.quotation.entity.Quotation;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface QuotationMapper extends BaseMapper<Quotation> {

    @Select("SELECT * FROM crm_quotation WHERE customer_id = #{customerId} AND deleted = 0 ORDER BY create_time DESC")
    List<Quotation> selectByCustomerId(@Param("customerId") Long customerId);

    @Select("SELECT * FROM crm_quotation WHERE opportunity_id = #{opportunityId} AND deleted = 0 ORDER BY create_time DESC")
    List<Quotation> selectByOpportunityId(@Param("opportunityId") Long opportunityId);

    @Select("SELECT * FROM crm_quotation WHERE sales_person_id = #{salesPersonId} AND deleted = 0 ORDER BY create_time DESC")
    List<Quotation> selectBySalesPersonId(@Param("salesPersonId") Long salesPersonId);

    @Select("SELECT * FROM crm_quotation WHERE parent_id = #{parentId} AND deleted = 0 ORDER BY version DESC")
    List<Quotation> selectVersionsByParentId(@Param("parentId") Long parentId);

    @Select("SELECT MAX(version) FROM crm_quotation WHERE parent_id = #{parentId} AND deleted = 0")
    Integer selectMaxVersionByParentId(@Param("parentId") Long parentId);

    @Select("SELECT COUNT(*) FROM crm_quotation WHERE status = #{status} AND deleted = 0 AND tenant_id = #{tenantId}")
    Integer countByStatus(@Param("status") Integer status, @Param("tenantId") Long tenantId);

    @Select("SELECT * FROM crm_quotation WHERE status IN (3, 4) AND valid_to < #{now} AND deleted = 0")
    List<Quotation> selectExpiredQuotations(@Param("now") java.time.LocalDate now);

    /**
     * 插入 erp_sale_order 表（报价转销售订单，Java 生成 ID 和单号）。
     *
     * <p>⚠️ {@code customer_id} 必须传 **ERP 往来单位 ID**（{@code biz_party.id}），
     * 由 {@code QuotationServiceImpl#createSaleOrder} 从 {@code crm_customer.md_partner_id} 解析后传入；
     * 早期版本直接写 {@code quotation.customerId}（CRM 客户 ID）属跨域 ID 混用，已禁止。</p>
     */
    @Insert("INSERT INTO erp_sale_order(id, tenant_id, order_no, customer_id, customer_name, salesman_name, "
        + "order_date, total_amount, product_amount, status, order_source, original_order_id, original_order_no, "
        + "remark, deleted, create_time, update_time, create_by, update_by) "
        + "VALUES(#{orderId}, #{quotation.tenantId}, #{orderNo}, "
        + "#{erpCustomerId}, #{quotation.customerName}, #{quotation.salesPersonName}, "
        + "CURRENT_DATE, #{quotation.totalAmount}, #{quotation.totalAmount}, 0, "
        + "1, #{quotation.id}, #{quotation.quotationNo}, '报价转单', "
        + "0, NOW(), NOW(), #{quotation.createBy}, #{quotation.updateBy})")
    void insertSaleOrderRaw(@Param("orderId") long orderId, @Param("orderNo") String orderNo,
        @Param("quotation") Quotation quotation, @Param("erpCustomerId") Long erpCustomerId);

    /**
     * 插入单条 erp_sale_order_item 明细行。
     *
     * <p>⚠️ 必须显式写 {@code tenant_id}：该列是 NOT NULL 且 **DEFAULT 1**，
     * 漏写不会报错，而是**静默落成租户 1**（早期版本即如此，租户 2 的报价转单会把明细写进租户 1）。
     * {@code deleted / create_by / update_by} 同理补齐，避免与主表口径不一致。</p>
     */
    @Insert("INSERT INTO erp_sale_order_item(id, tenant_id, order_id, line_no, product_id, product_name, "
        + "quantity, unit_price, amount, remark, deleted, create_time, update_time, create_by, update_by) "
        + "VALUES(#{id}, #{tenantId}, #{orderId}, #{lineNo}, #{item.productId}, #{item.productName}, "
        + "#{item.quantity}, #{item.unitPrice}, #{item.lineAmount}, '', 0, NOW(), NOW(), #{createBy}, #{createBy})")
    void insertSaleOrderItem(@Param("id") long id, @Param("orderId") long orderId,
        @Param("lineNo") int lineNo, @Param("item") cn.aiedge.crm.quotation.entity.QuotationItem item,
        @Param("tenantId") Long tenantId, @Param("createBy") Long createBy);

    /** 当日已有的最大销售订单号（用于生成「当日序号」，替代原先对毫秒 ID 取模 10000 的写法） */
    @Select("SELECT MAX(order_no) FROM erp_sale_order WHERE order_no LIKE #{prefix} || '%'")
    String selectMaxSaleOrderNo(@Param("prefix") String prefix);
}
