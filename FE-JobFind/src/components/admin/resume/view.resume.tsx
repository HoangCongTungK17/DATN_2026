import { callUpdateResumeStatus } from "@/config/api";
import { IResume } from "@/types/backend";
import {
  Badge,
  Button,
  Descriptions,
  Drawer,
  Form,
  Select,
  Space,
  message,
  notification,
} from "antd";
import { ThunderboltOutlined } from "@ant-design/icons";
import dayjs from "dayjs";
import { useState, useEffect } from "react";
import SmartMatchModal from "./smart-match.modal";
const { Option } = Select;

interface IProps {
  onClose: (v: boolean) => void;
  open: boolean;
  dataInit: IResume | null | any;
  setDataInit: (v: any) => void;
  reloadTable: () => void;
}
const ViewDetailResume = (props: IProps) => {
  const [isSubmit, setIsSubmit] = useState<boolean>(false);
  const [openSmartMatch, setOpenSmartMatch] = useState<boolean>(false);
  const { onClose, open, dataInit, setDataInit, reloadTable } = props;
  const [form] = Form.useForm();

  const handleChangeStatus = async () => {
    setIsSubmit(true);

    const status = form.getFieldValue("status");
    const res = await callUpdateResumeStatus(dataInit?.id, status);
    if (res.data) {
      message.success("Update Resume status thành công!");
      setDataInit(null);
      onClose(false);
      reloadTable();
    } else {
      notification.error({
        message: "Có lỗi xảy ra",
        description: res.message,
      });
    }

    setIsSubmit(false);
  };

  useEffect(() => {
    if (dataInit) {
      form.setFieldValue("status", dataInit.status);
    }
    return () => form.resetFields();
  }, [dataInit]);

  return (
    <>
      <Drawer
        title="Thông Tin Resume"
        placement="right"
        onClose={() => {
          onClose(false);
          setDataInit(null);
        }}
        open={open}
        width={"40vw"}
        maskClosable={false}
        destroyOnClose
        extra={
          <Space>
            <Button
              type="default"
              icon={<ThunderboltOutlined />}
              onClick={() => setOpenSmartMatch(true)}
              style={{
                borderColor: "#f97316",
                color: "#f97316",
                fontWeight: 600,
              }}
            >
              AI Smart Match
            </Button>
            <Button
              loading={isSubmit}
              type="primary"
              onClick={handleChangeStatus}
            >
              Change Status
            </Button>
          </Space>
        }
      >
        <Descriptions title="" bordered column={2} layout="vertical">
          <Descriptions.Item label="Email">{dataInit?.email}</Descriptions.Item>
          <Descriptions.Item label="CV File">
            {dataInit?.url ? (
              <a
                href={`${import.meta.env.VITE_BACKEND_URL}/storage/resume/${dataInit.url}`}
                target="_blank"
                rel="noopener noreferrer"
                style={{ color: '#1890ff' }}
              >
                📄 Xem CV
              </a>
            ) : (
              <span style={{ color: '#999' }}>Chưa có CV</span>
            )}
          </Descriptions.Item>
          <Descriptions.Item label="Trạng thái">
            <Form form={form}>
              <Form.Item name={"status"}>
                <Select
                  style={{ width: "100%" }}
                  defaultValue={dataInit?.status}
                >
                  <Option value="PENDING">PENDING</Option>
                  <Option value="REVIEWING">REVIEWING</Option>
                  <Option value="APPROVED">APPROVED</Option>
                  <Option value="REJECTED">REJECTED</Option>
                </Select>
              </Form.Item>
            </Form>
          </Descriptions.Item>
          <Descriptions.Item label="Tên Job">
            {dataInit?.job?.name}
          </Descriptions.Item>
          <Descriptions.Item label="Tên Công Ty">
            {dataInit?.companyName}
          </Descriptions.Item>
          <Descriptions.Item label="Ngày tạo">
            {dataInit && dataInit.createdAt
              ? dayjs(dataInit.createdAt).format("DD-MM-YYYY HH:mm:ss")
              : ""}
          </Descriptions.Item>
          <Descriptions.Item label="Ngày sửa">
            {dataInit && dataInit.updatedAt
              ? dayjs(dataInit.updatedAt).format("DD-MM-YYYY HH:mm:ss")
              : ""}
          </Descriptions.Item>
        </Descriptions>
      </Drawer>

      <SmartMatchModal
        open={openSmartMatch}
        onClose={setOpenSmartMatch}
        resumeId={dataInit?.id}
        resumeEmail={dataInit?.email || ""}
        jobName={dataInit?.job?.name || ""}
        reloadTable={reloadTable}
      />
    </>
  );
};

export default ViewDetailResume;

