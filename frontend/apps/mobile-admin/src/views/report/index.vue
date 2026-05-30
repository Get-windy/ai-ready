<template>
  <div class="report-page">
    <van-nav-bar title="报表查看">
      <template #right>
        <van-icon name="search" size="18" @click="showSearch = true" />
      </template>
    </van-nav-bar>

    <van-dropdown-menu>
      <van-dropdown-item v-model="reportType" :options="typeOptions" title="报表类型" />
      <van-dropdown-item v-model="dateRange" :options="dateOptions" title="时间范围" />
    </van-dropdown-menu>

    <van-pull-refresh v-model="refreshing" @refresh="onRefresh">
      <div class="report-content">
        <van-cell-group inset title="常用报表">
          <van-cell
            v-for="report in commonReports"
            :key="report.id"
            :title="report.name"
            :label="report.description"
            is-link
            @click="viewReport(report)"
          >
            <template #icon>
              <van-icon :name="report.icon" class="report-icon" />
            </template>
            <template #value>
              <van-tag :type="report.type">{{ report.typeLabel }}</van-tag>
            </template>
          </van-cell>
        </van-cell-group>

        <van-cell-group inset title="财务报表">
          <van-cell
            v-for="report in financeReports"
            :key="report.id"
            :title="report.name"
            :label="report.updateTime"
            is-link
            @click="viewReport(report)"
          >
            <template #icon>
              <van-icon name="balance-list-o" class="report-icon" />
            </template>
          </van-cell>
        </van-cell-group>

        <van-cell-group inset title="销售报表">
          <van-cell
            v-for="report in salesReports"
            :key="report.id"
            :title="report.name"
            :label="report.updateTime"
            is-link
            @click="viewReport(report)"
          >
            <template #icon>
              <van-icon name="chart-trending-o" class="report-icon" />
            </template>
          </van-cell>
        </van-cell-group>

        <van-cell-group inset title="库存报表">
          <van-cell
            v-for="report in inventoryReports"
            :key="report.id"
            :title="report.name"
            :label="report.updateTime"
            is-link
            @click="viewReport(report)"
          >
            <template #icon>
              <van-icon name="goods-o" class="report-icon" />
            </template>
          </van-cell>
        </van-cell-group>

        <van-cell-group inset title="自定义报表">
          <van-cell
            v-for="report in customReports"
            :key="report.id"
            :title="report.name"
            :label="report.creator"
            is-link
            @click="viewReport(report)"
          >
            <template #icon>
              <van-icon name="edit" class="report-icon" />
            </template>
          </van-cell>
          <van-cell title="新建报表" is-link @click="createReport">
            <template #icon>
              <van-icon name="plus" class="report-icon add" />
            </template>
          </van-cell>
        </van-cell-group>
      </div>
    </van-pull-refresh>

    <van-search
      v-model:show="showSearch"
      v-model="searchText"
      placeholder="搜索报表名称"
      show-action
      @search="onSearch"
      @cancel="showSearch = false"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast, showDialog } from 'vant'

const router = useRouter()

const refreshing = ref(false)
const showSearch = ref(false)
const searchText = ref('')
const reportType = ref(0)
const dateRange = ref(0)

const typeOptions = [
  { text: '全部类型', value: 0 },
  { text: '财务报表', value: 1 },
  { text: '销售报表', value: 2 },
  { text: '库存报表', value: 3 }
]

const dateOptions = [
  { text: '本月', value: 0 },
  { text: '本季度', value: 1 },
  { text: '本年', value: 2 },
  { text: '自定义', value: 3 }
]

const commonReports = ref([
  { id: 1, name: '销售日报', description: '每日销售数据汇总', icon: 'calendar-o', type: 'primary', typeLabel: '每日' },
  { id: 2, name: '销售月报', description: '月度销售数据分析', icon: 'chart-trending-o', type: 'success', typeLabel: '每月' },
  { id: 3, name: '应收账款报表', description: '应收账款明细汇总', icon: 'balance-list-o', type: 'warning', typeLabel: '实时' }
])

const financeReports = ref([
  { id: 4, name: '资产负债表', updateTime: '2024-01-15' },
  { id: 5, name: '利润表', updateTime: '2024-01-15' },
  { id: 6, name: '现金流量表', updateTime: '2024-01-15' },
  { id: 7, name: '费用明细表', updateTime: '2024-01-14' }
])

const salesReports = ref([
  { id: 8, name: '销售业绩报表', updateTime: '2024-01-15' },
  { id: 9, name: '客户分析报表', updateTime: '2024-01-15' },
  { id: 10, name: '产品销售报表', updateTime: '2024-01-14' },
  { id: 11, name: '区域销售报表', updateTime: '2024-01-14' }
])

const inventoryReports = ref([
  { id: 12, name: '库存明细表', updateTime: '2024-01-15' },
  { id: 13, name: '库存预警报表', updateTime: '2024-01-15' },
  { id: 14, name: '出入库报表', updateTime: '2024-01-14' }
])

const customReports = ref([
  { id: 15, name: '自定义销售分析', creator: '张三' },
  { id: 16, name: '客户跟进报表', creator: '李四' }
])

onMounted(() => {
  loadReports()
})

const loadReports = () => {
  // 加载报表数据
}

const onRefresh = async () => {
  await loadReports()
  refreshing.value = false
}

const onSearch = () => {
  showSearch.value = false
  loadReports()
}

const viewReport = (report: any) => {
  router.push(`/report/${report.id}`)
}

const createReport = () => {
  showDialog({ title: '新建报表', message: '请输入报表名称和选择报表类型', closeOnPopstate: true }).then(() => showToast('报表创建成功'))
}
</script>

<style scoped lang="scss">
.report-page {
  min-height: 100vh;
  background: #f5f5f5;
}

.report-content {
  padding: 12px;
}

.report-icon {
  margin-right: 8px;
  color: #1989fa;

  &.add {
    color: #07c160;
  }
}
</style>