<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        菜单管理（系统 → 系统管理 → 菜单管理，菜单 6130701）
        开发文档：docs/Yh-Spec/手动整理对标开发文档/系统模块/菜单管理开发文档.md
        金标准外壳：ErrorBoundary > PageContainer(full-height) > CategoryListLayout
      -->
      <CategoryListLayout
        :tabs="[]"
        :show-category-panel="false"
        :show-table-footer="true"
      >
        <!-- ═══ 工具栏左侧：新增菜单 / 展开收起 / 刷新缓存 ═══ -->
        <template #toolbar-left>
          <a-space :size="8">
            <a-button
              v-if="isButtonEnabled('add')"
              v-permission="'system:menu:create'"
              type="primary"
              size="small"
              :loading="submitLoading"
              @click="handleAdd"
            >
              <template #icon>
                <PlusOutlined />
              </template>
              新增菜单
            </a-button>
            <a-button
              v-if="isButtonEnabled('expand')"
              size="small"
              @click="toggleExpandAll"
            >
              <template #icon>
                <NodeExpandOutlined v-if="!isExpandAll" />
                <NodeCollapseOutlined v-else />
              </template>
              {{ isExpandAll ? '折叠全部' : '展开全部' }}
            </a-button>
            <a-button
              v-if="isButtonEnabled('refreshCache')"
              size="small"
              @click="handleRefreshCache"
            >
              <template #icon>
                <SyncOutlined />
              </template>
              刷新缓存
            </a-button>
          </a-space>
        </template>

        <!-- ═══ 工具栏右侧：刷新 + 页面配置 ═══ -->
        <template #toolbar-right>
          <a-space :size="8">
            <a-button
              v-if="isButtonEnabled('refresh')"
              size="small"
              title="刷新 (F5)"
              :loading="refreshLoading || loading"
              @click="debounceClick('refresh', loadMenuTree)()"
            >
              <template #icon>
                <ReloadOutlined />
              </template>
              刷新
            </a-button>
            <a-button
              size="small"
              title="页面配置"
              @click="showPageConfig = true"
            >
              <template #icon>
                <SettingOutlined />
              </template>
            </a-button>
          </a-space>
        </template>

        <!-- ═══ 查询区（横向网格）：菜单名称 / 菜单编码 / 状态 ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div class="search-row">
              <template v-if="isQueryFieldVisible('menuName')">
                <span class="search-label">菜单名称</span>
                <a-input
                  v-model:value="queryForm.menuName"
                  placeholder="请输入菜单名称"
                  size="small"
                  style="width: 180px"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </template>
              <template v-if="isQueryFieldVisible('menuCode')">
                <span class="search-label">菜单编码</span>
                <a-input
                  v-model:value="queryForm.menuCode"
                  placeholder="请输入权限标识"
                  size="small"
                  style="width: 180px"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </template>
              <template v-if="isQueryFieldVisible('status')">
                <span class="search-label">状态</span>
                <a-select
                  v-model:value="queryForm.status"
                  placeholder="全部状态"
                  size="small"
                  style="width: 120px"
                  allow-clear
                  :options="STATUS_OPTIONS"
                  @change="handleSearch"
                />
              </template>
              <a-button
                type="primary"
                size="small"
                @click="handleSearch"
              >
                查询
              </a-button>
              <a-button
                size="small"
                @click="handleReset"
              >
                重置
              </a-button>
            </div>
          </div>
        </template>

        <!-- ═══ 数据表（树形；列配置齿轮在表头 rowNo 列） ═══ -->
        <template #table>
          <!--
            数据口径说明（本页最容易被误判为「数据丢了」的一点）：
            后端 SysMenuServiceImpl 强制 eq(tenant_id, 0L) 取菜单，而
            「菜单管理」自身的记录（id 6130701）是 sys_menu 中唯一 tenant_id = 1 的行。
          -->
          <a-alert
            class="scope-alert"
            type="info"
            show-icon
            message="数据口径说明"
            description="本页数据口径为 tenant_id = 0；菜单管理自身的记录（id 6130701）是 sys_menu 中唯一 tenant_id = 1 的行，故不在此列表中。通过本页新增的菜单会继承当前会话租户（tenant_id = 1），同样不会出现在下面的表格里。"
          />

          <div class="table-area">
            <div class="menu-tree-table">
              <!--
                ⚠️ 为什么没有换 BillDetailTable：
                BillDetailTable 的行渲染是 displayRows 上的扁平 v-for，其 col.children
                只用于「分组表头列」，不支持记录级树形 children。
                故本页保留自研扁平化树形 a-table，外层套 ColumnConfigTable
                —— 该复用组件自带「表头齿轮 + ColumnConfigPanel + storage-key」，
                补上了本页此前完全缺失的列配置能力。
              -->
              <ColumnConfigTable
                :data-source="flatMenuRows"
                :column-defs="tableColumns"
                :loading="loading"
                :pagination="false"
                :bordered="true"
                :min-rows="0"
                row-key="id"
                size="small"
                :storage-key="TABLE_STORAGE_KEY"
              >
                <template #bodyCell="{ column, record, index }">
                  <!-- 行号（表头该列承载列配置齿轮） -->
                  <template v-if="column.key === 'rowNo'">
                    <span class="row-no">{{ index + 1 }}</span>
                  </template>
                  <template v-else-if="column.key === 'menuName'">
                    <span
                      class="tree-indent"
                      :style="{ paddingLeft: (record._level || 0) * 20 + 'px' }"
                    >
                      <span
                        v-if="record._hasChildren"
                        class="tree-expand-icon"
                        @click.stop="toggleRowExpand(record.id)"
                      >
                        <CaretDownOutlined v-if="expandedRowKeys.includes(record.id)" />
                        <CaretRightOutlined v-else />
                      </span>
                      <span
                        v-else
                        class="tree-expand-placeholder"
                      />
                      <component
                        :is="iconComponent(record.icon)"
                        v-if="record.icon"
                        class="menu-icon"
                      />
                      <span
                        :class="{ 'tree-node-clickable': record._hasChildren }"
                        @click="record._hasChildren && toggleRowExpand(record.id)"
                      >{{ record.menuName }}</span>
                    </span>
                  </template>
                  <template v-else-if="column.key === 'menuType'">
                    <a-tag v-if="record.menuType === 0">
                      目录
                    </a-tag>
                    <a-tag
                      v-else-if="record.menuType === 1"
                      color="green"
                    >
                      菜单
                    </a-tag>
                    <a-tag
                      v-else-if="record.menuType === 2"
                      color="orange"
                    >
                      按钮
                    </a-tag>
                  </template>
                  <template v-else-if="column.key === 'status'">
                    <!-- 状态开关走 PUT /api/menu/{id}/status → 后端要求 system:menu:update-status -->
                    <a-switch
                      v-permission="'system:menu:update-status'"
                      :checked="record.status === 1"
                      @change="(checked: boolean) => handleStatusChange(record, checked ? 1 : 0)"
                    />
                  </template>
                  <template v-else-if="column.key === 'visible'">
                    <a-tag
                      v-if="record.visible === 1"
                      color="green"
                    >
                      显示
                    </a-tag>
                    <a-tag v-else>
                      隐藏
                    </a-tag>
                  </template>
                  <template v-else-if="column.key === 'bizFlowTag'">
                    <a-tag
                      v-if="record.bizFlowTag"
                      :color="getBizFlowTagColor(record.bizFlowTag)"
                    >
                      {{ getBizFlowTagLabel(record.bizFlowTag) }}
                    </a-tag>
                    <span
                      v-else
                      class="text-muted"
                    >—</span>
                  </template>
                  <template v-else-if="column.key === 'displayMode'">
                    <a-tag
                      v-if="record.displayMode === 1"
                      color="purple"
                    >
                      双入口
                    </a-tag>
                    <span
                      v-else
                      class="text-muted"
                    >默认</span>
                  </template>
                  <template v-else-if="column.key === 'menuLevel'">
                    <a-tag
                      v-if="record.menuLevel === 1"
                      color="red"
                    >
                      系统
                    </a-tag>
                    <a-tag
                      v-else-if="record.menuLevel === 0"
                      color="green"
                    >
                      租户
                    </a-tag>
                    <span
                      v-else
                      class="text-muted"
                    >—</span>
                  </template>
                  <template v-else-if="column.key === 'action'">
                    <a-button
                      v-permission="'system:menu:create'"
                      type="link"
                      size="small"
                      @click="handleAddChild(record)"
                    >
                      <template #icon>
                        <PlusOutlined />
                      </template>新增
                    </a-button>
                    <a-button
                      v-permission="'system:menu:update'"
                      type="link"
                      size="small"
                      @click="handleEdit(record)"
                    >
                      <template #icon>
                        <EditOutlined />
                      </template>编辑
                    </a-button>
                    <!--
                      例外说明：「角色」按钮实际调用 POST /api/role/{id}/menus（角色域），
                      故使用该端点真实要求的 system:role:assign-menu，不强行归入 system:menu:*。
                      ⚠️ 遗留：其前置步骤 roleApi.getMenus() → GET /api/role/{id}/menus 后端不存在（404），
                      该功能在补齐后端端点前不可用（本轮只改前端，保持如实报错）。
                    -->
                    <a-button
                      v-permission="'system:role:assign-menu'"
                      type="link"
                      size="small"
                      @click="handleAssignRole(record)"
                    >
                      <template #icon>
                        <UserOutlined />
                      </template>角色
                    </a-button>
                    <a-button
                      v-permission="'system:menu:delete'"
                      type="link"
                      size="small"
                      danger
                      @click="handleDelete(record)"
                    >
                      <template #icon>
                        <DeleteOutlined />
                      </template>删除
                    </a-button>
                  </template>
                </template>

                <template #emptyText>
                  <a-empty
                    v-if="!hasError"
                    description="暂无数据"
                  />
                  <a-result
                    v-else
                    status="error"
                    title="数据加载失败"
                  >
                    <template #extra>
                      <a-button
                        type="primary"
                        @click="debounceClick('refresh', loadMenuTree)()"
                      >
                        <template #icon>
                          <ReloadOutlined />
                        </template>
                        重新加载
                      </a-button>
                    </template>
                  </a-result>
                </template>
              </ColumnConfigTable>
            </div>
          </div>
        </template>

        <!--
          ═══ 底部插槽 ═══
          本页是「树形全量展示」：GET /api/menu/tree 一次性返回整棵树（后端无分页参数），
          前端按展开状态渲染可见行，因此**不接分页器**（标准分页会切断父子行、
          出现「第 2 页全是孤立节点」的语义问题）。此处改放口径汇总条。
          原页面顶部 4 张统计卡（菜单总数 / 启用 / 禁用 / 按钮数量）口径不变，一并收纳在此。
        -->
        <template #table-footer>
          <div class="table-footer-bar">
            <div class="footer-left">
              <span class="footer-item">菜单总数 <b>{{ menuCount }}</b></span>
              <span class="footer-item">启用 <b>{{ enabledCount }}</b></span>
              <span class="footer-item">禁用 <b>{{ disabledCount }}</b></span>
              <span class="footer-item">按钮数量 <b>{{ buttonCount }}</b></span>
              <span class="footer-item">当前显示 <b>{{ flatMenuRows.length }}</b> 行</span>
            </div>
            <div class="footer-right">
              <span
                v-if="lastUpdateTime"
                class="footer-time"
              >更新于 {{ lastUpdateTime }}</span>
              <span class="footer-hint"><kbd>F5</kbd> 刷新 · <kbd>Ctrl</kbd>+<kbd>N</kbd> 新增</span>
            </div>
          </div>
        </template>
      </CategoryListLayout>

      <!-- ═══ 页面配置（查询条件显隐 + 功能按钮启用） ═══ -->
      <PageConfigPanel
        :open="showPageConfig"
        :query-fields-config="queryFields"
        :function-buttons-config="functionButtons"
        :default-query-fields-config="DEFAULT_QUERY_FIELDS"
        :default-function-buttons-config="DEFAULT_FUNCTION_BUTTONS"
        :storage-key="PAGE_CONFIG_STORAGE_KEY"
        :hide-print-config="true"
        @update:open="showPageConfig = $event"
        @change="handlePageConfigChange"
      />

      <!-- 菜单编辑弹窗 -->
      <FullScreenDetail
        :visible="dialogVisible"
        :title="dialogTitle"
        :dirty="formDirty"
        :save-loading="submitLoading"
        :show-save-and-new="!formData.id"
        @save="handleSubmit"
        @close="handleFormClose"
        @save-and-new="handleFormSaveAndNew"
      >
        <a-form
          ref="formRef"
          :model="formData"
          :rules="formRules"
          :label-col="{ style: { width: '100px' } }"
        >
          <a-form-item label="上级菜单">
            <a-tree-select
              v-model:value="formData.parentId"
              :tree-data="parentMenuTree"
              :field-names="{ label: 'menuName', value: 'id', children: 'children' }"
              placeholder="请选择上级菜单（留空则为顶级）"
              allow-clear
              tree-check-strictly
              tree-default-expand-all
              style="width: 100%"
            />
          </a-form-item>

          <a-form-item
            label="菜单类型"
            name="menuType"
          >
            <a-radio-group v-model:value="formData.menuType">
              <a-radio
                v-for="opt in MENU_TYPE_OPTIONS"
                :key="opt.value"
                :value="opt.value"
              >
                {{ opt.label }}
              </a-radio>
            </a-radio-group>
          </a-form-item>

          <a-form-item
            label="菜单名称"
            name="menuName"
          >
            <a-input
              v-model:value="formData.menuName"
              placeholder="请输入菜单名称"
            />
          </a-form-item>

          <a-form-item
            label="权限标识"
            name="menuCode"
          >
            <a-input
              v-model:value="formData.menuCode"
              placeholder="请输入权限标识，如：system:user:list"
            />
          </a-form-item>

          <a-form-item
            v-if="formData.menuType !== 2"
            label="路由路径"
            name="path"
          >
            <a-input
              v-model:value="formData.path"
              :placeholder="formData.menuType === 0 ? '顶层目录以 / 开头，如：/system；子目录不以 / 开头，如：user' : '请输入路由路径，如：orders'"
            />
          </a-form-item>

          <a-form-item
            v-if="formData.menuType === 0"
            label="组件路径"
          >
            <a-input
              v-model:value="formData.component"
              placeholder="可选，顶层目录填写如 layouts/BasicLayout.vue；子目录留空由系统自动推断"
            />
          </a-form-item>

          <a-form-item
            v-if="formData.menuType === 1"
            label="组件路径"
            name="component"
          >
            <a-input
              v-model:value="formData.component"
              placeholder="views/ 下的相对路径，如：views/erp/sale/index"
            />
          </a-form-item>

          <a-form-item
            v-if="formData.menuType !== 2"
            label="菜单图标"
          >
            <a-auto-complete
              v-model:value="formData.icon"
              :options="iconOptions"
              placeholder="输入或选择图标名，如：UserOutlined"
              :filter-option="iconFilterOption"
            />
          </a-form-item>

          <a-form-item
            label="排序"
            name="sortOrder"
          >
            <a-input-number
              v-model:value="formData.sortOrder"
              :min="0"
              :max="9999"
            />
          </a-form-item>

          <a-form-item
            v-if="formData.menuType !== 2"
            label="是否显示"
          >
            <a-radio-group v-model:value="formData.visible">
              <a-radio :value="1">
                显示
              </a-radio>
              <a-radio :value="0">
                隐藏
              </a-radio>
            </a-radio-group>
          </a-form-item>

          <a-form-item label="菜单状态">
            <a-radio-group v-model:value="formData.status">
              <a-radio :value="1">
                启用
              </a-radio>
              <a-radio :value="0">
                禁用
              </a-radio>
            </a-radio-group>
          </a-form-item>

          <a-form-item label="备注">
            <a-textarea
              v-model:value="formData.remark"
              :rows="3"
              placeholder="请输入备注"
            />
          </a-form-item>

          <a-form-item label="业务流向">
            <a-select
              v-model:value="formData.bizFlowTag"
              placeholder="请选择业务流向（可选）"
              allow-clear
            >
              <a-select-option
                v-for="opt in BIZ_FLOW_TAG_OPTIONS"
                :key="opt.value"
                :value="opt.value"
              >
                {{ opt.label }}
              </a-select-option>
            </a-select>
          </a-form-item>

          <a-form-item label="展示分组">
            <a-radio-group v-model:value="formData.displayGroup">
              <a-radio :value="0">
                正常路由目录
              </a-radio>
              <a-radio :value="1">
                纯展示分组（Sidebar分组标题，不生成路由嵌套）
              </a-radio>
            </a-radio-group>
          </a-form-item>

          <a-form-item label="链接图标">
            <a-input
              v-model:value="formData.linkIcon"
              placeholder="外链/快捷方式图标名，如 LinkOutlined"
            />
          </a-form-item>

          <a-form-item label="菜单层级">
            <a-radio-group v-model:value="formData.menuLevel">
              <a-radio :value="0">
                租户级
              </a-radio>
              <a-radio :value="1">
                系统级
              </a-radio>
            </a-radio-group>
          </a-form-item>

          <a-form-item label="展示模式">
            <a-radio-group v-model:value="formData.displayMode">
              <a-radio :value="0">
                默认（单入口）
              </a-radio>
              <a-radio :value="1">
                双入口（含标签按钮）
              </a-radio>
            </a-radio-group>
          </a-form-item>

          <a-form-item
            v-if="formData.displayMode === 1"
            label="列表路径"
          >
            <a-input
              v-model:value="formData.listPath"
              placeholder="双入口时，标签按钮跳转的列表页路由路径"
            />
          </a-form-item>

          <a-form-item label="标签文案">
            <a-select
              v-model:value="formData.tagLabel"
              placeholder="请选择标签文案（可选）"
              allow-clear
            >
              <a-select-option value="历史">
                历史
              </a-select-option>
              <a-select-option value="列表">
                列表
              </a-select-option>
              <a-select-option value="添加">
                添加
              </a-select-option>
            </a-select>
          </a-form-item>
        </a-form>
      </FullScreenDetail>

      <!-- 角色分配弹窗 -->
      <FullScreenDetail
        :visible="roleDialogVisible"
        title="分配角色"
        :save-loading="roleSubmitLoading"
        @save="handleRoleSubmit"
        @close="roleDialogVisible = false"
      >
        <a-form :label-col="{ style: { width: '80px' } }">
          <a-form-item label="菜单名称">
            <span>{{ currentMenu?.menuName }}</span>
          </a-form-item>
          <a-form-item label="选择角色">
            <a-select
              v-model:value="selectedRoles"
              mode="multiple"
              placeholder="请选择角色"
              size="small"
              style="width: 100%"
            >
              <a-select-option
                v-for="role in roleList"
                :key="role.id"
                :value="role.id"
              >
                {{ role.roleName }}
              </a-select-option>
            </a-select>
          </a-form-item>
        </a-form>
      </FullScreenDetail>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
