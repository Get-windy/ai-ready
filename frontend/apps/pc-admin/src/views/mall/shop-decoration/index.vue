<template>
  <ErrorBoundary>
    <PageContainer title="商城装修">
      <a-alert
        type="warning"
        show-icon
        message="可视化拖拽装修后端待补全"
        description="当前页面已对接真实的「页面模板选择」与「轮播图管理」接口；拖拽式页面设计师需要后端提供装修配置存储端点（如 /api/erp/mall/admin/decoration）后开放。"
        style="margin-bottom: 16px"
      />

      <!-- ═══ 页面模板（真实接口） ═══ -->
      <a-card
        title="页面模板"
        :bordered="false"
        class="section-card"
      >
        <a-spin :spinning="templateLoading">
          <a-empty
            v-if="!templates.length"
            description="暂无可用模板"
          />
          <div
            v-else
            class="template-list"
          >
            <div
              v-for="t in templates"
              :key="t.id"
              class="template-item"
              :class="{ 'template-item--active': selectedTemplateId === t.id }"
              @click="selectedTemplateId = t.id"
            >
              <div class="template-thumb">
                <img
                  v-if="t.thumbnail"
                  :src="t.thumbnail"
                  :alt="t.templateName"
                >
                <span v-else>{{ t.templateName }}</span>
              </div>
              <div class="template-name">
                {{ t.templateName }}
                <a-tag
                  v-if="t.isDefault === 1"
                  color="blue"
                >
                  默认
                </a-tag>
              </div>
              <div
                v-if="t.description"
                class="template-desc"
              >
                {{ t.description }}
              </div>
            </div>
          </div>
          <div
            v-if="templates.length"
            style="margin-top: 16px"
          >
            <a-button
              type="primary"
              :loading="templateSaving"
              @click="saveTemplate"
            >
              应用所选模板
            </a-button>
          </div>
        </a-spin>
      </a-card>

      <!-- ═══ 轮播图管理（真实接口 CRUD） ═══ -->
      <a-card
        title="轮播图管理"
        :bordered="false"
        class="section-card"
      >
        <template #extra>
          <a-button
            type="primary"
            size="small"
            @click="openCreate"
          >
            <template #icon>
              <PlusOutlined />
            </template>新增轮播图
          </a-button>
        </template>
        <a-table
          :data-source="banners"
          :columns="bannerColumns"
          :loading="bannerLoading"
          :pagination="false"
          size="small"
          row-key="id"
          :locale="{ emptyText: '暂无轮播图' }"
        >
          <template #bodyCell="{ column, text, record }">
            <template v-if="column.dataIndex === 'imageUrl'">
              <a-image
                v-if="text"
                :src="text"
                :width="80"
                :height="36"
                style="object-fit: cover; border-radius: 4px"
              />
              <span v-else>-</span>
            </template>
            <template v-else-if="column.dataIndex === 'linkType'">
              {{ LINK_TYPE_MAP[text]?.label || text || '-' }}
            </template>
            <template v-else-if="column.dataIndex === 'status'">
              <a-tag :color="text === 1 ? 'green' : 'default'">
                {{ text === 1 ? '启用' : '停用' }}
              </a-tag>
            </template>
            <template v-else-if="column.dataIndex === 'action'">
              <a-space>
                <a-button
                  type="link"
                  size="small"
                  @click="openEdit(record as ShopBanner)"
                >
                  编辑
                </a-button>
                <a-button
                  type="link"
                  size="small"
                  :style="record.status === 1 ? 'color: #fa8c16' : 'color: #52c41a'"
                  @click="toggleStatus(record as ShopBanner)"
                >
                  {{ record.status === 1 ? '停用' : '启用' }}
                </a-button>
                <a-popconfirm
                  title="确认删除该轮播图？"
                  ok-text="删除"
                  cancel-text="取消"
                  @confirm="handleDelete(record as ShopBanner)"
                >
                  <a-button
                    type="link"
                    size="small"
                    danger
                  >
                    删除
                  </a-button>
                </a-popconfirm>
              </a-space>
            </template>
          </template>
        </a-table>
      </a-card>

      <!-- 新增/编辑轮播图弹窗 -->
      <a-modal
        v-model:open="modalOpen"
        :title="editingId ? '编辑轮播图' : '新增轮播图'"
        :confirm-loading="saving"
        width="560px"
        @ok="handleSave"
      >
        <a-form
          ref="formRef"
          :model="form"
          :rules="rules"
          :label-col="{ span: 6 }"
          :wrapper-col="{ span: 16 }"
        >
          <a-form-item
            label="标题"
            name="title"
          >
            <a-input
              v-model:value="form.title"
              placeholder="轮播图标题"
            />
          </a-form-item>
          <a-form-item
            label="图片URL"
            name="imageUrl"
          >
            <a-input
              v-model:value="form.imageUrl"
              placeholder="轮播图图片地址"
            />
          </a-form-item>
          <a-form-item
            label="跳转类型"
            name="linkType"
          >
            <a-select
              v-model:value="form.linkType"
              :options="linkTypeOptions"
              placeholder="请选择跳转类型"
            />
          </a-form-item>
          <a-form-item
            v-if="form.linkType && form.linkType !== 'none'"
            label="跳转目标"
            name="linkValue"
          >
            <a-input
              v-model:value="form.linkValue"
              :placeholder="form.linkType === 'product' ? '商品ID' : '分类ID'"
            />
          </a-form-item>
          <a-form-item
            label="外部链接"
            name="linkUrl"
          >
            <a-input
              v-model:value="form.linkUrl"
              placeholder="可选，自定义跳转URL"
            />
          </a-form-item>
          <a-form-item
            label="排序"
            name="sortOrder"
          >
            <a-input-number
              v-model:value="form.sortOrder"
              :min="0"
              :precision="0"
              style="width: 100%"
            />
          </a-form-item>
          <a-form-item
            label="状态"
            name="status"
          >
            <a-radio-group v-model:value="form.status">
              <a-radio :value="1">
                启用
              </a-radio>
              <a-radio :value="0">
                停用
              </a-radio>
            </a-radio-group>
          </a-form-item>
        </a-form>
      </a-modal>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import type { Rule } from 'ant-design-vue/es/form'
