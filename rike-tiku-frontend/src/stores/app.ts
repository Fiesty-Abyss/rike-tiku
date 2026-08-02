import { defineStore } from 'pinia'

export const useAppStore = defineStore('app', {
  state: () => ({
    projectName: '集成大模型智能答疑的在线题库实训管理系统',
  }),
})

