<script setup>
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { init, use } from 'echarts/core'
import { BarChart, LineChart, PieChart } from 'echarts/charts'
import {
  GridComponent,
  LegendComponent,
  TooltipComponent
} from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

use([
  BarChart,
  LineChart,
  PieChart,
  GridComponent,
  LegendComponent,
  TooltipComponent,
  CanvasRenderer
])

const props = defineProps({
  option: { type: Object, required: true },
  height: { type: String, default: '300px' }
})

const chartElement = ref()
let chart
let resizeObserver

function renderChart() {
  if (!chartElement.value) {
    return
  }
  if (!chart) {
    chart = init(chartElement.value)
  }
  chart.setOption(props.option, { notMerge: true })
}

watch(() => props.option, renderChart, { deep: true })

onMounted(() => {
  renderChart()
  resizeObserver = new ResizeObserver(() => chart?.resize())
  resizeObserver.observe(chartElement.value)
})

onBeforeUnmount(() => {
  resizeObserver?.disconnect()
  chart?.dispose()
  chart = null
})
</script>

<template>
  <div ref="chartElement" class="mes-chart" :style="{ height }" />
</template>

<style scoped>
.mes-chart {
  width: 100%;
  min-width: 0;
}
</style>
