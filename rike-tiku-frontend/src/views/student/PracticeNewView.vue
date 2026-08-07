<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { createPracticeSession, fetchPracticeOptions, type KnowledgePoint, type QuestionType, type Subject } from '../../api/student/practice'
import type { ApiError } from '../../api/http'

const router = useRouter()
const route = useRoute()
const formRef = ref<FormInstance>()
const loading = ref(false)
const subjects = ref<Subject[]>([])
const points = ref<KnowledgePoint[]>([])
const form = reactive({ subjectId: 0, knowledgePointIds: [] as number[], questionTypes: [] as QuestionType[], difficulty: undefined as number|undefined, count: 5 })
const rules:FormRules = { subjectId:[{ required:true, message:'请选择学科', trigger:'change' }], count:[{ required:true, message:'请输入练习题数', trigger:'blur' }] }
const questionTypes:Array<{label:string;value:QuestionType}> = [{label:'单选题',value:'SINGLE_CHOICE'},{label:'多选题',value:'MULTIPLE_CHOICE'},{label:'填空题',value:'FILL_BLANK'}]
function message(error:unknown) { const api=error as ApiError; return ({ PRACTICE_QUESTION_INSUFFICIENT:'符合条件的已发布题目不足，请减少题数或调整筛选。', PRACTICE_KNOWLEDGE_POINT_INVALID:'知识点不存在、已停用或不属于所选学科。' } as Record<string,string>)[api.code || ''] || api.message || '创建练习失败，请稍后重试。' }
async function loadPoints() { if (!form.subjectId) return; try { points.value=(await fetchPracticeOptions(form.subjectId)).knowledgePoints; form.knowledgePointIds=[] } catch (error) { ElMessage.error(message(error)) } }
async function submit() { const valid=await formRef.value?.validate().catch(()=>false); if (!valid) return; loading.value=true; try { const session=await createPracticeSession({ subjectId:form.subjectId, knowledgePointIds:form.knowledgePointIds.length?form.knowledgePointIds:undefined, questionTypes:form.questionTypes.length?form.questionTypes:undefined, difficulty:form.difficulty, count:form.count }); ElMessage.success('练习已创建，题目集合已冻结。'); await router.push(`/student/practice/${session.id}`) } catch (error) { ElMessage.error(message(error)) } finally { loading.value=false } }
onMounted(async()=>{ try { const data=await fetchPracticeOptions(); subjects.value=data.subjects; const preset=Number(route.query.subjectId); form.subjectId=subjects.value.some(item=>item.id===preset)?preset:(subjects.value[0]?.id || 0); await loadPoints(); const pointId=Number(route.query.knowledgePointId); if(pointId && points.value.some(item=>item.id===pointId)) form.knowledgePointIds=[pointId] } catch(error){ ElMessage.error(message(error)) } })
watch(()=>form.subjectId, ()=>void loadPoints())
</script>
<template><section class="student-page"><div class="student-page-heading"><div><h1>创建自主练习</h1><p>仅从已发布、可自动判分的在线练习题中选择；题目不足时不会补题。</p></div><el-button @click="router.push('/student/practice')">返回</el-button></div><el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="practice-form"><el-form-item label="学科" prop="subjectId"><el-select v-model="form.subjectId"><el-option v-for="subject in subjects" :key="subject.id" :label="subject.name" :value="subject.id" /></el-select></el-form-item><el-form-item label="知识点（可选）"><el-select v-model="form.knowledgePointIds" multiple filterable collapse-tags placeholder="不选则覆盖全科"><el-option v-for="point in points" :key="point.id" :label="point.path" :value="point.id" /></el-select></el-form-item><el-form-item label="题型（可选）"><el-checkbox-group v-model="form.questionTypes"><el-checkbox v-for="type in questionTypes" :key="type.value" :value="type.value">{{ type.label }}</el-checkbox></el-checkbox-group></el-form-item><el-form-item label="难度（可选）"><el-radio-group v-model="form.difficulty"><el-radio :value="undefined">不限</el-radio><el-radio :value="1">简单</el-radio><el-radio :value="2">中等</el-radio><el-radio :value="3">困难</el-radio></el-radio-group></el-form-item><el-form-item label="题目数量" prop="count"><el-input-number v-model="form.count" :min="1" :max="50" /></el-form-item><el-button type="primary" :loading="loading" @click="submit">创建并开始作答</el-button></el-form></section></template>
