package cn.aiedge.erp.supplier.service;

import cn.aiedge.erp.supplier.model.entity.SupplierLevelEntity;
import cn.aiedge.erp.supplier.model.entity.SupplierPerformanceEntity;
import cn.aiedge.erp.supplier.model.entity.InquiryQuotationEntity;
import cn.aiedge.erp.supplier.model.entity.SupplierPointsRecordEntity;

import java.util.List;
import java.util.Map;

public interface SupplierPortalService {
    
    List<SupplierLevelEntity> getActiveLevels();
    
    SupplierLevelEntity getLevelByScore(Double score);
    
    SupplierLevelEntity createLevel(SupplierLevelEntity level);
    
    SupplierLevelEntity updateLevel(Long id, SupplierLevelEntity level);
    
    void deleteLevel(Long id);
    
    SupplierPerformanceEntity createPerformance(SupplierPerformanceEntity performance);
    
    SupplierPerformanceEntity getPerformance(Long id);
    
    List<SupplierPerformanceEntity> getSupplierPerformances(Long supplierId);
    
    SupplierPerformanceEntity getLatestPerformance(Long supplierId, String period);
    
    Double getAveragePerformanceScore(Long supplierId);
    
    Map<String, Object> calculatePerformanceSummary(Long supplierId);
    
    InquiryQuotationEntity createInquiry(InquiryQuotationEntity inquiry);
    
    InquiryQuotationEntity getInquiry(Long id);
    
    List<InquiryQuotationEntity> getSupplierInquiries(Long supplierId);
    
    InquiryQuotationEntity submitQuotation(Long inquiryId, InquiryQuotationEntity quotation);
    
    InquiryQuotationEntity acceptQuotation(Long quotationId);
    
    InquiryQuotationEntity rejectQuotation(Long quotationId, String reason);
    
    List<InquiryQuotationEntity> getPendingInquiries();
    
    SupplierPointsRecordEntity addPoints(Long supplierId, Integer points, String reason, String ruleId);
    
    SupplierPointsRecordEntity consumePoints(Long supplierId, Integer points, String reason);
    
    Integer getTotalPoints(Long supplierId);
    
    List<SupplierPointsRecordEntity> getPointsRecords(Long supplierId);
    
    Map<String, Object> getSupplierDashboard(Long supplierId);
}