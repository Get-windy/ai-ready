<template>
  <div>
    <div class="section-title">
      证件信息
    </div>
    <a-row :gutter="24">
      <a-col
        v-for="(row, idx) in rows"
        :key="row._uid"
        :span="8"
      >
        <div class="cert-upload">
          <div
            class="cert-placeholder"
            @click="pickFile(row)"
          >
            <img
              v-if="row.url"
              :src="row.url"
              class="cert-preview"
            >
            <div
              v-else
              class="cert-empty"
            >
              <PictureOutlined style="font-size: 32px; color: #d9d9d9" />
              <span>点击上传图片</span>
            </div>
          </div>
          <input
            :ref="el => setInputRef(row, el)"
            type="file"
            accept="image/*"
            hidden
            @change="(e) => onFileChange(e, row)"
          >

          <!-- 第一个固定为营业执照；其余可下拉选择或自定义名称 -->
          <div
            v-if="idx === 0"
            class="cert-label"
          >
            营业执照
          </div>
          <template v-else>
            <a-select
              v-model:value="row.certType"
              size="small"
              class="cert-type-select"
              placeholder="请选择证照类型"
              :options="typeOptions"
              @change="() => ensureTrailingEmpty()"
            />
            <a-input
              v-if="row.certType === CUSTOM"
              v-model:value="row.customName"
              size="small"
              class="cert-type-select"
              placeholder="请输入证照名称"
              @blur="ensureTrailingEmpty"
            />
          </template>

          <a-button
            v-if="idx > 0 && !isEmptyRow(row)"
            type="link"
            size="small"
            danger
            @click="removeRow(row)"
          >
            删除
          </a-button>
        </div>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
/**
 * 证件信息（往来单位通用）
 *
 * - 第一个证照固定为「营业执照」
 * - 其余证照类型可下拉选择，或选「自定义」后输入名称（生产许可证 / 经营许可证 / 食品经营许可证 …）
 * - **始终保留一个空白待输入位**：任一空位被填写后自动追加新的空位
 * - 数据落 `erp_partner_attachment`（category='CERT'，file_name=证照名称，file_url=图片地址）
 *
 * 用法：<CertUploadList :partner-id="savedPartnerId || loadedPartnerId" />
 * 新增场景可先上传图片，待往来单位保存拿到 id 后组件会自动落库（watch partnerId）。
 */
import { ref, watch, onMounted, nextTick } from 'vue'
import { message } from 'ant-design-vue'
import { PictureOutlined } from '@ant-design/icons-vue'
import { partnerAttachmentApi } from '@/api/erp/partner'
import request from '@/utils/request'

const props = defineProps<{
  /** 往来单位 id；为空时为新增未保存态（图片先本地暂存，拿到 id 后自动落库） */
  partnerId?: number | string | null
}>()

const CUSTOM = '__custom__'
const FIXED_FIRST = '营业执照'

/** 常用证照类型（可选择，也可自定义） */
const typeOptions = [
  { value: '营业执照', label: '营业执照' },
  { value: '生产许可证', label: '生产许可证' },
  { value: '经营许可证', label: '经营许可证' },
  { value: '食品经营许可证', label: '食品经营许可证' },
  { value: '食品生产许可证', label: '食品生产许可证' },
  { value: '药品经营许可证', label: '药品经营许可证' },
  { value: '卫生许可证', label: '卫生许可证' },
  { value: '排污许可证', label: '排污许可证' },
  { value: '道路运输经营许可证', label: '道路运输经营许可证' },
  { value: CUSTOM, label: '自定义…' },
]

interface CertRow {
  _uid: number
  id?: number
  certType: string
  customName: string
  url: string
  fileType?: string
  fileSize?: number
}

let uid = 0
function emptyRow(): CertRow {
  return { _uid: ++uid, certType: '', customName: '', url: '' }
}

const rows = ref<CertRow[]>([])

/** 证照展示名（自定义时取输入的名称） */
function displayName(row: CertRow): string {
  if (row.certType === CUSTOM) return (row.customName || '').trim()
  return row.certType || ''
}

/** 空位判定：无图片、无名称、且不是已落库记录 */
function isEmptyRow(row: CertRow): boolean {
  return !row.url && !displayName(row) && !row.id
}

/** 始终保留一个空白待输入位 */
function ensureTrailingEmpty() {
  const last = rows.value[rows.value.length - 1]
  if (!last || !isEmptyRow(last)) {
    rows.value.push(emptyRow())
  }
}

/** 初始化：第一行固定营业执照 */
function initRows() {
  const first: CertRow = { ...emptyRow(), certType: FIXED_FIRST }
  rows.value = [first]
  ensureTrailingEmpty()
}

// ── 文件上传（支持新增未保存态：先上传拿 url，保存后落库） ──
const inputRefs = new Map<number, HTMLInputElement>()
function setInputRef(row: CertRow, el: any) {
  if (el) inputRefs.set(row._uid, el as HTMLInputElement)
}
function pickFile(row: CertRow) {
  inputRefs.get(row._uid)?.click()
}

async function onFileChange(e: Event, row: CertRow) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  try {
    const formData = new FormData()
    formData.append('file', file)
    const res: any = await request.post('/file/upload', formData)
    const url = typeof res === 'string' ? res : (res?.url || res?.data?.url)
    if (!url) {
      message.error('图片上传失败')
      return
    }
    row.url = url
    row.fileType = file.type
    row.fileSize = file.size
    ensureTrailingEmpty()
    await nextTick()
  } catch {
    message.error('图片上传失败')
  }
}

