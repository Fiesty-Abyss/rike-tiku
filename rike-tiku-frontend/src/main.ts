import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import { ElMessage } from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import 'element-plus/dist/index.css'
import 'katex/dist/katex.min.css'

import App from './App.vue'
import router from './router'
import { setAuthenticationErrorHandler } from './api/http'
import { useAuthStore } from './stores/auth'
import './style.css'
import './styles/tokens.css'
import './styles/themes/mizuiro-aero.css'
import './styles/components.css'
import './styles/subject-environments.css'
import './styles/motion.css'

const app = createApp(App)
const pinia = createPinia()
const authStore = useAuthStore(pinia)

setAuthenticationErrorHandler((error) => {
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
