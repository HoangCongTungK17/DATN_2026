import { callFetchJob } from "@/config/api";
import { convertSlug, getLocationName, LOCATION_LIST } from "@/config/utils";
import { IJob } from "@/types/backend";
import { EnvironmentOutlined, ClockCircleOutlined } from "@ant-design/icons";
import { Card, Col, Empty, Pagination, Row, Spin } from "antd";
import { useState, useEffect } from "react";
import {
  Link,
  useLocation,
  useNavigate,
  useSearchParams,
} from "react-router-dom";
import styles from "@/styles/client.module.scss";
import { sfIn } from "spring-filter-query-builder";
import dayjs from "dayjs";
import relativeTime from "dayjs/plugin/relativeTime";

dayjs.extend(relativeTime);

interface IProps {
  showPagination?: boolean;
}

const JobCard = (props: IProps) => {
  const { showPagination = false } = props;

  const [displayJob, setDisplayJob] = useState<IJob[] | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(false);

  const [current, setCurrent] = useState(1);
  const [pageSize, setPageSize] = useState(6);
  const [total, setTotal] = useState(0);
  const [filter, setFilter] = useState("");
  const [sortQuery, setSortQuery] = useState("sort=updatedAt,desc");
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const location = useLocation();

  useEffect(() => {
    fetchJob();
  }, [current, pageSize, filter, sortQuery, location]);

  const fetchJob = async () => {
    setIsLoading(true);
    let query = `page=${current}&size=${pageSize}`;
    if (filter) {
      query += `&${filter}`;
    }
    if (sortQuery) {
      query += `&${sortQuery}`;
    }

    // --- LOGIC TÌM KIẾM TỪ URL ---
    const queryLocation = searchParams.get("location");
    const querySkills = searchParams.get("skills");

    if (queryLocation || querySkills) {
      let q = "";

      // 1. Tìm theo địa điểm (Location)
      if (queryLocation) {
        const locationValues = queryLocation.split(",");
        const locationLabels = locationValues.map((val) => {
          const found = LOCATION_LIST.find((item) => item.value === val);
          return found ? found.label : val;
        });

        q = sfIn("location", locationLabels).toString();
      }

      // 2. Tìm theo kỹ năng (Skills)
      if (querySkills) {
        // QUAN TRỌNG: Phải tìm theo "skills.name" thay vì "skills"
        const skillFilter = sfIn(
          "skills.name",
          querySkills.split(",")
        ).toString();

        // Nếu đã có filter location thì nối bằng "and", ngược lại lấy luôn
        q = q ? `${q} and ${skillFilter}` : skillFilter;
      }

      query += `&filter=${encodeURIComponent(q)}`;
    }
    // -----------------------------

    const res = await callFetchJob(query);
    if (res && res.data) {
      setDisplayJob(res.data.result);
      setTotal(res.data.meta.total);
    }
    setIsLoading(false);
  };

  const handleOnchangePage = (pagination: {
    current: number;
    pageSize: number;
  }) => {
    if (pagination && pagination.current !== current) {
      setCurrent(pagination.current);
    }
    if (pagination && pagination.pageSize !== pageSize) {
      setPageSize(pagination.pageSize);
      setCurrent(1);
    }
  };

  const handleViewDetailJob = (item: IJob) => {
    const slug = convertSlug(item.name);
    navigate(`/job/${slug}?id=${item.id}`);
  };

  return (
    <div className={styles["card-job-section"]}>
      <div className={styles["job-content"]}>
        <Spin spinning={isLoading} tip="Loading...">
          <Row gutter={[20, 20]}>
            <Col span={24}>
              <div className={styles["section-header"]}>
                <span className={styles["section-title"]}>
                  {showPagination
                    ? "Danh Sách Công Việc"
                    : "Công Việc Mới Nhất"}
                </span>
                {!showPagination && (
                  <Link to="/job" className={styles["view-all"]}>
                    Xem tất cả &rarr;
                  </Link>
                )}
              </div>
            </Col>

            {displayJob?.map((item) => {
              // Hiển thị địa điểm làm việc thực tế của job, không phải địa chỉ công ty
              const displayLocation = getLocationName(item.location);

              return (
                <Col span={24} md={12} key={item.id}>
                  <Card
                    className={styles["job-card-modern"]}
                    size="small"
                    title={null}
                    hoverable
                    onClick={() => handleViewDetailJob(item)}
                  >
                    <div className={styles["card-header"]}>
                      <img
                        alt="company-logo"
                        src={`${import.meta.env.VITE_BACKEND_URL
                          }/storage/company/${item?.company?.logo}`}
                        onError={(e: any) => {
                          e.target.onerror = null;
                          e.target.src = "/images/company/default-company.png";
                        }}
                      />
                      <div className={styles["info"]}>
                        <h3 className={styles["title"]}>{item.name}</h3>
                        <p className={styles["company"]}>
                          {item?.company?.name}
                        </p>
                      </div>
                    </div>

                    <div className={styles["card-body"]}>
                      <div className={styles["salary"]}>
                        {(item.salary + "")?.replace(
                          /\B(?=(\d{3})+(?!\d))/g,
                          ","
                        )}{" "}
                        đ
                      </div>

                      <div className={styles["footer"]}>
                        <div className={styles["location"]}>
                          <EnvironmentOutlined
                            style={{ color: "#0A65CC", marginRight: 4 }}
                          />
                          <span
                            className={styles["text-ellipsis"]}
                            title={displayLocation}
                          >
                            {displayLocation}
                          </span>
                        </div>
                        <div className={styles["time"]}>
                          <ClockCircleOutlined style={{ marginRight: 4 }} />
                          {item.updatedAt
                            ? dayjs(item.updatedAt).locale("en").fromNow()
                            : dayjs(item.createdAt).locale("en").fromNow()}
                        </div>
                      </div>
                    </div>
                  </Card>
                </Col>
              );
            })}

            {(!displayJob || (displayJob && displayJob.length === 0)) &&
              !isLoading && (
                <div className={styles["empty"]}>
                  <Empty description="Không tìm thấy công việc phù hợp" />
                </div>
              )}
          </Row>

          {showPagination && (
            <>
              <div style={{ marginTop: 30 }}></div>
              <Row style={{ display: "flex", justifyContent: "center" }}>
                <Pagination
                  current={current}
                  total={total}
                  pageSize={pageSize}
                  responsive
                  onChange={(p: number, s: number) =>
                    handleOnchangePage({ current: p, pageSize: s })
                  }
                />
              </Row>
            </>
          )}
        </Spin>
      </div>
    </div>
  );
};

export default JobCard;
