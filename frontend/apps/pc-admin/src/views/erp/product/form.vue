<template>
  <ErrorBoundary @error="handleError">
    <div class="product-create-fullscreen">
      <!-- 固定顶栏 -->
      <div class="create-header">
        <div class="create-header__left">
          <a-button
            type="text"
            @click="goBack"
          >
            <LeftOutlined /> 返回
          </a-button>
          <span class="create-title">{{ isEdit ? '编辑商品' : '新增商品' }}</span>
          <span
            v-if="isEdit && productId"
            class="create-code"
          >({{ form.productCode }})</span>
        </div>
        <div class="create-header__right">
          <a-space>
            <span class="shortcut-hints">
              <span class="shortcut-hint"><kbd>Ctrl+S</kbd> 保存</span>
            </span>
            <a-button @click="goBack">
              取消
            </a-button>
            <a-button
              type="primary"
              :loading="saving"
              @click="handleSave"
            >
              <SaveOutlined /> 保存
            </a-button>
            <a-dropdown v-if="!isEdit">
              <a-button
                type="primary"
                ghost
                :loading="saving"
              >
                <SaveOutlined /> 保存并新增
                <DownOutlined />
              </a-button>
              <template #overlay>
                <a-menu @click="handleSaveMenu">
                  <a-menu-item key="save">
                    保存
                  </a-menu-item>
                  <a-menu-item key="save_and_new">
                    保存并新增
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </a-space>
        </div>
      </div>

      <!-- 可滚动内容区 -->
      <div class="create-body">
        <a-spin :spinning="loading">
          <!-- 左侧竖向Tab导航 -->
          <a-tabs
            v-model:active-key="activeTab"
            tab-position="left"
            :tab-bar-style="{ width: '140px' }"
            class="create-tabs-left"
          >
            <!-- ==================== TAB 1: 基本信息 ==================== -->
            <a-tab-pane
              key="basic"
              tab="基本信息"
            >
              <div class="tab-content">
                <!-- 快速新增（仅新增模式显示） -->
                <a-card
                  v-if="!isEdit"
                  class="create-card"
                  size="small"
                  :bordered="false"
                >
                  <a-row
                    :gutter="12"
                    align="middle"
                  >
                    <a-col>
                      <span
                        class="field-label"
                        style="white-space:nowrap;"
                      >快速新增</span>
                    </a-col>
                    <a-col flex="auto">
                      <a-input-search
                        v-model:value="quickSearch"
                        placeholder="输入产品编码/名称快速检索"
                        enter-button="查询"
                        size="small"
                        @search="handleQuickSearch"
                      />
                    </a-col>
                  </a-row>
                </a-card>

                <!-- 商品特性 -->
                <a-card
                  title="商品特性（勾选即启用）"
                  class="create-card"
                  size="small"
                  :bordered="false"
                >
                  <a-checkbox
                    :checked="form.isBatchExpiryManaged === 1"
                    @change="(e: any) => form.isBatchExpiryManaged = e.target.checked ? 1 : 0"
                  >
                    保质期/批次号
                  </a-checkbox>
                </a-card>

                <!-- 商品类型认定 -->
                <a-card
                  title="商品类型认定（标品表示小单位数量不允许有小数，且单位换算关系也不能出现小数，非标品则允许）"
                  class="create-card"
                  size="small"
                  :bordered="false"
                >
                  <a-radio-group
                    v-model:value="form.isStandardProduct"
                    :default-value="1"
                  >
                    <a-radio :value="1">
                      标品
                    </a-radio>
                    <a-radio :value="0">
                      非标品
                    </a-radio>
                  </a-radio-group>
                </a-card>

                <!-- 基础信息 -->
                <a-card
                  title="基础信息"
                  class="create-card"
                  size="small"
                  :bordered="false"
                >
                  <template #extra>
                    <a-space>
                      <a-checkbox
                        :checked="form.useCoupon === 1"
                        @change="(e: any) => form.useCoupon = e.target.checked ? 1 : 0"
                      >
                        使用优惠券
                      </a-checkbox>
                      <a-button
                        type="link"
                        size="small"
                        @click="showFieldConfig"
                      >
                        商品字段及规则配置
                      </a-button>
                    </a-space>
                  </template>
                  <a-row :gutter="24">
                    <a-col :span="8">
                      <a-form-item
                        label="商品名称"
                        required
                      >
                        <a-input
                          v-model:value="form.productName"
                          placeholder="请输入商品名称"
                          size="small"
                        />
                      </a-form-item>
                    </a-col>
                    <a-col :span="8">
                      <a-form-item
                        label="货号"
                        required
                      >
                        <a-input
                          v-model:value="form.productCodeAlias"
                          placeholder="如: sp001"
                          size="small"
                        />
                      </a-form-item>
                    </a-col>
                    <a-col :span="8">
                      <a-form-item
                        label="所属分类"
                        required
                      >
                        <a-tree-select
                          v-model:value="form.categoryId"
                          :tree-data="categoryTree"
                          :field-names="{ children: 'children', label: 'categoryName', value: 'id' }"
                          placeholder="请选择分类"
                          allow-clear
                          size="small"
                          style="width: 100%"
                          :show-search="true"
                          :filter-tree-node="(inputValue: string, node: any) => node.categoryName?.toLowerCase().includes(inputValue.toLowerCase())"
                        />
                      </a-form-item>
                    </a-col>
                    <a-col :span="8">
                      <a-form-item label="保质期天数">
                        <a-input-number
                          v-model:value="form.shelfLifeDays"
                          :min="0"
                          style="width:100%"
                          size="small"
                          :disabled="form.isBatchExpiryManaged !== 1"
                        />
                        <span class="form-tip">天</span>
                      </a-form-item>
                    </a-col>
                    <a-col :span="8">
                      <a-form-item label="近效期天数">
                        <a-input-number
                          v-model:value="form.nearExpiryDays"
                          :min="0"
                          style="width:100%"
                          size="small"
                          :disabled="form.isBatchExpiryManaged !== 1"
                        />
                      </a-form-item>
                    </a-col>
                    <a-col :span="8">
                      <a-form-item label="规格">
                        <a-input
                          v-model:value="form.spec"
                          placeholder="如: 27寸 4K"
                          size="small"
                        />
                      </a-form-item>
                    </a-col>
                    <a-col :span="8">
                      <a-form-item label="型号">
                        <a-input
                          v-model:value="form.model"
                          placeholder="如: XYZ-100"
                          size="small"
                        />
                      </a-form-item>
                    </a-col>
                    <a-col :span="8">
                      <a-form-item
                        label="所属行业类别"
                        required
                      >
                        <a-select
                          v-model:value="form.industryCategory"
                          placeholder="请选择行业类别"
                          size="small"
                          default-value="其他"
                        >
                          <a-select-option value="餐饮">
                            餐饮
                          </a-select-option>
                          <a-select-option value="食品">
                            食品
                          </a-select-option>
                          <a-select-option value="日化">
                            日化
                          </a-select-option>
                          <a-select-option value="电子">
                            电子
                          </a-select-option>
                          <a-select-option value="服装">
                            服装
                          </a-select-option>
                          <a-select-option value="其他">
                            其他
                          </a-select-option>
                        </a-select>
                      </a-form-item>
                    </a-col>
                    <a-col :span="8">
                      <a-form-item label="产地">
                        <a-input
                          v-model:value="form.origin"
                          placeholder="产地"
                          size="small"
                        />
                      </a-form-item>
                    </a-col>
                    <a-col :span="8">
                      <a-form-item label="品牌">
                        <a-input
                          v-model:value="form.brand"
                          placeholder="请输入品牌"
                          size="small"
                        />
                      </a-form-item>
                    </a-col>
                    <a-col :span="24">
                      <a-form-item label="备注">
                        <a-textarea
                          v-model:value="form.remark"
                          :rows="2"
                          size="small"
                        />
                      </a-form-item>
                    </a-col>
                  </a-row>
                </a-card>

                <!-- 商品单位 -->
                <a-card
                  title="商品单位"
                  class="create-card"
                  size="small"
                  :bordered="false"
                >
                  <template #extra>
                    <a-space>
                      <a-button
                        type="primary"
                        size="small"
                        @click="showUnitGroupDialog"
                      >
                        <ApartmentOutlined /> 选择单位组
                      </a-button>
                      <a-button
                        type="link"
                        size="small"
                        @click="openGradeManage"
                      >
                        <CrownOutlined /> 管理价格等级
                      </a-button>
                      <a-button
                        size="small"
                        @click="batchCalcPrice"
                      >
                        <CalculatorOutlined /> 批量价格计算
                      </a-button>
                    </a-space>
                  </template>
                  <div
                    class="unit-table-wrap"
                    :style="{ height: unitTableHeight + 'px' }"
                  >
                    <BillDetailTable
                      :columns="unitColumns"
                      v-model:data-source="unitList"
                      :min-rows="UNIT_MIN_ROWS"
                      storage-key="product-unit-col-config"
                      @cell-change="onUnitCellChange"
                    >
                      <template #isBaseUnitCell="{ record }">
                        <a-tag
                          v-if="record.isBaseUnit"
                          color="blue"
                        >
                          基本单位
                        </a-tag>
                        <span
                          v-else
                          style="color:#999"
                        >换算单位</span>
                      </template>
                      <template #actionCell="{ record, index }">
                        <!-- 前三行是小/中/大单位固定槽位：只能清空数据，不能删行；
                             第 4 行起可删行，且必须从最后一行依次往上删 -->
                        <a-button
                          v-if="index < UNIT_MIN_ROWS"
                          type="link"
                          size="small"
                          title="清空本行数据（小/中/大单位行为固定槽位，不可删除）"
                          @click="clearUnitRow(index)"
                        >
                          <ClearOutlined />
                        </a-button>
                        <a-button
                          v-else
                          type="link"
                          size="small"
                          danger
                          :disabled="!canRemoveUnitRow(index)"
                          :title="canRemoveUnitRow(index) ? '删除此行' : '请从最后一行开始依次删除'"
                          @click="removeUnitRow(index)"
                        >
                          <CloseOutlined />
                        </a-button>
                      </template>
                    </BillDetailTable>
                  </div>
                  <div class="unit-actions">
                    <a-button
                      size="small"
                      type="dashed"
                      @click="addSingleUnitRow"
                    >
                      <PlusOutlined /> 新增行
                    </a-button>
                  </div>

                  <a-divider style="margin: 12px 0;" />
                  <a-row :gutter="16">
                    <a-col :span="8">
                      <a-form-item
                        label="销售常用单位"
                        required
                      >
                        <a-select
                          v-model:value="form.defaultSalesUnitId"
                          placeholder="请选择"
                          size="small"
                          allow-clear
                        >
                          <a-select-option
                            v-for="u in unitList"
                            :key="u.id"
                            :value="u.id"
                            :disabled="!u.unitName"
                          >
                            {{ u.unitName || '未命名' }}
                          </a-select-option>
                        </a-select>
                      </a-form-item>
                    </a-col>
                    <a-col :span="8">
                      <a-form-item
                        label="采购常用单位"
                        required
                      >
                        <a-select
                          v-model:value="form.defaultPurchaseUnitId"
                          placeholder="请选择"
                          size="small"
                          allow-clear
                        >
                          <a-select-option
                            v-for="u in unitList"
                            :key="u.id"
                            :value="u.id"
                            :disabled="!u.unitName"
                          >
                            {{ u.unitName || '未命名' }}
                          </a-select-option>
                        </a-select>
                      </a-form-item>
                    </a-col>
                    <a-col :span="8">
                      <a-form-item
                        label="库存单位"
                        required
                      >
                        <a-select
                          v-model:value="form.defaultStockUnitId"
                          placeholder="请选择"
                          size="small"
                          allow-clear
                        >
                          <a-select-option
                            v-for="u in unitList"
                            :key="u.id"
                            :value="u.id"
                            :disabled="!u.unitName"
                          >
                            {{ u.unitName || '未命名' }}
                          </a-select-option>
                        </a-select>
                      </a-form-item>
                    </a-col>
                  </a-row>
                </a-card>

                <!-- ═══ 选择单位组弹窗（来源：商品辅助资料 → 商品单位 → 单位组管理） ═══ -->
                <a-modal
                  v-model:open="unitGroupModalVisible"
                  title="单位组选择"
                  width="560px"
                  @ok="applyUnitGroup"
                >
                  <p style="color:#888;font-size:13px;margin-bottom:12px;">
                    选择单位组后按组内「小单位 / 中单位 / 大单位」重设本商品的多单位明细（含换算关系）。
                  </p>
                  <div style="display:flex;gap:8px;margin-bottom:8px;">
                    <a-input
                      v-model:value="unitGroupKeyword"
                      placeholder="请输入单位查询"
                      size="small"
                      style="width:200px"
                      allow-clear
                      @press-enter="loadUnitGroupList"
                    />
                    <a-button
                      type="primary"
                      size="small"
                      @click="loadUnitGroupList"
                    >
                      查询
                    </a-button>
                  </div>
                  <a-table
                    :data-source="unitGroupOptions"
                    :columns="unitGroupPickColumns"
                    :loading="unitGroupLoading"
                    :pagination="false"
                    :row-key="(r: any) => String(r.id)"
                    size="small"
                    :scroll="{ y: 260 }"
                    :custom-row="(record: any) => ({ onClick: () => (selectedUnitGroupId = String(record.id)) })"
                    :row-class-name="(record: any) => (String(record.id) === selectedUnitGroupId ? 'unit-group-row-active' : '')"
                  >
                    <template #bodyCell="{ column, record }">
                      <template v-if="column.key === 'pick'">
                        <a-radio :checked="String(record.id) === selectedUnitGroupId" />
                      </template>
                      <template v-else-if="column.key === 'unitNames'">
                        {{ record.unitNames || '-' }}
                      </template>
                      <template v-else-if="column.key === 'unitRates'">
                        {{ record.unitRates || '-' }}
                      </template>
                    </template>
                  </a-table>
                  <a-empty
                    v-if="!unitGroupLoading && unitGroupOptions.length === 0"
                    description="暂无单位组，请先在「商品辅助资料 → 商品单位 → 单位组管理」中维护"
                  />
                </a-modal>

                <!-- ═══ 批量价格计算弹窗 ═══ -->
                <a-modal
                  v-model:open="priceCalcModalVisible"
                  title="批量价格计算"
                  width="520px"
                  @ok="applyPriceCalc"
                >
                  <p style="color:#888;font-size:13px;margin-bottom:12px;">
                    按所选基准价与加价率重算「商品单位」明细中所有单位行的零售价 / 批发价 / 价格等级。
                  </p>
                  <a-form
                    layout="horizontal"
                    :label-col="{ span: 8 }"
                    :wrapper-col="{ span: 14 }"
                  >
                    <a-form-item label="计算基准价">
                      <a-select
                        v-model:value="priceCalc.baseField"
                        size="small"
                        style="width:100%"
                      >
                        <a-select-option
                          v-for="opt in priceCalcBaseOptions"
                          :key="opt.value"
                          :value="opt.value"
                        >
                          {{ opt.label }}
                        </a-select-option>
                      </a-select>
                    </a-form-item>
                    <a-form-item label="零售价加价率(%)">
                      <a-input-number
                        v-model:value="priceCalc.retailMarkup"
                        :min="0"
                        style="width:100%"
                        size="small"
                      />
                    </a-form-item>
                    <a-form-item label="批发价加价率(%)">
                      <a-input-number
                        v-model:value="priceCalc.wholesaleMarkup"
                        :min="0"
                        style="width:100%"
                        size="small"
                      />
                    </a-form-item>
                    <a-form-item label="价格等级折扣(%)">
                      <a-input-number
                        v-model:value="priceCalc.gradeDiscount"
                        :min="0"
                        :max="100"
                        style="width:100%"
                        size="small"
                      />
                      <span class="form-tip">0 表示不重算价格等级</span>
                    </a-form-item>
                    <a-form-item label="最低售价">
                      <a-checkbox v-model:checked="priceCalc.writeMinSalePrice">
                        同时写入最低售价 = 批发价
                      </a-checkbox>
                    </a-form-item>
                  </a-form>
                </a-modal>
              </div>
            </a-tab-pane>

            <!-- ==================== TAB 2: 商品图片 ==================== -->
            <a-tab-pane
              key="images"
              tab="商品图片"
            >
              <div class="tab-content">
                <!-- 商品图片（顺序上传） -->
                <a-card
                  title="商品图片"
                  class="create-card"
                  size="small"
                  :bordered="false"
                >
                  <template #extra>
                    <span class="image-hint">说明：首图为主图，建议尺寸720×720，最多支持5张，大小不超过10M</span>
                  </template>
                  <div class="image-space-bar">
                    <a-button
                      size="small"
                      @click="openImageSpaceModal"
                    >
                      <PictureOutlined /> 引用图片空间
                    </a-button>
                  </div>
                  <div class="image-upload-area">
                    <div
                      v-for="(img, idx) in imageFileList"
                      :key="img.url || idx"
                      class="image-upload-item"
                    >
                      <div class="image-upload-preview">
                        <img
                          :src="img.url"
                          :alt="img.name"
                        >
                        <div class="image-upload-mask">
                          <a-button
                            size="small"
                            danger
                            @click="handleImageRemove(img)"
                          >
                            删除
                          </a-button>
                        </div>
                      </div>
                      <div
                        v-if="idx === 0"
                        class="image-upload-main-tag"
                      >
                        主图
                      </div>
                    </div>
                    <div
                      v-if="imageFileList.length < 5"
                      class="image-upload-trigger"
                      @click="triggerImageUpload"
                    >
                      <UploadOutlined />
                      <div class="trigger-text">
                        点击上传图片
                      </div>
                    </div>
                  </div>
                  <input
                    ref="imageInputRef"
                    type="file"
                    accept="image/*"
                    style="display:none"
                    @change="onImageFileSelected"
                  >
                </a-card>

                <!-- 主图视频 -->
                <a-card
                  title="主图视频"
                  class="create-card"
                  size="small"
                  :bordered="false"
                >
                  <template #extra>
                    <span class="image-hint">说明：1. 格式：建议上传MP4格式，20M以内；2. 时长：建议一分钟以内的短视频；3. 内容：突出商品1-2个核心卖点。</span>
                  </template>
                  <div class="video-upload-area">
                    <div
                      v-if="form.videoUrl"
                      class="video-upload-preview"
                    >
                      <video
                        :src="form.videoUrl"
                        controls
                        style="max-width: 320px; max-height: 240px;"
                      />
                    </div>
                    <div
                      v-else
                      class="video-upload-trigger"
                    >
                      <div class="video-icon-placeholder">
                        <VideoCameraOutlined />
                      </div>
                    </div>
                    <div class="video-upload-actions">
                      <a-button
                        size="small"
                        @click="triggerVideoUpload"
                      >
                        <UploadOutlined /> 上传
                      </a-button>
                      <a-button
                        v-if="form.videoUrl"
                        size="small"
                        danger
                        @click="handleVideoRemove"
                      >
                        删除
                      </a-button>
                    </div>
                  </div>
                  <input
                    ref="videoInputRef"
                    type="file"
                    accept="video/mp4"
                    style="display:none"
                    @change="onVideoFileSelected"
                  >
                </a-card>

                <!-- ═══ 引用图片空间弹窗（对标「商品图片 → 引用图片空间」） ═══ -->
                <a-modal
                  v-model:open="imageSpaceModalVisible"
                  title="引用图片空间"
                  width="760px"
                  :confirm-loading="imageSpaceLoading"
                  @ok="applyImageSpace"
                >
                  <a-space style="margin-bottom:12px;">
                    <a-input
                      v-model:value="imageSpaceKeyword"
                      placeholder="按图片名称搜索"
                      size="small"
                      style="width: 220px"
                      allow-clear
                      @press-enter="loadImageSpace"
                    />
                    <a-button
                      size="small"
                      @click="loadImageSpace"
                    >
                      查询
                    </a-button>
                  </a-space>
                  <a-spin :spinning="imageSpaceLoading">
                    <div class="image-space-grid">
                      <div
                        v-for="m in imageSpaceList"
                        :key="m.id"
                        class="image-space-item"
                        :class="{ 'is-active': imageSpaceSelected.includes(m.id) }"
                        @click="toggleImageSpace(m.id)"
                      >
                        <img
                          :src="m.imageUrl"
                          :alt="m.imageName"
                        >
                        <div class="image-space-name">
                          {{ m.imageName || '未命名' }}
                        </div>
                      </div>
                    </div>
                    <a-empty
                      v-if="!imageSpaceLoading && imageSpaceList.length === 0"
                      description="图片空间暂无素材，请先在「资料 → 图片管理」上传"
                    />
                  </a-spin>
                </a-modal>
              </div>
            </a-tab-pane>

            <!-- ==================== TAB 3: 商城信息 ==================== -->
            <a-tab-pane
              key="mall"
              tab="商城信息"
            >
              <div class="tab-content">
                <!-- 商品上架 -->
                <a-card
                  title="商品上架"
                  class="create-card"
                  size="small"
                  :bordered="false"
                >
                  <a-checkbox
                    v-model:checked="isMallShelf"
                    @change="(e: any) => handleShelfChange(e.target.checked)"
                  >
                    立即上架
                  </a-checkbox>
                </a-card>

                <!-- 商城信息 -->
                <a-card
                  title="商城信息"
                  class="create-card"
                  size="small"
                  :bordered="false"
                >
                  <a-row :gutter="24">
                    <a-col :span="12">
                      <a-form-item label="商城显示标题">
                        <a-input
                          v-model:value="form.mallDisplayTitle"
                          placeholder="默认使用商品名称，可自定义"
                          size="small"
                        />
                      </a-form-item>
                    </a-col>
                    <a-col :span="12">
                      <a-form-item label="商品描述">
                        <a-input
                          v-model:value="form.mallDescription"
                          placeholder="简短的商品描述"
                          size="small"
                        />
                      </a-form-item>
                    </a-col>
                  </a-row>
                </a-card>

                <!-- 商品标签 -->
                <a-card
                  title="商品标签"
                  class="create-card"
                  size="small"
                  :bordered="false"
                >
                  <template #extra>
                    <span class="image-hint">商城首页标签商品显示位置与此处标签设置位置顺序相同</span>
                  </template>
                  <div class="tag-manage-area">
                    <div class="tag-list">
                      <div
                        v-for="tag in tagOptions"
                        :key="tag.tagCode"
                        class="tag-item"
                      >
                        <a-checkbox
                          :checked="selectedTags.includes(tag.tagCode as string)"
                          @change="(e: any) => handleTagChange(tag.tagCode as string, e.target.checked)"
                        >
                          {{ tag.tagName }}
                        </a-checkbox>
                      </div>
                      <a-empty
                        v-if="tagOptions.length === 0"
                        description="暂无启用的商品标签，可在「资料 → 商品辅助资料 → 商品标签」维护"
                      />
                    </div>
                  </div>
                </a-card>

                <!-- 其他设置 -->
                <a-card
                  title="其他"
                  class="create-card"
                  size="small"
                  :bordered="false"
                >
                  <a-row :gutter="24">
                    <a-col :span="8">
                      <a-form-item label="排序方式">
                        <a-select
                          v-model:value="form.mallSortType"
                          size="small"
                          style="width:100%"
                        >
                          <a-select-option value="DEFAULT">
                            默认
                          </a-select-option>
                          <a-select-option value="SALES">
                            按销量
                          </a-select-option>
                          <a-select-option value="MANUAL">
                            手动排序
                          </a-select-option>
                        </a-select>
                      </a-form-item>
                    </a-col>
                    <a-col :span="8">
                      <a-form-item label="排序值">
                        <a-input-number
                          v-model:value="form.mallSortOrder"
                          :min="0"
                          style="width:100%"
                          size="small"
                        />
                      </a-form-item>
                    </a-col>
                    <a-col :span="8">
                      <a-form-item label="起订量">
                        <a-input-number
                          v-model:value="form.mallMinOrderQty"
                          :min="0"
                          style="width:100%"
                          size="small"
                        />
                      </a-form-item>
                    </a-col>
                    <a-col :span="8">
                      <a-form-item label="限购量">
                        <a-input-number
                          v-model:value="form.mallPurchaseLimit"
                          :min="0"
                          style="width:100%"
                          size="small"
                        />
                        <span class="form-tip">0表示不限购</span>
                      </a-form-item>
                    </a-col>
                    <a-col :span="8">
                      <a-form-item label="商品积分">
                        <a-input-number
                          v-model:value="form.mallPoints"
                          :min="0"
                          style="width:100%"
                          size="small"
                        />
                      </a-form-item>
                    </a-col>
                    <a-col :span="8">
                      <a-form-item label="关键字">
                        <a-input
                          v-model:value="form.keywords"
                          placeholder="商城检索关键字，逗号分隔"
                          size="small"
                        />
                      </a-form-item>
                    </a-col>
                  </a-row>
                </a-card>

                <!-- 商品详情页图文编辑 -->
                <a-card
                  title="商品详情页图文编辑"
                  class="create-card"
                  size="small"
                  :bordered="false"
                >
                  <div class="rich-editor">
                    <a-textarea
                      v-model:value="form.richTextDetail"
                      :rows="10"
                      placeholder="请输入商品详情HTML内容"
                    />
                  </div>
                </a-card>

                <!-- 推荐商品 -->
                <a-card
                  title="推荐商品"
                  class="create-card"
                  size="small"
                  :bordered="false"
                >
                  <template #extra>
                    <span class="image-hint">温馨提示：1.关联商品最多添加12条 2.已经添加的关联商品将在商品详情页面的【相关推荐】模块做显示</span>
                  </template>
                  <vxe-table
                    :data="recommendList"
                    border
                    size="small"
                    max-height="350"
                    align="center"
                  >
                    <vxe-column
                      type="seq"
                      title="#"
                      width="50"
                    />
                    <vxe-column
                      title="操作"
                      width="70"
                    >
                      <template #default="{ row }">
                        <a-button
                          type="link"
                          size="small"
                          title="选择商品"
                          @click="openRecommendSelect(row)"
                        >
                          <PlusCircleOutlined />
                        </a-button>
                        <a-button
                          type="link"
                          size="small"
                          danger
                          title="删除"
                          @click="removeRecommend(row)"
                        >
                          <CloseCircleOutlined />
                        </a-button>
                      </template>
                    </vxe-column>
                    <vxe-column
                      field="recommendProductName"
                      title="商品名称"
                      min-width="140"
                    />
                    <vxe-column
                      field="recommendProductCode"
                      title="货号"
                      width="100"
                    />
                    <vxe-column
                      field="recommendProductModel"
                      title="型号"
                      width="100"
                    />
                    <vxe-column
                      field="recommendProductSpec"
                      title="规格"
                      width="100"
                    />
                    <vxe-column
                      field="recommendProductUnit"
                      title="商品单位"
                      width="80"
                    />
                    <vxe-column
                      field="recommendProductOrigin"
                      title="产地"
                      width="100"
                    />
                    <vxe-column
                      field="recommendProductBrand"
                      title="品牌"
                      width="100"
                    />
                    <vxe-column
                      title="排序值"
                      width="80"
                    >
                      <template #default="{ row }">
                        <a-input-number
                          v-model:value="row.sortOrder"
                          :min="1"
                          size="small"
                          style="width: 60px"
                        />
                      </template>
                    </vxe-column>
                  </vxe-table>
                </a-card>
              </div>
            </a-tab-pane>
          </a-tabs>
        </a-spin>
      </div>

      <!-- 推荐商品选择弹窗 -->
      <a-modal
        v-model:open="recommendModalVisible"
        title="选择推荐商品"
        width="700px"
        @ok="confirmRecommend"
      >
        <a-table
          :data-source="productOptions"
          :columns="recommendProductColumns"
          row-key="id"
          :row-selection="{ type: 'radio', selectedRowKeys: recommendSelectedKeys, onChange: onRecommendSelectChange }"
          :pagination="{ pageSize: 5 }"
          size="small"
          :scroll="{ y: 300 }"
        />
      </a-modal>
    </div>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted, nextTick, defineOptions } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  LeftOutlined, SaveOutlined, DownOutlined, PlusOutlined, UploadOutlined,
  ApartmentOutlined, CalculatorOutlined, VideoCameraOutlined, CrownOutlined,
  CloseOutlined, PlusCircleOutlined, CloseCircleOutlined, PictureOutlined,
  ClearOutlined
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import {
  productCategoryApi,
  productApi,
  productFormApi,
  productRecommendApi,
  productGradeApi,
  productUnitDictApi,
  productUnitGroupApi,
  mallTagApi,
  type MallTag,
  type Product,
  type ProductCategory,
  type ProductUnit,
  type ProductGrade,
  type ProductRecommend
} from '@/api/erp/product'
import { productImageApi, type ProductImageMaterial } from '@/api/erp/productImage'
import request from '@/utils/request'

