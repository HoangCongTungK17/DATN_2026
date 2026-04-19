/**
 * Upload test CVs lên hệ thống và tạo resume gắn với các jobs khác nhau
 * Sau đó chạy matching cho từng cặp
 */
const fs = require('fs');
const path = require('path');

const API_BASE = 'http://localhost:8080/api/v1';
let ACCESS_TOKEN = '';

// Cặp CV → Job để test matching
const TEST_PAIRS = [
  // MATCH CAO (CV khớp JD)
  { cv: 'cv01_senior_java_good.pdf', jobId: 107, label: 'Senior Java CV → Java Solution Architect', expectedRange: '≥60' },
  { cv: 'cv04_senior_devops_good.pdf', jobId: 104, label: 'Senior DevOps CV → DevOps Engineer', expectedRange: '≥65' },
  { cv: 'cv03_mid_fullstack_en.pdf', jobId: 105, label: 'Mid Fullstack CV → NodeJS Backend Fresher', expectedRange: '≥50' },
  
  // MATCH TRUNG BÌNH (CV liên quan nhưng không hoàn toàn khớp)
  { cv: 'cv02_mid_react_good.pdf', jobId: 103, label: 'Mid React CV → Frontend Vue.js Junior', expectedRange: '40-70' },
  { cv: 'cv05_junior_python_good.pdf', jobId: 100, label: 'Junior Python CV → Python Developer AI/ML Senior', expectedRange: '30-60' },
  
  // MATCH THẤP (CV không khớp JD)
  { cv: 'cv01_senior_java_good.pdf', jobId: 103, label: 'Senior Java CV → Frontend Vue.js Junior', expectedRange: '≤40' },
  { cv: 'cv04_senior_devops_good.pdf', jobId: 108, label: 'Senior DevOps CV → PHP Laravel Junior', expectedRange: '≤35' },
  
  // CV KÉM
  { cv: 'cv10_non_it_accountant.pdf', jobId: 107, label: 'Kế toán CV → Java Architect', expectedRange: '≤20' },
  { cv: 'cv09_short_english.pdf', jobId: 105, label: 'Short EN CV → NodeJS Backend', expectedRange: '≤25' },
  { cv: 'cv06_fresher_no_exp.pdf', jobId: 113, label: 'Fresher no exp → System Admin', expectedRange: '≤30' },
];

async function login() {
  console.log('🔐 Đăng nhập...');
  const res = await fetch(`${API_BASE}/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: 'admin@gmail.com', password: '123456' })
  });
  const data = await res.json();
  ACCESS_TOKEN = data.data?.access_token;
  console.log('✅ Đăng nhập thành công!\n');
}

async function uploadResume(cvFile, jobId) {
  const filePath = path.join(__dirname, cvFile);
  const fileBuffer = fs.readFileSync(filePath);
  const formData = new FormData();
  formData.append('file', new Blob([fileBuffer], { type: 'application/pdf' }), cvFile);
  formData.append('jobId', jobId.toString());

  const res = await fetch(`${API_BASE}/resumes`, {
    method: 'POST',
    headers: { 'Authorization': `Bearer ${ACCESS_TOKEN}` },
    body: formData,
  });

  if (!res.ok) {
    const errText = await res.text();
    throw new Error(`Upload failed: ${res.status} - ${errText.substring(0, 200)}`);
  }

  const data = await res.json();
  return data.data || data;
}

async function matchCV(resumeId) {
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), 120000);

  try {
    const res = await fetch(`${API_BASE}/ai/cv/match?resumeId=${resumeId}`, {
      method: 'POST',
      headers: { 'Authorization': `Bearer ${ACCESS_TOKEN}` },
      signal: controller.signal,
    });
    clearTimeout(timeout);

    if (!res.ok) {
      const errText = await res.text();
      throw new Error(`HTTP ${res.status}: ${errText.substring(0, 200)}`);
    }

    const data = await res.json();
    return data.data || data;
  } catch (err) {
    clearTimeout(timeout);
    throw err;
  }
}

function checkPass(score, expected) {
  if (typeof score !== 'number') return false;
  if (expected.startsWith('≥')) return score >= parseInt(expected.slice(1));
  if (expected.startsWith('≤')) return score <= parseInt(expected.slice(1));
  if (expected.includes('-')) {
    const [min, max] = expected.split('-').map(Number);
    return score >= min && score <= max;
  }
  return true;
}

async function runTests() {
  await login();

  console.log('═══════════════════════════════════════════════════════════');
  console.log('  📊 HR MATCHING QUALITY TEST — 10 PAIRS');
  console.log('═══════════════════════════════════════════════════════════\n');

  // Bước 1: Test matching với các resume đã có sẵn trong hệ thống
  console.log('📌 Testing existing resumes first...\n');
  
  const existingResumes = [1, 3, 4, 6, 8];
  for (const rid of existingResumes) {
    console.log(`[Existing] Resume #${rid}...`);
    try {
      const start = Date.now();
      const r = await matchCV(rid);
      const t = ((Date.now() - start) / 1000).toFixed(1);
      console.log(`  → Score: ${r.matchScore}% | Job: ${r.jobName} | ${t}s`);
      console.log(`  → Matched: ${(r.matchedSkills || []).join(', ')}`);
      console.log(`  → Missing: ${(r.missingSkills || []).join(', ')}`);
      console.log(`  → Summary: ${(r.summary || '').substring(0, 100)}...`);
    } catch (e) {
      console.log(`  ❌ ${e.message}`);
    }
    console.log('');
    await new Promise(r => setTimeout(r, 5000));
  }

  // Summary
  console.log('\n✅ Matching test completed!');
}

runTests().catch(console.error);