function removeRow(row: CertRow) {
  const idx = rows.value.findIndex(r => r._uid === row._uid)
  if (idx > 0) rows.value.splice(idx, 1)
  if (row.id) pendingDeleteIds.value.push(row.id)
  ensureTrailingEmpty()
}

/** 已删除但曾落库的附件 id（保存时统一删除） */
const pendingDeleteIds = ref<number[]>([])

// ── 加载 / 同步 ──
async function fetchRemoteCerts(pid: number | string) {
  const list: any = await partnerAttachmentApi.getByPartner(Number(pid))
  return ((list || []) as any[]).filter(a => a.category === 'CERT')
}

async function load(pid: number | string) {
  try {
    const certs = await fetchRemoteCerts(pid)
    const license = certs.find(c => c.fileName === FIXED_FIRST)
    const others = certs.filter(c => c.fileName !== FIXED_FIRST)
    const known = new Set(typeOptions.map(o => o.value))
    rows.value = [
      { ...emptyRow(), certType: FIXED_FIRST, id: license?.id, url: license?.fileUrl || '', fileType: license?.fileType },
      ...others.map(c => ({
        ...emptyRow(),
        id: c.id,
        certType: known.has(c.fileName) && c.fileName !== FIXED_FIRST ? c.fileName : CUSTOM,
        customName: known.has(c.fileName) && c.fileName !== FIXED_FIRST ? '' : (c.fileName || ''),
        url: c.fileUrl || '',
        fileType: c.fileType,
      })),
    ]
    ensureTrailingEmpty()
  } catch (e) {
    console.warn('[CertUploadList] 证件加载失败', e)
    initRows()
  }
}

/** 本地待落库的证件（已上传图片但还没拿到往来单位 id） */
function hasPendingLocal() {
  return rows.value.some(r => !isEmptyRow(r) && !!r.url && !r.id)
}

/** 并发保护：保存时「表单显式 sync」与「watch partnerId 触发 sync」会同时到达，
 *  不共用同一个 Promise 会各插一次（产生重复证件）。 */
let syncTask: Promise<void> | null = null

/** 将当前证件列表同步到往来单位（diff：新增 / 改名换图重建 / 删除） */
async function sync(pid: number | string) {
  if (syncTask) {
    await syncTask
    return
  }
  syncTask = doSync(pid)
  try {
    await syncTask
  } finally {
    syncTask = null
  }
}

async function doSync(pid: number | string) {
  const items = rows.value.filter(r => !isEmptyRow(r) && r.url)
  const remote = await fetchRemoteCerts(pid)
  const keepIds = new Set(items.filter(i => i.id).map(i => i.id as number))

  // 1) 删除远端多余（含本地已移除的）
  for (const rc of remote) {
    if (!keepIds.has(rc.id)) await partnerAttachmentApi.delete(rc.id)
  }
  // 2) 新增 / 变更重建
  for (const it of items) {
    const name = it.certType === FIXED_FIRST ? FIXED_FIRST : displayName(it)
    if (it.id) {
      const rc = remote.find(r => r.id === it.id)
      if (rc && (rc.fileName !== name || rc.fileUrl !== it.url)) {
        await partnerAttachmentApi.delete(it.id)
        await partnerAttachmentApi.create({ partnerId: Number(pid), fileName: name, fileUrl: it.url, fileType: it.fileType, fileSize: it.fileSize, category: 'CERT' } as any)
      }
    } else {
      await partnerAttachmentApi.create({ partnerId: Number(pid), fileName: name || '证件', fileUrl: it.url, fileType: it.fileType, fileSize: it.fileSize, category: 'CERT' } as any)
    }
  }
  pendingDeleteIds.value = []
  // 回填落库后的 id，保证重复调用 sync 幂等（不会重复插入）
  await load(pid)
}

onMounted(async () => {
  if (props.partnerId) await load(props.partnerId)
  else initRows()
})

// partnerId 可用时：
//   · 本地有待落库的新上传 → sync 落库（新增场景：保存后拿到 id）
//   · 否则 → load 回显（编辑场景：直接打开已有记录，绝不能 sync，否则会删掉已存证件）
// 切到另一单位时重新加载；重置为未保存态时清空。
watch(() => props.partnerId, async (val, old) => {
  if (val && !old) {
    try {
      if (hasPendingLocal()) await sync(val)
      else await load(val)
    } catch (e) { console.warn('[CertUploadList] 证件落库/加载失败', e) }
  } else if (val && old && String(val) !== String(old)) {
    await load(val)
  } else if (!val && old) {
    initRows()
  }
})

defineExpose({ sync, load, getRows: () => rows.value })
</script>

<style scoped>
.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #262626;
  margin-bottom: 16px;
  padding-bottom: 10px;
  border-bottom: 1px solid #f0f0f0;
}
.cert-upload {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}
.cert-placeholder {
  width: 120px;
  height: 90px;
  border: 1px dashed #d9d9d9;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  overflow: hidden;
  background: #fafafa;
}
.cert-placeholder:hover { border-color: #1890ff; }
.cert-preview { width: 100%; height: 100%; object-fit: cover; }
.cert-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  color: #bfbfbf;
  font-size: 12px;
}
.cert-label { font-size: 13px; color: #262626; }
.cert-type-select { width: 160px; }
</style>
