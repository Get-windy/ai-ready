package cn.aiedge.erp.signature.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("erp_signature_record")
public class SignatureRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String signatureCode;

    private String orderNo;

    private Long orderId;

    private String signatureType;

    private String signerName;

    private String signerPhone;

    private String signatureData;

    private String photoPath;

    private String photoUrl;

    private String location;

    private String latitude;

    private String longitude;

    private String deviceId;

    private String deliveryPersonId;

    private String deliveryPersonName;

    private LocalDateTime signTime;

    private String integrityHash;

    private Boolean verified;

    private LocalDateTime verifyTime;

    private Integer status;

    private String remark;

    private Long tenantId;

    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}