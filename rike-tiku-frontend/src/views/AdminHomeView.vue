<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue'
import gsap from 'gsap'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { fetchAdminDashboard, type AdminDashboard } from '../api/admin/dashboard'
import type { ApiError } from '../api/http'

const router = useRouter()
const dashboard = ref<AdminDashboard | null>(null)
const loading = ref(true)
const root = ref<HTMLElement>()
let motion: gsap.MatchMedia | undefined
let context: gsap.Context | undefined
const subjectTotal = computed(() => dashboard.value ? dashboard.value.physicsQuestionCount + dashboard.value.chemistryQuestionCount + dashboard.value.biologyQuestionCount : 0)
const percent = (value: number) => subjectTotal.value ? Math.round(value / subjectTotal.value * 100) : 0
const resultLabel = (value: string) => value === 'SUCCESS' ? '成功' : '失败'

onMounted(async () => {
  try {
    dashboard.value = await fetchAdminDashboard()
    await nextTick()
    if (root.value && typeof window.matchMedia === 'function') {
      motion = gsap.matchMedia()
      context = gsap.context(() => {
        motion?.add('(prefers-reduced-motion: no-preference)', () => {
          gsap.from('.dashboard-metrics article, .dashboard-panel', { autoAlpha: 0, y: 14, duration: 0.48, stagger: 0.08, ease: 'power3.out' })
        })
      }, root.value)
    }
  }
  catch (error) { ElMessage.error((error as ApiError).message || '系统总览加载失败。') }
  finally { loading.value = false }
})
onUnmounted(() => { motion?.revert(); context?.revert() })
</script>

<template>
  <section ref="root" class="admin-page dashboard-page" v-loading="loading">
    <div class="page-heading"><div><span class="page-kicker">SYSTEM OVERVIEW</span><h1>系统总览</h1><p>聚合教学组织、账号、题库审核与安全审计的实时只读数据。</p></div><el-button type="primary" @click="router.push('/admin/questions')">进入题库管理</el-button></div>
    <template v-if="dashboard">
      <div class="dashboard-metrics"><article><span>有效班级</span><strong>{{ dashboard.activeClassCount }}</strong><small>当前 ACTIVE 班级</small></article><article><span>启用学生</span><strong>{{ dashboard.enabledStudentCount }}</strong><small>账号与档案均有效</small></article><article><span>启用教师</span><strong>{{ dashboard.enabledTeacherCount }}</strong><small>账号与档案均有效</small></article><article><span>已发布题目</span><strong>{{ dashboard.publishedQuestionCount }}</strong><small>含普通练习与专题题</small></article></div>
      <div class="dashboard-grid">
        <section class="dashboard-panel subject-distribution"><div class="section-title-row"><div><h2>三科题库分布</h2><p>按当前已发布题目实时统计。</p></div></div><div v-for="subject in [{name:'物理',value:dashboard.physicsQuestionCount,code:'physics'},{name:'化学',value:dashboard.chemistryQuestionCount,code:'chemistry'},{name:'生物',value:dashboard.biologyQuestionCount,code:'biology'}]" :key="subject.code" class="subject-distribution-row"><div><span>{{ subject.name }}</span><strong>{{ subject.value }} 题</strong></div><el-progress :percentage="percent(subject.value)" :show-text="false" :class="`subject-progress--${subject.code}`" /></div></section>
        <section class="dashboard-panel dashboard-todo"><div class="section-title-row"><div><h2>待办提示</h2><p>来自真实题目状态。</p></div></div><div v-if="dashboard.pendingQuestionCount" class="todo-count"><strong>{{ dashboard.pendingQuestionCount }}</strong><span>道题等待审核</span><el-button type="primary" plain @click="router.push('/admin/questions')">前往处理</el-button></div><el-empty v-else description="当前没有待审核题目。" :image-size="72" /></section>
        <section class="dashboard-panel recent-operations"><div class="section-title-row"><div><h2>最近管理员操作</h2><p>仅显示最近 5 条审计事实。</p></div><el-button link @click="router.push('/admin/operation-logs')">查看全部</el-button></div><el-empty v-if="!dashboard.recentOperationLogs.length" description="暂无操作日志。" :image-size="72"/><ul v-else><li v-for="item in dashboard.recentOperationLogs" :key="item.id"><div><strong>{{ item.operatorUsername || '系统' }} · {{ item.module }}</strong><span>{{ item.summary || item.action }}</span></div><el-tag :type="item.result==='SUCCESS'?'success':'danger'">{{ resultLabel(item.result) }}</el-tag></li></ul></section>
        <section class="dashboard-panel quick-actions"><div class="section-title-row"><div><h2>快捷入口</h2><p>直达高频管理任务。</p></div></div><div><el-button @click="router.push('/admin/classes')">教学组织</el-button><el-button @click="router.push('/admin/teachers')">教师与任课</el-button><el-button @click="router.push('/admin/students')">学生管理</el-button><el-button @click="router.push('/admin/questions/import')">批量导入题目</el-button></div></section>
      </div>
    </template>
  </section>
</template>
