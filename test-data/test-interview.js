/**
 * AI Quality Test — Interview Coach
 * Chạy 1 phiên phỏng vấn: start → answer → get summary
 * Chạy: node test-interview.js
 */

const API_BASE = 'http://localhost:8080/api/v1';
let ACCESS_TOKEN = '';

const SESSIONS = [
  {
    label: 'Junior Java Backend Developer',
    jobPosition: 'Java Backend Developer',
    level: 'JUNIOR',
    totalQuestions: 3,
    answers: [
      'OOP là lập trình hướng đối tượng, có 4 tính chất: đóng gói, kế thừa, đa hình, trừu tượng. Đóng gói giúp ẩn dữ liệu bên trong class. Kế thừa cho phép class con kế thừa thuộc tính và phương thức từ class cha.',
      'Spring Boot giúp tạo ứng dụng Spring nhanh hơn vì nó có auto-configuration. Nó giúp giảm boilerplate code và có embedded server.',
      'RESTful API sử dụng các HTTP methods như GET, POST, PUT, DELETE. GET để lấy dữ liệu, POST để tạo mới, PUT để cập nhật, DELETE để xóa.'
    ]
  },
  {
    label: 'Mid React Frontend Developer',
    jobPosition: 'React Frontend Developer',
    level: 'MIDDLE',
    totalQuestions: 3,
    answers: [
      'Virtual DOM là bản sao nhẹ của DOM thật. Khi state thay đổi, React so sánh virtual DOM mới với cũ qua thuật toán diffing, chỉ cập nhật phần DOM thực sự thay đổi. Giúp tối ưu performance vì không phải re-render toàn bộ DOM.',
      'Trong dự án trước, tôi gặp race condition khi fetch data. Tôi dùng AbortController để cancel request cũ khi component unmount hoặc dependency thay đổi. Bọc trong custom hook useApi để reuse.',
      'Để tối ưu performance React app, tôi dùng React.memo cho pure components, useMemo cho expensive calculations, useCallback cho event handlers. Lazy loading routes bằng React.lazy và Suspense. Code splitting giúp giảm bundle size.'
    ]
  },
  {
    label: 'Senior System Architect',
    jobPosition: 'System Architect',
    level: 'SENIOR',
    totalQuestions: 3,
    answers: [
      'Thiết kế hệ thống chat real-time: Dùng WebSocket cho real-time communication, message broker (Kafka) cho message queue, Redis cho presence/session, PostgreSQL cho persistent storage. Horizontal scaling bằng consistent hashing. Rate limiting bằng token bucket. CDN cho media. Monitoring bằng Prometheus + Grafana.',
      'Microservices phù hợp khi team lớn (>20 người), cần deploy độc lập, các domain rõ ràng. Monolith phù hợp cho team nhỏ, MVP, khi domain chưa rõ ràng. Trade-off: microservices tăng complexity (network, data consistency) nhưng có scalability và team autonomy.',
      'Event sourcing lưu tất cả events thay vì state hiện tại. CQRS tách read và write models. Dùng khi cần audit trail, complex domain, high read/write ratio. Trade-off: eventual consistency, increased storage, phức tạp hơn. Trong dự án thanh toán, tôi dùng ES+CQRS để track mọi giao dịch, có thể replay để debug.'
    ]
  }
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
  console.log('✅ OK\n');
}

async function api(method, path, body = null) {
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), 120000);
  const opts = {
    method,
    headers: { 'Authorization': `Bearer ${ACCESS_TOKEN}`, 'Content-Type': 'application/json' },
    signal: controller.signal,
  };
  if (body) opts.body = JSON.stringify(body);
  
  try {
    const res = await fetch(`${API_BASE}${path}`, opts);
    clearTimeout(timeout);
    if (!res.ok) {
      const errText = await res.text();
      throw new Error(`HTTP ${res.status}: ${errText.substring(0, 200)}`);
    }
    const json = await res.json();
    return json.data || json;
  } catch (e) {
    clearTimeout(timeout);
    throw e;
  }
}

