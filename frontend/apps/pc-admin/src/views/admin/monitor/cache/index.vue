<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <template #header>
        <div class="page-header">
          <div class="page-header-left">
            <a-breadcrumb>
              <a-breadcrumb-item>
                <router-link to="/">
                  首页
                </router-link>
              </a-breadcrumb-item>
              <a-breadcrumb-item>系统监控</a-breadcrumb-item>
              <a-breadcrumb-item>缓存管理</a-breadcrumb-item>
            </a-breadcrumb>
            <h2 class="page-header-title">
              缓存管理
            </h2>
          </div>
          <div class="page-header-right">
            <a-button
              size="small"
              :loading="loading"
              @click="fetchCacheStatus"
            >
              <template #icon>
                <ReloadOutlined />
              </template>
              刷新
            </a-button>
          </div>
        </div>
      </template>

      <div class="cache-page">
        <!-- ═══ 错误态：后端读取 Redis 失败（/cache/status 返回 code 500）时显式报错，不显示 0/不编数 ═══ -->
        <a-alert
          v-if="errorMsg"
          type="error"
          show-icon
          class="cache-alert"
          message="读取缓存状态失败"
          :description="errorMsg"
        >
          <template #action>
            <a-button
              size="small"
              @click="fetchCacheStatus"
            >
              重试
            </a-button>
          </template>
        </a-alert>

        <!-- ═══ 概览卡：全部取自 /api/cache/status 的真实值 ═══ -->
        <a-row
          :gutter="[12, 12]"
          class="overview-row"
        >
          <a-col
            :xs="12"
            :sm="8"
            :md="6"
          >
            <a-card :bordered="false">
              <!-- 注意：totalSize 是带单位的字符串（如 "807.77K"），必须走 :formatter 展示，
                   直接交给 :value 会被 a-statistic 解析成 NaN/0（字符串陷阱）。
                   ⚠️ ant-design-vue 的 formatter 入参是 `{ value }` 对象（非裸值），详见下方 statText 注释 -->
              <a-statistic
                title="缓存总大小"
                :value="status?.totalSize || ''"
                :formatter="statText"
              />
            </a-card>
          </a-col>
          <a-col
            :xs="12"
            :sm="8"
            :md="6"
          >
            <a-card :bordered="false">
              <a-statistic
                title="缓存键数量"
                :value="status?.totalKeys ?? 0"
                :formatter="statInt"
              />
            </a-card>
          </a-col>
          <a-col
            :xs="12"
            :sm="8"
            :md="6"
          >
            <a-card :bordered="false">
              <a-statistic
                title="命中率"
                :value="status?.hitRate ?? 0"
                :formatter="statOne"
                :suffix="status?.hitRate == null ? undefined : '%'"
                :value-style="{ color: (status?.hitRate ?? 0) > 80 ? '#52c41a' : '#faad14' }"
              />
            </a-card>
          </a-col>
          <a-col
            :xs="12"
            :sm="8"
            :md="6"
          >
            <a-card :bordered="false">
              <a-statistic
                title="过期键"
                :value="status?.expiredKeys ?? 0"
                :formatter="statInt"
                :value-style="{ color: '#faad14' }"
              />
            </a-card>
          </a-col>
          <a-col
            :xs="12"
            :sm="8"
            :md="6"
          >
            <a-card :bordered="false">
              <a-statistic
                title="连接客户端"
                :value="status?.connectedClients ?? 0"
                :formatter="statInt"
              />
            </a-card>
          </a-col>
          <a-col
            :xs="12"
            :sm="8"
            :md="6"
          >
            <a-card :bordered="false">
              <a-statistic
                title="累计命令数"
                :value="status?.totalCommands ?? 0"
                :formatter="statInt"
              />
            </a-card>
          </a-col>
          <a-col
            :xs="12"
            :sm="8"
            :md="6"
          >
            <a-card :bordered="false">
              <a-statistic
                title="淘汰键"
                :value="status?.evictedKeys ?? 0"
                :formatter="statInt"
                :value-style="{ color: (status?.evictedKeys ?? 0) > 0 ? '#ff4d4f' : undefined }"
              />
            </a-card>
          </a-col>
          <a-col
            :xs="12"
            :sm="8"
            :md="6"
          >
            <a-card :bordered="false">
              <a-statistic
                title="采样键数"
                :value="status?.sampledKeys ?? 0"
                :formatter="statInt"
              />
            </a-card>
          </a-col>
        </a-row>

        <!-- 口径说明：写清楚哪些指标 Redis 不提供、为什么显示「—」 -->
        <div class="overview-note">
          指标口径：总大小 / 命中率 / 过期键 / 连接客户端 / 累计命令数 / 淘汰键取自 Redis <code>INFO</code>（全局口径）；
          「采样键数」为区域聚合时的 SCAN 采样上限（2000 个键），与 <code>DBSIZE</code> 的键总数可能不一致。
          区域级的「内存占用」「命中率」Redis <b>不提供</b>（只有全局统计），故两列固定显示「—」，不做任何估算。
        </div>

        <!-- ═══ 缓存区域（按键前缀真实聚合） ═══ -->
        <div class="region-panel">
          <div class="region-toolbar">
            <span class="panel-title">缓存区域</span>
            <div class="region-toolbar-right">
              <a-input
                v-model:value="regionKeyword"
                size="small"
                placeholder="按区域名前缀过滤"
                allow-clear
                style="width:180px"
              />
              <a-button
                size="small"
                :loading="loading"
                @click="fetchCacheStatus"
              >
                <template #icon>
                  <ReloadOutlined />
                </template>
                刷新
              </a-button>
              <a-button
                danger
                size="small"
                @click="confirmClearAll"
              >
                <template #icon>
                  <DeleteOutlined />
                </template>
                清空全部缓存
              </a-button>
            </div>
          </div>

          <div class="table-area">
            <BillDetailTable
              v-model:data-source="regionRows"
              :columns="regionColumns"
              :loading="loading"
              :view-mode="true"
              row-key="name"
              storage-key="system-cache-region-table-columns"
              global-config-key="system-cache-region-table-columns"
            >
              <!-- 区域名称 -->
              <template #regionNameCell="{ record }">
                <span>{{ record.name }}</span>
              </template>

              <!-- 键数量 -->
              <template #regionKeyCountCell="{ record }">
                <span>{{ fmtInt(record.keyCount) }}</span>
              </template>

              <!-- 内存占用：Redis 无区域级内存统计 → 恒为 null → 显示「—」 -->
              <template #regionMemoryCell="{ record }">
                <span>{{ record.memory ?? '—' }}</span>
              </template>

              <!-- 命中率：Redis 只在全局统计 keyspace_hits/misses → 恒为 null → 显示「—」 -->
              <template #regionHitRateCell="{ record }">
                <span>{{ record.hitRate === null || record.hitRate === undefined ? '—' : record.hitRate + '%' }}</span>
              </template>

              <!-- 平均 TTL：区域内带过期时间键的平均剩余秒数；无带过期时间的键时为 null -->
              <template #regionAvgTtlCell="{ record }">
                <span>{{ record.avgTtl === null || record.avgTtl === undefined ? '—' : fmtInt(record.avgTtl) + ' 秒' }}</span>
              </template>

              <!-- 操作 -->
              <template #regionActionCell="{ record }">
                <a-space :size="0">
                  <a-button
                    type="link"
                    size="small"
                    @click="viewCacheKeys(record)"
                  >
                    查看键
                  </a-button>
                  <a-popconfirm
                    :title="`确定清空缓存区域「${record.name}」？将真实删除 Redis 中匹配 ${record.name}:* 的全部键，删除后不可恢复。`"
                    ok-text="确定清空"
                    cancel-text="取消"
                    @confirm="clearRegion(record.name)"
                  >
                    <a-button
                      type="link"
                      size="small"
                      danger
                    >
                      清空
                    </a-button>
                  </a-popconfirm>
                </a-space>
              </template>
            </BillDetailTable>
          </div>
        </div>
      </div>

      <!-- ═══ 缓存键列表弹窗（真实键：键名 / 类型 / TTL） ═══ -->
      <a-modal
        v-model:open="keysVisible"
        :title="`缓存键列表 - ${selectedRegion}`"
        width="900px"
        :footer="null"
      >
        <div class="key-toolbar">
          <a-input
            v-model:value="keyKeyword"
            size="small"
            placeholder="按键名前缀过滤（仅过滤已列举的键）"
            allow-clear
            style="width:280px"
          />
          <span class="key-count">共 {{ allKeys.length }} 个键（后端单次最多列举 200 个）</span>
          <a-button
            size="small"
            :loading="keysLoading"
            @click="loadKeys(selectedRegion)"
          >
            <template #icon>
              <ReloadOutlined />
            </template>
            刷新
          </a-button>
        </div>

        <a-alert
          v-if="keyErrorMsg"
          type="error"
          show-icon
          class="key-alert"
          message="读取键列表失败"
          :description="keyErrorMsg"
        />

        <!-- 弹窗内的表：min-rows=0 —— 空数据时直接显示「暂无数据」，不铺 20 行空行 -->
        <div class="key-table-area">
          <BillDetailTable
            v-model:data-source="keyRows"
            :columns="keyColumns"
            :loading="keysLoading"
            :view-mode="true"
            :min-rows="0"
            row-key="key"
            storage-key="system-cache-key-table-columns"
            global-config-key="system-cache-key-table-columns"
          >
            <!-- 键名 -->
            <template #keyNameCell="{ record }">
              <span class="key-name">{{ record.key }}</span>
            </template>

            <!-- 类型（Redis 类型码：string / hash / list / set / zset / stream ...） -->
            <template #keyTypeCell="{ record }">
              <span>{{ record.type || '—' }}</span>
            </template>

            <!-- TTL：-1 = 未设置过期时间（永不过期）；-2 = 键已不存在 -->
            <template #keyTtlCell="{ record }">
              <span>{{ fmtTtl(record.ttl) }}</span>
            </template>

            <!-- 操作：失败（success=false）时不改动本地列表 -->
            <template #keyActionCell="{ record }">
              <a-popconfirm
                :title="`确定删除缓存键「${record.key}」？删除后不可恢复。`"
                ok-text="确定删除"
                cancel-text="取消"
                @confirm="deleteKey(record)"
              >
                <a-button
                  type="link"
                  size="small"
                  danger
                >
                  删除
                </a-button>
              </a-popconfirm>
            </template>
          </BillDetailTable>
        </div>
      </a-modal>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, watch, onMounted, h } from 'vue'
