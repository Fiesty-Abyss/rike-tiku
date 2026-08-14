const { chromium } = require('playwright-core')
const fs = require('node:fs')
const path = require('node:path')

const frontend = 'http://localhost:18080'
const api = 'http://localhost:18081/api/v1'
const demoPassword = process.env.RIKE_TIKU_DEMO_PASSWORD
const evidence = path.join(process.cwd(), 'docs', 'evidence', 'pr33-final-browser')
fs.mkdirSync(evidence, { recursive: true })

async function login(browser) {
  if (!demoPassword) throw new Error('RIKE_TIKU_DEMO_PASSWORD must be present for the isolated Demo browser check')
  const context = await browser.newContext({ viewport: { width: 1440, height: 900 } })
  const challenge = await context.request.get(`${api}/auth/captcha-challenge`).then(response => response.json())
  const response = await context.request.post(`${api}/auth/login`, {
    data: { username: 'demo_student', password: demoPassword, expectedRole: 'STUDENT', challengeId: challenge.challengeId, captchaCode: challenge.testCode },
  })
  if (!response.ok()) throw new Error(`anonymous Demo login failed: HTTP ${response.status()}`)
  const auth = await response.json()
  await context.setExtraHTTPHeaders({ Authorization: `Bearer ${auth.accessToken}` })
  await context.addInitScript(token => {
    localStorage.setItem('rike-tiku.access-token', token)
    localStorage.setItem('rike-tiku.token-type', 'Bearer')
    sessionStorage.setItem('rike-tiku.active-role', 'STUDENT')
  }, auth.accessToken)
  return context
}

async function wrongPage(context, params) {
  const query = new URLSearchParams({ page: '0', size: '20', ...params })
  const response = await context.request.get(`${api}/student/wrong-questions?${query}`)
  if (!response.ok()) throw new Error(`wrong-question query failed: HTTP ${response.status()}`)
  return response.json()
}

async function ensureWrongQuestion(context, subject) {
  let existing = await wrongPage(context, { subjectCode: subject.code })
  const reusable = existing.items.find(item => item.knowledgePoints?.length)
  if (reusable) return reusable.knowledgePoints[0]

  const options = await context.request.get(`${api}/student/practice-options?subjectId=${subject.id}`).then(response => response.json())
  for (const point of options.knowledgePoints) {
    const created = await context.request.post(`${api}/student/practice-sessions`, {
      data: { subjectId: subject.id, knowledgePointIds: [point.id], questionTypes: ['FILL_BLANK'], count: 1 },
    })
    if (!created.ok()) continue
    const session = await created.json()
    const question = session.questions[0]
    const answer = Array.from({ length: question.blankCount || 1 }, () => '__ANONYMOUS_WRONG_ANSWER__')
    const submitted = await context.request.post(`${api}/student/practice-sessions/${session.id}/submit`, {
      data: { answers: [{ practiceQuestionId: question.practiceQuestionId, answer, elapsedSeconds: 1 }] },
    })
    if (!submitted.ok()) continue
    existing = await wrongPage(context, { subjectCode: subject.code, knowledgePointId: String(point.id) })
    if (existing.items.length) return point
  }
  throw new Error(`cannot prepare an anonymous ${subject.code} wrong question with a stable knowledge point`)
}

async function choose(page, testId, label) {
  await page.getByTestId(testId).click()
  await page.locator('.el-select-dropdown:visible').getByText(label, { exact: true }).click()
}

