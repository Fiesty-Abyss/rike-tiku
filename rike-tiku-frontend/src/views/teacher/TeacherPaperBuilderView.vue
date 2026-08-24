<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from "vue";
import { useRouter } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import { fetchTeachingScopes, type TeachingScope } from "../../api/teacher";
import {
  createPaper,
  createRandomPaper,
  createRulePaper,
  fetchPaperKnowledgePoints,
  fetchPaperQuality,
  fetchPaperQuestions,
  fetchPapers,
  publishPaper,
  fetchPaperReleases, fetchPaperStats, fetchPaperSubmissions, fetchTeacherSubmission, cancelPaperRelease, deletePaper,
  fetchTeacherPaperReleases,
  requestAiPaperQuality,
  type AiPaperQualityAssessment,
  type PaperQualityAssessment,
  type PaperQuestionOption,
} from "../../api/teacher/papers";
import ScientificText from "../../components/question/ScientificText.vue";
import QuestionContent from "../../components/question/QuestionContent.vue";
import AnswerDisplay from "../../components/question/AnswerDisplay.vue";
import StandardAnalysis from "../../components/question/StandardAnalysis.vue";
import { questionTypeLabel, topicTypeLabel } from "../../utils/questionLabels";

const router = useRouter();
const scopes = ref<TeachingScope[]>([]),
  papers = ref<any[]>([]),
  questions = ref<PaperQuestionOption[]>([]),
  points = ref<Array<{ id: number; path: string }>>([]),
  selected = ref<(PaperQuestionOption & { score: number })[]>([]);
const mode = ref<"MANUAL" | "RANDOM" | "RULE">("MANUAL");
const quality = ref<PaperQualityAssessment>(),
  aiQuality = ref<AiPaperQualityAssessment>(),
  aiQualityLoading = ref(false),
  publishVisible = ref(false),
  releaseVisible = ref(false), statsVisible = ref(false), answerVisible = ref(false), releaseHistoryVisible = ref(false),
  publishPaperId = ref(0);
