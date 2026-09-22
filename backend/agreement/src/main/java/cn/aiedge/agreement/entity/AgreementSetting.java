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
 * 字段设定版的**值**（{@code agreement_setting}）：一行 = 某一版对某个字段约定的取值。
 *
 * <h3>这是"运行时配置"，不是文档（§13.1）</h3>
 * 它不是给人读的一段说明，而是**被下游链路直接消费**的运营内核：
 * 订单路由、发货单生成、库存扣减点、可售量校验、定价引擎、应收应付到期日、结算分账、
 * 发票生成、下单风控、取消/退货/理赔流程、保证金扣罚都会按字段来问它。
 * 因此它必须强类型、可判定"未约定"、并且**带着业务时点**被读取
 * （读取入口唯一：{@code AgreementRuntime}，见 §13.3）。</p>
 *
 * <h3>强类型怎么落</h3>
 * 一行里按类型分列存：{@code value_text} / {@code value_number} / {@code value_bool} / {@code value_date}，
 * 另冗余一列 {@code valueType} 记录"这一行是按哪一列解读的"。
 * 冗余是**刻意的**：几十年后有人直接查库取证时，不必再去 join 一张可能已被改过的元数据表
 * 才知道 {@code value_number} 里的 30 到底是"天"还是"元"。</p>
 *
 * <h3>⚠️ "未约定"与"约定为 0"是两件事</h3>
 * 某字段在某一版**没有行** ⇒ 未约定（{@code AgreementRuntime} 返回"未约定"，下游拦下报错）；
 * 有行且值为 0 ⇒ 已约定为 0。两者绝不允许混同（㉜：未约定就不自动执行、挂人工，
 * 不给平台默认值）。因此本表**不设任何默认值**。</p>
 */
@Data
@Accessors(chain = true)
@TableName("agreement_setting")
public class AgreementSetting {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 协议四表同为系统级：恒为 0（可见性由两端的 party_*_tenant_id 判定）。 */
    private Long tenantId;

    private Long agreementId;

    /** 设定挂在**版本**上：判"交易发生时生效的是哪一版"靠它（㉛：下单时刻生效的那一版）。 */
    private Long versionId;

    /** 字段编码，对应 {@code agreement_setting_def.setting_key} */
    private String settingKey;

    /** 本行的取值类型（冗余存，便于取证与解析，见类注释） */
    private String valueType;

    private String valueText;

    /** 数值 / 比例 / 天数（DURATION 也存这里，单位为天） */
    private BigDecimal valueNumber;

    private Boolean valueBool;

    private LocalDateTime valueDate;

    /** 约定时的备注（为什么这么定），可空；不参与任何自动执行 */
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
