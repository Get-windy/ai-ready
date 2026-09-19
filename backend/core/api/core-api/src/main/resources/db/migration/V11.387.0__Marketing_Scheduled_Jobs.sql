-- 营销模块：把三件「时间驱动」的事定时化（P0-P3 收尾闭环）
--
-- 背景：以下三件事此前**只能人工点按钮**，属"没闭环"——
--   ① 营销自动化的每日触达（页面「执行全部启用规则」）
--   ② 会员积分到期扣减（《会员设置》「立即执行过期」）
--   ③ 储值卡到期置为已过期（此前只在「交易时」按 expire_time 拦截，卡状态永不翻转）
-- 口径与既有定时作业一致（见 V11.362.0 / DmsDispatchEscalateJob）：
--   · job_key 白名单（JobHandler SPI 在 core-base，实现类在 erp-marketing）
--   · **默认 enabled = 0 / status = 'STOPPED'**：需人工在「系统管理 → 开发工具 → 定时任务」确认后启用
--   · 多实例互斥用 Redis 锁（实现类内）
--   · 自动化执行支持 execute_params {"dryRun": true} 演练（只统计候选，不真发券/短信/积分）
--   · 逐租户执行：定时线程无登录上下文，实现类内切临时租户上下文
--
-- Cron 选点（避开业务高峰与彼此重叠）：
--   积分过期 03:00 → 储值卡到期 03:20 → 自动化触达 09:00（短信发送时段 8:00–21:00 内）

-- ① 会员积分到期处理
INSERT INTO scheduled_task (task_name, task_desc, task_type, cron_expression, execute_params, job_key,
                            status, retry_count, retry_interval, timeout, enabled,
                            execute_count, success_count, fail_count, tenant_id, create_time, update_time, deleted)
SELECT '营销·积分到期处理',
       '每日把已到期积分批次的剩余清零并写 EXPIRE 流水（先到期的先扣，过期只吃批次剩余不吃账户总额）。'
       || '安全闸门：默认停用 + Redis 锁多实例互斥；本作业无演练模式（到期是确定性事实）',
       'CRON', '0 0 3 * * ?', NULL, 'mkt.member.pointsExpire',
       'STOPPED', 0, 60, 300, 0,
       0, 0, 0, 0, now(), now(), 0
WHERE NOT EXISTS (SELECT 1 FROM scheduled_task WHERE job_key = 'mkt.member.pointsExpire' AND deleted = 0);

-- ② 储值卡到期处理（只改状态、余额保留，退款按合规流程人工处理）
INSERT INTO scheduled_task (task_name, task_desc, task_type, cron_expression, execute_params, job_key,
                            status, retry_count, retry_interval, timeout, enabled,
                            execute_count, success_count, fail_count, tenant_id, create_time, update_time, deleted)
SELECT '营销·储值卡到期处理',
       '每日把已过有效期且仍为「正常」的储值卡置为「已过期」并写 ADJUST 流水留痕；'
       || '**只改状态、不动余额**——预付卡余额属消费者已付款项，退款/续期须人工按合规流程处理（法释〔2025〕4 号）。'
       || '安全闸门：默认停用 + Redis 锁多实例互斥',
       'CRON', '0 20 3 * * ?', NULL, 'mkt.storedCard.expire',
       'STOPPED', 0, 60, 300, 0,
       0, 0, 0, 0, now(), now(), 0
WHERE NOT EXISTS (SELECT 1 FROM scheduled_task WHERE job_key = 'mkt.storedCard.expire' AND deleted = 0);

-- ③ 营销自动化规则每日执行（默认演练，避免误发券/短信）
INSERT INTO scheduled_task (task_name, task_desc, task_type, cron_expression, execute_params, job_key,
                            status, retry_count, retry_interval, timeout, enabled,
                            execute_count, success_count, fail_count, tenant_id, create_time, update_time, deleted)
SELECT '营销·自动化规则每日执行',
       '每日执行全部「启用」状态的营销自动化规则（生日/沉睡/复购/积分到期/卡到期 → 发券·发短信·赠积分）。'
       || '安全闸门：默认停用 + Redis 锁多实例互斥 + execute_params 支持 {"dryRun": true} 只统计候选不触达；'
       || '短信动作复用《发短信》的合规四件套（时段 8:00–21:00 / 退订名单 / 频控 / 同意留痕）',
       'CRON', '0 0 9 * * ?', '{"dryRun": true}', 'mkt.autoCampaign.runAll',
       'STOPPED', 0, 120, 600, 0,
       0, 0, 0, 0, now(), now(), 0
WHERE NOT EXISTS (SELECT 1 FROM scheduled_task WHERE job_key = 'mkt.autoCampaign.runAll' AND deleted = 0);
