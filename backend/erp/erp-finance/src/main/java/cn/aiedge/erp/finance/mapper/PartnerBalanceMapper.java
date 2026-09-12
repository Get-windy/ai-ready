package cn.aiedge.erp.finance.mapper;

import cn.aiedge.erp.finance.dto.PartnerBalanceDTO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 往来余额表Mapper
 * 从应收/应付/预收/预付表按往来单位聚合未结清余额
 */
@Mapper
public interface PartnerBalanceMapper {

    /**
     * 分页查询往来余额表
     * 口径：
     * - 应收/应付：deleted_flag=0 且状态非 written_off/bad_debt 且 remaining_amount>0
     * - 预收/预付：deleted=0 且 remaining_amount>0
     * - 净额 = 应收 - 应付 + 预付 - 预收
     */
    @Select("<script>"
            + "SELECT partner_type AS partnerType, partner_id AS partnerId, partner_name AS partnerName,"
            + " SUM(receivable_balance) AS receivableBalance, SUM(payable_balance) AS payableBalance,"
            + " SUM(pre_receipt_balance) AS preReceiptBalance, SUM(pre_payment_balance) AS prePaymentBalance,"
            + " SUM(receivable_balance) - SUM(payable_balance) + SUM(pre_payment_balance) - SUM(pre_receipt_balance) AS netBalance,"
            + " MAX(last_biz_date) AS lastBizDate"
            + " FROM ("
            + "  SELECT 'customer' AS partner_type, r.customer_id AS partner_id, MAX(r.customer_name) AS partner_name,"
            + "   SUM(r.remaining_amount) AS receivable_balance, 0::numeric AS payable_balance,"
            + "   0::numeric AS pre_receipt_balance, 0::numeric AS pre_payment_balance,"
            + "   MAX(COALESCE(r.invoice_date, r.created_at::date)) AS last_biz_date"
            + "  FROM finance_receivable r"
            + "  WHERE r.deleted_flag = 0 AND r.status NOT IN ('written_off','bad_debt')"
            + "   AND (r.remaining_amount > 0 OR r.source_type = 'ar_ap_adjust')"
            + "  GROUP BY r.customer_id"
            + "  UNION ALL"
            + "  SELECT 'supplier', p.supplier_id, MAX(p.supplier_name), 0::numeric, SUM(p.remaining_amount), 0::numeric, 0::numeric,"
            + "   MAX(COALESCE(p.invoice_date, p.created_at::date))"
            + "  FROM finance_payable p"
            + "  WHERE p.deleted_flag = 0 AND p.status NOT IN ('written_off','bad_debt')"
            + "   AND (p.remaining_amount > 0 OR p.source_type = 'ar_ap_adjust')"
            + "  GROUP BY p.supplier_id"
            + "  UNION ALL"
            + "  SELECT 'customer', pr.customer_id::text, MAX(pr.customer_name), 0::numeric, 0::numeric, SUM(pr.remaining_amount), 0::numeric,"
            + "   MAX(pr.receipt_date)"
            + "  FROM erp_pre_receipt pr"
            + "  WHERE pr.deleted = 0 AND pr.remaining_amount > 0"
            + "  GROUP BY pr.customer_id"
            + "  UNION ALL"
            + "  SELECT 'supplier', pp.supplier_id::text, MAX(pp.supplier_name), 0::numeric, 0::numeric, 0::numeric, SUM(pp.remaining_amount),"
            + "   MAX(pp.payment_date)"
            + "  FROM erp_pre_payment pp"
            + "  WHERE pp.deleted = 0 AND pp.remaining_amount > 0"
            + "  GROUP BY pp.supplier_id"
            + " ) t"
            + "<where>"
            + "<if test='partnerType != null and partnerType != \"\"'> AND partner_type = #{partnerType}</if>"
            + "<if test='keyword != null and keyword != \"\"'> AND (partner_name ILIKE CONCAT('%', #{keyword}, '%') OR partner_id ILIKE CONCAT('%', #{keyword}, '%'))</if>"
            + "</where>"
            + " GROUP BY partner_type, partner_id, partner_name"
            + "<if test='onlyNonZero'> HAVING (SUM(receivable_balance) + SUM(payable_balance) + SUM(pre_receipt_balance) + SUM(pre_payment_balance)) &lt;&gt; 0</if>"
            + " ORDER BY partner_type, partner_id"
            + "</script>")
    IPage<PartnerBalanceDTO> selectPartnerBalancePage(Page<PartnerBalanceDTO> page,
                                                      @Param("partnerType") String partnerType,
                                                      @Param("keyword") String keyword,
                                                      @Param("onlyNonZero") boolean onlyNonZero);
}
