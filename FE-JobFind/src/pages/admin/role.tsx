import DataTable from "@/components/client/data-table";
import { useAppDispatch, useAppSelector } from "@/redux/hooks";
import { IPermission, IRole } from "@/types/backend";
import {
    DeleteOutlined,
    EditOutlined,
    PlusOutlined,
    CheckCircleOutlined,
    CloseCircleOutlined,
    SafetyCertificateOutlined,
} from "@ant-design/icons";
import { ActionType, ProColumns } from '@ant-design/pro-components';
import { Button, Popconfirm, Space, Tag, Tooltip, message, notification } from "antd";
import { useState, useRef, useEffect } from 'react';
import dayjs from 'dayjs';
import { callDeleteRole, callFetchPermission } from "@/config/api";
import queryString from 'query-string';
import { fetchRole } from "@/redux/slice/roleSlide";
import ModalRole from "@/components/admin/role/modal.role";
import { ALL_PERMISSIONS } from "@/config/permissions";
import Access from "@/components/share/access";
import { sfLike } from "spring-filter-query-builder";
import { groupByPermission } from "@/config/utils";

const RolePage = () => {
    const [openModal, setOpenModal] = useState<boolean>(false);

    const tableRef = useRef<ActionType>();

    const isFetching = useAppSelector(state => state.role.isFetching);
    const meta = useAppSelector(state => state.role.meta);
    const roles = useAppSelector(state => state.role.result);
    const dispatch = useAppDispatch();

    //all backend permissions
    const [listPermissions, setListPermissions] = useState<{
        module: string;
        permissions: IPermission[]
    }[] | null>(null);

    //current role
    const [singleRole, setSingleRole] = useState<IRole | null>(null);

    useEffect(() => {
        const init = async () => {
            const res = await callFetchPermission(`page=1&size=100`);
            if (res.data?.result) {
                setListPermissions(groupByPermission(res.data?.result))
            }
        }
        init();
    }, [])

    const handleDeleteRole = async (id: string | undefined) => {
        if (id) {
            const res = await callDeleteRole(id);
            if (res && res.statusCode === 200) {
                message.success('Xóa vai trò thành công');
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

    const getRoleNameDisplay = (name: string) => {
        const roleConfig: Record<string, { color: string; bg: string; icon: string }> = {
            'SUPER_ADMIN': { color: '#dc2626', bg: '#fee2e2', icon: '👑' },
            'ADMIN': { color: '#7c3aed', bg: '#f5f3ff', icon: '🛡️' },
            'HR': { color: '#2563eb', bg: '#eff6ff', icon: '💼' },
            'NORMAL_USER': { color: '#64748b', bg: '#f1f5f9', icon: '👤' },
        };
        const cfg = roleConfig[name] || null;
        if (cfg) {
            return (
                <span style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                    <span>{cfg.icon}</span>
                    <Tag style={{
                        borderRadius: 20, fontWeight: 700, fontSize: 12,
                        padding: '3px 14px', border: 'none',
                        background: cfg.bg, color: cfg.color,
                    }}>
                        {name}
                    </Tag>
                </span>
            );
        }
        return <span style={{ fontWeight: 600, color: '#0f172a' }}>{name}</span>;
    };

    const columns: ProColumns<IRole>[] = [
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
            title: 'Tên Vai Trò',
            dataIndex: 'name',
            sorter: true,
            fieldProps: { placeholder: 'Tìm theo tên vai trò...' },
            render: (text, record) => getRoleNameDisplay(record.name),
        },
        {
            title: 'Trạng Thái',
            dataIndex: 'active',
            width: 140,
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
                        Hoạt động
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
                        Vô hiệu
                    </Tag>
                );
            },
            hideInSearch: true,
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
                    <Access permission={ALL_PERMISSIONS.ROLES.UPDATE} hideChildren>
                        <Tooltip title="Chỉnh sửa">
                            <EditOutlined
                                style={{
                                    fontSize: 16, color: '#6366f1', cursor: 'pointer',
                                    padding: 6, borderRadius: 8, background: '#eef2ff',
                                }}
                                onClick={() => {
                                    setSingleRole(entity);
                                    setOpenModal(true);
                                }}
                            />
                        </Tooltip>
                    </Access>
                    <Access permission={ALL_PERMISSIONS.ROLES.DELETE} hideChildren>
                        <Popconfirm
                            placement="leftTop"
                            title="Xác nhận xóa"
                            description="Bạn có chắc chắn muốn xóa vai trò này?"
                            onConfirm={() => handleDeleteRole(entity.id)}
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
        const q: any = {
            page: params.current,
            size: params.pageSize,
            filter: ""
        }

        if (clone.name) q.filter = `${sfLike("name", clone.name)}`;

        if (!q.filter) delete q.filter;

        let temp = queryString.stringify(q);

        let sortBy = "";
        if (sort && sort.name) {
            sortBy = sort.name === 'ascend' ? "sort=name,asc" : "sort=name,desc";
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
            <Access permission={ALL_PERMISSIONS.ROLES.GET_PAGINATE}>
                <DataTable<IRole>
                    actionRef={tableRef}
                    headerTitle="Danh Sách Vai Trò"
                    rowKey="id"
                    loading={isFetching}
                    columns={columns}
                    dataSource={roles}
                    request={async (params, sort, filter): Promise<any> => {
                        const query = buildQuery(params, sort, filter);
                        dispatch(fetchRole({ query }))
                    }}
                    scroll={{ x: true }}
                    pagination={{
                        current: meta.page,
                        pageSize: meta.pageSize,
                        showSizeChanger: true,
                        total: meta.total,
                        showTotal: (total, range) => (
                            <span style={{ color: '#64748b', fontSize: 13 }}>
                                Hiển thị <strong style={{ color: '#0f172a' }}>{range[0]}-{range[1]}</strong> trên <strong style={{ color: '#0f172a' }}>{total}</strong> vai trò
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
                                Thêm Vai Trò
                            </Button>
                        );
                    }}
                />
            </Access>
            <ModalRole
                openModal={openModal}
                setOpenModal={setOpenModal}
                reloadTable={reloadTable}
                listPermissions={listPermissions!}
                singleRole={singleRole}
                setSingleRole={setSingleRole}
            />
        </div>
    )
}

export default RolePage;