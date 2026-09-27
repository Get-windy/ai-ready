<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        路线规划（配送 → 配送路线 → 路线规划）
        · 地理能力工具页：路线规划 / 地址编码 / 逆编码 / 坐标转换 / 围栏管理
        · 后端：/api/dms/route/*（规划 · 重规划 · 编码 · 逆编码 · 距离 · 围栏校验 · 坐标转换 · 配置）
        · 地图：RouteMapCanvas 矢量画布（选点 / 落图 / 折线 / 圆形+多边形围栏绘制，零外部依赖）
        · 降级：未配置地图 Key 时后端自动切直线模式（Haversine + 平均时速），页面顶部横幅提示
      -->
      <CategoryListLayout
        :tabs="[]"
        category-title="地理能力"
        :category-editable="false"
        :category-tree-data="capabilityTree"
        :selected-category-id="activeCapability"
        :current-path="currentCapabilityName"
        @category-select="onCapabilitySelect"
      >
        <!-- ═══ 工具栏左侧（按能力切换） ═══ -->
        <template #toolbar-left>
          <a-space :size="8">
            <template v-if="activeCapability === 'plan'">
              <a-button
                type="primary"
                size="small"
                :loading="planLoading"
                @click="handlePlanRoute"
              >
                <ThunderboltOutlined /> 规划路线
              </a-button>
              <a-button
                size="small"
                :disabled="!planResult?.stops?.length"
                @click="openGenerateModal"
              >
                <ExportOutlined /> 生成配送路线单
              </a-button>
            </template>
            <template v-else-if="activeCapability === 'fence'">
              <a-button
                type="primary"
                class="btn-add"
                size="small"
                @click="openFenceModal()"
              >
                <PlusOutlined /> 新增围栏
              </a-button>
            </template>
            <template v-else-if="activeCapability === 'geocode'">
              <a-button
                type="primary"
                size="small"
                :loading="geocodeLoading"
                @click="handleGeocode"
              >
                <SearchOutlined /> 地址编码
              </a-button>
            </template>
            <template v-else-if="activeCapability === 'reverse'">
              <a-button
                type="primary"
                size="small"
                :loading="reverseLoading"
                @click="handleReverseGeocode"
              >
                <SearchOutlined /> 逆编码
              </a-button>
            </template>
            <template v-else>
              <a-button
                type="primary"
                size="small"
                @click="handleConvert"
              >
                <SwapOutlined /> 坐标转换
              </a-button>
            </template>
          </a-space>
        </template>

        <!-- ═══ 工具栏右侧：刷新 / 打印(F8) / 导出 ═══ -->
        <template #toolbar-right>
          <a-space :size="8">
            <a-tag
              v-if="routeConfig"
              :color="routeConfig.degraded ? 'orange' : 'green'"
              class="service-tag"
            >
              地图服务：{{ routeConfig.degraded ? '降级(直线)' : `${routeConfig.provider} · ${KEY_SOURCE_TEXT[routeConfig.keySource] || routeConfig.keySource}` }}
            </a-tag>
            <a-tooltip
              title="用当前生效 Key 真实调用一次地址编码，验证配置是否可用"
              placement="bottom"
            >
              <a-button
                size="small"
                :loading="verifyLoading"
                @click="handleVerify"
              >
                <ApiOutlined /> 连通性自检
              </a-button>
            </a-tooltip>
            <a-button
              size="small"
              @click="handleRefreshAll"
            >
              <ReloadOutlined /> 刷新
            </a-button>
            <a-button
              size="small"
              @click="handlePrint"
            >
              <PrinterOutlined /> 打印(F8)
            </a-button>
            <a-button
              size="small"
              @click="handleExport"
            >
              <DownloadOutlined /> 导出
            </a-button>
          </a-space>
        </template>

        <!-- ═══ 查询区（按能力切换的固定项） ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div class="search-row">
              <template v-if="activeCapability === 'plan'">
                <div class="search-item">
                  <span class="search-label">出行方式</span>
                  <a-select
                    v-model:value="planForm.direction"
                    size="small"
                    style="width: 110px"
                  >
                    <a-select-option value="DRIVING">
                      驾车
                    </a-select-option>
                    <a-select-option value="CYCLING">
                      骑行
                    </a-select-option>
                    <a-select-option value="WALKING">
                      步行
                    </a-select-option>
                  </a-select>
                </div>
                <div class="search-item">
                  <span class="search-label">导航策略</span>
                  <a-select
                    v-model:value="planForm.strategy"
                    size="small"
                    style="width: 130px"
                  >
                    <a-select-option :value="0">
                      速度优先
                    </a-select-option>
                    <a-select-option :value="1">
                      距离优先
                    </a-select-option>
                    <a-select-option :value="2">
                      避免收费
                    </a-select-option>
                    <a-select-option :value="3">
                      避免拥堵
                    </a-select-option>
                  </a-select>
                </div>
                <div class="search-item">
                  <span class="search-label">已送达点位</span>
                  <a-input-number
                    v-model:value="reoptimizeVisited"
                    size="small"
                    :min="0"
                    :precision="0"
                    style="width: 80px"
                  />
                </div>
                <div class="search-item">
                  <span class="search-label">单车载重</span>
                  <a-input-number
                    v-model:value="planForm.vehicleCapacity"
                    size="small"
                    :min="0"
                    :precision="2"
                    placeholder="不限"
                    style="width: 100px"
                  />
                </div>
                <div class="search-item">
                  <span class="search-label">可用车辆</span>
                  <a-input-number
                    v-model:value="planForm.vehicleCount"
                    size="small"
                    :min="1"
                    :precision="0"
                    placeholder="不限"
                    style="width: 90px"
                  />
                </div>
                <div class="search-item">
                  <span class="search-label">出发时间</span>
                  <a-time-picker
                    v-model:value="planForm.departureTime"
                    size="small"
                    format="HH:mm"
                    value-format="HH:mm"
                    placeholder="08:00"
                    style="width: 100px"
                  />
                </div>
                <div class="search-item">
                  <a-tooltip
                    title="勾选后晚于时间窗的点不再派发，直接进入「未排入」，而不是只告警"
                    placement="bottom"
                  >
                    <a-checkbox v-model:checked="planForm.hardTimeWindow">
                      硬时间窗
                    </a-checkbox>
                  </a-tooltip>
                </div>
                <a-button
                  size="small"
                  :disabled="!planResult?.stops?.length"
                  @click="handleReoptimize"
                >
                  重新规划
                </a-button>
              </template>

              <template v-else-if="activeCapability === 'geocode'">
                <div class="search-item">
                  <span class="search-label">城市</span>
                  <a-input
                    v-model:value="geocodeForm.city"
                    size="small"
                    style="width: 140px"
                    placeholder="可选，辅助解析"
                    allow-clear
                    @press-enter="handleGeocode"
                  />
                </div>
                <div class="search-item">
                  <span class="search-label">坐标体系</span>
                  <a-tag color="blue">
                    {{ routeConfig?.coordSystem || 'GCJ02' }}
                  </a-tag>
                </div>
              </template>

              <template v-else-if="activeCapability === 'reverse'">
                <div class="search-item">
                  <span class="search-label">入参坐标体系</span>
                  <a-select
                    v-model:value="reverseForm.from"
                    size="small"
                    style="width: 120px"
                  >
                    <a-select-option value="GCJ02">
                      GCJ02
                    </a-select-option>
                    <a-select-option value="WGS84">
                      WGS84
                    </a-select-option>
                    <a-select-option value="BD09">
                      BD09
                    </a-select-option>
                  </a-select>
                </div>
              </template>

              <template v-else-if="activeCapability === 'convert'">
                <div class="search-item">
                  <span class="search-label">源体系</span>
                  <a-select
                    v-model:value="convertForm.from"
                    size="small"
                    style="width: 110px"
                  >
                    <a-select-option value="WGS84">
                      WGS84
                    </a-select-option>
                    <a-select-option value="GCJ02">
                      GCJ02
                    </a-select-option>
                    <a-select-option value="BD09">
                      BD09
                    </a-select-option>
                  </a-select>
                </div>
                <div class="search-item">
                  <span class="search-label">目标体系</span>
                  <a-select
                    v-model:value="convertForm.to"
                    size="small"
                    style="width: 110px"
                  >
                    <a-select-option value="GCJ02">
                      GCJ02
                    </a-select-option>
                    <a-select-option value="WGS84">
                      WGS84
                    </a-select-option>
                    <a-select-option value="BD09">
                      BD09
                    </a-select-option>
                  </a-select>
                </div>
              </template>

              <template v-else>
                <div class="search-item">
                  <span class="search-label">筛选条件</span>
                  <a-input
                    v-model:value="fenceQuery.keyword"
                    size="small"
                    style="width: 220px"
                    placeholder="围栏编码/名称/绑定对象"
                    allow-clear
                    @press-enter="handleFenceSearch"
                  />
                </div>
                <div class="search-item">
                  <span class="search-label">围栏类型</span>
                  <a-select
                    v-model:value="fenceQuery.fenceType"
                    size="small"
                    style="width: 110px"
                    @change="handleFenceSearch"
                  >
                    <a-select-option value="">
                      全部
                    </a-select-option>
                    <a-select-option value="CIRCLE">
                      圆形
                    </a-select-option>
                    <a-select-option value="POLYGON">
                      多边形
                    </a-select-option>
                  </a-select>
                </div>
                <div class="search-item">
                  <span class="search-label">状态</span>
                  <a-select
                    v-model:value="fenceQuery.status"
                    size="small"
                    style="width: 100px"
                    @change="handleFenceSearch"
                  >
                    <a-select-option value="ENABLED">
                      已启用
                    </a-select-option>
                    <a-select-option value="DISABLED">
                      已停用
                    </a-select-option>
                    <a-select-option value="">
                      全部
                    </a-select-option>
                  </a-select>
                </div>
                <a-button
                  type="primary"
                  size="small"
                  @click="handleFenceSearch"
                >
                  查询
                </a-button>
              </template>
            </div>

            <!-- 降级提示（未配置地图 Key）：直接给出「去哪配」的三种落位 + 一键跳转配置中心 -->
            <div
              v-if="routeConfig?.degraded"
              class="degrade-hint"
            >
              <span>{{ routeConfig.hint }}</span>
              <a-button
                type="link"
                size="small"
                class="degrade-action"
                @click="goConfigCenter"
              >
                去《配送参数》配置
              </a-button>
            </div>
            <a-alert
              v-if="verifyResult"
              class="verify-alert"
              :type="verifyResult.ok ? 'success' : 'error'"
              show-icon
              closable
              :message="verifyResult.ok
                ? `连通性正常：${verifyResult.provider} · ${verifyResult.latencyMs} ms`
                : '连通性异常'"
              :description="verifyResult.message
                + (verifyResult.formattedAddress ? `（样本：${verifyResult.sampleAddress} → ${verifyResult.formattedAddress}）` : '')"
              @close="verifyResult = null"
            />
            <div
              v-else-if="routeConfig"
              class="config-source-hint"
            >
              地图服务 Key 来源：{{ KEY_SOURCE_TEXT[routeConfig.keySource] || routeConfig.keySource }}
              <template v-if="routeConfig.keySource === 'ENV'">（环境变量 {{ routeConfig.envVarName }}）</template>
              <template v-else-if="routeConfig.keySource === 'CONFIG'">（《配送参数》{{ routeConfig.configKey }}）</template>
            </div>
          </div>
        </template>

        <!-- ═══ 主体（按能力切换） ═══ -->
        <template #table>
          <div class="tool-body">
            <!-- ── 路线规划 ── -->
            <template v-if="activeCapability === 'plan'">
              <div class="split-row">
                <div class="form-pane">
                  <a-card
                    size="small"
                    title="规划参数"
                  >
                    <a-form size="small">
                      <a-form-item label="起点">
                        <div class="coord-row">
                          <a-input
                            v-model:value="planForm.origin.address"
                            placeholder="起点地址/名称"
                            allow-clear
                          />
                          <a-input-number
                            v-model:value="planForm.origin.lat"
                            :precision="6"
                            placeholder="纬度"
                            style="width: 116px"
                          />
                          <a-input-number
                            v-model:value="planForm.origin.lng"
                            :precision="6"
                            placeholder="经度"
                            style="width: 116px"
                          />
                          <a-tooltip
                            title="从地图拾取起点"
                            placement="bottom"
                          >
                            <a-button
                              size="small"
                              :type="pickTarget === 'planOrigin' ? 'primary' : 'default'"
                              @click="armPick('planOrigin')"
                            >
                              <EnvironmentOutlined />
                            </a-button>
                          </a-tooltip>
                        </div>
                      </a-form-item>

                      <a-divider class="thin-divider">
                        目的地（{{ planForm.destinations.length }}）
                      </a-divider>

                      <div
                        v-for="(dest, index) in planForm.destinations"
                        :key="index"
                        class="dest-row"
                      >
                        <div class="dest-head">
                          <span class="dest-title">目的地 {{ index + 1 }}</span>
                          <a-button
                            v-if="planForm.destinations.length > 1"
                            type="link"
                            danger
                            size="small"
                            @click="removeDestination(index)"
                          >
                            删除
                          </a-button>
                        </div>
                        <div class="coord-row">
                          <a-input
                            v-model:value="dest.address"
                            placeholder="地址/客户名称"
                            allow-clear
                          />
                          <a-input-number
                            v-model:value="dest.lat"
                            :precision="6"
                            placeholder="纬度"
                            style="width: 116px"
                          />
                          <a-input-number
                            v-model:value="dest.lng"
                            :precision="6"
                            placeholder="经度"
                            style="width: 116px"
                          />
                          <a-tooltip
                            title="从地图拾取该目的地"
                            placement="bottom"
                          >
                            <a-button
                              size="small"
                              :type="pickTarget === `planDest:${index}` ? 'primary' : 'default'"
                              @click="armPick(`planDest:${index}`)"
                            >
                              <EnvironmentOutlined />
                            </a-button>
                          </a-tooltip>
                        </div>
                        <!-- VRP 约束：需求量 + 时间窗（可留空；填写后按载重分批、按窗口排优并校核晚到） -->
                        <div class="coord-row constraint-row">
                          <a-input-number
                            v-model:value="dest.demand"
                            :min="0"
                            :precision="2"
                            placeholder="需求量"
                            style="width: 116px"
                          />
                          <a-time-picker
                            v-model:value="dest.windowStart"
                            format="HH:mm"
                            value-format="HH:mm"
                            placeholder="窗口起"
                            style="width: 108px"
                          />
                          <a-time-picker
                            v-model:value="dest.windowEnd"
                            format="HH:mm"
                            value-format="HH:mm"
                            placeholder="窗口止"
                            style="width: 108px"
                          />
                          <a-input-number
                            v-model:value="dest.stayMinutes"
                            :min="0"
                            :precision="0"
                            placeholder="停留分"
                            style="width: 96px"
                          />
                        </div>
                      </div>

                      <a-button
                        type="dashed"
                        block
                        size="small"
                        style="margin-top: 8px"
                        @click="addDestination"
                      >
                        <PlusOutlined /> 添加目的地
                      </a-button>
                    </a-form>
                  </a-card>
                </div>

                <div class="map-pane">
                  <RouteMapCanvas
                    title="规划落图"
                    height="100%"
                    :markers="planMarkers"
                    :polyline="planPolyline"
                    :interactive="!!pickTarget"
                    :draggable-markers="true"
                    :base-map-key="routeConfig?.jsKey || ''"
                    :empty-text="pickTarget ? '在地图上点击以拾取坐标' : '填写或拾取起点/目的地坐标后点击「规划路线」（标记可拖拽微调）'"
                    @pick="handleMapPick"
                    @marker-move="handleMarkerMove"
                    @basemap-failed="handleBaseMapFailed"
                  />
                </div>
              </div>

              <!-- 规划结果 -->
              <div
                v-if="planResult"
                class="result-block"
              >
                <div class="result-head">
                  <a-descriptions
                    size="small"
                    :column="5"
                    class="result-desc"
                  >
                    <a-descriptions-item label="总距离">
                      {{ formatDistance(planResult.totalDistance) }}
                    </a-descriptions-item>
                    <a-descriptions-item label="预计时长">
                      {{ formatDuration(planResult.totalDuration) }}
                    </a-descriptions-item>
                    <a-descriptions-item label="途经点数">
                      {{ (planResult.stops?.length || 1) - 1 }}
                    </a-descriptions-item>
                    <a-descriptions-item label="服务商">
                      {{ planResult.provider }}
                      <a-tag
                        v-if="planResult.degraded"
                        color="orange"
                      >
                        降级
                      </a-tag>
                    </a-descriptions-item>
                    <a-descriptions-item label="出行方式">
                      {{ TRAVEL_MODE_TEXT[planResult.travelMode || 'DRIVING'] }}
                    </a-descriptions-item>
                  </a-descriptions>
                  <a-space>
                    <a-select
                      v-model:value="checkFenceId"
                      size="small"
                      style="width: 190px"
                      placeholder="选择围栏校验"
                      allow-clear
                    >
                      <a-select-option
                        v-for="f in fenceOptions"
                        :key="f.id"
                        :value="f.id"
                      >
                        {{ f.fenceName }}
                      </a-select-option>
                    </a-select>
                    <a-button
                      size="small"
                      :disabled="!checkFenceId"
                      :loading="fenceCheckLoading"
                      @click="checkPlanStopsInFence"
                    >
                      校验点位
                    </a-button>
                  </a-space>
                </div>

                <a-alert
                  v-if="planResult.warnings?.length"
                  class="warn-alert"
                  type="warning"
                  show-icon
                  :message="`约束告警（${planResult.warnings.length}）`"
                >
                  <template #description>
                    <ul class="warn-list">
                      <li
                        v-for="(w, i) in planResult.warnings"
                        :key="i"
                      >
                        {{ w }}
                      </li>
                    </ul>
                  </template>
                </a-alert>
                <a-alert
                  v-if="planResult.unassigned?.length"
                  class="warn-alert"
                  type="error"
                  show-icon
                  :message="`未排入点位（${planResult.unassigned.length}）`"
                >
                  <template #description>
                    <ul class="warn-list">
                      <li
                        v-for="(u, i) in planResult.unassigned"
                        :key="i"
                      >
                        {{ u.address || (u.lat + ',' + u.lng) }}
                        <template v-if="u.timeWindowEnd">（时间窗止 {{ u.timeWindowEnd }}）</template>
                      </li>
                    </ul>
                  </template>
                </a-alert>
                <div
                  v-if="planResult.batches?.length"
                  class="batch-summary"
                >
                  <a-tag
                    v-for="b in planResult.batches"
                    :key="b.batchNo"
                    color="blue"
                    class="batch-tag"
                  >
                    第 {{ b.batchNo }} 车 · {{ b.vehicleName || '默认车辆' }} · {{ (b.stops || []).filter((s: any) => s.type !== 'start').length }} 点 · 载重 {{ b.load ?? 0 }}<template v-if="b.capacity">/{{ b.capacity }}</template> · {{ formatDistance(b.totalDistance) }}<template v-if="b.originAddress"> · 起点：{{ b.originAddress }}</template>
                  </a-tag>
                </div>
                <a-table
                  :data-source="planResult.stops || []"
                  :columns="stopColumns"
                  :locale="locale"
                  row-key="index"
                  :pagination="false as any"
                  size="small"
                >
                  <template #bodyCell="{ column, record }">
                    <template v-if="column.key === 'type'">
                      <a-tag :color="STOP_TYPE_COLOR[record.type] || 'default'">
                        {{ STOP_TYPE_TEXT[record.type] || record.type }}
                      </a-tag>
                    </template>
                    <template v-else-if="column.key === 'timeWindow'">
                      <span v-if="record.timeWindowStart || record.timeWindowEnd">
                        {{ record.timeWindowStart || '--' }} ~ {{ record.timeWindowEnd || '--' }}
                      </span>
                      <span v-else>-</span>
                    </template>
                    <template v-else-if="column.key === 'batchNo'">
                      <a-tag v-if="record.batchNo" color="blue">
                        {{ record.batchNo }} 车
                      </a-tag>
                      <span v-else>-</span>
                    </template>
                    <template v-else-if="column.key === 'distanceFromPrev'">
                      {{ record.distanceFromPrev != null ? formatDistance(record.distanceFromPrev) : '-' }}
                    </template>
                    <template v-else-if="column.key === 'durationFromPrev'">
                      {{ record.durationFromPrev != null ? formatDuration(record.durationFromPrev) : '-' }}
                    </template>
                    <template v-else-if="column.key === 'fenceState'">
                      <a-tag
                        v-if="stopFenceResult(record.index) !== undefined"
                        :color="stopFenceResult(record.index) ? 'green' : 'red'"
                      >
                        {{ stopFenceResult(record.index) ? '围栏内' : '围栏外' }}
                      </a-tag>
                      <span v-else>-</span>
                    </template>
                    <template v-else-if="column.key === 'action'">
                      <a-space :size="0">
                        <a-button
                          type="link"
                          size="small"
                          :disabled="record.type === 'start'"
                          @click="useStopAsOrigin(record)"
                        >
                          设为起点
                        </a-button>
                        <a-button
                          type="link"
                          size="small"
                          danger
                          :disabled="record.type === 'start'"
                          @click="removeStop(record)"
                        >
                          移除
                        </a-button>
                      </a-space>
                    </template>
                  </template>
                </a-table>
              </div>
            </template>

            <!-- ── 地址编码 ── -->
            <template v-else-if="activeCapability === 'geocode'">
              <div class="split-row">
                <div class="form-pane">
                  <a-card
                    size="small"
                    title="地址 → 坐标"
                  >
                    <a-form size="small">
                      <a-form-item label="地址">
                        <a-textarea
                          v-model:value="geocodeForm.address"
                          :rows="2"
                          placeholder="如：浙江省杭州市余杭区文一西路969号"
                        />
                      </a-form-item>
                      <a-button
                        type="primary"
                        block
                        :loading="geocodeLoading"
                        @click="handleGeocode"
                      >
                        查询候选
                      </a-button>
                    </a-form>

                    <a-divider class="thin-divider">
                      候选地址（{{ geocodeResult?.candidates?.length || 0 }}）
                    </a-divider>
                    <a-empty
                      v-if="!geocodeResult"
                      description="尚未查询"
                    />
                    <a-alert
                      v-else-if="!geocodeResult.success"
                      type="warning"
                      show-icon
                      :message="geocodeResult.message || '地址编码失败'"
                    />
                    <div
                      v-else
                      class="candidate-list"
                    >
                      <div
                        v-for="(c, i) in geocodeResult.candidates || []"
                        :key="i"
                        class="candidate-item"
                        :class="{ active: selectedCandidateIndex === i }"
                        @click="selectCandidate(i)"
                      >
                        <div class="candidate-addr">
                          {{ c.formattedAddress || c.district || '-' }}
                        </div>
                        <div class="candidate-meta">
                          {{ c.lng?.toFixed(6) }}, {{ c.lat?.toFixed(6) }}
                          <a-tag v-if="c.level">
                            {{ c.level }}
                          </a-tag>
                        </div>
                        <a-space
                          class="candidate-actions"
                          @click.stop
                        >
                          <a-button
                            type="link"
                            size="small"
                            @click="applyCandidateToPlan('origin', i)"
                          >
                            填入起点
                          </a-button>
                          <a-button
                            type="link"
                            size="small"
                            @click="applyCandidateToPlan('destination', i)"
                          >
                            填入目的地
                          </a-button>
                        </a-space>
                      </div>
                    </div>
                  </a-card>
                </div>
                <div class="map-pane">
                  <RouteMapCanvas
                    title="候选落图"
                    height="100%"
                    :markers="geocodeMarkers"
                    :interactive="true"
                    empty-text="查询后可在地图上查看候选坐标"
                    @pick="handleMapPick"
                  />
                </div>
              </div>
            </template>

            <!-- ── 逆编码 ── -->
            <template v-else-if="activeCapability === 'reverse'">
              <div class="split-row">
                <div class="form-pane">
                  <a-card
                    size="small"
                    title="坐标 → 地址"
                  >
                    <a-form size="small">
                      <a-form-item label="纬度">
                        <a-input-number
                          v-model:value="reverseForm.lat"
                          :precision="6"
                          placeholder="纬度"
                          style="width: 100%"
                        />
                      </a-form-item>
                      <a-form-item label="经度">
                        <a-input-number
                          v-model:value="reverseForm.lng"
                          :precision="6"
                          placeholder="经度"
                          style="width: 100%"
                        />
                      </a-form-item>
                      <a-button
                        size="small"
                        block
                        style="margin-bottom: 8px"
                        @click="armPick('reverse')"
                      >
                        <EnvironmentOutlined />
                        {{ pickTarget === 'reverse' ? '请点击地图拾取…' : '地图拾取坐标' }}
                      </a-button>
                      <a-button
                        type="primary"
                        block
                        :loading="reverseLoading"
                        @click="handleReverseGeocode"
                      >
                        查询地址
                      </a-button>
                    </a-form>
                    <a-divider class="thin-divider">
                      结果
                    </a-divider>
                    <a-alert
                      v-if="reverseResult && !reverseResult.success"
                      type="warning"
                      show-icon
                      :message="reverseResult.message || '逆编码失败'"
                    />
                    <a-descriptions
                      v-else-if="reverseResult"
                      size="small"
                      :column="1"
                      bordered
                    >
                      <a-descriptions-item label="完整地址">
                        {{ reverseResult.formattedAddress || '-' }}
                      </a-descriptions-item>
                      <a-descriptions-item label="省">
                        {{ reverseResult.province || '-' }}
                      </a-descriptions-item>
                      <a-descriptions-item label="市">
                        {{ reverseResult.city || '-' }}
                      </a-descriptions-item>
                      <a-descriptions-item label="区县">
                        {{ reverseResult.district || '-' }}
                      </a-descriptions-item>
                      <a-descriptions-item label="街道">
                        {{ [reverseResult.street, reverseResult.streetNumber].filter(Boolean).join(' ') || '-' }}
                      </a-descriptions-item>
                    </a-descriptions>
                    <a-empty
                      v-else
                      description="尚未查询"
                    />
                  </a-card>
                </div>
                <div class="map-pane">
                  <RouteMapCanvas
                    title="坐标落图"
                    height="100%"
                    :markers="reverseMarkers"
                    :interactive="true"
                    empty-text="在地图上点击可直接拾取坐标"
                    @pick="handleMapPick"
                  />
                </div>
              </div>
            </template>

            <!-- ── 坐标转换 ── -->
            <template v-else-if="activeCapability === 'convert'">
              <div class="split-row">
                <div class="form-pane">
                  <a-card
                    size="small"
                    title="坐标体系转换"
                  >
                    <a-form size="small">
                      <a-form-item :label="convertForm.from + ' 纬度'">
                        <a-input-number
                          v-model:value="convertForm.lat"
                          :precision="6"
                          placeholder="纬度"
                          style="width: 100%"
                        />
                      </a-form-item>
                      <a-form-item :label="convertForm.from + ' 经度'">
                        <a-input-number
                          v-model:value="convertForm.lng"
                          :precision="6"
                          placeholder="经度"
                          style="width: 100%"
                        />
                      </a-form-item>
                      <a-button
                        size="small"
                        block
                        style="margin-bottom: 8px"
                        @click="armPick('convert')"
                      >
                        <EnvironmentOutlined />
                        {{ pickTarget === 'convert' ? '请点击地图拾取…' : '地图拾取坐标' }}
                      </a-button>
                      <a-button
                        type="primary"
                        block
                        :loading="convertLoading"
                        @click="handleConvert"
                      >
                        转换为 {{ convertForm.to }}
                      </a-button>
                    </a-form>
                    <a-divider class="thin-divider">
                      结果
                    </a-divider>
                    <a-descriptions
                      v-if="convertResult"
                      size="small"
                      :column="1"
                      bordered
                    >
                      <a-descriptions-item :label="`${convertResult.from} 坐标`">
                        {{ convertForm.lat?.toFixed(6) }}, {{ convertForm.lng?.toFixed(6) }}
                      </a-descriptions-item>
                      <a-descriptions-item :label="`${convertResult.to} 坐标`">
                        {{ convertResult.lat?.toFixed(6) }}, {{ convertResult.lng?.toFixed(6) }}
                      </a-descriptions-item>
                    </a-descriptions>
                    <a-empty
                      v-else
                      description="尚未转换"
                    />
                    <div class="tips">
                      <p>· 本系统对外统一 <b>GCJ-02</b>（高德/腾讯口径），GPS 原始坐标入库前需由 WGS-84 转换，避免围栏判定漂移。</p>
                      <p>· 百度 BD-09 仅在与百度地图交互时使用，落库前转回 GCJ-02。</p>
                    </div>
                  </a-card>
                </div>
                <div class="map-pane">
                  <RouteMapCanvas
                    title="转换落图"
                    height="100%"
                    :markers="convertMarkers"
                    :interactive="true"
                    empty-text="在地图上点击可直接拾取坐标"
                    @pick="handleMapPick"
                  />
                </div>
              </div>
            </template>

            <!-- ── 围栏管理 ── -->
            <template v-else>
              <div class="fence-wrap">
                <div class="fence-table">
                  <BillTableList
                    :columns="fenceColumns"
                    :data-source="fenceList"
                    :loading="fenceLoading"
                    :pagination="false"
                    :show-toolbar="false"
                    :show-search="false"
                    :show-add="false"
                    :show-export="false"
                    :show-batch-delete="false"
                    :selectable="false"
                    storage-key="dms-route-fence-columns"
                    global-config-key="dms-route-fence-columns"
                    row-key="id"
                  >
                    <template #nameCell="{ record }">
                      <a @click="openFenceModal(record)">{{ record.fenceName }}</a>
                    </template>
                    <template #typeCell="{ record }">
                      <a-tag :color="record.fenceType === 'POLYGON' ? 'magenta' : 'orange'">
                        {{ record.fenceType === 'POLYGON' ? '多边形' : '圆形' }}
                      </a-tag>
                    </template>
                    <template #geometryCell="{ record }">
                      {{ record.fenceType === 'POLYGON'
                        ? `${(record.polygonPoints || '').split(';').filter(Boolean).length} 个顶点`
                        : `${record.radiusMeters ?? '-'} 米` }}
                    </template>
                    <template #bizCell="{ record }">
                      {{ record.bizType ? `${BIZ_TYPE_TEXT[record.bizType] || record.bizType}：${record.bizName || record.bizId || '-'}` : '-' }}
                    </template>
                    <template #statusCell="{ record }">
                      <a-tag :color="record.status === 'ENABLED' ? 'green' : 'default'">
                        {{ record.status === 'ENABLED' ? '已启用' : '已停用' }}
                      </a-tag>
                    </template>
                    <template #actionCell="{ record }">
                      <a-space :size="0">
                        <a-button
                          type="link"
                          size="small"
                          @click="openFenceModal(record)"
                        >
                          修改
                        </a-button>
                        <a-button
                          type="link"
                          size="small"
                          @click="toggleFenceStatus(record)"
                        >
                          {{ record.status === 'ENABLED' ? '停用' : '启用' }}
                        </a-button>
                        <a-button
                          type="link"
                          size="small"
                          danger
                          @click="removeFence(record)"
                        >
                          删除
                        </a-button>
                      </a-space>
                    </template>
                  </BillTableList>
                </div>

                <div class="fence-check-pane">
                  <a-card
                    size="small"
                    title="围栏校验"
                  >
                    <a-form size="small">
                      <a-form-item label="围栏">
                        <a-select
                          v-model:value="checkFenceId"
                          placeholder="选择围栏（可留空用下方内联几何）"
                          allow-clear
                          @change="syncCheckFenceGeometry"
                        >
                          <a-select-option
                            v-for="f in fenceOptions"
                            :key="f.id"
                            :value="f.id"
                          >
                            {{ f.fenceName }}
                          </a-select-option>
                        </a-select>
                      </a-form-item>
                      <a-form-item label="待校验点">
                        <div class="coord-row">
                          <a-input-number
                            v-model:value="checkForm.lat"
                            :precision="6"
                            placeholder="纬度"
                            style="width: 110px"
                          />
                          <a-input-number
                            v-model:value="checkForm.lng"
                            :precision="6"
                            placeholder="经度"
                            style="width: 110px"
                          />
                          <a-tooltip
                            title="从地图拾取待校验点"
                            placement="bottom"
                          >
                            <a-button
                              size="small"
                              :type="pickTarget === 'fenceCheck' ? 'primary' : 'default'"
                              @click="armPick('fenceCheck')"
                            >
                              <EnvironmentOutlined />
                            </a-button>
                          </a-tooltip>
                        </div>
                      </a-form-item>
                      <a-space>
                        <a-button
                          type="primary"
                          size="small"
                          :loading="fenceCheckLoading"
                          @click="handleFenceCheck"
                        >
                          检查
                        </a-button>
                        <a-button
                          size="small"
                          :disabled="!planResult?.stops?.length"
                          :loading="fenceCheckLoading"
                          @click="checkPlanStopsInFence"
                        >
                          校验规划结果点位
                        </a-button>
                      </a-space>
                    </a-form>

                    <div
                      v-if="fenceCheckResult"
                      class="check-result"
                    >
                      <a-tag
                        :color="fenceCheckResult.inside ? 'green' : 'red'"
                      >
                        {{ fenceCheckResult.inside ? '在围栏内' : '在围栏外' }}
                      </a-tag>
                      <span class="check-distance">
                        距{{ fenceCheckResult.fenceType === 'POLYGON' ? '边界' : '中心' }}
                        {{ formatDistance(fenceCheckResult.distanceMeters) }}
                      </span>
                    </div>
                    <a-table
                      v-if="fenceCheckResult?.results?.length"
                      :data-source="fenceCheckResult.results"
                      :columns="checkResultColumns"
                      :pagination="false as any"
                      row-key="lat"
                      size="small"
                      style="margin-top: 8px"
                    >
                      <template #bodyCell="{ column, record }">
                        <template v-if="column.key === 'inside'">
                          <a-tag :color="record.inside ? 'green' : 'red'">
                            {{ record.inside ? '围栏内' : '围栏外' }}
                          </a-tag>
                        </template>
                        <template v-else-if="column.key === 'distanceMeters'">
                          {{ formatDistance(record.distanceMeters) }}
                        </template>
                      </template>
                    </a-table>
                  </a-card>
                </div>

                <div class="fence-map-pane">
                  <RouteMapCanvas
                    title="围栏落图"
                    height="100%"
                    :markers="fenceCheckMarkers"
                    :circle="fencePreviewCircle"
                    :polygon="fencePreviewPolygon"
                    :interactive="true"
                    empty-text="选择或新增围栏后在此查看几何范围"
                    @pick="handleMapPick"
                  />
                </div>
              </div>
            </template>
          </div>
        </template>

        <!-- ═══ 底部：围栏台账分页 ═══ -->
        <template #table-footer>
          <StandardPagination
            v-if="activeCapability === 'fence'"
            variant="classic"
            :current="fencePagination.current"
            :page-size="fencePagination.pageSize"
            :total="fencePagination.total"
            :page-size-options="[20, 50, 100]"
            @change="handleFencePageChange"
          />
        </template>
      </CategoryListLayout>

      <!-- ═══ 围栏新增 / 修改 ═══ -->
      <a-modal
        v-model:open="fenceModalVisible"
        title="电子围栏"
        :width="880"
        :mask-closable="false"
        :confirm-loading="fenceSaving"
        ok-text="保存(Enter)"
        cancel-text="关闭(Esc)"
        @ok="handleFenceSave"
      >
        <div class="fence-modal">
          <a-form
            :model="fenceForm"
            :label-col="{ span: 5 }"
            :wrapper-col="{ span: 18 }"
            size="small"
            class="fence-form"
          >
            <a-form-item label="围栏编码">
              <a-input
                v-model:value="fenceForm.fenceCode"
                placeholder="留空自动生成"
                :maxlength="64"
              />
            </a-form-item>
            <a-form-item
              label="围栏名称"
              required
            >
              <a-input
                v-model:value="fenceForm.fenceName"
                placeholder="请输入围栏名称"
                :maxlength="128"
              />
            </a-form-item>
            <a-form-item
              label="围栏类型"
              required
            >
              <a-radio-group v-model:value="fenceForm.fenceType">
                <a-radio value="CIRCLE">
                  圆形
                </a-radio>
                <a-radio value="POLYGON">
                  多边形
                </a-radio>
              </a-radio-group>
            </a-form-item>

            <template v-if="fenceForm.fenceType === 'CIRCLE'">
              <a-form-item label="中心点">
                <div class="coord-row">
                  <a-input-number
                    v-model:value="fenceForm.centerLat"
                    :precision="6"
                    placeholder="纬度"
                    style="width: 120px"
                  />
                  <a-input-number
                    v-model:value="fenceForm.centerLng"
                    :precision="6"
                    placeholder="经度"
                    style="width: 120px"
                  />
                  <a-tooltip
                    title="从地图拾取中心点"
                    placement="bottom"
                  >
                    <a-button
                      size="small"
                      :type="pickTarget === 'fenceCenter' ? 'primary' : 'default'"
                      @click="armPick('fenceCenter')"
                    >
                      <EnvironmentOutlined />
                    </a-button>
                  </a-tooltip>
                </div>
              </a-form-item>
              <a-form-item label="半径(米)">
                <a-input-number
                  v-model:value="fenceForm.radiusMeters"
                  :min="1"
                  :precision="2"
                  style="width: 160px"
                />
              </a-form-item>
            </template>

            <a-form-item
              v-else
              label="多边形"
            >
              <a-textarea
                v-model:value="fenceForm.polygonPoints"
                :rows="3"
                placeholder="顶点串：lng,lat;lng,lat;…（也可在右侧地图上点击绘制）"
              />
              <div class="polygon-actions">
                <a-button
                  size="small"
                  @click="startPolygonDraw"
                >
                  地图绘制
                </a-button>
                <a-button
                  size="small"
                  :disabled="!fenceForm.polygonPoints"
                  @click="syncDrawFromText"
                >
                  文本 → 地图
                </a-button>
                <span class="polygon-hint">顶点数：{{ polygonVertexCount }}</span>
              </div>
            </a-form-item>

            <a-form-item label="绑定对象">
              <a-space>
                <a-select
                  v-model:value="fenceForm.bizType"
                  style="width: 120px"
                  placeholder="业务类型"
                  allow-clear
                  @change="handleBizTypeChange"
                >
                  <a-select-option value="ROUTE">
                    线路档案
                  </a-select-option>
                  <a-select-option value="CHANNEL">
                    运力渠道
                  </a-select-option>
                  <a-select-option value="WAREHOUSE">
                    仓库区域
                  </a-select-option>
                  <a-select-option value="OTHER">
                    其它
                  </a-select-option>
                </a-select>
                <template v-if="fenceForm.bizType === 'ROUTE'">
                  <a-select
                    v-model:value="fenceForm.bizId"
                    style="width: 220px"
                    placeholder="选择线路档案"
                    allow-clear
                    show-search
                    option-filter-prop="label"
                    @change="handleBizObjectChange"
                  >
                    <a-select-option
                      v-for="r in routeOptions"
                      :key="r.id"
                      :value="String(r.id)"
                      :label="r.routeName"
                    >
                      {{ r.routeName }}
                    </a-select-option>
                  </a-select>
                </template>
                <template v-else-if="fenceForm.bizType === 'CHANNEL'">
                  <a-select
                    v-model:value="fenceForm.bizId"
                    style="width: 220px"
                    placeholder="选择运力渠道"
                    allow-clear
                    show-search
                    option-filter-prop="label"
                    @change="handleBizObjectChange"
                  >
                    <a-select-option
                      v-for="c in channelOptions"
                      :key="c.id"
                      :value="String(c.id)"
                      :label="c.channelName"
                    >
                      {{ c.channelName }}
                    </a-select-option>
                  </a-select>
                </template>
                <a-input
                  v-else-if="fenceForm.bizType"
                  v-model:value="fenceForm.bizId"
                  style="width: 220px"
                  placeholder="业务对象ID/名称"
                  allow-clear
                  @blur="syncBizName"
                />
              </a-space>
            </a-form-item>

            <a-form-item label="状态">
              <a-radio-group v-model:value="fenceForm.status">
                <a-radio value="ENABLED">
                  启用
                </a-radio>
                <a-radio value="DISABLED">
                  停用
                </a-radio>
              </a-radio-group>
            </a-form-item>

            <a-form-item label="备注">
              <a-textarea
                v-model:value="fenceForm.remark"
                :rows="2"
                :maxlength="500"
              />
            </a-form-item>
          </a-form>

          <div class="fence-map">
            <RouteMapCanvas
              ref="fenceMapRef"
              title="围栏几何"
              height="100%"
              :markers="fenceFormMarkers"
              :circle="fenceFormCircle"
              :polygon="fenceFormPolygon"
              :draw-mode="fenceForm.fenceType === 'POLYGON' && drawingPolygon ? 'polygon' : 'none'"
              :interactive="fenceForm.fenceType === 'CIRCLE' || drawingPolygon"
              empty-text="圆形：地图拾取中心点；多边形：点击「地图绘制」后依次点击顶点"
              @pick="handleFenceModalPick"
              @polygon-change="handlePolygonChange"
            />
          </div>
        </div>
      </a-modal>

      <!-- ═══ 生成配送路线单 ═══ -->
      <a-modal
        v-model:open="generateModalVisible"
        title="生成配送路线单"
        :width="620"
        :mask-closable="false"
        :confirm-loading="generating"
        ok-text="生成"
        cancel-text="取消"
        @ok="handleGenerateRoute"
      >
        <a-alert
          type="info"
          show-icon
          message="将当前规划结果的点位（起点 + 各目的地）写入《配送路线单》执行单，可在「配送 → 配送路线 → 配送路线单」继续派车与签收。"
          style="margin-bottom: 12px"
        />
        <a-form
          :label-col="{ span: 5 }"
          :wrapper-col="{ span: 18 }"
          size="small"
        >
          <a-form-item
            label="配送员"
            required
          >
            <a-select
              v-model:value="generateForm.deliveryPersonId"
              placeholder="选择配送员"
              show-search
              option-filter-prop="label"
            >
              <a-select-option
                v-for="r in riderOptions"
                :key="r.id"
                :value="String(r.id)"
                :label="r.realName"
              >
                {{ r.realName }}（{{ r.phone }}）
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="线路档案">
            <a-select
              v-model:value="generateForm.routeId"
              placeholder="可选，选择线路档案"
              allow-clear
              show-search
              option-filter-prop="label"
            >
              <a-select-option
                v-for="r in routeOptions"
                :key="r.id"
                :value="String(r.id)"
                :label="r.routeName"
              >
                {{ r.routeName }}
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="计划日期">
            <a-date-picker
              v-model:value="generateForm.planDate"
              style="width: 100%"
              value-format="YYYY-MM-DD"
            />
          </a-form-item>
          <a-form-item label="起始地址">
            <a-input
              v-model:value="generateForm.startPoint"
              :maxlength="200"
            />
          </a-form-item>
          <a-form-item label="点位数量">
            <span class="generate-count">{{ generatePoints.length }} 个</span>
          </a-form-item>
        </a-form>
      </a-modal>
    </PageContainer>
    <!-- 打印：结果集打印 -->
    <PrintDialog
      ref="printDialogRef"
      page-code="dms-route"
      :print-data="printData"
    />
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import {
  PlusOutlined,
  ReloadOutlined,
  PrinterOutlined,
  DownloadOutlined,
  EnvironmentOutlined,
  SearchOutlined,
  SwapOutlined,
  ThunderboltOutlined,
  ExportOutlined,
  ApiOutlined,
} from '@ant-design/icons-vue'
import * as XLSX from 'xlsx'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillTableList from '@/components/BillTableList/BillTableList.vue'
import StandardPagination from '@/components/Pagination/Pagination.vue'
import RouteMapCanvas from '@/components/business/RouteMapCanvas/index.vue'
import {
  routeApi,
  geoFenceApi,
  deliveryRouteApi,
  type DmsGeoFence,
  type DmsRouteConfig,
  type DmsRoutePlanResult,
  type DmsGeocodeResult,
  type DmsReverseGeocodeResult,
  type DmsFenceCheckResult,
} from '@/api/dms/route'
import { riderApi } from '@/api/dms/rider'
import { channelApi } from '@/api/dms/channel'
import { mdRouteApi } from '@/api/md'
import PrintDialog from '@/components/PrintDialog/index.vue'
import { useListPrint } from '@/composables/useListPrint'

const locale = { emptyText: '暂无数据' }

const router = useRouter()

const STOP_TYPE_TEXT: Record<string, string> = { start: '起点', waypoint: '途经点', destination: '终点' }
const STOP_TYPE_COLOR: Record<string, string> = { start: 'green', waypoint: 'blue', destination: 'orange' }
const TRAVEL_MODE_TEXT: Record<string, string> = { DRIVING: '驾车', CYCLING: '骑行', WALKING: '步行' }
const KEY_SOURCE_TEXT: Record<string, string> = {
  ENV: '环境变量',
  SPRING: '配置文件',
  CONFIG: '配送参数',
  NONE: '未配置',
}
const BIZ_TYPE_TEXT: Record<string, string> = { ROUTE: '线路档案', CHANNEL: '运力渠道', WAREHOUSE: '仓库区域', OTHER: '其它' }

type PickTarget = '' | 'planOrigin' | `planDest:${number}` | 'reverse' | 'convert' | 'fenceCheck' | 'fenceCenter'

// ═══ 能力菜单 ═══
const CAPABILITIES = [
  { id: 'plan', categoryName: '路线规划' },
  { id: 'geocode', categoryName: '地址编码' },
  { id: 'reverse', categoryName: '逆编码' },
  { id: 'convert', categoryName: '坐标转换' },
  { id: 'fence', categoryName: '围栏管理' },
]
const capabilityTree = CAPABILITIES
const activeCapability = ref('plan')
const currentCapabilityName = computed(
  () => CAPABILITIES.find((c) => c.id === activeCapability.value)?.categoryName || '',
)

// ═══ 配置状态（降级提示） ═══
const routeConfig = ref<DmsRouteConfig | null>(null)

async function loadConfig() {
  try {
    routeConfig.value = await routeApi.config()
  } catch (error) {
    console.warn('[路线规划] 地图服务配置加载失败', error)
  }
}

/** 未配置 Key 时一键跳《配送参数》配置中心（map.* 键在那里编辑，保存即热生效） */
function goConfigCenter() {
  router.push('/dms/config-params')
}

