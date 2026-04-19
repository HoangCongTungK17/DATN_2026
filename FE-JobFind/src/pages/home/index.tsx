import styles from "@/styles/client.module.scss";
import SearchClient from "@/components/client/search.client";
import JobCard from "@/components/client/card/job.card";
import CompanyCard from "@/components/client/card/company.card";

const HomePage = () => {
  return (
    <div className={styles["homepage-container"]}>
      {/* --- HERO SECTION --- */}
      <div className={styles["hero-section"]}>
        <div className={styles["container"]}>
          <div className={styles["hero-content"]}>
            <h1 className={styles["title"]}>
              Khám Phá Cơ Hội <br />
              <span>Sự Nghiệp IT</span> Dành Cho Bạn
            </h1>
            <p className={styles["subtitle"]}>
              Hàng ngàn vị trí từ các công ty công nghệ hàng đầu — tìm kiếm,
              ứng tuyển và bứt phá sự nghiệp ngay hôm nay.
            </p>

            {/* Thanh tìm kiếm */}
            <div className={styles["search-box"]}>
              <SearchClient />
            </div>
          </div>
        </div>
      </div>

      {/* --- BODY CONTENT --- */}
      <div className={styles["container"]} style={{ paddingBottom: 80, paddingTop: 40 }}>
        {/* Section: Top Công Ty */}
        <CompanyCard />

        <div style={{ margin: 64 }}></div>

        {/* Section: Việc làm mới nhất */}
        <JobCard />
      </div>
    </div>
  );
};

export default HomePage;