async function runSession(session) {
  console.log(`\n${'═'.repeat(60)}`);
  console.log(`  🎤 PHIÊN: ${session.label}`);
  console.log(`  Level: ${session.level} | Câu hỏi: ${session.totalQuestions}`);
  console.log(`${'═'.repeat(60)}\n`);

  try {
    // 1. Start interview
    console.log('📝 Khởi tạo phiên phỏng vấn...');
    const start = await api('POST', '/ai/interview/start', {
      jobPosition: session.jobPosition,
      level: session.level,
      totalQuestions: session.totalQuestions
    });
    
    const sessionId = start.sessionId;
    console.log(`  → Session ID: ${sessionId}`);
    console.log(`  → Q1 [${start.category}/${start.difficulty}]: ${start.question}`);
    
    const results = [];
    let nextQuestion = start;

    // 2. Answer each question
    for (let i = 0; i < session.totalQuestions; i++) {
      console.log(`\n--- Câu ${i+1}/${session.totalQuestions} ---`);
      console.log(`  ❓ ${nextQuestion.question}`);
      console.log(`  📋 Category: ${nextQuestion.category} | Difficulty: ${nextQuestion.difficulty}`);
      
      const answer = session.answers[i] || 'Tôi không biết câu trả lời cho câu hỏi này.';
      console.log(`  💬 Answer: ${answer.substring(0, 80)}...`);
      
      await new Promise(r => setTimeout(r, 3000)); // Rate limit

      console.log('  ⏳ Đang đánh giá...');
      const feedback = await api('POST', '/ai/interview/answer', {
        sessionId,
        answer
      });

      console.log(`  → Score: ${feedback.score}/100`);
      console.log(`  → Feedback: ${(feedback.feedback || '').substring(0, 120)}...`);
      console.log(`  → Better Answer: ${(feedback.betterAnswer || '').substring(0, 100)}...`);
      console.log(`  → Last question: ${feedback.lastQuestion}`);

      results.push({
        question: nextQuestion.question,
        category: nextQuestion.category,
        difficulty: nextQuestion.difficulty,
        answer: answer.substring(0, 50),
        score: feedback.score,
        feedback: feedback.feedback,
      });

      if (feedback.nextQuestion) {
        nextQuestion = feedback.nextQuestion;
      }
      
      await new Promise(r => setTimeout(r, 3000)); // Rate limit
    }

    // 3. Summary
    console.log(`\n📊 Kết quả phiên ${session.label}:`);
    results.forEach((r, i) => {
      console.log(`  Q${i+1} [${r.category}/${r.difficulty}]: Score ${r.score}/100`);
    });
    const avg = Math.round(results.reduce((s, r) => s + r.score, 0) / results.length);
    console.log(`  → Điểm trung bình: ${avg}/100`);

    return { label: session.label, avg, results };

  } catch (e) {
    console.log(`  ❌ ERROR: ${e.message}`);
    return { label: session.label, error: e.message };
  }
}

async function run() {
  await login();

  console.log('═══════════════════════════════════════════════════════════');
  console.log('  📊 INTERVIEW COACH QUALITY TEST — 3 Sessions');
  console.log('═══════════════════════════════════════════════════════════');

  const allResults = [];
  
  for (const session of SESSIONS) {
    const result = await runSession(session);
    allResults.push(result);
    await new Promise(r => setTimeout(r, 5000)); // Rate limit between sessions
  }

  // Final summary
  console.log(`\n${'═'.repeat(60)}`);
  console.log('  📋 FINAL SUMMARY');
  console.log(`${'═'.repeat(60)}`);
  allResults.forEach(r => {
    console.log(`  ${r.label}: ${r.avg ? r.avg + '/100' : 'ERROR'}`);
  });

  const fs = require('fs');
  const path = require('path');
  fs.writeFileSync(
    path.join(__dirname, 'interview-results.json'),
    JSON.stringify(allResults, null, 2)
  );
  console.log('\n💾 Saved to interview-results.json');
}

run().catch(console.error);
