import {
  Button,
  Form,
  Input,
  Row,
  Col,
  Select,
  message,
  notification,
} from "antd";
import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { callRegister } from "@/config/api";
import styles from "@/styles/auth.module.scss";
import { IUser } from "@/types/backend";
import { ArrowLeftOutlined } from "@ant-design/icons";

const { Option } = Select;

const RegisterPage = () => {
  const navigate = useNavigate();
  const [isSubmit, setIsSubmit] = useState(false);

  const onFinish = async (values: IUser) => {
    const { name, email, password, age, gender, address } = values;
    setIsSubmit(true);
    const res = await callRegister(
      name,
      email,
      password as string,
      +age,
      gender,
      address
    );
    setIsSubmit(false);
    if (res?.data?.id) {
      message.success("Đăng ký tài khoản thành công!");
      navigate("/login");
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
      {/* 1. Sidebar bên trái */}
      <div className={styles["auth-sidebar"]}>
        <div className={styles["sidebar-content"]}>
          <h2>Bắt đầu sự nghiệp của bạn</h2>
          <p style={{ marginTop: 20, lineHeight: 1.6 }}>
            Tạo tài khoản miễn phí để tiếp cận hàng ngàn công việc IT <br />
            chất lượng cao và phát triển sự nghiệp.
          </p>
        </div>
      </div>

      {/* 2. Form bên phải */}
      <div className={styles["auth-form-container"]}>
        <div className={styles["auth-form-wrapper"]}>
          <div className={styles["auth-header"]}>
            <Link to="/" className={styles["brand"]}>
              <ArrowLeftOutlined style={{ marginRight: 5 }} />
              Trang chủ
            </Link>
            <h2 style={{ marginTop: 30 }}>Đăng Ký Tài Khoản</h2>
            <p>
              Đã có tài khoản? <Link to="/login">Đăng nhập</Link>
            </p>
          </div>

          <Form<IUser>
            name="register-form"
            onFinish={onFinish}
            autoComplete="off"
            layout="vertical"
            size="large"
          >
            <Form.Item
              label="Họ tên"
              name="name"
              rules={[
                { required: true, message: "Họ tên không được để trống!" },
              ]}
            >
              <Input placeholder="Ví dụ: Nguyễn Văn A" />
            </Form.Item>

            <Form.Item
              label="Email"
              name="email"
              rules={[
                { required: true, message: "Email không được để trống!" },
              ]}
            >
              <Input type="email" placeholder="name@example.com" />
            </Form.Item>

            <Form.Item
              label="Mật khẩu"
              name="password"
              rules={[
                { required: true, message: "Mật khẩu không được để trống!" },
              ]}
            >
              <Input.Password placeholder="Tạo mật khẩu mạnh" />
            </Form.Item>

            <Row gutter={16}>
              <Col span={12}>
                <Form.Item
                  label="Tuổi"
                  name="age"
                  rules={[{ required: true, message: "Nhập tuổi!" }]}
                >
                  <Input type="number" placeholder="22" />
                </Form.Item>
              </Col>
              <Col span={12}>
                <Form.Item
                  name="gender"
                  label="Giới tính"
                  rules={[{ required: true, message: "Chọn giới tính!" }]}
                >
                  <Select placeholder="Chọn" allowClear>
                    <Option value="MALE">Nam</Option>
                    <Option value="FEMALE">Nữ</Option>
                    <Option value="OTHER">Khác</Option>
                  </Select>
                </Form.Item>
              </Col>
            </Row>

            <Form.Item
              label="Địa chỉ"
              name="address"
              rules={[
                { required: true, message: "Địa chỉ không được để trống!" },
              ]}
            >
              <Input placeholder="Hà Nội, Việt Nam" />
            </Form.Item>

            <Form.Item>
              <Button type="primary" htmlType="submit" loading={isSubmit} block>
                Đăng ký ngay
              </Button>
            </Form.Item>
          </Form>
        </div>
      </div>
    </div>
  );
};

export default RegisterPage;
