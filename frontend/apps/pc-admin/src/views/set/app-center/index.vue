<template>
  <ErrorBoundary @error="handleError">
    <PageContainer
      title="应用中心"
      full-height
    >
      <!--
        应用中心（设置 → 系统配置 → 应用中心，菜单 80625 / set:app-center，单入口）
        规格书：docs/Yh-Spec/手动整理对标开发文档/设置模块/应用中心开发文档.md

        · 定位（**租户级只读总览**）：「我这个租户安装了哪些模块 / 开通了哪些 / 到期没有 / 配额用了多少」。
          开通·停用·续费·授权下发属**平台侧**（系统模块），本页不提供写入口（整页只读）。
        · 外壳：路线 A′（ErrorBoundary > PageContainer(full-height) + 自绘分区）。
          **刻意不做页内 Tab / 左列纵向选择器**：开发文档 §3.2 已实测裁定「对标页无 Tab，本页单页多区，此点符合对标」，
          加 Tab 反而破坏该项达标；且本页非数据表列表 → 不接 BillDetailTable / PageConfigPanel / 列配置。
        · 数据源（全部为真实表，**不造假数据**）：
            信息卡    —— sys_tenant（公司名称 / 租户编码 / 等级 / 到期日期）
                          + sys_tenant_quota（用户数·存储·API 调用用量，真实配额表，此前未接）
                          + mkt_sms_setting + mkt_sms_record（短信用量与本月发送，2026-09-18 实核后接入）
            功能模块  —— sys_module（系统模块注册表：已安装） ⨝ sys_tenant_module（本租户开通记录：已开通）
            「短信及其他」—— sys_menu 中**真实存在**的租户端能力菜单（无则为空并如实提示）
            可购买套餐 —— sys_tenant_package（平台侧目录，只读；无平台权限时**显式提示**，不静默隐藏）
        · 明确不做的对标项（本系统无数据源，登记在页面底部「未提供的能力」区，绝不编造）：
            购买记录 / 硬件服务目录 / 增值服务商品目录 / 物流查询次数（无查询计量表） / 模块开通续费。
        · 取数：request 拦截器已拆包，直接拿数组/对象，**不要再取 res.data**（历史 P0 陷阱）。
      -->

      <!-- 顶栏：刷新（真实重取三方接口）/ 前往企业信息（真实跳转） -->
      <template #headerExtra>
        <a-space :size="8">
          <a-button
            size="small"
            :loading="loading"
            @click="loadAll"
          >
            <ReloadOutlined /> 刷新
          </a-button>
          <a-button
            size="small"
            @click="goPage('/set/company-info')"
          >
            <ProfileOutlined /> 企业信息
          </a-button>
        </a-space>
      </template>

      <div class="app-center">
        <!-- 加载/失败：失败**显式提示**（原实现在 catch 里只 console.warn，静默降级 = 页面少一整块且用户不知原因） -->
        <a-alert
          v-if="errorText"
          type="error"
          show-icon
          class="block-alert"
          :message="errorText"
        >
          <template #action>
            <a-button
              size="small"
              danger
              @click="loadAll"
            >
              重试
            </a-button>
          </template>
        </a-alert>

        <!-- ═══ ① 信息卡（ql361 四行信息：公司名称 / 剩余短信 / 物流查询 / 到期日期） ═══ -->
        <a-card
          size="small"
          class="section-card"
        >
          <template #title>
            租户信息
          </template>
          <a-spin :spinning="loading">
            <a-descriptions
              size="small"
              bordered
              :column="{ xs: 1, sm: 2, md: 3 }"
            >
              <a-descriptions-item label="公司名称">
                {{ overview.companyName || '—' }}
              </a-descriptions-item>
              <a-descriptions-item label="租户编码">
                {{ overview.tenantCode || '—' }}
              </a-descriptions-item>
              <a-descriptions-item label="套餐等级">
                {{ overview.tenantLevel || '未设置' }}
              </a-descriptions-item>

              <a-descriptions-item label="到期日期">
                <!-- 全租户 sys_tenant.expire_time 为空（实测）→ 如实显示「长期有效」，
                     不用 create_time + 套餐时长推算（那是本系统未建立的裁定口径，会造出假到期日） -->
                <template v-if="overview.expireDate">
                  {{ overview.expireDate }}
                  <a-tag :color="expireTag.color">
                    {{ expireTag.text }}
                  </a-tag>
                </template>
                <span v-else>长期有效（sys_tenant 未设置到期日期）</span>
              </a-descriptions-item>

              <!-- ql361 信息卡有「剩余短信 / 物流查询次数」：
                   短信 → 已接真实来源（mkt_sms_setting 配额 + mkt_sms_record 本月发送）；
                   物流查询 → 全库无查询计量表，保持「未接入」但**写清缺什么**（不再一句含糊的「未接入」） -->
              <a-descriptions-item label="剩余短信">
                <template v-if="overview.sms?.available">
                  <b>{{ overview.sms?.quotaRemain ?? '—' }}</b> 条
                  <span class="muted">
                    配额 {{ overview.sms?.quotaTotal ?? '—' }} 条 / 已用 {{ overview.sms?.quotaUsed ?? '—' }} 条<template
                      v-if="overview.sms?.monthSent !== null && overview.sms?.monthSent !== undefined"
                    >，本月发送 {{ overview.sms?.monthSent }} 条</template>
                  </span>
                </template>
                <template v-else>
                  <a-tag>未接入</a-tag>
                  <span class="muted">{{ overview.sms?.reason || '本租户未配置短信配额' }}</span>
                </template>
              </a-descriptions-item>
              <a-descriptions-item label="物流查询">
                <a-tag>未接入</a-tag>
                <span class="muted">{{ overview.logisticsQuery?.reason || '缺少物流查询计量表，暂无法统计' }}</span>
              </a-descriptions-item>
            </a-descriptions>

            <!-- 配额用量：sys_tenant_quota（真实表；无本租户行时显示「未配置」） -->
            <a-divider class="sub-divider">
              配额用量
            </a-divider>
            <a-row
              v-if="quotaItems.length"
              :gutter="[16, 12]"
            >
              <a-col
                v-for="item in quotaItems"
                :key="item.key"
                :xs="24"
                :sm="8"
              >
                <div class="quota-item">
                  <div class="quota-head">
                    <span class="quota-label">{{ item.label }}</span>
                    <span class="quota-value">{{ item.text }}</span>
                  </div>
                  <a-progress
                    :percent="item.percent"
                    :show-info="false"
                    size="small"
                    :status="item.percent >= 90 ? 'exception' : 'normal'"
                  />
                </div>
              </a-col>
            </a-row>
            <a-empty
              v-else
              :image="simpleImage"
              description="本租户未配置配额（sys_tenant_quota 无本租户记录）"
            />
          </a-spin>
        </a-card>

        <!-- ═══ ② 功能模块（已安装 = 注册表；已开通 = 本租户授权记录） ═══ -->
        <a-card
          size="small"
          class="section-card"
        >
          <template #title>
            功能模块
            <span class="title-count">已开通 {{ overview.openedModuleCount }} / 已安装 {{ overview.installedModuleCount }}</span>
          </template>
          <template #extra>
            <a-space :size="8">
              <a-input-search
                v-model:value="keyword"
                placeholder="搜索模块名称 / 编码"
                style="width: 240px"
                allow-clear
              />
              <a-button
                size="small"
                :loading="moduleLoading"
                @click="loadModules"
              >
                <ReloadOutlined /> 刷新
              </a-button>
            </a-space>
          </template>

          <!-- 一条开通记录都没有时：不空转，给出可操作提示 -->
          <a-alert
            v-if="!loading && overview.openedModuleCount === 0"
            type="warning"
            show-icon
            class="block-alert"
            message="本租户暂无模块开通记录（sys_tenant_module 无本租户行）"
            description="模块开通/续费属平台侧授权（系统模块 → 模块管理），本页为租户级只读页，无法自行开通。下方列出的是本系统已安装的模块，供对照查看。"
          />

          <a-spin :spinning="moduleLoading">
            <a-row :gutter="[16, 16]">
              <a-col
                v-for="mod in filteredModules"
                :key="mod.moduleCode"
                :xs="24"
                :sm="12"
                :md="8"
                :lg="6"
              >
                <div
                  class="module-card"
                  :class="{ 'is-off': !mod.licensed }"
                >
                  <div class="module-head">
                    <div
                      class="module-icon"
                      :style="{ background: moduleColor(mod.moduleCode) }"
                    >
                      {{ (mod.moduleName || '?').charAt(0) }}
                    </div>
                    <div class="module-title">
                      <div class="module-name">
                        {{ mod.moduleName || '—' }}
                      </div>
                      <a-tag class="module-code">
                        {{ mod.moduleCode }}
                      </a-tag>
                    </div>
                    <!-- 开关=**只读状态展示**（本页无开通/停用入口，故 disabled，不做假交互） -->
                    <a-switch
                      :checked="mod.licensed"
                      disabled
                      size="small"
                    />
                  </div>
                  <div class="module-meta">
                    <div class="meta-row">
                      <span class="meta-label">开通状态</span>
                      <a-tag :color="mod.licensed ? 'success' : 'default'">
                        {{ mod.licensed ? '已开通' : '未开通' }}
                      </a-tag>
                    </div>
                    <div class="meta-row">
                      <span class="meta-label">授权状态</span>
                      <a-tag :color="licenseStatusTag(mod).color">
                        {{ licenseStatusTag(mod).label }}
                      </a-tag>
                    </div>
                    <div class="meta-row">
                      <span class="meta-label">开通类型</span>
                      <a-tag :color="purchaseTypeTag(mod.purchaseType).color">
                        {{ purchaseTypeTag(mod.purchaseType).label }}
                      </a-tag>
                    </div>
                    <div class="meta-row">
                      <span class="meta-label">到期时间</span>
                      <span>{{ mod.licensed ? (mod.expireDate || '长期有效') : '—' }}</span>
                    </div>
                    <div class="meta-row">
                      <span class="meta-label">模块版本</span>
                      <span>{{ mod.version || '—' }}</span>
                    </div>
                    <div class="meta-row">
                      <span class="meta-label">系统状态</span>
                      <a-tag :color="sysStatusTag(mod.sysStatus).color">
                        {{ sysStatusTag(mod.sysStatus).label }}
                      </a-tag>
                    </div>
                    <div
                      v-if="mod.description"
                      class="module-desc"
                    >
                      {{ mod.description }}
                    </div>
                  </div>
                </div>
              </a-col>
            </a-row>
            <a-empty
              v-if="!moduleLoading && filteredModules.length === 0"
              :image="simpleImage"
              :description="keyword ? '没有匹配的模块' : '本系统未安装任何模块（sys_module 无记录）'"
            />
          </a-spin>
        </a-card>

        <!-- ═══ ③ 短信及其他（本系统真实存在的相关能力入口；无商品目录，不编造商品） ═══ -->
        <a-card
          size="small"
          class="section-card"
        >
          <template #title>
            短信及其他
          </template>
          <a-alert
            type="info"
            show-icon
            class="block-alert"
            message="本系统未建立增值服务商品目录"
            description="因此这里不展示可购买的增值商品（短信包 / 存储空间 / 物流查询 / 智能排线 等均无商品数据源）。下列为本系统已有的相关能力入口，点按直达对应页面。"
          />
          <a-row
            v-if="capabilities.length"
            :gutter="[16, 16]"
          >
            <a-col
              v-for="cap in capabilities"
              :key="cap.menuCode"
              :xs="12"
              :sm="8"
              :md="6"
            >
              <div
                class="cap-card"
                @click="goCapability(cap)"
              >
                <AppstoreOutlined class="cap-icon" />
                <div class="cap-name">
                  {{ cap.menuName || cap.menuCode }}
                </div>
                <div class="cap-path">
                  {{ cap.path || '—' }}
                </div>
              </div>
            </a-col>
          </a-row>
          <a-empty
            v-else-if="!loading"
            :image="simpleImage"
            description="本系统未匹配到相关能力菜单（sys_menu 中不存在对应编码）"
          />
        </a-card>

        <!-- ═══ ④ 可购买套餐（平台侧目录，只读；**无权限时显式提示，不静默隐藏整块**） ═══ -->
        <a-card
          size="small"
          class="section-card"
        >
          <template #title>
            可购买套餐<span class="title-count">只读 · 平台侧目录</span>
          </template>

          <a-alert
            v-if="!canReadPackages"
            type="warning"
            show-icon
            class="block-alert"
            message="当前账号无「平台租户套餐」查看权限"
            description="平台套餐目录需权限码 platform:tenant-package:list（属平台侧）。本页为租户级只读页，不展示套餐目录；如需了解可购买 / 升级套餐，请联系平台管理员。"
          />
          <template v-else>
            <a-spin :spinning="packageLoading">
              <a-row
                v-if="packageList.length"
                :gutter="[16, 16]"
              >
                <a-col
                  v-for="pkg in packageList"
                  :key="pkg.id"
                  :xs="24"
                  :sm="12"
                  :md="8"
                >
                  <div class="pkg-card">
                    <div class="pkg-header">
                      <span class="pkg-name">{{ pkg.packageName }}</span>
                      <a-tag :color="pkg.status === 1 ? 'success' : 'default'">
                        {{ pkg.status === 1 ? '生效中' : '已停用' }}
                      </a-tag>
                    </div>
                    <div class="pkg-meta">
                      <span>套餐编码：{{ pkg.packageCode }}</span>
                      <span v-if="pkg.purchaseType">计费方式：{{ packagePurchaseTypeText(pkg.purchaseType) }}</span>
                      <span v-if="pkg.maxUsers">最大用户数：{{ formatNumber(pkg.maxUsers) }}</span>
                      <span v-if="pkg.storageQuota">存储配额：{{ formatNumber(pkg.storageQuota) }} GB</span>
                      <span v-if="pkg.apiCallLimit">API 调用上限：{{ formatNumber(pkg.apiCallLimit) }} 次/月</span>
                      <!-- 价格列库侧单位是「分」（SysTenantPackage.price 注释「价格（分）」），故 /100 后再格式化 -->
                      <span v-if="pkg.price !== undefined && pkg.price !== null">价格：¥{{ formatPriceFen(pkg.price) }}</span>
                    </div>
                    <div
                      v-if="pkg.description"
                      class="pkg-desc"
                    >
                      {{ pkg.description }}
                    </div>
                  </div>
                </a-col>
              </a-row>
              <a-empty
                v-else
                :image="simpleImage"
                description="平台未定义套餐（sys_tenant_package 无记录）"
              />
            </a-spin>
          </template>
        </a-card>

        <!-- ═══ ⑤ 未提供的能力（如实登记，不造假数据） ═══ -->
        <a-card
          size="small"
          class="section-card"
        >
          <template #title>
            本页未提供的能力（如实登记）
          </template>
          <ul class="gap-list">
            <li><b>购买记录</b>：本系统无购买/订单记录表，故不提供入口（不做死按钮）。</li>
            <li><b>硬件服务</b>：本系统无硬件设备目录，故不展示。</li>
            <li><b>增值服务商品</b>：本系统无商品目录表，故不展示商品与价格（见上方「短信及其他」的真实能力入口）。</li>
            <li><b>物流查询次数</b>：缺少物流查询计量表（本系统未对接外部运单轨迹查询，现有物流表均为发货单/订单物流信息，不记录查询动作）→ 暂无法统计，如实标注「未接入」。</li>
            <li><b>短信用量</b>：已接真实来源（营销 → 发短信的配额表 mkt_sms_setting + 发送记录 mkt_sms_record），见上方信息卡；短信包等**商品目录**仍无数据源。</li>
            <li><b>模块开通 / 停用 / 续费</b>：属平台侧授权下发（系统模块 → 模块管理），本页只读。</li>
          </ul>
        </a-card>
      </div>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message, Empty } from 'ant-design-vue'
