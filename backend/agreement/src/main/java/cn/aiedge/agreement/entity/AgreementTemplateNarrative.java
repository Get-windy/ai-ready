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
 * 模板预填的**文字条款**（{@code agreement_template_narrative}，§13.9）。
 *
 * <p>⚠️ 模板里的文字段落是"发起时先替你写好的一段话"，只有被写进正式协议版本、
 * 双方确认之后才有意义。文字版本来就不自动执行（§13.2），模板中的文字同样不执行。</p>
 */
@Data
@Accessors(chain = true)
@TableName("agreement_template_narrative")
public class AgreementTemplateNarrative {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 与主档同口径：PLATFORM 模板为 0、TENANT 模板为本租户。 */
    private Long tenantId;

    private Long templateId;

    /** 段落类别：DISPUTE / CONFIDENTIALITY / FORCE_MAJEURE / SPECIAL_TERMS / BREACH_LIABILITY_TEXT */
    private String sectionCode;

    private String sectionTitle;

    private String contentText;

    private Integer sort;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
