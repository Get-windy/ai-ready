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
                  <BillDetailTable
                    :columns="unitColumns"
                    :data-source="unitList"
                    :min-rows="3"
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
                      <a-button
                        type="link"
                        size="small"
                        danger
                        title="删除此行"
                        @click="removeUnitRow(index)"
                      >
                        <CloseOutlined />
                      </a-button>
                    </template>
                  </BillDetailTable>
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

                <!-- ═══ 选择单位组弹窗 ═══ -->
                <a-modal
                  v-model:open="unitGroupModalVisible"
                  title="选择单位组"
                  width="480px"
                  @ok="applyUnitGroup"
                >
                  <p style="color:#888;font-size:13px;margin-bottom:12px;">
                    从单位字典中选择要添加的单位，已存在的单位不会重复添加。
                  </p>
                  <a-checkbox-group
                    v-model:value="selectedUnitDictIds"
                    style="display:flex;flex-direction:column;gap:8px;"
                  >
                    <a-checkbox
                      v-for="dict in unitDictList"
                      :key="dict.id"
                      :value="dict.id"
                    >
                      {{ dict.unitName }}
                      <span
                        v-if="dict.conversionRate && dict.conversionRate !== 1"
                        style="color:#999;font-size:12px;"
                      >(换算率: {{ dict.conversionRate }})</span>
                    </a-checkbox>
                  </a-checkbox-group>
                  <a-empty
                    v-if="unitDictList.length === 0"
                    description="单位字典暂无数据，请先在辅助资料中添加单位"
                  />
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
                        :key="tag.id || tag.tagName"
                        class="tag-item"
                      >
                        <a-checkbox
                          :checked="selectedTags.includes(tag.tagName)"
                          @change="(e: any) => handleTagChange(tag.tagName, e.target.checked)"
                        >
                          {{ tag.tagName }}
                        </a-checkbox>
                        <a-button
                          type="text"
                          size="small"
                          danger
                          class="tag-delete-btn"
                          @click="handleDeleteTag(tag)"
                        >
                          <CloseOutlined />
                        </a-button>
                      </div>
                      <!-- 新增标签 -->
                      <div class="tag-add-row">
                        <a-input
                          v-model:value="newTagName"
                          placeholder="输入新标签名称"
                          size="small"
                          style="width: 160px"
                          @press-enter="handleAddTag"
                        />
                        <a-button
                          size="small"
                          type="primary"
                          :disabled="!newTagName.trim()"
                          @click="handleAddTag"
                        >
                          <PlusOutlined /> 添加
                        </a-button>
                      </div>
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
                      field="recommendProductSpec"
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
import { ref, reactive, computed, onMounted, onUnmounted, defineOptions } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import {
  LeftOutlined, SaveOutlined, DownOutlined, PlusOutlined, UploadOutlined,
  ApartmentOutlined, CalculatorOutlined, VideoCameraOutlined, CrownOutlined,
  CloseOutlined, PlusCircleOutlined, CloseCircleOutlined
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import {
  productCategoryApi,
  productApi,
  productFormApi,
  productRecommendApi,
  productGradeApi,
  productUnitDictApi,
  mallTagApi,
  type Product,
  type ProductCategory,
  type ProductUnit,
  type ProductGrade,
  type ProductRecommend,
  type ProductUnitDict,
  type MallTag
} from '@/api/erp/product'
import request from '@/utils/request'

import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'

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
const productId = ref<number>(0)

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
  mallSortOrder: 0,
  mallMinOrderQty: 0,
  mallPurchaseLimit: 0,
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

// ── 标签（从API加载，用户自定义） ──
const tagOptions = ref<MallTag[]>([])
const selectedTags = ref<string[]>([])
const newTagName = ref('')

async function loadTags() {
  try {
    tagOptions.value = await mallTagApi.list()
  } catch {
    tagOptions.value = []
  }
}

function handleTagChange(tagValue: string, checked: boolean) {
  if (checked) {
    if (!selectedTags.value.includes(tagValue)) {
      selectedTags.value.push(tagValue)
    }
  } else {
    selectedTags.value = selectedTags.value.filter(t => t !== tagValue)
  }
  form.mallTags = selectedTags.value.join(',')
}

async function handleAddTag() {
  const name = newTagName.value.trim()
  if (!name) return
  // 检查重复
  if (tagOptions.value.some(t => t.tagName === name)) {
    message.warning('标签已存在')
    return
  }
  try {
    const maxSort = Math.max(...tagOptions.value.map(t => t.sortOrder || 0), 0)
    await mallTagApi.create({ tagName: name, sortOrder: maxSort + 1 })
    message.success('标签添加成功')
    newTagName.value = ''
    await loadTags()
  } catch {
    message.error('添加标签失败')
  }
}

async function handleDeleteTag(tag: MallTag) {
  if (tag.id) {
    Modal.confirm({
      title: '确认删除',
      content: `确定要删除标签"${tag.tagName}"吗？已关联该标签的商品将不再显示此标签。`,
      okType: 'danger',
      async onOk() {
        try {
          await mallTagApi.delete(tag.id)
          selectedTags.value = selectedTags.value.filter(t => t !== tag.tagName)
          form.mallTags = selectedTags.value.join(',')
          await loadTags()
          message.success('标签已删除')
        } catch {
          message.error('删除标签失败')
        }
      }
    })
  } else {
    // 无id：从列表中移除
    selectedTags.value = selectedTags.value.filter(t => t !== tag.tagName)
    form.mallTags = selectedTags.value.join(',')
  }
}

// ── 单位表格 ─
const unitTypeOptions = [
  { label: '大单位', value: 'LARGE' },
  { label: '中单位', value: 'MEDIUM' },
  { label: '小单位', value: 'SMALL' }
]

// ── 价格等级（从API动态加载，最多8个） ──
const gradeList = ref<ProductGrade[]>([])
const gradePriceFieldMap = [
  'gradePrice1', 'gradePrice2', 'gradePrice3', 'gradePrice4',
  'gradePrice5', 'gradePrice6', 'gradePrice7', 'gradePrice8'
]

const gradePriceColumns = computed(() => {
  // 最多展示8个等级价格列（受DB列数限制）
  return gradeList.value.slice(0, 8).map((g, i) => ({
    field: gradePriceFieldMap[i],
    title: g.gradeName
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

/** 构建默认8个等级（当API无数据时使用） */
function buildDefaultGrades(): ProductGrade[] {
  const defaultNames = ['餐饮店', '食堂团餐', '外围餐饮店', '自助vip', '大团餐', '重点vip01', '价格等级7', '价格等级8']
  return defaultNames.map((name, i) => ({
    id: i + 1,
    gradeCode: `GRADE_${i + 1}`,
    gradeName: name,
    gradeLevel: i + 1,
    sortOrder: i + 1,
    status: 1,
  }))
}

// ── 单位字典（选择单位组） ──
const unitDictList = ref<ProductUnitDict[]>([])
const unitGroupModalVisible = ref(false)
const selectedUnitDictIds = ref<string[]>([])

async function loadUnitDictList() {
  try {
    unitDictList.value = await productUnitDictApi.list()
  } catch {
    unitDictList.value = []
  }
}

async function showUnitGroupDialog() {
  await loadUnitDictList()
  // 预选已有的单位名称
  selectedUnitDictIds.value = unitList.value
    .filter(u => u.unitName)
    .map(u => {
      const match = unitDictList.value.find(d => d.unitName === u.unitName)
      return match?.id || ''
    })
    .filter(Boolean)
  unitGroupModalVisible.value = true
}

function applyUnitGroup() {
  const selectedDicts = unitDictList.value.filter(d => selectedUnitDictIds.value.includes(d.id))
  if (selectedDicts.length === 0) {
    message.warning('请至少选择一个单位')
    return
  }
  // 按字典中的单位名称填充单位列表
  const existingNames = new Set(unitList.value.map(u => u.unitName))
  const typeOrder = ['SMALL', 'MEDIUM', 'LARGE']

  selectedDicts.forEach((dict, idx) => {
    if (existingNames.has(dict.unitName)) return // 已存在则跳过
    unitList.value.push({
      id: 0,
      productId: 0,
      unitName: dict.unitName,
      isBaseUnit: idx === 0 && unitList.value.filter(u => u.isBaseUnit).length === 0 ? 1 : 0,
      conversionRate: dict.conversionRate || 1,
      barcode: '',
      sortOrder: unitList.value.length + 1,
      unitType: typeOrder[idx] || '',
    } as any)
    existingNames.add(dict.unitName)
  })

  // 确保第一行是基本单位
  if (unitList.value.length > 0 && unitList.value[0].isBaseUnit !== 1) {
    unitList.value[0].isBaseUnit = 1
  }

  unitGroupModalVisible.value = false
  message.success(`已加载 ${selectedDicts.length} 个单位`)
}

const unitList = ref<ProductUnit[]>([])

function addUnitRow() {
  // 新增模式：默认创建小/中/大 3行
  const types: Array<{ unitType: string; isBase: number }> = [
    { unitType: 'SMALL', isBase: 1 },
    { unitType: 'MEDIUM', isBase: 0 },
    { unitType: 'LARGE', isBase: 0 },
  ]
  types.forEach((t, i) => {
    unitList.value.push({
      id: 0,
      productId: 0,
      unitName: '',
      isBaseUnit: t.isBase,
      conversionRate: i === 0 ? 1 : undefined,
      barcode: '',
      sortOrder: unitList.value.length + 1,
      unitType: t.unitType,
    } as any)
  })
}

function removeUnitRow(index: number) {
  unitList.value.splice(index, 1)
  // 更新排序
  unitList.value.forEach((u, i) => { u.sortOrder = i + 1 })
}

/** 新增单行（按钮触发的新增行） */
function addSingleUnitRow() {
  unitList.value.push({
    id: 0,
    productId: 0,
    unitName: '',
    isBaseUnit: 0,
    conversionRate: 1,
    barcode: '',
    sortOrder: unitList.value.length + 1,
    unitType: '',
  } as any)
}

/** 列定义（BillDetailTable 格式） */
const unitColumns = computed<DetailColumnConfig[]>(() => {
  const baseColumns: DetailColumnConfig[] = [
    { key: 'rowNo', type: 'rowNo', title: '序号', width: 50, fixed: 'left' },
    { key: 'unitType', type: 'select', title: '类型', width: 90, options: unitTypeOptions, placeholder: '选择类型' },
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
  // 操作列
  const actionCol: DetailColumnConfig = {
    key: 'action',
    type: 'action',
    title: '操作',
    width: 50,
    fixed: 'right',
    slotName: 'actionCell',
  }
  return [...baseColumns, ...gradeCols, actionCol]
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

function batchCalcPrice() {
  message.info('批量计算价格功能待完善')
}

// ── 图片上传（顺序逐个，最多5张） ──
const imageFileList = ref<{ name: string; url: string; status: string }[]>([])
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
      productId.value = Number(route.params.id)
    }

    // 并行加载：分类树 + 标签 + 等级
    const [cats] = await Promise.all([
      productCategoryApi.getTree(),
      loadTags(),
      loadGrades()
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

async function loadProduct(id: number) {
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

    unitList.value = formData.units.map(u => ({ ...u }))
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
  if (unitList.value.length === 0) { message.warning('请至少添加一个商品单位'); return }
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

    const unitsPayload = unitList.value.map((u: any) => ({
      unitName: u.unitName,
      unitType: u.unitType,
      isBaseUnit: u.isBaseUnit || 0,
      conversionRate: u.conversionRate || 1,
      barcode: u.barcode || '',
      sortOrder: u.sortOrder || 0,
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
      gradePrice8: u.gradePrice8
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
      message.success('保存成功')
    } else {
      const result = await productFormApi.batchCreate({
        product: productPayload,
        units: unitsPayload,
        recommends: recommendsPayload
      })
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
    mallShelfStatus: 0, mallSortOrder: 0, mallMinOrderQty: 0, mallPurchaseLimit: 0,
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

onMounted(() => {
  init()
  document.addEventListener('keydown', handleKeydown)
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
</style>
