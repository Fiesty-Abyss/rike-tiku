import { defineStore } from 'pinia'

export const useAppStore = defineStore('app', {
  state: () => ({
    projectName: 'RIKE 理科学习辅助系统',
  }),
})

