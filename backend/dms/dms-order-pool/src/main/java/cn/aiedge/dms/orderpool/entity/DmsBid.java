package cn.aiedge.dms.orderpool.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 竞价记录实体
 */
@Data
@TableName("dms_bid")
public class DmsBid {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long poolId;

    private Long taskId;

    private Long riderId;

    private String riderName;

    private BigDecimal bidPrice;

    /** 0-否 1-是 */
    private Integer isWin;

    private LocalDateTime bidTime;

    // === Audit fields ===

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    @Version
    private Integer version;
}
