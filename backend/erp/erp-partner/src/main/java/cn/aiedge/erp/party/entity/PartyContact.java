package cn.aiedge.erp.party.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@TableName("biz_party_contact")
public class PartyContact {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long partyId;

    /** 关联的独立联系人 biz_contact.id（联系人 ↔ 往来单位 多对多） */
    private Long contactId;

    /** 名称：客户/供应商场景=联系人姓名；网点场景=网点名称 */
    private String contactName;

    /** 网点场景下的联系人姓名（网点名称存 contactName，联系人在此） */
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

    // ── 「全部联系人」子标签列（V11.151.0） ──
    /** 性别 */
    private String gender;

    /** 客户所属区域 */
    private String region;

    // ── 联系人所在地区（省/市/区县，来源 sys_region 行政区划三级联动） ──
    /** 所在地区-省 */
    private String province;

    /** 所在地区-市 */
    private String city;

    /** 所在地区-区县 */
    private String district;

    /** 配送方式 */
    private String deliveryMethod;

    /** 配送线路 */
    private String deliveryRoute;

    /** 物流公司 */
    private String logisticsCompany;

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