/**
 * 菜单管理（系统 → 系统管理 → 菜单管理，菜单 6130701）
 *
 * ⚠️ 本轮统一按钮权限码（修复「按钮码与后端不同域」缺陷）
 *   本页原先用 `system:permission:*`（**权限管理域**的码），而后端 `SysMenuController`
 *   方法级注解要求的是 `system:menu:create/update/delete/list/detail/update-status`。
 *   两个集合交集为空 → 按钮能显示、接口照样 403（开发文档 §5.5「看得见、点不动」）。
 *   现按「每个按钮实际调用的端点」逐一改为后端真实要求的码：
 *     POST   /api/menu                → system:menu:create         （工具栏新增菜单 / 行内新增）
 *     PUT    /api/menu/{id}           → system:menu:update         （行内编辑）
 *     DELETE /api/menu/{id}           → system:menu:delete         （行内删除）
 *     PUT    /api/menu/{id}/status    → system:menu:update-status  （状态开关）
 *     GET    /api/menu/tree           → system:menu:list           （树/列表查询）
 *   唯一例外：行内「角色」按钮实际调用 `POST /api/role/{id}/menus`，属**角色域**，
 *   故用该端点真实要求的 `system:role:assign-menu`，不强行改成 `system:menu:*`。
 *   🔴 遗留（需后端补种子，本轮只改前端）：`system:menu:*` 六个码在 `sys_permission`
 *   表中 0 行 → 除超管（前端指令对 `*` 放行）外仍会 403（开发文档 §10.1-⑯）。
 *
 * ⚠️ 表格选型说明（为什么没换 BillDetailTable）
 *   `BillDetailTable` 的行渲染是 `v-for="record in displayRows"` 的**扁平**渲染，
 *   其 `col.children` 只用于**分组表头列**（flattenColumns），**不支持记录级树形 children**
 *   → 本页保留自研扁平化树形 `a-table`，外层套 `ColumnConfigTable`
 *   （复用组件已自带表头齿轮 + ColumnConfigPanel + storage-key，补上此前缺失的列配置能力）。
 */

