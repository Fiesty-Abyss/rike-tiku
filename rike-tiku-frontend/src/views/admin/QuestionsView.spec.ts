// @vitest-environment jsdom
import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import QuestionsView from './QuestionsView.vue'

const { push, fetchSubjects, fetchQuestions, fetchQuestion, fetchKnowledgePoints, createQuestion, updateQuestion, uploadQuestionAttachment, replaceQuestionAttachment, deleteQuestionAttachment, confirmAttachmentDelete, messages } = vi.hoisted(() => ({
  push: vi.fn(),
  fetchSubjects: vi.fn(),
  fetchQuestions: vi.fn(),
  fetchQuestion: vi.fn(),
  fetchKnowledgePoints: vi.fn(),
  createQuestion: vi.fn(),
  updateQuestion: vi.fn(),
  uploadQuestionAttachment: vi.fn(),
  replaceQuestionAttachment: vi.fn(),
  deleteQuestionAttachment: vi.fn(),
  confirmAttachmentDelete: vi.fn(),
  messages: { success: vi.fn(), error: vi.fn(), warning: vi.fn() },
}))

vi.mock('vue-router', () => ({ useRouter: () => ({ push }) }))
vi.mock('../../api/admin/teachers', () => ({ fetchSubjects }))
vi.mock('../../api/admin/questions', () => ({ createQuestion, deleteQuestionAttachment, fetchKnowledgePoints, fetchQuestion, fetchQuestions, questionAction: vi.fn(), replaceQuestionAttachment, updateQuestion, uploadQuestionAttachment }))
vi.mock('element-plus', () => ({ ElMessage: messages, ElMessageBox: { confirm: confirmAttachmentDelete } }))

const attachment = (fileName = 'stem.png') => ({ id: 8, position: 'QUESTION', type: 'IMAGE', fileName, objectMarker: 'I001', status: 'ACTIVE', renderStatus: 'AVAILABLE', contentUrl: '/api/v1/admin/question-attachments/8/content' })
const detail = (stem = '服务器题干', standardAnalysis = '服务器解析', attachments = []) => ({
  question: { id: 91, subjectCode: 'PHYSICS', subjectName: '物理', questionType: 'SINGLE_CHOICE', usageMode: 'ONLINE_PRACTICE', stemSummary: stem, difficulty: 2, autoGradable: true, status: 'DRAFT', rightsStatus: 'AUTHORIZED', createdAt: '', updatedAt: '' },
  stem, correctAnswer: '{"schemaVersion":1,"type":"SINGLE_CHOICE","optionLabels":["A"]}', options: [{ label: 'A', content: '选项A', correct: true }, { label: 'B', content: '选项B', correct: false }], standardAnalysis,
  knowledgePoints: [{ id: 7, code: 'K7', name: '力学', path: '力学' }], sources: ['QUESTION', 'ANSWER', 'STANDARD_ANALYSIS'].map(contentType => ({ contentType, sourceType: 'TEACHER_CREATED', sourceName: '测试来源', rightsStatus: 'AUTHORIZED' })), attachments, reviews: [], allowedActions: ['SUBMIT'],
})

const stubs = {
  ElButton: { props: ['disabled', 'loading'], template: '<button :disabled="disabled" @click="$emit(\'click\')"><slot /></button>' },
  ElForm: { methods: { validate: () => Promise.resolve(true) }, template: '<form><slot /></form>' },
  ElFormItem: { template: '<div><slot /></div>' },
  ElInput: { props: ['modelValue', 'type'], emits: ['update:modelValue'], template: '<textarea v-if="type === \'textarea\'" :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" /><input v-else :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" />' },
  ElSelect: { template: '<div><slot /></div>' },
  ElOption: true,
  ElRadioGroup: { template: '<div><slot /></div>' },
  ElRadio: { template: '<span><slot /></span>' },
  ElCheckbox: { template: '<span><slot /></span>' },
  ElDivider: { template: '<hr />' },
  ElDialog: { props: ['modelValue'], template: '<div v-if="modelValue" class="dialog"><slot /><slot name="footer" /></div>' },
  ElTable: { template: '<div><slot /></div>' },
  ElTableColumn: true,
  ElPagination: true,
  ElDrawer: { props: ['modelValue'], template: '<div v-if="modelValue"><slot /></div>' },
  ElDescriptions: { template: '<div><slot /></div>' },
  ElDescriptionsItem: { template: '<div><slot /></div>' },
  ElTag: { template: '<span><slot /></span>' },
  QuestionContent: { props: ['content'], template: '<div class="question-content-stub">{{ content }}</div>' },
}

function mountView() {
  return mount(QuestionsView, { global: { stubs } })
}

async function createDraft(wrapper: ReturnType<typeof mountView>) {
  await wrapper.findAll('button').find(button => button.text() === '创建草稿')!.trigger('click')
  await wrapper.findAll('textarea')[0].setValue('管理员填写的题干')
  await wrapper.findAll('textarea')[1].setValue('管理员填写的标准解析')
  await wrapper.findAll('button').find(button => button.text() === '保存草稿')!.trigger('click')
  await flushPromises()
}

function changeFile(input: HTMLElement, file: File) {
  Object.defineProperty(input, 'files', { configurable: true, value: [file] })
  return input
}

