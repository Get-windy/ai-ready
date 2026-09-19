-- =============================================================================
-- 设置模块 → 系统参数（菜单 80621 / set:sys-params）· 补齐其余 6 个视图的配置项
-- （2026-09-18，依据 ql361 实机深度取证：tool-results/ql361/设置-deep/ 的
--   _summary.md「一、系统参数」整章 + 8 个标签的 JSON / PNG）
--
-- 【修的是什么】
--   V11.393.0 只 seed 了「行业设置」（industry）一个视图（当时 ql361 未采集到其余标签的内容），
--   系统参数页 8 个左标签里 6 个是**真实空态**（见《系统参数开发文档》§10.5 / §12）。
--   上一轮已用真实登录态抓完 ql361「设置 → 系统配置 → 系统参数」的全部 8 个标签，
--   本迁移把那批证据里**真实采集到**的配置项落成 sys_config 的 seed。
--
-- 【证据来源与口径（逐条可回溯）】
--   1. 卡片式标签（行业设置 / 库存设置 / 数据权限 / 其他）：取 JSON 的
--      `内容.cards[].配置项[]`（名称 / 控件 / 当前值 / 帮助文案_属性 / 温馨提示）。
--   2. 非卡片式标签（流程启用 / 单据设置 / 财务设置 / 消息提醒）：取 JSON 的
--      `通用形态.controls[]`（控件 / 标签 / 值）+ `通用形态.列表项[]`（结构顺序）
--      + `帮助属性汇总`（气泡正文）+ `系统参数-下拉选项-<标签>.json`（选项集）。
--   3. 逐字原则：`param_name` 一律照抄取证文本（含「：」等标点）；`help_text` / `tip_text`
--      照抄气泡正文（页面前端自己加「温馨提示：」前缀，故这里只存正文）。
--   4. 值口径：`param_value` 存文本（sys_config.param_value 是 text 列）；
--      布尔 true/false；枚举用**语义化 slug**（中文标签在 `.value-options` 里给，
--      避免把中文当键）；多段控件用逗号分隔。
--   5. 幂等：INSERT ... ON CONFLICT (param_key) DO NOTHING；UPDATE 只填空值列
--      （`WHERE ... IS NULL`）→ 重复执行不覆盖任何已有内容。
--
-- 【⚠️ 没有编造的部分（如实登记，禁止补）】
--   · 「消息提醒」的**控件清单是一份扁平 dump**（`通用形态.controls` 42 条），报告未把
--     每个勾选框归属到具体行。本迁移用「DOM 顺序 + 计数恒等式」做归属（见 §8 注释）：
--     列表项要求 33 个勾选框、dump 里恰好 33 个勾选框（idx1 + idx2..41 中的 32 个），
--     9 个文本输入（1 个「提前 N 天」+ 8 个收件人）与行结构一致 → 归属可用。
--     两处**缺失**不补：① idx1 那个无标签勾选框（归属不明，未落库）；
--     ② 货位预警补货通知的「系统通知」勾选框在 dump 里缺失 → 该行值按未勾选存放并在
--     remark 里标注「勾选状态未取到」。
--   · 「消息提醒」各行的**收件人文本**（如「杨生淮,高晓丽」）是 ql361 的演示用户，
--     与本系统用户无关 → 不落库（只落「短信通知 / 系统通知」两种通知方式开关）。
--   · 「短信通知」那条气泡正文（配置客户在商城线下转账付款时，通知的方式及人员）在报告里
--     归属不明（帮助属性汇总把它挂在「短信通知」这个 label 上，无法定位到具体行）→ 不挂载。
--   · 勾选状态未取到的行（流程启用·自提 / 拒收数量参与补单、单据设置·多仓库成本调价 /
--     启用销售税率 / 启用商品行属性管理 / 启用采购税率、库存设置·账面库存允许为负）：
--     值按未勾选存放，并在 `remark` 里逐条标注「勾选状态未取到（控件清单中无此项）」。
--   · 报告「未取到清单」§4.3 登记的「因卡片开关=关导致卡片体隐藏」的卡片：
--     这些卡片的**配置项清单本身取到了**（标题/开关/隐藏体内的项），故照采落库；
--     只是「卡片体展开后的可见性」未取到 —— 本页不模拟 ql361 的「整卡折叠」行为。
--   · 不落库的**按钮/链接型**项（启用电子秤旁的「设置」、启用销售类型管理旁的「设置」、
--     启用商品行属性管理旁的「设置」、其他·显示控制旁的「去设置」、元气经销商配置的
--     「OMS采购订单默认供应商」选择器）：前四个不是配置项，第五个是往来单位选择器
--     （本系统无对应控件）→ 选择器以文本行落库并在 remark 说明。
--
-- 【值类型的补充词汇表】
--   本迁移引入两种前面没有的 value_type，供「非卡片式标签」的分组结构使用：
--     · 'group'  分组/小标题行（自身无控件，仅作为子项的挂载点，页面渲染为一张无右侧
--                控件的卡片或一行标题）—— 用于「消息提醒」的事件分组与收件角色两层结构
--                （ql361 该标签是三层结构：通知事件 → 通知对象 → 通知方式），
--                以及「分享设置 / 锁屏设置 / 溯源系统对接设置 / 采购价格异常提醒」这类小节。
--     · 小数位数 / 税率 / 天数等数值项统一用 'number'（页面渲染数字输入框）。
-- =============================================================================

-- ── 0. 先给 V11.393.0 seed 的 10 行补上「当时未采集到」的 `?` 气泡正文 ──────────
-- 只填空值（WHERE help_text IS NULL）→ 不覆盖、不重命名任何已有行。
UPDATE sys_config SET help_text = '属性主要用于服装、五金、通讯等行业，实现对商品的多规格、多型号管理，启用属性管理后，可以对商品每个属性的价格、库存数量分开管理'
 WHERE param_key = 'industry.product.specAttr' AND help_text IS NULL;
UPDATE sys_config SET help_text = '适用于食品、化妆品、医药行业'
 WHERE param_key = 'industry.batch.enabled' AND help_text IS NULL;
UPDATE sys_config SET help_text = '启用后，则新增商品时，必须录入保质期天数；采购、销售时，必须选择录入生产、到期日期。'
 WHERE param_key = 'industry.batch.shelfLife' AND help_text IS NULL;
UPDATE sys_config SET help_text = '启用后，则采购时，必须录入批号；销售时，可不录入，默认先入先出的原则出库。'
 WHERE param_key = 'industry.batch.batchNo' AND help_text IS NULL;
UPDATE sys_config SET help_text = '未启用，则采购、销售时录入的所有批号自动转为大写【如：录入aBE，转化为ABE】；启用，则保持录入的内容。'
 WHERE param_key = 'industry.batch.batchNoCase' AND help_text IS NULL;
UPDATE sys_config SET help_text = '启用后，系统会根据规则自动给批次生成条码。'
 WHERE param_key = 'industry.batch.barcodeRule' AND help_text IS NULL;
UPDATE sys_config SET help_text = '批次商品成本与系统成本核算方法的关系： 系统成本核算方法为“先进先出”时，批次商品成本规则只能设为“按商品批次成本”； 系统成本核算方法为“移动加权平均”时，批次商品成本规则可设置'
 WHERE param_key = 'industry.batch.costRule' AND help_text IS NULL;
UPDATE sys_config SET help_text = '自动出库：可直接录入出库数量，系统根据临近效期先出原则自动出库；手动出库：需要手动选择出库批次'
 WHERE param_key = 'industry.batch.outboundRule' AND help_text IS NULL;
UPDATE sys_config SET help_text = '启用后销售订单、调拨申请单必须录入批号保质期信息，若未录入批次则提单时系统会根据临近效期先出原则自动分配批次'
 WHERE param_key = 'industry.batch.orderShelfLife' AND help_text IS NULL;
UPDATE sys_config SET help_text = '适用于电脑数码、通讯行业。启用，则采购、销售序列号商品时，需录入商品的序列号（串号），对各序列号独立管理'
 WHERE param_key = 'industry.serial.enabled' AND help_text IS NULL;

