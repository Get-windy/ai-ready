package cn.aiedge.erp.stock.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 盘点单（库存盘点录单，单号前缀 KCPDD-）
 * 保存 -> 盘点处理（按盈亏生成报损单/报溢单）
 */
@Data
@Accessors(chain = true)
@TableName("erp_stock_take")
public class StockTake {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long tenantId;
    private String stockTakeNo;
    private LocalDate stockTakeDate;
    /** 盘点方式：1 手工录入，2 扫码盘点，3 快速盘点 */
    private Integer checkMethod;
    /** 盘点类型：1 全面盘点，2 抽盘，3 动态盘点 */
    private Integer checkType;
    private Long warehouseId;
    private String warehouseName;
    /** 库区 */
    private String regionName;
    private Long handlerId;
    private String handlerName;
    private Long deptId;
    private String deptName;
    /** 盈亏数量合计 */
    private BigDecimal totalDiffQuantity;
    /** 盈亏金额合计 */
    private BigDecimal totalDiffAmount;
    private Integer totalItems;
    /** 单据状态：1 已保存，2 已盘点(处理完成) */
    private Integer status;
    /** 关联盘点单号（由其它单据/盘点派生，可为空） */
    private String linkedBillNo;
    private String summary;
    private String remark;
    private String attachment;
    private Long bookkeeperId;
    private String bookkeeperName;
    private LocalDateTime bookkeepingTime;
    private String creatorName;
    private Integer printCount;
    /** 盘点处理结果：已生成报损单/报溢单时的描述 */
    private String processResult;
    private Long applicantId;
    private String applicantName;
    private LocalDateTime applyTime;
    private Long approvedBy;
    private LocalDateTime approvedTime;
    private String approvedNote;
    private Long executedBy;
    private LocalDateTime executedTime;
    private String cancelReason;
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
    @TableField(exist = false)
    private List<StockTakeItem> items;
}
