package cn.aiedge.erp.stock.entity;

import com.baomidou.mybatisplus.annotation.*;
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
    private Long id;

    private Long tenantId;

    private String tagName;

    private Integer sortOrder;

    @TableLogic
    private Integer deleted;

    private Long createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    private Long updateBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
