<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        我要推广（营销 → 营销推广 → 我要推广，菜单 80330）
        对标 ql361「营销 → 营销推广 → 我要推广」：**6 Tab 推广物料工作台**（逐 Tab 独立列配置）
          · 商品     17 列（默认 15，零售价/批发价 默认隐藏）
          · 优惠券   11 列
          · 促销     12 列
          · 拼团     10 列
          · 秒杀      8 列
          · 我的推广  9 列（仅当前登录人的分享台账）
        对标文档：docs/Yh-Spec/手动整理对标开发文档/营销模块/我要推广开发文档.md
        页面配置弹窗：**仅「商品」「促销」两个 Tab 有**（README §5.5 实测），其余 Tab 无 → 按 Tab 条件挂载
        分享统计口径：mkt_share_record 按「分享类型 + 物料 id」聚合，页面侧与物料列表合并（雪花 id 一律按字符串比对）
      -->
      <CategoryListLayout
        :tabs="TABS"
        :active-tab="activeTab"
        :show-category-panel="false"
        :show-table-footer="true"
        @tab-change="handleTabChange"
      >
        <template #toolbar-right>
          <a-space :size="8">
            <a-button
              size="small"
              :loading="loading"
              @click="fetchList"
            >
              <ReloadOutlined /> 刷新
            </a-button>
            <a-button
              size="small"
              @click="handlePrint"
            >
              <PrinterOutlined /> 打印(F8)
            </a-button>
            <a-button
              v-if="hasPageConfig"
              size="small"
              title="页面配置"
              @click="showPageConfig = true"
            >
              <SettingOutlined />
            </a-button>
          </a-space>
        </template>

        <template #search-fields>
          <div class="search-area">
            <div class="search-row">
              <span class="search-label">{{ searchLabel }}</span>
              <a-input
                v-model:value="keyword"
                :placeholder="searchPlaceholder"
                size="small"
                style="width: 220px"
                allow-clear
                @press-enter="handleSearch"
              />
              <a-button
                type="primary"
                size="small"
                @click="handleSearch"
              >
                查询
              </a-button>
            </div>
          </div>
        </template>

        <template #table>
          <div class="table-area">
            <BillDetailTable
              v-model:data-source="tableData"
              :columns="columns"
              :loading="loading"
              :view-mode="true"
              :min-rows="20"
              row-key="id"
              :storage-key="tableStorageKey"
              :global-config-key="tableStorageKey"
            >
              <!-- ═══ 商品 Tab：图片 / 分享 ═══ -->
              <template #imageCell="{ record }">
                <img
                  v-if="!record.__ghost && record.imageUrl"
                  :src="record.imageUrl"
                  @error="onImageError"
                  class="promote-img"
                  alt="商品图片"
                >
                <span v-else-if="!record.__ghost">-</span>
              </template>
              <template #moneyCell="{ record, column }">
                <span v-if="!record.__ghost">{{ fmtMoney(record[column.key]) }}</span>
              </template>
              <template #timeCell="{ record, column }">
                <span v-if="!record.__ghost">{{ fmtTime(record[column.key]) }}</span>
              </template>
              <template #typeCell="{ record }">
                <span v-if="!record.__ghost">{{ COUPON_TYPE_MAP[record.couponType] || record.couponType || '-' }}</span>
              </template>
              <template #scopeCell="{ record }">
                <span v-if="!record.__ghost">{{ record.customerScope === 'SPECIFIED' ? '指定客户' : '全部客户' }}</span>
              </template>
              <template #promoStatusCell="{ record }">
                <a-tag
                  v-if="!record.__ghost"
                  :color="PROMO_STATUS_MAP[record.status]?.color || 'default'"
                >
                  {{ PROMO_STATUS_MAP[record.status]?.text || record.status || '-' }}
                </a-tag>
              </template>
              <template #flashStatusCell="{ record }">
                <a-tag
                  v-if="!record.__ghost"
                  :color="FLASH_STATUS_MAP[record.status]?.color || 'default'"
                >
                  {{ FLASH_STATUS_MAP[record.status]?.text || record.status }}
                </a-tag>
              </template>
              <template #shareTypeCell="{ record }">
                <span v-if="!record.__ghost">{{ SHARE_TYPE_MAP[record.shareType] || record.shareType || '-' }}</span>
              </template>

              <!-- ═══ 行级「分享」（前 5 个物料 Tab） ═══ -->
              <template #actionCell="{ record }">
                <a-space
                  v-if="!record.__ghost && activeTab !== 'mine'"
                  :size="0"
                >
                  <a-button
                    type="link"
                    size="small"
                    @click="openShare(record)"
                  >
                    分享
                  </a-button>
                </a-space>
              </template>
            </BillDetailTable>
          </div>
        </template>

        <template #table-footer>
          <StandardPagination
            variant="classic"
            :current="pagination.current"
            :page-size="pagination.pageSize"
            :total="pagination.total"
            :page-size-options="[20, 50, 100]"
            @change="handlePageChange"
          />
        </template>
      </CategoryListLayout>

      <!-- ═══ 页面配置（仅「商品」「促销」Tab 有，对标实测） ═══ -->
      <PageConfigPanel
        v-if="hasPageConfig"
        :open="showPageConfig"
        :query-fields-config="queryFieldsConfig"
        :function-buttons-config="functionButtonConfig"
        :default-query-fields-config="DEFAULT_QUERY_FIELDS"
        :default-function-buttons-config="DEFAULT_FUNCTION_BUTTONS"
        :storage-key="`marketing-promote-page-config-${activeTab}`"
        @update:open="showPageConfig = $event"
        @change="handlePageConfigChange"
      />

      <!-- ═══ 分享确认 ═══ -->
      <a-modal
        v-model:open="shareOpen"
        title="分享推广"
        :confirm-loading="sharing"
        width="560px"
        ok-text="确认分享"
        @ok="handleShare"
      >
        <a-form
          :label-col="{ span: 5 }"
          :wrapper-col="{ span: 18 }"
          size="small"
          style="margin-top: 12px"
        >
          <a-form-item label="分享类型">
            {{ SHARE_TYPE_MAP[activeTabType] || activeTabType }}
          </a-form-item>
          <a-form-item label="分享概要">
            <a-input v-model:value="shareForm.shareSummary" />
          </a-form-item>
          <a-form-item label="分享链接">
            <a-input
              :value="shareForm.link"
              read-only
            />
          </a-form-item>
        </a-form>
        <div class="share-tip">
          确认后将登记一次分享记录，并可在「我的推广」与「推广历史查询」中回溯。
        </div>
      </a-modal>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import { ReloadOutlined, PrinterOutlined, SettingOutlined } from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import StandardPagination from '@/components/Pagination/Pagination.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import {
  promoteApi, couponTemplateApi, promoActivityApi, groupBuyApi, flashSaleApi, shareApi,
  SHARE_TYPE_MAP, COUPON_TYPE_MAP, PROMO_STATUS_MAP,
} from '@/api/marketing'

