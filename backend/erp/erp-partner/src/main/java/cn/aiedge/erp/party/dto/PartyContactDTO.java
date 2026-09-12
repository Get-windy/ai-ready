package cn.aiedge.erp.party.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
public class PartyContactDTO {

    private Long id;

    private Long partyId;

    /** 名称：客户/供应商场景=联系人姓名；网点场景=网点名称 */
    private String contactName;

    /** 网点场景下的联系人姓名 */
    private String linkman;

    /** 联系地址（网点地址） */
    private String detailAddress;

    private String position;

    private String department;

    private String phone;

    private String mobile;

    private String email;

    private String wechat;

    private String qq;

    private Integer isPrimary;

    private Integer contactRole;

    private Integer status;

    private String remark;

    // ── 联系人扩展字段（与 PartyContact 实体对齐，避免前端提交后被 BeanUtils 丢弃） ──
    private String gender;

    /** 客户所属区域 */
    private String region;

    /** 关联的独立联系人 biz_contact.id */
    private Long contactId;

    /** 生日（来自 biz_contact） */
    private java.time.LocalDate birthday;

    /** 所在地区-省（行政区划 sys_region） */
    private String province;

    /** 所在地区-市 */
    private String city;

    /** 所在地区-区县 */
    private String district;

    private String deliveryMethod;

    private String deliveryRoute;

    private String logisticsCompany;


    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