import { PlusOutlined } from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import {
  shopBannerApi, shopConfigApi, shopTemplateApi,
  type ShopBanner, type ShopConfig, type ShopTemplate
} from '@/api/erp/mall'

defineOptions({ name: 'MallShopDecoration' })

// ═══ 跳转类型（与后端 ShopBanner.linkType 注释一致：product/category/none） ═══
const LINK_TYPE_MAP: Record<string, { label: string }> = {
  product: { label: '商品详情' },
  category: { label: '商品分类' },
  none: { label: '无跳转' }
}
const linkTypeOptions = Object.entries(LINK_TYPE_MAP).map(([value, v]) => ({ label: v.label, value }))

// ═══ 页面模板 ═══
const templates = ref<ShopTemplate[]>([])
const templateLoading = ref(false)
const templateSaving = ref(false)
const selectedTemplateId = ref<number | undefined>(undefined)
let fullConfig: ShopConfig | null = null

async function loadTemplates() {
  templateLoading.value = true
  try {
    const [tplRes, cfgRes]: any[] = await Promise.all([shopTemplateApi.list(), shopConfigApi.get()])
    templates.value = Array.isArray(tplRes) ? tplRes : (tplRes?.data ?? [])
    const cfg: ShopConfig | null = cfgRes?.data ?? cfgRes ?? null
    fullConfig = cfg
    selectedTemplateId.value = cfg?.templateId
  } catch (e) {
    console.warn('[商城装修] 模板/配置获取失败', e)
  } finally {
    templateLoading.value = false
  }
}

async function saveTemplate() {
  if (!selectedTemplateId.value) {
    message.warning('请选择要应用的模板')
    return
  }
  templateSaving.value = true
  try {
    await shopConfigApi.update({
      ...(fullConfig || {}),
      shopName: fullConfig?.shopName || '订货商城',
      templateId: selectedTemplateId.value
    })
    message.success('页面模板已应用')
  } catch (e) {
    console.warn('[商城装修] 模板保存失败', e)
  } finally {
    templateSaving.value = false
  }
}

