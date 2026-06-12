<template>
  <ErrorBoundary @error="handleError"><PageContainer full-height>
    <template #header>
      <div class="batch-page-header">
        <div class="batch-page-header-inner">
          <div>
            <a-breadcrumb>
              <a-breadcrumb-item><router-link to="/">首页</router-link></a-breadcrumb-item>
              <a-breadcrumb-item>库存管理</a-breadcrumb-item>
            </a-breadcrumb>
            <div class="batch-tabs-wrapper">
              <a-tabs v-model:activeKey="activeTab" @change="handleTabChange">
                <a-tab-pane key="batch" tab="批次管理" />
                <a-tab-pane key="serial" tab="序列号管理" />
              </a-tabs>
            </div>
          </div>
          <div class="batch-header-actions">
            <span class="data-status">
              <a-badge :status="loading ? 'processing' : 'success'" />
              <span v-if="lastUpdateTime" class="update-time">数据更新: {{ lastUpdateTime }}</span>
              <span v-if="autoRefreshCountdown > 0" class="auto-refresh-badge">
                <SyncOutlined /> {{ autoRefreshCountdown }}s
              </span>
            </span>
            <a-button size="small" :loading="loading" @click="debounceClick('refresh', fetchData)">
              <template #icon><ReloadOutlined /></template> 刷新
            </a-button>
            <a-button size="small" @click="debounceClick('export', handleExport)">
              <template #icon><DownloadOutlined /></template> 导出
            </a-button>
            <!-- 快捷键提示 -->
            <span class="shortcut-hints">
              <span class="shortcut-hint"><kbd>F5</kbd> 刷新</span>
              <span class="shortcut-hint"><kbd>Ctrl+N</kbd> 新增</span>
              <span class="shortcut-hint"><kbd>Ctrl+E</kbd> 导出</span>
            </span>
          </div>
        </div>
      </div>
    </template>

    <template #filter>
      <SearchBar
        :fields="searchFields"
        :loading="loading"
        @search="handleSearch"
        @reset="handleReset"
      />
    </template>

    <template #default>
      <div class="batch-body">
        <!-- 统计卡片 -->
        <a-row :gutter="12" style="margin-bottom: 12px;">
          <a-col :span="6">
            <div class="stat-card" style="border-top: 3px solid #1890ff;">
              <div class="stat-value" style="color:#1890ff">{{ statistics.total }}</div>
              <div class="stat-label">总批次数</div>
            </div>
          </a-col>
          <a-col :span="6">
            <div class="stat-card" style="border-top: 3px solid #52c41a;">
              <div class="stat-value" style="color:#52c41a">{{ statistics.active }}</div>
              <div class="stat-label">启用中</div>
            </div>
          </a-col>
          <a-col :span="6">
            <div class="stat-card" style="border-top: 3px solid #faad14;">
              <div class="stat-value" style="color:#faad14">{{ statistics.quarantined }}</div>
              <div class="stat-label">隔离</div>
            </div>
          </a-col>
          <a-col :span="6">
            <div class="stat-card" style="border-top: 3px solid #ff4d4f;">
              <div class="stat-value" style="color:#ff4d4f">{{ statistics.expired }}</div>
              <div class="stat-label">过期</div>
            </div>
          </a-col>
        </a-row>

        <!-- 操作按钮行 -->
        <div class="action-bar">
          <a-space>
            <a-button type="primary" v-permission="'erp:batch:create'" @click="handleCreate">
              <template #icon><PlusOutlined /></template>
              新建批次
            </a-button>
            <a-button @click="debounceClick('refresh', fetchData)">
              <template #icon><ReloadOutlined /></template>
              刷新
            </a-button>
            <a-button v-if="hasSelected" v-permission="'erp:batch:update-status'" danger @click="handleBatchStatusChange">
              <template #icon><SwapOutlined /></template>
              批量状态变更 ({{ selectedIds.length }})
            </a-button>
          </a-space>
          <span class="action-bar-hint">共 {{ pagination.total }} 条记录</span>
        </div>

        <!-- 临期预警（可折叠） -->
        <a-collapse
          v-if="expiringBatches.length > 0"
          v-model:activeKey="expiryCollapseKey"
          class="expiry-collapse"
        >
          <a-collapse-panel
            key="expiry"
            :header="`临期预警（${expiringBatches.length} 个批次将在 30 天内过期）`"
          >
            <a-table
              :dataSource="expiringBatches"
              :columns="expiryColumns"
              :pagination="false as any"
              size="small"
              :loading="expiryLoading"
              row-key="id"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.dataIndex === 'expirationDate'">
                  <span :class="getExpiryClass(record)">{{ record.expirationDate || '-' }}</span>
                </template>
                <template v-if="column.dataIndex === 'daysRemaining'">
                  <span :class="getExpiryClass(record)">
                    {{ getDaysRemaining(record) <= 0 ? '已过期' : `${getDaysRemaining(record)} 天` }}
                  </span>
                </template>
                <template v-if="column.dataIndex === 'batchStatus'">
                  <StatusTag :status="record.batchStatus" :map="BATCH_STATUS" />
                </template>
              </template>
            </a-table>
          </a-collapse-panel>
        </a-collapse>

        <!-- 批次列表 -->
        <div class="table-wrapper" ref="tableWrapperRef">
          <vxe-table
            ref="tableRef"
            :data="tableData"
            :loading="loading"
            :height="tableHeight"
            :row-config="{ keyField: 'id', height: 44 }"
            :header-config="{ height: 40 }"
            :column-config="{ minWidth: 80 }"
            :checkbox-config="{ highlight: true, range: true }"
            @checkbox-change="onSelectionChange"
            @checkbox-all="onSelectionChange"
            @cell-dblclick="({ row }) => handleDetail(row)"
          >
            <vxe-column type="checkbox" width="40" align="center" />
            <vxe-column type="seq" title="#" width="44" align="center" />
            <vxe-column field="batchNo" title="批次号" width="150" show-overflow="tooltip" sortable />
            <vxe-column field="productCode" title="商品编码" width="120" show-overflow="tooltip" sortable />
            <vxe-column field="productName" title="商品名称" width="150" show-overflow="tooltip" />
            <vxe-column field="warehouseName" title="仓库" width="120" show-overflow="tooltip" />
            <vxe-column field="totalQuantity" title="总数量" width="90" align="right" sortable />
            <vxe-column field="availableQuantity" title="可用数量" width="90" align="right" sortable>
              <template #default="{ row }">
                <span :class="getQuantityClass(row)">{{ row.availableQuantity }}</span>
              </template>
            </vxe-column>
            <vxe-column field="productionDate" title="生产日期" width="105" align="center" sortable>
              <template #default="{ row }">
                {{ row.productionDate || '-' }}
              </template>
            </vxe-column>
            <vxe-column field="expirationDate" title="到期日期" width="105" align="center" sortable>
              <template #default="{ row }">
                <span :class="getExpiryClass(row)">{{ row.expirationDate || '-' }}</span>
              </template>
            </vxe-column>
            <vxe-column field="batchStatus" title="批次状态" width="100" align="center" sortable>
              <template #default="{ row }">
                <StatusTag :status="row.batchStatus" :map="BATCH_STATUS" />
              </template>
            </vxe-column>
            <vxe-column field="qualityStatus" title="质量状态" width="100" align="center">
              <template #default="{ row }">
                <StatusTag v-if="row.qualityStatus" :status="row.qualityStatus" :map="QUALITY_STATUS" />
                <span v-else>-</span>
              </template>
            </vxe-column>
            <vxe-column field="sourceType" title="来源" width="80" align="center">
              <template #default="{ row }">
                {{ sourceTypeLabel(row.sourceType) }}
              </template>
            </vxe-column>
            <vxe-column field="createdByName" title="创建人" width="100" show-overflow="tooltip" />
            <vxe-column field="createdAt" title="创建时间" width="165" show-overflow="tooltip" sortable />
            <vxe-column title="操作" width="350" fixed="right" align="left">
              <template #default="{ row }">
                <a-space :size="4" wrap>
                  <a-button type="link" size="small" @click="handleDetail(row)">详情</a-button>
                  <a-button type="link" size="small" v-permission="'erp:batch:inbound'" @click="handleInbound(row)">入库</a-button>
                  <a-button type="link" size="small" v-permission="'erp:batch:outbound'" @click="handleOutbound(row)">出库</a-button>
                  <a-button type="link" size="small" v-permission="'erp:batch:inspect'" @click="handleQualityInspection(row)">质检</a-button>
                  <a-button type="link" size="small" v-permission="'erp:batch:update-status'" @click="handleStatusChange(row)">状态</a-button>
                  <a-button type="link" size="small" v-permission="'erp:batch:delete'" danger @click="handleDelete(row)">删除</a-button>
                </a-space>
              </template>
            </vxe-column>
            <template #empty>
              <EmptyState v-if="hasError" image="error" title="数据加载异常" description="数据获取失败，请刷新重试" :show-add="false" size="small" @refresh="fetchData" />
              <EmptyState v-else image="no-data" title="暂无批次数据" description="当前没有批次数据" add-text="新建批次" size="small" @refresh="fetchData" @add="handleCreate" />
            </template>
          </vxe-table>
        </div>

        <!-- 分页 -->
        <div class="pager-wrapper">
          <vxe-pager
            :current-page="pagination.current"
            :page-size="pagination.pageSize"
            :total="pagination.total"
            :layouts="['PrevPage', 'JumpNumber', 'NextPage', 'FullJump', 'Sizes', 'Total']"
            :page-sizes="[10, 20, 50, 100]"
            @page-change="handlePageChange"
          />
        </div>
      </div>
    </template>
  </PageContainer>
  </ErrorBoundary>

  <!-- ════════════════════════════════════════════════ -->
  <!-- 新建批次弹窗 -->
  <!-- ════════════════════════════════════════════════ -->
  <a-modal
    v-model:open="createVisible"
    title="新建批次"
    width="620px"
    :confirm-loading="createLoading"
    @ok="handleCreateSubmit"
    @cancel="handleCreateCancel"
  >
    <a-form ref="createFormRef" :model="createForm" :rules="createRules" layout="vertical">
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="批次号" name="batchNo">
            <a-input size="small" v-model:value="createForm.batchNo" placeholder="请输入批次号" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="商品" name="productId">
            <a-input
              v-model:value="createForm.productName"
              placeholder="请选择商品"
              readonly
            >
              <template #addonAfter>
                <a-button type="link" size="small" @click="handleSelectProduct">选择</a-button>
              </template>
            </a-input>
          </a-form-item>
        </a-col>
      </a-row>
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="商品编码">
            <a-input size="small" v-model:value="createForm.productCode" disabled />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="总数量" name="totalQuantity">
            <a-input-number v-model:value="createForm.totalQuantity" :min="0" :precision="0" style="width: 100%" placeholder="请输入数量" />
          </a-form-item>
        </a-col>
      </a-row>
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="生产日期" name="productionDate">
            <a-date-picker size="small" v-model:value="createForm.productionDate" style="width: 100%" value-format="YYYY-MM-DD" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="到期日期" name="expirationDate">
            <a-date-picker size="small" v-model:value="createForm.expirationDate" style="width: 100%" value-format="YYYY-MM-DD" />
          </a-form-item>
        </a-col>
      </a-row>
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="来源类型" name="sourceType">
            <a-select size="small" v-model:value="createForm.sourceType" placeholder="请选择" allow-clear>
              <a-select-option value="PURCHASE">采购入库</a-select-option>
              <a-select-option value="PRODUCTION">生产入库</a-select-option>
              <a-select-option value="TRANSFER">调拨入库</a-select-option>
              <a-select-option value="RETURN">退货入库</a-select-option>
              <a-select-option value="OTHER">其他</a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="来源单号">
            <a-input v-model:value="createForm.sourceRefNo" placeholder="请输入来源单号" />
          </a-form-item>
        </a-col>
      </a-row>
    </a-form>
  </a-modal>

  <!-- ════════════════════════════════════════════════ -->
  <!-- 批次入库弹窗 -->
  <!-- ════════════════════════════════════════════════ -->
  <a-modal
    v-model:open="inboundVisible"
    title="批次入库"
    width="520px"
    :confirm-loading="inboundLoading"
    @ok="handleInboundSubmit"
    @cancel="handleInboundCancel"
  >
    <a-form ref="inboundFormRef" :model="inboundForm" :rules="inboundRules" layout="vertical">
      <a-descriptions bordered size="small" :column="2" style="margin-bottom: 16px;">
        <a-descriptions-item label="批次号" :span="2">{{ currentRecord?.batchNo }}</a-descriptions-item>
        <a-descriptions-item label="商品编码">{{ currentRecord?.productCode }}</a-descriptions-item>
        <a-descriptions-item label="商品名称">{{ currentRecord?.productName }}</a-descriptions-item>
      </a-descriptions>
      <a-form-item label="仓库" name="warehouseId">
        <a-select size="small" v-model:value="inboundForm.warehouseId" placeholder="请选择仓库" show-search :filter-option="filterWarehouseOption">
          <a-select-option v-for="w in warehouseOptions" :key="w.id" :value="w.id">
            {{ w.warehouseName }}
          </a-select-option>
        </a-select>
      </a-form-item>
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="入库数量" name="quantity">
            <a-input-number v-model:value="inboundForm.quantity" :min="1" :precision="0" style="width: 100%" placeholder="请输入数量" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="库位编号">
            <a-input v-model:value="inboundForm.locationName" placeholder="库位编号（可选）" />
          </a-form-item>
        </a-col>
      </a-row>
    </a-form>
  </a-modal>

  <!-- ════════════════════════════════════════════════ -->
  <!-- 批次出库弹窗 -->
  <!-- ════════════════════════════════════════════════ -->
  <a-modal
    v-model:open="outboundVisible"
    title="批次出库"
    width="520px"
    :confirm-loading="outboundLoading"
    @ok="handleOutboundSubmit"
    @cancel="handleOutboundCancel"
  >
    <a-form ref="outboundFormRef" :model="outboundForm" :rules="outboundRules" layout="vertical">
      <a-descriptions bordered size="small" :column="2" style="margin-bottom: 16px;">
        <a-descriptions-item label="批次号" :span="2">{{ currentRecord?.batchNo }}</a-descriptions-item>
        <a-descriptions-item label="商品编码">{{ currentRecord?.productCode }}</a-descriptions-item>
        <a-descriptions-item label="商品名称">{{ currentRecord?.productName }}</a-descriptions-item>
        <a-descriptions-item label="可用数量">
          <span :class="getQuantityClass(currentRecord)">{{ currentRecord?.availableQuantity ?? 0 }}</span>
        </a-descriptions-item>
      </a-descriptions>
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="出库数量" name="quantity">
            <a-input-number
              v-model:value="outboundForm.quantity"
              :min="1"
              :max="currentRecord?.availableQuantity || 999999"
              :precision="0"
              style="width: 100%"
              placeholder="请输入出库数量"
            />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="库位编号">
            <a-input v-model:value="outboundForm.locationName" placeholder="库位编号（可选）" />
          </a-form-item>
        </a-col>
      </a-row>
    </a-form>
  </a-modal>

  <!-- ════════════════════════════════════════════════ -->
  <!-- 质量检验弹窗 -->
  <!-- ════════════════════════════════════════════════ -->
  <a-modal
    v-model:open="inspectionVisible"
    title="质量检验"
    width="520px"
    :confirm-loading="inspectionLoading"
    @ok="handleInspectionSubmit"
    @cancel="handleInspectionCancel"
  >
    <a-form ref="inspectionFormRef" :model="inspectionForm" :rules="inspectionRules" layout="vertical">
      <a-descriptions bordered size="small" :column="2" style="margin-bottom: 16px;">
        <a-descriptions-item label="批次号" :span="2">{{ currentRecord?.batchNo }}</a-descriptions-item>
        <a-descriptions-item label="商品编码">{{ currentRecord?.productCode }}</a-descriptions-item>
        <a-descriptions-item label="商品名称">{{ currentRecord?.productName }}</a-descriptions-item>
        <a-descriptions-item label="当前质量状态">
          <StatusTag v-if="currentRecord?.qualityStatus" :status="currentRecord.qualityStatus" :map="QUALITY_STATUS" />
          <span v-else>-</span>
        </a-descriptions-item>
      </a-descriptions>
      <a-form-item label="质检结果" name="status">
        <a-select v-model:value="inspectionForm.status" placeholder="请选择质检结果">
          <a-select-option value="NORMAL">合格（NORMAL）</a-select-option>
          <a-select-option value="QUARANTINED">隔离（QUARANTINED）</a-select-option>
          <a-select-option value="DEFECTIVE">不合格（DEFECTIVE）</a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item label="质检员" name="inspectorName">
        <a-input v-model:value="inspectionForm.inspectorName" placeholder="请输入质检员姓名" />
      </a-form-item>
    </a-form>
  </a-modal>

  <!-- ════════════════════════════════════════════════ -->
  <!-- 批次状态变更弹窗 -->
  <!-- ════════════════════════════════════════════════ -->
  <a-modal
    v-model:open="statusChangeVisible"
    title="批次状态变更"
    width="520px"
    :confirm-loading="statusChangeLoading"
    @ok="handleStatusChangeSubmit"
    @cancel="handleStatusChangeCancel"
  >
    <a-form ref="statusChangeFormRef" :model="statusChangeForm" :rules="statusChangeRules" layout="vertical">
      <a-descriptions bordered size="small" :column="2" style="margin-bottom: 16px;">
        <a-descriptions-item label="批次号" :span="2">{{ statusChangeForm.isBatch ? '（批量操作）' : currentRecord?.batchNo }}</a-descriptions-item>
        <a-descriptions-item label="商品编码" v-if="!statusChangeForm.isBatch">{{ currentRecord?.productCode }}</a-descriptions-item>
        <a-descriptions-item label="商品名称" v-if="!statusChangeForm.isBatch">{{ currentRecord?.productName }}</a-descriptions-item>
        <a-descriptions-item label="当前状态" v-if="!statusChangeForm.isBatch">
          <StatusTag :status="currentRecord?.batchStatus" :map="BATCH_STATUS" />
        </a-descriptions-item>
        <a-descriptions-item label="涉及批次" v-if="statusChangeForm.isBatch">
          <a-tag color="blue">{{ selectedIds.length }} 个批次</a-tag>
        </a-descriptions-item>
      </a-descriptions>
      <a-form-item label="目标状态" name="newStatus">
        <a-select v-model:value="statusChangeForm.newStatus" placeholder="请选择目标状态">
          <a-select-option value="ACTIVE">启用（ACTIVE）</a-select-option>
          <a-select-option value="QUARANTINED">隔离（QUARANTINED）</a-select-option>
          <a-select-option value="EXPIRED">过期（EXPIRED）</a-select-option>
          <a-select-option value="CANCELLED">取消（CANCELLED）</a-select-option>
        </a-select>
      </a-form-item>
    </a-form>
  </a-modal>

  <!-- ════════════════════════════════════════════════ -->
  <!-- 批次详情弹窗 -->
  <!-- ════════════════════════════════════════════════ -->
  <a-modal
    v-model:open="detailVisible"
    title="批次详情"
    width="740px"
    :footer="null"
  >
    <div style="text-align: right; margin-bottom: 12px;">
      <PrintButton :record="detailData" business-type="BATCH" button-size="small" />
    </div>
    <a-spin :spinning="detailLoading">
      <template v-if="detailData">
        <a-descriptions bordered :column="2" size="small">
          <a-descriptions-item label="批次号" :span="2">{{ detailData.batchNo }}</a-descriptions-item>
          <a-descriptions-item label="商品编码">{{ detailData.productCode }}</a-descriptions-item>
          <a-descriptions-item label="商品名称">{{ detailData.productName }}</a-descriptions-item>
          <a-descriptions-item label="规格">{{ detailData.specification || '-' }}</a-descriptions-item>
          <a-descriptions-item label="单位">{{ detailData.unit || '-' }}</a-descriptions-item>
          <a-descriptions-item label="总数量">{{ detailData.totalQuantity }}</a-descriptions-item>
          <a-descriptions-item label="可用数量">{{ detailData.availableQuantity }}</a-descriptions-item>
          <a-descriptions-item label="仓库">{{ detailData.warehouseName || '-' }}</a-descriptions-item>
          <a-descriptions-item label="生产日期">{{ detailData.productionDate || '-' }}</a-descriptions-item>
          <a-descriptions-item label="到期日期">
            <span :class="getExpiryClass(detailData)">{{ detailData.expirationDate || '-' }}</span>
          </a-descriptions-item>
          <a-descriptions-item label="批次状态">
            <StatusTag :status="detailData.batchStatus" :map="BATCH_STATUS" />
          </a-descriptions-item>
          <a-descriptions-item label="质量状态">
            <StatusTag v-if="detailData.qualityStatus" :status="detailData.qualityStatus" :map="QUALITY_STATUS" />
            <span v-else>-</span>
          </a-descriptions-item>
          <a-descriptions-item label="来源类型">{{ sourceTypeLabel(detailData.sourceType) }}</a-descriptions-item>
          <a-descriptions-item label="来源单号">{{ detailData.sourceRefNo || '-' }}</a-descriptions-item>
          <a-descriptions-item label="质检员">{{ detailData.qualityInspectorName || '-' }}</a-descriptions-item>
          <a-descriptions-item label="质检日期">{{ detailData.qualityInspectionDate || '-' }}</a-descriptions-item>
          <a-descriptions-item label="创建人">{{ detailData.createdByName || '-' }}</a-descriptions-item>
          <a-descriptions-item label="创建时间">{{ detailData.createdAt || '-' }}</a-descriptions-item>
        </a-descriptions>
      </template>
      <a-empty v-else description="暂无数据" />
    </a-spin>
  </a-modal>

  <!-- ════════════════════════════════════════════════ -->
  <!-- 商品选择弹窗 -->
  <!-- ════════════════════════════════════════════════ -->
  <a-modal
    v-model:open="productSelectVisible"
    title="选择商品"
    width="700px"
    :footer="null"
    destroy-on-close
  >
    <a-input-search
      v-model:value="productSearchKeyword"
      placeholder="搜索商品编码 / 名称"
      style="margin-bottom: 12px"
      @search="handleProductSearch"
    />
    <a-table
      :dataSource="productOptions"
      :columns="productColumns"
      :pagination="{ pageSize: 5, showSizeChanger: false }"
      :loading="productLoading"
      size="small"
      row-key="id"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'action'">
          <a-button type="link" size="small" @click="selectProduct(record)">选择</a-button>
        </template>
      </template>
    </a-table>
  </a-modal>
