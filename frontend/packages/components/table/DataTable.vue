<template>
  <div class="data-table-container">
    <!-- 搜索栏 -->
    <div class="table-toolbar" v-if="showToolbar">
      <van-search
        v-model="searchQuery"
        :placeholder="searchPlaceholder"
        @search="handleSearch"
        @clear="handleClear"
        v-if="showSearch"
      />
      <div class="toolbar-right">
        <van-button 
          v-if="showFilter" 
          size="small" 
          type="default" 
          @click="showFilterPanel = true"
        >
          <van-icon name="filter-o" />
          筛选
        </van-button>
      </div>
    </div>

    <!-- 数据表格 -->
    <div class="table-wrapper" :class="{ 'loading': loading }">
      <table class="data-table" :class="{ 'striped': striped, 'bordered': bordered }">
        <thead>
          <tr>
            <th 
              v-for="col in visibleColumns" 
              :key="col.key"
              :class="{ 
                'sortable': col.sortable, 
                'sorted': sortKey === col.key,
                'sticky-left': col.fixed === 'left',
                'sticky-right': col.fixed === 'right'
              }"
              :style="getColumnStyle(col)"
              @click="handleSort(col)"
            >
              <div class="th-content">
                <span>{{ col.title }}</span>
                <span v-if="col.sortable" class="sort-icons">
                  <van-icon 
                    name="arrow-up" 
                    :class="{ 'active': sortKey === col.key && sortOrder === 'asc' }"
                  />
                  <van-icon 
                    name="arrow-down" 
                    :class="{ 'active': sortKey === col.key && sortOrder === 'desc' }"
                  />
                </span>
              </div>
            </th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(row, index) in paginatedData" :key="getRowKey(row, index)">
            <td 
              v-for="col in visibleColumns" 
              :key="col.key"
              :class="{
                'sticky-left': col.fixed === 'left',
                'sticky-right': col.fixed === 'right'
              }"
              :style="getColumnStyle(col)"
            >
              <slot 
                :name="`cell-${col.key}`" 
                :row="row" 
                :value="row[col.key]"
                :index="index"
              >
                {{ formatCellValue(row[col.key], col) }}
              </slot>
            </td>
          </tr>
          <tr v-if="paginatedData.length === 0">
            <td :colspan="visibleColumns.length" class="empty-cell">
              <van-empty :description="emptyText" />
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 分页 -->
    <div class="table-pagination" v-if="showPagination && total > 0">
      <van-pagination
        v-model="currentPage"
        :total-items="total"
        :items-per-page="pageSize"
        :show-page-size="showPageSize"
        @change="handlePageChange"
      />
    </div>

    <!-- 筛选面板 -->
    <van-popup v-model:show="showFilterPanel" position="right" :style="{ width: '80%', height: '100%' }">
      <div class="filter-panel">
        <div class="filter-header">
          <span>筛选条件</span>
          <van-icon name="cross" @click="showFilterPanel = false" />
        </div>
        <div class="filter-content">
          <div v-for="col in filterableColumns" :key="col.key" class="filter-item">
            <div class="filter-label">{{ col.title }}</div>
            <van-field
              v-if="col.filterType === 'text'"
              v-model="filters[col.key]"
              :placeholder="`请输入${col.title}`"
              clearable
            />
            <van-field
              v-else-if="col.filterType === 'number'"
              v-model="filters[col.key]"
              type="number"
              :placeholder="`请输入${col.title}`"
              clearable
            />
          </div>
        </div>
        <div class="filter-footer">
          <van-button type="default" block @click="resetFilters">重置</van-button>
          <van-button type="primary" block @click="applyFilters">确定</van-button>
        </div>
      </div>
    </van-popup>

    <!-- 加载状态 -->
    <van-overlay :show="loading">
      <div class="loading-wrapper">
        <van-loading type="spinner" size="24px">加载中...</van-loading>
      </div>
    </van-overlay>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, PropType } from 'vue';

interface Column {
  key: string;
  title: string;
  sortable?: boolean;
  filterable?: boolean;
  filterType?: 'text' | 'number';
  width?: number | string;
  fixed?: 'left' | 'right';
  hidden?: boolean;
  formatter?: (value: any, row: any) => string;
}

const props = defineProps({
  data: { type: Array as PropType<any[]>, default: () => [] },
  columns: { type: Array as PropType<Column[]>, required: true },
  showToolbar: { type: Boolean, default: true },
  showSearch: { type: Boolean, default: true },
  searchPlaceholder: { type: String, default: '请输入搜索内容' },
  showFilter: { type: Boolean, default: true },
  showPagination: { type: Boolean, default: true },
  defaultPage: { type: Number, default: 1 },
  defaultPageSize: { type: Number, default: 10 },
  showPageSize: { type: Number, default: 5 },
  striped: { type: Boolean, default: false },
  bordered: { type: Boolean, default: true },
  emptyText: { type: String, default: '暂无数据' },
  loading: { type: Boolean, default: false },
  rowKey: { type: String, default: 'id' }
});

const emit = defineEmits(['sort-change', 'filter-change', 'page-change', 'search']);

const searchQuery = ref('');
const currentPage = ref(props.defaultPage);
const pageSize = ref(props.defaultPageSize);
const sortKey = ref('');
const sortOrder = ref<'asc' | 'desc'>('asc');
const showFilterPanel = ref(false);
const filters = ref<Record<string, any>>({});

