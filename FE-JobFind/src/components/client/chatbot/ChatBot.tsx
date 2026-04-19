import { useState, useRef, useEffect } from "react";
import "./chatbot.scss";
import { callFetchChat } from "@/config/api";

interface IMessage {
  text: string;
  sender: "user" | "bot";
  timestamp: Date;
  isError?: boolean;
}

const ChatBot = () => {
  const [isOpen, setIsOpen] = useState(false);
  const [messages, setMessages] = useState<IMessage[]>([
    {
      text: "Chào bạn! 👋 Mình là JobFind AI — trợ lý tuyển dụng IT thông minh.\n\nBạn có thể hỏi mình về:\n• Tìm việc làm theo kỹ năng\n• Tư vấn lộ trình sự nghiệp\n• Mức lương thị trường",
      sender: "bot",
      timestamp: new Date(),
    },
  ]);
  const [inputValue, setInputValue] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const messagesEndRef = useRef<HTMLDivElement>(null);

  // Auto scroll to bottom when new messages arrive
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages]);

  const handleSendMessage = async () => {
    if (!inputValue.trim() || isLoading) return;

    const userMsg = inputValue.trim();
    setMessages((prev) => [
      ...prev,
      { text: userMsg, sender: "user", timestamp: new Date() },
    ]);
    setInputValue("");
    setIsLoading(true);

    try {
      // Call the updated API with proper response handling
      const res = await callFetchChat(userMsg);

      // The axios interceptor already extracts res.data, so res is IBackendRes<string>
      // Therefore, res.data is the actual string response from the AI
      const botMsg =
        res?.data || "Xin lỗi, tôi không nhận được phản hồi từ server.";

      setMessages((prev) => [
        ...prev,
        {
          text: botMsg,
          sender: "bot",
          timestamp: new Date(),
        },
      ]);
    } catch (error: any) {
      console.error("ChatBot Error:", error);

      let errorMessage = "⚠️ Lỗi kết nối Server AI. Vui lòng thử lại!";

      // Handle specific error cases
      if (error?.response?.status === 500) {
        errorMessage = "⚠️ Server AI gặp lỗi. Vui lòng thử lại sau ít phút.";
      } else if (error?.response?.status === 404) {
        errorMessage = "⚠️ API không tồn tại. Vui lòng kiểm tra backend.";
      } else if (error?.message?.includes("Network Error")) {
        errorMessage = "⚠️ Không thể kết nối đến server. Vui lòng kiểm tra kết nối mạng.";
      }

      setMessages((prev) => [
        ...prev,
        {
          text: errorMessage,
          sender: "bot",
          timestamp: new Date(),
          isError: true,
        },
      ]);
    } finally {
      setIsLoading(false);
    }
  };

  const handleKeyPress = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      handleSendMessage();
    }
  };

  const formatMessage = (text: string) => {
    // Simple formatting to preserve line breaks and make text more readable
    return text.split('\n').map((line, index) => (
      <span key={index}>
        {line}
        {index < text.split('\n').length - 1 && <br />}
      </span>
    ));
  };

  const quickActions = [
    "Tìm việc React ở HN",
    "Lương Java Developer",
    "Tư vấn chuyển ngành",
  ];

  return (
    <div className="chatbot-container">
      {!isOpen && (
        <button
          className="toggle-btn"
          onClick={() => setIsOpen(true)}
          aria-label="Open ChatBot"
        >
          <span className="toggle-icon">💬</span>
          <span className="toggle-pulse"></span>
        </button>
      )}

      {isOpen && (
        <div className="chat-window">
          <div className="header">
            <div className="header-left">
              <div className="header-avatar">
                <span>🤖</span>
                <span className="online-dot"></span>
              </div>
              <div className="header-info">
                <span className="header-title">JobFind AI</span>
                <span className="header-status">Trực tuyến — sẵn sàng hỗ trợ</span>
              </div>
            </div>
            <button
              className="close-btn"
              onClick={() => setIsOpen(false)}
              aria-label="Close ChatBot"
            >
              ✕
            </button>
          </div>

          <div className="messages">
            {messages.map((msg, index) => (
              <div
                key={index}
                className={`message ${msg.sender} ${msg.isError ? 'error' : ''}`}
              >
                {msg.sender === 'bot' && (
                  <div className="bot-avatar">🤖</div>
                )}
                <div className="message-bubble">
                  <div className="message-content">
                    {formatMessage(msg.text)}
                  </div>
                  <div className="message-time">
                    {msg.timestamp.toLocaleTimeString('vi-VN', {
                      hour: '2-digit',
                      minute: '2-digit'
                    })}
                  </div>
                </div>
              </div>
            ))}

            {/* Quick actions after first bot message */}
            {messages.length === 1 && !isLoading && (
              <div className="quick-actions">
                {quickActions.map((action, index) => (
                  <button
                    key={index}
                    className="quick-action-btn"
                    onClick={() => {
                      setInputValue(action);
                    }}
                  >
                    {action}
                  </button>
                ))}
              </div>
            )}

            {isLoading && (
              <div className="message bot typing">
                <div className="bot-avatar">🤖</div>
                <div className="message-bubble">
                  <div className="typing-indicator">
                    <span></span>
                    <span></span>
                    <span></span>
                  </div>
                </div>
              </div>
            )}
            <div ref={messagesEndRef} />
          </div>

          <div className="input-area">
            <input
              type="text"
              placeholder="Hỏi gì về việc làm IT..."
              value={inputValue}
              onChange={(e) => setInputValue(e.target.value)}
              onKeyDown={handleKeyPress}
              disabled={isLoading}
              aria-label="Message input"
            />
            <button
              onClick={handleSendMessage}
              disabled={isLoading || !inputValue.trim()}
              aria-label="Send message"
              className="send-btn"
            >
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path d="M22 2L11 13" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
                <path d="M22 2L15 22L11 13L2 9L22 2Z" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
              </svg>
            </button>
          </div>
        </div>
      )}
    </div>
  );
};

export default ChatBot;
