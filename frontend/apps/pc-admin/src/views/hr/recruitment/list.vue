<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        招聘管理（人力资源 → 招聘管理）
        · 两个 Tab：① 招聘职位（职位发布登记簿）② 候选人（投递 → 面试 → 录用 → 转入职 主线）
        · 无左分类树（两表均无树形数据源）→ show-category-panel=false
        · 每个 Tab 各一套列定义 + 一套查询条件 + 一套功能按钮 + 一个 storage-key
        · 列配置齿轮在数据表表头 rowNo 列（个人配置 / 全局配置，与 storage-key 同值）
        · 所有请求一律走 @/api/hr 的封装方法（相对路径），视图内不出现任何带 /api 前缀的字面量
      -->
      <CategoryListLayout
        :tabs="TABS"
        :active-tab="activeTab"
        :show-category-panel="false"
        :show-table-footer="true"
        @tab-change="handleTabChange"
      >
        <!-- ═══ 工具栏左侧 ═══ -->
        <template #toolbar-left>
          <a-button
            v-if="activeTab === 'recruitment' && isButtonEnabled('add')"
            type="primary"
            size="small"
            class="btn-add"
            @click="handleAddRecruitment"
          >
            <PlusOutlined /> 新增招聘
          </a-button>
          <a-button
            v-else-if="activeTab === 'candidate' && isButtonEnabled('add')"
            type="primary"
            size="small"
            class="btn-add"
            @click="handleAddCandidate"
          >
            <PlusOutlined /> 新增候选人
          </a-button>
        </template>

        <!-- ═══ 工具栏右侧 ═══ -->
        <template #toolbar-right>
          <a-space :size="8">
            <a-button
              size="small"
              title="页面配置"
              @click="showPageConfig = true"
            >
              <SettingOutlined />
            </a-button>
            <a-button
              v-if="isButtonEnabled('refresh')"
              size="small"
              :loading="loading"
              @click="fetchList"
            >
              <ReloadOutlined /> 刷新
            </a-button>
            <a-button
              v-if="activeTab === 'recruitment' && isButtonEnabled('printF8')"
              size="small"
              @click="handlePrint"
            >
              <PrinterOutlined /> 打印(F8)
            </a-button>
            <a-button
              v-if="isButtonEnabled('export')"
              size="small"
              :loading="exporting"
              @click="handleExport"
            >
              <DownloadOutlined /> 导出
            </a-button>
          </a-space>
        </template>

        <!-- ═══ 查询区（字段显隐由页面配置控制，逐 Tab 独立） ═══ -->
        <template #search-fields>
          <!-- 招聘职位 -->
          <div
            v-if="activeTab === 'recruitment'"
            class="search-area"
          >
            <div class="search-row">
              <div
                v-if="isQueryVisible('positionName')"
                class="search-item"
              >
                <span class="search-label">岗位名称</span>
                <a-input
                  v-model:value="recruitmentQuery.positionName"
                  placeholder="请输入岗位名称"
                  size="small"
                  style="width: 200px"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('channel')"
                class="search-item"
              >
                <span class="search-label">招聘渠道</span>
                <a-select
                  v-model:value="recruitmentQuery.channel"
                  placeholder="全部渠道"
                  size="small"
                  style="width: 130px"
                  allow-clear
                  :options="RECRUITMENT_CHANNEL_OPTIONS"
                  @change="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('status')"
                class="search-item"
              >
                <span class="search-label">状态</span>
                <a-select
                  v-model:value="recruitmentQuery.status"
                  placeholder="全部状态"
                  size="small"
                  style="width: 120px"
                  allow-clear
                  :options="recruitmentStatusOptions"
                  @change="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('publishDate')"
                class="search-item"
              >
                <span class="search-label">发布日期</span>
                <a-range-picker
                  v-model:value="recruitmentQuery.publishDateRange"
                  size="small"
                  style="width: 230px"
                  value-format="YYYY-MM-DD"
                  @change="handleSearch"
                />
              </div>
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

          <!-- 候选人 -->
          <div
            v-else
            class="search-area"
          >
            <div class="search-row">
              <div
                v-if="isQueryVisible('name')"
                class="search-item"
              >
                <span class="search-label">候选人姓名</span>
                <a-input
                  v-model:value="candidateQuery.name"
                  placeholder="请输入候选人姓名"
                  size="small"
                  style="width: 180px"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('recruitmentId')"
                class="search-item"
              >
                <span class="search-label">应聘职位</span>
                <a-select
                  v-model:value="candidateQuery.recruitmentId"
                  placeholder="全部职位"
                  size="small"
                  style="width: 220px"
                  allow-clear
                  show-search
                  option-filter-prop="label"
                  :options="recruitmentOptions"
                  @change="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('candidateStatus')"
                class="search-item"
              >
                <span class="search-label">状态</span>
                <a-select
                  v-model:value="candidateQuery.status"
                  placeholder="全部状态"
                  size="small"
                  style="width: 120px"
                  allow-clear
                  :options="CANDIDATE_STATUS_OPTIONS"
                  @change="handleSearch"
                />
              </div>
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

        <!-- ═══ 数据表（两个 Tab 共用实例；:key 强制换 Tab 时重挂载以重载列配置） ═══ -->
        <template #table>
          <div class="table-area">
            <BillDetailTable
              :key="tableStorageKey"
              v-model:data-source="tableData"
              :columns="activeColumns"
              :loading="loading"
              :view-mode="true"
              :min-rows="20"
              row-key="id"
              :storage-key="tableStorageKey"
              :global-config-key="tableStorageKey"
            >
              <!-- ── 招聘职位：渠道 / 紧急程度 / 状态 / 学历 / 薪资 / 发布人 / 日期 ── -->
              <template #channelCell="{ record }">
                <span v-if="!record.__ghost">{{ RECRUITMENT_CHANNEL_MAP[record.channel] || record.channel || '-' }}</span>
              </template>

              <template #urgencyCell="{ record }">
                <a-tag
                  v-if="!record.__ghost"
                  :color="urgencyColor(record.urgency)"
                >
                  {{ URGENCY_MAP[record.urgency] || '-' }}
                </a-tag>
              </template>

              <template #recruitmentStatusCell="{ record }">
                <a-tag
                  v-if="!record.__ghost"
                  :color="recruitmentStatusColor(record.status)"
                >
                  {{ recruitmentStatusText(record.status) }}
                </a-tag>
              </template>

              <template #recruitmentEducationCell="{ record }">
                <span v-if="!record.__ghost">{{ EDUCATION_MAP[record.requiredEducation] || '-' }}</span>
              </template>

              <template #salaryCell="{ record }">
                <span v-if="!record.__ghost">{{ formatSalaryRange(record.salaryMin, record.salaryMax) }}</span>
              </template>

              <template #publisherCell="{ record }">
                <span v-if="!record.__ghost">{{ record.publisherName || record.publisherId || '-' }}</span>
              </template>

              <template #recruitmentActionCell="{ record }">
                <a-space
                  v-if="!record.__ghost"
                  :size="0"
                >
                  <a-button
                    type="link"
                    size="small"
                    @click="handleEditRecruitment(record)"
                  >
                    修改
                  </a-button>
                  <a-dropdown>
                    <a-button
                      type="link"
                      size="small"
                    >
                      状态流转
                    </a-button>
                    <template #overlay>
                      <a-menu>
                        <a-menu-item
                          v-for="item in RECRUITMENT_FLOW_OPTIONS"
                          :key="item.value"
                          :disabled="record.status === item.value"
                          @click="handleRecruitmentStatus(record, item.value)"
                        >
                          {{ item.label }}
                        </a-menu-item>
                      </a-menu>
                    </template>
                  </a-dropdown>
                  <a-button
                    type="link"
                    size="small"
                    danger
                    @click="handleDeleteRecruitment(record)"
                  >
                    删除
                  </a-button>
                </a-space>
              </template>

              <!-- ── 候选人：性别 / 学历 / 期望薪资 / 来源 / 状态 / 面试评分 / 日期时间 ── -->
              <template #genderCell="{ record }">
                <span v-if="!record.__ghost">{{ GENDER_MAP[record.gender] || '-' }}</span>
              </template>

              <template #candidateEducationCell="{ record }">
                <span v-if="!record.__ghost">{{ EDUCATION_MAP[record.education] || '-' }}</span>
              </template>

              <template #moneyCell="{ record, column }">
                <span v-if="!record.__ghost">{{ formatMoney(record[column.key]) }}</span>
              </template>

              <template #sourceCell="{ record }">
                <span v-if="!record.__ghost">{{ RECRUITMENT_CHANNEL_MAP[record.source] || record.source || '-' }}</span>
              </template>

              <template #candidateStatusCell="{ record }">
                <a-tag
                  v-if="!record.__ghost"
                  :color="candidateStatusColor(record.status)"
                >
                  {{ candidateStatusText(record.status) }}
                </a-tag>
              </template>

              <template #ratingCell="{ record }">
                <span v-if="!record.__ghost">
                  <a-rate
                    v-if="record.rating"
                    :value="record.rating"
                    disabled
                    style="font-size: 12px"
                  />
                  <span v-else>-</span>
                </span>
              </template>

              <template #dateCell="{ record, column }">
                <span v-if="!record.__ghost">{{ formatDate(record[column.key]) }}</span>
              </template>

              <template #dateTimeCell="{ record, column }">
                <span v-if="!record.__ghost">{{ formatDateTime(record[column.key]) }}</span>
              </template>

              <template #candidateActionCell="{ record }">
                <a-space
                  v-if="!record.__ghost"
                  :size="0"
                >
                  <a-button
                    type="link"
                    size="small"
                    @click="handleEditCandidate(record)"
                  >
                    修改
                  </a-button>
                  <a-button
                    type="link"
                    size="small"
                    @click="handleOpenInterview(record)"
                  >
                    面试评价
                  </a-button>
                  <a-dropdown>
                    <a-button
                      type="link"
                      size="small"
                    >
                      更多
                    </a-button>
                    <template #overlay>
                      <a-menu>
                        <a-sub-menu
                          v-if="record.status !== 7"
                          title="推进状态"
                        >
                          <a-menu-item
                            v-for="item in CANDIDATE_FLOW_OPTIONS"
                            :key="item.value"
                            :disabled="record.status === item.value"
                            @click="handleCandidateStatus(record, item.value)"
                          >
                            {{ item.label }}
                          </a-menu-item>
                        </a-sub-menu>
                        <a-menu-item
                          v-if="record.status !== 7"
                          @click="handleOpenHire(record)"
                        >
                          转入职
                        </a-menu-item>
                        <a-menu-divider />
                        <a-menu-item
                          danger
                          @click="handleDeleteCandidate(record)"
                        >
                          删除
                        </a-menu-item>
                      </a-menu>
                    </template>
                  </a-dropdown>
                </a-space>
              </template>
            </BillDetailTable>
          </div>
        </template>

        <!-- ═══ 底部：经典分页栏 ═══ -->
        <template #table-footer>
          <StandardPagination
            variant="classic"
            :current="pagination.current"
            :page-size="pagination.pageSize"
            :total="pagination.total"
            :page-size-options="[20, 50, 100]"
            @change="handlePageChange"
          />
        </template>
      </CategoryListLayout>

      <!-- ═══ 招聘职位表单（主数据：分区卡片 + 两列栅格 + 行内校验） ═══ -->
      <a-modal
        v-model:open="recruitmentOpen"
        :title="recruitmentForm.id ? '修改招聘职位' : '新增招聘职位'"
        :width="780"
        :mask-closable="false"
        :confirm-loading="recruitmentSaving"
        @ok="handleSaveRecruitment"
      >
        <a-form
          ref="recruitmentFormRef"
          :model="recruitmentForm"
          :rules="recruitmentRules"
          :label-col="{ span: 8 }"
          :wrapper-col="{ span: 15 }"
          size="small"
        >
          <FormSection
            title="岗位信息"
            tip="岗位名称必填；部门与招聘人数用于编制登记"
          >
            <a-row :gutter="24">
              <a-col :span="12">
                <a-form-item
                  label="岗位名称"
                  name="positionName"
                >
                  <a-input
                    v-model:value="recruitmentForm.positionName"
                    placeholder="如 前端开发工程师"
                    :maxlength="100"
                  />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="所属部门">
                  <a-tree-select
                    v-model:value="recruitmentForm.deptId"
                    :tree-data="deptTreeData"
                    placeholder="请选择部门"
                    allow-clear
                    tree-default-expand-all
                    :field-names="{ label: 'categoryName', value: 'id', children: 'children' }"
                  />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item
                  label="招聘人数"
                  name="headcount"
                >
                  <a-input-number
                    v-model:value="recruitmentForm.headcount"
                    :min="1"
                    :precision="0"
                    style="width: 100%"
                  />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="招聘渠道">
                  <a-select
                    v-model:value="recruitmentForm.channel"
                    :options="RECRUITMENT_CHANNEL_OPTIONS"
                  />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="紧急程度">
                  <a-select
                    v-model:value="recruitmentForm.urgency"
                    :options="URGENCY_OPTIONS"
                  />
                </a-form-item>
              </a-col>
            </a-row>
          </FormSection>

          <FormSection title="任职条件与薪酬">
            <a-row :gutter="24">
              <a-col :span="12">
                <a-form-item label="学历要求">
                  <a-select
                    v-model:value="recruitmentForm.requiredEducation"
                    placeholder="请选择学历要求"
                    allow-clear
                    :options="educationOptions"
                  />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="工作经验要求">
                  <a-input
                    v-model:value="recruitmentForm.requiredExperience"
                    placeholder="如 3年以上"
                    :maxlength="50"
                  />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item
                  label="薪资下限"
                  name="salaryMin"
                >
                  <a-input-number
                    v-model:value="recruitmentForm.salaryMin"
                    :min="0"
                    :precision="2"
                    style="width: 100%"
                    placeholder="元/月"
                  />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item
                  label="薪资上限"
                  name="salaryMax"
                >
                  <a-input-number
                    v-model:value="recruitmentForm.salaryMax"
                    :min="0"
                    :precision="2"
                    style="width: 100%"
                    placeholder="元/月"
                  />
                </a-form-item>
              </a-col>
            </a-row>
          </FormSection>

          <FormSection title="发布安排">
            <a-row :gutter="24">
              <a-col :span="12">
                <a-form-item label="发布日期">
                  <a-date-picker
                    v-model:value="recruitmentForm.publishDate"
                    style="width: 100%"
                    value-format="YYYY-MM-DD"
                  />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item
                  label="截止日期"
                  name="expireDate"
                >
                  <a-date-picker
                    v-model:value="recruitmentForm.expireDate"
                    style="width: 100%"
                    value-format="YYYY-MM-DD"
                  />
                </a-form-item>
              </a-col>
            </a-row>
          </FormSection>

          <FormSection title="岗位描述与要求">
            <a-row :gutter="24">
              <a-col :span="24">
                <a-form-item label="岗位描述">
                  <a-textarea
                    v-model:value="recruitmentForm.description"
                    :rows="3"
                    :maxlength="2000"
                    placeholder="岗位职责 / 工作内容"
                  />
                </a-form-item>
              </a-col>
              <a-col :span="24">
                <a-form-item label="任职要求">
                  <a-textarea
                    v-model:value="recruitmentForm.requirements"
                    :rows="3"
                    :maxlength="2000"
                    placeholder="技能 / 证书 / 其它硬性要求"
                  />
                </a-form-item>
              </a-col>
              <a-col :span="24">
                <a-form-item label="备注">
                  <a-textarea
                    v-model:value="recruitmentForm.remark"
                    :rows="2"
                    :maxlength="500"
                  />
                </a-form-item>
              </a-col>
            </a-row>
          </FormSection>
        </a-form>
      </a-modal>

      <!-- ═══ 候选人表单 ═══ -->
      <a-modal
        v-model:open="candidateOpen"
        :title="candidateForm.id ? '修改候选人' : '新增候选人'"
        :width="780"
        :mask-closable="false"
        :confirm-loading="candidateSaving"
        @ok="handleSaveCandidate"
      >
        <a-form
          ref="candidateFormRef"
          :model="candidateForm"
          :rules="candidateRules"
          :label-col="{ span: 8 }"
          :wrapper-col="{ span: 15 }"
          size="small"
        >
          <FormSection
            title="应聘信息"
            tip="应聘职位与姓名必填；修改时不可更换应聘职位"
          >
            <a-row :gutter="24">
              <a-col :span="12">
                <a-form-item
                  label="应聘职位"
                  name="recruitmentId"
                >
                  <a-select
                    v-model:value="candidateForm.recruitmentId"
                    placeholder="请选择应聘职位"
                    allow-clear
                    show-search
                    option-filter-prop="label"
                    :disabled="!!candidateForm.id"
                    :options="recruitmentOptions"
                  />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item
                  label="姓名"
                  name="name"
                >
                  <a-input
                    v-model:value="candidateForm.name"
                    placeholder="请输入候选人姓名"
                    :maxlength="50"
                  />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="性别">
                  <a-select
                    v-model:value="candidateForm.gender"
                    placeholder="请选择"
                    allow-clear
                    :options="genderOptions"
                  />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item
                  label="手机号"
                  name="phone"
                >
                  <a-input
                    v-model:value="candidateForm.phone"
                    placeholder="11 位手机号"
                    :maxlength="20"
                  />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item
                  label="邮箱"
                  name="email"
                >
                  <a-input
                    v-model:value="candidateForm.email"
                    :maxlength="100"
                  />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="出生日期">
                  <a-date-picker
                    v-model:value="candidateForm.birthDate"
                    style="width: 100%"
                    value-format="YYYY-MM-DD"
                  />
                </a-form-item>
              </a-col>
            </a-row>
          </FormSection>

          <FormSection title="教育与工作经历">
            <a-row :gutter="24">
              <a-col :span="12">
                <a-form-item label="学历">
                  <a-select
                    v-model:value="candidateForm.education"
                    placeholder="请选择学历"
                    allow-clear
                    :options="educationOptions"
                  />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="毕业院校">
                  <a-input
                    v-model:value="candidateForm.school"
                    :maxlength="100"
                  />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="专业">
                  <a-input
                    v-model:value="candidateForm.major"
                    :maxlength="100"
                  />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="工作年限">
                  <a-input
                    v-model:value="candidateForm.experience"
                    placeholder="如 3年 / 应届"
                    :maxlength="30"
                  />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="当前公司">
                  <a-input
                    v-model:value="candidateForm.currentCompany"
                    :maxlength="100"
                  />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="当前职位">
                  <a-input
                    v-model:value="candidateForm.currentPosition"
                    :maxlength="100"
                  />
                </a-form-item>
              </a-col>
            </a-row>
          </FormSection>

          <FormSection
            title="来源与期望"
            tip="简历附件请填写可访问的文件地址（本页不提供上传）"
          >
            <a-row :gutter="24">
              <a-col :span="12">
                <a-form-item label="期望薪资">
                  <a-input-number
                    v-model:value="candidateForm.expectedSalary"
                    :min="0"
                    :precision="2"
                    style="width: 100%"
                    placeholder="元/月"
                  />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="来源渠道">
                  <a-select
                    v-model:value="candidateForm.source"
                    placeholder="请选择来源"
                    allow-clear
                    :options="RECRUITMENT_CHANNEL_OPTIONS"
                  />
                </a-form-item>
              </a-col>
              <a-col :span="24">
                <a-form-item label="简历附件URL">
                  <a-input
                    v-model:value="candidateForm.resumeUrl"
                    :maxlength="500"
                    placeholder="https://..."
                  />
                </a-form-item>
              </a-col>
              <a-col :span="24">
                <a-form-item label="备注">
                  <a-textarea
                    v-model:value="candidateForm.remark"
                    :rows="2"
                    :maxlength="500"
                  />
                </a-form-item>
              </a-col>
            </a-row>
          </FormSection>
        </a-form>
      </a-modal>

      <!-- ═══ 面试评价（覆盖式写入：只传评分会清空已有评价） ═══ -->
      <a-modal
        v-model:open="interviewOpen"
        :title="`面试评价 - ${interviewTarget?.name || ''}`"
        :width="560"
        :confirm-loading="actionSaving"
        @ok="handleSubmitInterview"
      >
        <a-alert
          class="dialog-alert"
          type="warning"
          show-icon
          message="面试评价为覆盖式写入：本次只填评分将清空已保存的文字评价。"
        />
        <a-form
          :label-col="{ span: 5 }"
          :wrapper-col="{ span: 17 }"
          size="small"
        >
          <a-form-item label="面试评分">
            <a-rate v-model:value="interviewForm.rating" />
            <span class="form-hint">1 ~ 5 分</span>
          </a-form-item>
          <a-form-item label="面试评价">
            <a-textarea
              v-model:value="interviewForm.interviewComment"
              :rows="4"
              :maxlength="500"
              placeholder="面试表现 / 结论"
            />
          </a-form-item>
        </a-form>
      </a-modal>

      <!-- ═══ 转入职（其余字段由后端从候选人自动带入） ═══ -->
      <a-modal
        v-model:open="hireOpen"
        :title="`转入职 - ${hireTarget?.name || ''}`"
        :width="560"
        :confirm-loading="actionSaving"
        @ok="handleSubmitHire"
      >
        <a-alert
          class="dialog-alert"
          type="info"
          show-icon
          message="转入职将创建员工档案并置候选人为「已入职」；姓名 / 性别 / 手机号 / 邮箱 / 学历 / 院校 / 专业由候选人自动带入。"
        />
        <a-form
          :label-col="{ span: 6 }"
          :wrapper-col="{ span: 16 }"
          size="small"
        >
          <a-form-item label="部门">
            <a-tree-select
              v-model:value="hireForm.deptId"
              :tree-data="deptTreeData"
              placeholder="请选择部门"
              allow-clear
              tree-default-expand-all
              :field-names="{ label: 'categoryName', value: 'id', children: 'children' }"
            />
          </a-form-item>
          <a-form-item label="岗位">
            <a-select
              v-model:value="hireForm.positionId"
              placeholder="请选择岗位"
              allow-clear
              show-search
              option-filter-prop="label"
              :options="positionOptions"
            />
          </a-form-item>
          <a-form-item label="入职日期">
            <a-date-picker
              v-model:value="hireForm.hireDate"
              style="width: 100%"
              value-format="YYYY-MM-DD"
            />
          </a-form-item>
          <a-form-item label="员工类型">
            <a-select
              v-model:value="hireForm.employeeType"
              :options="employeeTypeOptions"
            />
          </a-form-item>
        </a-form>
      </a-modal>

      <!-- ═══ 页面配置（查询条件 / 功能按钮，storage-key 随 Tab 切换） ═══ -->
      <PageConfigPanel
        :open="showPageConfig"
        :query-fields-config="queryFields"
        :function-buttons-config="functionButtons"
        :default-query-fields-config="activeDefaultQueryFields"
        :default-function-buttons-config="activeDefaultFunctionButtons"
        :storage-key="pageConfigStorageKey"
        hide-print-config
        @update:open="showPageConfig = $event"
        @change="handlePageConfigChange"
      />
    </PageContainer>
    <!-- 打印：结果集打印 -->
    <PrintDialog
      ref="printDialogRef"
      page-code="hr-recruitment-list"
      :print-data="printData"
    />
  </ErrorBoundary>
