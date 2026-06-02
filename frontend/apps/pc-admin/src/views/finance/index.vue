<template>
  <div class="finance-dashboard">
    <!-- KPI 卡片 -->
    <a-row :gutter="[16, 16]">
      <a-col :span="6">
        <a-card hoverable>
          <a-statistic
            title="总资产"
            :value="dashboardData.totalAssets"
            :precision="2"
            prefix="¥"
            :value-style="{ color: '#1890ff' }"
          />
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card hoverable>
          <a-statistic
            title="总负债"
            :value="dashboardData.totalLiabilities"
            :precision="2"
            prefix="¥"
            :value-style="{ color: '#faad14' }"
          />
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card hoverable>
          <a-statistic
            title="本月收入"
            :value="dashboardData.monthlyIncome"
            :precision="2"
            prefix="¥"
            :value-style="{ color: '#52c41a' }"
          />
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card hoverable>
          <a-statistic
            title="本月费用"
            :value="dashboardData.monthlyExpense"
            :precision="2"
            prefix="¥"
            :value-style="{ color: '#ff4d4f' }"
          />
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card hoverable>
          <a-statistic
            title="净利润"
            :value="dashboardData.netProfit"
            :precision="2"
            prefix="¥"
            :value-style="{ color: dashboardData.netProfit >= 0 ? '#52c41a' : '#ff4d4f' }"
          />
        </a-card>
      </a-col>
    </a-row>

    <!-- 快速入口 -->
    <a-row :gutter="[16, 16]" class="mt-4">
      <a-col :span="6">
        <a-card hoverable class="quick-link-card" @click="navigateTo('subject')">
          <div class="quick-link-content">
            <FileTextOutlined class="quick-link-icon subject-icon" />
            <div class="quick-link-text">
              <div class="quick-link-title">科目管理</div>
              <div class="quick-link-desc">管理会计科目体系</div>
            </div>
          </div>
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card hoverable class="quick-link-card" @click="navigateTo('voucher')">
          <div class="quick-link-content">
            <FileTextOutlined class="quick-link-icon voucher-icon" />
            <div class="quick-link-text">
              <div class="quick-link-title">凭证管理</div>
              <div class="quick-link-desc">录入和审核会计凭证</div>
            </div>
          </div>
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card hoverable class="quick-link-card" @click="navigateTo('receivable')">
          <div class="quick-link-content">
            <DollarOutlined class="quick-link-icon receivable-icon" />
            <div class="quick-link-text">
              <div class="quick-link-title">应收账款</div>
              <div class="quick-link-desc">管理客户应收款项</div>
            </div>
          </div>
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card hoverable class="quick-link-card" @click="navigateTo('payable')">
          <div class="quick-link-content">
            <DollarOutlined class="quick-link-icon payable-icon" />
            <div class="quick-link-text">
              <div class="quick-link-title">应付账款</div>
              <div class="quick-link-desc">管理供应商应付款项</div>
            </div>
          </div>
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card hoverable class="quick-link-card" @click="navigateTo('report')">
          <div class="quick-link-content">
            <BarChartOutlined class="quick-link-icon report-icon" />
            <div class="quick-link-text">
              <div class="quick-link-title">财务报表</div>
              <div class="quick-link-desc">查看财务报表分析</div>
            </div>
          </div>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { FileTextOutlined, DollarOutlined, BarChartOutlined } from '@ant-design/icons-vue'
import { reportApi } from '@/api/finance'

const router = useRouter()

const dashboardData = ref({
  totalAssets: 0,
  totalLiabilities: 0,
  monthlyIncome: 0,
  monthlyExpense: 0,
  netProfit: 0
})

const navigateTo = (page: string) => {
  const routes: Record<string, string> = {
    subject: '/erp/finance/subject',
    voucher: '/erp/finance/voucher',
    receivable: '/erp/finance/receivable',
    payable: '/erp/finance/payable',
    report: '/erp/finance/report'
  }
  router.push(routes[page])
}

const loadDashboard = async () => {
  try {
    const res = await reportApi.getDashboard()
    if (res.data) {
      dashboardData.value = {
        totalAssets: res.data.totalAssets || 0,
        totalLiabilities: res.data.totalLiabilities || 0,
        monthlyIncome: res.data.monthlyIncome || 0,
        monthlyExpense: res.data.monthlyExpense || 0,
        netProfit: (res.data.monthlyIncome || 0) - (res.data.monthlyExpense || 0)
      }
    }
  } catch {
    // Silently handle - use default zeros
  }
}

onMounted(() => {
  loadDashboard()
})
</script>

<style scoped>
.finance-dashboard {
  padding: 16px;
}

.mt-4 {
  margin-top: 16px;
}

.quick-link-card {
  cursor: pointer;
  transition: all 0.3s;
}

.quick-link-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.quick-link-content {
  display: flex;
  align-items: center;
  gap: 12px;
}

.quick-link-icon {
  font-size: 32px;
}

.subject-icon {
  color: #1890ff;
}

.voucher-icon {
  color: #722ed1;
}

.receivable-icon {
  color: #52c41a;
}

.payable-icon {
  color: #fa8c16;
}

.report-icon {
  color: #eb2f96;
}

.quick-link-text {
  flex: 1;
}

.quick-link-title {
  font-size: 15px;
  font-weight: 500;
  color: #303133;
}

.quick-link-desc {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}
</style>