import {
  AppstoreOutlined,
  ProfileOutlined,
  ReloadOutlined
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import { appCenterApi, type AppCenterCapability, type AppCenterModule, type AppCenterOverview } from '@/api/set/app-center'
import { tenantPackageApi, type TenantPackageInfo } from '@/api/tenant'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

/** a-empty 用极简图，避免大块占位图在配置页里喧宾夺主 */
const simpleImage = Empty.PRESENTED_IMAGE_SIMPLE

const loading = ref(false)
const moduleLoading = ref(false)
const packageLoading = ref(false)

/** 三方取数的失败原因（**非静默**：任一失败都在页面顶部显式提示） */
const errors = reactive({ overview: '', modules: '', capabilities: '', packages: '' })

const overview = ref<AppCenterOverview>({
  companyName: null,
  tenantCode: null,
  tenantLevel: null,
  expireDate: null,
  expireDays: null,
  quota: null,
  sms: null,
  logisticsQuery: null,
  installedModuleCount: 0,
  openedModuleCount: 0
})
const modules = ref<AppCenterModule[]>([])
const capabilities = ref<AppCenterCapability[]>([])
const packageList = ref<TenantPackageInfo[]>([])
const keyword = ref('')

/**
 * 平台套餐目录的可见性：需权限码 platform:tenant-package:list（实测库中不存在 → 仅超管有 `*` 通配）。
 * 无权限时**不请求**（避免必然 403 的网络噪声），但**显式展示提示块**——这正是本页原实现的核心缺陷：
 * 原代码在 catch 里静默隐藏整块，用户既看不到内容也看不到原因。
 */
const canReadPackages = computed(() => userStore.hasPermission('platform:tenant-package:list'))

const errorText = computed(() => {
  const parts: string[] = []
  if (errors.overview) parts.push(`租户信息：${errors.overview}`)
  if (errors.modules) parts.push(`功能模块：${errors.modules}`)
  if (errors.capabilities) parts.push(`短信及其他：${errors.capabilities}`)
  if (errors.packages) parts.push(`可购买套餐：${errors.packages}`)
  return parts.length ? `部分数据加载失败 — ${parts.join('；')}` : ''
})

/** 到期标签：剩余天数 / 已过期 / 今天到期 */
const expireTag = computed(() => {
  const days = overview.value.expireDays
  if (days === null || days === undefined) return { text: '长期有效', color: 'default' }
  if (days < 0) return { text: `已过期 ${Math.abs(days)} 天`, color: 'error' }
  if (days === 0) return { text: '今天到期', color: 'warning' }
  return { text: `剩余 ${days} 天`, color: days <= 30 ? 'warning' : 'success' }
})

/**
 * 开通类型值域：**库侧 CHECK 约束 ck_module_purchase_type 只允许 permanent / auto_renew / manual**
 * （见 V3.7.0 建表语句与 SysTenantModule 实体注释）。
 * 原实现硬编码 trial / purchase / gift / free —— 这四个值在本库**不可能出现**（会被 CHECK 拒绝），
 * 属取值域写错，本次按库侧真实值域修正，并保留未命中兜底。
 */
const PURCHASE_TYPE_MAP: Record<string, { label: string; color: string }> = {
  permanent: { label: '永久', color: 'green' },
  auto_renew: { label: '自动续费', color: 'blue' },
  manual: { label: '手动延期', color: 'orange' }
}

/** 套餐级计费方式（与模块级 purchase_type 值域不同） */
const PACKAGE_PURCHASE_TYPE_MAP: Record<string, string> = {
  monthly: '按月',
  yearly: '按年',
  perpetual: '一次买断'
}

/** sys_module.status：0 = 禁用，1 = 启用（实体注释原文）；null = 不在注册表 */
const SYS_STATUS_MAP: Record<number, { label: string; color: string }> = {
  0: { label: '已禁用', color: 'default' },
  1: { label: '已启用', color: 'green' }
}

/** 固定色板，按模块编码哈希取色（不使用随机色，保证同一模块颜色稳定） */
const COLOR_PALETTE = ['#1890ff', '#52c41a', '#faad14', '#f5222d', '#722ed1', '#13c2c2', '#eb2f96', '#fa8c16']

const filteredModules = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  if (!kw) return modules.value
  return modules.value.filter(m =>
    (m.moduleName || '').toLowerCase().includes(kw) ||
    (m.moduleCode || '').toLowerCase().includes(kw)
  )
})

