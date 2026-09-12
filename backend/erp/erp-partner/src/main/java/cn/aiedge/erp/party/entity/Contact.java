package cn.aiedge.erp.party.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 联系人（独立主数据）
 * <p>
 * 与「往来单位」是**平行且相互关联**的两个实体：一个联系人可服务多个往来单位，
 * 一个往来单位可有多个联系人。二者的关系与关系上下文（是否主联系人 / 职务 / 配送方式 / 区域…）
 * 存在关联表 {@code biz_party_contact}。
 * </p>
 */
@Getter
@Setter
@TableName("biz_contact")
public class Contact {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    /** 姓名 */
    private String contactName;

    /** 性别 */
    private String gender;

    /** 手机 */
    private String mobile;

    /** 电话 */
    private String phone;

    private String email;

    private String wechat;

    private String qq;

    /** 生日 */
    private LocalDate birthday;

    private String remark;

    private Integer status;

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