</template>

<script setup lang="ts">
/**
 * 招聘管理（人力资源 → 招聘管理）
 */
// 缺口：招聘需求审批流（status=0「待审批」无审批端点，工作流未接线）—— 后端无端点，本页不实现
// 缺口：面试轮次表（多轮 / 多面试官 / 每轮结论）—— 后端仅一组面试字段且覆盖式写入，本页不实现
// 缺口：Offer 管理（Offer 金额 / 发放时间 / 答复期限 / 审批）—— 后端无表无端点，本页不实现
// 缺口：招聘阶段（可配置阶段 + 阶段流转日志）与漏斗统计（渠道转化 / Time to Hire）—— 后端无端点，本页不实现
// 缺口：候选人数据合规（保留期 / 匿名化 / 访问审计）—— 后端无端点，本页不实现
// 缺口：转入职时不自动开通系统账号（招聘 → 入职 → 员工档案仅到建档为止）—— 后端无端点，本页不实现
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import { message, Modal } from 'ant-design-vue'
import dayjs from 'dayjs'
import {
  PlusOutlined,
  ReloadOutlined,
  PrinterOutlined,
  DownloadOutlined,
  SettingOutlined,
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import StandardPagination from '@/components/Pagination/Pagination.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import FormSection from '@/components/FormSection/index.vue'
import { departmentApi } from '@/api/department'
import {
  hrRecruitmentApi,
  hrCandidateApi,
  hrPositionApi,
  RECRUITMENT_STATUS_MAP,
  CANDIDATE_STATUS_MAP,
  CANDIDATE_STATUS_OPTIONS,
  RECRUITMENT_CHANNEL_MAP,
  RECRUITMENT_CHANNEL_OPTIONS,
  URGENCY_MAP,
  URGENCY_OPTIONS,
  EDUCATION_MAP,
  GENDER_MAP,
  EMPLOYEE_TYPE_MAP,
  type HrRecruitment,
  type HrCandidate,
} from '@/api/hr'
import PrintDialog from '@/components/PrintDialog/index.vue'
import { useListPrint } from '@/composables/useListPrint'

defineOptions({ name: 'HrRecruitmentList' })

type TabKey = 'recruitment' | 'candidate'

const TABS = [
  { key: 'recruitment', label: '招聘职位' },
  { key: 'candidate', label: '候选人' },
]

// ═══ 状态 ═══
const activeTab = ref<TabKey>('recruitment')
const loading = ref(false)
const exporting = ref(false)
const actionSaving = ref(false)
const tableData = ref<any[]>([])
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

// ═══ 下拉选项（映射表一律取自 @/api/hr，页面内不再重复定义） ═══
const recruitmentStatusOptions = Object.entries(RECRUITMENT_STATUS_MAP).map(([k, v]) => ({ label: v.text, value: Number(k) }))
const educationOptions = Object.entries(EDUCATION_MAP).map(([k, v]) => ({ label: v, value: Number(k) }))
const genderOptions = Object.entries(GENDER_MAP).map(([k, v]) => ({ label: v, value: Number(k) }))
const employeeTypeOptions = Object.entries(EMPLOYEE_TYPE_MAP).map(([k, v]) => ({ label: v, value: Number(k) }))

/** 招聘职位的可选流转目标：不含 0 待审批（本系统无审批能力） */
const RECRUITMENT_FLOW_OPTIONS = [1, 2, 3, 4].map(v => ({ label: RECRUITMENT_STATUS_MAP[v].text, value: v }))
/** 候选人可选流转目标：不含 7 已入职（须走「转入职」以同时建档） */
const CANDIDATE_FLOW_OPTIONS = CANDIDATE_STATUS_OPTIONS.filter(o => o.value !== 7)

// ═══ 查询条件（逐 Tab 各一套） ═══
const recruitmentQuery = reactive({
  positionName: '',
  channel: undefined as string | undefined,
  status: undefined as number | undefined,
  publishDateRange: undefined as [string, string] | undefined,
})

const candidateQuery = reactive({
  name: '',
  recruitmentId: undefined as number | string | undefined,
  status: undefined as number | undefined,
})

// ═══ 列定义：招聘职位（每个 Tab 一套，storage-key 各自独立） ═══
const recruitmentColumns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'recruitmentActionCell', width: 200, fixed: 'left' },
  { key: 'positionName', title: '岗位名称', type: 'input', width: 150 },
  { key: 'deptName', title: '所属部门', type: 'input', width: 130 },
  { key: 'headcount', title: '招聘人数', type: 'input', width: 90, align: 'center' },
  { key: 'applicantCount', title: '已应聘', type: 'input', width: 80, align: 'center' },
  { key: 'hiredCount', title: '已录用', type: 'input', width: 80, align: 'center' },
  { key: 'channel', title: '招聘渠道', type: 'slot', slotName: 'channelCell', width: 110 },
  { key: 'urgency', title: '紧急程度', type: 'slot', slotName: 'urgencyCell', width: 90, align: 'center' },
  { key: 'status', title: '状态', type: 'slot', slotName: 'recruitmentStatusCell', width: 90, align: 'center' },
  { key: 'requiredEducation', title: '学历要求', type: 'slot', slotName: 'recruitmentEducationCell', width: 100 },
  { key: 'requiredExperience', title: '工作经验', type: 'input', width: 120 },
  { key: 'salaryRange', title: '薪资范围', type: 'slot', slotName: 'salaryCell', width: 150, align: 'right' },
  { key: 'publisherName', title: '发布人', type: 'slot', slotName: 'publisherCell', width: 100 },
  { key: 'publishDate', title: '发布日期', type: 'slot', slotName: 'dateCell', width: 120 },
  { key: 'expireDate', title: '截止日期', type: 'slot', slotName: 'dateCell', width: 120 },
  // ── 默认隐藏 ──
  { key: 'description', title: '岗位描述', type: 'input', width: 220, defaultHidden: true },
  { key: 'requirements', title: '任职要求', type: 'input', width: 220, defaultHidden: true },
  { key: 'remark', title: '备注', type: 'input', width: 200, defaultHidden: true },
]

