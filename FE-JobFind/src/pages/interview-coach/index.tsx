import { useState, useEffect, useRef } from 'react';
import {
    Button, Input, Select, InputNumber, Progress, Tag, Card,
    Spin, Typography, Divider, message, List
} from 'antd';
import {
    RobotOutlined, SendOutlined, TrophyOutlined,
    HistoryOutlined, ReloadOutlined
} from '@ant-design/icons';
import styles from '@/styles/interview-coach.module.scss';
import dayjs from 'dayjs';
import {
    callCancelAiTask,
    callStartInterview, callSubmitAnswer,
    callGetInterviewSummary, callGetInterviewHistory,
    callGetCurrentQuestion,
    IAiTaskStatus,
    isAiTaskSubmitted,
    waitForAiTaskResult
} from '@/config/api';


const { TextArea } = Input;
const { Title, Text, Paragraph } = Typography;

type Phase = 'setup' | 'interview' | 'feedback' | 'summary';

// Danh sách vị trí IT — CHỈ cho phép phỏng vấn các vị trí này
const IT_POSITIONS = [
    { value: 'Java Backend Developer', label: '☕ Java Backend Developer' },
    { value: 'Node.js Backend Developer', label: '🟢 Node.js Backend Developer' },
    { value: 'Python Backend Developer', label: '🐍 Python Backend Developer' },
    { value: '.NET Developer', label: '🔷 .NET Developer' },
    { value: 'React Frontend Developer', label: '⚛️ React Frontend Developer' },
    { value: 'Angular Frontend Developer', label: '🅰️ Angular Frontend Developer' },
    { value: 'Vue.js Frontend Developer', label: '💚 Vue.js Frontend Developer' },
    { value: 'Fullstack Developer', label: '🔄 Fullstack Developer' },
    { value: 'React Native Developer', label: '📱 React Native Developer' },
    { value: 'Flutter Developer', label: '🦋 Flutter Developer' },
    { value: 'iOS Developer (Swift)', label: '🍎 iOS Developer (Swift)' },
    { value: 'Android Developer (Kotlin)', label: '🤖 Android Developer (Kotlin)' },
    { value: 'DevOps Engineer', label: '🔧 DevOps Engineer' },
    { value: 'Cloud Engineer (AWS/Azure)', label: '☁️ Cloud Engineer (AWS/Azure)' },
    { value: 'Data Engineer', label: '📊 Data Engineer' },
    { value: 'Data Scientist / AI Engineer', label: '🧠 Data Scientist / AI Engineer' },
    { value: 'QA / Tester', label: '🧪 QA / Tester' },
    { value: 'Business Analyst (IT)', label: '📋 Business Analyst (IT)' },
    { value: 'System Administrator', label: '🖥️ System Administrator' },
    { value: 'Database Administrator', label: '🗄️ Database Administrator' },
    { value: 'Cyber Security Engineer', label: '🔒 Cyber Security Engineer' },
    { value: 'Project Manager (IT)', label: '📌 Project Manager (IT)' },
    { value: 'UI/UX Designer', label: '🎨 UI/UX Designer' },
    { value: 'Embedded / IoT Engineer', label: '🔌 Embedded / IoT Engineer' },
];

