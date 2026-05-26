<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { NavBar, Cell, CellGroup, Avatar, Button, Dialog } from 'vant'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const menuItems = [
  { icon: 'balance-list-o', title: '我的订单', path: '/order/list' },
  { icon: 'location-o', title: '收货地址', path: '/user/address' },
  { icon: 'coupon-o', title: '优惠券', badge: '3' },
  { icon: 'star-o', title: '我的收藏', path: '/user/favorites' },
  { icon: 'clock-o', title: '浏览历史', path: '/user/history' },
  { icon: 'service-o', title: '客服中心' },
  { icon: 'setting-o', title: '设置', path: '/user/settings' }
]

const handleLogout = () => {
  Dialog.confirm({
    title: '提示',
    message: '确定要退出登录吗？'
  }).then(() => {
    userStore.logout()
    router.push('/login')
  }).catch(() => {})
}

onMounted(() => {
  if (!userStore.isLoggedIn) {
    userStore.init()
  }
})
</script>

<template>
  <div class="user-page">
    <div class="user-header">
      <div v-if="userStore.isLoggedIn" class="user-info">
        <Avatar 
          :src="userStore.user?.avatar" 
          size="60"
        >
          {{ userStore.user?.nickname?.charAt(0) || 'U' }}
        </Avatar>
        <div class="user-detail">
          <div class="user-name">{{ userStore.user?.nickname || '用户' }}</div>
          <div class="user-level">
            <span class="level-tag">{{ userStore.user?.level || '普通会员' }}</span>
          </div>
        </div>
        <Button 
          size="small" 
          plain 
          @click="router.push('/user/profile')"
        >
          编辑
        </Button>
      </div>
      
      <div v-else class="login-prompt">
        <Avatar size="60">U</Avatar>
        <div class="prompt-text">
          <div class="prompt-title">登录/注册</div>
          <div class="prompt-desc">登录后享受更多权益</div>
        </div>
        <Button 
          type="primary" 
          size="small"
          @click="router.push('/login')"
        >
          立即登录
        </Button>
      </div>
    </div>
    
    <div v-if="userStore.isLoggedIn" class="user-stats">
      <div class="stat-item">
        <div class="stat-value">{{ userStore.user?.points || 0 }}</div>
        <div class="stat-label">积分</div>
      </div>
      <div class="stat-item">
        <div class="stat-value">{{ userStore.user?.balance || 0 }}</div>
        <div class="stat-label">余额</div>
      </div>
      <div class="stat-item">
        <div class="stat-value">3</div>
        <div class="stat-label">优惠券</div>
      </div>
    </div>
    
    <CellGroup inset class="menu-group">
      <Cell 
        v-for="item in menuItems"
        :key="item.title"
        :icon="item.icon"
        :title="item.title"
        :badge="item.badge"
        is-link
        @click="item.path && router.push(item.path)"
      />
    </CellGroup>
    
    <div v-if="userStore.isLoggedIn" class="logout-section">
      <Button 
        block 
        type="danger" 
        plain
        @click="handleLogout"
      >
        退出登录
      </Button>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.user-page {
  min-height: 100vh;
  background: #f7f8fa;
  padding-bottom: 60px;
}

.user-header {
  padding: 20px 16px;
  background: linear-gradient(135deg, #1988fa, #4facfe);
  color: #fff;
  
  .user-info {
    display: flex;
    align-items: center;
    
    .user-detail {
      flex: 1;
      margin-left: 16px;
      
      .user-name {
        font-size: 18px;
        font-weight: 600;
      }
      
      .user-level {
        margin-top: 4px;
        
        .level-tag {
          font-size: 12px;
          padding: 2px 8px;
          background: rgba(255, 255, 255, 0.2);
          border-radius: 4px;
        }
      }
    }
  }
  
  .login-prompt {
    display: flex;
    align-items: center;
    
    .prompt-text {
      flex: 1;
      margin-left: 16px;
      
      .prompt-title {
        font-size: 18px;
        font-weight: 600;
      }
      
      .prompt-desc {
        font-size: 14px;
        margin-top: 4px;
        opacity: 0.8;
      }
    }
  }
}

.user-stats {
  display: flex;
  justify-content: space-around;
  padding: 16px;
  background: #fff;
  margin: -20px 16px 16px;
  border-radius: 8px;
  
  .stat-item {
    text-align: center;
    
    .stat-value {
      font-size: 20px;
      font-weight: 600;
      color: #333;
    }
    
    .stat-label {
      font-size: 12px;
      color: #969799;
      margin-top: 4px;
    }
  }
}

.menu-group {
  margin: 16px;
}

.logout-section {
  margin: 16px;
}
</style>