// ═══ 列定义：候选人 ═══
const candidateColumns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'candidateActionCell', width: 200, fixed: 'left' },
  { key: 'name', title: '姓名', type: 'input', width: 100 },
  { key: 'gender', title: '性别', type: 'slot', slotName: 'genderCell', width: 70, align: 'center' },
  { key: 'phone', title: '手机号', type: 'input', width: 130 },
  { key: 'education', title: '学历', type: 'slot', slotName: 'candidateEducationCell', width: 90 },
  { key: 'experience', title: '工作年限', type: 'input', width: 100 },
  { key: 'currentCompany', title: '当前公司', type: 'input', width: 150 },
  { key: 'currentPosition', title: '当前职位', type: 'input', width: 130 },
  { key: 'expectedSalary', title: '期望薪资', type: 'slot', slotName: 'moneyCell', width: 110, align: 'right' },
  { key: 'source', title: '来源', type: 'slot', slotName: 'sourceCell', width: 110 },
  { key: 'status', title: '状态', type: 'slot', slotName: 'candidateStatusCell', width: 100, align: 'center' },
  { key: 'interviewerName', title: '面试官', type: 'input', width: 100 },
  { key: 'interviewTime', title: '面试时间', type: 'slot', slotName: 'dateTimeCell', width: 150 },
  { key: 'rating', title: '面试评分', type: 'slot', slotName: 'ratingCell', width: 120 },
  // ── 默认隐藏 ──
  { key: 'email', title: '邮箱', type: 'input', width: 180, defaultHidden: true },
  { key: 'school', title: '毕业院校', type: 'input', width: 160, defaultHidden: true },
  { key: 'major', title: '专业', type: 'input', width: 140, defaultHidden: true },
  { key: 'interviewComment', title: '面试评价', type: 'input', width: 220, defaultHidden: true },
  { key: 'remark', title: '备注', type: 'input', width: 200, defaultHidden: true },
]

