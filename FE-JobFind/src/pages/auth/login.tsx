import { Button, Form, Input, message, notification } from "antd";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { callLogin, callLoginGoogle } from "@/config/api";
import { useState, useEffect, useRef } from "react";
import { useDispatch } from "react-redux";
import { setUserLoginInfo } from "@/redux/slice/accountSlide";
import styles from "@/styles/auth.module.scss";
import { useAppSelector } from "@/redux/hooks";
import { ArrowLeftOutlined } from "@ant-design/icons";
import loginBg from "@/assets/login.png";

declare global {
  interface Window {
    google?: {
      accounts: {
        id: {
          initialize: (options: {
            client_id: string;
            callback: (response: { credential?: string }) => void;
          }) => void;
          renderButton: (
            parent: HTMLElement,
            options: Record<string, string | number | boolean>
          ) => void;
        };
      };
    };
  }
}

const LoginPage = () => {
  const navigate = useNavigate();
  const [isSubmit, setIsSubmit] = useState(false);
  const [isGoogleSubmit, setIsGoogleSubmit] = useState(false);
  const googleButtonRef = useRef<HTMLDivElement | null>(null);
  const dispatch = useDispatch();
  const isAuthenticated = useAppSelector(
    (state) => state.account.isAuthenticated
  );

  let location = useLocation();
  let params = new URLSearchParams(location.search);
  const callback = params?.get("callback");

  const redirectAfterLogin = (userRole?: string) => {
    if (callback) {
      window.location.href = callback;
      return;
    }

    window.location.href = userRole !== "USER" ? "/admin" : "/";
  };

  const handleLoginResult = (res: any, successMessage: string) => {
    if (res?.data) {
      localStorage.setItem("access_token", res.data.access_token);
      dispatch(setUserLoginInfo(res.data.user));
      message.success(successMessage);
      redirectAfterLogin(res.data.user?.role?.name);
      return;
    }

    notification.error({
      message: "Có lỗi xảy ra",
      description:
        res?.message && Array.isArray(res.message)
          ? res.message[0]
          : res?.message,
      duration: 5,
    });
  };

  useEffect(() => {
    //đã login => redirect to '/'
    if (isAuthenticated) {
      window.location.href = "/";
    }
  }, []);

  useEffect(() => {
    const clientId = import.meta.env.VITE_GOOGLE_CLIENT_ID;
    if (!clientId || !googleButtonRef.current) return;

    let cancelled = false;
    const scriptId = "google-identity-services";

    const initializeGoogleButton = () => {
      if (cancelled || !window.google || !googleButtonRef.current) return;

      window.google.accounts.id.initialize({
        client_id: clientId,
        callback: async (response) => {
          if (!response.credential) {
            notification.error({ message: "Không nhận được Google credential" });
            return;
          }

          setIsGoogleSubmit(true);
          try {
            const res = await callLoginGoogle(response.credential);
            handleLoginResult(res, "Đăng nhập Google thành công!");
          } catch {
            notification.error({ message: "Đăng nhập Google thất bại" });
          } finally {
            setIsGoogleSubmit(false);
          }
        },
      });

      googleButtonRef.current.innerHTML = "";
      window.google.accounts.id.renderButton(googleButtonRef.current, {
        type: "standard",
        theme: "outline",
        size: "large",
        text: "signin_with",
        shape: "rectangular",
        logo_alignment: "left",
        locale: "vi",
        width: googleButtonRef.current.offsetWidth || 360,
      });
    };

    const existingScript = document.getElementById(scriptId);
    if (window.google) {
      initializeGoogleButton();
      return;
    }

    if (existingScript) {
      existingScript.addEventListener("load", initializeGoogleButton);
      return () => {
        cancelled = true;
        existingScript.removeEventListener("load", initializeGoogleButton);
      };
    }

    const script = document.createElement("script");
    script.id = scriptId;
    script.src = "https://accounts.google.com/gsi/client";
    script.async = true;
    script.defer = true;
    script.onload = initializeGoogleButton;
    document.body.appendChild(script);

    return () => {
      cancelled = true;
      script.onload = null;
    };
  }, [callback]);

  const onFinish = async (values: any) => {
    const { username, password } = values;
    setIsSubmit(true);
    const res = await callLogin(username, password);
    setIsSubmit(false);

    handleLoginResult(res, "Đăng nhập tài khoản thành công!");
  };

  return (
    <div className={styles["auth-container"]}>
      {/* 1. Sidebar bên trái (Hình ảnh) */}
      <div className={styles["auth-sidebar"]}>
        <img src={loginBg} alt="JobFind Platform" className={styles["sidebar-image"]} />
      </div>

      {/* 2. Form bên phải */}
      <div className={styles["auth-form-container"]}>
        <div className={styles["auth-form-wrapper"]}>
          <div className={styles["auth-header"]}>
            <Link to="/" className={styles["brand"]}>
              <ArrowLeftOutlined style={{ marginRight: 5 }} />
              Quay lại trang chủ
            </Link>

            <h2>Đăng Nhập</h2>
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

          <div className={styles["auth-divider"]}>
            <span>Hoặc</span>
          </div>
          <div
            className={styles["google-login-wrapper"]}
            aria-busy={isGoogleSubmit}
          >
            <div ref={googleButtonRef} className={styles["google-login-button"]} />
          </div>
        </div>
      </div>
    </div>
  );
};

export default LoginPage;
