package cn.aiedge.erp.invoice.model.dto;

import cn.aiedge.erp.invoice.model.entity.Invoice;
import cn.aiedge.erp.invoice.model.enums.MatchingStatus;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 匹配统计信息DTO
 */
public class MatchingStatistics {
    
    private int totalInvoices = 0;
    private int unmatchedCount = 0;
    private int partiallyMatchedCount = 0;
    private int fullyMatchedCount = 0;
    private int matchingErrorCount = 0;
    private int matchingCancelledCount = 0;
    
    private BigDecimal unmatchedAmount = BigDecimal.ZERO;
    private BigDecimal partiallyMatchedAmount = BigDecimal.ZERO;
    private BigDecimal fullyMatchedAmount = BigDecimal.ZERO;
    private BigDecimal totalAmount = BigDecimal.ZERO;
    
    private int autoMatches = 0;
    private int manualMatches = 0;
    
    private List<String> errorMessages = new ArrayList<>();
    private List<String> warningMessages = new ArrayList<>();
    
    /**
     * 添加发票到统计中
     */
    public void addInvoice(Invoice invoice) {
        if (invoice == null) {
            return;
        }
        
        totalInvoices++;
        totalAmount = totalAmount.add(invoice.getTotalAmount() != null ? invoice.getTotalAmount() : BigDecimal.ZERO);
        
        MatchingStatus status = invoice.getMatchingStatus();
        BigDecimal matchedAmount = invoice.getMatchedAmount() != null ? invoice.getMatchedAmount() : BigDecimal.ZERO;
        BigDecimal totalAmount = invoice.getTotalAmount() != null ? invoice.getTotalAmount() : BigDecimal.ZERO;
        BigDecimal unmatchedAmount = totalAmount.subtract(matchedAmount).max(BigDecimal.ZERO);
        
        switch (status) {
            case UNMATCHED:
                unmatchedCount++;
                this.unmatchedAmount = this.unmatchedAmount.add(unmatchedAmount);
                break;
            case PARTIALLY_MATCHED:
                partiallyMatchedCount++;
                this.partiallyMatchedAmount = this.partiallyMatchedAmount.add(matchedAmount);
                this.unmatchedAmount = this.unmatchedAmount.add(unmatchedAmount);
                break;
            case FULLY_MATCHED:
                fullyMatchedCount++;
                this.fullyMatchedAmount = this.fullyMatchedAmount.add(matchedAmount);
                break;
            case MATCHING_ERROR:
                matchingErrorCount++;
                break;
            case MATCHING_CANCELLED:
                matchingCancelledCount++;
                break;
        }
    }
    
    /**
     * 计算匹配率
     */
    public double getMatchingRate() {
        if (totalInvoices == 0) {
            return 0.0;
        }
        return (double) (fullyMatchedCount + partiallyMatchedCount) / totalInvoices * 100;
    }
    
    /**
     * 计算完全匹配率
     */
    public double getFullMatchingRate() {
        if (totalInvoices == 0) {
            return 0.0;
        }
        return (double) fullyMatchedCount / totalInvoices * 100;
    }
    
    /**
     * 计算金额匹配率
     */
    public double getAmountMatchingRate() {
        if (totalAmount.compareTo(BigDecimal.ZERO) == 0) {
            return 0.0;
        }
        BigDecimal matchedAmount = fullyMatchedAmount.add(partiallyMatchedAmount);
        return matchedAmount.divide(totalAmount, 4, java.math.RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100")).doubleValue();
    }
    
    /**
     * 添加错误消息
     */
    public void addError(String error) {
        errorMessages.add(error);
    }
    
    /**
     * 添加警告消息
     */
    public void addWarning(String warning) {
        warningMessages.add(warning);
    }
    
    // Getters and Setters
    
    public int getTotalInvoices() {
        return totalInvoices;
    }
    
    public void setTotalInvoices(int totalInvoices) {
        this.totalInvoices = totalInvoices;
    }
    
    public int getUnmatchedCount() {
        return unmatchedCount;
    }
    
    public void setUnmatchedCount(int unmatchedCount) {
        this.unmatchedCount = unmatchedCount;
    }
    
    public int getPartiallyMatchedCount() {
        return partiallyMatchedCount;
    }
    
    public void setPartiallyMatchedCount(int partiallyMatchedCount) {
        this.partiallyMatchedCount = partiallyMatchedCount;
    }
    
    public int getFullyMatchedCount() {
        return fullyMatchedCount;
    }
    
    public void setFullyMatchedCount(int fullyMatchedCount) {
        this.fullyMatchedCount = fullyMatchedCount;
    }
    
    public int getMatchingErrorCount() {
        return matchingErrorCount;
    }
    
    public void setMatchingErrorCount(int matchingErrorCount) {
        this.matchingErrorCount = matchingErrorCount;
    }
    
    public int getMatchingCancelledCount() {
        return matchingCancelledCount;
    }
    
    public void setMatchingCancelledCount(int matchingCancelledCount) {
        this.matchingCancelledCount = matchingCancelledCount;
    }
    
    public BigDecimal getUnmatchedAmount() {
        return unmatchedAmount;
    }
    
    public void setUnmatchedAmount(BigDecimal unmatchedAmount) {
        this.unmatchedAmount = unmatchedAmount;
    }
    
    public BigDecimal getPartiallyMatchedAmount() {
        return partiallyMatchedAmount;
    }
    
    public void setPartiallyMatchedAmount(BigDecimal partiallyMatchedAmount) {
        this.partiallyMatchedAmount = partiallyMatchedAmount;
    }
    
    public BigDecimal getFullyMatchedAmount() {
        return fullyMatchedAmount;
    }
    
    public void setFullyMatchedAmount(BigDecimal fullyMatchedAmount) {
        this.fullyMatchedAmount = fullyMatchedAmount;
    }
    
    public BigDecimal getTotalAmount() {
        return totalAmount;
    }
    
    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }
    
    public int getAutoMatches() {
        return autoMatches;
    }
    
    public void setAutoMatches(int autoMatches) {
        this.autoMatches = autoMatches;
    }
    
    public int getManualMatches() {
        return manualMatches;
    }
    
    public void setManualMatches(int manualMatches) {
        this.manualMatches = manualMatches;
    }
    
    public List<String> getErrorMessages() {
        return errorMessages;
    }
    
    public void setErrorMessages(List<String> errorMessages) {
        this.errorMessages = errorMessages;
    }
    
    public List<String> getWarningMessages() {
        return warningMessages;
    }
    
    public void setWarningMessages(List<String> warningMessages) {
        this.warningMessages = warningMessages;
    }
    
    @Override
    public String toString() {
        return String.format(
            "MatchingStatistics{totalInvoices=%d, unmatched=%d, partially=%d, fully=%d, error=%d, cancelled=%d, " +
            "unmatchedAmount=%.2f, partiallyMatchedAmount=%.2f, fullyMatchedAmount=%.2f, totalAmount=%.2f, " +
            "autoMatches=%d, manualMatches=%d, matchingRate=%.2f%%}",
            totalInvoices, unmatchedCount, partiallyMatchedCount, fullyMatchedCount, 
            matchingErrorCount, matchingCancelledCount,
            unmatchedAmount, partiallyMatchedAmount, fullyMatchedAmount, totalAmount,
            autoMatches, manualMatches, getMatchingRate()
        );
    }
}