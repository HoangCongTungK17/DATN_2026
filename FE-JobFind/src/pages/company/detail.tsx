import { useLocation, useNavigate } from "react-router-dom";
import { useState, useEffect } from "react";
import { ICompany } from "@/types/backend";
import { callFetchCompanyById } from "@/config/api";
import styles from "@/styles/client.module.scss";
import parse from "html-react-parser";
import { Col, Row, Spin, Breadcrumb, Empty, Button } from "antd";
import { EnvironmentOutlined, ArrowLeftOutlined } from "@ant-design/icons";

const ClientCompanyDetailPage = (props: any) => {
  const [companyDetail, setCompanyDetail] = useState<ICompany | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const navigate = useNavigate();

  let location = useLocation();
  let params = new URLSearchParams(location.search);
  const id = params?.get("id");

  useEffect(() => {
    const init = async () => {
      if (id) {
        setIsLoading(true);
        try {
          const res = await callFetchCompanyById(id);
          if (res?.data) {
            setCompanyDetail(res.data);
          } else {
            // Nếu API trả về thành công nhưng không có data
            setCompanyDetail(null);
          }
        } catch (error) {
          console.error("Lỗi khi tải thông tin công ty:", error);
          setCompanyDetail(null);
        }
        setIsLoading(false);
      }
    };
    init();
  }, [id]);

  // 1. Loading State
  if (isLoading) {
    return (
      <div className={styles["job-detail-section"]}>
        <div
          style={{
            minHeight: "80vh",
            display: "flex",
            justifyContent: "center",
            alignItems: "center",
          }}
        >
          <Spin size="large" tip="Đang tải thông tin công ty..." />
        </div>
      </div>
    );
  }

  // 2. Error / Empty State (Xử lý màn hình trắng)
  if (!companyDetail || !companyDetail.id) {
    return (
      <div className={styles["job-detail-section"]}>
        <div className={styles["container"]}>
          <div style={{ padding: "100px 0", textAlign: "center" }}>
            <Empty description="Không tìm thấy thông tin công ty này" />
            <Button
              type="primary"
              onClick={() => navigate("/company")}
              style={{ marginTop: 20 }}
              icon={<ArrowLeftOutlined />}
            >
              Quay lại danh sách
            </Button>
          </div>
        </div>
      </div>
    );
  }

  // 3. Main Content (Khi có dữ liệu)
  return (
    <div className={styles["job-detail-section"]}>
      {/* HEADER SECTION */}
      <div className={styles["job-header-container"]}>
        <div className={styles["container"]}>
          <Breadcrumb
            style={{ marginBottom: 20 }}
            items={[
              { title: "Trang chủ", href: "/" },
              { title: "Công ty", href: "/company" },
              { title: companyDetail.name },
            ]}
          />

          <div className={styles["header-content"]}>
            <img
              className={styles["company-logo"]}
              src={`${import.meta.env.VITE_BACKEND_URL}/storage/company/${
                companyDetail?.logo
              }`}
              alt="Company Logo"
              onError={(e: any) => {
                e.target.onerror = null;
                e.target.src = "/images/company/default-company.png";
              }}
            />

            <div className={styles["job-info-header"]}>
              <h1 className={styles["job-title"]}>{companyDetail.name}</h1>
              <div className={styles["job-meta"]}>
                <span>
                  <EnvironmentOutlined />
                  &nbsp;{companyDetail?.address ?? "Đang cập nhật địa chỉ"}
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* BODY CONTENT */}
      <div className={styles["container"]}>
        <Row gutter={[24, 24]}>
          {/* Cột Trái: Giới thiệu */}
          <Col span={24} md={16}>
            <div className={styles["job-description-card"]}>
              <h3>Giới thiệu công ty</h3>
              <div style={{ marginTop: 15, lineHeight: "1.8", color: "#444" }}>
                {parse(
                  companyDetail?.description ?? "<p>Chưa có mô tả chi tiết.</p>"
                )}
              </div>
            </div>
          </Col>

          {/* Cột Phải: Liên hệ */}
          <Col span={24} md={8}>
            <div className={styles["company-sidebar-card"]}>
              <div className={styles["sidebar-title"]}>Thông tin liên hệ</div>

              <div className={styles["info-row"]}>
                <div className={styles["icon"]}>
                  <EnvironmentOutlined />
                </div>
                <div className={styles["text"]}>
                  <div>Địa chỉ</div>
                  <div>{companyDetail?.address ?? "Chưa cập nhật"}</div>
                </div>
              </div>
            </div>
          </Col>
        </Row>
      </div>
    </div>
  );
};

export default ClientCompanyDetailPage;
