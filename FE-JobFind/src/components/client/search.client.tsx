import { Button, Col, Form, Row, Select, notification } from "antd";
import {
  EnvironmentOutlined,
  MonitorOutlined,
  SearchOutlined,
} from "@ant-design/icons";
import { LOCATION_LIST } from "@/config/utils";
import { useEffect, useState } from "react";
import { callFetchAllSkill } from "@/config/api";
import { useLocation, useNavigate, useSearchParams } from "react-router-dom";

const SearchClient = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const [form] = Form.useForm();
  const [searchParams] = useSearchParams();

  const [optionsSkills, setOptionsSkills] = useState<
    { label: string; value: string }[]
  >([]);

  useEffect(() => {
    const fetchSkill = async () => {
      let query = `page=1&size=100&sort=createdAt,desc`;
      const res = await callFetchAllSkill(query);
      if (res && res.data) {
        const arr =
          res?.data?.result?.map((item) => ({
            label: item.name as string,
            // QUAN TRỌNG: Sửa value từ ID sang Name để khớp với bộ lọc skills.name
            value: item.name as string,
          })) ?? [];
        setOptionsSkills(arr);
      }
    };
    fetchSkill();
  }, []);

  useEffect(() => {
    // Sync URL param với Form khi load trang hoặc URL đổi
    const queryLocation = searchParams.get("location");
    const querySkills = searchParams.get("skills");

    if (queryLocation) {
      form.setFieldValue("location", queryLocation.split(","));
    } else {
      form.setFieldValue("location", []);
    }

    if (querySkills) {
      form.setFieldValue("skills", querySkills.split(","));
    } else {
      form.setFieldValue("skills", []);
    }
  }, [location.search]);

  const onFinish = async (values: any) => {
    const { skills, location } = values;

    // Sử dụng URLSearchParams để tạo query string chuẩn
    const params = new URLSearchParams();

    if (skills && skills.length > 0) {
      params.append("skills", skills.join(","));
    }

    if (location && location.length > 0) {
      params.append("location", location.join(","));
    }

    // Nếu không chọn gì cả thì báo lỗi
    if (!params.toString()) {
      notification.error({
        message: "Có lỗi xảy ra",
        description: "Vui lòng chọn tiêu chí để tìm kiếm",
      });
      return;
    }

    navigate(`/job?${params.toString()}`);
  };

  return (
    <Form
      form={form}
      onFinish={onFinish}
      layout="inline"
      style={{ width: "100%" }}
    >
      <Row gutter={[0, 0]} style={{ width: "100%" }} align="middle">
        {/* Input Skills */}
        <Col span={24} md={10}>
          <Form.Item name="skills" style={{ width: "100%", margin: 0 }}>
            <Select
              mode="multiple"
              allowClear
              showArrow={false}
              style={{ width: "100%" }}
              placeholder={
                <span
                  style={{
                    color: "#999",
                    display: "flex",
                    alignItems: "center",
                  }}
                >
                  <MonitorOutlined style={{ marginRight: 8 }} />
                  Tìm theo kỹ năng...
                </span>
              }
              optionLabelProp="label"
              options={optionsSkills}
              size="large"
              bordered={false}
              // @ts-ignore
              variant="borderless"
            />
          </Form.Item>
        </Col>

        {/* Đường gạch ngăn cách (Chỉ hiện trên PC) */}
        <Col
          xs={0}
          md={1}
          style={{ display: "flex", justifyContent: "center" }}
        >
          <div
            style={{
              width: 1,
              height: 35,
              background: "#E4E5E8",
            }}
          ></div>
        </Col>

        {/* Input Location */}
        <Col span={24} md={9}>
          <Form.Item name="location" style={{ width: "100%", margin: 0 }}>
            <Select
              mode="multiple"
              allowClear
              showArrow={false}
              style={{ width: "100%" }}
              placeholder={
                <span
                  style={{
                    color: "#999",
                    display: "flex",
                    alignItems: "center",
                  }}
                >
                  <EnvironmentOutlined style={{ marginRight: 8 }} />
                  Địa điểm...
                </span>
              }
              optionLabelProp="label"
              options={LOCATION_LIST}
              size="large"
              bordered={false}
              // @ts-ignore
              variant="borderless"
            />
          </Form.Item>
        </Col>

        {/* Nút Submit */}
        <Col span={24} md={4}>
          <Form.Item style={{ width: "100%", margin: 0 }}>
            <Button
              type="primary"
              htmlType="submit"
              size="large"
              block
              icon={<SearchOutlined />}
              style={{
                height: 48, // Cao bằng input
                borderRadius: "0 12px 12px 0", // Bo góc phải nếu muốn khớp với container
              }}
            >
              Tìm kiếm
            </Button>
          </Form.Item>
        </Col>
      </Row>
    </Form>
  );
};

export default SearchClient;
