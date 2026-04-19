import {
  Badge,
  Button,
  Col,
  Form,
  Input,
  InputNumber,
  Modal,
  Row,
  Select,
  Table,
  Tabs,
  Tag,
  message,
  notification,
} from "antd";
import { isMobile } from "react-device-detect";
import type { TabsProps } from "antd";
import { IResume, ISubscribers } from "@/types/backend";
import { useState, useEffect } from "react";
import {
  callCreateSubscriber,
  callFetchAllSkill,
  callFetchResumeByUser,
  callGetSubscriberSkills,
  callUpdateSubscriber,
} from "@/config/api";
import type { ColumnsType } from "antd/es/table";
import dayjs from "dayjs";
import {
  MonitorOutlined,
  FileTextOutlined,
  MailOutlined,
  UserOutlined,
  LockOutlined,
  SendOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  CloseCircleOutlined,
  SaveOutlined,
  BellOutlined,
  EditOutlined,
  SafetyCertificateOutlined,
  EyeOutlined,
} from "@ant-design/icons";
import { useAppSelector } from "@/redux/hooks";

interface IProps {
  open: boolean;
  onClose: (v: boolean) => void;
}

// =============================================
// TAB 1: Rải CV — Premium Table
// =============================================
const UserResume = (props: any) => {
  const [listCV, setListCV] = useState<IResume[]>([]);
  const [isFetching, setIsFetching] = useState<boolean>(false);

  useEffect(() => {
    const init = async () => {
      setIsFetching(true);
      const res = await callFetchResumeByUser();
      if (res && res.data) {
        setListCV(res.data.result as IResume[]);
      }
      setIsFetching(false);
    };
    init();
  }, []);

  const getStatusTag = (status: string) => {
    switch (status?.toUpperCase()) {
      case "PENDING":
        return (
          <Tag
            icon={<ClockCircleOutlined />}
            color="processing"
            style={tagStyle}
          >
            Đang chờ
          </Tag>
        );
      case "REVIEWING":
        return (
          <Tag icon={<EyeOutlined />} color="warning" style={tagStyle}>
            Đang xem
          </Tag>
        );
      case "APPROVED":
        return (
          <Tag
            icon={<CheckCircleOutlined />}
            color="success"
            style={tagStyle}
          >
            Đã duyệt
          </Tag>
        );
      case "REJECTED":
        return (
          <Tag
            icon={<CloseCircleOutlined />}
            color="error"
            style={tagStyle}
          >
            Từ chối
          </Tag>
        );
      default:
        return (
          <Tag icon={<ClockCircleOutlined />} color="default" style={tagStyle}>
            {status}
          </Tag>
        );
    }
  };

  const tagStyle: React.CSSProperties = {
    borderRadius: 20,
    padding: "2px 12px",
    fontWeight: 600,
    fontSize: 12,
  };

  const columns: ColumnsType<IResume> = [
    {
      title: "STT",
      key: "index",
      width: 55,
      align: "center",
      render: (text, record, index) => (
        <span
          style={{
            fontWeight: 700,
            color: "#64748b",
            fontSize: 13,
          }}
        >
          {index + 1}
        </span>
      ),
    },
    {
      title: "Công ty",
      dataIndex: "companyName",
      render: (text) => (
        <span style={{ fontWeight: 600, color: "#0f172a" }}>{text}</span>
      ),
    },
    {
      title: "Vị trí ứng tuyển",
      dataIndex: ["job", "name"],
      render: (text) => (
        <span style={{ color: "#334155" }}>{text}</span>
      ),
    },
    {
      title: "Trạng thái",
      dataIndex: "status",
      width: 140,
      align: "center",
      render: (status) => getStatusTag(status),
    },
    {
      title: "Ngày nộp",
      dataIndex: "createdAt",
      width: 160,
      render(value, record) {
        return (
          <span style={{ color: "#64748b", fontSize: 13 }}>
            {dayjs(record.createdAt).format("DD/MM/YYYY HH:mm")}
          </span>
        );
      },
    },
    {
      title: "",
      dataIndex: "",
      width: 100,
      align: "center",
      render(value, record) {
        return (
          <a
            href={`${import.meta.env.VITE_BACKEND_URL}/storage/resume/${record?.url}`}
            target="_blank"
            style={{
              color: "#2563eb",
              fontWeight: 600,
              fontSize: 13,
              display: "inline-flex",
              alignItems: "center",
              gap: 4,
              padding: "4px 12px",
              borderRadius: 6,
              background: "#eff6ff",
              transition: "all 0.2s",
            }}
          >
            <EyeOutlined /> Xem CV
          </a>
        );
      },
    },
  ];

  return (
    <div>
      {/* Header info */}
      <div
        style={{
          display: "flex",
          alignItems: "center",
          gap: 12,
          padding: "16px 20px",
          background: "linear-gradient(135deg, #eff6ff 0%, #f5f3ff 100%)",
          borderRadius: 12,
          marginBottom: 20,
        }}
      >
        <div
          style={{
            width: 42,
            height: 42,
            borderRadius: 10,
            background: "linear-gradient(135deg, #2563eb 0%, #7c3aed 100%)",
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            color: "#fff",
            fontSize: 18,
          }}
        >
          <FileTextOutlined />
        </div>
        <div>
          <div style={{ fontWeight: 700, fontSize: 15, color: "#0f172a" }}>
            Lịch sử ứng tuyển
          </div>
          <div style={{ color: "#64748b", fontSize: 13 }}>
            Theo dõi trạng thái các CV bạn đã nộp
          </div>
        </div>
        <Badge
          count={listCV.length}
          style={{
            backgroundColor: "#2563eb",
            marginLeft: "auto",
            fontWeight: 700,
          }}
        />
      </div>

      <Table<IResume>
        columns={columns}
        dataSource={listCV}
        loading={isFetching}
        pagination={false}
        rowKey={(record) => record.id || record.createdAt || ''}
        size="middle"
        style={{ borderRadius: 12, overflow: "hidden" }}
        locale={{ emptyText: "Bạn chưa nộp CV nào" }}
      />
    </div>
  );
};