-- ── 1. 行业设置（industry）· 补齐两张隐藏卡片的子项 ─────────────────────────
-- 商品规格属性（卡片开关=关 → 卡片体隐藏，但 5 个子项的名称/类型/当前值/气泡都取到了）
-- 序列号管理（同上，2 个子项）
INSERT INTO sys_config
    (param_key, param_name, param_value, config_type, config_group, nav_group, parent_key,
     value_type, builtin, remark, help_text, tip_text, sort_order, enabled, locked, tenant_id, deleted)
VALUES
    ('industry.product.attr1Enabled', '启用商品属性1', 'false', 'business', 'industry', 'industry', 'industry.product.specAttr',
     'boolean', true, '商品规格属性·属性1开关（ql361 实测默认：关）', NULL, NULL, 11, true, false, 0, 0),
    ('industry.product.attr1Name', '属性1名称：', '口味', 'business', 'industry', 'industry', 'industry.product.specAttr',
     'string', true, '商品规格属性·属性1的名称（ql361 实测值：口味）', NULL, NULL, 12, true, false, 0, 0),
    ('industry.product.attr1Image', '关联图片', 'false', 'business', 'industry', 'industry', 'industry.product.specAttr',
     'boolean', true, '商品规格属性·属性1是否关联图片（ql361 实测默认：关）',
     '选择属性关联图片，可以对每个属性单独上传图片，商城选择属性，直接显示各属性的图片', NULL, 13, true, false, 0, 0),
    ('industry.product.attr2Enabled', '启用商品属性2', 'false', 'business', 'industry', 'industry', 'industry.product.specAttr',
     'boolean', true, '商品规格属性·属性2开关（ql361 实测默认：关）', NULL, NULL, 14, true, false, 0, 0),
    ('industry.product.attr2Name', '属性2名称：', '型号', 'business', 'industry', 'industry', 'industry.product.specAttr',
     'string', true, '商品规格属性·属性2的名称（ql361 实测值：型号）', NULL, NULL, 15, true, false, 0, 0),
    ('industry.serial.unforced', '非强制录入', 'false', 'business', 'industry', 'industry', 'industry.serial.enabled',
     'boolean', true, '序列号管理·是否允许不录入序列号（ql361 实测默认：关）',
     '启用后，单据中序列号商品可以不录入序列号信息，只是填写数量进行出入库；未启用的话，单据中序列号商品数量与序列号个数必须一致', NULL, 71, true, false, 0, 0),
    ('industry.serial.caseSensitive', '序列号区分大小写', 'false', 'business', 'industry', 'industry', 'industry.serial.enabled',
     'boolean', true, '序列号管理·序列号是否区分大小写（ql361 实测默认：关）',
     '未启用，则采购、销售时录入的所有串码自动转为大写【如：录入aBE，转化为ABE】；启用，则保持录入的内容。', NULL, 72, true, false, 0, 0)
ON CONFLICT (param_key) DO NOTHING;

-- ── 2. 流程启用（flow）──────────────────────────────────────────────────────
-- 结构（ql361 面板正文）：销售流程设置 / 配送方式设置 / 收发货配置 / 其他业务。
-- 「销售流程设置 提交销售订单 发货 配送」是流程图（非配置项）→ 不落库。
-- 「启用电子秤」旁有一个「设置」按钮（第二层弹层未打开）→ 只落开关，不落二级内容。
INSERT INTO sys_config
    (param_key, param_name, param_value, config_type, config_group, nav_group, parent_key,
     value_type, builtin, remark, help_text, tip_text, sort_order, enabled, locked, tenant_id, deleted)
VALUES
    ('flow.delivery.self', '自配', 'false', 'business', 'flow', 'flow', NULL,
     'boolean', true, '配送方式设置·自配（ql361 实测默认：未勾选）',
     '启用后,销售流程设置中的配送将启用', NULL, 10, true, false, 0, 0),
    ('flow.delivery.selfToLogistics', '自配到物流', 'true', 'business', 'flow', 'flow', NULL,
     'boolean', true, '配送方式设置·自配到物流（ql361 实测默认：勾选）',
     '启用后,销售流程设置中的配送将启用', NULL, 20, true, false, 0, 0),
    ('flow.delivery.logistics', '物流', 'false', 'business', 'flow', 'flow', NULL,
     'boolean', true, '配送方式设置·物流（ql361 实测默认：未勾选）', NULL, NULL, 30, true, false, 0, 0),
    ('flow.delivery.pickup', '自提', 'false', 'business', 'flow', 'flow', NULL,
     'boolean', true, '配送方式设置·自提。⚠️ 勾选状态未取到（控件清单中无此项，只有标签与面板正文）→ 按未勾选存放', NULL, NULL, 40, true, false, 0, 0),
    ('flow.delivery.defaultMode', '默认配送方式：', 'none', 'business', 'flow', 'flow', NULL,
     'enum', true, '默认配送方式（ql361 实测：当前值「请选择」；展开选项只有「请选择 / 物流」两项，其余配送方式未显示为可选）', NULL, NULL, 50, true, false, 0, 0),
    ('flow.receive.beforePayment', '必须先收款后发货', 'true', 'business', 'flow', 'flow', NULL,
     'boolean', true, '收发货配置·必须先收款后发货（ql361 实测默认：勾选）',
     '启用后，进销存后台新增的销售订单需要先收订单全款后才可发货', NULL, 60, true, false, 0, 0),
    ('flow.receive.overShip', '允许超量发货', 'false', 'business', 'flow', 'flow', NULL,
     'boolean', true, '收发货配置·允许超量发货（ql361 实测默认：未勾选）',
     '启用后，可以超过销售订单数量进行发货', NULL, 70, true, false, 0, 0),
    ('flow.biz.vehicleSales', '启用车销业务', 'true', 'business', 'flow', 'flow', NULL,
     'boolean', true, '其他业务·启用车销业务（ql361 实测默认：勾选）。⚠️ 气泡正文未取到', NULL, NULL, 80, true, false, 0, 0),
    ('flow.biz.preOrder', '启用预订货', 'false', 'business', 'flow', 'flow', NULL,
     'boolean', true, '其他业务·启用预订货（ql361 实测默认：未勾选）',
     '适用于订货会、大型促销、年度订单等业务，预订货的商品必须先收款才能发货。', NULL, 90, true, false, 0, 0),
    ('flow.biz.expenseContract', '启用费用合同', 'true', 'business', 'flow', 'flow', NULL,
     'boolean', true, '其他业务·启用费用合同（ql361 实测默认：勾选）。⚠️ 气泡正文未取到', NULL, NULL, 100, true, false, 0, 0),
    ('flow.biz.borrow', '启用借进借出', 'false', 'business', 'flow', 'flow', NULL,
     'boolean', true, '其他业务·启用借进借出（ql361 实测默认：未勾选）',
     '适用于借欠业务，使用账内库存处理，处理过程将会影响财务数据。', NULL, 110, true, false, 0, 0),
    ('flow.biz.thirdSettlement', '启用三方结算', 'false', 'business', 'flow', 'flow', NULL,
     'boolean', true, '其他业务·启用三方结算（ql361 实测默认：未勾选）。⚠️ 气泡正文未取到', NULL, NULL, 120, true, false, 0, 0),
    ('flow.biz.scale', '启用电子秤', 'false', 'business', 'flow', 'flow', NULL,
     'boolean', true, '其他业务·启用电子秤（ql361 实测默认：未勾选；该行带「设置」按钮，二级内容未采集）', NULL, NULL, 130, true, false, 0, 0),
    ('flow.biz.rejectQtyReplenish', '拒收数量参与补单', 'false', 'business', 'flow', 'flow', NULL,
     'boolean', true, '其他业务·拒收数量参与补单。⚠️ 勾选状态未取到（只有标签与气泡，控件清单中无此项）→ 按未勾选存放',
     '启用后，订单中心履约列表将订单拒收数量纳入到未补数量内', NULL, 140, true, false, 0, 0)
ON CONFLICT (param_key) DO NOTHING;