</template>

<script setup lang="ts">
/**
 * 批次管理 - 批次号列表、入库、出库、质检、状态变更
 */
import { ref, reactive, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import dayjs from 'dayjs'
import {
  PlusOutlined,
  ReloadOutlined,
  SwapOutlined,
  SyncOutlined,
  DownloadOutlined
} from '@ant-design/icons-vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import SearchBar from '@/components/SearchBar/SearchBar.vue'
import EmptyState from '@/components/EmptyState/EmptyState.vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import StatusTag from '@/components/StatusTag/StatusTag.vue'
import { BATCH_STATUS, QUALITY_STATUS } from '@/utils/statusConfig'
import type { SearchField } from '@/components/SearchBar/SearchBar.vue'
import { batchApi, type BatchNumber } from '@/api/erp/batch'
import request from '@/utils/request'
import PrintButton from '@/components/business/print-button/PrintButton.vue'
import { stockApi } from '@/api/erp'

// ════════════════════════════════════════════════════════════════
// 常量
// ════════════════════════════════════════════════════════════════

/** 来源类型 -> 中文标签 */
const SOURCE_TYPE_LABEL: Record<string, string> = {
  PURCHASE: '采购入库',
  PRODUCTION: '生产入库',
  TRANSFER: '调拨入库',
  RETURN: '退货入库',
  OTHER: '其他',
  SALE: '销售出库'
}

// ════════════════════════════════════════════════════════════════
// 防抖工具
// ════════════════════════════════════════════════════════════════

const debounceMap = new Map<string, number>()

function debounceClick(key: string, fn: () => void, delay = 300) {
  const now = Date.now()
  const last = debounceMap.get(key) || 0
  if (now - last < delay) return
  debounceMap.set(key, now)
  fn()
}

// ════════════════════════════════════════════════════════════════
// 工具函数
// ════════════════════════════════════════════════════════════════

function sourceTypeLabel(type: string | undefined): string {
  return SOURCE_TYPE_LABEL[type ?? ''] || type || '-'
}

/** 计算到期剩余天数 */
function getDaysRemaining(record: { expirationDate?: string }): number {
  if (!record.expirationDate) return Infinity
  return dayjs(record.expirationDate).startOf('day').diff(dayjs().startOf('day'), 'day')
}

/** 获取到期日期的样式类 */
function getExpiryClass(record: { expirationDate?: string }): Record<string, boolean> {
  const days = getDaysRemaining(record)
  return {
    'expiry-warning': days > 0 && days <= 30,
    'expiry-danger': days <= 0
  }
}

/** 获取库存数量样式类 */
function handleError(err: any) {
  console.warn("[批次管理] ErrorBoundary 捕获异常:", err)
}

function getQuantityClass(record: { availableQuantity?: number } | null): Record<string, boolean> {
  return {
    'quantity-zero': (record?.availableQuantity ?? 0) <= 0
  }
}

// ════════════════════════════════════════════════════════════════
// 响应式状态
// ════════════════════════════════════════════════════════════════

const router = useRouter()
const loading = ref(false)
const lastUpdateTime = ref('')
const hasError = ref(false)
const hasSelected = ref(false)

const autoRefreshCountdown = ref(0)
let countdownTimer: ReturnType<typeof setInterval> | null = null
let refreshTimer: ReturnType<typeof setInterval> | null = null

const statistics = ref({ total: 0, active: 0, quarantined: 0, expired: 0 })
const selectedIds = ref<number[]>([])
const activeTab = ref('batch')
const tableData = ref<BatchNumber[]>([])
const expiringBatches = ref<BatchNumber[]>([])
const expiryLoading = ref(false)
const expiryCollapseKey = ref<string[]>(['expiry'])
const tableWrapperRef = ref<HTMLDivElement | null>(null)
const tableHeight = ref(500)
const currentRecord = ref<BatchNumber | null>(null)

// Form refs
const createFormRef = ref<any>(null)
const inboundFormRef = ref<any>(null)
const outboundFormRef = ref<any>(null)
const inspectionFormRef = ref<any>(null)
const statusChangeFormRef = ref<any>(null)

const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0
})

