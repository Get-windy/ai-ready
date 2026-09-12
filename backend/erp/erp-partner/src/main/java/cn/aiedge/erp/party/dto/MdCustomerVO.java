package cn.aiedge.erp.party.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * MD客户视图对象——向前端映射旧版 partner 字段名
 */
@Data
public class MdCustomerVO {
    private Long id;
    private String partnerCode;
    private String partnerName;
    private String partnerShortName;
    /** 助记码 */
    private String mnemonicCode;
    private String partnerType;
    private Long categoryId;
    private String categoryName;
    private String gradeName;
    private String settleType;
    private String phone;
    private String fax;
    private String email;
    private String website;
    private String legalPerson;
    private String taxNumber;
    private String bankName;
    private String bankAccount;
    /** 纳税人信息：公司全称 */
    private String companyFullName;
    /** 纳税人信息：开户行地址 */
    private String bankAddress;
    private BigDecimal creditLimit;
    private String remark;
    private String status;
    private String statusDesc;
    /** 默认经手人（业务员） */
    private Long defaultHandlerId;
    private String defaultHandlerName;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer deleted;

    // ── 主联系人/主网点（biz_party_contact 主记录带出） ──
    /** 联系人姓名（列表「联系人」列） */
    private String contactPerson;
    /** 联系电话（列表「联系电话」列） */
    private String contactPhone;
    /** 对方地址（列表「物流公司地址」/「联系地址」列） */
    private String address;

    // ── 供应商金标准补充 ──
    /** 多重身份，逗号分隔：CUSTOMER/SUPPLIER/LOGISTICS/OTHER */
    private String roles;
    /** 列表「新增时间」列 */
    private LocalDateTime addTime;
    /** 附件数量（列表「附件」列） */
    private Integer attachmentCount;
    /** 纳税人信息：地址 */
    private String taxAddress;
    /** 期初应付金额 */
    private BigDecimal openingPayable;
    /** 期初预付金额 */
    private BigDecimal openingPrepaid;
    /** 经营系列（供应商特有） */
    private String operatingSeries;
    /** 经营面积（供应商特有） */
    private BigDecimal operatingArea;
    /** 付款期限方式：DYNAMIC 动态付款期限 / FIXED 固定账期 */
    private String paymentTermType;
    /** 动态付款期限(天) */
    private Integer paymentDays;
    /** 固定账期日(号) */
    private Integer fixedPaymentDay;
    /** 结算期(号) */
    private Integer settlementDay;
    /** 启用价格跟踪 0/1 */
    private Integer priceTrackEnabled;

    // ── 客户金标准字段（V11.151.0，对标「全部客户」26 列 + 会员管理 10 列） ──
    /** 所属仓库 */
    private String warehouseName;
    /** 所属区域 */
    private String region;
    /** 所在地区-省（行政区划 sys_region） */
    private String province;
    /** 所在地区-市 */
    private String city;
    /** 所在地区-区县 */
    private String district;
    /** 推广人 */
    private Long promoterId;
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

    // ── 会员管理子标签 ──
    /** 会员名称 */
    private String memberName;
    /** 会员卡号 */
    private String memberCardNo;
    /** 会员级别 */
    private String memberLevel;
    /** 会员卡状态：NORMAL 正常 / STOPPED 停用 / EXPIRED 已过期 */
    private String memberCardStatus;
    private String memberCardStatusDesc;
    /** 会员卡有效期（起/止） */
    private LocalDate memberValidStart;
    private LocalDate memberValidEnd;
    /** 会员生日 */
    private LocalDate birthday;
    /** 当前积分 */
    private Integer points;
    /** 初始积分 */
    private Integer memberInitialPoints;
    /** 累计消费额 */
    private BigDecimal memberTotalConsume;
    /** 发卡时间 */
    private LocalDateTime memberIssueTime;
}
