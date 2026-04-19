import { useNavigate } from "react-router-dom";
import {
  FileSearchOutlined,
  AudioOutlined,
  ThunderboltOutlined,
  RobotOutlined,
  ArrowRightOutlined,
  CheckCircleFilled,
  StarFilled,
  SafetyCertificateFilled,
  RocketFilled,
} from "@ant-design/icons";
import styles from "@/styles/ai-hub.module.scss";

const AI_FEATURES = [
  {
    key: "cv-doctor",
    icon: <FileSearchOutlined />,
    title: "CV Doctor",
    tagline: "Phân Tích CV Thông Minh",
    description:
      "Upload CV của bạn và nhận đánh giá chi tiết từ AI. Chấm điểm 4 tiêu chí, phát hiện điểm yếu và đưa ra lời khuyên cải thiện cụ thể.",
    features: [
      "Chấm điểm Format, Nội dung, Từ khóa, Tác động",
      "Phân tích chuyên sâu phù hợp ngành IT",
      "Gợi ý cải thiện chi tiết từng mục",
      "Lịch sử phân tích để theo dõi tiến bộ",
    ],
    color: "#6366f1",
    gradient: "linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%)",
    path: "/cv-doctor",
    cta: "Phân Tích CV Ngay",
  },
  {
    key: "interview",
    icon: <AudioOutlined />,
    title: "Interview Coach",
    tagline: "Luyện Phỏng Vấn Với AI",
    description:
      "Mô phỏng buổi phỏng vấn kỹ thuật thực tế. AI đặt câu hỏi theo vị trí & level, đánh giá câu trả lời và cung cấp feedback chi tiết.",
    features: [
      "Câu hỏi theo vị trí: Java, React, DevOps...",
      "3 level: Junior, Middle, Senior",
      "Feedback tức thì + câu trả lời mẫu",
      "Báo cáo tổng kết sau mỗi phiên",
    ],
    color: "#10b981",
    gradient: "linear-gradient(135deg, #10b981 0%, #059669 100%)",
    path: "/interview-coach",
    cta: "Bắt Đầu Phỏng Vấn",
  },
  {
    key: "matching",
    icon: <ThunderboltOutlined />,
    title: "Smart Matching",
    tagline: "So Khớp CV — Việc Làm",
    description:
      "AI phân tích mức độ phù hợp giữa CV và Job Description. Xác định skills match, skills thiếu và đưa ra lộ trình phát triển.",
    features: [
      "Tính % phù hợp CV với từng vị trí",
      "Phân tích kỹ năng khớp & thiếu",
      "Đề xuất lộ trình học tập bổ sung",
      "Dành cho HR lọc ứng viên nhanh",
    ],
    color: "#f97316",
    gradient: "linear-gradient(135deg, #f97316 0%, #ea580c 100%)",
    path: "/job",
    cta: "Khám Phá Việc Làm",
  },
  {
    key: "chatbot",
    icon: <RobotOutlined />,
    title: "AI Chatbot",
    tagline: "Trợ Lý Tìm Việc 24/7",
    description:
      "Hỏi đáp tự nhiên về việc làm IT. AI tìm kiếm thông minh từ cơ sở dữ liệu với Retrieval-Augmented Generation (RAG).",
    features: [
      "Tìm việc bằng ngôn ngữ tự nhiên",
      "Gợi ý theo kỹ năng, lương, địa điểm",
      "Hỗ trợ tiếng Việt hoàn toàn",
      "Powered by Vector DB (Pinecone)",
    ],
    color: "#0ea5e9",
    gradient: "linear-gradient(135deg, #0ea5e9 0%, #0284c7 100%)",
    path: "/",
    cta: "Chat Với AI",
    isChat: true,
  },
];

const TECH_STACK = [
  { name: "Groq LPU", desc: "Inference siêu nhanh" },
  { name: "LLaMA 3.3 70B", desc: "LLM mạnh nhất" },
  { name: "Pinecone", desc: "Vector Database" },
  { name: "Spring AI", desc: "Enterprise framework" },
];

