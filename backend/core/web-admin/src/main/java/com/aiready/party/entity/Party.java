package com.aiready.party.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 往来单位实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("biz_party")
public class Party {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 单位编码
     */
    private String partyCode;
    
    /**
     * 单位名称
     */
    private String partyName;
    
    /**
     * 单位简称
     */
    private String shortName;
    
    /**
     * 单位类型（1：客户 2：供应商 3：物流商 4：配套商）
     */
    private Integer partyType;
    
    /**
     * 单位分类ID
     */
    private Long categoryId;
    
    /**
     * 单位等级（A/B/C/D）
     */
    private String partyLevel;
    
    /**
     * 信用额度
     */
    private BigDecimal creditLimit;
    
    /**
     * 当前欠款金额
     */
    private BigDecimal currentDebt;
    
    /**
     * 结算方式（1：现金 2：月结 3：季结 4：年结）
     */
    private Integer settlementType;
    
    /**
     * 结算周期（天）
     */
    private Integer settlementDays;
    
    /**
     * 统一社会信用代码
     */
    private String unifiedCode;
    
    /**
     * 营业执照号
     */
    private String businessLicense;
    
    /**
     * 纳税人识别号
     */
    private String taxNumber;
    
    /**
     * 开户银行
     */
    private String bankName;
    
    /**
     * 银行账号
     */
    private String bankAccount;
    
    /**
     * 注册地址
     */
    private String registeredAddress;
    
    /**
     * 经营地址
     */
    private String businessAddress;
    
    /**
     * 联系电话
     */
    private String phone;
    
    /**
     * 传真
     */
    private String fax;
    
    /**
     * 邮箱
     */
    private String email;
    
    /**
     * 网站
     */
    private String website;
    
    /**
     * 法人姓名
     */
    private String legalPerson;
    
    /**
     * 法人电话
     */
    private String legalPersonPhone;
    
    /**
     * 业务负责人
     */
    private String businessContact;
    
    /**
     * 业务负责人电话
     */
    private String businessContactPhone;
    
    /**
     * 财务负责人
     */
    private String financeContact;
    
    /**
     * 财务负责人电话
     */
    private String financeContactPhone;
    
    /**
     * 首次交易日期
     */
    private LocalDate firstTradeDate;
    
    /**
     * 最后交易日期
     */
    private LocalDate lastTradeDate;
    
    /**
     * 交易次数
     */
    private Integer tradeCount;
    
    /**
     * 交易总额
     */
    private BigDecimal tradeAmount;
    
    /**
     * 状态（0：禁用 1：启用 2：黑名单）
     */
    private Integer status;
    
    /**
     * 备注
     */
    private String remark;
    
    /**
     * 创建者
     */
    @TableField(fill = FieldFill.INSERT)
    private Long createBy;
    
    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    /**
     * 更新者
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;
    
    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    /**
     * 删除标志（0：未删除 1：已删除）
     */
    @TableLogic
    private Integer deleted;
}