// ════════════════════════════════════════════════════════════════
// 搜索字段定义
// ════════════════════════════════════════════════════════════════

const searchForm = reactive<Record<string, any>>({
  batchNo: '',
  productCode: '',
  warehouse: '',
  status: undefined
})

const searchFields: any = [
  {
    name: 'batchNo',
    label: '批次号',
    type: 'input',
    placeholder: '请输入批次号'
  },
  {
    name: 'productCode',
    label: '商品编码',
    type: 'input',
    placeholder: '请输入商品编码'
  },
  {
    name: 'warehouse',
    label: '仓库',
    type: 'input',
    placeholder: '请输入仓库名称'
  },
  {
    name: 'status',
    label: '批次状态',
    type: 'select',
    placeholder: '请选择状态',
    options: [
      { label: '启用', value: 'ACTIVE' },
      { label: '过期', value: 'EXPIRED' },
      { label: '隔离', value: 'QUARANTINED' },
      { label: '取消', value: 'CANCELLED' }
    ]
  }
]

// ════════════════════════════════════════════════════════════════
// 弹窗状态
// ════════════════════════════════════════════════════════════════

// 新建批次
const createVisible = ref(false)
const createLoading = ref(false)
const createForm = reactive<Record<string, any>>({
  batchNo: '',
  productCode: '',
  productName: '',
  productId: undefined,
  totalQuantity: undefined,
  productionDate: undefined,
  expirationDate: undefined,
  sourceType: undefined,
  sourceRefNo: ''
})
const createRules: Record<string, any[]> = {
  batchNo: [{ required: true, message: '请输入批次号' }],
  totalQuantity: [{ required: true, message: '请输入数量' }]
}

