const { chromium } = require('playwright-core')
const fs = require('node:fs')
const path = require('node:path')

const frontend = process.env.RIKE_PAPER_FRONTEND || 'http://localhost:18080'
const api = process.env.RIKE_PAPER_API || 'http://localhost:18081/api/v1'
const evidence = path.join(process.cwd(), 'docs', 'evidence', 'paper-submission-sync')
fs.mkdirSync(evidence, { recursive: true })

async function login(browser, username, role) {
  const context = await browser.newContext({ viewport: { width: 1440, height: 1000 } })
  const challenge = await context.request.get(`${api}/auth/captcha-challenge`).then(response => response.json())
  const response = await context.request.post(`${api}/auth/login`, {
    data: { username, password: 'a1234567', expectedRole: role, challengeId: challenge.challengeId, captchaCode: challenge.testCode },
  })
  if (!response.ok()) throw new Error(`${role} login HTTP ${response.status()}`)
  const auth = await response.json()
  await context.setExtraHTTPHeaders({ Authorization: `Bearer ${auth.accessToken}` })
  await context.addInitScript(({ token, activeRole }) => {
    localStorage.setItem('rike-tiku.access-token', token)
    localStorage.setItem('rike-tiku.token-type', 'Bearer')
    sessionStorage.setItem('rike-tiku.active-role', activeRole)
  }, { token: auth.accessToken, activeRole: role })
  return context
}

