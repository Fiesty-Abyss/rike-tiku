<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from "vue";
import {
  ElMessage,
  ElMessageBox,
  type FormInstance,
  type FormRules,
} from "element-plus";
import { useRouter } from "vue-router";
import { fetchTeachingScopes, type TeachingScope } from "../../api/teacher";
import { fetchPaperKnowledgePoints } from "../../api/teacher/papers";
import {
  createPrivateQuestion,
  deletePrivateQuestion,
  disablePrivateQuestion,
  fetchPrivateQuestion,
  fetchPrivateQuestions,
  publishPrivateQuestion,
  submitPrivateQuestionToAdmin,
  updatePrivateQuestion,
  type PrivateQuestion,
} from "../../api/teacher/privateQuestions";
import type { Save, QuestionType } from "../../api/admin/questions";
import {
  defaultOptions,
  normaliseForSave,
  sourceParts,
} from "../admin/questionForm";
import ScientificText from "../../components/question/ScientificText.vue";

const router = useRouter();
const rows = ref<PrivateQuestion[]>([]),
  scopes = ref<TeachingScope[]>([]),
  points = ref<Array<{ id: number; path: string }>>([]);
const loading = ref(false),
  saving = ref(false),
  visible = ref(false),
  editing = ref<PrivateQuestion | null>(null),
  formRef = ref<FormInstance>(),
  blanks = ref<string[][]>([[""]]);
