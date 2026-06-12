/**
 * Debug 400 errors with full login flow
 */
import { chromium } from '@playwright/test';

const BASE_URL = 'http://localhost:5656';

(async () => {
  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext();
  const page = await context.newPage();

  // Collect all 400+ responses with URLs
  page.on('response', response => {
    if (response.status() === 400 && response.url().includes(BASE_URL)) {
      const url = response.url().replace(BASE_URL, '');
      console.log(`400: ${url}`);
    }
  });

  page.on('console', msg => {
    if (msg.type() === 'error' && msg.text().includes('400')) {
      console.log(`CONSOLE: ${msg.text()}`);
    }
  });

  // Login
  await page.goto(BASE_URL + '/login');
  await page.waitForTimeout(1000);
  await page.fill('input[name="username"]', 'admin');
  await page.fill('input[name="password"]', 'admin123');
  await page.click('button[type="submit"]');
  await page.waitForTimeout(3000);

  // Visit ERP dashboard
  await page.goto(BASE_URL + '/erp/dashboard', { waitUntil: 'networkidle' });
  await page.waitForTimeout(3000);

  console.log('--- visiting /erp/sale ---');
  await page.goto(BASE_URL + '/erp/sale', { waitUntil: 'networkidle' });
  await page.waitForTimeout(3000);

  await browser.close();
})();
