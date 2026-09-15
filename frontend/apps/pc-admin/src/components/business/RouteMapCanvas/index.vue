<template>
  <div
    ref="wrapRef"
    class="route-map"
    :style="{ height }"
  >
    <!-- 工具条 -->
    <div class="map-toolbar">
      <span class="map-title">{{ title }}</span>
      <span class="legend">
        <span
          v-for="item in legendItems"
          :key="item.key"
          class="legend-item"
        >
          <i
            class="legend-dot"
            :style="{ background: item.color }"
          />{{ item.label }}
        </span>
      </span>
      <span class="basemap-tag" :class="baseMapClass">
        底图：{{ baseMapLabel }}
      </span>
      <span class="map-actions">
        <a-button
          v-if="drawMode === 'polygon'"
          size="small"
          :disabled="drawPoints.length === 0"
          @click="undoPoint"
        >
          撤销上一点
        </a-button>
        <a-button
          v-if="drawMode === 'polygon'"
          size="small"
          :disabled="drawPoints.length === 0"
          @click="clearPoints"
        >
          清空
        </a-button>
        <a-button
          size="small"
          @click="resetView"
        >
          重置视野
        </a-button>
      </span>
    </div>

    <!-- 画布 -->
    <div class="map-body">
      <!-- 高德 JS SDK 底图（配置了 JS Key 且加载成功时启用；否则回退下方自绘矢量画布） -->
      <div
        v-show="baseMapReady"
        ref="amapRef"
        class="amap-layer"
      />
      <svg
        v-show="!baseMapReady"
        :width="size.width"
        :height="size.height"
        :viewBox="`0 0 ${size.width} ${size.height}`"
        class="map-svg"
        :class="{ 'is-pickable': interactive }"
        @click="handleClick"
        @mousemove="handleMove"
        @mouseleave="hover = null"
      >
        <!-- 网格 -->
        <g
          v-if="gridLines.length"
          class="map-grid"
        >
          <line
            v-for="(line, i) in gridLines"
            :key="'g' + i"
            :x1="line.x1"
            :y1="line.y1"
            :x2="line.x2"
            :y2="line.y2"
          />
        </g>

        <!-- 圆(半径以米换算像素) -->
        <circle
          v-if="circleShape"
          class="fence-circle"
          :cx="circleShape.cx"
          :cy="circleShape.cy"
          :r="circleShape.r"
        />
        <circle
          v-if="circleShape"
          class="fence-center"
          :cx="circleShape.cx"
          :cy="circleShape.cy"
          r="3"
        />

        <!-- 多边形 -->
        <polygon
          v-if="polygonPx.length >= 3"
          class="fence-polygon"
          :points="polygonPx"
        />
        <polyline
          v-else-if="polygonPx"
          class="fence-polygon-line"
          :points="polygonPx"
        />

        <!-- 绘制中的多边形 -->
        <g v-if="drawMode === 'polygon' && drawPx.length">
          <polyline
            class="draw-line"
            :points="drawPx"
          />
          <circle
            v-for="(p, i) in drawPxList"
            :key="'d' + i"
            class="draw-vertex"
            :cx="p.x"
            :cy="p.y"
            r="4"
          />
        </g>

        <!-- 规划路线 -->
        <polyline
          v-if="linePx"
          class="route-line"
          :points="linePx"
        />

        <!-- 标记点 -->
        <g
          v-for="(m, i) in markerRender"
          :key="'m' + i"
          class="marker"
          :class="{ 'is-draggable': draggableMarkers, 'is-dragging': dragIndex === i }"
          @click.stop="emit('marker-click', m.raw)"
          @mousedown.stop="draggableMarkers && startDrag(i, $event)"
        >
          <circle
            :cx="m.x"
            :cy="m.y"
            r="6"
            :fill="m.color"
            stroke="#fff"
            stroke-width="2"
          />
          <text
            v-if="m.label"
            :x="m.x + 9"
            :y="m.y + 4"
            class="marker-label"
          >{{ m.label }}</text>
        </g>
      </svg>

      <!-- 刻度尺 + 坐标读数（仅矢量画布模式） -->
      <div
        v-if="scaleBar"
        class="scale-bar"
      >
        <span
          class="scale-line"
          :style="{ width: scaleBar.px + 'px' }"
        />
        <span class="scale-text">{{ scaleBar.label }}</span>
      </div>
      <div
        v-if="hover"
        class="coord-readout"
      >
        {{ hover.lat.toFixed(6) }}, {{ hover.lng.toFixed(6) }}
      </div>
      <div
        v-if="!hasGeometry"
        class="map-empty"
      >
        <span>{{ emptyText }}</span>
      </div>
      <div
        v-if="drawMode === 'polygon'"
        class="draw-hint"
      >
        点击地图添加顶点（已添加 {{ drawPoints.length }} 个，至少 3 个）
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'