-- ── 3. 单据设置（bill）──────────────────────────────────────────────────────
-- 结构：单据统一配置（单据编号 / 开单配置 / 小数位数 / 订单中心配置）/ 销售单据配置 /
--       采购单据配置。各小节标题不是配置项 → 不落库（页面按 sort_order 平铺）。
INSERT INTO sys_config
    (param_key, param_name, param_value, config_type, config_group, nav_group, parent_key,
     value_type, builtin, remark, help_text, tip_text, sort_order, enabled, locked, tenant_id, deleted)
VALUES
    ('bill.no.cycle', '单据编号流水：', 'day', 'business', 'bill', 'bill', NULL,
     'enum', true, '单据编号·流水周期（ql361 实测默认：按日编号）', NULL, NULL, 10, true, false, 0, 0),
    ('bill.no.digits', '流水位数：', '3', 'business', 'bill', 'bill', NULL,
     'enum', true, '单据编号·流水位数（ql361 实测默认：3）',
     '配置单据编号流水格式，按日、按月或按年统一编号；注意，流水位数只能从小到大进行切换', NULL, 20, true, false, 0, 0),
    ('bill.create.continuous', '单据连续新增', 'true', 'business', 'bill', 'bill', NULL,
     'boolean', true, '开单配置·单据连续新增（ql361 实测默认：勾选）',
     '启用后，单据草稿，提交，记账成功，系统自动新开一张空白单据页面，便于继续新增单据；未启用，则显示原单据信息', NULL, 30, true, false, 0, 0),
    ('bill.draft.continuous', '草稿连续新增', 'false', 'business', 'bill', 'bill', NULL,
     'boolean', true, '开单配置·草稿连续新增（ql361 实测默认：未勾选）',
     '启用后，单据保存草稿成功，系统自动新开一张空白单据页面；未启用，则显示原单据信息', NULL, 40, true, false, 0, 0),
    ('bill.multiWarehouse', '多仓库录单', 'false', 'business', 'bill', 'bill', NULL,
     'boolean', true, '开单配置·多仓库录单（ql361 实测默认：未勾选）',
     '启用后，支持一张单据录入多个仓库的商品。销售退货申请暂不支持', NULL, 50, true, false, 0, 0),
    ('bill.multiWarehouseCost', '多仓库成本调价', 'false', 'business', 'bill', 'bill', NULL,
     'boolean', true, '开单配置·多仓库成本调价。⚠️ 勾选状态未取到（只有标签与气泡）→ 按未勾选存放',
     '启用后，成本调价单可同时调整商品在多个仓库中的库存成本', NULL, 60, true, false, 0, 0),
    ('bill.picking.sortRule', '拣货生成的出库单据默认排序规则：', 'entryOrder', 'business', 'bill', 'bill', NULL,
     'enum', true, '拣货生成的出库单据的商品排序规则（ql361 实测默认：按录单商品顺序排列）', NULL, NULL, 70, true, false, 0, 0),
    ('bill.cost.abnormalRule', '成本异常读取原则：', 'lastPrice', 'business', 'bill', 'bill', NULL,
     'enum', true, '成本异常（无成本价/负成本价）时的默认取价规则（ql361 实测默认：最近进价）',
     '商品出入库，没有成本单价、或者成本单价为负数时，配置系统默认的取价规则', NULL, 80, true, false, 0, 0),
    ('bill.item.sortRule', '保存单据商品自动排序规则：', 'entryOrder', 'business', 'bill', 'bill', NULL,
     'enum', true, '保存单据时商品自动排序规则（ql361 实测默认：按录单排序）',
     '单据在保存时自动按照所选的规则对商品排序', NULL, 90, true, false, 0, 0),
    ('bill.transfer.defaultPrice', '调拨单默认调拨价：', 'cost', 'business', 'bill', 'bill', NULL,
     'enum', true, '调拨单默认调拨价的取价规则（ql361 实测默认：成本价）',
     '调拨单，配置调拨价默认的取价规则', NULL, 100, true, false, 0, 0),
    ('bill.account.count', '收/付款账户数：', '4', 'business', 'bill', 'bill', NULL,
     'enum', true, '收付款单可录入的账户数（ql361 实测默认：4账户）', NULL, NULL, 110, true, false, 0, 0),
    ('bill.qty.scale', '数量小数位数：', '3', 'business', 'bill', 'bill', NULL,
     'enum', true, '数量小数位数（ql361 实测默认：3位小数）', NULL, NULL, 120, true, false, 0, 0),
    ('bill.price.scale', '单价小数位数：', '4', 'business', 'bill', 'bill', NULL,
     'enum', true, '单价小数位数（ql361 实测默认：4位小数）', NULL, NULL, 130, true, false, 0, 0),
    ('bill.tab.autoRefresh', '切换页签自动刷新', 'true', 'business', 'bill', 'bill', NULL,
     'boolean', true, '订单中心配置·切换页签自动刷新（ql361 实测默认：勾选）',
     '切换页签时自动刷新页面数据，实时更新页签中列表的单据信息', NULL, 140, true, false, 0, 0),
    ('bill.sale.discount', '启用销售折扣', 'false', 'business', 'bill', 'bill', NULL,
     'boolean', true, '销售单据配置·启用销售折扣（ql361 实测默认：未勾选）',
     '启用,则新增销售类单据时系统可录入折扣、折后价、折后金额,折扣默认100,可以修改', NULL, 150, true, false, 0, 0),
    ('bill.sale.tax', '启用销售税率', 'false', 'business', 'bill', 'bill', NULL,
     'boolean', true, '销售单据配置·启用销售税率。⚠️ 勾选状态未取到（控件清单里该 checkbox 的标签被自动配对成「销售默认税率：」，无法判定归属）→ 按未勾选存放',
     '启用，则新增销售类单据时系统可录入税率，自动计算税额及税后价', NULL, 160, true, false, 0, 0),
    ('bill.sale.taxRate', '销售默认税率：', '16', 'business', 'bill', 'bill', NULL,
     'number', true, '销售类单据的默认税率（ql361 实测值：16）', NULL, NULL, 170, true, false, 0, 0),
    ('bill.sale.priceTrack', '启用销售价格跟踪', 'true', 'business', 'bill', 'bill', NULL,
     'boolean', true, '销售单据配置·启用销售价格跟踪（ql361 实测：勾选）',
     '启用后，销售类单据新增时，系统会自动带出往来单位最近的一次销售单价、折扣', NULL, 180, true, false, 0, 0),
    ('bill.sale.trackDiscount', '跟踪折扣', 'false', 'business', 'bill', 'bill', NULL,
     'boolean', true, '销售单据配置·跟踪折扣（ql361 实测：未勾选）', NULL, NULL, 190, true, false, 0, 0),
    ('bill.sale.notTrackDiscount', '不跟踪折扣', 'false', 'business', 'bill', 'bill', NULL,
     'boolean', true, '销售单据配置·不跟踪折扣（ql361 实测：未勾选；与「跟踪折扣」是一组互斥单选，ql361 采集到的两个信号都是未勾选，如实存放）', NULL, NULL, 200, true, false, 0, 0),
    ('bill.sale.trackUnit', '跟踪价格随单位联动', 'true', 'business', 'bill', 'bill', NULL,
     'boolean', true, '销售单据配置·跟踪价格随单位联动（ql361 实测：勾选）', NULL, NULL, 210, true, false, 0, 0),
    ('bill.sale.creditOver', '超过信用额度', 'notAllowed', 'business', 'bill', 'bill', NULL,
     'enum', true, '往来单位超过信用额度时的处理（ql361 实测：不允许开单）',
     '往来单位超过信用额度时，若启用了该条件的单据审核，则进入审核流程，若没有启用该条件的单据审核，则开单时会进行提醒、或者不允许开单', NULL, 220, true, false, 0, 0),
    ('bill.sale.typeManage', '启用销售类型管理', 'true', 'business', 'bill', 'bill', NULL,
     'boolean', true, '销售单据配置·启用销售类型管理（ql361 实测：勾选；该行带「设置」按钮，二级内容未采集）',
     '销售类单据，可以按单据标记销售类型，并进行汇总统计', NULL, 230, true, false, 0, 0),
    ('bill.sale.itemAttrManage', '启用商品行属性管理', 'false', 'business', 'bill', 'bill', NULL,
     'boolean', true, '销售单据配置·启用商品行属性管理。⚠️ 勾选状态未取到（只有按钮与气泡，无 checkbox 值）→ 按未勾选存放',
     '销售类单据，可以按商品行标记属性，以便进行特殊的业务处理', NULL, 240, true, false, 0, 0),
    ('bill.sale.priceAbnormal', '商品售价异常处理', 'true', 'business', 'bill', 'bill', NULL,
     'boolean', true, '销售单据配置·商品售价异常处理（ql361 实测：勾选）。⚠️ 气泡正文未取到', NULL, NULL, 250, true, false, 0, 0),
    ('bill.purchase.overReceive', '允许超量收货', 'false', 'business', 'bill', 'bill', NULL,
     'boolean', true, '采购单据配置·允许超量收货（ql361 实测默认：未勾选）',
     '启用后，可以超过采购订单数量进行收货', NULL, 260, true, false, 0, 0),
    ('bill.purchase.discount', '启用采购折扣', 'false', 'business', 'bill', 'bill', NULL,
     'boolean', true, '采购单据配置·启用采购折扣（ql361 实测默认：未勾选）',
     '启用,则新增采购类单据时系统可录入折扣、折后价、折后金额,折扣默认100,可以修改', NULL, 270, true, false, 0, 0),
    ('bill.purchase.tax', '启用采购税率', 'false', 'business', 'bill', 'bill', NULL,
     'boolean', true, '采购单据配置·启用采购税率。⚠️ 勾选状态未取到（同「启用销售税率」的标签自动配对问题）→ 按未勾选存放',
     '启用，则新增采购类单据时系统可录入税率，自动计算税额及税后价', NULL, 280, true, false, 0, 0),
    ('bill.purchase.taxRate', '采购默认税率：', '16', 'business', 'bill', 'bill', NULL,
     'number', true, '采购类单据的默认税率（ql361 实测值：16）', NULL, NULL, 290, true, false, 0, 0),
    ('bill.purchase.priceTrack', '启用采购价格跟踪', 'true', 'business', 'bill', 'bill', NULL,
     'boolean', true, '采购单据配置·启用采购价格跟踪（ql361 实测：勾选）',
     '启用后，采购类单据新增时，系统会自动带出往来单位最近的一次采购单价、折扣', NULL, 300, true, false, 0, 0),
    ('bill.purchase.priceAlert', '采购价格异常提醒', 'false', 'business', 'bill', 'bill', NULL,
     'group', true, '采购单据配置·采购价格异常提醒（ql361 面板正文里是一小节，含「相差 ± %」与「处理方式」两个子项；本节自身的勾选状态未取到 → 值不参与渲染）', NULL, NULL, 310, true, false, 0, 0),
    ('bill.purchase.priceAlertRange', '采购价与最近进价相差 ±', '2', 'business', 'bill', 'bill', 'bill.purchase.priceAlert',
     'number', true, '采购价格异常提醒·相差百分比阈值（ql361 实测值：2）',
     '当采购订单、采购入库单录入的商品价格低于或高于最近进价设置的范围时，系统则弹窗提醒', NULL, 311, true, false, 0, 0),
    ('bill.purchase.priceAlertAction', '采购价与最近进价相差 ± %时', 'notice', 'business', 'bill', 'bill', 'bill.purchase.priceAlert',
     'enum', true, '超过阈值时的处理方式（ql361 实测：仅提醒）', NULL, NULL, 312, true, false, 0, 0)