const AIHubPage = () => {
  const navigate = useNavigate();

  return (
    <div className={styles["aihub"]}>
      {/* ═══════ HERO ═══════ */}
      <section className={styles["hero"]}>
        <div className={styles["hero-bg"]}>
          <div className={styles["hero-orb"] + " " + styles["orb-1"]} />
          <div className={styles["hero-orb"] + " " + styles["orb-2"]} />
          <div className={styles["hero-orb"] + " " + styles["orb-3"]} />
        </div>
        <div className={styles["hero-content"]}>
          <div className={styles["hero-badge"]}>
            <RocketFilled /> AI-Powered Platform
          </div>
          <h1>
            Công Cụ AI <span>Cho Sự Nghiệp IT</span>
          </h1>
          <p>
            4 công cụ trí tuệ nhân tạo giúp bạn tối ưu CV, luyện phỏng vấn,
            so khớp việc làm và tìm kiếm cơ hội — tất cả hoàn toàn miễn phí.
          </p>
          <div className={styles["hero-stats"]}>
            <div className={styles["hero-stat"]}>
              <strong>4</strong>
              <span>Công cụ AI</span>
            </div>
            <div className={styles["hero-stat"]}>
              <strong>LLaMA 3.3</strong>
              <span>70B parameters</span>
            </div>
            <div className={styles["hero-stat"]}>
              <strong>&lt;3s</strong>
              <span>Thời gian xử lý</span>
            </div>
            <div className={styles["hero-stat"]}>
              <strong>100%</strong>
              <span>Miễn phí</span>
            </div>
          </div>
        </div>
      </section>

      {/* ═══════ FEATURES GRID ═══════ */}
      <section className={styles["features"]}>
        <div className={styles["container"]}>
          <div className={styles["section-header"]}>
            <h2>Khám Phá 4 Công Cụ AI</h2>
            <p>Mỗi công cụ được thiết kế riêng để giải quyết một thách thức trong hành trình tìm việc IT của bạn</p>
          </div>

          <div className={styles["features-grid"]}>
            {AI_FEATURES.map((feature, idx) => (
              <div
                key={feature.key}
                className={styles["feature-card"]}
                style={{ "--accent": feature.color, "--gradient": feature.gradient } as React.CSSProperties}
              >
                <div className={styles["card-header"]}>
                  <div className={styles["card-icon"]}>{feature.icon}</div>
                  <div className={styles["card-number"]}>0{idx + 1}</div>
                </div>

                <h3>{feature.title}</h3>
                <div className={styles["card-tagline"]}>{feature.tagline}</div>
                <p>{feature.description}</p>

                <ul className={styles["card-features"]}>
                  {feature.features.map((f, i) => (
                    <li key={i}>
                      <CheckCircleFilled style={{ color: feature.color }} />
                      {f}
                    </li>
                  ))}
                </ul>

                <button
                  className={styles["card-cta"]}
                  onClick={() => {
                    if (feature.isChat) {
                      // Trigger chatbot open
                      navigate("/");
                      setTimeout(() => {
                        const chatBtn = document.querySelector(".chatbot-toggle") as HTMLElement;
                        chatBtn?.click();
                      }, 500);
                    } else {
                      navigate(feature.path);
                    }
                  }}
                >
                  {feature.cta} <ArrowRightOutlined />
                </button>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* ═══════ HOW IT WORKS ═══════ */}
      <section className={styles["how-it-works"]}>
        <div className={styles["container"]}>
          <div className={styles["section-header"]}>
            <h2>Cách Hoạt Động</h2>
            <p>Quy trình AI đơn giản — kết quả chuyên nghiệp</p>
          </div>

          <div className={styles["steps"]}>
            <div className={styles["step"]}>
              <div className={styles["step-number"]}>1</div>
              <h4>Chọn Công Cụ</h4>
              <p>Chọn tính năng AI phù hợp với nhu cầu: phân tích CV, luyện phỏng vấn hoặc tìm việc</p>
            </div>
            <div className={styles["step-arrow"]}>→</div>
            <div className={styles["step"]}>
              <div className={styles["step-number"]}>2</div>
              <h4>Cung Cấp Thông Tin</h4>
              <p>Upload CV, chọn vị trí phỏng vấn hoặc đặt câu hỏi — AI sẽ hiểu ngữ cảnh của bạn</p>
            </div>
            <div className={styles["step-arrow"]}>→</div>
            <div className={styles["step"]}>
              <div className={styles["step-number"]}>3</div>
              <h4>Nhận Kết Quả AI</h4>
              <p>Trong vài giây, nhận phân tích chuyên sâu, đánh giá chi tiết và lời khuyên thực tế</p>
            </div>
          </div>
        </div>
      </section>

      {/* ═══════ TECH STACK ═══════ */}
      <section className={styles["tech-section"]}>
        <div className={styles["container"]}>
          <div className={styles["section-header"]}>
            <h2>Công Nghệ Đằng Sau</h2>
            <p>Xây dựng trên nền tảng công nghệ AI tiên tiến nhất</p>
          </div>

          <div className={styles["tech-grid"]}>
            {TECH_STACK.map((tech) => (
              <div key={tech.name} className={styles["tech-card"]}>
                <SafetyCertificateFilled className={styles["tech-icon"]} />
                <strong>{tech.name}</strong>
                <span>{tech.desc}</span>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* ═══════ CTA ═══════ */}
      <section className={styles["cta-section"]}>
        <div className={styles["container"]}>
          <div className={styles["cta-content"]}>
            <StarFilled className={styles["cta-icon"]} />
            <h2>Sẵn Sàng Nâng Cấp Sự Nghiệp?</h2>
            <p>Bắt đầu với CV Doctor — chỉ cần upload CV của bạn và AI sẽ làm phần còn lại</p>
            <div className={styles["cta-buttons"]}>
              <button
                className={styles["cta-primary"]}
                onClick={() => navigate("/cv-doctor")}
              >
                <FileSearchOutlined /> Phân Tích CV Miễn Phí
              </button>
              <button
                className={styles["cta-secondary"]}
                onClick={() => navigate("/interview-coach")}
              >
                <AudioOutlined /> Luyện Phỏng Vấn
              </button>
            </div>
          </div>
        </div>
      </section>
    </div>
  );
};

export default AIHubPage;
