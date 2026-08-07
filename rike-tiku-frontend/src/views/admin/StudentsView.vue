<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { type ApiError } from '../../api/http'
import { fetchClasses, type ClassItem } from '../../api/admin/classes'
import { confirmStudentPasswordReset } from './studentResetConfirmation'
import {
  createStudent,
  fetchStudent,
  fetchStudents,
  resetStudentPassword,
  transferStudent,
  updateStudent,
  type AccountStatus,
  type StudentDetail,
  type StudentItem,
  type StudentProfileStatus,
} from '../../api/admin/students'

const loading = ref(false)
const saving = ref(false)
const formVisible = ref(false)
const detailVisible = ref(false)
const transferVisible = ref(false)
const editing = ref(false)
const formRef = ref<FormInstance>()
const students = ref<StudentItem[]>([])
const classes = ref<ClassItem[]>([])
const detail = ref<StudentDetail | null>(null)
const total = ref(0)
const initialPassword = ref<string | null>(null)

const filters = reactive({
  studentNumber: '',
  name: '',
  username: '',
  classId: undefined as number | undefined,
  grade: '',
  accountStatus: '' as '' | AccountStatus,
  profileStatus: '' as '' | StudentProfileStatus,
  page: 1,
  size: 10,
})
const form = reactive({
  id: 0,
  studentNumber: '',
  name: '',
  username: '',
  grade: '高三',
  classId: undefined as number | undefined,
  accountStatus: 'ENABLED' as AccountStatus,
  profileStatus: 'ACTIVE' as StudentProfileStatus,
})
const transferClassId = ref<number>()

const accountStatuses: Array<{ label: string; value: AccountStatus }> = [
  { label: '启用', value: 'ENABLED' },
  { label: '停用', value: 'DISABLED' },
  { label: '锁定', value: 'LOCKED' },
]
const profileStatuses: Array<{ label: string; value: StudentProfileStatus }> = [
  { label: '有效', value: 'ACTIVE' },
  { label: '停用', value: 'DISABLED' },
]
const rules: FormRules = {
  studentNumber: [{ required: true, message: '请填写学号', trigger: 'blur' }],
  name: [{ required: true, message: '请填写姓名', trigger: 'blur' }],
  username: [{ required: true, message: '请填写用户名', trigger: 'blur' }],
  grade: [{ required: true, message: '请填写年级', trigger: 'blur' }],
  classId: [{ required: true, message: '请选择主班级', trigger: 'change' }],
}

function statusLabel(options: Array<{ label: string; value: string }>, value: string) {
  return options.find((item) => item.value === value)?.label || value
}

function errorMessage(error: unknown, fallback: string) {
  const api = error as ApiError
  const messages: Record<string, string> = {
    STUDENT_NUMBER_EXISTS: '学号已存在。',
    USERNAME_EXISTS: '用户名已存在。',
    CLASS_UNAVAILABLE: '班级不存在或已停用。',
    STUDENT_ALREADY_IN_CLASS: '学生已经在目标班级。',
    ACTIVE_MAIN_CLASS_INVALID: '学生当前主班级关系异常。',
  }
  return api.code ? messages[api.code] || api.message || fallback : api.message || fallback
}

async function loadClasses() {
  const data = await fetchClasses({ page: 1, size: 100, status: 'ACTIVE' })
  classes.value = data.records
}

async function loadStudents() {
  loading.value = true
  try {
    const data = await fetchStudents({
      ...filters,
      studentNumber: filters.studentNumber || undefined,
      name: filters.name || undefined,
      username: filters.username || undefined,
      grade: filters.grade || undefined,
      accountStatus: filters.accountStatus || undefined,
      profileStatus: filters.profileStatus || undefined,
    })
    students.value = data.records
    total.value = data.total
  } catch (error) {
    ElMessage.error(errorMessage(error, '学生列表加载失败。'))
  } finally {
    loading.value = false
  }
}

function resetFilters() {
  Object.assign(filters, { studentNumber: '', name: '', username: '', classId: undefined, grade: '', accountStatus: '', profileStatus: '', page: 1 })
  void loadStudents()
}

async function openCreate() {
  editing.value = false
  Object.assign(form, { id: 0, studentNumber: '', name: '', username: '', grade: '高三', classId: undefined, accountStatus: 'ENABLED', profileStatus: 'ACTIVE' })
  await loadClasses()
  formVisible.value = true
}

function openEdit(item: StudentItem) {
  editing.value = true
  Object.assign(form, { ...item, classId: item.currentClass?.id })
  formVisible.value = true
}

