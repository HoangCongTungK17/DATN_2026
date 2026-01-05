import { Card, Col, Empty, Pagination, Row, Spin } from "antd";
import { useState, useEffect } from "react";
import { isMobile } from "react-device-detect";
import { Link, useNavigate } from "react-router-dom";
import styles from "@/styles/client.module.scss";
import { callFetchCompany } from "@/config/api";
import { ICompany } from "@/types/backend";
import { convertSlug } from "@/config/utils";

interface IProps {
  showPagination?: boolean;
}

const CompanyCard = (props: IProps) => {
  const { showPagination = false } = props;
  const [displayCompany, setDisplayCompany] = useState<ICompany[] | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [current, setCurrent] = useState(1);
  const [pageSize, setPageSize] = useState(8); // Tăng lên 8 để hiển thị 2 hàng cho đẹp
  const [total, setTotal] = useState(0);

  const navigate = useNavigate();

  useEffect(() => {
    fetchCompany();
  }, [current, pageSize]);

  const fetchCompany = async () => {
    setIsLoading(true);
    let query = `page=${current}&size=${pageSize}`;
    const res = await callFetchCompany(query);
    if (res && res.data) {
      setDisplayCompany(res.data.result);
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

  const handleViewDetail = (item: ICompany) => {
    if (item.name) {
      const slug = convertSlug(item.name);
      navigate(`/company/${slug}?id=${item.id}`);
    }
  };

  return (
    <div className={styles["company-card-section"]}>
      <Spin spinning={isLoading} tip="Loading...">
        <Row gutter={[20, 20]}>
          <Col span={24}>
            {/* --- SỬA PHẦN NÀY ĐỂ FIX LỖI DÍNH CHỮ --- */}
            <div className={styles["section-header"]}>
              <span className={styles["section-title"]}>
                Nhà Tuyển Dụng Hàng Đầu
              </span>
              {!showPagination && (
                <Link to="/company" className={styles["view-all"]}>
                  Xem tất cả &rarr;
                </Link>
              )}
            </div>
            {/* ---------------------------------------- */}
          </Col>

          {displayCompany?.map((item) => {
            return (
              <Col span={24} md={8} lg={6} key={item.id}>
                <Card
                  className={styles["company-card-modern"]}
                  hoverable
                  onClick={() => handleViewDetail(item)}
                >
                  <div className={styles["company-logo-wrapper"]}>
                    <img
                      alt="example"
                      src={`${
                        import.meta.env.VITE_BACKEND_URL
                      }/storage/company/${item.logo}`}
                      onError={(e: any) => {
                        e.target.onerror = null;
                        e.target.src = "/images/company/default-company.png";
                      }}
                    />
                  </div>
                  <div className={styles["company-info"]}>
                    <h3 className={styles["company-name"]}>{item.name}</h3>
                    <p className={styles["company-address"]}>{item.address}</p>
                  </div>
                  <div className={styles["company-footer"]}>
                    <span className={styles["jobs-count"]}>Xem chi tiết</span>
                  </div>
                </Card>
              </Col>
            );
          })}

          {(!displayCompany || displayCompany.length === 0) && !isLoading && (
            <div className={styles["empty"]}>
              <Empty description="Không có dữ liệu" />
            </div>
          )}
        </Row>

        {showPagination && (
          <div
            style={{ marginTop: 30, display: "flex", justifyContent: "center" }}
          >
            <Pagination
              current={current}
              total={total}
              pageSize={pageSize}
              responsive
              onChange={(p, s) =>
                handleOnchangePage({ current: p, pageSize: s })
              }
            />
          </div>
        )}
      </Spin>
    </div>
  );
};

export default CompanyCard;