// ═══ 连通性自检（用当前 Key 真实调一次地理编码） ═══
const verifyLoading = ref(false)
const verifyResult = ref<any>(null)

async function handleVerify() {
  verifyLoading.value = true
  try {
    verifyResult.value = await routeApi.verify()
    if (verifyResult.value?.ok) {
      message.success(verifyResult.value.message || '地图服务连通正常')
    } else {
      message.warning(verifyResult.value?.message || '连通性异常')
    }
    await loadConfig()
  } catch (error: any) {
    message.error(error?.response?.data?.message || '连通性自检失败')
  } finally {
    verifyLoading.value = false
  }
}

// ═══ 地图拾取 ═══
const pickTarget = ref<PickTarget>('')

function armPick(target: PickTarget) {
  pickTarget.value = pickTarget.value === target ? '' : target
}

function handleMapPick(lat: number, lng: number) {
  switch (pickTarget.value) {
    case 'planOrigin':
      planForm.origin.lat = lat
      planForm.origin.lng = lng
      break
    case 'reverse':
      reverseForm.lat = lat
      reverseForm.lng = lng
      break
    case 'convert':
      convertForm.lat = lat
      convertForm.lng = lng
      break
    case 'fenceCheck':
      checkForm.lat = lat
      checkForm.lng = lng
      break
    case 'fenceCenter':
      fenceForm.centerLat = lat
      fenceForm.centerLng = lng
      break
    default:
      if (pickTarget.value.startsWith('planDest:')) {
        const index = Number(pickTarget.value.split(':')[1])
        const dest = planForm.destinations[index]
        if (dest) {
          dest.lat = lat
          dest.lng = lng
        }
      }
  }
  pickTarget.value = ''
}

