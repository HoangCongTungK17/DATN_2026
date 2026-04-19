const PDFDocument = require('pdfkit');
const fs = require('fs');
const path = require('path');

const FONT_PATH = path.join(__dirname, 'Roboto-Regular.ttf');

const CVS = [
  // ═══════ 5 CV TỐT ═══════
  {
    filename: 'cv01_senior_java_good.pdf',
    content: `NGUYỄN VĂN ANH
Senior Java Backend Developer
Email: vananh.dev@gmail.com | Phone: 0912 345 678
LinkedIn: linkedin.com/in/vananh-dev | GitHub: github.com/vananh-dev
Địa chỉ: Hà Nội, Việt Nam

===== TÓM TẮT =====
5+ năm kinh nghiệm phát triển backend với Java/Spring Boot. Chuyên xây dựng hệ thống microservices quy mô lớn phục vụ hàng triệu người dùng. Kinh nghiệm làm việc tại FPT Software và Viettel.

===== KỸ NĂNG =====
- Ngôn ngữ: Java 17, Kotlin, Python, SQL
- Framework: Spring Boot, Spring Cloud, Hibernate, JPA
- Database: MySQL, PostgreSQL, Redis, MongoDB, Elasticsearch
- DevOps: Docker, Kubernetes, Jenkins, GitLab CI/CD, AWS (EC2, S3, RDS, Lambda)
- Tools: Git, Jira, Confluence, IntelliJ IDEA, Postman
- Architecture: Microservices, Event-Driven, RESTful API, gRPC, Message Queue (Kafka, RabbitMQ)

===== KINH NGHIỆM =====

Senior Backend Developer | FPT Software | 01/2023 - Hiện tại
- Thiết kế và phát triển hệ thống microservices cho nền tảng e-commerce phục vụ 2M+ users
- Tối ưu query database, giảm 40% response time từ 800ms xuống 480ms
- Triển khai caching layer (Redis) giúp giảm 60% load database
- Xây dựng CI/CD pipeline tự động hoá deploy, giảm thời gian release từ 2 ngày xuống 2 giờ
- Tech lead team 5 người, code review hàng tuần, mentoring 2 junior developers

Backend Developer | Viettel | 06/2020 - 12/2022
- Phát triển RESTful APIs cho hệ thống quản lý thuê bao (3M+ records)
- Xây dựng batch processing system xử lý 500K+ transactions/ngày
- Viết unit tests đạt 85% code coverage
- Tham gia performance tuning, tăng throughput từ 200 TPS lên 800 TPS

===== HỌC VẤN =====
Đại học Bách Khoa Hà Nội | Kỹ sư CNTT | 2016-2021 | GPA: 3.5/4.0

===== CHỨNG CHỈ =====
- AWS Certified Solutions Architect – Associate (2023)
- Oracle Certified Professional Java SE 17 (2022)`
  },
  {
    filename: 'cv02_mid_react_good.pdf',
    content: `TRẦN THỊ BÌNH
Mid-level React Frontend Developer
Email: binh.tran@gmail.com | Phone: 0987 654 321
Portfolio: binhdev.io | GitHub: github.com/binh-dev

===== TÓM TẮT =====
3 năm kinh nghiệm phát triển web frontend với React. Đam mê tạo giao diện người dùng đẹp và hiệu suất cao. Có kinh nghiệm với TypeScript, Redux, và Next.js.

===== KỸ NĂNG =====
- Frontend: React 18, TypeScript, Next.js, Redux Toolkit, React Query
- Styling: Tailwind CSS, SASS/SCSS, Ant Design, Material UI
- Testing: Jest, React Testing Library, Cypress
- Tools: Git, Figma, VS Code, Webpack, Vite
- Other: RESTful API, GraphQL, WebSocket, PWA, Responsive Design

===== KINH NGHIỆM =====

Frontend Developer | Shopee Vietnam | 03/2022 - Hiện tại
- Phát triển và bảo trì trang seller center phục vụ 500K+ shop
- Xây dựng component library (30+ components) giúp giảm 50% thời gian development
- Tối ưu First Contentful Paint từ 3.2s xuống 1.5s bằng code splitting và lazy loading
- Implement real-time notification system bằng WebSocket
- Collaboration với UX team qua Figma, tham gia design review hàng tuần

Junior Frontend Developer | Grab Vietnam | 06/2021 - 02/2022
- Phát triển dashboard quản lý cho nội bộ team operations
- Viết unit tests đạt 80% coverage
- Fix 150+ bugs trong 9 tháng, cải thiện user satisfaction 15%

===== DỰ ÁN CÁ NHÂN =====
E-Commerce Platform (React + Node.js): Full-stack e-commerce app với payment integration, filter, cart. 2000+ stars GitHub.

===== HỌC VẤN =====
Đại học FPT | Kỹ sư Phần mềm | 2017-2021 | GPA: 3.7/4.0`
  },
  {
    filename: 'cv03_mid_fullstack_en.pdf',
    content: `LE MINH CUONG
Fullstack Developer
Email: cuong.le@email.com | Phone: +84 903 456 789
GitHub: github.com/cuong-le | LinkedIn: linkedin.com/in/cuong-fullstack

SUMMARY
3 years of experience in full-stack web development using Node.js, React, and cloud services.
Built and deployed production applications serving 100K+ monthly active users.

SKILLS
- Backend: Node.js, Express.js, NestJS, Python (Django), Java (Spring Boot basics)
- Frontend: React, Vue.js, TypeScript, HTML5, CSS3, SASS
- Database: PostgreSQL, MongoDB, Redis, Firebase
- Cloud: AWS (EC2, S3, Lambda, DynamoDB), Google Cloud Platform
- DevOps: Docker, GitHub Actions, Nginx, PM2
- Other: REST API, GraphQL, WebSocket, Agile/Scrum

EXPERIENCE

Fullstack Developer | VNG Corporation | 02/2022 - Present
- Developed and maintained ZaloPay merchant dashboard serving 50K+ merchants
- Built real-time analytics dashboard processing 1M+ events/day
- Implemented OAuth2 SSO reducing login friction by 25%
- Migrated legacy PHP codebase to Node.js, improving response time by 35%

Junior Developer | Tiki Corporation | 08/2021 - 01/2022
- Contributed to product catalog system handling 10M+ SKUs
- Built admin tools for content management team
- Participated in Agile sprints, completed 95% of sprint stories on time

PROJECTS
Task Management App: Built with React + NestJS, 500+ daily active users
AI Chat Widget: Integrated GPT API into customer support system

EDUCATION
HCMC University of Technology | Computer Science | 2017-2021 | GPA: 3.4/4.0

CERTIFICATIONS
- AWS Cloud Practitioner (2023)
- MongoDB Associate Developer (2022)`
  },
  {
    filename: 'cv04_senior_devops_good.pdf',
    content: `PHẠM ĐÌNH DŨNG
Senior DevOps / SRE Engineer
Email: dung.pham.devops@gmail.com | Phone: 0905 111 222
GitHub: github.com/dungpham-ops

===== TÓM TẮT =====
6 năm kinh nghiệm DevOps/SRE, chuyên xây dựng và vận hành hạ tầng cloud-native. Kinh nghiệm quản lý hệ thống phục vụ 5M+ users với uptime 99.95%.

===== KỸ NĂNG =====
- Cloud: AWS (EC2, ECS, EKS, RDS, S3, CloudFront, Route53, IAM), GCP (GKE, BigQuery)
- Container: Docker, Kubernetes, Helm, Istio
- IaC: Terraform, CloudFormation, Ansible, Pulumi
- CI/CD: Jenkins, GitLab CI, GitHub Actions, ArgoCD
- Monitoring: Prometheus, Grafana, ELK Stack, Datadog, PagerDuty
- Scripting: Bash, Python, Go
- Database: MySQL, PostgreSQL, Redis, MongoDB
- Security: HashiCorp Vault, AWS KMS, SSL/TLS, IAM policies

===== KINH NGHIỆM =====

Senior DevOps Engineer | MoMo | 01/2022 - Hiện tại
- Quản lý Kubernetes cluster 200+ pods phục vụ hệ thống thanh toán 5M+ users
- Thiết kế và triển khai blue-green deployment, zero-downtime releases
- Giảm chi phí cloud 35% ($50K/tháng) bằng auto-scaling và spot instances
- Xây dựng monitoring stack (Prometheus + Grafana) với 500+ dashboards
- Incident response: giảm MTTR từ 45 phút xuống 12 phút

DevOps Engineer | Tiki | 06/2019 - 12/2021
- Migrate hệ thống từ on-premise lên AWS, phục vụ 10M+ sản phẩm
- Xây dựng CI/CD pipeline cho 50+ microservices
- Triển khai ELK stack cho centralized logging (20GB logs/ngày)

===== HỌC VẤN =====
Đại học Bách Khoa TP.HCM | Kỹ sư Mạng & ATTT | 2014-2019

===== CHỨNG CHỈ =====
- AWS Solutions Architect Professional (2023)
- Certified Kubernetes Administrator - CKA (2022)
- HashiCorp Terraform Associate (2021)`
  },
  {
    filename: 'cv05_junior_python_good.pdf',
    content: `HOÀNG THỊ EM
Junior Python Developer
Email: em.hoang.py@gmail.com | Phone: 0908 333 444
GitHub: github.com/em-python

===== TÓM TẮT =====
Sinh viên mới tốt nghiệp với 1 năm kinh nghiệm thực tập Python. Đam mê AI/ML và backend development. Có 5 projects cá nhân trên GitHub.

===== KỸ NĂNG =====
- Ngôn ngữ: Python, JavaScript, SQL, HTML/CSS
- Framework: Django, Flask, FastAPI
- Database: PostgreSQL, SQLite, MongoDB
- AI/ML: TensorFlow, PyTorch, Pandas, NumPy, Scikit-learn
- Tools: Git, Docker (cơ bản), VS Code, Jupyter Notebook
- Other: REST API, Linux, Agile basics

===== KINH NGHIỆM =====

Python Intern -> Junior Developer | Zalo AI | 06/2025 - Hiện tại
- Phát triển API backend bằng FastAPI cho chatbot nội bộ
- Xây dựng data pipeline xử lý 100K+ records/ngày bằng Pandas
- Tham gia fine-tuning Vietnamese NLP model (PhoBERT)
- Viết documentation cho 10+ API endpoints

===== DỰ ÁN CÁ NHÂN =====
1. Movie Recommendation System (Python, Flask, ML): Content-based filtering, 85% accuracy
2. COVID-19 Dashboard (Django, Chart.js): Real-time data từ API, 1K visits/ngày
3. Expense Tracker API (FastAPI, PostgreSQL): CRUD + authentication + tests
4. Vietnamese Sentiment Analysis (PyTorch, PhoBERT): 90% accuracy trên Vietnamese reviews
5. Portfolio Website (HTML, CSS, JS): Personal portfolio showcase

===== HỌC VẤN =====
Đại học KHTN TP.HCM | Cử nhân CNTT | 2021-2025 | GPA: 3.6/4.0
- Giải 3 cuộc thi lập trình sinh viên cấp trường 2024
- Thành viên CLB AI Research`
  },

  // ═══════ 5 CV KÉM ═══════
  {
    filename: 'cv06_fresher_no_exp.pdf',
    content: `Nguyen Van F
Email: nguyenvanf@gmail.com
Sdt: 0123456789
Dia chi: Ha Noi

Hoc van: Tot nghiep Dai hoc Bach Khoa Ha Noi nganh CNTT nam 2025

Ky nang: Biet dung may tinh, Word, Excel, PowerPoint

Muc tieu: Tim viec lam on dinh trong linh vuc CNTT

So thich: Doc sach, choi game, co tuong`
  },
  {
    filename: 'cv07_messy_format.pdf',
    content: `toi la nguyen van g toi da hoc o dai hoc fpt va tot nghiep nam 2024 toi biet lam web co ban va da tung lam du an cuoi ky bang html css nhung chua biet nhieu ve javascript va cac framework hien dai toi muon tim mot cong viec developer va co the hoc them trong qua trinh lam viec toi da tung thuc tap 2 thang tai mot cong ty nho nhung cong ty do da dong cua toi cung biet mot chut ve database nhu mysql va da tung cai dat va su dung no cho du an cuoi ky email cua toi la nguyenvang@gmail.com so dien thoai 0909123456`
  },
  {
    filename: 'cv08_no_skills_listed.pdf',
    content: `TRẦN VĂN H
Mid-Level Developer
Email: tranvanh@yahoo.com | Phone: 0911222333

Thông tin cá nhân:
Sinh năm 1998, hiện sống tại TP.HCM

Kinh nghiệm:
- Làm việc tại công ty ABC từ 2022
- Tham gia phát triển các sản phẩm phần mềm
- Có kinh nghiệm làm việc nhóm
- Biết sử dụng máy tính thành thạo
- Đã hoàn thành nhiều dự án được giao

Học vấn:
Cử nhân CNTT - Đại học Mở TP.HCM - 2020

Người tham chiếu: Có thể cung cấp khi được yêu cầu`
  },
  {
    filename: 'cv09_short_english.pdf',
    content: `John Le
Email: john@email.com

I am looking for a junior developer position.
I graduated from university last year.
I know some programming.
I am a fast learner.`
  },
  {
    filename: 'cv10_non_it_accountant.pdf',
    content: `NGUYỄN THỊ KIM LOAN
Kế Toán Trưởng
Email: kimloan.kt@gmail.com | Phone: 0933 555 666

===== TÓM TẮT =====
15 năm kinh nghiệm kế toán tài chính. Chuyên gia về thuế, báo cáo tài chính, và kiểm toán.

===== KỸ NĂNG =====
- Phần mềm kế toán: MISA, Fast Accounting, SAP FI/CO
- Microsoft Office: Excel nâng cao (VBA, Pivot), Word, PowerPoint
- Kiến thức: Chuẩn mực kế toán VAS, IFRS, Luật thuế Việt Nam

===== KINH NGHIỆM =====

Kế Toán Trưởng | Công ty CP ABC | 2018 - Hiện tại
- Quản lý phòng kế toán 8 nhân viên
- Lập báo cáo tài chính hàng quý cho ban giám đốc
- Giảm 20% chi phí thuế thông qua tối ưu hóa kế toán thuế GTGT
- Hoàn thành kiểm toán Big4 3 năm liên tiếp không phát hiện sai sót trọng yếu

Kế Toán Viên | Công ty TNHH XYZ | 2010 - 2017
- Xử lý 500+ chứng từ kế toán mỗi tháng
- Lập tờ khai thuế GTGT, TNDN, TNCN hàng tháng/quý

===== HỌC VẤN =====
Đại học Kinh Tế TP.HCM | Cử nhân Kế Toán | 2006-2010

===== CHỨNG CHỈ =====
- Chứng chỉ Kế toán trưởng (2018)
- CPA Vietnam (2020)`
  }
];

// Tạo PDFs với font Roboto (hỗ trợ Vietnamese)
CVS.forEach((cv) => {
  const doc = new PDFDocument({ size: 'A4', margin: 50 });
  const stream = fs.createWriteStream(path.join(__dirname, cv.filename));
  doc.pipe(stream);
  
  // Đăng ký font Roboto hỗ trợ tiếng Việt
  doc.registerFont('Roboto', FONT_PATH);
  doc.font('Roboto').fontSize(11).text(cv.content, { lineGap: 3 });
  
  doc.end();
  console.log('Created: ' + cv.filename);
});

console.log('\nAll 10 test CVs created with Vietnamese font support!');
