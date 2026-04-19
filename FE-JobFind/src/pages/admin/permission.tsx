import DataTable from "@/components/client/data-table";
import { useAppDispatch, useAppSelector } from "@/redux/hooks";
import { IPermission } from "@/types/backend";
import {
    DeleteOutlined,
    EditOutlined,
    PlusOutlined,
    ApiOutlined,
    CodeOutlined,
} from "@ant-design/icons";
import { ActionType, ProColumns } from '@ant-design/pro-components';
import { Button, Popconfirm, Space, Tag, Tooltip, message, notification } from "antd";
import { useState, useRef } from 'react';
import dayjs from 'dayjs';
import { callDeletePermission } from "@/config/api";
import queryString from 'query-string';
import { fetchPermission } from "@/redux/slice/permissionSlide";
import ViewDetailPermission from "@/components/admin/permission/view.permission";
import ModalPermission from "@/components/admin/permission/modal.permission";
import Access from "@/components/share/access";
import { ALL_PERMISSIONS } from "@/config/permissions";

const getMethodTag = (method: string) => {
    const config: Record<string, { color: string; bg: string }> = {
        'GET': { color: '#059669', bg: '#ecfdf5' },
        'POST': { color: '#2563eb', bg: '#eff6ff' },
        'PUT': { color: '#f59e0b', bg: '#fef3c7' },
        'PATCH': { color: '#8b5cf6', bg: '#f5f3ff' },
        'DELETE': { color: '#dc2626', bg: '#fee2e2' },
    };
    const cfg = config[method?.toUpperCase()] || { color: '#64748b', bg: '#f1f5f9' };
    return (
        <Tag style={{
            borderRadius: 6, fontWeight: 700, fontSize: 11,
            padding: '3px 10px', border: 'none',
            background: cfg.bg, color: cfg.color,
            fontFamily: "'JetBrains Mono', 'Fira Code', monospace",
            letterSpacing: '0.04em',
            minWidth: 60, textAlign: 'center', display: 'inline-block',
        }}>
            {method?.toUpperCase()}
        </Tag>
    );
};

const getModuleTag = (module: string) => {
    const colors = [
        { color: '#6366f1', bg: '#eef2ff' },
        { color: '#ec4899', bg: '#fdf2f8' },
        { color: '#0ea5e9', bg: '#e0f2fe' },
        { color: '#059669', bg: '#ecfdf5' },
        { color: '#f59e0b', bg: '#fef3c7' },
        { color: '#ef4444', bg: '#fee2e2' },
    ];
    const idx = (module || '').split('').reduce((acc, c) => acc + c.charCodeAt(0), 0) % colors.length;
    const cfg = colors[idx];
    return (
        <Tag style={{
            borderRadius: 20, fontWeight: 600, fontSize: 11,
            padding: '2px 12px', border: 'none',
            background: cfg.bg, color: cfg.color,
        }}>
            {module}
        </Tag>
    );
};

