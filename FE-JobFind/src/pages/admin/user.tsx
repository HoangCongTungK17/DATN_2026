import DataTable from "@/components/client/data-table";
import { useAppDispatch, useAppSelector } from "@/redux/hooks";
import { fetchUser } from "@/redux/slice/userSlide";
import { IUser } from "@/types/backend";
import {
    DeleteOutlined,
    EditOutlined,
    PlusOutlined,
    MailOutlined,
    UserOutlined,
} from "@ant-design/icons";
import { ActionType, ProColumns } from '@ant-design/pro-components';
import { Button, Popconfirm, Space, Tag, Tooltip, Avatar, message, notification } from "antd";
import { useState, useRef } from 'react';
import dayjs from 'dayjs';
import { callDeleteUser } from "@/config/api";
import queryString from 'query-string';
import ModalUser from "@/components/admin/user/modal.user";
import ViewDetailUser from "@/components/admin/user/view.user";
import Access from "@/components/share/access";
import { ALL_PERMISSIONS } from "@/config/permissions";
import { sfLike } from "spring-filter-query-builder";

const UserPage = () => {
    const [openModal, setOpenModal] = useState<boolean>(false);
    const [dataInit, setDataInit] = useState<IUser | null>(null);
    const [openViewDetail, setOpenViewDetail] = useState<boolean>(false);

    const tableRef = useRef<ActionType>();

    const isFetching = useAppSelector(state => state.user.isFetching);
    const meta = useAppSelector(state => state.user.meta);
    const users = useAppSelector(state => state.user.result);
    const dispatch = useAppDispatch();

    const handleDeleteUser = async (id: string | undefined) => {
        if (id) {
            const res = await callDeleteUser(id);
            if (+res.statusCode === 200) {
                message.success('Xóa người dùng thành công');
                reloadTable();
            } else {
                notification.error({
                    message: 'Có lỗi xảy ra',
                    description: res.message
                });
            }
        }
    }

    const reloadTable = () => {
        tableRef?.current?.reload();
    }

    // Color based on the first letter of name
    const getAvatarColor = (name: string) => {
        const colors = [
            'linear-gradient(135deg, #6366f1, #a855f7)',
            'linear-gradient(135deg, #ec4899, #f43f5e)',
            'linear-gradient(135deg, #0ea5e9, #06b6d4)',
            'linear-gradient(135deg, #059669, #10b981)',
            'linear-gradient(135deg, #f59e0b, #ef4444)',
        ];
        const charCode = (name || 'A').charCodeAt(0);
        return colors[charCode % colors.length];
    };

    const getRoleTag = (roleName: string) => {
        const roleConfig: Record<string, { color: string; label: string }> = {
            'SUPER_ADMIN': { color: '#dc2626', label: 'Super Admin' },
            'ADMIN': { color: '#7c3aed', label: 'Admin' },
            'HR': { color: '#2563eb', label: 'HR' },
            'NORMAL_USER': { color: '#64748b', label: 'Người dùng' },
        };
        const cfg = roleConfig[roleName] || { color: '#64748b', label: roleName };
        return (
            <Tag
                style={{
                    borderRadius: 20, fontWeight: 600, fontSize: 11,
                    padding: '2px 10px', border: 'none',
                    background: `${cfg.color}15`, color: cfg.color,
                }}
            >
                {cfg.label}
            </Tag>
        );
    };

    const columns: ProColumns<IUser>[] = [
        {
            title: 'STT',
            key: 'index',
            width: 55,
            align: "center",
            render: (text, record, index) => (
                <span style={{
                    fontWeight: 700, color: '#64748b', fontSize: 13,
                    width: 28, height: 28, borderRadius: 8,
                    display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
                    background: '#f1f5f9',
                }}>
                    {(index + 1) + (meta.page - 1) * (meta.pageSize)}
                </span>
            ),
            hideInSearch: true,
        },
        {
            title: 'Họ Tên',
            dataIndex: 'name',
            sorter: true,
            fieldProps: { placeholder: 'Tìm theo họ tên...' },
            render: (text, record) => (
                <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                    <Avatar
                        size={36}
                        icon={<UserOutlined />}
                        style={{
                            background: getAvatarColor(record.name),
                            fontWeight: 700, fontSize: 13, flexShrink: 0,
                        }}
                    >
                        {record.name?.substring(0, 2)?.toUpperCase()}
                    </Avatar>
                    <span style={{ fontWeight: 600, color: '#0f172a' }}>{record.name}</span>
                </div>
            ),
        },
        {
            title: 'Email',
            dataIndex: 'email',
            sorter: true,
            fieldProps: { placeholder: 'Tìm theo email...' },
            render: (text) => (
                <span style={{ color: '#475569', display: 'flex', alignItems: 'center', gap: 6, fontSize: 13 }}>
                    <MailOutlined style={{ color: '#94a3b8', fontSize: 13 }} />
                    {text as string}
                </span>
            ),
        },
        {
            title: 'Vai Trò',
            dataIndex: ["role", "name"],
            sorter: true,
            width: 140,
            render: (text) => getRoleTag(text as string),
            hideInSearch: true
        },
        {
            title: 'Công Ty',
            dataIndex: ["company", "name"],
            sorter: true,
            render: (text) => text ? (
                <span style={{ color: '#475569' }}>
                    {text as string}
                </span>
            ) : <span style={{ color: '#cbd5e1' }}>—</span>,
            hideInSearch: true
        },
        {
            title: 'Ngày Tạo',
            dataIndex: 'createdAt',
            width: 155,
            sorter: true,
            render: (text, record) => (
                <span style={{ color: '#64748b', fontSize: 13 }}>
                    {record.createdAt ? dayjs(record.createdAt).format('DD/MM/YYYY HH:mm') : "—"}
                </span>
            ),
            hideInSearch: true,
        },
        {
            title: 'Cập Nhật',
            dataIndex: 'updatedAt',
            width: 155,
            sorter: true,
            render: (text, record) => (
                <span style={{ color: '#64748b', fontSize: 13 }}>
                    {record.updatedAt ? dayjs(record.updatedAt).format('DD/MM/YYYY HH:mm') : "—"}
                </span>
            ),
            hideInSearch: true,
        },
        {
            title: 'Thao Tác',
            hideInSearch: true,
            width: 100,
            align: 'center',
            render: (_value, entity, _index, _action) => (
                <Space>
                    <Access permission={ALL_PERMISSIONS.USERS.UPDATE} hideChildren>
                        <Tooltip title="Chỉnh sửa">
                            <EditOutlined
                                style={{
                                    fontSize: 16, color: '#6366f1', cursor: 'pointer',
                                    padding: 6, borderRadius: 8, background: '#eef2ff',
                                }}
                                onClick={() => {
                                    setOpenModal(true);
                                    setDataInit(entity);
                                }}
                            />
                        </Tooltip>
                    </Access>
                    <Access permission={ALL_PERMISSIONS.USERS.DELETE} hideChildren>
                        <Popconfirm
                            placement="leftTop"
                            title="Xác nhận xóa"
                            description="Bạn có chắc chắn muốn xóa người dùng này?"
                            onConfirm={() => handleDeleteUser(entity.id)}
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
        const q: any = {
            page: params.current,
            size: params.pageSize,
            filter: ""
        }

        const clone = { ...params };
        if (clone.name) q.filter = `${sfLike("name", clone.name)}`;
        if (clone.email) {
            q.filter = clone.name ?
                q.filter + " and " + `${sfLike("email", clone.email)}`
                : `${sfLike("email", clone.email)}`;
        }

        if (!q.filter) delete q.filter;
        let temp = queryString.stringify(q);

        let sortBy = "";
        if (sort && sort.name) {
            sortBy = sort.name === 'ascend' ? "sort=name,asc" : "sort=name,desc";
        }
        if (sort && sort.email) {
            sortBy = sort.email === 'ascend' ? "sort=email,asc" : "sort=email,desc";
        }
        if (sort && sort.createdAt) {
            sortBy = sort.createdAt === 'ascend' ? "sort=createdAt,asc" : "sort=createdAt,desc";
        }
        if (sort && sort.updatedAt) {
            sortBy = sort.updatedAt === 'ascend' ? "sort=updatedAt,asc" : "sort=updatedAt,desc";
        }

        if (Object.keys(sortBy).length === 0) {
            temp = `${temp}&sort=updatedAt,desc`;
        } else {
            temp = `${temp}&${sortBy}`;
        }

        return temp;
    }

    return (
        <div>
            <Access permission={ALL_PERMISSIONS.USERS.GET_PAGINATE}>
                <DataTable<IUser>
                    actionRef={tableRef}
                    headerTitle="Danh Sách Người Dùng"
                    rowKey="id"
                    loading={isFetching}
                    columns={columns}
                    dataSource={users}
                    request={async (params, sort, filter): Promise<any> => {
                        const query = buildQuery(params, sort, filter);
                        dispatch(fetchUser({ query }))
                    }}
                    scroll={{ x: true }}
                    pagination={{
                        current: meta.page,
                        pageSize: meta.pageSize,
                        showSizeChanger: true,
                        total: meta.total,
                        showTotal: (total, range) => (
                            <span style={{ color: '#64748b', fontSize: 13 }}>
                                Hiển thị <strong style={{ color: '#0f172a' }}>{range[0]}-{range[1]}</strong> trên <strong style={{ color: '#0f172a' }}>{total}</strong> người dùng
                            </span>
                        )
                    }}
                    rowSelection={false}
                    toolBarRender={(_action, _rows): any => {
                        return (
                            <Button
                                icon={<PlusOutlined />}
                                type="primary"
                                onClick={() => setOpenModal(true)}
                                style={{
                                    borderRadius: 10, height: 40, fontWeight: 600,
                                    background: 'linear-gradient(135deg, #6366f1, #a855f7)',
                                    border: 'none', boxShadow: '0 4px 14px rgba(99, 102, 241, 0.3)',
                                }}
                            >
                                Thêm Người Dùng
                            </Button>
                        );
                    }}
                />
            </Access>
            <ModalUser
                openModal={openModal}
                setOpenModal={setOpenModal}
                reloadTable={reloadTable}
                dataInit={dataInit}
                setDataInit={setDataInit}
            />
            <ViewDetailUser
                onClose={setOpenViewDetail}
                open={openViewDetail}
                dataInit={dataInit}
                setDataInit={setDataInit}
            />
        </div>
    )
}

export default UserPage;