/** 配额三行的展示文案与百分比（无配置时整块走空态） */
const quotaItems = computed(() => {
  const q = overview.value.quota
  if (!q) return []
  const items = [
    {
      key: 'users',
      label: '用户数',
      used: q.usedUsers,
      max: q.maxUsers,
      text: `${q.usedUsers ?? 0} / ${q.maxUsers ?? '—'} 个`
    },
    {
      key: 'storage',
      label: '存储空间',
      used: q.usedStorage,
      max: q.maxStorage,
      text: `${q.usedStorage ?? '—'} / ${q.maxStorage ?? '—'}`
    },
    {
      key: 'api',
      label: 'API 调用（本月）',
      used: q.usedApiCalls,
      max: q.maxApiCalls,
      text: `${q.usedApiCalls ?? 0} / ${q.maxApiCalls ?? '—'} 次`
    }
  ]
  return items.map(item => ({ ...item, percent: quotaPercent(item.used, item.max) }))
})

function quotaPercent(used: number | string | null, max: number | string | null): number {
  const usedNum = toNumber(used)
  const maxNum = toNumber(max)
  if (usedNum === null || maxNum === null || maxNum <= 0) return 0
  return Math.min(100, Math.round((usedNum / maxNum) * 100))
}

/** 数字或 "3GB" 这类文本统一取数（取不到返回 null） */
function toNumber(val: number | string | null | undefined): number | null {
  if (val === null || val === undefined || val === '') return null
  const num = typeof val === 'number' ? val : parseFloat(String(val))
  return Number.isFinite(num) ? num : null
}

