package cn.aiedge.erp.stock.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 商品图片/图片空间素材实体（统一口径）
 *
 * <p>对标「资料 → 商品管理 → 图片管理」：productId 为空 = 图片空间未匹配素材，
 * 非空 = 已关联到该商品的图片（isMain=1 为主图）。</p>
 */
@Data
@Accessors(chain = true)
@TableName("erp_product_image")
public class ProductImage {

    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long tenantId;

    /** 关联商品ID（NULL=图片空间未匹配素材） */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long productId;

    /** 图片名称（上传原始文件名，自动匹配依据） */
    private String imageName;

    /** 匹配键（去掉 -N 序号后缀的名称） */
    private String matchKey;

    /** 图片访问URL */
    private String imageUrl;

    /** 存储相对路径 */
    private String filePath;

    private Long fileSize;

    private String fileType;

    /** 是否主图 0否 1是 */
    private Integer isMain;

    private Integer sortNo;

    /** 来源：UPLOAD 上传 / AUTO_MATCH 自动匹配 / BIND 选择图片绑定 / MOVE 搬移 */
    private String source;

    private String remark;

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
