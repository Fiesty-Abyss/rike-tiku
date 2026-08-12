const { chromium } = require('playwright-core')
const { existsSync } = require('node:fs')
const { join } = require('node:path')

const frontend = 'http://localhost:8080'
const chromeCandidates = [
  process.env.RIKE_PR32_CHROME,
  process.env.ProgramFiles && join(process.env.ProgramFiles, 'Google', 'Chrome', 'Application', 'chrome.exe'),
  process.env['ProgramFiles(x86)'] && join(process.env['ProgramFiles(x86)'], 'Google', 'Chrome', 'Application', 'chrome.exe'),
].filter(Boolean)
const chrome = chromeCandidates.find(existsSync)

if (!chrome) throw new Error('Chrome executable not found; set RIKE_PR32_CHROME for this local walkthrough')

async function inspect(page, route) {
  const record = { route, consoleErrors: [], pageErrors: [], failedRequests: [] }
  page.on('console', message => {
    if (message.type() === 'error') record.consoleErrors.push(message.text())
  })
  page.on('pageerror', error => record.pageErrors.push(String(error)))
  page.on('requestfailed', request => {
    const failure = request.failure()?.errorText || 'unknown'
    if (!failure.includes('ERR_ABORTED')) record.failedRequests.push({ url: request.url(), failure })
  })
  const response = await page.goto(`${frontend}${route}`, { waitUntil: 'networkidle', timeout: 30000 })
  record.status = response?.status() || null
  record.finalPath = new URL(page.url()).pathname
  record.bodyLength = (await page.locator('body').innerText()).trim().length
  record.horizontalOverflow = await page.evaluate(() => document.documentElement.scrollWidth > document.documentElement.clientWidth + 1)
  return record
}

async function main() {
  const browser = await chromium.launch({ headless: true, executablePath: chrome, args: ['--disable-gpu'] })
  try {
    const desktop = await browser.newPage({ viewport: { width: 1440, height: 1000 } })
    const portal = await inspect(desktop, '/')
    const login = await inspect(desktop, '/login')
    const captcha = await desktop.locator('img').last().getAttribute('src')
    login.realCaptchaImage = Boolean(captcha?.startsWith('data:image/png;base64,'))

    const mobile = await browser.newPage({ viewport: { width: 390, height: 844 } })
    const mobilePortal = await inspect(mobile, '/')

    const protectedPage = await browser.newPage({ viewport: { width: 1440, height: 1000 } })
    const protectedRoute = await inspect(protectedPage, '/admin/ai-models')
    protectedRoute.redirectedToLogin = protectedRoute.finalPath.startsWith('/login')

    console.log(JSON.stringify({ portal, login, mobilePortal, protectedRoute }, null, 2))
  } finally {
    await browser.close()
  }
}

main().catch(error => {
  console.error(error)
  process.exitCode = 1
})
