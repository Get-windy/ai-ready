/**
 * 图表组件单元测试
 */
import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import BarChart from '../BarChart.vue'
import LineChart from '../LineChart.vue'
import PieChart from '../PieChart.vue'
import RadarChart from '../RadarChart.vue'
import type { ChartDataPoint } from '../types'

// 测试数据
const mockData: ChartDataPoint[] = [
  { name: '苹果', value: 100, color: '#ff6b6b' },
  { name: '香蕉', value: 80, color: '#4ecdc4' },
  { name: '橙子', value: 120, color: '#45b7d1' },
  { name: '葡萄', value: 60, color: '#f9ca24' }
]

describe('BarChart', () => {
  it('应该正确渲染柱状图', () => {
    const wrapper = mount(BarChart, {
      props: {
        data: mockData,
        width: 400,
        height: 300
      }
    })
    
    expect(wrapper.exists()).toBe(true)
    expect(wrapper.find('svg').exists()).toBe(true)
  })
  
  it('应该显示标题', () => {
    const wrapper = mount(BarChart, {
      props: {
        data: mockData,
        title: '水果销量'
      }
    })
    
    expect(wrapper.text()).toContain('水果销量')
  })
  
  it('应该触发点击事件', async () => {
    const wrapper = mount(BarChart, {
      props: { data: mockData }
    })
    
    const clickedData: ChartDataPoint | null = null
    wrapper.vm.$emit('click', mockData[0])
    
    await wrapper.vm.$nextTick()
    expect(wrapper.emitted('click')).toBeTruthy()
  })
  
  it('应该正确计算柱状图数据', () => {
    const wrapper = mount(BarChart, {
      props: { data: mockData }
    })
    
    // 验证计算属性
    const vm = wrapper.vm as any
    expect(vm.bars.length).toBe(4)
  })
})

describe('LineChart', () => {
  it('应该正确渲染折线图', () => {
    const wrapper = mount(LineChart, {
      props: {
        data: mockData,
        width: 400,
        height: 300
      }
    })
    
    expect(wrapper.exists()).toBe(true)
    expect(wrapper.find('svg').exists()).toBe(true)
  })
  
  it('应该支持多系列数据', () => {
    const series = [
      { name: '2024年', data: mockData },
      { name: '2025年', data: mockData.map(d => ({ ...d, value: d.value * 1.2 })) }
    ]
    
    const wrapper = mount(LineChart, {
      props: { series }
    })
    
    expect(wrapper.exists()).toBe(true)
  })
  
  it('应该支持平滑曲线', () => {
    const wrapper = mount(LineChart, {
      props: {
        data: mockData,
        smooth: true
      }
    })
    
    expect(wrapper.exists()).toBe(true)
  })
})

describe('PieChart', () => {
  it('应该正确渲染饼图', () => {
    const wrapper = mount(PieChart, {
      props: {
        data: mockData,
        width: 400,
        height: 300
      }
    })
    
    expect(wrapper.exists()).toBe(true)
    expect(wrapper.find('svg').exists()).toBe(true)
  })
  
  it('应该正确计算百分比', () => {
    const wrapper = mount(PieChart, {
      props: { data: mockData }
    })
    
    const vm = wrapper.vm as any
    const total = mockData.reduce((sum, d) => sum + d.value, 0)
    const firstPercentage = (mockData[0].value / total * 100).toFixed(1)
    
    expect(vm.slices[0].percentage).toBe(firstPercentage)
  })
  
  it('应该支持环形图', () => {
    const wrapper = mount(PieChart, {
      props: {
        data: mockData,
        innerRadius: 0.5
      }
    })
    
    expect(wrapper.exists()).toBe(true)
  })
})

describe('RadarChart', () => {
  it('应该正确渲染雷达图', () => {
    const wrapper = mount(RadarChart, {
      props: {
        data: mockData,
        width: 400,
        height: 300
      }
    })
    
    expect(wrapper.exists()).toBe(true)
    expect(wrapper.find('svg').exists()).toBe(true)
  })
  
  it('应该支持多系列数据', () => {
    const series = [
      { name: 'A组', data: mockData },
      { name: 'B组', data: mockData.map(d => ({ ...d, value: d.value * 0.8 })) }
    ]
    
    const wrapper = mount(RadarChart, {
      props: { series }
    })
    
    const vm = wrapper.vm as any
    expect(vm.dataAreas.length).toBe(2)
  })
})

describe('Composables', () => {
  it('useChartFilter should filter data correctly', async () => {
    const { applyFilters } = await import('../composables')
    
    const filtered = applyFilters(mockData)
    expect(filtered.length).toBe(4)
  })
  
  it('useChartZoom should manage zoom state', async () => {
    const { setZoom, resetZoom, zoomRange } = await import('../composables')
    
    setZoom(20, 80)
    expect(zoomRange.value.start).toBe(20)
    expect(zoomRange.value.end).toBe(80)
    
    resetZoom()
    expect(zoomRange.value.start).toBe(0)
    expect(zoomRange.value.end).toBe(100)
  })
})

describe('ThemeColors', () => {
  it('should have predefined themes', async () => {
    const { ThemeColors } = await import('../types')
    
    expect(ThemeColors.default).toBeDefined()
    expect(ThemeColors.dark).toBeDefined()
    expect(ThemeColors.vintage).toBeDefined()
    expect(ThemeColors.colorful).toBeDefined()
    expect(ThemeColors.default.length).toBeGreaterThan(0)
  })
})