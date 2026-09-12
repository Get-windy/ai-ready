package cn.aiedge.erp.party.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 「全部联系人」子标签行（联系人 × 归属客户联表结果）
 * <p>
 * 对标 ql361 客户页 → 全部联系人 子标签列：
 * 姓名 / 性别 / 对应客户 / 客户编号 / 客户所属区域 / 客户经手人 / 职务 / 手机 /
 * 联系地址 / 配送方式 / 物流公司 / 网点。
 * </p>
 */
@Data
public class PartyContactRow {

    private Long id;

    private Long partyId;

    /** 关联的独立联系人 biz_contact.id */
    private Long contactId;

    /** 姓名 */
    private String contactName;

    /** 性别 */
    private String gender;

    /** 职务 */
    private String position;

    /** 手机 */
    private String mobile;

    private String phone;

    /** 联系地址 */
    private String detailAddress;

    /** 配送方式 */
    private String deliveryMethod;

    /** 配送线路 */
    private String deliveryRoute;

    /** 物流公司 */
    private String logisticsCompany;

    /** 网点（网点场景 contact_name 存网点名） */
    private String outletName;

    private Integer isPrimary;

    private Integer status;

    // ── 归属客户（联表带出） ──
    /** 对应客户 */
    private String partnerName;

    /** 客户编号 */
    private String partnerCode;

    /** 客户所属区域 */
    private String partyRegion;

    /** 客户经手人 */
    private String handlerName;

    private LocalDateTime createTime;
}