ON CONFLICT (param_key) DO NOTHING;

-- ── 4. 库存设置（stock）────────────────────────────────────────────────────
-- 「库存预警设置 / 预警补货设置」在 ql361 是**单选组**（4 选 1，卡片无开关）→ 本页按
-- 一条 enum 配置项落库（选项集 = 4 个单选标签，页面渲染为一个下拉），选项与当前选中项
-- 逐字取自取证 JSON 的 `细节.选项[].checked`。「拣货位补货设置」卡片体为空（开关=关）。
INSERT INTO sys_config
    (param_key, param_name, param_value, config_type, config_group, nav_group, parent_key,
     value_type, builtin, remark, help_text, tip_text, sort_order, enabled, locked, tenant_id, deleted)
VALUES
    ('stock.alertRule', '库存预警设置', 'bookMinusPendingGtUpper', 'business', 'stock', 'stock', NULL,
     'enum', true, '库存预警的触发口径（ql361 实测当前选中：账面库存-待发货>库存上限）。⚠️ ql361 是 4 选 1 单选组，本页以一条 enum 落库',
     '配置系统库存高于上限、低于下限时进行提醒的计算规则', NULL, 10, true, false, 0, 0),
    ('stock.replenishRule', '预警补货设置', 'upperPlusPendingMinusBookMinusIncoming', 'business', 'stock', 'stock', NULL,
     'enum', true, '预警补货的缺货数量口径（ql361 实测当前选中：缺货数量=库存上限+待发货-账面数量-待收货）。⚠️ 同上是 4 选 1 单选组',
     '配置库存预警缺货数量的计算规则', NULL, 20, true, false, 0, 0),
    ('stock.indicate.enabled', '拣货位补货设置', 'false', 'business', 'stock', 'stock', NULL,
     'boolean', true, '拣货位补货开关（ql361 实测默认：关；卡片体无配置项）', NULL, NULL, 30, true, false, 0, 0),
    ('stock.available.formula', '可用库存等于：', 'bookMinusUnsoldMinusPending', 'business', 'stock', 'stock', NULL,
     'enum', true, '可用库存的计算口径（ql361 实测当前选中：账面库存-未发数量-待记账数量）',
     '自由配置可用库存的计算规则,仓配版时，先进先出的成本计算法下不处理退货报损数量', NULL, 40, true, false, 0, 0),
    ('stock.available.pendingOrder', '审核中订单影响可用库存', 'false', 'business', 'stock', 'stock', NULL,
     'boolean', true, '可用库存规则设置·审核中订单是否影响可用库存（ql361 实测默认：未勾选）',
     '启用后，审核中的订单会影响可用库存；未启用，则只有审核通过的订单才影响可用库存', NULL, 50, true, false, 0, 0),
    ('stock.available.autoFix', '可用库存不足自动修正', 'false', 'business', 'stock', 'stock', NULL,
     'boolean', true, '可用库存规则设置·可用库存不足时是否自动修正下单数量（ql361 实测默认：未勾选）',
     '启用后，销售订单、销售出库单开单提示可用库存不足时，支持系统自动修改下单数量（修改规则：将下单数量修改为最大可用库存数量，多单位商品则按大单位到小单位的顺序依次修改。如果商品的可用库存为0则移除掉该商品的下单信息。）', NULL, 60, true, false, 0, 0),
    ('stock.negative.available', '可用库存允许为负', 'false', 'business', 'stock', 'stock', NULL,
     'boolean', true, '负库存设置·可用库存允许为负（ql361 实测默认：未勾选）',
     '系统中商品可用库存为0、或者不足时，仍然可以提交销售订单', NULL, 70, true, false, 0, 0),
    ('stock.negative.book', '账面库存允许为负', 'false', 'business', 'stock', 'stock', NULL,
     'boolean', true, '负库存设置·账面库存允许为负。⚠️ 勾选状态未取到（控件清单中无此项，只有标签与气泡）→ 按未勾选存放',
     '系统中商品账面库存为0、或者不足时，仍然可以销售记账', NULL, 80, true, false, 0, 0)
ON CONFLICT (param_key) DO NOTHING;

-- ── 5. 财务设置（finance）──────────────────────────────────────────────────
-- 结构：成本核算设置 / 月结设置 / 收付款结算设置。「自动月结 [N 月前] 的会计期间」是一个
-- 勾选框 + 一个下拉的**行内结构** → 落成父项「自动月结」+ 子项「的会计期间」。
INSERT INTO sys_config
    (param_key, param_name, param_value, config_type, config_group, nav_group, parent_key,
     value_type, builtin, remark, help_text, tip_text, sort_order, enabled, locked, tenant_id, deleted)
