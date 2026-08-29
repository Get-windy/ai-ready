package cn.aiedge.quality.entity;

import cn.aiedge.base.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 质量证书(COA)实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("quality_certificate")
public class QualityCertificate extends BaseEntity {

    /** 证书编号 (COA-yyyymmdd-xxxx) */
    private String certificateNo;

    /** 证书类型: COA/COC/ISO/OTHER */
    private String certificateType;

    /** 产品名称 */
    private String productName;

    /** 产品编码 */
    private String productCode;

    /** 批次号 */
    private String batchNo;

    /** 供应商名称 */
    private String supplierName;

    /** 检验日期 */
    private LocalDate inspectionDate;

    /** 发证日期 */
    private LocalDate issueDate;

    /** 有效期至 */
    private LocalDate expiryDate;

    /** 检验结论: QUALIFIED/UNQUALIFIED/CONDITIONAL */
    private String result;

    /** 检验员ID */
    private Long inspectorId;

    /** 检验员姓名 */
    private String inspectorName;

    /** 证书附件URL */
    private String certificateUrl;

    /** 备注 */
    private String remark;

    /** 状态: 0-草稿 1-已生效 2-已过期 3-已撤销 */
    private Integer status;
}
