import DataTable from "@/components/client/data-table";
import { useAppDispatch, useAppSelector } from "@/redux/hooks";
import { IResume } from "@/types/backend";
import { ActionType, ProColumns, ProFormSelect } from '@ant-design/pro-components';
import { Popconfirm, Space, message, notification, Tag, Slider, Tooltip } from "antd";
import { useState, useRef, useMemo } from 'react';
import dayjs from 'dayjs';
import { callDeleteResume } from "@/config/api";
import queryString from 'query-string';
import { fetchResume } from "@/redux/slice/resumeSlide";
import ViewDetailResume from "@/components/admin/resume/view.resume";
import { ALL_PERMISSIONS } from "@/config/permissions";
import Access from "@/components/share/access";
import { sfIn } from "spring-filter-query-builder";
import {
    EditOutlined,
    DeleteOutlined,
    CheckCircleOutlined,
    ClockCircleOutlined,
    CloseCircleOutlined,
    EyeOutlined,
    FileSearchOutlined,
    ThunderboltOutlined,
    FilterOutlined,
} from "@ant-design/icons";

// ============================================
// MATCH SCORE — Đọc từ dữ liệu AI thật (backend field: aiMatchScore)
// ============================================
const getMatchScore = (resume: any): number | null => {
    return resume.aiMatchScore ?? null;
};

const getMatchColor = (score: number): string => {
    if (score >= 75) return '#059669';
    if (score >= 50) return '#f59e0b';
    return '#dc2626';
};

const getMatchLabel = (score: number): string => {
    if (score >= 85) return 'Rất phù hợp';
    if (score >= 75) return 'Phù hợp';
    if (score >= 50) return 'Tạm được';
    return 'Chưa khớp';
};

// ============================================

