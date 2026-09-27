<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <template #header>
        <div class="role-page-header">
          <div class="role-page-header-left">
            <a-breadcrumb>
              <a-breadcrumb-item>
                <router-link to="/">
                  首页
                </router-link>
              </a-breadcrumb-item>
              <a-breadcrumb-item>角色管理</a-breadcrumb-item>
            </a-breadcrumb>
            <h2 class="role-page-header-title">
              角色管理
            </h2>
          </div>
          <div class="role-page-header-right">
            <span
              v-if="lastUpdateTime"
              class="update-time"
            >更新于 {{ lastUpdateTime }}</span>
            <span
              v-if="autoRefreshCountdown > 0"
              class="auto-refresh-badge"
            >
              <SyncOutlined /> {{ autoRefreshCountdown }}s
            </span>
            <span class="shortcut-hints">
              <span class="shortcut-hint"><kbd>F5</kbd> 刷新</span>
              <span class="shortcut-hint"><kbd>Ctrl</kbd> + <kbd>N</kbd> 新增</span>
              <span class="shortcut-hint">双击行查看详情</span>
            </span>
            <a-button
              size="small"
              :loading="refreshLoading"
              @click="debounceClick('refresh', fetchData)"
            >
              <template #icon>
                <ReloadOutlined />
              </template>
              刷新
            </a-button>
          </div>
        </div>
      </template>

      <div
        ref="tableWrap"
        class="role-management"
      >
        <!-- 统计卡片 -->
        <div class="stat-cards">
          <div class="stat-card stat-total">
            <div class="stat-card-body">
              <div class="stat-card-value">
                {{ pagination.total }}
              </div>
              <div class="stat-card-label">
                角色总数
              </div>
            </div>
            <SafetyOutlined class="stat-card-icon" />
          </div>
          <div class="stat-card stat-active">
            <div class="stat-card-body">
              <div class="stat-card-value">
                {{ activeCount }}
              </div>
              <div class="stat-card-label">
                启用角色
              </div>
            </div>
            <CheckCircleOutlined class="stat-card-icon" />
          </div>
          <div class="stat-card stat-disabled">
            <div class="stat-card-body">
              <div class="stat-card-value">
                {{ disabledCount }}
              </div>
              <div class="stat-card-label">
                停用角色
              </div>
            </div>
            <StopOutlined class="stat-card-icon" />
          </div>
        </div>

        <a-skeleton
          v-if="loading && tableData.length === 0"
          active
          :paragraph="{ rows: 8 }"
          style="padding: 24px;"
        />

        <BillTableList
          ref="tableRef"
          :columns="vxeColumns"
          :data-source="tableDataSource"
          :loading="loading"
          :pagination="pagination"
          :row-key="'id'"
          :min-empty-rows="12"
          :filter-fields="filterFields"
          :show-search="false"
          :selectable="false"
          add-text="新增角色"
          add-permission="tenant-admin:role:create"
          @add="handleAdd"
          @edit="handleEdit"
          @delete="handleDeleteConfirm"
          @refresh="debounceClick('refresh', fetchData)"
          @page-change="handlePageChange"
          @filter-change="handleFilterChange"
        >
          <template #empty>
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
                  @click="debounceClick('refresh', fetchData)"
                >
                  <template #icon>
                    <ReloadOutlined />
                  </template>
                  重新加载
                </a-button>
              </template>
            </a-result>
          </template>

          <template #roleNameCell="{ record }">
            <a-space>
              <a-tag :color="getRoleTypeColor(record.roleType)">
                {{ getRoleTypeName(record.roleType) }}
              </a-tag>
              <span>{{ record.roleName }}</span>
            </a-space>
          </template>
          <template #scopeCell="{ record }">
            <a-tag :color="record.scope === 'PLATFORM' ? 'purple' : 'blue'">
              {{ record.scope === 'PLATFORM' ? '平台级' : '租户级' }}
            </a-tag>
          </template>
          <template #statusCell="{ record }">
            <a-switch
              :checked="record.status === 0"
              checked-children="启用"
              un-checked-children="停用"
              @change="(checked: string | boolean) => { if (typeof checked === 'boolean') handleStatusChange(record, checked) }"
            />
          </template>

          <template #action="{ record }">
            <a-space>
              <a-button
                v-permission="'tenant-admin:role:update'"
                type="link"
                size="small"
                @click="handleEdit(record)"
              >
                编辑
              </a-button>
              <!-- 只保留一个「设置权限」入口：功能权限 / 菜单权限 / 单据类型权限 收进同一个弹窗的 Tab 里
                   （对标 ql361 —— 它的行操作只有 修改 / 删除 / 设置权限 三个，所有授权都在一个面板内完成） -->
              <a-button
                v-permission="'tenant-admin:permission:assign'"
                type="link"
                size="small"
                @click="handlePermission(record)"
              >
                设置权限
              </a-button>
              <a-button
                v-permission="'tenant-admin:role:delete'"
                type="link"
                size="small"
                danger
                @click="handleDeleteConfirm(record)"
              >
                删除
              </a-button>
            </a-space>
          </template>
        </BillTableList>

        <!-- 角色表单弹窗 -->
        <FullScreenDetail
          :visible="modalVisible"
          :title="modalTitle"
          :dirty="formDirty"
          :save-loading="submittingLoading"
          :show-save-and-new="!isEdit"
          @save="handleModalOk"
          @close="handleFormClose"
          @save-and-new="handleFormSaveAndNew"
        >
          <a-form
            ref="formRef"
            :model="formState"
            :rules="formRules"
            :label-col="{ span: 6 }"
            :wrapper-col="{ span: 16 }"
          >
            <a-form-item
              label="角色名称"
              name="roleName"
            >
              <a-input
                v-model:value="formState.roleName"
                placeholder="请输入角色名称"
              />
            </a-form-item>
            <a-form-item
              label="角色编码"
              name="roleCode"
            >
              <a-input
                v-model:value="formState.roleCode"
                placeholder="请输入角色编码"
                :disabled="isEdit"
              />
            </a-form-item>
            <a-form-item
              label="角色类型"
              name="roleType"
            >
              <a-select
                v-model:value="formState.roleType"
                size="small"
                placeholder="请选择角色类型"
              >
                <a-select-option
                  v-for="opt in roleTypeOptions"
                  :key="opt.value"
                  :value="opt.value"
                >
                  {{ opt.label }}
                </a-select-option>
              </a-select>
            </a-form-item>
            <a-form-item
              label="作用域"
              name="scope"
            >
              <a-select
                v-model:value="formState.scope"
                size="small"
                placeholder="请选择作用域"
              >
                <a-select-option value="TENANT">
                  租户级
                </a-select-option>
                <a-select-option value="PLATFORM">
                  平台级
                </a-select-option>
              </a-select>
            </a-form-item>
            <!-- 数据范围（行级数据权限的默认口径，对齐后端 SysRole.dataScope）：
                 此前该字段只在只读详情里展示、表单中没有 → 配了也没入口改（2026-09-20 补） -->
            <a-form-item
              label="数据范围"
              name="dataScope"
            >
              <a-select
                v-model:value="formState.dataScope"
                size="small"
                placeholder="请选择数据范围"
              >
                <a-select-option
                  v-for="opt in dataScopeOptions"
                  :key="opt.value"
                  :value="opt.value"
                >
                  {{ opt.label }}
                </a-select-option>
              </a-select>
              <div class="form-item-hint">
                这个岗位能看到谁的数据。选「自定义」时，请到上方「设置权限 → 数据可见范围」里配置具体范围
              </div>
            </a-form-item>
            <a-form-item
              label="排序"
              name="sort"
            >
              <a-input-number
                v-model:value="formState.sort"
                :min="0"
                style="width: 100%"
              />
            </a-form-item>
            <a-form-item
              label="状态"
              name="status"
            >
              <a-radio-group v-model:value="formState.status">
                <a-radio :value="0">
                  正常
                </a-radio>
                <a-radio :value="1">
                  停用
                </a-radio>
              </a-radio-group>
            </a-form-item>
            <a-form-item
              label="备注"
              name="remark"
            >
              <a-textarea
                v-model:value="formState.remark"
                placeholder="请输入备注"
                :rows="3"
              />
            </a-form-item>
          </a-form>
        </FullScreenDetail>

        <!-- ═══ 角色详情（只读）═══ -->
        <!-- 双击行打开（见下方 useRowDblclick）；:show-footer="false" → 无保存/保存并新增按钮，纯只读查看，不提交任何写操作 -->
        <FullScreenDetail
          :visible="viewVisible"
          :title="viewTitle"
          :show-footer="false"
          @close="handleViewClose"
        >
          <a-skeleton
            v-if="viewLoading"
            active
            :paragraph="{ rows: 8 }"
            style="padding: 8px;"
          />

          <a-result
            v-else-if="viewError"
            status="error"
            title="角色详情加载失败"
            :sub-title="viewErrorMessage"
          >
            <template #extra>
              <a-button
                type="primary"
                @click="debounceClick('viewReload', reloadView)"
              >
                <template #icon>
                  <ReloadOutlined />
                </template>
                重新加载
              </a-button>
            </template>
          </a-result>

          <template v-else-if="viewDetail">
            <!-- 基本信息：字段口径对齐后端 SysRole 实体（GET /api/role/{id} 直接返回 sys_role 行） -->
            <a-descriptions
              :column="2"
              size="small"
              bordered
            >
              <a-descriptions-item label="角色名称">
                {{ viewDetail.roleName || '-' }}
              </a-descriptions-item>
              <a-descriptions-item label="角色编码">
                {{ viewDetail.roleCode || '-' }}
              </a-descriptions-item>
              <a-descriptions-item label="角色类型">
                <a-tag
                  v-if="viewRoleType"
                  :color="viewRoleType.color"
                >
                  {{ viewRoleType.label }}
                </a-tag>
                <span v-else>-</span>
              </a-descriptions-item>
              <a-descriptions-item label="作用域">
                <a-tag :color="viewDetail.scope === 'PLATFORM' ? 'purple' : 'blue'">
                  {{ viewDetail.scope === 'PLATFORM' ? '平台级' : (viewDetail.scope === 'TENANT' ? '租户级' : '-') }}
                </a-tag>
              </a-descriptions-item>
              <a-descriptions-item label="数据范围">
                {{ viewDataScopeLabel }}
              </a-descriptions-item>
              <a-descriptions-item label="排序">
                {{ viewDetail.sort ?? '-' }}
              </a-descriptions-item>
              <a-descriptions-item label="状态">
                <a-tag :color="viewDetail.status === 0 ? 'success' : 'default'">
                  {{ viewStatusText }}
                </a-tag>
              </a-descriptions-item>
              <a-descriptions-item label="角色ID">
                {{ viewDetail.id }}
              </a-descriptions-item>
              <a-descriptions-item label="创建时间">
                {{ viewDetail.createTime || '-' }}
              </a-descriptions-item>
              <a-descriptions-item label="更新时间">
                {{ viewDetail.updateTime || '-' }}
              </a-descriptions-item>
              <a-descriptions-item
                label="备注"
                :span="2"
              >
                {{ viewDetail.remark || '-' }}
              </a-descriptions-item>
            </a-descriptions>

            <!-- 已分配权限：GET /api/role/{id}/permissions（返回 sys_permission.id 清单，下方按模块分组还原权限码） -->
            <div class="detail-section-header">
              <span class="detail-section-title">已分配权限</span>
              <span class="detail-section-count">共 {{ viewPermissionIds.length }} 项</span>
              <span
                v-if="viewPermOptionsLoading"
                class="detail-section-hint"
              >权限名称解析中…</span>
            </div>

            <a-alert
              v-if="viewPermOptionsError && viewPermissionIds.length > 0"
              type="warning"
              show-icon
              message="权限名称解析失败"
              description="未能解析的项仅显示权限 ID，请检查网络或权限后重试"
              style="margin-bottom: 8px"
            >
              <template #action>
                <a-button
                  size="small"
                  @click="debounceClick('viewPermReload', loadPermissionOptions)"
                >
                  重试
                </a-button>
              </template>
            </a-alert>

            <a-empty
              v-if="viewPermissionIds.length === 0"
              description="该角色暂无已分配权限"
            />
            <div
              v-else
              class="detail-perm-groups"
            >
              <div
                v-for="group in viewPermissionGroups"
                :key="group.module"
                class="detail-perm-group"
              >
                <div class="detail-perm-module">
                  模块：{{ group.module }}（{{ group.items.length }}）
                </div>
                <div class="detail-perm-tags">
                  <!-- 展示权限中文名，权限码放进 tooltip 供实施核对：
                       普通管理员看不懂 tenant-admin:role:create 这类编码 -->
                  <a-tooltip
                    v-for="item in group.items"
                    :key="item.id"
                    :title="item.code || `权限ID ${item.id}`"
                    placement="bottom"
                  >
                    <a-tag>{{ item.name || item.code || `#${item.id}` }}</a-tag>
                  </a-tooltip>
                </div>
              </div>
            </div>

            <!-- 已分配菜单：GET /api/role/{id}/menus（返回 sys_menu.id 清单，下方用菜单树还原层级与名称） -->
            <div class="detail-section-header">
              <span class="detail-section-title">已分配菜单</span>
              <span class="detail-section-count">共 {{ viewMenuIds.length }} 项</span>
              <span
                v-if="viewMenuTreeLoading"
                class="detail-section-hint"
              >菜单名称解析中…</span>
            </div>

            <a-alert
              v-if="viewMenuTreeError && viewMenuIds.length > 0"
              type="warning"
              show-icon
              message="菜单名称解析失败"
              description="下面仅显示菜单 ID，请检查网络后重试"
              style="margin-bottom: 8px"
            >
              <template #action>
                <a-button
                  size="small"
                  @click="debounceClick('viewMenuReload', loadMenuTreeForView)"
                >
                  重试
                </a-button>
              </template>
            </a-alert>

            <a-empty
              v-if="viewMenuIds.length === 0"
              description="该角色暂无已分配菜单"
            />
            <a-tree
              v-else-if="viewMenuTree.length > 0"
              :tree-data="viewMenuTree"
              :checkable="false"
              :selectable="false"
              :default-expand-all="true"
            />
            <div
              v-else
              class="detail-id-list"
            >
              <a-tag
                v-for="id in viewMenuIds"
                :key="id"
              >
                #{{ id }}
              </a-tag>
            </div>
          </template>
        </FullScreenDetail>

        <!-- 角色权限设置（对标 ql361 单入口形态：功能权限 / 菜单权限 / 单据类型权限 三个 Tab 收进同一个弹窗） -->
        <a-modal
          :open="permMatrixVisible"
          :title="`角色权限设置（${permMatrixRoleName}）`"
          width="1040px"
          :confirm-loading="permMatrixSaving"
          ok-text="保存"
          cancel-text="关闭"
          @ok="handlePermSave"
          @cancel="handlePermCancel"
        >
          <a-spin :spinning="permMatrixLoading">
            <!-- 六个 Tab 分别管什么，先用白话讲清楚，避免管理员在术语里迷路 -->
            <a-alert
              type="info"
              show-icon
              class="perm-intro"
            >
              <template #message>
                <div class="perm-intro__title">
                  这个岗位「能做什么、能看什么」都在这里设置
                </div>
                <div class="perm-intro__body">
                  <b>功能权限</b>：能打开哪些功能；<b>菜单权限</b>：左侧能看到哪些菜单；
                  <b>单据权限</b>：对各类单据能查看、编辑还是审核。
                </div>
                <div class="perm-intro__body">
                  后三项是精细化管控，按需设置：<b>敏感信息保护</b>（手机号、成本价等字段打码或隐藏）、
                  <b>数据可见范围</b>（能看到谁的数据）、<b>精细数据规则</b>（更复杂的条件限制）。
                </div>
              </template>
            </a-alert>
            <a-tabs
              v-model:active-key="permTabKey"
              class="perm-tabs"
            >
              <!-- Tab1 功能权限：左侧域树 + 右侧 功能名称 × 7 类操作 勾选矩阵（原样保留，交互与样式不变） -->
              <a-tab-pane
                key="feature"
                tab="功能权限"
              >
                <div class="perm-matrix">
                  <!-- 左侧：域列表 -->
                  <div class="perm-matrix__side">
                    <div class="perm-matrix__side-title">
                      功能域
                    </div>
                    <a-input
                      v-model:value="permMatrixDomainKeyword"
                      placeholder="筛选域"
                      size="small"
                      allow-clear
                      style="margin-bottom: 8px"
                    />
                    <ul class="perm-matrix__domains">
                      <li
                        v-for="d in filteredDomains"
                        :key="d.key"
                        class="perm-matrix__domain"
                      >
                        <div
                          class="perm-matrix__domain-head"
                          @click="toggleDomainExpand(d.key)"
                        >
                          <span class="perm-matrix__domain-name">{{ d.label }}</span>
                          <span class="perm-matrix__domain-count">{{ d.modules.length }}</span>
                        </div>
                        <ul
                          v-show="permMatrixExpandedDomains.includes(d.key)"
                          class="perm-matrix__modules"
                        >
                          <li
                            v-for="m in d.modules"
                            :key="m.key"
                            :class="{ active: d.key === permMatrixDomainKey && m.key === permMatrixModuleKey }"
                            @click="selectModule(d.key, m.key)"
                          >
                            <span>{{ m.label }}</span>
                            <span class="cnt">{{ m.permCount }}</span>
                          </li>
                        </ul>
                      </li>
                      <li
                        v-if="!filteredDomains.length"
                        class="perm-matrix__domain-empty"
                      >
                        无匹配功能域
                      </li>
                    </ul>
                  </div>

                  <!-- 右侧：权限矩阵 -->
                  <div class="perm-matrix__main">
                    <div class="perm-matrix__toolbar">
                      <span class="perm-matrix__summary">
                        已选 <b>{{ permMatrixCheckedCount }}</b> 项权限
                      </span>
                      <!-- 本模块内「勾了也不会生效」的权限码条数：让管理员一眼知道这页有多少是空挂的 -->
                      <a-tooltip
                        v-if="currentModuleStaleCount > 0"
                        placement="bottom"
                        title="标「未生效」的权限码没有任何后端接口或前端按钮使用，勾选不会控制任何功能（可能是尚未接线的新功能）"
                      >
                        <span class="perm-matrix__stale-hint">
                          本模块 <b>{{ currentModuleStaleCount }}</b> 项未生效
                        </span>
                      </a-tooltip>
                      <a-space :size="8">
                        <a-checkbox
                          :checked="currentModuleAllChecked"
                          :indeterminate="currentModuleIndeterminate"
                          @change="(e: any) => toggleDomain(!!e.target.checked)"
                        >
                          当前模块全选
                        </a-checkbox>
                      </a-space>
                    </div>
                    <div class="perm-matrix__table-wrap">
                      <table class="perm-matrix__table">
                        <thead>
                          <tr>
                            <th class="col-no">
                              行号
                            </th>
                            <th class="col-module">
                              所属模块
                            </th>
                            <th class="col-rowcheck" />
                            <th class="col-feature">
                              功能名称
                            </th>
                            <th
                              v-for="col in ACTION_COLUMNS"
                              :key="col.key"
                              class="col-action"
                            >
                              {{ col.title }}
                            </th>
                          </tr>
                        </thead>
                        <tbody>
                          <tr
                            v-for="(row, idx) in permMatrixRows"
                            :key="row.featureKey"
                          >
                            <td class="col-no">
                              {{ idx + 1 }}
                            </td>
                            <td class="col-module">
                              {{ currentModule?.label || '-' }}
                            </td>
                            <td class="col-rowcheck">
                              <a-checkbox
                                :checked="row.allChecked"
                                :indeterminate="row.allIndeterminate"
                                @change="(e: any) => toggleRow(row, !!e.target.checked)"
                              />
                            </td>
                            <td
                              class="col-feature"
                              :class="{ 'col-feature--stale': row.allStale }"
                            >
                              {{ row.featureName }}
                              <!-- 该功能点下所有权限码都没有消费方：勾了不会生效（判定见 isIneffective 注释） -->
                              <a-tag
                                v-if="row.allStale"
                                class="perm-matrix__stale-tag"
                                title="该功能点下没有任何权限码被后端接口或前端按钮使用，勾选不会生效"
                              >
                                未生效
                              </a-tag>
                            </td>
                            <td
                              v-for="col in ACTION_COLUMNS"
                              :key="col.key"
                              class="col-action"
                            >
                              <!-- 悬停显示该格包含的原始权限名：本系统权限码粒度比 ql361 细，
                                   6 列是展示口径，不能因此把 转正离职/审批/规则配置 这类权限藏起来。
                                   同时逐条标注「未生效」（该权限码没有任何接口/按钮消费方）。 -->
                              <a-tooltip
                                v-if="row.cells[col.key].ids.length"
                                placement="bottom"
                              >
                                <template #title>
                                  <div
                                    v-for="(label, li) in row.cells[col.key].labels"
                                    :key="li"
                                  >
                                    {{ label }}
                                    <span v-if="isIneffective(row.cells[col.key].codes[li])">— 未生效</span>
                                  </div>
                                </template>
                                <span
                                  class="perm-matrix__cell"
                                  :class="{ 'perm-matrix__cell--stale': row.cells[col.key].stale }"
                                >
                                  <a-checkbox
                                    :checked="row.cells[col.key].checked"
                                    :indeterminate="row.cells[col.key].indeterminate"
                                    @change="(e: any) => toggleCell(row, col.key, !!e.target.checked)"
                                  />
                                  <span
                                    v-if="row.cells[col.key].ids.length > 1"
                                    class="perm-matrix__cnt"
                                  >{{ row.cells[col.key].ids.length }}</span>
                                </span>
                              </a-tooltip>
                              <span
                                v-else
                                class="perm-matrix__none"
                              >-</span>
                            </td>
                          </tr>
                          <tr v-if="!permMatrixRows.length">
                            <td
                              :colspan="4 + ACTION_COLUMNS.length"
                              class="perm-matrix__empty"
                            >
                              请选择左侧功能域
                            </td>
                          </tr>
                        </tbody>
                      </table>
                    </div>
                  </div>
                </div>
              </a-tab-pane>

              <!-- Tab2 菜单权限：沿用原「菜单」弹窗的菜单树（key 为 menu id，与角色-菜单关联表口径一致） -->
              <a-tab-pane
                key="menu"
                tab="菜单权限"
              >
                <a-alert
                  message="勾选这个岗位能看到哪些菜单"
                  type="info"
                  show-icon
                  style="margin-bottom: 16px"
                />
                <a-tree
                  v-model:checked-keys="checkedMenuKeys"
                  :tree-data="menuTree"
                  checkable
                  :default-expand-all="true"
                  :selectable="false"
                />
              </a-tab-pane>

              <!-- Tab3 单据类型权限：沿用原「单据权限」弹窗的单据表（权限级别 0-无权限 1-查看 2-编辑 3-审核） -->
              <a-tab-pane
                key="billType"
                tab="单据权限"
              >
                <a-alert
                  message="设置这个岗位对各类单据能做到哪一步"
                  type="info"
                  show-icon
                  style="margin-bottom: 16px"
                />
                <a-table
                  :data-source="billTypeData"
                  :columns="billTypeColumns"
                  :pagination="false as any"
                  size="small"
                  bordered
                >
                  <template #bodyCell="{ column, record }">
                    <template v-if="column.key === 'level'">
                      <a-select
                        v-model:value="record.permissionLevel"
                        style="width: 120px"
                        size="small"
                      >
                        <a-select-option :value="0">
                          无权限
                        </a-select-option>
                        <a-select-option :value="1">
                          查看
                        </a-select-option>
                        <a-select-option :value="2">
                          编辑
                        </a-select-option>
                        <a-select-option :value="3">
                          审核
                        </a-select-option>
                      </a-select>
                    </template>
                  </template>
                </a-table>
              </a-tab-pane>

              <!-- Tab4 字段权限：表 × 字段的可见性与脱敏（sys_field_permission，此前零入口） -->
              <a-tab-pane
                key="field"
                tab="敏感信息保护"
              >
                <!-- 只读态说明原因，否则管理员会以为界面坏了 -->
                <a-alert
                  v-if="readonlyFieldPerm"
                  type="warning"
                  show-icon
                  style="margin-bottom: 12px"
                  message="当前账号没有配置敏感信息保护的权限，以下内容仅供查看"
                />
                <RoleFieldPermissionTab
                  ref="fieldPermTabRef"
                  v-if="permMatrixVisible"
                  :role-id="permMatrixRoleId"
                  :readonly="readonlyFieldPerm"
                />
              </a-tab-pane>

              <!-- Tab5 数据范围：行级可见范围规则（sys_data_scope，此前零入口） -->
              <a-tab-pane
                key="dataScope"
                tab="数据可见范围"
              >
                <a-alert
                  v-if="readonlyDataScope"
                  type="warning"
                  show-icon
                  style="margin-bottom: 12px"
                  message="当前账号没有配置数据可见范围的权限，以下内容仅供查看"
                />
                <RoleDataScopeTab
                  ref="dataScopeTabRef"
                  v-if="permMatrixVisible"
                  :role-id="permMatrixRoleId"
                  :readonly="readonlyDataScope"
                />
              </a-tab-pane>

              <!-- Tab6 数据规则：Odoo 式记录级 domain 规则（sys_record_rule，此前零入口） -->
              <a-tab-pane
                key="recordRule"
                tab="精细数据规则"
              >
                <a-alert
                  type="warning"
                  show-icon
                  style="margin-bottom: 12px"
                >
                  <template #message>
                    这一页的修改是即时保存的（新增/编辑点确定即提交），不需要点下方「保存」。
                  </template>
                  <template #description>
                    注意：记录规则目前<b>尚未接入业务查询</b> —— 规则能保存，但还不会实际影响任何数据的可见范围。
                    请勿据此做安全判断；需要按范围管控数据请用「数据可见范围」。
                  </template>
                </a-alert>
                <a-alert
                  v-if="readonlyRecordRule"
                  type="warning"
                  show-icon
                  style="margin-bottom: 12px"
                  message="当前账号没有配置精细数据规则的权限，以下内容仅供查看"
                />
                <RoleRecordRuleTab
                  v-if="permMatrixVisible"
                  :role-id="permMatrixRoleId"
                  :readonly="readonlyRecordRule"
                />
              </a-tab-pane>
            </a-tabs>
          </a-spin>
        </a-modal>
      </div>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, nextTick, onMounted, onUnmounted, h } from 'vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import { useRowDblclick } from '@/composables/useRowDblclick'