const releases = ref<any[]>([]), releaseStats = ref<any>(), selectedReleaseId = ref(0), submissions = ref<any[]>([]), selectedSubmission = ref<any>();
const releaseRefreshing = ref(false), statsRefreshing = ref(false);
const releaseHistory = ref<any[]>([]), releaseHistoryTotal = ref(0);
let statsRefreshTimer:number|undefined;
const releaseFilters = reactive({ teachingScopeId: undefined as number | undefined, status: "", keyword: "", page: 1, size: 20 });
const publishForm = reactive({
  teachingScopeId: undefined as number | undefined,
  deadline: "",
});
const form = reactive({
  teachingScopeId: undefined as number | undefined,
  name: "",
  knowledgePointId: undefined as number | undefined,
  questionType: "",
  difficulty: undefined as number | undefined,
  keyword: "",
  knowledgePointIds: [] as number[],
  questionTypes: ["SINGLE_CHOICE"],
  difficulties: [1, 2, 3],
  count: 10,
  totalScore: 100,
});
const selectedScope = computed(() => scopes.value.find(scope => scope.teachingAssignmentId === form.teachingScopeId));
const selectedSubjectId = computed(() => selectedScope.value?.subjectId);
const total = computed(() =>
  selected.value.reduce((sum, item) => sum + Number(item.score || 0), 0),
);
const modeLabel = computed(
  () => ({ MANUAL: "手动", RANDOM: "随机", RULE: "规则" })[mode.value],
);
async function load() {
  scopes.value = (await fetchTeachingScopes()).filter(
    (item) => item.teachingStatus === "ACTIVE",
  );
  papers.value = await fetchPapers();
}
async function loadPoints() {
  points.value = selectedSubjectId.value
    ? await fetchPaperKnowledgePoints(selectedSubjectId.value)
    : [];
}
async function search() {
  if (!selectedSubjectId.value) return;
  questions.value = await fetchPaperQuestions({
    subjectId: selectedSubjectId.value,
    teachingScopeId: form.teachingScopeId,
    knowledgePointId: form.knowledgePointId,
    questionType: form.questionType || undefined,
    difficulty: form.difficulty,
    keyword: form.keyword || undefined,
  });
}
function add(question: PaperQuestionOption) {
  if (!selected.value.some((item) => item.id === question.id))
    selected.value.push({ ...question, score: 5 });
}
function remove(index: number) {
  selected.value.splice(index, 1);
}
function move(index: number, delta: number) {
  const target = index + delta;
  if (target < 0 || target >= selected.value.length) return;
  const [item] = selected.value.splice(index, 1);
  selected.value.splice(target, 0, item);
}
function ruleRequest() {
  return {
    subjectId: selectedSubjectId.value,
    name: form.name,
    knowledgePointIds: mode.value === "RANDOM" ? [] : form.knowledgePointIds,
    questionTypes: form.questionTypes,
    difficulties: form.difficulties,
    count: form.count,
    totalScore: form.totalScore,
  };
}
async function save() {
  try {
    if (mode.value === "MANUAL")
      await createPaper({
        subjectId: selectedSubjectId.value,
        name: form.name,
        mode: "MANUAL",
        items: selected.value.map((item) => ({
          questionId: item.id,
          score: item.score,
        })),
      });
    else if (mode.value === "RANDOM") await createRandomPaper(ruleRequest());
    else await createRulePaper(ruleRequest());
    ElMessage.success(`${modeLabel.value}试卷已保存。`);
    selected.value = [];
    await load();
  } catch (error: any) {
    ElMessage.warning(error.message || "组卷失败");
  }
}
async function openPublish(row: any) {
  publishPaperId.value = row.id;
  publishForm.teachingScopeId = scopes.value.find(
    (scope) => scope.subjectId === row.subjectId,
  )?.teachingAssignmentId;
  publishForm.deadline = "";
  aiQuality.value = undefined;
  quality.value = await fetchPaperQuality(row.id);
  publishVisible.value = true;
}
async function assessWithAi() {
  aiQualityLoading.value = true;
  try {
    aiQuality.value = await requestAiPaperQuality(publishPaperId.value);
  } catch (error: any) {
    ElMessage.warning(
      error.message || "AI 质量建议暂不可用，请稍后重试。",
    );
  } finally {
    aiQualityLoading.value = false;
  }
}
async function publish() {
  if (!publishForm.teachingScopeId || !publishForm.deadline) return;
  try {
    await publishPaper(publishPaperId.value, {
      teachingScopeId: publishForm.teachingScopeId,
      deadline: new Date(publishForm.deadline).toISOString().slice(0, 19),
    });
    ElMessage.success("试卷已发布到班级。");
    publishVisible.value = false;
    await load();
  } catch (error: any) {
    ElMessage.error(error.message || "发布失败");
  }
}
const releaseStatus=(s:string)=>s==='CANCELLED'?'已撤回':s==='CLOSED'||s==='EXPIRED'?'已截止':'进行中';
const submissionStatus=(s:string)=>({NOT_STARTED:'未开始',IN_PROGRESS:'作答中',SUBMITTED:'已提交'} as any)[s]||s;
const formatDateTime=(value?:string)=>value?value.replace('T',' ').replace(/\.\d+$/,''):'-';
async function refreshReleases(){if(!publishPaperId.value||releaseRefreshing.value)return;releaseRefreshing.value=true;try{releases.value=await fetchPaperReleases(publishPaperId.value)}catch(error:any){ElMessage.error(error.message||'发布记录刷新失败')}finally{releaseRefreshing.value=false}}
async function openReleases(row:any){publishPaperId.value=row.id;releaseVisible.value=true;await refreshReleases()}
async function loadReleaseHistory(page=releaseFilters.page){releaseFilters.page=page;const result=await fetchTeacherPaperReleases({teachingScopeId:releaseFilters.teachingScopeId,status:releaseFilters.status||undefined,keyword:releaseFilters.keyword||undefined,page,size:releaseFilters.size});releaseHistory.value=result.items;releaseHistoryTotal.value=result.total;}
async function openReleaseHistory(){releaseFilters.page=1;await loadReleaseHistory();releaseHistoryVisible.value=true;}
async function refreshStats(silent=false){if(!selectedReleaseId.value||statsRefreshing.value)return;statsRefreshing.value=true;try{const [nextStats,nextSubmissions]=await Promise.all([fetchPaperStats(selectedReleaseId.value),fetchPaperSubmissions(selectedReleaseId.value)]);releaseStats.value=nextStats;submissions.value=nextSubmissions}catch(error:any){if(!silent)ElMessage.error(error.message||'作答情况刷新失败')}finally{statsRefreshing.value=false}}
function stopStatsPolling(){window.clearInterval(statsRefreshTimer);statsRefreshTimer=undefined}
function startStatsPolling(){stopStatsPolling();statsRefreshTimer=window.setInterval(()=>void refreshStats(true),5000)}
async function openStats(row:any){selectedReleaseId.value=row.id ?? row.releaseId;selectedSubmission.value=null;statsVisible.value=true;await refreshStats();startStatsPolling()}
async function openSubmission(row:any){if(row.status!=='SUBMITTED')return;selectedSubmission.value=await fetchTeacherSubmission(selectedReleaseId.value,row.studentId);answerVisible.value=true;}
async function deleteSavedPaper(row:any){try{await ElMessageBox.confirm(`确认从试卷库删除“${row.name}”？\n\n删除后该试卷不再出现在“已保存试卷”中。\n\n已撤回的发布和学生作答仍会保留。`,'确认删除试卷',{confirmButtonText:'确认删除',cancelButtonText:'取消',type:'warning'});await deletePaper(row.id);await load();ElMessage.success('试卷已从试卷库删除，历史发布和作答仍已保留。')}catch(error:any){if(error!=='cancel')ElMessage.error(error.message||'删除失败')}}
async function cancelRelease(row:any){try{await ElMessageBox.confirm('撤回后，该班学生将不再看到这份试卷；已经产生的作答和统计记录仍保留供教师查看。','确认撤回这次班级发布？',{confirmButtonText:'确认撤回',cancelButtonText:'取消',type:'warning'});await cancelPaperRelease(row.id ?? row.releaseId);const paperId=row.paperId ?? publishPaperId.value;const current=await fetchPaperReleases(paperId);releases.value=current;if(releaseHistoryVisible.value)await loadReleaseHistory();const hasActive=current.some((item:any)=>item.status==='PUBLISHED'||item.status==='CLOSED'||item.status==='EXPIRED');if(hasActive){ElMessage.success('已撤回该班发布。该试卷仍发布在其他班级，因此继续保留在试卷库中。');return;}try{await ElMessageBox.confirm('发布已撤回。\n\n是否同时从“已保存试卷”中删除这张试卷？\n\n删除只会清理教师试卷库，历史发布、学生作答和统计数据仍会保留。','发布已撤回',{confirmButtonText:'删除试卷',cancelButtonText:'仅撤回',type:'warning'});await deletePaper(paperId);await load();ElMessage.success('已撤回发布并从试卷库删除，历史记录仍已保留。')}catch(choice:any){if(choice==='cancel')ElMessage.success('已撤回发布，试卷仍保留在试卷库中。');else throw choice}}catch(error:any){if(error!=='cancel')ElMessage.error(error.message||'撤回失败')}}
function handlePaperCommand(command:string,row:any){if(command==='releases')void openReleases(row);else if(command==='delete')void deleteSavedPaper(row)}
watch(
  () => form.teachingScopeId,
  () => {
    questions.value = [];
    selected.value = [];
    form.knowledgePointId = undefined;
    form.knowledgePointIds = [];
    void loadPoints();
  },
);
onMounted(load);
watch(statsVisible,visible=>{if(visible)startStatsPolling();else stopStatsPolling()});
onBeforeUnmount(stopStatsPolling);
</script>

