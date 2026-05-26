# 反馈组件使用指南

## 概述

本指南介绍如何在AI-Ready项目中使用新开发的反馈组件，以解决P0用户体验问题。

## 组件列表

### 1. ARSkeleton - 骨架屏组件
**用途**: 数据加载时的占位显示

**基本用法**:
```vue
<template>
  <!-- 基础骨架屏 -->
  <ARSkeleton active />
  
  <!-- 带标题和段落的骨架屏 -->
  <ARSkeleton 
    :title="true"
    :paragraph="{ rows: 4 }"
    :active="loading"
  />
  
  <!-- 卡片骨架屏 -->
  <ARSkeleton 
    type="card"
    :avatar="true"
    :title="true"
    :paragraph="{ rows: 3 }"
  />
</template>

<script setup>
import { ARSkeleton } from '@/components/@ai-ready/common/components/feedback'
</script>
```

**属性**:
| 属性 | 说明 | 类型 | 默认值 |
|------|------|------|--------|
| active | 是否显示骨架屏 | boolean | true |
| title | 是否显示标题 | boolean | false |
| titleWidth | 标题宽度 | string/number | '40%' |
| paragraph | 是否显示段落 | boolean | true |
| paragraphRows | 段落行数 | number | 4 |
| paragraphWidth | 段落宽度 | string/number/array | ['100%', '100%', '80%', '60%'] |
| avatar | 是否显示头像 | boolean | false |
| avatarSize | 头像大小 | string/number | 40 |
| avatarShape | 头像形状 | 'circle'/'square' | 'circle' |
| type | 骨架屏类型 | 'text'/'card'/'list'/'table' | 'text' |
| animated | 是否显示动画 | boolean | true |

### 2. ARLoading - 加载指示器
**用途**: 显示加载状态

**基本用法**:
```vue
<template>
  <!-- 旋转加载器 -->
  <ARLoading :type="'spinner'" :text="'加载中...'" />
  
  <!-- 进度条加载器 -->
  <ARLoading 
    :type="'progress'"
    :progress="progressValue"
    :show-percent="true"
  />
  
  <!-- 全屏加载 -->
  <ARLoading 
    :fullscreen="true"
    :overlay="true"
    :text="'正在处理数据...'"
  />
</template>

<script setup>
import { ARLoading } from '@/components/@ai-ready/common/components/feedback'
</script>
```

**属性**:
| 属性 | 说明 | 类型 | 默认值 |
|------|------|------|--------|
| type | 加载器类型 | 'spinner'/'progress'/'dots'/'skeleton' | 'spinner' |
| size | 加载器大小 | 'small'/'default'/'large'/number | 'default' |
| text | 加载文本 | string | '' |
| fullscreen | 是否全屏显示 | boolean | false |
| overlay | 是否显示遮罩 | boolean | false |
| progress | 进度值(0-100) | number | 0 |
| showPercent | 是否显示百分比 | boolean | false |

### 3. ARError - 错误提示组件
**用途**: 显示错误信息

**基本用法**:
```vue
<template>
  <!-- 网络错误 -->
  <ARError 
    :type="'network'"
    :title="'连接失败'"
    :description="'无法连接到服务器，请检查网络连接'"
    :show-retry="true"
    :show-back="true"
    @retry="handleRetry"
  />
  
  <!-- 权限错误 -->
  <ARError 
    :type="'permission'"
    :title="'权限不足'"
    :description="'您没有权限访问此资源'"
    :details="errorDetails"
    :show-details-toggle="true"
  />
</template>

<script setup>
import { ARError } from '@/components/@ai-ready/common/components/feedback'
</script>
```

**属性**:
| 属性 | 说明 | 类型 | 默认值 |
|------|------|------|--------|
| type | 错误类型 | 'network'/'permission'/'validation'/'server'/'generic' | 'generic' |
| title | 错误标题 | string | '' |
| description | 错误描述 | string | '' |
| details | 错误详情 | string | '' |
| showRetry | 是否显示重试按钮 | boolean | true |
| showBack | 是否显示返回按钮 | boolean | true |
| showDetailsToggle | 是否显示详情切换按钮 | boolean | true |

