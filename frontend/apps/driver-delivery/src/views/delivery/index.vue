<script setup lang="ts">
/**
 * 司机端「配送任务」列表 —— 真实接入 DMS 配送单（`/api/dms/task`）。
 *
 * 2026-09-13：原页面为早期原型（`api.delivery.getTasks()` → `/api/v1/delivery/deliveries`，
 * 该端点不存在，页面永远走本地假数据），本次改为：
 *   1) 按当前登录账号反查配送员档案（`/dms/verification/me/rider`）；
 *   2) 拉取本人未完结任务（`/dms/task/page?riderId=..&statusList=1,2,3,4`）；
 *   3) 状态机与后端 `TaskStatusEnum` 一致：1 已分配→2 已接单→3 取货中→4 配送中→签收。
 */
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { NavBar, Card, Button, Tag, Empty, PullRefresh, showLoadingToast, closeToast, showToast, showConfirmDialog } from 'vant'
import { dmsApi } from '@/api/dms'

const router = useRouter()

const tasks = ref<any[]>([])
const riderId = ref<string>('')
const riderName = ref('')
const loading = ref(false)

const STATUS_MAP: Record<number, { label: string; color: string }> = {
  1: { label: '已分配', color: '#969799' },
  2: { label: '已接单', color: '#1988fa' },
  3: { label: '取货中', color: '#ff976a' },
  4: { label: '配送中', color: '#07c160' },
}

const statusOf = (status: number) => STATUS_MAP[status] || { label: '待处理', color: '#969799' }

/** 下一步动作（与后端 TaskStatusEnum.canTransitionTo 对齐：1→2→3→4） */
const nextAction = (status: number): { label: string; to: number } | null => {
  if (status === 1) return { label: '接单', to: 2 }
  if (status === 2) return { label: '开始取货', to: 3 }
  if (status === 3) return { label: '开始配送', to: 4 }
  return null
}

const pick = (res: any) => res?.data ?? res ?? {}

const loadTasks = async () => {
  loading.value = true
  showLoadingToast({ message: '加载中...', forbidClick: true })
  try {
    if (!riderId.value) {
      const me = pick(await dmsApi.getMyRider())
      riderId.value = me?.id != null ? String(me.id) : ''
      riderName.value = me?.realName || ''
    }
    if (!riderId.value) {
      tasks.value = []
      showToast('当前账号未关联配送员档案')
      return
    }
    const res: any = await dmsApi.getMyTasks(riderId.value, {
      statusList: '1,2,3,4',
      current: 1,
      size: 50,
    })
    tasks.value = pick(res)?.records || []
  } catch (e: any) {
    tasks.value = []
    showToast(e?.message || '加载配送任务失败')
  } finally {
    loading.value = false
    closeToast()
  }
}

onMounted(loadTasks)

const advance = async (task: any) => {
  const action = nextAction(task.status)
  if (!action) return
  try {
    await dmsApi.updateTaskStatus(task.id, task.status, action.to)
    showToast({ type: 'success', message: `${action.label}成功` })
    loadTasks()
  } catch (e: any) {
    showToast(e?.message || '状态更新失败')
  }
}

const goSign = (task: any) => router.push(`/delivery/${task.id}/sign`)
const goDetail = (task: any) => router.push(`/delivery/${task.id}`)
const goNavigate = (task: any) => router.push(`/map/navigation/${task.id}`)

const callCustomer = (task: any) => {
  if (!task.customerPhone) return
  showConfirmDialog({ title: '联系客户', message: `拨打 ${task.customerPhone}？` })
    .then(() => { window.location.href = `tel:${task.customerPhone}` })
    .catch(() => { /* 取消 */ })
}

const money = (v: any) => (v == null || v === '' ? '0.00' : Number(v).toFixed(2))
const qty = (v: any) => (v == null || v === '' ? '0' : String(Number(v)))
</script>

<template>
  <div class="delivery-page">
    <NavBar :title="riderName ? `配送任务 · ${riderName}` : '配送任务'" />

    <PullRefresh @refresh="loadTasks">
      <div class="delivery-list">
        <Empty v-if="tasks.length === 0 && !loading" description="暂无待处理的配送任务" />

        <Card v-for="task in tasks" :key="task.id" class="delivery-card">
          <template #title>
            <div class="task-header">
              <span class="task-no">{{ task.taskNo }}</span>
              <Tag :color="statusOf(task.status).color">{{ statusOf(task.status).label }}</Tag>
            </div>
          </template>

          <template #desc>
            <div class="task-info">
              <div class="info-row">
                <span class="label">客户</span>
                <span class="value">{{ task.customerName || '-' }} {{ task.customerPhone || '' }}</span>
              </div>
              <div class="info-row">
                <span class="label">地址</span>
                <span class="value address">{{ task.customerAddress || '-' }}</span>
              </div>
              <div class="info-row">
                <span class="label">数量</span>
                <span class="value">{{ qty(task.totalQuantity) }}</span>
              </div>
              <div class="info-row">
                <span class="label">代收</span>
                <span class="value amount">¥{{ money(task.collectOnDelivery) }}</span>
              </div>
              <div class="info-row">
                <span class="label">配送费</span>
                <span class="value">¥{{ money(task.deliveryFee) }}</span>
              </div>
              <div class="info-row" v-if="task.deliveryDate">
                <span class="label">配送日期</span>
                <span class="value">{{ task.deliveryDate }}</span>
              </div>
              <div class="info-row" v-if="task.remark">
                <span class="label">备注</span>
                <span class="value remark">{{ task.remark }}</span>
              </div>
            </div>
          </template>

          <template #footer>
            <div class="task-actions">
              <Button
                v-if="nextAction(task.status)"
                type="primary"
                size="small"
                @click="advance(task)"
              >
                {{ nextAction(task.status)!.label }}
              </Button>
              <Button
                v-if="task.status === 4"
                type="success"
                size="small"
                @click="goSign(task)"
              >
                签收
              </Button>
              <Button size="small" @click="goNavigate(task)">导航</Button>
              <Button size="small" @click="callCustomer(task)">联系</Button>
              <Button size="small" @click="goDetail(task)">详情</Button>
            </div>
          </template>
        </Card>
      </div>
    </PullRefresh>
  </div>
</template>

<style lang="scss" scoped>
.delivery-page {
  min-height: 100vh;
  background: #f7f8fa;
  padding-bottom: 60px;
}

.delivery-list {
  padding: 12px;
}

.delivery-card {
  margin-bottom: 12px;

  .task-header {
    display: flex;
    align-items: center;
    gap: 8px;

    .task-no {
      font-size: 16px;
      font-weight: 600;
    }
  }

  .task-info {
    .info-row {
      display: flex;
      margin-top: 8px;

      .label {
        width: 60px;
        flex: none;
        color: #969799;
      }

      .value {
        color: #333;

        &.address {
          flex: 1;
          word-break: break-all;
        }

        &.amount {
          color: #f44;
          font-weight: 600;
        }

        &.remark {
          color: #969799;
        }
      }
    }
  }

  .task-actions {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    justify-content: flex-end;
  }
}
</style>
