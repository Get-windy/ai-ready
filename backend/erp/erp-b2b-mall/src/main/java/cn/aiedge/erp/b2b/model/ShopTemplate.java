package cn.aiedge.erp.b2b.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商城页面模板
 */
@Data
@TableName("shop_template")
public class ShopTemplate {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 模板名称 */
    private String templateName;

    /** 模板编码 */
    private String templateCode;

    /** 缩略图 */
    private String thumbnail;

    /** 描述 */
    private String description;

    /** 模板配置JSON */
    private String configJson;

    /** 是否默认 */
    private Integer isDefault;

    /** 1启用 0停用 */
    private Integer status;

    @TableLogic
    private Integer deleted;

    private Long createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    private Long updateBy;

    @TableField(fill = FieldFill.UPDATE)
    private LocalDateTime updateTime;
}