export interface MapPoint {
  lat: number
  lng: number
}

export interface MapMarker extends MapPoint {
  label?: string
  /** start / waypoint / destination / point / rider */
  type?: string
  color?: string
  /** 业务标识（拖拽回写时用于定位到具体表单字段，如 origin / dest-0） */
  key?: string
}

const props = withDefaults(defineProps<{
  title?: string
  height?: string
  markers?: MapMarker[]
  /** 路线（按访问顺序） */
  polyline?: MapPoint[]
  /** 圆形围栏 */
  circle?: { lat: number; lng: number; radiusMeters: number } | null
  /** 多边形围栏 */
  polygon?: MapPoint[]
  /** 是否允许点击地图拾取坐标 */
  interactive?: boolean
  /** 是否允许拖拽标记点（拖拽结束后 emit marker-move，用于回写表单坐标） */
  draggableMarkers?: boolean
  /**
   * 高德 JS SDK Key（前端底图用；由后端 /api/dms/route/config 下发）
   *
   * 留空 / 加载失败 → 自动回退自研矢量画布（功能不受影响），并 emit basemap-failed。
   */
  baseMapKey?: string
  /** polygon=绘制多边形模式 */
  drawMode?: 'none' | 'polygon'
  emptyText?: string
}>(), {
  title: '地图视图',
  height: '420px',
  markers: () => [],
  polyline: () => [],
  circle: null,
  polygon: () => [],
  interactive: true,
  draggableMarkers: false,
  baseMapKey: '',
  drawMode: 'none',
  emptyText: '暂无坐标数据，填写坐标或点击地图拾取',
})

const emit = defineEmits<{
  pick: [lat: number, lng: number]
  'polygon-change': [points: MapPoint[]]
  'marker-click': [marker: MapMarker]
  /** 拖拽标记结束：key 为业务标识（如 origin / dest-0） */
  'marker-move': [payload: { key?: string; index: number; lat: number; lng: number }]
  /** 底图加载失败（已自动回退矢量画布） */
  'basemap-failed': [message: string]
}>()

const COLORS: Record<string, string> = {
  start: '#52c41a',
  waypoint: '#1890ff',
  destination: '#fa541c',
  point: '#722ed1',
  rider: '#faad14',
}

const wrapRef = ref<HTMLElement | null>(null)
const size = ref({ width: 640, height: 380 })
const hover = ref<MapPoint | null>(null)
const drawPoints = ref<MapPoint[]>([])

const METERS_PER_DEG_LAT = 111320

// ── 尺寸自适应 ──
let observer: ResizeObserver | null = null

function measure() {
  const el = wrapRef.value
  if (!el) return
  const body = el.querySelector('.map-body') as HTMLElement | null
  size.value = {
    width: Math.max(body?.clientWidth || 320, 240),
    height: Math.max(body?.clientHeight || 300, 200),
  }
}

