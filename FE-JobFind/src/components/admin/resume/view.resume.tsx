import { callUpdateResumeStatus } from "@/config/api";
import { IResume } from "@/types/backend";
import {
  Badge,
  Button,
  Descriptions,
  Drawer,
  Form,
  Select,
  message,
  notification,
} from "antd";
import dayjs from "dayjs";
import { useState, useEffect } from "react";
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
          <Button
            loading={isSubmit}
            type="primary"
            onClick={handleChangeStatus}
          >
            Change Status
          </Button>
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
                  // placeholder="Select a option and change input text above"
                  // onChange={onGenderChange}
                  // allowClear
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
    </>
  );
};

export default ViewDetailResume;