import { useRouter, onBeforeRouteLeave } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
import { SafetyOutlined, CheckCircleOutlined, StopOutlined, ReloadOutlined, SyncOutlined, WarningOutlined } from '@ant-design/icons-vue'
import { roleApi, type RoleInfo } from '@/api/role'
import menuApi from '@/api/menu'
import { roleBillTypeApi, type BillTypeDetail } from '@/api/roleBillType'
import { permissionApi } from '@/api/permission'
import { dictItemApi } from '@/api/dict'
import { useSubmitLock } from '@/composables'
import { useUserStore } from '@/stores/user'
import BillTableList, { type FilterField } from '@/components/BillTableList/BillTableList.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import FullScreenDetail from '@/components/FullScreenDetail/FullScreenDetail.vue'
// 细粒度权限三个 Tab（字段权限 / 数据范围 / 数据规则）：此前后端与表齐全但零前端入口
import RoleFieldPermissionTab from './components/RoleFieldPermissionTab.vue'
import RoleDataScopeTab from './components/RoleDataScopeTab.vue'
import RoleRecordRuleTab from './components/RoleRecordRuleTab.vue'
import { isWriteFailed } from '@/utils/writeResult'

const userStore = useUserStore()
const lastUpdateTime = ref('')
const autoRefreshCountdown = ref(0)
const refreshLoading = ref(false)
const hasError = ref(false)
let refreshTimer: ReturnType<typeof setInterval> | null = null
let countdownTimer: ReturnType<typeof setInterval> | null = null
const searchForm = reactive({ roleName: '', roleCode: '', status: undefined as number | undefined })

