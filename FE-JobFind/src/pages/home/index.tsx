import { Divider } from "antd";
import styles from "@/styles/client.module.scss"; // Dùng alias @ cho đồng bộ
import SearchClient from "@/components/client/search.client";
import JobCard from "@/components/client/card/job.card";
import CompanyCard from "@/components/client/card/company.card";

const HomePage = () => {
  return (
    <div className={styles["homepage-container"]}>
      {/* --- HERO SECTION --- */}
      {/* Vùng Banner đầu trang với background gradient */}
      <div className={styles["hero-section"]}>
        <div className={styles["container"]}>
          <div className={styles["hero-content"]}>
            {/* Tiêu đề lớn gây ấn tượng */}
            <h1 className={styles["title"]}>
              Tìm Kiếm Cơ Hội <br />
              <span>Việc Làm Tốt Nhất</span> Của Bạn
            </h1>
            <p className={styles["subtitle"]}>
              Hàng ngàn tin tuyển dụng IT chất lượng cao đang chờ đón bạn ứng
              tuyển.
            </p>

            {/* Thanh tìm kiếm nằm nổi bật ở giữa */}
            <div className={styles["search-box"]}>
              <SearchClient />
            </div>
          </div>
        </div>
      </div>

      {/* --- BODY CONTENT --- */}
      <div className={styles["container"]} style={{ paddingBottom: 60 }}>
        {/* Section: Top Công Ty (Giữ nguyên component cũ, chỉ thêm tiêu đề đẹp) */}
        <CompanyCard />

        <div style={{ margin: 80 }}></div>

        {/* Section: Việc làm mới nhất */}
        {/* JobCard đã có sẵn tiêu đề bên trong (theo code Bước 3) nên không cần Divider ở đây */}
        <JobCard />
      </div>
    </div>
  );
};

export default HomePage;