// 入库
const inboundVisible = ref(false)
const inboundLoading = ref(false)
const inboundForm = reactive({
  warehouseId: undefined as number | undefined,
  quantity: undefined as number | undefined,
  locationName: ''
})
const inboundRules: Record<string, any[]> = {
  warehouseId: [{ required: true, message: '请选择仓库' }],
  quantity: [{ required: true, message: '请输入入库数量' }]
}

// 出库
const outboundVisible = ref(false)
const outboundLoading = ref(false)
const outboundForm = reactive({
  quantity: undefined as number | undefined,
  locationName: ''
})
const outboundRules: Record<string, any[]> = {
  quantity: [{ required: true, message: '请输入出库数量' }]
}

// 质检
const inspectionVisible = ref(false)
const inspectionLoading = ref(false)
const inspectionForm = reactive({
  status: undefined as string | undefined,
  inspectorName: ''
})
const inspectionRules: Record<string, any[]> = {
  status: [{ required: true, message: '请选择质检结果' }],
  inspectorName: [{ required: true, message: '请输入质检员姓名' }]
}

// 状态变更
const statusChangeVisible = ref(false)
const statusChangeLoading = ref(false)
const statusChangeForm = reactive({
  newStatus: undefined as string | undefined,
  isBatch: false
})
const statusChangeRules: Record<string, any[]> = {
  newStatus: [{ required: true, message: '请选择目标状态' }]
}