onMounted(() => {
  measure()
  if (typeof ResizeObserver !== 'undefined') {
    observer = new ResizeObserver(() => measure())
    if (wrapRef.value) observer.observe(wrapRef.value)
  }
  window.addEventListener('resize', measure)
})

onBeforeUnmount(() => {
  observer?.disconnect()
  window.removeEventListener('resize', measure)
  window.removeEventListener('mousemove', onDragMove)
  window.removeEventListener('mouseup', endDrag)
})

// ── 投影：以米为平面坐标的等距圆柱近似（城市尺度误差可忽略，且与服务端 Haversine 口径一致）──
const allPoints = computed<MapPoint[]>(() => {
  const pts: MapPoint[] = [...props.markers, ...props.polyline, ...props.polygon, ...drawPoints.value]
  if (props.circle) pts.push({ lat: props.circle.lat, lng: props.circle.lng })
  return pts.filter((p) => Number.isFinite(p.lat) && Number.isFinite(p.lng))
})

const hasGeometry = computed(() => allPoints.value.length > 0)

const view = computed(() => {
  const pts = allPoints.value
  const padding = 34
  const width = size.value.width
  const height = size.value.height

  let minLat = 0
  let maxLat = 0
  let minLng = 0
  let maxLng = 0
  if (pts.length === 0) {
    // 空态：用杭州湾附近默认视野，保证网格/刻度尺仍可渲染
    minLat = 30.24
    maxLat = 30.28
    minLng = 120.14
    maxLng = 120.2
  } else {
    minLat = Math.min(...pts.map((p) => p.lat))
    maxLat = Math.max(...pts.map((p) => p.lat))
    minLng = Math.min(...pts.map((p) => p.lng))
    maxLng = Math.max(...pts.map((p) => p.lng))
    if (maxLat - minLat < 1e-4) {
      minLat -= 5e-4
      maxLat += 5e-4
    }
    if (maxLng - minLng < 1e-4) {
      minLng -= 5e-4
      maxLng += 5e-4
    }
  }

  const lat0 = (minLat + maxLat) / 2
  const mPerDegLng = METERS_PER_DEG_LAT * Math.cos((lat0 * Math.PI) / 180)
  const toMeters = (p: MapPoint) => ({ x: p.lng * mPerDegLng, y: p.lat * METERS_PER_DEG_LAT })

  const corners = [
    toMeters({ lat: minLat, lng: minLng }),
    toMeters({ lat: maxLat, lng: maxLng }),
  ]
  // 圆形围栏需把半径一并纳入视野
  const extra = props.circle ? props.circle.radiusMeters * 1.1 : 0
  const spanX = corners[1].x - corners[0].x + extra * 2
  const spanY = corners[1].y - corners[0].y + extra * 2
  const scale = Math.min(
    (width - padding * 2) / Math.max(spanX, 1),
    (height - padding * 2) / Math.max(spanY, 1),
  )
  const centerM = {
    x: (corners[0].x + corners[1].x) / 2,
    y: (corners[0].y + corners[1].y) / 2,
  }

  return {
    width,
    height,
    scale,
    mPerDegLng,
    toMeters,
    toPx(p: MapPoint) {
      const m = toMeters(p)
      return {
        x: width / 2 + (m.x - centerM.x) * scale,
        y: height / 2 - (m.y - centerM.y) * scale,
      }
    },
    toGeo(x: number, y: number) {
      const mx = centerM.x + (x - width / 2) / scale
      const my = centerM.y - (y - height / 2) / scale
      return { lat: my / METERS_PER_DEG_LAT, lng: mx / mPerDegLng }
    },
  }
})

function markerColor(m: MapMarker) {
  return m.color || COLORS[m.type || 'point'] || COLORS.point
}

const markerPx = computed(() =>
  props.markers
    .filter((m) => Number.isFinite(m.lat) && Number.isFinite(m.lng))
    .map((m) => ({ ...view.value.toPx(m), label: m.label, color: markerColor(m), raw: m })),
)

