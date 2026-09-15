<template>
  <ErrorBoundary @error="handleError">
    <PageContainer
      full-height
    >
      <!--
        页面定位：商城 → 商城设置 → 商城装修（对标 ql361「商城 → 商城设置 → 商城装修」）
        对标文档：docs/Yh-Spec/手动整理对标开发文档/交易模块/商城装修开发文档.md
        结构说明：4 个顶部 Tab（风格选择 / 装修模板 / 分类页装修 / 商品详情）
                 · 风格选择：9 种主题色 → 写 ShopConfig.themeColor（既有列，已落库）
                 · 装修模板：子 Tab「我的模板」（shopTemplateApi 真实接口 + shop_config.template_id 发布）
                   +「模板库」（shopTemplateApi.library 真实接口 + reference 引用端点）
                   +「新增模板」→ 真实写 shop_decoration（scope=HOME）
                   + 轮播图管理（原页面真实接口 CRUD，保留）
                 · 分类页装修：4 项 → ShopConfig 的 category_display_mode/default_sort/style/show_count
                   （Flyway V11.366.0 新增列），真实读写
                 · 商品详情：富文本（⚠️ 暂用 a-textarea，富文本组件仍未实现）+ 设置应用商品
                   → shop_decoration（scope=PRODUCT_DETAIL）+ shop_decoration_product 真实读写
        金标准：ErrorBoundary > PageContainer(full-height) > a-tabs + 分组卡片（配置页走路线 A′）
        API：shopConfigApi / shopTemplateApi / shopDecorationApi / shopBannerApi / mallProductApi（均为真实后端接口）
        ⚠️ 未实测/未实现项：见文件末尾 GAP 注释 + 开发文档「剩余缺口」
      -->
      <div class="decoration-layout">
        <a-tabs
          v-model:active-key="activeTab"
          class="decoration-tabs"
        >
          <template #rightExtra>
            <span class="tab-hint">全部设置修改后需点击各 Tab 底部「保存」生效</span>
          </template>

          <!-- ══════ Tab 一：风格选择（9 种主题色） ══════ -->
          <a-tab-pane
            key="style"
            tab="风格选择"
          >
            <a-card
              title="主题色"
              :bordered="false"
              class="group-card"
            >
              <div class="group-tip">
                通过颜色的选择切换商城主题和主要操作按钮的颜色（对标 9 种主题色）；保存写入 ShopConfig.themeColor
              </div>
              <a-spin :spinning="loading">
                <div class="theme-grid">
                  <div
                    v-for="t in THEME_COLORS"
                    :key="t.value"
                    class="theme-item"
                    :class="{ 'theme-item--active': themeColor === t.value }"
                    @click="themeColor = t.value"
                  >
                    <span
                      class="theme-swatch"
                      :style="{ background: t.hex }"
                    />
                    <span class="theme-label">{{ t.label }}</span>
                    <CheckCircleFilled
                      v-if="themeColor === t.value"
                      class="theme-check"
                    />
                  </div>
                </div>
              </a-spin>
            </a-card>
            <div class="tab-footer">
              <a-space>
                <a-button
                  type="primary"
                  :loading="saving"
                  @click="handleSaveStyle"
                >
                  保存
                </a-button>
                <a-button
                  :disabled="loading"
                  @click="loadConfig"
                >
                  重置
                </a-button>
              </a-space>
            </div>
          </a-tab-pane>

          <!-- ══════ Tab 二：装修模板（我的模板 / 模板库） ══════ -->
          <a-tab-pane
            key="template"
            tab="装修模板"
          >
            <a-card
              :bordered="false"
              class="group-card"
            >
              <a-tabs
                v-model:active-key="templateSubTab"
                size="small"
              >
                <!-- 子 Tab · 我的模板 -->
                <a-tab-pane
                  key="mine"
                  tab="我的模板"
                >
                  <div class="template-tip">
                    1、您可点击"新增模板"然后进行编辑保存，保存后所有模板将显示在此页面；<br>
                    2、操作【发布模板】即可应用当前模板到微商城。
                  </div>
                  <div class="template-toolbar">
                    <a-space>
                      <a-button
                        size="small"
                        :loading="templateLoading"
                        @click="loadTemplates"
                      >
                        <ReloadOutlined /> 刷新
                      </a-button>
                      <a-button
                        size="small"
                        @click="handleCreateTemplate"
                      >
                        <PlusOutlined /> 新增模板
                      </a-button>
                    </a-space>
                    <div class="using-template">
                      正在使用模板：<a-tag color="blue">
                        {{ usingTemplateName }}
                      </a-tag>
                    </div>
                  </div>
                  <!-- 已保存的装修配置（shop_decoration，scope=HOME）——「新增模板」真实落库于此 -->
                  <div class="form-tip">
                    已保存装修配置（shop_decoration，scope=HOME）：{{ homeDecorations.length }} 条<template v-if="homeDecorations.length"> —— {{ homeDecorations.map(d => d.name).join('、') }}</template>
                  </div>
                  <a-spin :spinning="templateLoading">
                    <a-empty
                      v-if="!templates.length"
                      description="暂无模板，点击「新增模板」开始装修"
                    />
                    <div
                      v-else
                      class="template-grid"
                    >
                      <div
                        v-for="t in templates"
                        :key="t.id"
                        class="template-item"
                        :class="{ 'template-item--active': selectedTemplateId === t.id }"
                        @click="selectedTemplateId = t.id"
                      >
                        <div class="template-thumb">
                          <img
                            v-if="t.thumbnail"
                            :src="t.thumbnail"
                            :alt="t.templateName"
                          >
                          <span v-else>{{ t.templateName }}</span>
                        </div>
                        <div class="template-name">
                          {{ t.templateName }}
                          <a-tag
                            v-if="t.isDefault === 1"
                            color="blue"
                          >
                            默认
                          </a-tag>
                          <a-tag
                            v-if="isUsing(t)"
                            color="green"
                          >
                            使用中
                          </a-tag>
                        </div>
                        <div
                          v-if="t.description"
                          class="template-desc"
                        >
                          {{ t.description }}
                        </div>
                      </div>
                    </div>
                  </a-spin>
                  <div
                    v-if="templates.length"
                    class="tab-footer"
                  >
                    <a-space>
                      <a-button
                        type="primary"
                        :loading="templateSaving"
                        @click="publishTemplate"
                      >
                        发布模板
                      </a-button>
                      <span class="form-tip">
                        发布后商城立即应用该模板；发布后需对当前模板进行商品关联，以便用户访问才能看到商品数据
                      </span>
                    </a-space>
                  </div>
                </a-tab-pane>

                <!-- 子 Tab · 模板库（后端真实接口 /erp/mall/admin/template/library） -->
                <a-tab-pane
                  key="library"
                  tab="模板库"
                >
                  <div class="template-tip">
                    1、操作步骤：选择心仪的模板&gt;点击【引用】&gt;跳转至排版布局页面进行编辑&gt;点击【保存】即可完成，也可操作【发布】，发布后商城会立即应用该模板；<br>
                    2、模板不具备唯一性，您可以选择任意一个行业的模板进行布局；<br>
                    3、点击【发布】后，您需要对当前模板进行商品关联，以便用户访问才能看到商品数据。
                  </div>
                  <div
                    v-if="!libraryLoading"
                    class="form-tip"
                    style="margin-bottom: 12px"
                  >
                    本次仅落地**实测到的行业名清单**（模板条目来自后端行业模板库）；各行业模板的**内部布局内容未实测**，
                    库条目 `config_json` 一律为空，引用后需自行编辑装修结构。
                  </div>
                  <a-spin :spinning="libraryLoading">
                    <a-empty
                      v-if="!libraryTemplates.length"
                      description="行业模板库暂无条目（模板内容未实测，未预置任何模板结构）"
                    >
                      <template #description>
                        <div>行业模板库暂无条目（模板内容未实测，未预置任何模板结构）</div>
                        <div
                          v-if="missingIndustries.length"
                          style="font-size: 12px; color: #8c8c8c; margin-top: 4px"
                        >
                          实测 14 行业中未入库：{{ missingIndustries.join('、') }}
                        </div>
                      </template>
                    </a-empty>
                    <div
                      v-else
                      class="template-grid"
                    >
                      <div
                        v-for="t in libraryTemplates"
                        :key="t.id"
                        class="template-item"
                      >
                        <div class="template-thumb">
                          <img
                            v-if="t.thumbnail"
                            :src="t.thumbnail"
                            :alt="t.templateName"
                          >
                          <span v-else>{{ t.industryName || t.templateName }}</span>
                        </div>
                        <div class="template-name">
                          {{ t.industryName || t.templateName }}
                        </div>
                        <div class="template-desc">
                          模板内容未实测（仅行业名清单为实测）
                        </div>
                        <a-button
                          size="small"
                          :loading="templateSaving"
                          @click="handleReferenceTemplate(t)"
                        >
                          引用
                        </a-button>
                      </div>
                    </div>
                  </a-spin>
                  <!-- 后端已补行业模板库与引用端点（/template/library + /template/library/{id}/reference）； -->
                  <!-- 「跳转排版布局编辑器」仍为本系统缺口（无拖拽装修编辑器），故引用后只能编辑装修结构 -->
                  <div class="form-tip">
                    引用会将库模板复制为本租户「我的模板」；⚠️ 本系统暂无拖拽装修编辑器，
                    对标「跳转排版布局页面进行编辑」与各行业模板内容均**未实测/未实现**。
                  </div>
                </a-tab-pane>
              </a-tabs>
            </a-card>

            <!-- 轮播图管理（原页面真实接口 CRUD，保留） -->
            <a-card
              title="轮播图管理"
              :bordered="false"
              class="group-card"
            >
              <template #extra>
                <a-button
                  type="primary"
                  size="small"
                  @click="openCreate"
                >
                  <template #icon>
                    <PlusOutlined />
                  </template>新增轮播图
                </a-button>
              </template>
              <a-table
                :data-source="banners"
                :columns="bannerColumns"
                :loading="bannerLoading"
                :pagination="false"
                size="small"
                row-key="id"
                :locale="{ emptyText: '暂无轮播图' }"
              >
                <template #bodyCell="{ column, text, record }">
                  <template v-if="column.dataIndex === 'imageUrl'">
                    <a-image
                      v-if="text"
                      :src="text"
                      :width="80"
                      :height="36"
                      style="object-fit: cover; border-radius: 4px"
                    />
                    <span v-else>-</span>
                  </template>
                  <template v-else-if="column.dataIndex === 'linkType'">
                    {{ LINK_TYPE_MAP[text]?.label || text || '-' }}
                  </template>
                  <template v-else-if="column.dataIndex === 'status'">
                    <a-tag :color="text === 1 ? 'green' : 'default'">
                      {{ text === 1 ? '启用' : '停用' }}
                    </a-tag>
                  </template>
                  <template v-else-if="column.dataIndex === 'action'">
                    <a-space>
                      <a-button
                        type="link"
                        size="small"
                        @click="openEdit(record as ShopBanner)"
                      >
                        编辑
                      </a-button>
                      <a-button
                        type="link"
                        size="small"
                        :style="record.status === 1 ? 'color: #fa8c16' : 'color: #52c41a'"
                        @click="toggleStatus(record as ShopBanner)"
                      >
                        {{ record.status === 1 ? '停用' : '启用' }}
                      </a-button>
                      <a-popconfirm
                        title="确认删除该轮播图？"
                        ok-text="删除"
                        cancel-text="取消"
                        @confirm="handleDelete(record as ShopBanner)"
                      >
                        <a-button
                          type="link"
                          size="small"
                          danger
                        >
                          删除
                        </a-button>
                      </a-popconfirm>
                    </a-space>
                  </template>
                </template>
              </a-table>
            </a-card>
          </a-tab-pane>

          <!-- ══════ Tab 三：分类页装修（4 项） ══════ -->
          <a-tab-pane
            key="category"
            tab="分类页装修"
          >
            <a-card
              title="分类页展示配置"
              :bordered="false"
              class="group-card"
            >
              <a-form
                :label-col="{ span: 5 }"
                :wrapper-col="{ span: 16 }"
                size="small"
              >
                <a-form-item label="分类展示方式">
                  <a-radio-group v-model:value="category.displayMode">
                    <a-radio
                      v-for="o in DISPLAY_MODE_OPTIONS"
                      :key="o.value"
                      :value="o.value"
                    >
                      {{ o.label }}
                    </a-radio>
                  </a-radio-group>
                  <!-- 库内存在但未在实测选项内的原值：只读展示，保存时原样保留（不丢数据） -->
                  <a-tag
                    v-if="rawCategoryValues.displayMode"
                    color="orange"
                    style="margin-left: 8px"
                  >
                    库内原值：{{ rawCategoryValues.displayMode }}（对标选项未实测）
                  </a-tag>
                </a-form-item>
                <a-form-item label="商品默认排序">
                  <a-select
                    v-model:value="category.defaultSort"
                    style="width: 200px"
                    :options="SORT_OPTIONS"
                  />
                  <div
                    v-if="rawCategoryValues.defaultSort"
                    class="form-tip"
                  >
                    库内原值：{{ rawCategoryValues.defaultSort }}（不在对标值域内，保存时原样保留）
                  </div>
                  <div class="form-tip">
                    选项为对标**完整实测值域**（按货号升序/按货号降序/按销量降序/按商品名称/综合排序），
                    取值编码沿用对标 `mall_goodsorder`（1/3/5/7/9），默认「综合排序」= 9。
                  </div>
                </a-form-item>
                <a-form-item label="分类页分类样式">
                  <a-radio-group v-model:value="category.categoryStyle">
                    <a-radio
                      v-for="o in CATEGORY_STYLE_OPTIONS"
                      :key="o.value"
                      :value="o.value"
                    >
                      {{ o.label }}
                    </a-radio>
                  </a-radio-group>
                  <a-tag
                    v-if="rawCategoryValues.categoryStyle"
                    color="orange"
                    style="margin-left: 8px"
                  >
                    库内原值：{{ rawCategoryValues.categoryStyle }}（对标选项未实测）
                  </a-tag>
                  <div class="form-tip">
                    纯文本模式将不会展示分类图片
                  </div>
                </a-form-item>
                <a-form-item label="显示分类下商品数量">
                  <a-checkbox v-model:checked="category.showProductCount" />
                </a-form-item>
                <div class="form-tip">
                  以上 4 项对标实测控件与选项：单选/单选/下拉/复选框；取值编码（CATALOG/BRAND、TEXT/IMAGE）为
                  **本实现口径**（对标只给出中文标签，未实测存储编码）。
                </div>
              </a-form>
            </a-card>
            <div class="tab-footer">
              <a-space>
                <a-button
                  type="primary"
                  :loading="saving"
                  @click="handleSaveCategory"
                >
                  保存
                </a-button>
                <a-button @click="resetCategory">
                  重置
                </a-button>
              </a-space>
            </div>
          </a-tab-pane>

          <!-- ══════ Tab 四：商品详情（富文本 + 设置应用商品） ══════ -->
          <a-tab-pane
            key="detail"
            tab="商品详情"
          >
            <a-card
              title="商品详情"
              :bordered="false"
              class="group-card"
            >
              <!-- ⚠️ 缺口：项目暂无统一富文本组件，仍用 a-textarea 承载（对标工具栏未接入） -->
              <a-textarea
                v-model:value="detail.content"
                :rows="12"
                placeholder="请输入商品详情内容"
                show-count
                :maxlength="20000"
              />
              <div class="form-tip">
                ⚠️ 仍为**未实现项**：本系统暂无统一富文本组件，对标工具栏（加粗/斜体/下划线/删除线/字体颜色/
                背景色/有序·无序列表/对齐/单图·多图上传/超链接/分隔线/撤销·重做等）未接入。
                保存落库的是本文本框内容（`shop_decoration.config_json`，纯文本/JSON 文本，结构未实测）。
              </div>
            </a-card>

            <a-card
              title="设置应用商品"
              :bordered="false"
              class="group-card"
            >
              <div class="group-tip">
                关联应用该商品详情的商品（对标「设置应用商品」入口）。
                保存后写入 `shop_decoration_product`（按装修配置**全量替换**）。
              </div>
              <a-space
                direction="vertical"
                style="width: 100%"
              >
                <a-select
                  v-model:value="detail.productIds"
                  mode="multiple"
                  allow-clear
                  show-search
                  option-filter-prop="label"
                  :options="productOptions"
                  :loading="productLoading"
                  placeholder="请选择应用商品"
                  style="width: 100%"
                />
                <div class="form-tip">
                  ⚠️ 对标「设置应用商品」的**交互（弹窗/多选/单选）未实测**，本页沿用多选并落
                  `shop_decoration_product`（本实现口径）；未造对标没有的交互。
                  当前装修配置：{{ detailDecoration?.id ? `#${detailDecoration.id} ${detailDecoration.name || ''}` : '尚未创建（保存时自动创建 PRODUCT_DETAIL 装修配置）' }}
                </div>
              </a-space>
            </a-card>
            <div class="tab-footer">
              <a-space>
                <a-button
                  type="primary"
                  :loading="saving"
                  @click="handleSaveDetail"
                >
                  保存
                </a-button>
              </a-space>
            </div>
          </a-tab-pane>
        </a-tabs>

        <!-- 新增模板弹窗（真实接口：POST /erp/mall/admin/decoration，scope=HOME） -->
        <a-modal
          v-model:open="createModalOpen"
          title="新增模板"
          :confirm-loading="createModalSaving"
          width="460px"
          @ok="submitCreateTemplate"
        >
          <a-form
            :label-col="{ span: 6 }"
            :wrapper-col="{ span: 16 }"
            size="small"
          >
            <a-form-item label="模板名称">
              <a-input
                v-model:value="newTemplateForm.name"
                placeholder="请输入模板名称"
                allow-clear
                @press-enter="submitCreateTemplate"
              />
            </a-form-item>
            <div class="form-tip">
              模板将保存到「我的模板」（`shop_decoration`，scope=HOME）。
              ⚠️ 对标「进入排版布局/拖拽编辑器」的交互本系统未实现（无编辑器），
              故此处只落库空装修结构，装修结构（`config_json`）待后续编辑保存。
            </div>
          </a-form>
        </a-modal>

        <!-- 新增/编辑轮播图弹窗（保留原真实接口逻辑） -->
        <a-modal
          v-model:open="modalOpen"
          :title="editingId ? '编辑轮播图' : '新增轮播图'"
          :confirm-loading="saving"
          width="560px"
          @ok="handleSave"
        >
          <a-form
            ref="formRef"
            :model="form"
            :rules="rules"
            :label-col="{ span: 6 }"
            :wrapper-col="{ span: 16 }"
          >
            <a-form-item
              label="标题"
              name="title"
            >
              <a-input
                v-model:value="form.title"
                placeholder="轮播图标题"
              />
            </a-form-item>
            <a-form-item
              label="图片URL"
              name="imageUrl"
            >
              <a-input
                v-model:value="form.imageUrl"
                placeholder="轮播图图片地址"
              />
            </a-form-item>
            <a-form-item
              label="跳转类型"
              name="linkType"
            >
              <a-select
                v-model:value="form.linkType"
                :options="linkTypeOptions"
                placeholder="请选择跳转类型"
              />
            </a-form-item>
            <a-form-item
              v-if="form.linkType && form.linkType !== 'none'"
              label="跳转目标"
              name="linkValue"
            >
              <a-input
                v-model:value="form.linkValue"
                :placeholder="form.linkType === 'product' ? '商品ID' : '分类ID'"
              />
            </a-form-item>
            <a-form-item
              label="外部链接"
              name="linkUrl"
            >
              <a-input
                v-model:value="form.linkUrl"
                placeholder="可选，自定义跳转URL"
              />
            </a-form-item>
            <a-form-item
              label="排序"
              name="sortOrder"
            >
              <a-input-number
                v-model:value="form.sortOrder"
                :min="0"
                :precision="0"
                style="width: 100%"
              />
            </a-form-item>
            <a-form-item
              label="状态"
              name="status"
            >
              <a-radio-group v-model:value="form.status">
                <a-radio :value="1">
                  启用
                </a-radio>
                <a-radio :value="0">
                  停用
                </a-radio>
              </a-radio-group>
            </a-form-item>
          </a-form>
        </a-modal>
      </div>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