// 详情
const detailVisible = ref(false)
const detailLoading = ref(false)
const tableRef = ref<any>(null)
const detailData = ref<BatchNumber | null>(null)

// 商品选择
const productSelectVisible = ref(false)
const productSearchKeyword = ref('')
const productLoading = ref(false)
const productOptions = ref<any[]>([])

const productColumns: any = [
  { title: '商品编码', dataIndex: 'productCode', width: 130 },
  { title: '商品名称', dataIndex: 'productName', width: 180 },
  { title: '规格', dataIndex: 'specification', width: 120 },
  { title: '单位', dataIndex: 'unit', width: 60 },
  { title: '操作', dataIndex: 'action', width: 80 }
]

// 仓库选项
const warehouseOptions = ref<any[]>([])

async function loadWarehouseOptions() {
  try {
    const res = await stockApi.getWarehouses()
    warehouseOptions.value = Array.isArray(res) ? res : (res?.data || [])
  } catch { /* ignore */ }
}

function filterWarehouseOption(input: string, option: any) {
  return (option.children?.toString() || '').toLowerCase().includes(input.toLowerCase())
}

// ════════════════════════════════════════════════════════════════
// 临期预警表格列
// ════════════════════════════════════════════════════════════════

const expiryColumns: any = [
  { title: '批次号', dataIndex: 'batchNo', width: 150 },
  { title: '商品编码', dataIndex: 'productCode', width: 120 },
  { title: '商品名称', dataIndex: 'productName', width: 150 },
  { title: '仓库', dataIndex: 'warehouseName', width: 120 },
  { title: '到期日期', dataIndex: 'expirationDate', width: 110 },
  { title: '剩余天数', dataIndex: 'daysRemaining', width: 90 },
  { title: '批次状态', dataIndex: 'batchStatus', width: 90 }
]

