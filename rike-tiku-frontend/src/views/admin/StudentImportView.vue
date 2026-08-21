<script setup lang="ts">
import { computed, ref } from 'vue'
import { ElMessage, ElMessageBox, type UploadInstance, type UploadRawFile } from 'element-plus'

import { type ApiError } from '../../api/http'
import { confirmStudentImport, downloadAccountWorkbook, downloadStudentImportTemplate, previewStudentImport, type StudentImportConfirmResponse, type StudentImportPreviewResponse } from '../../api/admin/studentImport'

const uploadRef = ref<UploadInstance>()
const selectedFile = ref<File | null>(null)
const preview = ref<StudentImportPreviewResponse | null>(null)
const result = ref<StudentImportConfirmResponse | null>(null)
const templateDownloading = ref(false)
const previewing = ref(false)
const confirming = ref(false)
const fileError = ref('')

const canConfirm = computed(() => Boolean(selectedFile.value && preview.value && preview.value.invalidCount === 0 && preview.value.validCount > 0 && !confirming.value))
const selectedFileSize = computed(() => selectedFile.value ? `${(selectedFile.value.size / 1024 / 1024).toFixed(2)} MB` : '')

function readableError(error: unknown, fallback: string) {
  const apiError = error as ApiError
  const messages: Record<string, string> = {
    IMPORT_VALIDATION_FAILED: '文件存在无效行，请修正后重新预检查。',
    IMPORT_CONFLICT: '导入数据与当前数据库状态冲突，整批未导入。请重新预检查后再确认。',
  }
  return messages[apiError.code || ''] || apiError.message || fallback
}

function saveBlob(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  link.click()
  URL.revokeObjectURL(url)
}

async function downloadTemplate() {
  templateDownloading.value = true
  try { saveBlob(await downloadStudentImportTemplate(), '学生批量导入模板.xlsx'); ElMessage.success('模板已开始下载。') } catch (error) { ElMessage.error(readableError(error, '模板下载失败，请稍后重试。')) } finally { templateDownloading.value = false }
}

function validateFile(file: UploadRawFile) {
  fileError.value = ''
  const isXlsx = file.name.toLowerCase().endsWith('.xlsx')
  if (!isXlsx) { fileError.value = '只支持 .xlsx 文件。'; return false }
  if (file.size > 5 * 1024 * 1024) { fileError.value = '文件不能超过 5MB。'; return false }
  return true
}

function onFileChange(file: { raw?: UploadRawFile }) {
  if (!file.raw || !validateFile(file.raw)) { uploadRef.value?.clearFiles(); return }
  selectedFile.value = file.raw
  preview.value = null
  result.value = null
  fileError.value = ''
}

function removeFile() { selectedFile.value = null; preview.value = null; result.value = null; fileError.value = ''; uploadRef.value?.clearFiles() }

async function runPreview() {
  if (!selectedFile.value) return
  previewing.value = true
  try { preview.value = await previewStudentImport(selectedFile.value); result.value = null; ElMessage.success('预检查完成。') } catch (error) { ElMessage.error(readableError(error, '预检查失败，请检查 Excel 内容或网络连接。')) } finally { previewing.value = false }
}

async function confirmImport() {
  if (!selectedFile.value || !canConfirm.value) return
  try {
    await ElMessageBox.confirm('确认后会重新上传原始 Excel，并在一个事务中导入整批学生。初始密码仅在成功后显示一次，请妥善发放。', '确认导入学生', { type: 'warning', confirmButtonText: '确认导入', cancelButtonText: '返回检查' })
  } catch { return }
  confirming.value = true
  try { result.value = await confirmStudentImport(selectedFile.value); ElMessage.success(`已成功导入 ${result.value.importedCount} 名学生。`) } catch (error) { ElMessage.error(readableError(error, '导入失败，文件与预检查结果已保留。')) } finally { confirming.value = false }
}

function clearSensitiveResult() { result.value = null; ElMessage.success('账号发放结果已从当前页面清除。') }
async function downloadAccounts() {
  if (!result.value) return
  try { await downloadAccountWorkbook(result.value.accounts); ElMessage.success('账号发放 Excel 已开始下载。') } catch { ElMessage.error('账号发放 Excel 生成失败，请稍后重试。') }
}
</script>

