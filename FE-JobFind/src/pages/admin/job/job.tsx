import DataTable from "@/components/client/data-table";
import { useAppDispatch, useAppSelector } from "@/redux/hooks";
import { IJob } from "@/types/backend";
import {
    DeleteOutlined,
    EditOutlined,
    PlusOutlined,
    DollarOutlined,
    CheckCircleOutlined,
    CloseCircleOutlined,
    ThunderboltOutlined,
} from "@ant-design/icons";
import {
    ActionType,
    ProColumns,
    ProFormSelect,
} from "@ant-design/pro-components";
import { Button, Popconfirm, Space, Tag, Tooltip, message, notification } from "antd";
import { useRef } from "react";
import dayjs from "dayjs";
import { callDeleteJob } from "@/config/api";
import queryString from "query-string";
import { useNavigate } from "react-router-dom";
import { fetchJob } from "@/redux/slice/jobSlide";
import Access from "@/components/share/access";
import { ALL_PERMISSIONS } from "@/config/permissions";
import { sfIn } from "spring-filter-query-builder";

const getLevelTag = (level: string) => {
    const config: Record<string, { color: string; bg: string }> = {
        'INTERN': { color: '#64748b', bg: '#f1f5f9' },
        'FRESHER': { color: '#0ea5e9', bg: '#e0f2fe' },
        'JUNIOR': { color: '#059669', bg: '#ecfdf5' },
        'MIDDLE': { color: '#f59e0b', bg: '#fef3c7' },
        'SENIOR': { color: '#dc2626', bg: '#fee2e2' },
    };
    const cfg = config[level] || { color: '#64748b', bg: '#f1f5f9' };
    return (
        <Tag style={{
            borderRadius: 20, fontWeight: 700, fontSize: 11,
            padding: '2px 12px', border: 'none',
            background: cfg.bg, color: cfg.color,
            letterSpacing: '0.03em',
        }}>
            {level}
        </Tag>
    );
};

