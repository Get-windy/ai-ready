<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <template #header>
        <div class="page-header">
          <div class="page-header__left">
            <a-breadcrumb>
              <a-breadcrumb-item>
                <router-link to="/">
                  首页
                </router-link>
              </a-breadcrumb-item>
              <a-breadcrumb-item>DMS / 配送配置</a-breadcrumb-item>
            </a-breadcrumb>
            <h2>配送配置（按业务域总览）</h2>
          </div>
          <div class="page-header__right">
            <span
              v-if="lastUpdateTime"
              class="update-time"
            >更新于 {{ lastUpdateTime }}</span>
            <a-button
              size="small"
              :loading="loading"
              @click="fetchAll"
            >
              <ReloadOutlined /> 刷新
            </a-button>
            <a-button
              type="primary"
              size="small"
              @click="goParams"
            >
              <EditOutlined /> 去「配送参数」编辑
            </a-button>
          </div>
        </div>
      </template>

      <template #default>
        <div class="page-body">
          <!-- 分工说明：本页只读总览，编辑与变更历史在《配送参数》，避免同一张表两套可写入口 -->
          <a-alert
            type="info"
            show-icon
            banner
            class="scope-tip"
            message="本页是配置的「业务域总览」（只读）"
            description="配置的新增 / 修改 / 变更历史请到「配送 → 配送配置 → 配送参数」。两页共用 dms_config，本页不重复提供编辑能力。"
          />
          <a-spin :spinning="loading">
            <a-collapse
              v-model:activeKey="activeKeys"
              class="config-collapse"
            >
              <a-collapse-panel
                v-for="g in groups"
                :key="g.key"
                :header="`${g.label}（${g.items.length} 项）`"
              >
                <a-table
                  :columns="itemColumns"
                  :data-source="g.items"
                  :pagination="false"
                  size="small"
                  row-key="configKey"
                  bordered
                >
                  <template #bodyCell="{ column, record }">
                    <template v-if="column.key === 'configValue'">
                      <span class="cfg-value">{{ record.configValue || '（空）' }}</span>
                    </template>
                  </template>
                </a-table>
              </a-collapse-panel>
              <a-empty
                v-if="!loading && groups.length === 0"
                description="暂无配置"
              />
            </a-collapse>
          </a-spin>
        </div>
      </template>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
/**
 * 配送配置（配送 → 配送配置 → 配送配置，菜单 80890）
 *
 * 定位：**业务域总览（只读）** —— 把 `dms_config` 的配置按业务域分组集中呈现，
 * 便于排查"某项配置当前是什么值"。
 *
 * 分工（消除与《配送参数》80750 的重复）：
 *   · 本页 = 总览视图（按业务域分组、只读、不提供编辑）；
 *   · 《配送参数》= 唯一**参数维护入口**（列表 + 编辑 + 变更历史）。
 * 两页共用同一张 `dms_config`，但**只有一处可写**，避免两套入口维护同一张表。
 */
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import { configApi } from '@/api/dms/config'
import { ReloadOutlined, EditOutlined } from '@ant-design/icons-vue'

const router = useRouter()
const loading = ref(false)
const items = ref<any[]>([])
const lastUpdateTime = ref('')
const activeKeys = ref<string[]>(['settlement', 'payment'])

function handleError(err: any) { console.warn('[配送配置]', err) }

/** 业务域分组：按配置键前缀归类（新增业务域时在此登记即可） */
const GROUP_RULES: Array<{ key: string; label: string; prefixes: string[] }> = [
  { key: 'settlement', label: '配送结算计费', prefixes: ['dms.settlement.'] },
  { key: 'payment', label: '末端收款', prefixes: ['dms.payment.'] },
  { key: 'sign', label: '签收与位置校验', prefixes: ['dms.sign.'] },
  { key: 'dispatch', label: '调度与派单', prefixes: ['dms.dispatch.'] },
  { key: 'route', label: '路线与地理能力', prefixes: ['dms.route.', 'map.'] },
  { key: 'channel', label: '运力渠道', prefixes: ['dms.channel.'] },
  { key: 'vehicle', label: '车辆与能源', prefixes: ['dms.vehicle.', 'energy.'] },
  { key: 'other', label: '其它配送配置', prefixes: ['dms.'] }
]

const groups = computed(() => {
  const buckets = new Map<string, any[]>()
  for (const it of items.value) {
    const key = String(it.configKey || '')
    const rule = GROUP_RULES.find(r => r.prefixes.some(p => key.startsWith(p)))
      || { key: 'misc', label: '系统其它配置', prefixes: [] } as any
    if (!buckets.has(rule.key)) buckets.set(rule.key, [])
    buckets.get(rule.key)!.push(it)
  }
  // 按 GROUP_RULES 顺序输出（未命中的「系统其它配置」置末）
  const ordered: Array<{ key: string; label: string; items: any[] }> = []
  for (const r of GROUP_RULES) {
    const list = buckets.get(r.key)
    if (list && list.length) ordered.push({ key: r.key, label: r.label, items: list })
  }
  const misc = buckets.get('misc')
  if (misc && misc.length) ordered.push({ key: 'misc', label: '系统其它配置', items: misc })
  return ordered
})

const itemColumns = [
  { title: '配置键', dataIndex: 'configKey', key: 'configKey', width: 280 },
  { title: '当前值', dataIndex: 'configValue', key: 'configValue', width: 280 },
  { title: '说明', dataIndex: 'configDesc', key: 'configDesc' },
  { title: '作用域', dataIndex: 'scope', key: 'scope', width: 110 }
]

async function fetchAll() {
  loading.value = true
  try {
    // configApi.list 返回数组（按 tenantId 查询；0 = 全局缺省配置）
    const res: any = await configApi.list(0)
    const payload = res?.data?.data ?? res?.data ?? res ?? []
    items.value = Array.isArray(payload) ? payload : (payload.records || payload.list || [])
    lastUpdateTime.value = new Date().toLocaleString('zh-CN')
  } catch (e) {
    items.value = []
    message.error('配置加载失败')
    console.warn('[配送配置] 加载失败', e)
  } finally {
    loading.value = false
  }
}

function goParams() {
  router.push('/dms/config-params')
}

onMounted(fetchAll)
</script>

<style scoped>
.page-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 10px 16px; background: #fff; border-bottom: 1px solid #f0f0f0;
}
.page-header__left { display: flex; align-items: center; gap: 12px; }
.page-header__left h2 { margin: 0; font-size: 16px; font-weight: 600; }
.page-header__right { display: flex; align-items: center; gap: 8px; }
.update-time { font-size: 12px; color: #8c8c8c; }
.page-body { padding: 12px 16px; overflow: auto; height: 100%; }
.scope-tip { margin-bottom: 12px; }
.config-collapse { background: #fff; }
.cfg-value { word-break: break-all; }
</style>