/**
 * 商城装修（商城 → 商城设置 → 商城装修）
 * 对标文档：docs/Yh-Spec/手动整理对标开发文档/交易模块/商城装修开发文档.md
 * 结构：4 个顶部 Tab（风格选择 / 装修模板 / 分类页装修 / 商品详情）
 *
 * 落库情况（Flyway V11.366.0__Add_Mall_Shop_Decoration_Storage.sql）：
 *   1. 风格选择：ShopConfig.theme_color（**既有列，复用未新造**）→ 9 种主题色真实读写
 *   2. 装修模板：
 *      · 我的模板：/template/list 真实接口；「发布模板」写 shop_config.template_id（**既有列，复用**）
 *      · 新增模板：真实写 shop_decoration（scope=HOME）
 *      · 模板库：/template/library 真实接口 + /template/library/{id}/reference 引用（复制到本租户）
 *      · 轮播图：shop_banner 既有真实 CRUD
 *   3. 分类页装修：新增列 category_display_mode / category_default_sort / category_style /
 *      category_show_count（V11.366.0），走既有 fullConfig 合并保存 + bitToBool/boolToBit 转换
 *   4. 商品详情：shop_decoration（scope=PRODUCT_DETAIL，config_json 存富文本）
 *      + shop_decoration_product（「设置应用商品」全量替换）真实读写
 *
 * ⚠️ 未实测/未实现（如实留缺口，不臆造）：
 *   · 富文本组件：项目暂无统一富文本组件，仍用 a-textarea（对标工具栏未接入）
 *   · 拖拽排版编辑器：无（对标「新增模板→进入排版布局编辑器」只落库空结构）
 *   · 各行业模板的**内部布局内容未实测** → 库条目 config_json 为空，不预置模板结构
 *   · 「商品默认排序」下拉值域**已完整实测**（mall_goodsorder：1/3/5/7/9）
 *   · 分类展示方式/分类样式的**存储编码未实测**（对标只给中文标签）→ CATALOG/BRAND、TEXT/IMAGE 为本实现口径
 *   · shop_decoration / shop_decoration_product 表结构为**本实现口径**（对标未实测装修配置字段）
 *   · 「设置应用商品」的真实交互未实测 → 沿用多选 + 全量替换，未造交互
 *   · shop_template 无 tenant_id 列 ⇒ 行业库条目为平台共享，「我的模板」非按租户隔离（沿用既有口径）
 */
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { message } from 'ant-design-vue'
import type { Rule } from 'ant-design-vue/es/form'
import { PlusOutlined, ReloadOutlined, CheckCircleFilled } from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import {
  shopBannerApi, shopConfigApi, shopTemplateApi, shopDecorationApi, mallProductApi,
  bitToBool, boolToBit,
  type ShopBanner, type ShopConfig, type ShopTemplate, type ShopDecoration, type ShopDecorationScope
} from '@/api/erp/mall'