const tableData = ref<RoleInfo[]>([])
const loading = ref(false)

const pagination = reactive({ current: 1, pageSize: 20, total: 0, showSizeChanger: true, showQuickJumper: true, showTotal: (total: number) => `共 ${total} 条` })

// ── 统计数据 ────────────────────────────────────────────
const activeCount = computed(() => tableData.value.filter(r => r.status === 0).length)
const disabledCount = computed(() => tableData.value.filter(r => r.status === 1).length)

// ── 数据源 ────────────────────────────────────────────
const tableDataSource = tableData

const vxeColumns = computed(() => [
  { field: 'roleName', title: '角色信息', width: 200, slotName: 'roleNameCell' },
  { field: 'roleCode', title: '角色编码', width: 150 },
  { field: 'scope', title: '作用域', width: 100, slotName: 'scopeCell' },
  { field: 'sort', title: '排序', width: 80 },
  { field: 'status', title: '状态', width: 100, slotName: 'statusCell' },
  { field: 'createTime', title: '创建时间', width: 160 },
  { field: 'remark', title: '备注', showOverflow: 'tooltip' },
  { type: 'action', title: '操作', width: 220, fixed: 'right' }
])

const filterFields: FilterField[] = [
  { key: 'roleName', label: '角色名称', type: 'input', placeholder: '请输入角色名称' },
  { key: 'roleCode', label: '角色编码', type: 'input', placeholder: '请输入角色编码' },
  { key: 'scope', label: '作用域', type: 'select', options: [{ label: '平台级', value: 'PLATFORM' }, { label: '租户级', value: 'TENANT' }] },
  { key: 'status', label: '状态', type: 'select', options: [{ label: '正常', value: 0 }, { label: '停用', value: 1 }] },
]

const modalVisible = ref(false)
const { isSubmitting: submittingLoading, withSubmitLock } = useSubmitLock()
const modalTitle = computed(() => isEdit.value ? '编辑角色' : '新增角色')
const isEdit = ref(false)
const formRef = ref<FormInstance>()

// ── 角色类型选项（从API加载） ──────────────────────────
const roleTypeOptions = ref<{ label: string; value: number }[]>([])

async function loadRoleTypeOptions() {
  try {
    const res = await dictItemApi.getByDictCode('ROLE_TYPE')
    // ⚠️ 响应拦截器已拆包：res 即数组本体，旧写法 `if (res.data)` 恒 false → 角色类型下拉恒空
    const items = (Array.isArray(res) ? res : ((res as any)?.data ?? [])) as Array<{ sortOrder: number; itemText: string; itemValue: string }>
    roleTypeOptions.value = items
      .sort((a, b) => a.sortOrder - b.sortOrder)
      .map(item => ({ label: item.itemText, value: Number(item.itemValue) }))
  } catch (err) {
    console.warn('[角色管理] 加载角色类型失败', err)
  }
}

const formState = reactive({ id: 0, roleName: '', roleCode: '', roleType: 1, scope: 'TENANT', dataScope: 0, sort: 0, status: 0, remark: '' })
const formRules: any = {
  roleName: { required: true, message: '请输入角色名称', trigger: 'blur' },
  roleCode: [{ required: true, message: '请输入角色编码', trigger: 'blur' }, { pattern: /^[a-zA-Z_][a-zA-Z0-9_]*$/, message: '编码只能包含字母、数字和下划线', trigger: 'blur' }]
}

// ── 权限矩阵弹窗（对标 ql361「角色权限设置」2026-09-19 实抓） ──────────────────
// ql361 形态 = 左侧域树 + 右侧「行号｜所属模块｜☑｜功能名称｜查看｜打印｜增加｜删除｜修改｜导出」勾选矩阵。
// 数据源 sys_permission（454 条），permission_code 形如 `hr:employee:create`：
//   · 域（左树一级）= code 第一段；功能点（矩阵行）= 去掉动作段的完整前缀；动作 = 最后一段
//   · 动作按 ACTION_COLUMNS 归入 6 列，未列入的动作一律落「修改」列（语义上都是变更类操作）
// ⚠️ 改造前的老实现用 menuApi.getTree() 的**菜单 id** 去回显/提交 permission id 接口，
//    两套 ID 混用导致回显与保存都不生效；本实现统一以 sys_permission.id（字符串）为准。
const permMatrixVisible = ref(false)
const permMatrixLoading = ref(false)
const permMatrixSaving = ref(false)
const permMatrixRoleId = ref<string>('')
const permMatrixRoleName = ref('')
const permMatrixDomainKey = ref('')
const permMatrixModuleKey = ref('')
const permMatrixExpandedDomains = ref<string[]>([])
const permMatrixDomainKeyword = ref('')
/**
 * 细粒度权限 Tab 的只读态：**按 Tab 各自判断**。
 * 原实现是三个权限码取「或」，结果是只有功能权限分配权的人也能编辑字段权限，
 * 用户填完才被后端的 @SaCheckPermission 拒绝 —— 这里改成各 Tab 用各自的权限码。
 */
const readonlyFieldPerm = computed(() => !userStore.hasPermission('tenant-admin:field-permission:assign'))
const readonlyDataScope = computed(() => !userStore.hasPermission('tenant-admin:data-scope:assign'))
// 记录规则后端已补齐 system:record-rule:* 权限码（此前该 Controller 无任何权限校验）
const readonlyRecordRule = computed(() => !userStore.hasPermission('tenant-admin:record-rule:update'))

/**
 * 「敏感信息保护」「数据可见范围」两个 Tab 的子组件引用。
 *
 * 这两个 Tab 是**全量覆盖**式保存（提交时以列表为准），原先各自只有 Tab 内的保存按钮：
 * 用户在 Tab 里改完直接点弹窗底部的「保存」，弹窗关闭、组件卸载，改动就**静默丢失**了。
 * 现在底部「保存」会一并提交它们，关闭前也会拦截未保存的改动。
 *
 * 「精细数据规则」不在此列 —— 它是即时提交（新增/编辑点确定即落库），没有暂存态。
 */
const fieldPermTabRef = ref<{ save: () => Promise<boolean>; hasChanges: boolean } | null>(null)
const dataScopeTabRef = ref<{ save: () => Promise<boolean>; hasChanges: boolean } | null>(null)

/** 两个全量覆盖型 Tab 是否有未保存改动（关闭弹窗时据此拦截） */
const hasUnsavedFineGrained = computed(() =>
  !!fieldPermTabRef.value?.hasChanges || !!dataScopeTabRef.value?.hasChanges
)
/** 合并弹窗当前激活的 Tab：feature-功能权限 / menu-菜单权限 / billType-单据类型权限 */
const permTabKey = ref<string>('feature')

/**
 * 命名空间（`permission_code` 第一段）→ 中文模块名。
 * 注意：**这是域内的二级模块名**，不是业务域 —— 业务域的归并见 `DOMAIN_RULES`。
 * 理由：第一段是开发命名空间（`system`/`user`/`erp`…），一个业务域下往往有多个，
 * 二级展示既保住了开发侧的可读性，又不会让用户看到一堆伪业务域。
 */
const MODULE_LABELS: Record<string, string> = {
  system: '系统管理', user: '用户', role: '角色', permission: '权限', department: '组织架构',
  position: '岗位', tenant: '租户', 'data-permission': '数据权限', 'permission-template': '权限模板',
  'role-inheritance': '角色继承', log: '日志', datasource: '数据源', workflow: '工作流', set: '设置',
  platform: '平台管理', hr: '人力资源', crm: '客户关系', erp: 'ERP 通用',
  sale: '销售', purchase: '采购', stock: '库存', finance: '财务',
  payment: '收付款', dms: '配送',
}
/** 全量权限池（仅正常态：status=0） */
const permPool = ref<Array<{ id: string; code: string; name: string }>>([])
/** 工作副本：当前勾选的 permission id 集合（统一字符串，规避雪花 ID 精度丢失） */
const permCheckedIds = ref<Set<string>>(new Set())
/** 打开弹窗时的原始授权集合：保存前据此算出「本次新增/移除」明细，让改动可见 */
const permOriginalIds = ref<Set<string>>(new Set())

