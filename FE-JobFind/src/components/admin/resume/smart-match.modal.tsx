import { useRef, useState } from "react";
import {
  Modal,
  Button,
  Progress,
  Tag,
  Spin,
  Typography,
  Divider,
  message,
  List,
} from "antd";
import {
  ThunderboltOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  BulbOutlined,
} from "@ant-design/icons";
import {
  callCancelAiTask,
  callMatchCvWithJob,
  IAiTaskStatus,
  isAiTaskSubmitted,
  waitForAiTaskResult,
} from "@/config/api";

const { Text, Paragraph, Title } = Typography;

interface IProps {
  open: boolean;
  onClose: (v: boolean) => void;
  resumeId: number | string | undefined;
  resumeEmail: string;
  jobName: string;
  reloadTable?: () => void;
}

interface IMatchResult {
  resumeId: number;
  jobId: number;
  jobName: string;
  matchScore: number;
  summary: string;
  matchedSkills: string[];
  missingSkills: string[];
  recommendations: string[];
}

const SmartMatchModal = (props: IProps) => {
  const { open, onClose, resumeId, resumeEmail, jobName, reloadTable } = props;
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState<IMatchResult | null>(null);
  const [loadingText, setLoadingText] = useState("");
  const [currentTaskId, setCurrentTaskId] = useState<number | null>(null);
  const [taskProgress, setTaskProgress] = useState(0);
  const taskAbortRef = useRef<AbortController | null>(null);

  const handleAnalyze = async () => {
    if (!resumeId) {
      message.warning("Không tìm thấy Resume ID");
      return;
    }
    setLoading(true);
    setTaskProgress(0);
    setLoadingText("AI đang phân tích CV với Job Description...");
    taskAbortRef.current?.abort();
    const abortController = new AbortController();
    taskAbortRef.current = abortController;
    try {
      const res = await callMatchCvWithJob(Number(resumeId));
      const submitted = (res as any)?.data;
      if (isAiTaskSubmitted(submitted)) {
        setCurrentTaskId(submitted.taskId);
        const matchResult = await waitForAiTaskResult<IMatchResult>(submitted.taskId, {
          signal: abortController.signal,
          pollIntervalMs: submitted.pollIntervalMillis || 1500,
          onStatus: updateTaskLoadingText,
        });
        setResult(matchResult);
        message.success("Phân tích hoàn tất!");
        reloadTable?.();
      } else if (submitted) {
        setResult(submitted as any);
        message.success("Phân tích hoàn tất!");
        reloadTable?.();
      } else {
        message.error("Không thể phân tích. Vui lòng thử lại.");
      }
    } catch (error: any) {
      if (error?.name === "AbortError") return;
      const errorMsg =
        error?.response?.data?.message || error?.message || "Có lỗi xảy ra khi phân tích.";
      message.error(errorMsg);
    } finally {
      setLoading(false);
      setLoadingText("");
      setCurrentTaskId(null);
      setTaskProgress(0);
      taskAbortRef.current = null;
    }
  };

  const updateTaskLoadingText = (task: IAiTaskStatus) => {
    setTaskProgress(task.progress || 0);
    if (task.status === "PENDING") {
      setLoadingText(`Task #${task.taskId} đang chờ trong hàng đợi...`);
    } else if (task.status === "PROCESSING") {
      setLoadingText(`AI đang xử lý... ${task.progress}%`);
    } else if (task.status === "RETRYING") {
      setLoadingText(`AI lỗi tạm thời, đang retry ${task.retryCount}/${task.maxRetries}...`);
    }
  };

  const handleCancelTask = async () => {
    if (!currentTaskId) return;
    try {
      await callCancelAiTask(currentTaskId);
      taskAbortRef.current?.abort();
      message.info("Đã hủy task AI.");
    } catch {
      message.error("Không thể hủy task AI.");
    } finally {
      setLoading(false);
      setLoadingText("");
      setCurrentTaskId(null);
      setTaskProgress(0);
    }
  };

  const getScoreColor = (s: number) =>
    s >= 75 ? "#10b981" : s >= 50 ? "#f59e0b" : "#ef4444";

  const getScoreLabel = (s: number) =>
    s >= 75 ? "Phù hợp tốt" : s >= 50 ? "Phù hợp trung bình" : "Cần cải thiện";

  const getScoreEmoji = (s: number) =>
    s >= 75 ? "🎯" : s >= 50 ? "⚠️" : "❌";

  return (
    <Modal
      title={
        <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
          <ThunderboltOutlined style={{ color: "#f97316", fontSize: 20 }} />
          <span>Smart Matching — AI Đánh Giá CV</span>
        </div>
      }
      open={open}
      onCancel={() => {
        onClose(false);
        taskAbortRef.current?.abort();
        setResult(null);
        setCurrentTaskId(null);
        setTaskProgress(0);
      }}
      footer={null}
      width={860}
      destroyOnClose
    >
      {/* INFO */}
      <div
        style={{
          background: "#f8fafc",
          borderRadius: 12,
          padding: "16px 20px",
          marginBottom: 20,
          border: "1px solid #e2e8f0",
        }}
      >
        <Text type="secondary">Ứng viên: </Text>
        <Text strong>{resumeEmail}</Text>
        <br />
        <Text type="secondary">Vị trí: </Text>
        <Text strong>{jobName}</Text>
      </div>

      {/* BUTTON START */}
      {!result && !loading && (
        <div>
          <Button
            type="primary"
            size="large"
            block
            icon={<ThunderboltOutlined />}
            onClick={handleAnalyze}
            style={{
              height: 52,
              borderRadius: 12,
              fontWeight: 700,
              fontSize: 16,
              background: "linear-gradient(135deg, #f97316, #ea580c)",
              border: "none",
            }}
          >
            AI Phân Tích Mức Độ Phù Hợp Với Job Hiện Tại
          </Button>
        </div>
      )}

      {/* LOADING */}
      {loading && (
        <div style={{ textAlign: "center", padding: "48px 0" }}>
          <Spin size="large" />
          <Progress
            percent={taskProgress}
            style={{ maxWidth: 360, margin: "20px auto 0" }}
            status={taskProgress >= 100 ? "success" : "active"}
          />
          <div
            style={{
              fontSize: 16,
              fontWeight: 600,
              color: "#334155",
              marginTop: 16,
            }}
          >
            {loadingText || "AI đang xử lý..."}
          </div>
          <div style={{ color: "#94a3b8", marginTop: 8 }}>
            Frontend đang theo dõi task bằng SSE/polling.
          </div>
          {currentTaskId && (
            <Button danger onClick={handleCancelTask} style={{ marginTop: 16 }}>
              Hủy task #{currentTaskId}
            </Button>
          )}
        </div>
      )}

      {/* RESULT */}
      {result && !loading && (
        <>
          {/* Score */}
          <div style={{ textAlign: "center", marginBottom: 24 }}>
            <Progress
              type="circle"
              percent={result.matchScore}
              size={140}
              strokeColor={getScoreColor(result.matchScore)}
              format={(p) => (
                <div>
                  <div style={{ fontSize: 32, fontWeight: 800 }}>{p}%</div>
                  <div style={{ fontSize: 12, color: "#94a3b8" }}>
                    {getScoreLabel(result.matchScore)}
                  </div>
                </div>
              )}
            />
            <div style={{ marginTop: 12 }}>
              <Text style={{ fontSize: 16 }}>
                {getScoreEmoji(result.matchScore)} {getScoreLabel(result.matchScore)}
              </Text>
            </div>
          </div>

          {/* Summary */}
          <div
            style={{
              background: "#f8fafc",
              borderRadius: 12,
              padding: "16px 20px",
              marginBottom: 20,
              border: "1px solid #e2e8f0",
            }}
          >
            <Title level={5} style={{ marginBottom: 8 }}>
              💬 Nhận Xét Tổng Quan
            </Title>
            <Paragraph style={{ marginBottom: 0, lineHeight: 1.7 }}>
              {result.summary}
            </Paragraph>
          </div>

          {/* Matched Skills */}
          {result.matchedSkills?.length > 0 && (
            <div style={{ marginBottom: 16 }}>
              <Text strong>
                <CheckCircleOutlined
                  style={{ color: "#10b981", marginRight: 6 }}
                />
                Kỹ năng phù hợp ({result.matchedSkills.length}):
              </Text>
              <div style={{ marginTop: 8, display: "flex", flexWrap: "wrap", gap: 6 }}>
                {result.matchedSkills.map((skill, i) => (
                  <Tag key={i} color="green">
                    ✅ {skill}
                  </Tag>
                ))}
              </div>
            </div>
          )}

          {/* Missing Skills */}
          {result.missingSkills?.length > 0 && (
            <div style={{ marginBottom: 16 }}>
              <Text strong>
                <CloseCircleOutlined
                  style={{ color: "#ef4444", marginRight: 6 }}
                />
                Kỹ năng còn thiếu ({result.missingSkills.length}):
              </Text>
              <div style={{ marginTop: 8, display: "flex", flexWrap: "wrap", gap: 6 }}>
                {result.missingSkills.map((skill, i) => (
                  <Tag key={i} color="red">
                    ❌ {skill}
                  </Tag>
                ))}
              </div>
            </div>
          )}

          {/* Recommendations */}
          {result.recommendations?.length > 0 && (
            <>
              <Divider />
              <div>
                <Text strong>
                  <BulbOutlined style={{ color: "#f59e0b", marginRight: 6 }} />
                  Gợi ý cho ứng viên:
                </Text>
                <List
                  size="small"
                  dataSource={result.recommendations}
                  style={{ marginTop: 8 }}
                  renderItem={(item: string, index: number) => (
                    <List.Item
                      style={{
                        padding: "8px 0",
                        borderBottom: "1px solid #f1f5f9",
                      }}
                    >
                      <Text>
                        {index + 1}. {item}
                      </Text>
                    </List.Item>
                  )}
                />
              </div>
            </>
          )}

          {/* Re-analyze button */}
          <Button
            type="default"
            size="large"
            block
            onClick={() => setResult(null)}
            style={{ marginTop: 20, borderRadius: 12, fontWeight: 600 }}
          >
            Phân Tích Lại
          </Button>
        </>
      )}
    </Modal>
  );
};

export default SmartMatchModal;
