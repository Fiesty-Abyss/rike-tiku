<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { fetchOperationLogs, type OperationLogItem } from '../../api/admin/operationLogs'
import type { ApiError } from '../../api/http'

const loading = ref(false)
const records = ref<OperationLogItem[]>([])
const total = ref(0)
const filters = reactive({ page: 1, size: 20, module: '', action: '', result: '' as ''|'SUCCESS'|'FAILURE' })
const resultOptions = [{ label: '成功', value: 'SUCCESS' }, { label: '失败', value: 'FAILURE' }]
function readableError(error: unknown) { return (error as ApiError).message || '操作日志加载失败，请稍后重试。' }
function resultLabel(value: string) { return value === 'SUCCESS' ? '成功' : value === 'FAILURE' ? '失败' : value }
async function load() { loading.value = true; try { const data = await fetchOperationLogs({ ...filters, module: filters.module || undefined, action: filters.action || undefined, result: filters.result || undefined }); records.value = data.records; total.value = data.total } catch (error) { ElMessage.error(readableError(error)) } finally { loading.value = false } }
function reset() { Object.assign(filters, { page: 1, size: 20, module: '', action: '', result: '' }); void load() }
onMounted(load)
</script>

<template>
  <section class="admin-page">
    <div class="page-heading"><div><h1>管理员操作日志</h1><p>记录高风险管理操作的真实操作者、业务对象和结果；不展示密码、JWT 或异常详情。</p></div><el-button :loading="loading" @click="load">刷新</el-button></div>
    <el-form class="filter-panel" :inline="true" @submit.prevent="filters.page = 1; load()">
      <el-form-item label="模块"><el-input v-model="filters.module" clearable placeholder="如 QUESTION" /></el-form-item>
      <el-form-item label="动作"><el-input v-model="filters.action" clearable placeholder="如 APPROVED" /></el-form-item>
      <el-form-item label="结果"><el-select v-model="filters.result" clearable placeholder="全部结果"><el-option v-for="item in resultOptions" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item>
      <el-form-item><el-button type="primary" native-type="submit">查询</el-button><el-button @click="reset">重置</el-button></el-form-item>
    </el-form>
    <el-table v-loading="loading" :data="records" class="data-table" empty-text="暂无操作日志。">
      <el-table-column prop="createdAt" label="时间" min-width="180" /><el-table-column prop="operatorUsername" label="操作者" min-width="150" /><el-table-column prop="module" label="模块" min-width="150" /><el-table-column prop="action" label="动作" min-width="180" /><el-table-column prop="businessObjectId" label="业务对象 ID" min-width="120" /><el-table-column label="结果" min-width="90"><template #default="{ row }"><el-tag :type="row.result === 'SUCCESS' ? 'success' : 'danger'">{{ resultLabel(row.result) }}</el-tag></template></el-table-column><el-table-column prop="summary" label="摘要" min-width="280" /><el-table-column prop="errorCode" label="错误码" min-width="160" />
    </el-table>
    <el-pagination class="table-pagination" background layout="total, sizes, prev, pager, next" :total="total" v-model:current-page="filters.page" v-model:page-size="filters.size" :page-sizes="[20, 50, 100]" @current-change="load" @size-change="filters.page = 1; load()" />
  </section>
</template>