/**
 * 三个维度的「回显是否成功」标记 —— 回显失败的维度**禁止提交**。
 *
 * 原因：`POST /role/{id}/permissions`、`/menus`、`/bill-types` 都是**全量覆盖**语义，
 * 后端收到空数组即执行 delete 清空（见 SysRoleServiceImpl.assignPermissions 的空列表分支）。
 * 若回显失败后仍允许保存，就会把该角色「已有的授权」当成「用户取消勾选」提交上去，
 * 静默清空权限，而界面上看不出删了什么。
 */
const permTabLoaded = ref(false)
const menuTabLoaded = ref(false)
const billTypeTabLoaded = ref(false)

// ── 权限生效性（「勾了到底管不管用」） ───────────────────────────────
/**
 * 无消费方的权限码集合：后端没有 @SaCheckPermission/@RequirePermission 注解、前端也没有
 * v-permission 指令或 checkPermission() 调用 —— 勾进角色不会控制任何东西。
 *
 * 数据来自 `GET /permission/effectivity`（由 tools/gen-permission-effectivity.py 扫描源码生成）。
 * 实测 474 条权限码里 151 条属此类。矩阵据此**只做视觉标注、不禁用勾选**：
 * 这些码可能是尚未接线的新功能，禁用会让管理员无法预配置。
 */
const ineffectiveCodes = ref<Set<string>>(new Set())
/** 清单是否已加载（失败也置 true，避免每次开弹窗重试打日志） */
const effectivityLoaded = ref(false)

/** 该权限码是否「未生效」（无任何消费方） */
function isIneffective(code: string): boolean {
  return !!code && ineffectiveCodes.value.has(code)
}

/**
 * 加载权限生效性清单，失败静默降级（不标注），绝不阻断权限配置本身。
 */
async function loadPermissionEffectivity() {
  if (effectivityLoaded.value) return
  try {
    const res = await permissionApi.getEffectivity()
    // 兼容两种返回：拦截器已拆包（直接是清单对象）或未拆包（{ data: 清单 }）
    type EffectivityShape = { available?: boolean; ineffective?: string[] }
    const raw = res as unknown as EffectivityShape & { data?: EffectivityShape }
    const payload: EffectivityShape | undefined = Array.isArray(raw?.ineffective) ? raw : raw?.data
    if (payload && payload.available !== false && Array.isArray(payload.ineffective)) {
      ineffectiveCodes.value = new Set(payload.ineffective)
    }
  } catch (err) {
    console.warn('[系统管理] 权限生效性清单加载失败，本次不做「未生效」标注', err)
  } finally {
    effectivityLoaded.value = true
  }
}

// ── 菜单权限（合并弹窗 Tab2） ───────────────────────────────
const checkedMenuKeys = ref<number[]>([])
const menuTree = ref<any[]>([])

// ── 单据类型权限（合并弹窗 Tab3） ───────────────────────────
const billTypeData = ref<BillTypeDetail[]>([])
const billTypeColumns = [
  { title: '单据类型', dataIndex: 'billTypeName', key: 'billTypeName', width: 150 },
  { title: '能做什么', key: 'level', width: 180 },
  // 类型编码是开发/实施口径，放在最后，业务管理员一般不需要看
  { title: '类型编码', dataIndex: 'billType', key: 'billType', width: 100 },
]

// ── debounceClick ──────────────────────────────────────────
const clickLocks = new Map<string, boolean>()
function debounceClick(key: string, fn: () => void) {
  if (clickLocks.get(key)) return
  clickLocks.set(key, true)
  try { fn() } finally { setTimeout(() => clickLocks.set(key, false), 300) }
}

// ── Keyboard shortcuts ─────────────────────────────────────
function handleKeydown(e: KeyboardEvent) {
  const tag = (e.target as HTMLElement)?.tagName
  if (['INPUT', 'TEXTAREA', 'SELECT'].includes(tag)) return
  if (e.key === 'F5' || (e.ctrlKey && e.key === 'r')) { e.preventDefault(); debounceClick('refresh', fetchData) }
  if ((e.ctrlKey || e.metaKey) && e.key === 'n') { e.preventDefault(); handleAdd() }
}

// ── Form dirty tracking ────────────────────────────────────
const initialFormSnapshot = ref('')
const watchReady = ref(false)
const formDirty = computed(() => {
  if (!watchReady.value) return false
  return initialFormSnapshot.value !== JSON.stringify(formState)
})
function saveFormSnapshot() {
  initialFormSnapshot.value = JSON.stringify(formState)
}

onBeforeRouteLeave((to, from, next) => {
  if (formDirty.value) {
    Modal.confirm({
      title: '确认离开', content: '当前表单未保存，确定要离开吗？', okText: '确定', cancelText: '取消',
      onOk() { next() }, onCancel() { next(false) }
    })
  } else { next() }
})

const fetchData = async () => {
  loading.value = true
  hasError.value = false
  try {
    const res = await roleApi.getPage({ tenantId: userStore.tenantId, ...searchForm, current: pagination.current, size: pagination.pageSize } as any)
    if (res) { tableData.value = res.records || []; pagination.total = res.total || 0 }
  } catch (err) {
    hasError.value = true
    console.warn('[系统管理] 加载角色数据失败', err)
    tableData.value = []
    pagination.total = 0
  } finally {
    loading.value = false
    lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
    refreshLoading.value = false
  }
}

const handleFilterChange = (filters: Record<string, any>) => {
  if (Object.keys(filters).length === 0) Object.assign(searchForm, { roleName: '', roleCode: '', status: undefined })
  else Object.assign(searchForm, filters)
  pagination.current = 1; fetchData()
}

const handlePageChange = (page: number, pageSize: number) => {
  pagination.current = page; pagination.pageSize = pageSize; fetchData()
}

const handleAdd = () => {
  isEdit.value = false
  Object.assign(formState, { id: 0, roleName: '', roleCode: '', roleType: 1, scope: 'TENANT', dataScope: 0, sort: 0, status: 0, remark: '' })
  modalVisible.value = true
  nextTick(() => { saveFormSnapshot(); watchReady.value = true })
}

const handleEdit = (record: RoleInfo) => {
  isEdit.value = true
  Object.assign(formState, { id: record.id, roleName: record.roleName, roleCode: record.roleCode, roleType: record.roleType, scope: record.scope || 'TENANT', dataScope: record.dataScope ?? 0, sort: record.sort, status: record.status, remark: record.remark })
  modalVisible.value = true
  nextTick(() => { saveFormSnapshot(); watchReady.value = true })
}

const handleModalOk = async () => {
  try {
    const result = await withSubmitLock(async () => {
      await formRef.value?.validate()
      if (isEdit.value) { await roleApi.update(formState.id, formState); message.success('更新成功') }
      else { await roleApi.create(formState); message.success('创建成功') }
      modalVisible.value = false; fetchData()
    })
    void result
  } catch (error: any) { if (error) message.error(error?.message || '操作失败') }
}

const handleFormClose = () => {
  if (formDirty.value) {
    Modal.confirm({
      title: '确认关闭', content: '当前表单未保存，确定要关闭吗？', okText: '确定', cancelText: '取消',
      onOk() { modalVisible.value = false; watchReady.value = false; formRef.value?.resetFields() }
    })
  } else {
    modalVisible.value = false
    watchReady.value = false
    formRef.value?.resetFields()
  }
}

const handleFormSaveAndNew = async () => {
  try {
    const result = await withSubmitLock(async () => {
      await formRef.value?.validate()
      await roleApi.create(formState)
      message.success('创建成功')
      fetchData()
      handleAdd()
    })
    void result
  } catch (error: any) { if (error) message.error(error?.message || '操作失败') }
}

const handleDeleteConfirm = (record: RoleInfo) => {
  Modal.confirm({
    title: '确认删除', content: `确定要删除角色 "${record.roleName}" 吗？此操作不可撤销。`, okText: '确认删除', okType: 'danger', cancelText: '取消', centered: true,
    async onOk() {
      try {
        const delRes = await roleApi.delete(record.id)
        // 后端可能以 false / {success:false} 表达「没删成」（角色已分配给用户等）
        if (isWriteFailed(delRes)) { message.error('删除失败：角色不存在或已分配给用户，不允许删除'); return }
        message.success('删除成功')
        fetchData()
      } catch (error: any) { message.error(error.message || '删除失败') }
    }
  })
}

const handleStatusChange = async (record: RoleInfo, checked: boolean) => {
  const newStatus = checked ? 0 : 1
  try { await roleApi.updateStatus(record.id, newStatus); message.success('状态更新成功'); fetchData() } catch (err) { console.warn('[系统管理] 更新角色状态失败', err); message.error('状态更新失败') }
}

// ═══ 权限矩阵（ql361「角色权限设置」对标实现） ═══════════════════════════════

type ActionKey = 'view' | 'print' | 'create' | 'delete' | 'update' | 'export' | 'allow'

/**
 * 矩阵列：6 类标准 CRUD 操作 + 1 列「允许」。
 *
 * 前 6 列与 ql361 业务域完全一致；**第 7 列「允许」对应 ql361 的「特殊权限 / 价格权限」**。
 * 依据：2026-09-19 实抓 ql361「角色权限设置」面板确认 —— 它是两类形态：
 *   · 业务域（销售/采购/仓储/…）：`行号 | 所属模块 | ☑ | 功能名称 | 查看/打印/增加/删除/修改/导出`
 *   · 特殊域（特殊权限 / 价格权限 / 驾驶舱）：`行号 | 所属模块 | 功能名称 | 允许` ← **只有一列开关**
 *   例：特殊权限有「反审核 / 红冲单据 / 客商合并 / 强制结算 / 修改单据日期」等 27 项；
 *       价格权限有「成本查看 / 修改单价 / 修改折扣 / 修改税率 / 批发价查看」等 18 项。
 * 故甲方把「非 CRUD 的开关型权限」也收进同一张矩阵的「允许」列（比 ql361 分两个域更好查全），
 * 而**不再塞进「修改」列** —— 否则「修改」列的语义会被反审核/审批/规则配置之类污染。
 */
const ACTION_COLUMNS: Array<{ key: ActionKey; title: string }> = [
  { key: 'view', title: '查看' },
  { key: 'print', title: '打印' },
  { key: 'create', title: '增加' },
  { key: 'delete', title: '删除' },
  { key: 'update', title: '修改' },
  { key: 'export', title: '导出' },
  { key: 'allow', title: '允许' },
]

/**
 * 动作码 → 矩阵列。**未命中的动作一律归「允许」列**（开关型权限，对齐 ql361 的特殊权限/价格权限）。
 *
 * ⚠️ `update` 必须**显式列出**：它是最高频的动作（`:update` 54 条 + `:edit` 13 条 + `:update-status` 6 条），
 * 早先靠「兜底」落进来，一旦兜底改成 allow 就会整列掉进「允许」（本轮踩过：`:update` 全部落错列）。
 * 规则：**能表达为"改动既有数据"的都归 update**（含审批/分配/配置/流转），
 * 只有工具类、动作语义无法用 CRUD 表达（manage/check/cache/simulate/test…）才落「允许」。
 */
const ACTION_BUCKETS: Record<Exclude<ActionKey, 'allow'>, string[]> = {
  view: ['list', 'view', 'detail', 'query', 'search', 'stats', 'report', 'analysis', 'diagram', 'detailrefresh', 'candidates', 'auto', 'clock'],
  print: ['print', 'downloadpdf'],
  create: ['create', 'add', 'apply', 'submit', 'import', 'dataimport', 'register', 'generate', 'convert', 'batchconvert', 'convertfromdetail', 'renewapply', 'addtolevel'],
  delete: ['delete', 'remove', 'cancel', 'cancelconfirm', 'clear', 'reset', 'reset-password', 'kickout'],
  update: [
    'update', 'edit', 'modify', 'update-status', 'status',
    'approve', 'approval', 'batchapprove', 'audit', 'confirm',
    'assign', 'assign-role', 'assign-menu', 'assign-permission', 'batchassign', 'reassign',
    'config', 'rule', 'quota', 'policy', 'publish', 'activate', 'deactivate', 'disable', 'enable',
    'complete', 'ship', 'sign', 'transfer', 'move', 'issue', 'process', 'reopen', 'restore', 'operate', 'intervene',
  ],
  export: ['export', 'download'],
}

