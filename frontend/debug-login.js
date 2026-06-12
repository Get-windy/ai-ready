const puppeteer = require('C:\\Users\\Administrator\\AppData\\Roaming\\npm\\node_modules\\puppeteer-core');
const path = require('path');

(async () => {
  const browser = await puppeteer.launch({
    executablePath: 'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe',
    headless: true,
    args: ['--no-sandbox', '--disable-setuid-sandbox'],
  });
  const page = await browser.newPage();
  await page.setViewport({ width: 1920, height: 1080 });
  await page.goto('http://localhost:5656/login', { waitUntil: 'networkidle0', timeout: 30000 });
  await new Promise(r => setTimeout(r, 3000));

  // Get login page HTML structure
  const loginHtml = await page.evaluate(() => {
    const inputs = document.querySelectorAll('input');
    const buttons = document.querySelectorAll('button');
    const antInputs = document.querySelectorAll('.ant-input');
    return {
      inputCount: inputs.length,
      buttonCount: buttons.length,
      inputTypes: Array.from(inputs).map(i => ({
        type: i.type,
        name: i.name,
        placeholder: i.placeholder,
        className: (i.className || '').substring(0, 100),
        id: i.id,
      })),
      buttonTexts: Array.from(buttons).map(b => (b.textContent || '').substring(0, 50)),
      antInputCount: antInputs.length,
    };
  });
  console.log(JSON.stringify(loginHtml, null, 2));

  await page.screenshot({ path: path.join(__dirname, 'screenshots', 'login_debug.png'), fullPage: true });
  console.log('Screenshot saved');
  await browser.close();
})();