### 4. AREmpty - 空状态组件
**用途**: 显示空数据状态

**基本用法**:
```vue
<template>
  <!-- 无数据状态 -->
  <AREmpty 
    :type="'no-data'"
    :title="'暂无数据'"
    :description="'当前没有数据，您可以创建新的内容'"
    :show-create="true"
    :show-refresh="true"
    @create="handleCreate"
  />
  
  <!-- 无搜索结果 -->
  <AREmpty 
    :type="'no-result'"
    :title="'没有找到结果'"
    :description="'尝试调整搜索条件或关键词'"
    :image="'/images/no-results.svg'"
  />
</template>

<script setup>
import { AREmpty } from '@/components/@ai-ready/common/components/feedback'
</script>
```

**属性**:
| 属性 | 说明 | 类型 | 默认值 |
|------|------|------|--------|
| type | 空状态类型 | 'no-data'/'no-result'/'no-permission'/'no-network'/'no-file'/'no-message' | 'no-data' |
| title | 空状态标题 | string | '' |
| description | 空状态描述 | string | '' |
| image | 自定义图片URL | string | '' |
| showCreate | 是否显示创建按钮 | boolean | true |
| showRefresh | 是否显示刷新按钮 | boolean | true |

### 5. ARAlert - 告警组件
**用途**: 显示告警信息，支持P1-P3分级

**基本用法**:
```vue
<template>
  <!-- P1严重告警 -->
  <ARAlert 
    :severity="'critical'"
    :title="'CPU使用率超过95%'"
    :description="'服务器CPU使用率持续过高，可能影响系统性能'"
    :timestamp="alert.timestamp"
    :show-acknowledge="true"
    @acknowledge="handleAcknowledge"
  />
  
  <!-- P2高危告警 -->
  <ARAlert 
    :severity="'high'"
    :title="'内存使用率超过85%'"
    :description="'系统内存使用率偏高，建议优化或扩容'"
    :border="true"
    :closable="true"
  />
  
  <!-- P3中危告警 -->
  <ARAlert 
    :severity="'medium'"
    :title="'磁盘空间不足'"
    :description="'系统磁盘剩余空间不足20%，建议清理或扩容'"
    :compact="true"
  />
</template>

<script setup>
import { ARAlert } from '@/components/@ai-ready/common/components/feedback'
</script>
```

**属性**:
| 属性 | 说明 | 类型 | 默认值 |
|------|------|------|--------|
| severity | 告警严重性 | 'critical'/'high'/'medium'/'low'/'info' | 'info' |
| title | 告警标题 | string | '' |
| description | 告警描述 | string | '' |
| timestamp | 告警时间戳 | string/number/Date | '' |
| severity | 告警级别 | 'critical'(P1)/'high'(P2)/'medium'(P3) | 'info' |
| border | 是否显示边框 | boolean | false |
| closable | 是否可关闭 | boolean | true |
| showAcknowledge | 是否显示确认按钮 | boolean | false |

### 6. ARKpiCard - KPI卡片组件
**用途**: 显示关键指标，支持点击钻取

**基本用法**:
```vue
<template>
  <!-- 基础KPI卡片 -->
  <ARKpiCard 
    :title="'用户总数'"
    :value="userCount"
    :unit="'人'"
    :trend-direction="trendDirection"
    :trend-value="trendValue"
    :clickable="true"
    @click="handleCardClick"
  />
  
  <!-- 带状态的KPI卡片 -->
  <ARKpiCard 
    :title="'订单成功率'"
    :value="successRate"
    :unit="'%'"
    :format="'percent'"
    :status="'正常'"
    :status-type="'success'"
    :show-menu="true"
    @menu-command="handleMenuCommand"
  />
</template>

<script setup>
import { ARKpiCard } from '@/components/@ai-ready/common/components/feedback'
</script>
```