async function saveStudent() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    if (editing.value) {
      await updateStudent(form.id, { name: form.name.trim(), grade: form.grade.trim(), accountStatus: form.accountStatus, profileStatus: form.profileStatus })
      ElMessage.success('学生信息已保存。')
    } else if (form.classId !== undefined) {
      const result = await createStudent({ studentNumber: form.studentNumber.trim(), name: form.name.trim(), username: form.username.trim(), grade: form.grade.trim(), classId: form.classId })
      initialPassword.value = result.initialPassword
      ElMessage.success('学生已创建，初始密码仅显示本次。')
    }
    formVisible.value = false
    await loadStudents()
  } catch (error) {
    ElMessage.error(errorMessage(error, '学生保存失败。'))
  } finally {
    saving.value = false
  }
}

async function showDetail(item: StudentItem) {
  try {
    detail.value = await fetchStudent(item.id)
    detailVisible.value = true
  } catch (error) {
    ElMessage.error(errorMessage(error, '学生详情加载失败。'))
  }
}

async function openTransfer() {
  await loadClasses()
  transferClassId.value = undefined
  transferVisible.value = true
}

async function saveTransfer() {
  if (!detail.value || transferClassId.value === undefined) return
  saving.value = true
  try {
    detail.value = await transferStudent(detail.value.student.id, transferClassId.value)
    transferVisible.value = false
    await loadStudents()
    ElMessage.success('调班完成，原班级关系已保留为历史。')
  } catch (error) {
    ElMessage.error(errorMessage(error, '调班失败。'))
  } finally {
    saving.value = false
  }
}

async function resetPassword() {
  if (!detail.value) return
  const confirmed = await confirmStudentPasswordReset(() => ElMessageBox.confirm(
    '重置后原密码立即失效，新密码仅显示一次。',
    '确认重置密码',
    { type: 'warning', confirmButtonText: '确认重置', cancelButtonText: '取消' },
  ))
  if (!confirmed) return
  try {
    initialPassword.value = (await resetStudentPassword(detail.value.student.id)).initialPassword
    ElMessage.success('密码已重置。')
  } catch (error) {
    ElMessage.error(errorMessage(error, '密码重置失败。'))
  }
}

onMounted(async () => {
  await Promise.all([loadStudents(), loadClasses()])
})
</script>

