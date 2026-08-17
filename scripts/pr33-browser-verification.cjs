const { chromium } = require('playwright-core')
const fs = require('node:fs')
const path = require('node:path')

const frontend = 'http://localhost:18080'
const api = 'http://localhost:18081/api/v1'
const chrome = 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe'
const dir = path.join(process.cwd(), 'docs', 'evidence', 'thesis-final')
fs.mkdirSync(dir, { recursive: true })
const records = []

async function login(browser, username, role, viewport = { width: 1440, height: 1000 }) {
  const context = await browser.newContext({ viewport })
  const challenge = await context.request.get(`${api}/auth/captcha-challenge`).then(response => response.json())
  const response = await context.request.post(`${api}/auth/login`, {
    data: { username, password: 'a1234567', expectedRole: role, challengeId: challenge.challengeId, captchaCode: challenge.testCode },
  })
  if (!response.ok()) throw new Error(`${role} login ${response.status()}`)
  const auth = await response.json()
  const authorization = `Bearer ${auth.accessToken}`
  await context.setExtraHTTPHeaders({ Authorization: authorization })
  await context.addInitScript(({ token, expires, activeRole }) => {
    localStorage.setItem('rike-tiku.access-token', token)
    localStorage.setItem('rike-tiku.token-type', 'Bearer')
    localStorage.setItem('rike-tiku.expires-in', String(expires))
    sessionStorage.setItem('rike-tiku.active-role', activeRole)
  }, { token: auth.accessToken, expires: auth.expiresIn, activeRole: role })
  return context
}

async function inspect(page, route, file, expected) {
  const record = { route, consoleErrors: [], pageErrors: [], failedRequests: [] }
  page.on('console', message => { if (message.type() === 'error') record.consoleErrors.push(message.text()) })
  page.on('pageerror', error => record.pageErrors.push(String(error)))
  page.on('requestfailed', request => {
    if (!String(request.failure()?.errorText).includes('ERR_ABORTED')) record.failedRequests.push(request.url())
  })
  const response = await page.goto(frontend + route, { waitUntil: 'networkidle', timeout: 30000 })
  record.status = response?.status()
  record.finalPath = new URL(page.url()).pathname
  record.expected = expected ? await page.getByText(expected, { exact: false }).first().isVisible().catch(() => false) : true
  record.overflow = await page.evaluate(() => document.documentElement.scrollWidth > document.documentElement.clientWidth + 1)
  if (file) await page.screenshot({ path: path.join(dir, file), fullPage: true })
  records.push(record)
  return record
}

async function createPracticeResult(context) {
  const options = await context.request.get(`${api}/student/practice-options`).then(response => response.json())
  const subjectId = options.subjects[0].id
  const sessionResponse = await context.request.post(`${api}/student/practice-sessions`, { data: { subjectId, count: 1 } })
  if (!sessionResponse.ok()) throw new Error(`practice create ${sessionResponse.status()}`)
  const session = await sessionResponse.json()
  const question = session.questions[0]
  const answer = question.questionType === 'MULTIPLE_CHOICE'
    ? [question.options.at(-1).label]
    : question.questionType === 'FILL_BLANK'
      ? Array.from({ length: Math.max(1, question.blankCount) }, () => '审计答案')
      : question.options.at(-1).label
  const resultResponse = await context.request.post(`${api}/student/practice-sessions/${session.id}/submit`, {
    data: { answers: [{ practiceQuestionId: question.practiceQuestionId, answer, elapsedSeconds: 1 }] },
  })
  if (!resultResponse.ok()) throw new Error(`practice submit ${resultResponse.status()}`)
  return session.id
}

async function createPaper(context) {
  const existing = await context.request.get(`${api}/teacher/papers`).then(response => response.json())
  if (existing.length) return existing[0].id
  const scopes = await context.request.get(`${api}/teacher/teaching-scopes`).then(response => response.json())
  const subjectId = scopes.find(scope => scope.teachingStatus === 'ACTIVE').subjectId
  const questions = await context.request.get(`${api}/teacher/papers/questions?subjectId=${subjectId}`).then(response => response.json())
  const items = questions.slice(0, 5).map(question => ({ questionId: question.id, score: 20 }))
  const response = await context.request.post(`${api}/teacher/papers`, {
    data: { subjectId, name: 'RIKE 匿名演示试卷', mode: 'MANUAL', items },
  })
  if (!response.ok()) throw new Error(`paper create ${response.status()}`)
  return (await response.json()).id
}

