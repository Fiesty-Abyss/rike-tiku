import http from './http'

export interface MessageContact {
  teachingAssignmentId: number
  studentId: number | null
  name: string
  className: string
  subjectName: string
}

export interface MessageConversation {
  id: number
  teachingAssignmentId: number
  studentId: number
  peerName: string
  className: string
  subjectName: string
  latestMessage: string | null
  latestMessageTime: string | null
  unreadCount: number
  canSend: boolean
}

export interface ChatMessage {
  id: number
  senderUserId: number
  senderName: string
  content: string
  mine: boolean
  read: boolean
  sentAt: string
  readAt: string | null
}

export interface MessagePage {
  conversation: MessageConversation
  messages: ChatMessage[]
}

export const fetchMessageContacts = () =>
  http.get<MessageContact[]>('/messages/contacts').then((response) => response.data)

export const fetchConversations = () =>
  http.get<MessageConversation[]>('/messages/conversations').then((response) => response.data)

export const createConversation = (teachingAssignmentId: number, studentId?: number) =>
  http.post<MessageConversation>('/messages/conversations', { teachingAssignmentId, studentId }).then((response) => response.data)

export const fetchMessages = (conversationId: number) =>
  http.get<MessagePage>(`/messages/conversations/${conversationId}/messages`).then((response) => response.data)

export const sendMessage = (conversationId: number, content: string) =>
  http.post<ChatMessage>(`/messages/conversations/${conversationId}/messages`, { content }).then((response) => response.data)

export const markConversationRead = (conversationId: number) =>
  http.post<{ readCount: number }>(`/messages/conversations/${conversationId}/read`).then((response) => response.data)
