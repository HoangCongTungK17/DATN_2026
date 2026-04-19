import { useEffect, useRef } from "react";
import { Outlet, useLocation } from "react-router-dom";
import Header from "components/client/header.client";
import Footer from "components/client/footer.client";
import styles from "styles/app.module.scss";

const LayoutClient = () => {
  const location = useLocation();
  const rootRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (rootRef?.current) {
      rootRef.current.scrollIntoView({ behavior: "smooth" });
    }
  }, [location]);

  return (
    <div className="layout-app" ref={rootRef}>
      <Header />
      <div className={styles["content-app"]}>
        <Outlet />
      </div>
      <Footer />
    </div>
  );
};

export default LayoutClient;