function moduleColor(code: string | undefined): string {
  const str = code || ''
  let hash = 0
  for (let i = 0; i < str.length; i++) {
    hash = (hash * 31 + str.charCodeAt(i)) >>> 0
  }
  return COLOR_PALETTE[hash % COLOR_PALETTE.length]
}

function purchaseTypeTag(type: string | null | undefined): { label: string; color: string } {
  if (!type) return { label: '—', color: 'default' }
  return PURCHASE_TYPE_MAP[type.toLowerCase()] || { label: type, color: 'default' }
}

/**
 * 套餐级计费方式：值域 monthly / yearly / perpetual（SysTenantPackage 实体注释原文），
 * 与模块级 purchase_type（permanent / auto_renew / manual）是**两套值域**，不可共用一张表。
 */
function packagePurchaseTypeText(type: string | null | undefined): string {
  if (!type) return '—'
  return PACKAGE_PURCHASE_TYPE_MAP[type.toLowerCase()] || type
}

/**
 * 授权状态三态口径（开发文档 §5.1 / §5.2）：
 * 未开通 → 「未开通」；`licenseStatus !== 0` → 「已停用」；已过到期日 → 「已过期」；其余 → 「有效」。
 *
 * ⚠️ 原实现写的是 `status !== 1 → 已停用`，与库侧语义**正好相反**：
 * 表注释与 `TenantModuleService#getValidModuleCodes` 均以 **status = 0 为「正常」**、1 为「停用」，
 * 故原实现会把所有正常开通的模块显示成「已停用」。本次按库侧真实口径修正。
 */