import { ref, reactive, computed, nextTick, onMounted, onUnmounted } from 'vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import { onBeforeRouteLeave } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
// Rule type not available, using any
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  UserOutlined,
  ReloadOutlined,
  SyncOutlined,
  SettingOutlined,
  NodeExpandOutlined,
  NodeCollapseOutlined,
  CaretDownOutlined,
  CaretRightOutlined
} from '@ant-design/icons-vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import ColumnConfigTable from '@/components/ColumnConfigTable/index.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import FullScreenDetail from '@/components/FullScreenDetail/FullScreenDetail.vue'
import menuApi, { type MenuInfo, type MenuSaveRequest, type MenuUpdateRequest } from '@/api/menu'
import roleApi from '@/api/role'
import { useSubmitLock } from '@/composables'
import * as Icons from '@ant-design/icons-vue'

// ═══ 持久化键 ═══
/** 页面配置（查询条件显隐 / 功能按钮启用） */
const PAGE_CONFIG_STORAGE_KEY = 'system-menu-page-config'
/** 数据表列配置（表头齿轮；个人配置 + 全局配置） */
const TABLE_STORAGE_KEY = 'system-menu-table-columns'

// ═══ 枚举常量 ═══
/** 状态值域（开发文档 §5.1：1=启用 / 0=禁用） */
const STATUS_OPTIONS = [
  { label: '启用', value: 1 },
  { label: '禁用', value: 0 },
]
/**
 * 菜单类型：固定的三值枚举（与本页「类型」列 tag 同源）。
 * 不再依赖 `MENU_TYPE` 字典 —— 该字典在库中 0 行（开发文档 §5.2），
 * 原先会让「菜单类型」单选组恒空、用户无法切换类型。
 */
