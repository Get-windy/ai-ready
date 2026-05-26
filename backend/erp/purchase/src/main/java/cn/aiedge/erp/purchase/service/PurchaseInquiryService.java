package cn.aiedge.erp.purchase.service;

import cn.aiedge.erp.purchase.entity.PurchaseInquiry;
import java.util.List;

/**
 * 询价单Service接口
 */
public interface PurchaseInquiryService {

    /**
     * 创建询价单
     */
    PurchaseInquiry createInquiry(PurchaseInquiry inquiry);

    /**
     * 更新询价单
     */
    PurchaseInquiry updateInquiry(Long id, PurchaseInquiry inquiry);

    /**
     * 发布询价单
     */
    PurchaseInquiry publishInquiry(Long id);

    /**
     * 关闭询价单
     */
    PurchaseInquiry closeInquiry(Long id);

    /**
     * 关闭询价单（带原因）
     */
    PurchaseInquiry closeInquiry(Long id, String reason);

    /**
     * 取消询价单
     */
    PurchaseInquiry cancelInquiry(Long id);

    /**
     * 检查是否可以接受报价
     */
    boolean canAcceptQuote(Long id);

    /**
     * 根据ID查询询价单
     */
    PurchaseInquiry getInquiryById(Long id);

    /**
     * 根据编号查询询价单
     */
    PurchaseInquiry getInquiryByNo(String inquiryNo);

    /**
     * 查询所有询价单
     */
    List<PurchaseInquiry> getAllInquiries();

    /**
     * 根据状态查询询价单
     */
    List<PurchaseInquiry> getInquiriesByStatus(String status);

    /**
     * 根据采购员查询询价单
     */
    List<PurchaseInquiry> getInquiriesByPurchaser(Long purchaserId);

    /**
     * 删除询价单
     */
    void deleteInquiry(Long id);

    /**
     * 生成询价单编号
     */
    String generateInquiryNo();
}