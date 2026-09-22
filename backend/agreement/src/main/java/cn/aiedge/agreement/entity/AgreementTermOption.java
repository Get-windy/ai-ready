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
 * 平台条款选项字典：**只定义"有哪些选项、各自什么含义，不规定必须选哪个"**（§3.4.4d）。
 *
 * <p><b>⚠️⚠️ 本类刻意没有 defaultOption 字段，也不许加</b>：
 * 平台给默认值 = 平台替双方做了责任划分的决定 = 平台干预（㉜ / §3.4.4d1）。
 * 唯一事实来源是 DB 列，{@code tools/verify-agreement.cjs} 会直接断言
 * {@code agreement_term_option} 表<b>不存在</b> default_option / default_value 列。</p>
 *
 * <p>三个法律相关的硬校验点（§3.4.4d0「所有协议不得违法」是最高约束）：</p>
 * <ol>
 *   <li>字典维护时：{@code legalReviewStatus} 记录法务审核结论，{@code REJECTED} 的选项不许被选中生效；</li>
 *   <li>必填：责任划分类条款 {@code required = true}，没选完不许置生效（§3.4.4d1）；</li>
 *   <li>生效时：{@code status} 停用的选项不许出现在新签协议里（历史快照不受影响）。</li>
 * </ol>
 */
@Data
@Accessors(chain = true)
@TableName("agreement_term_option")
public class AgreementTermOption {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 条款类别（同类别的选项在协议详情页归到同一个下拉） */
    private String termCode;

    /** 选项编码（系统内部标识，同类内唯一） */
    private String optionCode;

    /** 选项中文名（双方在协议里看到的文字） */
    private String optionLabel;

    /** 语义说明：选择了它，系统会怎么执行（谁承担、怎么扣、什么时候扣） */
    private String semantics;

    /** 需要附带参数时填参数名（如"分账比例"），否则为空 */
    private String needsParam;

    /** 是否必填条款（责任划分类一律 true）；必填项没选完 ⇒ 协议不许置生效 */
    private Boolean required;

    /** 法务审核：PENDING 待审 / APPROVED 已通过 / REJECTED 判定违法 */
    private String legalReviewStatus;

    private Integer sort;

    /** 1=启用 / 0=停用（与前端「条款字典维护」页一致） */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