// =============================================
// TAB 2: Nhận Jobs qua Email
// =============================================
const JobByEmail = (props: any) => {
  const [form] = Form.useForm();
  const user = useAppSelector((state) => state.account.user);
  const [optionsSkills, setOptionsSkills] = useState<
    { label: string; value: string }[]
  >([]);
  const [subscriber, setSubscriber] = useState<ISubscribers | null>(null);
  const [isSubmit, setIsSubmit] = useState(false);

  useEffect(() => {
    const init = async () => {
      await fetchSkill();
      const res = await callGetSubscriberSkills();
      if (res && res.data) {
        setSubscriber(res.data);
        const d = res.data.skills;
        // Only save IDs (values), not {label, value} objects
        const skillIds = d.map((item: any) => item.id + "");
        form.setFieldValue("skills", skillIds);
      }
    };
    init();
  }, []);

  const fetchSkill = async () => {
    let query = `page=1&size=100&sort=createdAt,desc`;
    const res = await callFetchAllSkill(query);
    if (res && res.data) {
      const arr =
        res?.data?.result?.map((item) => ({
          label: item.name as string,
          value: (item.id + "") as string,
        })) ?? [];
      setOptionsSkills(arr);
    }
  };

  const onFinish = async (values: any) => {
    const { skills } = values;
    setIsSubmit(true);

    // Convert string IDs to {id: number} format
    const arr = skills?.map((skillId: string) => ({ id: skillId }));

    if (!subscriber?.id) {
      //create subscriber
      const data = {
        email: user.email,
        name: user.name,
        skills: arr,
      };

      const res = await callCreateSubscriber(data);
      if (res.data) {
        message.success("Đăng ký nhận việc làm thành công!");
        setSubscriber(res.data);
      } else {
        notification.error({
          message: "Có lỗi xảy ra",
          description: res.message,
        });
      }
    } else {
      //update subscriber
      const res = await callUpdateSubscriber({
        id: subscriber?.id,
        skills: arr,
      });
      if (res.data) {
        message.success("Cập nhật kỹ năng thành công!");
        setSubscriber(res.data);
      } else {
        notification.error({
          message: "Có lỗi xảy ra",
          description: res.message,
        });
      }
    }
    setIsSubmit(false);
  };

  return (
    <div style={{ padding: "4px 0" }}>
      {/* Header */}
      <div
        style={{
          display: "flex",
          alignItems: "center",
          gap: 12,
          padding: "16px 20px",
          background: "linear-gradient(135deg, #fef3c7 0%, #fff7ed 100%)",
          borderRadius: 12,
          marginBottom: 24,
        }}
      >
        <div
          style={{
            width: 42,
            height: 42,
            borderRadius: 10,
            background: "linear-gradient(135deg, #f59e0b 0%, #ef4444 100%)",
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            color: "#fff",
            fontSize: 18,
          }}
        >
          <BellOutlined />
        </div>
        <div>
          <div style={{ fontWeight: 700, fontSize: 15, color: "#0f172a" }}>
            Nhận thông báo việc làm
          </div>
          <div style={{ color: "#64748b", fontSize: 13 }}>
            Chọn kỹ năng để nhận email khi có việc phù hợp
          </div>
        </div>
      </div>

      <Form onFinish={onFinish} form={form} layout="vertical">
        <Form.Item
          label={
            <span style={{ fontWeight: 600, fontSize: 14, color: "#0f172a" }}>
              <MonitorOutlined style={{ marginRight: 6, color: "#2563eb" }} />
              Kỹ năng quan tâm
            </span>
          }
          name={"skills"}
          rules={[
            {
              required: true,
              message: "Vui lòng chọn ít nhất 1 kỹ năng!",
            },
          ]}
        >
          <Select
            mode="multiple"
            allowClear
            showSearch
            style={{ width: "100%" }}
            placeholder="Chọn kỹ năng bạn quan tâm..."
            optionLabelProp="label"
            options={optionsSkills}
            size="large"
          />
        </Form.Item>

        {/* Email info */}
        <div
          style={{
            padding: "12px 16px",
            background: "#f8fafc",
            borderRadius: 8,
            border: "1px solid #e2e8f0",
            marginBottom: 20,
            display: "flex",
            alignItems: "center",
            gap: 8,
          }}
        >
          <MailOutlined style={{ color: "#2563eb", fontSize: 16 }} />
          <span style={{ color: "#64748b", fontSize: 13 }}>
            Thông báo sẽ được gửi đến:{" "}
            <strong style={{ color: "#0f172a" }}>{user?.email}</strong>
          </span>
        </div>

        <Button
          type="primary"
          onClick={() => form.submit()}
          loading={isSubmit}
          icon={<SendOutlined />}
          size="large"
          style={{
            borderRadius: 10,
            height: 44,
            fontWeight: 700,
            background: "linear-gradient(135deg, #2563eb 0%, #7c3aed 100%)",
            border: "none",
            boxShadow: "0 4px 14px rgba(37, 99, 235, 0.3)",
          }}
        >
          {subscriber?.id ? "Cập nhật kỹ năng" : "Đăng ký nhận việc"}
        </Button>
      </Form>
    </div>
  );
};

