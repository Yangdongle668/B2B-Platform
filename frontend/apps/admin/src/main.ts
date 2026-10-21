import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import '@b2bweb/admin-kit/admin.css'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import App from './App.vue'
import { router } from './core/router'
createApp(App).use(router).use(ElementPlus, { locale: zhCn }).mount('#app')