const activeColumns = computed<DetailColumnConfig[]>(() =>
  activeTab.value === 'recruitment' ? recruitmentColumns : candidateColumns
)

/**
 * 列配置 storage-key 与 global-config-key 同值（同一份配置同时作为个人 key 与全局 key）；
 * 与 PageConfigPanel 的 storage-key 必须不同值（一个管列，一个管查询条件/按钮）。
 */
const tableStorageKey = computed(() => `hr-${activeTab.value}-table-columns`)
const pageConfigStorageKey = computed(() => `hr-${activeTab.value}-page-config`)

// ═══ 展示辅助 ═══
function recruitmentStatusText(status: any): string {
  return RECRUITMENT_STATUS_MAP[status]?.text || '-'
}

function recruitmentStatusColor(status: any): string {
  return RECRUITMENT_STATUS_MAP[status]?.color || 'default'
}

function candidateStatusText(status: any): string {
  return CANDIDATE_STATUS_MAP[status]?.text || '-'
}

function candidateStatusColor(status: any): string {
  return CANDIDATE_STATUS_MAP[status]?.color || 'default'
}

/** 紧急程度：URGENCY_MAP 只给文案，颜色在页面内按「普通/紧急/特急」三档指定 */
function urgencyColor(urgency: any): string {
  return ({ 1: 'default', 2: 'warning', 3: 'error' } as Record<number, string>)[urgency] || 'default'
}