import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import { DEFAULT_GRADE_NAMES, gradeNameOfLevel } from './columns'

import { useTabsStore } from '@/stores/tabs'

defineOptions({ name: 'ProductForm' })

const route = useRoute()
const router = useRouter()
const tabsStore = useTabsStore()

// ── 状态 ──
const loading = ref(false)
const saving = ref(false)
const activeTab = ref('basic')
const isEdit = ref(false)
// 雪花 ID 超出 JS 安全整数范围（> 2^53），必须保持字符串；Number() 会丢精度导致按 id 查不到商品
const productId = ref<string>('')

// ── 基础数据 ──
const categoryTree = ref<ProductCategory[]>([])
const quickSearch = ref('')

// ── 表单数据 ──
const form = reactive({
  productName: '',
  productCode: '',
  productCodeAlias: '',
  spec: '',
  model: '',
  unit: '',
  categoryId: undefined as number | undefined,
  barcode: '',
  sku: '',
  productType: 'SINGLE',
  status: 'ENABLED',
  remark: '',
  origin: '',
  brand: '',
  industryCategory: '其他',
  shelfLifeDays: undefined as number | undefined,
  nearExpiryDays: undefined as number | undefined,
  isBatchExpiryManaged: 0,
  isStandardProduct: 1,
  useCoupon: 0,
  defaultSalesUnitId: undefined as number | undefined,
  defaultPurchaseUnitId: undefined as number | undefined,
  defaultStockUnitId: undefined as number | undefined,
  mallDisplayTitle: '',
  mallDescription: '',
  mallTags: '',
  mallShelfStatus: 0,
  mallSortType: 'DEFAULT',
  mallSortOrder: 0,
  mallMinOrderQty: 0,
  mallPurchaseLimit: 0,
  mallPoints: undefined as number | undefined,
  keywords: '',
  richTextDetail: '',
  imageUrl: '',
  videoUrl: '',
  costPrice: undefined as number | undefined,
  standardPrice: undefined as number | undefined,
  wholesalePrice: undefined as number | undefined,
  weight: undefined as number | undefined,
  volume: undefined as number | undefined,
  taxRate: 13,
  purchasePrice: undefined as number | undefined,
  retailPrice: undefined as number | undefined
})