import { ReloadOutlined, DeleteOutlined } from '@ant-design/icons-vue'
import { message, Modal } from 'ant-design-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import request from '@/utils/request'

/**
 * 缓存管理（系统 → 系统监控 → 缓存管理，菜单 62206）
 *
 * 数据来源：`/api/cache/*`（2026-09-18 后端重写为真实读取 Redis）。
 * 本页**不保留任何假数据兜底**：接口失败即显式报错，绝不显示 0 / 随机数。
 *
 * ⚠️ Redis 只提供全局统计，不提供「按键前缀」维度的内存与命中率：
 *    - regions[].memory   → 后端恒返回 null → 本页显示「—」
 *    - regions[].hitRate  → 后端恒返回 null → 本页显示「—」
 *    这两列保留是为了对齐 Redis 监控的通用列形态，并为将来后端支持留位；**不要编数字或做兜底计算**。
 *
 * 开发文档：docs/Yh-Spec/手动整理对标开发文档/系统模块/缓存管理开发文档.md
 */
defineOptions({ name: 'AdminCacheManage' })

/** 缓存区域（按键前缀聚合） */
interface CacheRegion {
  name: string
  keyCount: number
  /** Redis 无区域级内存统计 → 恒为 null */
  memory: string | null
  /** Redis 无区域级命中率统计 → 恒为 null */
  hitRate: number | null
  /** 区域内带过期时间键的平均剩余秒数；无则为 null */
  avgTtl: number | null
}

