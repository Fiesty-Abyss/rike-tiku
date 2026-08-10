<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'
import gsap from 'gsap'

const root = ref<HTMLElement>()
let motion: gsap.MatchMedia | undefined
let context: gsap.Context | undefined

const learningLoop = [
  { index: '01', title: '练习', copy: '按学科、知识点、题型与难度建立一场冻结题集。' },
  { index: '02', title: '判分', copy: '单选、多选、填空由确定性规则完成判分。' },
  { index: '03', title: '复盘', copy: '逐题核对本人答案、正确答案与 STANDARD 解析。' },
  { index: '04', title: '错题', copy: '错误事实即时进入错题本，并按真实学科隔离。' },
  { index: '05', title: '掌握度 / 推荐', copy: '根据答题和错题状态计算，不制造虚构学习指标。' },
  { index: '06', title: '再练习', copy: '从知识点或相似规则题重新进入学习回路。' },
]

const subjects = [
  { code: 'PHYSICS', name: '物理', className: 'physics', copy: '用轨迹、力与场，把抽象关系还原为可以验证的推理。', note: '运动 · 能量 · 电磁 · 实验' },
  { code: 'CHEMISTRY', name: '化学', className: 'chemistry', copy: '在结构、反应和条件变化之间，建立层次清楚的判断链。', note: '计量 · 反应 · 平衡 · 实验' },
  { code: 'BIOLOGY', name: '生物', className: 'biology', copy: '从细胞到生态系统，以稳态、信息与证据理解生命过程。', note: '细胞 · 遗传 · 调节 · 生态' },
]

const roles = [
  { number: 'A', title: '学生学习舱', copy: '练习、逐题复盘、错题、掌握度、规则推荐和 Topic18 专题阅读。' },
  { number: 'T', title: '教师分析室', copy: '围绕真实任课范围查看学生、学情、高频考点与师生私信。' },
  { number: 'M', title: '管理员控制台', copy: '维护教学组织、账号、题库状态、附件与高风险操作审计。' },
]

onMounted(() => {
  if (!root.value || typeof window.matchMedia !== 'function') return
  motion = gsap.matchMedia()
  context = gsap.context(() => {
    motion?.add('(prefers-reduced-motion: no-preference)', () => {
      gsap.timeline({ defaults: { ease: 'power3.out' } })
        .from('.portal-nav', { autoAlpha: 0, y: -16, duration: 0.55 })
        .from('.portal-hero-copy > *', { autoAlpha: 0, y: 24, duration: 0.7, stagger: 0.08 }, '-=0.2')
        .from('.optical-instrument', { autoAlpha: 0, scale: 0.96, duration: 0.85 }, '-=0.55')

      gsap.to('.optical-core', { y: -8, duration: 3.8, repeat: -1, yoyo: true, ease: 'sine.inOut' })
      gsap.utils.toArray<HTMLElement>('.portal-reveal').forEach((section) => {
        gsap.from(section, {
          autoAlpha: 0,
          y: 34,
          duration: 0.75,
          ease: 'power3.out',
          scrollTrigger: { trigger: section, start: 'top 86%', once: true },
        })
      })
    })
    motion?.add('(prefers-reduced-motion: reduce)', () => {
      gsap.set('.portal-nav, .portal-hero-copy > *, .optical-instrument, .portal-reveal', { clearProps: 'all' })
    })
  }, root.value)
})

onUnmounted(() => {
  motion?.revert()
  context?.revert()
})
</script>

