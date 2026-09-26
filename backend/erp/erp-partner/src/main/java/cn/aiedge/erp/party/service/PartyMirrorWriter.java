package cn.aiedge.erp.party.service;

import cn.aiedge.erp.party.entity.Party;
import cn.aiedge.erp.party.mapper.PartyMirrorMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * **双写**：把 `biz_party` 的写入同步到阶段 3 的新载体 `party` / `party_tenant`。
 *
 * <h3>为什么需要它</h3>
 * 新表建好并回填之后，如果**只**写旧表，两边会**越落越远**（实测量到过 21 vs 19）。
 * 在"读路径还没切"的并存期，双写是保证"将来切过去时数据是齐的"的唯一手段。
 *
 * <h3>⚠️ 失败处理：**记 ERROR，不阻断建档**（2026-09-26 用户裁定，选项乙）</h3>
 * 并存期新表**还不是读路径** ⇒ 因为它写不进去而让用户建不了档，是把系统可用性押在一张
 * 尚未启用的表上。因此这里**吞掉异常并留痕**，缺口交给两条兜底：
 * <ol>
 *   <li>`tools/sync-party-from-biz-party.cjs` —— 幂等对账补齐（可随时跑）；</li>
 *   <li>**差额本身就是漂移监控** —— 日志 `ERROR` 与对账差额一起说明"哪些行没双写成功"。</li>
 * </ol>
 * ⚠️ 代价必须说清：**吞异常 = 会静默漂移**。所以这里的日志必须写成"可定位 + 可行动"，
 * 且切读路径**之前**必须先把差额归零（那是切读的前置检查，见方案 §3.3）。
 *
 * <h3>方向口径（与建表回填、对账脚本三方一致）</h3>
 * ⑬ 裁定「同一对主体在同一家店的角色**可以并存**」⇒ 一个主体**可以同时有两条边**：
 * `roles` 含 `CUSTOMER` ⇒ 建 `SALE` 边；含 `SUPPLIER` 或 `party_type = 2` ⇒ 建 `PURCHASE` 边。
 * **两个都不满足 ⇒ 一条边都不建**（如 `party_type=3` 第三方服务主体，方向未定 ⇒ 不猜）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PartyMirrorWriter {

    /** 贸易边方向：我卖给他（他是我的客户） */
    public static final String DIRECTION_SALE = "SALE";
    /** 贸易边方向：我向他买（他是我的供应商） */
    public static final String DIRECTION_PURCHASE = "PURCHASE";

    private final PartyMirrorMapper mirrorMapper;

    /**
     * 该主体应当有哪些贸易边（**纯函数**，可单测）。
     *
     * @return 方向列表；**判不出来时返回空列表**（不是"默认一个"）
     */
    public static List<String> directionsOf(Party p) {
        List<String> directions = new ArrayList<>(2);
        if (p == null) {
            return directions;
        }
        String roles = p.getRoles();
        boolean customer = roles != null && roles.contains("CUSTOMER");
        boolean supplier = roles != null && roles.contains("SUPPLIER");
        // roles 没说是供应商时，回落到 party_type（沿用 biz_party 的历史口径：2 = 供应商）
        if (!supplier && p.getPartyType() != null && p.getPartyType() == 2) {
            supplier = true;
        }
        if (customer) {
            directions.add(DIRECTION_SALE);
        }
        if (supplier) {
            directions.add(DIRECTION_PURCHASE);
        }
        return directions;
    }

    /** 主体被写入（新建或修改）后调用：刷主档 + 补/刷贸易边。 */
    public void onWrite(Party p) {
        if (p == null || p.getId() == null) {
            return;
        }
        try {
            mirrorMapper.upsertParty(p);
        } catch (Exception e) {
            // 唯一约束冲突（unified_code 与别的主体重复）是最可能的一种：它对用户是有意义的信息，
            // 但并存期不阻断建档 ⇒ 明确记出来，靠对账脚本兜底。
            log.error("【双写】party 主档写入失败（不阻断建档；请跑 tools/sync-party-from-biz-party.cjs 对账）: "
                            + "partyId={}, partyName={}, unifiedCode={}, 原因={}",
                    p.getId(), p.getPartyName(), p.getUnifiedCode(), e.getMessage());
            return;   // 主档没写成功就不写边：避免出现"边指向一个不存在的主档"
        }

        Long tenantId = p.getTenantId();
        if (tenantId == null) {
            // 主体档案必须有归属租户（"这条档案归哪个租户"）；没有就如实记出来，不编一个
            log.warn("【双写】主体没有 tenantId，跳过贸易边: partyId={}, partyName={}",
                    p.getId(), p.getPartyName());
            return;
        }
        for (String direction : directionsOf(p)) {
            try {
                mirrorMapper.upsertEdge(tenantId, p.getId(), direction, p);
            } catch (Exception e) {
                log.error("【双写】party_tenant 边写入失败（不阻断建档）: partyId={}, tenantId={}, direction={}, 原因={}",
                        p.getId(), tenantId, direction, e.getMessage());
            }
        }
        if (directionsOf(p).isEmpty()) {
            // 方向判不出来是**正常结论**（第三方服务主体），但要留痕：否则将来会有人以为漏了
            log.info("【双写】该主体不建贸易边（既非客户也非供应商，方向未定 ⇒ 不猜）: partyId={}, partyType={}, roles={}",
                    p.getId(), p.getPartyType(), StringUtils.hasText(p.getRoles()) ? p.getRoles() : "(空)");
        }
    }

    /** 主体被删除（逻辑删除）后调用：同步软删主档与全部贸易边。 */
    public void onDelete(Long partyId) {
        if (partyId == null) {
            return;
        }
        try {
            mirrorMapper.softDeleteEdges(partyId);
            mirrorMapper.softDeleteParty(partyId);
        } catch (Exception e) {
            log.error("【双写】软删同步失败（不阻断删除）: partyId={}, 原因={}", partyId, e.getMessage());
        }
    }
}