defineOptions({ name: 'MarketingPromoteCreate' })

const TABS = [
  { key: 'product', label: '商品' },
  { key: 'coupon', label: '优惠券' },
  { key: 'promotion', label: '促销' },
  { key: 'group', label: '拼团' },
  { key: 'flash', label: '秒杀' },
  { key: 'mine', label: '我的推广' },
]
const activeTab = ref('product')

/** 秒杀状态（与后端 FlashSale 注释一致：0 草稿 / 1 已发布 / 2 已取消 / 3 已结束） */
const FLASH_STATUS_MAP: Record<number, { text: string; color: string }> = {
  0: { text: '未开始', color: 'default' },
  1: { text: '进行中', color: 'green' },
  2: { text: '已取消', color: 'red' },
  3: { text: '已结束', color: 'orange' },
}

/** 分享统计用的物料类型（我的推广 Tab 无） */
const TAB_SHARE_TYPE: Record<string, string> = {
  product: 'PRODUCT',
  coupon: 'COUPON',
  promotion: 'PROMOTION',
  group: 'GROUP_BUY',
  flash: 'FLASH_SALE',
}
const activeTabType = computed(() => TAB_SHARE_TYPE[activeTab.value] || '')

const loading = ref(false)
const tableData = ref<any[]>([])
const keyword = ref('')
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

const searchLabel = computed(() => (activeTab.value === 'mine' ? '分享类型' : '筛选条件'))
const searchPlaceholder = computed(() => (activeTab.value === 'mine'
  ? '请输入分享概要' : '请输入名称/编号'))

