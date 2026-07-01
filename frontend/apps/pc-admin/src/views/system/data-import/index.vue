<template>
  <ErrorBoundary @error="handleError">
  <PageContainer full-height>
    <template #header>
      <div class="page-header">
        <div class="page-header-left">
          <a-breadcrumb>
            <a-breadcrumb-item><router-link to="/">首页</router-link></a-breadcrumb-item>
            <a-breadcrumb-item>设置</a-breadcrumb-item>
            <a-breadcrumb-item>数据导入</a-breadcrumb-item>
          </a-breadcrumb>
          <h2 class="page-title">数据导入</h2>
          <p class="page-desc">从文件批量导入数据，或配置外部系统自动同步</p>
        </div>
      </div>
    </template>

    <a-tabs v-model:activeKey="activeTab" class="data-import-tabs">
      <!-- ═══════════ Tab 1: 文件导入向导 ═══════════ -->
      <a-tab-pane key="file" tab="文件导入" force-render>
        <div class="wizard-container">
          <!-- 步骤指示器 -->
          <a-steps :current="currentStep" size="small" class="wizard-steps">
            <a-step title="选择类型" description="选择导入目标" />
            <a-step title="上传文件" description="选择导入文件" />
            <a-step title="字段映射" description="配置对应关系" />
            <a-step title="预览校验" description="确认数据正确" />
            <a-step title="导入完成" description="查看导入结果" />
          </a-steps>

          <div class="wizard-body">
            <!-- ──── Step 1: 选择数据类型 ──── -->
            <div v-show="currentStep === 0" class="step-content">
              <div class="step-header">
                <h3>选择要导入的数据类型</h3>
                <p class="step-desc">请选择需要导入的数据对象，系统将加载对应的字段定义</p>
              </div>
              <a-spin :spinning="dataTypesLoading">
                <a-row :gutter="[16, 16]">
                  <a-col :span="6" v-for="dt in dataTypes" :key="dt.type">
                    <a-card
                      hoverable
                      class="type-card"
                      :class="{ selected: selectedType === dt.type }"
                      @click="selectDataType(dt.type)"
                    >
                      <div class="type-card-body">
                        <div class="type-icon" :class="'icon-' + dt.type">
                          <component :is="getDataTypeIcon(dt.type)" />
                        </div>
                        <div class="type-info">
                          <div class="type-name">{{ dt.name }}</div>
                          <div class="type-desc">{{ dt.description }}</div>
                        </div>
                      </div>
                    </a-card>
                  </a-col>
                </a-row>
              </a-spin>

              <div v-if="selectedType" class="selected-type-info">
                <a-alert type="info" show-icon>
                  <template #message>
                    已选择 <strong>{{ getDataTypeName(selectedType) }}</strong>，
                    共 {{ fieldDefinitions.length }} 个字段
                    <template v-if="requiredFieldCount > 0">
                      （其中 {{ requiredFieldCount }} 个必填）
                    </template>
                  </template>
                  <template #description>
                    <a-button type="link" size="small" style="padding:0" @click.stop="downloadTemplate">
                      <template #icon><DownloadOutlined /></template>
                      下载导入模板
                    </a-button>
                  </template>
                </a-alert>
              </div>
            </div>

            <!-- ──── Step 2: 上传文件 ──── -->
            <div v-show="currentStep === 1" class="step-content">
              <div class="step-header">
                <h3>上传导入文件</h3>
                <p class="step-desc">支持 Excel (.xlsx, .xls) 和 CSV (.csv) 格式，文件大小不超过 10MB</p>
              </div>

              <a-upload-dragger
                :file-list="fileList"
                :before-upload="beforeUpload"
                :multiple="false"
                accept=".xlsx,.xls,.csv"
                class="upload-dragger"
              >
                <p class="ant-upload-drag-icon">
                  <InboxOutlined />
                </p>
                <p class="ant-upload-text">点击或拖拽文件到此区域</p>
                <p class="ant-upload-hint">
                  支持 .xlsx、.xls、.csv 格式，单次上传一个文件，最大 10MB
                </p>
              </a-upload-dragger>

              <div v-if="uploadedFile" class="file-info-card">
                <a-card size="small">
                  <div class="file-meta">
                    <FileExcelOutlined v-if="isExcelFile" class="file-icon excel" />
                    <FileTextOutlined v-else class="file-icon csv" />
                    <div class="file-detail">
                      <div class="file-name">{{ uploadedFile.name }}</div>
                      <div class="file-size">{{ formatFileSize(uploadedFile.size) }}</div>
                    </div>
                    <a-button type="text" danger size="small" @click="removeFile">
                      <DeleteOutlined />
                    </a-button>
                  </div>
                </a-card>
              </div>

              <div class="step-actions-left">
                <a-button @click="downloadTemplate" size="small">
                  <template #icon><DownloadOutlined /></template>
                  下载导入模板
                </a-button>
              </div>
            </div>

            <!-- ──── Step 3: 字段映射 ──── -->
            <div v-show="currentStep === 2" class="step-content">
              <div class="step-header">
                <h3>字段映射配置</h3>
                <div class="step-header-actions">
                  <a-button type="primary" ghost size="small" @click="autoMatchFields">
                    <template #icon><ThunderboltOutlined /></template>
                    智能匹配
                  </a-button>
                  <a-button size="small" @click="clearMapping">
                    <template #icon><ClearOutlined /></template>
                    清空映射
                  </a-button>
                </div>
              </div>
              <p class="step-desc">将文件中的列与系统字段对应，带 * 号的为必填字段</p>

              <!-- 映射统计 -->
              <div class="mapping-stats">
                <a-space>
                  <a-tag color="success">
                    <CheckCircleOutlined /> 已匹配: {{ mappedCount }}
                  </a-tag>
                  <a-tag color="warning">
                    <ExclamationCircleOutlined /> 待确认: {{ unmappedSourceCount }}
                  </a-tag>
                  <a-tag color="default">
                    <StopOutlined /> 忽略: {{ ignoredCount }}
                  </a-tag>
                </a-space>
              </div>

              <!-- 映射表 -->
              <div class="mapping-table-wrap">
                <a-table
                  :columns="mappingColumns"
                  :data-source="fieldMapping"
                  :pagination="false"
                  size="small"
                  :scroll="{ y: 400 }"
                  row-key="sourceField"
                  bordered
                >
                  <template #bodyCell="{ column, record, index }">
                    <template v-if="column.key === 'status'">
                      <CheckCircleFilled v-if="record.targetField && record.targetField !== '__ignore__'" style="color:#52c41a" />
                      <StopFilled v-else-if="record.targetField === '__ignore__'" style="color:#d9d9d9" />
                      <ExclamationCircleFilled v-else style="color:#faad14" />
                    </template>
                    <template v-if="column.key === 'sourceField'">
                      <span class="source-field-name">{{ record.sourceField }}</span>
                    </template>
                    <template v-if="column.key === 'sample'">
                      <span class="sample-value" :title="record.sampleValue">{{ record.sampleValue || '-' }}</span>
                    </template>
                    <template v-if="column.key === 'targetField'">
                      <a-select
                        :value="record.targetField"
                        @change="(val: string) => updateMapping(index, val)"
                        size="small"
                        style="width: 220px"
                        allow-clear
                        placeholder="选择目标字段"
                      >
                        <a-select-option value="__ignore__">
                          <span style="color:#999">-- 忽略此列 --</span>
                        </a-select-option>
                        <a-select-opt-group label="可映射字段">
                          <a-select-option
                            v-for="f in fieldDefinitions"
                            :key="f.field"
                            :value="f.field"
                          >
                            <span>{{ f.label }}<span v-if="f.required" style="color:#ff4d4f"> *</span></span>
                          </a-select-option>
                        </a-select-opt-group>
                      </a-select>
                    </template>
                  </template>
                </a-table>
              </div>

              <!-- 必填检查 -->
              <a-alert
                v-if="unmappedRequired.length > 0"
                type="warning"
                show-icon
                style="margin-top: 12px"
              >
                <template #message>
                  以下必填字段尚未映射: {{ unmappedRequired.map(f => f.label).join('、') }}
                </template>
              </a-alert>
            </div>

            <!-- ──── Step 4: 预览校验 ──── -->
            <div v-show="currentStep === 3" class="step-content">
              <div class="step-header">
                <h3>数据预览与校验</h3>
                <a-button
                  size="small"
                  :loading="validating"
                  @click="runValidation"
                  v-if="!validationResult"
                >
                  <template #icon><SafetyCertificateOutlined /></template>
                  执行校验
                </a-button>
              </div>

              <!-- 校验摘要 -->
              <div v-if="validationResult" class="validation-summary">
                <a-row :gutter="16">
                  <a-col :span="6">
                    <a-statistic title="总行数" :value="validationResult.totalRows" />
                  </a-col>
                  <a-col :span="6">
                    <a-statistic title="有效行数" :value="validationResult.validRows" value-style="color: #3f8600" />
                  </a-col>
                  <a-col :span="6">
                    <a-statistic title="错误行数" :value="validationResult.invalidRows" value-style="color: #cf1322" />
                  </a-col>
                  <a-col :span="6">
                    <a-statistic title="校验状态">
                      <template #default>
                        <a-tag :color="validationResult.valid ? 'success' : 'error'">
                          {{ validationResult.valid ? '全部通过' : '存在错误' }}
                        </a-tag>
                      </template>
                    </a-statistic>
                  </a-col>
                </a-row>
              </div>

              <!-- 预览表格 -->
              <div class="preview-table-wrap">
                <a-table
                  :columns="previewColumns"
                  :data-source="previewData"
                  :pagination="previewData.length > 20 ? { pageSize: 20 } : false"
                  size="small"
                  :scroll="{ x: 'max-content' }"
                  bordered
                  row-key="_rowIndex"
                  :row-class-name="previewRowClass"
                >
                  <template #bodyCell="{ column, record }">
                    <template v-if="column.key === '_rowIndex'">
                      {{ record._rowIndex + 1 }}
                    </template>
                    <template v-else-if="column.key === '_status'">
                      <a-tag v-if="getRowErrors(record._rowIndex).length" color="error">
                        {{ getRowErrors(record._rowIndex).length }}个错误
                      </a-tag>
                      <a-tag v-else color="success">通过</a-tag>
                    </template>
                    <template v-else>
                      <span :class="{ 'cell-error': isCellError(record._rowIndex, column.dataIndex as string) }">
                        {{ record[column.dataIndex as string] ?? '' }}
                      </span>
                    </template>
                  </template>
                </a-table>
              </div>

              <!-- 错误详情 -->
              <div v-if="validationErrors.length > 0" class="error-details">
                <a-collapse ghost>
                  <a-collapse-panel key="errors" :header="`错误详情 (${validationErrors.length})`">
                    <a-list
                      :data-source="validationErrors.slice(0, 50)"
                      size="small"
                      bordered
                    >
                      <template #renderItem="{ item }">
                        <a-list-item>
                          <a-list-item-meta>
                            <template #avatar>
                              <a-badge :count="item.rowIndex + 1" :number-style="{ backgroundColor: '#ff4d4f' }" />
                            </template>
                            <template #title>
                              第 {{ item.rowIndex + 1 }} 行 · {{ item.field }}
                              <span v-if="item.value" class="error-value">「{{ item.value }}」</span>
                            </template>
                            <template #description>{{ item.message }}</template>
                          </a-list-item-meta>
                        </a-list-item>
                      </template>
                    </a-list>
                    <a-alert v-if="validationErrors.length > 50" type="info" style="margin-top: 8px">
                      仅显示前50条错误，共 {{ validationErrors.length }} 条
                    </a-alert>
                  </a-collapse-panel>
                </a-collapse>
              </div>

              <!-- 导入策略 -->
              <div class="import-strategy" v-if="validationResult && !validationResult.valid">
                <a-divider>导入策略</a-divider>
                <a-radio-group v-model:value="importStrategy">
                  <a-radio value="skip_errors">
                    <span>跳过错误行</span>
                    <span class="strategy-desc"> — 仅导入有效数据，跳过有错误的行</span>
                  </a-radio>
                  <a-radio value="stop_on_error">
                    <span>遇错停止</span>
                    <span class="strategy-desc"> — 遇到第一个错误时中止导入</span>
                  </a-radio>
                  <a-radio value="ignore_all">
                    <span>忽略所有错误</span>
                    <span class="strategy-desc"> — 强制导入所有数据</span>
                  </a-radio>
                </a-radio-group>
              </div>
            </div>

            <!-- ──── Step 5: 导入结果 ──── -->
            <div v-show="currentStep === 4" class="step-content">
              <a-spin :spinning="importing" tip="正在导入数据...">
                <div v-if="!importing && importResult" class="result-section">
                  <a-result
                    :status="importResult.success && importResult.failureCount === 0 ? 'success' : importResult.success ? 'warning' : 'error'"
                    :title="importResult.message"
                  >
                    <template #extra>
                      <a-row :gutter="24" style="text-align: left; max-width: 500px; margin: 0 auto;">
                        <a-col :span="8">
                          <a-statistic title="总计" :value="importResult.totalCount" />
                        </a-col>
                        <a-col :span="8">
                          <a-statistic title="成功" :value="importResult.successCount" value-style="color: #3f8600" />
                        </a-col>
                        <a-col :span="8">
                          <a-statistic title="失败" :value="importResult.failureCount" value-style="color: #cf1322" />
                        </a-col>
                      </a-row>
                    </template>
                  </a-result>

                  <!-- 错误列表 -->
                  <div v-if="importErrors.length > 0" class="import-errors">
                    <a-collapse ghost>
                      <a-collapse-panel key="import-errors" :header="`失败详情 (${importErrors.length})`">
                        <a-table
                          :columns="importErrorColumns"
                          :data-source="importErrors"
                          :pagination="{ pageSize: 10 }"
                          size="small"
                          bordered
                          row-key="rowIndex"
                        />
                      </a-collapse-panel>
                    </a-collapse>
                  </div>
                </div>

                <div v-else-if="importing" class="import-progress">
                  <a-progress :percent="importProgress" :steps="20" size="small" />
                  <p style="margin-top: 16px; color: #8c8c8c;">正在导入数据，请勿关闭页面...</p>
                </div>
              </a-spin>
            </div>
          </div>

          <!-- 底部导航 -->
          <div class="wizard-footer">
            <div class="wizard-footer-left">
              <a-button v-if="currentStep > 0 && currentStep < 4" @click="prevStep">
                <template #icon><LeftOutlined /></template>
                上一步
              </a-button>
            </div>
            <div class="wizard-footer-right">
              <a-button v-if="currentStep === 4" type="primary" @click="resetWizard">
                <template #icon><PlusOutlined /></template>
                继续导入
              </a-button>
              <a-button
                v-else-if="currentStep === 3"
                type="primary"
                :disabled="!canImport"
                :loading="importing"
                @click="startImport"
              >
                <template #icon><ImportOutlined /></template>
                开始导入
              </a-button>
              <a-button
                v-else-if="currentStep === 2"
                type="primary"
                :disabled="!canProceedMapping"
                @click="nextStep"
              >
                下一步
                <template #icon><RightOutlined /></template>
              </a-button>
              <a-button
                v-else
                type="primary"
                :disabled="!canProceedCurrent"
                @click="nextStep"
              >
                下一步
                <template #icon><RightOutlined /></template>
              </a-button>
            </div>
          </div>
        </div>
      </a-tab-pane>

      <!-- ═══════════ Tab 2: 系统同步 ═══════════ -->
      <a-tab-pane key="sync" tab="系统同步" force-render>
        <div class="sync-header">
          <div class="sync-header-left">
            <span v-if="lastUpdateTime" class="update-time">更新于 {{ lastUpdateTime }}</span>
            <span v-if="autoRefreshCountdown > 0" class="auto-refresh-badge">
              <SyncOutlined /> {{ autoRefreshCountdown }}s
            </span>
            <a-button size="small" :loading="refreshLoading" v-permission="'system:dataimport:query'" @click="handleRefresh">
              <template #icon><ReloadOutlined /></template>
              刷新
            </a-button>
          </div>
          <div class="sync-header-right">
            <a-button type="primary" v-permission="'system:dataimport:create'" @click="showCreateDrawer">
              <template #icon><PlusOutlined /></template>
              新建同步配置
            </a-button>
          </div>
        </div>

        <!-- 配置列表 -->
        <a-skeleton v-if="syncLoading && syncConfigs.length === 0" active :paragraph="{ rows: 6 }" style="padding: 20px;" />
        <div class="config-list" v-else>
          <a-empty v-if="syncConfigs.length === 0 && !syncHasError" description="暂无同步配置">
            <template #extra>
              <a-button type="primary" @click="showCreateDrawer">新建配置</a-button>
            </template>
          </a-empty>

          <a-result v-else-if="syncConfigs.length === 0 && syncHasError" status="error" title="数据加载失败">
            <template #extra>
              <a-button type="primary" @click="loadSyncConfigs">
                <template #icon><ReloadOutlined /></template>
                重新加载
              </a-button>
            </template>
          </a-result>

          <div v-else class="config-cards">
            <a-card
              v-for="item in syncConfigs"
              :key="item.id"
              class="config-card"
              :class="{ disabled: item.status === 0 }"
            >
              <div class="card-header">
                <div class="card-source">
                  <span class="source-icon">
                    <CloudOutlined />
                  </span>
                  <div>
                    <div class="source-name">{{ item.displayName || getSystemName(item.sourceType) }}</div>
                    <div class="source-type">{{ item.sourceType }}</div>
                  </div>
                </div>
                <div class="card-status">
                  <a-switch
                    :checked="item.status === 1"
                    @change="(checked: any) => toggleSyncStatus(item.id, checked as boolean)"
                  />
                </div>
              </div>

              <a-divider style="margin: 12px 0" />

              <div class="card-body">
                <a-descriptions :column="2" size="small">
                  <a-descriptions-item label="登录账号">
                    {{ item.sourceUsername }}
                  </a-descriptions-item>
                  <a-descriptions-item label="同步方式">
                    <a-tag :color="item.syncMode === 'incremental' ? 'blue' : 'orange'">
                      {{ item.syncMode === 'incremental' ? '增量同步' : '全量同步' }}
                    </a-tag>
                  </a-descriptions-item>
                  <a-descriptions-item label="同步频率">
                    {{ item.syncCron || '未设置' }}
                  </a-descriptions-item>
                  <a-descriptions-item label="最后同步">
                    {{ item.lastSyncTime || '从未同步' }}
                  </a-descriptions-item>
                </a-descriptions>
              </div>

              <div class="card-footer">
                <a-space>
                  <a-button size="small" v-permission="'system:dataimport:update'" @click="editSyncConfig(item)">
                    <template #icon><EditOutlined /></template>
                    编辑
                  </a-button>
                  <a-button size="small" v-permission="'system:dataimport:test'" @click="testSyncConnection(item.id)">
                    <template #icon><ApiOutlined /></template>
                    测试连接
                  </a-button>
                  <a-button size="small" type="primary" ghost v-permission="'system:dataimport:sync'" @click="triggerSync(item.id)">
                    <template #icon><SyncOutlined /></template>
                    立即同步
                  </a-button>
                  <a-popconfirm title="确定删除此配置？" @confirm="deleteSyncConfig(item.id)">
                    <a-button size="small" danger v-permission="'system:dataimport:delete'">
                      <template #icon><DeleteOutlined /></template>
                      删除
                    </a-button>
                  </a-popconfirm>
                </a-space>
              </div>
            </a-card>
          </div>
        </div>
      </a-tab-pane>
    </a-tabs>

    <!-- ═══════════ 同步配置抽屉 ═══════════ -->
    <a-drawer
      :open="syncDrawerVisible"
      :title="syncEditingId ? '编辑同步配置' : '新建同步配置'"
      width="600px"
      @close="closeSyncDrawer"
    >
      <a-form
        ref="syncFormRef"
        :model="syncFormData"
        :rules="syncFormRules"
        layout="vertical"
      >
        <a-form-item label="导入系统" name="sourceType" required>
          <a-select
            v-model:value="syncFormData.sourceType"
            placeholder="请选择要绑定的外部系统"
            size="small"
            :options="sourceOptions"
            :loading="sourcesLoading"
            @change="onSourceChange"
          />
        </a-form-item>

        <a-form-item label="显示名称" name="displayName">
          <a-input v-model:value="syncFormData.displayName" placeholder="自定义显示名称（选填）" />
        </a-form-item>

        <a-divider>账号绑定</a-divider>

        <a-form-item label="登录账号" name="sourceUsername" required>
          <a-input v-model:value="syncFormData.sourceUsername" placeholder="外部系统的登录账号" />
        </a-form-item>

        <a-form-item label="登录密码" name="sourcePassword" required>
          <a-input-password v-model:value="syncFormData.sourcePassword" placeholder="外部系统的登录密码" />
        </a-form-item>

        <a-form-item label="API 地址">
          <a-input v-model:value="syncFormData.baseUrl" placeholder="API 基础地址" />
        </a-form-item>

        <a-divider>同步设置</a-divider>

        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="同步方式" name="syncMode">
              <a-radio-group v-model:value="syncFormData.syncMode">
                <a-radio value="incremental">增量同步</a-radio>
                <a-radio value="full">全量同步</a-radio>
              </a-radio-group>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="心跳间隔（秒）" name="heartbeatInterval">
              <a-input-number v-model:value="syncFormData.heartbeatInterval" :min="60" :max="86400" :step="60" style="width: 100%" />
            </a-form-item>
          </a-col>
        </a-row>

        <a-form-item label="同步频率（Cron）" name="syncCron">
          <a-input v-model:value="syncFormData.syncCron" placeholder="*/30 * * * *" />
          <div class="form-help">默认每30分钟一次。格式: 秒 分 时 日 月 周</div>
        </a-form-item>

        <a-form-item label="同步单据类型">
          <a-checkbox-group v-model:value="selectedBillTypes">
            <a-checkbox
              v-for="item in billTypeItems"
              :key="item.itemValue"
              :value="item.itemValue"
            >{{ item.itemText }}</a-checkbox>
          </a-checkbox-group>
        </a-form-item>

        <a-form-item label="备注">
          <a-textarea v-model:value="syncFormData.remark" :rows="2" />
        </a-form-item>
      </a-form>

      <template #footer>
        <a-space>
          <a-button @click="closeSyncDrawer">取消</a-button>
          <a-button type="primary" :loading="syncSaving" @click="saveSyncConfig">保存</a-button>
        </a-space>
      </template>
    </a-drawer>
  </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted, watch, markRaw, type Component } from 'vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import { message } from 'ant-design-vue'
