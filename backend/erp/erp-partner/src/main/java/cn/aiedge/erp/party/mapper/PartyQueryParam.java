package cn.aiedge.erp.party.mapper;

import lombok.Data;

import java.time.LocalDate;

/**
 * Party 分页/搜索查询参数
 */
@Data
public class PartyQueryParam {
    private String keyword;
    private Integer partyType;
    private Integer status;
    private Long categoryId;
    /** 结款方式：1 挂账 / 0 现结（null=全部）——库中为整型列，须用数值类型避免 PG 类型不匹配 */
    private Integer settleType;
    /** 所属区域（模糊） */
    private String region;
    /** 默认经手人（模糊） */
    private String handler;
    /** 联系地址（模糊，含主联系人地址） */
    private String address;
    /** 客户来源 */
    private String customerSource;
    private LocalDate createTimeStart;
    private LocalDate createTimeEnd;
    private LocalDate lastTradeStart;
    private LocalDate lastTradeEnd;
    /** 多重身份（CUSTOMER/SUPPLIER/...），配合 showAsRole 使用 */
    private String role;
    /** 显示供应商中的客户：本类型 ∪ roles 含 role 的记录 */
    private Boolean showAsRole;
    /** 只显示开通商城账号 */
    private Boolean onlyMallAccount;
    /** 只显示无销售记录客户（以 last_trade_time 为空判定） */
    private Boolean onlyNoTrade;
    /** 客户级别（模糊，对应 party_level） */
    private String gradeName;
    /** 所属仓库（模糊） */
    private String warehouse;
    /** 推广人（模糊） */
    private String promoter;

    // ── 全部联系人子标签查询条件 ──
    /** 对应客户 */
    private Long customerId;
    /** 配送方式 */
    private String deliveryMethod;

    // ── 会员管理子标签查询条件 ──
    /** 会员名称/会员卡号（模糊） */
    private String memberKeyword;
    /** 联系电话（模糊） */
    private String phone;
    /** 会员级别 */
    private String memberLevel;
    /** 会员卡状态：NORMAL/STOPPED/EXPIRED */
    private String memberCardStatus;
    /** 会员生日（精确到日） */
    private LocalDate birthday;
    /** 年龄区间 */
    private Integer ageMin;
    private Integer ageMax;
    /** 当前积分区间 */
    private Integer pointsMin;
    private Integer pointsMax;

    /** 排序字段（白名单：partyCode / partyName / createTime，在 XML 内映射到列名，避免注入） */
    private String sortField;
    /** 排序方向：asc / desc */
    private String sortOrder;

    private Integer pageSize;
}
