<script setup lang="ts">
/**
 * 消息（底部第 3 个主 Tab）——《商城App设计方案》§4.3。
 * 顶部二级 Tab：**客服（默认）** / 消息。
 *
 * 本页是商城 App 的**特色页**：客服侧计划对接 AI + 企业微信客服。
 * 但后端的 C 端会话接口（`F-09`）与买家消息接口（`F-10`）**都还没做**，
 * 所以这里**只搭骨架、不放假数据**：能用的先做（FAQ 快捷问跳搜索/跳订单），
 * 没接口的显示明确的"待接入"，不伪造会话记录。
 */
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { useUserStore } from '@/stores/user'
import { useMessageStore } from '@/stores/message'
import TopTabs, { type TopTabItem } from '@/components/layout/TopTabs.vue'

const router = useRouter()
const userStore = useUserStore()
const messageStore = useMessageStore()

const topTabs: TopTabItem[] = [
  { key: 'service', name: '客服' },
  { key: 'notice', name: '消息' }
]
const activeTab = ref('service')
/** 客服输入框（一期只做引导，不发请求） */
const askText = ref('')

/** FAQ 快捷问：一期先做成"跳对应页面"，不需要后端 */
const faqs = [
  { q: '我的订单到哪了？', action: () => requireLoginThen('/order/list') },
  { q: '怎么改收货地址？', action: () => requireLoginThen('/user/address') },
  { q: '如何申请退货/换货？', action: () => showToast('售后入口建设中（F-14）') },
  { q: '发票怎么开？', action: () => showToast('发票功能建设中（F-16）') },
  { q: '账期与额度怎么算？', action: () => showToast('请咨询业务员') }
]

function requireLoginThen(path: string) {
  if (!userStore.isLoggedIn) {
    router.push({ path: '/login', query: { redirect: path } })
    return
  }
  router.push(path)
}

function onAsk() {
  // ⚠️ F-09 未落地：`POST /v1/mall/chat/ask` 尚不存在，这里**不伪造回答**
  showToast('AI 客服接入中，请先用下方快捷问题')
}

function onTransferHuman() {
  // 二期：企业微信客服（wx.openCustomerServiceChat / 扫码）
  showToast('人工客服接入中（二期）')
}
</script>

<template>
  <div class="message-page">
    <TopTabs v-model="activeTab" :tabs="topTabs" />

    <!-- ═══ 客服 ═══ -->
    <div v-show="activeTab === 'service'" class="service">
      <div class="service-hero">
        <van-icon name="service-o" size="64" color="#fff" />
        <div class="service-hero__title">企智连商城客服</div>
        <div class="service-hero__sub">AI 智能问答 + 人工客服（建设中）</div>
      </div>

      <div class="service-ask">
        <van-field
          v-model="askText"
          placeholder="描述你的问题…"
          readonly
          is-link
          @click="onAsk"
        />
        <van-button type="primary" block round @click="onAsk">发送</van-button>
      </div>

      <div class="section">
        <div class="section-title">常见问题</div>
        <van-cell
          v-for="item in faqs"
          :key="item.q"
          :title="item.q"
          is-link
          @click="item.action"
        />
      </div>

      <div class="section">
        <van-cell title="转人工客服" is-link @click="onTransferHuman">
          <template #icon><van-icon name="friends-o" class="cell-icon" /></template>
        </van-cell>
      </div>

      <div class="tip">
        AI 客服接口（设计文档 F-09）与企业微信客服为二期内容；
        当前不提供自动应答，以免给出与真实库存/价格不符的回答。
      </div>
    </div>

    <!-- ═══ 消息 ═══ -->
    <div v-show="activeTab === 'notice'" class="notice">
      <van-cell
        v-if="userStore.isLoggedIn && messageStore.messages.length"
        title="全部已读"
        is-link
        @click="messageStore.markAllRead()"
      />
      <van-empty v-if="!userStore.isLoggedIn" description="登录后查看消息">
        <van-button type="primary" size="small" round @click="router.push('/login')">去登录</van-button>
      </van-empty>
      <van-empty v-else description="暂无消息">
        <div class="empty-tip">
          订单状态、审核结果、发货通知会出现在这里。<br />
          （买家消息接口建设中，见设计文档 F-10）
        </div>
      </van-empty>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.message-page {
  min-height: 100vh;
  background: #f7f8fa;
}

.service-hero {
  background: linear-gradient(135deg, var(--mall-primary, #1988fa), #4facfe);
  padding: 24px 16px;
  display: flex;
  flex-direction: column;
  align-items: center;
  color: #fff;

  &__title { margin-top: 8px; font-size: 17px; font-weight: 600; }
  &__sub { margin-top: 4px; font-size: 12px; opacity: 0.85; }
}

.service-ask {
  margin: -16px 12px 0;
  background: #fff;
  border-radius: 8px;
  padding: 8px 0 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);

  .van-button { width: calc(100% - 32px); margin: 8px 16px 0; }
}

.section {
  margin-top: 12px;
  background: #fff;

  .section-title {
    padding: 12px 16px 4px;
    font-size: 14px;
    color: #323233;
    font-weight: 600;
  }
}

.cell-icon { margin-right: 8px; color: var(--mall-primary, #1988fa); }

.tip {
  margin: 12px 16px;
  padding: 10px 12px;
  background: #fffbe8;
  color: #ed6a0c;
  font-size: 12px;
  line-height: 1.6;
  border-radius: 6px;
}

.empty-tip {
  margin-top: 8px;
  font-size: 12px;
  color: #969799;
  line-height: 1.7;
}
</style>
