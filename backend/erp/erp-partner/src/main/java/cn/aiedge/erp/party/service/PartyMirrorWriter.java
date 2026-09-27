package cn.aiedge.erp.party.service;

import cn.aiedge.erp.party.entity.Party;
import cn.aiedge.erp.party.mapper.PartyMirrorMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.jdbc.datasource.ConnectionHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Savepoint;
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
 * <p>⚠️ **"吞异常"必须连"回滚到保存点"一起做，否则这句裁定是假的** ——
 * 只用 try/catch 时，一条失败的镜像语句会把整个 PG 事务打成 aborted，用户那次建档
 * 照样被静默回滚（接口还返回 200）。详见 {@link #inSavepoint} 的说明与实测复现。</p>
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

    /** 证件类型：营业执照（与批 1 回填同一套取值，改一处必须两处一起改） */
    private static final String CERT_BUSINESS_LICENSE = "BUSINESS_LICENSE";
    /** 证件类型：税务登记 */
    private static final String CERT_TAX = "TAX";

    private final PartyMirrorMapper mirrorMapper;

    /**
     * 只用来拿"**MyBatis 正在用的那根连接**"（见 {@link #txBoundConnection()}）。
     * 注入 SqlSessionFactory 而不是 DataSource，是因为 Spring 按 DataSource **实例**做键
     * 绑定连接，只有 MyBatis 环境里那一个实例才对得上。
     */
    private final SqlSessionFactory sqlSessionFactory;

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
        List<String> directions = directionsOf(p);
        if (directions.isEmpty()) {
            // 方向判不出来是**正常结论**（第三方服务主体），但要留痕：否则将来会有人以为漏了
            log.info("【双写】该主体不建贸易边（既非客户也非供应商，方向未定 ⇒ 不猜）: partyId={}, partyType={}, roles={}",
                    p.getId(), p.getPartyType(), StringUtils.hasText(p.getRoles()) ? p.getRoles() : "(空)");
        }
        Long tenantId = p.getTenantId();
        boolean canWriteEdges = tenantId != null;
        if (!canWriteEdges && !directions.isEmpty()) {
            // 主体档案必须有归属租户（"这条档案归哪个租户"）；没有就如实记出来，不编一个
            log.warn("【双写】主体没有 tenantId ⇒ 本次只写 party 主档、不写贸易边: partyId={}, partyName={}",
                    p.getId(), p.getPartyName());
        }
        // 主档 / 子表 / 边**同生共死**：任一失败即整体回滚，不会留下"边指向不存在的主档"
        inSavepoint("party 主档 + 子表 + 贸易边", p.getId(), p.getPartyName(), () -> {
            mirrorMapper.upsertParty(p);
            mirrorSubTables(p);
            if (!canWriteEdges) {
                return;
            }
            for (String direction : directions) {
                mirrorMapper.upsertEdge(tenantId, p.getId(), direction, p);
            }
        });
    }

    /**
     * 三张子表（证件 / 银行 / 地址）的镜像 —— 批 2b（`V11.521.0`）。
     *
     * <p>⚠️ 映射与「只在有值时才建行」的口径**与批 1 的回填完全对齐**（`V11.506.0` §5.2~5.4）：
     * 证件各 `BUSINESS_LICENSE`/`TAX` 一条、银行一条默认账户、地址一条 `address_type = 1`。
     * 两条路落出来的行形状必须一样，否则切读时同一主体会出现两种形状。</p>
     *
     * <p>⚠️ 源列**被清空时必须软删**镜像行：只 upsert 不软删的话，用户把银行账号删掉之后
     * 镜像里还留着一条旧账户，而这类"多出来的"漂移**对账脚本看不见**（它只比对"缺").</p>
     */
    private void mirrorSubTables(Party p) {
        Long id = p.getId();
        // 证件①：营业执照（含到期日）
        if (StringUtils.hasText(p.getBusinessLicense())) {
            mirrorMapper.upsertCert(id, CERT_BUSINESS_LICENSE, p.getBusinessLicense(), p.getBusinessLicenseExpiry());
        } else {
            mirrorMapper.softDeleteCert(id, CERT_BUSINESS_LICENSE);
        }
        // 证件②：税务登记
        if (StringUtils.hasText(p.getTaxNumber())) {
            mirrorMapper.upsertCert(id, CERT_TAX, p.getTaxNumber(), null);
        } else {
            mirrorMapper.softDeleteCert(id, CERT_TAX);
        }
        // 银行：源表那唯一一个账户 = 默认账户
        if (StringUtils.hasText(p.getBankName()) || StringUtils.hasText(p.getBankAccount())) {
            mirrorMapper.upsertDefaultBank(id, p);
        } else {
            mirrorMapper.softDeleteDefaultBank(id);
        }
        // 地址：注册地址
        if (StringUtils.hasText(p.getAddress()) || StringUtils.hasText(p.getCity())
                || StringUtils.hasText(p.getProvince())) {
            mirrorMapper.upsertPrimaryAddress(id, p);
        } else {
            mirrorMapper.softDeletePrimaryAddress(id);
        }
    }

    /**
     * 主体被删除（逻辑删除）后调用：同步软删**主档 + 三张子表 + 全部贸易边**。
     *
     * <p>⚠️ 子表必须一起删（2026-09-27 补）：批 2b 把三张子表纳入双写时只加了"写"忘了"删" ⇒
     * 删掉一个主体后，它的证件/银行/地址行还挂着 `deleted = 0`，指向一个已软删的主体 ——
     * 而 `V11.520.0`/`V11.521.0` 的迁移自检里**恰好有"子表不悬空"这条断言**
     * （所以这个洞下次跑迁移就会炸出来，但那是迁移期才发现，已经晚了一步）。
     * 删主体时一并软删，语义上也才对：证件/银行/地址是主体的附属物，主体没了它们就不该在。</p>
     */
    public void onDelete(Long partyId) {
        if (partyId == null) {
            return;
        }
        inSavepoint("软删同步", partyId, null, () -> {
            mirrorMapper.softDeleteEdges(partyId);
            mirrorMapper.softDeleteCert(partyId, CERT_BUSINESS_LICENSE);
            mirrorMapper.softDeleteCert(partyId, CERT_TAX);
            mirrorMapper.softDeleteDefaultBank(partyId);
            mirrorMapper.softDeletePrimaryAddress(partyId);
            mirrorMapper.softDeleteParty(partyId);
        });
    }

    /**
     * 把一次双写包进 **SAVEPOINT**，失败只回滚双写自己，**绝不连累建档**。
     *
     * <p>⚠️ 为什么"在 Java 里 catch 住"远远不够 —— 2026-09-27 实机踩到并已复现：</p>
     * <p>PostgreSQL 里**任何一条语句报错**都会让整个事务进入 aborted 状态：后续语句全部被拒，
     * 提交时**静默变成回滚**。当时的现场是 `POST /erp/md/customer` 返回 **HTTP 200**，
     * 但 `biz_party` 里**根本没有这一行**（起因只是 `party_tenant.price_track_enabled`
     * 布尔列收到了整型参数）。对照实验：不建贸易边的主体正常落库，建边失败的主体
     * 连 `biz_party` 一起消失 —— 也就是说"双写失败不阻断建档"这条裁定，
     * **只靠 try/catch 是拦不住的，必须回滚到保存点**。</p>
     *
     * <h3>语义：一次写入 = 一个保存点 = 要么全成、要么只回滚双写</h3>
     * 一个保存点管住"主档 + 全部边"，避免出现"边指向不存在的主档"这种半截状态；
     * 缺口统一交给 {@code tools/sync-party-from-biz-party.cjs} 幂等补齐。
     *
     * @param what     写的是什么（进日志，便于定位是哪一半出了问题）
     * @param partyId  主体 id（进日志）
     * @param partyName 主体名称（进日志；软删场景没有，传 null）
     */
    private void inSavepoint(String what, Long partyId, String partyName, Runnable work) {
        Connection conn = txBoundConnection();
        Savepoint savepoint = null;
        try {
            if (conn != null) {
                savepoint = conn.setSavepoint();
            }
            work.run();
        } catch (Exception e) {
            if (savepoint != null) {
                try {
                    conn.rollback(savepoint);
                } catch (Exception nested) {
                    // 连保存点都回不去 ⇒ 外层事务确实被拖坏了。这是必须让人看见的严重情况
                    log.error("【双写】回滚到保存点失败，外层事务可能已被拖坏: partyId={}, 原因={}",
                            partyId, nested.getMessage());
                }
            }
            log.error("【双写】{} 写入失败 ⇒ 本次双写已整体回滚（`biz_party` 不受影响；"
                            + "请跑 tools/sync-party-from-biz-party.cjs 对账补数）: partyId={}, partyName={}, 原因={}",
                    what, partyId, partyName, e.getMessage());
            return;
        }
        if (savepoint != null) {
            try {
                conn.releaseSavepoint(savepoint);
            } catch (Exception ignored) {
                // 释放失败无害：提交时 PostgreSQL 自会清理子事务
            }
        }
    }

    /**
     * 取"当前事务绑定的那根 JDBC 连接"——就是 **MyBatis 自己正在用的那根**。
     *
     * <h3>为什么不走 Spring 的 {@code TransactionStatus.createSavepoint()}</h3>
     * 本应用的 {@code PlatformTransactionManager} 是 {@code JpaTransactionManager}，
     * 它的 {@code createSavepoint()} 会直接抛
     * 「JpaDialect does not support savepoints - check your JPA provider's capabilities」
     * （2026-09-27 实测，日志里连一次镜像都没写成）。而保存点其实是**连接级**能力：
     * 只要能拿到事务绑定的那根连接，用 JDBC 的
     * {@code setSavepoint / rollback(savepoint) / releaseSavepoint} 语义完全等价，
     * 还不必新开事务（新开事务会在外层回滚时留下孤儿镜像行）。
     *
     * @return 事务绑定的连接；**没有事务、或拿不到绑定的连接时返回 {@code null}**
     *         —— 调用方退化为"直接执行 + catch"：此时单条语句自带隔离，
     *         失败不会牵连任何别人，不需要保存点。
     */
    private Connection txBoundConnection() {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            return null;
        }
        DataSource mybatisDataSource = sqlSessionFactory.getConfiguration().getEnvironment().getDataSource();
        Object resource = TransactionSynchronizationManager.getResource(mybatisDataSource);
        if (resource instanceof ConnectionHolder holder) {
            try {
                // ConnectionHolder 在"绑定了但还没开连接"时 getConnection() 会抛 IllegalStateException
                // （hasConnection() 是 protected，只能这么探），那就是拿不到 ⇒ 交给调用方退化处理
                return holder.getConnection();
            } catch (IllegalStateException noConnectionYet) {
                return null;
            }
        }
        return null;
    }
}
