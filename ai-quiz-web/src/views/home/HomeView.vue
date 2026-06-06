<template>
  <div class="home-page">
    <!-- ====== Stat Cards ====== -->
    <div class="stat-row">
      <div class="stat-card" v-for="(card, i) in statCards" :key="card.label"
           :style="{ animationDelay: (i * 0.08) + 's' }">
        <div class="stat-card-inner">
          <div class="stat-icon-box" :style="{ background: card.bg, color: card.color }">
            <el-icon :size="28"><component :is="card.icon" /></el-icon>
          </div>
          <div class="stat-info">
            <span class="stat-num" ref="numRefs">{{ card.display }}</span>
            <span class="stat-label">{{ card.label }}</span>
          </div>
          <div class="stat-glow" :style="{ background: card.color }"></div>
        </div>
      </div>
    </div>

    <!-- ====== Charts ====== -->
    <div class="chart-row">
      <div class="chart-card chart-pie">
        <div class="chart-card-head">
          <span class="chart-dot" style="background:var(--success)"></span>
          <span>答题正确率</span>
        </div>
        <div class="chart-body">
          <v-chart :option="accuracyOption" style="height:280px" v-if="hasData" />
          <el-empty v-else description="暂无数据" />
        </div>
      </div>

      <div class="chart-card chart-bar">
        <div class="chart-card-head">
          <span class="chart-dot" style="background:var(--accent)"></span>
          <span>分数段分布</span>
        </div>
        <div class="chart-body">
          <v-chart :option="distOption" style="height:280px" v-if="hasData" />
          <el-empty v-else description="暂无数据" />
        </div>
      </div>

      <div class="chart-card chart-summary">
        <div class="chart-card-head">
          <span class="chart-dot" style="background:var(--warning)"></span>
          <span>答题概况</span>
        </div>
        <div class="summary-grid" v-if="hasData">
          <div class="summary-cell" v-for="s in summaryItems" :key="s.label">
            <div class="summary-num" :style="{ color: s.color }">{{ s.value }}</div>
            <div class="summary-label">{{ s.label }}</div>
          </div>
        </div>
        <el-empty v-else description="暂无数据" :image-size="60" />
      </div>
    </div>

    <!-- ====== Trend ====== -->
    <div class="chart-row trend-row">
      <div class="chart-card chart-trend">
        <div class="chart-card-head">
          <span class="chart-dot" style="background:var(--deco-violet)"></span>
          <span>近期得分趋势</span>
        </div>
        <div class="chart-body">
          <v-chart :option="trendOption" style="height:300px" v-if="hasTrendData" />
          <el-empty v-else description="暂无数据" />
        </div>
      </div>
    </div>

    <!-- ====== Quick Actions ====== -->
    <div class="quick-row">
      <div class="quick-card" v-for="btn in quickBtns" :key="btn.label"
           @click="$router.push(btn.path)">
        <div class="quick-icon" :style="{ background: btn.bg, color: btn.color }">
          <el-icon :size="24"><component :is="btn.icon" /></el-icon>
        </div>
        <span class="quick-label">{{ btn.label }}</span>
        <el-icon class="quick-arrow"><ArrowRight /></el-icon>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { use } from 'echarts/core'
