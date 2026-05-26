package cn.aiedge.crm.marketing.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@TableName("crm_marketing_content")
public class MarketingContent {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    private String contentCode;

    private String contentName;

    private Integer contentType;

    private String contentTitle;

    private String contentBody;

    private String contentUrl;

    private String contentImage;

    private String contentVideo;

    private String contentAttachment;

    private Integer contentCategory;

    private String description;

    private Boolean active;

    private Integer usageCount;

    private LocalDateTime lastUsedTime;

    private Long lastUsedBy;

    private String remark;

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