function formatDate(val: any): string {
  return val ? String(val).slice(0, 10) : '-'
}

function formatDateTime(val: any): string {
  return val ? String(val).replace('T', ' ').slice(0, 16) : '-'
}

function formatMoney(val: any): string {
  if (val === null || val === undefined || val === '') return '-'
  const n = Number(val)
  return Number.isFinite(n) ? n.toFixed(2) : '-'
}

/** 薪资区间：≥1 万折算为「N.N万」，否则取原值 */
function formatSalaryValue(val: any): string {
  if (val === null || val === undefined || val === '') return ''
  const n = Number(val)
  if (!Number.isFinite(n)) return ''
  return n >= 10000 ? `${(n / 10000).toFixed(1)}万` : String(n)
}

function formatSalaryRange(min: any, max: any): string {
  const lo = formatSalaryValue(min)
  const hi = formatSalaryValue(max)
  if (!lo && !hi) return '-'
  return `${lo || '?'} - ${hi || '?'}`
}

// ═══ 部门树（数据源 sys_department，HR 只读引用）与部门名回填 ═══
const deptTreeData = ref<any[]>([])
const deptNameMap = ref<Record<string, string>>({})

async function loadDeptTree() {
  try {
    const res: any = await departmentApi.getTree()
    const list: any[] = Array.isArray(res) ? res : res?.data || []
    const map: Record<string, string> = {}
    const walk = (nodes: any[]): any[] => (nodes || []).map((n: any) => {
      map[String(n.id)] = n.departmentName
      return { id: n.id, categoryName: n.departmentName, children: walk(n.children) }
    })
    deptTreeData.value = walk(list)
    deptNameMap.value = map
  } catch (error) {
    console.warn('[招聘管理] 部门树加载失败', error)
  }
}

// ═══ 岗位下拉（转入职用） ═══
const positionOptions = ref<{ label: string; value: number | string }[]>([])

async function loadPositions() {
  try {
    const res: any = await hrPositionApi.list()
    const list: any[] = Array.isArray(res) ? res : res?.records || []
    positionOptions.value = list.map((p: any) => ({
      label: p.positionCode ? `[${p.positionCode}] ${p.positionName}` : p.positionName,
      value: p.id,
    }))
  } catch (error) {
    console.warn('[招聘管理] 岗位列表加载失败', error)
  }
}

// ═══ 应聘职位下拉（候选人 Tab 查询与表单共用） ═══
const recruitmentOptions = ref<{ label: string; value: number | string }[]>([])

async function loadRecruitmentOptions() {
  try {
    const res: any = await hrRecruitmentApi.page({ pageNum: 1, pageSize: 200 })
    const list: any[] = res?.records || []
    recruitmentOptions.value = list.map((r: any) => ({
      label: r.deptName ? `${r.positionName}（${r.deptName}）` : r.positionName,
      value: r.id,
    }))
  } catch (error) {
    console.warn('[招聘管理] 应聘职位下拉加载失败', error)
  }
}

// ═══ 数据加载 ═══
function buildRecruitmentParams(): Record<string, any> {
  const params: Record<string, any> = {
    pageNum: pagination.current,
    pageSize: pagination.pageSize,
  }
  if (recruitmentQuery.positionName) params.positionName = recruitmentQuery.positionName.trim()
  if (recruitmentQuery.channel) params.channel = recruitmentQuery.channel
  if (recruitmentQuery.status !== undefined && recruitmentQuery.status !== null) params.status = recruitmentQuery.status
  if (recruitmentQuery.publishDateRange?.length === 2) {
    params.startDate = recruitmentQuery.publishDateRange[0]
    params.endDate = recruitmentQuery.publishDateRange[1]
  }
  return params
}

function buildCandidateParams(): Record<string, any> {
  const params: Record<string, any> = {
    pageNum: pagination.current,
    pageSize: pagination.pageSize,
  }
  if (candidateQuery.name) params.name = candidateQuery.name.trim()
  if (candidateQuery.recruitmentId !== undefined && candidateQuery.recruitmentId !== null) {
    params.recruitmentId = candidateQuery.recruitmentId
  }
  if (candidateQuery.status !== undefined && candidateQuery.status !== null) params.status = candidateQuery.status
  return params
}

