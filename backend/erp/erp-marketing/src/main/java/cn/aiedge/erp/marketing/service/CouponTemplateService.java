package cn.aiedge.erp.marketing.service;

import cn.aiedge.erp.marketing.dto.CouponRecordRow;
import cn.aiedge.erp.marketing.entity.CouponCustomer;
import cn.aiedge.erp.marketing.entity.CouponTemplate;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

public interface CouponTemplateService extends IService<CouponTemplate> {

    /** 券模板分页（派生「未领取」列） */
    IPage<CouponTemplate> pageWithCounts(String couponName, String status, String customerScope,
                                         Integer openReceive, Integer pageNum, Integer pageSize);

    /** 领用明细分页（对标 13 列） */
    IPage<CouponRecordRow> pageRecords(Long templateId, String status, String billNo, String couponName,
                                       Long partnerId, Integer pageNum, Integer pageSize);

    /** 模板指定客户列表 */
    List<CouponCustomer> listCustomers(Long templateId);

    /** 覆盖保存模板指定客户（传空数组即清空） */
    void saveCustomers(Long templateId, List<CouponCustomer> customers);

    /**
     * 发放优惠券给指定客户（营销→优惠券「发优惠券」、会员管理「发优惠券」共用）
     *
     * @return 实际发放张数
     */
    int issue(Long templateId, List<Long> partnerIds, Integer quantityPerPartner, String sourceBillNo);

    /** 作废券模板：模板置 VOID，并把该券下「未使用」的券一并作废 */
    void voidTemplate(Long templateId);

    /** 作废单张券（领用明细行级；仅未使用的券可作废） */
    void voidCoupon(Long couponId);

    /** 券模板统计（总数 / 已领取 / 已使用 / 未领取） */
    Map<String, Object> stat(Long templateId);
}