import { PieChart, BarChart, LineChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, LegendComponent, GridComponent, GraphicComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import VChart from 'vue-echarts'
import { Document, Tickets, EditPen, DataLine, Upload, List, Cpu, ArrowRight } from '@element-plus/icons-vue'
import { getStatsOverview } from '@/api/stats'
import { formatSeconds } from '@/utils'
import type { StatsOverview } from '@/types'

use([CanvasRenderer, PieChart, BarChart, LineChart,
  TitleComponent, TooltipComponent, LegendComponent, GridComponent, GraphicComponent])

const stats = reactive<StatsOverview>({
  docCount: 0, paperCount: 0, quizCount: 0,
  totalQuestions: 0, correctCount: 0, accuracyRate: 0,
  avgScore: 0, avgDuration: 0,
  scoreDistribution: [], recentQuizzes: []
})

const hasData = computed(() => stats.totalQuestions > 0)
const hasTrendData = computed(() => stats.recentQuizzes.length > 0)

// Animated counter values
const anim = reactive({ doc: 0, paper: 0, quiz: 0, rate: 0, total: 0, correct: 0, score: 0, dur: 0 })
let animating = false

function animateCounter(key: keyof typeof anim, to: number, duration = 800) {
  const from = anim[key]
  const start = performance.now()
  const tick = (now: number) => {
    const elapsed = now - start
    const progress = Math.min(elapsed / duration, 1)
    const eased = 1 - Math.pow(1 - progress, 3) // ease-out cubic
    anim[key] = Math.round(from + (to - from) * eased)
    if (progress < 1) requestAnimationFrame(tick)
    else anim[key] = to
  }
  requestAnimationFrame(tick)
}

watch(() => stats.docCount, v => { if (v) animateCounter('doc', v, 600) })
watch(() => stats.paperCount, v => { if (v) animateCounter('paper', v, 700) })
watch(() => stats.quizCount, v => { if (v) animateCounter('quiz', v, 800) })
watch(() => stats.accuracyRate, v => { if (v) animateCounter('rate', v, 900) })
watch(() => stats.totalQuestions, v => { if (v) animateCounter('total', v, 600) })
watch(() => stats.correctCount, v => { if (v) animateCounter('correct', v, 700) })
watch(() => stats.avgScore, v => { if (v) animateCounter('score', v, 800) })
watch(() => stats.avgDuration, v => { if (v) animateCounter('dur', v, 900) })

// Stat cards config
const statCards = computed(() => [
  { label: '文档总数',  display: anim.doc,    icon: Document, bg: '#f3f0ff', color: '#7c3aed' },
  { label: '试题总数',  display: anim.paper,   icon: Tickets,  bg: '#e8f8f4', color: '#0ea882' },
  { label: '答题次数',  display: anim.quiz,    icon: EditPen,  bg: '#fff8f2', color: '#d97706' },
  { label: '正确率',    display: anim.rate + '%', icon: DataLine,bg: '#eff6fb', color: '#4f8bc9' },
])

const summaryItems = computed(() => [
  { label: '累计做题', value: anim.total,     color: 'var(--text-primary)' },
  { label: '累计正确', value: anim.correct,    color: 'var(--success)' },
  { label: '平均得分', value: anim.score,      color: 'var(--accent)' },
  { label: '平均用时', value: formatSeconds(stats.avgDuration), color: 'var(--warning)' },
])

const quickBtns = [
  { label: '上传文档', icon: Upload,  path: '/app/document',      bg: '#efeffc', color: '#4f4fd9' },
  { label: '试题列表', icon: Tickets, path: '/app/test-paper',    bg: '#e8f8f4', color: '#0ea882' },
  { label: '答题记录', icon: List,    path: '/app/quiz-results',  bg: '#eff6fb', color: '#4f8bc9' },
  { label: 'AI 配置',  icon: Cpu,     path: '/system/ai',         bg: '#fff8f2', color: '#d97706' },
]

// ── Chart options ──
const accuracyOption = computed(() => {
  const wrong = stats.totalQuestions - stats.correctCount
  return {
    tooltip: { trigger: 'item', formatter: '{b}: {c} 题 ({d}%)' },
    series: [{
      type: 'pie',
      radius: ['55%', '78%'],
      center: ['50%', '50%'],
      animationDuration: 1200,
      animationEasing: 'cubicOut',
      avoidLabelOverlap: false,
      itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 3 },
      label: { show: false },
      emphasis: { label: { show: true, fontSize: 16, fontWeight: 'bold' }, scaleSize: 8 },
      data: [
        { value: stats.correctCount, name: '正确', itemStyle: { color: '#16a34a' } },
        { value: wrong, name: '错误', itemStyle: { color: '#dc2626' } }
      ]
    }],
    graphic: [{
      type: 'text', left: 'center', top: 'center',
      style: { text: anim.rate + '%', fontSize: 24, fontWeight: 'bold', fill: '#14171a' }
    }]
  }
})

const distOption = computed(() => ({
  tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
  xAxis: { type: 'category', data: stats.scoreDistribution.map(d => d.label), axisLabel: { fontSize: 11 } },
  yAxis: { type: 'value', minInterval: 1 },
  series: [{
    type: 'bar', barWidth: '50%', animationDuration: 1000, animationEasing: 'cubicOut',
    data: stats.scoreDistribution.map(d => ({
      value: d.count,
      itemStyle: { color: d.color, borderRadius: [8, 8, 0, 0] }
    })),
    label: { show: true, position: 'top', fontSize: 12, fontWeight: 600 }
  }]
}))

const trendOption = computed(() => {
  const reversed = [...stats.recentQuizzes].reverse()
  return {
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: reversed.map(r => r.submitTime?.substring(0, 10) || ''), axisLabel: { rotate: 30, fontSize: 11 } },
    yAxis: { type: 'value', min: 0 },
    series: [
      {
        name: '得分', type: 'line', smooth: true, animationDuration: 1200, animationEasing: 'cubicOut',
        data: reversed.map(r => r.userScore),
        lineStyle: { color: '#4f4fd9', width: 3 },
        itemStyle: { color: '#4f4fd9' },
        symbol: 'circle', symbolSize: 6,
        areaStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [{ offset: 0, color: 'rgba(79,79,217,0.15)' }, { offset: 1, color: 'rgba(79,79,217,0.0)' }] } }
      },
      {
        name: '满分', type: 'line',
        data: reversed.map(r => r.totalScore),
        lineStyle: { color: '#d0d3d8', type: 'dashed', width: 1.5 },
        itemStyle: { color: '#d0d3d8' }, symbol: 'none'
      }
    ],
    legend: { bottom: 0, data: ['得分', '满分'] }
  }
})

