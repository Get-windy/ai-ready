<template>
  <PageContainer>
    <template #header>
      <div class="page-header">
        <a-breadcrumb>
          <a-breadcrumb-item><router-link to="/">首页</router-link></a-breadcrumb-item>
          <a-breadcrumb-item><router-link to="/erp/sale">销售管理</router-link></a-breadcrumb-item>
          <a-breadcrumb-item>销售订单明细列配置</a-breadcrumb-item>
        </a-breadcrumb>
        <h2 class="page-title">销售订单明细列配置</h2>
      </div>
    </template>

    <template #actions>
      <a-space>
        <a-button type="primary" @click="handleSave" :loading="saving">
          <template #icon><SaveOutlined /></template>
          保存配置
        </a-button>
        <a-button @click="handleReset">
          <template #icon><ReloadOutlined /></template>
          恢复默认
        </a-button>
      </a-space>
    </template>

    <div class="column-config-container">
      <div class="config-description">
        <a-alert
          message="说明"
          description="您可以在此配置销售订单明细表格中显示的列。勾选显示的列，拖拽调整列顺序，设置列宽度和冻结选项。"
          type="info"
          show-icon
        />
      </div>

    <div class="column-list">
      <draggable
        v-model="columnSettings"
        handle=".drag-handle"
        item-key="key"
        class="draggable-list"
        ghost-class="drag-ghost"
        chosen-class="drag-chosen"
        drag-class="dragging"
        @change="onColumnSettingChange"
      >
        <template #item="{ element: setting, index }">
          <div
            class="column-item"
            :class="{ 'locked-column': isLockedColumn(setting.key) }"
          >
            <div class="column-item-content">
              <div class="drag-handle" v-if="!isLockedColumn(setting.key)">⠿</div>

              <a-checkbox
                v-model:checked="setting.visible"
                :disabled="isLockedColumn(setting.key)"
                @change="onColumnVisibilityChange"
              >
                <span class="column-title">{{ setting.title }}</span>
              </a-checkbox>

              <div class="column-controls">
                <div class="control-group">
                  <span class="control-label">冻结</span>
                  <a-select
                    v-model:value="setting.fixed"
                    size="small"
                    style="width: 100px;"
                    :disabled="isLockedColumn(setting.key)"
                    @change="onColumnSettingChange"
                  >
                    <a-select-option value="">不冻结</a-select-option>
                    <a-select-option value="left">左侧冻结</a-select-option>
                    <a-select-option value="right">右侧冻结</a-select-option>
                  </a-select>
                </div>

                <div class="control-group">
                  <span class="control-label">宽度</span>
                  <a-input-number
                    v-model:value="setting.width"
                    size="small"
                    :min="50"
                    :max="500"
                    style="width: 80px;"
                    :disabled="isLockedColumn(setting.key)"
                    @change="onColumnSettingChange"
                  />
                </div>

                <a-button
                  v-if="!isLockedColumn(setting.key)"
                  type="link"
                  size="small"
                  danger
                  @click="removeColumn(index)"
                >
                  <template #icon><DeleteOutlined /></template>
                </a-button>
              </div>
            </div>

            <div v-if="isLockedColumn(setting.key)" class="locked-indicator">
              <LockOutlined />
            </div>
          </div>
        </template>
      </draggable>
    </div>

      <div class="column-management">
        <div class="management-header">
          <h3>添加列</h3>
          <a-input
            v-model:value="searchTerm"
            placeholder="搜索列名..."
            size="small"
            style="width: 200px; margin-bottom: 12px;"
          >
            <template #prefix><SearchOutlined /></template>
          </a-input>
        </div>

        <div class="available-columns">
          <div
            v-for="column in availableColumns"
            :key="column.key"
            class="available-column-item"
            @click="addColumn(column)"
          >
            <span class="column-name">{{ column.title }}</span>
            <PlusCircleOutlined class="add-icon" />
          </div>

          <div v-if="availableColumns.length === 0" class="no-available-columns">
            没有可添加的列
          </div>
        </div>
      </div>
    </div>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import {
  SaveOutlined,
  ReloadOutlined,
  DeleteOutlined,
  LockOutlined,
  SearchOutlined,
  PlusCircleOutlined
} from '@ant-design/icons-vue'
import draggable from 'vuedraggable'
import PageContainer from '@/components/PageContainer/PageContainer.vue'

// 锁定的列（不允许隐藏或删除）
const LOCKED_COLUMNS = ['rowNo', 'action']

