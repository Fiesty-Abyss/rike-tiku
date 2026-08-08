<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { createPracticeSession, fetchPracticeOptions, type Subject } from '../../api/student/practice'
import { fetchStudentHighFrequencyPoints, type StudentHighFrequencyPoint } from '../../api/student/highFrequency'
import { fetchStudentLearningSummary, type MasteryLevel, type StudentLearningSummary } from '../../api/student/learningMastery'
import type { ApiError } from '../../api/http'
const route = useRoute()
const router = useRouter()
const subject = ref<Subject | null>(null)
const pointCount = ref(0)
const loading = ref(false)
const highFrequencyPoints = ref<StudentHighFrequencyPoint[]>([])
const learningSummary = ref<StudentLearningSummary | null>(null)
const subjectCode = computed(() => String(route.params.subjectCode).toUpperCase())
const valid = computed(() => ['PHYSICS', 'CHEMISTRY', 'BIOLOGY'].includes(subjectCode.value))

async function load() {
  if (!valid.value) {
    await router.replace('/student')
    return
  }
  try {
    const all = await fetchPracticeOptions()
    subject.value = all.subjects.find(item => item.code === subjectCode.value) || null
    if (!subject.value) return
    const [options, highFrequency, mastery] = await Promise.all([
      fetchPracticeOptions(subject.value.id),
      fetchStudentHighFrequencyPoints(subject.value.id),
      fetchStudentLearningSummary(subject.value.id),
    ])
    pointCount.value = options.knowledgePoints.length
    highFrequencyPoints.value = highFrequency
    learningSummary.value = mastery
  } catch (error) {
    ElMessage.error((error as ApiError).message || '学科信息加载失败。')
  }
}

async function randomPractice() {
  if (!subject.value) return
  loading.value = true
  try {
    const session = await createPracticeSession({ subjectId: subject.value.id, count: 5 })
    ElMessage.success('已创建本学科随机练习。')
    await router.push(`/student/practice/${session.id}`)
  } catch (error) {
    const api = error as ApiError
    ElMessage.error(api.code === 'PRACTICE_QUESTION_INSUFFICIENT'
      ? '本学科符合条件的题目不足5道，请调整题数或筛选条件。'
      : api.message || '创建练习失败。')
  } finally {
    loading.value = false
  }
}

function masteryLabel(level: MasteryLevel) {
  return ({ NOT_STARTED: '未练习', INSUFFICIENT: '数据较少', WEAK: '薄弱', IMPROVING: '巩固中', MASTERED: '已掌握' } as Record<MasteryLevel, string>)[level]
}

function masteryTag(level: MasteryLevel) {
  return ({ NOT_STARTED: 'info', INSUFFICIENT: 'warning', WEAK: 'danger', IMPROVING: 'warning', MASTERED: 'success' } as const)[level]
}

function accuracy(value: number | null) {
  return value === null ? '暂无练习数据' : `${value.toFixed(1)}%`
}

async function startReinforcement(subjectId: number, knowledgePointId: number, count: number) {
  await router.push({ path: '/student/practice/new', query: { subjectId, knowledgePointId, count } })
}
onMounted(() => void load())
</script>