async function fetchList() {
  loading.value = true
  try {
    if (activeTab.value === 'recruitment') {
      const res: any = await hrRecruitmentApi.page(buildRecruitmentParams())
      tableData.value = res?.records || []
      pagination.total = Number(res?.total) || 0
    } else {
      const res: any = await hrCandidateApi.page(buildCandidateParams())
      tableData.value = res?.records || []
      pagination.total = Number(res?.total) || 0
    }
  } catch (error: any) {
    console.error('[招聘管理] 加载列表失败', error)
    message.error(error?.response?.data?.message || '加载列表失败')
    tableData.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  fetchList()
}

function handleReset() {
  if (activeTab.value === 'recruitment') {
    recruitmentQuery.positionName = ''
    recruitmentQuery.channel = undefined
    recruitmentQuery.status = undefined
    recruitmentQuery.publishDateRange = undefined
  } else {
    candidateQuery.name = ''
    candidateQuery.recruitmentId = undefined
    candidateQuery.status = undefined
  }
  handleSearch()
}

function handlePageChange(page: number, pageSize: number) {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchList()
}

/** 切 Tab：重载该 Tab 的页面配置（查询/按钮）与数据；列配置由 :key + storage-key 强制重载 */
function handleTabChange(key: string) {
  if (key !== 'recruitment' && key !== 'candidate') return
  if (key === activeTab.value) return
  activeTab.value = key
  pagination.current = 1
  loadPageConfig()
  if (key === 'candidate') loadRecruitmentOptions()
  fetchList()
}

// ═══ 招聘职位表单 ═══
const recruitmentOpen = ref(false)
const recruitmentSaving = ref(false)
const recruitmentFormRef = ref()

const emptyRecruitmentForm = () => ({
  id: null as number | string | null,
  positionName: '',
  deptId: undefined as number | string | undefined,
  headcount: 1 as number | undefined,
  channel: 'ONLINE' as string | undefined,
  urgency: 1 as number | undefined,
  requiredEducation: undefined as number | undefined,
  requiredExperience: '',
  salaryMin: undefined as number | undefined,
  salaryMax: undefined as number | undefined,
  publishDate: undefined as string | undefined,
  expireDate: undefined as string | undefined,
  description: '',
  requirements: '',
  remark: '',
})

const recruitmentForm = reactive(emptyRecruitmentForm())

const recruitmentRules = {
  positionName: [{ required: true, message: '请输入岗位名称', trigger: 'blur' }],
  headcount: [{ required: true, message: '请输入招聘人数', trigger: 'blur' }],
  salaryMax: [{
    validator: async (_rule: any, value: any) => {
      if (value === undefined || value === null || recruitmentForm.salaryMin === undefined || recruitmentForm.salaryMin === null) return
      if (Number(value) < Number(recruitmentForm.salaryMin)) throw new Error('薪资上限不能低于下限')
    },
    trigger: 'blur',
  }],
  expireDate: [{
    validator: async (_rule: any, value: any) => {
      if (!value || !recruitmentForm.publishDate) return
      if (String(value) < String(recruitmentForm.publishDate)) throw new Error('截止日期不能早于发布日期')
    },
    trigger: 'change',
  }],
}

function handleAddRecruitment() {
  Object.assign(recruitmentForm, emptyRecruitmentForm())
  recruitmentOpen.value = true
}

async function handleEditRecruitment(record: HrRecruitment) {
  Object.assign(recruitmentForm, emptyRecruitmentForm())
  recruitmentForm.id = record.id
  recruitmentForm.positionName = record.positionName || ''
  recruitmentOpen.value = true
  try {
    const detail: any = await hrRecruitmentApi.getById(record.id)
    if (!detail) return
    recruitmentForm.positionName = detail.positionName || ''
    recruitmentForm.deptId = detail.deptId ?? undefined
    recruitmentForm.headcount = detail.headcount ?? 1
    recruitmentForm.channel = detail.channel || undefined
    recruitmentForm.urgency = detail.urgency ?? 1
    recruitmentForm.requiredEducation = detail.requiredEducation ?? undefined
    recruitmentForm.requiredExperience = detail.requiredExperience || ''
    recruitmentForm.salaryMin = detail.salaryMin ?? undefined
    recruitmentForm.salaryMax = detail.salaryMax ?? undefined
    recruitmentForm.publishDate = detail.publishDate || undefined
    recruitmentForm.expireDate = detail.expireDate || undefined
    recruitmentForm.description = detail.description || ''
    recruitmentForm.requirements = detail.requirements || ''
    recruitmentForm.remark = detail.remark || ''
  } catch (error: any) {
    message.error(error?.response?.data?.message || '加载招聘职位详情失败')
  }
}

async function handleSaveRecruitment() {
  try {
    await recruitmentFormRef.value?.validate()
  } catch {
    return
  }
  const payload: Partial<HrRecruitment> = {
    positionName: recruitmentForm.positionName.trim(),
    deptId: (recruitmentForm.deptId ?? null) as any,
    deptName: recruitmentForm.deptId === undefined || recruitmentForm.deptId === null
      ? ''
      : (deptNameMap.value[String(recruitmentForm.deptId)] || ''),
    headcount: recruitmentForm.headcount ?? 1,
    channel: recruitmentForm.channel || undefined,
    urgency: recruitmentForm.urgency ?? 1,
    requiredEducation: (recruitmentForm.requiredEducation ?? null) as any,
    requiredExperience: recruitmentForm.requiredExperience || '',
    salaryMin: (recruitmentForm.salaryMin ?? null) as any,
    salaryMax: (recruitmentForm.salaryMax ?? null) as any,
    publishDate: (recruitmentForm.publishDate || null) as any,
    expireDate: (recruitmentForm.expireDate || null) as any,
    description: recruitmentForm.description || '',
    requirements: recruitmentForm.requirements || '',
    remark: recruitmentForm.remark || '',
  }
  recruitmentSaving.value = true
  try {
    if (recruitmentForm.id) {
      await hrRecruitmentApi.update(recruitmentForm.id, payload)
      message.success('修改成功')
    } else {
      await hrRecruitmentApi.create(payload)
      message.success('新增成功')
    }
    recruitmentOpen.value = false
    fetchList()
    loadRecruitmentOptions()
  } catch (error: any) {
    message.error(error?.response?.data?.message || '保存失败')
  } finally {
    recruitmentSaving.value = false
  }
}

function handleRecruitmentStatus(record: HrRecruitment, status: number) {
  Modal.confirm({
    title: '状态流转',
    content: `确定将「${record.positionName}」置为「${recruitmentStatusText(status)}」吗？`,
    onOk: async () => {
      try {
        await hrRecruitmentApi.updateStatus(record.id, status)
        message.success('状态更新成功')
        fetchList()
      } catch (error: any) {
        message.error(error?.response?.data?.message || '状态更新失败')
      }
    },
  })
}

function handleDeleteRecruitment(record: HrRecruitment) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除「${record.positionName}」吗？删除后该职位不再出现在台账中（已投递的候选人不会被一并删除）。`,
    okText: '确认删除',
    okType: 'danger',
    onOk: async () => {
      try {
        await hrRecruitmentApi.remove(record.id)
        message.success('删除成功')
        fetchList()
        loadRecruitmentOptions()
      } catch (error: any) {
        message.error(error?.response?.data?.message || '删除失败')
      }
    },
  })
}

// ═══ 候选人表单 ═══
const candidateOpen = ref(false)
const candidateSaving = ref(false)
const candidateFormRef = ref()

const emptyCandidateForm = () => ({
  id: null as number | string | null,
  recruitmentId: undefined as number | string | undefined,
  name: '',
  gender: undefined as number | undefined,
  phone: '',
  email: '',
  birthDate: undefined as string | undefined,
  education: undefined as number | undefined,
  school: '',
  major: '',
  experience: '',
  currentCompany: '',
  currentPosition: '',
  expectedSalary: undefined as number | undefined,
  source: undefined as string | undefined,
  resumeUrl: '',
  remark: '',
})

const candidateForm = reactive(emptyCandidateForm())

const candidateRules = {
  recruitmentId: [{ required: true, message: '请选择应聘职位', trigger: 'change' }],
  name: [{ required: true, message: '请输入候选人姓名', trigger: 'blur' }],
  phone: [{ pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }],
}

function handleAddCandidate() {
  Object.assign(candidateForm, emptyCandidateForm())
  candidateOpen.value = true
}

async function handleEditCandidate(record: HrCandidate) {
  Object.assign(candidateForm, emptyCandidateForm())
  candidateForm.id = record.id
  candidateForm.recruitmentId = record.recruitmentId
  candidateForm.name = record.name || ''
  candidateOpen.value = true
  try {
    const detail: any = await hrCandidateApi.getById(record.id)
    if (!detail) return
    candidateForm.recruitmentId = detail.recruitmentId
    candidateForm.name = detail.name || ''
    candidateForm.gender = detail.gender ?? undefined
    candidateForm.phone = detail.phone || ''
    candidateForm.email = detail.email || ''
    candidateForm.birthDate = detail.birthDate || undefined
    candidateForm.education = detail.education ?? undefined
    candidateForm.school = detail.school || ''
    candidateForm.major = detail.major || ''
    candidateForm.experience = detail.experience || ''
    candidateForm.currentCompany = detail.currentCompany || ''
    candidateForm.currentPosition = detail.currentPosition || ''
    candidateForm.expectedSalary = detail.expectedSalary ?? undefined
    candidateForm.source = detail.source || undefined
    candidateForm.resumeUrl = detail.resumeUrl || ''
    candidateForm.remark = detail.remark || ''
  } catch (error: any) {
    message.error(error?.response?.data?.message || '加载候选人详情失败')
  }
}

async function handleSaveCandidate() {
  try {
    await candidateFormRef.value?.validate()
  } catch {
    return
  }
  const payload: Partial<HrCandidate> = {
    recruitmentId: candidateForm.recruitmentId as any,
    name: candidateForm.name.trim(),
    gender: (candidateForm.gender ?? null) as any,
    phone: candidateForm.phone || '',
    email: candidateForm.email || '',
    birthDate: (candidateForm.birthDate || null) as any,
    education: (candidateForm.education ?? null) as any,
    school: candidateForm.school || '',
    major: candidateForm.major || '',
    experience: candidateForm.experience || '',
    currentCompany: candidateForm.currentCompany || '',
    currentPosition: candidateForm.currentPosition || '',
    expectedSalary: (candidateForm.expectedSalary ?? null) as any,
    source: candidateForm.source || undefined,
    resumeUrl: candidateForm.resumeUrl || '',
    remark: candidateForm.remark || '',
  }
  candidateSaving.value = true
  try {
    if (candidateForm.id) {
      await hrCandidateApi.update(candidateForm.id, payload)
      message.success('修改成功')
    } else {
      await hrCandidateApi.create(payload)
      message.success('新增成功')
    }
    candidateOpen.value = false
    fetchList()
    // 新增候选人会自增招聘职位的「已应聘」，切回职位 Tab 时数据需为最新
    if (activeTab.value === 'candidate') loadRecruitmentOptions()
  } catch (error: any) {
    message.error(error?.response?.data?.message || '保存失败')
  } finally {
    candidateSaving.value = false
  }
}

function handleCandidateStatus(record: HrCandidate, status: number) {
  Modal.confirm({
    title: '推进状态',
    content: `确定将「${record.name}」置为「${candidateStatusText(status)}」吗？`,
    onOk: async () => {
      try {
        await hrCandidateApi.updateStatus(record.id, status)
        message.success('状态更新成功')
        fetchList()
      } catch (error: any) {
        message.error(error?.response?.data?.message || '状态更新失败')
      }
    },
  })
}

function handleDeleteCandidate(record: HrCandidate) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除候选人「${record.name}」吗？`,
    okText: '确认删除',
    okType: 'danger',
    onOk: async () => {
      try {
        await hrCandidateApi.remove(record.id)
        message.success('删除成功')
        fetchList()
      } catch (error: any) {
        message.error(error?.response?.data?.message || '删除失败')
      }
    },
  })
}