const isMallShelf = ref(false)

// ── 商品标签 ──
// 数据源「资料 → 商品辅助资料 → 商品标签」：标准槽位 TAG_1..TAG_20 + 用户自定义昵称。
// 勾选值与商品侧存储都是**槽位编码**，昵称（默认「标签1…标签20」，可改成业务别名）仅用于显示。
const tagOptions = ref<MallTag[]>([])
const selectedTags = ref<string[]>([])

async function loadTags() {
  try {
    const list = await mallTagApi.list()
    tagOptions.value = (Array.isArray(list) ? list : [])
      .filter(t => t.status === 1 && t.tagCode)
      .sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0))
  } catch {
    tagOptions.value = []
  }
}

function handleTagChange(tagCode: string, checked: boolean) {
  if (checked) {
    if (!selectedTags.value.includes(tagCode)) {
      selectedTags.value.push(tagCode)
    }
  } else {
    selectedTags.value = selectedTags.value.filter(t => t !== tagCode)
  }
  form.mallTags = selectedTags.value.join(',')
}

// ── 单位表格 ─
/** 数据表默认预留的行数：小单位 / 中单位 / 大单位（表头 + 3 行） */
const UNIT_MIN_ROWS = 3

/** 前三个槽位是固定单位类型；第 4 行起为扩展槽位 UNIT_4 → 「单位4」，依次类推 */
const FIXED_UNIT_TYPE_OPTIONS = [
  { label: '小单位', value: 'SMALL' },
  { label: '中单位', value: 'MEDIUM' },
  { label: '大单位', value: 'LARGE' },
]