defineOptions({ name: 'MallShopDecoration' })

// ═══ Tab 一 · 风格选择：9 种主题色（对标：绯红/宝蓝/亮橙/桃粉/天蓝/翠绿/香槟/炫紫/深绿） ═══
// 说明：对标仅给出 9 种主题色名称，未提供色值；hex 为按名称取的标准色，接入后端时可替换为对标精确色值
const THEME_COLORS = [
  { label: '绯红', value: 'crimson', hex: '#DC143C' },
  { label: '宝蓝', value: 'royalblue', hex: '#4169E1' },
  { label: '亮橙', value: 'orange', hex: '#FFA500' },
  { label: '桃粉', value: 'pink', hex: '#FFB6C1' },
  { label: '天蓝', value: 'skyblue', hex: '#87CEEB' },
  { label: '翠绿', value: 'emerald', hex: '#00A86B' },
  { label: '香槟', value: 'champagne', hex: '#D4B483' },
  { label: '炫紫', value: 'purple', hex: '#8A2BE2' },
  { label: '深绿', value: 'darkgreen', hex: '#006400' },
]

// ═══ Tab 二 · 装修模板 ═══
const LINK_TYPE_MAP: Record<string, { label: string }> = {
  product: { label: '商品详情' },
  category: { label: '商品分类' },
  none: { label: '无跳转' }
}
const linkTypeOptions = Object.entries(LINK_TYPE_MAP).map(([value, v]) => ({ label: v.label, value }))

