<template>
  <div class="alert-rule-list">
    <!-- 搜索和操作栏 -->
    <div class="toolbar">
      <div class="search-area">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索规则名称/描述"
          clearable
          @clear="handleSearch"
          @keyup.enter="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        
        <el-select v-model="filterSeverity" placeholder="告警级别" clearable>
          <el-option label="严重" value="critical" />
          <el-option label="警告" value="warning" />
          <el-option label="信息" value="info" />
        </el-select>
        
        <el-select v-model="filterStatus" placeholder="规则状态" clearable>
          <el-option label="已启用" value="enabled" />
          <el-option label="已禁用" value="disabled" />
        </el-select>
        
        <el-select v-model="filterMetricType" placeholder="指标类型" clearable>
          <el-option-group label="订单指标">
            <el-option label="订单数量" value="ORDER_COUNT" />
            <el-option label="订单金额" value="ORDER_AMOUNT" />
            <el-option label="订单成功率" value="ORDER_SUCCESS_RATE" />
          </el-option-group>
          <el-option-group label="支付指标">
            <el-option label="支付笔数" value="PAYMENT_COUNT" />
            <el-option label="支付金额" value="PAYMENT_AMOUNT" />
            <el-option label="支付成功率" value="PAYMENT_SUCCESS_RATE" />
          </el-option-group>
          <el-option-group label="库存指标">
            <el-option label="库存总量" value="INVENTORY_TOTAL" />
            <el-option label="库存准确率" value="INVENTORY_ACCURACY" />
            <el-option label="库存预警数" value="INVENTORY_WARNING_COUNT" />
          </el-option-group>
          <el-option-group label="客户指标">
            <el-option label="活跃客户" value="CUSTOMER_ACTIVE" />
            <el-option label="客户满意度" value="CUSTOMER_SATISFACTION" />
          </el-option-group>
        </el-select>
        
        <el-button type="primary" @click="handleSearch">
          <el-icon><Search /></el-icon>搜索
        </el-button>
        <el-button @click="handleReset">
          <el-icon><RefreshRight /></el-icon>重置
        </el-button>
      </div>
      
      <div class="action-area">
        <el-button type="primary" @click="handleCreate">
          <el-icon><Plus /></el-icon>新建规则
        </el-button>
        <el-button @click="handleBatchEnable" :disabled="selectedRules.length === 0">
          <el-icon><Check /></el-icon>批量启用
        </el-button>
        <el-button @click="handleBatchDisable" :disabled="selectedRules.length === 0">
          <el-icon><Close /></el-icon>批量禁用
        </el-button>
        <el-button @click="handleBatchDelete" :disabled="selectedRules.length === 0" type="danger">
          <el-icon><Delete /></el-icon>批量删除
        </el-button>
      </div>
    </div>
    
    <!-- 统计卡片 -->
    <div class="statistics-cards">
      <el-card class="stat-card">
        <div class="stat-value">{{ statistics.total }}</div>
        <div class="stat-label">规则总数</div>
      </el-card>
      <el-card class="stat-card">
        <div class="stat-value" :class="{ 'text-success': statistics.enabled > 0 }">
          {{ statistics.enabled }}
        </div>
        <div class="stat-label">已启用</div>
      </el-card>
      <el-card class="stat-card">
        <div class="stat-value" :class="{ 'text-warning': statistics.disabled > 0 }">
          {{ statistics.disabled }}
        </div>
        <div class="stat-label">已禁用</div>
      </el-card>
      <el-card class="stat-card">
        <div class="stat-value" :class="{ 'text-danger': statistics.triggeredToday > 0 }">
          {{ statistics.triggeredToday }}
        </div>
        <div class="stat-label">今日触发</div>
      </el-card>
    </div>
    
    <!-- 规则列表 -->
    <el-table
      v-loading="loading"
      :data="ruleList"
      @selection-change="handleSelectionChange"
      stripe
      border
    >
      <el-table-column type="selection" width="55" />
      
      <el-table-column label="规则名称" min-width="180">
        <template #default="{ row }">
          <div class="rule-name">
            <el-icon :size="16" :color="getSeverityColor(row.severity)">
              <WarningFilled />
            </el-icon>
            <span>{{ row.name }}</span>
          </div>
          <div class="rule-description">{{ row.description }}</div>
        </template>
      </el-table-column>
      
      <el-table-column label="监控指标" width="150">
        <template #default="{ row }">
          <el-tag size="small">{{ getMetricLabel(row.metricType) }}</el-tag>
        </template>
      </el-table-column>
      
      <el-table-column label="告警条件" width="200">
        <template #default="{ row }">
          <span class="condition-text">
            {{ getMetricLabel(row.metricType) }} {{ getOperatorLabel(row.operator) }} {{ row.threshold }}{{ row.unit || '' }}
          </span>
          <div class="condition-duration">持续 {{ row.duration }} 分钟</div>
        </template>
      </el-table-column>
      
      <el-table-column label="级别" width="100">
        <template #default="{ row }">
          <el-tag :type="getSeverityType(row.severity)" size="small">
            {{ getSeverityLabel(row.severity) }}
          </el-tag>
        </template>
      </el-table-column>
      
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-switch
            v-model="row.status"
            :active-value="'enabled'"
            :inactive-value="'disabled'"
            @change="(val) => handleStatusChange(row, val)"
          />
        </template>
      </el-table-column>
      
      <el-table-column label="触发次数" width="100" align="center">
        <template #default="{ row }">
          <el-link type="primary" @click="handleViewHistory(row)">
            {{ row.triggerCount }}
          </el-link>
        </template>
      </el-table-column>
      
      <el-table-column label="最后触发" width="150">
        <template #default="{ row }">
          <span v-if="row.lastTriggeredAt">{{ formatTime(row.lastTriggeredAt) }}</span>
          <span v-else class="text-muted">-</span>
        </template>
      </el-table-column>
      
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link @click="handleEdit(row)">
            <el-icon><Edit /></el-icon>编辑
          </el-button>
          <el-button type="primary" link @click="handleTest(row)">
            <el-icon><VideoPlay /></el-icon>测试
          </el-button>
          <el-button type="danger" link @click="handleDelete(row)">
            <el-icon><Delete /></el-icon>删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    
    <!-- 分页 -->
    <div class="pagination-wrapper">
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="pagination.total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handlePageChange"
      />
    </div>
    
    <!-- 创建/编辑对话框 -->
    <AlertRuleDialog
      v-model:visible="dialogVisible"
      :rule="currentRule"
      @success="handleDialogSuccess"
    />
    
    <!-- 测试对话框 -->
    <AlertRuleTestDialog
      v-model:visible="testDialogVisible"
      :rule="currentRule"
    />
  </div>
</template>
