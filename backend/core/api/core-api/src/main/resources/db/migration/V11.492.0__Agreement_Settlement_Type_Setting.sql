-- =====================================================================================
-- 协议「结算方式」字段（DOMAIN-MODEL §13.3 补充口径 3 · v3.8~v3.14 七轮演进）
--
-- 【为什么要加这一项】2026-09-22 实测病根：字段字典 21 项里**没有「结算方式」**，
--   而 AgreementCreditTermProvider **一上来就问 AR_CREDIT_DAYS** ⇒
--   协议约定「现金结算」时也照样报「未约定账期」（用户第 4 轮点名的毛病）。
--
-- 【判定是两层，顺序不许反】
--   第 1 层 先取「结算方式」：无协议 / 已到期 / 没约定结算方式 ⇒ 走「无协议无特定客户模式」
--          （一律现款现结，低风险缺省）；
--   第 2 层 约定的结算方式是哪一种：现款现货（三种）与滚结 ⇒ 压根不问账期天数；
--          账期结算 ⇒ 账期天数是条件必填，缺则**拒单**。
--
-- 【为什么是扁平五项】「现款现货」在真实生意里是三种（用户第 7 轮）：
--   先款后货（款在发货前）/ 现款现结（钱货同时）/ 货到付款（货在付款前）。
--   三者都无账期，但资金与货物先后不同 ⇒ 到期日与风控含义都不同，
--   压成一个笼统的「现金」会同时丢掉这两样。
--   ⚠️ 拆成两个字段（先选现金/账期、再选子类型）会让"没选第二个"变成一种新的
--   **「未约定」态**，又得重新定它归哪一层 ⇒ 按 §13.3 的建议保持扁平。
--
-- 【为什么 required = false】「没约定结算方式」是**合法状态**（走现款现结），
--   不是配置缺项。把它设成必填等于逼所有协议都必须表态，与"无协议模式独立且合法"冲突。
--
-- 【配套改动（同一批）】AgreementCreditTermProvider 的判定顺序、CreditTermResult 新增
--   NO_CREDIT_TERM 态、BusinessAccountingServiceImpl 对账期缺项拒单、
--   采购/销售去掉硬编码的 30 天默认账期（§13.3 ⑤：无协议口径是现款现结，不是 30 天账期）。
-- =====================================================================================

-- ─────────────── 1. 新增字段字典项 ───────────────
-- sort = 55：紧挨在「结算周期」(50) 之后、「账期天数」(60) 之前 ——
-- 界面上顺序即判定顺序，让人一眼看出"先选结算方式，再谈账期天数"。
INSERT INTO agreement_setting_def
    (id, tenant_id, setting_key, label, value_type, options, required, consumer_point,
     semantics, sort, status, create_time, update_time, deleted)
SELECT 111222, 0, 'SETTLEMENT_TYPE', '结算方式', 'ENUM',
       'CASH_PREPAY,CASH_SPOT,CASH_ON_DELIVERY,CREDIT,ROLLING', false, 'AR_DUE_DATE',
       '决定账期天数是否适用：现款现货（先款后货/现款现结/货到付款）与滚结都无账期，'
       || '只有「账期结算」才需要填账期天数，缺天数本笔单据不许提交。'
       || '未约定 ⇒ 按现款现结处理（低风险缺省，不是平台替双方定条款）。'
       || '⚠️ 现款现货分三种：先款后货（款在发货前，通常不产生应收）/ 现款现结（钱货同时，到期日=业务日）/ '
       || '货到付款（货先到，到期日应为到货日），三者资金与货物先后不同，不许压成一个笼统的「现金」。',
       55, 1, now(), now(), 0
WHERE NOT EXISTS (SELECT 1 FROM agreement_setting_def
                   WHERE setting_key = 'SETTLEMENT_TYPE' AND deleted = 0);


-- ─────────────── 2. 自检（任一断言不成立则整个迁移回滚，不留半成品） ───────────────
DO $$
DECLARE
    n_row          int;
    n_opts         int;
    n_bad_consumer int;
    n_required     int;
    n_dup          int;
BEGIN
    -- 2.1 这一行必须**恰好一条**（重复的行会让 AgreementRuntime 取值不确定）
    SELECT count(*) INTO n_row FROM agreement_setting_def
     WHERE setting_key = 'SETTLEMENT_TYPE' AND deleted = 0;
    IF n_row <> 1 THEN
        RAISE EXCEPTION '「结算方式」应有且仅有 1 行，实际 % 行', n_row;
    END IF;

    -- 2.2 候选值必须**恰好五项**且不多不少 —— 少一项会让某种真实结算方式无值可选，
    --     多一项则是有消费方没认得的编码（枚举是扁平的，多一项就得多改一处判定）
    SELECT count(*) INTO n_opts FROM agreement_setting_def
     WHERE setting_key = 'SETTLEMENT_TYPE' AND deleted = 0
       AND options = 'CASH_PREPAY,CASH_SPOT,CASH_ON_DELIVERY,CREDIT,ROLLING';
    IF n_opts <> 1 THEN
        RAISE EXCEPTION '「结算方式」的候选值与约定的五项不符（应为 CASH_PREPAY,CASH_SPOT,CASH_ON_DELIVERY,CREDIT,ROLLING）';
    END IF;

    -- 2.3 消费方必须在 AgreementRuntime.ConsumerPoint 白名单内（否则运行时枚举认不出它）
    SELECT count(*) INTO n_bad_consumer FROM agreement_setting_def
     WHERE setting_key = 'SETTLEMENT_TYPE' AND deleted = 0
       AND consumer_point NOT IN (
        'ORDER_ROUTING','SHIPMENT_GEN','STOCK_DEDUCT','AVAILABLE_QTY','PRICING','AR_DUE_DATE','SETTLEMENT_SPLIT',
        'INVOICING','ORDER_RISK','CANCEL_FLOW','RETURN_FLOW','CLAIM_FLOW','DEPOSIT_FORFEIT');
    IF n_bad_consumer > 0 THEN
        RAISE EXCEPTION '「结算方式」的 consumer_point 不在允许的下游环节清单里';
    END IF;

    -- 2.4 必须是**非必填**：没约定结算方式是合法状态（走无协议模式），不是配置缺项
    SELECT count(*) INTO n_required FROM agreement_setting_def
     WHERE setting_key = 'SETTLEMENT_TYPE' AND deleted = 0 AND required = true;
    IF n_required > 0 THEN
        RAISE EXCEPTION '「结算方式」不得设为必填 —— 没约定它是合法状态（走无协议无特定客户模式）';
    END IF;

    -- 2.5 全表 setting_key 不许有重号（本项是新增，顺带守住"软删后重加"这类残留）
    SELECT count(*) INTO n_dup FROM (
        SELECT setting_key FROM agreement_setting_def WHERE deleted = 0
         GROUP BY setting_key HAVING count(*) > 1) x;
    IF n_dup > 0 THEN
        RAISE EXCEPTION 'agreement_setting_def 有 % 个 setting_key 重复', n_dup;
    END IF;
END $$;
