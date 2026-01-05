import { Button, Form, Input, message, notification } from "antd";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { callLogin } from "@/config/api";
import { useState, useEffect } from "react";
import { useDispatch } from "react-redux";
import { setUserLoginInfo } from "@/redux/slice/accountSlide";
import styles from "@/styles/auth.module.scss";
import { useAppSelector } from "@/redux/hooks";
import { ArrowLeftOutlined } from "@ant-design/icons";

const LoginPage = () => {
  const navigate = useNavigate();
  const [isSubmit, setIsSubmit] = useState(false);
  const dispatch = useDispatch();
  const isAuthenticated = useAppSelector(
    (state) => state.account.isAuthenticated
  );

  let location = useLocation();
  let params = new URLSearchParams(location.search);
  const callback = params?.get("callback");

  useEffect(() => {
    //đã login => redirect to '/'
    if (isAuthenticated) {
      window.location.href = "/";
    }
  }, []);

  const onFinish = async (values: any) => {
    const { username, password } = values;
    setIsSubmit(true);
    const res = await callLogin(username, password);
    setIsSubmit(false);

    if (res?.data) {
      localStorage.setItem("access_token", res.data.access_token);
      dispatch(setUserLoginInfo(res.data.user));
      message.success("Đăng nhập tài khoản thành công!");

      // --- LOGIC ĐIỀU HƯỚNG CỦA BẠN (GIỮ NGUYÊN) ---
      const userRole = res.data.user?.role?.name;
      if (callback) {
        window.location.href = callback;
      } else {
        // Nếu là admin (khác NORMAL_USER/USER) thì về trang admin
        if (userRole !== "USER") {
          window.location.href = "/admin";
        } else {
          // User thường về trang chủ
          window.location.href = "/";
        }
      }
      // ----------------------------------------------
    } else {
      notification.error({
        message: "Có lỗi xảy ra",
        description:
          res.message && Array.isArray(res.message)
            ? res.message[0]
            : res.message,
        duration: 5,
      });
    }
  };

  return (
    <div className={styles["auth-container"]}>
      {/* 1. Sidebar bên trái (Hình ảnh/Thông điệp) */}
      <div className={styles["auth-sidebar"]}>
        <div className={styles["sidebar-content"]}>
          <h2>Chào mừng bạn trở lại!</h2>
          <p style={{ marginTop: 20, lineHeight: 1.6 }}>
            Kết nối với hàng ngàn cơ hội việc làm hấp dẫn <br />
            từ các công ty công nghệ hàng đầu.
          </p>
        </div>
      </div>

      {/* 2. Form bên phải */}
      <div className={styles["auth-form-container"]}>
        <div className={styles["auth-form-wrapper"]}>
          <div className={styles["auth-header"]}>
            <Link to="/" className={styles["brand"]}>
              <ArrowLeftOutlined style={{ marginRight: 5 }} />
              Quay lại trang chủ
            </Link>

            <h2 style={{ marginTop: 30 }}>Đăng Nhập</h2>
            <p>
              Bạn chưa có tài khoản? <Link to="/register">Đăng ký ngay</Link>
            </p>
          </div>

          <Form
            name="login-form"
            onFinish={onFinish}
            autoComplete="off"
            layout="vertical"
            size="large"
          >
            <Form.Item
              label="Email"
              name="username"
              rules={[
                { required: true, message: "Email không được để trống!" },
              ]}
            >
              <Input placeholder="name@example.com" />
            </Form.Item>

            <Form.Item
              label="Mật khẩu"
              name="password"
              rules={[
                { required: true, message: "Mật khẩu không được để trống!" },
              ]}
            >
              <Input.Password placeholder="Nhập mật khẩu" />
            </Form.Item>

            <Form.Item>
              <Button type="primary" htmlType="submit" loading={isSubmit} block>
                Đăng nhập
              </Button>
            </Form.Item>
          </Form>
        </div>
      </div>
    </div>
  );
};

export default LoginPage;