const MENU_TYPE_OPTIONS = [
  { label: '目录', value: 0 },
  { label: '菜单', value: 1 },
  { label: '按钮', value: 2 },
]

// ═══ 页面配置（查询条件 / 功能按钮） ═══
interface QueryFieldSetting { key: string; label: string; visible: boolean }
interface FunctionButtonSetting { key: string; label: string; enabled: boolean }
interface PageConfig { queryFields: QueryFieldSetting[]; functionButtons: FunctionButtonSetting[] }

const DEFAULT_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'menuName', label: '菜单名称', visible: true },
  { key: 'menuCode', label: '菜单编码', visible: true },
  { key: 'status', label: '状态', visible: true },
]
const DEFAULT_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'add', label: '新增菜单', enabled: true },
  { key: 'expand', label: '展开/折叠全部', enabled: true },
  { key: 'refreshCache', label: '刷新缓存', enabled: true },
  { key: 'refresh', label: '刷新', enabled: true },
]
const queryFields = ref<QueryFieldSetting[]>(DEFAULT_QUERY_FIELDS.map(f => ({ ...f })))
const functionButtons = ref<FunctionButtonSetting[]>(DEFAULT_FUNCTION_BUTTONS.map(f => ({ ...f })))
const showPageConfig = ref(false)

function isQueryFieldVisible(key: string) {
  return queryFields.value.find(f => f.key === key)?.visible !== false
}
function isButtonEnabled(key: string) {
  return functionButtons.value.find(f => f.key === key)?.enabled !== false
}
function handlePageConfigChange(config: PageConfig) {
  if (config?.queryFields?.length) queryFields.value = config.queryFields
  if (config?.functionButtons?.length) functionButtons.value = config.functionButtons
}

// ═══ 页面状态 ═══
const lastUpdateTime = ref('')
const refreshLoading = ref(false)
const hasError = ref(false)

// ── 防抖工具 ────────────────────────────────────────────
const clickLocks = new Map<string, boolean>()
function debounceClick(key: string, fn: (...args: any[]) => any) {
  return (...args: any[]) => {
    if (clickLocks.get(key)) return
    clickLocks.set(key, true)
    try { fn(...args) } finally { setTimeout(() => clickLocks.delete(key), 300) }
  }
}

// ═══ 查询表单 ═══
/**
 * 三个查询条件**全部是前端本地过滤**：
 * `GET /api/menu/tree` 只声明 `@RequestParam Long tenantId`（SysMenuController.java:83），
 * 其余参数会被 Spring 静默忽略（不会 400，也不生效）。
 * 故不再把 menuName/menuCode/status 发给后端（避免「装饰性查询条件」），改为拉全量树后本地过滤。
 */
const queryForm = reactive({
  menuName: '' as string,
  menuCode: '' as string,
  status: undefined as number | undefined,
})

// ═══ 菜单树数据 ═══
const menuTree = ref<MenuInfo[]>([])
const loading = ref(false)
const isExpandAll = ref(false)

// ── 树展开状态（扁平化方案）─────────────────────────────
const expandedRowKeys = ref<number[]>([])

function toggleRowExpand(id: number) {
  const idx = expandedRowKeys.value.indexOf(id)
  if (idx === -1) {
    expandedRowKeys.value = [...expandedRowKeys.value, id]
  } else {
    expandedRowKeys.value = expandedRowKeys.value.filter(k => k !== id)
  }
}

function collectAllParentIds(nodes: MenuInfo[]): number[] {
  const ids: number[] = []
  const walk = (list: MenuInfo[]) => {
    for (const n of list) {
      if (n.children?.length) { ids.push(n.id); walk(n.children) }
    }
  }
  walk(nodes)
  return ids
}

/** 默认只展开第一层（避免一次性渲染 380+ 行） */
function applyDefaultExpand() {
  const firstLevelIds: number[] = []
  for (const node of menuTree.value) {
    if (node.children?.length) firstLevelIds.push(node.id)
  }
  expandedRowKeys.value = firstLevelIds
}

// ═══ 本地过滤 ═══
const hasFilter = computed(() =>
  !!(queryForm.menuName.trim() || queryForm.menuCode.trim() || queryForm.status !== undefined)
)

/** 过滤后的树：自身命中 → 保留完整子树；仅后代命中 → 只保留命中分支 */
const filteredMenuTree = computed<MenuInfo[]>(() => {
  if (!hasFilter.value) return menuTree.value
  const name = queryForm.menuName.trim().toLowerCase()
  const code = queryForm.menuCode.trim().toLowerCase()
  const status = queryForm.status
  const hit = (n: MenuInfo) =>
    (!name || (n.menuName || '').toLowerCase().includes(name)) &&
    (!code || (n.menuCode || '').toLowerCase().includes(code)) &&
    (status === undefined || n.status === status)
  const walk = (nodes: MenuInfo[]): MenuInfo[] => {
    const out: MenuInfo[] = []
    for (const node of nodes) {
      const hitSelf = hit(node)
      const kids = node.children?.length ? walk(node.children) : []
      if (hitSelf || kids.length) {
        out.push({
          ...node,
          children: hitSelf && node.children?.length ? node.children : (kids.length ? kids : undefined),
        })
      }
    }
    return out
  }
  return walk(menuTree.value)
})

