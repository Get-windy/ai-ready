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
 * 协议约定的**履约方式集合**（{@code agreement_fulfillment_mode}，§13.10）。
 *
 * <p><b>一版多行并存</b>：同一份合作协议可以"同城直发 + 异地中转"同时存在 —— 这才是真实交易
 * （用户 2026-09-22 明确要求）。因此本表的唯一索引是
 * {@code (version_id, mode) WHERE deleted = 0}（同一个方式不重复登记），
 * <b>而不是</b> {@code (version_id)}（那会退回成单选）。</p>
 *
 * <p>订单路由要问的第一个问题就是"这份协议允许哪些履约方式"，取数入口：
 * {@code AgreementRuntime#resolve(...)} 的结果里带 {@code fulfillmentModes}，
 * 或直接用 {@code AgreementRuntime#fulfillmentModes(versionId)}。下单时在该集合内确定具体方式
 * （订单行粒度），并记录是谁定的（规则 / 人工）—— 属阶段 B 的订单路由接线。</p>
 */
@Data
@Accessors(chain = true)
@TableName("agreement_fulfillment_mode")
public class AgreementFulfillmentMode {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 系统级归属位，恒为 0。 */
    private Long tenantId;

    private Long agreementId;

    private Long versionId;

    /** 履约方式：DROP_SHIP 直发 / TRANSIT_STOCK 中转 / PICKUP 自提 / LOCAL_STOCK 自有库存 */
    private String mode;

    /** 适用范围说明（如"仅限江浙沪""仅限 3C 类目"），人读，不参与自动执行 */
    private String scopeNote;

    private Integer sort;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