/** 第 N 个槽位的类型默认值（1/2/3 → 小/中/大单位；N≥4 → UNIT_N） */
function defaultUnitTypeAt(slot: number): string {
  if (slot === 1) return 'SMALL'
  if (slot === 2) return 'MEDIUM'
  if (slot === 3) return 'LARGE'
  return `UNIT_${slot}`
}

/** 扩展槽位下拉项：按当前行数动态补齐「单位4/单位5…」 */
const unitTypeOptions = computed(() => {
  const extra: { label: string; value: string }[] = []
  for (let i = 4; i <= Math.max(4, unitList.value.length); i++) {
    extra.push({ label: `单位${i}`, value: `UNIT_${i}` })
  }
  return [...FIXED_UNIT_TYPE_OPTIONS, ...extra]
})

// ── 价格等级（从API动态加载，最多8个） ──
const gradeList = ref<ProductGrade[]>([])
const gradePriceFieldMap = [
  'gradePrice1', 'gradePrice2', 'gradePrice3', 'gradePrice4',
  'gradePrice5', 'gradePrice6', 'gradePrice7', 'gradePrice8'
]

const gradePriceColumns = computed(() => {
  // 固定 8 个标准价格等级槽位（grade_price_1..8 ↔ GRADE_1..8）；
  // 列标题取用户自定义昵称，未配置时回落标准名「价格等级N」，与接口返回顺序无关
  return gradePriceFieldMap.map((field, i) => ({
    field,
    title: gradeNameOfLevel(gradeList.value, i + 1),
  }))
})

async function loadGrades() {
  try {
    const list = await productGradeApi.list()
    if (list && list.length > 0) {
      gradeList.value = list
    } else {
      // API 返回空数据时使用默认等级名称
      gradeList.value = buildDefaultGrades()
    }
  } catch {
    // API 调用失败时使用默认等级名称
    gradeList.value = buildDefaultGrades()
  }
}

/** 构建标准 8 个价格等级（接口无数据时的兜底，昵称回落标准名） */
function buildDefaultGrades(): ProductGrade[] {
  return DEFAULT_GRADE_NAMES.map((name, i) => ({
    id: i + 1,
    gradeCode: `GRADE_${i + 1}`,
    gradeName: name,
    gradeLevel: i + 1,
    sortOrder: i + 1,
    status: 1,
  }))
}

// ── 单位组（选择单位组 → 带出小/中/大单位与换算关系） ──
const unitGroupModalVisible = ref(false)
const unitGroupOptions = ref<any[]>([])
const unitGroupKeyword = ref('')
const unitGroupLoading = ref(false)
const selectedUnitGroupId = ref<string>('')

/** 单位组选择表格列（对标单位组管理的「单位 / 单位关系」） */
const unitGroupPickColumns = [
  { key: 'pick', title: '', width: 48 },
  { key: 'unitNames', title: '单位' },
  { key: 'unitRates', title: '单位关系', width: 140 },
]

async function showUnitGroupDialog() {
  unitGroupModalVisible.value = true
  unitGroupKeyword.value = ''
  selectedUnitGroupId.value = ''
  await loadUnitGroupList()
  // 预选：当前单位明细与某单位组的单位串完全一致时默认选中
  const currentNames = unitList.value.map(u => u.unitName).filter(Boolean).join(',')
  const hit = currentNames
    ? unitGroupOptions.value.find((g: any) => (g.unitNames || '') === currentNames)
    : null
  if (hit) selectedUnitGroupId.value = String(hit.id)
}

async function loadUnitGroupList() {
  unitGroupLoading.value = true
  try {
    const res: any = await productUnitGroupApi.page({
      pageNum: 1,
      pageSize: 50,
      status: 1,
      ...(unitGroupKeyword.value ? { keyword: unitGroupKeyword.value } : {}),
    })
    unitGroupOptions.value = res?.records || []
  } catch (e) {
    console.error('[商品表单] 加载单位组失败', e)
    unitGroupOptions.value = []
  } finally {
    unitGroupLoading.value = false
  }
}

