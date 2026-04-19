import { useState, useEffect } from "react";
import {
  CodeOutlined,
  LogoutOutlined,
  MenuFoldOutlined,
  RiseOutlined,
  ThunderboltFilled,
  UserOutlined,
  HomeOutlined,
  RobotOutlined,
  SettingOutlined,
  CrownOutlined,
  CaretDownOutlined,
} from "@ant-design/icons";
import {
  Avatar,
  Drawer,
  Dropdown,
  MenuProps,
  Space,
  message,
  Menu,
  Button,
  Layout,
} from "antd";
import styles from "@/styles/client.module.scss";
import { isMobile } from "react-device-detect";
import { useLocation, useNavigate, Link } from "react-router-dom";
import { useAppDispatch, useAppSelector } from "@/redux/hooks";
import { callLogout } from "@/config/api";
import { setLogoutAction } from "@/redux/slice/accountSlide";
import ManageAccount from "./modal/manage.account";

const { Header: AntHeader } = Layout;

const Header = (props: any) => {
  const navigate = useNavigate();
  const dispatch = useAppDispatch();

  const isAuthenticated = useAppSelector(
    (state) => state.account.isAuthenticated
  );
  const user = useAppSelector((state) => state.account.user);
  const [openMobileMenu, setOpenMobileMenu] = useState<boolean>(false);

  const [current, setCurrent] = useState("home");
  const location = useLocation();

  const [openMangeAccount, setOpenManageAccount] = useState<boolean>(false);

  useEffect(() => {
    setCurrent(location.pathname);
  }, [location]);

  const items: MenuProps["items"] = [
    {
      label: <Link to={"/"}>Trang Chủ</Link>,
      key: "/",
      icon: <HomeOutlined />,
    },
    {
      label: <Link to={"/job"}>Việc Làm IT</Link>,
      key: "/job",
      icon: <CodeOutlined />,
    },
    {
      label: <Link to={"/company"}>Top Công ty</Link>,
      key: "/company",
      icon: <RiseOutlined />,
    },
    {
      label: <Link to={"/ai-hub"}>AI Hub</Link>,
      key: "/ai-hub",
      icon: <RobotOutlined />,
    },
  ];

  const onClick: MenuProps["onClick"] = (e) => {
    setCurrent(e.key);
  };

  const handleLogout = async () => {
    const res = await callLogout();
    if (res && +res.statusCode === 200) {
      dispatch(setLogoutAction({}));
      message.success("Đăng xuất thành công");
      navigate("/");
    }
  };

  // Custom Dropdown Overlay — Premium Design
  const dropdownContent = (
    <div className={styles["dropdown-premium"]}>
      {/* User Info Header */}
      <div className={styles["dropdown-user-header"]}>
        <Avatar
          src={`${import.meta.env.VITE_BACKEND_URL}/images/avatar/${user?.name}`}
          icon={<UserOutlined />}
          size={44}
          style={{
            background: "linear-gradient(135deg, #2563eb, #7c3aed)",
            border: "2px solid #e0e7ff",
          }}
        />
        <div className={styles["dropdown-user-info"]}>
          <div className={styles["dropdown-user-name"]}>{user?.name}</div>
          <div className={styles["dropdown-user-email"]}>{user?.email}</div>
        </div>
      </div>

      {/* Divider */}
      <div className={styles["dropdown-divider"]} />

      {/* Menu Items */}
      <div className={styles["dropdown-menu-items"]}>
        <div
          className={styles["dropdown-menu-item"]}
          onClick={() => setOpenManageAccount(true)}
        >
          <div className={styles["dropdown-item-icon"]} style={{ background: "#eff6ff", color: "#2563eb" }}>
            <SettingOutlined />
          </div>
          <div className={styles["dropdown-item-content"]}>
            <span className={styles["dropdown-item-label"]}>Quản lý tài khoản</span>
            <span className={styles["dropdown-item-desc"]}>CV, thông tin, bảo mật</span>
          </div>
        </div>

        {user.role?.permissions?.length ? (
          <div
            className={styles["dropdown-menu-item"]}
            onClick={() => navigate("/admin")}
          >
            <div className={styles["dropdown-item-icon"]} style={{ background: "#f5f3ff", color: "#7c3aed" }}>
              <CrownOutlined />
            </div>
            <div className={styles["dropdown-item-content"]}>
              <span className={styles["dropdown-item-label"]}>Trang Quản Trị</span>
              <span className={styles["dropdown-item-desc"]}>Dashboard Admin</span>
            </div>
          </div>
        ) : null}
      </div>

      {/* Divider */}
      <div className={styles["dropdown-divider"]} />

      {/* Logout */}
      <div className={styles["dropdown-menu-items"]}>
        <div
          className={`${styles["dropdown-menu-item"]} ${styles["dropdown-item-danger"]}`}
          onClick={() => handleLogout()}
        >
          <div className={styles["dropdown-item-icon"]} style={{ background: "#fee2e2", color: "#dc2626" }}>
            <LogoutOutlined />
          </div>
          <div className={styles["dropdown-item-content"]}>
            <span className={styles["dropdown-item-label"]}>Đăng xuất</span>
          </div>
        </div>
      </div>
    </div>
  );

  const itemsMobiles = [
    ...items,
    {
      label: (
        <label
          style={{ cursor: "pointer" }}
          onClick={() => setOpenManageAccount(true)}
        >
          Quản lý tài khoản
        </label>
      ),
      key: "manage-account",
      icon: <SettingOutlined />,
    },
    ...(user.role?.permissions?.length
      ? [
          {
            label: <Link to={"/admin"}>Trang Quản Trị</Link>,
            key: "admin",
            icon: <CrownOutlined />,
          },
        ]
      : []),
    {
      label: (
        <label style={{ cursor: "pointer" }} onClick={() => handleLogout()}>
          Đăng xuất
        </label>
      ),
      key: "logout",
      icon: <LogoutOutlined />,
    },
  ];

  return (
    <>
      <AntHeader className={styles["header-section"]}>
        <div className={styles["container"]}>
          {!isMobile ? (
            <div className={styles["header-desktop"]}>
              {/* Logo Area */}
              <div className={styles["brand"]} onClick={() => navigate("/")}>
                <ThunderboltFilled className={styles["brand-icon"]} />
                <span className={styles["brand-text"]}>
                  JOB<span className={styles["brand-highlight"]}>FIND</span>
                </span>
              </div>

              {/* Navigation Menu */}
              <div className={styles["top-menu"]}>
                <Menu
                  onClick={onClick}
                  selectedKeys={[current]}
                  mode="horizontal"
                  items={items}
                  className={styles["menu-custom"]}
                  disabledOverflow
                />
              </div>

              {/* User Actions */}
              <div className={styles["extra"]}>
                {isAuthenticated === false ? (
                  <Space size="small">
                    <Link to={"/login"} className={styles["login-link"]}>
                      Đăng Nhập
                    </Link>
                    <Button
                      type="primary"
                      onClick={() => navigate("/register")}
                      className={styles["register-btn"]}
                    >
                      Đăng Ký Ngay
                    </Button>
                  </Space>
                ) : (
                  <Dropdown
                    dropdownRender={() => dropdownContent}
                    trigger={["click"]}
                    placement="bottomRight"
                  >
                    <Space className={styles["user-dropdown"]}>
                      <Avatar
                        src={`${
                          import.meta.env.VITE_BACKEND_URL
                        }/images/avatar/${user?.name}`}
                        icon={<UserOutlined />}
                        className={styles["user-avatar"]}
                        size="large"
                      />
                      <span className={styles["user-name"]}>{user?.name}</span>
                      <CaretDownOutlined
                        style={{ fontSize: 10, color: "#94a3b8", marginLeft: -4 }}
                      />
                    </Space>
                  </Dropdown>
                )}
              </div>
            </div>
          ) : (
            <div className={styles["header-mobile"]}>
              <div
                className={styles["brand-mobile"]}
                onClick={() => navigate("/")}
              >
                <ThunderboltFilled className={styles["brand-icon"]} />
                <span className={styles["brand-text"]}>JOBFIND</span>
              </div>
              <MenuFoldOutlined
                className={styles["menu-trigger"]}
                onClick={() => setOpenMobileMenu(true)}
              />
            </div>
          )}
        </div>
      </AntHeader>

      <Drawer
        title="Menu Chức năng"
        placement="right"
        onClose={() => setOpenMobileMenu(false)}
        open={openMobileMenu}
        width={280}
      >
        <Menu
          onClick={onClick}
          selectedKeys={[current]}
          mode="inline"
          items={itemsMobiles}
          style={{ border: "none" }}
        />
      </Drawer>

      <ManageAccount open={openMangeAccount} onClose={setOpenManageAccount} />
    </>
  );
};

export default Header;
