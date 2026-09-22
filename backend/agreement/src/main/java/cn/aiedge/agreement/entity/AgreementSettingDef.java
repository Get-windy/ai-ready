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
 * 字段设定版的**字段元数据**（{@code agreement_setting_def}，平台维护，DOMAIN-MODEL §13.3）。
 *
 * <p>它回答的是"协议里可以约定哪些设定项、每一项是什么类型、<b>被谁消费</b>"，
 * 而不是"这一版约定了什么"（那是 {@link AgreementSetting}）。</p>
 *
 * <h3>⚠️ {@code consumerPoint} 是硬要求，不是注释</h3>
 * 本仓最贵的历史包袱恰恰是"配置界面能勾、勾了不生效"（游客浏览开关的两个字段零消费、
 * {@code sys_user_data_scope} 配了不生效、{@code sys_permission.api_path} 只填一半……），
 * 因此 §13.3 立了一条规矩：<b>没有消费方的字段不许进设定版</b>。
 * 落法就是本列 —— 登记"这个字段被哪个下游环节消费"，取值必须是
 * {@code AgreementRuntime.ConsumerPoint} 里的枚举名，且：
 * <ul>
 *   <li>迁移里的 {@code DO $$} 自检会**在真库上断言没有一行 consumer_point 为空**；</li>
 *   <li>界面拿它显示"这一项会影响什么"（用户改一个字段前能看到后果）。</li>
 * </ul>
 *
 * <p><b>⚠️ 本表刻意没有"默认值"列</b>（同 {@code agreement_term_option} 没有 default_option）：
 * 平台给默认值 = 平台替双方决定商业条款 = 平台干预（㉜ / §3.4.4d1）。
 * 未约定就是未约定，由 {@code AgreementRuntime} 如实返回"未约定"并让下游拦下。</p>
 */
@Data
@Accessors(chain = true)
@TableName("agreement_setting_def")
public class AgreementSettingDef {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 平台级参考数据，恒为 0（与约定本身无关，见类注释）。 */
    private Long tenantId;

    /** 设定项编码（如 AR_CREDIT_DAYS / COMMISSION_RATE），全局唯一 */
    private String settingKey;

    /** 中文名（双方在界面上看到的名字） */
    private String label;

    /** 取值类型：ENUM / NUMBER / TEXT / BOOL / DATE / DURATION（见 {@code AgreementSettingValueType}） */
    private String valueType;

    /** ENUM 的候选值（逗号分隔的编码，如 {@code DROP_SHIP,TRANSIT_STOCK}）；非 ENUM 留空 */
    private String options;

    /** 是否必填（责任划分类一律 true）；未约定必填项时保存回执与就绪查询会明确列出 */
    private Boolean required;

    /**
     * ⚠️ **消费方**（{@code AgreementRuntime.ConsumerPoint} 的枚举名）：这个字段被谁消费。
     * 为空的行不许存在（迁移里的 DO $$ 自检会直接报错回滚）。
     */
    private String consumerPoint;

    /** 对消费后果的中文说明（"改了这一项会影响什么"），给界面显示 */
    private String semantics;

    private Integer sort;

    /** 1=启用 / 0=停用。停用只影响以后新签的协议，历史版本快照不受影响。 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