const visibleColumns = computed(() => props.columns.filter(col => !col.hidden));
const filterableColumns = computed(() => props.columns.filter(col => col.filterable));

const filteredData = computed(() => {
  let data = [...props.data];
  if (searchQuery.value) {
    const query = searchQuery.value.toLowerCase();
    data = data.filter(row => visibleColumns.value.some(col => {
      const value = String(row[col.key] || '').toLowerCase();
      return value.includes(query);
    }));
  }
  Object.keys(filters.value).forEach(key => {
    if (filters.value[key]) {
      data = data.filter(row => String(row[key]).includes(String(filters.value[key])));
    }
  });
  return data;
});

const sortedData = computed(() => {
  if (!sortKey.value) return filteredData.value;
  const data = [...filteredData.value];
  data.sort((a, b) => {
    const aVal = a[sortKey.value];
    const bVal = b[sortKey.value];
    if (aVal === bVal) return 0;
    if (aVal == null) return 1;
    if (bVal == null) return -1;
    const result = aVal > bVal ? 1 : -1;
    return sortOrder.value === 'asc' ? result : -result;
  });
  return data;
});

const total = computed(() => sortedData.value.length);

const paginatedData = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value;
  return sortedData.value.slice(start, start + pageSize.value);
});

const getColumnStyle = (col: Column) => {
  const style: Record<string, string> = {};
  if (col.width) style.width = typeof col.width === 'number' ? `${col.width}px` : col.width;
  if (col.minWidth) style.minWidth = typeof col.minWidth === 'number' ? `${col.minWidth}px` : col.minWidth;
  return style;
};

const getRowKey = (row: any, index: number) => row[props.rowKey] || index;

const formatCellValue = (value: any, col: Column) => {
  if (col.formatter) return col.formatter(value, {});
  return value ?? '-';
};

const handleSort = (col: Column) => {
  if (!col.sortable) return;
  if (sortKey.value === col.key) {
    sortOrder.value = sortOrder.value === 'asc' ? 'desc' : 'asc';
  } else {
    sortKey.value = col.key;
    sortOrder.value = 'asc';
  }
  currentPage.value = 1;
  emit('sort-change', { key: sortKey.value, order: sortOrder.value });
};

const handleSearch = () => {
  currentPage.value = 1;
  emit('search', searchQuery.value);
};

const handleClear = () => {
  searchQuery.value = '';
  currentPage.value = 1;
  emit('search', '');
};

const handlePageChange = (page: number) => {
  emit('page-change', { page, pageSize: pageSize.value });
};

const applyFilters = () => {
  currentPage.value = 1;
  showFilterPanel.value = false;
  emit('filter-change', filters.value);
};

const resetFilters = () => {
  filters.value = {};
  currentPage.value = 1;
  showFilterPanel.value = false;
  emit('filter-change', {});
};
</script>

<style scoped>
.data-table-container {
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
}

.table-toolbar {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #ebedf0;
  gap: 12px;
}

.table-toolbar .van-search {
  flex: 1;
  padding: 0;
}

.toolbar-right {
  display: flex;
  gap: 8px;
}

.table-wrapper {
  overflow-x: auto;
  position: relative;
}

.table-wrapper.loading {
  opacity: 0.6;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}

.data-table th,
.data-table td {
  padding: 12px 16px;
  text-align: left;
}

.data-table.bordered th,
.data-table.bordered td {
  border: 1px solid #ebedf0;
}

.data-table thead {
  background: #f7f8fa;
}

.data-table th {
  font-weight: 500;
  color: #323233;
  white-space: nowrap;
}

.data-table th.sortable {
  cursor: pointer;
  user-select: none;
}

.data-table th.sortable:hover {
  background: #ebedf0;
}

.th-content {
  display: flex;
  align-items: center;
  gap: 4px;
}

.sort-icons {
  display: flex;
  flex-direction: column;
  font-size: 10px;
  color: #c8c9cc;
}

.sort-icons .van-icon.active {
  color: #1989fa;
}

.data-table tbody tr:hover {
  background: #f7f8fa;
}

.data-table.striped tbody tr:nth-child(even) {
  background: #fafafa;
}

.data-table td {
  color: #646566;
}

.sticky-left {
  position: sticky;
  left: 0;
  background: inherit;
  z-index: 1;
}

.sticky-right {
  position: sticky;
  right: 0;
  background: inherit;
  z-index: 1;
}

.empty-cell {
  text-align: center;
  padding: 40px;
}

.table-pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 16px;
  gap: 16px;
  border-top: 1px solid #ebedf0;
}

.filter-panel {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.filter-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-bottom: 1px solid #ebedf0;
  font-weight: 500;
}

.filter-content {
  flex: 1;
  padding: 16px;
  overflow-y: auto;
}

.filter-item {
  margin-bottom: 16px;
}

.filter-label {
  margin-bottom: 8px;
  color: #323233;
  font-weight: 500;
}

.filter-footer {
  display: flex;
  gap: 12px;
  padding: 16px;
  border-top: 1px solid #ebedf0;
}

.filter-footer .van-button {
  flex: 1;
}

.loading-wrapper {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
}

@media (max-width: 768px) {
  .table-toolbar {
    flex-direction: column;
    align-items: stretch;
  }
  
  .table-pagination {
    flex-direction: column;
    gap: 12px;
  }
  
  .data-table th,
  .data-table td {
    padding: 8px 12px;
    font-size: 13px;
  }
}
</style>