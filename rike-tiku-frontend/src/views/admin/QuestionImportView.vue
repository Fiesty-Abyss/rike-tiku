<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type UploadInstance, type UploadRawFile } from 'element-plus'

import { type ApiError } from '../../api/http'
import { confirmQuestionImport, previewQuestionImport, type QuestionImportConfirm, type QuestionImportPreview } from '../../api/admin/questionImport'
import { canConfirmQuestionImport, questionImportFileError } from './questionImportForm'

const router = useRouter()
const uploadRef = ref<UploadInstance>()
const selectedFile = ref<File | null>(null)
const preview = ref<QuestionImportPreview | null>(null)
const result = ref<QuestionImportConfirm | null>(null)
const previewing = ref(false)
const confirming = ref(false)
const fileError = ref('')
const canConfirm = computed(() => canConfirmQuestionImport(selectedFile.value, preview.value, confirming.value))

function readableError(error:unknown, fallback:string) {
  const api = error as ApiError
  const messages:Record<string, string> = {
    IMPORT_FILE_CHANGED: '确认文件与预检查文件不一致，请重新预检查。',
    IMPORT_ALREADY_CONFIRMED: '该文件已经成功导入，不能重复确认。',
    IMPORT_VALIDATION_FAILED: '存在无效或重复题目，整批不能确认入库。',
    ATTACHMENT_OBJECT_MISSING: '存在未找到的附件对象，请补齐对象文件后重新检查。',
    MUST_CHANGE_PASSWORD: '请先修改初始密码后再使用导入功能。',
  }
  return messages[api.code || ''] || api.message || fallback
}

function validateFile(file:UploadRawFile) {
  fileError.value = questionImportFileError(file)
  return !fileError.value
}

function onFileChange(file:{ raw?:UploadRawFile }) {
  selectedFile.value = null
  preview.value = null
  result.value = null
  if (!file.raw || !validateFile(file.raw)) { uploadRef.value?.clearFiles(); return }
  selectedFile.value = file.raw
}

function clear() {
  selectedFile.value = null
  preview.value = null
  result.value = null
  fileError.value = ''
  uploadRef.value?.clearFiles()
}

async function runPreview() {
  if (!selectedFile.value) return
  preview.value = null
  result.value = null
  previewing.value = true
  try {
    preview.value = await previewQuestionImport(selectedFile.value)
    result.value = null
    ElMessage.success('题库预检查完成。')
  } catch (error) {
    preview.value = null
    ElMessage.error(readableError(error, '预检查失败，请检查 Excel 与附件对象。'))
  } finally {
    previewing.value = false
  }
}

async function confirmImport() {
  if (!selectedFile.value || !preview.value || !canConfirm.value) return
  try {
    await ElMessageBox.confirm('确认后会重新上传当前原始 Excel，后端将重新解析并在一个事务中导入整批题目。所有题目均进入待审核，不会自动发布。', '确认导入题库', { type:'warning', confirmButtonText:'确认入库', cancelButtonText:'返回检查' })
  } catch { return }
  confirming.value = true
  try {
    result.value = await confirmQuestionImport(selectedFile.value, preview.value.fileHash)
    selectedFile.value = null
    preview.value = null
    uploadRef.value?.clearFiles()
    ElMessage.success(`已导入 ${result.value.importedCount} 道待审核题目。`)
  } catch (error) {
    ElMessage.error(readableError(error, '导入失败，已保留文件与预检查结果。'))
  } finally {
    confirming.value = false
  }
}
</script>

