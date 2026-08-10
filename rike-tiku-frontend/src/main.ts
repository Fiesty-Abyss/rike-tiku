import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import { ElMessage } from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import 'element-plus/dist/index.css'

import App from './App.vue'
import router from './router'
import { setAuthenticationErrorHandler } from './api/http'
import { useAuthStore } from './stores/auth'
import '../tokens.css'
import './style.css'

const app = createApp(App)
const pinia = createPinia()
const authStore = useAuthStore(pinia)

setAuthenticationErrorHandler((error) => {
  if (error.code === 'MUST_CHANGE_PASSWORD') {
    void router.replace('/change-initial-password')
    return
  }
  if (error.status === 401 || error.code === 'TOKEN_EXPIRED' || error.code === 'TOKEN_INVALID') {
    authStore.logout()
    void router.replace('/login')
    return
  }
  if (error.code === 'ACCESS_DENIED') {
    ElMessage.error('当前账号没有访问该页面的权限。')
  }
})

app.use(pinia).use(router).use(ElementPlus, { locale: zhCn }).mount('#app')