/** 模板库 14 行业（对标实测逐字）—— 仅用于「库里没有条目时」的兜底展示 */
const INDUSTRY_TEMPLATES = [
  '办公用品', '服装鞋帽', '化妆用品', '机械机电', '家电数码', '母婴用品', '汽修汽配',
  '生鲜农贸', '食品快消品', '手机通讯', '通用行业', '五金建材', '医药制品', '珠宝钟表',
]

// ═══ 装修配置（shop_decoration，V11.366.0）范围常量 ═══
// ⚠️ scope 取值为**本实现口径**：对标抓取只实测到 4 个 Tab，无 scope 编码。
/** 「我的模板」对应的装修范围 */
const SCOPE_HOME: ShopDecorationScope = 'HOME'
/** 商品详情装修对应的装修范围 */
const SCOPE_PRODUCT_DETAIL: ShopDecorationScope = 'PRODUCT_DETAIL'

const activeTab = ref('style')
const templateSubTab = ref('mine')

const templates = ref<ShopTemplate[]>([])
const templateLoading = ref(false)
const templateSaving = ref(false)
const selectedTemplateId = ref<number | undefined>(undefined)
/** 行业模板库条目（GET /erp/mall/admin/template/library；内容未实测，configJson 一律为空） */
const libraryTemplates = ref<ShopTemplate[]>([])
const libraryLoading = ref(false)
let fullConfig: ShopConfig | null = null

