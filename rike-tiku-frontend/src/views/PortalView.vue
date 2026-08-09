<script setup lang="ts">
const learningLoop = [
  { title: '练习', copy: '按学科、知识点、题型和难度创建练习。' },
  { title: '判分', copy: '单选、多选、填空由确定性规则自动判分。' },
  { title: '错题', copy: '错误记录持续保留，支持后续复习。' },
  { title: '标准解析', copy: '以 STANDARD 答案和解析作为权威事实。' },
  { title: '掌握度 / 推荐', copy: '依据真实答题记录计算并给出规则推荐。' },
  { title: '再练习', copy: '从薄弱知识点重新进入练习闭环。' },
]

const availableCapabilities = [
  ['在线练习与自动判分', '围绕高中物理、化学、生物开展条件练习与随机练习。'],
  ['错题与标准解析', '提交后查看本人答案、正确答案、知识点和标准解析。'],
  ['知识点掌握与规则推荐', '使用已提交答题事实计算掌握状态，不依赖 AI。'],
  ['教师和教学组织管理', '支持班级、学生、教师、三元任课范围、学情与师生私信。'],
]

const subjects = [
  { code: 'PHYSICS', name: '物理', copy: '从力学、电磁学等知识点进入练习，以清晰步骤核对概念与计算。' },
  { code: 'CHEMISTRY', name: '化学', copy: '围绕物质结构、反应原理等内容训练规范表达与条件判断。' },
  { code: 'BIOLOGY', name: '生物', copy: '通过生命活动规律与实验情境，巩固概念辨析和信息提取。' },
]

const roles = [
  ['学生', '练习、结果、错题、掌握度、规则推荐与师生私信'],
  ['教师', '任课范围、班级学生、高频考点、班级学情与师生私信'],
  ['管理员', '教学组织、账号、题库导入、审核和发布管理'],
]
</script>

