package cn.aiedge.erp.party.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@TableName("biz_party")
public class Party {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String partyCode;

    private String partyName;

    private String shortName;

    /** 助记码（基础资料快速检索，如物流公司/客户名称拼音首字母） */
    private String mnemonicCode;

    private Integer partyType;

    private Long categoryId;

    private String partyLevel;

    private BigDecimal creditLimit;

    private BigDecimal currentDebt;

    private Integer settlementType;

    private Integer settlementDays;

    private String unifiedCode;

    private String businessLicense;

    private String taxNumber;

    private String bankName;

    private String bankAccount;

    // ── 纳税人信息（基础资料：公司全称 / 地址 / 开户行地址） ──
    private String companyFullName;

    private String address;

    private String bankAddress;

    private String phone;

    private String fax;

    private String email;

    private String website;

    private String legalPerson;

    private String legalPersonPhone;

    private Integer status;

    private String remark;

    // ── 默认经手人（客户主数据：销售单据默认带出的业务员/经手人） ──
    private Long defaultHandlerId;

    private String defaultHandlerName;

    // ── 期初信息 ──
    /** 期初应付金额 */
    private BigDecimal openingPayable;

    /** 期初预付金额 */
    private BigDecimal openingPrepaid;

    // ── 供应商经营信息 ──
    /** 经营系列 */
    private String operatingSeries;

    /** 经营面积 */
    private BigDecimal operatingArea;

    // ── 账期（动态付款期限 / 固定账期 / 结算期） ──
    /** 付款期限方式：DYNAMIC 动态付款期限 / FIXED 固定账期 */
    private String paymentTermType;

    /** 动态付款期限(天) */
    private Integer paymentDays;

    /** 固定账期日(号) */
    private Integer fixedPaymentDay;

    /** 结算期(号) */
    private Integer settlementDay;

    /** 启用价格跟踪：0-否 1-是 */
    private Integer priceTrackEnabled;

    /** 多重身份，逗号分隔：CUSTOMER/SUPPLIER/LOGISTICS/OTHER */
    private String roles;

    // ── 客户列表/表单专属字段（V11.151.0） ──
    /** 所属仓库 */
    private String warehouseName;

    /** 所属区域 */
    private String region;

    // ── 所在地区（省/市/区县，来源 sys_region 行政区划；保存时同步默认联系人所在地区） ──
    /** 所在地区-省 */
    private String province;

    /** 所在地区-市 */
    private String city;

    /** 所在地区-区县 */
    private String district;

    /** 推广人ID */
    private Long promoterId;

    /** 推广人 */
    private String promoterName;

    /** 买家账号（商城账号） */
    private String buyerAccount;

    /** 客户一票通 */
    private String customerOnePass;

    /** 客户来源 */
    private String customerSource;

    /** 营业执照有效期 */
    private LocalDate businessLicenseExpiry;

    /** 最近交易时间 */
    private LocalDateTime lastTradeTime;

    /** 动态收款期限（天） */
    private Integer creditDays;

    /** 固定账期（号） */
    private Integer fixedCreditDay;

    /** 结算期（号） */
    private Integer statementDay;

    /** 期初应收金额 */
    private BigDecimal openingReceivable;

    /** 期初预收金额 */
    private BigDecimal openingPreReceived;

    // ── 个人会员扩展字段（party_level = 'MEMBER' 时使用） ──
    private String memberCardNo;

    private LocalDate birthday;

    private Integer points;

    // ── 会员管理子标签字段（V11.151.0） ──
    /** 会员名称 */
    private String memberName;

    /** 会员级别 */
    private String memberLevel;

    /** 会员卡状态：NORMAL 正常 / STOPPED 停用 / EXPIRED 已过期 */
    private String memberCardStatus;

    /** 会员卡有效期（起） */
    private LocalDate memberValidStart;

    /** 会员卡有效期（止） */
    private LocalDate memberValidEnd;

    /** 累计消费额 */
    private BigDecimal memberTotalConsume;

    /** 发卡时间 */
    private LocalDateTime memberIssueTime;

    /** 初始积分 */
    private Integer memberInitialPoints;

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
