const { chromium } = require('playwright-core')
const fs = require('node:fs')
const path = require('node:path')

const frontend = process.env.RIKE_PR31_FRONTEND_URL || 'http://localhost:18080'
const api = process.env.RIKE_PR31_API_URL || 'http://localhost:18081/api/v1'
const chrome = process.env.RIKE_PR31_CHROME || 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe'
const resultSessionId = process.env.RIKE_PR31_STUDENT_SESSION_ID
const evidence = path.join(process.cwd(), 'docs', 'evidence', 'pr31-final')
fs.mkdirSync(evidence, { recursive: true })

const results = []
const browserErrors = []

async function saveScreenshot(page, fileName) {
  const target = path.join(evidence, fileName)
  if (!fs.existsSync(target)) await page.screenshot({ path: target, fullPage: true })
}

async function credentials(context, username, role) {
  const challengeResponse = await context.request.get(`${api}/auth/captcha-challenge`)
  const challenge = await challengeResponse.json()
  const loginResponse = await context.request.post(`${api}/auth/login`, {
    data: {
      username,
      password: 'a1234567',
      expectedRole: role,
      challengeId: challenge.challengeId,
      captchaCode: challenge.testCode,
    },
  })
  if (!loginResponse.ok()) throw new Error(`${role} browser login failed: ${loginResponse.status()}`)
  return loginResponse.json()
}

function observe(page, route) {
  const record = { route, consoleErrors: [], pageErrors: [], failedRequests: [] }
  page.on('console', message => {
    if (message.type() === 'error') record.consoleErrors.push(message.text())
  })
  page.on('pageerror', error => record.pageErrors.push(String(error)))
  page.on('requestfailed', request => {
    const failure = request.failure()?.errorText || 'unknown'
    if (!failure.includes('ERR_ABORTED')) record.failedRequests.push({ url: request.url(), failure })
  })
  return record
}

async function inspect(page, route, options = {}) {
  const record = observe(page, route)
  const response = await page.goto(`${frontend}${route}`, { waitUntil: 'networkidle', timeout: 30000 })
  await page.waitForTimeout(250)
  record.httpStatus = response?.status() || null
  record.finalPath = new URL(page.url()).pathname
  record.title = await page.title()
  record.bodyLength = (await page.locator('body').innerText()).trim().length
  record.horizontalOverflow = await page.evaluate(() => document.documentElement.scrollWidth > document.documentElement.clientWidth + 1)
  if (options.expectedText) record.expectedText = await page.getByText(options.expectedText, { exact: false }).first().isVisible().catch(() => false)
  if (options.screenshot) await saveScreenshot(page, options.screenshot)
  results.push(record)
  browserErrors.push(...record.consoleErrors, ...record.pageErrors, ...record.failedRequests.map(item => `${item.failure}: ${item.url}`))
  return record
}

async function authenticatedContext(browser, username, role, viewport = { width: 1440, height: 1000 }) {
  const context = await browser.newContext({ viewport })
  const login = await credentials(context, username, role)
  await context.addInitScript(({ token, expiresIn, selectedRole }) => {
    localStorage.setItem('rike-tiku.access-token', token)
    localStorage.setItem('rike-tiku.token-type', 'Bearer')
    localStorage.setItem('rike-tiku.expires-in', String(expiresIn))
    sessionStorage.setItem('rike-tiku.active-role', selectedRole)
  }, { token: login.accessToken, expiresIn: login.expiresIn, selectedRole: role })
  return context
}

