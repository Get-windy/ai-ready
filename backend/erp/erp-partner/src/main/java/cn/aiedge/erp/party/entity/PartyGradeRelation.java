package cn.aiedge.erp.party.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("biz_party_grade_relation")
public class PartyGradeRelation {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long partyId;

    private Long gradeId;

    private String gradeCode;

    private String gradeName;

    private BigDecimal totalAmount;

    private Integer totalOrders;

    private LocalDateTime obtainTime;

    private LocalDateTime expireTime;

    private Boolean permanent;

    private Long previousGradeId;

    private LocalDateTime upgradeTime;

    private LocalDateTime downgradeTime;

    private String downgradeReason;

    private Boolean manualAdjust;

    private Long adjustBy;

    private String adjustReason;

    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