const form = reactive({
  scopeId: undefined as number | undefined,
  question: emptyQuestion(),
});
const questionTypes: Array<{ label: string; value: QuestionType }> = [
  { label: "单选", value: "SINGLE_CHOICE" },
  { label: "多选", value: "MULTIPLE_CHOICE" },
  { label: "填空", value: "FILL_BLANK" },
];
const rules: FormRules = {
  scopeId: [{ required: true, message: "请选择任课范围", trigger: "change" }],
  stem: [{ required: true, message: "请填写题干", trigger: "blur" }],
  standardAnalysis: [
    { required: true, message: "请填写 STANDARD", trigger: "blur" },
  ],
  knowledgePointIds: [
    {
      required: true,
      type: "array",
      min: 1,
      message: "至少选择一个知识点",
      trigger: "change",
    },
  ],
};
const isChoice = computed(() =>
  ["SINGLE_CHOICE", "MULTIPLE_CHOICE"].includes(form.question.questionType),
);
function emptyQuestion(): Save {
  return {
    subjectId: 0,
    questionType: "SINGLE_CHOICE",
    usageMode: "ONLINE_PRACTICE",
    stem: "",
    correctAnswer: "{}",
    difficulty: 2,
    autoGradable: true,
    options: defaultOptions(),
    standardAnalysis: "",
    knowledgePointIds: [],
    sources: sourceParts.map((contentType) => ({
      contentType,
      sourceType: "TEACHER_CREATED",
      sourceName: "教师自建",
      rightsStatus: "AUTHORIZED",
      sourceAddress: "",
      rightsBasis: "教师原创",
    })),
  };
}
function typeLabel(value: string) {
  return (
    (
      {
        SINGLE_CHOICE: "单选",
        MULTIPLE_CHOICE: "多选",
        FILL_BLANK: "填空",
      } as Record<string, string>
    )[value] || value
  );
}
function statusLabel(value: string) {
  return (
    (
      {
        DRAFT: "草稿",
        PENDING: "已提交管理员",
        PUBLISHED: "已发布",
        DISABLED: "已停用",
      } as Record<string, string>
    )[value] || value
  );
}
async function load() {
  loading.value = true;
  try {
    const [privateRows, teachingScopes] = await Promise.all([
      fetchPrivateQuestions(),
      fetchTeachingScopes(),
    ]);
    rows.value = privateRows;
    scopes.value = teachingScopes.filter(
      (scope) => scope.teachingStatus === "ACTIVE",
    );
  } finally {
    loading.value = false;
  }
}
async function loadPoints() {
  const scope = scopes.value.find(
    (item) => item.teachingAssignmentId === form.scopeId,
  );
  if (!scope) {
    points.value = [];
    return;
  }
  form.question.subjectId = scope.subjectId;
  points.value = await fetchPaperKnowledgePoints(scope.subjectId);
}
watch(
  () => form.scopeId,
  () => {
    void loadPoints();
  },
);
function openCreate() {
  editing.value = null;
  form.scopeId = scopes.value[0]?.teachingAssignmentId;
  form.question = emptyQuestion();
  blanks.value = [[""]];
  visible.value = true;
  void loadPoints();
}
async function openEdit(row: PrivateQuestion) {
  const scope = scopes.value.find(
    (item) => item.teachingAssignmentId === row.teachingAssignmentId,
  );
  try {
    const detail = await fetchPrivateQuestion(row.id);
    editing.value = row;
    form.scopeId = row.teachingAssignmentId;
    form.question = {
      subjectId: scope?.subjectId || 0,
      questionType: detail.question.questionType as QuestionType,
      usageMode: detail.question.usageMode,
      stem: detail.stem,
      correctAnswer: detail.correctAnswer,
      difficulty: detail.question.difficulty,
      autoGradable: detail.question.autoGradable,
      options: detail.options,
      standardAnalysis: detail.standardAnalysis,
      knowledgePointIds: detail.knowledgePoints.map((item) => item.id),
      sources: detail.sources,
    };
    try {
      const answer = JSON.parse(detail.correctAnswer);
      blanks.value = Array.isArray(answer.blanks)
        ? answer.blanks.map((item: { acceptedAnswers?: string[] }) =>
            item.acceptedAnswers?.length ? item.acceptedAnswers : [""],
          )
        : [[""]];
    } catch {
      blanks.value = [[""]];
    }
    await loadPoints();
    visible.value = true;
  } catch (error: any) {
    ElMessage.error(error.message || "草稿详情加载失败");
  }
}
function changeType() {
  if (form.question.questionType === "FILL_BLANK") {
    form.question.options = [];
    form.question.autoGradable = true;
  } else {
    form.question.autoGradable = true;
    if (form.question.options.length < 2)
      form.question.options = defaultOptions();
  }
}
function addOption() {
  form.question.options.push({
    label: String.fromCharCode(65 + form.question.options.length),
    content: "",
    correct: false,
  });
}
function chooseSingle(index: number) {
  form.question.options.forEach((option, optionIndex) => {
    option.correct = optionIndex === index;
  });
}
function addBlank() {
  blanks.value.push([""]);
}
function addAccepted(index: number) {
  blanks.value[index].push("");
}
async function save() {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid || !form.scopeId) return;
  saving.value = true;
  try {
    const body = normaliseForSave(form.question, blanks.value);
    if (editing.value) await updatePrivateQuestion(editing.value.id, body);
    else await createPrivateQuestion(form.scopeId, body);
    ElMessage.success(
      editing.value ? "私有题草稿已保存。" : "私有题草稿已创建。",
    );
    visible.value = false;
    await load();
  } catch (error: any) {
    ElMessage.error(error.message || "保存失败，请检查任课范围和题目字段。");
  } finally {
    saving.value = false;
  }
}
async function publish(row: PrivateQuestion) {
  try {
    await publishPrivateQuestion(row.id);
    ElMessage.success("已发布到绑定班级。");
    await load();
  } catch (error: any) {
    ElMessage.error(error.message || "发布失败");
  }
}
async function submitAdmin(row: PrivateQuestion) {
  try {
    await submitPrivateQuestionToAdmin(row.id);
    ElMessage.success("已提交管理员审核。");
    await load();
  } catch (error: any) {
    ElMessage.error(error.message || "提交失败");
  }
}
async function disable(row: PrivateQuestion) {
  try {
    await disablePrivateQuestion(row.id);
    ElMessage.success("题目已停用。");
    await load();
  } catch (error: any) {
    ElMessage.error(error.message || "停用失败");
  }
}
async function remove(row: PrivateQuestion) {
  try {
    await ElMessageBox.confirm(
      "确认删除该未引用草稿？此操作不可恢复。",
      "删除草稿",
      { type: "warning" },
    );
    await deletePrivateQuestion(row.id);
    ElMessage.success("草稿已删除。");
    await load();
  } catch (error: any) {
    if (error !== "cancel" && error !== "close")
      ElMessage.error(error.message || "删除失败");
  }
}
onMounted(load);
</script>

