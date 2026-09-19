package cn.aiedge.tenant.rebuild;

import cn.aiedge.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

/**
 * 「系统重建」执行引擎（设置 → 账套操作 → 系统重建，菜单 70560 / {@code set:rebuild}）。
 *
 * <h2>为什么是「清数据」而不是「台账列表」</h2>
 * <p>对标 ql361 实测（2026-09-18）本页是<b>危险操作台</b>：红色不可恢复警告 + 2 列（选项/描述）
 * 的 <b>12 个清除范围复选项</b> + <b>必输登录密码</b> + 一个「确定」按钮。
 * 原实现把本页做成了「异步任务只读列表」，与对标<b>模型完全错位</b>（见《系统重建开发文档》§1.1）。</p>
 *
 * <h2>安全红线（本类的全部设计围绕这 4 条）</h2>
 * <ol>
 *   <li><b>只清当前租户</b>：每条 SQL 都显式带 {@code tenant_id = ?}（本表没有该列时，落在
 *       {@code ... IN (SELECT id FROM 父表 WHERE tenant_id = ?)} 的<b>子查询内部</b>，租户条件一样不可省），
 *       且<b>不使用</b> MyBatis-Plus（走 {@link JdbcTemplate} 直连），从根上排除「租户拦截器漏注入」的风险。
 *       执行前逐表预检租户限定链是否成立（自有 {@code tenant_id} 列，或父表有该列且关联列真实存在）——
 *       不成立的表<b>直接阻断</b>整个请求并如实告知「不支持」，绝不退化成「清全表」。</li>
 *   <li><b>绝不用 TRUNCATE</b>：一律 {@code DELETE}（可回滚、不重置序列、不隐式提交）。
 *       期初类数据（往来期初 / 银行现金期初）的语义是「数值归零」，走 {@code UPDATE ... SET 0}，
 *       也不是「只改个状态位」。</li>
 *   <li><b>单事务</b>：{@link #execute} 标 {@code @Transactional(rollbackFor = Exception.class)}，
 *       任一选项失败则整体回滚（不做「部分成功」——危险操作宁可全不做，也不留半清状态）。</li>
 *   <li><b>先校验再动手</b>：密码与范围校验在控制器完成；本类还会做「目标表存在 + 有 tenant_id 列」
 *       的预检，预检失败<b>一行都不删</b>。</li>
 * </ol>
 *
 * <h2>12 个选项 → 真实表映射（devdb 实测 2026-09-18）</h2>
 * <pre>
 *  1 业务草稿      → 13 张单据主表的 status=0 行 + 12 张明细表（按主表 id 子查询）
 *  2 库存期初      → erp_stock.is_initial = 1
 *  3 买家账号      → shop_user
 *  4 往来期初      → biz_party.opening_receivable/opening_payable/opening_prepaid/opening_pre_received 归零
 *  5 银行现金期初  → finance_account.balance 归零
 *  6 商品          → erp_product 及其从属数据（条码/等级价/图片/单位/属性值/货位绑定/推荐关系）+ erp_product_category
 *  7 仓库区域      → erp_warehouse_category + wms_location（对标描述含「和所有货位信息」）
 *  8 货位信息      → wms_location
 *  9 往来单位      → biz_party + biz_party_contact + biz_party_address（经 party_id）
 *                    + shop_user_party_link（经 party_id）
 * 10 职员          → hr_employee
 * 11 操作员        → sys_user（排除本人与超管）+ sys_user_role + sys_user_tenant
 * 12 发票          → invoice
 * </pre>
 * <p>其中 {@code biz_party_address} / {@code shop_user_party_link} <b>无 tenant_id 列</b>，
 * 经关联列 {@code party_id → biz_party.tenant_id} 子查询限定租户清理（见 {@link #delByParent}）；
 * 预检会校验「关联列存在 + 父表有 tenant_id」，任何一项缺失即整体拒绝。</p>
 * <p>⚠️ 如实登记的缺口（见每个 {@link Scope#note()}）：{@code biz_party_role} 是**无租户维度的
 * 往来单位角色字典**（不挂在 biz_party 上），无法限定租户 → <b>不清</b>，页面提示；
 * 其余从属表（发票明细、HR 合同、联系人渠道/证件等）为保守起见也未纳入。</p>
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SystemRebuildService {

    private final JdbcTemplate jdbcTemplate;

    /** 单个目标的执行 SQL 与参数装配器 */
    public record Target(
            /** 目标表名（仅用于回显与预检，不参与拼接） */
            String table,
            /** DELETE = 真删；UPDATE = 期初归零（非状态位） */
            String mode,
            /** 影响行数预估 SQL（与 execSql 同 WHERE 口径） */
            String countSql,
            /** 实际执行 SQL（DELETE / UPDATE） */
            String execSql,
            /** 参数装配器：(租户ID, 当前用户ID) → SQL 占位符数组 */
            BiFunction<Long, Long, Object[]> params,
            /** 该表的补充说明（缺口 / 取舍），如实标注 */
            String note,
            /**
             * 父表名（**本表无 tenant_id 列、经父表子查询限定租户**时必填；null = 本表自有 tenant_id 列）。
             *
             * <p>仅用于预检：父表必须存在且父表必须有 tenant_id 列，关联列必须存在于本表。</p>
             */
            String parentTable,
            /** 关联父表的列名（{@link #parentTable} 非空时必填） */
            String parentFkColumn
    ) {
        /** 自有 tenant_id 列的常规目标（绝大多数表） */
        public Target(String table, String mode, String countSql, String execSql,
                      BiFunction<Long, Long, Object[]> params, String note) {
            this(table, mode, countSql, execSql, params, note, null, null);
        }
    }

    /** 一个清除范围（= 对标页面上的一个复选项） */
    public record Scope(
            /** 范围键（前后端唯一契约，前端只用它提交） */
            String key,
            /** 选项名（逐字取自 ql361 实测） */
            String name,
            /** 描述（逐字取自 ql361 实测） */
            String description,
            /** 目标表集合（按执行顺序：先子表后主表） */
            List<Target> targets,
            /** 该范围的缺口 / 取舍说明 */
            String note
    ) {
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  目标构造小工具（统一保证 WHERE 首条就是 tenant_id = ?）
    // ═══════════════════════════════════════════════════════════════════════

    /** 单租户参数（绝大多数表） */
    private static final BiFunction<Long, Long, Object[]> T1 = (tid, uid) -> new Object[]{tid};

    /** 真删：{@code DELETE FROM t WHERE tenant_id = ? [AND extra]} */
    private static Target del(String table, String extraWhere) {
        String where = where(extraWhere);
        return new Target(table, "DELETE",
                "SELECT count(*) FROM " + table + " WHERE " + where,
                "DELETE FROM " + table + " WHERE " + where,
                T1, null);
    }

    /** 真删 + 缺口说明 */
    private static Target del(String table, String extraWhere, String note) {
        Target t = del(table, extraWhere);
        return new Target(t.table(), t.mode(), t.countSql(), t.execSql(), t.params(), note);
    }

    /**
     * 真删 —— {@code tenant_id} 是 <b>varchar</b> 的例外表（devdb 实测：{@code expense_application}、
     * {@code invoice} 两张 JPA 表的 tenant_id 列类型为 character varying）。
     *
     * <p>这两张表不能直接写 {@code tenant_id = ?}：Postgres 会报
     * 「操作符不存在: character varying = bigint」。因此参数侧显式 {@code CAST(? AS varchar)}，
     * 并把租户 id 以字符串绑定（双重保险），语义仍是「只清当前租户」。</p>
     */
    private static Target delTextTenant(String table, String extraWhere, String note) {
        String where = "tenant_id = CAST(? AS varchar)"
                + ((extraWhere == null || extraWhere.isBlank()) ? "" : " AND " + extraWhere);
        return new Target(table, "DELETE",
                "SELECT count(*) FROM " + table + " WHERE " + where,
                "DELETE FROM " + table + " WHERE " + where,
                (tid, uid) -> new Object[]{String.valueOf(tid)}, note);
    }

    /**
     * 期初归零（UPDATE，不是 setStatus）。
     * <p>countSql 与 execSql 用同一条件：只统计/只更新<b>真正需要变动</b>的行（值 <> 0），
     * 避免把「已经是 0」的行也算成「清理了 N 行」。</p>
     */
    private static Target reset(String table, String setClause, String nonZeroCondition, String note) {
        String where = where(nonZeroCondition);
        return new Target(table, "UPDATE",
                "SELECT count(*) FROM " + table + " WHERE " + where,
                "UPDATE " + table + " SET " + setClause + " WHERE " + where,
                T1, note);
    }

    /**
     * 真删 —— <b>本表无 {@code tenant_id} 列</b>的子表：经父表子查询限定租户。
     *
     * <p>2026-09-18 补齐 README §10.5 缺口：{@code biz_party_address} / {@code shop_user_party_link}
     * 自身没有 {@code tenant_id}（实测 information_schema），原先整表被挡在预检之外、**完全不清**；
     * 但两者都经外键列挂在<b>有 tenant_id 的主表</b>（{@code biz_party}）上，故改为：</p>
     * <pre>
     *   DELETE FROM 子表 WHERE 关联列 IN (SELECT id FROM 父表 WHERE tenant_id = ?)
     * </pre>
     * <p>租户条件落在**子查询内部**（子查询不可省，否则退化为清全表 = 跨租户误删）；
     * 仍是 DELETE 而非 TRUNCATE；与其它目标同一事务、同一批 SQL 顺序（先子表后主表）。</p>
     *
     * <p>⚠️ 关联列必须实测确认（本表真有的外键语义列），不得猜；找不到可靠关联列的表
     * <b>一律不纳入</b>（宁可不做，不许越权删）。</p>
     */
    private static Target delByParent(String childTable, String fkColumn, String parentTable, String note) {
        String where = fkColumn + " IN (SELECT id FROM " + parentTable + " WHERE tenant_id = ?)";
        return new Target(childTable, "DELETE",
                "SELECT count(*) FROM " + childTable + " WHERE " + where,
                "DELETE FROM " + childTable + " WHERE " + where,
                T1, note, parentTable, fkColumn);
    }

    /** 业务草稿的明细表：按主表 id 子查询删除（主表的草稿条件由调用方给出） */
    private static Target draftItems(String itemTable, String fkColumn, String parentTable, String parentDraftCond) {
        String where = "tenant_id = ? AND " + fkColumn + " IN (SELECT id FROM " + parentTable
                + " WHERE tenant_id = ? AND " + parentDraftCond + ")";
        return new Target(itemTable, "DELETE",
                "SELECT count(*) FROM " + itemTable + " WHERE " + where,
                "DELETE FROM " + itemTable + " WHERE " + where,
                (tid, uid) -> new Object[]{tid, tid}, null);
    }

    private static String where(String extra) {
        return (extra == null || extra.isBlank()) ? "tenant_id = ?" : "tenant_id = ? AND " + extra;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  12 个范围的注册表（顺序 = 对标页面行序，前端按此顺序渲染）
    // ═══════════════════════════════════════════════════════════════════════

    /** 单据草稿条件：全部单据主表的 status 均为整型 0 = 草稿（实核 information_schema） */
    private static final String DRAFT_ZERO = "status = 0";

    private static final List<Scope> SCOPES = buildScopes();

    private static List<Scope> buildScopes() {
        List<Scope> list = new ArrayList<>();

        // ── 1 业务草稿：13 张单据主表 + 12 张明细表 ────────────────────────
        // 先删明细（按草稿主表 id 子查询），再删主表本身；顺序不可颠倒。
        // expense_application 的 status 是 varchar（JPA 表），草稿态为 'DRAFT'。
        List<Target> draft = new ArrayList<>(List.of(
                draftItems("erp_sale_order_item", "order_id", "erp_sale_order", DRAFT_ZERO),
                draftItems("erp_sale_outbound_item", "outbound_id", "erp_sale_outbound", DRAFT_ZERO),
                draftItems("erp_sale_return_item", "return_id", "erp_sale_return", DRAFT_ZERO),
                draftItems("erp_sale_pre_order_item", "order_id", "erp_sale_pre_order", DRAFT_ZERO),
                draftItems("erp_purchase_order_item", "order_id", "erp_purchase_order", DRAFT_ZERO),
                draftItems("erp_purchase_inbound_item", "inbound_id", "erp_purchase_inbound", DRAFT_ZERO),
                draftItems("erp_purchase_return_item", "return_id", "erp_purchase_return", DRAFT_ZERO),
                draftItems("erp_receipt_item", "receipt_id", "erp_receipt", DRAFT_ZERO),
                draftItems("erp_payment_item", "payment_id", "erp_payment", DRAFT_ZERO),
                draftItems("erp_stock_transfer_item", "transfer_id", "erp_stock_transfer", DRAFT_ZERO),
                draftItems("erp_stock_damage_item", "damage_id", "erp_stock_damage", DRAFT_ZERO),
                draftItems("erp_stock_overflow_item", "overflow_id", "erp_stock_overflow", DRAFT_ZERO)
        ));
        for (String t : List.of("erp_sale_order", "erp_sale_outbound", "erp_sale_return", "erp_sale_pre_order",
                "erp_purchase_order", "erp_purchase_inbound", "erp_purchase_return", "erp_receipt",
                "erp_payment", "erp_stock_transfer", "erp_stock_damage", "erp_stock_overflow")) {
            draft.add(del(t, DRAFT_ZERO));
        }
        draft.add(delTextTenant("expense_application", "status = 'DRAFT'",
                "费用申请单用 varchar 状态列（草稿态 'DRAFT'，其余单据为整型 0）；该表 tenant_id 亦为 varchar，SQL 已显式 CAST"));
        list.add(new Scope("draft", "业务草稿", "将清除所有业务草稿", List.copyOf(draft),
                "仅清「草稿态」单据（不动已提交/已审核单据）；含 12 张明细表。"
                        + "其他模块的草稿态文档未纳入（未取证，保守不删）。"));

        // ── 2 库存期初 ────────────────────────────────────────────────────
        list.add(new Scope("stock_initial", "库存期初", "将清除所有库存期初",
                List.of(del("erp_stock", "is_initial = 1")),
                "erp_stock 中 is_initial = 1 的行；不动日常库存行（is_initial = 0）。"));

        // ── 3 买家账号 ────────────────────────────────────────────────────
        list.add(new Scope("buyer_account", "买家账号", "将清除所有买家账号",
                List.of(del("shop_user", null)),
                "商城买家账号（shop_user）。买家与往来单位的互联关联（shop_user_party_link，无 tenant_id 列）"
                        + "归第 9 项「往来单位」按 party_id 方向清理（对标文案「包含互联好友」），本项不重复处理。"));

        // ── 4 往来期初（归零，非删除） ────────────────────────────────────
        String openingCond = "(COALESCE(opening_receivable, 0) <> 0 OR COALESCE(opening_payable, 0) <> 0"
                + " OR COALESCE(opening_prepaid, 0) <> 0 OR COALESCE(opening_pre_received, 0) <> 0)";
        list.add(new Scope("partner_initial", "往来期初", "将清除所有期初往来账",
                List.of(reset("biz_party",
                        "opening_receivable = 0, opening_payable = 0, opening_prepaid = 0, opening_pre_received = 0",
                        openingCond,
                        "往来单位的 4 个期初金额字段归零（期初语义是数值，删行会连带删除客户/供应商档案）")),
                "本系统无独立「往来期初」表：期初往来账落在 biz_party 的 opening_* 四列上，故为「归零」而非删行。"));

        // ── 5 银行现金期初（归零，非删除） ────────────────────────────────
        list.add(new Scope("bank_cash_initial", "银行现金期初", "将清除银行现金期初",
                List.of(reset("finance_account", "balance = 0", "COALESCE(balance, 0) <> 0",
                        "银行/现金账户的余额归零（账户档案本身保留）")),
                "本系统无独立「银行现金期初」表：期初额体现在 finance_account.balance，故为「归零」而非删行。"));

        // ── 6 商品（含分类结构） ──────────────────────────────────────────
        list.add(new Scope("product", "商品", "将清除所有商品和分类结构数据",
                List.of(
                        // 先删从属数据，再删商品本体，最后删分类树
                        del("erp_product_recommend", null, "商品推荐关系（对 erp_product 有外键，必须先删）"),
                        del("erp_product_barcode", null),
                        del("erp_product_grade_price", null),
                        del("erp_product_image", null),
                        del("erp_product_unit", null),
                        del("erp_product_attribute_value", null),
                        del("erp_product_location", null, "商品×仓库货位绑定"),
                        del("erp_product", null),
                        del("erp_product_category", null, "分类结构数据（对标描述明示）")
                ),
                "不含「商品辅助资料」（单位组/品牌/等级/属性定义等字典表）与商城展示表（mall_product）—— 对标文案未提及。"));

        // ── 7 仓库区域（含货位，对标描述明示） ────────────────────────────
        list.add(new Scope("warehouse_region", "仓库区域", "将清除所有区域信息和所有货位信息",
                List.of(del("wms_location", null), del("erp_warehouse_category", null,
                        "仓库区域分类（erp_warehouse_category）")),
                "对标描述含「和所有货位信息」→ 连带清除货位（与第 8 项为包含关系，前端已做联动）。"
                        + "仓库档案本身（erp_warehouse）未纳入 —— 对标文案未提及。"));
        // 注：执行顺序上先删货位再删区域（区域是分类树，货位挂仓库），此处列表顺序即执行顺序。

        // ── 8 货位信息 ────────────────────────────────────────────────────
        list.add(new Scope("location", "货位信息", "将清除所有货位信息",
                List.of(del("wms_location", null)),
                "货位表为 wms_location（本库不存在 erp_warehouse_location / erp_warehouse_location 同族表）。"));

        // ── 9 往来单位 ────────────────────────────────────────────────────
        list.add(new Scope("partner", "往来单位", "清除所有客户、供应商信息，包含互联好友",
                List.of(
                        // 无 tenant_id 列的子表：经 party_id → biz_party 子查询限定租户（先删子表，再删主表）
                        delByParent("biz_party_address", "party_id", "biz_party",
                                "往来单位地址（收货/开票地址）。本表无 tenant_id 列（实测），经关联列 party_id → biz_party.tenant_id 限定租户"),
                        delByParent("shop_user_party_link", "party_id", "biz_party",
                                "商城买家 ↔ 往来单位的互联关联（对标文案「包含互联好友」）。本表无 tenant_id 列（实测），"
                                        + "经关联列 party_id → biz_party.tenant_id 限定租户；不按 shop_user_id 方向关联，避免删到他租户买家账号的关联行"),
                        del("biz_party_contact", null, "往来单位的联系人关联"),
                        del("biz_party", null)),
                "客户/供应商主数据现落在 biz_party（erp_partner 为历史表，已无写入）。"
                        + "biz_party_address / shop_user_party_link 无 tenant_id 列，改经父表 biz_party 子查询限定租户清理。"
                        + "biz_party_role **仍不清**：它是「往来单位角色」字典（role_name/role_code，无 tenant_id 列），"
                        + "不挂在 biz_party 上（biz_party 只用 roles 字符串存角色码），无法限定租户 → 宁可不做，不越权删。"
                        + "biz_party_contact 的渠道/证件附属表未纳入（保守）。"));

        // ── 10 职员 ───────────────────────────────────────────────────────
        list.add(new Scope("employee", "职员", "将清除职员和对应的操作员",
                List.of(del("hr_employee", null)),
                "只清职员档案（hr_employee）；「对应的操作员」由第 11 项单独勾选（对标文案的包含关系，前端联动勾选）。"
                        + "HR 合同/异动记录未纳入（保守）。"));

        // ── 11 操作员 ─────────────────────────────────────────────────────
        // ⚠️ 排除「当前登录用户」与「平台超管」：删到自己会当场摧毁会话，删到超管会锁死租户。
        String operatorCond = "id <> ? AND COALESCE(is_super_admin, false) = false";
        list.add(new Scope("operator", "操作员", "将清除操作员登录账号信息",
                List.of(
                        new Target("sys_user_role", "DELETE",
                                "SELECT count(*) FROM sys_user_role WHERE tenant_id = ? AND user_id IN (SELECT id FROM sys_user WHERE tenant_id = ? AND " + operatorCond + ")",
                                "DELETE FROM sys_user_role WHERE tenant_id = ? AND user_id IN (SELECT id FROM sys_user WHERE tenant_id = ? AND " + operatorCond + ")",
                                (tid, uid) -> new Object[]{tid, tid, uid}, null),
                        new Target("sys_user_tenant", "DELETE",
                                "SELECT count(*) FROM sys_user_tenant WHERE tenant_id = ? AND user_id IN (SELECT id FROM sys_user WHERE tenant_id = ? AND " + operatorCond + ")",
                                "DELETE FROM sys_user_tenant WHERE tenant_id = ? AND user_id IN (SELECT id FROM sys_user WHERE tenant_id = ? AND " + operatorCond + ")",
                                (tid, uid) -> new Object[]{tid, tid, uid}, null),
                        new Target("sys_user", "DELETE",
                                "SELECT count(*) FROM sys_user WHERE tenant_id = ? AND " + operatorCond,
                                "DELETE FROM sys_user WHERE tenant_id = ? AND " + operatorCond,
                                (tid, uid) -> new Object[]{tid, uid},
                                "排除当前登录用户与 is_super_admin = true 的账号（否则会当场锁死会话/租户）")
                ),
                "安全排除：当前登录账号 + 平台超管账号永不删除；关联的角色/租户绑定表同批清理。"));

        // ── 12 发票 ───────────────────────────────────────────────────────
        list.add(new Scope("invoice", "发票", "将清除所有发票",
                List.of(delTextTenant("invoice", null,
                        "invoice.tenant_id 为 varchar，SQL 已显式 CAST")),
                "只清 invoice（发票主表）；开票申请（invoice_application）与发票明细/税额表未纳入（保守）。"));

        return List.copyOf(list);
    }

    /** 全部范围（只读，顺序即页面顺序） */
    public List<Scope> scopes() {
        return SCOPES;
    }

    /** 按 key 取范围；key 不存在返回 null */
    public Scope scope(String key) {
        if (key == null) {
            return null;
        }
        for (Scope s : SCOPES) {
            if (s.key().equals(key)) {
                return s;
            }
        }
        return null;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  预检
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * 预检：每个目标都必须能<b>在 SQL 上限定租户</b>，并真实存在。
     *
     * <p>这是本项目「跨租户清数据」红线上的最后一道闸：无法限定租户的目标 → <b>整个请求直接拒绝</b>，
     * 一行都不删，并如实告知「不支持」，绝不退化成清全表。两种合法的租户限定方式：</p>
     * <ul>
     *   <li>① 本表<b>自有</b> {@code tenant_id} 列（绝大多数目标）；</li>
     *   <li>② 本表无 {@code tenant_id}，但经关联列挂在有 {@code tenant_id} 的<b>父表</b>上
     *       （{@link #delByParent}）→ 校验：本表存在 + 关联列存在 + 父表存在 + 父表有 {@code tenant_id} 列。
     *       任一缺失即判「不支持」（关联列猜错会造成越权/空删，宁可拒绝）。</li>
     * </ul>
     *
     * @return 不满足条件的目标表名列表（空 = 全部通过）
     */
    public List<String> preflight(List<String> scopeKeys) {
        Set<String> tenantScoped = new HashSet<>(jdbcTemplate.queryForList(
                "SELECT table_name FROM information_schema.columns "
                        + "WHERE table_schema = current_schema() AND column_name = 'tenant_id'",
                String.class));
        Set<String> existing = new HashSet<>(jdbcTemplate.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = current_schema()",
                String.class));
        // 列存在性（"表名.列名"）——用于校验父表式目标的关联列
        Set<String> columns = new HashSet<>(jdbcTemplate.queryForList(
                "SELECT table_name || '.' || column_name FROM information_schema.columns "
                        + "WHERE table_schema = current_schema()",
                String.class));
        Set<String> bad = new LinkedHashSet<>();
        for (String key : scopeKeys) {
            Scope s = scope(key);
            if (s == null) {
                continue;
            }
            for (Target t : s.targets()) {
                if (!existing.contains(t.table())) {
                    bad.add(t.table() + "（表不存在）");
                    continue;
                }
                if (t.parentTable() == null) {
                    if (!tenantScoped.contains(t.table())) {
                        bad.add(t.table() + "（无 tenant_id 列）");
                    }
                    continue;
                }
                // 父表式：本表关联列 + 父表租户列，缺一不可
                if (!columns.contains(t.table() + "." + t.parentFkColumn())) {
                    bad.add(t.table() + "（缺租户关联列 " + t.parentFkColumn() + "）");
                } else if (!existing.contains(t.parentTable())) {
                    bad.add(t.parentTable() + "（关联父表不存在）");
                } else if (!tenantScoped.contains(t.parentTable())) {
                    bad.add(t.parentTable() + "（关联父表无 tenant_id 列）");
                }
            }
        }
        return new ArrayList<>(bad);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  影响行数预估（dry-run）
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * 逐范围预估影响行数（不改任何数据）。
     *
     * @param tenantId  当前会话租户（服务端强制，不取前端入参）
     * @param userId    当前登录用户（用于「操作员」范围的自我排除）
     * @param scopeKeys 需要预估的范围键
     */
    public List<Map<String, Object>> estimate(Long tenantId, Long userId, List<String> scopeKeys) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (String key : scopeKeys) {
            Scope s = scope(key);
            if (s == null) {
                continue;
            }
            long total = 0;
            List<Map<String, Object>> tables = new ArrayList<>();
            for (Target t : s.targets()) {
                Integer c;
                try {
                    c = jdbcTemplate.queryForObject(t.countSql(), Integer.class, t.params().apply(tenantId, userId));
                } catch (Exception e) {
                    // 预估失败不影响页面渲染：如实给 -1（前端显示「未知」），绝不编造数字
                    log.warn("[系统重建] 预估失败 table={} err={}", t.table(), e.getMessage());
                    c = null;
                }
                long cnt = c == null ? -1L : c;
                if (cnt > 0) {
                    total += cnt;
                }
                tables.add(row("table", t.table(), "mode", t.mode(), "rows", cnt));
            }
            rows.add(row("key", s.key(), "name", s.name(), "description", s.description(),
                    "estimatedRows", total, "tables", tables, "note", s.note()));
        }
        return rows;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  执行
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * 执行系统重建（单事务，全部成功或全部回滚）。
     *
     * @param tenantId  当前会话租户（服务端强制覆盖，前端伪造无效）
     * @param userId    当前登录用户（服务端取，用于自我排除）
     * @param scopeKeys 勾选的范围键（已由控制器做非空校验）
     * @return 逐范围执行结果
     */
    @Transactional(rollbackFor = Exception.class)
    public List<Map<String, Object>> execute(Long tenantId, Long userId, List<String> scopeKeys) {
        LocalDateTime start = LocalDateTime.now();
        List<Map<String, Object>> results = new ArrayList<>();
        long grandTotal = 0;

        for (String key : scopeKeys) {
            Scope s = scope(key);
            if (s == null) {
                throw BusinessException.badRequest("未知的清除范围：" + key);
            }
            long scopeRows = 0;
            List<Map<String, Object>> tables = new ArrayList<>();
            for (Target t : s.targets()) {
                int affected = jdbcTemplate.update(t.execSql(), t.params().apply(tenantId, userId));
                scopeRows += affected;
                tables.add(row("table", t.table(), "mode", t.mode(), "rows", (long) affected));
            }
            grandTotal += scopeRows;
            results.add(row("key", s.key(), "name", s.name(), "description", s.description(),
                    "clearedRows", scopeRows, "tables", tables, "note", s.note()));
            log.warn("[系统重建] 租户 {} 清除范围「{}」{} 行", tenantId, s.name(), scopeRows);
        }

        log.warn("[系统重建] 租户 {} 执行完成：范围 {} 项，合计 {} 行，耗时 {} ms",
                tenantId, results.size(), grandTotal,
                java.time.Duration.between(start, LocalDateTime.now()).toMillis());
        return results;
    }

    /** 小工具：构造有序 Map（回显字段顺序稳定，便于前端与 E2E 断言） */
    private static Map<String, Object> row(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            m.put(String.valueOf(kv[i]), kv[i + 1]);
        }
        return m;
    }
}
