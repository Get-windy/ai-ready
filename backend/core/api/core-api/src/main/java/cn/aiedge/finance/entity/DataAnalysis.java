package cn.aiedge.finance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 数据分析实体
 */
@Data
@TableName("fin_data_analysis")
public class DataAnalysis {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;
    private String analysisCode; // 分析编码
    private String analysisName; // 分析名称
    private String analysisType; // 分析类型 (FINANCIAL-财务分析, OPERATIONAL-运营分析, SALES-销售分析, CUSTOMER-客户分析)
    private String analysisCategory; // 分析分类 (RATIO-比率分析, TREND-趋势分析, COMPARATIVE-对比分析, PREDICTIVE-预测分析)
    
    private LocalDate startDate; // 分析开始日期
    private LocalDate endDate; // 分析结束日期
    private String period; // 分析期间
    
    private String dataSource; // 数据来源
    private String analysisMethod; // 分析方法
    private String analysisFormula; // 分析公式
    private String parameters; // 分析参数 (JSON格式)
    
    private String status; // 状态 (DRAFT-草稿, PROCESSING-处理中, COMPLETED-已完成, FAILED-失败)
    private String executionResult; // 执行结果 (JSON格式)
    private String visualizationData; // 可视化数据 (JSON格式)
    
    private String analyzer; // 分析员
    private String analysisNotes; // 分析备注
    
    private BigDecimal confidenceLevel; // 置信度
    private String recommendation; // 分析建议
    
    @TableField(fill = FieldFill.INSERT)
    private String createBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updateBy;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}
