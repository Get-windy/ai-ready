package cn.aiedge.erp.stock.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 商品单位组（商品多单位换算模板）
 *
 * <p>对标 ql361「商品辅助资料 → 商品单位 → 单位组管理」实测（2026-09-11）：
 * 单位组 = 小单位 / 中单位 / 大单位 三个单位 + 各自相对小单位的换算关系；
 * 列表展示「单位」（单位名逗号串，如 袋,提,箱）与「单位关系」（换算关系冒号串，如 1:12:48）。
 * 对标既无组名列也无备注列（新增表单亦无名称输入项），故本实体不承载名称类字段，
 * 明细见 {@link ProductUnitGroupItem}。
 *
 * <p>单位组成员通过 unit_id 引用 {@link ProductUnitDict}，不复制单位字典（P0 单一口径）。
 */
@Data
@Accessors(chain = true)
@TableName("erp_product_unit_group")
public class ProductUnitGroup {

    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 租户ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long tenantId;

    /** 状态: 1启用 0停用 */
    private Integer status;

    /** 是否删除 */
    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
