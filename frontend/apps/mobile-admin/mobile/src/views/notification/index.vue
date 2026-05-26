<template>
  <div class="notification-page">
    <van-nav-bar
      title="通知中心"
      left-arrow
      @click-left="goBack"
    >
      <template #right>
        <van-icon name="delete-o" size="18" @click="clearAll" />
      </template>
    </van-nav-bar>

    <van-tabs v-model:active="activeTab" sticky @change="onTabChange">
      <van-tab title="全部" :badge="totalCount">
        <notification-list :notifications="allNotifications" @click="goDetail" @delete="deleteNotification" />
      </van-tab>
      <van-tab title="未读" :badge="unreadCount">
        <notification-list :notifications="unreadNotifications" @click="goDetail" @delete="deleteNotification" />
      </van-tab>
      <van-tab title="系统">
        <notification-list :notifications="systemNotifications" @click="goDetail" @delete="deleteNotification" />
      </van-tab>
      <van-tab title="审批">
        <notification-list :notifications="approvalNotifications" @click="goDetail" @delete="deleteNotification" />
      </van-tab>
      <van-tab title="订单">
        <notification-list :notifications="orderNotifications" @click="goDetail" @delete="deleteNotification" />
      </van-tab>
    </van-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showConfirmDialog, showSuccessToast } from 'vant'

const router = useRouter()

const activeTab = ref(0)
const notifications = ref<any[]>([])

const totalCount = computed(() => notifications.value.length)
const unreadCount = computed(() => notifications.value.filter(n => !n.read).length)

const allNotifications = computed(() => notifications.value)
const unreadNotifications = computed(() => notifications.value.filter(n => !n.read))
const systemNotifications = computed(() => notifications.value.filter(n => n.type === 'system'))
const approvalNotifications = computed(() => notifications.value.filter(n => n.type === 'approval'))
const orderNotifications = computed(() => notifications.value.filter(n => n.type === 'order'))

onMounted(() => {
  loadNotifications()
})

const loadNotifications = () => {
  notifications.value = [
    { id: 1, title: '审批提醒', content: '您有一个新的采购订单审批待处理', type: 'approval', read: false, createTime: '2024-01-15 10:30', icon: 'todo-list-o' },
    { id: 2, title: '订单通知', content: '订单SO20240115001已发货', type: 'order', read: false, createTime: '2024-01-15 09:20', icon: 'logistics' },
    { id: 3, title: '系统通知', content: '系统将于今晚22:00进行维护升级', type: 'system', read: true, createTime: '2024-01-14 18:00', icon: 'info-o' },
    { id: 4, title: '审批提醒', content: '费用报销审批已通过', type: 'approval', read: true, createTime: '2024-01-14 15:30', icon: 'todo-list-o' },
    { id: 5, title: '订单通知', content: '客户北京科技有限公司下单成功', type: 'order', read: false, createTime: '2024-01-14 14:20', icon: 'orders-o' },
    { id: 6, title: '系统通知', content: '您的密码已成功修改', type: 'system', read: true, createTime: '2024-01-13 10:00', icon: 'lock' }
  ]
}

const goBack = () => router.back()

const onTabChange = () => {
  // Tab切换
}

const goDetail = (notification: any) => {
  notification.read = true
  switch (notification.type) {
    case 'approval':
      router.push('/approval')
      break
    case 'order':
      router.push('/order')
      break
    case 'system':
      showToast(notification.content)
      break
  }
}

const deleteNotification = (notification: any) => {
  notifications.value = notifications.value.filter(n => n.id !== notification.id)
}

const clearAll = async () => {
  try {
    await showConfirmDialog({
      title: '清空通知',
      message: '确定要清空所有通知吗？'
    })
    notifications.value = []
    showSuccessToast('已清空')
  } catch {
    // 用户取消
  }
}
</script>

<script lang="ts">
import { defineComponent, h } from 'vue'
import { Cell, CellGroup, Tag, Icon, SwipeCell, Button } from 'vant'

const NotificationList = defineComponent({
  name: 'NotificationList',
  props: {
    notifications: {
      type: Array,
      default: () => []
    }
  },
  emits: ['click', 'delete'],
  setup(props, { emit }) {
    return () => h('div', { class: 'notification-list' }, [
      h(CellGroup, { inset: true }, () =>
        props.notifications.length === 0
          ? h('van-empty', { description: '暂无通知' })
          : props.notifications.map((notification: any) =>
            h(SwipeCell, { key: notification.id }, {
              default: () => h(Cell, {
                title: notification.title,
                label: notification.content,
                isLink: true,
                onClick: () => emit('click', notification)
              }, {
                icon: () => h('div', { class: 'notification-icon' }, [
                  h(Icon, { name: notification.icon, size: '20' })
                ]),
                title: () => h('div', { class: 'notification-title' }, [
                  h('span', notification.title),
                  !notification.read ? h(Tag, { type: 'danger', size: 'small' }, () => '未读') : null
                ]),
                value: () => h('div', { class: 'notification-time' }, notification.createTime)
              }),
              right: () => h(Button, {
                square: true,
                type: 'danger',
                text: '删除',
                class: 'swipe-btn',
                onClick: () => emit('delete', notification)
              })
            })
          )
      )
    ])
  }
})

export default {
  components: { NotificationList }
}
</script>

<style scoped lang="scss">
.notification-page {
  min-height: 100vh;
  background: #f5f5f5;
}

.notification-list {
  padding-top: 12px;
}

:deep(.notification-icon) {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  background: #e8f4ff;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 12px;
  color: #1989fa;
}

:deep(.notification-title) {
  display: flex;
  align-items: center;
  gap: 8px;
}

:deep(.notification-time) {
  font-size: 12px;
  color: #999;
}

:deep(.swipe-btn) {
  height: 100%;
}
</style>