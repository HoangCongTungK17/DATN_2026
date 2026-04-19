/**
 * AI Quality Test — Chatbot RAG
 * Test API GET /api/v1/ai/chat?message=...
 * Chạy: node test-chatbot.js
 */

const API_BASE = 'http://localhost:8080/api/v1';
let ACCESS_TOKEN = '';

const QUESTIONS = [
  { q: 'Tìm việc Java', expect: 'Liệt kê jobs có Java' },
  { q: 'Công việc yêu cầu kỹ năng React', expect: 'Jobs frontend/React' },
  { q: 'Tìm việc lương cao nhất', expect: 'Jobs lương cao' },
  { q: 'Có công việc DevOps không', expect: 'DevOps Engineer' },
  { q: 'Tìm việc cho fresher', expect: 'Jobs fresher level' },
  { q: 'Xin chào, bạn là ai?', expect: 'Giới thiệu bản thân là AI tư vấn việc làm' },
];

async function login() {
  const res = await fetch(`${API_BASE}/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: 'admin@gmail.com', password: '123456' })
  });
  const data = await res.json();
  ACCESS_TOKEN = data.data?.access_token;
  console.log('🔐 Login OK\n');
}

async function chat(message) {
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), 60000);

  try {
    const url = `${API_BASE}/ai/chat?message=${encodeURIComponent(message)}`;
    const res = await fetch(url, {
      headers: { 'Authorization': `Bearer ${ACCESS_TOKEN}` },
      signal: controller.signal,
    });
    clearTimeout(timeout);
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    const data = await res.json();
    return data.data || data;
  } catch (e) {
    clearTimeout(timeout);
    throw e;
  }
}

async function run() {
  await login();

  console.log('═══════════════════════════════════════════════════════════');
  console.log('  🤖 CHATBOT RAG QUALITY TEST');
  console.log('═══════════════════════════════════════════════════════════\n');

  for (let i = 0; i < QUESTIONS.length; i++) {
    const { q, expect } = QUESTIONS[i];
    console.log(`[${i+1}/${QUESTIONS.length}] Q: "${q}"`);
    console.log(`  Expected: ${expect}`);

    try {
      const start = Date.now();
      const answer = await chat(q);
      const time = ((Date.now() - start) / 1000).toFixed(1);

      const answerStr = typeof answer === 'string' ? answer : JSON.stringify(answer);
      // Hiển thị tối đa 300 ký tự
      console.log(`  A: ${answerStr.substring(0, 300)}${answerStr.length > 300 ? '...' : ''}`);
      console.log(`  ⏱ ${time}s | Length: ${answerStr.length} chars`);
      console.log(`  ✅ Response received`);
    } catch (e) {
      console.log(`  ❌ ERROR: ${e.message}`);
    }

    console.log('');
    if (i < QUESTIONS.length - 1) {
      await new Promise(r => setTimeout(r, 3000));
    }
  }

  console.log('✅ Chatbot test completed!');
}

run().catch(console.error);