// ═══ 面试评价 ═══
const interviewOpen = ref(false)
const interviewTarget = ref<HrCandidate | null>(null)
const interviewForm = reactive({ rating: 0, interviewComment: '' })

function handleOpenInterview(record: HrCandidate) {
  interviewTarget.value = record
  interviewForm.rating = record.rating || 0
  interviewForm.interviewComment = record.interviewComment || ''
  interviewOpen.value = true
}

async function handleSubmitInterview() {
  if (!interviewTarget.value) return
  actionSaving.value = true
  try {
    await hrCandidateApi.recordInterview(interviewTarget.value.id, {
      rating: interviewForm.rating || undefined,
      interviewComment: interviewForm.interviewComment || undefined,
    })
    message.success('面试评价记录成功')
    interviewOpen.value = false
    fetchList()
  } catch (error: any) {
    message.error(error?.response?.data?.message || '记录面试评价失败')
  } finally {
    actionSaving.value = false
  }
}

// ═══ 转入职 ═══
const hireOpen = ref(false)
const hireTarget = ref<HrCandidate | null>(null)
const hireForm = reactive({
  deptId: undefined as number | string | undefined,
  positionId: undefined as number | string | undefined,
  hireDate: undefined as string | undefined,
  employeeType: 1 as number | undefined,
})

function handleOpenHire(record: HrCandidate) {
  hireTarget.value = record
  hireForm.deptId = undefined
  hireForm.positionId = undefined
  hireForm.hireDate = dayjs().format('YYYY-MM-DD')
  hireForm.employeeType = 1
  hireOpen.value = true
}

async function handleSubmitHire() {
  if (!hireTarget.value) return
  actionSaving.value = true
  try {
    await hrCandidateApi.hire(hireTarget.value.id, {
      deptId: (hireForm.deptId ?? undefined) as any,
      positionId: (hireForm.positionId ?? undefined) as any,
      hireDate: hireForm.hireDate || undefined,
      employeeType: hireForm.employeeType ?? 1,
    } as any)
    message.success('转入职成功，已创建员工档案')
    hireOpen.value = false
    fetchList()
  } catch (error: any) {
    message.error(error?.response?.data?.message || '转入职失败')
  } finally {
    actionSaving.value = false
  }
}

// ═══ 打印(F8) / 导出：共用「所见即所打」列口径（仅招聘职位 Tab） ═══
const DATE_KEYS = new Set(['publishDate', 'expireDate'])
const DATETIME_KEYS = new Set(['interviewTime', 'createTime', 'updateTime'])
const MONEY_KEYS = new Set(['expectedSalary'])

/** 打印/导出列：剔除系统列，仅取默认可见列（即「所见即所打」） */
const printableColumns = computed<DetailColumnConfig[]>(() =>
  activeColumns.value.filter(c =>
    c.type !== 'rowNo' && c.type !== 'action' && c.key !== '__filler__' && !c.defaultHidden
  )
)

function cellText(col: DetailColumnConfig, record: any): string {
  const value = record[col.key]
  switch (col.key) {
    case 'channel':
      return RECRUITMENT_CHANNEL_MAP[value] || value || ''
    case 'urgency':
      return URGENCY_MAP[value] || ''
    case 'status':
      return activeTab.value === 'recruitment' ? recruitmentStatusText(value) : candidateStatusText(value)
    case 'requiredEducation':
      return EDUCATION_MAP[value] || ''
    case 'education':
      return EDUCATION_MAP[value] || ''
    case 'gender':
      return GENDER_MAP[value] || ''
    case 'source':
      return RECRUITMENT_CHANNEL_MAP[value] || value || ''
    case 'salaryRange':
      return formatSalaryRange(record.salaryMin, record.salaryMax)
    case 'publisherName':
      return record.publisherName || record.publisherId || ''
    case 'rating':
      return value ? `${value} 分` : ''
    default:
      break
  }
  if (MONEY_KEYS.has(col.key)) return formatMoney(value)
  if (DATETIME_KEYS.has(col.key)) return formatDateTime(value)
  if (DATE_KEYS.has(col.key)) return formatDate(value)
  return value === null || value === undefined || value === '' ? '' : String(value)
}

function tabLabel(): string {
  return TABS.find(t => t.key === activeTab.value)?.label || '招聘管理'
}

function buildFilterSummary(): string {
  if (activeTab.value === 'recruitment') {
    const parts: string[] = []
    if (recruitmentQuery.positionName) parts.push(`岗位名称：${recruitmentQuery.positionName}`)
    if (recruitmentQuery.channel) parts.push(`招聘渠道：${RECRUITMENT_CHANNEL_MAP[recruitmentQuery.channel] || recruitmentQuery.channel}`)
    if (recruitmentQuery.status !== undefined && recruitmentQuery.status !== null) parts.push(`状态：${recruitmentStatusText(recruitmentQuery.status)}`)
    if (recruitmentQuery.publishDateRange?.length === 2) {
      parts.push(`发布日期：${recruitmentQuery.publishDateRange[0]} ~ ${recruitmentQuery.publishDateRange[1]}`)
    }
    return parts.join('　')
  }
  const parts: string[] = []
  if (candidateQuery.name) parts.push(`候选人：${candidateQuery.name}`)
  const rec = recruitmentOptions.value.find(r => r.value === candidateQuery.recruitmentId)
  if (rec) parts.push(`应聘职位：${rec.label}`)
  if (candidateQuery.status !== undefined && candidateQuery.status !== null) parts.push(`状态：${candidateStatusText(candidateQuery.status)}`)
  return parts.join('　')
}

/** 可打印行（去掉树形占位行）——标题里的记录数与表格行同源 */
function printableRows(): any[] {
  return tableData.value.filter((r: any) => !r.__ghost)
}