const legendItems = computed(() => {
  const types = new Set(props.markers.map((m) => m.type || 'point'))
  const labels: Record<string, string> = {
    start: '起点',
    waypoint: '途经点',
    destination: '终点',
    point: '标记点',
    rider: '配送员',
  }
  const items = [...types].map((t) => ({ key: t, label: labels[t] || t, color: COLORS[t] || COLORS.point }))
  if (props.polyline.length > 1) items.push({ key: 'route', label: '规划路线', color: '#1890ff' })
  if (props.circle) items.push({ key: 'circle', label: '圆形围栏', color: '#fa8c16' })
  if (props.polygon.length > 2) items.push({ key: 'polygon', label: '多边形围栏', color: '#eb2f96' })
  return items
})

const linePx = computed(() =>
  props.polyline.length > 1
    ? props.polyline.map((p) => {
        const px = view.value.toPx(p)
        return `${px.x.toFixed(1)},${px.y.toFixed(1)}`
      }).join(' ')
    : '',
)

const polygonPx = computed(() =>
  props.polygon.length
    ? props.polygon.map((p) => {
        const px = view.value.toPx(p)
        return `${px.x.toFixed(1)},${px.y.toFixed(1)}`
      }).join(' ')
    : '',
)

const drawPxList = computed(() => drawPoints.value.map((p) => view.value.toPx(p)))
const drawPx = computed(() => drawPxList.value.map((p) => `${p.x.toFixed(1)},${p.y.toFixed(1)}`).join(' '))

const circleShape = computed(() => {
  if (!props.circle) return null
  const px = view.value.toPx({ lat: props.circle.lat, lng: props.circle.lng })
  return { cx: px.x, cy: px.y, r: Math.max(props.circle.radiusMeters * view.value.scale, 2) }
})

/** 网格（每格取 100/200/500/1000… 米整数步长，屏幕间距约 70~110px） */
const gridLines = computed(() => {
  const { width, height, scale, toGeo } = view.value
  const targetPx = 90
  const rawStep = targetPx / scale
  const steps = [50, 100, 200, 500, 1000, 2000, 5000, 10000, 20000, 50000]
  const step = steps.find((s) => s >= rawStep) || 100000

  const lines: Array<{ x1: number; y1: number; x2: number; y2: number }> = []
  const leftTop = toGeo(0, 0)
  const rightBottom = toGeo(width, height)
  const startLng = Math.floor(leftTop.lng / (step / view.value.mPerDegLng)) * (step / view.value.mPerDegLng)
  const startLat = Math.ceil(leftTop.lat / (step / METERS_PER_DEG_LAT)) * (step / METERS_PER_DEG_LAT)

  for (let lng = startLng; lng <= rightBottom.lng; lng += step / view.value.mPerDegLng) {
    const x = view.value.toPx({ lat: leftTop.lat, lng }).x
    if (x >= 0 && x <= width) lines.push({ x1: x, y1: 0, x2: x, y2: height })
  }
  for (let lat = startLat; lat >= rightBottom.lat; lat -= step / METERS_PER_DEG_LAT) {
    const y = view.value.toPx({ lat, lng: leftTop.lng }).y
    if (y >= 0 && y <= height) lines.push({ x1: 0, y1: y, x2: width, y2: y })
  }
  return lines
})

/** 比例尺：取 100/200/500/1000… 米中不超过 140px 的最大值 */
const scaleBar = computed(() => {
  const scale = view.value.scale
  if (!Number.isFinite(scale) || scale <= 0) return null
  const candidates = [50, 100, 200, 500, 1000, 2000, 5000, 10000, 20000, 50000, 100000]
  const picked = [...candidates].reverse().find((m) => m * scale <= 140) || candidates[0]
  return {
    px: picked * scale,
    label: picked >= 1000 ? `${picked / 1000} km` : `${picked} m`,
  }
})

