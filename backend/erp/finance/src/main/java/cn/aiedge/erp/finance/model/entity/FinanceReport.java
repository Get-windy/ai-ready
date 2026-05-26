package cn.aiedge.erp.finance.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 财务报表实体
 * 存储财务报表模板和数据
 */
@Data
@Entity
@Table(name = "finance_report")
@EqualsAndHashCode(callSuper = true)
public class FinanceReport extends BaseEntity {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 报表编号
     */
    @Column(name = "report_no", nullable = false, unique = true, length = 32)
    private String reportNo;
    
    /**
     * 报表名称
     */
    @Column(name = "report_name", nullable = false, length = 100)
    private String reportName;
    
    /**
     * 报表类型
     * 1-资产负债表 2-利润表 3-现金流量表 4-所有者权益变动表 5-其他
     */
    @Column(name = "report_type", nullable = false)
    private Integer reportType;
    
    /**
     * 报表期间
     * 格式: YYYY-MM
     */
    @Column(name = "report_period", nullable = false, length = 7)
    private String reportPeriod;
    
    /**
     * 报表状态
     * 0-草稿 1-已生成 2-已审核
     */
    @Column(name = "status", nullable = false)
    private Integer status = 0;
    
    /**
     * 报表数据 (JSON格式)
     */
    @Column(name = "report_data", columnDefinition = "TEXT")
    private String reportData;
    
    /**
     * 报表文件URL
     */
    @Column(name = "file_url", length = 500)
    private String fileUrl;
    
    /**
     * 生成人ID
     */
    @Column(name = "generated_by", length = 64)
    private String generatedBy;
    
    /**
     * 生成时间
     */
    @Column(name = "generated_at")
    private LocalDateTime generatedAt;
    
    /**
     * 审核人ID
     */
    @Column(name = "approved_by", length = 64)
    private String approvedBy;
    
    /**
     * 审核时间
     */
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;
}
