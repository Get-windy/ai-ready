package cn.aiedge.erp.finance.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 财务报表实体
 * 存储财务报表模板和数据
 */
@Data
@TableName("finance_report")
@EqualsAndHashCode(callSuper = true)
public class FinanceReport extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 报表编号
     */
    @TableField("report_no")
    private String reportNo;

    /**
     * 报表名称
     */
    @TableField("report_name")
    private String reportName;

    /**
     * 报表类型
     * 1-资产负债表 2-利润表 3-现金流量表 4-所有者权益变动表 5-其他
     */
    @TableField("report_type")
    private Integer reportType;

    /**
     * 报表期间
     * 格式: YYYY-MM
     */
    @TableField("report_period")
    private String reportPeriod;

    /**
     * 报表状态
     * 0-草稿 1-已生成 2-已审核
     */
    @TableField("status")
    private Integer status = 0;

    /**
     * 报表数据 (JSON格式)
     */
    @TableField("report_data")
    private String reportData;

    /**
     * 报表文件URL
     */
    @TableField("file_url")
    private String fileUrl;

    /**
     * 生成人ID
     */
    @TableField("generated_by")
    private String generatedBy;

    /**
     * 生成时间
     */
    @TableField("generated_at")
    private LocalDateTime generatedAt;

    /**
     * 审核人ID
     */
    @TableField("approved_by")
    private String approvedBy;

    /**
     * 审核时间
     */
    @TableField("approved_at")
    private LocalDateTime approvedAt;
}