<template>
  <div ref="root" class="portal-page">
    <header class="portal-nav" aria-label="公共门户导航">
      <RouterLink class="portal-wordmark" to="/" aria-label="返回公共首页">
        <span class="portal-mark" aria-hidden="true"><i></i></span>
        <span><strong>RIKE</strong><small>理科学习辅助系统</small></span>
      </RouterLink>
      <div class="portal-nav-status"><span>NON-AI FOUNDATION</span><RouterLink class="portal-nav-login" to="/login">统一登录</RouterLink></div>
    </header>

    <main>
      <section class="portal-hero" aria-labelledby="portal-title">
        <div class="portal-hero-copy">
          <p class="portal-overline">AQUA LIMINAL LEARNING SYSTEM · 2026</p>
          <h1 id="portal-title">在安静而清晰的界面里，<span>完成一次真正闭环的理科学习。</span></h1>
          <p class="portal-subtitle">集成大模型智能答疑的在线题库实训管理系统，面向高中物理、化学、生物的练习、复盘与教学组织。</p>
          <p class="portal-ai-boundary"><strong>能力边界</strong> 当前运行时 AI 智能答疑尚未上线；现阶段以标准答案与标准解析为权威。</p>
          <div class="portal-actions">
            <RouterLink data-testid="portal-login" class="portal-primary-action" to="/login">进入统一登录 <span aria-hidden="true">↗</span></RouterLink>
            <a class="portal-secondary-action" href="#learning-loop">沿学习闭环向下</a>
          </div>
        </div>

        <div class="optical-instrument" aria-hidden="true">
          <div class="horizon-line"></div>
          <div class="optical-orbit orbit-one"></div>
          <div class="optical-orbit orbit-two"></div>
          <div class="optical-core"><span>RIKE</span><small>LEARN · VERIFY · RETURN</small></div>
          <span class="instrument-label label-a">03:42</span>
          <span class="instrument-label label-b">MIST / 18°C</span>
          <span class="instrument-label label-c">PH · CH · BI</span>
        </div>
      </section>

      <section id="learning-loop" class="portal-section portal-story portal-reveal" aria-labelledby="learning-title">
        <header class="portal-story-heading">
          <p class="portal-overline">LEARNING CURRENT</p>
          <h2 id="learning-title">一条可回到知识点的学习水流</h2>
          <p>学习不是把答案堆成列表。每次提交都留下可追溯事实，并自然指向下一次练习。</p>
        </header>
        <ol class="learning-current">
          <li v-for="step in learningLoop" :key="step.index">
            <span>{{ step.index }}</span><div><h3>{{ step.title }}</h3><p>{{ step.copy }}</p></div>
          </li>
        </ol>
      </section>

      <section class="portal-section science-section portal-reveal" aria-labelledby="science-title">
        <header class="portal-section-heading">
          <p class="portal-overline">THREE SCIENCES</p>
          <h2 id="science-title">三门学科，不是三张换色卡片</h2>
          <p>每个入口保留自己的思维气质；普通题库仍共享确定性判分与标准解析。</p>
        </header>
        <div class="science-compositions">
          <article v-for="subject in subjects" :key="subject.code" :class="`science-composition science-${subject.className}`">
            <div class="science-visual" aria-hidden="true"><i></i><i></i><i></i></div>
            <div><span>{{ subject.code }}</span><h3>{{ subject.name }}</h3><p>{{ subject.copy }}</p><small>{{ subject.note }}</small></div>
          </article>
        </div>
      </section>

      <section class="portal-section role-section portal-reveal" aria-labelledby="role-title">
        <header class="portal-section-heading">
          <p class="portal-overline">ROLE-NATIVE WORKSPACES</p>
          <h2 id="role-title">同一个 RIKE，不同的工作节奏</h2>
          <p>角色来自数据库配置；视觉结构帮助辨认当前任务，但绝不代替后端权限。</p>
        </header>
        <div class="role-runway">
          <article v-for="role in roles" :key="role.number"><span>{{ role.number }}</span><h3>{{ role.title }}</h3><p>{{ role.copy }}</p></article>
        </div>
      </section>

      <section class="portal-section boundary-section portal-reveal" aria-labelledby="boundary-title">
        <div><p class="portal-overline">PRODUCT TRUTH</p><h2 id="boundary-title">AI 智能答疑：后续能力规划</h2></div>
        <p>当前系统尚未实现运行时 AI 答疑。AI 不可用时，登录、题库、练习、判分、错题、标准解析、Topic18 与教学管理仍应正常工作。</p>
      </section>

      <section class="portal-entrance portal-reveal" aria-labelledby="entrance-title">
        <span class="entrance-horizon" aria-hidden="true"></span>
        <div><p class="portal-overline">GATE 01 · READY</p><h2 id="entrance-title">从一次清晰的练习开始。</h2><p>使用管理员发放的账号，通过图形验证码进入真实角色工作台。</p></div>
        <RouterLink class="portal-primary-action" to="/login">进入统一登录 <span aria-hidden="true">↗</span></RouterLink>
      </section>
    </main>

    <footer class="portal-footer"><span>RIKE · AQUA LIMINAL FUTURE</span><span>物理 · 化学 · 生物</span><span>标准答案与解析始终权威</span></footer>
  </div>
</template>

<style src="../styles/portal.css"></style>
