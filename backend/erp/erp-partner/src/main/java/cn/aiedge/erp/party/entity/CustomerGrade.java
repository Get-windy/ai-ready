package cn.aiedge.erp.party.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("biz_customer_grade")
public class CustomerGrade {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String gradeCode;

    private String gradeName;

    private Integer gradeLevel;

    private String gradeIcon;

    private String gradeColor;

    private BigDecimal minAmount;

    private BigDecimal maxAmount;

    private BigDecimal minPrice;

    private BigDecimal maxPrice;

    private BigDecimal discountRate;

    private BigDecimal pointRate;

    private BigDecimal creditLimit;

    private Integer creditDays;

    private Boolean freeShipping;

    private BigDecimal freeShippingMinAmount;

    private String birthdayPrivilege;

    private Boolean exclusiveService;

    private Boolean priorityShipping;

    private String description;

    private Integer sortOrder;

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
