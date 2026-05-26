package cn.aiedge.erp.supplier.service.impl;

import cn.aiedge.erp.supplier.mapper.*;
import cn.aiedge.erp.supplier.model.entity.*;
import cn.aiedge.erp.supplier.service.SupplierPortalService;
import cn.aiedge.common.core.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class SupplierPortalServiceImpl implements SupplierPortalService {
    
    private final SupplierLevelMapper levelMapper;
    private final SupplierPerformanceMapper performanceMapper;
    private final InquiryQuotationMapper inquiryQuotationMapper;
    private final SupplierPointsRecordMapper pointsRecordMapper;
    private final SupplierRepository supplierRepository;
    
    private String getCurrentTenantId() {
        return SecurityUtils.getCurrentTenantId();
    }
    
    private String getCurrentUserId() {
        return SecurityUtils.getCurrentUserId();
    }
    
    @Override
    public List<SupplierLevelEntity> getActiveLevels() {
        return levelMapper.findActiveLevels(getCurrentTenantId());
    }
    
    @Override
    public SupplierLevelEntity getLevelByScore(Double score) {
        return levelMapper.findLevelByScore(getCurrentTenantId(), score);
    }
    
    @Override
    @Transactional
    public SupplierLevelEntity createLevel(SupplierLevelEntity level) {
        level.setTenantId(getCurrentTenantId());
        level.setCreatedTime(LocalDateTime.now());
        level.setCreatedBy(getCurrentUserId());
        level.setIsActive(true);
        level.setDeleted(0);
        level.setVersion(1);
        levelMapper.insert(level);
        return level;
    }
    
    @Override
    @Transactional
    public SupplierLevelEntity updateLevel(Long id, SupplierLevelEntity level) {
        SupplierLevelEntity existing = levelMapper.selectById(id);
        if (existing == null) {
            throw new RuntimeException("供应商等级不存在");
        }
        
        level.setId(id);
        level.setUpdatedTime(LocalDateTime.now());
        level.setUpdatedBy(getCurrentUserId());
        levelMapper.updateById(level);
        return level;
    }
    
    @Override
    @Transactional
    public void deleteLevel(Long id) {
        SupplierLevelEntity level = levelMapper.selectById(id);
        if (level != null) {
            level.setDeleted(1);
            level.setUpdatedTime(LocalDateTime.now());
            level.setUpdatedBy(getCurrentUserId());
            levelMapper.updateById(level);
        }
    }
    
    @Override
    @Transactional
    public SupplierPerformanceEntity createPerformance(SupplierPerformanceEntity performance) {
        performance.setTenantId(getCurrentTenantId());
        performance.setCreateTime(LocalDateTime.now());
        performance.setCreateBy(getCurrentUserId());
        performance.setEvaluationDate(LocalDateTime.now());
        performance.setDeleted(0);
        performance.setVersion(1);
        
        Double comprehensiveScore = calculateComprehensiveScore(performance);
        performance.setComprehensiveScore(comprehensiveScore);
        
        SupplierLevelEntity level = getLevelByScore(comprehensiveScore);
        if (level != null) {
            performance.setPerformanceLevel(level.getLevelName());
        }
        
        performanceMapper.insert(performance);
        return performance;
    }
    
    private Double calculateComprehensiveScore(SupplierPerformanceEntity performance) {
        double total = 0;
        int count = 0;
        
        if (performance.getQualityScore() != null) { total += performance.getQualityScore() * 0.3; count++; }
        if (performance.getDeliveryScore() != null) { total += performance.getDeliveryScore() * 0.2; count++; }
        if (performance.getPriceScore() != null) { total += performance.getPriceScore() * 0.15; count++; }
        if (performance.getServiceScore() != null) { total += performance.getServiceScore() * 0.15; count++; }
        if (performance.getTechnologyScore() != null) { total += performance.getTechnologyScore() * 0.1; count++; }
        if (performance.getResponseScore() != null) { total += performance.getResponseScore() * 0.05; count++; }
        if (performance.getComplianceScore() != null) { total += performance.getComplianceScore() * 0.05; count++; }
        
        return count > 0 ? total : 0;
    }
    
    @Override
    public SupplierPerformanceEntity getPerformance(Long id) {
        return performanceMapper.selectById(id);
    }
    
    @Override
    public List<SupplierPerformanceEntity> getSupplierPerformances(Long supplierId) {
        return performanceMapper.findBySupplierId(getCurrentTenantId(), supplierId);
    }
    
    @Override
    public SupplierPerformanceEntity getLatestPerformance(Long supplierId, String period) {
        return performanceMapper.findLatestByPeriod(getCurrentTenantId(), supplierId, period);
    }
    
    @Override
    public Double getAveragePerformanceScore(Long supplierId) {
        return performanceMapper.getAverageScore(getCurrentTenantId(), supplierId);
    }
    
    @Override
    public Map<String, Object> calculatePerformanceSummary(Long supplierId) {
        Map<String, Object> summary = new HashMap<>();
        
        List<SupplierPerformanceEntity> performances = getSupplierPerformances(supplierId);
        
        if (performances.isEmpty()) {
            summary.put("totalEvaluations", 0);
            summary.put("averageScore", 0);
            return summary;
        }
        
        double avgQuality = performances.stream().filter(p -> p.getQualityScore() != null).mapToDouble(SupplierPerformanceEntity::getQualityScore).average().orElse(0);
        double avgDelivery = performances.stream().filter(p -> p.getDeliveryScore() != null).mapToDouble(SupplierPerformanceEntity::getDeliveryScore).average().orElse(0);
        double avgPrice = performances.stream().filter(p -> p.getPriceScore() != null).mapToDouble(SupplierPerformanceEntity::getPriceScore).average().orElse(0);
        double avgService = performances.stream().filter(p -> p.getServiceScore() != null).mapToDouble(SupplierPerformanceEntity::getServiceScore).average().orElse(0);
        double avgComprehensive = performances.stream().filter(p -> p.getComprehensiveScore() != null).mapToDouble(SupplierPerformanceEntity::getComprehensiveScore).average().orElse(0);
        
        summary.put("totalEvaluations", performances.size());
        summary.put("averageQualityScore", avgQuality);
        summary.put("averageDeliveryScore", avgDelivery);
        summary.put("averagePriceScore", avgPrice);
        summary.put("averageServiceScore", avgService);
        summary.put("averageComprehensiveScore", avgComprehensive);
        summary.put("currentLevel", performances.get(0).getPerformanceLevel());
        
        return summary;
    }
    
    @Override
    @Transactional
    public InquiryQuotationEntity createInquiry(InquiryQuotationEntity inquiry) {
        inquiry.setTenantId(getCurrentTenantId());
        inquiry.setInquiryNo(generateInquiryNo());
        inquiry.setCreateTime(LocalDateTime.now());
        inquiry.setCreateBy(getCurrentUserId());
        inquiry.setInquirySentDate(LocalDateTime.now());
        inquiry.setInquiryStatus(1);
        inquiry.setQuotationStatus(0);
        inquiry.setStatus(1);
        inquiry.setDeleted(0);
        inquiry.setVersion(1);
        
        inquiryQuotationMapper.insert(inquiry);
        return inquiry;
    }
    
    private String generateInquiryNo() {
        return "INQ" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    }
    
    private String generateQuotationNo() {
        return "QUO" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    }
    
    @Override
    public InquiryQuotationEntity getInquiry(Long id) {
        return inquiryQuotationMapper.selectById(id);
    }
    
    @Override
    public List<InquiryQuotationEntity> getSupplierInquiries(Long supplierId) {
        return inquiryQuotationMapper.findBySupplierId(getCurrentTenantId(), supplierId);
    }
    
    @Override
    @Transactional
    public InquiryQuotationEntity submitQuotation(Long inquiryId, InquiryQuotationEntity quotation) {
        InquiryQuotationEntity inquiry = inquiryQuotationMapper.selectById(inquiryId);
        if (inquiry == null) {
            throw new RuntimeException("询价单不存在");
        }
        
        inquiry.setQuotationNo(generateQuotationNo());
        inquiry.setQuotationPrice(quotation.getQuotationPrice());
        inquiry.setQuotationQuantity(quotation.getQuotationQuantity());
        inquiry.setQuotationTotalAmount(quotation.getQuotationPrice().multiply(quotation.getQuotationQuantity()));
        inquiry.setQuotationValidityPeriod(quotation.getQuotationValidityPeriod());
        inquiry.setQuotationRemark(quotation.getQuotationRemark());
        inquiry.setQuotationAttachments(quotation.getQuotationAttachments());
        inquiry.setQuotationDate(LocalDateTime.now());
        inquiry.setQuotationStatus(1);
        inquiry.setUpdateTime(LocalDateTime.now());
        inquiry.setUpdateBy(getCurrentUserId());
        
        inquiryQuotationMapper.updateById(inquiry);
        return inquiry;
    }
    
    @Override
    @Transactional
    public InquiryQuotationEntity acceptQuotation(Long quotationId) {
        InquiryQuotationEntity quotation = inquiryQuotationMapper.selectById(quotationId);
        if (quotation == null) {
            throw new RuntimeException("报价单不存在");
        }
        
        quotation.setQuotationStatus(2);
        quotation.setInquiryStatus(3);
        quotation.setInquiryConfirmedDate(LocalDateTime.now());
        quotation.setUpdateTime(LocalDateTime.now());
        quotation.setUpdateBy(getCurrentUserId());
        
        inquiryQuotationMapper.updateById(quotation);
        return quotation;
    }
    
    @Override
    @Transactional
    public InquiryQuotationEntity rejectQuotation(Long quotationId, String reason) {
        InquiryQuotationEntity quotation = inquiryQuotationMapper.selectById(quotationId);
        if (quotation == null) {
            throw new RuntimeException("报价单不存在");
        }
        
        quotation.setQuotationStatus(3);
        quotation.setQuotationRemark(reason);
        quotation.setUpdateTime(LocalDateTime.now());
        quotation.setUpdateBy(getCurrentUserId());
        
        inquiryQuotationMapper.updateById(quotation);
        return quotation;
    }
    
    @Override
    public List<InquiryQuotationEntity> getPendingInquiries() {
        return inquiryQuotationMapper.findByInquiryStatus(getCurrentTenantId(), 1);
    }
    
    @Override
    @Transactional
    public SupplierPointsRecordEntity addPoints(Long supplierId, Integer points, String reason, String ruleId) {
        SupplierPointsRecordEntity record = new SupplierPointsRecordEntity();
        record.setTenantId(getCurrentTenantId());
        record.setSupplierId(supplierId);
        record.setPointsAmount(new BigDecimal(points));
        record.setRecordType("EARN");
        record.setRecordDescription(reason);
        record.setRuleCode(ruleId);
        record.setRecordDate(LocalDateTime.now());
        record.setEffectiveTime(LocalDateTime.now());
        record.setCreatedTime(LocalDateTime.now());
        record.setCreatedBy(getCurrentUserId());
        record.setApprovalStatus("APPROVED");
        record.setDeleted(0);
        record.setVersion(1);
        
        pointsRecordMapper.insert(record);
        return record;
    }
    
    @Override
    @Transactional
    public SupplierPointsRecordEntity consumePoints(Long supplierId, Integer points, String reason) {
        BigDecimal totalPoints = getTotalPointsAsBigDecimal(supplierId);
        if (totalPoints.compareTo(new BigDecimal(points)) < 0) {
            throw new RuntimeException("积分不足");
        }
        
        SupplierPointsRecordEntity record = new SupplierPointsRecordEntity();
        record.setTenantId(getCurrentTenantId());
        record.setSupplierId(supplierId);
        record.setPointsAmount(new BigDecimal(-points));
        record.setRecordType("CONSUME");
        record.setRecordDescription(reason);
        record.setRecordDate(LocalDateTime.now());
        record.setEffectiveTime(LocalDateTime.now());
        record.setCreatedTime(LocalDateTime.now());
        record.setCreatedBy(getCurrentUserId());
        record.setApprovalStatus("APPROVED");
        record.setDeleted(0);
        record.setVersion(1);
        
        pointsRecordMapper.insert(record);
        return record;
    }
    
    private BigDecimal getTotalPointsAsBigDecimal(Long supplierId) {
        return new BigDecimal(getTotalPoints(supplierId));
    }
    
    @Override
    public Integer getTotalPoints(Long supplierId) {
        List<SupplierPointsRecordEntity> records = pointsRecordMapper.findBySupplierId(getCurrentTenantId(), supplierId);
        
        BigDecimal total = BigDecimal.ZERO;
        for (SupplierPointsRecordEntity record : records) {
            if (record.getPointsAmount() != null) {
                total = total.add(record.getPointsAmount());
            }
        }
        
        return total.intValue();
    }
    
    @Override
    public List<SupplierPointsRecordEntity> getPointsRecords(Long supplierId) {
        return pointsRecordMapper.findBySupplierId(getCurrentTenantId(), supplierId);
    }
    
    @Override
    public Map<String, Object> getSupplierDashboard(Long supplierId) {
        Map<String, Object> dashboard = new HashMap<>();
        
        dashboard.put("performanceSummary", calculatePerformanceSummary(supplierId));
        dashboard.put("totalPoints", getTotalPoints(supplierId));
        
        List<InquiryQuotationEntity> inquiries = getSupplierInquiries(supplierId);
        dashboard.put("totalInquiries", inquiries.size());
        dashboard.put("pendingInquiries", inquiries.stream().filter(i -> i.getInquiryStatus() == 1).count());
        dashboard.put("acceptedQuotations", inquiries.stream().filter(i -> i.getQuotationStatus() == 2).count());
        
        return dashboard;
    }
}