function applyUnitGroup() {
  const group = unitGroupOptions.value.find((g: any) => String(g.id) === selectedUnitGroupId.value)
  if (!group) {
    message.warning('请选择一个单位组')
    return
  }
  const items: any[] = group.items || []
  if (items.length === 0) {
    message.warning('该单位组未配置单位')
    return
  }
  // 按单位组重设多单位明细：第 1 个槽位=基本单位（换算关系 1），其余按槽位带出类型
  unitList.value = items.map((item: any, idx: number) => ({
    id: 0,
    productId: 0,
    unitName: item.unitName,
    isBaseUnit: item.unitType === 'SMALL' || (!item.unitType && idx === 0) ? 1 : 0,
    conversionRate: Number(item.conversionRate ?? 1),
    barcode: '',
    sortOrder: idx + 1,
    unitType: item.unitType || defaultUnitTypeAt(idx + 1),
  } as any))

  unitGroupModalVisible.value = false
  message.success(`已按单位组「${group.unitNames || ''}」重设 ${unitList.value.length} 个单位`)
}

const unitList = ref<ProductUnit[]>([])

function addUnitRow() {
  // 新增模式：默认 3 行固定槽位 —— 小单位(基本单位) / 中单位 / 大单位
  for (let i = 1; i <= UNIT_MIN_ROWS; i++) {
    unitList.value.push({
      id: 0,
      productId: 0,
      unitName: '',
      isBaseUnit: i === 1 ? 1 : 0,
      conversionRate: i === 1 ? 1 : undefined,
      barcode: '',
      sortOrder: i,
      unitType: defaultUnitTypeAt(i),
    } as any)
  }
}

/** 补齐到默认 3 行固定槽位（小/中/大）：编辑只有 1~2 个单位的商品时也要占满 3 行并预填类型 */
function ensureUnitRows() {
  for (let slot = unitList.value.length + 1; slot <= UNIT_MIN_ROWS; slot++) {
    unitList.value.push({
      id: 0,
      productId: 0,
      unitName: '',
      isBaseUnit: 0,
      conversionRate: undefined,
      barcode: '',
      sortOrder: slot,
      unitType: defaultUnitTypeAt(slot),
    } as any)
  }
  // 首行必须是基本单位（换算关系缺省为 1）
  const first: any = unitList.value[0]
  if (first && first.isBaseUnit !== 1) {
    first.isBaseUnit = 1
    if (first.conversionRate == null) first.conversionRate = 1
  }
}

/** 第 4 行起且为最后一行时才可删除（必须从最后一行依次往上删） */
function canRemoveUnitRow(index: number): boolean {
  return index >= UNIT_MIN_ROWS && index === unitList.value.length - 1
}

function removeUnitRow(index: number) {
  if (index < UNIT_MIN_ROWS) {
    // 前三行是固定槽位，不能删行
    message.warning('小/中/大单位为固定槽位，只能清空数据，不能删除行')
    return
  }
  if (!canRemoveUnitRow(index)) {
    message.warning('请从最后一行开始依次删除')
    return
  }
  unitList.value.splice(index, 1)
  // 更新排序
  unitList.value.forEach((u, i) => { u.sortOrder = i + 1 })
}

/** 清空一行的数据（保留单位类型槽位与基本单位标记），用于小/中/大单位行 */
function clearUnitRow(index: number) {
  const row: any = unitList.value[index]
  if (!row) return
  const keepType = row.unitType
  const keepBase = row.isBaseUnit
  Object.keys(row).forEach(k => {
    if (['id', 'productId', 'tenantId', 'sortOrder', 'unitType', 'isBaseUnit'].includes(k)) return
    row[k] = undefined
  })
  row.unitType = keepType
  row.isBaseUnit = keepBase
  unitList.value = [...unitList.value]
  message.success('已清空该单位行数据')
}

/** 新增单行：类型列按槽位预填「单位N」（第 4 行 → 单位4，删掉再加仍为 单位4） */
function addSingleUnitRow() {
  const slot = unitList.value.length + 1
  unitList.value.push({
    id: 0,
    productId: 0,
    unitName: '',
    isBaseUnit: 0,
    conversionRate: 1,
    barcode: '',
    sortOrder: slot,
    unitType: defaultUnitTypeAt(slot),
  } as any)
}

// ── 数据表区域高度：默认预留「表头 + 3 行」，行数增减时容器高度同步自适应 ──
const UNIT_HEADER_HEIGHT_FALLBACK = 38
const UNIT_ROW_HEIGHT_FALLBACK = 33
const unitHeaderHeight = ref(UNIT_HEADER_HEIGHT_FALLBACK)
const unitRowHeight = ref(UNIT_ROW_HEIGHT_FALLBACK)

/** 横向滚动条 + 表格描边余量：不留会让内容高度刚好等于容器高而被判成「需展开」，末行被折叠 */
const UNIT_TABLE_EXTRA = 12
const unitTableHeight = computed(() =>
  unitHeaderHeight.value + Math.max(UNIT_MIN_ROWS, unitList.value.length) * unitRowHeight.value + UNIT_TABLE_EXTRA)

/** 按实际渲染尺寸校准表头/行高（主题字号变化也能自适应） */
function measureUnitTable() {
  const head = document.querySelector('.unit-table-wrap .ss-grid thead') as HTMLElement | null
  const row = document.querySelector('.unit-table-wrap .ss-grid tbody tr') as HTMLElement | null
  const hh = head ? Math.round(head.getBoundingClientRect().height) : 0
  const rh = row ? Math.round(row.getBoundingClientRect().height) : 0
  if (hh > 10) unitHeaderHeight.value = hh
  if (rh > 10) unitRowHeight.value = rh
}

/** 列定义（BillDetailTable 格式） */
const unitColumns = computed<DetailColumnConfig[]>(() => {
  const baseColumns: DetailColumnConfig[] = [
    { key: 'rowNo', type: 'rowNo', title: '序号', width: 50, fixed: 'left' },
    { key: 'unitType', type: 'select', title: '类型', width: 90, options: unitTypeOptions.value, placeholder: '选择类型' },
    { key: 'unitName', type: 'input', title: '单位名称', width: 100, placeholder: '如: 袋/箱/件' },
    { key: 'isBaseUnit', type: 'slot', title: '单位关系', width: 100, slotName: 'isBaseUnitCell' },
    { key: 'conversionRate', type: 'number', title: '换算关系', width: 100, precision: 6, min: 0.000001 },
    { key: 'barcode', type: 'input', title: '条码', width: 120 },
    { key: 'presetPurchasePrice', type: 'number', title: '预设进价', width: 100, precision: 2, min: 0 },
    { key: 'referenceCost', type: 'number', title: '参考成本', width: 100, precision: 2, min: 0 },
    { key: 'recentPurchasePrice', type: 'number', title: '最近进价', width: 100, precision: 2, min: 0 },
    { key: 'wholesalePrice', type: 'number', title: '批发价', width: 100, precision: 2, min: 0 },
    { key: 'retailPrice', type: 'number', title: '零售价', width: 100, precision: 2, min: 0 },
    { key: 'minSalePrice', type: 'number', title: '最低售价', width: 100, precision: 2, min: 0 },
    { key: 'minDiscount', type: 'number', title: '最低折扣(%)', width: 110, precision: 2, min: 0, max: 100 },
  ]
  // 等级价格列
  const gradeCols: DetailColumnConfig[] = gradePriceColumns.value.map(g => ({
    key: g.field,
    title: g.title,
    type: 'number' as const,
    width: 100,
    precision: 2,
    min: 0,
  }))
  // 重量 / 体积（单位级，对标商品单位明细表末尾两列）
  const measureCols: DetailColumnConfig[] = [
    { key: 'weight', type: 'number', title: '重量（kg）', width: 100, precision: 4, min: 0 },
    { key: 'volume', type: 'number', title: '体积（m³）', width: 100, precision: 6, min: 0 },
  ]
  // 操作列
  const actionCol: DetailColumnConfig = {
    key: 'action',
    type: 'action',
    title: '操作',
    width: 50,
    fixed: 'right',
    slotName: 'actionCell',
  }
  return [...baseColumns, ...gradeCols, ...measureCols, actionCol]
})

/** 单元格变更回调（BillDetailTable 已直接修改 record，仅用于触发响应式） */
function onUnitCellChange(_record: any, _fieldKey: string, _value: any) {
  // 触发 unitList 的响应式更新
  unitList.value = [...unitList.value]
}

function openGradeManage() {
  const routeData = router.resolve({ path: '/erp/product/grade' })
  window.open(routeData.href, '_blank')
}

// ── 批量价格计算（对标 ql361 商品单位「批量价格计算」）──
const priceCalcModalVisible = ref(false)
const priceCalc = reactive({
  baseField: 'presetPurchasePrice',
  retailMarkup: 30,
  wholesaleMarkup: 20,
  gradeDiscount: 0,
  writeMinSalePrice: true,
})

const priceCalcBaseOptions = [
  { label: '预设进价', value: 'presetPurchasePrice' },
  { label: '参考成本', value: 'referenceCost' },
  { label: '最近进价', value: 'recentPurchasePrice' },
]

