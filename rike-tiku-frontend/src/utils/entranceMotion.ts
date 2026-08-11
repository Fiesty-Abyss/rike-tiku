import gsap from 'gsap'
import { onMounted, onUnmounted, type Ref } from 'vue'

export function useEntranceMotion(root: Ref<HTMLElement | undefined>, selector: string, stagger = 0.07) {
  let context: gsap.Context | undefined
  let media: gsap.MatchMedia | undefined

  onMounted(() => {
    if (!root.value || typeof window.matchMedia !== 'function') return
    media = gsap.matchMedia()
    context = gsap.context(() => {
      media?.add('(prefers-reduced-motion: no-preference)', () => {
        gsap.from(selector, { autoAlpha: 0, y: 18, duration: 0.55, stagger, ease: 'power3.out' })
      })
      media?.add('(prefers-reduced-motion: reduce)', () => gsap.set(selector, { clearProps: 'all' }))
    }, root.value)
  })

  onUnmounted(() => {
    media?.revert()
    context?.revert()
  })
}