/** 缓存键（SCAN 真实列举，单次上限 200） */
interface CacheKeyItem {
  key: string
  /** Redis 类型码：string / hash / list / set / zset / stream ... */
  type: string
  /** 剩余秒数；-1 永不过期，-2 键已不存在 */
  ttl: number | null
}

interface CacheStatus {
  totalSize: string | null
  totalKeys: number | null
  hitRate: number | null
  expiredKeys: number | null
  connectedClients: number | null
  totalCommands: number | null
  evictedKeys: number | null
  sampledKeys: number | null
  regions: CacheRegion[]
}

// ═══ 状态 ═══
const loading = ref(false)
const status = ref<CacheStatus | null>(null)
/** 概览读取失败时的错误文案（Redis 不可用 / 后端异常） */
const errorMsg = ref('')

// ═══ 区域表 ═══
const regionRows = ref<CacheRegion[]>([])
/** 前端本地按「区域名前缀」过滤（后端未提供区域检索接口；区域本身即键前缀视图） */
const regionKeyword = ref('')

// ═══ 键列表弹窗 ═══
const keysVisible = ref(false)
const keysLoading = ref(false)
const selectedRegion = ref('')
const allKeys = ref<CacheKeyItem[]>([])
const keyRows = ref<CacheKeyItem[]>([])
/** 前端本地按键名前缀过滤（后端 /keys 只有上限 200 的列举，无 pattern 参数） */
const keyKeyword = ref('')
const keyErrorMsg = ref('')

