import { Col, Row } from "antd";
import styles from "@/styles/client.module.scss";
import CompanyCard from "@/components/client/card/company.card";
import {
  BankOutlined,
  StarOutlined,
  TeamOutlined,
} from "@ant-design/icons";

const ClientCompanyPage = (props: any) => {
  return (
    <div className={styles["page-company"]}>
      {/* --- MINI HERO / PAGE HEADER --- */}
      <div className={styles["page-hero"]}>
        <div className={styles["container"]}>
          <div className={styles["page-hero-content"]}>
            <div className={styles["page-hero-badge"]}>
              <BankOutlined />
              <span>Công Ty IT</span>
            </div>
            <h1 className={styles["page-hero-title"]}>
              Khám Phá <span>Công Ty Công Nghệ</span> Hàng Đầu
            </h1>
            <p className={styles["page-hero-desc"]}>
              Tìm hiểu văn hóa, môi trường làm việc và cơ hội nghề nghiệp tại
              các doanh nghiệp công nghệ uy tín nhất Việt Nam.
            </p>
          </div>
        </div>
      </div>

      {/* --- STATS BAR --- */}
      <div className={styles["container"]}>
        <div className={styles["stats-bar"]}>
          <div className={styles["stat-item"]}>
            <BankOutlined />
            <span>Doanh nghiệp uy tín</span>
          </div>
          <div className={styles["stat-divider"]}></div>
          <div className={styles["stat-item"]}>
            <StarOutlined />
            <span>Đánh giá chất lượng</span>
          </div>
          <div className={styles["stat-divider"]}></div>
          <div className={styles["stat-item"]}>
            <TeamOutlined />
            <span>Môi trường chuyên nghiệp</span>
          </div>
        </div>
      </div>

      {/* --- COMPANY LISTING --- */}
      <div className={styles["container"]} style={{ paddingBottom: 60 }}>
        <Row gutter={[20, 20]}>
          <Col span={24}>
            <CompanyCard showPagination={true} />
          </Col>
        </Row>
      </div>
    </div>
  );
};

export default ClientCompanyPage;