package cn.aiedge.erp.budget.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 预算模板
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "budget_template")
public class BudgetTemplate extends BaseEntity {

    @Column(name = "template_code", unique = true, length = 50)
    private String templateCode;

    @Column(name = "template_name", nullable = false, length = 200)
    private String templateName;

    @Column(name = "fiscal_year", nullable = false)
    private Integer fiscalYear;

    @Column(name = "total_amount", precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
}