/** 装修配置（排版布局存储）列表，覆盖「我的模板」需要落库的装修结构 */
const decorations = ref<ShopDecoration[]>([])

/** 按 scope 找本租户的装修配置行（每条 scope 只维护一行） */
function decorationOf(scope: ShopDecorationScope): ShopDecoration | undefined {
  return decorations.value.find(d => d.scope === scope)
}

/** 「我的模板」当前使用的装修配置行（发布模板 = 把 templateId 写到 config.templateId，装修结构写本行） */
const homeDecorations = computed(() => decorations.value.filter(d => d.scope === SCOPE_HOME))
/** 商品详情装修配置行（承载富文本 + 关联商品；见 Tab 四） */
const detailDecoration = computed(() => decorationOf(SCOPE_PRODUCT_DETAIL))

const usingTemplateName = computed(() => {
  const t = templates.value.find(x => x.id === fullConfig?.templateId)
  return t?.templateName || (fullConfig?.templateId ? `模板#${fullConfig.templateId}` : '未设置')
})

function isUsing(t: ShopTemplate) {
  return !!fullConfig?.templateId && fullConfig.templateId === t.id
}

/** ═══ 配置读写（themeColor / templateId / 分类页装修 4 项已落库） ═══ */
const loading = ref(false)
const saving = ref(false)
const configId = ref<number | undefined>(undefined)
const themeColor = ref<string>('royalblue')

async function loadConfig() {
  loading.value = true
  try {
    const res: any = await shopConfigApi.get()
    const cfg: ShopConfig | null = res?.data ?? res ?? null
    fullConfig = cfg
    configId.value = cfg?.id
    themeColor.value = cfg?.themeColor || 'royalblue'
    selectedTemplateId.value = cfg?.templateId
    loadCategoryConfig(cfg)
  } catch (e) {
    console.warn('[商城装修] 商城配置获取失败', e)
  } finally {
    loading.value = false
  }
}

async function loadTemplates() {
  templateLoading.value = true
  try {
    const res: any = await shopTemplateApi.list()
    templates.value = Array.isArray(res) ? res : (res?.data ?? [])
  } catch (e) {
    console.warn('[商城装修] 模板列表获取失败', e)
    templates.value = []
  } finally {
    templateLoading.value = false
  }
}

/** 行业模板库列表（真实接口；内容未实测，卡片对空内容显式提示） */
async function loadLibrary() {
  libraryLoading.value = true
  try {
    const res: any = await shopTemplateApi.library()
    libraryTemplates.value = Array.isArray(res) ? res : (res?.data ?? [])
  } catch (e) {
    console.warn('[商城装修] 行业模板库获取失败', e)
    libraryTemplates.value = []
  } finally {
    libraryLoading.value = false
  }
}

/** 装修配置列表（真实接口） */
async function loadDecorations() {
  try {
    const res: any = await shopDecorationApi.list()
    decorations.value = Array.isArray(res) ? res : (res?.data ?? [])
  } catch (e) {
    console.warn('[商城装修] 装修配置获取失败', e)
    decorations.value = []
  }
}

/** 实测 14 行业中，库列表里尚未出现的行业名（仅用于空态提示，不伪造模板条目） */
const missingIndustries = computed(() => {
  const loaded = new Set(libraryTemplates.value.map(t => t.industryName).filter(Boolean) as string[])
  return INDUSTRY_TEMPLATES.filter(name => !loaded.has(name))
})

function handleError(err: any) {
  console.warn('[商城装修] ErrorBoundary:', err)
}

