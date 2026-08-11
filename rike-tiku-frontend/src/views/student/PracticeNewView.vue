<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import {
  createPracticeSession,
  fetchPracticeAvailability,
  fetchPracticeOptions,
  type KnowledgePoint,
  type QuestionType,
  type Subject,
} from '../../api/student/practice'
import type { ApiError } from '../../api/http'
import { subjectTheme } from '../../utils/subjectTheme'

const router = useRouter()
const route = useRoute()
const formRef = ref<FormInstance>()
const loading = ref(false)
const availabilityLoading = ref(false)
const initialized = ref(false)
const subjects = ref<Subject[]>([])
const points = ref<KnowledgePoint[]>([])
const availableCount = ref(0)
const referenceQuestionId = ref<number | undefined>()
let availabilitySequence = 0
type DifficultyChoice = 'ALL' | 1 | 2 | 3
const form = reactive({ subjectId: 0, knowledgePointIds: [] as number[], questionTypes: [] as QuestionType[], difficulty: 'ALL' as DifficultyChoice, count: 5 })
const rules: FormRules = { subjectId: [{ required: true, message: '请选择学科', trigger: 'change' }], count: [{ required: true, message: '请输入练习题数', trigger: 'blur' }] }
const questionTypes: Array<{ label: string; value: QuestionType }> = [{ label: '单选题', value: 'SINGLE_CHOICE' }, { label: '多选题', value: 'MULTIPLE_CHOICE' }, { label: '填空题', value: 'FILL_BLANK' }]
const availabilityKey = computed(() => JSON.stringify({
  subjectId: form.subjectId,
  knowledgePointIds: [...form.knowledgePointIds].sort((a, b) => a - b),
  questionTypes: [...form.questionTypes].sort(),
  difficulty: form.difficulty,
  referenceQuestionId: referenceQuestionId.value,
}))
const canCreate = computed(() => availableCount.value > 0 && form.count <= availableCount.value)
const environment = computed(() => subjectTheme(subjects.value.find(item => item.id === form.subjectId)?.code || route.query.subjectCode))

function message(error: unknown) {
  const api = error as ApiError
  return ({
    PRACTICE_QUESTION_INSUFFICIENT: '符合条件的已发布题目不足，请减少题数或调整筛选。',
    PRACTICE_KNOWLEDGE_POINT_INVALID: '知识点不存在、已停用或不属于所选学科。',
    PRACTICE_REFERENCE_INVALID: '参考题已不可用于类似练习，请重新选择。',
  } as Record<string, string>)[api.code || ''] || api.message || '练习请求失败，请稍后重试。'
}

function availabilityParams() {
  return {
    subjectId: form.subjectId,
    knowledgePointIds: form.knowledgePointIds.length ? form.knowledgePointIds : undefined,
    questionTypes: form.questionTypes.length ? form.questionTypes : undefined,
    difficulty: form.difficulty === 'ALL' ? undefined : form.difficulty,
    referenceQuestionId: referenceQuestionId.value,
  }
}

async function loadAvailability() {
  if (!form.subjectId) {
    availableCount.value = 0
    return
  }
  const sequence = ++availabilitySequence
  availabilityLoading.value = true
  try {
    const result = await fetchPracticeAvailability(availabilityParams())
    if (sequence !== availabilitySequence) return
    availableCount.value = result.availableCount
  } catch (error) {
    if (sequence !== availabilitySequence) return
    availableCount.value = 0
    ElMessage.error(message(error))
  } finally {
    if (sequence === availabilitySequence) availabilityLoading.value = false
  }
}

async function loadPoints() {
  if (!form.subjectId) return
  try {
    points.value = (await fetchPracticeOptions(form.subjectId)).knowledgePoints
    const availableIds = new Set(points.value.map(point => point.id))
    form.knowledgePointIds = form.knowledgePointIds.filter(id => availableIds.has(id))
  } catch (error) {
    ElMessage.error(message(error))
  }
}

