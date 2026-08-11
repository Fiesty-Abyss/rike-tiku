<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import gsap from 'gsap'
import { ScrollTrigger } from 'gsap/ScrollTrigger'
import aquaWorld from '../assets/aqua/rike-aqua-world.webp'
import physicsVisual from '../assets/aqua/physics-field-lab.webp'
import chemistryVisual from '../assets/aqua/chemistry-equilibrium-lab.webp'
import biologyVisual from '../assets/aqua/biology-living-system.webp'

gsap.registerPlugin(ScrollTrigger)

const root = ref<HTMLElement>()
const hero = ref<HTMLElement>()
const heroOptic = ref<HTMLElement>()
const physicsChapter = ref<HTMLElement>()
const physicsPin = ref<HTMLElement>()
const loopScene = ref<HTMLElement>()

let context: gsap.Context | undefined
let motion: gsap.MatchMedia | undefined
let pointerHandler: ((event: PointerEvent) => void) | undefined

onMounted(() => {
  if (!root.value) return
  document.documentElement.classList.add('aqua-motion-ready')
  motion = gsap.matchMedia()

  context = gsap.context(() => {
    motion?.add('(min-width: 64rem) and (prefers-reduced-motion: no-preference)', () => {
      let desktopPointerTarget: HTMLElement | undefined
      let desktopPointerHandler: ((event: PointerEvent) => void) | undefined

      gsap.timeline({ defaults: { ease: 'power3.out' } })
        .from('.portal-nav', { autoAlpha: 0, duration: 0.46 })
        .from('.portal-hero-kicker, .portal-hero h1, .portal-subtitle', { autoAlpha: 0, y: 28, duration: 0.62, stagger: 0.08 }, '-=0.16')
        .from('.portal-actions', { autoAlpha: 0, y: 16, duration: 0.44 }, '-=0.28')
        .from('.portal-hero-world', { autoAlpha: 0, scale: 1.035, duration: 1.05, ease: 'power2.out' }, '-=0.68')
        .from('.portal-hero-instrument', { autoAlpha: 0, scale: 0.88, rotate: -6, duration: 0.75 }, '-=0.62')

      if (hero.value) {
        gsap.timeline({
          scrollTrigger: {
            trigger: hero.value,
            start: 'top top',
            end: 'bottom top',
            scrub: 0.75,
          },
        })
          .to('.portal-hero-world', { yPercent: 13, scale: 1.08, ease: 'none' }, 0)
          .to('.portal-hero-copy', { yPercent: -16, autoAlpha: 0.24, ease: 'none' }, 0)
          .to('.portal-hero-instrument', { yPercent: -28, rotate: 8, ease: 'none' }, 0)
          .to('.portal-scroll-cue', { autoAlpha: 0, y: -12, ease: 'none' }, 0)
      }

      gsap.to(root.value, {
        '--portal-nav-opacity': 0.9,
        '--portal-nav-scale': 0.965,
        ease: 'none',
        scrollTrigger: {
          trigger: root.value,
          start: 'top top',
          end: '+=360',
          scrub: 0.6,
        },
      })

      if (physicsChapter.value && physicsPin.value) {
        gsap.timeline({
          scrollTrigger: {
            trigger: physicsChapter.value,
            start: 'top top',
            end: '+=2300',
            scrub: 0.82,
            pin: physicsPin.value,
            anticipatePin: 1,
            invalidateOnRefresh: true,
          },
        })
          .fromTo('.physics-wave-path', { strokeDashoffset: 720 }, { strokeDashoffset: 0, duration: 0.28, ease: 'none' }, 0.02)
          .fromTo('.physics-field-path', { strokeDashoffset: 560 }, { strokeDashoffset: 0, duration: 0.28, ease: 'none' }, 0.18)
          .to('.physics-lens', { xPercent: -24, scale: 1.08, duration: 0.34, ease: 'none' }, 0.22)
          .to('.physics-beam', { scaleX: 1, autoAlpha: 1, duration: 0.2, ease: 'none' }, 0.34)
          .to('.physics-readout--wave', { autoAlpha: 0, y: -28, duration: 0.16 }, 0.28)
          .fromTo('.physics-readout--field', { autoAlpha: 0, y: 34 }, { autoAlpha: 1, y: 0, duration: 0.18 }, 0.33)
          .to('.physics-readout--field', { autoAlpha: 0, y: -28, duration: 0.16 }, 0.56)
          .fromTo('.physics-readout--light', { autoAlpha: 0, y: 34 }, { autoAlpha: 1, y: 0, duration: 0.18 }, 0.61)
          .to('.physics-material-solid', { autoAlpha: 1, duration: 0.22, ease: 'none' }, 0.66)
          .to('.physics-spectrum', { scaleX: 1, autoAlpha: 1, duration: 0.26, ease: 'none' }, 0.72)
          .to('.physics-scene-media', { xPercent: -5, scale: 1.035, duration: 0.26, ease: 'none' }, 0.72)
      }

      const disciplines = gsap.utils.toArray<HTMLElement>('.portal-discipline')
      disciplines.forEach((section) => {
        const atmosphere = section.dataset.atmosphere
        const media = section.querySelector('.portal-discipline-media')
        const copy = section.querySelector('.portal-discipline-copy')
        if (atmosphere) {
          gsap.timeline({
            scrollTrigger: { trigger: section, start: 'top 84%', end: 'center 42%', scrub: 0.72 },
          })
            .to(`.portal-atmosphere--${atmosphere}`, { autoAlpha: 1, duration: 0.48, ease: 'none' }, 0)
            .fromTo(media, { autoAlpha: 0, yPercent: 8, scale: 0.97 }, { autoAlpha: 1, yPercent: -2, scale: 1, duration: 1, ease: 'none' }, 0)
            .fromTo(copy, { autoAlpha: 0, yPercent: 14 }, { autoAlpha: 1, yPercent: 0, duration: 0.82, ease: 'none' }, 0.08)
        }
      })

      gsap.utils.toArray<HTMLElement>('[data-aqua-reveal]').filter((element) => !element.closest('.portal-discipline')).forEach((element) => {
        gsap.fromTo(element, { autoAlpha: 0, y: 30 }, {
          autoAlpha: 1,
          y: 0,
          duration: 0.7,
          ease: 'power3.out',
          scrollTrigger: { trigger: element, start: 'top 86%', once: true },
        })
      })

      if (loopScene.value) {
        gsap.timeline({
          scrollTrigger: { trigger: loopScene.value, start: 'top 70%', end: 'bottom 62%', scrub: 0.68 },
        })
          .fromTo('.portal-loop-progress', { scaleX: 0 }, { scaleX: 1, duration: 1, ease: 'none' }, 0)
          .fromTo('.portal-loop-step', { autoAlpha: 0.34 }, { autoAlpha: 1, duration: 0.82, stagger: 0.15, ease: 'none' }, 0.08)
      }

      if (hero.value && heroOptic.value && matchMedia('(hover: hover) and (pointer: fine)').matches) {
        const opticX = gsap.quickTo(heroOptic.value, 'x', { duration: 0.55, ease: 'power3.out' })
        const opticY = gsap.quickTo(heroOptic.value, 'y', { duration: 0.55, ease: 'power3.out' })
        desktopPointerTarget = hero.value
        desktopPointerHandler = (event: PointerEvent) => {
          const rect = hero.value?.getBoundingClientRect()
          if (!rect || !root.value) return
          const x = ((event.clientX - rect.left) / rect.width - 0.5) * 2
          const y = ((event.clientY - rect.top) / rect.height - 0.5) * 2
          root.value.style.setProperty('--portal-pointer-x', `${50 + x * 8}%`)
          root.value.style.setProperty('--portal-pointer-y', `${42 + y * 7}%`)
          opticX(x * 12)
          opticY(y * 9)
        }
        pointerHandler = desktopPointerHandler
        desktopPointerTarget.addEventListener('pointermove', desktopPointerHandler, { passive: true })
      }

      return () => {
        if (desktopPointerTarget && desktopPointerHandler) {
          desktopPointerTarget.removeEventListener('pointermove', desktopPointerHandler)
        }
        if (pointerHandler === desktopPointerHandler) pointerHandler = undefined
        root.value?.style.removeProperty('--portal-pointer-x')
        root.value?.style.removeProperty('--portal-pointer-y')
      }
    })

    motion?.add('(max-width: 63.99rem) and (prefers-reduced-motion: no-preference)', () => {
      gsap.utils.toArray<HTMLElement>('[data-aqua-reveal]').forEach((element) => {
        gsap.fromTo(element, { autoAlpha: 0, y: 22 }, {
          autoAlpha: 1,
          y: 0,
          duration: 0.62,
          ease: 'power3.out',
          scrollTrigger: { trigger: element, start: 'top 88%', once: true },
        })
      })
    })

    motion?.add('(prefers-reduced-motion: reduce)', () => {
      gsap.set('.portal-nav, .portal-hero-copy > *, .portal-hero-world, .portal-hero-instrument, [data-aqua-reveal], .physics-readout', { clearProps: 'all' })
    })
  }, root.value)

  requestAnimationFrame(() => ScrollTrigger.refresh())
})