/**
 * 权限码 → **业务域**（对齐 `sys_menu` 顶层菜单，我方共 14 个业务域，与 ql361 的 13 个域同量级）
 *
 * ⚠️ 为什么不能用 `permission_code` 第一段直接当域（本轮踩过）：
 * 第一段是**开发者命名空间**、不是业务域，实测 25 个命名空间里大量是同一业务域被拆散：
 *   · `purchase` 与 `purchase_order` 是同一个采购域，`hr` 与 `hr-recruitment` 同属人力资源
 *     （后两者是历史命名空间，其权限码已于 2026-09-20 随重复实现一并清理）；
 *   · `system`/`user`/`role`/`permission`/`tenant`/`log`/`datasource`/`workflow`/`set`/`platform`/
 *     `permission-template`/`role-inheritance`/`data-permission` 这 **13 个命名空间其实都在「系统/设置」域**；
 *   · `erp` 更是**无业务含义的大包名** —— `erp:expense:*`(23 条) 属财务、`erp:product:*`(8 条) 属资料。
 * 直接把命名空间当域会让用户看到 25 个域（采购被拆成两个、系统被拆成十三个），与产品的一级导航对不上。
 *
 * 故这里显式声明「命名空间 → 业务域」的归属规则；**未命中的码落入「其他」**（宁可暴露未归类，
 * 也不要再造出一堆伪业务域）。域顺序 = `sys_menu` 顶层菜单顺序，便于与产品导航对照。
 */
const DOMAIN_RULES: Array<{ domain: string; test: (code: string) => boolean }> = [
  { domain: '销售', test: c => c.startsWith('sale:') },
  { domain: '采购', test: c => c.startsWith('purchase:') },
  { domain: '仓储', test: c => c.startsWith('stock:') },
  { domain: '配送', test: c => c.startsWith('dms:') },
  // erp: 是混合包名，按第二段再分：expense 归财务、product 归资料（资料=主数据）
  { domain: '财务', test: c => c.startsWith('finance:') || c.startsWith('payment:') || c.startsWith('receipt:') || c.startsWith('erp:expense:') },
  { domain: 'CRM', test: c => c.startsWith('crm:') },
  {
    domain: '资料',
    test: c => c.startsWith('department:') || c.startsWith('position:') || c.startsWith('erp:product:')
      || c.startsWith('product:') || c.startsWith('partner:'),
  },
  // 「特殊权限」：单据级动作（反审核/红冲/作废/改单据日期/看他人草稿/草稿打印）——
  // 对应 ql361 的「特殊权限」域，其列结构是**单列「允许」开关**（见 ACTION_COLUMNS 注释）
  { domain: '特殊权限', test: c => c.startsWith('doc:') || c.startsWith('print:') },
  { domain: '人力资源', test: c => c.startsWith('hr:') },
  {
    domain: '设置',
    test: c => /^(set|workflow|datasource|tenant|data-permission|permission-template|role-inheritance):/.test(c),
  },
  {
    domain: '系统',
    test: c => /^(system|user|role|permission|log|platform):/.test(c),
  },
]

/** 解析一个权限码所属的业务域（未命中的统一落「其他」） */
function resolveDomainLabel(code: string): string {
  for (const rule of DOMAIN_RULES) {
    if (rule.test(code)) return rule.domain
  }
  return '其他'
}

/** 取一组中文名的**最长公共前缀**作为功能点显示名（比动作词表剥离更稳） */
function commonPrefix(names: string[]): string {
  const valid = names.filter(n => !!n)
  if (!valid.length) return ''
  let prefix = valid[0]
  for (const n of valid.slice(1)) {
    let i = 0
    while (i < prefix.length && i < n.length && prefix[i] === n[i]) i++
    prefix = prefix.slice(0, i)
    if (!prefix) break
  }
  return prefix.trim()
}

/**
 * 权限池 → 业务域 / 模块（命名空间） / 功能点 / 动作 分组。
 *
 * 左树是**两级**（域 → 模块），与 ql361 的「资料 → 职员权限 → 右侧功能点矩阵」同构；
 * 右侧矩阵始终展示「当前模块」下的功能点 × 6 类操作。
 */
const permDomains = computed(() => {
  type PermEntry = { id: string; code: string; name: string; action: string }
  // 域 → 模块（命名空间）→ 功能点 → 权限
  const byDomain = new Map<string, Map<string, Map<string, PermEntry[]>>>()
  for (const p of permPool.value) {
    const domain = resolveDomainLabel(p.code)
    const segs = p.code.split(':')
    const moduleKey = segs[0] || '其他'
    const action = segs.length > 1 ? segs[segs.length - 1] : ''
    const feature = segs.length > 1 ? segs.slice(0, -1).join(':') : segs[0]
    if (!byDomain.has(domain)) byDomain.set(domain, new Map())
    const modules = byDomain.get(domain)!
    if (!modules.has(moduleKey)) modules.set(moduleKey, new Map())
    const features = modules.get(moduleKey)!
    if (!features.has(feature)) features.set(feature, [])
    features.get(feature)!.push({ id: p.id, code: p.code, name: p.name, action })
  }
  // 域顺序对齐顶层菜单（DOMAIN_RULES 声明顺序），未归类的「其他」恒排最后
  const order = DOMAIN_RULES.map(r => r.domain)
  const sorted = Array.from(byDomain.entries()).sort((a, b) => {
    const ia = order.indexOf(a[0])
    const ib = order.indexOf(b[0])
    return (ia < 0 ? Number.MAX_SAFE_INTEGER : ia) - (ib < 0 ? Number.MAX_SAFE_INTEGER : ib)
  })
  return sorted.map(([domain, modules]) => ({
    key: domain,
    label: domain,
    permCount: Array.from(modules.values()).reduce(
      (sum, features) => sum + Array.from(features.values()).reduce((s, arr) => s + arr.length, 0), 0),
    modules: Array.from(modules.entries()).map(([moduleKey, features]) => ({
      key: moduleKey,
      label: MODULE_LABELS[moduleKey] || moduleKey,
      permCount: Array.from(features.values()).reduce((s, arr) => s + arr.length, 0),
      featureCount: features.size,
      features: Array.from(features.entries()).map(([featureKey, perms]) => {
        const cells: Record<ActionKey, { ids: string[]; labels: string[]; codes: string[]; stale: boolean }> = {
          view: { ids: [], labels: [], codes: [], stale: false },
          print: { ids: [], labels: [], codes: [], stale: false },
          create: { ids: [], labels: [], codes: [], stale: false },
          delete: { ids: [], labels: [], codes: [], stale: false },
          update: { ids: [], labels: [], codes: [], stale: false },
          export: { ids: [], labels: [], codes: [], stale: false },
          allow: { ids: [], labels: [], codes: [], stale: false },
        }
        for (const p of perms) {
          // 命不中 6 类标准动作的（转正离职/审批/反审核/规则配置…）落「允许」列 —— 与 ql361 的
          // 「特殊权限 / 价格权限」同构；单元格 tooltip 仍会列出原始权限名，不丢粒度
          const bucket = (Object.keys(ACTION_BUCKETS) as Array<Exclude<ActionKey, 'allow'>>)
            .find(k => ACTION_BUCKETS[k].includes(p.action)) || 'allow'
          cells[bucket].ids.push(p.id)
          cells[bucket].labels.push(p.name || p.code)
          cells[bucket].codes.push(p.code)
        }
        // 该格内的权限码是否**全部**没有消费方（勾了也不生效）—— 见 isIneffective 注释
        for (const k of Object.keys(cells) as ActionKey[]) {
          cells[k].stale = cells[k].codes.length > 0 && cells[k].codes.every(c => isIneffective(c))
        }
        return {
          featureKey,
          featureName: commonPrefix(perms.map(p => p.name)) || featureKey,
          cells,
          allIds: perms.map(p => p.id),
          // 整个功能点都无消费方：该功能点下没有任何一条权限码会被后端注解或前端指令检查
          allStale: perms.length > 0 && perms.every(p => isIneffective(p.code)),
        }
      }),
    })),
  }))
})

/** 左树筛选：域或模块名命中即保留 */
const filteredDomains = computed(() => {
  const kw = permMatrixDomainKeyword.value.trim().toLowerCase()
  if (!kw) return permDomains.value
  return permDomains.value
    .map(d => ({
      ...d,
      modules: d.modules.filter(m => m.label.toLowerCase().includes(kw) || m.key.toLowerCase().includes(kw)),
    }))
    .filter(d => d.modules.length > 0 || d.label.toLowerCase().includes(kw) || d.key.toLowerCase().includes(kw))
})

/** 当前选中的模块（右侧矩阵的数据源） */
const currentModule = computed(() => {
  const domain = permDomains.value.find(d => d.key === permMatrixDomainKey.value)
  if (!domain) return null
  return domain.modules.find(m => m.key === permMatrixModuleKey.value) || null
})

/** 当前模块下的矩阵行（含选中态） */
const permMatrixRows = computed(() => {
  const domain = currentModule.value
  if (!domain) return []
  return domain.features.map(f => {
    const cells: Record<ActionKey, {
      ids: string[]; labels: string[]; codes: string[]
      checked: boolean; indeterminate: boolean; stale: boolean
    }> = {} as any
    for (const col of ACTION_COLUMNS) {
      const ids = f.cells[col.key]?.ids || []
      const labels = f.cells[col.key]?.labels || []
      const hit = ids.filter(id => permCheckedIds.value.has(id)).length
      cells[col.key] = {
        ids,
        labels,
        codes: f.cells[col.key]?.codes || [],
        stale: !!f.cells[col.key]?.stale,
        checked: ids.length > 0 && hit === ids.length,
        indeterminate: hit > 0 && hit < ids.length,
      }
    }
    const hitAll = f.allIds.filter(id => permCheckedIds.value.has(id)).length
    return {
      ...f,
      cells,
      allChecked: f.allIds.length > 0 && hitAll === f.allIds.length,
      allIndeterminate: hitAll > 0 && hitAll < f.allIds.length,
    }
  })
})

const permMatrixCheckedCount = computed(() => permCheckedIds.value.size)

/** 当前模块下「未生效」的权限码条数（工具栏提示用，不给数字就没法判断这页能不能信） */
const currentModuleStaleCount = computed(() => {
  const mod = currentModule.value
  if (!mod) return 0
  let n = 0
  for (const f of mod.features) {
    for (const k of Object.keys(f.cells) as ActionKey[]) {
      n += (f.cells[k]?.codes || []).filter(c => isIneffective(c)).length
    }
  }
  return n
})

/**
 * 加载 Tab1「功能权限」数据：全量权限池（仅正常态）+ 该角色已授权权限 id。
 * 失败只提示、不抛出 —— 由 handlePermission 汇总后不影响其它 Tab 展示。
 */