// ═══ 路线规划 ═══
interface PlanDestination {
  address: string
  lat?: number
  lng?: number
  /** 需求量（重量/件数）——填写后按「单车载重」分批 */
  demand?: number
  /** 时间窗起 "HH:mm" */
  windowStart?: string
  /** 时间窗止 "HH:mm"（填写后该点优先配送，晚到会告警） */
  windowEnd?: string
  /** 停留分钟数（计入 ETA 推算） */
  stayMinutes?: number
}

const planForm = reactive({
  origin: { address: '', lat: undefined as number | undefined, lng: undefined as number | undefined },
  destinations: [{ address: '', lat: undefined, lng: undefined }] as PlanDestination[],
  direction: 'DRIVING',
  strategy: 0,
  /** VRP 约束：单车载重上限（空=不分批） */
  vehicleCapacity: undefined as number | undefined,
  /** 可用车辆数（批次超出时告警） */
  vehicleCount: undefined as number | undefined,
  /** 出发时间 "HH:mm"（ETA 与时间窗校核的基准） */
  departureTime: undefined as string | undefined,
  /** 硬时间窗：晚到点移出排程（进入 unassigned），而非仅告警 */
  hardTimeWindow: false,
})
const planLoading = ref(false)
const planResult = ref<DmsRoutePlanResult | null>(null)
const reoptimizeVisited = ref(0)