// ── Tab 一保存：主题色 → ShopConfig.themeColor（既有列，复用不新造） ──
async function handleSaveStyle() {
  saving.value = true
  try {
    await shopConfigApi.update({
      ...(fullConfig || {}),
      id: configId.value,
      shopName: fullConfig?.shopName || '订货商城',
      themeColor: themeColor.value,
    })
    message.success('风格选择已保存')
    await loadConfig()
  } catch (e) {
    console.warn('[商城装修] 主题色保存失败', e)
    message.error('风格选择保存失败')
  } finally {
    saving.value = false
  }
}

// ── Tab 二：发布模板（应用 templateId 到商城；既有列 template_id，复用不新造） ──
async function publishTemplate() {
  if (!selectedTemplateId.value) {
    message.warning('请选择要发布/应用的模板')
    return
  }
  templateSaving.value = true
  try {
    await shopConfigApi.update({
      ...(fullConfig || {}),
      id: configId.value,
      shopName: fullConfig?.shopName || '订货商城',
      templateId: selectedTemplateId.value,
    })
    message.success('模板已发布并应用到微商城，请对模板进行商品关联')
    await loadConfig()
  } catch (e) {
    console.warn('[商城装修] 模板发布失败', e)
    message.error('模板发布失败')
  } finally {
    templateSaving.value = false
  }
}

/**
 * 新增模板（对标「装修模板 → 我的模板 → 新增模板」）。
 * 真实落库：在 shop_decoration 建一条 scope=HOME 的装修配置（结构留空，待排版布局编辑器写入）。
 * ⚠️ 对标「进入排版布局/拖拽编辑器」的交互本系统未实现（无编辑器），故此处只落库空结构。
 */
const createModalOpen = ref(false)
const createModalSaving = ref(false)
const newTemplateForm = reactive({ name: '' })

function handleCreateTemplate() {
  newTemplateForm.name = ''
  createModalOpen.value = true
}

async function submitCreateTemplate() {
  const name = newTemplateForm.name.trim()
  if (!name) {
    message.warning('请输入模板名称')
    return
  }
  createModalSaving.value = true
  try {
    await shopDecorationApi.create({ name, scope: SCOPE_HOME, status: 1 })
    message.success('模板已新增，可在「我的模板」中查看')
    createModalOpen.value = false
    await loadDecorations()
  } catch (e) {
    console.warn('[商城装修] 新增模板失败', e)
    message.error('新增模板失败')
  } finally {
    createModalSaving.value = false
  }
}

/**
 * 引用行业库模板（真实接口：复制库条目为本租户「我的模板」）。
 * ⚠️ 库条目的模板内容未实测（configJson 为空）⇒ 引用结果内容同样为空，这是如实留缺口。
 */
async function handleReferenceTemplate(record: ShopTemplate) {
  templateSaving.value = true
  try {
    await shopTemplateApi.reference(record.id)
    message.success(`已引用「${record.industryName || record.templateName}」，可在「我的模板」中查看（模板内容未实测，需自行编辑装修结构）`)
    await Promise.all([loadTemplates(), loadLibrary()])
  } catch (e) {
    console.warn('[商城装修] 引用模板失败', e)
    message.error('引用模板失败')
  } finally {
    templateSaving.value = false
  }
}

// ═══ Tab 三 · 分类页装修（4 项 → tenant_shop_config 真实读写，V11.366.0） ═══
/**
 * 对标实测（2026-09-07 ql361 界面抓取 + 2026-09-14 存储键补抓）4 项真实控件与选项：
 *   ① 分类展示方式     单选 按目录分类（默认勾选）/ 按品牌分类
 *   ② 商品默认排序     下拉 —— ✅ **选项全集已实测**，见 SORT_OPTIONS
 *   ③ 分类页分类样式   单选 纯文本模式（默认勾选）/ 图文模式
 *   ④ 显示分类下商品数量 复选框（默认开启）
 *
 * 对标真实存储键与编码（2026-09-14 从 ql361 自己的 GetSettings 响应内层设置字典解出，非推测）：
 *   · 分类展示方式     → `mall_product_categorytype`，实测当前值 "0" = 按目录分类
 *   · 商品默认排序     → `mall_goodsorder`，实测当前值 "9" = 综合排序（值域见下）
 *   · 分类页分类样式   → `mall_categorypage_model`，实测当前值 "1" = 纯文本模式
 *       （卫星键佐证：`..._url=desktop/images/mallcommon/textmodel.png`、
 *        `..._explain=提示：纯文本模式将不会展示分类图片`）
 *   证据：`docs/Yh-Spec/抓取结果/商城装修_存储键_补抓20260914.json`
 *
 * ⚠️ 本页**沿用对标的值域与编码**（SORT_OPTIONS 直接用对标码 1/3/5/7/9）；
 *    ①③ 两项仍用本实现字符串码（CATALOG/BRAND、TEXT/IMAGE），因为「按品牌分类 / 图文模式」
 *    在对标侧对应的编码**未取到**，不猜；我方 CATALOG↔对标 0、TEXT↔对标 1 语义一致。
 */
const DISPLAY_MODE_OPTIONS = [
  { label: '按目录分类', value: 'CATALOG' },
  { label: '按品牌分类', value: 'BRAND' },
]
/**
 * ✅ 对标「商品默认排序」**完整值域**（实测，非自造）。
 * 来源：ql361 前端常量表 `mallgoodsorder`
 *   `[{label:"按货号升序",value:"1"},{label:"按货号降序",value:"3"},{label:"按销量降序",value:"5"},
 *     {label:"按商品名称",value:"7"},{label:"综合排序",value:"9"}]`
 * 默认「综合排序」= **9**，与 GetSettings 实测当前值 `mall_goodsorder="9"` 一致。
 * ⚠️ 此前自造的「销量排序／价格升序／价格降序／上新时间」**在标的中不存在**，已删除改正。
 */
