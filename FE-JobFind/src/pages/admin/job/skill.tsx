
import DataTable from "@/components/client/data-table";
import { useAppDispatch, useAppSelector } from "@/redux/hooks";
import { ISkill } from "@/types/backend";
import {
    DeleteOutlined,
    EditOutlined,
    PlusOutlined,
} from "@ant-design/icons";
import { ActionType, ProColumns } from '@ant-design/pro-components';
import { Button, Popconfirm, Space, Tag, Tooltip, message, notification } from "antd";
import { useState, useRef } from 'react';
import dayjs from 'dayjs';
import { callDeleteSkill } from "@/config/api";
import queryString from 'query-string';
import { sfLike } from "spring-filter-query-builder";
import { fetchSkill } from "@/redux/slice/skillSlide";
import ModalSkill from "@/components/admin/skill/modal.skill";
import { isHrRoleName } from "@/config/admin-navigation";

const SkillPage = () => {
    const [openModal, setOpenModal] = useState<boolean>(false);
    const [dataInit, setDataInit] = useState<ISkill | null>(null);

    const tableRef = useRef<ActionType>();

    const isFetching = useAppSelector(state => state.skill.isFetching);
    const meta = useAppSelector(state => state.skill.meta);
    const skills = useAppSelector(state => state.skill.result);
    const roleName = useAppSelector(state => state.account.user.role?.name);
    const isHr = isHrRoleName(roleName);
    const dispatch = useAppDispatch();

    const handleDeleteSkill = async (id: string | undefined) => {
        if (id) {
            const res = await callDeleteSkill(id);
            if (res && +res.statusCode === 200) {
                message.success('Xóa kỹ năng thành công');
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

    const columns: ProColumns<ISkill>[] = [
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
            title: 'Tên Kỹ Năng',
            dataIndex: 'name',
            sorter: true,
            fieldProps: { placeholder: 'Tìm theo tên kỹ năng...' },
            render: (text, record) => (
                <span style={{ fontWeight: 600, color: '#0f172a', fontSize: 14 }}>
                    {record.name}
                </span>
            ),
        },
        {
            title: 'Người Tạo',
            dataIndex: 'createdBy',
            hideInSearch: true,
            render: (text) => (
                <span style={{ color: '#475569', fontSize: 13 }}>
                    {text as string || '—'}
                </span>
            ),
        },
        {
            title: 'Người Cập Nhật',
            dataIndex: 'updatedBy',
            hideInSearch: true,
            render: (text) => (
                <span style={{ color: '#475569', fontSize: 13 }}>
                    {text as string || '—'}
                </span>
            ),
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
            hideInTable: isHr,
            width: 100,
            align: 'center',
            render: (_value, entity, _index, _action) => isHr ? null : (
                <Space>
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
                    <Popconfirm
                        placement="leftTop"
                        title="Xác nhận xóa"
                        description="Bạn có chắc chắn muốn xóa kỹ năng này?"
                        onConfirm={() => handleDeleteSkill(entity.id)}
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

        //mặc định sort theo updatedAt
        if (Object.keys(sortBy).length === 0) {
            temp = `${temp}&sort=updatedAt,desc`;
        } else {
            temp = `${temp}&${sortBy}`;
        }

        return temp;
    }

    return (
        <div>
            <DataTable<ISkill>
                actionRef={tableRef}
                headerTitle="Danh Sách Kỹ Năng"
                rowKey="id"
                loading={isFetching}
                columns={columns}
                dataSource={skills}
                request={async (params, sort, filter): Promise<any> => {
                    const query = buildQuery(params, sort, filter);
                    dispatch(fetchSkill({ query }))
                }}
                scroll={{ x: true }}
                pagination={{
                    current: meta.page,
                    pageSize: meta.pageSize,
                    showSizeChanger: true,
                    total: meta.total,
                    showTotal: (total, range) => (
                        <span style={{ color: '#64748b', fontSize: 13 }}>
                            Hiển thị <strong style={{ color: '#0f172a' }}>{range[0]}-{range[1]}</strong> trên <strong style={{ color: '#0f172a' }}>{total}</strong> kỹ năng
                        </span>
                    )
                }}
                rowSelection={false}
                toolBarRender={(_action, _rows): any => {
                    return !isHr ? (
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
                            Thêm Kỹ Năng
                        </Button>
                    ) : null;
                }}
            />
            <ModalSkill
                openModal={openModal}
                setOpenModal={setOpenModal}
                reloadTable={reloadTable}
                dataInit={dataInit}
                setDataInit={setDataInit}
            />
        </div>
    )
}

export default SkillPage;