<template>
  <div class="portal-page">
    <header class="portal-nav" aria-label="公共门户导航">
      <RouterLink class="portal-wordmark" to="/" aria-label="返回公共首页">
        <span class="portal-mark" aria-hidden="true">理科</span>
        <span class="portal-wordmark-full">在线题库实训管理系统</span>
        <span class="portal-wordmark-short">题库系统</span>
      </RouterLink>
      <RouterLink class="portal-nav-login" to="/login">统一登录</RouterLink>
    </header>

    <main>
      <section class="portal-hero" aria-labelledby="portal-title">
        <div class="portal-hero-copy portal-enter" style="--portal-order: 0">
          <h1 id="portal-title">集成大模型智能答疑的在线题库实训管理系统</h1>
          <p class="portal-subtitle">面向高中物理、化学、生物的在线题库实训与学习辅助系统</p>
          <p class="portal-ai-boundary">
            <strong>能力边界</strong>
            当前运行时 AI 智能答疑尚未上线；现阶段以标准答案与标准解析为权威。
          </p>
          <div class="portal-actions">
            <RouterLink data-testid="portal-login" class="portal-primary-action" to="/login">
              进入统一登录
              <span class="action-arrow" aria-hidden="true"></span>
            </RouterLink>
            <a class="portal-secondary-action" href="#learning-loop">查看学习闭环</a>
          </div>
        </div>

        <dl class="portal-scope portal-enter" style="--portal-order: 1" aria-label="系统范围">
          <div><dt>学科范围</dt><dd>物理 · 化学 · 生物</dd></div>
          <div><dt>自动判分</dt><dd>单选 · 多选 · 填空</dd></div>
          <div><dt>使用角色</dt><dd>学生 · 教师 · 管理员</dd></div>
          <div><dt>事实边界</dt><dd>标准答案与标准解析始终权威</dd></div>
        </dl>
      </section>

      <section id="learning-loop" class="portal-section learning-section" aria-labelledby="learning-title">
        <header class="portal-section-heading">
          <h2 id="learning-title">一条可重复验证的学习闭环</h2>
          <p>每一步都来自系统中的真实练习数据；规则推荐不会改写标准答案或标准解析。</p>
        </header>
        <ol class="learning-map" aria-label="练习到再练习的学习闭环">
          <li v-for="(step, index) in learningLoop" :key="step.title">
            <span class="learning-index" aria-hidden="true">{{ String(index + 1).padStart(2, '0') }}</span>
            <h3>{{ step.title }}</h3>
            <p>{{ step.copy }}</p>
          </li>
        </ol>
        <p class="loop-return"><span class="loop-return-mark" aria-hidden="true"></span> 完成一次练习后，可从薄弱知识点再次进入练习。</p>
      </section>

      <section class="portal-section capability-section" aria-labelledby="capability-title">
        <header class="portal-section-heading capability-heading">
          <h2 id="capability-title">当前可用的非 AI 能力</h2>
          <p>首页只介绍已经进入系统并有测试证据的功能，不展示虚构指标。</p>
        </header>
        <dl class="capability-list">
          <div v-for="capability in availableCapabilities" :key="capability[0]">
            <dt>{{ capability[0] }}</dt>
            <dd>{{ capability[1] }}</dd>
          </div>
        </dl>
        <aside class="ai-plan-note" aria-label="AI 能力边界">
          <strong>AI 智能答疑：后续能力规划</strong>
          <p>当前系统尚未实现运行时 AI 答疑。AI 不可用时，登录、题库、练习、判分、错题和标准解析仍应正常工作。</p>
        </aside>
      </section>

      <section class="portal-section subject-section" aria-labelledby="subject-title">
        <header class="portal-section-heading">
          <h2 id="subject-title">三科学习入口，共用同一套可靠规则</h2>
          <p>当前正式自动判分题型为单选、多选和填空；综合题不进行自动评分。</p>
        </header>
        <div class="subject-grid">
          <article v-for="subject in subjects" :key="subject.code">
            <span class="subject-code">{{ subject.code }}</span>
            <h3>{{ subject.name }}</h3>
            <p>{{ subject.copy }}</p>
            <small>单选 · 多选 · 填空</small>
          </article>
        </div>
      </section>

      <section class="portal-section role-section" aria-labelledby="role-title">
        <header class="portal-section-heading role-heading">
          <h2 id="role-title">同一登录入口，按真实角色进入工作台</h2>
          <p>角色来自后台账号配置；公共门户不会授予、切换或推测用户权限。</p>
        </header>
        <dl class="role-list">
          <div v-for="role in roles" :key="role[0]">
            <dt>{{ role[0] }}</dt>
            <dd>{{ role[1] }}</dd>
          </div>
        </dl>
      </section>

      <section class="portal-close" aria-labelledby="portal-close-title">
        <div>
          <h2 id="portal-close-title">从一次清晰的练习开始。</h2>
          <p>使用管理员发放的账号，经统一入口进入对应工作台。</p>
        </div>
        <RouterLink class="portal-primary-action" to="/login">进入统一登录 <span class="action-arrow" aria-hidden="true"></span></RouterLink>
      </section>
    </main>

    <footer class="portal-footer">
      <span>集成大模型智能答疑的在线题库实训管理系统</span>
      <span>高中物理 · 化学 · 生物</span>
    </footer>
  </div>
</template>

<style src="../../portal-tokens.css"></style>

<style scoped>
.portal-page {
  min-height: 100dvh;
  color: var(--color-portal-ink);
  background: var(--color-portal-paper);
  font-family: var(--font-portal-body);
  font-size: var(--text-portal-base);
  line-height: 1.65;
}

.portal-nav,
.portal-hero,
.portal-section,
.portal-close,
.portal-footer {
  width: min(100% - 2rem, 76rem);
  margin-inline: auto;
}

.portal-nav {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-portal-md);
  min-height: 4.5rem;
  border-bottom: var(--rule-portal) solid var(--color-portal-rule);
}

.portal-wordmark,
.portal-nav-login,
.portal-primary-action,
.portal-secondary-action {
  white-space: nowrap;
}

.portal-wordmark {
  display: inline-flex;
  align-items: center;
  gap: var(--space-portal-sm);
  min-width: 0;
  color: var(--color-portal-ink);
  font-family: var(--font-portal-display);
  font-weight: 700;
  line-height: 1.25;
  text-decoration: none;
}

.portal-wordmark-short {
  display: none;
}