const tableStorageKey = computed(() => `marketing-promote-${activeTab.value}-table-columns`)

// ═══ 各 Tab 列定义（逐 Tab 与对标实测一致） ═══
const ACTION_COLUMN: DetailColumnConfig = {
  key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 90, fixed: 'left',
}

const COLUMNS: Record<string, DetailColumnConfig[]> = {
  // 商品 17 列（零售价/批发价 默认隐藏 → 默认可见 15）
  product: [
    { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
    ACTION_COLUMN,
    { key: 'imageUrl', title: '图片', type: 'slot', slotName: 'imageCell', width: 90 },
    { key: 'productCode', title: '商品货号', type: 'input', width: 140 },
    { key: 'productName', title: '商品名称', type: 'input', width: 240 },
    { key: 'brand', title: '品牌', type: 'input', width: 120 },
    { key: 'spec', title: '规格', type: 'input', width: 130 },
    { key: 'model', title: '型号', type: 'input', width: 120 },
    { key: 'origin', title: '产地', type: 'input', width: 120 },
    { key: 'unit', title: '单位', type: 'input', width: 80 },
    { key: 'stock', title: '库存', type: 'input', width: 100 },
    { key: 'lastSaleTime', title: '最近销售时间', type: 'slot', slotName: 'timeCell', width: 160 },
    { key: 'createTime', title: '新增时间', type: 'slot', slotName: 'timeCell', width: 160 },
    { key: 'retailPrice', title: '零售价', type: 'slot', slotName: 'moneyCell', width: 110, defaultHidden: true },
    { key: 'wholesalePrice', title: '批发价', type: 'slot', slotName: 'moneyCell', width: 110, defaultHidden: true },
    { key: 'lastShareTime', title: '最近分享时间', type: 'slot', slotName: 'timeCell', width: 160 },
    { key: 'shareCount', title: '分享次数', type: 'input', width: 100 },
    { key: 'viewerCount', title: '浏览人数', type: 'input', width: 100 },
    { key: 'viewCount', title: '浏览次数', type: 'input', width: 100 },
  ],
  // 优惠券 11 列
  coupon: [
    { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
    ACTION_COLUMN,
    { key: 'couponName', title: '优惠券名称', type: 'input', width: 220 },
    { key: 'couponType', title: '类型', type: 'slot', slotName: 'typeCell', width: 100 },
    { key: 'totalCount', title: '总数', type: 'input', width: 90 },
    { key: 'receivedCount', title: '已领取', type: 'input', width: 90 },
    { key: 'remainingCount', title: '未领取', type: 'input', width: 90 },
    { key: 'customerScope', title: '指定客户', type: 'slot', slotName: 'scopeCell', width: 110 },
    { key: 'lastShareTime', title: '最近分享时间', type: 'slot', slotName: 'timeCell', width: 160 },
    { key: 'shareCount', title: '分享次数', type: 'input', width: 100 },
    { key: 'viewerCount', title: '浏览人数', type: 'input', width: 100 },
    { key: 'viewCount', title: '浏览次数', type: 'input', width: 100 },
    { key: 'receiveCount', title: '分享领取数', type: 'input', width: 110 },
  ],
  // 促销 12 列
  promotion: [
    { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
    ACTION_COLUMN,
    { key: 'name', title: '活动名称', type: 'input', width: 240 },
    { key: 'startTime', title: '起始时间', type: 'slot', slotName: 'timeCell', width: 170 },
    { key: 'endTime', title: '结束时间', type: 'slot', slotName: 'timeCell', width: 170 },
    { key: 'status', title: '状态', type: 'slot', slotName: 'promoStatusCell', width: 110 },
    { key: 'promoMethod', title: '促销方式', type: 'input', width: 110 },
    { key: 'promoMode', title: '促销模式', type: 'input', width: 110 },
    { key: 'productIds', title: '促销商品', type: 'input', width: 110 },
    { key: 'customerLevels', title: '促销客户', type: 'input', width: 130 },
    { key: 'lastShareTime', title: '最近分享时间', type: 'slot', slotName: 'timeCell', width: 160 },
    { key: 'shareCount', title: '分享次数', type: 'input', width: 100 },
    { key: 'viewerCount', title: '浏览人数', type: 'input', width: 100 },
    { key: 'viewCount', title: '浏览次数', type: 'input', width: 100 },
  ],
  // 拼团 10 列
  group: [
    { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
    ACTION_COLUMN,
    { key: 'activityCode', title: '活动ID', type: 'input', width: 140 },
    { key: 'activityName', title: '活动名称', type: 'input', width: 240 },
    { key: 'startTime', title: '起始时间', type: 'slot', slotName: 'timeCell', width: 170 },
    { key: 'endTime', title: '结束时间', type: 'slot', slotName: 'timeCell', width: 170 },
    { key: 'groupType', title: '成团类型', type: 'input', width: 110 },
    { key: 'status', title: '状态', type: 'input', width: 110 },
    { key: 'lastShareTime', title: '最近分享时间', type: 'slot', slotName: 'timeCell', width: 160 },
    { key: 'shareCount', title: '分享次数', type: 'input', width: 100 },
    { key: 'viewerCount', title: '浏览人数', type: 'input', width: 100 },
    { key: 'viewCount', title: '浏览次数', type: 'input', width: 100 },
  ],
  // 秒杀 8 列
  flash: [
    { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
    ACTION_COLUMN,
    { key: 'title', title: '活动名称', type: 'input', width: 260 },
    { key: 'startTime', title: '起始时间', type: 'slot', slotName: 'timeCell', width: 170 },
    { key: 'endTime', title: '结束时间', type: 'slot', slotName: 'timeCell', width: 170 },
    { key: 'status', title: '状态', type: 'slot', slotName: 'flashStatusCell', width: 110 },
    { key: 'lastShareTime', title: '最近分享时间', type: 'slot', slotName: 'timeCell', width: 160 },
    { key: 'shareCount', title: '分享次数', type: 'input', width: 100 },
    { key: 'viewerCount', title: '浏览人数', type: 'input', width: 100 },
    { key: 'viewCount', title: '浏览次数', type: 'input', width: 100 },
  ],
  // 我的推广 9 列
  mine: [
    { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
    { key: 'shareTime', title: '分享时间', type: 'slot', slotName: 'timeCell', width: 170 },
    { key: 'shareType', title: '分享类型', type: 'slot', slotName: 'shareTypeCell', width: 110 },
    { key: 'shareSummary', title: '分享概要', type: 'input', width: 320 },
    { key: 'viewCount', title: '浏览次数', type: 'input', width: 110 },
    { key: 'viewerCount', title: '浏览人数', type: 'input', width: 110 },
    { key: 'receiveCount', title: '分享领取数', type: 'input', width: 110 },
    { key: 'orderUserCount', title: '下单人数', type: 'input', width: 110 },
    { key: 'orderCount', title: '下单笔数', type: 'input', width: 110 },
    { key: 'orderAmount', title: '下单金额', type: 'slot', slotName: 'moneyCell', width: 120 },
  ],
}

const columns = computed(() => COLUMNS[activeTab.value] || [])

// ═══ 页面配置（仅 商品 / 促销 两个 Tab，对标实测） ═══
interface QueryFieldSetting { key: string; label: string; visible: boolean }
interface FunctionButtonSetting { key: string; label: string; enabled: boolean }

const hasPageConfig = computed(() => activeTab.value === 'product' || activeTab.value === 'promotion')
const DEFAULT_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'promote.keyword', label: '筛选条件', visible: true },
]
const DEFAULT_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'printF8', label: '打印(F8)', enabled: true },
  { key: 'share', label: '分享', enabled: true },
  { key: 'config', label: '页面配置', enabled: true },
]
const showPageConfig = ref(false)
const queryFieldsConfig = ref<QueryFieldSetting[]>(DEFAULT_QUERY_FIELDS.map(f => ({ ...f })))
const functionButtonConfig = ref<FunctionButtonSetting[]>(DEFAULT_FUNCTION_BUTTONS.map(f => ({ ...f })))

function handlePageConfigChange(cfg: any) {
  if (cfg?.queryFields) queryFieldsConfig.value = cfg.queryFields
  if (cfg?.functionButtons) functionButtonConfig.value = cfg.functionButtons
}

// ═══ 工具 ═══
function fmtTime(v: any): string {
  return v ? dayjs(v).format('YYYY-MM-DD HH:mm:ss') : '-'
}
function fmtMoney(v: any): string {
  return v == null ? '-' : Number(v).toFixed(2)
}

/** 把分享统计按 targetId 合并进物料行（雪花 id 一律按字符串比对，避免精度丢失去重） */
function mergeShareStats(rows: any[], stats: any[]): any[] {
  const map = new Map<string, any>()
  for (const s of stats || []) map.set(String(s.targetId), s)
  return (rows || []).map(r => {
    const s = map.get(String(r.id))
    return {
      ...r,
      lastShareTime: s?.lastShareTime,
      shareCount: s?.shareCount ?? 0,
      viewerCount: s?.viewerCount ?? 0,
      viewCount: s?.viewCount ?? 0,
      receiveCount: s?.receiveCount ?? 0,
    }
  })
}

// ═══ 数据 ═══
async function fetchList() {
  loading.value = true
  try {
    const tab = activeTab.value
    let rows: any[] = []
    let total = 0
    if (tab === 'product') {
      const res: any = await promoteApi.productPage({
        keyword: keyword.value || undefined,
        pageNum: pagination.current, pageSize: pagination.pageSize,
      })
      rows = res?.records || []
      total = Number(res?.total) || 0
    } else if (tab === 'coupon') {
      const res: any = await couponTemplateApi.page({
        couponName: keyword.value || undefined,
        pageNum: pagination.current, pageSize: pagination.pageSize,
      })
      rows = res?.records || []
      total = Number(res?.total) || 0
    } else if (tab === 'promotion') {
      const res: any = await promoActivityApi.page({
        name: keyword.value || undefined,
        pageNum: pagination.current, pageSize: pagination.pageSize,
      })
      rows = (res?.records || []).map((r: any) => ({ ...r, promoMethod: r.type }))
      total = Number(res?.total) || 0
    } else if (tab === 'group') {
      const res: any = await groupBuyApi.activityPage({
        name: keyword.value || undefined,
        pageNum: pagination.current, pageSize: pagination.pageSize,
      })
      rows = res?.records || []
      total = Number(res?.total) || 0
    } else if (tab === 'flash') {
      const res: any = await flashSaleApi.page({
        title: keyword.value || undefined,
        pageNum: pagination.current, pageSize: pagination.pageSize,
      })
      rows = res?.records || []
      total = Number(res?.total) || 0
    } else {
      const res: any = await shareApi.myPage({
        pageNum: pagination.current, pageSize: pagination.pageSize,
      })
      rows = res?.records || []
      total = Number(res?.total) || 0
    }

    // 前 5 个 Tab：合并分享统计
    if (tab !== 'mine') {
      try {
        const stats = await shareApi.summary(TAB_SHARE_TYPE[tab])
        rows = mergeShareStats(rows, stats as any[])
      } catch (e) {
        console.error('[我要推广] 加载分享统计失败（列表仍展示）', e)
      }
    }

    tableData.value = rows
    pagination.total = total
  } catch (error: any) {
    console.error('[我要推广] 加载列表失败', error)
    message.error(error?.response?.data?.message || '加载列表失败')
    tableData.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

function handleTabChange(key: string) {
  activeTab.value = key
  keyword.value = ''
  pagination.current = 1
  fetchList()
}
function handleSearch() {
  pagination.current = 1
  fetchList()
}
function handlePageChange(page: number, pageSize: number) {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchList()
}

// ═══ 分享 ═══
const shareOpen = ref(false)
const sharing = ref(false)
const shareTarget = ref<any>(null)
const shareForm = reactive({ shareSummary: '', link: '' })

function summarize(record: any): string {
  const tab = activeTab.value
  if (tab === 'product') return `商品 ${record.productName || ''}`
  if (tab === 'coupon') return `优惠券 ${record.couponName || ''}`
  if (tab === 'promotion') return `促销 ${record.name || ''}`
  if (tab === 'group') return `拼团 ${record.activityName || ''}`
  if (tab === 'flash') return `秒杀 ${record.title || ''}`
  return ''
}

function openShare(record: any) {
  shareTarget.value = record
  shareForm.shareSummary = summarize(record)
  shareForm.link = `${window.location.origin}/promote/${activeTab.value}/${record.id}`
  shareOpen.value = true
}

async function handleShare() {
  if (!shareTarget.value) return
  sharing.value = true
  try {
    await shareApi.create({
      shareType: activeTabType.value,
      targetId: shareTarget.value.id,
      targetName: summarize(shareTarget.value).replace(/^\S+\s/, ''),
      shareSummary: shareForm.shareSummary,
    })
    message.success('已登记分享')
    shareOpen.value = false
    fetchList()
  } catch (error: any) {
    message.error(error?.response?.data?.message || '分享登记失败')
  } finally {
    sharing.value = false
  }
}

// ═══ 打印 ═══
function escapeHtml(v: any): string {
  return String(v ?? '').replace(/[&<>"']/g, c => (
    { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c] as string
  ))
}

function handlePrint() {
  const rows = (tableData.value || []).filter((r: any) => !r.__ghost)
  if (!rows.length) {
    message.warning('没有可打印的数据')
    return
  }
  const cols = columns.value.filter(c => c.type !== 'rowNo' && c.type !== 'action')
  const tabLabel = TABS.find(t => t.key === activeTab.value)?.label || ''
  const cellValue = (r: any, c: any) => {
    const v = r[c.key]
    if (v == null) return ''
    if (c.slotName === 'timeCell') return fmtTime(v)
    if (c.slotName === 'moneyCell') return fmtMoney(v)
    if (c.slotName === 'typeCell') return COUPON_TYPE_MAP[v] || v
    if (c.slotName === 'scopeCell') return v === 'SPECIFIED' ? '指定客户' : '全部客户'
    if (c.slotName === 'promoStatusCell') return PROMO_STATUS_MAP[v]?.text || v
    if (c.slotName === 'flashStatusCell') return FLASH_STATUS_MAP[v]?.text || v
    if (c.slotName === 'shareTypeCell') return SHARE_TYPE_MAP[v] || v
    return String(v)
  }
  const body = rows.map((r: any, i: number) => `<tr><td>${i + 1}</td>${
    cols.map(c => `<td>${escapeHtml(cellValue(r, c))}</td>`).join('')}</tr>`).join('')
  const html = `<!DOCTYPE html><html><head><meta charset="utf-8" /><title>我要推广-${tabLabel}</title>
    <style>body{font-family:"Microsoft YaHei",Arial,sans-serif;margin:0;padding:16px}
    h2{text-align:center;margin:0 0 12px;font-size:18px}
    table{width:100%;border-collapse:collapse;font-size:12px}
    th,td{border:1px solid #999;padding:4px 6px;text-align:left}th{background:#f2f2f2}</style></head><body>
    <h2>我要推广 · ${tabLabel}</h2>
    <table><thead><tr><th>#</th>${cols.map(c => `<th>${c.title}</th>`).join('')}</tr></thead>
    <tbody>${body}</tbody></table></body></html>`
  const win = window.open('', '_blank', 'width=1200,height=800')
  if (!win) {
    message.warning('浏览器阻止了打印窗口，请允许弹出窗口后重试')
    return
  }
  win.document.write(html)
  win.document.close()
  win.focus()
  win.print()
}

function handleF8Key(e: KeyboardEvent) {
  if ((e.key === 'F8' || e.code === 'F8') && !e.ctrlKey && !e.altKey && !e.metaKey) {
    e.preventDefault()
    handlePrint()
  }
}

/** 图片源缺失时不显示破图（商品图片可能为历史遗留 URL，文件已不存在） */
function onImageError(e: Event) {
  const el = e.target as HTMLImageElement
  if (el) el.style.display = 'none'
}

function handleError(error: Error) {
  console.error('[我要推广] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

onMounted(() => {
  fetchList()
  window.addEventListener('keydown', handleF8Key)
})
onBeforeUnmount(() => window.removeEventListener('keydown', handleF8Key))
</script>

<style scoped>
.search-area { padding: 8px 0; }
.search-row { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.search-label { font-size: 13px; color: #666; white-space: nowrap; }
.table-area { flex: 1; min-height: 0; overflow: hidden; display: flex; flex-direction: column; }
.promote-img { width: 48px; height: 48px; object-fit: cover; border-radius: 4px; }
.share-tip { margin-top: 8px; font-size: 12px; color: #999; }
:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) { height: 28px; line-height: 28px; }
</style>