// ═══ 轮播图 ═══
const banners = ref<ShopBanner[]>([])
const bannerLoading = ref(false)

const bannerColumns: any[] = [
  { title: '图片', dataIndex: 'imageUrl', key: 'imageUrl', width: 100 },
  { title: '标题', dataIndex: 'title', key: 'title', width: 150, ellipsis: true },
  { title: '跳转类型', dataIndex: 'linkType', key: 'linkType', width: 100 },
  { title: '跳转目标', dataIndex: 'linkValue', key: 'linkValue', width: 110 },
  { title: '排序', dataIndex: 'sortOrder', key: 'sortOrder', width: 70, align: 'right' },
  { title: '状态', dataIndex: 'status', key: 'status', width: 80 },
  { title: '操作', dataIndex: 'action', key: 'action', width: 160, fixed: 'right' }
]

async function loadBanners() {
  bannerLoading.value = true
  try {
    const res: any = await shopBannerApi.list()
    banners.value = Array.isArray(res) ? res : (res?.data ?? [])
  } catch (e) {
    console.warn('[商城装修] 轮播图获取失败', e)
  } finally {
    bannerLoading.value = false
  }
}

const formRef = ref()
const modalOpen = ref(false)
const saving = ref(false)
const editingId = ref<number | null>(null)

const emptyForm = () => ({
  title: '',
  imageUrl: '',
  linkType: 'none' as string,
  linkValue: '',
  linkUrl: '',
  sortOrder: 0 as number,
  status: 1 as number
})
const form = reactive(emptyForm())

const rules: Record<string, Rule[]> = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  imageUrl: [{ required: true, message: '请输入图片URL', trigger: 'blur' }]
}

function openCreate() {
  editingId.value = null
  Object.assign(form, emptyForm())
  modalOpen.value = true
}

function openEdit(record: ShopBanner) {
  editingId.value = record.id ?? null
  Object.assign(form, emptyForm(), {
    title: record.title || '',
    imageUrl: record.imageUrl,
    linkType: record.linkType || 'none',
    linkValue: record.linkValue || '',
    linkUrl: record.linkUrl || '',
    sortOrder: record.sortOrder ?? 0,
    status: record.status ?? 1
  })
  modalOpen.value = true
}

async function handleSave() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  saving.value = true
  try {
    if (editingId.value) {
      await shopBannerApi.update(editingId.value, { ...form } as ShopBanner)
      message.success('轮播图已更新')
    } else {
      await shopBannerApi.create({ ...form } as ShopBanner)
      message.success('轮播图已创建')
    }
    modalOpen.value = false
    loadBanners()
  } catch (e) {
    console.warn('[商城装修] 轮播图保存失败', e)
  } finally {
    saving.value = false
  }
}

async function toggleStatus(record: ShopBanner) {
  const target = record.status === 1 ? 0 : 1
  try {
    await shopBannerApi.update(record.id!, { ...record, status: target })
    message.success(target === 1 ? '轮播图已启用' : '轮播图已停用')
    loadBanners()
  } catch (e) {
    console.warn('[商城装修] 状态切换失败', e)
  }
}

async function handleDelete(record: ShopBanner) {
  try {
    await shopBannerApi.delete(record.id!)
    message.success('轮播图已删除')
    loadBanners()
  } catch (e) {
    console.warn('[商城装修] 删除失败', e)
  }
}

onMounted(() => {
  loadTemplates()
  loadBanners()
})
</script>

<style scoped>
.section-card {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  margin-bottom: 16px;
}
.template-list {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
}
.template-item {
  width: 180px;
  padding: 8px;
  border: 2px solid #f0f0f0;
  border-radius: 8px;
  cursor: pointer;
  transition: border-color 0.2s;
}
.template-item--active {
  border-color: #1890ff;
}
.template-thumb {
  height: 100px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fafafa;
  border-radius: 4px;
  overflow: hidden;
  color: #909399;
}
.template-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.template-name {
  margin-top: 8px;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 6px;
}
.template-desc {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}
</style>