VALUES
    ('finance.cost.method', '成本核算方法：', 'movingAverage', 'business', 'finance', 'finance', NULL,
     'enum', true, '成本核算设置·成本核算方法（ql361 实测当前值：移动加权平均）', NULL, NULL, 10, true, false, 0, 0),
    ('finance.monthClose.auto', '自动月结', 'false', 'business', 'finance', 'finance', NULL,
     'boolean', true, '月结设置·自动月结（ql361 实测：未勾选；行内结构为「自动月结 [N] 的会计期间」）', NULL, NULL, 20, true, false, 0, 0),
    ('finance.monthClose.autoBefore', '的会计期间', '2', 'business', 'finance', 'finance', 'finance.monthClose.auto',
     'enum', true, '自动月结的提前期数（ql361 实测当前值：二月前）',
     '举例:1、 会计期间设置的2024年3月31日为期末结账日期； 2、设置的 自动月结1个月前的会计期间；3、到2024年4月1日时，系统将对2024年2月进行自动月结，2024年3月不自动月结；', NULL, 21, true, false, 0, 0),
    ('finance.monthClose.adjustDate', '月结后未记账单据日期自动修改为下个未月结会计月日期', 'false', 'business', 'finance', 'finance', NULL,
     'boolean', true, '月结设置·月结后未记账单据日期是否自动改期（ql361 实测：未勾选）',
     '单据包括 待记账的 销售出库单/销售退货单,待确认的收款单/预收款单,待审核的业务单据', NULL, 30, true, false, 0, 0),
    ('finance.payment.settleOrder', '收付款单结算顺序：', 'docOrder', 'business', 'finance', 'finance', NULL,
     'enum', true, '收付款结算设置·结算顺序。⚠️ ql361 采集到两个单选信号且自动配对互相矛盾（控件清单里「收付款单结算顺序：」勾选、「按单据录单顺序由远及近」未勾选）；当前值按面板正文的选项顺序取第 1 项存放',
     '此配置影响【web端】收付款单/【司机端】完成配送收款', NULL, 40, true, false, 0, 0)
ON CONFLICT (param_key) DO NOTHING;

-- ── 6. 数据权限（data_perm）────────────────────────────────────────────────
-- 7 张卡片：6 张是「开关 + 去设置」；只有「仓库数据权限」取到了卡片体（3 个子项）。
-- 「去设置」跳到操作员授权页（非本页配置项）→ 不落库。
INSERT INTO sys_config
    (param_key, param_name, param_value, config_type, config_group, nav_group, parent_key,
     value_type, builtin, remark, help_text, tip_text, sort_order, enabled, locked, tenant_id, deleted)
VALUES
    ('dataPerm.btype.enabled', '部门/职员数据权限', 'true', 'security', 'data_perm', 'data_perm', NULL,
     'boolean', true, '部门/职员数据权限开关（ql361 实测默认：开）',
     '启用后，可对操作员设置部门/职员数据权限；设置后，操作员在单据中只能选择所授权职员，报表只能查询所授权职员的业务数据', NULL, 10, true, false, 0, 0),
    ('dataPerm.ptype.enabled', '商品数据权限', 'true', 'security', 'data_perm', 'data_perm', NULL,
     'boolean', true, '商品数据权限开关（ql361 实测默认：开）',
     '启用后，可对操作员设置商品数据权限；设置后，操作员在单据中只能选择所授权商品信息，报表只能查询所授权商品的业务数据', NULL, 20, true, false, 0, 0),
    ('dataPerm.dtype.enabled', '往来单位数据权限', 'true', 'security', 'data_perm', 'data_perm', NULL,
     'boolean', true, '往来单位数据权限开关（ql361 实测默认：开）',
     '启用后，可对操作员设置往来单位数据权限；设置后，操作员在单据中只能选择所授权往来单位，报表只能查询所授权单位的业务数据', NULL, 30, true, false, 0, 0),
    ('dataPerm.dealerType.enabled', '客户级别权限', 'true', 'security', 'data_perm', 'data_perm', NULL,
     'boolean', true, '客户级别权限开关（ql361 实测默认：开）',
     '启用后，可设置操作员对客户级别的选择及查看权限', NULL, 40, true, false, 0, 0),
    ('dataPerm.stype.enabled', '仓库数据权限', 'true', 'security', 'data_perm', 'data_perm', NULL,
     'boolean', true, '仓库数据权限开关（ql361 实测默认：开）',
     '启用后，可对操作员设置仓库数据权限；设置后，操作员在单据中只能选择所授权仓库信息，报表只能查询所授权仓库的业务数据', NULL, 50, true, false, 0, 0),
    ('dataPerm.stype.stockUnaffected', '库存状况不受仓库数据权限控制', 'false', 'security', 'data_perm', 'data_perm', 'dataPerm.stype.enabled',
     'boolean', true, '仓库数据权限·库存状况是否豁免权限（ql361 实测默认：未勾选）',
     '启用后，操作员在查库存报表中，仓库数据权限无效，可查看全部仓库的库存数据；单据中可查看全部仓库的可用库存', NULL, 51, true, false, 0, 0),
    ('dataPerm.stype.billUnaffected', '单据可用库存分布不受数据权限控制', 'false', 'security', 'data_perm', 'data_perm', 'dataPerm.stype.enabled',
     'boolean', true, '仓库数据权限·单据可用库存分布是否豁免权限（ql361 实测默认：未勾选）',
     '启用后，操作员在单据中查看可用库存，仓库数据权限无效，可查看全部仓库的可用库存数据', NULL, 52, true, false, 0, 0),
    ('dataPerm.stype.viewPerm', '仓库查看数据权限', 'false', 'security', 'data_perm', 'data_perm', 'dataPerm.stype.enabled',
     'boolean', true, '仓库数据权限·收发货页面仓库查看权限（ql361 实测默认：未勾选）',
     ' 启用该设置，可设置操作员在收发货页面仓库的查看权限', NULL, 53, true, false, 0, 0),
    ('dataPerm.mtype.enabled', '调拨数据权限', 'true', 'security', 'data_perm', 'data_perm', NULL,
     'boolean', true, '调拨数据权限开关（ql361 实测默认：开）',
     '启用后，可对操作员设置发生调拨业务的仓库数据权限，调拨申请单的调出仓库或调拨单的调入仓库，只能选择所授权的仓库', NULL, 60, true, false, 0, 0),
    ('dataPerm.atype.enabled', '现金银行数据权限', 'true', 'security', 'data_perm', 'data_perm', NULL,
     'boolean', true, '现金银行数据权限开关（ql361 实测默认：开）',
     '启用后，可对操作员设置现金银行账户权限；设置后，操作员在单据中只能选择所授权账户信息，报表只能查询所授权账户的业务数据', NULL, 70, true, false, 0, 0)
ON CONFLICT (param_key) DO NOTHING;

-- ── 7. 其他（other）───────────────────────────────────────────────────────
-- 结构：分享设置 / 锁屏设置 / 溯源系统对接设置 / 启用缓存机制 / 其他设置（显示控制=按钮）。
-- 存量 8 行（system.*）的 sort_order 是 1..8，会排在这些新项之前。
INSERT INTO sys_config
    (param_key, param_name, param_value, config_type, config_group, nav_group, parent_key,
     value_type, builtin, remark, help_text, tip_text, sort_order, enabled, locked, tenant_id, deleted)