<template>
  <main class="workspace-page">
    <header class="workspace-header">
      <div>
        <h1>我的班级题库</h1>
        <p>仅面向本人当前任课范围创建、发布和维护题目。</p>
      </div>
      <div>
        <el-button @click="router.push('/teacher/ai-generation')"
          >AI 生成候选题</el-button
        ><el-button type="primary" @click="openCreate">新建私有题</el-button>
      </div>
    </header>
    <el-alert
      title="AI 候选题需要教师预览、编辑后再发布私有范围或提交管理员审核；AI 不可用不影响手动建题。"
      type="info"
      :closable="false"
    />
    <el-table v-loading="loading" :data="rows" class="data-table"
      ><el-table-column prop="className" label="班级" /><el-table-column
        prop="subjectName"
        label="学科"
      /><el-table-column label="题型"
        ><template #default="{ row }">{{
          typeLabel(row.questionType)
        }}</template></el-table-column
      ><el-table-column label="题干" min-width="280"
        ><template #default="{ row }"
          ><ScientificText :content="row.stem" /></template></el-table-column
      ><el-table-column label="状态"
        ><template #default="{ row }"
          ><el-tag>{{ statusLabel(row.status) }}</el-tag></template
        ></el-table-column
      ><el-table-column label="操作" min-width="310"
        ><template #default="{ row }"
          ><el-button
            v-if="row.status === 'DRAFT'"
            link
            type="primary"
            @click="openEdit(row)"
            >编辑</el-button
          ><el-button
            v-if="row.status === 'DRAFT'"
            link
            type="success"
            @click="publish(row)"
            >发布班级</el-button
          ><el-button
            v-if="row.status === 'DRAFT'"
            link
            type="primary"
            @click="submitAdmin(row)"
            >提交管理员</el-button
          ><el-button
            v-if="row.status !== 'DISABLED'"
            link
            type="warning"
            @click="disable(row)"
            >停用</el-button
          ><el-button
            v-if="row.status === 'DRAFT'"
            link
            type="danger"
            @click="remove(row)"
            >删除草稿</el-button
          ></template
        ></el-table-column
      ></el-table
    >
    <el-dialog
      v-model="visible"
      :title="editing ? '编辑私有题草稿' : '新建私有题草稿'"
      width="min(860px, calc(100vw - 32px))"
      destroy-on-close
      ><el-form
        ref="formRef"
        :model="{ ...form.question, scopeId: form.scopeId }"
        :rules="rules"
        label-width="100px"
        ><el-form-item label="任课范围" prop="scopeId"
          ><el-select v-model="form.scopeId" filterable
            ><el-option
              v-for="scope in scopes"
              :key="scope.teachingAssignmentId"
              :label="`${scope.className} · ${scope.subjectName}`"
              :value="scope.teachingAssignmentId" /></el-select></el-form-item
        ><el-form-item label="学科"
          ><el-input
            :model-value="
              scopes.find((item) => item.teachingAssignmentId === form.scopeId)
                ?.subjectName || ''
            "
            disabled /></el-form-item
        ><el-form-item label="知识点" prop="knowledgePointIds"
          ><el-select
            v-model="form.question.knowledgePointIds"
            multiple
            filterable
            style="width: 100%"
            ><el-option
              v-for="point in points"
              :key="point.id"
              :label="point.path"
              :value="point.id" /></el-select></el-form-item
        ><el-form-item label="题型"
          ><el-select v-model="form.question.questionType" @change="changeType"
            ><el-option
              v-for="type in questionTypes"
              :key="type.value"
              :label="type.label"
              :value="type.value" /></el-select></el-form-item
        ><el-form-item label="难度"
          ><el-radio-group v-model="form.question.difficulty"
            ><el-radio v-for="level in 5" :key="level" :value="level">{{
              level
            }}</el-radio></el-radio-group
          ></el-form-item
        ><el-form-item label="题干" prop="stem"
          ><el-input
            v-model="form.question.stem"
            type="textarea"
            :rows="4" /></el-form-item
        ><el-form-item v-if="isChoice" label="选项"
          ><div class="question-editor">
            <div
              v-for="(option, index) in form.question.options"
              :key="option.label"
              class="option-row"
            >
              <el-radio
                v-if="form.question.questionType === 'SINGLE_CHOICE'"
                :model-value="option.correct"
                :value="true"
                @change="chooseSingle(index)"
              /><el-checkbox v-else v-model="option.correct" /><strong>{{
                option.label
              }}</strong
              ><el-input v-model="option.content" /><el-button
                link
                type="danger"
                :disabled="form.question.options.length <= 2"
                @click="form.question.options.splice(index, 1)"
                >删除</el-button
              >
            </div>
            <el-button link type="primary" @click="addOption"
              >添加选项</el-button
            >
          </div></el-form-item
        ><el-form-item
          v-if="form.question.questionType === 'FILL_BLANK'"
          label="填空答案"
          ><div class="question-editor">
            <div
              v-for="(blank, index) in blanks"
              :key="index"
              class="blank-row"
            >
              <el-input
                v-for="(_, answerIndex) in blank"
                :key="answerIndex"
                v-model="blank[answerIndex]"
                placeholder="可接受答案"
              /><el-button link @click="addAccepted(index)"
                >添加同义答案</el-button
              >
            </div>
            <el-button link @click="addBlank">添加空位</el-button>
          </div></el-form-item
        ><el-form-item label="STANDARD" prop="standardAnalysis"
          ><el-input
            v-model="form.question.standardAnalysis"
            type="textarea"
            :rows="4" /></el-form-item></el-form
      ><template #footer
        ><el-button @click="visible = false">取消</el-button
        ><el-button type="primary" :loading="saving" @click="save"
          >保存草稿</el-button
        ></template
      ></el-dialog
    >
  </main>
</template>
