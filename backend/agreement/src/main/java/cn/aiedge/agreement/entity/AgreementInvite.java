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
 * 唯一送达邀请（{@code agreement_invite}）—— §13.5 的落地形态。
 *
 * <h3>token 绑定**五件事**（本表的列就是这个"绑定"本身）</h3>
 * <ol>
 *   <li>{@link #targetPartyId} 目标主体；</li>
 *   <li>{@link #targetTenantId} 目标租户；</li>
 *   <li>{@link #versionId} 目标文稿版本；</li>
 *   <li>{@link #expiresAt} 时效；</li>
 *   <li>{@link #status} 一次性（领取即置 {@code ACCEPTED}）。</li>
 * </ol>
 * <p>⇒ 打开链接的人必须先登录；其**会话租户**与**所代表主体**都要与目标匹配，
 * 否则一律回「<b>这份契约不是发给你的</b>」。**转发给别人打不开**，这就是"保证另一方唯一收到"。</p>
 *
 * <h3>⚠️ 明文 token 不落库</h3>
 * 库里只有 {@link #tokenHash}（SHA-256）。校验 = 把对方传入的明文**现算**一次哈希再等值比对。
 * 为了还能运维排查，另存 {@link #tokenHint}（明文前 8 位）与 {@link #inviteCode}（人读短码）——
 * 只存哈希就"没法用明文搜库"，这是刻意的取舍，见迁移文件头。
 *
 * <h3>系统级归属位</h3>
 * {@code tenant_id} 恒为 0（裁定⑥）：本表已登记进 {@code MyBatisPlusConfig.IGNORE_TENANT_TABLES}，
 * 可见性由父协议两端判定（{@code AgreementVisibility}，唯一构造处）。
 */
@Data
@Accessors(chain = true)
@TableName("agreement_invite")
public class AgreementInvite {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 系统级归属位，恒为 0。不要用它判断归属，可见性看父协议两端。 */
    private Long tenantId;

    private Long agreementId;

    /** ③ 目标文稿版本：这份邀请针对哪一版发出（对方打开看到的就是这一版） */
    private Long versionId;

    /** ① 目标主体：token 绑定的是"谁" */
    private Long targetPartyId;

    /** ② 目标租户：会话租户必须等于它，否则「这份契约不是发给你的」 */
    private Long targetTenantId;

    /** 目标是协议的哪一端：A / B（{@code AgreementPartySide}） */
    private String targetSide;

    /** token 哈希 = SHA-256(明文)。明文永不落库，校验时现算再比对。 */
    private String tokenHash;

    /** 明文前 8 位：仅供运维/客诉在库里对上是哪一条（不足以反推 43 位随机串） */
    private String tokenHint;

    /** 人读短码：可口述、可人工输入（二维码渲染交给前端，后端不引入二维码依赖） */
    private String inviteCode;

    /** 送达渠道：QRCODE / LINK（留痕"通过哪个渠道领取或查看的"） */
    private String channel;

    /** 0=待领取 / 1=已领取 / 2=已撤回 / 3=已过期（{@code AgreementInviteStatus}） */
    private Integer status;

    /** ④ 时效：到这个时刻即失效 */
    private LocalDateTime expiresAt;

    /** 留痕：被看过多少次；首次查看的人/时间/渠道 */
    private Integer viewCount;
    private Long firstViewedBy;
    private LocalDateTime firstViewedAt;
    private String firstViewChannel;

    /** 领取留痕：谁、何时、从哪个渠道领取的、他当时声明的代表主体 */
    private Long acceptedBy;
    private LocalDateTime acceptedAt;
    private String acceptChannel;
    private Long acceptedPartyId;

    /** 撤回留痕（发起方自己的动作） */
    private Long revokeBy;
    private LocalDateTime revokeAt;
    private String revokeReason;

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