// ═══ 表格列 ═══
// 序号列承载表头「列配置」齿轮；操作列固定左侧
const regionColumns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'regionActionCell', width: 130, fixed: 'left' },
  { key: 'name', title: '区域名称', type: 'slot', slotName: 'regionNameCell', width: 180 },
  { key: 'keyCount', title: '键数量', type: 'slot', slotName: 'regionKeyCountCell', width: 100, align: 'right' },
  {
    key: 'memory',
    title: '内存占用',
    type: 'slot',
    slotName: 'regionMemoryCell',
    width: 110,
    align: 'right',
    // 如实说明该列恒为「—」的原因，避免用户以为是加载失败
    headerTip: 'Redis 不提供按区域（键前缀）的内存统计，只有全局 used_memory，故此处恒为「—」，不做估算',
  },
  {
    key: 'hitRate',
    title: '命中率',
    type: 'slot',
    slotName: 'regionHitRateCell',
    width: 100,
    align: 'right',
    headerTip: 'Redis 只在全局统计 keyspace_hits / keyspace_misses，不提供区域级命中率，故此处恒为「—」',
  },
  {
    key: 'avgTtl',
    title: '平均 TTL',
    type: 'slot',
    slotName: 'regionAvgTtlCell',
    width: 110,
    align: 'right',
    headerTip: '该区域下所有「设置了过期时间」的键的平均剩余秒数；若区域内没有这类键则显示「—」',
  },
]

const keyColumns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'keyActionCell', width: 80, fixed: 'left' },
  { key: 'key', title: '键名', type: 'slot', slotName: 'keyNameCell' },
  { key: 'type', title: '类型', type: 'slot', slotName: 'keyTypeCell', width: 90 },
  {
    key: 'ttl',
    title: 'TTL(秒)',
    type: 'slot',
    slotName: 'keyTtlCell',
    width: 110,
    align: 'right',
    headerTip: '-1 表示该键未设置过期时间（永不过期）；-2 表示键已不存在',
  },
]

// ═══ 格式化 ═══
/**
 * 后端全局把 Long 序列化为字符串（防 JS 精度丢失），故所有数值一律经此归一。
 * 解析不出来时返回 null（而不是 0）—— 0 会被误读成「真的是 0」。
 */
function num(v: unknown): number | null {
  if (v === null || v === undefined || v === '') return null
  const n = Number(v)
  return Number.isFinite(n) ? n : null
}

/** 整数展示（千分位）；无值显示「—」 */
function fmtInt(v: unknown): string {
  const n = num(v)
  return n === null ? '—' : n.toLocaleString('zh-CN')
}

/** 一位小数展示；无值显示「—」 */
function fmtOne(v: unknown): string {
  const n = num(v)
  return n === null ? '—' : n.toFixed(1)
}

/**
 * a-statistic 的 :formatter 入参是 `{ value }` **对象**而不是裸值（ant-design-vue 4 的 Number.js 实现），
 * 直接写成 `(v) => v || '—'` 会渲染出 `[object Object]`，故这里统一包一层拆包。
 */
function statInt({ value }: any): string {
  return fmtInt(value)
}
function statOne({ value }: any): string {
  return fmtOne(value)
}
/** 直接展示字符串值（totalSize 形如 "807.77K"）；无值显示「—」 */
function statText({ value }: any): string {
  return value === null || value === undefined || value === '' ? '—' : String(value)
}

