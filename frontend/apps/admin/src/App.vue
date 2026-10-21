<script setup lang="ts">
import { ref, onMounted, watch, provide } from 'vue'
import { adminRuntimeKey } from '@b2bweb/admin-kit'
import type { SiteBootstrap } from '@b2bweb/admin-kit'
import { router, runtime, authStage } from './core/router'
import QRCode from 'qrcode'
import { api } from '@b2bweb/api-client'
import { texts } from './texts'
interface SessionInfo { state: State; displayName: string; sites: Array<{ id: string; name: string }> }
provide(adminRuntimeKey, runtime)
const siteReady = ref(false); const readonlySite = ref(false)
const availableSites = ref<SessionInfo['sites']>([]); const selectedSite = ref(localStorage.getItem('b2bweb.admin.siteId') ?? ''); const siteModules = ref<Array<{ moduleId: string; name: string }>>([]); const moduleMenus = ref<Array<{ key: string; name: string; route: string | null }>>([])
interface State { stage: string; userId: string; recoveryCodes?: string[] }
const qr = ref(''); const reauthVisible = ref(false); const reauthPassword = ref(''); const reauthCode = ref(''); const reauthSuccess = ref(false)
const email = ref(''); const password = ref(''); const code = ref(''); const busy = ref(false); const error = ref(''); const state = ref<State>(); const secret = ref(''); const recoveryCodes = ref<string[]>([]); const newPassword = ref('')
async function loadSite() {
  const siteId = selectedSite.value
  siteReady.value = false; readonlySite.value = false; siteModules.value = []; moduleMenus.value = []
  if (!siteId || state.value?.stage !== 'AUTHENTICATED') { runtime.clear(); return }
  try {
    const bootstrap = await runtime.switchSite(siteId, () => api<SiteBootstrap>('/api/admin/v1/system/bootstrap', { headers: { 'X-Site-Id': siteId } }))
    if (!bootstrap) return
    siteModules.value = bootstrap.modules; moduleMenus.value = runtime.menus(); readonlySite.value = runtime.readonly; siteReady.value = true
    localStorage.setItem('b2bweb.admin.siteId', siteId)
    const current = router.currentRoute.value
    const target = router.resolve(current.fullPath)
    const access = target.meta.moduleId ? runtime.access(String(target.name), true) : target.name === 'unmatched' ? 'not-found' : 'allowed'
    if (current.name === 'login' || current.meta.moduleId && access === 'not-found') await router.replace('/')
    else if (access !== 'allowed') await router.replace({ name: access })
    else await router.replace(target.fullPath)
  } catch (failure) { if (selectedSite.value === siteId) { error.value = texts.failed; if ((failure as { status?: number }).status === 401) { state.value = undefined; authStage.value = 'ANONYMOUS'; runtime.clear(); await router.replace('/login') } } }
}
async function loadSession() { const me = await api<SessionInfo>('/api/admin/v1/system/auth/me'); state.value = me.state; availableSites.value = me.sites; if (me.sites.length && !me.sites.some(site => site.id === selectedSite.value)) selectedSite.value = me.sites[0]!.id; if (me.state.stage === 'AUTHENTICATED') await loadSite() }
onMounted(async () => { try { await loadSession() } catch { state.value = undefined; authStage.value = 'ANONYMOUS'; await router.replace('/login') } })
watch(() => state.value?.stage, async stage => { authStage.value = stage ?? 'ANONYMOUS'; if (stage === 'AUTHENTICATED') { try { await loadSession() } catch { error.value = texts.failed } } })
watch(selectedSite, loadSite)
async function send(path: string, body: object): Promise<unknown> { const csrf = await api<{ token: string }>('/api/admin/v1/system/auth/csrf'); return api(path, { method: 'POST', body: JSON.stringify(body) }, csrf.token) }
async function login() { busy.value = true; error.value = ''; try { state.value = await send('/api/admin/v1/system/auth/login', { email: email.value, password: password.value }) as State; password.value = '' } catch { error.value = texts.failed } finally { busy.value = false } }
async function setup() { busy.value = true; try { const enrollment = await send('/api/admin/v1/system/auth/mfa/setup', {}) as { secret: string; otpauthUri: string }; secret.value = enrollment.secret; qr.value = await QRCode.toDataURL(enrollment.otpauthUri, { width: 220, margin: 2 }) } catch { error.value = texts.failed } finally { busy.value = false } }
async function verify() { busy.value = true; try { state.value = await send(`/api/admin/v1/system/auth/mfa/${secret.value ? 'activate' : 'verify'}`, { code: code.value }) as State; recoveryCodes.value = state.value.recoveryCodes ?? recoveryCodes.value; secret.value = ''; qr.value = ''; code.value = '' } catch { error.value = texts.failed } finally { busy.value = false } }
async function changePassword() { busy.value = true; try { state.value = await send('/api/admin/v1/system/auth/password', { password: newPassword.value }) as State; newPassword.value = '' } catch { error.value = texts.failed } finally { busy.value = false } }
async function reauthenticate() { busy.value = true; error.value = ''; try { await send('/api/admin/v1/system/auth/reauthenticate', { password: reauthPassword.value, code: reauthCode.value }); reauthSuccess.value = true; reauthVisible.value = false } catch { error.value = texts.failed } finally { reauthPassword.value = ''; reauthCode.value = ''; busy.value = false } }
async function logout() { await send('/api/admin/v1/system/auth/logout', {}); authStage.value = 'ANONYMOUS'; runtime.clear(); siteReady.value = false; reauthSuccess.value = false; await router.replace('/login'); state.value = undefined; availableSites.value = []; selectedSite.value = ''; siteModules.value = []; moduleMenus.value = []; secret.value = ''; qr.value = ''; recoveryCodes.value = []; error.value = '' }
</script>
<template>
  <el-container class="admin-shell"><el-header class="admin-header"><strong>{{ texts.title }}</strong><span>{{ texts.environment }}</span></el-header><el-main><div :class="['admin-card', { 'admin-workspace': state?.stage === 'AUTHENTICATED' }]">
    <el-alert v-if="error" :title="error" type="error" :closable="false" role="alert" />
    <aside v-if="recoveryCodes.length"><h2>{{ texts.recovery }}</h2><ul><li v-for="item in recoveryCodes" :key="item"><code>{{ item }}</code></li></ul></aside>
    <p v-if="!state" class="auth-hint">{{ texts.authHint }}</p>
    <el-form v-if="!state" label-position="top" @submit.prevent="login"><h1>{{ texts.login }}</h1><el-form-item :label="texts.email"><el-input v-model="email" type="email" autocomplete="username" /></el-form-item><el-form-item :label="texts.password"><el-input v-model="password" type="password" autocomplete="current-password" /></el-form-item><el-button native-type="submit" :loading="busy">{{ texts.submit }}</el-button></el-form>
    <section v-else-if="state.stage.startsWith('MFA_')"><h1>{{ texts.verify }}</h1><el-button v-if="state.stage === 'MFA_ENROLLMENT_REQUIRED' && !secret" :loading="busy" @click="setup">{{ texts.mfaSetup }}</el-button><img v-if="qr" :src="qr" :alt="texts.qrAlt" width="220" height="220" /><p v-if="secret">{{ texts.enrollment }} <code>{{ secret }}</code></p><el-form @submit.prevent="verify"><el-form-item :label="texts.code"><el-input v-model="code" autocomplete="one-time-code" /></el-form-item><el-button native-type="submit" :loading="busy">{{ texts.verify }}</el-button></el-form></section>
    <el-form v-else-if="state.stage === 'PASSWORD_CHANGE_REQUIRED'" @submit.prevent="changePassword"><h1>{{ texts.changePassword }}</h1><el-form-item :label="texts.newPassword"><el-input v-model="newPassword" type="password" autocomplete="new-password" /></el-form-item><el-button native-type="submit" :loading="busy">{{ texts.submit }}</el-button></el-form>
    <section v-else><h1>{{ texts.welcome }}</h1><el-alert v-if="readonlySite" :title="texts.readonly" type="info" :closable="false" /><el-form-item :label="texts.site"><el-select v-model="selectedSite" :placeholder="texts.selectSite"><el-option v-for="site in availableSites" :key="site.id" :value="site.id" :label="site.name" /></el-select></el-form-item><nav v-if="siteReady" class="module-navigation"><RouterLink to="/">{{ texts.home }}</RouterLink><RouterLink v-for="menu in moduleMenus" :key="menu.key" :to="menu.route!">{{ menu.name }}</RouterLink></nav><el-skeleton v-if="!siteReady" animated :aria-label="texts.loading" /><RouterView v-if="siteReady" :key="selectedSite" /><p>{{ texts.modules }}</p><ul><li v-for="module in siteModules" :key="module.moduleId">{{ module.name }}</li></ul><el-empty v-if="!siteModules.length" :description="texts.empty" /><el-button @click="reauthVisible = true">{{ texts.account }}</el-button><el-alert v-if="reauthSuccess" :title="texts.verified" type="success" :closable="false" /><el-button @click="logout">{{ texts.logout }}</el-button></section>
  </div></el-main>
    <el-dialog v-model="reauthVisible" :title="texts.reauthenticate" width="min(90vw, 420px)" @closed="reauthPassword = ''; reauthCode = ''">
      <el-form label-position="top" @submit.prevent="reauthenticate"><el-form-item :label="texts.password"><el-input v-model="reauthPassword" type="password" autocomplete="current-password" /></el-form-item><el-form-item :label="texts.code"><el-input v-model="reauthCode" autocomplete="one-time-code" /></el-form-item><el-button native-type="submit" :loading="busy">{{ texts.verify }}</el-button></el-form>
    </el-dialog>
  </el-container>
</template>

<style scoped>
.admin-shell { min-height: 100vh; font-family: Inter, "PingFang SC", "Microsoft YaHei", sans-serif; background: var(--el-fill-color-light); color: var(--el-text-color-primary); }
.admin-header { display: flex; align-items: center; justify-content: space-between; gap: 24px; border-bottom: 1px solid var(--el-border-color-lighter); background: var(--el-bg-color); }
.admin-workspace { max-width: 960px; }
.module-navigation { display: flex; flex-wrap: wrap; gap: 16px; padding: 16px 0; border-bottom: 1px solid var(--el-border-color-lighter); }
.admin-header span, .auth-hint { color: var(--el-text-color-regular); font-size: 14px; }
.admin-card { max-width: 440px; margin: 7vh auto; padding: 32px; border-radius: 12px; background: var(--el-bg-color); border: 1px solid var(--el-border-color-lighter); }
h1 { margin-top: 12px; margin-bottom: 28px; font-size: 28px; font-weight: 600; }
.el-form .el-button { width: 100%; margin-top: 12px; }
code { overflow-wrap: anywhere; }
@media (max-width: 480px) { .admin-header span { display: none; } .admin-card { padding: 24px; margin: 3vh auto; } }
</style>