// =============================================
// TAB 3: Cập nhật thông tin
// =============================================
const UserUpdateInfo = (props: any) => {
  const [form] = Form.useForm();
  const user = useAppSelector((state) => state.account.user);
  const [isSubmit, setIsSubmit] = useState<boolean>(false);

  useEffect(() => {
    if (user) {
      form.setFieldsValue({
        name: user.name,
        age: (user as any).age || "",
        gender: (user as any).gender || "MALE",
        address: (user as any).address || "",
      });
    }
  }, [user]);

  const onFinish = async (values: any) => {
    setIsSubmit(true);
    const { callUpdateProfile } = await import("@/config/api");

    const res = await callUpdateProfile({
      name: values.name,
      age: values.age,
      gender: values.gender,
      address: values.address,
    });

    if (res.data) {
      message.success("Cập nhật thông tin thành công!");
    } else {
      notification.error({
        message: "Có lỗi xảy ra",
        description: res.message,
      });
    }
    setIsSubmit(false);
  };

  return (
    <div style={{ padding: "4px 0" }}>
      {/* Header */}
      <div
        style={{
          display: "flex",
          alignItems: "center",
          gap: 12,
          padding: "16px 20px",
          background: "linear-gradient(135deg, #ecfdf5 0%, #eff6ff 100%)",
          borderRadius: 12,
          marginBottom: 24,
        }}
      >
        <div
          style={{
            width: 42,
            height: 42,
            borderRadius: 10,
            background: "linear-gradient(135deg, #059669 0%, #0ea5e9 100%)",
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            color: "#fff",
            fontSize: 18,
          }}
        >
          <EditOutlined />
        </div>
        <div>
          <div style={{ fontWeight: 700, fontSize: 15, color: "#0f172a" }}>
            Thông tin cá nhân
          </div>
          <div style={{ color: "#64748b", fontSize: 13 }}>
            Cập nhật thông tin hồ sơ của bạn
          </div>
        </div>
      </div>

      <Form form={form} onFinish={onFinish} layout="vertical">
        <Row gutter={20}>
          <Col span={12}>
            <Form.Item
              label={
                <span style={{ fontWeight: 600, color: "#0f172a" }}>
                  Họ và tên
                </span>
              }
              name="name"
              rules={[
                { required: true, message: "Vui lòng nhập họ tên!" },
              ]}
            >
              <Input
                prefix={<UserOutlined style={{ color: "#94a3b8" }} />}
                size="large"
                placeholder="Nhập họ và tên"
                style={{ borderRadius: 10 }}
              />
            </Form.Item>
          </Col>
          <Col span={12}>
            <Form.Item
              label={
                <span style={{ fontWeight: 600, color: "#0f172a" }}>Tuổi</span>
              }
              name="age"
              rules={[{ required: true, message: "Vui lòng nhập tuổi!" }]}
            >
              <InputNumber
                style={{ width: "100%", borderRadius: 10 }}
                size="large"
                min={1}
                max={150}
                placeholder="Nhập tuổi"
              />
            </Form.Item>
          </Col>
        </Row>

        <Row gutter={20}>
          <Col span={12}>
            <Form.Item
              label={
                <span style={{ fontWeight: 600, color: "#0f172a" }}>
                  Giới tính
                </span>
              }
              name="gender"
              rules={[
                { required: true, message: "Vui lòng chọn giới tính!" },
              ]}
            >
              <Select size="large" style={{ borderRadius: 10 }}>
                <Select.Option value="MALE">Nam</Select.Option>
                <Select.Option value="FEMALE">Nữ</Select.Option>
                <Select.Option value="OTHER">Khác</Select.Option>
              </Select>
            </Form.Item>
          </Col>
          <Col span={12}>
            <Form.Item
              label={
                <span style={{ fontWeight: 600, color: "#0f172a" }}>
                  Địa chỉ
                </span>
              }
              name="address"
              rules={[
                { required: true, message: "Vui lòng nhập địa chỉ!" },
              ]}
            >
              <Input
                size="large"
                placeholder="Nhập địa chỉ"
                style={{ borderRadius: 10 }}
              />
            </Form.Item>
          </Col>
        </Row>

        <Form.Item style={{ marginBottom: 0 }}>
          <Button
            type="primary"
            htmlType="submit"
            loading={isSubmit}
            icon={<SaveOutlined />}
            size="large"
            style={{
              borderRadius: 10,
              height: 44,
              fontWeight: 700,
              background: "linear-gradient(135deg, #059669 0%, #0ea5e9 100%)",
              border: "none",
              boxShadow: "0 4px 14px rgba(5, 150, 105, 0.3)",
            }}
          >
            Lưu thay đổi
          </Button>
        </Form.Item>
      </Form>
    </div>
  );
};