<template>
  <main class="paper-builder">
    <div class="heading">
      <div>
        <h1>组卷与打印</h1>
      </div>
      <el-segmented
        v-model="mode"
        :options="[
          { label: '手动组卷', value: 'MANUAL' },
          { label: '随机组卷', value: 'RANDOM' },
          { label: '规则组卷', value: 'RULE' },
        ]"
      />
    </div>
    <el-form label-position="top" class="builder-form"
      ><div class="base-fields">
        <el-form-item label="任教学科"
          ><el-select v-model="form.teachingScopeId" placeholder="选择任课范围"
            ><el-option
              v-for="scope in scopes"
              :key="scope.teachingAssignmentId"
              :label="`${scope.subjectName}（${scope.className}）`"
              :value="scope.teachingAssignmentId" /></el-select></el-form-item
        ><el-form-item label="试卷名称"
          ><el-input v-model="form.name" maxlength="120"
        /></el-form-item>
      </div>
      <template v-if="mode === 'MANUAL'"
        ><section class="filters">
          <el-input v-model="form.keyword" placeholder="题干关键词" /><el-select
            v-model="form.knowledgePointId"
            clearable
            filterable
            placeholder="完整知识路径"
            ><el-option
              v-for="point in points"
              :key="point.id"
              :label="point.path"
              :value="point.id" /></el-select
          ><el-select v-model="form.questionType" clearable placeholder="题型"
            ><el-option label="单选" value="SINGLE_CHOICE" /><el-option
              label="多选"
              value="MULTIPLE_CHOICE" /><el-option
              label="填空"
              value="FILL_BLANK" /><el-option label="主观大题" value="SUBJECTIVE" /></el-select
          ><el-input-number
            v-model="form.difficulty"
            :min="1"
            :max="5"
            placeholder="难度"
          /><el-button :disabled="!selectedSubjectId" @click="search"
            >检索已发布题目</el-button
          >
        </section>
        <div class="manual-grid">
          <section>
            <h2>题库结果</h2>
            <article
              v-for="question in questions"
              :key="question.id"
              class="question-card"
            >
              <div>
                <el-tag>{{ questionTypeLabel(question.type) }}</el-tag
                ><el-tag v-if="question.topicType" type="warning" effect="plain">{{ topicTypeLabel(question.topicType) }}</el-tag
                ><span>难度 {{ question.difficulty }}</span>
              </div>
              <QuestionContent :content="question.stem" :attachments="question.stemAttachments" position="QUESTION" /><small>{{
                question.knowledgePoints.join(" · ")
              }}</small
              ><el-button @click="add(question)">加入试卷</el-button>
            </article>
            <el-empty
              v-if="!questions.length"
              description="请选择学科并检索题目"
            />
          </section>
          <section class="basket">
            <h2>题篮 · {{ selected.length }} 题 · {{ total }} 分</h2>
            <article v-for="(question, index) in selected" :key="question.id">
              <strong>{{ index + 1 }}.</strong
              ><div><el-tag size="small" effect="plain">{{ questionTypeLabel(question.type) }}</el-tag><ScientificText :content="question.stem" /></div><el-input-number
                v-model="question.score"
                :min="0.5"
                :step="0.5"
              />
              <div>
                <el-button :disabled="index === 0" @click="move(index, -1)"
                  >上移</el-button
                ><el-button
                  :disabled="index === selected.length - 1"
                  @click="move(index, 1)"
                  >下移</el-button
                ><el-button type="danger" plain @click="remove(index)"
                  >移除</el-button
                >
              </div>
            </article>
            <el-empty v-if="!selected.length" description="从左侧加入题目" />
          </section></div
      ></template>
      <template v-else
        ><div class="rule-fields">
          <el-form-item v-if="mode === 'RULE'" label="知识点"
            ><el-select
              v-model="form.knowledgePointIds"
              multiple
              filterable
              placeholder="完整知识路径"
              ><el-option
                v-for="point in points"
                :key="point.id"
                :label="point.path"
                :value="point.id" /></el-select></el-form-item
          ><el-form-item label="题型"
            ><el-checkbox-group v-model="form.questionTypes"
              ><el-checkbox value="SINGLE_CHOICE">单选</el-checkbox
              ><el-checkbox value="MULTIPLE_CHOICE">多选</el-checkbox
              ><el-checkbox value="FILL_BLANK"
                >填空</el-checkbox
              ></el-checkbox-group
            ></el-form-item
          ><el-form-item label="难度"
            ><el-checkbox-group v-model="form.difficulties"
              ><el-checkbox v-for="level in 5" :key="level" :value="level">{{
                level
              }}</el-checkbox></el-checkbox-group
            ></el-form-item
          ><el-form-item label="数量"
            ><el-input-number
              v-model="form.count"
              :min="1"
              :max="100" /></el-form-item
          ><el-form-item label="目标总分"
            ><el-input-number v-model="form.totalScore" :min="1"
          /></el-form-item>
        </div>
        <el-alert
          v-if="mode !== 'MANUAL'"
          title="随机与规则组卷仅包含客观题；主观大题请使用手动组卷。"
          type="info"
          :closable="false"
      /></template>
      <el-button
        type="primary"
        :disabled="
          !selectedSubjectId ||
          !form.name.trim() ||
          (mode === 'MANUAL' && !selected.length)
        "
        @click="save"
        >保存试卷</el-button
      ></el-form
    >
    <div class="paper-library-heading"><h2>我的试卷</h2><el-button plain @click="openReleaseHistory">班级发布记录</el-button></div>
    <el-table :data="papers"
      ><el-table-column
        prop="name"
        label="名称"
        min-width="180"
      /><el-table-column prop="subjectName" label="学科" /><el-table-column
        prop="questionCount"
        label="题数"
      /><el-table-column prop="totalScore" label="总分" /><el-table-column
        prop="status"
        label="状态"
      /><el-table-column prop="mode" label="组卷方式" /><el-table-column
        label="操作"
        min-width="300"
        ><template #default="{ row }"
          ><div class="paper-actions"><el-button @click="router.push(`/teacher/papers/${row.id}/student`)"
            >学生版打印</el-button
          ><el-button @click="router.push(`/teacher/papers/${row.id}/answer`)"
            >答案解析版打印</el-button
          ><el-button type="primary" plain @click="openPublish(row)"
            >发布到班级</el-button
          ><el-dropdown trigger="click" @command="(command:string)=>handlePaperCommand(command,row)"><el-button plain>更多操作 ▾</el-button><template #dropdown><el-dropdown-menu><el-dropdown-item command="releases">发布管理</el-dropdown-item><el-dropdown-item command="delete" divided class="danger-dropdown-item">删除试卷</el-dropdown-item></el-dropdown-menu></template></el-dropdown></div></template
        ></el-table-column
      ></el-table
    >
    <el-dialog
      v-model="publishVisible"
      title="发布试卷"
      width="min(560px, calc(100vw - 24px))"
      ><el-alert
        title="发布后试卷内容不能修改。"
        type="info"
        :closable="false"
      /><el-form label-position="top"
        ><el-form-item label="发布班级"
          ><el-select v-model="publishForm.teachingScopeId"
            ><el-option
              v-for="scope in scopes.filter(
                (item) =>
                  !papers.find((paper) => paper.id === publishPaperId) ||
                  item.subjectId ===
                    papers.find((paper) => paper.id === publishPaperId)
                      ?.subjectId,
              )"
              :key="scope.teachingAssignmentId"
              :label="`${scope.className} · ${scope.subjectName}`"
              :value="scope.teachingAssignmentId" /></el-select></el-form-item
        ><el-form-item label="截止时间"
          ><el-date-picker
            v-model="publishForm.deadline"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss" /></el-form-item
      ></el-form>
      <section v-if="quality" class="quality">
        <strong>{{ quality.notice }}</strong>
        <p v-for="item in quality.coverage" :key="item">{{ item }}</p>
        <el-alert
          v-for="risk in quality.risks"
          :key="risk"
          :title="risk"
          type="warning"
          :closable="false"
        /><el-button :loading="aiQualityLoading" @click="assessWithAi"
          >AI 质量建议</el-button
        >
        <p v-if="aiQuality">
          <b>{{ aiQuality.notice }}</b
          ><br />{{ aiQuality.content }}
        </p>
      </section>
      <template #footer
        ><el-button @click="publishVisible = false">取消</el-button
        ><el-button
          type="primary"
          :disabled="!publishForm.teachingScopeId || !publishForm.deadline"
          @click="publish"
          >确认发布</el-button
        ></template
      ></el-dialog
    >
    <el-dialog v-model="releaseVisible" width="min(860px, calc(100vw - 24px))"><template #header><div class="dialog-heading"><strong>发布管理</strong><el-button :loading="releaseRefreshing" @click="refreshReleases">刷新</el-button></div></template><el-table :data="releases"><el-table-column prop="className" label="班级"/><el-table-column label="发布时间" min-width="170"><template #default="{row}">{{formatDateTime(row.publishedAt)}}</template></el-table-column><el-table-column label="截止时间" min-width="170"><template #default="{row}">{{formatDateTime(row.deadline)}}</template></el-table-column><el-table-column label="状态"><template #default="{row}"><el-tag>{{releaseStatus(row.status)}}</el-tag></template></el-table-column><el-table-column label="操作" min-width="190"><template #default="{row}"><el-button @click="openStats(row)">作答情况</el-button><el-button v-if="row.status!=='CANCELLED'" type="danger" plain @click="cancelRelease(row)">撤回发布</el-button></template></el-table-column></el-table></el-dialog>
    <el-dialog v-model="releaseHistoryVisible" title="班级发布记录" width="min(1040px, calc(100vw - 24px))"><section class="release-filters"><el-select v-model="releaseFilters.teachingScopeId" clearable placeholder="全部任课范围"><el-option v-for="scope in scopes" :key="scope.teachingAssignmentId" :label="`${scope.subjectName}（${scope.className}）`" :value="scope.teachingAssignmentId"/></el-select><el-select v-model="releaseFilters.status" placeholder="有效发布"><el-option label="有效发布" value=""/><el-option label="进行中" value="PUBLISHED"/><el-option label="已截止" value="CLOSED"/><el-option label="已撤回" value="CANCELLED"/></el-select><el-input v-model="releaseFilters.keyword" clearable placeholder="搜索试卷名称" @keyup.enter="loadReleaseHistory(1)"/><el-button type="primary" @click="loadReleaseHistory(1)">查询</el-button></section><el-table :data="releaseHistory"><el-table-column prop="paperName" label="试卷名称" min-width="180"/><el-table-column label="任课范围" min-width="150"><template #default="{row}">{{row.subjectName}}（{{row.className}}）</template></el-table-column><el-table-column label="发布时间" min-width="170"><template #default="{row}">{{formatDateTime(row.publishedAt)}}</template></el-table-column><el-table-column label="截止时间" min-width="170"><template #default="{row}">{{formatDateTime(row.deadline)}}</template></el-table-column><el-table-column label="状态"><template #default="{row}"><el-tag>{{releaseStatus(row.status)}}</el-tag></template></el-table-column><el-table-column label="操作" min-width="190"><template #default="{row}"><el-button @click="openStats(row)">{{row.status==='CANCELLED'?'查看历史':'作答情况'}}</el-button><el-button v-if="row.status!=='CANCELLED'" type="danger" plain @click="cancelRelease(row)">撤回发布</el-button></template></el-table-column></el-table><el-pagination class="release-pagination" layout="total, prev, pager, next" :current-page="releaseFilters.page" :page-size="releaseFilters.size" :total="releaseHistoryTotal" @current-change="loadReleaseHistory"/></el-dialog>
    <el-dialog v-model="statsVisible" width="min(980px, calc(100vw - 24px))"><template #header><div class="dialog-heading"><strong>班级作答情况</strong><el-button :loading="statsRefreshing" @click="refreshStats(false)">{{statsRefreshing?'刷新中…':'刷新作答情况'}}</el-button></div></template><el-descriptions v-if="releaseStats" :column="4" border><el-descriptions-item label="应交">{{releaseStats.assigned}}</el-descriptions-item><el-descriptions-item label="已提交">{{releaseStats.submitted}}</el-descriptions-item><el-descriptions-item label="未提交">{{releaseStats.unsubmitted}}</el-descriptions-item><el-descriptions-item label="客观题平均分">{{releaseStats.averageScore}}</el-descriptions-item></el-descriptions><h3>学生作答</h3><el-table :data="submissions"><el-table-column prop="studentNumber" label="学号"/><el-table-column prop="studentName" label="姓名"/><el-table-column label="状态"><template #default="{row}"><el-tag>{{submissionStatus(row.status)}}</el-tag></template></el-table-column><el-table-column label="客观得分"><template #default="{row}">{{row.objectiveScore==null?'-':`${row.objectiveScore} / ${row.objectiveTotal}`}}</template></el-table-column><el-table-column label="主观题"><template #default="{row}">{{row.subjectivePendingCount?`${row.subjectivePendingCount}题待人工处理`:''}}</template></el-table-column><el-table-column label="提交时间" min-width="170"><template #default="{row}">{{formatDateTime(row.submittedAt)}}</template></el-table-column><el-table-column label="操作"><template #default="{row}"><el-button v-if="row.status==='SUBMITTED'" @click="openSubmission(row)">查看答卷</el-button></template></el-table-column></el-table></el-dialog>
    <el-dialog v-model="answerVisible" title="学生已提交答卷" width="min(900px, calc(100vw - 24px))"><article v-for="q in selectedSubmission?.questions" :key="q.itemId" class="submission-question"><b>第{{q.order}}题 · {{questionTypeLabel(q.type)}} · {{q.score}}分</b><QuestionContent :content="q.stem" :attachments="q.stemAttachments" position="QUESTION"/><p v-for="o in q.options" :key="o.label"><b>{{o.label}}.</b> <ScientificText :content="o.content"/></p><h4>学生答案</h4><AnswerDisplay :question-type="q.type" :value="q.submittedAnswer" :options="q.options"/><template v-if="q.type!=='SUBJECTIVE'"><h4>正确答案</h4><AnswerDisplay :question-type="q.type" :value="q.correctAnswer" :options="q.options"/><p>客观得分：{{q.awardedScore}} / {{q.score}}</p></template><p v-else>状态：待人工处理</p><section class="submission-standard"><h4>STANDARD 标准解析</h4><StandardAnalysis :content="q.standardAnalysis" :attachments="q.analysisAttachments"/></section></article></el-dialog>
  </main>