async function loadPermissionMatrixData(record: RoleInfo) {
  try {
    const tenantIds: number[] = [0]
    const roleTenantId = (record as any).tenantId
    if (roleTenantId !== null && roleTenantId !== undefined && String(roleTenantId) !== '0') tenantIds.push(Number(roleTenantId))
    const pool: Array<{ id: string; code: string; name: string }> = []
    const seen = new Set<string>()
    const results = await Promise.allSettled(
      tenantIds.map(tenantId => permissionApi.getPage({ tenantId, current: 1, size: 1000 }))
    )
    for (const r of results) {
      if (r.status !== 'fulfilled') continue
      const page = r.value as unknown as { records?: any[] }
      for (const p of (page?.records || [])) {
        if (p?.id === undefined || p?.id === null) continue
        const id = String(p.id)
        if (seen.has(id)) continue
        if (p.status !== undefined && Number(p.status) !== 0) continue
        seen.add(id)
        pool.push({ id, code: p.permissionCode || '', name: p.permissionName || '' })
      }
    }
    permPool.value = pool.filter(p => !!p.code)
    if (!permPool.value.length) message.warning('未取到任何权限码，请检查权限表数据')
    const res = await roleApi.getPermissions(record.id)
    // ⚠️ 响应拦截器已把 Result 拆成 data 本体（utils/request.ts），旧写法读 `res.data` 恒 undefined
    //    → 勾选集合恒为空 → 保存时向「全量覆盖」接口提交空数组 → 静默清空该角色全部权限。
    const authorized = normalizeIdList(unwrap<unknown[]>(res))
    permCheckedIds.value = new Set(authorized)
    permOriginalIds.value = new Set(permCheckedIds.value)
    permTabLoaded.value = true
    // 默认展开第一个域并选中它的第一个模块（ql361 是左树展开状态）
    const first = filteredDomains.value[0]
    const firstModule = first?.modules?.[0]
    permMatrixDomainKey.value = first ? first.key : ''
    permMatrixModuleKey.value = firstModule ? firstModule.key : ''
    permMatrixExpandedDomains.value = first ? [first.key] : []
  } catch (err) {
    console.warn('[角色管理] 加载权限矩阵失败', err)
    message.error('功能权限数据加载失败')
    // 回显失败必须清空并标记：否则会残留「上一个角色」的勾选，或把空集合误当成用户的取消勾选提交
    permCheckedIds.value = new Set()
    permOriginalIds.value = new Set()
    permTabLoaded.value = false
  }
}

/** 加载 Tab2「菜单权限」数据：菜单树 + 该角色已授权菜单 id */
async function loadMenuAssignData(record: RoleInfo) {
  try {
    const menuRes = await menuApi.getTree({})
    menuTree.value = menuRes ? buildPermissionTree(menuRes) : []
  } catch (err) {
    menuTree.value = []
    console.warn('[系统管理] 加载菜单树失败', err)
    message.error('菜单权限数据加载失败')
  }
  try {
    const res = await roleApi.getMenus(record.id)
    // 同上：拦截器已拆包，`res.data` 恒 undefined ⇒ 菜单勾选恒空 ⇒ 保存时全量覆盖清空
    checkedMenuKeys.value = normalizeIdList(unwrap<unknown[]>(res)).map(Number)
    menuTabLoaded.value = true
  } catch (err) {
    console.warn('[系统管理] 获取菜单列表失败', err)
    message.error('菜单权限数据加载失败')
    checkedMenuKeys.value = []
    menuTabLoaded.value = false
  }
}

/** 加载 Tab3「单据类型权限」数据 */
async function loadBillTypeAssignData(record: RoleInfo) {
  try {
    const res = await roleBillTypeApi.getRoleBillTypes(record.id)
    billTypeData.value = unwrap<BillTypeDetail[]>(res) || []
    billTypeTabLoaded.value = true
  } catch (err) {
    console.warn('[系统管理] 加载单据类型权限失败', err)
    message.error('单据类型权限数据加载失败')
    billTypeData.value = []
    billTypeTabLoaded.value = false
  }
}

/**
 * 打开「角色权限设置」弹窗（唯一授权入口）：
 * 一次性并发加载三个维度（功能权限 / 菜单权限 / 单据类型权限）。
 * 用 allSettled + 各自 try/catch：任一维度失败只提示，弹窗照常打开、其它 Tab 照常可用。
 *
 * ⚠️ 加载失败的维度会被标成「未加载」并**禁止保存**（见 savePermissionMatrix 的提交前守卫）：
 * 三个 assign* 接口都是全量覆盖语义，未加载成功 = 空集合，提交上去等于清空该维度已有授权。
 */
const handlePermission = async (record: RoleInfo) => {
  permMatrixRoleId.value = String(record.id)
  permMatrixRoleName.value = record.roleName || ''
  permMatrixDomainKeyword.value = ''
  permTabKey.value = 'feature'
  permMatrixVisible.value = true
  permMatrixLoading.value = true
  // 清空三个维度的旧数据，避免上一个角色的勾选残留
  permPool.value = []
  permCheckedIds.value = new Set()
  menuTree.value = []
  checkedMenuKeys.value = []
  billTypeData.value = []
  // 重置「已加载」标记：上一次成功不代表这一次也成功
  permTabLoaded.value = false
  menuTabLoaded.value = false
  billTypeTabLoaded.value = false
  try {
    await Promise.allSettled([
      loadPermissionMatrixData(record),
      loadMenuAssignData(record),
      loadBillTypeAssignData(record),
      // 生效性清单与权限矩阵并行：它只影响标注，加载失败也不影响矩阵本身
      loadPermissionEffectivity(),
    ])
  } finally {
    permMatrixLoading.value = false
  }
}

/** 勾选/取消一个功能点在某一列下的全部权限码 */
function toggleCell(row: { cells: Record<ActionKey, { ids: string[]; checked: boolean }> }, colKey: ActionKey, checked: boolean) {
  const next = new Set(permCheckedIds.value)

  // 🔴「查看」必备规则（对标 ql361 实测行为，2026-09-20）：
  //   · 勾选任一非查看列 → **自动补勾「查看」**（不能操作一个自己看不到的功能）
  //   · 反向：该行还有其它列勾着时，点「查看」想取消会被**强制回勾**（表现为"点不动"），
  //     只有该行只剩「查看」一项时才允许取消
  //   ql361 源码里这条被租户开关 BillPowerSeparate 包裹，对标租户实测生效，故我方默认启用。
  if (colKey === 'view' && !checked) {
    const hasOther = ACTION_COLUMNS
      .filter(c => c.key !== 'view')
      .some(c => (row.cells[c.key]?.ids || []).some(id => permCheckedIds.value.has(id)))
    if (hasOther) {
      // 强制回勾：不改动任何 id，直接返回（与 ql361「点不动」的表现一致）
      return
    }
  }

  for (const id of row.cells[colKey]?.ids || []) {
    if (checked) next.add(id)
    else next.delete(id)
  }
  if (checked && colKey !== 'view') {
    for (const id of row.cells.view?.ids || []) next.add(id)
  }
  permCheckedIds.value = next
}

/** 勾选/取消一整行的全部权限码 */
function toggleRow(row: { allIds: string[]; allChecked: boolean }, checked: boolean) {
  const next = new Set(permCheckedIds.value)
  for (const id of row.allIds) {
    if (checked) next.add(id)
    else next.delete(id)
  }
  permCheckedIds.value = next
}

/** 勾选/取消当前域的全部权限码 */
function toggleDomain(checked: boolean) {
  const mod = currentModule.value
  if (!mod) return
  const next = new Set(permCheckedIds.value)
  for (const f of mod.features) {
    for (const id of f.allIds) {
      if (checked) next.add(id)
      else next.delete(id)
    }
  }
  permCheckedIds.value = next
}

/** 展开/收起左树的某个业务域 */
function toggleDomainExpand(domainKey: string) {
  const list = permMatrixExpandedDomains.value
  permMatrixExpandedDomains.value = list.includes(domainKey)
    ? list.filter(k => k !== domainKey)
    : [...list, domainKey]
}

/** 选中某个模块（右侧矩阵随之切换） */
function selectModule(domainKey: string, moduleKey: string) {
  permMatrixDomainKey.value = domainKey
  permMatrixModuleKey.value = moduleKey
}

/** 当前模块是否全部选中（用于「当前模块全选」勾选框的三态） */
const currentModuleAllChecked = computed(() => {
  const mod = currentModule.value
  if (!mod || !mod.features.length) return false
  return mod.features.every(f => f.allIds.every(id => permCheckedIds.value.has(id)))
})
const currentModuleIndeterminate = computed(() => {
  const mod = currentModule.value
  if (!mod) return false
  const anyHit = mod.features.some(f => f.allIds.some(id => permCheckedIds.value.has(id)))
  return anyHit && !currentModuleAllChecked.value
})

/**
 * 统一保存：依次提交 功能权限 → 菜单权限 → 单据类型权限 三个维度。
 * 某一维度失败**不中断**后续维度的提交，最后按维度名汇总提示（不笼统报「保存失败」）；
 * 只要有一处失败就保持弹窗打开，便于就地重试。
 */
/**
 * 本次功能权限的变更明细（新增 / 移除）。
 *
 * 为什么要有：授权是「一次点一片」的操作，保存后很难回想刚才到底改了哪几项；
 * 对齐业界的做法（SAP 授权配置文件生成前后对比、用友权限变更结构化日志），
 * 保存前把改动摊开给管理员看一眼。
 */
const permDiff = computed(() => {
  const nameOf = (id: string) => permPool.value.find(p => p.id === id)?.name || `#${id}`
  const added: string[] = []
  const removed: string[] = []
  for (const id of permCheckedIds.value) {
    if (!permOriginalIds.value.has(id)) added.push(nameOf(id))
  }
  for (const id of permOriginalIds.value) {
    if (!permCheckedIds.value.has(id)) removed.push(nameOf(id))
  }
  return { added, removed }
})

/** 变更确认弹窗：列出新增/移除明细，超过 12 项折叠 */
function confirmPermDiff(added: string[], removed: string[]): Promise<boolean> {
  const MAX = 12
  const renderBlock = (title: string, items: string[], color: string) =>
    h('div', { style: { marginBottom: '10px' } }, [
      h('div', { style: { fontWeight: 600, color, marginBottom: '2px' } }, `${title} ${items.length} 项`),
      h('div', { style: { fontSize: '12px', color: '#595959', maxHeight: '120px', overflowY: 'auto' } },
        items.slice(0, MAX).join('、') + (items.length > MAX ? ` … 等共 ${items.length} 项` : '')),
    ])
  return new Promise(resolve => {
    Modal.confirm({
      title: '确认权限变更',
      width: 520,
      content: h('div', [
        added.length ? renderBlock('新增', added, '#389e0d') : null,
        removed.length ? renderBlock('移除', removed, '#cf1322') : null,
      ]),
      okText: '确认保存',
      cancelText: '再检查一下',
      onOk: () => resolve(true),
      onCancel: () => resolve(false),
    })
  })
}

const handlePermSave = async () => {
  if (!permMatrixRoleId.value) return
  const roleId = Number(permMatrixRoleId.value)
  // 保存前把本次改动摊开确认（无变更则不打扰）
  const { added, removed } = permDiff.value
  if (added.length || removed.length) {
    const confirmed = await confirmPermDiff(added, removed)
    if (!confirmed) return
  }
  // ── 提交前守卫：回显失败的维度一律不提交 ────────────────────────────
  // assign* 三个接口都是「全量覆盖」语义，拿一份没加载成功（=空）的集合去提交，
  // 等于把该维度已有授权全部删掉。宁可拒绝保存，也不能静默清空。
  const notLoaded = [
    permTabLoaded.value ? '' : '功能权限',
    menuTabLoaded.value ? '' : '菜单权限',
    billTypeTabLoaded.value ? '' : '单据类型权限',
  ].filter(Boolean)
  if (notLoaded.length) {
    message.error(`${notLoaded.join('、')}未能成功加载，已禁止保存（避免把该角色已有授权清空）。请关闭弹窗后重试。`)
    return
  }

  permMatrixSaving.value = true
  const failedDimensions: string[] = []
  // 单据类型权限：0-无权限的不提交（与后端约定一致）
  const billTypeAssignments = billTypeData.value
    .filter(d => d.permissionLevel > 0)
    .map(d => ({ billType: d.billType, permissionLevel: d.permissionLevel }))
  try {
    // ① 功能权限
    try {
      await roleApi.assignPermissions(roleId, Array.from(permCheckedIds.value) as unknown as number[])
    } catch (err) {
      failedDimensions.push('功能权限')
      console.warn('[角色管理] 分配权限失败', err)
    }
    // ② 菜单权限
    try {
      await roleApi.assignMenus(roleId, checkedMenuKeys.value as number[])
    } catch (err) {
      failedDimensions.push('菜单权限')
      console.warn('[系统管理] 分配菜单失败', err)
    }
    // ③ 单据类型权限
    try {
      await roleBillTypeApi.assignBillTypes(roleId, billTypeAssignments)
    } catch (err) {
      failedDimensions.push('单据类型权限')
      console.warn('[系统管理] 分配单据类型权限失败', err)
    }
    // ④ 敏感信息保护 / ⑤ 数据可见范围：全量覆盖型，由子组件自己校验后回报成败。
    //    这两个 Tab 以前只能靠各自 Tab 内的保存按钮，点弹窗底部的「保存」不会提交它们，
    //    弹窗一关组件就卸载，用户以为存了其实丢了 —— 这里统一纳入。
    for (const [name, tabRef] of [
      ['敏感信息保护', fieldPermTabRef],
      ['数据可见范围', dataScopeTabRef],
    ] as const) {
      try {
        const ok = await tabRef.value?.save()
        if (ok === false) failedDimensions.push(name)
      } catch (err) {
        failedDimensions.push(name)
        console.warn(`[岗位权限] 保存${name}失败`, err)
      }
    }
    if (failedDimensions.length) {
      message.error(`${failedDimensions.join('、')}保存失败，请重试`)
      return
    }
    message.success(`权限配置成功：功能权限 ${permCheckedIds.value.size} 项、菜单 ${checkedMenuKeys.value.length} 项、单据类型 ${billTypeAssignments.length} 项`)
    permMatrixVisible.value = false
    fetchData()
  } finally {
    permMatrixSaving.value = false
  }
}

