import { useLocation, useNavigate } from "react-router-dom";
import { useState, useEffect } from "react";
import { IJob } from "@/types/backend";
import { callFetchJobById } from "@/config/api";
import styles from "@/styles/client.module.scss";
import parse from "html-react-parser";
import { Col, Divider, Row, Tag, Button, Breadcrumb, Spin } from "antd";
import {
  EnvironmentOutlined,
  DollarOutlined,
  ClockCircleOutlined,
  CalendarOutlined,
  SafetyCertificateOutlined,
} from "@ant-design/icons";
import dayjs from "dayjs";
import relativeTime from "dayjs/plugin/relativeTime";
import ApplyModal from "@/components/client/modal/apply.modal";

dayjs.extend(relativeTime);

const ClientJobDetailPage = (props: any) => {
  const [jobDetail, setJobDetail] = useState<IJob | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [isModalOpen, setIsModalOpen] = useState<boolean>(false);

  let location = useLocation();
  let params = new URLSearchParams(location.search);
  const id = params?.get("id"); // job id

  useEffect(() => {
    const init = async () => {
      if (id) {
        setIsLoading(true);
        const res = await callFetchJobById(id);
        if (res?.data) {
          setJobDetail(res.data);
        }
        setIsLoading(false);
      }
    };
    init();
  }, [id]);

  // HÀM XỬ LÝ ĐỊA CHỈ AN TOÀN (FIX LỖI UNKNOWN & TYPE ERROR)
  // Nếu công ty không có địa chỉ -> Lấy địa điểm Job -> Mặc định
  const getJobLocation = () => {
    if (!jobDetail) return "";
    // Dùng (as any) để tránh lỗi TS nếu file type chưa cập nhật
    const companyAddress = (jobDetail.company as any)?.address;
    return companyAddress ?? jobDetail.location ?? "Đang cập nhật";
  };

  return (
    <div className={styles["job-detail-section"]}>
      {isLoading ? (
        <div
          style={{
            height: "80vh",
            display: "flex",
            justifyContent: "center",
            alignItems: "center",
          }}
        >
          <Spin size="large" tip="Đang tải thông tin công việc..." />
        </div>
      ) : (
        jobDetail &&
        jobDetail.id && (
          <>
            {/* --- HEADER SECTION (Hero Banner) --- */}
            <div className={styles["job-header-container"]}>
              <div className={styles["container"]}>
                <Breadcrumb
                  style={{ marginBottom: 20 }}
                  items={[
                    { title: "Trang chủ", href: "/" },
                    { title: "Việc làm IT", href: "/job" },
                    { title: jobDetail.name },
                  ]}
                />

                <div className={styles["header-content"]}>
                  <img
                    className={styles["company-logo"]}
                    src={`${import.meta.env.VITE_BACKEND_URL}/storage/company/${
                      jobDetail.company?.logo
                    }`}
                    alt="Company Logo"
                    onError={(e: any) => {
                      e.target.onerror = null;
                      e.target.src = "/images/company/default-company.png";
                    }}
                  />
                  <div className={styles["job-info-header"]}>
                    <h1 className={styles["job-title"]}>{jobDetail.name}</h1>
                    <div className={styles["job-meta"]}>
                      <span className={styles["salary"]}>
                        <DollarOutlined />
                        {(jobDetail.salary + "")?.replace(
                          /\B(?=(\d{3})+(?!\d))/g,
                          ","
                        )}{" "}
                        đ
                      </span>
                      <span>
                        <EnvironmentOutlined /> {getJobLocation()}
                      </span>
                      <span>
                        <ClockCircleOutlined />{" "}
                        {jobDetail.updatedAt
                          ? dayjs(jobDetail.updatedAt).locale("en").fromNow()
                          : dayjs(jobDetail.createdAt).locale("en").fromNow()}
                      </span>
                    </div>
                  </div>
                  <Button
                    type="primary"
                    size="large"
                    className={styles["btn-apply"]}
                    onClick={() => setIsModalOpen(true)}
                  >
                    Ứng Tuyển Ngay
                  </Button>
                </div>
              </div>
            </div>

            {/* --- MAIN CONTENT --- */}
            <div className={styles["container"]}>
              <Row gutter={[24, 24]}>
                {/* Cột trái: Nội dung chi tiết */}
                <Col span={24} md={16}>
                  <div className={styles["job-description-card"]}>
                    <h3>Mô tả công việc</h3>
                    <Divider style={{ margin: "12px 0 20px" }} />
                    {parse(jobDetail.description)}

                    {/* Phần Kỹ năng bổ sung nếu cần */}
                    {jobDetail.skills && jobDetail.skills.length > 0 && (
                      <div style={{ marginTop: 20 }}>
                        <h4>Kỹ năng yêu cầu:</h4>
                        <div
                          style={{
                            display: "flex",
                            flexWrap: "wrap",
                            gap: 10,
                            marginTop: 10,
                          }}
                        >
                          {jobDetail?.skills?.map((item, index) => (
                            <Tag
                              color="geekblue"
                              key={index}
                              style={{ fontSize: 14, padding: "5px 10px" }}
                            >
                              {item.name}
                            </Tag>
                          ))}
                        </div>
                      </div>
                    )}
                  </div>
                </Col>

                {/* Cột phải: Thông tin tóm tắt & Công ty */}
                <Col span={24} md={8}>
                  {/* Card 1: Tổng quan Job */}
                  <div className={styles["company-sidebar-card"]}>
                    <div className={styles["sidebar-title"]}>
                      Thông tin chung
                    </div>

                    <div className={styles["info-row"]}>
                      <div className={styles["icon"]}>
                        <CalendarOutlined />
                      </div>
                      <div className={styles["text"]}>
                        <div>Ngày đăng</div>
                        <div>
                          {dayjs(jobDetail.createdAt).format("DD/MM/YYYY")}
                        </div>
                      </div>
                    </div>

                    <div className={styles["info-row"]}>
                      <div className={styles["icon"]}>
                        <ClockCircleOutlined />
                      </div>
                      <div className={styles["text"]}>
                        <div>Hạn nộp hồ sơ</div>
                        <div>
                          {dayjs(jobDetail.endDate).format("DD/MM/YYYY")}
                        </div>
                      </div>
                    </div>

                    <div className={styles["info-row"]}>
                      <div className={styles["icon"]}>
                        <SafetyCertificateOutlined />
                      </div>
                      <div className={styles["text"]}>
                        <div>Cấp bậc</div>
                        <div>{jobDetail.level ?? "Nhân viên"}</div>
                      </div>
                    </div>

                    <div className={styles["info-row"]}>
                      <div className={styles["icon"]}>
                        <DollarOutlined />
                      </div>
                      <div className={styles["text"]}>
                        <div>Mức lương</div>
                        <div>
                          {(jobDetail.salary + "")?.replace(
                            /\B(?=(\d{3})+(?!\d))/g,
                            ","
                          )}{" "}
                          đ
                        </div>
                      </div>
                    </div>

                    <div className={styles["info-row"]}>
                      <div className={styles["icon"]}>
                        <EnvironmentOutlined />
                      </div>
                      <div className={styles["text"]}>
                        <div>Địa điểm</div>
                        <div>{jobDetail.location}</div>
                      </div>
                    </div>
                  </div>

                  {/* Card 2: Thông tin công ty */}
                  <div className={styles["company-sidebar-card"]}>
                    <div className={styles["sidebar-title"]}>Về công ty</div>
                    <div
                      style={{
                        display: "flex",
                        alignItems: "center",
                        gap: 12,
                        marginBottom: 15,
                      }}
                    >
                      <img
                        src={`${
                          import.meta.env.VITE_BACKEND_URL
                        }/storage/company/${jobDetail.company?.logo}`}
                        alt="logo"
                        style={{
                          width: 50,
                          height: 50,
                          objectFit: "contain",
                          borderRadius: 8,
                          border: "1px solid #f0f0f0",
                        }}
                        onError={(e: any) => {
                          e.target.onerror = null;
                          e.target.src = "/images/company/default-company.png";
                        }}
                      />
                      <div
                        style={{
                          fontWeight: 700,
                          fontSize: 16,
                          lineHeight: 1.4,
                        }}
                      >
                        {jobDetail.company?.name}
                      </div>
                    </div>
                    <div
                      style={{
                        color: "#666",
                        fontSize: 14,
                        marginBottom: 15,
                        lineHeight: 1.5,
                      }}
                    >
                      <EnvironmentOutlined style={{ marginRight: 6 }} />
                      {/* Gọi hàm fix lỗi địa chỉ */}
                      {getJobLocation()}
                    </div>
                  </div>
                </Col>
              </Row>
            </div>
          </>
        )
      )}

      <ApplyModal
        isModalOpen={isModalOpen}
        setIsModalOpen={setIsModalOpen}
        jobDetail={jobDetail}
      />
    </div>
  );
};

export default ClientJobDetailPage;