describe('管理员题目图片编辑流程', () => {
  let current = detail()

  beforeEach(() => {
    vi.clearAllMocks()
    current = detail()
    fetchSubjects.mockResolvedValue([{ id: 1, subjectCode: 'PHYSICS', subjectName: '物理' }])
    fetchQuestions.mockResolvedValue({ records: [], total: 0 })
    fetchKnowledgePoints.mockResolvedValue([{ id: 7, code: 'K7', name: '力学', path: '力学' }])
    fetchQuestion.mockImplementation(() => Promise.resolve(current))
    createQuestion.mockImplementation(async (body: { stem: string; standardAnalysis: string }) => { current = detail(body.stem, body.standardAnalysis); return current })
    updateQuestion.mockImplementation(async (_id: number, body: { stem: string; standardAnalysis: string }) => { current = detail(body.stem, body.standardAnalysis, current.attachments); return current })
    uploadQuestionAttachment.mockImplementation(async (_id: number, position: string, file: File) => {
      const next = attachment(file.name)
      current = detail(position === 'QUESTION' ? `${current.stem}\n〔图片对象 I001〕` : current.stem, position === 'STANDARD_ANALYSIS' ? `${current.standardAnalysis}\n〔图片对象 I001〕` : current.standardAnalysis, [...current.attachments, { ...next, position }])
      return next
    })
    replaceQuestionAttachment.mockImplementation(async (_questionId: number, _attachmentId: number, file: File) => { current = detail(current.stem, current.standardAnalysis, [attachment(file.name)]); return current.attachments[0] })
    deleteQuestionAttachment.mockImplementation(async () => { current = detail(current.stem.replace('\n〔图片对象 I001〕', ''), current.standardAnalysis, []); return undefined })
    confirmAttachmentDelete.mockResolvedValue(true)
  })

  it('创建草稿后不关闭对话框即可进入图片编辑', async () => {
    const wrapper = mountView()
    await flushPromises()
    await createDraft(wrapper)
    expect(createQuestion).toHaveBeenCalled()
    expect(wrapper.find('.dialog').exists()).toBe(true)
    expect(wrapper.findAll('input[type="file"]')).toHaveLength(2)
    expect(messages.success).toHaveBeenCalledWith('草稿已创建，现在可以上传题干或标准解析图片。')
  })

  it('编辑题干后直接上传图片会先保存文字且保留 marker', async () => {
    const wrapper = mountView()
    await flushPromises()
    await createDraft(wrapper)
    await wrapper.findAll('textarea')[0].setValue('未保存的新题干')
    const input = wrapper.findAll('input[type="file"]')[0]
    changeFile(input.element, new File(['png'], 'stem.png', { type: 'image/png' }))
    await input.trigger('change')
    await flushPromises()
    expect(updateQuestion).toHaveBeenLastCalledWith(91, expect.objectContaining({ stem: '未保存的新题干' }))
    expect(uploadQuestionAttachment).toHaveBeenCalledWith(91, 'QUESTION', expect.any(File))
    expect(wrapper.findAll('textarea')[0].element.value).toContain('未保存的新题干')
    expect(wrapper.findAll('textarea')[0].element.value).toContain('I001')
  })

  it('编辑标准解析后直接上传图片会先保存解析文字', async () => {
    const wrapper = mountView()
    await flushPromises()
    await createDraft(wrapper)
    await wrapper.findAll('textarea')[1].setValue('未保存的新标准解析')
    const input = wrapper.findAll('input[type="file"]')[1]
    changeFile(input.element, new File(['png'], 'analysis.png', { type: 'image/png' }))
    await input.trigger('change')
    await flushPromises()
    expect(updateQuestion).toHaveBeenLastCalledWith(91, expect.objectContaining({ standardAnalysis: '未保存的新标准解析' }))
    expect(uploadQuestionAttachment).toHaveBeenCalledWith(91, 'STANDARD_ANALYSIS', expect.any(File))
    expect(wrapper.findAll('textarea')[1].element.value).toContain('未保存的新标准解析')
    expect(wrapper.findAll('textarea')[1].element.value).toContain('I001')
  })

  it('上传、替换、删除后保持对象标识和展示状态一致', async () => {
    const wrapper = mountView()
    await flushPromises()
    await createDraft(wrapper)
    const uploadInput = wrapper.findAll('input[type="file"]')[0]
    changeFile(uploadInput.element, new File(['png'], 'first.png', { type: 'image/png' }))
    await uploadInput.trigger('change')
    await flushPromises()
    expect(wrapper.text()).toContain('I001')

    const replaceInput = wrapper.findAll('input[type="file"]')[1]
    changeFile(replaceInput.element, new File(['png'], 'replacement.png', { type: 'image/png' }))
    await replaceInput.trigger('change')
    await flushPromises()
    expect(replaceQuestionAttachment).toHaveBeenCalledWith(91, 8, expect.any(File))
    expect(wrapper.text()).toContain('replacement.png')
    expect(wrapper.text()).toContain('I001')

    await wrapper.findAll('button').find(button => button.text() === '删除')!.trigger('click')
    await flushPromises()
    expect(deleteQuestionAttachment).toHaveBeenCalledWith(91, 8)
    expect(wrapper.text()).not.toContain('replacement.png')
    expect(wrapper.findAll('textarea')[0].element.value).not.toContain('I001')
  })
})