.portal-mark {
  display: grid;
  flex: 0 0 auto;
  place-items: center;
  width: 2.5rem;
  height: 2.5rem;
  border: var(--rule-portal) solid var(--color-portal-accent);
  border-radius: var(--radius-portal-control);
  color: var(--color-portal-accent-deep);
  font-size: var(--text-portal-sm);
}

.portal-nav-login,
.portal-secondary-action {
  color: var(--color-portal-accent-deep);
  font-weight: 700;
  text-underline-offset: 0.3em;
}

.portal-nav-login {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 2.75rem;
  padding-inline: var(--space-portal-md);
  border: var(--rule-portal) solid var(--color-portal-rule-strong);
  border-radius: var(--radius-portal-control);
  text-decoration: none;
  transition: color var(--dur-portal-micro) var(--ease-portal-out), border-color var(--dur-portal-micro) var(--ease-portal-out), transform var(--dur-portal-micro) var(--ease-portal-out);
}

.portal-hero {
  display: grid;
  gap: var(--space-portal-2xl);
  padding-block: var(--space-portal-2xl) var(--space-portal-3xl);
}

.portal-hero-copy {
  min-width: 0;
}

.portal-hero h1 {
  max-width: 16ch;
  margin: 0;
  color: var(--color-portal-ink);
  font-family: var(--font-portal-display);
  font-size: var(--text-portal-display);
  font-style: normal;
  font-weight: 700;
  letter-spacing: -0.035em;
  line-height: 1.16;
  overflow-wrap: anywhere;
}

.portal-subtitle {
  max-width: 38rem;
  margin: var(--space-portal-lg) 0 0;
  color: var(--color-portal-ink-soft);
  font-size: var(--text-portal-md);
}

.portal-ai-boundary {
  max-width: 66ch;
  margin: var(--space-portal-md) 0 0;
  color: var(--color-portal-ink-soft);
  font-size: var(--text-portal-sm);
}

.portal-ai-boundary strong {
  margin-inline-end: var(--space-portal-xs);
  color: var(--color-portal-plan-ink);
}

.portal-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-portal-md);
  margin-top: var(--space-portal-xl);
}

.portal-primary-action {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-portal-sm);
  min-height: 3rem;
  padding-inline: var(--space-portal-lg);
  border: var(--rule-portal) solid var(--color-portal-accent);
  border-radius: var(--radius-portal-control);
  color: var(--color-portal-accent-ink);
  background: var(--color-portal-accent);
  font-weight: 700;
  text-decoration: none;
  transition: background-color var(--dur-portal-micro) var(--ease-portal-out), transform var(--dur-portal-micro) var(--ease-portal-out);
}

.action-arrow {
  width: 0.5rem;
  height: 0.5rem;
  border-block-start: 0.125rem solid currentColor;
  border-inline-end: 0.125rem solid currentColor;
  transform: rotate(45deg);
}

.portal-secondary-action {
  display: inline-flex;
  align-items: center;
  min-height: 3rem;
  padding-inline: var(--space-portal-xs);
}

.portal-scope {
  display: grid;
  margin: 0;
  border-top: var(--rule-portal) solid var(--color-portal-rule-strong);
}

.portal-scope div {
  display: grid;
  grid-template-columns: minmax(6rem, 0.7fr) minmax(0, 1.4fr);
  gap: var(--space-portal-md);
  padding-block: var(--space-portal-md);
  border-bottom: var(--rule-portal) solid var(--color-portal-rule);
}

.portal-scope dt,
.capability-list dt,
.role-list dt {
  color: var(--color-portal-muted);
  font-size: var(--text-portal-sm);
  font-weight: 700;
}

.portal-scope dd,
.capability-list dd,
.role-list dd {
  margin: 0;
  color: var(--color-portal-ink-soft);
}

.portal-section {
  padding-block: var(--space-portal-3xl);
  border-top: var(--rule-portal) solid var(--color-portal-rule);
}

.portal-section-heading {
  display: grid;
  gap: var(--space-portal-md);
  margin-bottom: var(--space-portal-xl);
}

