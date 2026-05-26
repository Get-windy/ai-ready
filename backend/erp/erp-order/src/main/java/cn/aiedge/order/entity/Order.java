package cn.aiedge.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单主表实体
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
@TableName("erp_order")
public class Order {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 订单编号 */
    private String orderNo;

    /** 客户ID */
    private Long customerId;

    /** 客户名称 */
    private String customerName;

    /** 订单类型：1-销售订单 2-采购订单 3-退货订单 */
    private Integer orderType;

    /** 订单状态：0-草稿 1-待审核 2-已审核 3-已发货 4-已完成 5-已取消 6-已退货 */
    private Integer status;

    /** 订单来源：1-手动创建 2-线上下单 3-转单 */
    private Integer source;

    /** 销售人员ID */
    private Long saleId;

    /** 销售人员名称 */
    private String saleName;

    /** 部门ID */
    private Long deptId;

    /** 订单总金额 */
    private BigDecimal totalAmount;

    /** 实际金额（折扣后） */
    private BigDecimal actualAmount;

    /** 优惠金额 */
    private BigDecimal discountAmount;

    /** 已收金额 */
    private BigDecimal receivedAmount;

    /** 订单日期 */
    private LocalDateTime orderDate;

    /** 预计发货日期 */
    private LocalDateTime expectedShipDate;

    /** 实际发货日期 */
    private LocalDateTime actualShipDate;

    /** 收货人 */
    private String receiverName;

    /** 收货电话 */
    private String receiverPhone;

    /** 收货省份 */
    private String receiverProvince;

    /** 收货城市 */
    private String receiverCity;

    /** 收货区县 */
    private String receiverDistrict;

    /** 收货详细地址 */
    private String receiverAddress;

    /** 备注 */
    private String remark;

    /** 审核人ID */
    private Long auditId;

    /** 审核人名称 */
    private String auditName;

    /** 审核时间 */
    private LocalDateTime auditTime;

    /** 审核备注 */
    private String auditRemark;

    /** 扩展信息 */
    @TableField(typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    private String extInfo;

    /** 是否删除 */
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
}
