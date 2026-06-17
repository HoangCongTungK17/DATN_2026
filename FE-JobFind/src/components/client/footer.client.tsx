import { Layout, Row, Col } from "antd";
import {
  ThunderboltFilled,
  FacebookOutlined,
  TwitterOutlined,
  LinkedinOutlined,
  InstagramOutlined,
  MailOutlined,
  PhoneOutlined,
  EnvironmentOutlined,
  GithubOutlined,
  HeartFilled,
} from "@ant-design/icons";
import styles from "@/styles/client.module.scss";
import { useNavigate } from "react-router-dom";
import logo from '@/assets/logo-optimized.png';

const Footer = () => {
  const navigate = useNavigate();

  return (
    <footer className={styles["footer-section"]}>
      <div className={styles["container"]}>
        <div className={styles["footer-top"]}>
          <Row gutter={[40, 40]}>
            {/* Cột 1: Thông tin thương hiệu */}
            <Col xs={24} sm={24} md={6}>
              <div
                className={styles["footer-brand"]}
                onClick={() => navigate("/")}
                style={{ height: '80px', width: '200px', display: 'flex', alignItems: 'center', justifyContent: 'center', cursor: 'pointer' }}
              >
                <img src={logo} alt="JobFind Logo" style={{ maxWidth: '100%', maxHeight: '100%', objectFit: 'contain' }} />
              </div>
              <p className={styles["footer-desc"]}>
                Nền tảng tuyển dụng IT hàng đầu Việt Nam — kết nối ứng viên
                tài năng với doanh nghiệp công nghệ uy tín.
              </p>
              <div className={styles["social-icons"]}>
                <div className={styles["icon-wrapper"]}>
                  <FacebookOutlined />
                </div>
                <div className={styles["icon-wrapper"]}>
                  <LinkedinOutlined />
                </div>
                <div className={styles["icon-wrapper"]}>
                  <GithubOutlined />
                </div>
                <div className={styles["icon-wrapper"]}>
                  <TwitterOutlined />
                </div>
              </div>
            </Col>

            {/* Cột 2: Dành cho ứng viên */}
            <Col xs={24} sm={12} md={6}>
              <h3 className={styles["footer-heading"]}>Ứng viên</h3>
              <ul className={styles["footer-links"]}>
                <li onClick={() => navigate("/job")}>Tìm việc làm IT</li>
                <li onClick={() => navigate("/company")}>Khám phá công ty</li>
                <li onClick={() => navigate("/ai-hub")}>AI Hub — Công cụ AI</li>
                <li>Cẩm nang nghề nghiệp</li>
                <li>Xây dựng CV chuyên nghiệp</li>
              </ul>
            </Col>

            {/* Cột 3: Dành cho nhà tuyển dụng */}
            <Col xs={24} sm={12} md={6}>
              <h3 className={styles["footer-heading"]}>Nhà tuyển dụng</h3>
              <ul className={styles["footer-links"]}>
                <li>Đăng tin tuyển dụng</li>
                <li>Tìm kiếm hồ sơ ứng viên</li>
                <li>Giải pháp tuyển dụng AI</li>
                <li>Thương hiệu nhà tuyển dụng</li>
                <li>Liên hệ hợp tác</li>
              </ul>
            </Col>

            {/* Cột 4: Liên hệ */}
            <Col xs={24} sm={24} md={6}>
              <h3 className={styles["footer-heading"]}>Liên hệ</h3>
              <div className={styles["contact-info"]}>
                <div className={styles["contact-item"]}>
                  <EnvironmentOutlined />
                  <span>Số 1 Đại Cồ Việt, Hai Bà Trưng, Hà Nội</span>
                </div>
                <div className={styles["contact-item"]}>
                  <PhoneOutlined />
                  <span>(024) 3869 4242</span>
                </div>
                <div className={styles["contact-item"]}>
                  <MailOutlined />
                  <span>support@jobfind.vn</span>
                </div>
              </div>
            </Col>
          </Row>
        </div>

        <div className={styles["footer-bottom"]}>
          <div className={styles["copyright"]}>
            &copy; {new Date().getFullYear()} JobFind — Made with{" "}
            <HeartFilled style={{ color: "#ef4444", fontSize: 12 }} /> by Hoang
            Tung. All rights reserved.
          </div>
        </div>
      </div>
    </footer>
  );
};

export default Footer;