const SORT_OPTIONS = [
  { label: '按货号升序', value: '1' },
  { label: '按货号降序', value: '3' },
  { label: '按销量降序', value: '5' },
  { label: '按商品名称', value: '7' },
  { label: '综合排序', value: '9' },
]
const CATEGORY_STYLE_OPTIONS = [
  { label: '纯文本模式', value: 'TEXT' },
  { label: '图文模式', value: 'IMAGE' },
]
const DEFAULT_CATEGORY = {
  displayMode: 'CATALOG',
  // 对标默认「综合排序」= 9（GetSettings 实测 mall_goodsorder="9"）
  defaultSort: '9',
  categoryStyle: 'TEXT',
  showProductCount: true,
}
const category = reactive({ ...DEFAULT_CATEGORY })
/** 库内存在但本实现枚举未覆盖的原值 —— 只读展示，保存时原样保留，不丢数据 */
const rawCategoryValues = reactive({ displayMode: '', defaultSort: '', categoryStyle: '' })

/** 把后端配置回填到「分类页装修」表单（布尔列用 bitToBool 显式转换） */
function loadCategoryConfig(cfg: ShopConfig | null) {
  category.displayMode = cfg?.categoryDisplayMode || DEFAULT_CATEGORY.displayMode
  category.defaultSort = cfg?.categoryDefaultSort || DEFAULT_CATEGORY.defaultSort
  category.categoryStyle = cfg?.categoryStyle || DEFAULT_CATEGORY.categoryStyle
  category.showProductCount = bitToBool(cfg?.categoryShowCount, DEFAULT_CATEGORY.showProductCount)
  // 记录未在枚举内的原值（历史库值可能是旧的 COMPOSITE 口径，避免保存时被悄悄改写）
  rawCategoryValues.displayMode = DISPLAY_MODE_OPTIONS.some(o => o.value === category.displayMode) ? '' : category.displayMode
  rawCategoryValues.defaultSort = SORT_OPTIONS.some(o => o.value === category.defaultSort) ? '' : category.defaultSort
  rawCategoryValues.categoryStyle = CATEGORY_STYLE_OPTIONS.some(o => o.value === category.categoryStyle) ? '' : category.categoryStyle
}

function resetCategory() {
  Object.assign(category, DEFAULT_CATEGORY)
  loadCategoryConfig(fullConfig)
}

async function handleSaveCategory() {
  saving.value = true
  try {
    // 部分字段提交：后端 updateById 忽略 null，不会覆盖其它配置页字段
    await shopConfigApi.update({
      categoryDisplayMode: category.displayMode || undefined,
      categoryDefaultSort: category.defaultSort || undefined,
      categoryStyle: category.categoryStyle || undefined,
      categoryShowCount: boolToBit(category.showProductCount),
    })
    message.success('分类页装修已保存')
    await loadConfig()
  } catch (e) {
    console.warn('[商城装修] 分类页装修保存失败', e)
    message.error('分类页装修保存失败')
  } finally {
    saving.value = false
  }
}

// ═══ Tab 四 · 商品详情（富文本 + 设置应用商品） ═══
const detail = reactive({
  /** 富文本内容（⚠️ 本系统暂无统一富文本组件，仍用 a-textarea 承载，见页面文档「剩余缺口」） */
  content: '',
  productIds: [] as number[],
})
const productOptions = ref<{ label: string; value: number }[]>([])
const productLoading = ref(false)

async function loadProducts() {
  productLoading.value = true
  try {
    const res: any = await mallProductApi.page({ pageNum: 1, pageSize: 200 } as any)
    const list = res?.records ?? res?.data?.records ?? (Array.isArray(res) ? res : [])
    productOptions.value = list.map((p: any) => ({
      label: p.productName || p.productCode || String(p.id),
      value: p.id,
    }))
  } catch (e) {
    console.warn('[商城装修] 商品列表获取失败', e)
    productOptions.value = []
  } finally {
    productLoading.value = false
  }
}

/** 商品详情装修行变化时回填「设置应用商品」（含其已关联商品） */
watch(detailDecoration, async (deco) => {
  if (!deco?.id) {
    detail.productIds = []
    return
  }
  detail.content = deco.configJson || ''
  try {
    const res: any = await shopDecorationApi.listProducts(deco.id)
    detail.productIds = Array.isArray(res) ? res : (res?.data ?? [])
  } catch (e) {
    console.warn('[商城装修] 商品详情关联商品获取失败', e)
    detail.productIds = []
  }
}, { immediate: true })

/**
 * 保存商品详情装修（真实读写）：装修内容落 shop_decoration.config_json，
 * 「设置应用商品」落 shop_decoration_product（全量替换）。
 * ⚠️ 对标「排版布局编辑器/设置应用商品交互」未实测；config_json 结构为本实现口径。
 */
async function handleSaveDetail() {
  saving.value = true
  try {
    const payload: ShopDecoration = {
      name: detailDecoration.value?.name || '商品详情装修',
      scope: SCOPE_PRODUCT_DETAIL,
      configJson: detail.content || '',
      status: 1,
    }
    let id = detailDecoration.value?.id
    if (id) {
      await shopDecorationApi.update(id, payload)
    } else {
      const res: any = await shopDecorationApi.create(payload)
      id = (res?.data ?? res)?.id
    }
    if (id) {
      await shopDecorationApi.replaceProducts(id, detail.productIds)
    }
    message.success('商品详情装修已保存')
    await loadDecorations()
  } catch (e) {
    console.warn('[商城装修] 商品详情保存失败', e)
    message.error('商品详情保存失败')
  } finally {
    saving.value = false
  }
}

// ═══ 轮播图（原页面真实接口 CRUD，保留） ═══
const banners = ref<ShopBanner[]>([])
const bannerLoading = ref(false)

const bannerColumns: any[] = [
  { title: '图片', dataIndex: 'imageUrl', key: 'imageUrl', width: 100 },
  { title: '标题', dataIndex: 'title', key: 'title', width: 150, ellipsis: true },
  { title: '跳转类型', dataIndex: 'linkType', key: 'linkType', width: 100 },
  { title: '跳转目标', dataIndex: 'linkValue', key: 'linkValue', width: 110 },
  { title: '排序', dataIndex: 'sortOrder', key: 'sortOrder', width: 70, align: 'right' },
  { title: '状态', dataIndex: 'status', key: 'status', width: 80 },
  { title: '操作', dataIndex: 'action', key: 'action', width: 160, fixed: 'right' }
]