async function changeSubject() {
  initialized.value = false
  form.knowledgePointIds = []
  referenceQuestionId.value = undefined
  await loadPoints()
  initialized.value = true
  await loadAvailability()
}

function adjustCount() {
  form.count = Math.max(1, Math.min(availableCount.value, 50))
}

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid || !canCreate.value) return
  loading.value = true
  try {
    const session = await createPracticeSession({ ...availabilityParams(), count: form.count })
    ElMessage.success('练习已创建，题目集合已冻结。')
    await router.push(`/student/practice/${session.id}`)
  } catch (error) {
    ElMessage.error(message(error))
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  try {
    const data = await fetchPracticeOptions()
    subjects.value = data.subjects
    const presetId = Number(route.query.subjectId)
    const presetCode = String(route.query.subjectCode || '').toUpperCase()
    form.subjectId = subjects.value.some(item => item.id === presetId)
      ? presetId
      : subjects.value.find(item => item.code === presetCode)?.id || subjects.value[0]?.id || 0
    referenceQuestionId.value = Number(route.query.referenceQuestionId) || undefined
    await loadPoints()
    const pointId = Number(route.query.knowledgePointId)
    if (pointId && points.value.some(item => item.id === pointId)) form.knowledgePointIds = [pointId]
    const count = Number(route.query.count)
    if (Number.isInteger(count) && count >= 1 && count <= 50) form.count = count
    await nextTick()
    initialized.value = true
    await loadAvailability()
  } catch (error) {
    ElMessage.error(message(error))
  }
})

watch(availabilityKey, () => {
  if (initialized.value) void loadAvailability()
})
</script>

<template>
  <section class="student-page practice-builder-page" :data-subject="environment">
    <div class="student-page-heading">
      <div><h1>{{ referenceQuestionId ? '练习类似题' : '创建自主练习' }}</h1><p>{{ referenceQuestionId ? '按同学科、共享知识点、同题型优先和相邻难度生成规则型练习。' : '仅从已发布、可自动判分的在线练习题中选择。' }}</p></div>
      <el-button @click="router.push('/student')">返回学习主页</el-button>
    </div>
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="practice-form">
      <el-form-item label="学科" prop="subjectId"><el-select v-model="form.subjectId" @change="changeSubject"><el-option v-for="subject in subjects" :key="subject.id" :label="subject.name" :value="subject.id" /></el-select></el-form-item>
      <el-form-item label="知识点（可选）"><el-select v-model="form.knowledgePointIds" multiple filterable collapse-tags placeholder="不选则覆盖全科"><el-option v-for="point in points" :key="point.id" :label="point.path" :value="point.id" /></el-select></el-form-item>
      <el-form-item label="题型（可选）"><el-checkbox-group v-model="form.questionTypes"><el-checkbox v-for="type in questionTypes" :key="type.value" :value="type.value">{{ type.label }}</el-checkbox></el-checkbox-group></el-form-item>
      <el-form-item label="难度（可选）"><el-radio-group v-model="form.difficulty"><el-radio value="ALL">不限</el-radio><el-radio :value="1">简单</el-radio><el-radio :value="2">中等</el-radio><el-radio :value="3">困难</el-radio></el-radio-group></el-form-item>
      <el-form-item label="题目数量" prop="count"><el-input-number v-model="form.count" :min="1" :max="50" /></el-form-item>
      <div class="practice-availability" :class="{ 'is-empty': !availableCount }" aria-live="polite">
        <span v-if="availabilityLoading">正在核对题池…</span>
        <template v-else><strong>当前条件可用 {{ availableCount }} 题</strong><el-button v-if="availableCount && form.count > availableCount" link type="primary" @click="adjustCount">调整为 {{ availableCount }} 题</el-button><span v-if="!availableCount">请放宽知识点、题型或难度条件。</span></template>
      </div>
      <el-button type="primary" :loading="loading" :disabled="!canCreate || availabilityLoading" @click="submit">创建并开始作答</el-button>
    </el-form>
  </section>
</template>