// ═══ 打印（结果集打印）：招聘职位台账 ═══
// 原先是自己拼 HTML + 浏览器打印，现在交给 PrintDialog：列与行由页面给，模板负责版式。
// 列是 computed（随 Tab 与列配置变），静态生成器写不进模板 → 明确按数据列打；
// 单元格文本仍走 cellText（与导出同一口径），逐列还原原来的中文/日期/金额格式。
const { printDialogRef, printData, handlePrint } = useListPrint({
  pageCode: 'hr-recruitment-list',
  useDataColumns: true,
  // 原打印抬头的筛选摘要/记录数元信息行并入标题；打印时间由引擎按本次打印时间给
  title: () => `招聘职位台账（${buildFilterSummary()}，记录数：${printableRows().length}）`,
  columns: () => [
    { key: '__seq', title: '#', align: 'center' },
    ...printableColumns.value.map(c => ({ key: c.key, title: c.title, align: c.align })),
  ],
  rows: () => printableRows().map((r: any, i: number) => {
    const out: Record<string, any> = { __seq: i + 1 }
    printableColumns.value.forEach(c => { out[c.key] = cellText(c, r) })
    return out
  }),
  emptyTip: '没有可打印的数据',
})

function handleF8Key(e: KeyboardEvent) {
  if (activeTab.value !== 'recruitment') return
  if ((e.key === 'F8' || e.code === 'F8') && !e.ctrlKey && !e.altKey && !e.metaKey) {
    e.preventDefault()
    handlePrint()
  }
}

/** 导出（CSV，按当前筛选全量）：职位走 /list 全量端点，候选人走分页端点放大页长 */
async function handleExport() {
  exporting.value = true
  try {
    const cols = printableColumns.value
    let rows: any[] = []
    if (activeTab.value === 'recruitment') {
      const params = buildRecruitmentParams()
      delete params.pageNum
      delete params.pageSize
      const res: any = await hrRecruitmentApi.list(params)
      rows = Array.isArray(res) ? res : res?.records || []
    } else {
      const res: any = await hrCandidateApi.page({ ...buildCandidateParams(), pageNum: 1, pageSize: 10000 })
      rows = res?.records || []
    }
    if (!rows.length) {
      message.warning('没有可导出的数据')
      return
    }
    const header = ['序号', ...cols.map(c => c.title)]
    const lines = rows.map((r, i) => [i + 1, ...cols.map(c => cellText(c, r))]
      .map(cell => `"${String(cell ?? '').replace(/"/g, '""')}"`).join(','))
    const csv = `\uFEFF${header.join(',')}\n${lines.join('\n')}`
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' })
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `${tabLabel()}_${dayjs().format('YYYYMMDD_HHmmss')}.csv`
    a.click()
    window.URL.revokeObjectURL(url)
    message.success(`导出成功，共 ${rows.length} 条`)
  } catch (error: any) {
    message.error(error?.response?.data?.message || '导出失败')
  } finally {
    exporting.value = false
  }
}

// ═══ 页面配置（逐 Tab 独立 storage-key 与默认项） ═══
interface QueryFieldSetting { key: string; label: string; visible: boolean }
interface FunctionButtonSetting { key: string; label: string; enabled: boolean }

const DEFAULT_QUERY_FIELDS_RECRUITMENT: QueryFieldSetting[] = [
  { key: 'positionName', label: '岗位名称', visible: true },
  { key: 'channel', label: '招聘渠道', visible: true },
  { key: 'status', label: '状态', visible: true },
  { key: 'publishDate', label: '发布日期区间', visible: true },
]

const DEFAULT_FUNCTION_BUTTONS_RECRUITMENT: FunctionButtonSetting[] = [
  { key: 'add', label: '新增招聘', enabled: true },
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'printF8', label: '打印(F8)', enabled: true },
  { key: 'export', label: '导出', enabled: true },
]

const DEFAULT_QUERY_FIELDS_CANDIDATE: QueryFieldSetting[] = [
  { key: 'name', label: '候选人姓名', visible: true },
  { key: 'recruitmentId', label: '应聘职位', visible: true },
  { key: 'candidateStatus', label: '状态', visible: true },
]

const DEFAULT_FUNCTION_BUTTONS_CANDIDATE: FunctionButtonSetting[] = [
  { key: 'add', label: '新增候选人', enabled: true },
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'export', label: '导出', enabled: true },
]

const queryFields = ref<QueryFieldSetting[]>([])
const functionButtons = ref<FunctionButtonSetting[]>([])
const showPageConfig = ref(false)

const activeDefaultQueryFields = computed(() =>
  activeTab.value === 'recruitment' ? DEFAULT_QUERY_FIELDS_RECRUITMENT : DEFAULT_QUERY_FIELDS_CANDIDATE
)
const activeDefaultFunctionButtons = computed(() =>
  activeTab.value === 'recruitment' ? DEFAULT_FUNCTION_BUTTONS_RECRUITMENT : DEFAULT_FUNCTION_BUTTONS_CANDIDATE
)

function isQueryVisible(key: string): boolean {
  const hit = queryFields.value.find(f => f.key === key)
  return hit ? hit.visible : false
}

function isButtonEnabled(key: string): boolean {
  const hit = functionButtons.value.find(b => b.key === key)
  return hit ? hit.enabled : false
}

/** 按已保存配置的顺序还原，未记录的项追加到默认顺序末尾（保证拖拽排序可持久） */
function mergeSavedOrder<T extends { key: string }>(
  defaults: T[],
  saved: T[] | undefined,
  merge: (def: T, item: T) => T
): T[] {
  if (!Array.isArray(saved) || saved.length === 0) return defaults.map(d => ({ ...d }))
  const result: T[] = []
  saved.forEach((s) => {
    const def = defaults.find(d => d.key === s.key)
    if (def) result.push(merge(def, s))
  })
  defaults.forEach((d) => {
    if (!saved.some(s => s.key === d.key)) result.push({ ...d })
  })
  return result
}

/** 载入当前 Tab 的页面配置（localStorage 优先，无则用默认）；切 Tab 必须重载 */
function loadPageConfig() {
  const qDefaults = activeDefaultQueryFields.value
  const bDefaults = activeDefaultFunctionButtons.value
  try {
    const raw = localStorage.getItem(pageConfigStorageKey.value)
    if (raw) {
      const parsed = JSON.parse(raw)
      queryFields.value = mergeSavedOrder(qDefaults, parsed?.queryFields, (df, saved) => ({ ...df, ...saved }))
      functionButtons.value = mergeSavedOrder(bDefaults, parsed?.functionButtons, (bf, saved) => ({ ...bf, ...saved }))
      return
    }
  } catch (error) {
    console.warn('[招聘管理] 页面配置读取失败', error)
  }
  queryFields.value = qDefaults.map(f => ({ ...f }))
  functionButtons.value = bDefaults.map(b => ({ ...b }))
}

function handlePageConfigChange(config: { queryFields: QueryFieldSetting[]; functionButtons: FunctionButtonSetting[] }) {
  if (config?.queryFields) queryFields.value = config.queryFields.map(f => ({ ...f }))
  if (config?.functionButtons) functionButtons.value = config.functionButtons.map(b => ({ ...b }))
}

function handleError(error: Error) {
  console.error('[招聘管理] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

// ═══ 初始化 ═══
onMounted(() => {
  loadPageConfig()
  loadDeptTree()
  loadPositions()
  loadRecruitmentOptions()
  fetchList()
  window.addEventListener('keydown', handleF8Key)
})
onBeforeUnmount(() => window.removeEventListener('keydown', handleF8Key))
</script>

<style scoped>
.search-area { padding: 8px 16px; background: #fff; border-bottom: 1px solid #e8e8e8; flex-shrink: 0; }
.search-row { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.search-item { display: flex; align-items: center; gap: 6px; }
.search-label { font-size: 13px; color: #666; white-space: nowrap; }
/* 必须是 flex 纵向容器：BillDetailTable 根元素为 flex:1，父级非 flex 时表格高度会塌陷为 0 */
.table-area { flex: 1; min-height: 0; overflow: hidden; display: flex; flex-direction: column; }

.dialog-alert { margin-bottom: 12px; }
.form-hint { margin-left: 8px; color: #999; font-size: 12px; }

.btn-add { background: #ff6b35 !important; border-color: #ff6b35 !important; }
.btn-add:hover { background: #e55a2b !important; border-color: #e55a2b !important; }

:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) { height: 28px; line-height: 28px; }
</style>