const stopColumns: any[] = [
  { title: '序号', dataIndex: 'index', key: 'index', width: 60 },
  { title: '类型', dataIndex: 'type', key: 'type', width: 90 },
  { title: '地址/客户', dataIndex: 'address', key: 'address', ellipsis: true },
  { title: '纬度', dataIndex: 'lat', key: 'lat', width: 110 },
  { title: '经度', dataIndex: 'lng', key: 'lng', width: 110 },
  { title: '批次', dataIndex: 'batchNo', key: 'batchNo', width: 70 },
  { title: '需求', dataIndex: 'demand', key: 'demand', width: 80 },
  { title: '时间窗', key: 'timeWindow', width: 120 },
  { title: '预计到达', dataIndex: 'eta', key: 'eta', width: 90 },
  { title: '距上点', key: 'distanceFromPrev', width: 100 },
  { title: '时长', key: 'durationFromPrev', width: 90 },
  { title: '围栏校验', key: 'fenceState', width: 100 },
  { title: '操作', key: 'action', width: 160 },
]

const planMarkers = computed(() => {
  const markers: any[] = []
  if (Number.isFinite(planForm.origin.lat) && Number.isFinite(planForm.origin.lng)) {
    markers.push({ key: 'origin', lat: planForm.origin.lat, lng: planForm.origin.lng, label: '起点', type: 'start' })
  }
  planForm.destinations.forEach((d, i) => {
    if (Number.isFinite(d.lat) && Number.isFinite(d.lng)) {
      markers.push({
        key: `dest-${i}`,
        lat: d.lat as number,
        lng: d.lng as number,
        label: d.address?.trim() || `目的地${i + 1}`,
        type: 'waypoint',
      })
    }
  })
  return markers
})