<template>
  <section class="admin-page student-import-page">
    <div class="page-heading"><div><h1>学生 Excel 导入</h1><p>预检查通过后才可确认入库；导入为整批事务，账号初始密码只显示一次。</p></div><el-button :loading="templateDownloading" @click="downloadTemplate">下载导入模板</el-button></div>
    <el-steps :active="result ? 4 : preview ? 3 : selectedFile ? 2 : 1" simple class="import-steps"><el-step title="下载模板" /><el-step title="选择文件" /><el-step title="预检查" /><el-step title="确认导入" /><el-step title="发放账号" /></el-steps>
    <el-alert title="安全提示：Excel 中的明文密码不会在预览中展示；确认导入后生成的初始密码不会保存到浏览器存储。" type="info" :closable="false" show-icon />
    <section class="import-stage"><h2>1. 选择 Excel 文件</h2><p>仅支持单个 .xlsx 文件，大小不超过 5MB。重新选择文件会清除旧预览和账号结果。</p>
      <el-upload ref="uploadRef" drag :auto-upload="false" :limit="1" accept=".xlsx" :on-change="onFileChange" :on-remove="removeFile" :before-upload="validateFile">
        <div class="upload-copy"><strong>拖拽 Excel 到这里，或点击选择文件</strong><span>请选择管理员下载的学生批量导入模板</span></div>
      </el-upload>
      <el-alert v-if="fileError" :title="fileError" type="error" show-icon :closable="false" class="inline-alert" />
      <div v-if="selectedFile" class="selected-file"><div><strong>{{ selectedFile.name }}</strong><span>{{ selectedFileSize }}</span></div><el-button link type="danger" @click="removeFile">移除文件</el-button></div>
      <div class="stage-actions"><el-button type="primary" :disabled="!selectedFile" :loading="previewing" @click="runPreview">上传并预检查</el-button></div>
    </section>
    <section v-if="preview" class="import-stage"><div class="section-title-row"><div><h2>2. 预检查结果</h2><p>请修正全部无效行后再确认导入。</p></div><el-button :loading="previewing" @click="runPreview">重新预检查</el-button></div>
      <div class="preview-summary"><div><span>总行数</span><strong>{{ preview.totalCount }}</strong></div><div class="success"><span>有效行</span><strong>{{ preview.validCount }}</strong></div><div :class="{ danger: preview.invalidCount > 0 }"><span>无效行</span><strong>{{ preview.invalidCount }}</strong></div></div>
      <el-alert v-if="preview.invalidCount > 0" title="当前存在无效行，确认导入已禁用。请修正 Excel 后重新选择并预检查。" type="warning" :closable="false" show-icon />
      <el-table :data="preview.rows" class="data-table import-table" max-height="460" empty-text="Excel 未包含可检查的数据行。"><el-table-column prop="rowNumber" label="Excel行" width="84" /><el-table-column prop="studentNumber" label="学号" min-width="120" /><el-table-column prop="name" label="姓名" min-width="100" /><el-table-column prop="classCode" label="班级" min-width="120" /><el-table-column prop="grade" label="年级" min-width="90" /><el-table-column prop="username" label="用户名" min-width="120" /><el-table-column prop="accountStatus" label="账号状态" min-width="100" /><el-table-column label="提供密码" min-width="100"><template #default="{ row }">{{ row.passwordProvided ? '是' : row.passwordWillGenerate ? '否，将生成' : '否' }}</template></el-table-column><el-table-column label="行状态" min-width="100"><template #default="{ row }"><el-tag :type="row.status === 'VALID' ? 'success' : 'danger'">{{ row.status === 'VALID' ? '有效' : '无效' }}</el-tag></template></el-table-column><el-table-column label="字段错误" min-width="260"><template #default="{ row }"><span v-if="!row.errors?.length">—</span><ul v-else class="error-list"><li v-for="error in row.errors" :key="`${error.field}-${error.code}`"><strong>{{ error.code }}</strong>：{{ error.message }}</li></ul></template></el-table-column></el-table>
      <div class="stage-actions confirm-area"><el-button type="primary" :disabled="!canConfirm" :loading="confirming" @click="confirmImport">确认入库</el-button><span>确认时会重新上传当前原始 Excel，不会提交预览 JSON。</span></div>
    </section>
    <section v-if="result" class="import-stage sensitive-result"><div class="section-title-row"><div><h2>3. 账号发放结果</h2><p>请立即安全发放初始密码；刷新页面或点击清空后不能恢复。</p></div><div><el-button @click="downloadAccounts">下载账号发放 Excel</el-button><el-button type="danger" plain @click="clearSensitiveResult">清空敏感结果</el-button></div></div>
      <el-alert :title="`本次已导入 ${result.importedCount}/${result.totalCount} 名学生。所有账号首次登录后必须修改初始密码。`" type="success" :closable="false" show-icon />
      <el-table :data="result.accounts" class="data-table" max-height="420"><el-table-column prop="studentNumber" label="学号" min-width="120" /><el-table-column prop="name" label="姓名" min-width="100" /><el-table-column prop="classCode" label="班级" min-width="120" /><el-table-column prop="username" label="用户名" min-width="120" /><el-table-column prop="initialPassword" label="初始密码" min-width="150" /><el-table-column prop="accountStatus" label="账号状态" min-width="105" /><el-table-column label="首次登录" min-width="170"><template #default="{ row }">{{ row.mustChangePassword ? '必须修改初始密码' : '无需修改' }}</template></el-table-column></el-table>
    </section>
  </section>
</template>
