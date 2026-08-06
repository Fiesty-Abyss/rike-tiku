<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { fetchPracticeResult, type PracticeResult } from '../../api/student/practice'
import { formatPracticeAnswer } from './practiceAnswerFormatter'
import type { ApiError } from '../../api/http'
const route=useRoute(); const router=useRouter(); const result=ref<PracticeResult|null>(null); const loading=ref(true)
onMounted(async()=>{try{result.value=await fetchPracticeResult(Number(route.params.id))}catch(error){const api=error as ApiError;ElMessage.error(api.message||'练习结果加载失败。');await router.replace('/student/practice')}finally{loading.value=false}})
</script>
<template><section class="student-page" v-loading="loading"><template v-if="result"><div class="student-page-heading"><div><h1>练习结果</h1><p>共 {{ result.totalCount }} 题，答对 {{ result.correctCount }} 题，得分 {{ result.totalScore }} 分。</p></div><el-button type="primary" @click="router.push('/student/practice/new')">再练一场</el-button></div><article v-for="item in result.questions" :key="item.question.practiceQuestionId" class="result-question" :class="item.correct?'is-correct':'is-wrong'"><div class="result-state"><el-tag :type="item.correct?'success':'danger'">{{ item.correct?'回答正确':'需要复习' }}</el-tag><span>{{ item.question.order }}. {{ item.question.stem }}</span></div><p>你的答案：{{ formatPracticeAnswer(item.question.questionType,item.studentAnswer) }}</p><p>正确答案：{{ formatPracticeAnswer(item.question.questionType,item.correctAnswer) }}</p><p>标准解析：{{ item.standardAnalysis }}</p><p class="knowledge-copy">知识点：{{ item.question.knowledgePoints.map(point=>point.path).join('；') }}</p></article></template></section></template>
