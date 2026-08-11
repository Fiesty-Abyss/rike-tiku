<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import gsap from 'gsap'
import { ScrollTrigger } from 'gsap/ScrollTrigger'
import physicsVisual from '../assets/portal/physics-optical-field.webp'
import chemistryVisual from '../assets/portal/chemistry-glass-spectrum.webp'
import biologyVisual from '../assets/portal/biology-living-network.webp'

gsap.registerPlugin(ScrollTrigger)

const root = ref<HTMLElement>()
let context: gsap.Context | undefined
let motion: gsap.MatchMedia | undefined

onMounted(() => {
  if (!root.value) return
  motion = gsap.matchMedia()
  context = gsap.context(() => {
    motion?.add('(prefers-reduced-motion: no-preference)', () => {
      gsap.timeline({ defaults: { ease: 'power3.out' } })
        .from('.portal-nav', { autoAlpha: 0, y: -8, duration: 0.32 })
        .from('.portal-hero-copy > *', { autoAlpha: 0, y: 14, duration: 0.42, stagger: 0.07 }, '-=0.12')
        .from('.portal-system-visual', { autoAlpha: 0, scale: 0.985, duration: 0.52 }, '-=0.28')

      gsap.utils.toArray<HTMLElement>('.portal-subject').forEach((section) => {
        const copy = section.querySelector('.portal-subject-copy')
        const visual = section.querySelector('.portal-subject-visual')
        const ambient = section.querySelector('.subject-visual__ambient')
        gsap.from([copy, visual], {
          autoAlpha: 0,
          y: 18,
          duration: 0.56,
          stagger: 0.09,
          ease: 'power3.out',
          scrollTrigger: { trigger: section, start: 'top 78%', once: true },
        })
        if (ambient) {
          const drift = gsap.to(ambient, { xPercent: 8, duration: 7, repeat: -1, yoyo: true, ease: 'sine.inOut', paused: true })
          ScrollTrigger.create({
            trigger: section,
            start: 'top bottom',
            end: 'bottom top',
            onEnter: () => drift.play(),
            onEnterBack: () => drift.play(),
            onLeave: () => drift.pause(),
            onLeaveBack: () => drift.pause(),
          })
        }
      })
    })

    motion?.add('(prefers-reduced-motion: reduce)', () => {
      gsap.set('.portal-nav, .portal-hero-copy > *, .portal-system-visual, .portal-subject-copy, .portal-subject-visual', { clearProps: 'all' })
    })
  }, root.value)
})

onBeforeUnmount(() => {
  context?.revert()
  motion?.revert()
})
</script>

