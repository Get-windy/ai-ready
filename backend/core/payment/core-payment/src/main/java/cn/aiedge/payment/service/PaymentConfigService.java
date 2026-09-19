package cn.aiedge.payment.service;

import cn.aiedge.payment.dto.PaymentChannelConfigVO;
import cn.aiedge.payment.dto.PaymentChannelParam;
import cn.aiedge.payment.dto.PaymentConfigItemVO;
import cn.aiedge.payment.dto.PaymentSceneVO;

import java.util.List;

/**
 * 支付配置服务（设置 → 系统配置 → 支付配置，菜单 80623）。
 *
 * <p>职责：把本页 4 个 Tab 的配置值真实读写到 {@code sys_project_config}
 * （系统在用的配置中心 KV），替代原先「写缓存 / 读内置 12 条」的假保存链路。</p>
 */
public interface PaymentConfigService {

    // ── Tab② 支付方式：渠道参数 ─────────────────────────────────────────────

    /**
     * 渠道配置列表（渠道清单来自渠道 Bean，参数来自 sys_project_config）。
     *
     * @param keyword 渠道编码/名称模糊（可空）
     * @param enabled 只筛启用/停用（可空 = 不筛）
     */
    List<PaymentChannelConfigVO> listChannelConfigs(String keyword, Boolean enabled);

    /** 单渠道参数（编辑抽屉回填用，含密钥原文）；未配置时返回 all-null 对象而非 null */
    PaymentChannelParam getChannelParam(String channelCode);

    /** 保存渠道参数（真实 upsert 到 sys_project_config） */
    void saveChannelParam(String channelCode, PaymentChannelParam param);

    // ── Tab① 微信公众号配置 / Tab④ 在线退款：配置项 ──────────────────────────

    /**
     * 配置项列表。
     *
     * @param tab     wechat（微信公众号配置）/ refund（在线退款）
     * @param keyword 配置项名称/键模糊（可空）
     */
    List<PaymentConfigItemVO> listItems(String tab, String keyword);

    /** 保存单个配置项（键必须在白名单目录内） */
    void saveItem(String itemKey, String itemValue);

    // ── Tab③ 场景配置 ─────────────────────────────────────────────────────

    /** 场景配置列表（场景清单来自后端目录，渠道集合来自 sys_project_config） */
    List<PaymentSceneVO> listScenes(String keyword);

    /** 保存某场景启用的渠道集合 */
    void saveScene(String sceneCode, List<String> channels);

    /** 在线退款 Tab 的两段说明文案（后端下发，逐字对标） */
    List<String> refundNotes();
}