VALUES
    ('other.share', '分享设置', 'false', 'business', 'other', 'other', NULL,
     'group', true, '分享设置小节（ql361 面板正文的小节标题；气泡正文取自「单据分享接收者」帮助图标）',
     '系统支持将销售订单，销售出库单，采购订单，采购入库单、客户对账单/供应商对账单，通过小程序分享给客户/供应商查看，并可填写反馈信息。', NULL, 100, true, false, 0, 0),
    ('other.share.verifyIdentity', '查看分享信息需验证身份', 'true', 'business', 'other', 'other', 'other.share',
     'boolean', true, '分享设置·查看分享信息是否需验证身份（ql361 实测：勾选 —— 该标签是「其他」标签页里唯一的勾选框）',
     '查看分享信息需要验证身份：启用后，查看分享消息需要对客户/供应商默认联系人手机号进行验证码验证，以防信息泄露。', NULL, 101, true, false, 0, 0),
    ('other.share.expireDays', '分享单据有效期：', '1', 'business', 'other', 'other', 'other.share',
     'enum', true, '分享设置·分享单据有效期（ql361 实测当前值：1天）',
     '单据分享后，在设置的有效期内允许查看，超过有效期后就无法正常查看，以便保护信息安全', NULL, 102, true, false, 0, 0),
    ('other.lockScreen', '锁屏设置', 'false', 'business', 'other', 'other', NULL,
     'group', true, '锁屏设置小节（ql361 面板正文的小节标题；该小节只采到一个下拉，勾选状态未取到）', NULL, NULL, 200, true, false, 0, 0),
    ('other.lockScreen.timeout', '启用屏幕锁定：', '5', 'business', 'other', 'other', 'other.lockScreen',
     'enum', true, '锁屏设置·自动锁屏时长（ql361 实测当前值：5分钟）',
     '启用后，在设置的时间内未进行操作，系统自动锁定屏幕，需重新录入密码才可继续使用。', NULL, 201, true, false, 0, 0),
    ('other.trace', '溯源系统对接设置', 'false', 'integration', 'other', 'other', NULL,
     'group', true, '溯源系统对接设置小节（ql361 面板正文的小节标题）', NULL,
     '对接系统后，需要在客户资料里录入营业执照等必备信息，系统才能自动推送客户及单据到对方系统', 300, true, false, 0, 0),
    ('other.trace.vendor', '选择系统：', 'none', 'integration', 'other', 'other', 'other.trace',
     'enum', true, '溯源系统对接·目标系统（ql361 实测当前值：无）', NULL, NULL, 301, true, false, 0, 0),
    ('other.yuanqi.supplier', 'OMS采购订单默认供应商：', '', 'integration', 'other', 'other', NULL,
     'string', true, '元气经销商通用配置·OMS采购订单默认供应商。⚠️ ql361 是「选择器(ItemField)」（往来单位选择器），本系统无对应控件 → 以文本行落库，当前值为空',
     NULL, NULL, 400, true, false, 0, 0),
    ('other.yuanqi.deliveryFlow', '元气巡店推送过来的销售订单，需要走配送流程;', 'false', 'integration', 'other', 'other', NULL,
     'boolean', true, '元气经销商通用配置·推送订单是否走配送流程（ql361 实测默认：未勾选）。⚠️ 气泡正文未取到', NULL, NULL, 410, true, false, 0, 0),
    ('other.yuanqi.batchAttr', '元气商品导入来肯时，开启商品的【保质期/批次】属性', 'false', 'integration', 'other', 'other', NULL,
     'boolean', true, '元气经销商通用配置·商品导入时是否开启保质期/批次属性（ql361 实测默认：未勾选）。⚠️ 气泡正文未取到', NULL, NULL, 420, true, false, 0, 0),
    ('other.cache.enabled', '启用缓存机制', 'false', 'business', 'other', 'other', NULL,
     'boolean', true, '启用缓存机制开关（ql361 实测默认：关）',
     '启用后可设置对应数据是否走缓存机制，走缓存机制可提升加载速度', NULL, 500, true, false, 0, 0),
    ('other.cache.subject', '会计科目', 'false', 'business', 'other', 'other', 'other.cache.enabled',
     'boolean', true, '启用缓存机制·会计科目走缓存（ql361 实测默认：未勾选）', NULL, NULL, 501, true, false, 0, 0),
    ('other.cache.dept', '部门', 'false', 'business', 'other', 'other', 'other.cache.enabled',
     'boolean', true, '启用缓存机制·部门走缓存（ql361 实测默认：未勾选）', NULL, NULL, 502, true, false, 0, 0),
    ('other.cache.operator', '经手人', 'false', 'business', 'other', 'other', 'other.cache.enabled',
     'boolean', true, '启用缓存机制·经手人走缓存（ql361 实测默认：未勾选）', NULL, NULL, 503, true, false, 0, 0),
    ('other.cache.partner', '往来单位', 'false', 'business', 'other', 'other', 'other.cache.enabled',
     'boolean', true, '启用缓存机制·往来单位走缓存（ql361 实测默认：未勾选）', NULL, NULL, 504, true, false, 0, 0),
    ('other.cache.product', '商品', 'false', 'business', 'other', 'other', 'other.cache.enabled',
     'boolean', true, '启用缓存机制·商品走缓存（ql361 实测默认：未勾选）', NULL, NULL, 505, true, false, 0, 0)
ON CONFLICT (param_key) DO NOTHING;

-- ── 8. 消息提醒（notify）──────────────────────────────────────────────────
-- ql361 该标签是**三层结构**：通知事件（10 个，取自 `通用形态.列表项[]` 的逐字文本）
--   → 通知对象（通知客户 / 通知经手人 / 通知操作员 / 通知审核人 / 通知制单人）
--   → 通知方式（短信通知 / 系统通知，两个可同时勾选）。
-- 当前值的归属方法（如实登记，可复核）：
--   · `通用形态.controls` 是一份按 DOM 顺序的扁平清单。按列表项「每个通知对象的通知方式
--     个数」求和，全标签需要 33 个勾选框；dump 里恰有 33 个勾选框（idx1 无标签 + idx2..41
--     中的 32 个），文本输入 9 个（1 个「提前 N 天」+ 8 个通知对象收件人）——与行结构一一对应，
--     故按 idx 递增对齐落值。
--   · 不补的两处：① idx1 那个**无标签勾选框**（未勾选）归属不明 → 不落库；
--     ② 货位预警补货通知的「系统通知」勾选框在 dump 里缺失 → 该行值按未勾选存放并标注。
--   · 各行的**收件人文本**（杨生淮/高晓丽 等）是 ql361 的演示用户 → 不落库。
-- 说明：三层结构在页面上靠 parent_key 的递归挂载渲染（见 index.vue / ParamNodeRow.vue）。
INSERT INTO sys_config
    (param_key, param_name, param_value, config_type, config_group, nav_group, parent_key,
     value_type, builtin, remark, help_text, tip_text, sort_order, enabled, locked, tenant_id, deleted)