**属性**:
| 属性 | 说明 | 类型 | 默认值 |
|------|------|------|--------|
| title | 卡片标题 | string | '' |
| value | 指标值 | number/string | 0 |
| unit | 数值单位 | string | '' |
| trendDirection | 趋势方向 | 'up'/'down'/'neutral' | 'neutral' |
| trendValue | 趋势值 | number | null |
| clickable | 是否可点击 | boolean | true |
| format | 数值格式 | 'number'/'currency'/'percent'/'duration' | 'number' |
| status | 状态标签 | string | '' |
| statusType | 状态类型 | 'success'/'warning'/'danger'/'info' | 'info' |

### 7. ARKpiDetailDialog - KPI详情弹窗
**用途**: KPI卡片点击后的详情展示

**基本用法**:
```vue
<template>
  <!-- KPI卡片 -->
  <ARKpiCard 
    :title="'API成功率'"
    :value="apiSuccessRate"
    :unit="'%'"
    :clickable="true"
    @click="showDetail = true"
  />
  
  <!-- 详情弹窗 -->
  <ARKpiDetailDialog 
    v-model="showDetail"
    :title="'API成功率详情'"
    :value="apiSuccessRate"
    :unit="'%'"
    :trend-direction="trendDirection"
    :trend-value="trendValue"
    :chart-data="chartData"
    :table-data="tableData"
    @close="showDetail = false"
    @refresh-data="fetchData"
  />
</template>

<script setup>
import { ARKpiCard, ARKpiDetailDialog } from '@/components/@ai-ready/common/components/feedback'
</script>
```

## 集成示例

### 在页面中集成所有反馈组件

```vue
<template>
  <div class="dashboard-page">
    <!-- 加载状态 -->
    <ARLoading 
      v-if="loading" 
      :fullscreen="true" 
      :text="'正在加载数据...'"
    />
    
    <!-- 错误状态 -->
    <ARError 
      v-else-if="error"
      :type="'network'"
      :title="'加载失败'"
      :description="errorMessage"
      :show-retry="true"
      @retry="loadData"
    />
    
    <!-- 空状态 -->
    <AREmpty 
      v-else-if="isEmpty"
      :type="'no-data'"
      :title="'暂无数据'"
      :description="'当前没有监控数据'"
      :show-refresh="true"
      @refresh="loadData"
    />
    
    <!-- 正常状态 -->
    <div v-else>
      <!-- 告警列表 -->
      <div class="alerts-section">
        <h3>系统告警</h3>
        <ARAlert 
          v-for="alert in alerts"
          :key="alert.id"
          :severity="alert.severity"
          :title="alert.title"
          :description="alert.description"
          :timestamp="alert.timestamp"
          :show-acknowledge="true"
          @acknowledge="acknowledgeAlert(alert.id)"
        />
      </div>
      
      <!-- KPI卡片网格 -->
      <div class="kpi-grid">
        <ARKpiCard 
          v-for="metric in metrics"
          :key="metric.id"
          :title="metric.title"
          :value="metric.value"
          :unit="metric.unit"
          :trend-direction="metric.trend"
          :trend-value="metric.trendValue"
          :clickable="true"
          @click="showMetricDetail(metric)"
        />
      </div>
    </div>
    
    <!-- KPI详情弹窗 -->
    <ARKpiDetailDialog 
      v-model="detailVisible"
      :title="selectedMetric?.title"
      :value="selectedMetric?.value"
      :unit="selectedMetric?.unit"
      :trend-direction="selectedMetric?.trend"
      :trend-value="selectedMetric?.trendValue"
      :chart-data="detailChartData"
      :table-data="detailTableData"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import {
  ARLoading,
  ARError,
  AREmpty,
  ARAlert,
  ARKpiCard,
  ARKpiDetailDialog
} from '@/components/@ai-ready/common/components/feedback'

// 状态管理
const loading = ref(true)
const error = ref(false)
const errorMessage = ref('')
const isEmpty = ref(false)
const alerts = ref([])
const metrics = ref([])
const detailVisible = ref(false)
const selectedMetric = ref(null)
const detailChartData = ref([])
const detailTableData = ref([])

// 加载数据
const loadData = async () => {
  try {
    loading.value = true
    error.value = false
    
    // 模拟API调用
    const [alertsRes, metricsRes] = await Promise.all([
      fetchAlerts(),
      fetchMetrics()
    ])
    
    alerts.value = alertsRes
    metrics.value = metricsRes
    isEmpty.value = metricsRes.length === 0
  } catch (err) {
    error.value = true
    errorMessage.value = err.message
  } finally {
    loading.value = false
  }
}

// 显示指标详情
const showMetricDetail = async (metric) => {
  selectedMetric.value = metric
  try {
    const [chartData, tableData] = await Promise.all([
      fetchMetricChartData(metric.id),
      fetchMetricTableData(metric.id)
    ])
    detailChartData.value = chartData
    detailTableData.value = tableData
    detailVisible.value = true
  } catch (err) {
    console.error('加载详情失败:', err)
  }
}

// 确认告警
const acknowledgeAlert = async (alertId) => {
  try {
    await acknowledgeAlertApi(alertId)
    alerts.value = alerts.value.filter(alert => alert.id !== alertId)
  } catch (err) {
    console.error('确认告警失败:', err)
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.dashboard-page {
  padding: 20px;
}

.alerts-section {
  margin-bottom: 24px;
}

.alerts-section h3 {
  margin-bottom: 16px;
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.kpi-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
}

@media (max-width: 768px) {
  .kpi-grid {
    grid-template-columns: 1fr;
  }
}
</style>
```