const planPolyline = computed(() =>
  (planResult.value?.routePoints || []).map((p) => ({ lat: p.lat, lng: p.lng })),
)

function addDestination() {
  planForm.destinations.push({ address: '', lat: undefined, lng: undefined })
}

function removeDestination(index: number) {
  planForm.destinations.splice(index, 1)
}

function collectPlanStops() {
  const stops: Array<Record<string, any>> = []
  planForm.destinations.forEach((d, i) => {
    if (Number.isFinite(d.lat) && Number.isFinite(d.lng)) {
      stops.push({
        lat: d.lat as number,
        lng: d.lng as number,
        address: d.address?.trim() || `目的地${i + 1}`,
        demand: d.demand ?? undefined,
        timeWindowStart: d.windowStart || undefined,
        timeWindowEnd: d.windowEnd || undefined,
        stayDuration: d.stayMinutes ? Math.round(Number(d.stayMinutes) * 60) : undefined,
      })
    }
  })
  return stops
}

async function handlePlanRoute() {
  if (!Number.isFinite(planForm.origin.lat) || !Number.isFinite(planForm.origin.lng)) {
    message.warning('请填写或拾取起点坐标')
    return
  }
  const stops = collectPlanStops()
  if (stops.length === 0) {
    message.warning('请至少填写一个目的地坐标')
    return
  }
  planLoading.value = true
  try {
    const res = await routeApi.plan({
      origin: {
        lat: planForm.origin.lat as number,
        lng: planForm.origin.lng as number,
        address: planForm.origin.address || '起点',
      },
      destinations: stops as any,
      direction: planForm.direction,
      strategy: planForm.strategy,
      vehicleCapacity: planForm.vehicleCapacity ?? undefined,
      vehicleCount: planForm.vehicleCount ?? undefined,
      departureTime: planForm.departureTime || undefined,
      hardTimeWindow: planForm.hardTimeWindow || undefined,
    })
    planResult.value = res
    stopFenceMap.value = {}
    if (!res.success) {
      message.warning(res.message || '规划失败')
    } else {
      message.success(res.degraded ? '已按直线距离降级规划' : '路线规划成功')
    }
  } catch (error: any) {
    message.error(error?.message || '路线规划失败')
  } finally {
    planLoading.value = false
  }
}

async function handleReoptimize() {
  if (!Number.isFinite(planForm.origin.lat) || !Number.isFinite(planForm.origin.lng)) {
    message.warning('请填写或拾取当前位置（起点）坐标')
    return
  }
  const stops = collectPlanStops()
  if (stops.length === 0) {
    message.warning('请至少填写一个剩余目的地')
    return
  }
  planLoading.value = true
  try {
    const res = await routeApi.reoptimize({
      origin: {
        lat: planForm.origin.lat as number,
        lng: planForm.origin.lng as number,
        address: planForm.origin.address || '当前位置',
      },
      destinations: stops as any,
      direction: planForm.direction,
      strategy: planForm.strategy,
      visitedCount: reoptimizeVisited.value || 0,
      vehicleCapacity: planForm.vehicleCapacity ?? undefined,
      vehicleCount: planForm.vehicleCount ?? undefined,
      departureTime: planForm.departureTime || undefined,
    })
    planResult.value = res
    message.success(res.message || '已重新规划')
  } catch (error: any) {
    message.error(error?.message || '重新规划失败')
  } finally {
    planLoading.value = false
  }
}

/** 把结果表中的某点设为起点（其余点保持为目的地） */
function useStopAsOrigin(record: any) {
  if (record.type === 'start') return
  planForm.origin.lat = record.lat
  planForm.origin.lng = record.lng
  planForm.origin.address = record.address || '起点'
  message.success('已设为起点，可重新规划')
}

/** 从规划结果中移除某个目的地（同步回参数区） */
function removeStop(record: any) {
  if (record.sourceIndex == null) return
  const idx = Number(record.sourceIndex)
  if (idx >= 0 && idx < planForm.destinations.length) {
    planForm.destinations.splice(idx, 1)
    if (planForm.destinations.length === 0) {
      planForm.destinations.push({ address: '', lat: undefined, lng: undefined })
    }
    message.success('已从规划参数中移除该目的地')
  }
}

// ═══ 底图（高德 JS SDK）加载失败：已自动回退矢量画布，仅提示一次 ═══
const baseMapWarned = ref(false)
function handleBaseMapFailed(msg: string) {
  if (baseMapWarned.value) return
  baseMapWarned.value = true
  message.warning('底图加载失败，已回退矢量画布：' + msg)
}