/** TTL 展示：-1 永不过期 / -2 已不存在 / null — */
function fmtTtl(v: unknown): string {
  const n = num(v)
  if (n === null) return '—'
  if (n === -1) return '永不过期'
  if (n === -2) return '已不存在'
  return String(n)
}

/** 接口返回 → 归一化（字符串数值转数；regions 缺失时为空数组，不编造） */
function normalizeStatus(raw: any): CacheStatus {
  const d = raw || {}
  const regions = Array.isArray(d.regions)
    ? d.regions.map((r: any) => ({
        name: String(r?.name ?? ''),
        keyCount: num(r?.keyCount) ?? 0,
        memory: r?.memory ?? null,
        hitRate: r?.hitRate ?? null,
        avgTtl: num(r?.avgTtl),
      }))
    : []
  return {
    totalSize: d.totalSize ?? null,
    totalKeys: num(d.totalKeys),
    hitRate: num(d.hitRate),
    expiredKeys: num(d.expiredKeys),
    connectedClients: num(d.connectedClients),
    totalCommands: num(d.totalCommands),
    evictedKeys: num(d.evictedKeys),
    sampledKeys: num(d.sampledKeys),
    regions,
  }
}

// ═══ 数据加载 ═══
async function fetchCacheStatus() {
  loading.value = true
  try {
    const res: any = await request.get('/cache/status')
    status.value = normalizeStatus(res)
    errorMsg.value = ''
    applyRegionFilter()
  } catch (e: any) {
    console.error('[缓存管理] 读取缓存状态失败', e)
    message.error(e?.message || '读取缓存状态失败')
    // 失败即清空并进入错误态：不保留旧值、不显示 0、不使用假数据
    status.value = null
    regionRows.value = []
    errorMsg.value = e?.message || '读取缓存状态失败（后端 /api/cache/status 未返回可用数据）'
  } finally {
    loading.value = false
  }
}

/** 区域本地前缀过滤（仅过滤当前已加载区域，不触发请求） */
function applyRegionFilter() {
  const list = status.value?.regions ?? []
  const kw = regionKeyword.value.trim().toLowerCase()
  regionRows.value = kw ? list.filter(r => r.name.toLowerCase().startsWith(kw)) : list
}

watch(regionKeyword, applyRegionFilter)

/** 键列表本地前缀过滤（仅过滤当前已列举的键，不触发请求） */
function applyKeyFilter() {
  const kw = keyKeyword.value.trim()
  keyRows.value = kw ? allKeys.value.filter(k => k.key.startsWith(kw)) : allKeys.value
}

watch(keyKeyword, applyKeyFilter)

function viewCacheKeys(record: CacheRegion) {
  selectedRegion.value = record.name
  keyKeyword.value = ''
  keyErrorMsg.value = ''
  allKeys.value = []
  keyRows.value = []
  keysVisible.value = true
  loadKeys(record.name)
}

async function loadKeys(region: string) {
  if (!region) return
  keysLoading.value = true
  try {
    const res: any = await request.get('/cache/region/' + encodeURIComponent(region) + '/keys')
    allKeys.value = Array.isArray(res)
      ? res.map((k: any) => ({ key: String(k?.key ?? ''), type: String(k?.type ?? ''), ttl: num(k?.ttl) }))
      : []
    keyErrorMsg.value = ''
    applyKeyFilter()
  } catch (e: any) {
    console.error('[缓存管理] 读取缓存键列表失败', e)
    message.error(e?.message || '读取缓存键列表失败')
    allKeys.value = []
    keyRows.value = []
    keyErrorMsg.value = e?.message || '读取缓存键列表失败'
  } finally {
    keysLoading.value = false
  }
}

// ═══ 清除操作（一律以后端 success 判定；失败不改动本地数据） ═══

/** 清空单个区域：真实删除 Redis 中匹配 `{name}:*` 的键 */
async function clearRegion(name: string) {
  try {
    const res: any = await request.delete('/cache/region/' + encodeURIComponent(name))
    if (res?.success) {
      message.success(res?.message || `缓存区域「${name}」已清空`)
      await fetchCacheStatus()
    } else {
      message.error(res?.message || `清空缓存区域「${name}」失败`)
    }
  } catch (e: any) {
    console.error('[缓存管理] 清空缓存区域失败', e)
    message.error(e?.message || `清空缓存区域「${name}」失败`)
  }
}