async function main() {
  const browser = await chromium.launch({ headless: true, channel: 'chrome', args: ['--disable-gpu'] })
  const result = { generatedAt: new Date().toISOString(), database: 'rike_tiku_demo', steps: [], consoleErrors: [], pageErrors: [], failedRequests: [], apiResponses: [], overflow: false }
  try {
    const context = await login(browser)
    const options = await context.request.get(`${api}/student/practice-options`).then(response => response.json())
    const biology = options.subjects.find(subject => subject.code === 'BIOLOGY')
    const physics = options.subjects.find(subject => subject.code === 'PHYSICS')
    if (!biology || !physics) throw new Error('Demo API did not return PHYSICS and BIOLOGY subjects')
    const biologyPoint = await ensureWrongQuestion(context, biology)
    await ensureWrongQuestion(context, physics)

    const page = await context.newPage()
    page.on('console', message => { if (message.type() === 'error') result.consoleErrors.push(message.text()) })
    page.on('pageerror', error => result.pageErrors.push(String(error)))
    page.on('requestfailed', request => { if (!String(request.failure()?.errorText).includes('ERR_ABORTED')) result.failedRequests.push(request.url()) })
    page.on('response', response => { if (response.url().includes('/student/wrong-questions')) result.apiResponses.push({ url: response.url(), status: response.status() }) })

    await page.goto(`${frontend}/student/wrong-questions`, { waitUntil: 'networkidle' })
    result.steps.push({ name: 'open-all-wrong-questions', pass: await page.getByRole('heading', { name: '错题本', exact: true }).isVisible() })

    await Promise.all([
      page.waitForResponse(response => response.url().includes('/student/wrong-questions') && response.url().includes('subjectCode=BIOLOGY') && response.ok()),
      choose(page, 'wrong-subject-filter', '生物'),
    ])
    const biologyRows = page.locator('.el-table__body-wrapper tbody tr')
    await biologyRows.first().waitFor()
    const biologyTexts = await biologyRows.allInnerTexts()
    result.steps.push({ name: 'biology-filter-only-biology', pass: biologyTexts.length > 0 && biologyTexts.every(text => text.includes('生物') && !text.includes('物理') && !text.includes('化学')) })

    await choose(page, 'wrong-knowledge-filter', biologyPoint.path)
    const responseCount = result.apiResponses.length
    await page.getByRole('button', { name: '筛选' }).click()
    await page.waitForTimeout(1500)
    const knowledgeResponse = result.apiResponses.slice(responseCount).at(-1)
    result.steps.push({ name: 'biology-knowledge-filter-request-uses-stable-id', pass: knowledgeResponse?.status === 200 && knowledgeResponse.url.includes(`knowledgePointId=${biologyPoint.id}`), url: knowledgeResponse ? new URL(knowledgeResponse.url).pathname + new URL(knowledgeResponse.url).search : null })
    await biologyRows.first().waitFor()
    const knowledgeTexts = await biologyRows.allInnerTexts()
    result.steps.push({ name: 'biology-knowledge-filter-stable-id-and-path', pass: knowledgeTexts.length > 0 && knowledgeTexts.every(text => text.includes(biologyPoint.path)) })

    await Promise.all([
      page.waitForResponse(response => response.url().includes('/student/wrong-questions') && response.url().includes('subjectCode=PHYSICS') && response.ok()),
      choose(page, 'wrong-subject-filter', '物理'),
    ])
    const physicsRows = page.locator('.el-table__body-wrapper tbody tr')
    await physicsRows.first().waitFor()
    const physicsTexts = await physicsRows.allInnerTexts()
    const knowledgeInput = page.getByTestId('wrong-knowledge-filter').locator('input')
    result.steps.push({ name: 'switch-physics-clears-biology-knowledge-point', pass: (await knowledgeInput.inputValue()) === '' && physicsTexts.every(text => text.includes('物理') && !text.includes('生物')) })

    result.overflow = await page.evaluate(() => document.documentElement.scrollWidth > document.documentElement.clientWidth + 1)
    await page.screenshot({ path: path.join(evidence, '43-wrong-subject-knowledge-filter.png'), fullPage: true })
    await context.close()
  } finally {
    await browser.close()
  }
  result.pass = result.steps.every(step => step.pass) && !result.consoleErrors.length && !result.pageErrors.length && !result.failedRequests.length && !result.overflow
  fs.writeFileSync(path.join(evidence, 'wrong-filter-browser-v29.json'), JSON.stringify(result, null, 2))
  console.log(JSON.stringify({ pass: result.pass, steps: result.steps.length, consoleErrors: result.consoleErrors.length, pageErrors: result.pageErrors.length, failedRequests: result.failedRequests.length, overflow: result.overflow }))
  if (!result.pass) process.exitCode = 1
}

main().catch(error => { console.error(error); process.exitCode = 1 })