.portal-section-heading h2,
.portal-close h2 {
  margin: 0;
  color: var(--color-portal-ink);
  font-family: var(--font-portal-display);
  font-size: clamp(1.75rem, 3vw, 2.75rem);
  font-style: normal;
  font-weight: 700;
  letter-spacing: -0.03em;
  line-height: 1.2;
  overflow-wrap: anywhere;
}

.portal-section-heading p,
.portal-close p {
  max-width: 44rem;
  margin: 0;
  color: var(--color-portal-muted);
}

.learning-map {
  display: grid;
  gap: 0;
  margin: 0;
  padding: 0;
  border-block: var(--rule-portal) solid var(--color-portal-rule-strong);
  list-style: none;
}

.learning-map li {
  position: relative;
  min-width: 0;
  padding: var(--space-portal-lg) var(--space-portal-md);
  border-bottom: var(--rule-portal) solid var(--color-portal-rule);
}

.learning-map li:last-child {
  border-bottom: 0;
}

.learning-index,
.subject-code {
  color: var(--color-portal-accent);
  font-family: var(--font-portal-display);
  font-size: var(--text-portal-sm);
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.learning-map h3,
.subject-grid h3 {
  margin: var(--space-portal-xs) 0 0;
  color: var(--color-portal-ink);
  font-family: var(--font-portal-display);
  font-size: var(--text-portal-lg);
  font-style: normal;
}

.learning-map p,
.subject-grid p {
  margin: var(--space-portal-sm) 0 0;
  color: var(--color-portal-ink-soft);
}

.loop-return {
  display: flex;
  align-items: center;
  gap: var(--space-portal-sm);
  margin: var(--space-portal-lg) 0 0;
  color: var(--color-portal-accent-deep);
  font-weight: 700;
}

.loop-return-mark {
  position: relative;
  width: 1rem;
  height: 0.75rem;
  border-block-end: 0.125rem solid currentColor;
  border-inline-start: 0.125rem solid currentColor;
  border-radius: 0 0 0 0.375rem;
}

.loop-return-mark::before {
  position: absolute;
  inset-block-start: 0.25rem;
  inset-inline-start: -0.1875rem;
  width: 0.375rem;
  height: 0.375rem;
  content: '';
  border-block-start: 0.125rem solid currentColor;
  border-inline-start: 0.125rem solid currentColor;
  transform: rotate(-45deg);
}

.capability-list,
.role-list {
  margin: 0;
  border-top: var(--rule-portal) solid var(--color-portal-rule-strong);
}

.capability-list div,
.role-list div {
  display: grid;
  gap: var(--space-portal-sm);
  padding-block: var(--space-portal-lg);
  border-bottom: var(--rule-portal) solid var(--color-portal-rule);
}

.capability-list dt,
.role-list dt {
  color: var(--color-portal-ink);
  font-size: var(--text-portal-md);
}

.ai-plan-note {
  display: grid;
  gap: var(--space-portal-xs);
  margin-top: var(--space-portal-xl);
  padding: var(--space-portal-lg);
  border: var(--rule-portal) solid var(--color-portal-rule-strong);
  border-radius: var(--radius-portal-panel);
  color: var(--color-portal-plan-ink);
  background: var(--color-portal-plan);
}

.ai-plan-note p {
  max-width: 54rem;
  margin: 0;
}

.subject-grid {
  display: grid;
  border-block: var(--rule-portal) solid var(--color-portal-rule-strong);
}

.subject-grid article {
  position: relative;
  min-width: 0;
  padding: var(--space-portal-xl) 0;
  border-bottom: var(--rule-portal) solid var(--color-portal-rule);
}

.subject-grid article:last-child {
  border-bottom: 0;
}

.subject-grid small {
  display: block;
  margin-top: var(--space-portal-lg);
  color: var(--color-portal-muted);
  font-size: var(--text-portal-sm);
}

.portal-close {
  display: grid;
  gap: var(--space-portal-xl);
  align-items: end;
  padding-block: var(--space-portal-3xl);
  border-top: var(--rule-portal) solid var(--color-portal-rule-strong);
}

.portal-close p {
  margin-top: var(--space-portal-md);
}

.portal-close .portal-primary-action {
  justify-self: start;
}

.portal-footer {
  display: grid;
  gap: var(--space-portal-xs);
  padding-block: var(--space-portal-lg) var(--space-portal-xl);
  border-top: var(--rule-portal) solid var(--color-portal-rule);
  color: var(--color-portal-muted);
  font-size: var(--text-portal-sm);
}

.portal-enter {
  animation: portal-enter var(--dur-portal-long) var(--ease-portal-out) both;
  animation-delay: calc(var(--portal-order, 0) * 70ms);
}

@keyframes portal-enter {
  from { opacity: 0; transform: translateY(0.5rem); }
  to { opacity: 1; transform: translateY(0); }
}

.portal-nav-login:hover,
.portal-nav-login:focus-visible {
  color: var(--color-portal-accent);
  border-color: var(--color-portal-accent);
}

.portal-primary-action:hover {
  background: var(--color-portal-accent-deep);
  transform: translateY(-1px);
}

.portal-nav-login:active,
.portal-primary-action:active {
  transform: translateY(1px);
}

.portal-wordmark:focus-visible,
.portal-nav-login:focus-visible,
.portal-primary-action:focus-visible,
.portal-secondary-action:focus-visible {
  outline: 3px solid var(--color-portal-focus);
  outline-offset: 3px;
}

@media (min-width: 48rem) {
  .portal-nav,
  .portal-hero,
  .portal-section,
  .portal-close,
  .portal-footer {
    width: min(100% - 4rem, 76rem);
  }

  .portal-hero {
    grid-template-columns: minmax(0, 1.45fr) minmax(18rem, 0.75fr);
    align-items: end;
    padding-block: var(--space-portal-3xl);
  }

  .portal-section-heading {
    grid-template-columns: minmax(0, 1fr) minmax(18rem, 0.7fr);
    align-items: end;
  }

  .learning-map {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .learning-map li {
    border-right: var(--rule-portal) solid var(--color-portal-rule);
  }

  .learning-map li:nth-child(3n) {
    border-right: 0;
  }

  .learning-map li:nth-child(n + 4) {
    border-bottom: 0;
  }

  .capability-list div,
  .role-list div {
    grid-template-columns: minmax(13rem, 0.7fr) minmax(0, 1.3fr);
    gap: var(--space-portal-xl);
  }

  .subject-grid {
    grid-template-columns: 1.15fr 1fr 0.85fr;
  }

  .subject-grid article {
    padding-inline: var(--space-portal-lg);
    border-right: var(--rule-portal) solid var(--color-portal-rule);
    border-bottom: 0;
  }

  .subject-grid article:first-child {
    padding-inline-start: 0;
  }

  .subject-grid article:last-child {
    padding-inline-end: 0;
    border-right: 0;
  }

  .portal-close {
    grid-template-columns: minmax(0, 1fr) auto;
  }

  .portal-close .portal-primary-action {
    justify-self: end;
  }

  .portal-footer {
    grid-template-columns: minmax(0, 1fr) auto;
  }
}

@media (min-width: 72rem) {
  .learning-map {
    grid-template-columns: repeat(6, minmax(0, 1fr));
  }

  .learning-map li,
  .learning-map li:nth-child(n + 4) {
    border-bottom: 0;
  }

  .learning-map li:nth-child(3n) {
    border-right: var(--rule-portal) solid var(--color-portal-rule);
  }

  .learning-map li:last-child {
    border-right: 0;
  }
}

@media (max-width: 30rem) {
  .portal-wordmark-full {
    display: none;
  }

  .portal-wordmark-short {
    display: inline;
  }

  .portal-nav-login {
    padding-inline: var(--space-portal-sm);
  }
}

@media (hover: hover) and (pointer: fine) {
  .portal-secondary-action:hover {
    color: var(--color-portal-accent);
  }
}

@media (pointer: coarse) {
  .portal-nav-login,
  .portal-primary-action,
  .portal-secondary-action {
    min-height: 3rem;
  }
}

@media (prefers-reduced-motion: reduce) {
  .portal-enter {
    animation: none;
  }

  .portal-nav-login,
  .portal-primary-action {
    transition-duration: var(--dur-portal-micro);
  }
}
</style>