async function loadBanners() {
  bannerLoading.value = true
  try {
    const res: any = await shopBannerApi.list()
    banners.value = Array.isArray(res) ? res : (res?.data ?? [])
  } catch (e) {
    console.warn('[商城装修] 轮播图获取失败', e)
  } finally {
    bannerLoading.value = false
  }
}

const formRef = ref()
const modalOpen = ref(false)
const editingId = ref<number | null>(null)

const emptyForm = () => ({
  title: '',
  imageUrl: '',
  linkType: 'none' as string,
  linkValue: '',
  linkUrl: '',
  sortOrder: 0 as number,
  status: 1 as number
})
const form = reactive(emptyForm())

const rules: Record<string, Rule[]> = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  imageUrl: [{ required: true, message: '请输入图片URL', trigger: 'blur' }]
}

function openCreate() {
  editingId.value = null
  Object.assign(form, emptyForm())
  modalOpen.value = true
}

function openEdit(record: ShopBanner) {
  editingId.value = record.id ?? null
  Object.assign(form, emptyForm(), {
    title: record.title || '',
    imageUrl: record.imageUrl,
    linkType: record.linkType || 'none',
    linkValue: record.linkValue || '',
    linkUrl: record.linkUrl || '',
    sortOrder: record.sortOrder ?? 0,
    status: record.status ?? 1
  })
  modalOpen.value = true
}

async function handleSave() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  saving.value = true
  try {
    if (editingId.value) {
      await shopBannerApi.update(editingId.value, { ...form } as ShopBanner)
      message.success('轮播图已更新')
    } else {
      await shopBannerApi.create({ ...form } as ShopBanner)
      message.success('轮播图已创建')
    }
    modalOpen.value = false
    loadBanners()
  } catch (e) {
    console.warn('[商城装修] 轮播图保存失败', e)
  } finally {
    saving.value = false
  }
}

async function toggleStatus(record: ShopBanner) {
  const target = record.status === 1 ? 0 : 1
  try {
    await shopBannerApi.update(record.id!, { ...record, status: target })
    message.success(target === 1 ? '轮播图已启用' : '轮播图已停用')
    loadBanners()
  } catch (e) {
    console.warn('[商城装修] 状态切换失败', e)
  }
}

async function handleDelete(record: ShopBanner) {
  try {
    await shopBannerApi.delete(record.id!)
    message.success('轮播图已删除')
    loadBanners()
  } catch (e) {
    console.warn('[商城装修] 删除失败', e)
  }
}

onMounted(() => {
  loadConfig()
  loadTemplates()
  loadBanners()
  loadProducts()
  loadDecorations()
})

// 切到「模板库」子 Tab 时按需拉取行业模板库（真实接口）
watch(templateSubTab, (v) => {
  if (v === 'library') loadLibrary()
})

// 切到「装修模板」Tab 时若模板库为空则预取（首屏不阻塞）
watch(activeTab, (v) => {
  if (v === 'template') {
    loadTemplates()
    loadDecorations()
  }
})
</script>

<style scoped>
.decoration-layout {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding: 12px 16px 0;
  box-sizing: border-box;
}
.decoration-tabs {
  flex: 1;
  min-height: 0;
}
.tab-hint {
  font-size: 12px;
  color: #8c8c8c;
  margin-right: 8px;
}
.group-card {
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 6px;
  margin-bottom: 16px;
}
.group-card :deep(.ant-card-head) {
  min-height: 40px;
  padding: 0 16px;
  border-bottom: 1px solid #f5f5f5;
}
.group-card :deep(.ant-card-head-title) {
  font-size: 14px;
  font-weight: 600;
  padding: 10px 0;
}
.group-card :deep(.ant-card-body) {
  padding: 16px 16px 4px;
}
.group-tip {
  font-size: 12px;
  color: #8c8c8c;
  line-height: 1.6;
  margin: -4px 0 12px;
}
.form-tip {
  font-size: 12px;
  color: #8c8c8c;
  line-height: 1.6;
  margin-top: 2px;
}
/* ── 主题色（9 种） ── */
.theme-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}
.theme-item {
  position: relative;
  width: 110px;
  padding: 10px;
  border: 2px solid #f0f0f0;
  border-radius: 6px;
  cursor: pointer;
  text-align: center;
  transition: border-color 0.2s;
}
.theme-item:hover {
  border-color: #d9d9d9;
}
.theme-item--active {
  border-color: #1890ff;
}
.theme-swatch {
  display: block;
  height: 44px;
  border-radius: 4px;
  margin-bottom: 8px;
}
.theme-label {
  font-size: 13px;
  color: #333;
}
.theme-check {
  position: absolute;
  top: 4px;
  right: 4px;
  color: #1890ff;
  font-size: 16px;
  background: #fff;
  border-radius: 50%;
}
/* ── 装修模板 ── */
.template-tip {
  font-size: 12px;
  color: #8c8c8c;
  line-height: 1.8;
  background: #fafafa;
  border-radius: 4px;
  padding: 8px 12px;
  margin-bottom: 12px;
}
.template-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}
.using-template {
  font-size: 13px;
  color: #595959;
}
.template-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
}
.template-item {
  width: 180px;
  padding: 8px;
  border: 2px solid #f0f0f0;
  border-radius: 8px;
  cursor: pointer;
  transition: border-color 0.2s;
}
.template-item--active {
  border-color: #1890ff;
}
.template-thumb {
  height: 100px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fafafa;
  border-radius: 4px;
  overflow: hidden;
  color: #909399;
}
.template-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.template-name {
  margin-top: 8px;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}
.template-desc {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}
.tab-footer {
  padding: 12px 0 16px;
  border-top: 1px solid #f0f0f0;
  margin-top: 4px;
}
</style>
