package cn.aiedge.erp.finance.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 月结操作日志实体
 * action: close-月结 reopen-反月结
 */
@Data
@TableName("fin_month_closing_log")
public class MonthClosingLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 租户ID
     */
    @TableField("tenant_id")
    private Long tenantId;

    /**
     * 期间编码，格式 yyyy-MM
     */
    @TableField("period_code")
    private String periodCode;

    /**
     * 操作类型：close-月结 reopen-反月结
     */
    @TableField("action")
    private String action;

    /**
     * 操作人ID
     */
    @TableField("operator_id")
    private String operatorId;

    /**
     * 操作人姓名
     */
    @TableField("operator_name")
    private String operatorName;

    /**
     * 月结检查结果快照（JSON字符串）
     */
    @TableField("check_result")
    private String checkResult;

    /**
     * 操作时间
     */
    @TableField("create_time")
    private LocalDateTime createTime;
}
