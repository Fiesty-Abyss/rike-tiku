const { chromium } = require('playwright-core')
const fs = require('node:fs')
const os = require('node:os')
const path = require('node:path')

const frontend = process.env.RIKE_FORMAL_FRONTEND_URL || 'http://localhost:18080'
const api = process.env.RIKE_FORMAL_API_URL || 'http://localhost:18081/api/v1'
const username = process.env.RIKE_FORMAL_STUDENT_USER
const password = process.env.RIKE_FORMAL_TEST_PASSWORD || 'a1234567'
const chromiumPath = process.env.RIKE_PLAYWRIGHT_CHROMIUM_PATH || 'C:\\Users\\鱼皇\\AppData\\Local\\ms-playwright\\chromium-1228\\chrome-win64\\chrome.exe'
const evidence = path.join(process.cwd(), 'docs', 'evidence', 'pr33-formal-student')
fs.mkdirSync(evidence, { recursive: true })

if (!username) throw new Error('RIKE_FORMAL_STUDENT_USER is absent')
if (!fs.existsSync(chromiumPath)) throw new Error(`Chromium executable not found: ${chromiumPath}`)

const assertions = []
const records = []
function assert(name, pass, detail = '') {
  assertions.push({ name, pass: Boolean(pass), detail })
  if (!pass) console.error(`ASSERTION_FAILED ${name} ${detail}`)
}
function hasRawScientificMarkup(text) {
  return /\\(?:\\(|\\)|\\[|\\]|ce\{|frac\{|sum|cdot|tan|pi|mu|vec|times|text\{)/.test(text)
}
async function json(response) {
  if (!response.ok()) throw new Error(`HTTP ${response.status()} ${response.url()}`)
  return response.json()
}
async function inspect(page, route, expectedText, screenshotName) {
  const result = { route, status: null, finalPath: null, consoleErrors: [], pageErrors: [], failedRequests: [], overflow: false }
  const onConsole = message => { if (message.type() === 'error') result.consoleErrors.push(message.text()) }
  const onPageError = error => result.pageErrors.push(String(error))
  const onRequestFailed = request => {
    const failure = request.failure()?.errorText || ''
    if (failure !== 'net::ERR_ABORTED') result.failedRequests.push(request.url())
  }
  page.on('console', onConsole); page.on('pageerror', onPageError); page.on('requestfailed', onRequestFailed)
  const response = await page.goto(frontend + route, { waitUntil: 'networkidle', timeout: 30000 })
  result.status = response?.status() ?? null
  result.finalPath = new URL(page.url()).pathname
  result.overflow = await page.evaluate(() => document.documentElement.scrollWidth > document.documentElement.clientWidth + 1)
  const visible = expectedText ? await page.getByText(expectedText, { exact: false }).first().isVisible().catch(() => false) : true
  assert(`page:${route}`, result.status === 200 && visible, `status=${result.status}, visible=${visible}`)
  if (screenshotName) await page.screenshot({ path: path.join(evidence, screenshotName), fullPage: true })
  page.off('console', onConsole); page.off('pageerror', onPageError); page.off('requestfailed', onRequestFailed)
  records.push(result)
  return result
}

async function main() {
  const profile = fs.mkdtempSync(path.join(os.tmpdir(), 'rike-formal-student-'))
  const context = await chromium.launchPersistentContext(profile, {
    headless: true,
    executablePath: chromiumPath,
    viewport: { width: 1440, height: 900 },
    args: ['--disable-gpu']
  })
  let activePassword = password
  let changedInitialPassword = false
  let accountRestored = false
  try {
    const challengeResponse = await context.request.get(`${api}/auth/captcha-challenge`)
    const challenge = await json(challengeResponse)
    const loginResponse = await context.request.post(`${api}/auth/login`, {
      data: { username, password, expectedRole: 'STUDENT', challengeId: challenge.challengeId, captchaCode: challenge.testCode }
    })
    let auth = await json(loginResponse)
    await context.setExtraHTTPHeaders({ Authorization: `Bearer ${auth.accessToken}` })
    if (auth.mustChangePassword && process.env.RIKE_FORMAL_INITIAL_PASSWORD) {
      const nextPassword = process.env.RIKE_FORMAL_INITIAL_PASSWORD
      const changeResponse = await context.request.post(`${api}/auth/change-initial-password`, {
        data: { oldPassword: activePassword, newPassword: nextPassword, confirmPassword: nextPassword }
      })
      auth = await json(changeResponse)
      activePassword = nextPassword
      changedInitialPassword = true
      await context.setExtraHTTPHeaders({ Authorization: `Bearer ${auth.accessToken}` })
      assert('formal-initial-password-change', !auth.mustChangePassword)
    }
    await context.addInitScript(({ token }) => {
      localStorage.setItem('rike-tiku.access-token', token)
      localStorage.setItem('rike-tiku.token-type', 'Bearer')
      sessionStorage.setItem('rike-tiku.active-role', 'STUDENT')
    }, { token: auth.accessToken })
    assert('formal-student-login', Boolean(auth.accessToken))

    const health = await context.request.get(`${api}/health`)
    assert('backend-health', health.ok(), `status=${health.status()}`)

    const wrongResponse = await context.request.get(`${api}/student/wrong-questions`)
    const wrongBody = await json(wrongResponse)
    const wrongItems = Array.isArray(wrongBody) ? wrongBody : (wrongBody.items || wrongBody.records || [])
    assert('wrong-question-api-shape', Array.isArray(wrongItems), 'list is not an array')
    const wrongQuestionId = wrongItems[0]?.questionId || wrongItems[0]?.id

    const units = await json(await context.request.get(`${api}/student/topic-learning/units`))
    assert('formal-topic-unit-count', Array.isArray(units) && units.length >= 15, `count=${Array.isArray(units) ? units.length : 'invalid'}`)
    if (Array.isArray(units)) {
      const subjectCounts = units.reduce((counts, item) => {
        counts[item.subjectCode] = (counts[item.subjectCode] || 0) + 1
        return counts
      }, {})
      assert('formal-topic-subject-coverage', subjectCounts.PHYSICS >= 6 && subjectCounts.CHEMISTRY >= 5 && subjectCounts.BIOLOGY >= 4, JSON.stringify(subjectCounts))
    }
    const unit = units[0]
    if (unit?.id) {
      const detail = await json(await context.request.get(`${api}/student/topic-learning/units/${unit.id}`))
      assert('formal-topic-unit-question-count', Array.isArray(detail.questions) && detail.questions.length >= 2 && detail.questions.length <= 3, `count=${detail.questions?.length}`)
    }

    const cards = await json(await context.request.get(`${api}/student/knowledge-cards`))
    const cardItems = Array.isArray(cards) ? cards : []
    const cardTypes = new Set(cardItems.map(card => card.type || card.cardType || card.dataType))
    assert('formal-knowledge-card-count', cardItems.length >= 30, `count=${cardItems.length}`)
    assert('formal-knowledge-card-structured-types', ['DEFINITION', 'FORMULA', 'CONCLUSION'].some(type => cardTypes.has(type)), `types=${[...cardTypes].join(',')}`)

    const page = await context.newPage()
    await inspect(page, '/student/wrong-questions', '错题本', 'student-wrong-questions.png')
    const bodyText = await page.locator('body').innerText()
    assert('wrong-book-no-date-filter', !bodyText.includes('最近答题时间') && !bodyText.includes('日期范围'))
    assert('wrong-book-no-direct-archive-action', !bodyText.includes('移出错题本'))

    await inspect(page, '/student/topics', '专题单元', 'student-topic-units.png')
    assert('topic-page-unit-only-index', await page.locator('.topic-index').count() === 1 && await page.locator('.topic-index button').count() >= 6)
    if (unit?.id) {
      await inspect(page, `/student/topics/units/${unit.id}`, '专题单元', 'student-topic-unit-detail.png')
      const unitPageText = await page.locator('body').innerText()
      assert('topic-page-stage-navigation', unitPageText.includes('基础理解') && await page.locator('.topic-unit-navigation button').count() === 3)
      assert('topic-detail-no-raw-scientific-markup', !hasRawScientificMarkup(unitPageText), 'raw TeX delimiter or command found in visible text')
      assert('topic-detail-no-render-fallback', await page.locator('[data-render-status="fallback"]').count() === 0)
    }

    await inspect(page, '/student/knowledge-cards', '物化生高频考点与二级结论', 'student-high-frequency-points.png')
    const cardsText = await page.locator('body').innerText()
    assert('knowledge-card-human-readable', !cardsText.includes('最近3年') && !cardsText.includes('生成练习') && !cardsText.includes('"scientificContent"'))
    assert('knowledge-card-no-raw-scientific-markup', !hasRawScientificMarkup(cardsText), 'raw TeX delimiter or command found in visible text')
    assert('knowledge-card-no-render-fallback', await page.locator('[data-render-status="fallback"]').count() === 0)
    assert('knowledge-card-has-rendered-math', await page.locator('.katex').count() > 0)

    if (wrongQuestionId) {
      const retry = await context.request.post(`${api}/student/wrong-questions/${wrongQuestionId}/retry`)
      assert('wrong-question-retry-api', retry.ok(), `status=${retry.status()}`)
      if (retry.ok()) {
        const session = await retry.json()
        assert('wrong-question-retry-session-id', Boolean(session.id))
      }
    } else {
      assert('wrong-question-retry-api', true, 'formal account has no active wrong question; skipped non-destructive retry')
    }

    if (changedInitialPassword && process.env.RIKE_FORMAL_RESTORE_PASSWORD === 'true') {
      const restoreResponse = await context.request.post(`${api}/auth/change-password`, {
        data: { oldPassword: activePassword, newPassword: password, confirmPassword: password }
      })
      accountRestored = restoreResponse.ok()
      assert('formal-account-password-restored', accountRestored, `status=${restoreResponse.status()}`)
    }

    const summary = {
      generatedAt: new Date().toISOString(),
      database: 'rike_tiku',
      providerDisclosure: 'No current rotated Provider credentials were present; no Provider-dependent request was made.',
      summary: {
        assertions: assertions.length,
        assertionFailures: assertions.filter(item => !item.pass).length,
        routes: records.length,
        consoleErrors: records.reduce((count, item) => count + item.consoleErrors.length, 0),
        pageErrors: records.reduce((count, item) => count + item.pageErrors.length, 0),
        failedRequests: records.reduce((count, item) => count + item.failedRequests.length, 0),
        overflows: records.filter(item => item.overflow).map(item => item.route)
      },
      assertions,
      records,
      formalAccountRestored: accountRestored
    }
    fs.writeFileSync(path.join(evidence, 'browser-results-formal-v29.json'), JSON.stringify(summary, null, 2))
    console.log(JSON.stringify(summary.summary))
    if (summary.summary.assertionFailures || summary.summary.consoleErrors || summary.summary.pageErrors || summary.summary.failedRequests || summary.summary.overflows.length) process.exitCode = 1
  } finally {
    await context.close()
    fs.rmSync(profile, { recursive: true, force: true })
  }
}

main().catch(error => { console.error(error); process.exitCode = 1 })