/** 地图标记拖拽结束 → 回写起点/目的地坐标（其余指标待重新规划刷新） */
function handleMarkerMove(payload: { key?: string; index: number; lat: number; lng: number }) {
  if (payload.key === 'origin') {
    planForm.origin.lat = payload.lat
    planForm.origin.lng = payload.lng
    message.success('已更新起点坐标，可重新规划')
    return
  }
  const matched = /^dest-(\d+)$/.exec(payload.key || '')
  const idx = matched ? Number(matched[1]) : payload.index - 1
  const dest = planForm.destinations[idx]
  if (dest) {
    dest.lat = payload.lat
    dest.lng = payload.lng
    message.success(`已更新目的地 ${idx + 1} 坐标，可重新规划`)
  }
}

// ═══ 地址编码 ═══
const geocodeForm = reactive({ address: '', city: '' })
const geocodeLoading = ref(false)
const geocodeResult = ref<DmsGeocodeResult | null>(null)
const selectedCandidateIndex = ref(0)

const geocodeMarkers = computed(() => {
  const list = geocodeResult.value?.candidates || []
  return list.map((c, i) => ({
    lat: c.lat,
    lng: c.lng,
    label: i === selectedCandidateIndex.value ? c.formattedAddress || `候选${i + 1}` : undefined,
    type: 'point',
  }))
})

async function handleGeocode() {
  if (!geocodeForm.address.trim()) {
    message.warning('请输入地址')
    return
  }
  geocodeLoading.value = true
  try {
    const res = await routeApi.geocode(geocodeForm.address.trim(), geocodeForm.city || undefined)
    geocodeResult.value = res
    selectedCandidateIndex.value = 0
    if (!res.success) {
      message.warning(res.message || '地址编码失败')
    } else {
      message.success(`解析到 ${res.candidates?.length || 1} 个候选${res.cached ? '（缓存命中）' : ''}`)
    }
  } catch (error: any) {
    message.error(error?.message || '地址编码失败')
  } finally {
    geocodeLoading.value = false
  }
}

function selectCandidate(index: number) {
  selectedCandidateIndex.value = index
}

/** 候选地址 → 规划参数（起点 / 目的地） */
function applyCandidateToPlan(target: 'origin' | 'destination', index: number) {
  const c = geocodeResult.value?.candidates?.[index]
  if (!c) return
  const address = c.formattedAddress || geocodeForm.address
  if (target === 'origin') {
    planForm.origin.lat = c.lat
    planForm.origin.lng = c.lng
    planForm.origin.address = address
    message.success('已填入起点')
  } else {
    const empty = planForm.destinations.find((d) => !Number.isFinite(d.lat))
    if (empty) {
      empty.lat = c.lat
      empty.lng = c.lng
      empty.address = address
    } else {
      planForm.destinations.push({ lat: c.lat, lng: c.lng, address })
    }
    message.success('已填入目的地')
  }
}

// ═══ 逆编码 ═══
const reverseForm = reactive({
  lat: undefined as number | undefined,
  lng: undefined as number | undefined,
  from: 'GCJ02',
})
const reverseLoading = ref(false)
const reverseResult = ref<DmsReverseGeocodeResult | null>(null)

const reverseMarkers = computed(() =>
  Number.isFinite(reverseForm.lat) && Number.isFinite(reverseForm.lng)
    ? [{ lat: reverseForm.lat as number, lng: reverseForm.lng as number, label: reverseResult.value?.formattedAddress || '待校验点', type: 'point' }]
    : [],
)

async function handleReverseGeocode() {
  if (!Number.isFinite(reverseForm.lat) || !Number.isFinite(reverseForm.lng)) {
    message.warning('请填写或拾取经纬度')
    return
  }
  reverseLoading.value = true
  try {
    const res = await routeApi.reverseGeocode({
      lat: reverseForm.lat as number,
      lng: reverseForm.lng as number,
      from: reverseForm.from,
    })
    reverseResult.value = res
    if (!res.success) {
      message.warning(res.message || '逆编码失败')
    }
  } catch (error: any) {
    message.error(error?.message || '逆编码失败')
  } finally {
    reverseLoading.value = false
  }
}

// ═══ 坐标转换 ═══
const convertForm = reactive({
  lat: undefined as number | undefined,
  lng: undefined as number | undefined,
  from: 'WGS84',
  to: 'GCJ02',
})
const convertLoading = ref(false)
const convertResult = ref<{ from: string; to: string; lat: number; lng: number } | null>(null)

const convertMarkers = computed(() => {
  const markers: any[] = []
  if (Number.isFinite(convertForm.lat) && Number.isFinite(convertForm.lng)) {
    markers.push({ lat: convertForm.lat as number, lng: convertForm.lng as number, label: convertForm.from, type: 'waypoint' })
  }
  if (convertResult.value) {
    markers.push({ lat: convertResult.value.lat, lng: convertResult.value.lng, label: convertResult.value.to, type: 'destination' })
  }
  return markers
})

async function handleConvert() {
  if (!Number.isFinite(convertForm.lat) || !Number.isFinite(convertForm.lng)) {
    message.warning('请填写或拾取待转换坐标')
    return
  }
  convertLoading.value = true
  try {
    const res: any = await routeApi.convert({
      lat: convertForm.lat as number,
      lng: convertForm.lng as number,
      from: convertForm.from,
      to: convertForm.to,
    })
    convertResult.value = res
    message.success('转换完成')
  } catch (error: any) {
    message.error(error?.message || '坐标转换失败')
  } finally {
    convertLoading.value = false
  }
}

// ═══ 围栏台账 ═══
const fenceLoading = ref(false)
const fenceList = ref<DmsGeoFence[]>([])
const fenceOptions = ref<DmsGeoFence[]>([])
const fenceQuery = reactive({ keyword: '', fenceType: '', status: 'ENABLED' })
const fencePagination = reactive({ current: 1, pageSize: 20, total: 0 })

const fenceColumns: any[] = [
  { title: '', key: 'rowNo', type: 'rowNo', width: 40, fixed: 'left' },
  { title: '操作', key: 'action', type: 'action', slotName: 'actionCell', width: 170, fixed: 'left' },
  { title: '围栏编码', key: 'fenceCode', width: 110 },
  { title: '围栏名称', key: 'fenceName', type: 'slot', slotName: 'nameCell', width: 180 },
  { title: '围栏类型', key: 'fenceType', type: 'slot', slotName: 'typeCell', width: 100 },
  { title: '几何范围', key: 'geometry', type: 'slot', slotName: 'geometryCell', width: 110 },
  { title: '绑定对象', key: 'biz', type: 'slot', slotName: 'bizCell', width: 220 },
  { title: '状态', key: 'status', type: 'slot', slotName: 'statusCell', width: 90 },
  { title: '备注', key: 'remark', width: 180 },
]

async function loadFences() {
  fenceLoading.value = true
  try {
    const res: any = await geoFenceApi.page({
      pageNum: fencePagination.current,
      pageSize: fencePagination.pageSize,
      keyword: fenceQuery.keyword || undefined,
      fenceType: fenceQuery.fenceType || undefined,
      status: fenceQuery.status || undefined,
    })
    fenceList.value = res?.records || []
    fencePagination.total = Number(res?.total) || 0
  } catch (error: any) {
    message.error(error?.response?.data?.message || '围栏列表加载失败')
  } finally {
    fenceLoading.value = false
  }
}

async function loadFenceOptions() {
  try {
    fenceOptions.value = await geoFenceApi.options()
  } catch (error) {
    console.warn('[路线规划] 围栏下拉加载失败', error)
  }
}

function handleFenceSearch() {
  fencePagination.current = 1
  loadFences()
}

function handleFencePageChange(page: number, pageSize: number) {
  fencePagination.current = page
  fencePagination.pageSize = pageSize
  loadFences()
}

// ═══ 围栏编辑 ═══
const fenceModalVisible = ref(false)
const fenceSaving = ref(false)
const drawingPolygon = ref(false)
const fenceMapRef = ref<InstanceType<typeof RouteMapCanvas> | null>(null)
const fenceForm = reactive({
  id: null as number | string | null,
  fenceCode: '',
  fenceName: '',
  fenceType: 'CIRCLE',
  centerLat: undefined as number | undefined,
  centerLng: undefined as number | undefined,
  radiusMeters: 1000 as number | undefined,
  polygonPoints: '',
  bizType: undefined as string | undefined,
  bizId: undefined as string | undefined,
  bizName: undefined as string | undefined,
  status: 'ENABLED',
  remark: '',
})

const fenceFormCircle = computed(() =>
  fenceForm.fenceType === 'CIRCLE'
    && Number.isFinite(fenceForm.centerLat) && Number.isFinite(fenceForm.centerLng)
    && Number(fenceForm.radiusMeters) > 0
    ? { lat: fenceForm.centerLat as number, lng: fenceForm.centerLng as number, radiusMeters: Number(fenceForm.radiusMeters) }
    : null,
)

const fenceFormPolygon = computed(() => parsePolygonText(fenceForm.polygonPoints))

const fenceFormMarkers = computed(() =>
  fenceForm.fenceType === 'CIRCLE' && Number.isFinite(fenceForm.centerLat) && Number.isFinite(fenceForm.centerLng)
    ? [{ lat: fenceForm.centerLat as number, lng: fenceForm.centerLng as number, label: '中心', type: 'point' }]
    : [],
)

const polygonVertexCount = computed(() => parsePolygonText(fenceForm.polygonPoints).length)

function parsePolygonText(text?: string | null) {
  if (!text) return []
  return text
    .split(';')
    .map((pair) => pair.split(','))
    .filter((xy) => xy.length === 2 && Number.isFinite(Number(xy[0])) && Number.isFinite(Number(xy[1])))
    .map((xy) => ({ lat: Number(xy[1]), lng: Number(xy[0]) }))
}

function startPolygonDraw() {
  drawingPolygon.value = true
  fenceMapRef.value?.setDrawPoints(parsePolygonText(fenceForm.polygonPoints))
}

function syncDrawFromText() {
  drawingPolygon.value = true
  fenceMapRef.value?.setDrawPoints(parsePolygonText(fenceForm.polygonPoints))
}

function handlePolygonChange(points: Array<{ lat: number; lng: number }>) {
  fenceForm.polygonPoints = points.map((p) => `${p.lng},${p.lat}`).join(';')
}

function handleFenceModalPick(lat: number, lng: number) {
  if (fenceForm.fenceType === 'CIRCLE') {
    fenceForm.centerLat = lat
    fenceForm.centerLng = lng
  }
}

