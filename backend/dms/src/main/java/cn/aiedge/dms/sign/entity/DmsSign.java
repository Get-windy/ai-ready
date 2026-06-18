package cn.aiedge.dms.sign.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 签收记录实体
 *
 * 对应数据库 dms_sign 表，记录签收类型、照片、手写签名、定位比对等信息。
 *
 * @author AI-Ready Team
 */
@Data
@TableName("dms_sign")
public class DmsSign {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 关联任务ID */
    private Long taskId;

    /** 签收类型：1-正常签收 2-部分签收 3-拒收 */
    private Integer signType;

    /** 照片URL列表（JSON数组，支持多张） */
    private String photoUrls;

    /** 手写签名图片URL */
    private String signatureUrl;

    /** 签收纬度 */
    private BigDecimal signLat;

    /** 签收经度 */
    private BigDecimal signLng;

    /** 客户实际纬度 */
    private BigDecimal customerLat;

    /** 客户实际经度 */
    private BigDecimal customerLng;

    /** 定位偏差（米） */
    private BigDecimal locationDeviation;

    /** 定位偏差警告：0-正常 1-超限 */
    private Integer locationWarning;

    /** 修正后的客户纬度 */
    private BigDecimal newCustomerLat;

    /** 修正后的客户经度 */
    private BigDecimal newCustomerLng;

    /** 签收备注 */
    private String remark;

    /** 签收时间 */
    private LocalDateTime signTime;

    // ========== 审核字段 ==========

    /** 审核状态：0-待审核 1-通过 2-驳回 */
    private Integer auditStatus;

    /** 审核人 */
    private Long auditBy;

    /** 审核时间 */
    private LocalDateTime auditTime;

    /** 审核意见 */
    private String auditRemark;

    /** 逻辑删除 */
    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    private Long createBy;

    private Long updateBy;

    @Version
    private Integer version;
}