VALUES
    -- ① 销售订单未发货通知（含「提前 N 天」）
    ('notify.orderNotShipped', '销售订单未发货通知', 'false', 'notification', 'notify', 'notify', NULL,
     'group', true, '消息提醒·通知事件分组（ql361「内部通知」下的第 1 个事件）', NULL, NULL, 100, true, false, 0, 0),
    ('notify.orderNotShipped.advanceDays', '提前预计发货日期', '0', 'notification', 'notify', 'notify', 'notify.orderNotShipped',
     'number', true, '销售订单未发货通知·提前提醒天数（ql361 实测值：0；该行原文为「提前预计发货日期 N 天进行提醒」）',
     '配置在预计发货日期前提前通知的天数，请录入整数天数，负数代表超期提醒的天数', NULL, 101, true, false, 0, 0),
    ('notify.orderNotShipped.handler', '通知经手人', 'false', 'notification', 'notify', 'notify', 'notify.orderNotShipped',
     'group', true, '销售订单未发货通知·通知对象：经手人', NULL, NULL, 102, true, false, 0, 0),
    ('notify.orderNotShipped.handler.sms', '短信通知', 'true', 'notification', 'notify', 'notify', 'notify.orderNotShipped.handler',
     'boolean', true, '销售订单未发货通知·通知经手人·短信通知（ql361 实测：勾选）', NULL, NULL, 103, true, false, 0, 0),
    ('notify.orderNotShipped.handler.sys', '系统通知', 'false', 'notification', 'notify', 'notify', 'notify.orderNotShipped.handler',
     'boolean', true, '销售订单未发货通知·通知经手人·系统通知（ql361 实测：未勾选）', NULL, NULL, 104, true, false, 0, 0),
    ('notify.orderNotShipped.operator', '通知操作员', 'false', 'notification', 'notify', 'notify', 'notify.orderNotShipped',
     'group', true, '销售订单未发货通知·通知对象：操作员', NULL, NULL, 105, true, false, 0, 0),
    ('notify.orderNotShipped.operator.sms', '短信通知', 'true', 'notification', 'notify', 'notify', 'notify.orderNotShipped.operator',
     'boolean', true, '销售订单未发货通知·通知操作员·短信通知（ql361 实测：勾选）', NULL, NULL, 106, true, false, 0, 0),
    ('notify.orderNotShipped.operator.sys', '系统通知', 'false', 'notification', 'notify', 'notify', 'notify.orderNotShipped.operator',
     'boolean', true, '销售订单未发货通知·通知操作员·系统通知（ql361 实测：未勾选）', NULL, NULL, 107, true, false, 0, 0),

    -- ② 销售订单通知
    ('notify.orderCreated', '销售订单通知', 'false', 'notification', 'notify', 'notify', NULL,
     'group', true, '消息提醒·通知事件分组', NULL, NULL, 200, true, false, 0, 0),
    ('notify.orderCreated.customer', '通知客户', 'false', 'notification', 'notify', 'notify', 'notify.orderCreated',
     'group', true, '销售订单通知·通知对象：客户（ql361 该对象只有「短信通知」一种方式）', NULL, NULL, 201, true, false, 0, 0),
    ('notify.orderCreated.customer.sms', '短信通知', 'false', 'notification', 'notify', 'notify', 'notify.orderCreated.customer',
     'boolean', true, '销售订单通知·通知客户·短信通知（ql361 实测：未勾选）', NULL, NULL, 202, true, false, 0, 0),
    ('notify.orderCreated.handler', '通知经手人', 'false', 'notification', 'notify', 'notify', 'notify.orderCreated',
     'group', true, '销售订单通知·通知对象：经手人', NULL, NULL, 203, true, false, 0, 0),
    ('notify.orderCreated.handler.sms', '短信通知', 'true', 'notification', 'notify', 'notify', 'notify.orderCreated.handler',
     'boolean', true, '销售订单通知·通知经手人·短信通知（ql361 实测：勾选）', NULL, NULL, 204, true, false, 0, 0),
    ('notify.orderCreated.handler.sys', '系统通知', 'false', 'notification', 'notify', 'notify', 'notify.orderCreated.handler',
     'boolean', true, '销售订单通知·通知经手人·系统通知（ql361 实测：未勾选）', NULL, NULL, 205, true, false, 0, 0),
    ('notify.orderCreated.operator', '通知操作员', 'false', 'notification', 'notify', 'notify', 'notify.orderCreated',
     'group', true, '销售订单通知·通知对象：操作员', NULL, NULL, 206, true, false, 0, 0),
    ('notify.orderCreated.operator.sms', '短信通知', 'true', 'notification', 'notify', 'notify', 'notify.orderCreated.operator',
     'boolean', true, '销售订单通知·通知操作员·短信通知（ql361 实测：勾选）', NULL, NULL, 207, true, false, 0, 0),
    ('notify.orderCreated.operator.sys', '系统通知', 'false', 'notification', 'notify', 'notify', 'notify.orderCreated.operator',
     'boolean', true, '销售订单通知·通知操作员·系统通知（ql361 实测：未勾选）', NULL, NULL, 208, true, false, 0, 0),

    -- ③ 销售退货申请通知
    ('notify.returnApply', '销售退货申请通知', 'false', 'notification', 'notify', 'notify', NULL,
     'group', true, '消息提醒·通知事件分组', NULL, NULL, 300, true, false, 0, 0),
    ('notify.returnApply.customer', '通知客户', 'false', 'notification', 'notify', 'notify', 'notify.returnApply',
     'group', true, '销售退货申请通知·通知对象：客户', NULL, NULL, 301, true, false, 0, 0),
    ('notify.returnApply.customer.sms', '短信通知', 'false', 'notification', 'notify', 'notify', 'notify.returnApply.customer',
     'boolean', true, '销售退货申请通知·通知客户·短信通知（ql361 实测：未勾选）', NULL, NULL, 302, true, false, 0, 0),
    ('notify.returnApply.handler', '通知经手人', 'false', 'notification', 'notify', 'notify', 'notify.returnApply',
     'group', true, '销售退货申请通知·通知对象：经手人', NULL, NULL, 303, true, false, 0, 0),
    ('notify.returnApply.handler.sms', '短信通知', 'true', 'notification', 'notify', 'notify', 'notify.returnApply.handler',
     'boolean', true, '销售退货申请通知·通知经手人·短信通知（ql361 实测：勾选）', NULL, NULL, 304, true, false, 0, 0),
    ('notify.returnApply.handler.sys', '系统通知', 'false', 'notification', 'notify', 'notify', 'notify.returnApply.handler',
     'boolean', true, '销售退货申请通知·通知经手人·系统通知（ql361 实测：未勾选）', NULL, NULL, 305, true, false, 0, 0),
    ('notify.returnApply.operator', '通知操作员', 'false', 'notification', 'notify', 'notify', 'notify.returnApply',
     'group', true, '销售退货申请通知·通知对象：操作员', NULL, NULL, 306, true, false, 0, 0),
    ('notify.returnApply.operator.sms', '短信通知', 'true', 'notification', 'notify', 'notify', 'notify.returnApply.operator',
     'boolean', true, '销售退货申请通知·通知操作员·短信通知（ql361 实测：勾选）', NULL, NULL, 307, true, false, 0, 0),
    ('notify.returnApply.operator.sys', '系统通知', 'false', 'notification', 'notify', 'notify', 'notify.returnApply.operator',
     'boolean', true, '销售退货申请通知·通知操作员·系统通知（ql361 实测：未勾选）', NULL, NULL, 308, true, false, 0, 0),

    -- ④ 审核通知（审核人 / 经手人 / 制单人）
    ('notify.audit', '审核通知', 'false', 'notification', 'notify', 'notify', NULL,
     'group', true, '消息提醒·通知事件分组', NULL, NULL, 400, true, false, 0, 0),
    ('notify.audit.auditor', '通知审核人', 'false', 'notification', 'notify', 'notify', 'notify.audit',
     'group', true, '审核通知·通知对象：审核人', NULL, NULL, 401, true, false, 0, 0),
    ('notify.audit.auditor.sms', '短信通知', 'true', 'notification', 'notify', 'notify', 'notify.audit.auditor',
     'boolean', true, '审核通知·通知审核人·短信通知（ql361 实测：勾选）', NULL, NULL, 402, true, false, 0, 0),
    ('notify.audit.auditor.sys', '系统通知', 'false', 'notification', 'notify', 'notify', 'notify.audit.auditor',
     'boolean', true, '审核通知·通知审核人·系统通知（ql361 实测：未勾选）', NULL, NULL, 403, true, false, 0, 0),
    ('notify.audit.handler', '通知经手人', 'false', 'notification', 'notify', 'notify', 'notify.audit',
     'group', true, '审核通知·通知对象：经手人', NULL, NULL, 404, true, false, 0, 0),
    ('notify.audit.handler.sms', '短信通知', 'true', 'notification', 'notify', 'notify', 'notify.audit.handler',
     'boolean', true, '审核通知·通知经手人·短信通知（ql361 实测：勾选）', NULL, NULL, 405, true, false, 0, 0),
    ('notify.audit.handler.sys', '系统通知', 'false', 'notification', 'notify', 'notify', 'notify.audit.handler',
     'boolean', true, '审核通知·通知经手人·系统通知（ql361 实测：未勾选）', NULL, NULL, 406, true, false, 0, 0),
    ('notify.audit.creator', '通知制单人', 'false', 'notification', 'notify', 'notify', 'notify.audit',
     'group', true, '审核通知·通知对象：制单人', NULL, NULL, 407, true, false, 0, 0),
    ('notify.audit.creator.sms', '短信通知', 'true', 'notification', 'notify', 'notify', 'notify.audit.creator',
     'boolean', true, '审核通知·通知制单人·短信通知（ql361 实测：勾选）', NULL, NULL, 408, true, false, 0, 0),
    ('notify.audit.creator.sys', '系统通知', 'false', 'notification', 'notify', 'notify', 'notify.audit.creator',
     'boolean', true, '审核通知·通知制单人·系统通知（ql361 实测：未勾选）', NULL, NULL, 409, true, false, 0, 0),

    -- ⑤ 消费通知（只有「通知客户·短信通知」）
    ('notify.consume', '消费通知', 'false', 'notification', 'notify', 'notify', NULL,
     'group', true, '消息提醒·通知事件分组', NULL, NULL, 500, true, false, 0, 0),
    ('notify.consume.customer', '通知客户', 'false', 'notification', 'notify', 'notify', 'notify.consume',
     'group', true, '消费通知·通知对象：客户', NULL, NULL, 501, true, false, 0, 0),
    ('notify.consume.customer.sms', '短信通知', 'false', 'notification', 'notify', 'notify', 'notify.consume.customer',
     'boolean', true, '消费通知·通知客户·短信通知（ql361 实测：未勾选）', NULL, NULL, 502, true, false, 0, 0),

    -- ⑥ 收款待确认通知
    ('notify.receiptPending', '收款待确认通知', 'false', 'notification', 'notify', 'notify', NULL,
     'group', true, '消息提醒·通知事件分组', NULL, NULL, 600, true, false, 0, 0),
    ('notify.receiptPending.handler', '通知经手人', 'false', 'notification', 'notify', 'notify', 'notify.receiptPending',
     'group', true, '收款待确认通知·通知对象：经手人', NULL, NULL, 601, true, false, 0, 0),
    ('notify.receiptPending.handler.sms', '短信通知', 'true', 'notification', 'notify', 'notify', 'notify.receiptPending.handler',
     'boolean', true, '收款待确认通知·通知经手人·短信通知（ql361 实测：勾选）', NULL, NULL, 602, true, false, 0, 0),
    ('notify.receiptPending.handler.sys', '系统通知', 'false', 'notification', 'notify', 'notify', 'notify.receiptPending.handler',
     'boolean', true, '收款待确认通知·通知经手人·系统通知（ql361 实测：未勾选）', NULL, NULL, 603, true, false, 0, 0),
    ('notify.receiptPending.operator', '通知操作员', 'false', 'notification', 'notify', 'notify', 'notify.receiptPending',
     'group', true, '收款待确认通知·通知对象：操作员', NULL, NULL, 604, true, false, 0, 0),
    ('notify.receiptPending.operator.sms', '短信通知', 'true', 'notification', 'notify', 'notify', 'notify.receiptPending.operator',
     'boolean', true, '收款待确认通知·通知操作员·短信通知（ql361 实测：勾选）', NULL, NULL, 605, true, false, 0, 0),
    ('notify.receiptPending.operator.sys', '系统通知', 'false', 'notification', 'notify', 'notify', 'notify.receiptPending.operator',
     'boolean', true, '收款待确认通知·通知操作员·系统通知（ql361 实测：未勾选）', NULL, NULL, 606, true, false, 0, 0),

    -- ⑦ 经营概况通知
    ('notify.businessOverview', '经营概况通知', 'false', 'notification', 'notify', 'notify', NULL,
     'group', true, '消息提醒·通知事件分组', NULL, NULL, 700, true, false, 0, 0),
    ('notify.businessOverview.operator', '通知操作员', 'false', 'notification', 'notify', 'notify', 'notify.businessOverview',
     'group', true, '经营概况通知·通知对象：操作员', NULL, NULL, 701, true, false, 0, 0),
    ('notify.businessOverview.operator.sms', '短信通知', 'false', 'notification', 'notify', 'notify', 'notify.businessOverview.operator',
     'boolean', true, '经营概况通知·通知操作员·短信通知（ql361 实测：未勾选）', NULL, NULL, 702, true, false, 0, 0),
    ('notify.businessOverview.operator.sys', '系统通知', 'false', 'notification', 'notify', 'notify', 'notify.businessOverview.operator',
     'boolean', true, '经营概况通知·通知操作员·系统通知（ql361 实测：未勾选）', NULL, NULL, 703, true, false, 0, 0),

    -- ⑧ 流失客户通知
    ('notify.churn', '流失客户通知', 'false', 'notification', 'notify', 'notify', NULL,
     'group', true, '消息提醒·通知事件分组', NULL, NULL, 800, true, false, 0, 0),
    ('notify.churn.operator', '通知操作员', 'false', 'notification', 'notify', 'notify', 'notify.churn',
     'group', true, '流失客户通知·通知对象：操作员', NULL, NULL, 801, true, false, 0, 0),
    ('notify.churn.operator.sms', '短信通知', 'true', 'notification', 'notify', 'notify', 'notify.churn.operator',
     'boolean', true, '流失客户通知·通知操作员·短信通知（ql361 实测：勾选）', NULL, NULL, 802, true, false, 0, 0),
    ('notify.churn.operator.sys', '系统通知', 'false', 'notification', 'notify', 'notify', 'notify.churn.operator',
     'boolean', true, '流失客户通知·通知操作员·系统通知（ql361 实测：未勾选）', NULL, NULL, 803, true, false, 0, 0),

    -- ⑨ 商城买家账号审核通知（「系统通知」的气泡正文归属在此行 —— 文案与事件名逐字对应）
    ('notify.mallBuyerAudit', '商城买家账号审核通知', 'false', 'notification', 'notify', 'notify', NULL,
     'group', true, '消息提醒·通知事件分组', NULL, NULL, 900, true, false, 0, 0),
    ('notify.mallBuyerAudit.operator', '通知操作员', 'false', 'notification', 'notify', 'notify', 'notify.mallBuyerAudit',
     'group', true, '商城买家账号审核通知·通知对象：操作员', NULL, NULL, 901, true, false, 0, 0),
    ('notify.mallBuyerAudit.operator.sms', '短信通知', 'true', 'notification', 'notify', 'notify', 'notify.mallBuyerAudit.operator',
     'boolean', true, '商城买家账号审核通知·通知操作员·短信通知（ql361 实测：勾选）', NULL, NULL, 902, true, false, 0, 0),
    ('notify.mallBuyerAudit.operator.sys', '系统通知', 'false', 'notification', 'notify', 'notify', 'notify.mallBuyerAudit.operator',
     'boolean', true, '商城买家账号审核通知·通知操作员·系统通知（ql361 实测：未勾选）',
     '商城开启买家账号申请审核后，可设置有买家注册需申请时的消息通知', NULL, 903, true, false, 0, 0),

    -- ⑩ 货位预警补货通知
    ('notify.locationReplenish', '货位预警补货通知', 'false', 'notification', 'notify', 'notify', NULL,
     'group', true, '消息提醒·通知事件分组', NULL, NULL, 1000, true, false, 0, 0),
    ('notify.locationReplenish.operator', '通知操作员', 'false', 'notification', 'notify', 'notify', 'notify.locationReplenish',
     'group', true, '货位预警补货通知·通知对象：操作员', NULL, NULL, 1001, true, false, 0, 0),
    ('notify.locationReplenish.operator.sms', '短信通知', 'false', 'notification', 'notify', 'notify', 'notify.locationReplenish.operator',
     'boolean', true, '货位预警补货通知·通知操作员·短信通知（ql361 实测：未勾选）', NULL, NULL, 1002, true, false, 0, 0),
    ('notify.locationReplenish.operator.sys', '系统通知', 'false', 'notification', 'notify', 'notify', 'notify.locationReplenish.operator',
     'boolean', true, '货位预警补货通知·通知操作员·系统通知。⚠️ 勾选状态未取到（该组是控件清单末尾的最后一组，dump 在此截断，系统通知勾选框缺失）→ 按未勾选存放', NULL, NULL, 1003, true, false, 0, 0)
ON CONFLICT (param_key) DO NOTHING;

-- ── 9. 收尾：本页 8 个视图的配置项都存在之后的「视图 -> 行数」核对（只读，便于人工复核）
--   SELECT nav_group, count(*) FROM sys_config WHERE deleted = 0 GROUP BY nav_group ORDER BY 1;
