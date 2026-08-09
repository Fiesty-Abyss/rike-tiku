<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { createHighFrequencyPoint, fetchTeacherLearningSummary, fetchTeacherWorkspace, updateHighFrequencyPoint, updateHighFrequencyPointStatus, type HighFrequencyPoint, type TeacherScopeLearningSummary, type TeacherWorkspace } from '../../api/teacher'
import { createConversation, fetchConversations } from '../../api/messages'

const route = useRoute()
const router = useRouter()
const scopeId = computed(() => Number(route.params.scopeId))
const workspace = ref<TeacherWorkspace | null>(null)
const learningSummary = ref<TeacherScopeLearningSummary | null>(null)
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const editing = ref<HighFrequencyPoint | null>(null)
const unreadCount = ref(0)
const formRef = ref<FormInstance>()
const form = reactive({ knowledgePointId: 0, title: '', content: '', memoryTrick: '', commonMistake: '', sortOrder: 1 })
const rules: FormRules = {
  knowledgePointId: [{ required: true, message: '请选择知识点', trigger: 'change' }],
  title: [{ required: true, message: '请填写标题', trigger: 'blur' }],
  content: [{ required: true, message: '请填写正文', trigger: 'blur' }],
}

const activePoints = computed(() => workspace.value?.highFrequencyPoints || [])

function message(error: unknown, fallback: string) {
  const api = error as { message?: string; code?: string }
  const labels: Record<string, string> = {
    TEACHING_SCOPE_FORBIDDEN: '当前任教范围不可访问。',
    KNOWLEDGE_POINT_INVALID: '知识点不存在或不属于当前科目。',
  }
  return (api.code && labels[api.code]) || api.message || fallback
}

async function load() {
  loading.value = true
  try {
    const [workspaceData, learningData] = await Promise.all([
      fetchTeacherWorkspace(scopeId.value),
      fetchTeacherLearningSummary(scopeId.value),
    ])
    workspace.value = workspaceData
    learningSummary.value = learningData
  } catch (error) {
    ElMessage.error(message(error, '工作台加载失败。'))
  } finally {
    loading.value = false
  }

  // 消息未读数属于辅助信息，不应阻塞班级工作台的主数据和页面渲染。
  unreadCount.value = 0
  try {
    const conversations = await fetchConversations()
    unreadCount.value = conversations
      .filter((item) => item.teachingAssignmentId === scopeId.value)
      .reduce((total, item) => total + item.unreadCount, 0)
  } catch {
    // 消息接口暂不可用时保留工作台，未读数按 0 展示。
  }
}

function accuracy(value: number | null) {
  return value === null ? '暂无数据' : `${value.toFixed(1)}%`
}

function openCreate() {
  editing.value = null
  Object.assign(form, { knowledgePointId: 0, title: '', content: '', memoryTrick: '', commonMistake: '', sortOrder: 1 })
  dialogVisible.value = true
}

function openEdit(point: HighFrequencyPoint) {
  editing.value = point
  Object.assign(form, { knowledgePointId: point.knowledgePointId, title: point.title, content: point.content, memoryTrick: point.memoryTrick || '', commonMistake: point.commonMistake || '', sortOrder: point.sortOrder })
  dialogVisible.value = true
}

async function savePoint() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid || !workspace.value) return
  saving.value = true
  try {
    if (editing.value) {
      await updateHighFrequencyPoint(editing.value.id, { title: form.title.trim(), content: form.content.trim(), memoryTrick: form.memoryTrick.trim() || undefined, commonMistake: form.commonMistake.trim() || undefined, sortOrder: form.sortOrder })
      ElMessage.success('高频考点已保存。')
    } else {
      await createHighFrequencyPoint(scopeId.value, { knowledgePointId: form.knowledgePointId, title: form.title.trim(), content: form.content.trim(), memoryTrick: form.memoryTrick.trim() || undefined, commonMistake: form.commonMistake.trim() || undefined, sortOrder: form.sortOrder })
      ElMessage.success('高频考点已新增。')
    }
    dialogVisible.value = false
    await load()
  } catch (error) {
    ElMessage.error(message(error, '高频考点保存失败。'))
  } finally {
    saving.value = false
  }
}

