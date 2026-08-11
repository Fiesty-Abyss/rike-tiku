import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'

function style(name: string) {
  return readFileSync(resolve(process.cwd(), `src/styles/${name}`), 'utf8')
}

describe('RIKE Aqua Future design system', () => {
  it('defines the required water, glass, scientific and subject tokens', () => {
    const css = style('tokens.css')
    ;[
      '--aero-sky', '--aero-water', '--aero-mist', '--aero-horizon',
      '--aero-glass-clear', '--aero-glass-frosted', '--aero-glass-thick',
      '--aero-specular', '--aero-edge-light', '--aero-depth-shadow',
      '--aero-caustic', '--aero-iridescence',
      '--physics-field', '--physics-light', '--physics-cobalt', '--physics-deep', '--physics-silver',
      '--chemistry-fluid', '--chemistry-plum', '--chemistry-spectrum', '--chemistry-silver', '--chemistry-deep',
      '--biology-water', '--biology-jade', '--biology-leaf', '--biology-forest', '--biology-deep',
      '--type-display', '--type-hero', '--type-section', '--type-title', '--type-body', '--type-caption', '--type-metric', '--type-scientific',
    ].forEach(token => expect(css).toContain(token))
  })

  it('keeps glass at shell/tool layers and solid surfaces for reading and data', () => {
    const theme = style('themes/mizuiro-aero.css')
    const components = style('components.css')
    expect(theme).toContain('.aero-shell')
    expect(theme).toContain('.aero-glass-heavy')
    expect(theme).toContain('.aero-solid')
    expect(theme).toContain('.aero-control')
    expect(theme).toContain('.aero-orb')
    expect(components).toContain('.practice-question')
    expect(components).toContain('.topic-reader')
    expect(components).toContain('.data-table')
    expect(components).toContain('background: var(--surface-solid)')
  })

  it('drives student and teacher environments from explicit subjectCode surfaces without cross-subject card leakage', () => {
    const css = style('subject-environments.css')
    expect(css).toContain('.student-shell:has(.student-page[data-subject="physics"])')
    expect(css).toContain('.student-shell:has(.student-page[data-subject="chemistry"])')
    expect(css).toContain('.student-shell:has(.student-page[data-subject="biology"])')
    expect(css).not.toContain('.student-shell:has([data-subject="physics"])')
    expect(css).toContain('.workspace-page[data-subject="physics"]')
    expect(css).toContain('.workspace-page[data-subject="chemistry"]')
    expect(css).toContain('.workspace-page[data-subject="biology"]')
  })

  it('provides keyboard focus, 44px controls, mobile reflow and reduced-motion fallbacks', () => {
    const components = style('components.css')
    const motion = style('motion.css')
    expect(components).toContain(':focus-visible')
    expect(components).toContain('min-height: 44px')
    expect(components).toContain('@media (max-width: 30rem)')
    expect(motion).toContain('@media (prefers-reduced-motion: reduce)')
    expect(motion).toContain('animation-duration: 1ms')
  })
})
