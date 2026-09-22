package cn.aiedge.agreement.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 邀请 token 的生成、哈希与短链拼装 —— <b>纯函数，不依赖 Spring / 数据库</b>。
 *
 * <h3>不可枚举（§13.5 的硬要求）</h3>
 * 明文 token = <b>32 字节 {@link SecureRandom}</b> 的 Base64URL 无填充编码（43 字符）。
 * 32 字节 = 256 位熵，暴力猜解不现实；用 {@code SecureRandom} 而不是
 * {@code Math.random}/{@code UUID.randomUUID()}，因为前者的随机性是可证明的，
 * 后者在部分 JDK 实现里熵源较弱（本仓对"不可枚举"的口径要求按密码学安全随机源来）。
 *
 * <h3>落库只存哈希，怎么校验、运维怎么排查</h3>
 * <ul>
 *   <li><b>怎么校验</b>：对方打开链接时把明文传进来，服务端用 {@link #hash(String)}
 *       <b>现算一次</b>，再与 {@code agreement_invite.token_hash} <b>等值比较</b>。
 *       明文永不落库 ⇒ 整库被拖走也换不出可用的邀请链接。</li>
 *   <li><b>运维怎么排查</b>：另存 {@link #hint(String)}（明文前 8 位）与邀请短码。
 *       排查路径是"用 hint + 协议编号 + 状态去查"，而不是"用明文搜库"——
 *       这是刻意接受的取舍（安全性优先，可运维性用 hint 兜住）。</li>
 * </ul>
 *
 * <h3>二维码：后端不生成图片</h3>
 * 本仓没有任何二维码依赖（实测无 zxing、无二维码工具类），因此后端只给
 * {@link #shortLink(String)} 这样的**可供前端渲染二维码的短链**，以及人读邀请码；
 * 渲染留给前端。不为"生成图片"引入新依赖。
 */
public final class AgreementInviteToken {

    /** 明文 token 的字节数（256 位熵）。 */
    private static final int TOKEN_BYTES = 32;

    /** 人读邀请码长度（去掉易混字符后的字母表）。 */
    private static final int CODE_LENGTH = 10;

    /**
     * 人读邀请码字母表：刻意去掉 0/O、1/I/L 这些"念出来会听错"的字符 ——
     * 邀请码的用途是**口头/人工输入**，可读性优先于密度。
     */
    private static final char[] CODE_ALPHABET = "23456789ABCDEFGHJKMNPQRSTUVWXYZ".toCharArray();

    private static final SecureRandom RANDOM = new SecureRandom();

    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();

    private AgreementInviteToken() {
    }

    /** 生成不可枚举的明文 token（32 字节 SecureRandom → 43 字符 Base64URL 无填充）。 */
    public static String newToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        return ENCODER.encodeToString(bytes);
    }

    /**
     * 生成人读邀请码（大写字母 + 数字，去掉易混字符）。
     *
     * <p>与 token 是**两条独立的路**：token 走链接/二维码，邀请码走人工口述。
     * 两者都能定位到同一条邀请记录，但都受同一套五绑定校验约束。</p>
     */
    public static String newInviteCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(CODE_ALPHABET[RANDOM.nextInt(CODE_ALPHABET.length)]);
        }
        return sb.toString();
    }

    /**
     * token 哈希 = SHA-256(明文)，十六进制小写（与 {@code AgreementSnapshot.sha256} 同口径）。
     *
     * <p>这里不另写一套哈希实现：口径必须与协议内容哈希完全一致，否则日后核对时会
     * 出现"两个 SHA-256 谁是谁"的混乱。</p>
     */
    public static String hash(String rawToken) {
        if (rawToken == null) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("计算邀请 token 哈希失败", e);
        }
    }

    /**
     * 明文前 8 位，供运维/客诉在库里对上是哪一条邀请。
     *
     * <p>8 位字符来自 43 位随机串，剩余 35 位仍是未知的 ⇒ 不足以反推完整 token。
     * 真要不放心，可把这条 hint 也改成哈希 —— 代价是"按明文搜库"这条路彻底没有，
     * 运维只能靠协议编号 + 时间 + 状态去找记录。</p>
     */
    public static String hint(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return "????????";
        }
        String t = rawToken.trim();
        return t.length() <= 8 ? t : t.substring(0, 8);
    }

    /**
     * 可供前端渲染二维码 / 点开的**短链**。
     *
     * <p>形如 {@code /agreement/invite?token=xxx}（站内相对路径）：
     * 不带域名是因为后端不知道前端站点域名，硬编码一个反而会写错；
     * 前端拼上自己的域名再渲染二维码即可。</p>
     */
    public static String shortLink(String rawToken) {
        return "/agreement/invite?token=" + (rawToken == null ? "" : rawToken);
    }

    /** 相对明文 token 的等值比较（避免调用方误用 {@code equals} 时把 null 当匹配）。 */
    public static boolean matches(String rawToken, String storedHash) {
        if (rawToken == null || storedHash == null) {
            return false;
        }
        return storedHash.equalsIgnoreCase(hash(rawToken));
    }
}
