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
 * 模板预填的**条款**（{@code agreement_template_term}，§13.9 / §13.11）。
 *
 * <p>⚠️ 这是"草稿上先替你勾好"，不是"已经约定"。写入正式协议时，它落到**新建 DRAFT 版本**的
 * {@code agreement_term} 上，仍需双方在那一版上显式确认（双签）才算约定。</p>
 */
@Data
@Accessors(chain = true)
@TableName("agreement_template_term")
public class AgreementTemplateTerm {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 与主档同口径：PLATFORM 模板为 0、TENANT 模板为本租户。 */
    private Long tenantId;

    private Long templateId;

    /** 条款类别（对应平台字典 {@code agreement_term_option.term_code}） */
    private String termCode;

    private String optionCode;

    private String paramValue;

    private Integer sort;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