async function main() {
  const browser = await chromium.launch({ headless: true, executablePath: chrome, args: ['--disable-gpu'] })
  try {
    const publicContext = await browser.newContext({ viewport: { width: 1440, height: 1000 } })
    const publicPage = await publicContext.newPage()
    await inspect(publicPage, '/', { screenshot: 'portal-desktop.png', expectedText: 'RIKE' })
    await inspect(publicPage, '/login', { screenshot: 'login.png', expectedText: '欢迎登录' })

    let browserChallenge
    publicPage.on('response', async response => {
      if (response.url().includes('/captcha-challenge')) browserChallenge = await response.json().catch(() => null)
    })
    await publicPage.reload({ waitUntil: 'networkidle' })
    await publicPage.getByPlaceholder('请输入用户名').fill('demo_student')
    await publicPage.getByPlaceholder('请输入密码').fill('a1234567')
    if (!browserChallenge?.testCode) throw new Error('Machine smoke CAPTCHA testCode was not exposed')
    await publicPage.getByPlaceholder('请输入验证码').fill(browserChallenge.testCode)
    await publicPage.getByRole('button', { name: '登录系统' }).click()
    await publicPage.waitForURL('**/student', { timeout: 15000 })
    results.push({ route: '/login -> /student', uiLogin: true, finalPath: new URL(publicPage.url()).pathname })
    await publicContext.close()

    const mobileContext = await browser.newContext({ viewport: { width: 390, height: 844 } })
    const mobilePage = await mobileContext.newPage()
    await inspect(mobilePage, '/', { screenshot: 'portal-mobile.png', expectedText: 'RIKE' })
    await mobileContext.close()

    const studentContext = await authenticatedContext(browser, 'demo_student', 'STUDENT')
    const studentPage = await studentContext.newPage()
    await inspect(studentPage, '/student', { screenshot: 'student-dashboard.png', expectedText: '学习' })
    await inspect(studentPage, '/student/practice/new', { expectedText: '练习' })
    await inspect(studentPage, '/student/wrong-questions', { screenshot: 'student-wrong-questions.png', expectedText: '错题本' })
    if (resultSessionId) {
      await inspect(studentPage, `/student/practice/${resultSessionId}/result`, { screenshot: 'student-ai-analysis.png', expectedText: 'AI 辅助分析' })
      const tutor = studentPage.getByRole('button', { name: '当前题目答疑' }).first()
      if (await tutor.isVisible().catch(() => false)) {
        await tutor.click()
        await studentPage.getByText('RIKE 理科学习助手', { exact: false }).first().waitFor({ timeout: 10000 })
        await saveScreenshot(studentPage, 'student-chat.png')
      }
    }
    await inspect(studentPage, '/profile', { expectedText: '个人' })
    await inspect(studentPage, '/messages', { expectedText: '消息' })
    await studentContext.close()

    const mobileStudentContext = await authenticatedContext(browser, 'demo_student', 'STUDENT', { width: 390, height: 844 })
    const mobileStudentPage = await mobileStudentContext.newPage()
    await inspect(mobileStudentPage, '/student', { screenshot: 'student-mobile.png', expectedText: '学习' })
    await mobileStudentContext.close()

    const teacherContext = await authenticatedContext(browser, 'demo_teacher', 'TEACHER')
    const teacherPage = await teacherContext.newPage()
    await inspect(teacherPage, '/teacher', { expectedText: '教师工作台' })
    const scopes = await teacherContext.request.get(`${api}/teacher/teaching-scopes`, { headers: { Authorization: `Bearer ${(await credentials(teacherContext, 'demo_teacher', 'TEACHER')).accessToken}` } }).then(response => response.json())
    if (scopes[0]?.teachingAssignmentId) await inspect(teacherPage, `/teacher/scopes/${scopes[0].teachingAssignmentId}`, { expectedText: '高频' })
    await inspect(teacherPage, '/teacher/ai-generation', { screenshot: 'teacher-ai-generation.png', expectedText: 'AI' })
    await inspect(teacherPage, '/messages', { expectedText: '消息' })
    const teacherAdminRedirect = await inspect(teacherPage, '/admin/ai-models')
    teacherAdminRedirect.accessDeniedByRedirect = teacherAdminRedirect.finalPath !== '/admin/ai-models'
    await teacherContext.close()

    const adminContext = await authenticatedContext(browser, 'demo_admin', 'ADMIN')
    const adminPage = await adminContext.newPage()
    const adminRoutes = ['/admin', '/admin/classes', '/admin/teachers', '/admin/students', '/admin/questions', '/admin/questions/import', '/admin/operation-logs']
    for (const route of adminRoutes) await inspect(adminPage, route)
    const models = await inspect(adminPage, '/admin/ai-models', { screenshot: 'admin-ai-models.png', expectedText: 'AI 模型' })
    const modelBody = await adminPage.locator('body').innerText()
    models.maskedKeys = modelBody.includes('••••••••') || modelBody.includes('已配置')
    models.noSecretPattern = !/(sk-[A-Za-z0-9]{16,}|[a-f0-9]{32}\.[A-Za-z0-9]{8,})/.test(modelBody)
    await inspect(adminPage, '/admin/ai-generation', { screenshot: 'admin-ai-generation.png', expectedText: 'AI' })
    await adminContext.close()

    const report = {
      generatedAt: new Date().toISOString(),
      routes: results,
      summary: {
        routesChecked: results.length,
        consoleErrors: results.reduce((sum, item) => sum + (item.consoleErrors?.length || 0), 0),
        pageErrors: results.reduce((sum, item) => sum + (item.pageErrors?.length || 0), 0),
        failedRequests: results.reduce((sum, item) => sum + (item.failedRequests?.length || 0), 0),
        overflowRoutes: results.filter(item => item.horizontalOverflow).map(item => item.route),
      },
    }
    fs.writeFileSync(path.join(evidence, 'browser-results.json'), JSON.stringify(report, null, 2))
    console.log(JSON.stringify(report.summary))
    if (report.summary.consoleErrors || report.summary.pageErrors || report.summary.failedRequests || report.summary.overflowRoutes.length) process.exitCode = 1
  } finally {
    await browser.close()
  }
}

main().catch(error => {
  console.error(error.message)
  process.exitCode = 1
})
