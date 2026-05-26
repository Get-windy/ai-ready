package cn.aiedge.finance.vo;

import lombok.Data;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * 发票查询VO
 */
@Data
public class InvoiceQueryVO {
    
    private String invoiceNo;
    private Integer invoiceType;
    private Integer direction;
    private Integer status;
    private Integer bizType;
    private String sellerName;
    private String sellerTaxNo;
    private String buyerName;
    private String buyerTaxNo;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer certified;
    private Long deptId;
    
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        if (invoiceNo != null) map.put("invoiceNo", invoiceNo);
        if (invoiceType != null) map.put("invoiceType", invoiceType);
        if (direction != null) map.put("direction", direction);
        if (status != null) map.put("status", status);
        if (bizType != null) map.put("bizType", bizType);
        if (sellerName != null) map.put("sellerName", sellerName);
        if (sellerTaxNo != null) map.put("sellerTaxNo", sellerTaxNo);
        if (buyerName != null) map.put("buyerName", buyerName);
        if (buyerTaxNo != null) map.put("buyerTaxNo", buyerTaxNo);
        if (startDate != null) map.put("startDate", startDate);
        if (endDate != null) map.put("endDate", endDate);
        if (certified != null) map.put("certified", certified);
        if (deptId != null) map.put("deptId", deptId);
        return map;
    }
}
