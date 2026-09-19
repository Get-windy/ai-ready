package cn.aiedge.erp.pricing.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 价格审批单。
 *
 * <p>前端契约见 `frontend/apps/pc-admin/src/api/pricing-approval.ts` 的 {@code PriceApproval} 接口，
 * 字段一一对应（驼峰同名）。此前后端零实现，导致 `views/erp/pricing/approval/index.vue`
 * 的全部请求 404；本类与 {@code PriceApprovalController}、迁移 V11.424.0 一起补齐。</p>
 *
 * <p>审批状态流转：{@code pending → approved | rejected}，单向不可逆。</p>
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
@TableName("erp_price_approval")
public class PriceApproval {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 租户 ID —— 由租户拦截器自动注入，不要手工赋值 */
    private Long tenantId;

    private Long productId;
    private String productName;
    private String productCode;

    /** 为空表示「不针对特定客户」的通用调价 */
    private Long customerId;
    private String customerName;

    /** 变更前价格（申请时从商品/客户价快照而来） */
    private BigDecimal oldPrice;
    private BigDecimal newPrice;
    /** newPrice - oldPrice，可能为负 */
    private BigDecimal priceChange;
    /** increase | decrease */
    private String priceChangeType;

    private String approvalType;
    private String approvalTypeLabel;

    private Long applicantId;
    private String applicantName;
    private LocalDateTime applyTime;

    /** pending | approved | rejected */
    private String status;

    private Long approverId;
    private String approverName;
    private LocalDateTime approveTime;

    private String approvalReason;
    private String approveRemark;

    /** 调价生效区间（来自申请单，可为空） */
    private LocalDate effectiveStart;
    private LocalDate effectiveEnd;

    @TableLogic
    private Integer deleted;

    private Long createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    private Long updateBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
