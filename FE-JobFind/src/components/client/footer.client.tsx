import { Layout, Row, Col } from "antd";
import {
  FireOutlined,
  FacebookOutlined,
  TwitterOutlined,
  LinkedinOutlined,
  InstagramOutlined,
  MailOutlined,
  PhoneOutlined,
  EnvironmentOutlined,
} from "@ant-design/icons";
import styles from "@/styles/client.module.scss";
import { useNavigate } from "react-router-dom";

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
              >
                <FireOutlined className={styles["brand-icon"]} />
                <span className={styles["brand-text"]}>
                  JOB<span style={{ color: "#0A65CC" }}>FIND</span>
                </span>
              </div>
              <p className={styles["footer-desc"]}>
                Nền tảng tuyển dụng công nghệ hàng đầu, kết nối ứng viên tiềm
                năng với các doanh nghiệp uy tín.
              </p>
              <div className={styles["social-icons"]}>
                <div className={styles["icon-wrapper"]}>
                  <FacebookOutlined />
                </div>
                <div className={styles["icon-wrapper"]}>
                  <TwitterOutlined />
                </div>
                <div className={styles["icon-wrapper"]}>
                  <LinkedinOutlined />
                </div>
                <div className={styles["icon-wrapper"]}>
                  <InstagramOutlined />
                </div>
              </div>
            </Col>

            {/* Cột 2: Dành cho ứng viên */}
            <Col xs={24} sm={12} md={6}>
              <h3 className={styles["footer-heading"]}>Dành cho ứng viên</h3>
              <ul className={styles["footer-links"]}>
                <li onClick={() => navigate("/job")}>Việc làm mới nhất</li>
                <li onClick={() => navigate("/company")}>Danh sách công ty</li>
                <li>Cẩm nang nghề nghiệp</li>
                <li>Tạo CV trực tuyến</li>
              </ul>
            </Col>

            {/* Cột 3: Dành cho nhà tuyển dụng */}
            <Col xs={24} sm={12} md={6}>
              <h3 className={styles["footer-heading"]}>Nhà tuyển dụng</h3>
              <ul className={styles["footer-links"]}>
                <li>Đăng tin tuyển dụng</li>
                <li>Tìm kiếm hồ sơ</li>
                <li>Giải pháp nhân sự</li>
                <li>Liên hệ báo giá</li>
              </ul>
            </Col>

            {/* Cột 4: Liên hệ */}
            <Col xs={24} sm={24} md={6}>
              <h3 className={styles["footer-heading"]}>Liên hệ</h3>
              <div className={styles["contact-info"]}>
                <div className={styles["contact-item"]}>
                  <EnvironmentOutlined />
                  <span>Tầng 10, Tòa nhà Tech, Hà Nội</span>
                </div>
                <div className={styles["contact-item"]}>
                  <PhoneOutlined />
                  <span>(024) 6688 9999</span>
                </div>
                <div className={styles["contact-item"]}>
                  <MailOutlined />
                  <span>support@jobfind.com</span>
                </div>
              </div>
            </Col>
          </Row>
        </div>

        <div className={styles["footer-bottom"]}>
          <div className={styles["copyright"]}>
            &copy; {new Date().getFullYear()} JobFind. Design by Hoang Tung. All
            rights reserved.
          </div>
        </div>
      </div>
    </footer>
  );
};

export default Footer;