// ════════════════════════════════════════════════════════════════
// 表格高度自适应
// ════════════════════════════════════════════════════════════════

function updateTableHeight() {
  if (!tableWrapperRef.value) {
    tableHeight.value = Math.max(300, window.innerHeight - 350)
    return
  }
  const wrapperRect = tableWrapperRef.value.getBoundingClientRect()
  const windowHeight = window.innerHeight
  // 可用高度 = 窗口高度 - 表格容器顶部偏移 - 分页器(48) - 内边距(12)
  const available = windowHeight - wrapperRect.top - 48 - 12
  tableHeight.value = Math.max(300, available)
}

// ════════════════════════════════════════════════════════════════
// 数据获取
// ════════════════════════════════════════════════════════════════

async function fetchData() {
  hasError.value = false
  loading.value = true
  try {
    const params: Record<string, any> = {
      pageNum: pagination.current,
      pageSize: pagination.pageSize
    }
    if (searchForm.batchNo) params.batchNo = searchForm.batchNo
    if (searchForm.productCode) params.productCode = searchForm.productCode
    if (searchForm.status) params.status = searchForm.status
    if (searchForm.warehouse) params.warehouseName = searchForm.warehouse

    const res = await batchApi.page(params)
    tableData.value = res.records || []
    pagination.total = res.total || 0
    // 统计
    const stats = { total: tableData.value.length, active: 0, quarantined: 0, expired: 0 }
    tableData.value.forEach((r) => {
      if (r.batchStatus === 'ACTIVE') stats.active++
      else if (r.batchStatus === 'QUARANTINED') stats.quarantined++
      else if (r.batchStatus === 'EXPIRED') stats.expired++
    })
    statistics.value = stats
    lastUpdateTime.value = new Date().toLocaleString('zh-CN')
    // 清除选择状态
    selectedIds.value = []
    hasSelected.value = false
  } catch (err: any) {
    hasError.value = true
    console.warn('[批次管理] 加载数据失败', err)
    message.error(err?.message || '加载批次数据失败，请稍后重试')
  } finally {
    loading.value = false
    await nextTick()
    updateTableHeight()
  }
}

async function fetchExpiringBatches() {
  expiryLoading.value = true
  try {
    const res = await batchApi.getExpiringBatches(30)
    expiringBatches.value = Array.isArray(res) ? res : []
  } catch (err: any) {
    console.warn('[批次管理] 获取临期预警失败', err)
    expiringBatches.value = []
  } finally {
    expiryLoading.value = false
  }
}

// ════════════════════════════════════════════════════════════════
// 搜索处理
// ════════════════════════════════════════════════════════════════

function handleSearch(formData: Record<string, any>) {
  Object.assign(searchForm, formData)
  pagination.current = 1
  fetchData()
}

function handleReset() {
  Object.assign(searchForm, {
    batchNo: '',
    productCode: '',
    warehouse: '',
    status: undefined
  })
  pagination.current = 1
  fetchData()
}

// ════════════════════════════════════════════════════════════════
// 分页 / 选择 / Tab
// ════════════════════════════════════════════════════════════════

function handlePageChange({ currentPage, pageSize }: { currentPage: number; pageSize: number }) {
  pagination.current = currentPage
  pagination.pageSize = pageSize
  fetchData()
}

function onSelectionChange() {
  const $table = tableRef.value
  if (!$table) return
  const records = $table.getCheckboxRecords() || []
  selectedIds.value = records.map((r: BatchNumber) => r.id)
  hasSelected.value = records.length > 0
}

function handleTabChange(key: string) {
  if (key === 'serial') {
    router.push('/erp/serial')
  }
}

// ════════════════════════════════════════════════════════════════
// 详情
// ════════════════════════════════════════════════════════════════

async function handleDetail(record: BatchNumber) {
  detailVisible.value = true
  detailLoading.value = true
  detailData.value = null
  try {
    const res = await batchApi.getById(record.id)
    detailData.value = res
  } catch (err: any) {
    console.warn('[批次管理] 获取详情失败', err)
    // 降级使用行数据
    detailData.value = { ...record }
    message.warning('获取详情失败，显示表格中的部分数据')
  } finally {
    detailLoading.value = false
  }
}

// ════════════════════════════════════════════════════════════════
// 新建批次
// ════════════════════════════════════════════════════════════════

function handleCreate() {
  Object.assign(createForm, {
    batchNo: '',
    productCode: '',
    productName: '',
    productId: undefined,
    totalQuantity: undefined,
    productionDate: undefined,
    expirationDate: undefined,
    sourceType: undefined,
    sourceRefNo: ''
  })
  createVisible.value = true
  nextTick(() => createFormRef.value?.resetFields?.())
}

async function handleCreateSubmit() {
  try {
    await createFormRef.value?.validate()
  } catch {
    return
  }
  createLoading.value = true
  try {
    const payload: Record<string, any> = {
      batchNo: createForm.batchNo,
      productCode: createForm.productCode,
      productId: createForm.productId,
      totalQuantity: createForm.totalQuantity,
      sourceType: createForm.sourceType || 'OTHER',
      sourceRefNo: createForm.sourceRefNo || undefined,
      productionDate: createForm.productionDate || undefined,
      expirationDate: createForm.expirationDate || undefined
    }
    await batchApi.create(payload)
    message.success('批次创建成功')
    createVisible.value = false
    fetchData()
    fetchExpiringBatches()
  } catch (err: any) {
    console.warn('[批次管理] 创建批次失败', err)
    message.error(err?.message || '创建失败，请稍后重试')
  } finally {
    createLoading.value = false
  }
}

