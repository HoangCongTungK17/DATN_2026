import { useState, useEffect } from "react";
import {
  Upload,
  Button,
  Card,
  message,
  Spin,
  Row,
  Col,
  Progress,
  Typography,
  List,
  Tag,
} from "antd";
import {
  InboxOutlined,
  FileTextOutlined,
  CheckCircleOutlined,
  WarningOutlined,
  HistoryOutlined,
  CalendarOutlined,
  ReloadOutlined,
  DownloadOutlined,
} from "@ant-design/icons";
import {
  callAnalyzeCV,
  callFetchCvHistory,
  callFetchCvAnalysisById,
} from "@/config/api";
import type {
  ICvAnalysis,
  ICvAnalysisSuggestion,
  ICvHistory,
} from "@/types/backend";
import dayjs from "dayjs";

const { Title, Text, Paragraph } = Typography;
const { Dragger } = Upload;

const CvDoctorPage = () => {
  const [file, setFile] = useState<File | null>(null);
  const [fileName, setFileName] = useState("");
  const [loading, setLoading] = useState(false);
  const [loadingStep, setLoadingStep] = useState("");
  const [result, setResult] = useState<ICvAnalysis | null>(null);
  const [historyList, setHistoryList] = useState<ICvHistory[]>([]);
  const [historyLoading, setHistoryLoading] = useState(false);

  useEffect(() => {
    fetchHistory();
  }, []);

  const handleFileChange = (info: {
    file: { originFileObj?: File; type?: string; size?: number };
  }) => {
    const selectedFile =
      info.file.originFileObj || (info.file as unknown as File);
    if (selectedFile) {
      if (selectedFile.type !== "application/pdf") {
        message.error("Chỉ chấp nhận file PDF!");
        return;
      }
      if (selectedFile.size > 5 * 1024 * 1024) {
        message.error("File quá lớn! Tối đa 5MB.");
        return;
      }
      setFile(selectedFile);
      setFileName(selectedFile.name);
      setResult(null);
    }
  };

  const handleAnalyze = async () => {
    if (!file) {
      message.warning("Vui lòng chọn file CV trước!");
      return;
    }
    setLoading(true);
    setLoadingStep("Đang đọc CV...");
    try {
      setTimeout(() => setLoadingStep("Đang phân tích nội dung..."), 2000);
      setTimeout(() => setLoadingStep("Đang chấm điểm..."), 5000);
      const res = await callAnalyzeCV(file);
      if (res.data) {
        setResult(res.data as unknown as ICvAnalysis);
        message.success("Phân tích CV thành công!");
        fetchHistory();
      } else {
        message.error("Không thể phân tích CV.");
      }
    } catch (error: unknown) {
      const axiosError = error as {
        response?: { data?: { message?: string } };
      };
      message.error(axiosError?.response?.data?.message || "Có lỗi xảy ra.");
    } finally {
      setLoading(false);
      setLoadingStep("");
    }
  };

  const fetchHistory = async () => {
    setHistoryLoading(true);
    try {
      const res = await callFetchCvHistory("page=1&size=5");
      if (res?.data) setHistoryList((res.data as any).result || []);
    } catch {
    } finally {
      setHistoryLoading(false);
    }
  };

  const handleViewDetail = async (id: number) => {
    setLoading(true);
    setLoadingStep("Đang tải kết quả...");
    try {
      const res = await callFetchCvAnalysisById(id);
      if (res?.data) setResult(res.data as any);
    } catch {
      message.error("Không thể tải kết quả.");
    } finally {
      setLoading(false);
      setLoadingStep("");
    }
  };

  const getScoreColor = (s: number) =>
    s >= 75 ? "#10b981" : s >= 50 ? "#f59e0b" : "#ef4444";
  const getScoreLabel = (s: number) =>
    s >= 75 ? "Tốt" : s >= 50 ? "Trung bình" : "Cần cải thiện";
  const getPriorityColor = (p: string) =>
    p === "HIGH" ? "red" : p === "MEDIUM" ? "orange" : "blue";

  const exportPDF = () => {
    if (!result) return;
    const w = window.open("", "_blank");
    if (!w) {
      message.error("Popup bị chặn!");
      return;
    }

    const categories = [
      { label: "Format", score: result.formatScore },
      { label: "Content", score: result.contentScore },
      { label: "Keyword", score: result.keywordScore },
      { label: "Impact", score: result.impactScore },
    ];

    w.document.write(`<!DOCTYPE html><html><head><meta charset="utf-8">
        <title>CV Doctor Report - ${result.fileName}</title>
        <style>
            * { margin: 0; padding: 0; box-sizing: border-box; }
            body { font-family: 'Segoe UI', Arial, sans-serif; color: #1e293b; padding: 40px; max-width: 800px; margin: 0 auto; }
            .header { text-align: center; margin-bottom: 32px; border-bottom: 3px solid #4f46e5; padding-bottom: 20px; }
            .header h1 { font-size: 28px; color: #4f46e5; margin-bottom: 4px; }
            .header p { color: #64748b; font-size: 14px; }
            .score-main { text-align: center; margin: 32px 0; }
            .score-big { font-size: 72px; font-weight: 900; }
            .score-label { font-size: 16px; color: #64748b; }
            .scores-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin: 24px 0; }
            .score-item { padding: 16px; border: 1px solid #e2e8f0; border-radius: 8px; }
            .score-item .label { font-weight: 600; margin-bottom: 4px; }
            .score-item .value { font-size: 24px; font-weight: 700; }
            .section { margin: 28px 0; }
            .section h2 { font-size: 18px; border-bottom: 2px solid #f1f5f9; padding-bottom: 8px; margin-bottom: 12px; }
            .section p { line-height: 1.7; color: #475569; }
            .strength { padding: 8px 0; border-bottom: 1px solid #f8fafc; }
            .strength::before { content: '✅ '; }
            .suggestion { padding: 10px 0; border-bottom: 1px solid #f8fafc; }
            .suggestion .tags { margin-bottom: 4px; }
            .suggestion .tag { display: inline-block; padding: 2px 8px; border-radius: 4px; font-size: 12px; margin-right: 6px; }
            .tag-high { background: #fef2f2; color: #dc2626; }
            .tag-medium { background: #fff7ed; color: #ea580c; }
            .tag-low { background: #eff6ff; color: #2563eb; }
            .footer { text-align: center; margin-top: 40px; padding-top: 16px; border-top: 1px solid #e2e8f0; color: #94a3b8; font-size: 12px; }
            @media print { body { padding: 20px; } }
        </style></head><body>
        <div class="header">
            <h1>🩺 CV Doctor — Báo Cáo Phân Tích</h1>
            <p>File: ${result.fileName} | Ngày: ${new Date().toLocaleDateString("vi-VN")}</p>
        </div>
        <div class="score-main">
            <div class="score-big" style="color: ${getScoreColor(result.overallScore)}">${result.overallScore}</div>
            <div class="score-label">Điểm tổng / 100 — ${getScoreLabel(result.overallScore)}</div>
        </div>
        <div class="scores-grid">
            ${categories
              .map(
                (c) => `<div class="score-item">
                <div class="label">${c.label}</div>
                <div class="value" style="color: ${getScoreColor(c.score)}">${c.score}/100</div>
            </div>`,
              )
              .join("")}
        </div>
        <div class="section"><h2>💬 Nhận Xét Tổng Quan</h2><p>${result.summary}</p></div>
        ${
          result.strengths?.length
            ? `<div class="section"><h2>✅ Điểm Mạnh</h2>
            ${result.strengths.map((s: string) => `<div class="strength">${s}</div>`).join("")}</div>`
            : ""
        }
        ${
          result.suggestions?.length
            ? `<div class="section"><h2>💡 Gợi Ý Cải Thiện</h2>
            ${result.suggestions
              .map(
                (s: ICvAnalysisSuggestion) => `<div class="suggestion">
                <div class="tags"><span class="tag tag-${s.priority?.toLowerCase()}">${s.priority}</span><span class="tag" style="background:#f1f5f9">${s.category}</span></div>
                <div><strong>⚠ ${s.issue}</strong></div>
                <div style="color:#64748b;margin-top:2px">→ ${s.suggestion}</div>
            </div>`,
              )
              .join("")}</div>`
            : ""
        }
        <div class="footer">Được tạo bởi JobFind AI — CV Doctor | Powered by Groq LLaMA 3.3</div>
        </body></html>`);
    w.document.close();
    setTimeout(() => w.print(), 500);
  };

  return (
    <div style={{ maxWidth: 900, margin: "0 auto", padding: "48px 24px" }}>
      {/* PAGE HEADER */}
      <div style={{ textAlign: "center", marginBottom: 40 }}>
        <h1
          style={{
            fontSize: 36,
            fontWeight: 800,
            letterSpacing: -0.5,
            background: "linear-gradient(135deg, #4f46e5, #7c3aed)",
            WebkitBackgroundClip: "text",
            WebkitTextFillColor: "transparent",
            marginBottom: 8,
          }}
        >
          🩺 CV Doctor
        </h1>
        <p style={{ color: "#64748b", fontSize: 16 }}>
          AI phân tích và chấm điểm CV — Gợi ý cải thiện cụ thể
        </p>
      </div>

      {/* UPLOAD */}
      {!result && (
        <div
          style={{
            background: "#fff",
            borderRadius: 20,
            padding: 32,
            border: "1px solid #f1f5f9",
            boxShadow: "0 1px 3px rgba(0,0,0,0.04)",
          }}
        >
          <Dragger
            accept=".pdf"
            maxCount={1}
            beforeUpload={() => false}
            onChange={handleFileChange}
            showUploadList={false}
            disabled={loading}
            style={{
              borderRadius: 16,
              border: "2px dashed #e2e8f0",
              padding: "32px 0",
            }}
          >
            <p className="ant-upload-drag-icon">
              <InboxOutlined style={{ fontSize: 48, color: "#4f46e5" }} />
            </p>
            <p style={{ fontSize: 16, fontWeight: 600, color: "#334155" }}>
              Kéo thả file CV vào đây hoặc click để chọn
            </p>
            <p style={{ color: "#94a3b8" }}>
              Chỉ chấp nhận file PDF, tối đa 5MB
            </p>
          </Dragger>

          {fileName && (
            <div
              style={{
                display: "flex",
                alignItems: "center",
                gap: 8,
                padding: "12px 16px",
                background: "#f8fafc",
                borderRadius: 12,
                marginTop: 16,
              }}
            >
              <FileTextOutlined style={{ color: "#4f46e5" }} />
              <Text strong>{fileName}</Text>
              <Text type="secondary">— Sẵn sàng</Text>
            </div>
          )}

          <Button
            type="primary"
            size="large"
            block
            onClick={handleAnalyze}
            loading={loading}
            disabled={!file}
            style={{
              marginTop: 20,
              height: 48,
              borderRadius: 12,
              fontWeight: 700,
              background: "linear-gradient(135deg, #4f46e5, #7c3aed)",
              border: "none",
            }}
          >
            {loading ? loadingStep : "🤖 Phân Tích CV"}
          </Button>
        </div>
      )}

      {/* LOADING */}
      {loading && (
        <div
          style={{
            textAlign: "center",
            padding: "60px 0",
            background: "#fff",
            borderRadius: 20,
            border: "1px solid #f1f5f9",
            marginTop: 24,
          }}
        >
          <Spin size="large" />
          <div
            style={{
              fontSize: 16,
              fontWeight: 600,
              color: "#334155",
              marginTop: 16,
            }}
          >
            {loadingStep}
          </div>
          <div style={{ color: "#94a3b8", marginTop: 8 }}>
            Quá trình này mất khoảng 10-15 giây...
          </div>
        </div>
      )}

      {/* RESULTS */}
      {result && !loading && (
        <>
          {/* Score Card */}
          <div
            style={{
              background: "#fff",
              borderRadius: 20,
              padding: 36,
              border: "1px solid #f1f5f9",
              textAlign: "center",
              marginBottom: 24,
            }}
          >
            <h3 style={{ fontSize: 20, fontWeight: 700, marginBottom: 4 }}>
              📊 Kết Quả Phân Tích
            </h3>
            <p style={{ color: "#94a3b8", marginBottom: 24 }}>
              {result.fileName}
            </p>

            <Progress
              type="circle"
              percent={result.overallScore}
              size={160}
              strokeColor={getScoreColor(result.overallScore)}
              format={(p) => (
                <div>
                  <div style={{ fontSize: 36, fontWeight: 800 }}>{p}</div>
                  <div style={{ fontSize: 13, color: "#94a3b8" }}>/ 100</div>
                </div>
              )}
            />

            <Row gutter={[16, 16]} style={{ marginTop: 32 }}>
              {[
                { label: "📐 Format", score: result.formatScore },
                { label: "📝 Content", score: result.contentScore },
                { label: "🔑 Keyword", score: result.keywordScore },
                { label: "🚀 Impact", score: result.impactScore },
              ].map((item) => (
                <Col span={12} key={item.label}>
                  <div
                    style={{
                      background: "#f8fafc",
                      borderRadius: 12,
                      padding: "16px 20px",
                      border: "1px solid #f1f5f9",
                    }}
                  >
                    <div
                      style={{
                        display: "flex",
                        justifyContent: "space-between",
                        marginBottom: 8,
                      }}
                    >
                      <Text strong>{item.label}</Text>
                      <Text
                        style={{
                          color: getScoreColor(item.score),
                          fontWeight: 700,
                        }}
                      >
                        {item.score}/100
                      </Text>
                    </div>
                    <Progress
                      percent={item.score}
                      showInfo={false}
                      strokeColor={getScoreColor(item.score)}
                      size="small"
                    />
                  </div>
                </Col>
              ))}
            </Row>
          </div>

          {/* Summary */}
          <div
            style={{
              background: "#fff",
              borderRadius: 16,
              padding: 28,
              border: "1px solid #f1f5f9",
              marginBottom: 20,
            }}
          >
            <Title level={5}>💬 Nhận Xét Tổng Quan</Title>
            <Paragraph style={{ lineHeight: 1.8, color: "#475569" }}>
              {result.summary}
            </Paragraph>
          </div>

          {/* Strengths */}
          {result.strengths?.length > 0 && (
            <div
              style={{
                background: "#fff",
                borderRadius: 16,
                padding: 28,
                border: "1px solid #f1f5f9",
                marginBottom: 20,
              }}
            >
              <Title level={5}>✅ Điểm Mạnh</Title>
              <List
                dataSource={result.strengths}
                renderItem={(item) => (
                  <List.Item
                    style={{
                      borderBottom: "1px solid #f8fafc",
                      padding: "10px 0",
                    }}
                  >
                    <CheckCircleOutlined
                      style={{ color: "#10b981", marginRight: 10 }}
                    />
                    {item}
                  </List.Item>
                )}
              />
            </div>
          )}

          {/* Suggestions */}
          {result.suggestions?.length > 0 && (
            <div
              style={{
                background: "#fff",
                borderRadius: 16,
                padding: 28,
                border: "1px solid #f1f5f9",
                marginBottom: 20,
              }}
            >
              <Title level={5}>💡 Gợi Ý Cải Thiện</Title>
              <List
                dataSource={result.suggestions}
                renderItem={(item: ICvAnalysisSuggestion) => (
                  <List.Item
                    style={{
                      borderBottom: "1px solid #f8fafc",
                      padding: "12px 0",
                    }}
                  >
                    <div style={{ width: "100%" }}>
                      <div style={{ marginBottom: 6 }}>
                        <Tag color={getPriorityColor(item.priority)}>
                          {item.priority}
                        </Tag>
                        <Tag>{item.category}</Tag>
                      </div>
                      <div>
                        <WarningOutlined
                          style={{ color: "#f59e0b", marginRight: 8 }}
                        />
                        <Text strong>{item.issue}</Text>
                      </div>
                      <div style={{ marginTop: 4, paddingLeft: 22 }}>
                        <Text type="secondary">→ {item.suggestion}</Text>
                      </div>
                    </div>
                  </List.Item>
                )}
              />
            </div>
          )}

          <div style={{ display: "flex", gap: 12 }}>
            <Button
              type="primary"
              size="large"
              block
              icon={<DownloadOutlined />}
              onClick={exportPDF}
              style={{
                height: 48,
                borderRadius: 12,
                fontWeight: 700,
                background: "linear-gradient(135deg, #10b981, #059669)",
                border: "none",
              }}
            >
              📥 Tải Báo Cáo PDF
            </Button>
            <Button
              type="primary"
              size="large"
              block
              icon={<ReloadOutlined />}
              onClick={() => {
                setResult(null);
                setFile(null);
                setFileName("");
              }}
              style={{
                height: 48,
                borderRadius: 12,
                fontWeight: 700,
                background: "linear-gradient(135deg, #4f46e5, #7c3aed)",
                border: "none",
              }}
            >
              📄 Phân Tích CV Khác
            </Button>
          </div>
        </>
      )}

      {/* HISTORY */}
      {historyList.length > 0 && !loading && (
        <div
          style={{
            background: "#fff",
            borderRadius: 20,
            padding: 28,
            border: "1px solid #f1f5f9",
            marginTop: 32,
          }}
        >
          <Title level={5}>
            <HistoryOutlined style={{ marginRight: 8 }} />
            Lịch Sử Phân Tích
          </Title>
          <List
            loading={historyLoading}
            dataSource={historyList}
            renderItem={(item: ICvHistory) => (
              <List.Item
                onClick={() => handleViewDetail(item.id)}
                style={{
                  cursor: "pointer",
                  padding: "14px 16px",
                  borderRadius: 12,
                  marginBottom: 8,
                  border: "1px solid #f1f5f9",
                  transition: "all 0.25s",
                }}
                onMouseEnter={(e: React.MouseEvent<HTMLDivElement>) => {
                  e.currentTarget.style.borderColor = "rgba(79,70,229,0.2)";
                  e.currentTarget.style.boxShadow =
                    "0 4px 16px rgba(0,0,0,0.06)";
                }}
                onMouseLeave={(e: React.MouseEvent<HTMLDivElement>) => {
                  e.currentTarget.style.borderColor = "#f1f5f9";
                  e.currentTarget.style.boxShadow = "none";
                }}
              >
                <div
                  style={{
                    display: "flex",
                    justifyContent: "space-between",
                    alignItems: "center",
                    width: "100%",
                  }}
                >
                  <div>
                    <FileTextOutlined
                      style={{ color: "#4f46e5", marginRight: 8 }}
                    />
                    <Text strong>{item.fileName}</Text>
                    <br />
                    <Text type="secondary" style={{ fontSize: 12 }}>
                      <CalendarOutlined style={{ marginRight: 4 }} />
                      {dayjs(item.createdAt).format("DD/MM/YYYY HH:mm")}
                    </Text>
                  </div>
                  <div
                    style={{ display: "flex", alignItems: "center", gap: 12 }}
                  >
                    <Progress
                      type="circle"
                      percent={item.overallScore}
                      size={48}
                      strokeColor={getScoreColor(item.overallScore)}
                    />
                    <Text type="secondary" style={{ fontSize: 12 }}>
                      Xem →
                    </Text>
                  </div>
                </div>
              </List.Item>
            )}
          />
        </div>
      )}
    </div>
  );
};

export default CvDoctorPage;
