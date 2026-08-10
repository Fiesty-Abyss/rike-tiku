<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'
import gsap from 'gsap'

const root = ref<HTMLElement>()
let motion: gsap.MatchMedia | undefined
let context: gsap.Context | undefined

onMounted(() => {
  if (!root.value || typeof window.matchMedia !== 'function') return
  motion = gsap.matchMedia()
  context = gsap.context(() => {
    motion?.add('(prefers-reduced-motion: no-preference)', () => {
      gsap.timeline({ defaults: { ease: 'power3.out' } })
        .from('.portal-nav', { autoAlpha: 0, y: -14, duration: 0.45 })
        .from('.portal-hero-copy > *', { autoAlpha: 0, y: 22, duration: 0.62, stagger: 0.07 }, '-=0.14')
        .from('.optical-instrument', { autoAlpha: 0, scale: 0.97, duration: 0.72 }, '-=0.48')
    })
    motion?.add('(prefers-reduced-motion: reduce)', () => {
      gsap.set('.portal-nav, .portal-hero-copy > *, .optical-instrument', { clearProps: 'all' })
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
      <RouterLink class="portal-nav-login" to="/login">登录</RouterLink>
    </header>

    <main>
      <section class="portal-hero" aria-labelledby="portal-title">
        <div class="portal-hero-copy">
          <h1 id="portal-title">RIKE 理科学习辅助系统</h1>
          <p class="portal-subtitle">高中物理、化学、生物练习与学习管理</p>
          <div class="portal-actions">
            <RouterLink data-testid="portal-login" class="portal-primary-action" to="/login">登录</RouterLink>
            <a class="portal-secondary-action" href="#subjects">了解系统</a>
          </div>
        </div>

        <div class="optical-instrument" aria-hidden="true">
          <span class="horizon-line"></span>
          <span class="optical-arc optical-arc--wide"></span>
          <span class="optical-arc optical-arc--narrow"></span>
          <span class="optical-core"></span>
        </div>
      </section>

      <section id="subjects" class="portal-section science-section" aria-labelledby="science-title">
        <header class="portal-section-heading">
          <h2 id="science-title">三科学习</h2>
        </header>
        <div class="science-compositions">
          <article class="science-composition science-physics">
            <div class="science-visual">
              <svg viewBox="0 0 360 220" role="img" aria-label="物理轨迹、波和场线示意">
                <defs><linearGradient id="physics-wash" x1="0" x2="1"><stop stop-color="#dcecff"/><stop offset="1" stop-color="#9fc9f5" stop-opacity=".25"/></linearGradient></defs>
                <path class="science-fill" d="M18 184C83 177 103 48 174 52c64 4 78 120 168 116v42H18Z" fill="url(#physics-wash)"/>
                <path class="science-line science-line--strong" d="M22 179C86 170 106 44 174 48c66 4 82 119 167 112"/>
                <path class="science-line" d="M42 123c40-44 82-44 122 0s82 44 124 0"/>
                <path class="science-line" d="M42 139c40-44 82-44 122 0s82 44 124 0"/>
                <path class="science-vector" d="M82 170 154 90m0 0-7 19m7-19-20 3"/>
                <circle class="science-node" cx="174" cy="48" r="7"/>
              </svg>
            </div>
            <div><h3>物理</h3><p>运动、力与场</p></div>
          </article>

          <article class="science-composition science-chemistry">
            <div class="science-visual">
              <svg viewBox="0 0 360 220" role="img" aria-label="化学溶液、分子键和光谱示意">
                <defs><linearGradient id="chem-liquid" x1="0" y1="0" x2="0" y2="1"><stop stop-color="#c8c9f0" stop-opacity=".36"/><stop offset="1" stop-color="#8f85bd" stop-opacity=".68"/></linearGradient></defs>
                <path class="science-vessel" d="M108 24h78m-55 0v52l-58 103c-8 14 2 27 18 27h154c16 0 26-13 18-27L207 76V24"/>
                <path class="science-liquid" d="M91 153c32-16 56 9 88-3 26-10 50-4 78 12l17 30c4 8-1 14-11 14H81c-10 0-15-7-10-15Z" fill="url(#chem-liquid)"/>
                <path class="science-bond" d="m118 125 32-22 34 20 34-25"/>
                <circle class="science-atom" cx="118" cy="125" r="7"/><circle class="science-atom" cx="150" cy="103" r="9"/><circle class="science-atom" cx="184" cy="123" r="6"/><circle class="science-atom" cx="218" cy="98" r="8"/>
                <g class="science-spectrum"><path d="M286 58v58"/><path d="M300 48v68"/><path d="M314 69v47"/><path d="M328 38v78"/></g>
              </svg>
            </div>
            <div><h3>化学</h3><p>结构、反应与平衡</p></div>
          </article>

          <article class="science-composition science-biology">
            <div class="science-visual">
              <svg viewBox="0 0 360 220" role="img" aria-label="生物细胞膜、叶脉和信息网络示意">
                <path class="science-cell" d="M59 116c0-55 45-91 102-86 64 5 79 38 130 50 35 8 30 65 1 82-46 28-88 4-134 22-53 21-99-12-99-68Z"/>
                <path class="science-vein science-line--strong" d="M88 160c48-25 74-62 112-108"/>
                <path class="science-vein" d="m121 139-8-42m40 13 42-13m-17-18-3-34m-20 67-7 45m35-67 52 31"/>
                <g class="membrane-nodes"><circle cx="84" cy="78" r="7"/><circle cx="105" cy="66" r="7"/><circle cx="128" cy="57" r="7"/><circle cx="152" cy="51" r="7"/><circle cx="178" cy="50" r="7"/><circle cx="203" cy="54" r="7"/><circle cx="227" cy="62" r="7"/></g>
                <circle class="science-nucleus" cx="210" cy="124" r="27"/><path class="science-helix" d="M198 106c22 8 22 28 0 36m24-36c-22 8-22 28 0 36m-20-29h16m-16 18h16"/>
              </svg>
            </div>
            <div><h3>生物</h3><p>细胞、遗传与生态</p></div>
          </article>
        </div>
      </section>

      <section class="portal-snapshot" aria-label="系统数据概览">
        <div><strong>3</strong><span>个学科</span></div>
        <div><strong>360</strong><span>道自动练习题</span></div>
        <div><strong>18</strong><span>道专题综合题</span></div>
      </section>

      <section class="portal-entrance" aria-labelledby="entrance-title">
        <div><h2 id="entrance-title">进入 RIKE</h2><p>使用管理员发放的账号登录。</p></div>
        <RouterLink class="portal-primary-action" to="/login">登录系统</RouterLink>
      </section>
    </main>

    <footer class="portal-footer"><span>RIKE 理科学习辅助系统</span><span>物理 · 化学 · 生物</span><span>当前版本以标准答案与标准解析为准</span></footer>
  </div>
</template>

<style src="../styles/portal.css"></style>