// =============================================
// TAB 4: Đổi mật khẩu
// =============================================
const ChangePasswordUser = (props: any) => {
  const [form] = Form.useForm();
  const [isSubmit, setIsSubmit] = useState<boolean>(false);

  const onFinish = async (values: any) => {
    const { currentPassword, newPassword, confirmPassword } = values;

    if (newPassword !== confirmPassword) {
      notification.error({
        message: "Lỗi",
        description: "Xác nhận mật khẩu không khớp!",
      });
      return;
    }

    setIsSubmit(true);
    const { callChangePassword } = await import("@/config/api");

    const res = await callChangePassword(currentPassword, newPassword);

    if (res.data) {
      message.success("Đổi mật khẩu thành công!");
      form.resetFields();
    } else {
      notification.error({
        message: "Có lỗi xảy ra",
        description: res.message,
      });
    }
    setIsSubmit(false);
  };

  return (
    <div style={{ padding: "4px 0", maxWidth: 520 }}>
      {/* Header */}
      <div
        style={{
          display: "flex",
          alignItems: "center",
          gap: 12,
          padding: "16px 20px",
          background: "linear-gradient(135deg, #fee2e2 0%, #fef3c7 100%)",
          borderRadius: 12,
          marginBottom: 24,
        }}
      >
        <div
          style={{
            width: 42,
            height: 42,
            borderRadius: 10,
            background: "linear-gradient(135deg, #dc2626 0%, #f59e0b 100%)",
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            color: "#fff",
            fontSize: 18,
          }}
        >
          <SafetyCertificateOutlined />
        </div>
        <div>
          <div style={{ fontWeight: 700, fontSize: 15, color: "#0f172a" }}>
            Bảo mật tài khoản
          </div>
          <div style={{ color: "#64748b", fontSize: 13 }}>
            Thay đổi mật khẩu để bảo vệ tài khoản
          </div>
        </div>
      </div>

      <Form form={form} onFinish={onFinish} layout="vertical">
        <Form.Item
          label={
            <span style={{ fontWeight: 600, color: "#0f172a" }}>
              Mật khẩu hiện tại
            </span>
          }
          name="currentPassword"
          rules={[
            {
              required: true,
              message: "Vui lòng nhập mật khẩu hiện tại!",
            },
          ]}
        >
          <Input.Password
            prefix={<LockOutlined style={{ color: "#94a3b8" }} />}
            size="large"
            placeholder="Nhập mật khẩu hiện tại"
            style={{ borderRadius: 10 }}
          />
        </Form.Item>

        <Form.Item
          label={
            <span style={{ fontWeight: 600, color: "#0f172a" }}>
              Mật khẩu mới
            </span>
          }
          name="newPassword"
          rules={[
            { required: true, message: "Vui lòng nhập mật khẩu mới!" },
            { min: 6, message: "Mật khẩu phải có ít nhất 6 ký tự!" },
          ]}
        >
          <Input.Password
            prefix={<LockOutlined style={{ color: "#94a3b8" }} />}
            size="large"
            placeholder="Nhập mật khẩu mới (tối thiểu 6 ký tự)"
            style={{ borderRadius: 10 }}
          />
        </Form.Item>

        <Form.Item
          label={
            <span style={{ fontWeight: 600, color: "#0f172a" }}>
              Xác nhận mật khẩu mới
            </span>
          }
          name="confirmPassword"
          rules={[
            {
              required: true,
              message: "Vui lòng xác nhận mật khẩu mới!",
            },
          ]}
        >
          <Input.Password
            prefix={<LockOutlined style={{ color: "#94a3b8" }} />}
            size="large"
            placeholder="Nhập lại mật khẩu mới"
            style={{ borderRadius: 10 }}
          />
        </Form.Item>

        <Form.Item style={{ marginBottom: 0 }}>
          <Button
            type="primary"
            htmlType="submit"
            loading={isSubmit}
            icon={<SafetyCertificateOutlined />}
            size="large"
            danger
            style={{
              borderRadius: 10,
              height: 44,
              fontWeight: 700,
              boxShadow: "0 4px 14px rgba(220, 38, 38, 0.25)",
            }}
          >
            Đổi mật khẩu
          </Button>
        </Form.Item>
      </Form>
    </div>
  );
};