// ── 标记拖拽（拖动中只改本地视图，松手才回写表单）──
const dragIndex = ref(-1)
const dragPos = ref<MapPoint | null>(null)

/** 渲染用标记：被拖拽的那个用临时坐标绘制（不改 props，避免脏写父组件数据） */
const markerRender = computed(() =>
  markerPx.value.map((m, i) => {
    if (i !== dragIndex.value || !dragPos.value) return m
    const px = view.value.toPx(dragPos.value)
    return { ...m, x: px.x, y: px.y, raw: { ...m.raw, ...dragPos.value } }
  }),
)

function startDrag(index: number, e: MouseEvent) {
  e.preventDefault()
  dragIndex.value = index
  dragPos.value = null
  window.addEventListener('mousemove', onDragMove)
  window.addEventListener('mouseup', endDrag)
}

function onDragMove(e: MouseEvent) {
  if (dragIndex.value < 0) return
  const svgEl = wrapRef.value?.querySelector('.map-svg') as SVGElement | null
  if (!svgEl) return
  const rect = svgEl.getBoundingClientRect()
  const geo = view.value.toGeo(e.clientX - rect.left, e.clientY - rect.top)
  dragPos.value = { lat: round6(geo.lat), lng: round6(geo.lng) }
}

function endDrag() {
  if (dragIndex.value >= 0 && dragPos.value) {
    const marker = props.markers[dragIndex.value]
    emit('marker-move', {
      key: marker?.key,
      index: dragIndex.value,
      lat: dragPos.value.lat,
      lng: dragPos.value.lng,
    })
  }
  dragIndex.value = -1
  dragPos.value = null
  window.removeEventListener('mousemove', onDragMove)
  window.removeEventListener('mouseup', endDrag)
}

// ── 交互 ──
function localPoint(e: MouseEvent) {
  const target = e.currentTarget as SVGElement
  const rect = target.getBoundingClientRect()
  return { x: e.clientX - rect.left, y: e.clientY - rect.top }
}

function handleClick(e: MouseEvent) {
  if (!props.interactive) return
  const { x, y } = localPoint(e)
  const geo = view.value.toGeo(x, y)
  if (props.drawMode === 'polygon') {
    drawPoints.value = [...drawPoints.value, { lat: round6(geo.lat), lng: round6(geo.lng) }]
    emit('polygon-change', [...drawPoints.value])
    return
  }
  emit('pick', round6(geo.lat), round6(geo.lng))
}

function handleMove(e: MouseEvent) {
  const { x, y } = localPoint(e)
  const geo = view.value.toGeo(x, y)
  hover.value = { lat: round6(geo.lat), lng: round6(geo.lng) }
}

function undoPoint() {
  drawPoints.value = drawPoints.value.slice(0, -1)
  emit('polygon-change', [...drawPoints.value])
}

function clearPoints() {
  drawPoints.value = []
  emit('polygon-change', [])
}

function setDrawPoints(points: MapPoint[]) {
  drawPoints.value = points ? [...points] : []
}

function resetView() {
  measure()
  if (baseMapReady.value && amapOverlays.length) {
    amapMap?.setFitView(amapOverlays, false, [40, 40, 40, 40])
  }
}

function round6(v: number) {
  return Math.round(v * 1e6) / 1e6
}


// ── 高德 JS SDK 底图（可选）：配了 JS Key 才加载，失败自动回退矢量画布 ──
const amapRef = ref<HTMLElement | null>(null)
const baseMapStatus = ref<'none' | 'loading' | 'ready' | 'failed'>('none')
let amapMap: any = null
let amapOverlays: any[] = []