## 最佳实践

### 1. 加载状态管理
- 使用`ARLoading`进行全屏加载
- 使用`ARSkeleton`进行内容占位
- 避免同时显示多个加载状态

### 2. 错误处理
- 根据错误类型使用不同的`ARError`样式
- 提供明确的恢复操作
- 记录错误详情供调试

### 3. 空状态处理
- 根据场景使用不同的`AREmpty`类型
- 提供创建或刷新操作
- 使用友好的描述文案

### 4. 告警分级
- P1严重告警: 使用`critical`级别（红色）
- P2高危告警: 使用`high`级别（橙色）
- P3中危告警: 使用`medium`级别（黄色）

### 5. KPI卡片交互
- 为重要的KPI卡片启用点击钻取
- 在详情弹窗中提供历史趋势
- 支持数据导出和打印

## 主题定制

所有组件都支持深色模式，会自动根据`data-theme`属性切换样式：

```html
<html data-theme="dark">
  <!-- 组件会自动应用深色样式 -->
</html>
```

## 性能优化

1. **按需加载**: 只导入需要的组件
2. **虚拟滚动**: 大数据量时使用虚拟滚动
3. **防抖节流**: 高频操作使用防抖节流
4. **缓存策略**: 缓存频繁使用的数据

## 无障碍支持

所有组件都包含基本的无障碍支持：
- 键盘导航
- 屏幕阅读器支持
- 高对比度模式
- 焦点管理

## 浏览器兼容性

支持所有现代浏览器（Chrome ≥ 60, Firefox ≥ 55, Safari ≥ 12, Edge ≥ 79）

## 故障排除

### 常见问题
1. **组件不显示**: 检查是否正确导入和注册
2. **样式异常**: 检查CSS冲突和主题设置
3. **交互无效**: 检查事件绑定和组件状态

### 调试建议
1. 使用Vue Devtools检查组件状态
2. 检查控制台错误信息
3. 验证props传递是否正确

## 更新日志

### v1.0.0 (2026-04-28)
- 初始版本发布
- 包含7个核心反馈组件
- 支持P1-P3告警分级
- 支持KPI卡片点击钻取
- 完整的深色模式支持