package cn.aiedge.erp.budget.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 预算模板科目明细
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "budget_template_item")
public class BudgetTemplateItem extends BaseEntity {

    @Column(name = "template_id", nullable = false)
    private Long templateId;

    @Column(name = "subject_code", length = 50)
    private String subjectCode;

    @Column(name = "subject_name", length = 200)
    private String subjectName;

    @Column(name = "budget_amount", precision = 15, scale = 2)
    private BigDecimal budgetAmount;

    @Column(name = "sort_order")
    private Integer sortOrder;
}