</template>

<style scoped>
.paper-builder {
  max-width: 1200px;
  margin: 0 auto;
  padding: 28px;
}
.heading {
  display: flex;
  justify-content: space-between;
  gap: 24px;
  align-items: end;
}
.heading p {
  letter-spacing: 0.12em;
  color: #547a87;
}
.heading h1 {
  margin: 4px 0;
}
.builder-form {
  margin: 24px 0;
  padding: 22px;
  border: 1px solid var(--el-border-color);
  border-radius: 18px;
}
.base-fields,
.rule-fields {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}
.filters {
  display: grid;
  grid-template-columns: 2fr 2fr 1fr 1fr auto;
  gap: 10px;
  margin: 10px 0 18px;
}
.manual-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 18px;
}
.manual-grid > section {
  min-width: 0;
  padding: 16px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 14px;
}
.question-card,
.basket article {
  display: grid;
  gap: 8px;
  padding: 14px 0;
  border-bottom: 1px solid var(--el-border-color-lighter);
}
.question-card > div {
  display: flex;
  gap: 10px;
  align-items: center;
}
.question-card small {
  color: var(--el-text-color-secondary);
}
.basket article {
  grid-template-columns: auto 1fr auto;
}
.basket article > div {
  grid-column: 2/4;
}
.paper-actions { display:grid; grid-template-columns:repeat(2,minmax(110px,1fr)); gap:8px; }
.paper-actions .el-button { width:100%; margin:0; }
.paper-actions :deep(.el-dropdown) { width:100%; }
.dialog-heading { display:flex; align-items:center; justify-content:space-between; gap:16px; padding-right:28px; }
:deep(.danger-dropdown-item) { color:var(--el-color-danger); }
.paper-library-heading { display:flex; align-items:center; justify-content:space-between; gap:16px; }
.release-filters { display:grid; grid-template-columns:1.2fr 1fr 1.4fr auto; gap:10px; margin-bottom:16px; }
.release-pagination { margin-top:16px; justify-content:flex-end; }
.question-card { overflow:hidden; }
.question-card :deep(.katex-display) { overflow-x:auto; overflow-y:hidden; }
.submission-question { display:block; padding:20px 0; border-bottom:1px solid var(--el-border-color-lighter); overflow:hidden; }
.submission-standard { display:block; margin-top:14px; padding:14px 16px; border-left:3px solid var(--el-color-primary); background:var(--el-fill-color-lighter); }
.submission-standard :deep(.standard-analysis),.submission-standard :deep(.standard-analysis__block) { display:block; width:100%; margin:0 0 12px; }
.submission-question :deep(.katex-display) { overflow-x:auto; }
@media (max-width: 800px) {
  .heading {
    align-items: stretch;
    flex-direction: column;
  }
  .base-fields,
  .rule-fields,
  .filters,
  .release-filters,
  .manual-grid {
    grid-template-columns: 1fr;
  }
  .basket article {
    grid-template-columns: auto 1fr;
  }
  .basket article > div {
    grid-column: 1/3;
  }
}
</style>