function handleCreateCancel() {
  createVisible.value = false
}

// ════════════════════════════════════════════════════════════════
// 入库
// ════════════════════════════════════════════════════════════════

function handleInbound(record: BatchNumber) {
  currentRecord.value = record
  inboundForm.warehouseId = record.warehouseId
  inboundForm.quantity = undefined
  inboundForm.locationName = ''
  inboundVisible.value = true
  nextTick(() => inboundFormRef.value?.resetFields?.())
}

async function handleInboundSubmit() {
  try {
    await inboundFormRef.value?.validate()
  } catch {
    return
  }
  if (!currentRecord.value) return
  inboundLoading.value = true
  try {
    const warehouse = warehouseOptions.value.find((w: any) => w.id === inboundForm.warehouseId)
    await batchApi.inbound(
      currentRecord.value,
      inboundForm.warehouseId!,
      warehouse?.warehouseName || '',
      undefined
    )
    message.success('入库成功')
    inboundVisible.value = false
    fetchData()
    fetchExpiringBatches()
  } catch (err: any) {
    console.warn('[批次管理] 入库失败', err)
    message.error(err?.message || '入库失败，请稍后重试')
  } finally {
    inboundLoading.value = false
  }
}

function handleInboundCancel() {
  inboundVisible.value = false
}

// ════════════════════════════════════════════════════════════════
// 出库
// ════════════════════════════════════════════════════════════════

function handleOutbound(record: BatchNumber) {
  if ((record.availableQuantity ?? 0) <= 0) {
    message.warning('该批次无可用库存，无法出库')
    return
  }
  currentRecord.value = record
  outboundForm.quantity = undefined
  outboundForm.locationName = ''
  outboundVisible.value = true
  nextTick(() => outboundFormRef.value?.resetFields?.())
}

async function handleOutboundSubmit() {
  try {
    await outboundFormRef.value?.validate()
  } catch {
    return
  }
  if (!currentRecord.value) return
  if ((outboundForm.quantity ?? 0) > (currentRecord.value.availableQuantity ?? 0)) {
    message.error('出库数量不能超过可用数量')
    return
  }
  outboundLoading.value = true
  try {
    await batchApi.outbound(
      currentRecord.value.id,
      outboundForm.quantity!,
      currentRecord.value.warehouseId ?? 0,
      undefined
    )
    message.success('出库成功')
    outboundVisible.value = false
    fetchData()
    fetchExpiringBatches()
  } catch (err: any) {
    console.warn('[批次管理] 出库失败', err)
    message.error(err?.message || '出库失败，请稍后重试')
  } finally {
    outboundLoading.value = false
  }
}

function handleOutboundCancel() {
  outboundVisible.value = false
}

// ════════════════════════════════════════════════════════════════
// 质检
// ════════════════════════════════════════════════════════════════

function handleQualityInspection(record: BatchNumber) {
  currentRecord.value = record
  inspectionForm.status = undefined
  inspectionForm.inspectorName = ''
  inspectionVisible.value = true
  nextTick(() => inspectionFormRef.value?.resetFields?.())
}

async function handleInspectionSubmit() {
  try {
    await inspectionFormRef.value?.validate()
  } catch {
    return
  }
  if (!currentRecord.value) return
  inspectionLoading.value = true
  try {
    await batchApi.qualityInspection(
      currentRecord.value.id,
      inspectionForm.status!,
      inspectionForm.inspectorName,
      inspectionForm.inspectorName
    )
    message.success('质检完成')
    inspectionVisible.value = false
    fetchData()
    fetchExpiringBatches()
  } catch (err: any) {
    console.warn('[批次管理] 质检失败', err)
    message.error(err?.message || '质检提交失败，请稍后重试')
  } finally {
    inspectionLoading.value = false
  }
}

function handleInspectionCancel() {
  inspectionVisible.value = false
}

// ════════════════════════════════════════════════════════════════
// 状态变更（单条 / 批量）
// ════════════════════════════════════════════════════════════════

function handleStatusChange(record: BatchNumber) {
  currentRecord.value = record
  statusChangeForm.newStatus = undefined
  statusChangeForm.isBatch = false
  statusChangeVisible.value = true
  nextTick(() => statusChangeFormRef.value?.resetFields?.())
}

function handleBatchStatusChange() {
  if (selectedIds.value.length === 0) {
    message.warning('请先选择要操作的批次')
    return
  }
  statusChangeForm.newStatus = undefined
  statusChangeForm.isBatch = true
  statusChangeVisible.value = true
  nextTick(() => statusChangeFormRef.value?.resetFields?.())
}

async function handleStatusChangeSubmit() {
  try {
    await statusChangeFormRef.value?.validate()
  } catch {
    return
  }
  if (!statusChangeForm.newStatus) return

  const ids = statusChangeForm.isBatch
    ? selectedIds.value
    : (currentRecord.value ? [currentRecord.value.id] : [])

  if (ids.length === 0) {
    message.warning('没有可操作的批次')
    return
  }

  // 单条时校验状态无变化
  if (!statusChangeForm.isBatch && currentRecord.value) {
    if (currentRecord.value.batchStatus === statusChangeForm.newStatus) {
      message.warning('目标状态与当前状态相同')
      return
    }
  }

  statusChangeLoading.value = true
  try {
    await batchApi.updateStatus(ids, statusChangeForm.newStatus)
    message.success(`状态变更成功（${ids.length} 个批次）`)
    statusChangeVisible.value = false
    fetchData()
    fetchExpiringBatches()
  } catch (err: any) {
    console.warn('[批次管理] 状态变更失败', err)
    message.error(err?.message || '状态变更失败，请稍后重试')
  } finally {
    statusChangeLoading.value = false
  }
}

function handleStatusChangeCancel() {
  statusChangeVisible.value = false
}

// ════════════════════════════════════════════════════════════════
// 商品选择
// ════════════════════════════════════════════════════════════════