import {
  PlusOutlined, EditOutlined, DeleteOutlined, SyncOutlined, ApiOutlined,
  CloudOutlined, ReloadOutlined, DownloadOutlined, ImportOutlined,
  LeftOutlined, RightOutlined, InboxOutlined, FileExcelOutlined, FileTextOutlined,
  ThunderboltOutlined, ClearOutlined, SafetyCertificateOutlined,
  CheckCircleOutlined, ExclamationCircleOutlined, StopOutlined,
  CheckCircleFilled, StopFilled, ExclamationCircleFilled,
  TeamOutlined, ShoppingOutlined, BarcodeOutlined, UserOutlined,
} from '@ant-design/icons-vue'
import request from '@/utils/request'
import { getToken } from '@/utils/tokenRefresher'
import { dictItemApi, type DictItem } from '@/api/dict'

/**
 * DataImportController 的响应不走标准 ApiResponse 包装（无 code/data/message），
 * 因此使用原生 fetch 绕过 request 拦截器，手动携带 token。
 */
const API_BASE = '/api'

async function importApi<T = any>(path: string, options: RequestInit = {}): Promise<T> {
  const token = getToken()
  const headers = new Headers(options.headers || {})
  if (token) headers.set('Authorization', `Bearer ${token}`)
  const res = await fetch(`${API_BASE}${path}`, { ...options, headers })
  if (!res.ok) throw new Error(`API ${path} → ${res.status}`)
  return res.json() as Promise<T>
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
//  类型定义
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

interface DataType {
  type: string
  name: string
  description: string
}

interface FieldDefinition {
  field: string
  label: string
  type: string
  required: boolean
  maxLength: number
  pattern: string
  description: string
  options: { value: string; label: string }[]
}

interface FieldMappingItem {
  sourceField: string
  targetField: string
  sampleValue: string
}

interface PreviewRow {
  _rowIndex: number
  [key: string]: any
}

interface ValidateError {
  rowIndex: number
  field: string
  value: string
  rule: string
  message: string
}

interface ValidationResult {
  valid: boolean
  totalRows: number
  validRows: number
  invalidRows: number
  errors: ValidateError[]
}

interface ImportError {
  rowIndex: number
  field: string
  value: string
  errorMessage: string
}

interface ImportResult {
  success: boolean
  totalCount: number
  successCount: number
  failureCount: number
  errors: ImportError[]
  message: string
}

interface SyncConfig {
  id: number
  sourceType: string
  displayName: string
  sourceUsername: string
  sourcePassword: string
  baseUrl: string
  syncMode: string
  syncCron: string
  heartbeatInterval: number
  billTypes: string
  status: number
  lastSyncTime: string
  remark: string
}

interface SourceOption {
  systemCode: string
  systemName: string
  description: string
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
//  Tab 切换
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

const activeTab = ref('file')

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
//  Step 1: 选择数据类型
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

const currentStep = ref(0)
const dataTypes = ref<DataType[]>([])
const dataTypesLoading = ref(false)
const selectedType = ref('')
const fieldDefinitions = ref<FieldDefinition[]>([])

const requiredFieldCount = computed(() => fieldDefinitions.value.filter(f => f.required).length)

const dataTypeIconMap: Record<string, Component> = {
  customer: markRaw(TeamOutlined),
  product: markRaw(ShoppingOutlined),
  order: markRaw(BarcodeOutlined),
  user: markRaw(UserOutlined),
}

function getDataTypeIcon(type: string): Component {
  return dataTypeIconMap[type] || markRaw(FileTextOutlined)
}

function getDataTypeName(type: string): string {
  return dataTypes.value.find(d => d.type === type)?.name || type
}

async function loadDataTypes() {
  dataTypesLoading.value = true
  try {
    const data = await importApi<{ dataTypes: DataType[]; count: number }>('/import/v2/datatypes')
    dataTypes.value = data.dataTypes || []
  } catch (e) {
    console.error('加载数据类型失败:', e)
  } finally {
    dataTypesLoading.value = false
  }
}

async function selectDataType(type: string) {
  selectedType.value = type
  try {
    const data = await importApi<{ fields: FieldDefinition[]; fieldCount: number }>(`/import/v2/fields/${type}`)
    fieldDefinitions.value = data.fields || []
  } catch (e) {
    console.error('加载字段定义失败:', e)
    fieldDefinitions.value = []
  }
}

function downloadTemplate() {
  if (!selectedType.value) return
  const url = `/api/import/v2/template/${selectedType.value}`
  const a = document.createElement('a')
  a.href = url
  a.download = ''
  a.click()
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
//  Step 2: 上传文件
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

const uploadedFile = ref<File | null>(null)
const fileList = ref<any[]>([])

const isExcelFile = computed(() => {
  const name = uploadedFile.value?.name || ''
  return name.endsWith('.xlsx') || name.endsWith('.xls')
})

function beforeUpload(file: File) {
  const isValidType = /\.(xlsx|xls|csv)$/i.test(file.name)
  if (!isValidType) {
    message.error('仅支持 .xlsx、.xls、.csv 格式')
    return false
  }
  if (file.size > 10 * 1024 * 1024) {
    message.error('文件大小不能超过 10MB')
    return false
  }
  uploadedFile.value = file
  fileList.value = [{ name: file.name, status: 'done', uid: Date.now().toString() }]
  return false // 阻止自动上传
}

function removeFile() {
  uploadedFile.value = null
  fileList.value = []
  previewData.value = []
  validationResult.value = null
  fieldMapping.value = []
}

function formatFileSize(bytes: number): string {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
//  Step 3: 字段映射
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

const fieldMapping = ref<FieldMappingItem[]>([])

const mappingColumns = [
  { title: '状态', key: 'status', width: 50, align: 'center' as const },
  { title: '文件列名', key: 'sourceField', dataIndex: 'sourceField', width: 160 },
  { title: '示例数据', key: 'sample', dataIndex: 'sampleValue', width: 200, ellipsis: true },
  { title: '映射到系统字段', key: 'targetField', dataIndex: 'targetField', width: 260 },
]

const mappedCount = computed(() => fieldMapping.value.filter(m => m.targetField && m.targetField !== '__ignore__').length)
const unmappedSourceCount = computed(() => fieldMapping.value.filter(m => !m.targetField).length)
const ignoredCount = computed(() => fieldMapping.value.filter(m => m.targetField === '__ignore__').length)

const unmappedRequired = computed(() => {
  const mappedTargets = new Set(fieldMapping.value.filter(m => m.targetField && m.targetField !== '__ignore__').map(m => m.targetField))
  return fieldDefinitions.value.filter(f => f.required && !mappedTargets.has(f.field))
})

const canProceedMapping = computed(() => {
  return unmappedRequired.value.length === 0 && mappedCount.value > 0
})

function updateMapping(index: number, targetField: string) {
  fieldMapping.value[index].targetField = targetField
}

function autoMatchFields() {
  fieldMapping.value.forEach(m => {
    const source = m.sourceField.toLowerCase().trim()
    // 精确匹配 field
    const exactField = fieldDefinitions.value.find(f => f.field.toLowerCase() === source)
    if (exactField) { m.targetField = exactField.field; return }
    // 精确匹配 label
    const exactLabel = fieldDefinitions.value.find(f => f.label.toLowerCase() === source)
    if (exactLabel) { m.targetField = exactLabel.field; return }
    // 模糊匹配：source 包含 label 或 label 包含 source
    const fuzzy = fieldDefinitions.value.find(f =>
      f.field.toLowerCase().includes(source) || source.includes(f.field.toLowerCase()) ||
      f.label.includes(m.sourceField.trim()) || m.sourceField.trim().includes(f.label)
    )
    if (fuzzy) { m.targetField = fuzzy.field; return }
  })
  message.success('智能匹配完成')
}

function clearMapping() {
  fieldMapping.value.forEach(m => { m.targetField = '' })
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
//  Step 4: 预览校验
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

const previewData = ref<PreviewRow[]>([])
const previewHeaders = ref<string[]>([])
const validationResult = ref<ValidationResult | null>(null)
const validating = ref(false)
const importStrategy = ref('skip_errors')

const previewColumns = computed(() => {
  const cols = [
    { title: '#', key: '_rowIndex', width: 60, fixed: 'left' as const },
    ...previewHeaders.value.map(h => ({
      title: h,
      dataIndex: h,
      key: h,
      width: 140,
      ellipsis: true,
    })),
    { title: '状态', key: '_status', width: 100, fixed: 'right' as const },
  ]
  return cols
})

const validationErrors = computed(() => validationResult.value?.errors || [])

function getRowErrors(rowIndex: number): ValidateError[] {
  return validationErrors.value.filter(e => e.rowIndex === rowIndex)
}

function isCellError(rowIndex: number, field: string): boolean {
  return validationErrors.value.some(e => e.rowIndex === rowIndex && e.field === field)
}

function previewRowClass(record: PreviewRow): string {
  if (getRowErrors(record._rowIndex).length > 0) return 'row-error'
  return ''
}

async function loadPreview() {
  if (!uploadedFile.value || !selectedType.value) return
  try {
    const formData = new FormData()
    formData.append('file', uploadedFile.value)
    formData.append('maxRows', '50')
    const data = await importApi<{
      headers: string[]
      rows: Record<string, any>[]
      totalRows: number
      previewRows: number
    }>(`/import/v2/preview/${selectedType.value}`, {
      method: 'POST',
      body: formData,
    })
    previewHeaders.value = data.headers || []
    const rows = data.rows || []
    previewData.value = rows.map((row, i) => ({ ...row, _rowIndex: i }))
    // 自动构建字段映射
    buildFieldMapping(data.headers || [], rows)
  } catch (e) {
    console.error('预览失败:', e)
    message.error('文件解析失败，请检查文件格式')
  }
}

function buildFieldMapping(headers: string[], rows: any[]) {
  fieldMapping.value = headers.map(h => {
    const sampleRow = rows[0] || {}
    const sample = sampleRow[h] !== undefined ? String(sampleRow[h]) : ''
    // 尝试自动匹配
    const sourceLower = h.toLowerCase().trim()
    const matched = fieldDefinitions.value.find(f =>
      f.field.toLowerCase() === sourceLower ||
      f.label.toLowerCase() === sourceLower
    )
    return {
      sourceField: h,
      targetField: matched?.field || '',
      sampleValue: sample,
    }
  })
}

async function runValidation() {
  if (!uploadedFile.value || !selectedType.value) return
  validating.value = true
  try {
    const formData = new FormData()
    formData.append('file', uploadedFile.value)
    const data = await importApi<{
      valid: boolean
      totalRows: number
      validRows: number
      invalidRows: number
      errors: ValidateError[]
    }>(`/import/v2/validate/${selectedType.value}`, {
      method: 'POST',
      body: formData,
    })
    validationResult.value = {
      valid: data.valid ?? true,
      totalRows: data.totalRows || previewData.value.length,
      validRows: data.validRows || 0,
      invalidRows: data.invalidRows || 0,
      errors: data.errors || [],
    }
  } catch (e) {
    console.error('校验失败:', e)
    message.error('数据校验失败')
  } finally {
    validating.value = false
  }
}

const canImport = computed(() => {
  if (!validationResult.value) return previewData.value.length > 0 && mappedCount.value > 0
  return mappedCount.value > 0
})

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
//  Step 5: 导入执行
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

const importing = ref(false)
const importProgress = ref(0)
const importResult = ref<ImportResult | null>(null)

const importErrors = computed(() => importResult.value?.errors || [])
const importErrorColumns = [
  { title: '行号', dataIndex: 'rowIndex', width: 80 },
  { title: '字段', dataIndex: 'field', width: 120 },
  { title: '原始值', dataIndex: 'value', width: 160, ellipsis: true },
  { title: '错误原因', dataIndex: 'errorMessage', ellipsis: true },
]

async function startImport() {
  if (!uploadedFile.value || !selectedType.value) return
  importing.value = true
  importProgress.value = 10
  importResult.value = null

  try {
    const formData = new FormData()
    formData.append('file', uploadedFile.value)

    // 模拟进度
    const progressTimer = setInterval(() => {
      if (importProgress.value < 90) {
        importProgress.value += Math.random() * 15
      }
    }, 500)

    const isCsv = uploadedFile.value.name.endsWith('.csv')
    const endpoint = isCsv
      ? `/import/v2/csv/${selectedType.value}`
      : `/import/v2/excel/${selectedType.value}`

    const data = await importApi<{
      success: boolean
      totalCount: number
      successCount: number
      failureCount: number
      errors: ImportError[]
      message: string
    }>(endpoint, {
      method: 'POST',
      body: formData,
    })

    clearInterval(progressTimer)
    importProgress.value = 100

    importResult.value = {
      success: data.success ?? true,
      totalCount: data.totalCount || 0,
      successCount: data.successCount || 0,
      failureCount: data.failureCount || 0,
      errors: data.errors || [],
      message: data.message || '导入完成',
    }
  } catch (e: any) {
    importResult.value = {
      success: false,
      totalCount: 0,
      successCount: 0,
      failureCount: 0,
      errors: [],
      message: e?.message || '导入失败，请重试',
    }
  } finally {
    importing.value = false
  }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
//  向导导航
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

const canProceedCurrent = computed(() => {
  if (currentStep.value === 0) return !!selectedType.value
  if (currentStep.value === 1) return !!uploadedFile.value
  return true
})

function nextStep() {
  if (currentStep.value === 1 && previewData.value.length === 0) {
    loadPreview()
  }
  if (currentStep.value === 3 && !validationResult.value) {
    runValidation()
  }
  if (currentStep.value < 4) currentStep.value++
}

function prevStep() {
  if (currentStep.value > 0) currentStep.value--
}

function resetWizard() {
  currentStep.value = 0
  selectedType.value = ''
  fieldDefinitions.value = []
  uploadedFile.value = null
  fileList.value = []
  fieldMapping.value = []
  previewData.value = []
  previewHeaders.value = []
  validationResult.value = null
  importResult.value = null
  importProgress.value = 0
  importStrategy.value = 'skip_errors'
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
//  Tab 2: 系统同步
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

const syncLoading = ref(false)
const syncHasError = ref(false)
const syncConfigs = ref<SyncConfig[]>([])
const lastUpdateTime = ref('')
const autoRefreshCountdown = ref(0)
const refreshLoading = ref(false)
let refreshTimer: ReturnType<typeof setInterval> | null = null
let countdownTimer: ReturnType<typeof setInterval> | null = null

const syncDrawerVisible = ref(false)
const syncEditingId = ref<number | null>(null)
const syncSaving = ref(false)
const sourcesLoading = ref(false)
const sourceOptions = ref<SourceOption[]>([])
const billTypeItems = ref<DictItem[]>([])

const syncFormRef = ref()
const selectedBillTypes = ref<string[]>(['601', '604', '504', '801'])

const defaultSyncForm = {
  sourceType: '',
  displayName: '',
  sourceUsername: '',
  sourcePassword: '',
  baseUrl: 'https://www.ql361.com',
  syncMode: 'incremental',
  syncCron: '*/30 * * * *',
  heartbeatInterval: 300,
  billTypes: '["601","604","504","801"]',
  remark: '',
}

const syncFormData = reactive({ ...defaultSyncForm })

const syncFormRules: any = {
  sourceType: [{ required: true, message: '请选择导入系统' }],
  sourceUsername: [{ required: true, message: '请输入登录账号' }],
  sourcePassword: [{ required: true, message: '请输入登录密码' }],
}

function getSystemName(sourceType: string): string {
  const found = sourceOptions.value.find(s => s.systemCode === sourceType)
  return found ? found.systemName : sourceType
}

async function loadBillTypes() {
  try {
    const res = await dictItemApi.getByDictCode('BILL_TYPE')
    billTypeItems.value = res.data || []
  } catch (e) {
    console.error('加载单据类型失败:', e)
  }
}

async function loadSources() {
  sourcesLoading.value = true
  try {
    const res = await request.get('/v1/sync-config/sources')
    sourceOptions.value = (res.data || []).map((s: any) => ({
      label: s.systemName,
      value: s.systemCode,
      ...s,
    }))
  } catch (e) {
    console.error('加载导入系统列表失败:', e)
  } finally {
    sourcesLoading.value = false
  }
}

async function loadSyncConfigs() {
  syncLoading.value = true
  syncHasError.value = false
  try {
    const res = await request.get('/v1/sync-config')
    syncConfigs.value = res.data || []
  } catch (e) {
    syncHasError.value = true
    console.error('加载配置列表失败:', e)
  } finally {
    syncLoading.value = false
    lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
    refreshLoading.value = false
  }
}

const handleRefresh = () => {
  refreshLoading.value = true
  loadSyncConfigs()
}

function showCreateDrawer() {
  syncEditingId.value = null
  Object.assign(syncFormData, defaultSyncForm)
  selectedBillTypes.value = ['601', '604', '504', '801']
  syncDrawerVisible.value = true
}

function editSyncConfig(item: SyncConfig) {
  syncEditingId.value = item.id
  syncFormData.sourceType = item.sourceType
  syncFormData.displayName = item.displayName
  syncFormData.sourceUsername = item.sourceUsername
  syncFormData.sourcePassword = ''
  syncFormData.baseUrl = item.baseUrl
  syncFormData.syncMode = item.syncMode
  syncFormData.syncCron = item.syncCron
  syncFormData.heartbeatInterval = item.heartbeatInterval
  syncFormData.remark = item.remark
  try {
    selectedBillTypes.value = JSON.parse(item.billTypes || '[]')
  } catch {
    selectedBillTypes.value = []
  }
  syncDrawerVisible.value = true
}

function closeSyncDrawer() {
  syncDrawerVisible.value = false
  syncFormRef.value?.resetFields()
}

function onSourceChange(value: string) {
  if (!syncFormData.displayName) {
    syncFormData.displayName = getSystemName(value)
  }
}

async function saveSyncConfig() {
  try {
    await syncFormRef.value.validate()
  } catch { return }

  syncSaving.value = true
  try {
    syncFormData.billTypes = JSON.stringify(selectedBillTypes.value)
    if (syncEditingId.value) {
      await request.put(`/v1/sync-config/${syncEditingId.value}`, syncFormData)
      message.success('更新成功')
    } else {
      await request.post('/v1/sync-config', syncFormData)
      message.success('创建成功')
    }
    closeSyncDrawer()
    await loadSyncConfigs()
  } catch (e: any) {
    message.error(e?.message || '保存失败')
  } finally {
    syncSaving.value = false
  }
}

async function deleteSyncConfig(id: number) {
  try {
    await request.delete(`/v1/sync-config/${id}`)
    message.success('删除成功')
    await loadSyncConfigs()
  } catch (e: any) {
    message.error(e?.message || '删除失败')
  }
}

async function toggleSyncStatus(id: number, enabled: boolean) {
  try {
    await request.post(`/v1/sync-config/${id}/toggle`, null, { params: { enabled } })
    message.success(enabled ? '已启用' : '已禁用')
    await loadSyncConfigs()
  } catch (e: any) {
    message.error(e?.message || '操作失败')
  }
}

async function testSyncConnection(id: number) {
  try {
    const res = await request.post(`/v1/sync-config/${id}/test`)
    if (res?.connected) {
      message.success(`连接成功 (${res?.latency}ms)`)
    } else {
      message.error(res?.message || '连接失败')
    }
  } catch (e: any) {
    message.error(e?.message || '连接测试失败')
  }
}

async function triggerSync(id: number) {
  try {
    const res = await request.post(`/v1/sync-config/${id}/sync`, null, { params: { syncType: 'incremental' } })
    if (res.data?.success) message.success('同步任务已提交')
  } catch (e: any) {
    message.error(e?.message || '触发同步失败')
  }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
//  生命周期
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

function handleError(err: any) { console.warn('[ErrorBoundary]', err) }

onMounted(async () => {
  await loadDataTypes()
  await loadSources()
  await loadBillTypes()
  await loadSyncConfigs()

  refreshTimer = setInterval(() => {
    if (activeTab.value === 'sync') loadSyncConfigs()
    autoRefreshCountdown.value = 30
  }, 30000)
  countdownTimer = setInterval(() => {
    if (autoRefreshCountdown.value > 0) autoRefreshCountdown.value--
  }, 1000)
})

onUnmounted(() => {
  if (refreshTimer) clearInterval(refreshTimer)
  if (countdownTimer) clearInterval(countdownTimer)
})
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}
.page-header-left { flex: 1; }
.page-title { margin: 8px 0 4px; font-size: 20px; font-weight: 600; }
.page-desc { margin: 0; color: #8c8c8c; font-size: 13px; }

/* ── Tab 容器 ── */
.data-import-tabs :deep(.ant-tabs-content) {
  padding-top: 0;
}

/* ── 向导容器 ── */
.wizard-container {
  background: #fff;
  border-radius: 8px;
}

.wizard-steps {
  padding: 20px 40px 16px;
  border-bottom: 1px solid #f0f0f0;
}

.wizard-body {
  padding: 24px 40px;
  min-height: 400px;
}

/* ── 步骤内容 ── */
.step-content {
  animation: fadeIn 0.3s ease;
}
@keyframes fadeIn {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}

.step-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}
.step-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}
.step-header-actions {
  display: flex;
  gap: 8px;
}
.step-desc {
  color: #8c8c8c;
  font-size: 13px;
  margin: 0 0 20px;
}

/* ── Step 1: 类型卡片 ── */
.type-card {
  cursor: pointer;
  transition: all 0.2s;
  border-radius: 8px;
}
.type-card:hover {
  border-color: #1890ff;
}
.type-card.selected {
  border-color: #1890ff;
  background: #e6f7ff;
  box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.15);
}
.type-card-body {
  display: flex;
  align-items: center;
  gap: 12px;
}
.type-icon {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  background: #f0f5ff;
  color: #1890ff;
  flex-shrink: 0;
}
.type-icon.icon-customer { background: #f6ffed; color: #52c41a; }
.type-icon.icon-product { background: #fff7e6; color: #fa8c16; }
.type-icon.icon-order { background: #f9f0ff; color: #722ed1; }
.type-icon.icon-user { background: #e6f7ff; color: #1890ff; }
.type-name { font-weight: 600; font-size: 14px; }
.type-desc { font-size: 12px; color: #8c8c8c; margin-top: 2px; }
.selected-type-info { margin-top: 20px; }

/* ── Step 2: 上传区域 ── */
.upload-dragger {
  margin-bottom: 16px;
}
.upload-dragger :deep(.ant-upload-drag) {
  border-radius: 8px;
  padding: 32px;
}
.upload-dragger :deep(.ant-upload-drag-icon) {
  margin-bottom: 12px;
}
.upload-dragger :deep(.ant-upload-drag-icon .anticon) {
  font-size: 48px;
  color: #1890ff;
}
.file-info-card {
  margin-top: 12px;
}
.file-meta {
  display: flex;
  align-items: center;
  gap: 12px;
}
.file-icon { font-size: 24px; }
.file-icon.excel { color: #52c41a; }
.file-icon.csv { color: #fa8c16; }
.file-name { font-weight: 500; font-size: 14px; }
.file-size { font-size: 12px; color: #8c8c8c; }
.step-actions-left {
  margin-top: 12px;
}

/* ── Step 3: 映射表 ── */
.mapping-stats {
  margin-bottom: 12px;
}
.mapping-table-wrap {
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  overflow: hidden;
}
.source-field-name {
  font-family: 'SFMono-Regular', Consolas, monospace;
  font-size: 13px;
  color: #1890ff;
}
.sample-value {
  color: #8c8c8c;
  font-size: 12px;
}

/* ── Step 4: 预览 ── */
.validation-summary {
  padding: 16px;
  background: #fafafa;
  border-radius: 8px;
  margin-bottom: 16px;
}
.preview-table-wrap {
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  overflow: hidden;
}
.preview-table-wrap :deep(.row-error) {
  background: #fff2f0;
}
.cell-error {
  color: #ff4d4f;
  text-decoration: underline wavy #ff4d4f;
}
.error-details {
  margin-top: 16px;
}
.error-value {
  color: #8c8c8c;
  font-size: 12px;
}
.import-strategy {
  margin-top: 16px;
}
.strategy-desc {
  color: #8c8c8c;
  font-size: 12px;
}

/* ── Step 5: 结果 ── */
.result-section {
  text-align: center;
}
.import-progress {
  padding: 40px 0;
  text-align: center;
}
.import-errors {
  margin-top: 16px;
  text-align: left;
}

/* ── 底部导航 ── */
.wizard-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 40px;
  border-top: 1px solid #f0f0f0;
}
.wizard-footer-left, .wizard-footer-right {
  display: flex;
  gap: 8px;
}

/* ═══════════ 系统同步 Tab ═══════════ */
.sync-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.sync-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.update-time { font-size: 12px; color: #999; }
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
.config-cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(460px, 1fr));
  gap: 16px;
}
.config-card {
  border-radius: 8px;
  transition: all 0.3s;
}
.config-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}
.config-card.disabled { opacity: 0.6; }
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.card-source {
  display: flex;
  align-items: center;
  gap: 12px;
}
.source-icon {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  background: #e6f7ff;
  color: #1890ff;
}
.source-name { font-weight: 600; font-size: 15px; }
.source-type { color: #8c8c8c; font-size: 12px; }
.card-footer {
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}
.form-help {
  font-size: 12px;
  color: #8c8c8c;
  margin-top: 4px;
}
</style>