onMounted(async () => {
  try {
    const { data } = await getStatsOverview()
    Object.assign(stats, data.data)
  } catch { /* ignore */ }
})
</script>

<style scoped>
.home-page {
  max-width: 1400px;
  animation: pageIn .5s ease;
}
@keyframes pageIn {
  from { opacity: 0; transform: translateY(12px); }
  to   { opacity: 1; transform: translateY(0); }
}

/* ====== Stat Cards ====== */
.stat-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 24px;
}
.stat-card {
  opacity: 0;
  animation: cardUp .5s ease forwards;
}
@keyframes cardUp {
  from { opacity: 0; transform: translateY(24px); }
  to   { opacity: 1; transform: translateY(0); }
}
.stat-card-inner {
  position: relative;
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 22px 20px;
  background: var(--bg-surface);
  border: 1px solid var(--border-default);
  border-radius: var(--radius-md);
  overflow: hidden;
  transition: all .3s ease;
  cursor: default;
}
.stat-card-inner:hover {
  transform: translateY(-2px);
  border-color: transparent;
  box-shadow: 0 8px 24px rgba(0,0,0,.07);
}
/* Glow on hover */
.stat-glow {
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  opacity: 0;
  transition: opacity .4s;
  background: radial-gradient(circle at center, currentColor 0%, transparent 70%);
  pointer-events: none;
  filter: blur(40px);
}
.stat-card-inner:hover .stat-glow {
  opacity: .06;
}
.stat-icon-box {
  width: 54px;
  height: 54px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: transform .3s;
}
.stat-card-inner:hover .stat-icon-box {
  transform: scale(1.08);
}
.stat-info {
  display: flex;
  flex-direction: column;
}
.stat-num {
  font-size: 28px;
  font-weight: 800;
  color: var(--text-primary);
  letter-spacing: -.03em;
  line-height: 1.1;
  font-variant-numeric: tabular-nums;
}
.stat-label {
  color: var(--text-tertiary);
  font-size: 12px;
  margin-top: 4px;
  font-weight: 500;
  letter-spacing: .02em;
}

/* ====== Chart Row ====== */
.chart-row {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 20px;
  margin-bottom: 24px;
}
.chart-row.trend-row {
  grid-template-columns: 1fr;
}
.chart-card {
  background: var(--bg-surface);
  border: 1px solid var(--border-default);
  border-radius: var(--radius-md);
  padding: 20px;
  transition: all .3s ease;
}
.chart-card:hover {
  box-shadow: 0 4px 16px rgba(0,0,0,.05);
}
.chart-card-head {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  font-size: 14px;
  color: var(--text-primary);
  margin-bottom: 16px;
}
.chart-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}
.chart-body {
  min-height: 200px;
}

/* ====== Summary Grid ====== */
.summary-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}
.summary-cell {
  text-align: center;
  padding: 20px 12px;
  background: var(--bg-muted);
  border-radius: 10px;
  border: 1px solid var(--border-subtle);
  transition: all .25s;
}
.summary-cell:hover {
  background: var(--bg-hover);
  transform: translateY(-2px);
}
.summary-num {
  font-size: 26px;
  font-weight: 800;
  letter-spacing: -.02em;
  line-height: 1.2;
  font-variant-numeric: tabular-nums;
}
.summary-label {
  color: var(--text-tertiary);
  font-size: 12px;
  margin-top: 6px;
  font-weight: 500;
}

/* ====== Quick Actions ====== */
.quick-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-top: 4px;
}
.quick-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 20px;
  background: var(--bg-surface);
  border: 1px solid var(--border-default);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all .25s ease;
}
.quick-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 6px 20px rgba(0,0,0,.06);
  border-color: transparent;
}
.quick-card:hover .quick-arrow {
  opacity: 1;
  transform: translateX(0);
}
.quick-icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: transform .3s;
}
.quick-card:hover .quick-icon {
  transform: scale(1.1);
}
.quick-label {
  flex: 1;
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  letter-spacing: -.01em;
}
.quick-arrow {
  color: var(--text-tertiary);
  opacity: 0;
  transform: translateX(-6px);
  transition: all .25s;
}
</style>