/**
 * 关闭弹窗。
 *
 * 弹窗已改为受控（:open + @cancel），所以这里不主动置 false 时弹窗保持打开 ——
 * 借此拦住「改完细粒度 Tab 直接关掉」导致的静默丢失。
 */
function handlePermCancel() {
  if (!hasUnsavedFineGrained.value) {
    permMatrixVisible.value = false
    return
  }
  Modal.confirm({
    title: '有未保存的修改',
    content: '「敏感信息保护」或「数据可见范围」里有尚未保存的修改，直接关闭会丢失。',
    okText: '放弃修改并关闭',
    okType: 'danger',
    cancelText: '返回保存',
    onOk: () => { permMatrixVisible.value = false },
  })
}

/** 菜单树 → a-tree 结构（合并弹窗 Tab2 用；其 key 是 menu id，与角色-菜单关联表口径一致） */
const buildPermissionTree = (menus: any[]): any[] => {
  return menus.map((m: any) => ({
    title: m.menuName,
    key: m.id,
    children: m.children?.length ? buildPermissionTree(m.children) : undefined
  }))
}

const roleTypeColorMap: Record<number, string> = { 0: 'blue', 1: 'green' }
const getRoleTypeColor = (type: number) => roleTypeColorMap[type] || 'default'
const getRoleTypeName = (type: number) => {
  const found = roleTypeOptions.value.find(o => o.value === type)
  return found ? found.label : '未知'
}

onMounted(() => {
  fetchData()
  loadRoleTypeOptions()
  autoRefreshCountdown.value = 30
  refreshTimer = setInterval(() => {
    fetchData()
    autoRefreshCountdown.value = 30
  }, 30000)
  countdownTimer = setInterval(() => {
    if (autoRefreshCountdown.value > 0) autoRefreshCountdown.value--
  }, 1000)
  document.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  if (refreshTimer) clearInterval(refreshTimer)
  if (countdownTimer) clearInterval(countdownTimer)
  document.removeEventListener('keydown', handleKeydown)
})

defineExpose({ handleQuery: fetchData })

function handleError(err: any) { console.warn('[ErrorBoundary]', err) }

// ==================== 角色详情（只读） ====================
// 容器复用本页既有的 FullScreenDetail（:show-footer="false" → 无保存按钮，纯只读，不提交/不删除）
// 三块数据全部取自真实接口，且不复用列表行快照：
//   1. 基本信息   GET /api/role/{id}               （后端直接返回 sys_role 行）
//   2. 已分配权限 GET /api/role/{id}/permissions    （返回 sys_permission.id 清单）
//   3. 已分配菜单 GET /api/role/{id}/menus          （返回 sys_menu.id 清单）
// 名称还原：权限码来自 GET /api/permission/page（sys_permission 属系统级共享表，接口按 tenantId 显式过滤，
// 角色可能同时挂着 tenant_id=0 的全局权限与本租户权限，故两段都要取）；菜单名来自 GET /api/menu/tree。
type RoleDetail = RoleInfo & { updateTime?: string }

const viewVisible = ref(false)
const viewLoading = ref(false)
const viewError = ref(false)
const viewErrorMessage = ref('')
const viewDetail = ref<RoleDetail | null>(null)
/** 当前查看的角色 id：雪花 ID 按原值（字符串）透传，不做 Number() 转换 */
const viewRoleId = ref<string | number | null>(null)

const viewTitle = computed(() => viewDetail.value?.roleName
  ? `角色详情 - ${viewDetail.value.roleName}`
  : '角色详情')

// 数据范围口径对齐后端 SysRole.dataScope 注释（0-全部 1-本部门 2-本部门及以下 3-仅本人 4-自定义）
// 4「自定义」的名字带上指引，否则管理员选完不知道去哪里配具体范围
const DATA_SCOPE_LABELS: Record<number, string> = { 0: '全部数据', 1: '本部门', 2: '本部门及以下', 3: '仅本人', 4: '自定义（需另配明细）' }
/** 数据范围下拉选项：与只读详情共用 DATA_SCOPE_LABELS，避免两处口径漂移 */
const dataScopeOptions = Object.entries(DATA_SCOPE_LABELS).map(([value, label]) => ({
  value: Number(value),
  label,
}))
// 角色类型兜底口径对齐后端 SysRole.roleType 注释（0-系统角色 1-自定义角色）
// 说明：当前库里没有 ROLE_TYPE 字典（loadRoleTypeOptions 取不到值），详情里不再回落成「未知」而误报
const ROLE_TYPE_LABELS: Record<number, string> = { 0: '系统角色', 1: '自定义角色' }

const viewDataScopeLabel = computed(() => {
  const v = viewDetail.value?.dataScope
  if (v === null || v === undefined) return '-'
  return DATA_SCOPE_LABELS[v] || String(v)
})

const viewStatusText = computed(() => {
  const v = viewDetail.value?.status
  return v === 0 ? '正常' : (v === 1 ? '停用' : '-')
})

const viewRoleType = computed(() => {
  const v = viewDetail.value?.roleType
  if (v === null || v === undefined || v === '') return null
  const num = Number(v)
  const dictLabel = roleTypeOptions.value.find(o => o.value === num)?.label
  return { label: dictLabel || ROLE_TYPE_LABELS[num] || '未知', color: getRoleTypeColor(num) }
})

/**
 * 拆包：响应拦截器在成功时已把 Result 拆成 data 本体（见 utils/request.ts），
 * 因此运行时拿到的通常就是数据本身；这里同时兼容尚未拆包的 { data: ... } 形态。
 */
function unwrap<T>(res: unknown): T | null {
  if (res === null || res === undefined) return null
  const wrapped = res as { data?: T }
  return wrapped.data !== undefined ? wrapped.data : (res as T)
}

/** 归一化为字符串数组（雪花 ID 一律按字符串处理，禁止 Number()） */
function normalizeIdList(raw: unknown): string[] {
  const list = Array.isArray(raw) ? raw : []
  return list.map((v: unknown) => String(v))
}

/** 传给 roleApi 的 id：始终原样透传（雪花 ID 不做 Number()），仅补一处类型断言 */
function asApiId(id: string | number): number {
  return id as unknown as number
}

// ── 已分配权限 ──────────────────────────────────────────
const viewPermissionIds = ref<string[]>([])
const viewPermOptionsLoading = ref(false)
const viewPermOptionsError = ref(false)
/** sys_permission.id → { name, code }，用于把分配到角色的权限 ID 还原成权限码 */
const viewPermOptionMap = ref<Map<string, { name: string; code: string }>>(new Map())

/** 权限码形如 tenant-admin:role:list，第一段即模块标识，据此分组（sys_permission 无可用父子层级） */
const viewPermissionGroups = computed(() => {
  const groups = new Map<string, { id: string; name: string; code: string }[]>()
  for (const id of viewPermissionIds.value) {
    const hit = viewPermOptionMap.value.get(id)
    const code = hit?.code || ''
    const module = code ? code.split(':')[0] : '未解析权限'
    const bucket = groups.get(module)
    if (bucket) bucket.push({ id, name: hit?.name || '', code })
    else groups.set(module, [{ id, name: hit?.name || '', code }])
  }
  return Array.from(groups.entries())
    .map(([module, items]) => ({ module, items }))
    .sort((a, b) => a.module.localeCompare(b.module))
})

/** 权限名称解析：GET /api/permission/page（分别取全局 tenant_id=0 与角色所属租户） */
async function loadPermissionOptions() {
  viewPermOptionsLoading.value = true
  viewPermOptionsError.value = false
  try {
    const tenantIds: number[] = [0]
    const roleTenantId = viewDetail.value?.tenantId
    if (roleTenantId !== null && roleTenantId !== undefined && String(roleTenantId) !== '0') {
      tenantIds.push(roleTenantId)
    }
    const results = await Promise.allSettled(tenantIds.map(tenantId =>
      permissionApi.getPage({ tenantId, current: 1, size: 1000 })
    ))
    const map = new Map<string, { name: string; code: string }>()
    let failed = false
    for (const result of results) {
      if (result.status === 'rejected') {
        // 单段失败不影响另一段已解析出的名称，但要显式进入错误态给重试，不能静默当成"该角色没有权限"
        failed = true
        console.warn('[角色管理] 解析权限名称失败', result.reason)
        continue
      }
      const page = result.value as unknown as {
        records?: Array<{ id?: string | number; permissionName?: string; permissionCode?: string }>
      }
      const records = Array.isArray(page?.records) ? page.records : []
      for (const p of records) {
        if (p?.id === undefined || p?.id === null) continue
        map.set(String(p.id), { name: p.permissionName || '', code: p.permissionCode || '' })
      }
    }
    viewPermOptionMap.value = map
    viewPermOptionsError.value = failed
  } finally {
    viewPermOptionsLoading.value = false
  }
}

// ── 已分配菜单 ──────────────────────────────────────────
const viewMenuIds = ref<string[]>([])
const viewMenuTreeLoading = ref(false)
const viewMenuTreeError = ref(false)
const viewMenuTree = ref<ReadonlyTreeNode[]>([])

/** 菜单树节点（GET /api/menu/tree 返回体；雪花 ID 运行时可能是字符串） */
interface MenuTreeNode {
  id?: string | number
  menuName?: string
  menuType?: number
  children?: MenuTreeNode[]
}

/** 只读菜单树节点（a-tree 的 tree-data） */
interface ReadonlyTreeNode {
  key: string
  title: string
  children?: ReadonlyTreeNode[]
}

/** 菜单类型口径对齐前端 MenuInfo.menuType 注释（0-目录 1-菜单 2-按钮） */
const MENU_TYPE_LABELS: Record<number, string> = { 0: '目录', 1: '菜单', 2: '按钮' }

/**
 * 用「已分配菜单 ID」重建菜单树：只保留被分配的节点及其祖先目录；
 * 祖先仅用于体现层级，标题里显式标注「上级目录」以免被误认为已分配。
 */
function buildAssignedMenuTree(nodes: MenuTreeNode[], assigned: Set<string>): ReadonlyTreeNode[] {
  const result: ReadonlyTreeNode[] = []
  for (const node of nodes || []) {
    if (!node || node.id === undefined || node.id === null) continue
    const children = buildAssignedMenuTree(node.children || [], assigned)
    const selfAssigned = assigned.has(String(node.id))
    if (!selfAssigned && children.length === 0) continue
    const typeLabel = node.menuType !== undefined ? (MENU_TYPE_LABELS[node.menuType] || '') : ''
    result.push({
      key: String(node.id),
      title: `${node.menuName || String(node.id)}${typeLabel ? `（${typeLabel}）` : ''}${selfAssigned ? '' : '（上级目录，未分配）'}`,
      children: children.length > 0 ? children : undefined,
    })
  }
  return result
}