const baseMapReady = computed(() => baseMapStatus.value === 'ready')
const baseMapLabel = computed(() => {
  switch (baseMapStatus.value) {
    case 'ready': return '高德'
    case 'loading': return '加载中…'
    case 'failed': return '加载失败（已回退矢量）'
    default: return '矢量（未配 JS Key）'
  }
})
const baseMapClass = computed(() => ({
  'is-ready': baseMapStatus.value === 'ready',
  'is-failed': baseMapStatus.value === 'failed',
}))

/** 动态加载高德 JS SDK（全局只加载一次），超时/失败即认为不可用 */
function loadAmapSdk(key: string): Promise<any> {
  const w = window as any
  if (w.AMap && w.AMap.Map) {
    return Promise.resolve(w.AMap)
  }
  if (w.__amapSdkPromise) {
    return w.__amapSdkPromise
  }
  w.__amapSdkPromise = new Promise((resolve, reject) => {
    const script = document.createElement('script')
    script.src = `https://webapi.amap.com/maps?v=2.0&key=${encodeURIComponent(key)}`
    script.async = true
    const timer = window.setTimeout(() => reject(new Error('底图 SDK 加载超时')), 8000)
    script.onload = () => {
      window.clearTimeout(timer)
      // 无效 Key 时脚本能加载但不会注册 AMap.Map
      const check = (retry: number) => {
        if (w.AMap && w.AMap.Map) return resolve(w.AMap)
        if (retry <= 0) return reject(new Error('底图 SDK 未就绪（Key 可能无效）'))
        window.setTimeout(() => check(retry - 1), 500)
      }
      check(6)
    }
    script.onerror = () => {
      window.clearTimeout(timer)
      reject(new Error('底图 SDK 加载失败（网络或 Key 无效）'))
    }
    document.head.appendChild(script)
  })
  return w.__amapSdkPromise
}

/** 初始化底图并与当前几何联动 */
async function initBaseMap(key: string) {
  baseMapStatus.value = 'loading'
  try {
    const AMap = await loadAmapSdk(key)
    await nextTick()
    if (!amapRef.value) throw new Error('底图容器未就绪')
    const center = view.value.toGeo(size.value.width / 2, size.value.height / 2)
    amapMap = new AMap.Map(amapRef.value, {
      zoom: 13,
      center: [center.lng, center.lat],
      viewMode: '2D',
    })
    baseMapStatus.value = 'ready'
    renderAmapOverlays()
  } catch (e: any) {
    baseMapStatus.value = 'failed'
    amapMap = null
    emit('basemap-failed', e?.message || '底图加载失败')
  }
}

/** 用高德原生对象绘制标记/折线/围栏（与矢量画布同源数据） */
function renderAmapOverlays() {
  const w = window as any
  if (!amapMap || !w.AMap) return
  amapOverlays.forEach((o) => amapMap.remove(o))
  amapOverlays = []
  const AMap = w.AMap
  props.polyline.forEach((p, i) => {
    if (i === 0) return
    const prev = props.polyline[i - 1]
    amapOverlays.push(new AMap.Polyline({
      path: [[prev.lng, prev.lat], [p.lng, p.lat]],
      strokeColor: '#1890ff', strokeWeight: 4,
    }))
  })
  props.markers.forEach((m) => {
    amapOverlays.push(new AMap.Marker({
      position: [m.lng, m.lat],
      title: m.label || '',
    }))
  })
  if (props.circle) {
    amapOverlays.push(new AMap.Circle({
      center: [props.circle.lng, props.circle.lat],
      radius: props.circle.radiusMeters,
      strokeColor: '#fa8c16', fillColor: '#fa8c16', fillOpacity: 0.15,
    }))
  }
  if (props.polygon.length >= 3) {
    amapOverlays.push(new AMap.Polygon({
      path: props.polygon.map((p) => [p.lng, p.lat]),
      strokeColor: '#eb2f96', fillColor: '#eb2f96', fillOpacity: 0.15,
    }))
  }
  if (amapOverlays.length) {
    amapMap.add(amapOverlays)
    amapMap.setFitView(amapOverlays, false, [40, 40, 40, 40])
  }
}