// =============================================
// MAIN COMPONENT: ManageAccount
// =============================================
const ManageAccount = (props: IProps) => {
  const { open, onClose } = props;

  const onChange = (key: string) => {
    // console.log(key);
  };

  const items: TabsProps["items"] = [
    {
      key: "user-resume",
      label: (
        <span style={{ display: "flex", alignItems: "center", gap: 6 }}>
          <FileTextOutlined />
          Rải CV
        </span>
      ),
      children: <UserResume />,
    },
    {
      key: "email-by-skills",
      label: (
        <span style={{ display: "flex", alignItems: "center", gap: 6 }}>
          <MailOutlined />
          Nhận Jobs qua Email
        </span>
      ),
      children: <JobByEmail />,
    },
    {
      key: "user-update-info",
      label: (
        <span style={{ display: "flex", alignItems: "center", gap: 6 }}>
          <UserOutlined />
          Cập nhật thông tin
        </span>
      ),
      children: <UserUpdateInfo />,
    },
    {
      key: "user-password",
      label: (
        <span style={{ display: "flex", alignItems: "center", gap: 6 }}>
          <LockOutlined />
          Đổi mật khẩu
        </span>
      ),
      children: <ChangePasswordUser />,
    },
  ];

  return (
    <>
      <Modal
        title={
          <div
            style={{
              display: "flex",
              alignItems: "center",
              gap: 10,
            }}
          >
            <div
              style={{
                width: 36,
                height: 36,
                borderRadius: 10,
                background:
                  "linear-gradient(135deg, #2563eb 0%, #7c3aed 100%)",
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                color: "#fff",
                fontSize: 16,
              }}
            >
              <UserOutlined />
            </div>
            <div>
              <div
                style={{
                  fontWeight: 700,
                  fontSize: 17,
                  color: "#0f172a",
                  lineHeight: 1.3,
                }}
              >
                Quản lý tài khoản
              </div>
              <div
                style={{
                  fontSize: 12,
                  color: "#94a3b8",
                  fontWeight: 400,
                }}
              >
                Quản lý hồ sơ, thông báo và bảo mật
              </div>
            </div>
          </div>
        }
        open={open}
        onCancel={() => onClose(false)}
        maskClosable={false}
        footer={null}
        destroyOnClose={true}
        width={isMobile ? "100%" : "1000px"}
        styles={{
          header: {
            borderBottom: "1px solid #f1f5f9",
            paddingBottom: 16,
          },
          body: {
            padding: "0 24px 24px",
          },
        }}
      >
        <div style={{ minHeight: 400 }}>
          <Tabs
            defaultActiveKey="user-resume"
            items={items}
            onChange={onChange}
            tabBarStyle={{
              fontWeight: 600,
              marginBottom: 20,
            }}
          />
        </div>
      </Modal>
    </>
  );
};

export default ManageAccount;