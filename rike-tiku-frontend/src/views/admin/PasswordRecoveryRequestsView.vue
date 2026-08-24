<script setup lang="ts">
import { onMounted,ref } from 'vue';import { ElMessage,ElMessageBox } from 'element-plus';import { deletePasswordRecovery,fetchPasswordRecoveries,rejectPasswordRecovery,resolvePasswordRecovery,type PasswordRecoveryItem } from '../../api/admin/passwordRecovery'
const records=ref<PasswordRecoveryItem[]>([]);const loading=ref(false)
const roleLabel=(value:string)=>({STUDENT:'学生',TEACHER:'教师',ADMIN:'管理员'} as Record<string,string>)[value]||value
const statusLabel=(value:string)=>({PENDING:'待处理',RESOLVED:'已恢复',REJECTED:'已驳回'} as Record<string,string>)[value]||value
async function load(){loading.value=true;try{records.value=(await fetchPasswordRecoveries()).records}finally{loading.value=false}}
async function resolve(item:PasswordRecoveryItem){await ElMessageBox.confirm(`确认恢复账号 ${item.username} 的默认密码？恢复后须首次改密。`,'确认恢复',{type:'warning'});await resolvePasswordRecovery(item.id);ElMessage.success('已恢复默认密码');await load()}
async function reject(item:PasswordRecoveryItem){const result=await ElMessageBox.prompt('请输入驳回原因','驳回请求',{inputValidator:v=>Boolean(v.trim())||'请输入原因'});await rejectPasswordRecovery(item.id,result.value);ElMessage.success('已驳回');await load()}
async function remove(item:PasswordRecoveryItem){await ElMessageBox.confirm('确定删除这条已处理的密码恢复记录吗？','删除记录',{type:'warning'});await deletePasswordRecovery(item.id);ElMessage.success('记录已删除');await load()}
onMounted(load)
</script>
<template><section v-loading="loading"><el-table :data="records"><el-table-column prop="username" label="账号"/><el-table-column prop="name" label="姓名"/><el-table-column label="角色"><template #default="{row}">{{roleLabel(row.role)}}</template></el-table-column><el-table-column prop="requestedAt" label="申请时间"/><el-table-column label="状态"><template #default="{row}">{{statusLabel(row.status)}}</template></el-table-column><el-table-column label="操作"><template #default="{row}"><template v-if="row.status==='PENDING'"><el-button type="primary" @click="resolve(row)">恢复默认密码</el-button><el-button @click="reject(row)">驳回</el-button></template><el-button v-else type="danger" link @click="remove(row)">删除</el-button></template></el-table-column></el-table></section></template>
