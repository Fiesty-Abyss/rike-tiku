<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from "vue";
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
  fetchPaperReleases, fetchPaperStats, fetchPaperSubmissions, fetchTeacherSubmission, cancelPaperRelease,
  requestAiPaperQuality,
  type AiPaperQualityAssessment,
  type PaperQualityAssessment,
  type PaperQuestionOption,
} from "../../api/teacher/papers";
import ScientificText from "../../components/question/ScientificText.vue";
import QuestionContent from "../../components/question/QuestionContent.vue";
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
  releaseVisible = ref(false), statsVisible = ref(false), answerVisible = ref(false),
  publishPaperId = ref(0);
const releases = ref<any[]>([]), releaseStats = ref<any>(), selectedReleaseId = ref(0), submissions = ref<any[]>([]), selectedSubmission = ref<any>();
const publishForm = reactive({
  teachingScopeId: undefined as number | undefined,
  deadline: "",
});
const form = reactive({
  subjectId: undefined as number | undefined,
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
  points.value = form.subjectId
    ? await fetchPaperKnowledgePoints(form.subjectId)
    : [];
}
async function search() {
  if (!form.subjectId) return;
  questions.value = await fetchPaperQuestions({
    subjectId: form.subjectId,
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
    subjectId: form.subjectId,
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
        subjectId: form.subjectId,
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
      error.message || "当前 AI Provider 不可用，确定性质量建议仍可使用。",
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
    ElMessage.success("试卷已发布，题干、选项、分值和 STANDARD 已冻结。");
    publishVisible.value = false;
    await load();
  } catch (error: any) {
    ElMessage.error(error.message || "发布失败");
  }
}
const releaseStatus=(s:string)=>s==='CANCELLED'?'已撤回':s==='CLOSED'||s==='EXPIRED'?'已截止':'进行中';
const submissionStatus=(s:string)=>({NOT_STARTED:'未开始',IN_PROGRESS:'作答中',SUBMITTED:'已提交'} as any)[s]||s;
async function openReleases(row:any){publishPaperId.value=row.id;releases.value=await fetchPaperReleases(row.id);releaseVisible.value=true;}
async function openStats(row:any){selectedReleaseId.value=row.id;releaseStats.value=await fetchPaperStats(row.id);submissions.value=await fetchPaperSubmissions(row.id);selectedSubmission.value=null;statsVisible.value=true;}
async function openSubmission(row:any){if(row.status!=='SUBMITTED')return;selectedSubmission.value=await fetchTeacherSubmission(selectedReleaseId.value,row.studentId);answerVisible.value=true;}
async function cancelRelease(row:any){try{await ElMessageBox.confirm('撤回后，该班学生将不再看到这份试卷；已经产生的作答和统计记录仍保留供教师查看。','确认撤回这次班级发布？',{confirmButtonText:'确认撤回',cancelButtonText:'取消',type:'warning'});await cancelPaperRelease(row.id);await openReleases({id:publishPaperId.value});ElMessage.success('已撤回发布，历史记录已保留。')}catch(error:any){if(error!=='cancel')ElMessage.error(error.message||'撤回失败')}}
watch(
  () => form.subjectId,
  () => {
    questions.value = [];
    selected.value = [];
    form.knowledgePointId = undefined;
    form.knowledgePointIds = [];
    void loadPoints();
  },
);
onMounted(load);
</script>

<template>
  <main class="paper-builder">
    <div class="heading">
      <div>
        <p>TEACHER PAPER STUDIO</p>
        <h1>组卷与打印</h1>
        <span>仅使用本人任教学科内已发布题目；AI 建议不修改 STANDARD。</span>
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
          ><el-select v-model="form.subjectId"
            ><el-option
              v-for="scope in scopes"
              :key="scope.subjectId"
              :label="scope.subjectName"
              :value="scope.subjectId" /></el-select></el-form-item
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
          /><el-button :disabled="!form.subjectId" @click="search"
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
          title="随机与规则组卷默认只抽取可确定性判分的客观题；主观大题请在手动组卷中选择。"
          type="info"
          :closable="false"
      /></template>
      <el-button
        type="primary"
        :disabled="
          !form.subjectId ||
          !form.name.trim() ||
          (mode === 'MANUAL' && !selected.length)
        "
        @click="save"
        >保存试卷</el-button
      ></el-form
    >
    <h2>已保存试卷</h2>
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
          ><el-button @click="router.push(`/teacher/papers/${row.id}/student`)"
            >学生版打印</el-button
          ><el-button @click="router.push(`/teacher/papers/${row.id}/answer`)"
            >答案解析版打印</el-button
          ><el-button type="primary" plain @click="openPublish(row)"
            >发布到班级</el-button
          ><el-button plain @click="openReleases(row)">发布管理</el-button
          ></template
        ></el-table-column
      ></el-table
    >
    <el-dialog
      v-model="publishVisible"
      title="发布冻结试卷"
      width="min(560px, calc(100vw - 24px))"
      ><el-alert
        title="发布后题干、选项、分值和 STANDARD 全部冻结。"
        type="info"
        :closable="false"
      /><el-form label-position="top"
        ><el-form-item label="本人 ACTIVE 任课班级"
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
    <el-dialog v-model="releaseVisible" title="发布管理" width="min(860px, calc(100vw - 24px))"><el-table :data="releases"><el-table-column prop="className" label="班级"/><el-table-column prop="publishedAt" label="发布时间"/><el-table-column prop="deadline" label="截止时间"/><el-table-column label="状态"><template #default="{row}"><el-tag>{{releaseStatus(row.status)}}</el-tag></template></el-table-column><el-table-column label="操作" min-width="190"><template #default="{row}"><el-button @click="openStats(row)">作答情况</el-button><el-button v-if="row.status!=='CANCELLED'" type="danger" plain @click="cancelRelease(row)">撤回发布</el-button></template></el-table-column></el-table></el-dialog>
    <el-dialog v-model="statsVisible" title="班级作答情况" width="min(980px, calc(100vw - 24px))"><el-descriptions v-if="releaseStats" :column="4" border><el-descriptions-item label="应交">{{releaseStats.assigned}}</el-descriptions-item><el-descriptions-item label="已提交">{{releaseStats.submitted}}</el-descriptions-item><el-descriptions-item label="未提交">{{releaseStats.unsubmitted}}</el-descriptions-item><el-descriptions-item label="客观题平均分">{{releaseStats.averageScore}}</el-descriptions-item></el-descriptions><h3>学生作答</h3><el-table :data="submissions"><el-table-column prop="studentNumber" label="学号"/><el-table-column prop="studentName" label="姓名"/><el-table-column label="状态"><template #default="{row}"><el-tag>{{submissionStatus(row.status)}}</el-tag></template></el-table-column><el-table-column label="客观得分"><template #default="{row}">{{row.objectiveScore==null?'-':`${row.objectiveScore} / ${row.objectiveTotal}`}}</template></el-table-column><el-table-column label="主观题"><template #default="{row}">{{row.subjectivePendingCount?`${row.subjectivePendingCount}题待人工处理`:''}}</template></el-table-column><el-table-column prop="submittedAt" label="提交时间"/><el-table-column label="操作"><template #default="{row}"><el-button v-if="row.status==='SUBMITTED'" @click="openSubmission(row)">查看答卷</el-button></template></el-table-column></el-table></el-dialog>
    <el-dialog v-model="answerVisible" title="学生已提交答卷" width="min(900px, calc(100vw - 24px))"><article v-for="q in selectedSubmission?.questions" :key="q.itemId" class="question-card"><b>第{{q.order}}题 · {{questionTypeLabel(q.type)}} · {{q.score}}分</b><QuestionContent :content="q.stem" :attachments="q.stemAttachments"/><p>学生答案：{{q.submittedAnswer||'未作答'}}</p><p v-if="q.type!=='SUBJECTIVE'">客观得分：{{q.awardedScore}} / {{q.score}}；正确答案：{{q.correctAnswer}}</p><p v-else>状态：待人工处理</p><ScientificText :content="q.standardAnalysis"/></article></el-dialog>
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
@media (max-width: 800px) {
  .heading {
    align-items: stretch;
    flex-direction: column;
  }
  .base-fields,
  .rule-fields,
  .filters,
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