async function openFenceModal(record?: DmsGeoFence) {
  drawingPolygon.value = false
  pickTarget.value = ''
  if (record?.id) {
    try {
      const detail: any = await geoFenceApi.detail(record.id)
      Object.assign(fenceForm, {
        id: detail.id,
        fenceCode: detail.fenceCode || '',
        fenceName: detail.fenceName || '',
        fenceType: detail.fenceType || 'CIRCLE',
        centerLat: detail.centerLat != null ? Number(detail.centerLat) : undefined,
        centerLng: detail.centerLng != null ? Number(detail.centerLng) : undefined,
        radiusMeters: detail.radiusMeters != null ? Number(detail.radiusMeters) : undefined,
        polygonPoints: detail.polygonPoints || '',
        bizType: detail.bizType || undefined,
        bizId: detail.bizId || undefined,
        bizName: detail.bizName || undefined,
        status: detail.status || 'ENABLED',
        remark: detail.remark || '',
      })
    } catch (error: any) {
      message.error(error?.response?.data?.message || '围栏详情加载失败')
      return
    }
  } else {
    Object.assign(fenceForm, {
      id: null,
      fenceCode: '',
      fenceName: '',
      fenceType: 'CIRCLE',
      centerLat: undefined,
      centerLng: undefined,
      radiusMeters: 1000,
      polygonPoints: '',
      bizType: undefined,
      bizId: undefined,
      bizName: undefined,
      status: 'ENABLED',
      remark: '',
    })
    try {
      fenceForm.fenceCode = await geoFenceApi.nextCode()
    } catch (error) {
      console.warn('[路线规划] 围栏编码生成失败', error)
    }
  }
  fenceModalVisible.value = true
  loadBizOptions()
}

// 绑定对象下拉（线路档案 / 运力渠道）
const routeOptions = ref<Array<{ id: number; routeName: string }>>([])
const channelOptions = ref<Array<{ id: number; channelName: string }>>([])
const bizOptionsLoaded = ref(false)

async function loadBizOptions() {
  if (bizOptionsLoaded.value) return
  bizOptionsLoaded.value = true
  try {
    const routes: any = await mdRouteApi.options()
    routeOptions.value = (routes || []).map((r: any) => ({ id: r.id, routeName: r.routeName || r.name }))
  } catch (error) {
    console.warn('[路线规划] 线路档案下拉加载失败', error)
  }
  try {
    const channels: any = await channelApi.page({ pageNum: 1, pageSize: 200 })
    channelOptions.value = (channels?.records || channels?.list || []).map((c: any) => ({ id: c.id, channelName: c.channelName }))
  } catch (error) {
    console.warn('[路线规划] 运力渠道下拉加载失败', error)
  }
}

function handleBizTypeChange() {
  fenceForm.bizId = undefined
  fenceForm.bizName = undefined
}

function handleBizObjectChange(value: any) {
  if (fenceForm.bizType === 'ROUTE') {
    fenceForm.bizName = routeOptions.value.find((r) => String(r.id) === String(value))?.routeName
  } else if (fenceForm.bizType === 'CHANNEL') {
    fenceForm.bizName = channelOptions.value.find((c) => String(c.id) === String(value))?.channelName
  }
}

function syncBizName() {
  if (!fenceForm.bizName) fenceForm.bizName = fenceForm.bizId
}

async function handleFenceSave() {
  if (!fenceForm.fenceName.trim()) {
    message.warning('请输入围栏名称')
    return
  }
  if (fenceForm.fenceType === 'CIRCLE') {
    if (!Number.isFinite(fenceForm.centerLat) || !Number.isFinite(fenceForm.centerLng) || !(Number(fenceForm.radiusMeters) > 0)) {
      message.warning('圆形围栏需填写中心点与大于 0 的半径')
      return
    }
  } else if (polygonVertexCount.value < 3) {
    message.warning('多边形围栏至少需要 3 个顶点（可在地图上绘制）')
    return
  }

  const payload: DmsGeoFence = {
    fenceCode: fenceForm.fenceCode || undefined,
    fenceName: fenceForm.fenceName.trim(),
    fenceType: fenceForm.fenceType,
    centerLat: fenceForm.fenceType === 'CIRCLE' ? Number(fenceForm.centerLat) : null,
    centerLng: fenceForm.fenceType === 'CIRCLE' ? Number(fenceForm.centerLng) : null,
    radiusMeters: fenceForm.fenceType === 'CIRCLE' ? Number(fenceForm.radiusMeters) : null,
    polygonPoints: fenceForm.fenceType === 'POLYGON' ? fenceForm.polygonPoints : null,
    bizType: fenceForm.bizType,
    bizId: fenceForm.bizId,
    bizName: fenceForm.bizName,
    status: fenceForm.status,
    remark: fenceForm.remark,
  }

  fenceSaving.value = true
  try {
    if (fenceForm.id) {
      await geoFenceApi.update(fenceForm.id, payload)
      message.success('修改成功')
    } else {
      await geoFenceApi.create(payload)
      message.success('新增成功')
    }
    fenceModalVisible.value = false
    loadFences()
    loadFenceOptions()
  } catch (error: any) {
    message.error(error?.response?.data?.message || '保存失败')
  } finally {
    fenceSaving.value = false
  }
}

function toggleFenceStatus(record: DmsGeoFence) {
  const target = record.status === 'ENABLED' ? 'DISABLED' : 'ENABLED'
  const text = target === 'ENABLED' ? '启用' : '停用'
  Modal.confirm({
    title: '确认',
    content: `确定要${text}围栏「${record.fenceName}」吗？`,
    onOk: async () => {
      try {
        await geoFenceApi.updateStatus(record.id as number | string, target)
        message.success(`已${text}`)
        loadFences()
        loadFenceOptions()
      } catch (error: any) {
        message.error(error?.response?.data?.message || '操作失败')
      }
    },
  })
}