<template>
  <div ref="root" class="portal-page">
    <!--
      THESIS: RIKE is a clear scientific learning path, not a generic three-card portal.
      OWN-WORLD: quartz paper, optical water, one restrained subject accent per chapter, authored science imagery.
      STORY: understand the system, encounter three distinct disciplines, then enter the practice-feedback loop.
      FIRST VIEWPORT: concise product copy at left; one cross-discipline optical instrument at right; login stays visible.
      FORM: Split Studio, user-pinned mizuiro-aero direction, key RIKE-PR27-R3.
      FINISH: unreviewed and undocumented is unfinished; this build ends with the finish review, the verdict, and DESIGN.md
    -->
    <header class="portal-nav" aria-label="公共门户导航">
      <router-link class="portal-wordmark" to="/" aria-label="RIKE 公共首页">
        <span class="portal-mark" aria-hidden="true"><i></i></span>
        <span><strong>RIKE</strong><small>理科学习辅助系统</small></span>
      </router-link>
      <router-link data-testid="portal-login" class="portal-nav-login" to="/login">登录</router-link>
    </header>

    <main>
      <section class="portal-hero" aria-labelledby="portal-title">
        <div class="portal-hero-copy">
          <h1 id="portal-title">RIKE 理科学习辅助系统</h1>
          <p class="portal-subtitle">高中物理、化学、生物练习与学习管理。</p>
          <div class="portal-actions">
            <router-link class="portal-primary-action" to="/login">进入系统</router-link>
            <a class="portal-secondary-action" href="#physics">查看三科学习</a>
          </div>
        </div>
        <div class="portal-system-visual" role="img" aria-label="波动、分子键与叶脉共同构成的三科学习路径">
          <svg viewBox="0 0 720 560" aria-hidden="true">
            <defs>
              <linearGradient id="portal-glass" x1="0" y1="0" x2="1" y2="1">
                <stop offset="0" stop-color="currentColor" stop-opacity=".1" />
                <stop offset="1" stop-color="currentColor" stop-opacity=".02" />
              </linearGradient>
            </defs>
            <path class="system-glass" d="M118 96C203 42 335 54 421 113c88 60 176 60 215 143 39 82-1 193-93 243-98 53-241 42-328-28C118 393 39 337 54 236c9-61 28-111 64-140Z" />
            <path class="system-wave" d="M84 284c42-83 83 83 125 0s83 83 125 0 83 83 125 0 83 83 125 0" />
            <path class="system-path" d="M103 387c113-136 281-155 500-61" />
            <g class="system-molecule">
              <path d="m255 174 70-34 67 45 72-24 65 47" />
              <circle cx="255" cy="174" r="12" /><circle cx="325" cy="140" r="15" /><circle cx="392" cy="185" r="11" /><circle cx="464" cy="161" r="14" /><circle cx="529" cy="208" r="10" />
            </g>
            <g class="system-vein">
              <path d="M239 442c99-18 194-63 285-137" />
              <path d="m306 423-30-42m93 15-20-54m83 22-4-56m57 18 22-49" />
            </g>
            <circle class="system-node system-node--physics" cx="103" cy="387" r="9" />
            <circle class="system-node system-node--chemistry" cx="334" cy="284" r="9" />
            <circle class="system-node system-node--biology" cx="603" cy="326" r="9" />
          </svg>
        </div>
      </section>

      <section id="physics" class="portal-subject portal-subject--physics" data-subject="physics" aria-labelledby="physics-title">
        <div class="portal-subject-copy">
          <h2 id="physics-title">物理</h2>
          <p>从受力、运动和场的关系出发，在练习中保留条件、单位与推导过程。</p>
          <ul aria-label="物理学习内容"><li>力学</li><li>电磁学</li><li>光学与热学</li></ul>
        </div>
        <figure class="portal-subject-visual">
          <span class="subject-visual__ambient" aria-hidden="true"></span>
          <img :src="physicsVisual" width="1600" height="1000" loading="eager" fetchpriority="high" decoding="async" alt="透明光学介质中的钴蓝波动、场线与运动轨迹" />
        </figure>
      </section>

      <section class="portal-subject portal-subject--chemistry" data-subject="chemistry" aria-labelledby="chemistry-title">
        <div class="portal-subject-copy">
          <h2 id="chemistry-title">化学</h2>
          <p>把物质组成、反应条件和实验现象放在同一条证据链中理解。</p>
          <ul aria-label="化学学习内容"><li>物质的量</li><li>元素化合物</li><li>实验与反应原理</li></ul>
        </div>
        <figure class="portal-subject-visual">
          <span class="subject-visual__ambient" aria-hidden="true"></span>
          <img :src="chemistryVisual" width="1600" height="1000" loading="lazy" decoding="async" alt="日光下的玻璃器皿、梅紫液面、分子结构与光谱折射" />
        </figure>
      </section>

      <section class="portal-subject portal-subject--biology" data-subject="biology" aria-labelledby="biology-title">
        <div class="portal-subject-copy">
          <h2 id="biology-title">生物</h2>
          <p>沿着结构、功能和证据层级，连接细胞、遗传与生态系统。</p>
          <ul aria-label="生物学习内容"><li>分子与细胞</li><li>遗传与进化</li><li>稳态与生态</li></ul>
        </div>
        <figure class="portal-subject-visual">
          <span class="subject-visual__ambient" aria-hidden="true"></span>
          <img :src="biologyVisual" width="1600" height="1000" loading="lazy" decoding="async" alt="叶脉、细胞膜、遗传双螺旋与生态网络融合的生命结构" />
        </figure>
      </section>

      <section class="portal-loop" aria-labelledby="loop-title">
        <header>
          <h2 id="loop-title">一次练习，形成完整反馈。</h2>
          <p>练习事实冻结后，判分、错题与标准解析保持同一条可复验链路。</p>
        </header>
        <ol class="portal-loop-steps">
          <li><span>1</span><div><h3>练习</h3><p>按学科、知识点、题型和难度创建题组。</p></div></li>
          <li><span>2</span><div><h3>判分</h3><p>客观题使用确定性规则即时判定。</p></div></li>
          <li><span>3</span><div><h3>错题</h3><p>错误答案进入本人错题记录并持续更新。</p></div></li>
          <li><span>4</span><div><h3>解析</h3><p>查看冻结答案、逐项说明与结构化标准解析。</p></div></li>
          <li><span>5</span><div><h3>再练习</h3><p>回到相同知识点，验证是否真正掌握。</p></div></li>
        </ol>
        <dl class="portal-facts" aria-label="演示内容规模">
          <div><dt>学科</dt><dd>3</dd></div>
          <div><dt>自动练习题</dt><dd>360</dd></div>
          <div><dt>专题综合题</dt><dd>18</dd></div>
        </dl>
        <div class="portal-entrance">
          <div><h2>从真实账号开始体验。</h2><p>登录后按角色进入管理员、教师或学生工作台。</p></div>
          <router-link class="portal-primary-action" to="/login">前往登录</router-link>
        </div>
      </section>
    </main>

    <footer class="portal-footer">
      <p>练习、判分、错题、解析与再练习，在同一套冻结事实中完成。</p>
      <div><strong>RIKE</strong><span>本科毕业设计 · 非 AI 基础能力</span></div>
    </footer>
  </div>
</template>

<style src="../styles/portal.css"></style>
