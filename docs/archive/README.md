# 留档（archive）

这里放**故意不再被代码使用、但删之前必须先留一份**的东西。与 `docs/old/`（旧版本文档）不同：
本目录是**数据/事实的快照**，用于"列要删了但数据无处迁移"这类场景 ——
**不假装迁移，也不让它在删列时无声消失**。

## 已有内容

| 文件 | 是什么 | 为什么在这里 |
|---|---|---|
| `party-member-cards-20260927.csv` | `biz_party` 上**带 `member_card_no` 的 4 行**（全列快照，2026-09-27 导） | 阶段 3 批 3 的裁定是「会员 = 自然人属性 ⇒ 并入 `shop_user`」，但这 4 张卡**没有可承接的自然人**（`shop_user` 当时 0 行、也没有卡号/积分字段）⇒ 按纪律**导出留档 + 列弃用**，而不是给它们编一个自然人。（出处：`docs/PHASE-3-5-MIGRATION-PLAN-v1.md` §3.2b C 组处置） |

## ⚠️ 读这份留档时要知道的三件事

1. **这 4 行是 dev 库的模拟/E2E 造数**（`party_code` 形如 `VIPKH02298723` / `VIPE2EM127455` /
   `DBGC358224`），不是真实业务数据 —— 留档的意义是"删列前留下事实"，不是"这些卡要恢复"。
2. **导出的列包含 `points`**，而 `biz_party.points` 至今仍是**零售/商城的积分余额权威值**
   （`RetailOrderServiceImpl` 拿它做余额校验与扣减）⇒ 这 4 行的 `points` 只是**当时的快照**，
   不要拿它当积分台账。
3. **同表还有 12 个会员列，其中 8 列当前"停不了"**（`points`/`member_card_no`/`member_level`/
   `member_name`/`member_total_consume`/`member_valid_end`/`birthday`/`customer_one_pass`
   都有业务逻辑在读）。逐列的"能不能停写"判定与前置条件见
   `docs/PHASE-3-5-MIGRATION-PLAN-v1.md` §3.2f —— **先看那张表再动手**。
