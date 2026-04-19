/**
 * Quick test v2 - 2 CV bị fail, with large timeout
 */
const fs = require('fs');
const path = require('path');

const API_BASE = 'http://localhost:8080/api/v1';
let ACCESS_TOKEN = '';

async function login() {
  const res = await fetch(`${API_BASE}/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: 'admin@gmail.com', password: '123456' })
  });
  const data = await res.json();
  ACCESS_TOKEN = data.data?.access_token;
  console.log('Login OK');
}

async function analyzeCV(cvFile) {
  const filePath = path.join(__dirname, cvFile);
  const fileBuffer = fs.readFileSync(filePath);
  const formData = new FormData();
  formData.append('file', new Blob([fileBuffer], { type: 'application/pdf' }), cvFile);

  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), 120000); // 120s timeout

  try {
    const res = await fetch(`${API_BASE}/ai/cv/analyze`, {
      method: 'POST',
      headers: { 'Authorization': `Bearer ${ACCESS_TOKEN}` },
      body: formData,
      signal: controller.signal,
    });
    clearTimeout(timeout);
    const data = await res.json();
    return data.data || data;
  } catch (err) {
    clearTimeout(timeout);
    throw err;
  }
}

async function run() {
  await login();

  // Wait 5s to avoid rate limit
  console.log('Waiting 5s for rate limit cooldown...');
  await new Promise(r => setTimeout(r, 5000));

  // Test CV08: No skills (expected < 50)
  console.log('\n[1] Testing CV08 (No skills) - expected < 50...');
  try {
    const start = Date.now();
    const r1 = await analyzeCV('cv08_no_skills_listed.pdf');
    const time = ((Date.now() - start) / 1000).toFixed(1);
    console.log(`  Overall: ${r1.overallScore} | F:${r1.formatScore} C:${r1.contentScore} K:${r1.keywordScore} I:${r1.impactScore} | ${time}s`);
    console.log(`  Summary: ${(r1.summary || '').substring(0, 150)}`);
    console.log(`  ${r1.overallScore < 50 ? '✅ PASS' : '❌ FAIL (expected < 50, got ' + r1.overallScore + ')'}`);
  } catch (e) {
    console.log(`  ❌ ERROR: ${e.message}`);
  }

  console.log('\nWaiting 5s...');
  await new Promise(r => setTimeout(r, 5000));

  // Test CV10: Kế toán (expected < 30)
  console.log('\n[2] Testing CV10 (Kế toán non-IT) - expected < 30...');
  try {
    const start = Date.now();
    const r2 = await analyzeCV('cv10_non_it_accountant.pdf');
    const time = ((Date.now() - start) / 1000).toFixed(1);
    console.log(`  Overall: ${r2.overallScore} | F:${r2.formatScore} C:${r2.contentScore} K:${r2.keywordScore} I:${r2.impactScore} | ${time}s`);
    console.log(`  Summary: ${(r2.summary || '').substring(0, 150)}`);
    console.log(`  ${r2.overallScore < 30 ? '✅ PASS' : '❌ FAIL (expected < 30, got ' + r2.overallScore + ')'}`);
  } catch (e) {
    console.log(`  ❌ ERROR: ${e.message}`);
  }
}

run().catch(console.error);
