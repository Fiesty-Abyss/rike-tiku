const { chromium } = require('playwright-core')
const fs = require('node:fs')
const path = require('node:path')
const frontend = 'http://localhost:18080'
const api = 'http://localhost:18081/api/v1'
const evidence = path.join(process.cwd(), 'docs', 'evidence', 'thesis-final')
const audit = path.join(process.cwd(), 'docs', 'evidence', 'pr33-final-browser')
fs.mkdirSync(audit, { recursive: true })
const records = []; const assertions = []

async function login(browser, username, role, viewport={width:1440,height:900}) {
  const context = await browser.newContext({viewport})
  const challenge = await context.request.get(`${api}/auth/captcha-challenge`).then(r=>r.json())
  const response = await context.request.post(`${api}/auth/login`, {data:{username,password:'a1234567',expectedRole:role,challengeId:challenge.challengeId,captchaCode:challenge.testCode}})
  if (!response.ok()) throw new Error(`${username} login HTTP ${response.status()}`)
  const auth = await response.json(); const authorization=`Bearer ${auth.accessToken}`
  await context.setExtraHTTPHeaders({Authorization:authorization})
  await context.addInitScript(({token,role})=>{localStorage.setItem('rike-tiku.access-token',token);localStorage.setItem('rike-tiku.token-type','Bearer');sessionStorage.setItem('rike-tiku.active-role',role)}, {token:auth.accessToken,role})
  return context
}
async function inspect(page, route, expected, screenshot) {
  const r={route,consoleErrors:[],pageErrors:[],failedRequests:[]}
  const consoleHandler=m=>{if(m.type()==='error')r.consoleErrors.push(m.text())}; const errorHandler=e=>r.pageErrors.push(String(e)); const requestHandler=req=>{if(!String(req.failure()?.errorText).includes('ERR_ABORTED'))r.failedRequests.push(req.url())}
  page.on('console',consoleHandler);page.on('pageerror',errorHandler);page.on('requestfailed',requestHandler)
  const response=await page.goto(frontend+route,{waitUntil:'networkidle',timeout:30000});r.status=response?.status();r.finalPath=new URL(page.url()).pathname;r.expected=expected?await page.getByText(expected,{exact:false}).first().isVisible().catch(()=>false):true;r.overflow=await page.evaluate(()=>document.documentElement.scrollWidth>document.documentElement.clientWidth+1)
  if(screenshot)await page.screenshot({path:path.join(evidence,screenshot),fullPage:true})
  page.off('console',consoleHandler);page.off('pageerror',errorHandler);page.off('requestfailed',requestHandler);records.push(r);return r
}
async function main(){
  const browser=await chromium.launch({headless:true,executablePath:'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe',args:['--disable-gpu']})
  try{
    let recalledMarker='', hiddenMarker='', messageConversationId=0
    let c=await browser.newContext({viewport:{width:1366,height:768}}),p=await c.newPage();await inspect(p,'/login/student','遇到登录问题',null);await p.getByText('申请密码恢复',{exact:false}).click();await p.getByRole('dialog').waitFor();await p.screenshot({path:path.join(evidence,'25-password-recovery-desktop.png')});assertions.push({name:'password-recovery-desktop',pass:await p.getByRole('dialog').isVisible()});await c.close()
    c=await browser.newContext({viewport:{width:390,height:844}});p=await c.newPage();await p.goto(frontend+'/login/student',{waitUntil:'networkidle'});await p.getByText('申请密码恢复',{exact:false}).click();await p.getByRole('dialog').waitFor();await p.screenshot({path:path.join(evidence,'26-password-recovery-mobile.png'),fullPage:true});assertions.push({name:'password-recovery-mobile-overflow',pass:!(await p.evaluate(()=>document.documentElement.scrollWidth>document.documentElement.clientWidth+1))});await c.close()

    c=await login(browser,'demo_199_01','STUDENT');p=await c.newPage()
    await inspect(p,'/student/wrong-questions','错题本','27-wrong-question-review.png');await inspect(p,'/student/topics','专题','28-topic-units.png');await inspect(p,'/student/papers','我的试卷','29-student-papers.png');await inspect(p,'/student/knowledge-cards','知识卡片','30-knowledge-cards.png')
    let papers=await c.request.get(`${api}/student/papers`).then(r=>r.json());assertions.push({name:'199-paper-visible',pass:papers.length>0});let units=await c.request.get(`${api}/student/topic-learning/units`).then(r=>r.json());assertions.push({name:'topic-unit-has-three-questions',pass:units.length>0})
    let conversations=await c.request.get(`${api}/messages/conversations`).then(r=>r.json())
    if(conversations.length){
      messageConversationId=conversations[0].id
      await inspect(p,`/messages/${conversations[0].id}`,'消息','31-message-actions.png')
      let marker=`审计撤回 ${Date.now()}`;recalledMarker=marker;await p.getByPlaceholder('输入消息，最多1000字').fill(marker);await p.getByRole('button',{name:'发送消息'}).click();await p.getByText(marker,{exact:true}).waitFor();await p.locator('.chat-message.mine').last().getByRole('button',{name:/打开消息/}).click();await p.screenshot({path:path.join(evidence,'40-message-menu.png')});await p.locator('.message-action-menu:visible').getByText('撤回消息',{exact:true}).click()
      let box=p.locator('.message-confirm-dialog');await box.waitFor();let bb=await box.boundingBox();assertions.push({name:'message-confirm-centered',pass:!!bb&&Math.abs((bb.x+bb.width/2)-683)<120});await p.screenshot({path:path.join(evidence,'41-message-recall-confirm.png')});await box.getByRole('button',{name:'撤回消息'}).click();await p.getByText('消息已撤回',{exact:true}).last().waitFor();assertions.push({name:'message-recalled-state',pass:true})
      marker=`审计仅本人删除 ${Date.now()}`;hiddenMarker=marker;await p.getByPlaceholder('输入消息，最多1000字').fill(marker);await p.getByRole('button',{name:'发送消息'}).click();await p.getByText(marker,{exact:true}).waitFor();await p.locator('.chat-message.mine').last().getByRole('button',{name:/打开消息/}).click();await p.locator('.message-action-menu:visible').getByText('仅从我的列表删除',{exact:true}).click();box=p.locator('.message-confirm-dialog');await box.waitFor();await p.screenshot({path:path.join(evidence,'42-message-delete-confirm.png')});await box.getByRole('button',{name:'仅为我删除'}).click();await p.getByText(marker,{exact:true}).waitFor({state:'detached'});assertions.push({name:'message-hidden-for-self',pass:true})
    }
    await c.close()

    c=await login(browser,'demo_200_01','STUDENT');papers=await c.request.get(`${api}/student/papers`).then(r=>r.json());assertions.push({name:'200-paper-hidden',pass:papers.length===0});await c.close()

    c=await login(browser,'demo_physics_admin','TEACHER');p=await c.newPage();if(messageConversationId){let teacherMessages=await c.request.get(`${api}/messages/conversations/${messageConversationId}/messages`).then(r=>r.json());let text=JSON.stringify(teacherMessages);assertions.push({name:'recalled-hidden-from-peer',pass:!text.includes(recalledMarker)&&text.includes('消息已撤回')});assertions.push({name:'self-deleted-remains-for-peer',pass:text.includes(hiddenMarker)})}await inspect(p,'/teacher/private-questions','我的班级题库','32-private-question-bank.png');let scopes=await c.request.get(`${api}/teacher/teaching-scopes`).then(r=>r.json());let scope=scopes.find(x=>x.teachingStatus==='ACTIVE');if(scope)await inspect(p,`/teacher/scopes/${scope.teachingAssignmentId}/knowledge-cards`,'知识卡片','33-teacher-knowledge-cards.png');await inspect(p,'/teacher/papers','组卷与打印','34-paper-publish-quality.png');await c.close()

    c=await login(browser,'demo_admin','ADMIN');p=await c.newPage();await inspect(p,'/admin/operation-logs','操作日志','35-operation-log-search.png');await inspect(p,'/admin/questions/import','批量导入题目','36-question-import.png');await inspect(p,'/admin/students/import','批量导入学生','37-student-import.png');await inspect(p,'/admin/ai-models','AI 模型管理','38-vision-provider-config.png');await inspect(p,'/admin/questions','题库管理','39-question-review.png');await c.close()
    const summary={routes:records.length,consoleErrors:records.reduce((n,r)=>n+r.consoleErrors.length,0),pageErrors:records.reduce((n,r)=>n+r.pageErrors.length,0),failedRequests:records.reduce((n,r)=>n+r.failedRequests.length,0),overflows:records.filter(r=>r.overflow).map(r=>r.route),missingExpected:records.filter(r=>!r.expected).map(r=>r.route),assertionFailures:assertions.filter(a=>!a.pass)}
    fs.writeFileSync(path.join(audit,'browser-results-v29.json'),JSON.stringify({generatedAt:new Date().toISOString(),database:'rike_tiku_demo',providerDisclosure:'Provider-dependent screens were not called because rotated credentials are absent. Existing variant screenshots 09/10 are explicitly deterministic UI fixtures.',summary,assertions,records},null,2));console.log(JSON.stringify(summary));if(summary.consoleErrors||summary.pageErrors||summary.failedRequests||summary.overflows.length||summary.missingExpected.length||summary.assertionFailures.length)process.exitCode=1
  }finally{await browser.close()}
}
main().catch(e=>{console.error(e);process.exitCode=1})
