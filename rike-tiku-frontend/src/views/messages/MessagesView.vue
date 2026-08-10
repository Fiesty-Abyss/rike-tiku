<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { createConversation, fetchConversations, fetchMessageContacts, type MessageContact, type MessageConversation } from '../../api/messages'
import { useAuthStore } from '../../stores/auth'

const auth = useAuthStore()
const router = useRouter()
const loading = ref(false)
const conversations = ref<MessageConversation[]>([])
const contacts = ref<MessageContact[]>([])
const isTeacher = computed(() => auth.activeRole === 'TEACHER')

function errorMessage(error: unknown, fallback: string) {
  const api = error as { code?: string; message?: string }
  const messages: Record<string, string> = {
    MESSAGE_RELATIONSHIP_FORBIDDEN: '当前教学关系无效，无法建立会话。',
    MESSAGE_ROLE_FORBIDDEN: '当前账号不能使用私信。',
  }
  return (api.code && messages[api.code]) || api.message || fallback
}

async function load() {
  loading.value = true
  try {
    [conversations.value, contacts.value] = await Promise.all([fetchConversations(), fetchMessageContacts()])
  } catch (error) {
    ElMessage.error(errorMessage(error, '消息列表加载失败，请稍后重试。'))
  } finally {
    loading.value = false
  }
}

async function openContact(contact: MessageContact) {
  try {
    const conversation = await createConversation(contact.teachingAssignmentId, contact.studentId ?? undefined)
    await router.push(`/messages/${conversation.id}`)
  } catch (error) {
    ElMessage.error(errorMessage(error, '会话建立失败，请稍后重试。'))
  }
}

function formatTime(value: string | null) {
  return value ? new Date(value).toLocaleString('zh-CN', { hour12: false }) : '暂无消息'
}

onMounted(() => void load())
</script>

<template>
  <main class="workspace-page message-page">
    <header class="workspace-header shared-workspace-header">
      <div>
        <button class="workspace-brand workspace-brand-button" type="button" @click="router.push(isTeacher ? '/teacher' : '/student')">
          RIKE · {{ isTeacher ? '教师工作台' : '学生工作台' }}
        </button>
        <h1>师生消息</h1>
        <p>仅可联系当前有效教学关系中的{{ isTeacher ? '学生' : '任课教师' }}。</p>
      </div>
      <el-button @click="router.push(isTeacher ? '/teacher' : '/student')">返回工作台</el-button>
    </header>
    <section v-loading="loading" class="workspace-content message-content">
      <section class="workspace-card">
        <div class="section-title-row">
          <div><h2>最近会话</h2><p>未读消息会在这里提示。</p></div>
        </div>
        <div v-if="conversations.length" class="conversation-list">
          <button v-for="item in conversations" :key="item.id" type="button" class="conversation-row" @click="router.push(`/messages/${item.id}`)">
            <span><strong>{{ item.peerName }}</strong><small>{{ item.className }} · {{ item.subjectName }}</small></span>
            <span class="conversation-preview">{{ item.latestMessage || '会话已建立，暂时没有消息。' }}</span>
            <span class="conversation-meta"><el-badge :value="item.unreadCount" :hidden="item.unreadCount === 0" /><small>{{ formatTime(item.latestMessageTime) }}</small></span>
          </button>
        </div>
        <el-empty v-else description="当前暂无私信会话。" :image-size="72" />
      </section>
      <section class="workspace-card">
        <div class="section-title-row">
          <div><h2>{{ isTeacher ? '可联系学生' : '联系老师' }}</h2><p>{{ isTeacher ? '也可从班级学科工作台的学生名单发起私信。' : '名单由当前主班级的 ACTIVE 任课关系自动生成。' }}</p></div>
        </div>
        <div v-if="contacts.length" class="contact-list">
          <button v-for="contact in contacts" :key="`${contact.teachingAssignmentId}-${contact.studentId || 0}`" type="button" class="contact-row" @click="openContact(contact)">
            <span><strong>{{ contact.name }}</strong><small>{{ contact.className }} · {{ contact.subjectName }}</small></span>
            <span class="contact-action">开始私信</span>
          </button>
        </div>
        <el-empty v-else description="当前没有可联系的教学关系。" :image-size="72" />
      </section>
    </section>
  </main>
</template>