<template>
  <section class="admin-page question-import-page">
    <div class="page-heading"><div><h1>MVP30 题库 Excel 导入</h1><p>上传后先逐行预检查；确认时重新解析原始文件，成功题目统一进入待审核。</p></div><el-button @click="router.push('/admin/questions')">返回题库审核</el-button></div>
    <el-alert title="安全边界：页面不会保存原始 Excel、附件内容或预检查结果到浏览器存储；附件仅按正文对象标识精确匹配。" type="info" :closable="false" show-icon />
    <section class="import-stage"><h2>1. 选择文件</h2><p>每次仅导入一份学科 Excel。仅支持 .xlsx，最大 10MB；重新选择会清除当前预览。</p>
      <el-upload ref="uploadRef" drag :auto-upload="false" :limit="1" accept=".xlsx" :on-change="onFileChange" :on-remove="clear" :before-upload="validateFile"><div class="upload-copy"><strong>拖拽题库 Excel 到这里，或点击选择</strong><span>请选择“待审核_清洗版.xlsx”原文件</span></div></el-upload>
      <el-alert v-if="fileError" :title="fileError" type="error" :closable="false" show-icon class="inline-alert" />
      <div v-if="selectedFile" class="selected-file"><div><strong>{{ selectedFile.name }}</strong><span>{{ (selectedFile.size / 1024 / 1024).toFixed(2) }} MB</span></div><el-button link type="danger" @click="clear">移除文件</el-button></div>
      <div class="stage-actions"><el-button type="primary" :disabled="!selectedFile" :loading="previewing" @click="runPreview">上传并预检查</el-button></div>
    </section>
    <section v-if="preview" class="import-stage"><div class="section-title-row"><div><h2>2. 逐行预检查</h2><p>学科：{{ preview.subjectCode || '未确认' }}；文件哈希：{{ preview.fileHash.slice(0, 12) }}…</p></div><el-button :loading="previewing" @click="runPreview">重新预检查</el-button></div>
      <div class="preview-summary"><div><span>总行数</span><strong>{{ preview.totalCount }}</strong></div><div class="success"><span>有效行</span><strong>{{ preview.validCount }}</strong></div><div :class="{ danger: preview.invalidCount > 0 }"><span>无效行</span><strong>{{ preview.invalidCount }}</strong></div><div :class="{ danger: preview.duplicateCount > 0 }"><span>重复行</span><strong>{{ preview.duplicateCount }}</strong></div></div>
      <el-alert v-if="preview.alreadyImported" title="该文件哈希已成功导入，确认按钮已禁用。" type="warning" :closable="false" show-icon />
      <el-alert v-else-if="preview.invalidCount > 0" title="当前有无效行，确认导入已禁用。修正原文件或附件后请重新选择并预检查。" type="warning" :closable="false" show-icon />
      <el-table :data="preview.rows" class="data-table import-table" max-height="500" empty-text="Excel 未包含可检查的题目行。"><el-table-column prop="rowNumber" label="Excel行" width="84" /><el-table-column prop="questionType" label="映射题型" min-width="145" /><el-table-column prop="usageMode" label="使用模式" min-width="125" /><el-table-column prop="stemSummary" label="题干摘要" min-width="230" show-overflow-tooltip /><el-table-column label="知识点" min-width="220"><template #default="{ row }">{{ row.knowledgePointPaths.join('；') || '—' }}</template></el-table-column><el-table-column prop="attachmentCount" label="附件对象" min-width="100" /><el-table-column label="状态" min-width="90"><template #default="{ row }"><el-tag :type="row.status === 'VALID' ? 'success' : 'danger'">{{ row.status === 'VALID' ? '有效' : '无效' }}</el-tag></template></el-table-column><el-table-column label="错误与提示" min-width="280"><template #default="{ row }"><ul v-if="row.errors.length" class="error-list"><li v-for="error in row.errors" :key="`${error.field}-${error.code}`"><strong>{{ error.code }}</strong>：{{ error.message }}</li></ul><ul v-else-if="row.warnings.length" class="error-list"><li v-for="warning in row.warnings" :key="warning">提示：{{ warning }}</li></ul><span v-else>—</span></template></el-table-column></el-table>
      <div class="stage-actions confirm-area"><el-button type="primary" :disabled="!canConfirm" :loading="confirming" @click="confirmImport">重新上传原文件并确认入库</el-button><span>确认不提交预览 JSON，服务端会重新解析、校验和写入整批事务。</span></div>
    </section>
    <section v-if="result" class="import-stage"><h2>3. 导入完成</h2><el-alert :title="`批次 ${result.batchCode} 已导入 ${result.importedCount}/${result.totalCount} 道题目，全部为待审核状态。`" type="success" :closable="false" show-icon /><div class="stage-actions"><el-button type="primary" @click="router.push('/admin/questions')">前往题库审核列表</el-button><el-button @click="clear">继续导入下一文件</el-button></div></section>
  </section>
</template>