watch(() => props.baseMapKey, (key) => {
  if (key && baseMapStatus.value === 'none') {
    initBaseMap(key)
  }
}, { immediate: true })

watch([() => props.markers, () => props.polyline, () => props.circle, () => props.polygon], () => {
  if (baseMapReady.value) renderAmapOverlays()
}, { deep: true })

// 外部（如切换围栏编辑对象）注入既有顶点
watch(() => props.drawMode, (mode) => {
  if (mode !== 'polygon') drawPoints.value = []
})

defineExpose({ undoPoint, clearPoints, setDrawPoints })
</script>

<style scoped>
.route-map {
  display: flex;
  flex-direction: column;
  border: 1px solid #e8e8e8;
  border-radius: 4px;
  background: #fff;
  overflow: hidden;
}
.map-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 6px 10px;
  border-bottom: 1px solid #f0f0f0;
  background: #fafafa;
  flex-shrink: 0;
  flex-wrap: wrap;
}
.map-title { font-size: 13px; font-weight: 600; color: #303133; }
.legend { display: inline-flex; align-items: center; gap: 10px; flex: 1; flex-wrap: wrap; }
.legend-item { display: inline-flex; align-items: center; gap: 4px; font-size: 12px; color: #666; }
.legend-dot { width: 8px; height: 8px; border-radius: 50%; display: inline-block; }
.map-actions { display: inline-flex; gap: 6px; }

.map-body { position: relative; flex: 1; min-height: 0; background: #f7fbff; }
.amap-layer { position: absolute; inset: 0; }
.basemap-tag { font-size: 12px; color: #909399; white-space: nowrap; }
.basemap-tag.is-ready { color: #52c41a; }
.basemap-tag.is-failed { color: #d46b08; }
.map-svg { display: block; }
.map-svg.is-pickable { cursor: crosshair; }

.map-grid line { stroke: #e6f0fa; stroke-width: 1; }

.route-line { fill: none; stroke: #1890ff; stroke-width: 3; stroke-linejoin: round; stroke-linecap: round; }
.fence-circle { fill: rgba(250, 140, 22, 0.12); stroke: #fa8c16; stroke-width: 2; stroke-dasharray: 6 4; }
.fence-center { fill: #fa8c16; }
.fence-polygon { fill: rgba(235, 47, 150, 0.12); stroke: #eb2f96; stroke-width: 2; }
.fence-polygon-line { fill: none; stroke: #eb2f96; stroke-width: 2; stroke-dasharray: 5 4; }
.draw-line { fill: none; stroke: #722ed1; stroke-width: 2; stroke-dasharray: 5 4; }
.draw-vertex { fill: #722ed1; stroke: #fff; stroke-width: 1.5; }

.marker circle { cursor: pointer; }
.marker.is-draggable circle { cursor: grab; }
.marker.is-dragging circle { cursor: grabbing; stroke: #faad14; stroke-width: 3; }
.marker-label { font-size: 11px; fill: #303133; paint-order: stroke; stroke: #fff; stroke-width: 3px; }

.scale-bar {
  position: absolute;
  left: 10px;
  bottom: 10px;
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 11px;
  color: #666;
}
.scale-line { height: 8px; border: 1px solid #909399; border-top: none; display: inline-block; }
.scale-text { white-space: nowrap; }

.coord-readout {
  position: absolute;
  right: 10px;
  bottom: 10px;
  font-size: 11px;
  color: #909399;
  font-family: 'SFMono-Regular', Consolas, monospace;
}
.map-empty {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  color: #a8abb2;
  pointer-events: none;
}
.draw-hint {
  position: absolute;
  left: 10px;
  top: 8px;
  padding: 2px 8px;
  font-size: 12px;
  color: #722ed1;
  background: rgba(114, 46, 209, 0.08);
  border-radius: 3px;
}
</style>
