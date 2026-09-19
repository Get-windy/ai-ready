package cn.aiedge.payment.config;

import java.util.List;
import java.util.Map;

/**
 * 支付配置的**配置项目录（白名单）**。
 *
 * <p>为什么要白名单：本页 4 个 Tab 的配置值统一落在 {@code sys_project_config}
 * （系统在用的配置中心 KV，见开发文档 §11「真实 KV」），若不对可写键做白名单，
 * 前端就能借本页接口写任意 KV（越权面）。因此：</p>
 * <ul>
 *   <li><b>键名</b>只允许出现在本类里的常量；</li>
 *   <li><b>配置项名称 / 说明文案</b>逐字取自《设置模块/支付配置开发文档.md》§2 的 ql361 实测清单
 *       （截图 + JSON，2026-09-18），<b>不发明字段</b>；</li>
 *   <li>文档 §8.4 已裁定「场景矩阵 / 费率 / 自动退款开关 —— 走现成 KV 的新键即可，不需要新增列」，
 *       故本页不新增表、不新增列。</li>
 * </ul>
 *
 * <p><b>与本系统其它配置的隔离</b>：{@code sys_project_config} 里已有的键为 {@code system.*} /
 * {@code expense.approval.*} / {@code user_page_config:*} 等，与本类统一使用的 {@code payment.*}
 * 前缀无交集（2026-09-18 实测：{@code SELECT count(*) FROM sys_project_config WHERE config_key
 * LIKE 'payment%'} = 0）。系统参数页走的是另一张表 {@code sys_config}，两页互不影响。</p>
 */
public final class PaymentConfigCatalog {

    private PaymentConfigCatalog() {
    }

    // ═══════════════════════════════════════════════════════════════════
    // 配置分组（写入 sys_project_config.config_group）
    // ═══════════════════════════════════════════════════════════════════

    /** 4 个 Tab 共用的分组名，便于按组排查「本页写进去的行」 */
    public static final String CONFIG_GROUP = "payment";

    /** Tab① 微信公众号配置 */
    public static final String TAB_WECHAT = "wechat";

    /** Tab④ 在线退款 */
    public static final String TAB_REFUND = "refund";

    /** Tab② 支付方式（渠道参数）的键前缀；值 = PaymentChannelParam 的 JSON */
    public static final String CHANNEL_KEY_PREFIX = "payment.channel.";

    /** 场景配置的键前缀；值 = 逗号分隔的渠道码 */
    public static final String SCENE_KEY_PREFIX = "payment.scene.";

    // ═══════════════════════════════════════════════════════════════════
    // Tab① 微信公众号配置（逐字取自开发文档 §2「微信公众号配置」Tab 实测）
    // ═══════════════════════════════════════════════════════════════════

    public static final String WECHAT_BIND = "payment.wechat.bind";
    public static final String WECHAT_ORIGINAL_ID = "payment.wechat.originalId";
    public static final String WECHAT_APP_ID = "payment.wechat.appId";
    public static final String WECHAT_APP_SECRET = "payment.wechat.appSecret";

    /** 微信公众号配置的 4 个配置项（顺序 = ql361 页面出现顺序） */
    public static final List<ItemDef> WECHAT_ITEMS = List.of(
            new ItemDef(WECHAT_BIND, "微信公众号绑定", "boolean", "true",
                    "微信公众号绑定开关（ql361 实测默认：开）"),
            new ItemDef(WECHAT_ORIGINAL_ID, "微信原始ID", "text", "",
                    "微信原始ID,如果使用多门店二维码填写"),
            new ItemDef(WECHAT_APP_ID, "appid", "text", "",
                    "微信公众号授权；appid如果为空其他项不需要填"),
            new ItemDef(WECHAT_APP_SECRET, "appsecret", "password", "",
                    "微信公众号 appsecret")
    );

    // ═══════════════════════════════════════════════════════════════════
    // Tab④ 在线退款（逐字取自开发文档 §2「在线退款」Tab 实测）
    // ═══════════════════════════════════════════════════════════════════

    public static final String REFUND_AUTO_ENABLED = "payment.refund.autoRefundEnabled";

    /** 在线退款配置项（ql361 实测只有 1 个开关 + 两段说明文字） */
    public static final List<ItemDef> REFUND_ITEMS = List.of(
            new ItemDef(REFUND_AUTO_ENABLED, "在线支付自动退款", "boolean", "false",
                    "在线支付自动退款开关（ql361 实测默认：关）")
    );