function handleSelectProduct() {
  productSelectVisible.value = true
  productSearchKeyword.value = ''
  productOptions.value = []
  handleProductSearch()
}

async function handleProductSearch() {
  productLoading.value = true
  try {
    const res = await request.get('/erp/product/list', {
      params: { keyword: productSearchKeyword.value || undefined, pageSize: 50 }
    })
    const data = res?.data ?? res
    productOptions.value = Array.isArray(data) ? data : []
  } catch (err: any) {
    console.warn('[批次管理] 加载商品列表失败', err)
    productOptions.value = []
  } finally {
    productLoading.value = false
  }
}

function selectProduct(product: any) {
  createForm.productId = product.id
  createForm.productCode = product.productCode
  createForm.productName = product.productName
  productSelectVisible.value = false
}

// ════════════════════════════════════════════════════════════════
// 导出 / 删除
// ════════════════════════════════════════════════════════════════

async function handleExport() {
  try {
    const blob = await request.get('/erp/batch-sn/batches/export', { responseType: 'blob' })
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a'); a.href = url; a.download = `批次_${new Date().toISOString().slice(0, 10)}.xlsx`; a.click()
    window.URL.revokeObjectURL(url); message.success('导出成功')
  } catch {
    message.warning('导出失败')
  }
}

function handleDelete(row: BatchNumber) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除批次 "${row.batchNo}" 吗？此操作不可恢复。`,
    okText: '确认删除',
    okType: 'danger',
    onOk: async () => {
      try {
        await request.delete(`/erp/batch-sn/batches/${row.id}`)
        message.success('删除成功')
        fetchData()
      } catch (err: any) {
        message.error(err?.message || '删除失败')
      }
    }
  })
}

// ════════════════════════════════════════════════════════════════
// 键盘快捷键
// ════════════════════════════════════════════════════════════════

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'F5') {
    e.preventDefault()
    debounceClick('refresh', fetchData)
    return
  }
  if ((e.ctrlKey || e.metaKey) && e.key === 'n') {
    e.preventDefault()
    handleCreate()
  }
  if ((e.ctrlKey || e.metaKey) && e.key === 'e') {
    e.preventDefault()
    debounceClick('export', handleExport)
  }
}

// ════════════════════════════════════════════════════════════════
// 生命周期
// ════════════════════════════════════════════════════════════════

onMounted(() => {
  window.addEventListener('keydown', handleKeydown)
  window.addEventListener('resize', updateTableHeight)
  fetchData()
  fetchExpiringBatches()
  loadWarehouseOptions()
  nextTick(updateTableHeight)
  autoRefreshCountdown.value = 300
  refreshTimer = setInterval(() => {
    fetchData()
    autoRefreshCountdown.value = 300
  }, 300000)
  countdownTimer = setInterval(() => {
    if (autoRefreshCountdown.value > 0) autoRefreshCountdown.value--
  }, 1000)
})

onUnmounted(() => {
  window.removeEventListener('keydown', handleKeydown)
  window.removeEventListener('resize', updateTableHeight)
  if (refreshTimer) { clearInterval(refreshTimer); refreshTimer = null }
  if (countdownTimer) { clearInterval(countdownTimer); countdownTimer = null }
})
</script>

<style scoped>
/* ── 页面头部 ── */
.batch-page-header {
  width: 100%;
}

.batch-page-header-inner {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.batch-header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
  padding-top: 4px;
}

.data-status {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.auto-refresh-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #909399;
  padding: 2px 8px;
  border-radius: 4px;
  background: #f5f7fa;
  user-select: none;
}

.update-time {
  font-size: 12px;
  color: #999;
  white-space: nowrap;
}

.stat-card {
  background: #fff;
  border-radius: 8px;
  padding: 14px 16px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
}

.stat-value {
  font-size: 22px;
  font-weight: 700;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, 'Courier New', monospace;
  line-height: 1.2;
}

.stat-label {
  font-size: 13px;
  color: #666;
  margin-top: 4px;
}

.batch-tabs-wrapper {
  margin-top: -4px;
}

.batch-tabs-wrapper :deep(.ant-tabs-nav) {
  margin-bottom: 0;
}

/* ── 主体区域 ── */
.batch-body {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
  padding: 16px;
  gap: 12px;
}

/* ── 操作栏 ── */
.action-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-shrink: 0;
}

.action-bar-hint {
  font-size: 13px;
  color: #909399;
}

/* ── 临期预警折叠面板 ── */
.expiry-collapse {
  flex-shrink: 0;
  background: #fff7e6;
  border: 1px solid #ffd591;
  border-radius: 4px;
}

.expiry-collapse :deep(.ant-collapse-header) {
  color: #d46b08;
  font-weight: 500;
}

.expiry-warning {
  color: #fa8c16;
  font-weight: 600;
}

.expiry-danger {
  color: #f5222d;
  font-weight: 600;
}

.quantity-zero {
  color: #f5222d;
  font-weight: 500;
}

/* ── 快捷键提示 ──────────────────────── */
.shortcut-hints {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #909399;
  user-select: none;
}
.shortcut-hint {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  padding: 1px 4px;
  border-radius: 3px;
  background: #f5f7fa;
}
.shortcut-hint kbd {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 3px;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 11px;
  color: #606266;
  background: #fff;
  border: 1px solid #d0d5dd;
  border-radius: 3px;
  box-shadow: 0 1px 0 #d0d5dd;
  line-height: 18px;
}

/* ── 表格区域 ── */
.table-wrapper {
  flex: 1;
  overflow: hidden;
  min-height: 0;
}

/* ── 分页 ── */
.pager-wrapper {
  flex-shrink: 0;
  display: flex;
  justify-content: flex-end;
  padding: 4px 0;
}

/* ── 紧凑尺寸覆盖 ── */
:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) {
  height: 28px;
  line-height: 28px;
}

:deep(.ant-select-single.ant-select-sm .ant-select-selector) {
  line-height: 26px;
}

:deep(.ant-input-number-sm input) {
  height: 26px;
}

/* ── vxe-table 微调 ── */
:deep(.vxe-table .vxe-table--render-default .vxe-body--column) {
  padding: 0 6px;
}
</style>