<template>
  <section v-if="subject" class="student-page subject-page">
    <div class="student-page-heading">
      <div>
        <h1>{{ subject.name }}学习工作台</h1>
        <p>围绕本学科建立练习与错题复习入口，数据来自已发布的可自动判分题目。</p>
      </div>
      <el-button @click="router.push('/student')">返回三科主页</el-button>
    </div>
    <div class="subject-action-grid">
      <article>
        <h2>随机练习</h2>
        <p>默认抽取本学科5题，题目在创建后立即冻结。</p>
        <el-button type="primary" :loading="loading" @click="randomPractice">开始5题随机练习</el-button>
      </article>
      <article>
        <h2>定向练习</h2>
        <p>按知识点、题型、难度和数量组合练习计划。</p>
        <el-button @click="router.push({ path: '/student/practice/new', query: { subjectId: subject.id } })">
          设置练习条件
        </el-button>
      </article>
      <article>
        <h2>本学科错题</h2>
        <p>当前可用知识点 {{ pointCount }} 个；错题状态和历史会保留。</p>
        <el-button @click="router.push({ path: '/student/wrong-questions', query: { subjectId: subject.id } })">
          查看{{ subject.name }}错题
        </el-button>
      </article>
    </div>
    <section v-if="learningSummary" class="student-mastery-section">
      <div class="section-title-row">
        <div>
          <h2>学习掌握</h2>
          <p>根据本人已提交练习的实际判分、练习次数和当前错题状态实时计算。</p>
        </div>
      </div>
      <div class="mastery-overview">
        <div class="mastery-accuracy">
          <span>总体正确率</span>
          <strong>{{ accuracy(learningSummary.overall.overallAccuracy) }}</strong>
          <el-progress v-if="learningSummary.overall.overallAccuracy !== null" :percentage="learningSummary.overall.overallAccuracy" :stroke-width="8" />
          <small>{{ learningSummary.overall.totalCorrectCount }} / {{ learningSummary.overall.totalAnsweredCount }} 题答对</small>
        </div>
        <dl class="mastery-counts">
          <div><dt>已练习知识点</dt><dd>{{ learningSummary.overall.practicedKnowledgePointCount }} / {{ learningSummary.overall.totalKnowledgePointCount }}</dd></div>
          <div><dt>已掌握</dt><dd>{{ learningSummary.overall.masteredKnowledgePointCount }}</dd></div>
          <div><dt>巩固中</dt><dd>{{ learningSummary.overall.improvingKnowledgePointCount }}</dd></div>
          <div><dt>薄弱</dt><dd>{{ learningSummary.overall.weakKnowledgePointCount }}</dd></div>
          <div><dt>数据较少</dt><dd>{{ learningSummary.overall.insufficientKnowledgePointCount }}</dd></div>
          <div><dt>未练习</dt><dd>{{ learningSummary.overall.notStartedKnowledgePointCount }}</dd></div>
        </dl>
      </div>
    </section>
    <section v-if="learningSummary" class="student-mastery-section">
      <div class="section-title-row">
        <div>
          <h2>知识点掌握</h2>
          <p>同一道题关联多个知识点时，会计入每个关联知识点。</p>
        </div>
      </div>
      <el-table :data="learningSummary.knowledgePoints" class="data-table" empty-text="当前学科暂无有效知识点。">
        <el-table-column prop="fullPath" label="知识点" min-width="240" />
        <el-table-column prop="answeredCount" label="答题数" width="88" />
        <el-table-column label="正确率" width="130"><template #default="{ row }">{{ accuracy(row.accuracy) }}</template></el-table-column>
        <el-table-column prop="activeWrongQuestionCount" label="当前错题" width="100" />
        <el-table-column label="掌握状态" width="105"><template #default="{ row }"><el-tag :type="masteryTag(row.masteryLevel)">{{ masteryLabel(row.masteryLevel) }}</el-tag></template></el-table-column>
      </el-table>
    </section>
    <section v-if="learningSummary" class="student-mastery-section recommendation-section">
      <div class="section-title-row">
        <div>
          <h2>推荐练习</h2>
          <p>按活动错题、正确率和练习样本量的固定规则排序，最多显示三项。</p>
        </div>
      </div>
      <el-alert v-if="learningSummary.recommendationMessage" :title="learningSummary.recommendationMessage" type="success" :closable="false" show-icon />
      <div v-else class="recommendation-list">
        <article v-for="item in learningSummary.recommendations" :key="item.knowledgePointId">
          <div>
            <h3>{{ item.knowledgePointName }}</h3>
            <p>{{ item.reason }}</p>
          </div>
          <el-button type="primary" plain @click="startReinforcement(item.practiceParameters.subjectId, item.practiceParameters.knowledgePointId, item.practiceParameters.count)">开始巩固</el-button>
        </article>
      </div>
    </section>
    <section class="student-knowledge-section">
      <div class="section-title-row">
        <div>
          <h2>高频考点</h2>
          <p>内容来自当前学生有效主班级对应的 ACTIVE 任课关系。</p>
        </div>
      </div>
      <el-empty v-if="!highFrequencyPoints.length" description="当前学科暂无高频考点。" />
      <div v-else class="knowledge-point-cards">
        <article v-for="point in highFrequencyPoints" :key="point.id" class="knowledge-point-card">
          <div class="knowledge-point-card-heading">
            <h3>{{ point.title }}</h3>
            <el-tag>{{ point.knowledgePointName }}</el-tag>
          </div>
          <p>{{ point.content }}</p>
          <p v-if="point.memoryTrick"><strong>记忆口诀：</strong>{{ point.memoryTrick }}</p>
          <p v-if="point.commonMistake"><strong>常见误区：</strong>{{ point.commonMistake }}</p>
          <small>任课教师：{{ point.teacherName }}</small>
        </article>
      </div>
    </section>
  </section>
</template>