// 所有可配置的列定义
const ALL_COLUMNS = [
  { key: 'rowNo', title: '行号', width: 60, fixed: 'left' as const },
  { key: 'action', title: '操作', width: 80, fixed: 'left' as const },
  { key: 'image', title: '图片', width: 80 },
  { key: 'productName', title: '商品名称', width: 150 },
  { key: 'itemCode', title: '货号', width: 100 },
  { key: 'preOrderNo', title: '预订货单编号', width: 120 },
  { key: 'smallUnitBarcode', title: '小单位条码', width: 120 },
  { key: 'usePreOrderAmount', title: '使用预订货款', width: 120 },
  { key: 'barcode', title: '条码', width: 120 },
  { key: 'specification', title: '规格', width: 100 },
  { key: 'model', title: '型号', width: 100 },
  { key: 'area', title: '区域', width: 80 },
  { key: 'location', title: '货位', width: 80 },
  { key: 'unit', title: '计价单位', width: 80 },
  { key: 'origin', title: '产地', width: 100 },
  { key: 'brand', title: '品牌', width: 100 },
  { key: 'lineAttribute', title: '商品行属性', width: 120 },
  { key: 'batchCode', title: '批次条码', width: 120 },
  { key: 'productionDate', title: '生产日期', width: 110 },
  { key: 'shelfLife', title: '保质期', width: 80 },
  { key: 'expiryDate', title: '到期日期', width: 110 },
  { key: 'customField1', title: '单据自定义1(数字字段)', width: 140 },
  { key: 'customField2', title: '单据自定义2(数字字段)', width: 140 },
  { key: 'pieceQuantity', title: '件散数量', width: 100 },
  { key: 'bigPack', title: '大包装', width: 80 },
  { key: 'midPack', title: '中包装', width: 80 },
  { key: 'smallPack', title: '小包装', width: 80 },
  { key: 'customField3', title: '单据自定义3(数字字段)', width: 140 },
  { key: 'customField4', title: '单据自定义4(文本字段)', width: 140 },
  { key: 'customField5', title: '单据自定义5(文本字段)', width: 140 },
  { key: 'customField6', title: '单据自定义6(数字字段)', width: 140 },
  { key: 'customField7', title: '单据自定义7(数字字段)', width: 140 },
  { key: 'customField8', title: '单据自定义8(往来单位)', width: 150 },
  { key: 'customField9', title: '单据自定义9(职员)', width: 120 },
  { key: 'smallUnit', title: '小单位', width: 80 },
  { key: 'smallUnitPrice', title: '小单位单价', width: 100 },
  { key: 'smallUnitQuantity', title: '小单位数量', width: 100 },
  { key: 'customField10', title: '单据自定义10(部门)', width: 120 },
  { key: 'latestSaleDate', title: '最近销售日期', width: 110 },
  { key: 'latestSalePrice', title: '最近售价', width: 100 },
  { key: 'retailPrice', title: '零售价', width: 100 },
  { key: 'wholesalePrice', title: '批发价', width: 100 },
  { key: 'lowestPrice', title: '最低售价', width: 100 },
  { key: 'restaurant', title: '餐饮店', width: 80 },
  { key: 'canteen', title: '食堂团餐', width: 100 },
  { key: 'vipSelf', title: '自助vip', width: 100 },
  { key: 'largeGroup', title: '大团餐', width: 80 },
  { key: 'specialCustomer', title: '特价客户', width: 100 },
  { key: 'availableStock', title: '可用库存', width: 100 },
  { key: 'availableStockConverted', title: '可用库存换算结果', width: 150 },
  { key: 'bookStock', title: '账面库存', width: 100 },
  { key: 'quantity', title: '数量', width: 80 },
  { key: 'conversionRelation', title: '换算关系', width: 100 },
  { key: 'unshippedQuantity', title: '未发数量', width: 100 },
  { key: 'shippedQuantityDetail', title: '已发数量', width: 100 },
  { key: 'unitPrice', title: '单价', width: 80 },
  { key: 'amount', title: '金额', width: 80 },
  { key: 'costPrice', title: '参考成本单价', width: 120 },
  { key: 'costAmount', title: '参考成本金额', width: 120 },
  { key: 'grossProfit', title: '参考毛利', width: 100 },
  { key: 'discount', title: '折扣(%)', width: 80 },
  { key: 'outRestaurant', title: '外围餐饮店', width: 120 },
  { key: 'discountedUnitPrice', title: '折后单价', width: 100 },
  { key: 'originalPrice', title: '折单原价', width: 100 },
  { key: 'vipLevel1', title: '重点|vip01', width: 100 },
  { key: 'vipLevel2', title: '连锁|vip', width: 100 },
  { key: 'discountedAmount', title: '折后金额', width: 100 },
  { key: 'discountPercent', title: '优惠折扣(%)', width: 120 },
  { key: 'favorableUnitPrice', title: '惠后单价', width: 100 },
  { key: 'favorableAmount', title: '优惠后金额', width: 120 },
  { key: 'giftItem', title: '兑换礼品', width: 100 },
  { key: 'exchangePoints', title: '兑换积分', width: 100 },
  { key: 'usedPoints', title: '使用积分', width: 100 },
  { key: 'volume', title: '体积（m³）', width: 100 },
  { key: 'weight', title: '重量（kg）', width: 100 },
  { key: 'gift', title: '赠品', width: 80 },
  { key: 'remark', title: '备注', width: 150 },
]

// 本地存储键
const STORAGE_KEY = 'sale-order-item-columns-config'

// 响应式状态
const columnSettings = ref<any[]>([])
const searchTerm = ref('')
const saving = ref(false)

// 计算属性
const availableColumns = computed(() => {
  const term = searchTerm.value.toLowerCase()
  return ALL_COLUMNS.filter(col =>
    col.title.toLowerCase().includes(term) &&
    !columnSettings.value.some(setting => setting.key === col.key)
  )
})

