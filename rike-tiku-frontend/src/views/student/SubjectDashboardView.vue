<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { createPracticeSession, fetchPracticeOptions, type Subject } from '../../api/student/practice'
import type { ApiError } from '../../api/http'
const route=useRoute();const router=useRouter();const subject=ref<Subject|null>(null);const pointCount=ref(0);const loading=ref(false)
const subjectCode=computed(()=>String(route.params.subjectCode).toUpperCase()); const valid=computed(()=>['PHYSICS','CHEMISTRY','BIOLOGY'].includes(subjectCode.value))
async function load(){if(!valid.value){await router.replace('/student');return}try{const all=await fetchPracticeOptions();subject.value=all.subjects.find(item=>item.code===subjectCode.value)||null;if(!subject.value)return;pointCount.value=(await fetchPracticeOptions(subject.value.id)).knowledgePoints.length}catch(error){ElMessage.error((error as ApiError).message||'学科信息加载失败。')}}
async function randomPractice(){if(!subject.value)return;loading.value=true;try{const session=await createPracticeSession({subjectId:subject.value.id,count:5});ElMessage.success('已创建本学科随机练习。');await router.push(`/student/practice/${session.id}`)}catch(error){const api=error as ApiError;ElMessage.error(api.code==='PRACTICE_QUESTION_INSUFFICIENT'?'本学科符合条件的题目不足5道，请调整题数或筛选条件。':api.message||'创建练习失败。')}finally{loading.value=false}}
onMounted(()=>void load())
</script>
<template><section v-if="subject" class="student-page subject-page"><div class="student-page-heading"><div><h1>{{ subject.name }}学习工作台</h1><p>围绕本学科建立练习与错题复习入口，数据来自已发布的可自动判分题目。</p></div><el-button @click="router.push('/student')">返回三科主页</el-button></div><div class="subject-action-grid"><article><h2>随机练习</h2><p>默认抽取本学科5题，题目在创建后立即冻结。</p><el-button type="primary" :loading="loading" @click="randomPractice">开始5题随机练习</el-button></article><article><h2>定向练习</h2><p>按知识点、题型、难度和数量组合练习计划。</p><el-button @click="router.push({path:'/student/practice/new',query:{subjectId:subject.id}})">设置练习条件</el-button></article><article><h2>本科错题</h2><p>当前可用知识点 {{ pointCount }} 个；错题状态和历史会保留。</p><el-button @click="router.push({path:'/student/wrong-questions',query:{subjectId:subject.id}})">查看{{ subject.name }}错题</el-button></article></div></section></template>