/** 菜单名称解析：复用本页「菜单配置」弹窗同款接口 GET /api/menu/tree */
async function loadMenuTreeForView() {
  viewMenuTreeLoading.value = true
  viewMenuTreeError.value = false
  try {
    const menuRes = await menuApi.getTree({})
    const nodes = unwrap<MenuTreeNode[]>(menuRes) || []
    viewMenuTree.value = buildAssignedMenuTree(nodes, new Set(viewMenuIds.value))
  } catch (err) {
    viewMenuTree.value = []
    viewMenuTreeError.value = true
    console.warn('[角色管理] 解析菜单名称失败', err)
  } finally {
    viewMenuTreeLoading.value = false
  }
}

/** 基本信息 + 已分配权限 ID + 已分配菜单 ID（任一失败整体进入错误态，并提供重试） */
const fetchViewDetail = async (roleId: string | number) => {
  viewLoading.value = true
  viewError.value = false
  viewErrorMessage.value = ''
  viewDetail.value = null
  viewPermissionIds.value = []
  viewMenuIds.value = []
  viewMenuTree.value = []
  viewPermOptionMap.value = new Map()
  viewPermOptionsError.value = false
  viewMenuTreeError.value = false
  try {
    const [roleRes, permRes, menuRes] = await Promise.all([
      roleApi.getById(asApiId(roleId)),
      roleApi.getPermissions(asApiId(roleId)),
      roleApi.getMenus(asApiId(roleId)),
    ])
    const role = unwrap<RoleDetail>(roleRes)
    if (!role || role.id === undefined || role.id === null) {
      viewError.value = true
      viewErrorMessage.value = '未找到该角色，可能已被删除'
      return
    }
    viewDetail.value = role
    viewPermissionIds.value = normalizeIdList(unwrap<string[] | number[]>(permRes))
    viewMenuIds.value = normalizeIdList(unwrap<string[] | number[]>(menuRes))
  } catch (err) {
    viewError.value = true
    viewErrorMessage.value = '角色详情加载失败'
    console.warn('[角色管理] 加载角色详情失败', err)
    return
  } finally {
    viewLoading.value = false
  }
  // 附属数据（名称解析）失败不阻塞主体，各自在对应分区给错误态与重试
  loadPermissionOptions()
  loadMenuTreeForView()
}

// 重试：基本信息 + 已分配权限/菜单一起重载
const reloadView = () => {
  if (viewRoleId.value === null) return
  fetchViewDetail(viewRoleId.value)
}

const handleViewClose = () => {
  viewVisible.value = false
}

// 查看详情（只读，双击行触发）
const handleView = (record: RoleInfo) => {
  const roleId = record?.id
  if (roleId === undefined || roleId === null) return
  viewRoleId.value = roleId
  viewVisible.value = true
  fetchViewDetail(roleId)
}

// 双击行查看角色详情 —— 页面侧自行实现（不依赖共享表格组件派发事件）
// 行标识由表格行上的 data-row-key（= row-key 指定的 id）反查得到；占位空行不带该属性
const tableWrap = ref<HTMLElement | null>(null)
useRowDblclick(tableWrap, () => tableDataSource.value, handleView, 'id')
</script>

<style scoped>
/* ═══ 角色权限设置矩阵（对标 ql361「角色权限设置」面板） ═══ */
.perm-matrix {
  display: flex;
  gap: 12px;
  min-height: 420px;
}
.perm-matrix__side {
  width: 190px;
  flex-shrink: 0;
  border: 1px solid #e8e8e8;
  border-radius: 4px;
  padding: 8px;
  display: flex;
  flex-direction: column;
  max-height: 460px;
}
.perm-matrix__side-title {
  font-size: 13px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
}
.perm-matrix__domains {
  list-style: none;
  margin: 0;
  padding: 0;
  overflow-y: auto;
  flex: 1;
}
/* 左树两级：业务域（可展开）→ 模块（可选中） */
.perm-matrix__domain {
  display: block;
  cursor: default;
}
.perm-matrix__domain-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 8px;
  font-size: 13px;
  font-weight: 600;
  color: #303133;
  border-radius: 4px;
  cursor: pointer;
}
.perm-matrix__domain-head:hover {
  background: #f5f7fa;
}
.perm-matrix__modules {
  list-style: none;
  margin: 0;
  padding: 0 0 0 10px;
}
.perm-matrix__modules li {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 5px 8px;
  font-size: 13px;
  color: #303133;
  border-radius: 4px;
  cursor: pointer;
}
.perm-matrix__modules li:hover {
  background: #f5f7fa;
}
.perm-matrix__modules li.active {
  background: #e6f4ff;
  color: #1677ff;
  font-weight: 600;
}
.perm-matrix__modules .cnt {
  font-size: 12px;
  color: #909399;
}
.perm-matrix__domain-count {
  font-size: 12px;
  color: #909399;
}
.perm-matrix__domain-empty {
  justify-content: center;
  color: #909399;
  cursor: default;
}
.perm-matrix__main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}
.perm-matrix__toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}
.perm-matrix__summary {
  font-size: 13px;
  color: #606266;
}
.perm-matrix__summary b {
  color: #1677ff;
  padding: 0 2px;
}
.perm-matrix__table-wrap {
  flex: 1;
  overflow: auto;
  border: 1px solid #e8e8e8;
  border-radius: 4px;
  max-height: 420px;
}
.perm-matrix__table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}
.perm-matrix__table th,
.perm-matrix__table td {
  border-bottom: 1px solid #f0f0f0;
  border-right: 1px solid #f0f0f0;
  padding: 6px 8px;
  text-align: center;
  color: #303133;
}
.perm-matrix__table thead th {
  position: sticky;
  top: 0;
  background: #fafafa;
  font-weight: 600;
  z-index: 1;
}
.perm-matrix__table .col-no {
  width: 52px;
}
.perm-matrix__table .col-module {
  width: 110px;
}
.perm-matrix__table .col-rowcheck {
  width: 44px;
}
.perm-matrix__table .col-feature {
  text-align: left;
  min-width: 130px;
}
.perm-matrix__table .col-action {
  width: 62px;
}
.perm-matrix__none {
  color: #d9d9d9;
}
.perm-matrix__cell {
  display: inline-flex;
  align-items: center;
  gap: 2px;
}
.perm-matrix__cnt {
  font-size: 11px;
  color: #909399;
  line-height: 1;
}
/* ── 未生效权限码标注 ───────────────────────────────────────────
   只做视觉降级、不禁用勾选：这些权限码可能是尚未接线的新功能，
   禁用会让管理员无法预配置（判定口径见 isIneffective 注释）。 */
.perm-matrix__cell--stale {
  opacity: 0.5;
}
.perm-matrix__stale-tag {
  margin-left: 6px;
  padding: 0 4px;
  font-size: 11px;
  line-height: 18px;
  color: #8c8c8c;
  background: #fafafa;
  border-color: #d9d9d9;
  cursor: help;
}
.perm-matrix__table .col-feature--stale {
  color: #8c8c8c;
}
.perm-matrix__stale-hint {
  margin-left: 12px;
  font-size: 12px;
  color: #d48806;
  cursor: help;
}
/* 表单项下方的说明文字（如「数据范围」的生效前提） */
.form-item-hint {
  margin-top: 4px;
  font-size: 12px;
  line-height: 1.5;
  color: #909399;
}
.perm-matrix__empty {
  color: #909399;
  padding: 32px 0;
}
/* 弹窗顶部的白话引导：先讲清六个 Tab 分别管什么，再让管理员去点 */
.perm-intro {
  margin-bottom: 12px;
}
.perm-intro__title {
  font-weight: 600;
}
.perm-intro__body {
  margin-top: 2px;
  font-size: 12px;
  line-height: 1.7;
}
/* ═══ 合并弹窗的 Tab 容器 ═══
   三个 Tab 内容高度不一（矩阵约 460、菜单树/单据表随数据增长），
   这里给内容区一个上限并允许纵向滚动，既保证矩阵左右两栏的固定高度（420/460）不塌陷，
   也避免单据类型行数多时把弹窗撑出屏幕。 */
.perm-tabs :deep(.ant-tabs-content-holder) {
  max-height: 520px;
  overflow-y: auto;
}
/* 菜单树在 Tab 内独立滚动，层级更深时不会挤压弹窗高度 */
.perm-tabs :deep(.ant-tabs-tabpane) {
  min-height: 0;
}
.role-page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}
.role-page-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.role-page-header-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}
.role-page-header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}
.update-time {
  font-size: 12px;
  color: #999;
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

.role-management {
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: 16px;
  overflow: hidden;
  min-height: 0;
}

.role-management > :deep(.vxe-table-list-container) {
  flex: 1;
  min-height: 0;
}

/* 统计卡片 */
.stat-cards {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
}

.stat-card {
  flex: 1;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-radius: 8px;
}

.stat-total { background: linear-gradient(135deg, #e6f7ff 0%, #bae7ff 100%); }
.stat-active { background: linear-gradient(135deg, #f6ffed 0%, #d9f7be 100%); }
.stat-disabled { background: linear-gradient(135deg, #fff7e6 0%, #ffe7ba 100%); }

.stat-card-value {
  font-size: 20px;
  font-weight: 600;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  color: #333;
}

.stat-card-label {
  font-size: 12px;
  color: #666;
  margin-top: 4px;
}

.stat-card-icon {
  font-size: 28px;
  color: rgba(0, 0, 0, 0.15);
}





/* ── VxeTable 表头边框线 2px ─────────────────────────── */
.role-management :deep(.vxe-table .vxe-header--row th) {
  border-bottom: 2px solid #e8e8e8 !important;
}
.role-management :deep(.vxe-table .vxe-header--row th:not(:last-child)) {
  border-right: 1px solid #e8e8e8 !important;
}

/* 响应式 */
@media (max-width: 768px) {
  .stat-cards { flex-wrap: wrap; }
  .stat-card { flex: 1 1 45%; min-width: 120px; }
}

/* ── 角色详情（只读）分区 ──────────────────────────── */
.detail-section-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 16px 0 8px;
}

.detail-section-title {
  font-size: 14px;
  font-weight: 500;
  color: #1890ff;
}

.detail-section-count {
  font-size: 12px;
  color: #666;
}

.detail-section-hint {
  font-size: 12px;
  color: #999;
}

.detail-perm-group {
  margin-bottom: 10px;
}

.detail-perm-module {
  font-size: 12px;
  color: #666;
  margin-bottom: 6px;
}

.detail-perm-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.detail-perm-tags :deep(.ant-tag) {
  margin-inline-end: 0;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 12px;
}

.detail-id-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

/* ── FullScreenDetail form compact overrides ── */
.fsd-body .ant-form-item {
  margin-bottom: 12px !important;
}
.fsd-body .ant-form-item:last-child {
  margin-bottom: 0 !important;
}
.fsd-body .ant-input,
.fsd-body .ant-input-password,
.fsd-body .ant-input-number,
.fsd-body .ant-select,
.fsd-body .ant-picker,
.fsd-body .ant-tree-select,
.fsd-body .ant-cascader-picker {
  min-height: 28px !important;
  font-size: 13px !important;
}
.fsd-body .ant-form-item-label > label {
  font-size: 13px !important;
}

/* ── 紧凑尺寸覆盖：28px 输入框 ──────────────────────── */
.role-management :deep(.ant-input-sm),
.role-management :deep(.ant-input-number-sm),
.role-management :deep(.ant-select-single.ant-select-sm .ant-select-selector),
.role-management :deep(.ant-picker-small),
.role-management :deep(.ant-btn-sm) {
  height: 28px; line-height: 28px;
}
.role-management :deep(.ant-select-single.ant-select-sm .ant-select-selector) { line-height: 26px; }
.role-management :deep(.ant-input-number-sm input) { height: 26px; }

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

</style>