onBeforeUnmount(() => {
  if (pointerHandler && hero.value) hero.value.removeEventListener('pointermove', pointerHandler)
  context?.revert()
  motion?.revert()
  document.documentElement.classList.remove('aqua-motion-ready')
})
</script>

<template>
  <div ref="root" class="portal-page" data-portal-theme="aqua-future">
    <div class="portal-atmospheres" aria-hidden="true">
      <span class="portal-atmosphere portal-atmosphere--physics"></span>
      <span class="portal-atmosphere portal-atmosphere--chemistry"></span>
      <span class="portal-atmosphere portal-atmosphere--biology"></span>
    </div>

    <header class="portal-nav aero-shell" aria-label="公共门户导航">
      <router-link class="portal-wordmark" to="/" aria-label="RIKE 公共首页">
        <span class="portal-mark aero-orb" aria-hidden="true"><i></i></span>
        <span><strong>RIKE</strong><small>理科学习辅助系统</small></span>
      </router-link>
      <nav aria-label="页面章节">
        <a href="#physics">物理</a><a href="#chemistry">化学</a><a href="#biology">生物</a>
      </nav>
      <router-link data-testid="portal-login" class="portal-nav-login aero-control" to="/login">登录</router-link>
    </header>

    <main>
      <section ref="hero" class="portal-hero" data-portal-scene="hero" aria-labelledby="portal-title">
        <figure class="portal-hero-media">
          <img class="portal-hero-world portal-depth-layer" :src="aquaWorld" width="1586" height="992" loading="eager" fetchpriority="high" decoding="async" alt="清水与日光中的透明光学仪器连接波动、化学液面和生命叶脉" />
          <span class="portal-caustics" aria-hidden="true"></span>
          <span ref="heroOptic" class="portal-hero-instrument aero-orb" aria-hidden="true"><i></i><b></b></span>
        </figure>
        <div class="portal-hero-copy">
          <p class="portal-hero-kicker">FIELD · EQUILIBRIUM · LIFE</p>
          <h1 id="portal-title" aria-label="RIKE 理科学习辅助系统"><span>RIKE</span>理科学习辅助系统</h1>
          <p class="portal-subtitle">高中物理、化学、生物练习与学习管理。</p>
          <div class="portal-actions">
            <router-link class="portal-primary-action aero-control" to="/login">进入系统</router-link>
            <a class="portal-secondary-action" href="#physics">沿科学路径向下</a>
          </div>
        </div>
        <div class="portal-scroll-cue" aria-hidden="true"><span></span><p>SCROLL TO OBSERVE</p></div>
      </section>

      <section ref="physicsChapter" id="physics" class="portal-physics-chapter" data-portal-scene="physics" data-subject="physics" aria-labelledby="physics-title">
        <div ref="physicsPin" class="portal-physics-pin">
          <div class="physics-scene-media">
            <img :src="physicsVisual" width="1586" height="992" loading="lazy" decoding="async" alt="清水实验场中的透明透镜、钴蓝波干涉、场线和精密测量环" />
            <svg class="physics-overlay" viewBox="0 0 1200 760" aria-hidden="true">
              <path class="physics-wave-path" pathLength="720" d="M40 474c78-150 156 150 234 0s156 150 234 0 156 150 234 0 156 150 234 0" />
              <path class="physics-field-path" pathLength="560" d="M122 624C310 472 533 414 806 430c122 7 214-8 292-62" />
            </svg>
            <span class="physics-lens" aria-hidden="true"></span>
            <span class="physics-beam" aria-hidden="true"></span>
            <span class="physics-spectrum" aria-hidden="true"></span>
          </div>
          <div class="physics-copy">
            <p class="portal-scene-index">01 / PHYSICS</p>
            <h2 id="physics-title">场、波与运动，<br />在同一次观察里展开。</h2>
            <p>练习保留条件、单位、推导和知识点，让物理过程可以被测量、回看和再次验证。</p>
            <div class="physics-material aero-glass">
              <span class="physics-material-solid" aria-hidden="true"></span>
              <div class="physics-readout physics-readout--wave"><b>WAVE</b><span>干涉与传播</span></div>
              <div class="physics-readout physics-readout--field"><b>FIELD</b><span>方向与作用</span></div>
              <div class="physics-readout physics-readout--light"><b>OPTICS</b><span>折射与测量</span></div>
            </div>
          </div>
        </div>
      </section>

      <section id="chemistry" class="portal-discipline portal-discipline--chemistry" data-portal-scene="chemistry" data-atmosphere="chemistry" data-subject="chemistry" aria-labelledby="chemistry-title">
        <div class="portal-discipline-copy" data-aqua-reveal>
          <p class="portal-scene-index">02 / CHEMISTRY</p>
          <h2 id="chemistry-title">变化并不混乱。<br />条件决定平衡。</h2>
          <p>物质组成、反应条件、实验现象和计算依据在一条证据链中被逐项解释。</p>
          <ul aria-label="化学学习内容"><li>物质的量</li><li>元素化合物</li><li>实验与反应原理</li></ul>
        </div>
        <figure class="portal-discipline-media" data-aqua-reveal>
          <img :src="chemistryVisual" width="1586" height="992" loading="lazy" decoding="async" alt="水面实验平台上的透明器皿、梅紫液面、分子几何和光谱折射" />
          <figcaption><span>MENISCUS</span><span>TRANSFORMATION</span><span>SPECTRUM</span></figcaption>
        </figure>
      </section>

      <section id="biology" class="portal-discipline portal-discipline--biology" data-portal-scene="biology" data-atmosphere="biology" data-subject="biology" aria-labelledby="biology-title">
        <figure class="portal-discipline-media" data-aqua-reveal>
          <img :src="biologyVisual" width="1586" height="992" loading="lazy" decoding="async" alt="清水上下相连的巨幅叶脉、膜结构、水滴和根系生命网络" />
          <figcaption><span>GROWTH</span><span>MEMBRANE</span><span>CONNECTION</span></figcaption>
        </figure>
        <div class="portal-discipline-copy" data-aqua-reveal>
          <p class="portal-scene-index">03 / BIOLOGY</p>
          <h2 id="biology-title">生命不是一个细胞。<br />它是持续连接的系统。</h2>
          <p>从膜结构与遗传，到稳态与生态，沿材料证据逐层建立结构、功能和系统关系。</p>
          <ul aria-label="生物学习内容"><li>分子与细胞</li><li>遗传与进化</li><li>稳态与生态</li></ul>
        </div>
      </section>

      <section ref="loopScene" class="portal-loop" data-portal-scene="learning-loop" aria-labelledby="loop-title">
        <header data-aqua-reveal>
          <p class="portal-scene-index">04 / LEARNING LOOP</p>
          <h2 id="loop-title">一次练习，形成可复验的反馈闭环。</h2>
          <p>练习事实冻结后，判分、错题和标准解析保持同一条链路。</p>
        </header>
        <div class="portal-loop-rail">
          <span class="portal-loop-track" aria-hidden="true"><i class="portal-loop-progress"></i></span>
          <ol>
            <li class="portal-loop-step"><b>01</b><h3>练习</h3><p>按学科与知识点创建题组。</p></li>
            <li class="portal-loop-step"><b>02</b><h3>判分</h3><p>固定答案规则即时判断。</p></li>
            <li class="portal-loop-step"><b>03</b><h3>错题</h3><p>错误事实进入本人记录。</p></li>
            <li class="portal-loop-step"><b>04</b><h3>标准解析</h3><p>查看冻结答案与逐项说明。</p></li>
            <li class="portal-loop-step"><b>05</b><h3>再练习</h3><p>回到同一知识点验证掌握。</p></li>
          </ol>
        </div>
        <dl class="portal-facts" aria-label="演示内容规模" data-aqua-reveal>
          <div><dt>学科</dt><dd>3</dd></div>
          <div><dt>自动练习题</dt><dd>360</dd></div>
          <div><dt>专题综合题</dt><dd>18</dd></div>
        </dl>
      </section>

      <section class="portal-entrance-scene" data-portal-scene="entrance" aria-labelledby="entrance-title">
        <div class="portal-entrance-optic" aria-hidden="true"><span></span><i></i></div>
        <div class="portal-entrance aero-glass-heavy" data-aqua-reveal>
          <p class="portal-scene-index">05 / ENTER RIKE</p>
          <h2 id="entrance-title">从真实账号，进入自己的科学工作台。</h2>
          <p>学生进入学科练习，教师进入任课范围，管理员进入中性管理环境。</p>
          <router-link class="portal-primary-action aero-control" to="/login">前往登录</router-link>
        </div>
      </section>
    </main>

    <footer class="portal-footer">
      <strong>RIKE</strong><p>练习、判分、错题、标准解析与再练习。</p><span>本科毕业设计 · 非 AI 基础能力</span>
    </footer>
  </div>
</template>

<style src="../styles/portal.css"></style>