const PermissionPage = () => {
    const [openModal, setOpenModal] = useState<boolean>(false);
    const [dataInit, setDataInit] = useState<IPermission | null>(null);
    const [openViewDetail, setOpenViewDetail] = useState<boolean>(false);

    const tableRef = useRef<ActionType>();

    const isFetching = useAppSelector(state => state.permission.isFetching);
    const meta = useAppSelector(state => state.permission.meta);
    const permissions = useAppSelector(state => state.permission.result);
    const dispatch = useAppDispatch();

    const handleDeletePermission = async (id: string | undefined) => {
        if (id) {
            const res = await callDeletePermission(id);
            if (res && res.statusCode === 200) {
                message.success('Xóa quyền hạn thành công');
                reloadTable();
            } else {
                notification.error({
                    message: 'Có lỗi xảy ra',
                    description: res.error
                });
            }
        }
    }

    const reloadTable = () => {
        tableRef?.current?.reload();
    }

    const columns: ProColumns<IPermission>[] = [
        {
            title: 'STT',
            key: 'index',
            width: 55,
            align: 'center',
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
            title: 'Tên',
            dataIndex: 'name',
            sorter: true,
            fieldProps: { placeholder: 'Tìm theo tên quyền...' },
            render: (text, record) => (
                <a
                    href="#"
                    onClick={() => {
                        setOpenViewDetail(true);
                        setDataInit(record);
                    }}
                    style={{ fontWeight: 600, color: '#0f172a', fontSize: 14 }}
                >
                    {record.name}
                </a>
            ),
        },
            title: 'API Path',
            dataIndex: 'apiPath',
            sorter: true,
            fieldProps: { placeholder: 'Tìm theo đường dẫn API...' },
            render: (text) => (
                <code style={{
                    fontSize: 12, color: '#6366f1', fontWeight: 500,
                    background: '#f8fafc', padding: '3px 8px', borderRadius: 6,
                    fontFamily: "'JetBrains Mono', 'Fira Code', monospace",
                    border: '1px solid #e2e8f0',
                }}>
                    {text as string}
                </code>
            ),
        },
            title: 'Method',
            dataIndex: 'method',
            sorter: true,
            width: 100,
            align: 'center',
            fieldProps: { placeholder: 'GET, POST, PUT...' },
            render: (text, entity) => getMethodTag(entity?.method as string),
        },
            title: 'Module',
            dataIndex: 'module',
            sorter: true,
            width: 140,
            fieldProps: { placeholder: 'Tìm theo module...' },
            render: (text) => getModuleTag(text as string),
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
                    <Access permission={ALL_PERMISSIONS.PERMISSIONS.UPDATE} hideChildren>
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
                    <Access permission={ALL_PERMISSIONS.PERMISSIONS.DELETE} hideChildren>
                        <Popconfirm
                            placement="leftTop"
                            title="Xác nhận xóa"
                            description="Bạn có chắc chắn muốn xóa quyền hạn này?"
                            onConfirm={() => handleDeletePermission(entity.id)}
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
        if (clone.apiPath) parts.push(`apiPath ~ '${clone.apiPath}'`);
        if (clone.method) parts.push(`method ~ '${clone.method}'`);
        if (clone.module) parts.push(`module ~ '${clone.module}'`);

        clone.filter = parts.join(' and ');
        if (!clone.filter) delete clone.filter;

        clone.page = clone.current;
        clone.size = clone.pageSize;

        delete clone.current;
        delete clone.pageSize;
        delete clone.name;
        delete clone.apiPath;
        delete clone.method;
        delete clone.module;

        let temp = queryString.stringify(clone);

        let sortBy = "";
        const fields = ["name", "apiPath", "method", "module", "createdAt", "updatedAt"];

        if (sort) {
            for (const field of fields) {
                if (sort[field]) {
                    sortBy = `sort=${field},${sort[field] === 'ascend' ? 'asc' : 'desc'}`;
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
    }

    return (
        <div>
            <Access permission={ALL_PERMISSIONS.PERMISSIONS.GET_PAGINATE}>
                <DataTable<IPermission>
                    actionRef={tableRef}
                    headerTitle="Danh Sách Quyền Hạn"
                    rowKey="id"
                    loading={isFetching}
                    columns={columns}
                    dataSource={permissions}
                    request={async (params, sort, filter): Promise<any> => {
                        const query = buildQuery(params, sort, filter);
                        dispatch(fetchPermission({ query }))
                    }}
                    scroll={{ x: true }}
                    pagination={{
                        current: meta.page,
                        pageSize: meta.pageSize,
                        showSizeChanger: true,
                        total: meta.total,
                        showTotal: (total, range) => (
                            <span style={{ color: '#64748b', fontSize: 13 }}>
                                Hiển thị <strong style={{ color: '#0f172a' }}>{range[0]}-{range[1]}</strong> trên <strong style={{ color: '#0f172a' }}>{total}</strong> quyền
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
                                Thêm Quyền Hạn
                            </Button>
                        );
                    }}
                />
            </Access>
            <ModalPermission
                openModal={openModal}
                setOpenModal={setOpenModal}
                reloadTable={reloadTable}
                dataInit={dataInit}
                setDataInit={setDataInit}
            />

            <ViewDetailPermission
                onClose={setOpenViewDetail}
                open={openViewDetail}
                dataInit={dataInit}
                setDataInit={setDataInit}
            />
        </div>
    )
}

export default PermissionPage;