function removeFence(record: DmsGeoFence) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除围栏「${record.fenceName}」吗？此操作不可恢复。`,
    okText: '确认删除',
    okType: 'danger',
    onOk: async () => {
      try {
        await geoFenceApi.remove(record.id as number | string)
        message.success('删除成功')
        loadFences()
        loadFenceOptions()
      } catch (error: any) {
        message.error(error?.response?.data?.message || '删除失败')
      }
    },
  })
}

// ═══ 围栏校验 ═══
const checkFenceId = ref<number | string | undefined>(undefined)
const checkForm = reactive({ lat: undefined as number | undefined, lng: undefined as number | undefined })
const fenceCheckLoading = ref(false)
const fenceCheckResult = ref<DmsFenceCheckResult | null>(null)
const stopFenceMap = ref<Record<number, boolean>>({})

const checkResultColumns: any[] = [
  { title: '纬度', dataIndex: 'lat', key: 'lat', width: 110 },
  { title: '经度', dataIndex: 'lng', key: 'lng', width: 110 },
  { title: '结果', dataIndex: 'inside', key: 'inside', width: 90 },
  { title: '距离', dataIndex: 'distanceMeters', key: 'distanceMeters', width: 110 },
]

const fenceCheckMarkers = computed(() =>
  Number.isFinite(checkForm.lat) && Number.isFinite(checkForm.lng)
    ? [{ lat: checkForm.lat as number, lng: checkForm.lng as number, label: '待校验点', type: 'point' }]
    : [],
)

/** 选中的围栏（用于地图预览） */
const selectedFence = computed(() => fenceOptions.value.find((f) => f.id === checkFenceId.value) || null)

const fencePreviewCircle = computed(() => {
  const f = selectedFence.value
  if (!f || f.fenceType !== 'CIRCLE' || f.centerLat == null || f.centerLng == null) return null
  return { lat: Number(f.centerLat), lng: Number(f.centerLng), radiusMeters: Number(f.radiusMeters || 0) }
})

const fencePreviewPolygon = computed(() =>
  selectedFence.value?.fenceType === 'POLYGON' ? parsePolygonText(selectedFence.value.polygonPoints) : [],
)

function syncCheckFenceGeometry() {
  // 选中围栏后地图几何由 computed 自动联动，这里仅清空上一次校验结果
  fenceCheckResult.value = null
}

async function handleFenceCheck() {
  if (!checkFenceId.value) {
    message.warning('请选择围栏')
    return
  }
  if (!Number.isFinite(checkForm.lat) || !Number.isFinite(checkForm.lng)) {
    message.warning('请填写或拾取待校验点坐标')
    return
  }
  fenceCheckLoading.value = true
  try {
    fenceCheckResult.value = await routeApi.fenceCheck({
      fenceId: checkFenceId.value,
      lat: checkForm.lat as number,
      lng: checkForm.lng as number,
    })
  } catch (error: any) {
    message.error(error?.response?.data?.message || '围栏校验失败')
  } finally {
    fenceCheckLoading.value = false
  }
}

/** 批量校验当前规划结果的点位是否落在所选围栏内 */
async function checkPlanStopsInFence() {
  if (!checkFenceId.value) {
    message.warning('请先选择围栏')
    return
  }
  const stops = planResult.value?.stops || []
  if (stops.length === 0) {
    message.warning('请先完成路线规划')
    return
  }
  fenceCheckLoading.value = true
  try {
    const res = await routeApi.fenceCheck({
      fenceId: checkFenceId.value,
      points: stops.map((s) => ({ lat: s.lat, lng: s.lng, address: s.address })),
    })
    fenceCheckResult.value = res
    const map: Record<number, boolean> = {}
    ;(res.results || []).forEach((item, i) => {
      const stop = stops[i]
      if (stop) map[stop.index] = item.inside
    })
    stopFenceMap.value = map
    const insideCount = (res.results || []).filter((r) => r.inside).length
    message.success(`围栏内 ${insideCount} / ${stops.length} 个点位`)
  } catch (error: any) {
    message.error(error?.response?.data?.message || '批量校验失败')
  } finally {
    fenceCheckLoading.value = false
  }
}

function stopFenceResult(index: number): boolean | undefined {
  return stopFenceMap.value[index]
}

// ═══ 生成配送路线单 ═══
const generateModalVisible = ref(false)
const generating = ref(false)
const riderOptions = ref<Array<{ id: number; realName: string; phone: string }>>([])
const generateForm = reactive({
  deliveryPersonId: undefined as string | undefined,
  routeId: undefined as string | undefined,
  planDate: undefined as string | undefined,
  startPoint: '',
})

const generatePoints = computed(() =>
  (planResult.value?.stops || [])
    .filter((s) => s.type !== 'start')
    .map((s) => ({
      pointOrder: s.index,
      customerName: s.address || `配送点${s.index}`,
      address: s.address || `${s.lat},${s.lng}`,
      latitude: String(s.lat),
      longitude: String(s.lng),
    })),
)

async function openGenerateModal() {
  if (!planResult.value?.stops?.length) {
    message.warning('请先完成路线规划')
    return
  }
  generateForm.deliveryPersonId = undefined
  generateForm.routeId = undefined
  generateForm.planDate = new Date().toISOString().slice(0, 10)
  generateForm.startPoint = planForm.origin.address || planResult.value.stops?.[0]?.address || ''
  generateModalVisible.value = true
  loadRiders()
}

async function loadRiders() {
  try {
    const res: any = await riderApi.page({ pageNum: 1, pageSize: 200 })
    const rows = res?.records || res?.list || []
    riderOptions.value = rows.map((r: any) => ({ id: r.id, realName: r.realName || r.riderName, phone: r.phone || '' }))
  } catch (error) {
    console.warn('[路线规划] 配送员下拉加载失败', error)
  }
}

async function handleGenerateRoute() {
  if (!generateForm.deliveryPersonId) {
    message.warning('请选择配送员')
    return
  }
  const points = generatePoints.value
  if (points.length === 0) {
    message.warning('当前规划结果没有可生成的配送点位')
    return
  }
  generating.value = true
  try {
    await deliveryRouteApi.create({
      deliveryPersonId: generateForm.deliveryPersonId,
      deliveryPersonName: riderOptions.value.find((r) => String(r.id) === generateForm.deliveryPersonId)?.realName,
      routeId: generateForm.routeId ? Number(generateForm.routeId) : undefined,
      planDate: generateForm.planDate,
      startPoint: generateForm.startPoint,
      startLatitude: planForm.origin.lat != null ? String(planForm.origin.lat) : undefined,
      startLongitude: planForm.origin.lng != null ? String(planForm.origin.lng) : undefined,
      points: points as any,
    })
    message.success('已生成配送路线单，可前往「配送路线 → 配送路线单」查看')
    generateModalVisible.value = false
  } catch (error: any) {
    message.error(error?.response?.data?.message || '生成配送路线单失败')
  } finally {
    generating.value = false
  }
}

// ═══ 能力切换 / 刷新 / 打印 / 导出 ═══
function onCapabilitySelect(keys: (string | number)[]) {
  const key = keys?.[0]
  if (!key) return
  activeCapability.value = String(key)
  pickTarget.value = ''
  if (activeCapability.value === 'fence') {
    loadFences()
    loadFenceOptions()
  }
}

async function handleRefreshAll() {
  pickTarget.value = ''
  await loadConfig()
  if (activeCapability.value === 'fence') {
    await loadFences()
    await loadFenceOptions()
  } else if (activeCapability.value === 'plan' && planResult.value) {
    await handlePlanRoute()
  }
  message.success('已刷新')
}

// ═══ 打印（结果集打印） ═══
// 原先是 window.print() —— 打出来是整个后台界面（菜单/工具栏/翻页都跟着上纸）。
// 列取页面自己的列定义，模板按数据里的列画表头（改列不用改模板）。
const { printDialogRef, printData, handlePrint } = useListPrint({
  pageCode: 'dms-route',
  title: '配送路线',
  columns: () => stopColumns,
  rows: () => planResult.value?.stops || [],
  emptyTip: '没有可打印的数据',
})

const exportRows = computed<Record<string, any>[]>(() => {
  if (activeCapability.value === 'plan') {
    return (planResult.value?.stops || []).map((s) => ({
      序号: s.index,
      批次: s.batchNo ?? '',
      类型: STOP_TYPE_TEXT[s.type] || s.type,
      地址: s.address || '',
      纬度: s.lat,
      经度: s.lng,
      需求量: s.demand ?? '',
      时间窗起: s.timeWindowStart || '',
      时间窗止: s.timeWindowEnd || '',
      预计到达: s.eta || '',
      距上点米: s.distanceFromPrev ?? '',
      时长秒: s.durationFromPrev ?? '',
    }))
  }
  if (activeCapability.value === 'geocode') {
    return (geocodeResult.value?.candidates || []).map((c, i) => ({
      序号: i + 1,
      地址: c.formattedAddress || '',
      纬度: c.lat,
      经度: c.lng,
      省: c.province || '',
      市: c.city || '',
      区县: c.district || '',
      匹配级别: c.level || '',
    }))
  }
  if (activeCapability.value === 'reverse') {
    return reverseResult.value
      ? [{
          完整地址: reverseResult.value.formattedAddress || '',
          省: reverseResult.value.province || '',
          市: reverseResult.value.city || '',
          区县: reverseResult.value.district || '',
          街道: reverseResult.value.street || '',
          门牌号: reverseResult.value.streetNumber || '',
          纬度: reverseResult.value.lat ?? '',
          经度: reverseResult.value.lng ?? '',
        }]
      : []
  }
  if (activeCapability.value === 'convert') {
    return convertResult.value
      ? [{
          源体系: convertResult.value.from,
          目标体系: convertResult.value.to,
          原纬度: convertForm.lat ?? '',
          原经度: convertForm.lng ?? '',
          转换后纬度: convertResult.value.lat,
          转换后经度: convertResult.value.lng,
        }]
      : []
  }
  return fenceList.value.map((f) => ({
    围栏编码: f.fenceCode || '',
    围栏名称: f.fenceName,
    围栏类型: f.fenceType === 'POLYGON' ? '多边形' : '圆形',
    半径米: f.radiusMeters ?? '',
    顶点串: f.polygonPoints || '',
    绑定类型: f.bizType ? BIZ_TYPE_TEXT[f.bizType] || f.bizType : '',
    绑定对象: f.bizName || f.bizId || '',
    状态: f.status === 'ENABLED' ? '已启用' : '已停用',
    备注: f.remark || '',
  }))
})

const exportFileName = computed(() => {
  const names: Record<string, string> = {
    plan: '路线规划结果',
    geocode: '地址编码候选',
    reverse: '逆编码结果',
    convert: '坐标转换结果',
    fence: '电子围栏',
  }
  return `${names[activeCapability.value] || '导出'}_${new Date().toISOString().slice(0, 10)}.xlsx`
})

function handleExport() {
  const rows = exportRows.value
  if (rows.length === 0) {
    message.warning('没有可导出的数据')
    return
  }
  const sheet = XLSX.utils.json_to_sheet(rows)
  const book = XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(book, sheet, 'Sheet1')
  XLSX.writeFile(book, exportFileName.value)
  message.success('导出成功')
}

// ═══ 格式化 ═══
function formatDistance(meters?: number | null) {
  if (meters == null || !Number.isFinite(Number(meters))) return '-'
  const value = Number(meters)
  return value >= 1000 ? `${(value / 1000).toFixed(2)} km` : `${Math.round(value)} m`
}

function formatDuration(seconds?: number | null) {
  if (seconds == null || !Number.isFinite(Number(seconds))) return '-'
  const value = Number(seconds)
  if (value < 60) return `${Math.round(value)} 秒`
  return `${Math.round(value / 60)} 分钟`
}

// ═══ 快捷键：F5 刷新 / F8 打印 / Esc 关闭弹窗 ═══
function handleKeydown(e: KeyboardEvent) {
  if ((e.key === 'F5' || e.code === 'F5') && !e.ctrlKey && !e.altKey && !e.metaKey) {
    e.preventDefault()
    handleRefreshAll()
    return
  }
  if ((e.key === 'F8' || e.code === 'F8') && !e.ctrlKey && !e.altKey && !e.metaKey) {
    e.preventDefault()
    handlePrint()
    return
  }
  if (e.key === 'Enter' && fenceModalVisible.value && !e.ctrlKey && !e.altKey && !e.metaKey) {
    const target = e.target as HTMLElement | null
    if (target && target.tagName === 'TEXTAREA') return
    e.preventDefault()
    handleFenceSave()
  }
}

function handleError(error: Error) {
  console.error('[路线规划] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

onMounted(() => {
  loadConfig()
  window.addEventListener('keydown', handleKeydown)
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', handleKeydown)
})
</script>

<style scoped>
.tool-body {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: auto;
  padding: 10px 12px;
}
.split-row {
  display: flex;
  gap: 12px;
  min-height: 460px;
  height: 100%;
}
.form-pane {
  width: 440px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
}
.form-pane :deep(.ant-card) { height: 100%; display: flex; flex-direction: column; }
.form-pane :deep(.ant-card-body) { flex: 1; min-height: 0; overflow: auto; }
.map-pane { flex: 1; min-width: 0; display: flex; flex-direction: column; }
.map-pane :deep(.route-map) { flex: 1; min-height: 0; }

.coord-row { display: flex; align-items: center; gap: 6px; flex-wrap: wrap; }
.dest-row { border: 1px solid #f0f0f0; border-radius: 4px; padding: 6px 8px; margin-bottom: 6px; }
.dest-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 4px; }
.dest-title { font-size: 12px; color: #606266; }
.thin-divider { margin: 8px 0 !important; font-size: 12px; color: #909399; }

.result-block {
  margin-top: 12px;
  border-top: 1px solid #f0f0f0;
  padding-top: 8px;
}
.result-head { display: flex; align-items: center; justify-content: space-between; gap: 12px; flex-wrap: wrap; }
.result-desc { flex: 1; min-width: 420px; }
.result-desc :deep(.ant-descriptions-item) { padding-bottom: 4px; }

.candidate-list { max-height: 320px; overflow: auto; }
.candidate-item {
  border: 1px solid #f0f0f0;
  border-radius: 4px;
  padding: 6px 8px;
  margin-bottom: 6px;
  cursor: pointer;
}
.candidate-item:hover { border-color: #91caff; }
.candidate-item.active { border-color: #1890ff; background: #f0f7ff; }
.candidate-addr { font-size: 13px; color: #303133; }
.candidate-meta { font-size: 12px; color: #909399; margin-top: 2px; display: flex; align-items: center; gap: 6px; }
.candidate-actions { margin-top: 2px; }

.tips { margin-top: 10px; font-size: 12px; color: #909399; line-height: 1.7; }
.tips p { margin: 0; }

.fence-wrap { display: flex; gap: 12px; flex: 1; min-height: 520px; }
.fence-table { flex: 1.35; min-width: 0; display: flex; flex-direction: column; }
.fence-table :deep(.bill-table-list-container) { flex: 1; min-height: 0; }
.fence-check-pane { width: 330px; flex-shrink: 0; }
.fence-check-pane :deep(.ant-card) { height: 100%; }
.fence-map-pane { flex: 1; min-width: 240px; display: flex; flex-direction: column; }
.fence-map-pane :deep(.route-map) { flex: 1; min-height: 0; }

.check-result { margin-top: 10px; display: flex; align-items: center; gap: 8px; }
.check-distance { font-size: 12px; color: #606266; }

.fence-modal { display: flex; gap: 12px; }
.fence-form { flex: 1; min-width: 0; }
.fence-map { width: 360px; height: 480px; flex-shrink: 0; }
.polygon-actions { display: flex; align-items: center; gap: 8px; margin-top: 6px; }
.polygon-hint { font-size: 12px; color: #909399; }

.degrade-hint {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-top: 6px;
  padding: 4px 8px;
  font-size: 12px;
  color: #d46b08;
  background: #fff7e6;
  border: 1px solid #ffe7ba;
  border-radius: 3px;
}
.degrade-action { padding: 0; height: auto; color: #d46b08; text-decoration: underline; }
.verify-alert { margin-top: 6px; }
.warn-alert { margin-bottom: 8px; }
.warn-list { margin: 0; padding-left: 18px; }
.batch-summary { margin-bottom: 8px; }
.batch-tag { margin-bottom: 4px; }
.constraint-row { margin-top: 4px; }
.config-source-hint {
  margin-top: 6px;
  font-size: 12px;
  color: #52c41a;
}

.search-area { padding: 8px 16px; background: #fff; border-bottom: 1px solid #e8e8e8; flex-shrink: 0; }
.search-row { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.search-item { display: flex; align-items: center; gap: 6px; }
.search-label { font-size: 13px; color: #666; white-space: nowrap; }
.service-tag { margin-right: 4px; }
.generate-count { font-size: 13px; color: #1890ff; }
.cell-link, .fence-table a { color: #1890ff; cursor: pointer; }
.cell-link:hover, .fence-table a:hover { text-decoration: underline; }
.btn-add { background: #ff6b35 !important; border-color: #ff6b35 !important; }

@media print {
  .search-area, .degrade-hint, .route-map, .ant-btn, .ant-pagination { display: none !important; }
}
</style>