// 扁平化：根据展开状态生成当前可见行
interface FlatRow extends MenuInfo {
  _level: number
  _hasChildren: boolean
}

const flatMenuRows = computed<FlatRow[]>(() => {
  const result: FlatRow[] = []
  const expandedSet = new Set(expandedRowKeys.value)
  const walk = (nodes: MenuInfo[], level: number) => {
    for (const node of nodes) {
      const hasChildren = !!(node.children?.length)
      // 剔除 children，避免 Ant Design 检测到 children 自动生成展开列
      const { children: _children, ...rest } = node
      result.push({ ...rest, _level: level, _hasChildren: hasChildren } as FlatRow)
      if (hasChildren && expandedSet.has(node.id)) {
        walk(node.children!, level + 1)
      }
    }
  }
  walk(filteredMenuTree.value, 0)
  return result
})

// 上级菜单树（过滤掉按钮，添加虚拟根节点）
const parentMenuTree = computed(() => {
  const filterButtons = (nodes: MenuInfo[]): MenuInfo[] => {
    return nodes
      .filter(n => n.menuType !== 2)
      .map(n => ({
        ...n,
        children: n.children ? filterButtons(n.children) : undefined
      }))
  }
  return [
    { id: 0, menuName: '主类目（顶级）', children: filterButtons(menuTree.value) } as unknown as MenuInfo
  ]
})

// 图标选项列表（常用图标）
const commonIconNames = Object.keys(Icons).filter(name => name.endsWith('Outlined')).slice(0, 60)
const iconOptions = computed(() => commonIconNames.map(name => ({ value: name })))
const iconFilterOption = (inputValue: string, option: { value: string }) => {
  return option.value.toLowerCase().includes(inputValue.toLowerCase())
}

// ── 统计口径（客户端计算，与开发文档 §3.3 一致）──────────
const flattenMenuTree = (tree: MenuInfo[]): MenuInfo[] => {
  const result: MenuInfo[] = []
  const traverse = (nodes: MenuInfo[]) => {
    for (const node of nodes) {
      result.push(node)
      if (node.children?.length) traverse(node.children)
    }
  }
  traverse(tree)
  return result
}
/** ⚠️ 「菜单总数」不含按钮（menuType !== 2），按钮单独计数，两者相加 = 树节点总数 */
const menuCount = computed(() => flattenMenuTree(menuTree.value).filter(m => m.menuType !== 2).length)
const buttonCount = computed(() => flattenMenuTree(menuTree.value).filter(m => m.menuType === 2).length)
const enabledCount = computed(() => flattenMenuTree(menuTree.value).filter(m => m.status === 1).length)
const disabledCount = computed(() => flattenMenuTree(menuTree.value).filter(m => m.status === 0).length)

// ═══ 弹窗控制 ═══
const dialogVisible = ref(false)
const dialogTitle = ref('新增菜单')
const { isSubmitting: submitLoading, withSubmitLock } = useSubmitLock()
const formRef = ref<FormInstance>()

// ── 表单脏检测 ──────────────────────────────────────────
const initialFormSnapshot = ref('')
let watchReady = false
const formDirty = computed(() => {
  if (!watchReady) return false
  return JSON.stringify(formData) !== initialFormSnapshot.value
})
function saveFormSnapshot() { initialFormSnapshot.value = JSON.stringify(formData) }

// ── 离开守卫 ────────────────────────────────────────────
onBeforeRouteLeave((to, from, next) => {
  if (!formDirty.value) { next(); return }
  Modal.confirm({
    title: '确认离开',
    content: '您有未保存的修改，确定要离开吗？',
    okText: '离开',
    cancelText: '继续编辑',
    onOk: () => next(),
    onCancel: () => next(false),
  })
})

// ═══ 表格列配置 ═══
// rowNo 列承载表头「列配置」齿轮（由 ColumnConfigTable 注入），行内渲染行号；
// action 与 rowNo 在 useColumnConfig 中为锁定列，不允许隐藏。
const tableColumns: any[] = [
  { title: '', key: 'rowNo', width: 45, align: 'center' },
  { title: '菜单名称', dataIndex: 'menuName', key: 'menuName', width: 220, ellipsis: true },
  { title: '上级菜单', dataIndex: 'parentName', key: 'parentName', width: 100, ellipsis: true },
  { title: '权限标识', dataIndex: 'menuCode', key: 'menuCode', width: 150, ellipsis: true },
  { title: '路由路径', dataIndex: 'path', key: 'path', width: 120, ellipsis: true },
  { title: '组件路径', dataIndex: 'component', key: 'component', width: 150, ellipsis: true },
  { title: '类型', dataIndex: 'menuType', key: 'menuType', width: 60, align: 'center' },
  { title: '排序', dataIndex: 'sortOrder', key: 'sortOrder', width: 50, align: 'center' },
  { title: '状态', dataIndex: 'status', key: 'status', width: 60, align: 'center' },
  { title: '显示', dataIndex: 'visible', key: 'visible', width: 55, align: 'center' },
  { title: '流向', dataIndex: 'bizFlowTag', key: 'bizFlowTag', width: 75, align: 'center' },
  { title: '模式', dataIndex: 'displayMode', key: 'displayMode', width: 65, align: 'center' },
  { title: '层级', dataIndex: 'menuLevel', key: 'menuLevel', width: 55, align: 'center' },
  { title: '操作', key: 'action', width: 240 },
]

// 将图标字符串转为组件
const iconComponent = (iconName: string) => {
  if (!iconName) return null
  return (Icons as any)[iconName] || null
}

// ═══ 业务流向（前端词表；开发文档 §11 登记为「无落位/构建期常量」） ═══
const BIZ_FLOW_TAG_OPTIONS = [
  { value: '', label: '无' },
  { value: 'sales', label: '销售管理' },
  { value: 'purchase', label: '采购管理' },
  { value: 'warehouse', label: '仓库管理' },
  { value: 'wms', label: 'WMS仓储' },
  { value: 'delivery', label: '配送管理' },
  { value: 'customer', label: '客户关系' },
  { value: 'finance', label: '财务管理' },
  { value: 'expense', label: '费用管理' },
  { value: 'asset', label: '资产管理' },
  { value: 'budget', label: '预算管理' },
  { value: 'mall', label: '商城管理' },
  { value: 'orders', label: '订单中心' },
  { value: 'workflow', label: '工作流' },
  { value: 'product', label: '产品数据' },
  { value: 'printing', label: '打印管理' },
  { value: 'system', label: '系统管理' }
]