    /**
     * 在线退款 Tab 的两段说明文案 —— <b>逐字</b>照抄开发文档 §2 的 ql361 截图实测文本
     * （编号与断句保持原样），由后端下发，避免前端硬编码后与对标文案漂移。
     */
    public static final List<String> REFUND_NOTES = List.of(
            "自动退款说明：1.自动退款仅针对商城订单的退款处理（含退货申请处理、拼团订单失败退款、"
                    + "预售订单失败退款）; 2.订单有在线支付且在线支付可退金额大于0时，进行在线退款流程，"
                    + "否则进行手工开收款单退款流程。",
            "3. 异常退款说明：1）系统对应账户余额不足时不受【系统参数-单据设置-财务账户金额允许为负】控制，"
                    + "默认在线支付的账户余额不足自动记账; 2）第三方账户余额不足则不能自动退款; "
                    + "3）有处理中的在线退款时，不能再次提交退款，在线退款处理完成后方可发起;"
    );

    // ═══════════════════════════════════════════════════════════════════
    // Tab③ 场景配置
    // ═══════════════════════════════════════════════════════════════════

    /**
     * 支付场景清单。
     *
     * <p>取值口径：{@code PaymentRequest} 实体（{@code entity/PaymentRequest.java:19}）的注释
     * 登记的 bizType 三个字面值 —— {@code SALE_ORDER, TRADE_ORDER, DMS_DELIVERY}。
     * 本系统**没有**支付场景字典表，也没有任何代码向 {@code /api/payment/request} 写入过数据
     * （{@code payment_request} 实测 0 行），故场景名由枚举字面值推定，如实标注：
     * {@code TRADE_ORDER} / {@code DMS_DELIVERY} 的中文名**未在源码中找到权威字典**。</p>
     *
     * <p><b>不采用</b> ql361 的场景名（线下扫码付款 / 微信公众号付款 / 微信小程序付款 /
     * 互联小程序 / 共享商城）—— 开发文档 §8.3 已明确「不承诺在本系统建立同名渠道或同名场景」，
     * 且 §8.3 明确「不借鉴 ql361 的渠道清单本身」。</p>
     */
    public static final List<SceneDef> SCENES = List.of(
            new SceneDef("SALE_ORDER", "销售订单"),
            new SceneDef("TRADE_ORDER", "交易订单"),
            new SceneDef("DMS_DELIVERY", "配送单")
    );

    // ═══════════════════════════════════════════════════════════════════

    /**
     * 配置项定义（键 / 名称 / 值类型 / 出厂默认值 / 说明）。
     *
     * <p>{@code defaultValue} 逐字取自 ql361 实测的初始状态（开发文档 §2）：
     * 微信公众号绑定=开、在线支付自动退款=关；未实测的文本项默认空串。
     * 它只在库中**无该行**时用于展示，不写库（写库仅发生在用户点保存时）。</p>
     */
    public record ItemDef(String key, String name, String valueType, String defaultValue, String description) {
    }

    /** 场景定义（场景码 / 场景名） */
    public record SceneDef(String code, String name) {
    }

    private static final Map<String, List<ItemDef>> ITEMS_BY_TAB = Map.of(
            TAB_WECHAT, WECHAT_ITEMS,
            TAB_REFUND, REFUND_ITEMS
    );

    /** 按 Tab 取配置项目录；未知 Tab 返回空列表（控制器会据此报 400） */
    public static List<ItemDef> itemsOf(String tab) {
        return ITEMS_BY_TAB.getOrDefault(tab, List.of());
    }

    /** 按配置键反查定义（不在目录内返回 null） */
    public static ItemDef itemOf(String key) {
        for (ItemDef def : WECHAT_ITEMS) {
            if (def.key().equals(key)) {
                return def;
            }
        }
        for (ItemDef def : REFUND_ITEMS) {
            if (def.key().equals(key)) {
                return def;
            }
        }
        return null;
    }

    /** 渠道参数的配置键：payment.channel.{渠道码小写}（与原前端键名规则一致） */
    public static String channelKey(String channelCode) {
        return CHANNEL_KEY_PREFIX + channelCode.toLowerCase();
    }

    /** 场景参数的配置键：payment.scene.{场景码} */
    public static String sceneKey(String sceneCode) {
        return SCENE_KEY_PREFIX + sceneCode;
    }

    /** 场景码是否在白名单内 */
    public static boolean isScene(String sceneCode) {
        return SCENES.stream().anyMatch(s -> s.code().equals(sceneCode));
    }
}
