package cn.aiedge.erp.stock.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 商城标签（用户自定义）
 */
@Data
@Accessors(chain = true)
@TableName("erp_mall_tag")
public class MallTag {

    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long tenantId;

    /**
     * 标签标准槽位编码 TAG_1..TAG_20（系统内置槽位，不可改）。
     * 商品侧 erp_product.mall_tags 存的就是该编码；tagName 仅作显示昵称，改名不影响历史数据。
     */
    private String tagCode;

    /** 标签显示名（用户可自定义的昵称，默认「标签N」） */
    private String tagName;

    private Integer sortOrder;

    /** 状态: 1启用 0停用（对标 Tab3 行内「停用」） */
    private Integer status;

    @TableLogic
    private Integer deleted;

    private Long createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    private Long updateBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