// 业务流向标签颜色映射
const bizFlowTagColorMap: Record<string, string> = {
  sales: 'blue',
  purchase: 'cyan',
  warehouse: 'orange',
  wms: 'purple',
  delivery: 'geekblue',
  customer: 'green',
  finance: 'red',
  expense: 'volcano',
  asset: 'gold',
  budget: 'lime',
  mall: 'magenta',
  orders: 'blue',
  workflow: 'cyan',
  product: 'green',
  printing: 'purple',
  system: 'geekblue'
}

function getBizFlowTagLabel(tag: string): string {
  return BIZ_FLOW_TAG_OPTIONS.find(o => o.value === tag)?.label || tag
}

function getBizFlowTagColor(tag: string): string {
  return bizFlowTagColorMap[tag] || 'default'
}

// ═══ 表单数据 ═══
const formData = reactive<MenuUpdateRequest>({
  id: 0,
  parentId: undefined as unknown as number,
  menuName: '',
  menuCode: '',
  menuType: 1,
  icon: '',
  path: '',
  component: '',
  permissions: '',
  sortOrder: 0,
  status: 1,
  visible: 1,
  keepAlive: 0,
  external: 0,
  remark: '',
  bizFlowTag: '',
  displayGroup: 0,
  linkIcon: '',
  displayMode: 0,
  listPath: '',
  tagLabel: '',
  menuLevel: 0
})

// 表单验证规则
const formRules = computed<any>(() => ({
  menuType: [{ required: true, message: '请选择菜单类型', trigger: 'change' }],
  menuName: [{ required: true, message: '请输入菜单名称', trigger: 'blur' }],
  menuCode: [{ required: true, message: '请输入权限标识', trigger: 'blur' }],
  path: formData.menuType === 1 ? [{ required: true, message: '请输入路由路径', trigger: 'blur' }] : [],
  sortOrder: [{ required: true, message: '请输入排序', trigger: 'blur' }]
}))

// ═══ 角色分配弹窗 ═══
const roleDialogVisible = ref(false)
const { isSubmitting: roleSubmitLoading, withSubmitLock: withRoleSubmitLock } = useSubmitLock()
const currentMenu = ref<MenuInfo | null>(null)
const selectedRoles = ref<number[]>([])
const roleList = ref<any[]>([])

/**
 * 刷新菜单缓存并重装动态路由。
 * 返回 false 表示「路由重装失败」（已提示），调用方据此决定是否再报成功。
 */
const refreshAllMenus = async (): Promise<boolean> => {
  let ok = true
  try {
    // 清除菜单缓存，强制从后端重新拉取
    Object.keys(localStorage).forEach(key => {
      if (key.startsWith('menu_cache')) localStorage.removeItem(key)
    })
    const { setupDynamicRoutes } = await import('@/router')
    await setupDynamicRoutes()
  } catch (error) {
    ok = false
    console.error('[菜单管理] 重装动态路由失败', error)
    message.error('菜单路由缓存刷新失败，请重试')
  }
  await loadMenuTree()
  return ok
}

// 加载菜单树
const loadMenuTree = async () => {
  loading.value = true
  hasError.value = false
  try {
    // 只传 tenantId（由 api 层注入）；menuName/menuCode/status 后端不接收 → 本地过滤
    const res = await menuApi.getTree()
    if (res) {
      const rawTree = Array.isArray(res) ? res : [res]
      // 注入 parentName：构建 id→menuName 映射，遍历树填充
      const nameMap = new Map<number, string>()
      const buildNameMap = (nodes: MenuInfo[]) => {
        for (const node of nodes) {
          nameMap.set(node.id, node.menuName)
          if (node.children?.length) buildNameMap(node.children)
        }
      }
      buildNameMap(rawTree)
      const injectParentName = (nodes: MenuInfo[]) => {
        for (const node of nodes) {
          // ⚠️ id / parentId 被 JacksonConfig 序列化为**字符串**（实测 "0" / "60001"），
          // 必须用 Number() 比较，否则顶级节点的「上级菜单」会一律显示 '-'
          const isTop = Number(node.parentId) === 0
          ;(node as any).parentName = !isTop && nameMap.has(node.parentId)
            ? nameMap.get(node.parentId)!
            : isTop ? '顶级菜单' : '-'
          if (node.children?.length) injectParentName(node.children)
        }
      }
      injectParentName(rawTree)
      menuTree.value = rawTree
      applyDefaultExpand()
    } else {
      // 返回空数据 → 清空列表，不做假数据兜底
      message.error('获取菜单列表失败：返回空数据')
      menuTree.value = []
      expandedRowKeys.value = []
    }
  } catch (error) {
    hasError.value = true
    console.error('[菜单管理] 获取菜单列表失败', error)
    message.error('获取菜单列表失败')
    // 失败即清空，禁止假数据兜底
    menuTree.value = []
    expandedRowKeys.value = []
  } finally {
    loading.value = false
    lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
    refreshLoading.value = false
  }
}

// ═══ 查询 / 重置（本地过滤） ═══
function handleSearch() {
  // 过滤生效后自动展开所有命中的父节点，避免「筛了却看不到」的困惑
  isExpandAll.value = false
  expandedRowKeys.value = collectAllParentIds(filteredMenuTree.value)
}

function handleReset() {
  queryForm.menuName = ''
  queryForm.menuCode = ''
  queryForm.status = undefined
  isExpandAll.value = false
  applyDefaultExpand()
}

// 展开/折叠全部
const toggleExpandAll = () => {
  isExpandAll.value = !isExpandAll.value
  if (isExpandAll.value) {
    expandedRowKeys.value = collectAllParentIds(filteredMenuTree.value)
  } else {
    expandedRowKeys.value = []
  }
}

// 刷新菜单缓存（清除路由缓存并重新加载）
const handleRefreshCache = async () => {
  const ok = await refreshAllMenus()
  if (ok) message.success('菜单缓存已刷新')
}

// 新增菜单
const handleAdd = () => {
  dialogTitle.value = '新增菜单'
  resetForm()
  dialogVisible.value = true
  nextTick(() => { saveFormSnapshot(); watchReady = true })
}

// 新增子菜单
const handleAddChild = (row: any) => {
  dialogTitle.value = `新增子菜单 - ${row.menuName}`
  resetForm()
  formData.parentId = row.id
  dialogVisible.value = true
  nextTick(() => { saveFormSnapshot(); watchReady = true })
}

