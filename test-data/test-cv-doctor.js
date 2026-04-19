/**
 * AI Quality Test Runner
 * Tự động test CV Doctor API với 10 CV mẫu
 * Chạy: node test-cv-doctor.js
 * Yêu cầu: Backend chạy tại localhost:8080, user đã đăng nhập
 */

const fs = require('fs');
const path = require('path');

const API_BASE = 'http://localhost:8080/api/v1';
let ACCESS_TOKEN = ''; // Sẽ lấy từ login

const CVS = [
  { file: 'cv01_senior_java_good.pdf', label: 'Senior Java (TỐT)', expected: '≥70' },
  { file: 'cv02_mid_react_good.pdf', label: 'Mid React (TỐT)', expected: '≥70' },
  { file: 'cv03_mid_fullstack_en.pdf', label: 'Mid Fullstack EN (TỐT)', expected: '≥70' },
  { file: 'cv04_senior_devops_good.pdf', label: 'Senior DevOps (TỐT)', expected: '≥70' },
  { file: 'cv05_junior_python_good.pdf', label: 'Junior Python (TỐT)', expected: '≥60' },
  { file: 'cv06_fresher_no_exp.pdf', label: 'Fresher no exp (KÉM)', expected: '<50' },
  { file: 'cv07_messy_format.pdf', label: 'Messy format (KÉM)', expected: '<50' },
  { file: 'cv08_no_skills_listed.pdf', label: 'No skills (KÉM)', expected: '<50' },
  { file: 'cv09_short_english.pdf', label: 'Short EN (KÉM)', expected: '<40' },
  { file: 'cv10_non_it_accountant.pdf', label: 'Kế toán (KÉM)', expected: '<30' },
];

async function login() {
  console.log('🔐 Đang đăng nhập...');
  try {
    const res = await fetch(`${API_BASE}/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username: 'admin@gmail.com', password: '123456' })
    });
    const data = await res.json();
    ACCESS_TOKEN = data.data?.access_token;
    if (!ACCESS_TOKEN) {
      console.error('❌ Login failed:', JSON.stringify(data));
      process.exit(1);
    }
    console.log('✅ Đăng nhập thành công!\n');
  } catch (err) {
    console.error('❌ Không kết nối được backend:', err.message);
    console.error('👉 Đảm bảo backend đang chạy tại localhost:8080');
    process.exit(1);
  }
}

async function analyzeCV(cvFile) {
  const filePath = path.join(__dirname, cvFile);
  const fileBuffer = fs.readFileSync(filePath);
  
  const formData = new FormData();
  formData.append('file', new Blob([fileBuffer], { type: 'application/pdf' }), cvFile);

  const res = await fetch(`${API_BASE}/ai/cv/analyze`, {
    method: 'POST',
    headers: { 'Authorization': `Bearer ${ACCESS_TOKEN}` },
    body: formData,
  });

  if (!res.ok) {
    const errText = await res.text();
    throw new Error(`HTTP ${res.status}: ${errText}`);
  }

  const data = await res.json();
  return data.data || data;
}

async function runTests() {
  await login();

  console.log('═══════════════════════════════════════════════════════════');
  console.log('  📊 CV DOCTOR QUALITY TEST — 10 CVs');
  console.log('═══════════════════════════════════════════════════════════\n');

  const results = [];

  for (let i = 0; i < CVS.length; i++) {
    const cv = CVS[i];
    console.log(`[${i+1}/10] Testing: ${cv.label} ...`);
    
    try {
      const startTime = Date.now();
      const result = await analyzeCV(cv.file);
      const duration = ((Date.now() - startTime) / 1000).toFixed(1);

      const pass = checkPass(result.overallScore, cv.expected);
      
      console.log(`  → Overall: ${result.overallScore} | F:${result.formatScore} C:${result.contentScore} K:${result.keywordScore} I:${result.impactScore} | ${duration}s | ${pass ? '✅' : '❌'}`);
      console.log(`  → Summary: ${(result.summary || '').substring(0, 100)}...`);
      console.log(`  → Strengths: ${(result.strengths || []).length} | Suggestions: ${(result.suggestions || []).length}`);
      console.log('');

      results.push({
        ...cv,
        overall: result.overallScore,
        format: result.formatScore,
        content: result.contentScore,
        keyword: result.keywordScore,
        impact: result.impactScore,
        summary: result.summary,
        strengths: (result.strengths || []).length,
        suggestions: (result.suggestions || []).length,
        duration: duration + 's',
        pass: pass ? '✅' : '❌'
      });

      // Delay 3s giữa mỗi request (rate limit)
      if (i < CVS.length - 1) {
        console.log('  ⏳ Waiting 3s (rate limit)...\n');
        await new Promise(r => setTimeout(r, 3000));
      }

    } catch (err) {
      console.log(`  → ❌ ERROR: ${err.message}\n`);
      results.push({ ...cv, overall: 'ERROR', pass: '❌', error: err.message });
    }
  }

  // Summary Table
  console.log('\n═══════════════════════════════════════════════════════════');
  console.log('  📋 SUMMARY TABLE');
  console.log('═══════════════════════════════════════════════════════════');
  console.log('CV                          | Overall | F  | C  | K  | I  | Time  | Pass');
  console.log('----------------------------|---------|----|----|----|----|-------|-----');
  results.forEach(r => {
    const name = r.label.padEnd(28);
    const o = String(r.overall).padStart(7);
    const f = String(r.format || '-').padStart(3);
    const c = String(r.content || '-').padStart(3);
    const k = String(r.keyword || '-').padStart(3);
    const imp = String(r.impact || '-').padStart(3);
    const t = String(r.duration || '-').padStart(5);
    console.log(`${name}| ${o} | ${f}| ${c}| ${k}| ${imp}| ${t} | ${r.pass}`);
  });

  const passCount = results.filter(r => r.pass === '✅').length;
  console.log(`\n🎯 Result: ${passCount}/${results.length} tests passed`);
  
  // Save results to file
  fs.writeFileSync(
    path.join(__dirname, 'cv-doctor-results.json'),
    JSON.stringify(results, null, 2)
  );
  console.log('\n💾 Results saved to cv-doctor-results.json');
}

function checkPass(score, expected) {
  if (typeof score !== 'number') return false;
  if (expected.startsWith('≥')) return score >= parseInt(expected.slice(1));
  if (expected.startsWith('<')) return score < parseInt(expected.slice(1));
  return true;
}

runTests().catch(console.error);