const ResumePage = () => {
    const tableRef = useRef<ActionType>();

    const isFetching = useAppSelector(state => state.resume.isFetching);
    const meta = useAppSelector(state => state.resume.meta);
    const resumes = useAppSelector(state => state.resume.result);
    const dispatch = useAppDispatch();

    const [dataInit, setDataInit] = useState<IResume | null>(null);
    const [openViewDetail, setOpenViewDetail] = useState<boolean>(false);
    const [matchFilter, setMatchFilter] = useState<[number, number]>([0, 100]);

    const handleDeleteResume = async (id: string | undefined) => {
        if (id) {
            const res = await callDeleteResume(id);
            if (res && +res.statusCode === 200) {
                message.success('Xóa Resume thành công');
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

    // Filtered resumes by match score
    const filteredResumes = useMemo(() => {
        if (!resumes) return resumes;
        if (matchFilter[0] === 0 && matchFilter[1] === 100) return resumes;
        return resumes.filter((resume: any) => {
            const score = getMatchScore(resume);
            if (score === null) return matchFilter[0] === 0;
            return score >= matchFilter[0] && score <= matchFilter[1];
        });
    }, [resumes, matchFilter]);

    const getStatusTag = (status: string) => {
        const config: Record<string, { color: string; icon: React.ReactNode; label: string }> = {
            'PENDING': { color: 'processing', icon: <ClockCircleOutlined />, label: 'Chờ duyệt' },
            'REVIEWING': { color: 'warning', icon: <EyeOutlined />, label: 'Đang xét' },
            'APPROVED': { color: 'success', icon: <CheckCircleOutlined />, label: 'Đã duyệt' },
            'REJECTED': { color: 'error', icon: <CloseCircleOutlined />, label: 'Từ chối' },
        };
        const cfg = config[status] || config['PENDING'];
        return (
            <Tag
                icon={cfg.icon}
                color={cfg.color}
                style={{ borderRadius: 20, padding: '2px 12px', fontWeight: 600, fontSize: 12 }}
            >
                {cfg.label}
            </Tag>
        );
    };

    const columns: ProColumns<IResume>[] = [
        {
            title: 'ID',
            dataIndex: 'id',
            width: 50,
            render: (text, record, index, action) => {
                return (
                    <a
                        href="#"
                        onClick={() => {
                            setOpenViewDetail(true);
                            setDataInit(record);
                        }}
                        style={{ fontWeight: 600, color: '#2563eb' }}
                    >
                        #{record.id?.toString().slice(-4)}
                    </a>
                )
            },
            hideInSearch: true,
        },
        {
            title: 'Trạng Thái',
            dataIndex: 'status',
            sorter: true,
            width: 140,
            render: (text, record) => getStatusTag(record.status || 'PENDING'),
            renderFormItem: (item, props, form) => (
                <ProFormSelect
                    showSearch
                    mode="multiple"
                    allowClear
                    valueEnum={{
                        PENDING: 'Chờ duyệt',
                        REVIEWING: 'Đang xét',
                        APPROVED: 'Đã duyệt',
                        REJECTED: 'Từ chối',
                    }}
                    placeholder="Chọn trạng thái"
                />
            ),
        },
        {
            title: 'Vị Trí',
            dataIndex: ["job", "name"],
            hideInSearch: true,
            render: (text) => (
                <span style={{ fontWeight: 600, color: '#0f172a' }}>{text as string}</span>
            ),
        },
        {
            title: 'Công Ty',
            dataIndex: "companyName",
            hideInSearch: true,
            render: (text) => (
                <span style={{ color: '#334155' }}>{text as string}</span>
            ),
        },
        {
            title: 'CV',
            dataIndex: 'url',
            width: 90,
            align: 'center',
            render: (text, record) => {
                return (
                    <>
                        {record.url ? (
                            <a
                                href={`${import.meta.env.VITE_BACKEND_URL}/storage/resume/${record.url}`}
                                target="_blank"
                                rel="noopener noreferrer"
                                style={{
                                    color: '#2563eb',
                                    fontWeight: 600,
                                    fontSize: 12,
                                    display: 'inline-flex',
                                    alignItems: 'center',
                                    gap: 4,
                                    padding: '4px 10px',
                                    borderRadius: 6,
                                    background: '#eff6ff',
                                    transition: 'all 0.2s',
                                }}
                            >
                                <EyeOutlined /> Xem
                            </a>
                        ) : (
                            <span style={{ color: '#94a3b8' }}>—</span>
                        )}
                    </>
                )
            },
            hideInSearch: true,
        },
        // ========== CỘT MATCH SCORE MỚI ==========
        {
            title: (
                <Tooltip title="Tỉ lệ khớp giữa CV và vị trí ứng tuyển (AI-powered)">
                    <span style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
                        <ThunderboltOutlined style={{ color: '#f59e0b' }} />
                        Match Score
                    </span>
                </Tooltip>
            ),
            dataIndex: 'aiMatchScore',
            width: 160,
            hideInSearch: true,
            sorter: (a: any, b: any) => (getMatchScore(a) ?? -1) - (getMatchScore(b) ?? -1),
            defaultSortOrder: 'descend',
            render: (_text: any, record: any) => {
                const score = getMatchScore(record);
                if (score === null) {
                    return (
                        <div className="match-score-cell">
                            <span style={{ color: '#94a3b8', fontSize: 13, fontStyle: 'italic' }}>Chưa phân tích</span>
                        </div>
                    );
                }
                const color = getMatchColor(score);
                const label = getMatchLabel(score);

                return (
                    <div className="match-score-cell">
                        <div className="match-bar">
                            <div
                                className="match-fill"
                                style={{
                                    width: `${score}%`,
                                    background: `linear-gradient(90deg, ${color}, ${color}dd)`,
                                }}
                            />
                        </div>
                        <Tooltip title={label}>
                            <span
                                className={`match-value ${score >= 75 ? 'high' : score >= 50 ? 'medium' : 'low'}`}
                            >
                                {score}%
                            </span>
                        </Tooltip>
                    </div>
                );
            },
        },
        // ==========================================
        {
            title: 'Ngày Nộp',
            dataIndex: 'createdAt',
            width: 140,
            sorter: true,
            render: (text, record) => (
                <span style={{ color: '#64748b', fontSize: 13 }}>
                    {record.createdAt ? dayjs(record.createdAt).format('DD/MM/YYYY HH:mm') : ""}
                </span>
            ),
            hideInSearch: true,
        },
        {

            title: 'Thao Tác',
            hideInSearch: true,
            width: 120,
            align: 'center',
            render: (_value, entity, _index, _action) => (
                <Space>
                    <Tooltip title="Xem chi tiết">
                        <EditOutlined
                            style={{
                                fontSize: 18,
                                color: '#2563eb',
                                cursor: 'pointer',
                                padding: 6,
                                borderRadius: 8,
                                background: '#eff6ff',
                                transition: 'all 0.2s',
                            }}
                            onClick={() => {
                                setOpenViewDetail(true);
                                setDataInit(entity);
                            }}
                        />
                    </Tooltip>
                    <Access permission={ALL_PERMISSIONS.RESUMES.DELETE} hideChildren>
                        <Popconfirm
                            placement="leftTop"
                            title="Xac nhan xoa"
                            description="Ban co chac chan muon xoa CV nay?"
                            onConfirm={() => handleDeleteResume(entity.id)}
                            okText="Xac nhan"
                            cancelText="Huy"
                            okButtonProps={{ danger: true }}
                        >
                            <Tooltip title="Xoa CV">
                                <DeleteOutlined
                                    style={{
                                        fontSize: 18,
                                        color: '#dc2626',
                                        cursor: 'pointer',
                                        padding: 6,
                                        borderRadius: 8,
                                        background: '#fee2e2',
                                        transition: 'all 0.2s',
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

        if (clone?.status?.length) {
            clone.filter = sfIn("status", clone.status).toString();
            delete clone.status;
        }

        clone.page = clone.current;
        clone.size = clone.pageSize;

        delete clone.current;
        delete clone.pageSize;

        let temp = queryString.stringify(clone);

        let sortBy = "";
        if (sort && sort.status) {
            sortBy = sort.status === 'ascend' ? "sort=status,asc" : "sort=status,desc";
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
            <Access
                permission={ALL_PERMISSIONS.RESUMES.GET_PAGINATE}
            >
                {/* Match Score Filter */}
                <div className="match-filter-bar">
                    <div className="match-filter-label">
                        <FilterOutlined />
                        Lọc theo Match Score:
                    </div>
                    <div style={{ flex: 1, maxWidth: 300 }}>
                        <Slider
                            range
                            value={matchFilter}
                            onChange={(value) => setMatchFilter(value as [number, number])}
                            min={0}
                            max={100}
                            tooltip={{
                                formatter: (value) => `${value}%`,
                            }}
                            marks={{
                                0: '0%',
                                50: '50%',
                                75: '75%',
                                100: '100%',
                            }}
                        />
                    </div>
                    <Tag
                        color={matchFilter[0] >= 75 ? 'success' : matchFilter[0] >= 50 ? 'warning' : 'default'}
                        style={{ borderRadius: 20, fontWeight: 600, fontSize: 12 }}
                    >
                        {matchFilter[0]}% — {matchFilter[1]}%
                    </Tag>
                </div>

                <DataTable<IResume>
                    actionRef={tableRef}
                    headerTitle="Quản Lý Hồ Sơ CV"
                    rowKey="id"
                    loading={isFetching}
                    columns={columns}
                    dataSource={filteredResumes}
                    request={async (params, sort, filter): Promise<any> => {
                        const query = buildQuery(params, sort, filter);
                        dispatch(fetchResume({ query }))
                    }}
                    scroll={{ x: true }}
                    pagination={
                        {
                            current: meta.page,
                            pageSize: meta.pageSize,
                            showSizeChanger: true,
                            total: meta.total,
                            showTotal: (total, range) => {
                                return (
                                    <span style={{ color: '#64748b', fontSize: 13 }}>
                                        Hiển thị <strong style={{ color: '#0f172a' }}>{range[0]}-{range[1]}</strong> trên <strong style={{ color: '#0f172a' }}>{total}</strong> hồ sơ
                                    </span>
                                )
                            }
                        }
                    }
                    rowSelection={false}
                    toolBarRender={(_action, _rows): any => {
                        return (
                            <></>
                        );
                    }}
                />
            </Access>
            <ViewDetailResume
                open={openViewDetail}
                onClose={setOpenViewDetail}
                dataInit={dataInit}
                setDataInit={setDataInit}
                reloadTable={reloadTable}
            />
        </div >
    )
}

export default ResumePage;