const JobPage = () => {
    const tableRef = useRef<ActionType>();

    const isFetching = useAppSelector((state) => state.job.isFetching);
    const meta = useAppSelector((state) => state.job.meta);
    const jobs = useAppSelector((state) => state.job.result);
    const dispatch = useAppDispatch();
    const navigate = useNavigate();

    const handleDeleteJob = async (id: string | undefined) => {
        if (id) {
            const res = await callDeleteJob(id);
            if (res && res.data) {
                message.success("Xóa việc làm thành công");
                reloadTable();
            } else {
                notification.error({
                    message: "Có lỗi xảy ra",
                    description: res.message,
                });
            }
        }
    };

    const reloadTable = () => {
        tableRef?.current?.reload();
    };

    const columns: ProColumns<IJob>[] = [
        {
            title: "STT",
            key: "index",
            width: 55,
            align: "center",
            render: (text, record, index) => (
                <span style={{
                    fontWeight: 700, color: '#64748b', fontSize: 13,
                    width: 28, height: 28, borderRadius: 8,
                    display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
                    background: '#f1f5f9',
                }}>
                    {index + 1 + (meta.page - 1) * meta.pageSize}
                </span>
            ),
            hideInSearch: true,
        },
        {
            title: "Tên Việc Làm",
            dataIndex: "name",
            sorter: true,
            fieldProps: { placeholder: 'Tìm theo tên việc làm...' },
            render: (text, record) => (
                <span style={{ fontWeight: 600, color: '#0f172a', fontSize: 14 }}>
                    {record.name}
                </span>
            ),
        },
        {
            title: "Công Ty",
            dataIndex: ["company", "name"],
            sorter: true,
            hideInSearch: true,
            render: (text) => (
                <span style={{ color: '#475569' }}>{text as string}</span>
            ),
        },
        {
            title: "Mức Lương",
            dataIndex: "salary",
            width: 140,
            sorter: true,
            fieldProps: { placeholder: 'Tìm theo mức lương...' },
            render(dom, entity) {
                const str = "" + entity.salary;
                return (
                    <span style={{
                        color: '#059669', fontWeight: 700, fontSize: 13,
                        display: 'flex', alignItems: 'center', gap: 4,
                    }}>
                        <DollarOutlined style={{ fontSize: 13 }} />
                        {str?.replace(/\B(?=(\d{3})+(?!\d))/g, ",")} đ
                    </span>
                );
            },
        },
        {
            title: "Level",
            dataIndex: "level",
            width: 120,
            align: 'center',
            render: (text) => getLevelTag(text as string),
            renderFormItem: (item, props, form) => (
                <ProFormSelect
                    showSearch
                    mode="multiple"
                    allowClear
                    valueEnum={{
                        INTERN: "Intern",
                        FRESHER: "Fresher",
                        JUNIOR: "Junior",
                        MIDDLE: "Middle",
                        SENIOR: "Senior",
                    }}
                    placeholder="Chọn level"
                />
            ),
        },
        {
            title: "Trạng Thái",
            dataIndex: "active",
            width: 120,
            align: 'center',
            render(dom, entity) {
                return entity.active ? (
                    <Tag
                        icon={<CheckCircleOutlined />}
                        style={{
                            borderRadius: 20, fontWeight: 600, fontSize: 12,
                            padding: '2px 12px', border: 'none',
                            background: '#ecfdf5', color: '#059669',
                        }}
                    >
                        Đang tuyển
                    </Tag>
                ) : (
                    <Tag
                        icon={<CloseCircleOutlined />}
                        style={{
                            borderRadius: 20, fontWeight: 600, fontSize: 12,
                            padding: '2px 12px', border: 'none',
                            background: '#fee2e2', color: '#dc2626',
                        }}
                    >
                        Ngừng tuyển
                    </Tag>
                );
            },
            hideInSearch: true,
        },
        {
            title: "Ngày Tạo",
            dataIndex: "createdAt",
            width: 150,
            sorter: true,
            render: (text, record) => (
                <span style={{ color: '#64748b', fontSize: 13 }}>
                    {record.createdAt ? dayjs(record.createdAt).format("DD/MM/YYYY HH:mm") : "—"}
                </span>
            ),
            hideInSearch: true,
        },
        {
            title: "Cập Nhật",
            dataIndex: "updatedAt",
            width: 150,
            sorter: true,
            render: (text, record) => (
                <span style={{ color: '#64748b', fontSize: 13 }}>
                    {record.updatedAt ? dayjs(record.updatedAt).format("DD/MM/YYYY HH:mm") : "—"}
                </span>
            ),
            hideInSearch: true,
        },
        {
            title: "Thao Tác",
            hideInSearch: true,
            width: 100,
            align: 'center',
            render: (_value, entity, _index, _action) => (
                <Space>
                    <Access permission={ALL_PERMISSIONS.JOBS.UPDATE} hideChildren>
                        <Tooltip title="Chỉnh sửa">
                            <EditOutlined
                                style={{
                                    fontSize: 16, color: '#6366f1', cursor: 'pointer',
                                    padding: 6, borderRadius: 8, background: '#eef2ff',
                                }}
                                onClick={() => {
                                    navigate(`/admin/job/upsert?id=${entity.id}`);
                                }}
                            />
                        </Tooltip>
                    </Access>
                    <Access permission={ALL_PERMISSIONS.JOBS.DELETE} hideChildren>
                        <Popconfirm
                            placement="leftTop"
                            title="Xác nhận xóa"
                            description="Bạn có chắc chắn muốn xóa việc làm này?"
                            onConfirm={() => handleDeleteJob(entity.id)}
                            okText="Xác nhận"
                            cancelText="Hủy"
                            okButtonProps={{ danger: true }}
                        >
                            <Tooltip title="Xóa">
                                <DeleteOutlined
                                    style={{
                                        fontSize: 16, color: '#dc2626', cursor: 'pointer',
                                        padding: 6, borderRadius: 8, background: '#fee2e2',
                                    }}
                                />
                            </Tooltip>
                        </Popconfirm>
                    </Access>
                </Space>
            ),
        },
    ];

    const buildQuery = (params: any, sort: any, filter: any) => {
        const clone = { ...params };
        let parts = [];
        if (clone.name) parts.push(`name ~ '${clone.name}'`);
        if (clone.salary) parts.push(`salary ~ '${clone.salary}'`);
        if (clone?.level?.length) {
            parts.push(`${sfIn("level", clone.level).toString()}`);
        }

        clone.filter = parts.join(" and ");
        if (!clone.filter) delete clone.filter;

        clone.page = clone.current;
        clone.size = clone.pageSize;

        delete clone.current;
        delete clone.pageSize;
        delete clone.name;
        delete clone.salary;
        delete clone.level;

        let temp = queryString.stringify(clone);

        let sortBy = "";
        const fields = ["name", "salary", "createdAt", "updatedAt"];
        if (sort) {
            for (const field of fields) {
                if (sort[field]) {
                    sortBy = `sort=${field},${sort[field] === "ascend" ? "asc" : "desc"}`;
                    break;
                }
            }
        }

        if (Object.keys(sortBy).length === 0) {
            temp = `${temp}&sort=updatedAt,desc`;
        } else {
            temp = `${temp}&${sortBy}`;
        }

        return temp;
    };

    return (
        <div>
            <Access permission={ALL_PERMISSIONS.JOBS.GET_PAGINATE}>
                <DataTable<IJob>
                    actionRef={tableRef}
                    headerTitle="Danh Sách Việc Làm"
                    rowKey="id"
                    loading={isFetching}
                    columns={columns}
                    dataSource={jobs}
                    request={async (params, sort, filter): Promise<any> => {
                        const query = buildQuery(params, sort, filter);
                        dispatch(fetchJob({ query }));
                    }}
                    scroll={{ x: true }}
                    pagination={{
                        current: meta.page,
                        pageSize: meta.pageSize,
                        showSizeChanger: true,
                        total: meta.total,
                        showTotal: (total, range) => (
                            <span style={{ color: '#64748b', fontSize: 13 }}>
                                Hiển thị <strong style={{ color: '#0f172a' }}>{range[0]}-{range[1]}</strong> trên <strong style={{ color: '#0f172a' }}>{total}</strong> việc làm
                            </span>
                        ),
                    }}
                    rowSelection={false}
                    toolBarRender={(_action, _rows): any => {
                        return (
                            <Button
                                icon={<PlusOutlined />}
                                type="primary"
                                onClick={() => navigate("upsert")}
                                style={{
                                    borderRadius: 10, height: 40, fontWeight: 600,
                                    background: 'linear-gradient(135deg, #6366f1, #a855f7)',
                                    border: 'none', boxShadow: '0 4px 14px rgba(99, 102, 241, 0.3)',
                                }}
                            >
                                Thêm Việc Làm
                            </Button>
                        );
                    }}
                />
            </Access>
        </div>
    );
};

export default JobPage;
