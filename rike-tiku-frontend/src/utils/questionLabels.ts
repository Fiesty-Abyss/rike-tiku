const questionTypeLabels: Record<string, string> = {
  SINGLE_CHOICE: '单选',
  MULTIPLE_CHOICE: '多选',
  FILL_BLANK: '填空',
  SUBJECTIVE: '主观大题',
}

const topicTypeLabels: Record<string, string> = {
  CALCULATION: '计算题',
  EXPERIMENT: '实验题',
  PROCESS: '流程题',
  MATERIAL_ANALYSIS: '材料分析题',
  COMPREHENSIVE: '综合题',
}

export const questionTypeLabel = (type?: string) => questionTypeLabels[type || ''] || '未分类题型'
export const topicTypeLabel = (type?: string) => topicTypeLabels[type || ''] || '专题题'
