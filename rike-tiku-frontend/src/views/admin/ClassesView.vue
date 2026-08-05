<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'

import { type ApiError } from '../../api/http'
import { changeClassStatus, createClass, fetchClass, fetchClasses, type ClassItem, type ClassStatus, updateClass } from '../../api/admin/classes'

const tableLoading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref<FormInstance>()
const classes = ref<ClassItem[]>([])
const total = ref(0)
const filters = reactive({ code: '', name: '', grade: '', status: '' as '' | ClassStatus, page: 1, size: 10 })
const form = reactive({ id: 0, classCode: '', className: '', grade: '', enrollmentYear: new Date().getFullYear() })
const statusOptions: Array<{ label: string; value: ClassStatus }> = [
  { label: '在读', value: 'ACTIVE' }, { label: '已毕业', value: 'GRADUATED' }, { label: '已停用', value: 'DISABLED' },
]
const rules: FormRules = {
  classCode: [{ required: true, message: '请填写班级编码', trigger: 'blur' }, { max: 64, message: '班级编码最多64个字符', trigger: 'blur' }],
  className: [{ required: true, message: '请填写班级名称', trigger: 'blur' }, { max: 128, message: '班级名称最多128个字符', trigger: 'blur' }],
  grade: [{ required: true, message: '请填写年级', trigger: 'blur' }, { max: 32, message: '年级最多32个字符', trigger: 'blur' }],
  enrollmentYear: [{ required: true, type: 'number', min: 2000, max: 2100, message: '入学年份应在2000至2100之间', trigger: 'change' }],
}

function errorMessage(error: unknown, fallback: string) {
  const apiError = error as ApiError
  if (apiError.code === 'CLASS_CODE_EXISTS') return '班级编码已存在，请更换后重试。'
  if (apiError.code === 'CLASS_NOT_FOUND') return '该班级不存在或已不可用，请刷新列表。'
  return apiError.message || fallback
}

async function loadClasses() {
  tableLoading.value = true
  try {
    const response = await fetchClasses({ ...filters, code: filters.code || undefined, name: filters.name || undefined, grade: filters.grade || undefined, status: filters.status || undefined })
    classes.value = response.records
    total.value = response.total
  } catch (error) { ElMessage.error(errorMessage(error, '班级列表加载失败，请稍后重试。')) } finally { tableLoading.value = false }
}

function resetFilters() { Object.assign(filters, { code: '', name: '', grade: '', status: '', page: 1 }); void loadClasses() }
function openCreate() { isEdit.value = false; Object.assign(form, { id: 0, classCode: '', className: '', grade: '', enrollmentYear: new Date().getFullYear() }); dialogVisible.value = true }
async function openEdit(item: ClassItem) {
  try { const detail = await fetchClass(item.id); isEdit.value = true; Object.assign(form, detail); dialogVisible.value = true } catch (error) { ElMessage.error(errorMessage(error, '班级详情读取失败。')) }
}
async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    if (isEdit.value) await updateClass(form.id, { className: form.className.trim(), grade: form.grade.trim(), enrollmentYear: form.enrollmentYear })
    else await createClass({ classCode: form.classCode.trim(), className: form.className.trim(), grade: form.grade.trim(), enrollmentYear: form.enrollmentYear })
    ElMessage.success(isEdit.value ? '班级信息已保存。' : '班级已创建。')
    dialogVisible.value = false
    await loadClasses()
  } catch (error) { ElMessage.error(errorMessage(error, '班级保存失败，请稍后重试。')) } finally { saving.value = false }
}
async function updateStatus(item: ClassItem, status: ClassStatus) {
  if (status === item.status) return
  try { await changeClassStatus(item.id, { status }); ElMessage.success('班级状态已更新。'); await loadClasses() } catch (error) { ElMessage.error(errorMessage(error, '状态更新失败，请稍后重试。')) }
}
function statusLabel(status: ClassStatus) { return statusOptions.find((item) => item.value === status)?.label || status }

onMounted(loadClasses)
</script>

<template>
  <section class="admin-page">
    <div class="page-heading"><div><h1>班级管理</h1><p>维护班级基础信息与可用状态。班级编码创建后不可修改。</p></div><el-button type="primary" @click="openCreate">创建班级</el-button></div>
    <el-form class="filter-panel" :inline="true" @submit.prevent="filters.page = 1; loadClasses()">
      <el-form-item label="班级编码"><el-input v-model="filters.code" clearable placeholder="支持包含匹配" /></el-form-item>
      <el-form-item label="班级名称"><el-input v-model="filters.name" clearable placeholder="支持包含匹配" /></el-form-item>
      <el-form-item label="年级"><el-input v-model="filters.grade" clearable placeholder="如：高一" /></el-form-item>
      <el-form-item label="状态"><el-select v-model="filters.status" clearable placeholder="全部状态" style="width: 128px"><el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item>
      <el-form-item><el-button type="primary" native-type="submit">查询</el-button><el-button @click="resetFilters">重置</el-button><el-button :loading="tableLoading" @click="loadClasses">刷新</el-button></el-form-item>
    </el-form>
    <el-table v-loading="tableLoading" :data="classes" class="data-table" empty-text="暂无班级数据，可先创建一个班级。">
      <el-table-column prop="classCode" label="班级编码" min-width="140" /> <el-table-column prop="className" label="班级名称" min-width="160" /> <el-table-column prop="grade" label="年级" min-width="100" /> <el-table-column prop="enrollmentYear" label="入学年份" min-width="110" />
      <el-table-column label="状态" min-width="120"><template #default="{ row }"><el-tag :type="row.status === 'ACTIVE' ? 'success' : row.status === 'GRADUATED' ? 'info' : 'danger'">{{ statusLabel(row.status) }}</el-tag></template></el-table-column>
      <el-table-column label="操作" min-width="220" fixed="right"><template #default="{ row }"><el-button link type="primary" @click="openEdit(row)">编辑</el-button><el-dropdown trigger="click" @command="(status: ClassStatus) => updateStatus(row, status)"><el-button link type="primary">切换状态</el-button><template #dropdown><el-dropdown-menu><el-dropdown-item v-for="item in statusOptions" :key="item.value" :command="item.value" :disabled="item.value === row.status">{{ item.label }}</el-dropdown-item></el-dropdown-menu></template></el-dropdown></template></el-table-column>
    </el-table>
    <el-pagination class="table-pagination" background layout="total, sizes, prev, pager, next" :total="total" v-model:current-page="filters.page" v-model:page-size="filters.size" :page-sizes="[10, 20, 50]" @current-change="loadClasses" @size-change="filters.page = 1; loadClasses()" />
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑班级' : '创建班级'" width="min(520px, calc(100vw - 32px))" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="88px"><el-form-item label="班级编码" prop="classCode"><el-input v-model="form.classCode" :disabled="isEdit" placeholder="如：G1-01" /></el-form-item><el-form-item label="班级名称" prop="className"><el-input v-model="form.className" /></el-form-item><el-form-item label="年级" prop="grade"><el-input v-model="form.grade" placeholder="如：高一" /></el-form-item><el-form-item label="入学年份" prop="enrollmentYear"><el-input-number v-model="form.enrollmentYear" :min="2000" :max="2100" controls-position="right" /></el-form-item></el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" :loading="saving" @click="submit">保存</el-button></template>
    </el-dialog>
  </section>
</template>