/**
 * 清空全部缓存 —— 危险操作二次确认。
 * 该操作删除的是整个 Redis db（含 sa-token 登录会话），操作者自己也会被踢下线，
 * 故确认文案必须如实写明后果（后端无 dry-run，无法预览影响面）。
 */
function confirmClearAll() {
  Modal.confirm({
    title: '确定清空全部缓存？',
    okText: '确定清空',
    okType: 'danger',
    cancelText: '取消',
    width: 520,
    content: h('div', { class: 'danger-confirm-content' }, [
      h('p', { style: 'margin-bottom:6px' }, '此操作会真实删除 Redis 中的全部键，且不可撤销：'),
      h('ul', { style: 'padding-left:20px;margin:0' }, [
        h('li', '包含登录会话（Authorization:login:* 等），当前所有在线用户（含你自己）会被立即踢下线'),
        h('li', '页面上的「缓存区域」只是按键前缀聚合的视图，本操作并不只清这些区域'),
        h('li', '清空后需要重新登录才能继续使用系统'),
      ]),
    ]),
    onOk: () => clearAllCache(),
  })
}

async function clearAllCache() {
  try {
    const res: any = await request.delete('/cache/all')
    if (res?.success) {
      message.success(res?.message || '已清空全部缓存')
      // 会话已被删除，此处刷新多半会因 401 跳转登录页；仍调一次以反映真实状态
      await fetchCacheStatus()
    } else {
      message.error(res?.message || '清空全部缓存失败')
    }
  } catch (e: any) {
    console.error('[缓存管理] 清空全部缓存失败', e)
    message.error(e?.message || '清空全部缓存失败')
  }
}

/** 删除单个键：success=false（如键不存在）时提示错误，且不动本地列表 */
async function deleteKey(record: CacheKeyItem) {
  try {
    const res: any = await request.delete(
      '/cache/region/' + encodeURIComponent(selectedRegion.value) + '/key/' + encodeURIComponent(record.key),
    )
    if (res?.success) {
      message.success(res?.message || '缓存键已删除')
      await loadKeys(selectedRegion.value)
    } else {
      message.error(res?.message || '删除缓存键失败')
    }
  } catch (e: any) {
    console.error('[缓存管理] 删除缓存键失败', e)
    message.error(e?.message || '删除缓存键失败')
  }
}

function handleError(error: Error) {
  console.error('[缓存管理] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

onMounted(fetchCacheStatus)
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; }
.page-header-left { display: flex; align-items: center; gap: 12px; }
.page-header-title { font-size: 18px; font-weight: 600; color: #303133; margin: 0; }
.page-header-right { display: flex; align-items: center; gap: 8px; }
.cache-page {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 12px 16px;
  overflow: auto;
}
.cache-alert { flex-shrink: 0; }
.overview-row { flex-shrink: 0; }
.overview-note {
  flex-shrink: 0;
  font-size: 12px;
  line-height: 1.7;
  color: #8c8c8c;
}
.overview-note code {
  padding: 0 4px;
  background: #f5f5f5;
  border-radius: 3px;
}
.region-panel {
  flex: 1;
  min-height: 320px;
  display: flex;
  flex-direction: column;
  border: 1px solid #f0f0f0;
  border-radius: 6px;
  overflow: hidden;
}
.region-toolbar {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 8px 12px;
  border-bottom: 1px solid #f0f0f0;
}
.panel-title { font-size: 14px; font-weight: 600; color: #303133; }
.region-toolbar-right { display: flex; align-items: center; gap: 8px; }
/* ⚠️ 必须是 flex 纵向容器：BillDetailTable 根元素为 flex:1，父级非 flex 时表格高度会塌陷为 0 */
.table-area { flex: 1; min-height: 0; overflow: hidden; display: flex; flex-direction: column; }
.key-toolbar { display: flex; align-items: center; gap: 12px; margin-bottom: 8px; }
.key-count { font-size: 12px; color: #8c8c8c; }
.key-alert { margin-bottom: 8px; }
.key-table-area { height: 58vh; min-height: 260px; display: flex; flex-direction: column; overflow: hidden; }
.key-name { word-break: break-all; }
</style>