// 方法
const isLockedColumn = (key: string) => LOCKED_COLUMNS.includes(key)

const handleSave = async () => {
  saving.value = true
  try {
    // 保存到本地存储
    const config = columnSettings.value.map(({ visible, fixed, width, key, title }) => ({
      key, title, visible, fixed, width
    }))
    localStorage.setItem(STORAGE_KEY, JSON.stringify(config))
    message.success('列配置保存成功！')
  } catch (error) {
    console.error('保存列配置失败:', error)
    message.error('保存失败，请重试')
  } finally {
    saving.value = false
  }
}

const handleReset = () => {
  columnSettings.value = ALL_COLUMNS.map(col => ({
    ...col,
    visible: true,
    fixed: col.fixed || '',
  }))
  message.info('已恢复默认配置')
}

const onColumnVisibilityChange = () => {
  // 可以在这里添加一些验证逻辑
  // 确保 columnSettings 已定义
  if (columnSettings.value && Array.isArray(columnSettings.value)) {
    // 数据验证通过
  }
}

const onColumnSettingChange = () => {
  // 列设置变化时的处理
  // 确保 columnSettings 已定义
  if (columnSettings.value && Array.isArray(columnSettings.value)) {
    // 数据验证通过
  }
}

const addColumn = (column: any) => {
  const exists = columnSettings.value.some(setting => setting.key === column.key)
  if (!exists) {
    columnSettings.value.push({
      ...column,
      visible: true,
      fixed: '',
    })
  }
}

const removeColumn = (index: number) => {
  if (columnSettings.value[index].key === 'rowNo' || columnSettings.value[index].key === 'action') {
    message.warning('锁定的列不能删除')
    return
  }
  columnSettings.value.splice(index, 1)
}

// 生命周期
onMounted(() => {
  // 尝试从本地存储加载配置
  try {
    const stored = localStorage.getItem(STORAGE_KEY)
    if (stored) {
      const parsed = JSON.parse(stored)
      // 合并存储的配置与当前可用列
      const mergedConfig = ALL_COLUMNS.map(col => {
        const storedCol = parsed.find((sc: any) => sc.key === col.key)
        return storedCol ? { ...col, ...storedCol } : { ...col, visible: true, fixed: col.fixed || '' }
      })
      columnSettings.value = mergedConfig
    } else {
      // 默认配置
      handleReset()
    }
  } catch (error) {
    console.error('加载列配置失败:', error)
    handleReset()
  }
})
</script>

<style scoped>
.column-config-container {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.config-description {
  margin-bottom: 16px;
}

.column-list {
  border: 1px solid #e8e8e8;
  border-radius: 6px;
  background: #fff;
  padding: 12px;
}

.column-item {
  display: flex;
  align-items: center;
  padding: 8px 12px;
  border: 1px solid #f0f0f0;
  border-radius: 4px;
  margin-bottom: 6px;
  background: #fafafa;
  transition: all 0.2s;
}

.column-item:hover {
  background: #f5f7fa;
  border-color: #d9d9d9;
}

.column-item-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex: 1;
  gap: 12px;
}

.column-item.locked-column {
  background: #e6f7ff;
  border-color: #91d5ff;
}

.drag-handle {
  cursor: move;
  padding: 4px;
  color: #bfbfbf;
  font-size: 14px;
  user-select: none;
}

.drag-handle:hover {
  color: #1890ff;
}

.column-title {
  font-size: 14px;
  color: #262626;
}

.column-controls {
  display: flex;
  align-items: center;
  gap: 16px;
}

.control-group {
  display: flex;
  align-items: center;
  gap: 6px;
}

.control-label {
  font-size: 12px;
  color: #8c8c8c;
}

.locked-indicator {
  color: #1890ff;
  font-size: 14px;
}

.draggable-list {
  min-height: 100px;
}

.drag-ghost {
  opacity: 0.5;
  background: #f0f0f0;
}

.drag-chosen {
  background: #e6f7ff;
  border-color: #91d5ff;
}

.column-management {
  border: 1px solid #e8e8e8;
  border-radius: 6px;
  background: #fff;
  padding: 16px;
}

.management-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.management-header h3 {
  margin: 0;
  font-size: 16px;
  color: #262626;
}

.available-columns {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: 8px;
}

.available-column-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  border: 1px solid #e8e8e8;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s;
  background: #fafafa;
}

.available-column-item:hover {
  background: #e6f7ff;
  border-color: #91d5ff;
  transform: translateY(-1px);
}

.column-name {
  font-size: 14px;
  color: #262626;
}

.add-icon {
  color: #52c41a;
  font-size: 16px;
}

.no-available-columns {
  text-align: center;
  padding: 20px;
  color: #8c8c8c;
  font-style: italic;
}

:deep(.ant-checkbox-wrapper) {
  display: flex;
  align-items: center;
}

:deep(.ant-btn-link) {
  padding: 0 4px;
  height: auto;
}

:deep(.ant-input-number) {
  width: 80px !important;
}
</style>