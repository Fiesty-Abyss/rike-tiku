<script setup lang="ts">
import { nextTick, ref, watch } from "vue";
import { ElMessage } from "element-plus";
import { requestPasswordRecovery } from "../../api/auth";
import type { ApiError } from "../../api/http";
import ImageCaptcha from "./ImageCaptcha.vue";

const visible = defineModel<boolean>({ required: true });
const username = ref("");
const captchaCode = ref("");
const challengeId = ref("");
const captcha = ref<InstanceType<typeof ImageCaptcha>>();
const usernameInput = ref<{ focus: () => void }>();
const loading = ref(false);

watch(visible, async (value) => {
  if (!value) return;
  await nextTick();
  usernameInput.value?.focus();
});

async function submit() {
  if (
    !username.value.trim() ||
    !captchaCode.value.trim() ||
    !challengeId.value ||
    loading.value
  )
    return;
  loading.value = true;
  try {
    const result = await requestPasswordRecovery({
      username: username.value.trim(),
      challengeId: challengeId.value,
      captchaCode: captchaCode.value,
    });
    ElMessage.success(result.message);
    visible.value = false;
  } catch (error) {
    ElMessage.warning(
      (error as ApiError).message || "提交失败，请刷新验证码后重试。",
    );
    captchaCode.value = "";
    challengeId.value = "";
    await captcha.value?.refresh();
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <el-dialog
    v-model="visible"
    class="password-recovery-dialog"
    modal-class="password-recovery-modal"
    width="min(520px, calc(100vw - 24px))"
    append-to-body
    align-center
    destroy-on-close
    lock-scroll
    close-on-click-modal
    close-on-press-escape
    aria-label="忘记密码"
  >
    <template #header>
      <div class="password-recovery-heading">
        <span>忘记密码</span>
        <small>提交安全恢复申请</small>
      </div>
    </template>
    <div class="password-recovery-body">
      <el-alert
        title="请勿在此输入旧密码或其他账号凭据"
        type="info"
        :closable="false"
        show-icon
      />
      <el-form label-position="top" @submit.prevent="submit">
        <el-form-item label="用户名">
          <el-input
            ref="usernameInput"
            v-model="username"
            maxlength="64"
            autocomplete="username"
            placeholder="请输入登录用户名"
          />
        </el-form-item>
        <el-form-item label="图形验证码">
          <ImageCaptcha
            ref="captcha"
            v-model="captchaCode"
            @challenge="challengeId = $event"
          />
        </el-form-item>
        <el-button
          class="password-recovery-submit"
          type="primary"
          native-type="submit"
          :loading="loading"
          :disabled="!username.trim() || !captchaCode.trim() || !challengeId"
        >
          {{ loading ? "正在提交…" : "提交恢复申请" }}
        </el-button>
      </el-form>
      <p class="password-recovery-safe-note">
        申请不会立即修改密码；处理结果会显示在登录页通知中。
      </p>
    </div>
  </el-dialog>
</template>

<style>
.password-recovery-modal {
  position: fixed !important;
  inset: 0 !important;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 12px;
  overflow: hidden;
}
.password-recovery-dialog {
  margin: 0 !important;
  max-height: calc(100dvh - 24px);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border-radius: 24px !important;
}
.password-recovery-dialog .el-dialog__header {
  flex: 0 0 auto;
  margin: 0;
  padding: 22px 24px 12px;
}
.password-recovery-dialog .el-dialog__body {
  min-height: 0;
  overflow-y: auto;
  padding: 8px 24px 24px;
  overscroll-behavior: contain;
}
.password-recovery-heading {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 21px;
  font-weight: 760;
}
.password-recovery-heading small {
  font-size: 13px;
  font-weight: 500;
  color: var(--el-text-color-secondary);
}
.password-recovery-body {
  display: grid;
  gap: 16px;
}
.password-recovery-safe-note {
  margin: 0;
  color: var(--el-text-color-secondary);
  line-height: 1.65;
  font-size: 12px;
}
.password-recovery-submit {
  width: 100%;
  min-height: 44px;
  margin-top: 2px;
}
@media (max-width: 480px), (max-height: 700px) {
  .password-recovery-dialog .el-dialog__header {
    padding: 17px 18px 9px;
  }
  .password-recovery-dialog .el-dialog__body {
    padding: 6px 18px 18px;
  }
  .password-recovery-body {
    gap: 12px;
  }
}
</style>
