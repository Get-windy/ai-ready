package cn.aiedge.agreement.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 模板预填的**设定项**（{@code agreement_template_setting}，§13.9）。
 *
 * <p><b>⚠️⚠️ 本表的存在本身就是最容易踩的坑</b>：字段与 {@code agreement_setting} 几乎一样，
 * 但语义完全不同 ——
 * 这里是"模板里建议这么填"，**不是**"这一版已经约定了"。
 * 模板项只在"从模板发起"时**预填**到新草稿版本上，仍需双方显式确认。
 * <b>{@code AgreementRuntime} 只读 {@code agreement_setting}，绝不读本表</b>（㉜ 的防线）。</p>
 *
 * <p>强类型列与 {@code agreement_setting} 同口径（按 {@code valueType} 决定读哪一列）。</p>
 */
@Data
@Accessors(chain = true)
@TableName("agreement_template_setting")
public class AgreementTemplateSetting {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 与主档同口径：PLATFORM 模板为 0、TENANT 模板为本租户。 */
    private Long tenantId;

    private Long templateId;

    private String settingKey;

    private String valueType;

    private String valueText;

    private BigDecimal valueNumber;

    private Boolean valueBool;

    private LocalDateTime valueDate;

    private Integer sort;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