function batchCalcPrice() {
  if (unitList.value.length === 0) {
    message.warning('请先添加商品单位')
    return
  }
  priceCalcModalVisible.value = true
}

/** 按「基准价 × (1 + 加价率)」重算所有单位行的零售价/批发价/价格等级 */
function applyPriceCalc() {
  let changed = 0
  unitList.value = unitList.value.map((row: any) => {
    const base = Number(row[priceCalc.baseField] ?? 0)
    if (!base) return row
    const round2 = (n: number) => Math.round(n * 100) / 100
    const retail = round2(base * (1 + Number(priceCalc.retailMarkup ?? 0) / 100))
    const wholesale = round2(base * (1 + Number(priceCalc.wholesaleMarkup ?? 0) / 100))
    const updated: any = { ...row, retailPrice: retail, wholesalePrice: wholesale }
    if (priceCalc.writeMinSalePrice) {
      updated.minSalePrice = wholesale
    }
    if (Number(priceCalc.gradeDiscount) > 0) {
      const factor = 1 - Number(priceCalc.gradeDiscount) / 100
      gradePriceFieldMap.forEach((field, i) => {
        if (row[field] != null && row[field] !== '') {
          updated[field] = round2(retail * factor)
        }
      })
    }
    changed++
    return updated
  })
  if (!changed) {
    message.warning('所选基准价在各单位行均为空，未计算')
    return
  }
  message.success(`已按基准价重算 ${changed} 个单位行的价格`)
  priceCalcModalVisible.value = false
}

// ── 图片上传（顺序逐个，最多5张） ──
const imageFileList = ref<{ name: string; url: string; status: string; materialId?: string }[]>([])

// ── 引用图片空间（对标「商品图片 → 引用图片空间」）──
const imageSpaceModalVisible = ref(false)
const imageSpaceLoading = ref(false)
const imageSpaceKeyword = ref('')
const imageSpaceList = ref<ProductImageMaterial[]>([])
const imageSpaceSelected = ref<string[]>([])

async function loadImageSpace() {
  imageSpaceLoading.value = true
  try {
    const res: any = await productImageApi.spacePage({
      keyword: imageSpaceKeyword.value || undefined,
      onlyImage: 1,
      pageNum: 1,
      pageSize: 60,
    })
    imageSpaceList.value = res?.records || []
  } catch {
    imageSpaceList.value = []
  } finally {
    imageSpaceLoading.value = false
  }
}

function openImageSpaceModal() {
  imageSpaceSelected.value = []
  imageSpaceModalVisible.value = true
  loadImageSpace()
}

function toggleImageSpace(id: string) {
  const i = imageSpaceSelected.value.indexOf(id)
  if (i >= 0) {
    imageSpaceSelected.value.splice(i, 1)
    return
  }
  if (imageFileList.value.length + imageSpaceSelected.value.length >= 5) {
    message.warning('商品图片最多支持 5 张')
    return
  }
  imageSpaceSelected.value.push(id)
}

/** 把选中的图片空间素材引用到当前商品（保存时写入绑定关系） */
function applyImageSpace() {
  if (!imageSpaceSelected.value.length) {
    imageSpaceModalVisible.value = false
    return
  }
  const picked = imageSpaceList.value.filter(m => imageSpaceSelected.value.includes(m.id))
  picked.forEach(m => {
    if (imageFileList.value.length >= 5) return
    imageFileList.value.push({ name: m.imageName || '图片空间', url: m.imageUrl, status: 'done', materialId: m.id })
  })
  if (!form.imageUrl && imageFileList.value[0]) {
    form.imageUrl = imageFileList.value[0].url
  }
  message.success(`已引用 ${picked.length} 张图片空间素材`)
  imageSpaceSelected.value = []
  imageSpaceModalVisible.value = false
}

/** 保存后把引用的图片空间素材绑定到该商品（首张为主图） */
async function bindImageMaterials(pid: number | string) {
  const items = imageFileList.value.filter((f: any) => f.materialId)
  for (let i = 0; i < items.length; i++) {
    try {
      await productImageApi.bind({
        imageId: String((items[i] as any).materialId),
        productId: String(pid),
        isMain: i === 0 ? 1 : 0,
      })
    } catch {
      // 绑定失败不阻断商品保存
    }
  }
}
const imageInputRef = ref<HTMLInputElement>()

function triggerImageUpload() {
  imageInputRef.value?.click()
}

async function onImageFileSelected(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  input.value = '' // 重置，允许重复选择同名文件

  const isLt10M = file.size / 1024 / 1024 < 10
  if (!isLt10M) {
    message.error('图片不能超过10M')
    return
  }
  if (!file.type.startsWith('image/')) {
    message.error('请选择图片文件')
    return
  }

  try {
    const formData = new FormData()
    formData.append('file', file)
    const res = await request.post('/file/upload', formData)
    const url = typeof res === 'string' ? res : (res as any).url || res
    if (!form.imageUrl) {
      form.imageUrl = url
    }
    imageFileList.value.push({ name: file.name, url, status: 'done' })
    message.success('上传成功')
  } catch {
    message.error('上传失败')
  }
}

function handleImageRemove(img: { url: string }) {
  const idx = imageFileList.value.findIndex(f => f.url === img.url)
  if (idx >= 0) {
    imageFileList.value.splice(idx, 1)
    if (form.imageUrl === img.url) {
      form.imageUrl = imageFileList.value[0]?.url || ''
    }
  }
}

// ── 视频上传 ──
const videoInputRef = ref<HTMLInputElement>()

function triggerVideoUpload() {
  videoInputRef.value?.click()
}

async function onVideoFileSelected(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  input.value = ''

  const isLt20M = file.size / 1024 / 1024 < 20
  if (!isLt20M) {
    message.error('视频不能超过20M')
    return
  }
  if (file.type !== 'video/mp4') {
    message.error('仅支持MP4格式')
    return
  }

  try {
    const formData = new FormData()
    formData.append('file', file)
    const res = await request.post('/file/upload', formData)
    const url = typeof res === 'string' ? res : (res as any).url || res
    form.videoUrl = url
    message.success('视频上传成功')
  } catch {
    message.error('视频上传失败')
  }
}

function handleVideoRemove() {
  form.videoUrl = ''
  message.success('视频已删除')
}

// ── 推荐商品 ──
const recommendList = ref<ProductRecommend[]>([])
const recommendModalVisible = ref(false)
const productOptions = ref<Product[]>([])
const recommendSelectedKeys = ref<number[]>([])
let currentEditRecommendIdx = -1

const recommendProductColumns = [
  { title: '编码', dataIndex: 'productCode', key: 'productCode', width: 120 },
  { title: '名称', dataIndex: 'productName', key: 'productName', ellipsis: true },
  { title: '规格', dataIndex: 'spec', key: 'spec', width: 100 },
  { title: '单位', dataIndex: 'unit', key: 'unit', width: 60 }
]

function openRecommendSelect(row: ProductRecommend) {
  currentEditRecommendIdx = recommendList.value.findIndex(r => r === row)
  recommendSelectedKeys.value = row.recommendProductId ? [row.recommendProductId] : []
  productApi.page({ pageNum: 1, pageSize: 200 } as any).then(res => {
    productOptions.value = (res as any).records || []
  })
  recommendModalVisible.value = true
}

function onRecommendSelectChange(keys: number[]) {
  recommendSelectedKeys.value = keys
}

function confirmRecommend() {
  if (recommendSelectedKeys.value.length === 0) {
    message.warning('请选择商品')
    return
  }
  const selectedId = recommendSelectedKeys.value[0]
  const opt = productOptions.value.find(p => p.id === selectedId)
  if (!opt) return

  if (currentEditRecommendIdx >= 0) {
    // 更新已有推荐
    recommendList.value[currentEditRecommendIdx] = {
      ...recommendList.value[currentEditRecommendIdx],
      recommendProductId: opt.id,
      recommendProductCode: opt.productCode,
      recommendProductName: opt.productName,
      recommendProductSpec: opt.spec,
      recommendProductModel: opt.model,
      recommendProductUnit: opt.unit,
      recommendProductOrigin: opt.origin,
      recommendProductBrand: opt.brand
    }
  } else {
    // 新增推荐（限制12个）
    if (recommendList.value.length >= 12) {
      message.warning('最多推荐12个商品')
      recommendModalVisible.value = false
      return
    }
    const maxSort = Math.max(...recommendList.value.map(r => r.sortOrder || 0), 0)
    recommendList.value.push({
      id: 0,
      productId: 0,
      recommendProductId: opt.id,
      sortOrder: maxSort + 1,
      recommendProductCode: opt.productCode,
      recommendProductName: opt.productName,
      recommendProductSpec: opt.spec,
      recommendProductModel: opt.model,
      recommendProductUnit: opt.unit,
      recommendProductOrigin: opt.origin,
      recommendProductBrand: opt.brand
    })
  }
  recommendModalVisible.value = false
  recommendSelectedKeys.value = []
  currentEditRecommendIdx = -1
}

function removeRecommend(row: ProductRecommend) {
  const idx = recommendList.value.findIndex(r => r.recommendProductId === row.recommendProductId)
  if (idx >= 0) recommendList.value.splice(idx, 1)
}