<template>
  <section class="admin-page">
    <div class="page-heading">
      <div><h1>学生管理</h1><p>管理单个学生账号、档案、主班级历史和初始密码。</p></div>
      <div><el-button @click="$router.push('/admin/students/import')">Excel 批量导入</el-button><el-button type="primary" @click="openCreate">新增学生</el-button></div>
    </div>
    <el-alert v-if="initialPassword" class="inline-alert" type="warning" :closable="false" show-icon>
      <template #title>初始密码仅显示一次：<strong>{{ initialPassword }}</strong><el-button link type="danger" @click="initialPassword = null">立即清空</el-button></template>
    </el-alert>
    <el-form class="filter-panel" :inline="true" @submit.prevent="filters.page = 1; loadStudents()">
      <el-form-item label="学号"><el-input v-model="filters.studentNumber" clearable /></el-form-item>
      <el-form-item label="姓名"><el-input v-model="filters.name" clearable /></el-form-item>
      <el-form-item label="用户名"><el-input v-model="filters.username" clearable /></el-form-item>
      <el-form-item label="班级"><el-select v-model="filters.classId" clearable filterable><el-option v-for="item in classes" :key="item.id" :label="item.className" :value="item.id" /></el-select></el-form-item>
      <el-form-item label="年级"><el-input v-model="filters.grade" clearable /></el-form-item>
      <el-form-item label="账号"><el-select v-model="filters.accountStatus" clearable><el-option v-for="item in accountStatuses" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item>
      <el-form-item label="档案"><el-select v-model="filters.profileStatus" clearable><el-option v-for="item in profileStatuses" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item>
      <el-form-item><el-button type="primary" native-type="submit">查询</el-button><el-button @click="resetFilters">重置</el-button></el-form-item>
    </el-form>
    <el-table v-loading="loading" :data="students" class="data-table" empty-text="暂无学生数据。">
      <el-table-column prop="studentNumber" label="学号" min-width="130" />
      <el-table-column prop="name" label="姓名" min-width="110" />
      <el-table-column prop="username" label="用户名" min-width="140" />
      <el-table-column prop="grade" label="年级" min-width="90" />
      <el-table-column label="当前主班级" min-width="130"><template #default="{ row }">{{ row.currentClass?.className || '未分班' }}</template></el-table-column>
      <el-table-column label="账号状态" min-width="100"><template #default="{ row }"><el-tag :type="row.accountStatus === 'ENABLED' ? 'success' : 'danger'">{{ statusLabel(accountStatuses, row.accountStatus) }}</el-tag></template></el-table-column>
      <el-table-column label="档案状态" min-width="100"><template #default="{ row }"><el-tag :type="row.profileStatus === 'ACTIVE' ? 'success' : 'info'">{{ statusLabel(profileStatuses, row.profileStatus) }}</el-tag></template></el-table-column>
      <el-table-column label="操作" fixed="right" min-width="140"><template #default="{ row }"><el-button link type="primary" @click="showDetail(row)">详情</el-button><el-button link type="primary" @click="openEdit(row)">编辑</el-button></template></el-table-column>
    </el-table>
    <el-pagination class="table-pagination" background layout="total, sizes, prev, pager, next" :total="total" v-model:current-page="filters.page" v-model:page-size="filters.size" :page-sizes="[10, 20, 50]" @current-change="loadStudents" @size-change="filters.page = 1; loadStudents()" />

    <el-dialog v-model="formVisible" :title="editing ? '编辑学生' : '新增学生'" width="min(560px, calc(100vw - 32px))">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="学号" prop="studentNumber"><el-input v-model="form.studentNumber" :disabled="editing" /></el-form-item>
        <el-form-item label="姓名" prop="name"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="用户名" prop="username"><el-input v-model="form.username" :disabled="editing" /></el-form-item>
        <el-form-item label="年级" prop="grade"><el-input v-model="form.grade" /></el-form-item>
        <el-form-item v-if="!editing" label="主班级" prop="classId"><el-select v-model="form.classId" filterable style="width:100%"><el-option v-for="item in classes" :key="item.id" :label="`${item.className}（${item.classCode}）`" :value="item.id" /></el-select></el-form-item>
        <el-form-item v-if="editing" label="账号状态"><el-select v-model="form.accountStatus"><el-option v-for="item in accountStatuses" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item>
        <el-form-item v-if="editing" label="档案状态"><el-select v-model="form.profileStatus"><el-option v-for="item in profileStatuses" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item>
      </el-form>
      <template #footer><el-button @click="formVisible = false">取消</el-button><el-button type="primary" :loading="saving" @click="saveStudent">保存</el-button></template>
    </el-dialog>

    <el-drawer v-model="detailVisible" title="学生详情" size="min(820px, 100%)">
      <template v-if="detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="姓名">{{ detail.student.name }}</el-descriptions-item><el-descriptions-item label="学号">{{ detail.student.studentNumber }}</el-descriptions-item>
          <el-descriptions-item label="用户名">{{ detail.student.username }}</el-descriptions-item><el-descriptions-item label="年级">{{ detail.student.grade }}</el-descriptions-item>
          <el-descriptions-item label="账号状态">{{ statusLabel(accountStatuses, detail.student.accountStatus) }}</el-descriptions-item><el-descriptions-item label="学生角色">{{ detail.roles.join('、') }}</el-descriptions-item>
          <el-descriptions-item label="当前主班级">{{ detail.student.currentClass?.className || '未分班' }}</el-descriptions-item><el-descriptions-item label="档案状态">{{ statusLabel(profileStatuses, detail.student.profileStatus) }}</el-descriptions-item>
        </el-descriptions>
        <div class="section-title-row"><div><h2>班级历史</h2><p>调班会结束旧关系并新增当前主班级。</p></div><div><el-button @click="resetPassword">重置密码</el-button><el-button type="primary" @click="openTransfer">调班</el-button></div></div>
        <el-table :data="detail.classHistory" class="data-table" empty-text="暂无班级历史。">
          <el-table-column prop="className" label="班级" /><el-table-column prop="joinedAt" label="加入时间" min-width="180" /><el-table-column prop="exitedAt" label="退出时间" min-width="180"><template #default="{ row }">{{ row.exitedAt || '—' }}</template></el-table-column><el-table-column label="当前有效"><template #default="{ row }"><el-tag :type="row.current ? 'success' : 'info'">{{ row.current ? '是' : '否' }}</el-tag></template></el-table-column>
        </el-table>
      </template>
    </el-drawer>

    <el-dialog v-model="transferVisible" title="调班" width="min(480px, calc(100vw - 32px))">
      <el-alert type="info" :closable="false" title="原主班级关系将结束并保留在班级历史中。" />
      <el-select v-model="transferClassId" filterable placeholder="请选择目标班级" style="width:100%;margin-top:20px"><el-option v-for="item in classes" :key="item.id" :label="`${item.className}（${item.classCode}）`" :value="item.id" :disabled="item.id === detail?.student.currentClass?.id" /></el-select>
      <template #footer><el-button @click="transferVisible = false">取消</el-button><el-button type="primary" :loading="saving" :disabled="transferClassId === undefined" @click="saveTransfer">确认调班</el-button></template>
    </el-dialog>
  </section>
</template>
