// src/components/client/header.client.tsx

import { useState, useEffect } from "react";
import {
  CodeOutlined,
  ContactsOutlined,
  DashOutlined,
  LogoutOutlined,
  MenuFoldOutlined,
  RiseOutlined,
  FireOutlined, // Đổi icon sang ngọn lửa cho "Hot/Trendy"
  UserOutlined,
  HomeOutlined,
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
  theme, // Giữ lại theme nhưng style chủ yếu sẽ do CSS module xử lý
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
  // Không dùng colorBgContainer mặc định nữa để dùng Glassmorphism trong CSS

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

  const itemsDropdown = [
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
      icon: <ContactsOutlined />,
    },
    ...(user.role?.permissions?.length
      ? [
          {
            label: <Link to={"/admin"}>Trang Quản Trị</Link>,
            key: "admin",
            icon: <DashOutlined />,
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

  const itemsMobiles = [...items, ...itemsDropdown];

  return (
    <>
      <AntHeader className={styles["header-section"]}>
        <div className={styles["container"]}>
          {!isMobile ? (
            <div className={styles["header-desktop"]}>
              {/* Logo Area */}
              <div className={styles["brand"]} onClick={() => navigate("/")}>
                <FireOutlined className={styles["brand-icon"]} />
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
                    {" "}
                    {/* Giảm size space để CSS control tốt hơn */}
                    <Link to={"/login"} className={styles["login-link"]}>
                      Đăng Nhập
                    </Link>
                    <Button
                      type="primary"
                      // shape="round" -> Đã xử lý border-radius trong CSS
                      onClick={() => navigate("/register")}
                      className={styles["register-btn"]}
                    >
                      Đăng Ký Ngay
                    </Button>
                  </Space>
                ) : (
                  <Dropdown
                    menu={{ items: itemsDropdown }}
                    trigger={["click", "hover"]} // Thêm hover cho tiện
                    placement="bottomRight"
                    arrow // Thêm mũi tên chỉ vào avatar cho đẹp
                  >
                    <Space className={styles["user-dropdown"]}>
                      <Avatar
                        src={`${
                          import.meta.env.VITE_BACKEND_URL
                        }/images/avatar/${user?.name}`}
                        icon={<UserOutlined />}
                        className={styles["user-avatar"]}
                        size="large" // Tăng kích thước avatar
                      />
                      <span className={styles["user-name"]}>{user?.name}</span>
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
                <FireOutlined className={styles["brand-icon"]} />
                <span className={styles["brand-text"]}>JOBHUNT</span>
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
