<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { fetchMessages, hideMessage, markConversationRead, recallMessage, sendMessage, type MessagePage } from '../../api/messages'
import AquaBrand from '../../components/layout/AquaBrand.vue'
import { useAuthStore } from '../../stores/auth'
import { startMessagePolling } from './messagePolling'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const isTeacher = computed(() => auth.activeRole === 'TEACHER')
const conversationId = computed(() => Number(route.params.id))
const page = ref<MessagePage | null>(null)
const content = ref('')
const loading = ref(false)
const sending = ref(false)
const messageList = ref<HTMLElement>()
let stopPolling: (() => void) | undefined

function errorMessage(error: unknown, fallback: string) {
  const api = error as { code?: string; message?: string }
  const messages: Record<string, string> = {
    MESSAGE_CONVERSATION_FORBIDDEN: '无权访问该会话。',
    MESSAGE_RELATIONSHIP_INACTIVE: '当前教学关系已失效，只能查看历史消息。',
  }
  return (api.code && messages[api.code]) || api.message || fallback
}

async function load(showError = true) {
  try {
    page.value = await fetchMessages(conversationId.value)
    await markConversationRead(conversationId.value)
    await nextTick()
    if (messageList.value) messageList.value.scrollTop = messageList.value.scrollHeight
  } catch (error) {
    if (showError) ElMessage.error(errorMessage(error, '消息加载失败，请稍后重试。'))
  }
}

async function submit() {
  const value = content.value.trim()
  if (!value) {
    ElMessage.warning('请输入消息内容。')
    return
  }
  sending.value = true
  try {
    await sendMessage(conversationId.value, value)
    content.value = ''
    await load()
  } catch (error) {
    ElMessage.error(errorMessage(error, '消息发送失败，请稍后重试。'))
  } finally {
    sending.value = false
  }
}

async function recall(id:number){
  if(!window.confirm('撤回后双方都只会看到“消息已撤回”。确认撤回？'))return
  try{await recallMessage(conversationId.value,id);await load(false)}catch(error){ElMessage.warning(errorMessage(error,'消息撤回失败。'))}
}
async function hide(id:number){
  if(!window.confirm('仅从你的消息列表中删除，对方仍可查看。确认删除？'))return
  try{await hideMessage(conversationId.value,id);await load(false)}catch(error){ElMessage.warning(errorMessage(error,'消息删除失败。'))}
}

function formatTime(value: string) {
  return new Date(value).toLocaleString('zh-CN', { hour12: false })
}

onMounted(async () => {
  loading.value = true
  await load()
  loading.value = false
  stopPolling = startMessagePolling(() => load(false))
})
onBeforeUnmount(() => stopPolling?.())
</script>

<template>
  <main class="workspace-page message-page">
    <header class="workspace-header shared-workspace-header">
      <div>
        <AquaBrand class="workspace-brand-aqua" :to="isTeacher ? '/teacher' : '/student'" :subtitle="isTeacher ? '教师科学工作台' : '学生科学工作台'" compact />
        <h1 v-if="page">{{ page.conversation.subjectName }} · {{ page.conversation.peerName }}</h1>
        <p v-if="page">{{ page.conversation.className }} · {{ page.conversation.canSend ? '教学关系有效' : '历史会话，只读' }}</p>
      </div>
      <el-button @click="router.push('/messages')">返回消息列表</el-button>
    </header>
    <section v-loading="loading" class="workspace-content chat-shell">
      <template v-if="page">
        <div ref="messageList" class="chat-history" aria-live="polite">
          <el-empty v-if="page.messages.length === 0" description="暂无消息，可以发送第一条私信。" :image-size="72" />
          <article v-for="item in page.messages" :key="item.id" class="chat-message" :class="{ mine: item.mine, recalled:item.recalled }">
            <div><strong>{{ item.mine ? '我' : item.senderName }}</strong><p>{{ item.content }}</p><small>{{ formatTime(item.sentAt) }}</small><div class="message-actions"><button v-if="item.mine&&item.recallable" type="button" @click="recall(item.id)">撤回</button><button type="button" @click="hide(item.id)">删除</button></div></div>
          </article>
        </div>
        <div class="chat-composer">
          <el-input v-model="content" type="textarea" :rows="3" maxlength="1000" show-word-limit placeholder="输入消息，最多1000字" :disabled="!page.conversation.canSend" @keydown.ctrl.enter.prevent="submit" />
          <div><span>{{ page.conversation.canSend ? 'Ctrl + Enter 发送' : '教学关系已失效，历史消息仍可查看。' }}</span><el-button type="primary" :loading="sending" :disabled="!page.conversation.canSend" @click="submit">发送消息</el-button></div>
        </div>
      </template>
    </section>
  </main>
</template>