async function togglePoint(point: HighFrequencyPoint) {
  try {
    await updateHighFrequencyPointStatus(point.id, point.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE')
    ElMessage.success(point.status === 'ACTIVE' ? '高频考点已停用。' : '高频考点已启用。')
    await load()
  } catch (error) {
    ElMessage.error(message(error, '高频考点状态更新失败。'))
  }
}

async function back() {
  await router.push('/teacher')
}

async function messageStudent(studentId: number) {
  try {
    const conversation = await createConversation(scopeId.value, studentId)
    await router.push(`/messages/${conversation.id}`)
  } catch (error) {
    ElMessage.error(message(error, '私信会话建立失败。'))
  }
}

onMounted(() => void load())
</script>

<template>
  <main class="workspace-page">
    <header class="workspace-header">
      <div>
        <h1>班级学科工作台</h1>
        <p v-if="workspace">{{ workspace.className }} · {{ workspace.subjectName }}</p>
      </div>
      <div class="scope-header-actions">
        <el-button type="primary" plain @click="router.push('/messages')">消息<span v-if="unreadCount">（{{ unreadCount }} 条未读）</span></el-button>
        <el-button @click="back">返回任教范围</el-button>
      </div>
    </header>
    <section v-loading="loading" class="workspace-content teacher-scope-page">
      <template v-if="workspace">
        <div class="scope-overview">
          <div><span>教师</span><strong>{{ workspace.teacherName }}</strong></div>
          <div><span>班级</span><strong>{{ workspace.className }} · {{ workspace.grade }}</strong></div>
          <div><span>科目</span><strong>{{ workspace.subjectName }}</strong></div>
          <div><span>学生人数</span><strong>{{ workspace.studentCount }} 人</strong></div>
        </div>
        <section class="workspace-card">
          <div class="section-title-row">
            <div><h2>学生名单</h2><p>仅展示学生基础信息。</p></div>
          </div>
          <el-table :data="workspace.students" class="data-table" empty-text="当前班级暂无有效学生。">
            <el-table-column prop="studentNumber" label="学号" />
            <el-table-column prop="name" label="姓名" />
            <el-table-column prop="grade" label="年级" />
            <el-table-column label="操作" width="90"><template #default="{ row }"><el-button link type="primary" @click="messageStudent(row.studentId)">私信</el-button></template></el-table-column>
          </el-table>
        </section>
        <section v-if="learningSummary" class="workspace-card">
          <div class="section-title-row">
            <div><h2>班级学习情况</h2><p>仅统计当前班级、当前科目的已提交自动判分练习，不进行排名。</p></div>
          </div>
          <el-table :data="learningSummary.students" class="data-table" empty-text="当前班级暂无有效学生。">
            <el-table-column prop="studentNumber" label="学号" min-width="130" />
            <el-table-column prop="name" label="姓名" min-width="110" />
            <el-table-column prop="answeredCount" label="答题数" width="88" />
            <el-table-column label="正确率" width="100"><template #default="{ row }">{{ accuracy(row.accuracy) }}</template></el-table-column>
            <el-table-column prop="weakKnowledgePointCount" label="薄弱知识点" width="110" />
            <el-table-column prop="masteredKnowledgePointCount" label="已掌握" width="90" />
          </el-table>
        </section>
        <section class="workspace-card">
          <div class="section-title-row">
            <div><h2>高频考点</h2><p>只维护当前教师、当前班级和当前科目的考点。</p></div>
            <el-button type="primary" @click="openCreate">新增高频考点</el-button>
          </div>
          <el-table :data="activePoints" class="data-table" empty-text="当前学科暂无高频考点。">
            <el-table-column prop="title" label="标题" min-width="190" />
            <el-table-column prop="knowledgePointName" label="知识点" min-width="130" />
            <el-table-column prop="content" label="正文" min-width="260" show-overflow-tooltip />
            <el-table-column prop="sortOrder" label="排序" width="80" />
            <el-table-column label="状态" width="90"><template #default="{ row }"><el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'">{{ row.status === 'ACTIVE' ? '启用' : '停用' }}</el-tag></template></el-table-column>
            <el-table-column label="操作" width="170"><template #default="{ row }"><el-button link type="primary" @click="openEdit(row)">编辑</el-button><el-button link type="primary" @click="togglePoint(row)">{{ row.status === 'ACTIVE' ? '停用' : '启用' }}</el-button></template></el-table-column>
          </el-table>
        </section>
      </template>
    </section>
    <el-dialog v-model="dialogVisible" :title="editing ? '编辑高频考点' : '新增高频考点'" width="min(680px, calc(100vw - 32px))" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="知识点" prop="knowledgePointId"><el-select v-model="form.knowledgePointId" placeholder="请选择当前科目知识点" :disabled="Boolean(editing)"><el-option v-for="point in workspace.knowledgePoints" :key="point.id" :label="point.path" :value="point.id" /></el-select></el-form-item>
        <el-form-item label="标题" prop="title"><el-input v-model="form.title" maxlength="200" show-word-limit /></el-form-item>
        <el-form-item label="正文" prop="content"><el-input v-model="form.content" type="textarea" :rows="4" /></el-form-item>
        <el-form-item label="记忆口诀"><el-input v-model="form.memoryTrick" maxlength="500" /></el-form-item>
        <el-form-item label="常见误区"><el-input v-model="form.commonMistake" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sortOrder" :min="0" :max="100000" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" :loading="saving" @click="savePoint">保存</el-button></template>
    </el-dialog>
  </main>
</template>