async function captureVariantFixture(page, resultRoute) {
  const variant = {
    id: 900001, answerFactId: 1, motherQuestionId: 1, questionId: 900001, status: 'READY',
    questionType: 'SINGLE_CHOICE', stem: '**电场力**满足 \\(F=qE\\)。若正电荷只受电场力，其方向是？', difficulty: 2,
    options: [{ label: 'A', content: '沿电场方向' }, { label: 'B', content: '垂直电场方向' }, { label: 'C', content: '与电场方向相反' }],
    reviewStatus: 'PENDING',
  }
  await page.route('**/api/v1/student/ai/variants', route => route.request().method() === 'POST'
    ? route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(variant) }) : route.continue())
  await page.route('**/api/v1/student/ai/variants/900001/answer', route => route.fulfill({
    status: 200, contentType: 'application/json',
    body: JSON.stringify({ ...variant, status: 'ANSWERED', correct: true, correctAnswer: { optionLabels: ['A'] }, aiAnalysis: '**AI 生成解析**：正电荷所受电场力方向与电场方向相同。' }),
  }))
  await page.goto(frontend + resultRoute, { waitUntil: 'networkidle' })
  await page.getByRole('button', { name: '生成变式题' }).click()
  await page.getByText('沿电场方向', { exact: false }).waitFor()
  await page.screenshot({ path: path.join(dir, '09-student-ai-variant.png'), fullPage: true })
  await page.getByText('A. 沿电场方向', { exact: false }).click()
  await page.getByRole('button', { name: '提交答案' }).click()
  await page.getByText('回答正确', { exact: false }).waitFor()
  await page.screenshot({ path: path.join(dir, '10-student-ai-variant-result.png'), fullPage: true })
}

async function main() {
  const browser = await chromium.launch({ headless: true, executablePath: chrome, args: ['--disable-gpu'] })
  try {
    let context = await browser.newContext({ viewport: { width: 1440, height: 1000 } })
    let page = await context.newPage()
    await inspect(page, '/', '01-portal-desktop.png', 'RIKE')
    await inspect(page, '/login', '02-login.png', '申请密码恢复')
    await context.close()

    context = await browser.newContext({ viewport: { width: 390, height: 844 } })
    page = await context.newPage()
    await inspect(page, '/', '20-portal-mobile.png', 'RIKE')
    await context.close()

    context = await login(browser, 'demo_student', 'STUDENT')
    const resultId = await createPracticeResult(context)
    page = await context.newPage()
    await inspect(page, '/student', '03-student-dashboard.png', '学习')
    await inspect(page, '/student/practice/new', '04-practice.png', '练习')
    await inspect(page, `/student/practice/${resultId}/result`, '05-result-standard.png', '标准解析')
    await page.screenshot({ path: path.join(dir, '07-student-ai-analysis.png'), fullPage: true })
    await page.getByRole('button', { name: '当前题目答疑' }).first().click()
    await page.getByText('已绑定当前题目', { exact: false }).waitFor()
    await page.screenshot({ path: path.join(dir, '08-student-ai-chat.png'), fullPage: true })
    await page.keyboard.press('Escape')
    await inspect(page, '/student/wrong-questions', '06-wrong-questions.png', '错题')
    await captureVariantFixture(page, `/student/practice/${resultId}/result`)
    await context.close()

    context = await login(browser, 'demo_student', 'STUDENT', { width: 390, height: 844 })
    page = await context.newPage()
    await inspect(page, '/student', '21-student-mobile.png', '学习')
    await context.close()

    context = await login(browser, 'demo_teacher', 'TEACHER')
    const paperId = await createPaper(context)
    page = await context.newPage()
    await inspect(page, '/teacher', '11-teacher-workspace.png', '组卷与打印')
    await inspect(page, '/teacher/ai-generation', '12-teacher-ai-review.png', 'AI')
    await inspect(page, '/teacher/papers', '13-teacher-paper-builder.png', '题篮')
    await inspect(page, `/teacher/papers/${paperId}/student`, '14-paper-student-preview.png', '打印 / 另存为 PDF')
    await inspect(page, `/teacher/papers/${paperId}/answer`, '15-paper-answer-preview.png', 'STANDARD 解析')
    await context.close()

    context = await login(browser, 'demo_admin', 'ADMIN')
    page = await context.newPage()
    await inspect(page, '/admin', '16-admin-dashboard.png', '管理')
    await inspect(page, '/admin/ai-models', '17-admin-ai-models.png', '智谱联网搜索')
    await inspect(page, '/admin/password-recovery', '18-admin-password-notifications.png', '账号')
    await inspect(page, '/admin/ai-generation', '19-admin-ai-generation.png', 'AI')
    await context.close()

    const summary = {
      routes: records.length,
      consoleErrors: records.reduce((sum, record) => sum + record.consoleErrors.length, 0),
      pageErrors: records.reduce((sum, record) => sum + record.pageErrors.length, 0),
      failedRequests: records.reduce((sum, record) => sum + record.failedRequests.length, 0),
      overflows: records.filter(record => record.overflow).map(record => record.route),
      missingExpected: records.filter(record => !record.expected).map(record => record.route),
    }
    fs.writeFileSync(path.join(dir, 'browser-results-pr33.json'), JSON.stringify({ generatedAt: new Date().toISOString(), fixtureDisclosure: 'Screenshots 09 and 10 use a deterministic intercepted variant response to verify UI states; all other routes use rike_tiku_demo APIs.', summary, records }, null, 2))
    console.log(JSON.stringify(summary))
    if (summary.consoleErrors || summary.pageErrors || summary.failedRequests || summary.overflows.length || summary.missingExpected.length) process.exitCode = 1
  } finally {
    await browser.close()
  }
}

main().catch(error => { console.error(error); process.exitCode = 1 })