function licenseStatusTag(mod: AppCenterModule): { label: string; color: string } {
  if (!mod.licensed) return { label: '未开通', color: 'default' }
  if (mod.licenseStatus !== 0) return { label: '已停用', color: 'default' }
  if (mod.expireDate && new Date(`${mod.expireDate}T23:59:59`).getTime() < Date.now()) {
    return { label: '已过期', color: 'error' }
  }
  return { label: '有效', color: 'success' }
}

function sysStatusTag(status: number | null | undefined): { label: string; color: string } {
  if (status === null || status === undefined) return { label: '不在注册表', color: 'default' }
  return SYS_STATUS_MAP[status] || { label: String(status), color: 'default' }
}

function formatMoney(val: number | null | undefined): string {
  const num = toNumber(val)
  if (num === null) return '-'
  return num.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function formatNumber(val: number | null | undefined): string {
  const num = toNumber(val)
  if (num === null) return '-'
  return num.toLocaleString('zh-CN')
}

/** 金额（库侧单位：分）→ 元；null/undefined/NaN → '-' */
function formatPriceFen(fen: number | null | undefined): string {
  const num = toNumber(fen)
  if (num === null) return '-'
  return formatMoney(num / 100)
}

/** 菜单 path 是相对路径（如 dms/tracking），补前导斜杠后再跳转 */
function goPage(path: string) {
  const target = path.startsWith('/') ? path : `/${path}`
  router.push(target)
}

function goCapability(cap: AppCenterCapability) {
  if (!cap.path) {
    message.warning('该菜单未配置路由路径')
    return
  }
  goPage(cap.path)
}

async function loadOverview() {
  errors.overview = ''
  try {
    overview.value = await appCenterApi.overview()
  } catch (e) {
    errors.overview = '加载失败'
    console.warn('[应用中心] 概览获取失败', e)
  }
}

async function loadModules() {
  moduleLoading.value = true
  errors.modules = ''
  try {
    const res = await appCenterApi.modules()
    modules.value = Array.isArray(res) ? res : []
  } catch (e) {
    errors.modules = '加载失败'
    console.warn('[应用中心] 功能模块获取失败', e)
  } finally {
    moduleLoading.value = false
  }
}

async function loadCapabilities() {
  errors.capabilities = ''
  try {
    const res = await appCenterApi.capabilities()
    capabilities.value = Array.isArray(res) ? res : []
  } catch (e) {
    errors.capabilities = '加载失败'
    console.warn('[应用中心] 能力入口获取失败', e)
  }
}

async function loadPackages() {
  if (!canReadPackages.value) return
  packageLoading.value = true
  errors.packages = ''
  try {
    const res: any = await tenantPackageApi.getList()
    packageList.value = res?.records || []
  } catch (e) {
    errors.packages = '加载失败'
    console.warn('[应用中心] 套餐列表获取失败', e)
  } finally {
    packageLoading.value = false
  }
}

async function loadAll() {
  loading.value = true
  try {
    await Promise.all([loadOverview(), loadModules(), loadCapabilities(), loadPackages()])
  } finally {
    loading.value = false
  }
}

function handleError(err: unknown) {
  console.warn('[应用中心] ErrorBoundary:', err)
}

onMounted(loadAll)
</script>

<style scoped>
.app-center { flex: 1; min-height: 0; overflow-y: auto; padding: 12px 16px 24px; display: flex; flex-direction: column; gap: 12px; }
.section-card { border-radius: 8px; }
.block-alert { margin-bottom: 12px; }
.title-count { margin-left: 8px; font-size: 12px; font-weight: 400; color: #8c8c8c; }
.sub-divider { margin: 16px 0 12px; font-size: 13px; color: #595959; }
.muted { margin-left: 8px; font-size: 12px; color: #8c8c8c; }

/* 配额用量 */
.quota-item { padding: 4px 0; }
.quota-head { display: flex; justify-content: space-between; align-items: baseline; font-size: 13px; margin-bottom: 4px; }
.quota-label { color: #8c8c8c; }
.quota-value { color: #262626; font-weight: 600; }

/* 功能模块卡 */
.module-card { height: 100%; border: 1px solid #f0f0f0; border-radius: 8px; padding: 12px; background: #fff; }
.module-card.is-off { background: #fafafa; }
.module-head { display: flex; align-items: center; gap: 12px; margin-bottom: 12px; }
.module-icon { width: 40px; height: 40px; border-radius: 8px; display: flex; align-items: center; justify-content: center; font-size: 18px; font-weight: bold; color: #fff; flex-shrink: 0; }
.module-title { min-width: 0; flex: 1; }
.module-name { font-size: 14px; font-weight: 600; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.module-code { margin-top: 4px; }
.module-meta { display: flex; flex-direction: column; gap: 6px; }
.meta-row { display: flex; align-items: center; gap: 8px; font-size: 13px; }
.meta-label { color: #8c8c8c; width: 64px; flex-shrink: 0; }
.module-desc { font-size: 12px; color: #8c8c8c; line-height: 1.5; }

/* 「短信及其他」能力入口 */
.cap-card { border: 1px solid #f0f0f0; border-radius: 8px; padding: 16px 12px; text-align: center; cursor: pointer; transition: all .2s; }
.cap-card:hover { border-color: #1890ff; box-shadow: 0 2px 8px rgba(0, 0, 0, .09); }
.cap-icon { font-size: 24px; color: #1890ff; }
.cap-name { margin-top: 8px; font-size: 13px; font-weight: 600; }
.cap-path { margin-top: 2px; font-size: 12px; color: #bfbfbf; word-break: break-all; }

/* 套餐卡（只读） */
.pkg-card { height: 100%; border: 1px solid #f0f0f0; border-radius: 8px; padding: 12px; }
.pkg-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.pkg-name { font-size: 15px; font-weight: 600; }
.pkg-meta { display: flex; flex-direction: column; gap: 4px; color: #666; font-size: 13px; }
.pkg-desc { margin-top: 8px; color: #999; font-size: 12px; }

/* 未提供能力清单 */
.gap-list { margin: 0; padding-left: 20px; color: #595959; font-size: 13px; line-height: 1.9; }
</style>
