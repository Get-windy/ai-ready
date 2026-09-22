package cn.aiedge.agreement.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 协议条款实例：一行 = 某个<b>版本</b>里对某个条款类别选定的选项。
 *
 * <p>条款挂在版本上（不是挂在主档上）：{@code agreement_version} 是快照，
 * 判「交易发生时生效的是哪一版」以及「那一刻约定了什么」都靠版本 + 本表。</p>
 *
 * <p><b>⚠️ 未约定就是未约定</b>：某条款类别在生效版本里没有行 = 这一项**未约定**，
 * 服务层必须能如实回答"未约定"，**不许**回落到任何平台默认值（§3.4.4d1）。
 * 见 {@code AgreementServiceImpl#findAgreedTerm}。</p>
 */
@Data
@Accessors(chain = true)
@TableName("agreement_term")
public class AgreementTerm {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long agreementId;

    /** 该条款属于哪个版本（条款写在草稿版本上，生效后随版本一起冻结） */
    private Long versionId;

    /** 条款类别，如 CANCEL_POLICY / RETURN_POLICY / RETURN_FREIGHT / QUALITY_LIABILITY */
    private String termCode;

    /** 选中的选项（来自平台字典 agreement_term_option.option_code） */
    private String optionCode;

    /** 选项的附带参数（字典 needs_param 指定的那个，如分账比例） */
    private String paramValue;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