// 编辑菜单
const handleEdit = (row: any) => {
  dialogTitle.value = '编辑菜单'
  resetForm()
  Object.assign(formData, {
    id: row.id,
    // parentId 是字符串（"0" = 顶级）→ 顶级时置空，父级选择器才会显示占位而不错位
    parentId: row.parentId && Number(row.parentId) !== 0 ? row.parentId : undefined,
    menuName: row.menuName,
    menuCode: row.menuCode,
    menuType: row.menuType,
    icon: row.icon || '',
    path: row.path || '',
    component: row.component || '',
    permissions: row.permissions || '',
    sortOrder: row.sortOrder,
    status: row.status,
    visible: row.visible,
    keepAlive: row.keepAlive || 0,
    external: row.external || 0,
    remark: row.remark || '',
    bizFlowTag: row.bizFlowTag || '',
    displayGroup: row.displayGroup ?? 0,
    linkIcon: row.linkIcon || '',
    displayMode: row.displayMode ?? 0,
    listPath: row.listPath || '',
    tagLabel: row.tagLabel || '',
    menuLevel: row.menuLevel ?? 0
  })
  dialogVisible.value = true
  nextTick(() => { saveFormSnapshot(); watchReady = true })
}

// 删除菜单
const handleDelete = async (row: any) => {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除菜单"${row.menuName}"吗？删除后不可恢复！`,
    okText: '确定',
    cancelText: '取消',
    okType: 'danger',
    onOk: async () => {
      try {
        await menuApi.delete(row.id)
        message.success('删除成功')
        await refreshAllMenus()
      } catch (error: any) {
        console.error('[菜单管理] 删除菜单失败', error)
        message.error(error?.message || '删除失败')
      }
    },
    onCancel: () => { /* noop */ }
  })
}

// 提交表单
const handleSubmit = async () => {
  if (!formRef.value) return

  // 表单校验失败：antd 已在控件上标红，不再弹提示（也避免把校验异常误报成「提交失败」）
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  try {
    const result = await withSubmitLock(async () => {
      if (formData.id) {
        await menuApi.update(formData.id, { ...formData, parentId: formData.parentId ?? 0 })
      } else {
        const saveData: MenuSaveRequest = {
          parentId: formData.parentId ?? 0,
          menuName: formData.menuName,
          menuCode: formData.menuCode,
          menuType: formData.menuType,
          icon: formData.icon,
          path: formData.path,
          component: formData.component,
          permissions: formData.permissions,
          sortOrder: formData.sortOrder,
          status: formData.status,
          visible: formData.visible,
          keepAlive: formData.keepAlive,
          external: formData.external,
          remark: formData.remark
        }
        if (formData.bizFlowTag) saveData.bizFlowTag = formData.bizFlowTag
        if (formData.displayGroup !== undefined) saveData.displayGroup = formData.displayGroup
        if (formData.linkIcon) saveData.linkIcon = formData.linkIcon
        saveData.displayMode = formData.displayMode ?? 0
        if (formData.listPath) saveData.listPath = formData.listPath
        if (formData.tagLabel) saveData.tagLabel = formData.tagLabel
        saveData.menuLevel = formData.menuLevel ?? 0
        await menuApi.create(saveData)
      }

      message.success(formData.id ? '更新成功' : '创建成功')
      dialogVisible.value = false
      await refreshAllMenus()
    })
    void result
  } catch (error: any) {
    // 写路径失败保留用户输入（不清表单），否则用户需重新录入
    console.error('[菜单管理] 提交表单失败', error)
    message.error(error?.message || '提交失败')
  }
}

const handleFormClose = () => {
  if (formDirty.value) {
    Modal.confirm({
      title: '确认关闭',
      content: '您有未保存的修改，确定要关闭吗？',
      okText: '确定',
      cancelText: '取消',
      onOk: () => { dialogVisible.value = false },
    })
  } else {
    dialogVisible.value = false
  }
}

const handleFormSaveAndNew = () => {
  handleSubmit()
}

// 状态变更
const handleStatusChange = async (row: any, status: number) => {
  try {
    await menuApi.updateStatus(row.id, status)
    message.success('状态更新成功')
    await refreshAllMenus()
  } catch (error) {
    console.error('[菜单管理] 更新状态失败', error)
    message.error('状态更新失败')
    // 失败回滚开关，避免 UI 与后端不一致
    row.status = status === 1 ? 0 : 1
  }
}

// 分配角色（正向逻辑：遍历所选角色，逐一更新其菜单列表）
const handleAssignRole = async (row: any) => {
  currentMenu.value = row
  selectedRoles.value = []
  roleList.value = []
  roleDialogVisible.value = true

  try {
    const data = await roleApi.listAll()
    roleList.value = Array.isArray(data) ? data : []
    if (roleList.value.length === 0) {
      message.warning('未获取到角色列表')
    }
  } catch (error) {
    console.error('[菜单管理] 获取角色列表失败', error)
    message.error('获取角色列表失败')
    // 失败即清空，禁止假数据兜底
    roleList.value = []
  }
}

// 提交角色分配
const handleRoleSubmit = async () => {
  if (!currentMenu.value) return
  try {
    const menuId = currentMenu.value.id
    const result = await withRoleSubmitLock(async () => {
      // 对每个选中的角色：获取其当前菜单列表，确保包含本菜单 ID，然后更新
      for (const roleId of selectedRoles.value) {
        const currentMenuIds: number[] = (await roleApi.getMenus(roleId) as any) || []
        if (!currentMenuIds.includes(menuId)) {
          await roleApi.assignMenus(roleId, [...currentMenuIds, menuId])
        }
      }
      message.success('角色分配成功')
      roleDialogVisible.value = false
    })
    void result
  } catch (error: any) {
    console.error('[菜单管理] 分配角色失败', error)
    message.error(error?.message || '分配角色失败')
  }
}

// 重置表单
const resetForm = () => {
  formData.id = 0
  formData.parentId = undefined as unknown as number
  formData.menuName = ''
  formData.menuCode = ''
  formData.menuType = 1
  formData.icon = ''
  formData.path = ''
  formData.component = ''
  formData.permissions = ''
  formData.sortOrder = 0
  formData.status = 1
  formData.visible = 1
  formData.keepAlive = 0
  formData.external = 0
  formData.remark = ''
  formData.bizFlowTag = ''
  formData.displayGroup = 0
  formData.linkIcon = ''
  formData.displayMode = 0
  formData.listPath = ''
  formData.tagLabel = ''
  formData.menuLevel = 0
  formRef.value?.clearValidate()
}

// ── 键盘快捷键 ──────────────────────────────────────────
function handleKeydown(e: KeyboardEvent) {
  const tag = (e.target as HTMLElement)?.tagName
  const isInput = tag === 'INPUT' || tag === 'TEXTAREA' || tag === 'SELECT'
  if (e.key === 'F5' || (e.ctrlKey && e.key === 'r')) {
    e.preventDefault()
    debounceClick('refresh', loadMenuTree)()
  }
  if ((e.ctrlKey || e.metaKey) && e.key === 'n' && !isInput) {
    e.preventDefault()
    handleAdd()
  }
}

// 初始化
onMounted(() => {
  loadMenuTree()
  document.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
})

defineExpose({ handleQuery: loadMenuTree })

/** 页面级错误兜底：ErrorBoundary 捕获后统一记录并提示 */
function handleError(error: any) {
  console.error('[菜单管理] 页面错误', error)
  message.error(`页面错误：${error?.message || '未知错误'}`)
}
</script>

<style scoped>
/* ═══ 查询区（横向网格） ═══ */
.search-area {
  padding: 8px 16px;
  background: #fff;
  border-bottom: 1px solid #e8e8e8;
  flex-shrink: 0;
}
.search-row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.search-label {
  font-size: 13px;
  color: #666;
}

/* ═══ 数据口径提示（表格上方） ═══ */
.scope-alert {
  flex-shrink: 0;
  margin: 8px 16px 0;
}

/* ═══ 表格区 ═══ */
/* 必须是 flex 纵向容器：表格组件根元素为 flex:1，父级非 flex 时表格高度会塌陷 */
.table-area {
  flex: 1;
  min-height: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}
/* 树形表格自身是滚动容器（sticky 表头相对它定位） */
.menu-tree-table {
  flex: 1;
  min-height: 0;
  overflow: auto;
}

/* ── a-table 仿 BillDetailTable 样式 ─────────────────────────── */
/* 表格本体：必须 separate + spacing:0，sticky 才能正常工作 */
.menu-tree-table :deep(.ant-table) {
  font-size: 13px;
}
.menu-tree-table :deep(.ant-table table) {
  border-collapse: separate;
  border-spacing: 0;
}
/* 表头行 — sticky 吸顶 */
.menu-tree-table :deep(.ant-table-thead > tr > th) {
  position: sticky;
  top: 0;
  z-index: 10;
  background: #fafafa !important;
  border-right: 1px solid #e8e8e8;
  border-bottom: 1px solid #e8e8e8;
  padding: 0 6px !important;
  text-align: center;
  font-weight: 600;
  color: #262626;
  font-size: 13px;
  height: 32px !important;
  line-height: 32px !important;
  white-space: nowrap;
  vertical-align: middle;
}
.menu-tree-table :deep(.ant-table-thead > tr > th:last-child) {
  border-right: none;
}
/* 数据行 — BillDetailTable .ss-grid td 参数
 * ⚠️ Ant Design Vue 4.x CSS-in-JS 注入 padding，必须 !important 覆盖 */
.menu-tree-table :deep(.ant-table-tbody > tr),
.menu-tree-table :deep(.ant-table-row) {
  height: 28px !important;
}
.menu-tree-table :deep(.ant-table-tbody > tr > td),
.menu-tree-table :deep(.ant-table-cell) {
  border-right: 1px solid #e8e8e8;
  border-bottom: 1px solid #e8e8e8;
  padding: 0 6px !important;
  height: 28px !important;
  max-height: 28px !important;
  line-height: 28px !important;
  font-size: 13px;
  vertical-align: middle;
  box-sizing: border-box;
  overflow: hidden;
}
/* 行内组件尺寸约束，防止撑高行 */
.menu-tree-table :deep(.ant-table-tbody .ant-switch) {
  height: 18px !important;
  line-height: 18px !important;
  min-width: 36px;
  margin: 0;
  vertical-align: middle;
}
.menu-tree-table :deep(.ant-table-tbody .ant-switch .ant-switch-handle) {
  width: 14px;
  height: 14px;
  top: 2px;
  inset-inline-start: 2px;
}
.menu-tree-table :deep(.ant-table-tbody .ant-switch-checked .ant-switch-handle) {
  inset-inline-start: calc(100% - 14px - 2px);
}
.menu-tree-table :deep(.ant-table-tbody .ant-tag) {
  margin: 0 !important;
  padding: 0 4px !important;
  line-height: 18px !important;
  height: 18px !important;
  font-size: 12px;
  vertical-align: middle;
}
/* 操作按钮尺寸约束 */
.menu-tree-table :deep(.ant-table-tbody .ant-btn-link) {
  height: 22px !important;
  line-height: 22px !important;
  padding: 0 4px !important;
  margin: 0 !important;
  font-size: 13px;
  vertical-align: middle;
}
/* 操作列禁止换行 */
.menu-tree-table :deep(.ant-table-tbody > tr > td:last-child) {
  white-space: nowrap !important;
}
/* 全局约束：所有行内元素垂直居中，防止撑高 */
.menu-tree-table :deep(.ant-table-tbody > tr > td *) {
  vertical-align: middle;
}
/* 行悬浮 */
.menu-tree-table :deep(.ant-table-tbody > tr:hover > td),
.menu-tree-table :deep(.ant-table-tbody > tr:hover > .ant-table-cell) {
  background: #f5f7fa !important;
}
/* 空数据提示 */
.menu-tree-table :deep(.ant-table-placeholder) {
  padding: 40px 0;
  font-size: 13px;
  color: #999;
}
/* 表格内容溢出省略 */
.menu-tree-table :deep(.ant-table-cell-ellipsis) {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* ═══ 底部汇总条（树形全量展示，不接分页器） ═══ */
.table-footer-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding: 6px 16px;
  background: #fff;
  border-top: 1px solid #e8e8e8;
  font-size: 13px;
  color: #666;
  flex-wrap: wrap;
}
.footer-left {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}
.footer-item b {
  color: #1890ff;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
}
.footer-right {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 12px;
  color: #999;
}
.footer-hint kbd {
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
}

/* ═══ 单元格 ═══ */
.row-no {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  color: #888;
}
.menu-icon {
  margin-right: 4px;
  font-size: 14px;
  vertical-align: middle;
}
.text-muted {
  color: #bbb;
}

/* ── 树缩进与展开图标 ──────────────────────────── */
.tree-indent {
  display: inline-flex;
  align-items: center;
  white-space: nowrap;
}
.tree-expand-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
  margin-right: 4px;
  cursor: pointer;
  border-radius: 3px;
  font-size: 10px;
  color: #606266;
  transition: background 0.15s;
  flex-shrink: 0;
}
.tree-expand-icon:hover {
  background: #e8e8e8;
}
.tree-expand-placeholder {
  display: inline-block;
  width: 16px;
  height: 16px;
  margin-right: 4px;
  flex-shrink: 0;
}
.tree-node-clickable {
  cursor: pointer;
}
.tree-node-clickable:hover {
  color: #1890ff;
}

/* ── FullScreenDetail 内部紧凑样式 ────────────────────── */
:deep(.fsd-body .ant-form-item) { margin-bottom: 8px; }
:deep(.fsd-body .ant-form-item-label > label) { font-size: 12px; height: 28px; }
:deep(.fsd-body .ant-input), :deep(.fsd-body .ant-input-number), :deep(.fsd-body .ant-select), :deep(.fsd-body .ant-picker), :deep(.fsd-body .ant-cascader-picker) { font-size: 12px; }
:deep(.fsd-body .ant-input-number-input) { font-size: 12px; }
:deep(.fsd-body .ant-select-selection-item) { font-size: 12px; }
:deep(.fsd-body .ant-btn) { font-size: 12px; }

/* ── 紧凑尺寸覆盖：28px 输入框 ──────────────────────── */
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
</style>