// ── 初始化 ──
async function init() {
  loading.value = true
  try {
    if (route.params.id) {
      isEdit.value = true
      productId.value = String(route.params.id)
    }

    // 并行加载：分类树 + 价格等级 + 商品标签字典
    const [cats] = await Promise.all([
      productCategoryApi.getTree(),
      loadGrades(),
      loadTags()
    ])
    categoryTree.value = cats
    if (isEdit.value && productId.value) {
      await loadProduct(productId.value)
    } else {
      addUnitRow()
    }
  } catch (e: any) {
    message.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

async function loadProduct(id: number | string) {
  try {
    const formData = await productFormApi.getById(id)
    const prod = formData.product
    Object.assign(form, {
      productName: prod.productName,
      productCode: prod.productCode,
      productCodeAlias: prod.productCodeAlias || '',
      spec: prod.spec || '',
      model: prod.model || '',
      categoryId: prod.categoryId,
      barcode: prod.barcode || '',
      sku: prod.sku || '',
      productType: prod.productType || 'SINGLE',
      status: prod.status || 'ENABLED',
      remark: prod.remark || '',
      origin: prod.origin || '',
      brand: prod.brand || '',
      industryCategory: prod.industryCategory || '其他',
      shelfLifeDays: prod.shelfLifeDays,
      nearExpiryDays: prod.nearExpiryDays,
      isBatchExpiryManaged: prod.isBatchExpiryManaged || 0,
      isStandardProduct: prod.isStandardProduct ?? 1,
      useCoupon: prod.useCoupon || 0,
      defaultSalesUnitId: prod.defaultSalesUnitId,
      defaultPurchaseUnitId: prod.defaultPurchaseUnitId,
      defaultStockUnitId: prod.defaultStockUnitId,
      mallDisplayTitle: prod.mallDisplayTitle || '',
      mallDescription: prod.mallDescription || '',
      mallTags: prod.mallTags || '',
      mallShelfStatus: prod.mallShelfStatus || 0,
      mallSortType: prod.mallSortType || 'DEFAULT',
      mallPoints: prod.mallPoints,
      keywords: prod.keywords || '',
      mallSortOrder: prod.mallSortOrder || 0,
      mallMinOrderQty: prod.mallMinOrderQty || 0,
      mallPurchaseLimit: prod.mallPurchaseLimit || 0,
      richTextDetail: prod.richTextDetail || '',
      imageUrl: prod.imageUrl || '',
      videoUrl: prod.videoUrl || '',
      costPrice: prod.costPrice,
      standardPrice: prod.standardPrice,
      wholesalePrice: prod.wholesalePrice,
      weight: prod.weight,
      volume: prod.volume,
      taxRate: prod.taxRate ?? 13,
      purchasePrice: prod.purchasePrice,
      retailPrice: prod.retailPrice
    })

    isMallShelf.value = form.mallShelfStatus === 1
    if (form.mallTags) {
      selectedTags.value = form.mallTags.split(',').filter(Boolean)
    }

    // 回填单位；历史数据缺类型时按槽位补默认值（1/2/3 → 小/中/大单位，N≥4 → UNIT_N）
    unitList.value = formData.units.map((u, idx) => ({
      ...u,
      unitType: u.unitType || defaultUnitTypeAt(idx + 1),
    }))
    // 商品只有 1~2 个单位时也要占满默认 3 行：补齐小/中/大固定槽位并预填类型
    ensureUnitRows()
    recommendList.value = formData.recommends.map(r => ({ ...r }))

    // 加载图片列表
    if (form.imageUrl) {
      imageFileList.value = [{ name: '主图', url: form.imageUrl, status: 'done' }]
    }
    // 编辑模式下，如果后端返回了多张图片的URL，解析加载
    if (form.imageUrl && !imageFileList.value.length) {
      imageFileList.value = [{ name: '主图', url: form.imageUrl, status: 'done' }]
    }
  } catch (e: any) {
    message.error('加载商品信息失败')
  }
}

// ── 快速检索 ──
function handleQuickSearch(value: string) {
  if (!value) return
  productApi.page({ keyword: value, pageNum: 1, pageSize: 20 } as any).then((res: any) => {
    if (res.records && res.records.length > 0) {
      const p = res.records[0]
      message.info(`已匹配到: ${p.productName}，可参考其信息`)
    } else {
      message.info('未找到匹配商品')
    }
  })
}

function showFieldConfig() {
  message.info('商品字段配置功能待完善')
}

function handleShelfChange(checked: boolean) {
  form.mallShelfStatus = checked ? 1 : 0
}

// ── 保存 ──
function handleSave() {
  saveAction('save')
}

function handleSaveMenu(e: any) {
  saveAction(e.key as string)
}

async function saveAction(action: string) {
  if (!form.productName) { message.warning('请输入商品名称'); return }
  if (!form.categoryId) { message.warning('请选择所属分类'); return }
  if (!form.industryCategory) { message.warning('请选择所属行业类别'); return }
  // 表格恒占 3 行（含占位行），以「填写了单位名称的行」为有效单位口径
  const validUnits = unitList.value.filter((u: any) => String(u.unitName || '').trim())
  if (validUnits.length === 0) { message.warning('请至少填写一个商品单位'); return }
  if (!form.defaultSalesUnitId) { message.warning('请选择销售常用单位'); return }

  if (!form.productCodeAlias) {
    form.productCodeAlias = `SP${Date.now().toString(36).toUpperCase()}`
  }

  saving.value = true
  try {
    const productPayload: Partial<Product> = {
      productName: form.productName,
      productCode: form.productCode || form.productCodeAlias,
      spec: form.spec || undefined,
      model: form.model || undefined,
      categoryId: form.categoryId,
      barcode: form.barcode || undefined,
      sku: form.sku || undefined,
      productType: form.productType,
      status: 'ENABLED',
      remark: form.remark || undefined,
      origin: form.origin || undefined,
      brand: form.brand || undefined,
      industryCategory: form.industryCategory,
      shelfLifeDays: form.shelfLifeDays,
      nearExpiryDays: form.nearExpiryDays,
      isBatchExpiryManaged: form.isBatchExpiryManaged,
      isStandardProduct: form.isStandardProduct,
      useCoupon: form.useCoupon,
      defaultSalesUnitId: form.defaultSalesUnitId,
      defaultPurchaseUnitId: form.defaultPurchaseUnitId,
      defaultStockUnitId: form.defaultStockUnitId,
      mallDisplayTitle: form.mallDisplayTitle || undefined,
      mallDescription: form.mallDescription || undefined,
      mallTags: form.mallTags || undefined,
      mallShelfStatus: isMallShelf.value ? 1 : 0,
      mallSortType: form.mallSortType,
      mallPoints: form.mallPoints,
      keywords: form.keywords || undefined,
      mallSortOrder: form.mallSortOrder,
      mallMinOrderQty: form.mallMinOrderQty,
      mallPurchaseLimit: form.mallPurchaseLimit,
      richTextDetail: form.richTextDetail || undefined,
      imageUrl: form.imageUrl || undefined,
      videoUrl: form.videoUrl || undefined,
      costPrice: form.costPrice,
      standardPrice: form.standardPrice,
      wholesalePrice: form.wholesalePrice,
      weight: form.weight,
      volume: form.volume,
      taxRate: form.taxRate,
      purchasePrice: form.purchasePrice,
      retailPrice: form.retailPrice
    }

    // 只提交填了单位名称的行：表格恒占 3 行，未使用的占位行不落库
    const unitsPayload = validUnits.map((u: any, idx: number) => ({
      unitName: u.unitName,
      unitType: u.unitType,
      isBaseUnit: u.isBaseUnit || 0,
      conversionRate: u.conversionRate || 1,
      barcode: u.barcode || '',
      sortOrder: idx + 1,
      presetPurchasePrice: u.presetPurchasePrice,
      referenceCost: u.referenceCost,
      recentPurchasePrice: u.recentPurchasePrice,
      wholesalePrice: u.wholesalePrice,
      retailPrice: u.retailPrice,
      minSalePrice: u.minSalePrice,
      minDiscount: u.minDiscount,
      gradePrice1: u.gradePrice1,
      gradePrice2: u.gradePrice2,
      gradePrice3: u.gradePrice3,
      gradePrice4: u.gradePrice4,
      gradePrice5: u.gradePrice5,
      gradePrice6: u.gradePrice6,
      gradePrice7: u.gradePrice7,
      gradePrice8: u.gradePrice8,
      weight: u.weight,
      volume: u.volume
    }))

    const recommendsPayload = recommendList.value.map(r => ({
      recommendProductId: r.recommendProductId,
      sortOrder: r.sortOrder
    }))

    if (isEdit.value && productId.value) {
      await productFormApi.batchUpdate(productId.value, {
        product: productPayload,
        units: unitsPayload,
        recommends: recommendsPayload
      })
      await bindImageMaterials(productId.value)
      message.success('保存成功')
    } else {
      const result: any = await productFormApi.batchCreate({
        product: productPayload,
        units: unitsPayload,
        recommends: recommendsPayload
      })
      // 引用的图片空间素材在商品创建后建立绑定（首张为主图）
      const newId = result?.productId || result?.id
      if (newId) await bindImageMaterials(newId)
      message.success('创建成功')

      if (action === 'save_and_new') {
        resetForm()
        return
      }
    }
    goBack()
  } catch (e: any) {
    const msg = e?.response?.data?.msg || e?.message || '保存失败'
    message.error(msg)
  } finally {
    saving.value = false
  }
}

function resetForm() {
  Object.assign(form, {
    productName: '', productCode: '', productCodeAlias: '', spec: '',
    model: '', categoryId: undefined, barcode: '', sku: '', remark: '',
    origin: '', brand: '', industryCategory: '其他',
    shelfLifeDays: undefined, nearExpiryDays: undefined,
    isBatchExpiryManaged: 0, isStandardProduct: 1, useCoupon: 0,
    defaultSalesUnitId: undefined, defaultPurchaseUnitId: undefined, defaultStockUnitId: undefined,
    mallDisplayTitle: '', mallDescription: '', mallTags: '',
    mallShelfStatus: 0, mallSortType: 'DEFAULT', mallPoints: undefined, keywords: '',
    mallSortOrder: 0, mallMinOrderQty: 0, mallPurchaseLimit: 0,
    richTextDetail: '', imageUrl: '', videoUrl: ''
  })
  isMallShelf.value = false
  selectedTags.value = []
  unitList.value = []
  addUnitRow()
  recommendList.value = []
  imageFileList.value = []
  activeTab.value = 'basic'
}

function goBack() {
  // 新增商品保存后直接关闭当前表单标签页，不跳转到列表页
  // 因为用户可能从多个入口打开（如快捷菜单、其他页面链接等）
  const currentPath = route.path
  const nextPath = tabsStore.closeTab(currentPath)
  router.push(nextPath)
}

function handleError(err: any) {
  console.warn('[新增商品] ErrorBoundary 捕获异常:', err)
}

function handleKeydown(e: KeyboardEvent) {
  if ((e.ctrlKey || e.metaKey) && e.key === 's') {
    e.preventDefault()
    if (!saving.value) handleSave()
  }
}

onMounted(async () => {
  await init()
  document.addEventListener('keydown', handleKeydown)
  // 表格渲染完成后校准表头/行高，使区域高度精确等于「表头 + 3 行」
  nextTick(() => setTimeout(measureUnitTable, 300))
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
})
</script>

<style scoped>
.product-create-fullscreen {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #f5f6fa;
  overflow: hidden;
}

/* ── 顶栏 ── */
.create-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 24px;
  background: #fff;
  border-bottom: 1px solid #e8e8e8;
  flex-shrink: 0;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
  z-index: 10;
}

.create-header__left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.create-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.create-code {
  font-size: 13px;
  color: #999;
}

.create-header__right {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* ── 内容区 ── */
.create-body {
  flex: 1;
  overflow-y: auto;
  padding: 0 24px;
}

/* ── 左侧竖向Tab样式 ── */
.create-tabs-left {
  padding-top: 8px;
}

.create-tabs-left :deep(.ant-tabs-nav) {
  min-width: 140px;
}

.create-tabs-left :deep(.ant-tabs-tab) {
  padding: 12px 20px !important;
  margin: 0 0 4px 0 !important;
  border: none !important;
  border-radius: 0 !important;
  background: #fafafa;
  transition: all 0.2s;
  font-size: 14px;
  position: relative;
}

.create-tabs-left :deep(.ant-tabs-tab:hover) {
  background: #e6f4ff;
  color: #1677ff;
}

.create-tabs-left :deep(.ant-tabs-tab-active) {
  background: #f0f5ff !important;
  border-left: 3px solid #1677ff !important;
}

.create-tabs-left :deep(.ant-tabs-tab-active .ant-tabs-tab-btn) {
  color: #1677ff !important;
  font-weight: 600;
}

.create-tabs-left :deep(.ant-tabs-ink-bar) {
  display: none;
}

.create-tabs-left :deep(.ant-tabs-content-holder) {
  padding-left: 16px;
  border-left: 1px solid #f0f0f0;
}

/* ── 内容区 ── */
.tab-content {
  max-width: 1100px;
  padding: 8px 0;
}

.create-card {
  margin-bottom: 12px;
  border-radius: 6px;
  background: #fff;
}

.create-card :deep(.ant-card-head) {
  padding: 0 16px;
  min-height: 40px;
  border-bottom: 1px solid #f0f0f0;
}

.create-card :deep(.ant-card-head-title) {
  font-size: 14px;
  font-weight: 600;
  padding: 8px 0;
}

.create-card :deep(.ant-card-body) {
  padding: 16px;
}

.field-label {
  font-size: 13px;
  color: #606266;
  font-weight: 500;
}

.image-hint {
  font-size: 12px;
  color: #999;
}

.form-tip {
  font-size: 12px;
  color: #999;
  margin-left: 8px;
}

.unit-actions {
  margin-top: 8px;
}

/* BillDetailTable 的高度由 flex 链驱动（内部 .spreadsheet-table 是 height:0 + flex-grow:1），
 * 嵌在 a-card（block 容器）里必须由外层给出确定高度，否则表格塌陷成 1px 不可见。
 * 高度由 :style 动态给出 = 表头 + max(3, 行数) 行 → 默认预留 4 行（表头+小/中/大 3 行），
 * 用户新增/删除单位行时容器高度同步增减（内容区自适应表格高度）。 */
.unit-table-wrap {
  display: flex;
  flex-direction: column;
  min-height: 0;
}

/* 本区域高度已随单位行数自适应（行多则容器变高），不需要组件内置的「展开/收起」条，
   且该条 flex-shrink:0 会额外吞掉一行高度，故隐藏 */
.unit-table-wrap :deep(.detail-expand) {
  display: none;
}

/* ── 引用图片空间 ── */
.image-space-bar {
  margin-bottom: 8px;
}

.image-space-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  max-height: 360px;
  overflow-y: auto;
}

.image-space-item {
  width: 110px;
  border: 1px solid #f0f0f0;
  border-radius: 4px;
  padding: 4px;
  cursor: pointer;
  transition: border-color .2s, box-shadow .2s;
}
.image-space-item:hover {
  border-color: #91caff;
}
.image-space-item.is-active {
  border-color: #1677ff;
  box-shadow: 0 0 0 2px rgba(22,119,255,.15);
}
.image-space-item img {
  width: 100%;
  height: 90px;
  object-fit: cover;
  display: block;
}
.image-space-name {
  font-size: 12px;
  color: #666;
  text-align: center;
  margin-top: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* ── 图片上传区（顺序展示） ── */
.image-upload-area {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  padding: 8px 0;
}

.image-upload-item {
  position: relative;
  width: 120px;
  height: 120px;
}

.image-upload-preview {
  width: 120px;
  height: 120px;
  border-radius: 6px;
  overflow: hidden;
  border: 1px solid #e8e8e8;
  position: relative;
}

.image-upload-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.image-upload-mask {
  position: absolute;
  inset: 0;
  background: rgba(0,0,0,0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.2s;
}

.image-upload-item:hover .image-upload-mask {
  opacity: 1;
}

.image-upload-main-tag {
  position: absolute;
  bottom: 4px;
  left: 4px;
  background: #1677ff;
  color: #fff;
  font-size: 11px;
  padding: 1px 6px;
  border-radius: 3px;
  z-index: 1;
}

.image-upload-trigger {
  width: 120px;
  height: 120px;
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  background: #fafafa;
  transition: all 0.2s;
  color: #999;
}

.image-upload-trigger:hover {
  border-color: #1677ff;
  color: #1677ff;
  background: #e6f4ff;
}

.image-upload-trigger .anticon {
  font-size: 24px;
}

.trigger-text {
  font-size: 12px;
  margin-top: 4px;
}

/* ─ 视频上传区 ── */
.video-upload-area {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.video-upload-preview {
  border: 1px solid #e8e8e8;
  border-radius: 6px;
  padding: 8px;
  display: inline-block;
}

.video-upload-trigger {
  width: 160px;
  height: 100px;
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fafafa;
}

.video-icon-placeholder {
  font-size: 32px;
  color: #ccc;
}

.video-upload-actions {
  display: flex;
  gap: 8px;
}

/* ── 标签管理区 ── */
.tag-manage-area {
  padding: 4px 0;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.tag-item {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 8px;
  border: 1px solid #e8e8e8;
  border-radius: 4px;
  background: #fafafa;
  transition: all 0.15s;
}

.tag-item:hover {
  border-color: #1677ff;
  background: #e6f4ff;
}

.tag-item :deep(.ant-checkbox-wrapper) {
  margin: 0;
}

.tag-delete-btn {
  font-size: 12px;
  color: #999;
  padding: 0 2px;
}

.tag-delete-btn:hover {
  color: #ff4d4f;
}

.tag-add-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 0;
  border-top: 1px dashed #e8e8e8;
  margin-top: 4px;
  padding-top: 8px;
}

/* ── 紧凑尺寸 ── */
:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-btn-sm) {
  height: 28px;
  line-height: 28px;
}

/* ── 快捷键提示 ─ */
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

/* 单位组选择：选中行高亮 */
:deep(.unit-group-row-active) > td {
  background: #fff3e0 !important;
}
</style>
