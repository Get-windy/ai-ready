<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        会员设置（营销 → 会员中心 → 会员设置，菜单 80302）
        对标 ql361「营销 → 会员中心 → 会员设置」：**参数设置单页**（无查询区、无数据表格、无列配置）
        对标文档：docs/Yh-Spec/手动整理对标开发文档/营销模块/会员设置开发文档.md
        结构：三分区卡片（会员设置 / 会员积分 / 积分抵现）+ 页底唯一「保存」
        金标准：ErrorBoundary > PageContainer(full-height) > 分组卡片表单（路线 A′）
        API：memberConfigApi（GET/PUT /erp/marketing/member-config，单行配置，无行时服务端按对标默认值建行）
        ⚠️ 本系统建模：「详细设置」（商品级积分系数）弹窗对标明细未实测，按「商品 × 积分系数」最小结构落库
      -->
      <a-spin :spinning="loading">
        <div
          class="member-config-page"
          data-testid="member-config-form"
        >
          <a-form
            :label-col="{ span: 6 }"
            :wrapper-col="{ span: 16 }"
            size="small"
          >
            <!-- ══════ 分区一：会员设置 ══════ -->
            <a-card
              title="会员设置"
              :bordered="false"
              class="group-card"
            >
              <a-row :gutter="24">
                <a-col :span="12">
                  <a-form-item>
                    <template #label>
                      <span>客户 | 会员管理</span>
                      <a-tooltip title="开启后会员（积分卡）档案依托客户（ERP 往来单位）管理">
                        <QuestionCircleOutlined class="field-help" />
                      </a-tooltip>
                    </template>
                    <a-switch
                      v-model:checked="switchModel.memberEnabled"
                      checked-children="开"
                      un-checked-children="关"
                    />
                  </a-form-item>
                </a-col>
                <a-col :span="12">
                  <a-form-item>
                    <template #label>
                      <span>会员自动升级</span>
                      <a-tooltip title="按会员级别规则自动升级（分级规则由会员级别维护）">
                        <QuestionCircleOutlined class="field-help" />
                      </a-tooltip>
                    </template>
                    <a-switch
                      v-model:checked="switchModel.autoUpgradeEnabled"
                      checked-children="开"
                      un-checked-children="关"
                    />
                  </a-form-item>
                </a-col>
              </a-row>
            </a-card>

            <!-- ══════ 分区二：会员积分 ══════ -->
            <a-card
              title="会员积分"
              :bordered="false"
              class="group-card"
            >
              <!-- 积分奖励 -->
              <div class="sub-block">
                <div class="sub-block__head">
                  <span class="sub-block__title">积分奖励</span>
                  <a-switch
                    v-model:checked="switchModel.pointsRewardEnabled"
                    checked-children="开"
                    un-checked-children="关"
                  />
                </div>
                <a-row :gutter="24">
                  <a-col :span="12">
                    <a-form-item required>
                      <template #label>
                        <span>注册初始积分</span>
                        <a-tooltip title="新会员开户即赠送的积分">
                          <QuestionCircleOutlined class="field-help" />
                        </a-tooltip>
                      </template>
                      <a-input-number
                        v-model:value="form.registerPoints"
                        :min="0"
                        :precision="0"
                        :disabled="!switchModel.pointsRewardEnabled"
                        style="width: 200px"
                      />
                    </a-form-item>
                  </a-col>
                  <a-col :span="12">
                    <a-form-item required>
                      <template #label>
                        <span>会员生日</span>
                        <a-tooltip title="生日当天消费按 N 倍累计积分">
                          <QuestionCircleOutlined class="field-help" />
                        </a-tooltip>
                      </template>
                      <div class="inline-suffix">
                        <a-input-number
                          v-model:value="form.birthdayMultiple"
                          :min="0"
                          :precision="0"
                          :disabled="!switchModel.pointsRewardEnabled"
                          style="width: 120px"
                        />
                        <span class="suffix-text">倍积分</span>
                      </div>
                    </a-form-item>
                  </a-col>
                </a-row>
              </div>

              <!-- 消费积分 -->
              <div class="sub-block">
                <div class="sub-block__head">
                  <span class="sub-block__title">消费积分</span>
                  <a-switch
                    v-model:checked="switchModel.consumePointsEnabled"
                    checked-children="开"
                    un-checked-children="关"
                  />
                </div>
                <a-form-item>
                  <a-radio-group
                    v-model:value="form.consumePointsMode"
                    :disabled="!switchModel.consumePointsEnabled"
                  >
                    <a-radio value="BY_AMOUNT">
                      按销售金额积分
                    </a-radio>
                  </a-radio-group>
                  <a-tooltip title="全局统一比例：N 元销售金额累计 1 分">
                    <QuestionCircleOutlined class="field-help" />
                  </a-tooltip>
                  <a-input-number
                    v-model:value="form.amountPerPoint"
                    :min="0.01"
                    :disabled="!switchModel.consumePointsEnabled || form.consumePointsMode !== 'BY_AMOUNT'"
                    style="width: 120px; margin: 0 6px"
                  />
                  <span class="suffix-text">元=1分</span>

                  <a-radio-group
                    v-model:value="form.consumePointsMode"
                    :disabled="!switchModel.consumePointsEnabled"
                    style="margin-left: 32px"
                  >
                    <a-radio value="BY_PRODUCT">
                      按不同商品累计积分
                    </a-radio>
                  </a-radio-group>
                  <a-tooltip title="按商品级积分系数累计，系数由「详细设置」维护">
                    <QuestionCircleOutlined class="field-help" />
                  </a-tooltip>
                  <a-button
                    size="small"
                    style="margin-left: 8px"
                    @click="openDetailSetting"
                  >
                    详细设置
                  </a-button>

                  <a-checkbox
                    v-model:checked="switchModel.pointsByDiscount"
                    :disabled="!switchModel.consumePointsEnabled"
                    style="margin-left: 32px"
                  >
                    按折扣积分
                  </a-checkbox>
                  <a-tooltip title="按折后金额计积分">
                    <QuestionCircleOutlined class="field-help" />
                  </a-tooltip>
                </a-form-item>

                <a-row :gutter="24">
                  <a-col :span="10">
                    <a-form-item label="积分规则">
                      <a-select
                        v-model:value="form.pointsRoundRule"
                        :disabled="!switchModel.consumePointsEnabled"
                        style="width: 160px"
                        :options="ROUND_RULE_OPTIONS"
                      />
                    </a-form-item>
                  </a-col>
                  <a-col :span="14">
                    <a-form-item label="积分应用场景">
                      <a-checkbox
                        v-model:checked="switchModel.applySceneOffline"
                        :disabled="!switchModel.consumePointsEnabled"
                      >
                        线下开单
                      </a-checkbox>
                      <a-checkbox
                        v-model:checked="switchModel.applySceneMall"
                        :disabled="!switchModel.consumePointsEnabled"
                        style="margin-left: 16px"
                      >
                        微商城
                      </a-checkbox>
                    </a-form-item>
                  </a-col>
                </a-row>

                <!-- 积分有效期（业界通行 12 个月 + 到期前 30 天提醒） -->
                <a-row :gutter="24">
                  <a-col :span="10">
                    <a-form-item>
                      <template #label>
                        <span>积分有效期</span>
                        <a-tooltip title="按批次先进先出计算；0 或留空＝永不过期">
                          <QuestionCircleOutlined class="field-help" />
                        </a-tooltip>
                      </template>
                      <div class="inline-suffix">
                        <a-input-number
                          v-model:value="form.pointsValidMonths"
                          :min="0"
                          :precision="0"
                          :disabled="!switchModel.consumePointsEnabled"
                          style="width: 120px"
                        />
                        <span class="suffix-text">个月（0＝永久）</span>
                      </div>
                    </a-form-item>
                  </a-col>
                  <a-col :span="14">
                    <a-form-item>
                      <template #label>
                        <span>到期前提醒</span>
                        <a-tooltip title="到期前 N 天可筛出「即将过期」会员，供营销自动化触达">
                          <QuestionCircleOutlined class="field-help" />
                        </a-tooltip>
                      </template>
                      <div class="inline-suffix">
                        <a-input-number
                          v-model:value="form.pointsExpireRemindDays"
                          :min="0"
                          :precision="0"
                          :disabled="!switchModel.consumePointsEnabled"
                          style="width: 120px"
                        />
                        <span class="suffix-text">天</span>
                        <a-button
                          size="small"
                          style="margin-left: 12px"
                          :loading="expiring"
                          @click="openExpiring"
                        >
                          查看即将过期
                        </a-button>
                        <a-button
                          size="small"
                          :loading="expiringRun"
                          @click="handleRunExpire"
                        >
                          立即执行过期
                        </a-button>
                      </div>
                    </a-form-item>
                  </a-col>
                </a-row>
              </div>

              <!-- 签到积分 -->
              <div class="sub-block">
                <div class="sub-block__head">
                  <span class="sub-block__title">签到积分</span>
                  <a-tooltip title="线上签到玩法：首日得分，连续签到每日递增，达到封顶后持平">
                    <QuestionCircleOutlined class="field-help" />
                  </a-tooltip>
                  <a-switch
                    v-model:checked="switchModel.signinEnabled"
                    checked-children="开"
                    un-checked-children="关"
                  />
                </div>
                <div class="signin-row">
                  <div class="signin-fields">
                    <a-form-item
                      required
                      label="第一天签到积分"
                      :label-col="{ span: 14 }"
                      :wrapper-col="{ span: 10 }"
                    >
                      <a-input-number
                        v-model:value="form.signinFirstPoints"
                        :min="0"
                        :precision="0"
                        :disabled="!switchModel.signinEnabled"
                        style="width: 120px"
                      />
                    </a-form-item>
                    <a-form-item
                      required
                      label="连续签到每天增加"
                      :label-col="{ span: 14 }"
                      :wrapper-col="{ span: 10 }"
                    >
                      <a-input-number
                        v-model:value="form.signinIncrement"
                        :min="0"
                        :precision="0"
                        :disabled="!switchModel.signinEnabled"
                        style="width: 120px"
                      />
                    </a-form-item>
                    <a-form-item
                      required
                      label="连续签到最大获得"
                      :label-col="{ span: 14 }"
                      :wrapper-col="{ span: 10 }"
                    >
                      <a-input-number
                        v-model:value="form.signinMaxPoints"
                        :min="0"
                        :precision="0"
                        :disabled="!switchModel.signinEnabled"
                        style="width: 120px"
                      />
                    </a-form-item>
                  </div>

                  <!-- 月历可视化：直观呈现「连续签到递增到顶持平」（对标实测有该可视化） -->
                  <div class="signin-calendar">
                    <div class="signin-calendar__head">
                      <span
                        v-for="w in WEEK_LABELS"
                        :key="w"
                      >{{ w }}</span>
                    </div>
                    <div class="signin-calendar__grid">
                      <span
                        v-for="(v, i) in signinCalendar"
                        :key="i"
                        class="signin-calendar__cell"
                      >{{ v }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </a-card>

            <!-- ══════ 分区三：积分抵现 ══════ -->
            <a-card
              title="积分抵现"
              :bordered="false"
              class="group-card"
            >
              <div class="sub-block__head">
                <span class="sub-block__title">积分抵现</span>
                <a-switch
                  v-model:checked="switchModel.cashDeductEnabled"
                  checked-children="开"
                  un-checked-children="关"
                />
              </div>
              <a-row :gutter="24">
                <a-col :span="12">
                  <a-form-item required>
                    <template #label>
                      <span>抵现</span>
                      <a-tooltip title="N 积分抵扣 1 元">
                        <QuestionCircleOutlined class="field-help" />
                      </a-tooltip>
                    </template>
                    <div class="inline-suffix">
                      <a-input-number
                        v-model:value="form.pointsPerYuan"
                        :min="0.01"
                        :disabled="!switchModel.cashDeductEnabled"
                        style="width: 160px"
                      />
                      <span class="suffix-text">积分=1元</span>
                    </div>
                  </a-form-item>
                </a-col>
                <a-col :span="12">
                  <a-form-item required>
                    <template #label>
                      <span>单笔订单最高可抵扣的金额</span>
                      <a-tooltip title="占订单金额的百分比，100% 即允许全额抵扣">
                        <QuestionCircleOutlined class="field-help" />
                      </a-tooltip>
                    </template>
                    <div class="inline-suffix">
                      <a-input-number
                        v-model:value="form.maxDeductPercent"
                        :min="0"
                        :max="100"
                        :disabled="!switchModel.cashDeductEnabled"
                        style="width: 160px"
                      />
                      <span class="suffix-text">%</span>
                    </div>
                  </a-form-item>
                </a-col>
              </a-row>
            </a-card>

            <!-- ══════ 分区四：会员等级（本系统新增；对标为三分区） ══════ -->
            <a-card
              title="会员等级"
              :bordered="false"
              class="group-card"
            >
              <template #extra>
                <a-space :size="8">
                  <a-button
                    size="small"
                    :loading="levelEvaluating"
                    @click="handleEvaluateLevels"
                  >
                    等级评估
                  </a-button>
                  <a-button
                    size="small"
                    type="primary"
                    :loading="levelApplying"
                    @click="handleApplyLevels"
                  >
                    执行升降级
                  </a-button>
                  <a-button
                    size="small"
                    class="btn-add"
                    @click="openLevelCreate"
                  >
                    <PlusOutlined /> 新增等级
                  </a-button>
                </a-space>
              </template>
              <a-table
                :columns="LEVEL_COLUMNS"
                :data-source="levelRules"
                :loading="levelLoading"
                :pagination="false"
                row-key="id"
                size="small"
              >
                <template #bodyCell="{ column, record }">
                  <template v-if="column.key === 'discountRate'">
                    {{ record.discountRate == null ? '-' : record.discountRate + '%' }}
                  </template>
                  <template v-else-if="column.key === 'upgradeAmount'">
                    {{ record.upgradeAmount == null ? '不限' : record.upgradeAmount }}
                  </template>
                  <template v-else-if="column.key === 'upgradePoints'">
                    {{ record.upgradePoints == null ? '不限' : record.upgradePoints }}
                  </template>
                  <template v-else-if="column.key === 'keepMonths'">
                    {{ record.keepMonths == null ? '永久' : record.keepMonths + ' 个月' }}
                  </template>
                  <template v-else-if="column.key === 'isDefault'">
                    <a-tag
                      v-if="record.isDefault === 1"
                      color="green"
                    >
                      默认
                    </a-tag>
                  </template>
                  <template v-else-if="column.key === 'action'">
                    <a-button
                      type="link"
                      size="small"
                      @click="openLevelEdit(record)"
                    >
                      修改
                    </a-button>
                  </template>
                </template>
              </a-table>
              <div class="detail-tip">
                升级门槛满足「累计消费额」或「积分」任一即升到该级；命中门槛最高的那一级为目标等级。
                执行升降级受本页「会员自动升级」开关门控。
              </div>
            </a-card>

            <!-- ══════ 页底唯一提交动作 ══════ -->
            <div class="page-footer">
              <a-button
                type="primary"
                class="btn-save"
                :loading="saving"
                @click="handleSave"
              >
                保存
              </a-button>
            </div>
          </a-form>
        </div>
      </a-spin>

      <!-- ══════ 「详细设置」：商品级积分系数（本系统建模） ══════ -->
      <a-modal
        v-model:open="detailOpen"
        title="详细设置 · 商品级积分系数"
        width="720px"
        :footer="null"
      >
        <div class="detail-toolbar">
          <a-input
            v-model:value="detailKeyword"
            placeholder="请输入货号/商品名称"
            size="small"
            style="width: 220px"
            allow-clear
            @press-enter="loadDetailRules"
          />
          <a-button
            type="primary"
            size="small"
            class="btn-add"
            @click="openPickProduct"
          >
            <PlusOutlined /> 新增商品系数
          </a-button>
        </div>
        <a-table
          :columns="detailColumns"
          :data-source="detailRules"
          :loading="detailLoading"
          :pagination="false"
          row-key="id"
          size="small"
          :scroll="{ y: 320 }"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'coefficient'">
              <a-input-number
                v-model:value="record.pointsCoefficient"
                :min="0"
                size="small"
                style="width: 120px"
                @blur="handleUpdateRule(record)"
              />
            </template>
            <template v-else-if="column.key === 'action'">
              <a-button
                type="link"
                size="small"
                danger
                @click="handleDeleteRule(record)"
              >
                删除
              </a-button>
            </template>
          </template>
        </a-table>
        <div class="detail-tip">
          积分系数 = 每 1 元销售金额累计的积分数；未配置系数的商品按「按销售金额积分」的全局比例计算。
        </div>
      </a-modal>

      <!-- ══════ 等级编辑 ══════ -->
      <a-modal
        v-model:open="levelFormOpen"
        :title="levelEditingId ? '修改会员等级' : '新增会员等级'"
        :confirm-loading="levelSaving"
        width="560px"
        @ok="handleLevelSave"
      >
        <a-form
          :label-col="{ span: 7 }"
          :wrapper-col="{ span: 15 }"
          size="small"
          style="margin-top: 12px"
        >
          <a-form-item label="等级名称">
            <a-input
              v-model:value="levelForm.levelName"
              placeholder="如 银卡会员"
            />
          </a-form-item>
          <a-form-item label="会员折扣率">
            <a-input-number
              v-model:value="levelForm.discountRate"
              :min="1"
              :max="100"
              :precision="2"
              style="width: 160px"
              addon-after="%"
            />
          </a-form-item>
          <a-form-item label="升级门槛·消费额">
            <a-input-number
              v-model:value="levelForm.upgradeAmount"
              :min="0"
              :precision="2"
              style="width: 200px"
              placeholder="累计消费额达标即升到此级"
            />
          </a-form-item>
          <a-form-item label="升级门槛·积分">
            <a-input-number
              v-model:value="levelForm.upgradePoints"
              :min="0"
              :precision="0"
              style="width: 200px"
              placeholder="积分达标即升到此级"
            />
          </a-form-item>
          <a-form-item label="保级周期">
            <a-input-number
              v-model:value="levelForm.keepMonths"
              :min="0"
              :precision="0"
              style="width: 160px"
              addon-after="个月"
            />
          </a-form-item>
          <a-form-item label="默认等级">
            <a-switch
              v-model:checked="levelForm.isDefaultBool"
              checked-children="是"
              un-checked-children="否"
            />
          </a-form-item>
          <a-form-item label="排序">
            <a-input-number
              v-model:value="levelForm.sortOrder"
              :min="0"
              :precision="0"
              style="width: 160px"
            />
          </a-form-item>
          <a-form-item label="备注">
            <a-input v-model:value="levelForm.remark" />
          </a-form-item>
        </a-form>
      </a-modal>

      <!-- ══════ 等级评估结果 ══════ -->
      <a-modal
        v-model:open="levelEvalOpen"
        title="会员等级评估结果（只算不改，确认后可执行升降级）"
        width="960px"
        :footer="null"
      >
        <a-alert
          :type="levelEval?.autoUpgradeEnabled ? 'success' : 'warning'"
          show-icon
          :message="levelEval?.autoUpgradeEnabled
            ? '「会员自动升级」已开启，可直接执行升降级'
            : '「会员自动升级」当前为关闭：执行前请先在《会员设置》开启（或执行时勾选强制）'"
        />
        <div class="eval-summary">
          共评估 <b>{{ levelEval?.total ?? 0 }}</b> 人：升级 <b>{{ levelEval?.upgrade ?? 0 }}</b>、
          降级 <b>{{ levelEval?.downgrade ?? 0 }}</b>、首次定级 <b>{{ levelEval?.init ?? 0 }}</b>
        </div>
        <a-table
          :columns="EVAL_COLUMNS"
          :data-source="levelEval?.changes || []"
          :pagination="{ pageSize: 10 }"
          row-key="partnerId"
          size="small"
          :scroll="{ y: 320 }"
        />
      </a-modal>

      <!-- ══════ 即将过期积分（到期提醒数据源） ══════ -->
      <a-modal
        v-model:open="expiringOpen"
        title="即将过期的会员积分"
        width="860px"
        :footer="null"
      >
        <a-table
          :columns="EXPIRING_COLUMNS"
          :data-source="expiringRows"
          :loading="expiring"
          :pagination="{ pageSize: 10 }"
          row-key="id"
          size="small"
          :scroll="{ y: 320 }"
        />
        <div class="detail-tip">
          到期前提醒天数可在本页「积分有效期」处配置；该清单可作为营销自动化「积分即将过期」触发的对象来源。
        </div>
      </a-modal>

      <!-- 商品选择器（复用通用组件） -->
      <ProductSelectModal
        v-model:open="productPickerOpen"
        multiple
        @confirm="handleProductsPicked"
      />
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { QuestionCircleOutlined, PlusOutlined } from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import ProductSelectModal from '@/components/ProductSelectModal/index.vue'
import {
  memberConfigApi, productPointsRuleApi, memberLevelRuleApi, pointsLedgerApi,
  type MemberConfig, type MemberLevelRule, type PointsBatch,
} from '@/api/marketing'

defineOptions({ name: 'MarketingMemberConfig' })

const ROUND_RULE_OPTIONS = [
  { label: '四舍五入', value: 'ROUND' },
  { label: '舍去', value: 'FLOOR' },
  { label: '进位', value: 'CEIL' }
]

const WEEK_LABELS = ['日', '一', '二', '三', '四', '五', '六']

// ── 状态 ──
const loading = ref(false)
const saving = ref(false)

/** 布尔开关（库内 INTEGER 0/1，页内与 a-switch 的 boolean 互转） */
const switchModel = reactive({
  memberEnabled: true,
  autoUpgradeEnabled: false,
  pointsRewardEnabled: true,
  consumePointsEnabled: true,
  pointsByDiscount: false,
  applySceneOffline: false,
  applySceneMall: false,
  signinEnabled: true,
  cashDeductEnabled: true
})

/** 数值/枚举参数（库内原类型） */
const form = reactive({
  consumePointsMode: 'BY_AMOUNT' as string,
  amountPerPoint: 100 as number | null,
  pointsRoundRule: 'ROUND' as string,
  registerPoints: 0 as number | null,
  birthdayMultiple: 1 as number | null,
  signinFirstPoints: 3 as number | null,
  signinIncrement: 1 as number | null,
  signinMaxPoints: 8 as number | null,
  pointsPerYuan: 100 as number | null,
  maxDeductPercent: 100 as number | null,
  // 积分有效期（P1）
  pointsValidMonths: 12 as number | null,
  pointsExpireRemindDays: 30 as number | null
})

/** 签到月历：连续签到递增到顶持平（对标实测可视化） */
const signinCalendar = computed(() => {
  const first = Number(form.signinFirstPoints ?? 0)
  const inc = Number(form.signinIncrement ?? 0)
  const max = Number(form.signinMaxPoints ?? 0)
  const out: number[] = []
  for (let i = 0; i < 56; i++) {
    out.push(Math.min(first + i * inc, max))
  }
  return out
})

// ── 加载 / 保存 ──
function toBool(v: any): boolean {
  return v === 1 || v === true
}
function toBit(v: any): number {
  return v ? 1 : 0
}

function applyConfig(data: MemberConfig | null | undefined) {
  if (!data) return
  switchModel.memberEnabled = toBool(data.memberEnabled)
  switchModel.autoUpgradeEnabled = toBool(data.autoUpgradeEnabled)
  switchModel.pointsRewardEnabled = toBool(data.pointsRewardEnabled)
  switchModel.consumePointsEnabled = toBool(data.consumePointsEnabled)
  switchModel.pointsByDiscount = toBool(data.pointsByDiscount)
  switchModel.applySceneOffline = toBool(data.applySceneOffline)
  switchModel.applySceneMall = toBool(data.applySceneMall)
  switchModel.signinEnabled = toBool(data.signinEnabled)
  switchModel.cashDeductEnabled = toBool(data.cashDeductEnabled)

  form.consumePointsMode = data.consumePointsMode || 'BY_AMOUNT'
  form.amountPerPoint = data.amountPerPoint ?? 100
  form.pointsRoundRule = data.pointsRoundRule || 'ROUND'
  form.registerPoints = data.registerPoints ?? 0
  form.birthdayMultiple = data.birthdayMultiple ?? 1
  form.signinFirstPoints = data.signinFirstPoints ?? 3
  form.signinIncrement = data.signinIncrement ?? 1
  form.signinMaxPoints = data.signinMaxPoints ?? 8
  form.pointsPerYuan = data.pointsPerYuan ?? 100
  form.maxDeductPercent = data.maxDeductPercent ?? 100
  form.pointsValidMonths = data.pointsValidMonths ?? 12
  form.pointsExpireRemindDays = data.pointsExpireRemindDays ?? 30
}

async function loadConfig() {
  loading.value = true
  try {
    const data = await memberConfigApi.get()
    applyConfig(data)
  } catch (error: any) {
    console.error('[会员设置] 加载配置失败', error)
    message.error(error?.response?.data?.message || '加载会员设置失败')
  } finally {
    loading.value = false
  }
}

/** 前端校验：只拦自相矛盾的输入（与后端 validate 同口径） */
function validateForm(): string | null {
  if (switchModel.signinEnabled) {
    if (form.signinFirstPoints == null || form.signinIncrement == null || form.signinMaxPoints == null) {
      return '启用签到积分时，签到三项积分参数均为必填'
    }
    if (form.signinMaxPoints < form.signinFirstPoints) {
      return '连续签到最大获得不得小于第一天签到积分'
    }
  }
  if (switchModel.consumePointsEnabled && form.consumePointsMode === 'BY_AMOUNT') {
    if (!form.amountPerPoint || form.amountPerPoint <= 0) {
      return '按销售金额积分（N 元=1 分）须大于 0'
    }
  }
  if (switchModel.cashDeductEnabled) {
    if (!form.pointsPerYuan || form.pointsPerYuan <= 0) {
      return '抵现比例（N 积分=1 元）须大于 0'
    }
    if (form.maxDeductPercent == null || form.maxDeductPercent < 0 || form.maxDeductPercent > 100) {
      return '单笔订单最高可抵扣的金额百分比须在 0~100 之间'
    }
  }
  return null
}

async function handleSave() {
  const err = validateForm()
  if (err) {
    message.warning(err)
    return
  }
  saving.value = true
  try {
    const payload: Partial<MemberConfig> = {
      memberEnabled: toBit(switchModel.memberEnabled),
      autoUpgradeEnabled: toBit(switchModel.autoUpgradeEnabled),
      pointsRewardEnabled: toBit(switchModel.pointsRewardEnabled),
      registerPoints: form.registerPoints ?? 0,
      birthdayMultiple: form.birthdayMultiple ?? 0,
      consumePointsEnabled: toBit(switchModel.consumePointsEnabled),
      consumePointsMode: form.consumePointsMode,
      amountPerPoint: form.amountPerPoint ?? 0,
      pointsByDiscount: toBit(switchModel.pointsByDiscount),
      pointsRoundRule: form.pointsRoundRule,
      applySceneOffline: toBit(switchModel.applySceneOffline),
      applySceneMall: toBit(switchModel.applySceneMall),
      signinEnabled: toBit(switchModel.signinEnabled),
      signinFirstPoints: form.signinFirstPoints ?? 0,
      signinIncrement: form.signinIncrement ?? 0,
      signinMaxPoints: form.signinMaxPoints ?? 0,
      pointsValidMonths: form.pointsValidMonths ?? 0,
      pointsExpireRemindDays: form.pointsExpireRemindDays ?? 30,
      cashDeductEnabled: toBit(switchModel.cashDeductEnabled),
      pointsPerYuan: form.pointsPerYuan ?? 0,
      maxDeductPercent: form.maxDeductPercent ?? 0
    }
    const saved = await memberConfigApi.save(payload)
    applyConfig(saved)
    message.success('会员设置已保存')
  } catch (error: any) {
    message.error(error?.response?.data?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

// ── 详细设置：商品级积分系数（本系统建模） ──
const detailOpen = ref(false)
const detailLoading = ref(false)
const detailKeyword = ref('')
const detailRules = ref<any[]>([])
const productPickerOpen = ref(false)

const detailColumns = [
  { title: '货号', dataIndex: 'productCode', key: 'productCode', width: 140 },
  { title: '商品名称', dataIndex: 'productName', key: 'productName' },
  { title: '积分系数', dataIndex: 'pointsCoefficient', key: 'coefficient', width: 150 },
  { title: '操作', key: 'action', width: 80 }
]

function openDetailSetting() {
  detailOpen.value = true
  loadDetailRules()
}

async function loadDetailRules() {
  detailLoading.value = true
  try {
    const res: any = await productPointsRuleApi.list(detailKeyword.value || undefined)
    detailRules.value = Array.isArray(res) ? res : (res?.records || [])
  } catch (error: any) {
    console.error('[会员设置] 加载商品级积分系数失败', error)
    message.error(error?.response?.data?.message || '加载商品级积分系数失败')
    detailRules.value = []
  } finally {
    detailLoading.value = false
  }
}

function openPickProduct() {
  productPickerOpen.value = true
}

async function handleProductsPicked(products: any[]) {
  if (!products?.length) return
  try {
    for (const p of products) {
      await productPointsRuleApi.create({
        productId: p.id,
        productCode: p.productCode || p.code,
        productName: p.productName || p.name,
        pointsCoefficient: 1,
        status: 1
      })
    }
    message.success(`已新增 ${products.length} 条商品系数`)
    loadDetailRules()
  } catch (error: any) {
    message.error(error?.response?.data?.message || '新增商品系数失败')
  }
}

async function handleUpdateRule(record: any) {
  try {
    await productPointsRuleApi.update(record.id, {
      productId: record.productId,
      productCode: record.productCode,
      productName: record.productName,
      pointsCoefficient: record.pointsCoefficient,
      status: record.status
    })
    message.success('已更新')
  } catch (error: any) {
    message.error(error?.response?.data?.message || '更新失败')
  }
}

async function handleDeleteRule(record: any) {
  try {
    await productPointsRuleApi.remove(record.id)
    message.success('已删除')
    loadDetailRules()
  } catch (error: any) {
    message.error(error?.response?.data?.message || '删除失败')
  }
}

// ══════ 会员等级（本系统新增分区；对标为三分区） ══════
const levelLoading = ref(false)
const levelRules = ref<MemberLevelRule[]>([])
const levelEvalOpen = ref(false)
const levelEvaluating = ref(false)
const levelApplying = ref(false)
const levelEval = ref<any>(null)
const levelFormOpen = ref(false)
const levelSaving = ref(false)
const levelEditingId = ref<any>(null)

const LEVEL_COLUMNS = [
  { title: '等级名称', dataIndex: 'levelName', key: 'levelName', width: 160 },
  { title: '会员折扣率', dataIndex: 'discountRate', key: 'discountRate', width: 110 },
  { title: '升级门槛·消费额', dataIndex: 'upgradeAmount', key: 'upgradeAmount', width: 150 },
  { title: '升级门槛·积分', dataIndex: 'upgradePoints', key: 'upgradePoints', width: 140 },
  { title: '保级周期', dataIndex: 'keepMonths', key: 'keepMonths', width: 110 },
  { title: '默认等级', dataIndex: 'isDefault', key: 'isDefault', width: 100 },
  { title: '排序', dataIndex: 'sortOrder', key: 'sortOrder', width: 80 },
  { title: '操作', key: 'action', width: 90 },
]

const EVAL_COLUMNS = [
  { title: '客户编号', dataIndex: 'partyCode', key: 'partyCode', width: 130 },
  { title: '客户/会员', dataIndex: 'partyName', key: 'partyName' },
  { title: '当前等级', dataIndex: 'currentLevel', key: 'currentLevel', width: 120 },
  { title: '目标等级', dataIndex: 'targetLevel', key: 'targetLevel', width: 120 },
  { title: '动作', dataIndex: 'action', key: 'action', width: 100 },
  { title: '累计消费', dataIndex: 'totalConsume', key: 'totalConsume', width: 110 },
  { title: '积分', dataIndex: 'points', key: 'points', width: 90 },
  { title: '依据', dataIndex: 'reason', key: 'reason', width: 240 },
]

const EXPIRING_COLUMNS = [
  { title: '会员卡号', dataIndex: 'memberCardNo', key: 'memberCardNo', width: 160 },
  { title: '获得积分', dataIndex: 'earnedPoints', key: 'earnedPoints', width: 110 },
  { title: '剩余积分', dataIndex: 'remainingPoints', key: 'remainingPoints', width: 110 },
  { title: '获得时间', dataIndex: 'earnedTime', key: 'earnedTime', width: 170 },
  { title: '到期时间', dataIndex: 'expireTime', key: 'expireTime', width: 170 },
]

const levelForm = reactive({
  levelName: '',
  discountRate: 100 as number | undefined,
  upgradeAmount: undefined as number | undefined,
  upgradePoints: undefined as number | undefined,
  keepMonths: 12 as number | undefined,
  isDefaultBool: false,
  sortOrder: 0 as number | undefined,
  remark: '',
})

async function loadLevelRules() {
  levelLoading.value = true
  try {
    const res: any = await memberLevelRuleApi.rules()
    levelRules.value = Array.isArray(res) ? res : (res?.records || [])
  } catch (error: any) {
    console.error('[会员设置] 加载会员等级失败', error)
    levelRules.value = []
  } finally {
    levelLoading.value = false
  }
}

function openLevelCreate() {
  levelEditingId.value = null
  Object.assign(levelForm, {
    levelName: '', discountRate: 100, upgradeAmount: undefined, upgradePoints: undefined,
    keepMonths: 12, isDefaultBool: false, sortOrder: (levelRules.value.length + 1), remark: '',
  })
  levelFormOpen.value = true
}

function openLevelEdit(record: any) {
  levelEditingId.value = record.id
  Object.assign(levelForm, {
    levelName: record.levelName || '',
    discountRate: record.discountRate == null ? 100 : Number(record.discountRate),
    upgradeAmount: record.upgradeAmount == null ? undefined : Number(record.upgradeAmount),
    upgradePoints: record.upgradePoints == null ? undefined : Number(record.upgradePoints),
    keepMonths: record.keepMonths == null ? undefined : Number(record.keepMonths),
    isDefaultBool: record.isDefault === 1,
    sortOrder: record.sortOrder ?? 0,
    remark: record.remark || '',
  })
  levelFormOpen.value = true
}

async function handleLevelSave() {
  if (!levelForm.levelName.trim()) {
    message.warning('请输入等级名称')
    return
  }
  if (levelForm.upgradeAmount == null && levelForm.upgradePoints == null && !levelForm.isDefaultBool) {
    message.warning('请至少填写一个升级门槛（消费额或积分），或把它设为默认等级')
    return
  }
  levelSaving.value = true
  try {
    const payload: any = {
      levelName: levelForm.levelName,
      discountRate: levelForm.discountRate ?? 100,
      upgradeAmount: levelForm.upgradeAmount ?? null,
      upgradePoints: levelForm.upgradePoints ?? null,
      keepMonths: levelForm.keepMonths ?? null,
      isDefault: levelForm.isDefaultBool ? 1 : 0,
      sortOrder: levelForm.sortOrder ?? 0,
      status: 1,
      remark: levelForm.remark,
    }
    if (levelEditingId.value) {
      await memberLevelRuleApi.update(levelEditingId.value, payload)
      message.success('等级已更新')
    } else {
      await memberLevelRuleApi.create(payload)
      message.success('等级已新增')
    }
    levelFormOpen.value = false
    loadLevelRules()
  } catch (error: any) {
    message.error(error?.response?.data?.message || '保存失败')
  } finally {
    levelSaving.value = false
  }
}

/** 等级评估（dry-run）：只算不改，弹窗展示明细 */
async function handleEvaluateLevels() {
  levelEvaluating.value = true
  try {
    levelEval.value = await memberLevelRuleApi.evaluate(500)
    levelEvalOpen.value = true
  } catch (error: any) {
    message.error(error?.response?.data?.message || '评估失败')
  } finally {
    levelEvaluating.value = false
  }
}

/** 执行升降级（受「会员自动升级」开关门控） */
async function handleApplyLevels() {
  const enabled = toBool(switchModel.autoUpgradeEnabled)
  Modal.confirm({
    title: '执行会员等级升降级',
    content: enabled
      ? '将按当前等级门槛重算全部会员等级并写入。是否继续？'
      : '「会员自动升级」当前为关闭状态。继续将以 force 方式覆盖本次执行，请在执行后确认是否符合预期。是否继续？',
    okText: '执行',
    onOk: async () => {
      levelApplying.value = true
      try {
        const res = await memberLevelRuleApi.apply(500, !enabled)
        message.success(`已执行：评估 ${res?.evaluated ?? 0} 人，写入 ${res?.applied ?? 0} 人`)
        levelEvalOpen.value = false
      } catch (error: any) {
        message.error(error?.response?.data?.message || '执行失败')
      } finally {
        levelApplying.value = false
      }
    },
  })
}

// ══════ 积分有效期：即将过期清单 + 立即执行过期 ══════
const expiring = ref(false)
const expiringOpen = ref(false)
const expiringRows = ref<PointsBatch[]>([])
const expiringRun = ref(false)

async function openExpiring() {
  expiringOpen.value = true
  expiring.value = true
  try {
    const res: any = await pointsLedgerApi.expiringSoon(form.pointsExpireRemindDays ?? 30)
    expiringRows.value = Array.isArray(res) ? res : (res?.records || [])
  } catch (error: any) {
    message.error(error?.response?.data?.message || '加载即将过期积分失败')
    expiringRows.value = []
  } finally {
    expiring.value = false
  }
}

function handleRunExpire() {
  Modal.confirm({
    title: '执行积分过期',
    content: '将把「已到有效期」的积分批次剩余清零，并写入积分过期流水（不可撤销）。是否继续？',
    okText: '执行过期',
    okType: 'danger',
    onOk: async () => {
      expiringRun.value = true
      try {
        const res = await pointsLedgerApi.expire()
        message.success(`过期处理完成：${res?.memberCount ?? 0} 个会员，共 ${res?.expiredPoints ?? 0} 分`)
        if (expiringOpen.value) openExpiring()
      } catch (error: any) {
        message.error(error?.response?.data?.message || '执行过期失败')
      } finally {
        expiringRun.value = false
      }
    },
  })
}

function handleError(error: Error) {
  console.error('[会员设置] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

onMounted(() => {
  loadConfig()
  loadLevelRules()
})
</script>

<style scoped>
.member-config-page {
  height: 100%;
  overflow-y: auto;
  padding: 12px 16px 72px;
  background: #f0f2f5;
}
.group-card {
  margin-bottom: 12px;
  border: 1px solid #e8e8e8;
}
.sub-block {
  padding: 4px 0 8px;
  border-bottom: 1px dashed #f0f0f0;
}
.sub-block:last-child {
  border-bottom: none;
}
.sub-block__head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}
.sub-block__title {
  font-weight: 600;
  font-size: 13px;
  color: #303133;
}
.field-help {
  margin-left: 4px;
  color: #b0b0b0;
  font-size: 12px;
}
.inline-suffix {
  display: flex;
  align-items: center;
  gap: 6px;
}
.suffix-text {
  font-size: 13px;
  color: #666;
}
.signin-row {
  display: flex;
  align-items: flex-start;
  gap: 48px;
}
.signin-fields {
  min-width: 360px;
}
.signin-calendar {
  width: 240px;
  border: 1px solid #e8e8e8;
  border-radius: 4px;
  overflow: hidden;
}
.signin-calendar__head {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  background: #ff6b35;
  color: #fff;
  font-size: 12px;
  text-align: center;
}
.signin-calendar__grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  font-size: 12px;
  text-align: center;
}
.signin-calendar__cell {
  padding: 4px 0;
  border-top: 1px solid #f0f0f0;
  border-right: 1px solid #f0f0f0;
  color: #333;
}
.page-footer {
  display: flex;
  justify-content: center;
  padding: 8px 0 16px;
}
.btn-save {
  min-width: 120px;
  height: 32px;
  background: #ff6b35;
  border-color: #ff6b35;
}
.btn-save:hover {
  background: #e55a2b;
  border-color: #e55a2b;
}
.detail-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
.detail-tip {
  margin-top: 8px;
  font-size: 12px;
  color: #999;
}
.eval-summary {
  margin: 10px 0;
  font-size: 13px;
  color: #333;
}
.btn-add {
  background: #ff6b35 !important;
  border-color: #ff6b35 !important;
}
</style>