async function main() {
  const browser = await chromium.launch({ headless: true, executablePath: 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe', args: ['--disable-gpu'] })
  const audit = { consoleErrors: [], pageErrors: [], failedRequests: [], submitBody: null, assertions: {} }
  try {
    const teacher = await login(browser, 'demo_physics_admin', 'TEACHER')
    const teacherPage = await teacher.newPage()
    teacherPage.on('console', message => { if (message.type() === 'error') audit.consoleErrors.push(message.text()) })
    teacherPage.on('pageerror', error => audit.pageErrors.push(String(error)))
    teacherPage.on('requestfailed', request => { if (!String(request.failure()?.errorText).includes('ERR_ABORTED')) audit.failedRequests.push(request.url()) })
    await teacherPage.goto(`${frontend}/teacher/papers`, { waitUntil: 'networkidle', timeout: 30000 })
    const paperRow = teacherPage.locator('.el-table__row').filter({ hasText: 'RIKE Demo 199' }).first()
    await paperRow.getByRole('button', { name: '更多操作' }).click()
    await teacherPage.locator('.el-dropdown-menu:visible').getByText('发布管理', { exact: true }).click()
    const releaseDialog = teacherPage.getByRole('dialog').filter({ hasText: '发布管理' })
    await releaseDialog.getByRole('button', { name: '作答情况' }).first().click()
    const statsDialog = teacherPage.getByRole('dialog').filter({ hasText: '班级作答情况' })
    await statsDialog.waitFor()
    audit.assertions.teacherStatsOpened = await statsDialog.isVisible()
    const submissionRows = await teacher.request.get(`${api}/teacher/papers/releases/1/submissions`).then(response => response.json())
    const targetStudent = submissionRows.find(row => row.status !== 'SUBMITTED')
    if (!targetStudent) throw new Error('No unsubmitted 199-class demo student remains for browser verification')

    const student = await login(browser, targetStudent.studentNumber.toLowerCase(), 'STUDENT')
    const studentPage = await student.newPage()
    studentPage.on('console', message => { if (message.type() === 'error') audit.consoleErrors.push(message.text()) })
    studentPage.on('pageerror', error => audit.pageErrors.push(String(error)))
    studentPage.on('requestfailed', request => { if (!String(request.failure()?.errorText).includes('ERR_ABORTED')) audit.failedRequests.push(request.url()) })
    studentPage.on('request', request => {
      if (request.url().endsWith('/student/papers/1/submit') && request.method() === 'POST') audit.submitBody = request.postDataJSON()
    })
    await studentPage.goto(`${frontend}/student/papers/1`, { waitUntil: 'networkidle', timeout: 30000 })
    const questions = studentPage.locator('.paper-question')
    audit.assertions.studentQuestionCount = await questions.count() === 3
    await questions.nth(0).locator('.el-radio').filter({ hasText: 'A.' }).click()
    await questions.nth(1).locator('.el-radio').filter({ hasText: 'A.' }).click()
    await questions.nth(2).locator('.el-checkbox').filter({ hasText: 'A.' }).click()
    await questions.nth(2).locator('.el-checkbox').filter({ hasText: 'B.' }).click()
    await studentPage.getByRole('button', { name: '提交试卷' }).click()
    await studentPage.locator('.el-message-box').getByRole('button', { name: '确认提交' }).click()
    await studentPage.getByText('提交成功：客观题自动得分 30/30', { exact: false }).waitFor({ timeout: 10000 })
    await studentPage.locator('.result').first().waitFor()
    await studentPage.locator('.el-message-box').waitFor({ state: 'hidden' })
    const resultText = await studentPage.locator('.question-list').innerText()
    audit.assertions.studentResult = resultText.includes('本题得分 10 / 10') && resultText.includes('学生答案') && resultText.includes('正确答案') && resultText.includes('STANDARD')
    audit.assertions.submitUsesItemIds = Array.isArray(audit.submitBody?.answers) && audit.submitBody.answers.length === 3 && audit.submitBody.answers.every(answer => Number.isInteger(answer.itemId))
    await studentPage.screenshot({ path: path.join(evidence, 'student-submitted-result.png'), fullPage: true })
    await studentPage.goto(`${frontend}/student/papers`, { waitUntil: 'networkidle' })
    audit.assertions.studentListUpdated = (await studentPage.locator('body').innerText()).includes('客观题自动得分 30 / 30')

    await statsDialog.getByText('30 / 30', { exact: false }).first().waitFor({ timeout: 12000 })
    const afterText = await statsDialog.innerText()
    audit.assertions.teacherPollingUpdated = afterText.includes('已提交') && afterText.includes('30 / 30') && afterText.includes('查看答卷')
    await statsDialog.getByRole('button', { name: '查看答卷' }).first().click()
    const answerDialog = teacherPage.getByRole('dialog').filter({ hasText: '学生已提交答卷' })
    await answerDialog.waitFor()
    const answerText = await answerDialog.innerText()
    audit.assertions.teacherSubmissionReadable = answerText.includes('学生答案') && answerText.includes('正确答案') && answerText.includes('客观得分：10 / 10') && answerText.includes('STANDARD 标准解析') && !answerText.includes('optionLabels')
    await teacherPage.screenshot({ path: path.join(evidence, 'teacher-submission-synced.png'), fullPage: true })
    audit.assertions.noOverflow = !(await teacherPage.evaluate(() => document.documentElement.scrollWidth > document.documentElement.clientWidth + 1))
    await student.close()
    await teacher.close()
  } finally {
    await browser.close()
  }
  const failures = Object.entries(audit.assertions).filter(([, passed]) => !passed).map(([name]) => name)
  const summary = { generatedAt: new Date().toISOString(), database: 'rike_tiku_demo', evidenceType: 'MACHINE_BROWSER_VERIFIED', ...audit, failures }
  fs.writeFileSync(path.join(evidence, 'results.json'), JSON.stringify(summary, null, 2))
  console.log(JSON.stringify({ assertions: Object.keys(audit.assertions).length, failures, consoleErrors: audit.consoleErrors.length, pageErrors: audit.pageErrors.length, failedRequests: audit.failedRequests.length }))
  if (failures.length || audit.consoleErrors.length || audit.pageErrors.length || audit.failedRequests.length) process.exitCode = 1
}

main().catch(error => { console.error(error); process.exitCode = 1 })