const InterviewCoachPage = () => {
    // === STATE ===
    const [phase, setPhase] = useState<Phase>('setup');
    const [loading, setLoading] = useState(false);
    const [loadingText, setLoadingText] = useState('');
    const [currentTaskId, setCurrentTaskId] = useState<number | null>(null);
    const [taskProgress, setTaskProgress] = useState(0);
    const taskAbortRef = useRef<AbortController | null>(null);

    // Setup
    const [jobPosition, setJobPosition] = useState('');
    const [level, setLevel] = useState('JUNIOR');
    const [totalQuestions, setTotalQuestions] = useState(5);

    // Interview
    const [sessionId, setSessionId] = useState<number>(0);
    const [question, setQuestion] = useState<any>(null);
    const [answer, setAnswer] = useState('');

    // Feedback
    const [feedback, setFeedback] = useState<any>(null);

    // Summary
    const [summary, setSummary] = useState<any>(null);

    // History
    const [history, setHistory] = useState<any[]>([]);
    const [showHistory, setShowHistory] = useState(false);

    // === BẮT ĐẦU PHỎNG VẤN ===
    const handleStart = async () => {
        if (!jobPosition.trim()) {
            message.warning('Vui lòng nhập vị trí ứng tuyển');
            return;
        }
        setLoading(true);
        setTaskProgress(0);
        setLoadingText('Đang đưa yêu cầu vào hàng đợi AI...');
        taskAbortRef.current?.abort();
        const abortController = new AbortController();
        taskAbortRef.current = abortController;
        try {
            const res = await callStartInterview({ jobPosition, level, totalQuestions });
            const submitted = (res as any)?.data;
            if (isAiTaskSubmitted(submitted)) {
                setCurrentTaskId(submitted.taskId);
                const data = await waitForAiTaskResult<any>(submitted.taskId, {
                    signal: abortController.signal,
                    pollIntervalMs: submitted.pollIntervalMillis || 1500,
                    onStatus: updateTaskLoadingText,
                });
                setSessionId(data.sessionId);
                setQuestion(data);
                setPhase('interview');
                message.success('Phiên phỏng vấn bắt đầu!');
            } else if (submitted) {
                const data = submitted as any;
                setSessionId(data.sessionId);
                setQuestion(data);
                setPhase('interview');
                message.success('Phiên phỏng vấn bắt đầu!');
            }
        } catch (error: any) {
            if (error?.name === 'AbortError') return;
            message.error(error?.message || 'Không thể bắt đầu phỏng vấn');
        } finally {
            setLoading(false);
            setLoadingText('');
            setCurrentTaskId(null);
            setTaskProgress(0);
            taskAbortRef.current = null;
        }
    };

    // === GỬI CÂU TRẢ LỜI ===
    const handleSubmitAnswer = async () => {
        if (!answer.trim()) {
            message.warning('Vui lòng nhập câu trả lời');
            return;
        }
        setLoading(true);
        setTaskProgress(0);
        setLoadingText('AI đang đánh giá câu trả lời...');
        taskAbortRef.current?.abort();
        const abortController = new AbortController();
        taskAbortRef.current = abortController;
        try {
            const res = await callSubmitAnswer({ sessionId, answer });
            const submitted = (res as any)?.data;
            if (isAiTaskSubmitted(submitted)) {
                setCurrentTaskId(submitted.taskId);
                const data = await waitForAiTaskResult<any>(submitted.taskId, {
                    signal: abortController.signal,
                    pollIntervalMs: submitted.pollIntervalMillis || 1500,
                    onStatus: updateTaskLoadingText,
                });
                setFeedback(data);
                setPhase('feedback');
                setAnswer('');
            } else if (submitted) {
                const data = submitted as any;
                setFeedback(data);
                setPhase('feedback');
                setAnswer('');
            }
        } catch (error: any) {
            if (error?.name === 'AbortError') return;
            message.error(error?.message || 'Không thể gửi câu trả lời');
        } finally {
            setLoading(false);
            setLoadingText('');
            setCurrentTaskId(null);
            setTaskProgress(0);
            taskAbortRef.current = null;
        }
    };

    const updateTaskLoadingText = (task: IAiTaskStatus) => {
        setTaskProgress(task.progress || 0);
        if (task.status === 'PENDING') {
            setLoadingText(`Task #${task.taskId} đang chờ trong hàng đợi...`);
        } else if (task.status === 'PROCESSING') {
            setLoadingText(`AI đang xử lý... ${task.progress}%`);
        } else if (task.status === 'RETRYING') {
            setLoadingText(`AI lỗi tạm thời, đang retry ${task.retryCount}/${task.maxRetries}...`);
        }
    };

    const handleCancelTask = async () => {
        if (!currentTaskId) return;
        try {
            await callCancelAiTask(currentTaskId);
            taskAbortRef.current?.abort();
            message.info('Đã hủy task AI.');
        } catch {
            message.error('Không thể hủy task AI.');
        } finally {
            setLoading(false);
            setLoadingText('');
            setCurrentTaskId(null);
            setTaskProgress(0);
        }
    };

    // === TIẾP TỤC CÂU TIẾP HOẶC XEM TỔNG KẾT ===
    const handleNext = async () => {
        if (feedback?.lastQuestion) {
            setLoading(true);
            try {
                const res = await callGetInterviewSummary(sessionId);
                if (res?.data) {
                    setSummary(res.data as any);
                    setPhase('summary');
                }
            } catch {
                message.error('Không thể lấy tổng kết');
            } finally {
                setLoading(false);
            }
        } else {
            // Dùng nextQuestion từ feedback (không cần gọi API thêm)
            if (feedback?.nextQuestion) {
                setQuestion(feedback.nextQuestion);
            }
            setPhase('interview');
            setFeedback(null);
        }
    };


    // === TẢI LỊCH SỬ ===
    const loadHistory = async () => {
        try {
            const res = await callGetInterviewHistory('page=1&size=10');
            if (res?.data) {
                setHistory((res.data as any)?.result || []);
            }
        } catch { }
    };

    // === XEM CHI TIẾT PHIÊN CŨ ===
    const handleViewSession = async (sid: number) => {
        setLoading(true);
        try {
            const res = await callGetInterviewSummary(sid);
            if (res?.data) {
                setSummary(res.data as any);
                setPhase('summary');
            }
        } catch {
            message.error('Không thể tải phiên phỏng vấn');
        } finally {
            setLoading(false);
        }
    };

    // === RESET ===
    const handleReset = () => {
        setPhase('setup');
        setSessionId(0);
        setQuestion(null);
        setAnswer('');
        setFeedback(null);
        setSummary(null);
    };

    useEffect(() => {
        loadHistory();
    }, []);

    // Hàm lấy màu theo điểm
    const getScoreColor = (score: number) => {
        if (score >= 75) return '#52c41a';
        if (score >= 50) return '#faad14';
        return '#ff4d4f';
    };

    return (
        <div className={styles.container}>
            <div className={styles.header}>
                <h1>🎤 Interview <span>Coach</span></h1>
                <p>Luyện phỏng vấn với AI — Nhận feedback tức thì cho từng câu trả lời</p>
            </div>

            {/* ===== PHASE 1: FORM BẮT ĐẦU ===== */}
            {phase === 'setup' && (
                <>
                    <div className={styles.startForm}>
                        <Title level={4}>Bắt đầu phỏng vấn mới</Title>

                        <div style={{ marginBottom: 16 }}>
                            <Text strong>Vị trí ứng tuyển (IT):</Text>
                            <Select
                                showSearch
                                placeholder="Chọn hoặc tìm vị trí IT..."
                                value={jobPosition || undefined}
                                onChange={(val) => setJobPosition(val)}
                                size="large"
                                style={{ width: '100%', marginTop: 8 }}
                                options={IT_POSITIONS}
                                filterOption={(input, option) =>
                                    (option?.label ?? '').toLowerCase().includes(input.toLowerCase()) ||
                                    (option?.value ?? '').toLowerCase().includes(input.toLowerCase())
                                }
                                notFoundContent="Không tìm thấy vị trí IT phù hợp"
                            />
                        </div>

                        <div style={{ marginBottom: 16 }}>
                            <Text strong>Level:</Text>
                            <Select
                                value={level}
                                onChange={setLevel}
                                size="large"
                                style={{ width: '100%', marginTop: 8 }}
                                options={[
                                    { value: 'INTERN', label: 'Intern' },
                                    { value: 'FRESHER', label: 'Fresher' },
                                    { value: 'JUNIOR', label: 'Junior' },
                                    { value: 'MIDDLE', label: 'Mid-level' },
                                    { value: 'SENIOR', label: 'Senior' },
                                ]}
                            />
                        </div>

                        <div style={{ marginBottom: 24 }}>
                            <Text strong>Số câu hỏi:</Text>
                            <InputNumber
                                min={3}
                                max={10}
                                value={totalQuestions}
                                onChange={(v) => setTotalQuestions(v || 5)}
                                size="large"
                                style={{ width: '100%', marginTop: 8 }}
                            />
                        </div>

                        <Button
                            type="primary"
                            size="large"
                            icon={<RobotOutlined />}
                            onClick={handleStart}
                            loading={loading}
                            block
                        >
                            Bắt đầu phỏng vấn
                        </Button>

                        {loading && currentTaskId && (
                            <div style={{ marginTop: 16 }}>
                                <Progress percent={taskProgress} status={taskProgress >= 100 ? 'success' : 'active'} />
                                <Text type="secondary">{loadingText}</Text>
                                <Button danger block style={{ marginTop: 12 }} onClick={handleCancelTask}>
                                    Hủy task #{currentTaskId}
                                </Button>
                            </div>
                        )}
                    </div>

                    {/* LỊCH SỬ */}
                    <div className={styles.historySection}>
                        <Divider>
                            <Button
                                type="text"
                                icon={<HistoryOutlined />}
                                onClick={() => { setShowHistory(!showHistory); loadHistory(); }}
                            >
                                {showHistory ? 'Ẩn lịch sử' : 'Xem lịch sử phỏng vấn'}
                            </Button>
                        </Divider>

                        {showHistory && history.map((item: any) => (
                            <div
                                key={item.sessionId}
                                className={styles.historyItem}
                                onClick={() => handleViewSession(item.sessionId)}
                            >
                                <div>
                                    <Text strong>{item.jobPosition}</Text>
                                    <br />
                                    <Text type="secondary">
                                        {item.level} • {dayjs(item.createdAt).format('DD/MM/YYYY HH:mm')}
                                    </Text>
                                </div>
                                <Progress
                                    type="circle"
                                    percent={item.overallScore}
                                    size={48}
                                    strokeColor={getScoreColor(item.overallScore)}
                                />
                            </div>
                        ))}
                    </div>
                </>
            )}

            {/* ===== PHASE 2: CÂU HỎI PHỎNG VẤN ===== */}
            {phase === 'interview' && question && (
                <div className={styles.interviewArea}>
                    <div className={styles.progressBar}>
                        <Progress
                            percent={Math.round((question.questionNumber / question.totalQuestions) * 100)}
                            format={() => `${question.questionNumber}/${question.totalQuestions}`}
                        />
                    </div>

                    <div className={styles.questionCard}>
                        <div className={styles.questionNumber}>
                            Câu hỏi {question.questionNumber}
                        </div>
                        <div className={styles.questionText}>
                            {question.question}
                        </div>
                        <div className={styles.tags}>
                            <Tag color="blue">{question.category}</Tag>
                            <Tag color={
                                question.difficulty === 'EASY' ? 'green' :
                                    question.difficulty === 'HARD' ? 'red' : 'orange'
                            }>{question.difficulty}</Tag>
                        </div>
                    </div>

                    <div className={styles.answerArea}>
                        <TextArea
                            placeholder="Nhập câu trả lời của bạn..."
                            value={answer}
                            onChange={(e) => setAnswer(e.target.value)}
                            rows={5}
                            maxLength={2000}
                            showCount
                        />
                    </div>

                    <Button
                        type="primary"
                        size="large"
                        icon={<SendOutlined />}
                        onClick={handleSubmitAnswer}
                        loading={loading}
                        block
                        disabled={!answer.trim() || loading}
                    >
                        {loading ? '🤖 AI đang đánh giá câu trả lời...' : 'Gửi câu trả lời'}
                    </Button>

                    {loading && (
                        <div style={{ textAlign: 'center', marginTop: 16 }}>
                            <Spin />
                            <Text type="secondary" style={{ display: 'block', marginTop: 8 }}>
                                {loadingText || 'Quá trình này có thể mất 5-10 giây...'}
                            </Text>
                            <Progress percent={taskProgress} style={{ marginTop: 12 }} />
                            {currentTaskId && (
                                <Button danger style={{ marginTop: 12 }} onClick={handleCancelTask}>
                                    Hủy task #{currentTaskId}
                                </Button>
                            )}
                        </div>
                    )}
                </div>
            )}

            {/* ===== PHASE 3: FEEDBACK ===== */}
            {phase === 'feedback' && feedback && (
                <div className={styles.interviewArea}>
                    <Card className={styles.feedbackCard}>
                        <div className={styles.scoreCircle}>
                            <Progress
                                type="circle"
                                percent={feedback.score}
                                size={100}
                                strokeColor={getScoreColor(feedback.score)}
                            />
                        </div>
                        <div className={styles.feedbackText}>
                            <Text strong>Nhận xét: </Text>
                            <Paragraph>{feedback.feedback}</Paragraph>
                        </div>
                        <div className={styles.betterAnswer}>
                            <h4>💡 Câu trả lời gợi ý:</h4>
                            <p>{feedback.betterAnswer}</p>
                        </div>
                    </Card>

                    <Button
                        type="primary"
                        size="large"
                        icon={feedback.lastQuestion ? <TrophyOutlined /> : <SendOutlined />}
                        onClick={handleNext}
                        loading={loading}
                        block
                    >
                        {feedback.lastQuestion ? 'Xem tổng kết' : 'Câu hỏi tiếp theo →'}
                    </Button>
                </div>
            )}

            {/* ===== PHASE 4: TỔNG KẾT ===== */}
            {phase === 'summary' && summary && (
                <div className={styles.summaryCard}>
                    <TrophyOutlined style={{ fontSize: 48, color: '#faad14' }} />
                    <Title level={3}>Kết quả phỏng vấn</Title>
                    <Text type="secondary">{summary.jobPosition} • {summary.level}</Text>

                    <div className={styles.summaryScore}>
                        <Progress
                            type="circle"
                            percent={summary.overallScore}
                            size={120}
                            strokeColor={getScoreColor(summary.overallScore)}
                            format={(p) => `${p}%`}
                        />
                    </div>

                    <Paragraph className={styles.summaryText}>
                        {summary.finalSummary}
                    </Paragraph>

                    <Divider>Chi tiết từng câu</Divider>

                    {summary.questions?.map((q: any) => (
                        <div key={q.questionNumber} className={styles.questionResult}>
                            <div className={styles.qHeader}>
                                <Tag>Câu {q.questionNumber}</Tag>
                                <Progress
                                    type="circle"
                                    percent={q.score}
                                    size={36}
                                    strokeColor={getScoreColor(q.score)}
                                />
                            </div>
                            <div className={styles.qQuestion}>❓ {q.question}</div>
                            <div className={styles.qAnswer}>💬 {q.answer}</div>
                            <Text type="secondary" style={{ fontSize: 13 }}>
                                📝 {q.feedback}
                            </Text>
                        </div>
                    ))}

                    <Button
                        type="primary"
                        icon={<ReloadOutlined />}
                        onClick={handleReset}
                        size="large"
                        style={{ marginTop: 24 }}
                        block
                    >
                        Phỏng vấn mới
                    </Button>
                </div>
            )}

            {/* Loading overlay */}
            {loading && phase === 'setup' && (
                <div style={{ textAlign: 'center', padding: 40 }}>
                    <Spin size="large" />
                    <Progress percent={taskProgress} style={{ maxWidth: 360, margin: '16px auto 0' }} />
                    <p style={{ marginTop: 12, color: '#666' }}>{loadingText || 'AI đang chuẩn bị câu hỏi...'}</p>
                    {currentTaskId && (
                        <Button danger onClick={handleCancelTask}>
                            Hủy task #{currentTaskId}
                        </Button>
                    )}
                </div>
            )}
        </div>
    );
};

export default InterviewCoachPage;
