<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { exportOperationLogs, fetchOperationLog, fetchOperationLogs, type OperationLogItem } from '../../api/admin/operationLogs'
import type { ApiError } from '../../api/http'

const loading = ref(false)
const records = ref<OperationLogItem[]>([])
const total = ref(0)
const filters = reactive({ page: 1, size: 20, module: '', action: '', result: '' as ''|'SUCCESS'|'FAILURE', operatorId:undefined as number|undefined, objectId:undefined as number|undefined, keyword:'', range:[] as string[], sort:'DESC' as 'ASC'|'DESC' })
const detail=ref<OperationLogItem|null>(null)
const detailVisible=ref(false)
const resultOptions = [{ label: '成功', value: 'SUCCESS' }, { label: '失败', value: 'FAILURE' }]
function readableError(error: unknown) { return (error as ApiError).message || '操作日志加载失败，请稍后重试。' }
function resultLabel(value: string) { return value === 'SUCCESS' ? '成功' : value === 'FAILURE' ? '失败' : value }
function query(){return {page:filters.page,size:filters.size,module:filters.module||undefined,action:filters.action||undefined,result:filters.result||undefined,operatorId:filters.operatorId,objectId:filters.objectId,keyword:filters.keyword||undefined,start:filters.range[0],end:filters.range[1],sort:filters.sort}}
async function load() { loading.value = true; try { const data = await fetchOperationLogs(query()); records.value = data.records; total.value = data.total } catch (error) { ElMessage.error(readableError(error)) } finally { loading.value = false } }
function reset() { Object.assign(filters, { page: 1, size: 20, module: '', action: '', result: '',operatorId:undefined,objectId:undefined,keyword:'',range:[],sort:'DESC' }); void load() }
async function show(id:number){try{detail.value=await fetchOperationLog(id);detailVisible.value=true}catch(error){ElMessage.error(readableError(error))}}
async function download(){try{const {page:_p,size:_s,sort:_o,...params}=query();const blob=await exportOperationLogs(params);const url=URL.createObjectURL(blob);const link=document.createElement('a');link.href=url;link.download='operation-logs.csv';link.click();URL.revokeObjectURL(url)}catch(error){ElMessage.error(readableError(error))}}
onMounted(load)
</script>

<template>
  <section class="admin-page">
    <div class="page-heading"><div><h1>管理员操作日志</h1><p>记录高风险管理操作的真实操作者、业务对象和结果；不展示密码、JWT 或异常详情。</p></div><el-button :loading="loading" @click="load">刷新</el-button></div>
    <el-form class="filter-panel" :inline="true" @submit.prevent="filters.page = 1; load()">
      <el-form-item label="模块"><el-input v-model="filters.module" clearable placeholder="如 QUESTION" /></el-form-item>
      <el-form-item label="动作"><el-input v-model="filters.action" clearable placeholder="如 APPROVED" /></el-form-item>
      <el-form-item label="结果"><el-select v-model="filters.result" clearable placeholder="全部结果"><el-option v-for="item in resultOptions" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item>
      <el-form-item label="操作者 ID"><el-input-number v-model="filters.operatorId" :min="1" controls-position="right" /></el-form-item>
      <el-form-item label="对象 ID"><el-input-number v-model="filters.objectId" :min="1" controls-position="right" /></el-form-item>
      <el-form-item label="关键词"><el-input v-model="filters.keyword" clearable placeholder="摘要或错误码" /></el-form-item>
      <el-form-item label="时间"><el-date-picker v-model="filters.range" type="datetimerange" value-format="YYYY-MM-DDTHH:mm:ss" start-placeholder="开始" end-placeholder="结束" /></el-form-item>
      <el-form-item label="排序"><el-select v-model="filters.sort"><el-option label="最新优先" value="DESC"/><el-option label="最早优先" value="ASC"/></el-select></el-form-item>
      <el-form-item><el-button type="primary" native-type="submit">查询</el-button><el-button @click="reset">重置</el-button><el-button @click="download">导出 CSV</el-button></el-form-item>
    </el-form>
    <el-table v-loading="loading" :data="records" class="data-table" empty-text="暂无操作日志。">
      <el-table-column prop="createdAt" label="时间" min-width="180" /><el-table-column prop="operatorUsername" label="操作者" min-width="150" /><el-table-column prop="module" label="模块" min-width="150" /><el-table-column prop="action" label="动作" min-width="180" /><el-table-column prop="businessObjectId" label="业务对象 ID" min-width="120" /><el-table-column label="结果" min-width="90"><template #default="{ row }"><el-tag :type="row.result === 'SUCCESS' ? 'success' : 'danger'">{{ resultLabel(row.result) }}</el-tag></template></el-table-column><el-table-column prop="summary" label="摘要" min-width="280" /><el-table-column prop="errorCode" label="错误码" min-width="160" /><el-table-column label="详情" fixed="right" width="80"><template #default="{row}"><el-button link type="primary" @click="show(row.id)">查看</el-button></template></el-table-column>
    </el-table>
    <el-pagination class="table-pagination" background layout="total, sizes, prev, pager, next" :total="total" v-model:current-page="filters.page" v-model:page-size="filters.size" :page-sizes="[20, 50, 100]" @current-change="load" @size-change="filters.page = 1; load()" />
    <el-dialog v-model="detailVisible" title="操作日志详情" width="min(620px, calc(100vw - 24px))"><el-descriptions v-if="detail" :column="1" border><el-descriptions-item label="时间">{{detail.createdAt}}</el-descriptions-item><el-descriptions-item label="操作者">{{detail.operatorUsername||detail.operatorId}}</el-descriptions-item><el-descriptions-item label="模块 / 动作">{{detail.module}} / {{detail.action}}</el-descriptions-item><el-descriptions-item label="业务对象">{{detail.businessObjectId||'—'}}</el-descriptions-item><el-descriptions-item label="结果">{{resultLabel(detail.result)}}</el-descriptions-item><el-descriptions-item label="安全摘要">{{detail.summary||'—'}}</el-descriptions-item><el-descriptions-item label="错误码">{{detail.errorCode||'—'}}</el-descriptions-item></el-descriptions><template #footer><el-button @click="detailVisible=false">关闭</el-button></template></el-dialog>
  </section>
</template>
