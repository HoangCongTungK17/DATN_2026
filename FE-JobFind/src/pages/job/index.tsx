import SearchClient from "@/components/client/search.client";
import { Col, Row } from "antd";
import styles from "@/styles/client.module.scss";
import JobCard from "@/components/client/card/job.card";
import {
  CodeOutlined,
  SearchOutlined,
  ThunderboltOutlined,
} from "@ant-design/icons";

const ClientJobPage = (props: any) => {
  return (
    <div className={styles["page-job"]}>
      {/* --- MINI HERO / PAGE HEADER --- */}
      <div className={styles["page-hero"]}>
        <div className={styles["container"]}>
          <div className={styles["page-hero-content"]}>
            <div className={styles["page-hero-badge"]}>
              <CodeOutlined />
              <span>Việc Làm IT</span>
            </div>
            <h1 className={styles["page-hero-title"]}>
              Tìm Kiếm <span>Việc Làm IT</span> Phù Hợp
            </h1>
            <p className={styles["page-hero-desc"]}>
              Khám phá hàng ngàn vị trí công nghệ từ các công ty hàng đầu.
              Lọc theo kỹ năng và địa điểm để tìm đúng cơ hội cho bạn.
            </p>
          </div>

          {/* Search bar */}
          <div className={styles["page-search-wrapper"]}>
            <SearchClient />
          </div>
        </div>
      </div>

      {/* --- STATS BAR --- */}
      <div className={styles["container"]}>
        <div className={styles["stats-bar"]}>
          <div className={styles["stat-item"]}>
            <ThunderboltOutlined />
            <span>Cập nhật mỗi ngày</span>
          </div>
          <div className={styles["stat-divider"]}></div>
          <div className={styles["stat-item"]}>
            <SearchOutlined />
            <span>Tìm kiếm thông minh</span>
          </div>
          <div className={styles["stat-divider"]}></div>
          <div className={styles["stat-item"]}>
            <CodeOutlined />
            <span>Chuyên ngành IT</span>
          </div>
        </div>
      </div>

      {/* --- JOB LISTING --- */}
      <div className={styles["container"]} style={{ paddingBottom: 60 }}>
        <Row gutter={[20, 20]}>
          <Col span={24}>
            <JobCard showPagination={true} />
          </Col>
        </Row>
      </div>
    </div>
  );
};

export default ClientJobPage;