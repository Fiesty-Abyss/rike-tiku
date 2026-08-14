<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
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
  try{await ElMessageBox.confirm('撤回后双方将看到“消息已撤回”，原文不会再通过普通接口返回。','确认撤回消息',{confirmButtonText:'撤回消息',cancelButtonText:'取消',type:'warning',customClass:'message-confirm-dialog',center:true});await recallMessage(conversationId.value,id);await load(false)}catch(error){if(error==='cancel'||error==='close')return;ElMessage.warning(errorMessage(error,'消息撤回失败。'))}
}
async function hide(id:number){
  try{await ElMessageBox.confirm('仅从我的列表删除，对方仍可查看；历史记录不会被物理删除。','确认仅为我删除',{confirmButtonText:'仅为我删除',cancelButtonText:'取消',type:'warning',customClass:'message-confirm-dialog',center:true});await hideMessage(conversationId.value,id);await load(false)}catch(error){if(error==='cancel'||error==='close')return;ElMessage.warning(errorMessage(error,'消息删除失败。'))}
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
            <div class="message-bubble">
              <header class="message-bubble__header"><strong>{{ item.mine ? '我' : item.senderName }}</strong>
                <el-dropdown v-if="!item.recalled" trigger="click" placement="bottom-end" popper-class="message-action-menu">
                  <button class="message-more-button" type="button" :aria-label="`打开消息 ${item.id} 的操作菜单`">···</button>
                  <template #dropdown><el-dropdown-menu>
                    <el-dropdown-item v-if="item.mine&&item.recallable" class="message-action-recall" @click="recall(item.id)"><button class="message-menu-command message-menu-command--recall" type="button"><span aria-hidden="true">↶</span><span><strong>撤回消息</strong><small>仅限发送后 5 分钟内</small></span></button></el-dropdown-item>
                    <el-dropdown-item>
                      <button class="message-menu-command" type="button" @click="hide(item.id)"><span aria-hidden="true">⌫</span><span><strong>仅从我的列表删除</strong><small>不会影响对方的消息记录</small></span></button>
                    </el-dropdown-item>
                  </el-dropdown-menu></template>
                </el-dropdown>
              </header>
              <p>{{ item.content }}</p><small class="message-bubble__time">{{ formatTime(item.sentAt) }}</small>
            </div>
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

<style scoped>
.message-bubble{position:relative;min-width:min(18rem,80vw);max-width:min(42rem,82%);padding:.85rem 1rem .7rem;border:1px solid var(--glass-border);border-radius:18px 18px 18px 6px;background:var(--surface-solid);box-shadow:0 .45rem 1.4rem oklch(30% .035 225/.07)}
.chat-message.mine .message-bubble{margin-left:auto;border-color:oklch(62% .09 221/.34);border-radius:18px 18px 6px 18px;color:oklch(25% .035 225);background:oklch(90% .055 213)}
.chat-message.recalled .message-bubble{opacity:.72;background:var(--surface-muted)}
.message-bubble__header{display:flex;align-items:center;justify-content:space-between;gap:1rem;min-height:1.6rem}.message-bubble p{margin:.42rem 0 .55rem;line-height:1.65;white-space:pre-wrap;overflow-wrap:anywhere}.message-bubble__time{display:block;color:var(--ink-muted);font-variant-numeric:tabular-nums;text-align:right}
.message-more-button{appearance:none;-webkit-appearance:none;width:2rem;height:1.75rem;padding:0;border:1px solid transparent;border-radius:999px;color:var(--ink-muted);background:transparent;font:700 1rem/1 var(--font-body);letter-spacing:.08em;cursor:pointer;opacity:.35;transition:opacity .16s ease,background .16s ease,color .16s ease}
.message-bubble:hover .message-more-button,.message-more-button:focus-visible,.message-more-button[aria-expanded="true"]{opacity:1;color:var(--brand-deep);border-color:var(--glass-border);background:var(--surface-glass-strong)}
.message-menu-command{appearance:none;-webkit-appearance:none;display:flex;width:100%;align-items:center;gap:.7rem;padding:.25rem;border:0;color:var(--ink-secondary);background:transparent;text-align:left;cursor:pointer}.message-menu-command>span:first-child{width:1.4rem;color:var(--ink-muted);font-size:1rem;text-align:center}.message-menu-command>span:last-child{display:grid}.message-menu-command strong{font-size:.86rem}.message-menu-command small{color:var(--ink-muted);font-size:.72rem}.message-menu-command--recall strong,.message-menu-command--recall>span:first-child{color:oklch(54% .12 37)}
@media(max-width:600px){.message-bubble{max-width:90%;min-width:0}.message-more-button{opacity:.72}}
@media(prefers-reduced-motion:reduce){.message-more-button{transition:none}}
</style>
