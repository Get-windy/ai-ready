package cn.aiedge.erp.sale.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 销售订单实体
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
@TableName("erp_sale_order")
public class SaleOrder {

    /**
     * 订单ID（主键）
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 订单号
     */
    private String orderNo;

    /**
     * 客户ID
     */
    private Long customerId;

    /**
     * 客户名称
     */
    private String customerName;

    /**
     * 订单日期
     */
    private LocalDateTime orderDate;

    /**
     * 预计发货日期
     */
    private LocalDateTime expectedShipDate;

    /**
     * 订单状态（0-草稿 1-待审批 2-已审批 3-部分出库 4-完成 5-取消）
     */
    private Integer status;

    /**
     * 订单金额（不含税）
     */
    private BigDecimal totalAmount;

    /**
     * 税额
     */
    private BigDecimal taxAmount;

    /**
     * 订单金额（含税）
     */
    private BigDecimal totalAmountWithTax;

    /**
     * 已收款金额
     */
    private BigDecimal receivedAmount;

    /**
     * 销售员ID
     */
    private Long salesmanId;

    /**
     * 销售员名称
     */
    private String salesmanName;

    /**
     * 部门ID
     */
    private Long deptId;

    /**
     * 仓库ID
     */
    private Long warehouseId;

    /**
     * 收货地址
     */
    private String shippingAddress;

    /**
     * 收货人
     */
    private String receiverName;

    /**
     * 联系电话
     */
    private String receiverPhone;

    /**
     * 备注
     */
    private String remark;

    /**
     * 扩展信息（JSON）
     */
    @TableField(typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    private String extInfo;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer deleted;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 创建人ID
     */
    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    /**
     * 更新人ID
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;
}