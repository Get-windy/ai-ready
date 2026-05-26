package cn.aiedge.erp.supplierbatchanalysis.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 批次质量评估实体
 * 用于记录批次的质量评估结果
 *
 * @author team-member
 * @date 2026-04-30
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("supplier_batch_quality_assessment")
public class BatchQualityAssessment {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 批次号
     */
    @TableField("batch_no")
    private String batchNo;

    /**
     * 产品ID
     */
    @TableField("product_id")
    private Long productId;

    /**
     * 产品编码
     */
    @TableField("product_code")
    private String productCode;

    /**
     * 产品名称
     */
    @TableField("product_name")
    private String productName;

    /**
     * 供应商ID
     */
    @TableField("supplier_id")
    private Long supplierId;

    /**
     * 供应商编码
     */
    @TableField("supplier_code")
    private String supplierCode;

    /**
     * 供应商名称
     */
    @TableField("supplier_name")
    private String supplierName;

    /**
     * 评估日期
     */
    @TableField("assessment_date")
    private LocalDateTime assessmentDate;

    /**
     * 批次数量
     */
    @TableField("batch_quantity")
    private BigDecimal batchQuantity;

    /**
     * 检测数量
     */
    @TableField("tested_quantity")
    private BigDecimal testedQuantity;

    /**
     * 合格数量
     */
    @TableField("qualified_quantity")
    private BigDecimal qualifiedQuantity;

    /**
     * 不合格数量
     */
    @TableField("defective_quantity")
    private BigDecimal defectiveQuantity;

    /**
     * 合格率
     */
    @TableField("qualification_rate")
    private BigDecimal qualificationRate;

    /**
     * 质量得分 (0-100)
     */
    @TableField("quality_score")
    private BigDecimal qualityScore;

    /**
     * 缺陷类型统计（JSON格式）
     * 例如: {"外观缺陷": 10, "尺寸偏差": 5, "功能故障": 2}
     */
    @TableField("defect_statistics")
    private String defectStatistics;

    /**
     * 检测报告编号
     */
    @TableField("inspection_report_no")
    private String inspectionReportNo;

    /**
     * 检测员ID
     */
    @TableField("inspector_id")
    private String inspectorId;

    /**
     * 检测员姓名
     */
    @TableField("inspector_name")
    private String inspectorName;

    /**
     * 检测方法/标准
     */
    @TableField("inspection_method")
    private String inspectionMethod;

    /**
     * 质量等级：EXCELLENT-优秀, GOOD-良好, ACCEPTABLE-合格, POOR-较差, UNACCEPTABLE-不合格
     */
    @TableField("quality_level")
    private String qualityLevel;

    /**
     * 评估结论
     */
    @TableField("assessment_conclusion")
    private String assessmentConclusion;

    /**
     * 改进建议
     */
    @TableField("improvement_suggestions")
    private String improvementSuggestions;

    /**
     * 关联供应商绩效评估ID
     */
    @TableField("supplier_performance_id")
    private Long supplierPerformanceId;

    /**
     * 状态：0-待评估, 1-已评估, 2-已确认, 3-已归档
     */
    @TableField("status")
    private Integer status;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField("create_by")
    private String createBy;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField("update_by")
    private String updateBy;

    @Version
    @TableField("version")
    private Integer version;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    @TableField("extend_info")
    private String extendInfo;
}