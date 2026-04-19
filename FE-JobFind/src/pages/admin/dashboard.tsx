import { Card, Col, Row, Statistic, Spin, Table } from "antd";
import CountUp from 'react-countup';
import {
    ScheduleOutlined,
    BankOutlined,
    TeamOutlined,
    FileSearchOutlined,
    ArrowUpOutlined,
    TrophyOutlined,
} from "@ant-design/icons";
import { useEffect, useState } from "react";
import { callFetchJob, callFetchCompany, callFetchUser, callFetchResume } from "@/config/api";
import {
    BarChart,
    Bar,
    XAxis,
    YAxis,
    CartesianGrid,
    Tooltip,
    ResponsiveContainer,
    PieChart,
    Pie,
    Cell,
    LineChart,
    Line,
    Area,
    AreaChart,
} from "recharts";

const COLORS = ["#2563eb", "#7c3aed", "#f59e0b", "#ef4444", "#059669", "#ec4899", "#0ea5e9"];

const DashboardPage = () => {
    const [loading, setLoading] = useState(false);
    const [stats, setStats] = useState({
        totalJobs: 0,
        totalCompanies: 0,
        totalUsers: 0,
        totalResumes: 0,
    });
    const [locationData, setLocationData] = useState<any[]>([]);
    const [skillsData, setSkillsData] = useState<any[]>([]);
    const [resumeStatusData, setResumeStatusData] = useState<any[]>([]);
    const [topCompanies, setTopCompanies] = useState<any[]>([]);
    const [jobsOverTime, setJobsOverTime] = useState<any[]>([]);
    const [jobStatusData, setJobStatusData] = useState<any[]>([]);

    const formatter = (value: number | string) => {
        return <CountUp end={Number(value)} separator="," />;
    };

    useEffect(() => {
        fetchDashboardData();
    }, []);

    const fetchDashboardData = async () => {
        setLoading(true);
        try {
            const [jobsRes, companiesRes, usersRes, resumesRes] = await Promise.all([
                callFetchJob("page=1&size=100"),
                callFetchCompany("page=1&size=100"),
                callFetchUser("page=1&size=100"),
                callFetchResume("page=1&size=100"),
            ]);

            setStats({
                totalJobs: jobsRes?.data?.meta?.total || 0,
                totalCompanies: companiesRes?.data?.meta?.total || 0,
                totalUsers: usersRes?.data?.meta?.total || 0,
                totalResumes: resumesRes?.data?.meta?.total || 0,
            });

            if (jobsRes?.data?.result) {
                const locationCount: Record<string, number> = {};
                jobsRes.data.result.forEach((job: any) => {
                    const location = job.location || "Others";
                    const key = location.includes("Hà Nội") ? "Hà Nội" :
                        location.includes("Hồ Chí Minh") ? "TP. HCM" :
                            location.includes("Đà Nẵng") ? "Đà Nẵng" : "Khác";
                    locationCount[key] = (locationCount[key] || 0) + 1;
                });
                setLocationData(Object.entries(locationCount).map(([name, value]) => ({ name, value })));

                const skillCount: Record<string, number> = {};
                jobsRes.data.result.forEach((job: any) => {
                    if (job.skills && Array.isArray(job.skills)) {
                        job.skills.forEach((skill: any) => {
                            const skillName = skill.name || skill;
                            skillCount[skillName] = (skillCount[skillName] || 0) + 1;
                        });
                    }
                });
                setSkillsData(
                    Object.entries(skillCount)
                        .sort(([, a], [, b]) => (b as number) - (a as number))
                        .slice(0, 8)
                        .map(([name, value]) => ({ name, value }))
                );

                const statusCount = { "Đang tuyển": 0, "Hết hạn": 0 };
                const now = new Date();
                jobsRes.data.result.forEach((job: any) => {
                    const endDate = new Date(job.endDate);
                    if (endDate > now && job.active !== false) {
                        statusCount["Đang tuyển"]++;
                    } else {
                        statusCount["Hết hạn"]++;
                    }
                });
                setJobStatusData([
                    { name: "Đang tuyển", value: statusCount["Đang tuyển"] },
                    { name: "Hết hạn", value: statusCount["Hết hạn"] },
                ]);
            }

            if (companiesRes?.data?.result && jobsRes?.data?.result) {
                const companyJobCount: Record<string, number> = {};
                jobsRes.data.result.forEach((job: any) => {
                    const companyName = job.company?.name || "Unknown";
                    companyJobCount[companyName] = (companyJobCount[companyName] || 0) + 1;
                });
                setTopCompanies(
                    Object.entries(companyJobCount)
                        .sort(([, a], [, b]) => (b as number) - (a as number))
                        .slice(0, 5)
                        .map(([name, value], index) => ({ name, jobs: value, rank: index + 1 }))
                );
            }

            if (resumesRes?.data?.result) {
                const statusCount: Record<string, number> = {};
                resumesRes.data.result.forEach((resume: any) => {
                    const status = resume.status || "PENDING";
                    const statusMap: Record<string, string> = {
                        "PENDING": "Chờ duyệt",
                        "REVIEWING": "Đang xét",
                        "APPROVED": "Đã duyệt",
                        "REJECTED": "Từ chối"
                    };
                    const statusName = statusMap[status] || status;
                    statusCount[statusName] = (statusCount[statusName] || 0) + 1;
                });
                setResumeStatusData(Object.entries(statusCount).map(([name, value]) => ({ name, value })));
            }

            if (jobsRes?.data?.result) {
                const dailyJobs: Record<string, number> = {};
                const today = new Date();
                for (let i = 6; i >= 0; i--) {
                    const date = new Date(today);
                    date.setDate(date.getDate() - i);
                    const dateStr = date.toLocaleDateString("vi-VN", { month: "short", day: "numeric" });
                    dailyJobs[dateStr] = 0;
                }
                jobsRes.data.result.forEach((job: any) => {
                    const createdDate = new Date(job.createdAt);
                    const dateStr = createdDate.toLocaleDateString("vi-VN", { month: "short", day: "numeric" });
                    if (dailyJobs.hasOwnProperty(dateStr)) {
                        dailyJobs[dateStr]++;
                    }
                });
                setJobsOverTime(Object.entries(dailyJobs).map(([date, count]) => ({ date, jobs: count })));
            }
        } catch (error) {
            console.error("Error fetching dashboard data:", error);
            setStats({ totalJobs: 0, totalCompanies: 0, totalUsers: 0, totalResumes: 0 });
        } finally {
            setLoading(false);
        }
    };

    const companyColumns = [
        {
            title: "#",
            dataIndex: "rank",
            key: "rank",
            width: 50,
            render: (rank: number) => (
                <span style={{
                    width: 28, height: 28, borderRadius: 8,
                    display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
                    background: rank <= 3 ? 'linear-gradient(135deg, #f59e0b, #ef4444)' : '#f1f5f9',
                    color: rank <= 3 ? '#fff' : '#64748b',
                    fontWeight: 700, fontSize: 12,
                }}>
                    {rank}
                </span>
            ),
        },
        {
            title: "Công ty",
            dataIndex: "name",
            key: "name",
            render: (text: string) => (
                <span style={{ fontWeight: 600, color: '#0f172a' }}>{text}</span>
            ),
        },
        {
            title: "Việc làm",
            dataIndex: "jobs",
            key: "jobs",
            width: 80,
            align: 'center' as const,
            render: (jobs: number) => (
                <span style={{
                    background: '#eff6ff', color: '#2563eb', padding: '4px 12px',
                    borderRadius: 20, fontWeight: 700, fontSize: 13,
                }}>
                    {jobs}
                </span>
            ),
        },
    ];

    // Stat cards configuration
    const statCards = [
        {
            title: 'Việc Làm',
            value: stats.totalJobs,
            icon: <ScheduleOutlined />,
            gradient: 'linear-gradient(135deg, #2563eb 0%, #7c3aed 100%)',
            iconBg: 'rgba(255,255,255,0.2)',
        },
        {
            title: 'Công Ty',
            value: stats.totalCompanies,
            icon: <BankOutlined />,
            gradient: 'linear-gradient(135deg, #ec4899 0%, #f43f5e 100%)',
            iconBg: 'rgba(255,255,255,0.2)',
        },
        {
            title: 'Người Dùng',
            value: stats.totalUsers,
            icon: <TeamOutlined />,
            gradient: 'linear-gradient(135deg, #0ea5e9 0%, #06b6d4 100%)',
            iconBg: 'rgba(255,255,255,0.2)',
        },
        {
            title: 'Hồ Sơ CV',
            value: stats.totalResumes,
            icon: <FileSearchOutlined />,
            gradient: 'linear-gradient(135deg, #059669 0%, #10b981 100%)',
            iconBg: 'rgba(255,255,255,0.2)',
        },
    ];

    const CustomTooltip = ({ active, payload, label }: any) => {
        if (active && payload && payload.length) {
            return (
                <div style={{
                    background: '#0f172a', color: '#fff', padding: '10px 14px',
                    borderRadius: 10, fontSize: 13, boxShadow: '0 8px 24px rgba(0,0,0,0.2)',
                }}>
                    <div style={{ fontWeight: 600, marginBottom: 4 }}>{label}</div>
                    <div style={{ color: '#94a3b8' }}>
                        {payload[0].name}: <span style={{ color: '#fff', fontWeight: 700 }}>{payload[0].value}</span>
                    </div>
                </div>
            );
        }
        return null;
    };

    return (
        <Spin spinning={loading} tip="Đang tải dữ liệu...">
            <div>
                {/* Stat Cards */}
                <Row gutter={[20, 20]}>
                    {statCards.map((card, index) => (
                        <Col span={24} md={6} key={index}>
                            <Card
                                bordered={false}
                                className="admin-stat-card"
                                style={{ background: card.gradient }}
                            >
                                <div style={{
                                    width: 48, height: 48, borderRadius: 14,
                                    background: card.iconBg, display: 'flex',
                                    alignItems: 'center', justifyContent: 'center',
                                    fontSize: 22, color: '#fff', marginBottom: 16,
                                    backdropFilter: 'blur(8px)',
                                }}>
                                    {card.icon}
                                </div>
                                <Statistic
                                    title={<span style={{ color: 'rgba(255,255,255,0.8)', fontSize: 13, fontWeight: 500 }}>{card.title}</span>}
                                    value={card.value}
                                    formatter={formatter}
                                    valueStyle={{ color: '#fff', fontSize: 32, fontWeight: 800, lineHeight: 1.2 }}
                                />
                            </Card>
                        </Col>
                    ))}
                </Row>

                {/* Charts Row 1 */}
                <Row gutter={[20, 20]} style={{ marginTop: 20 }}>
                    <Col span={24} md={12}>
                        <Card
                            title={<span style={{ fontSize: 15 }}>📍 Việc Làm Theo Địa Điểm</span>}
                            bordered={false}
                        >
                            <ResponsiveContainer width="100%" height={300}>
                                <PieChart>
                                    <Pie
                                        data={locationData}
                                        cx="50%"
                                        cy="50%"
                                        innerRadius={60}
                                        outerRadius={100}
                                        paddingAngle={4}
                                        dataKey="value"
                                        label={({ name, value }) => `${name}: ${value}`}
                                        labelLine={false}
                                    >
                                        {locationData.map((entry, index) => (
                                            <Cell
                                                key={`cell-${index}`}
                                                fill={COLORS[index % COLORS.length]}
                                                stroke="none"
                                            />
                                        ))}
                                    </Pie>
                                    <Tooltip content={<CustomTooltip />} />
                                </PieChart>
                            </ResponsiveContainer>
                        </Card>
                    </Col>

                    <Col span={24} md={12}>
                        <Card
                            title={<span style={{ fontSize: 15 }}>📋 Trạng Thái Hồ Sơ</span>}
                            bordered={false}
                        >
                            <ResponsiveContainer width="100%" height={300}>
                                <PieChart>
                                    <Pie
                                        data={resumeStatusData}
                                        cx="50%"
                                        cy="50%"
                                        innerRadius={60}
                                        outerRadius={100}
                                        paddingAngle={4}
                                        dataKey="value"
                                        label={({ name, value }) => `${name}: ${value}`}
                                        labelLine={false}
                                    >
                                        {resumeStatusData.map((entry, index) => (
                                            <Cell
                                                key={`cell-${index}`}
                                                fill={COLORS[index % COLORS.length]}
                                                stroke="none"
                                            />
                                        ))}
                                    </Pie>
                                    <Tooltip content={<CustomTooltip />} />
                                </PieChart>
                            </ResponsiveContainer>
                        </Card>
                    </Col>
                </Row>

                {/* Charts Row 2 */}
                <Row gutter={[20, 20]} style={{ marginTop: 20 }}>
                    <Col span={24} md={12}>
                        <Card
                            title={<span style={{ fontSize: 15 }}>🔥 Kỹ Năng Được Yêu Cầu Nhiều Nhất</span>}
                            bordered={false}
                        >
                            <ResponsiveContainer width="100%" height={300}>
                                <BarChart data={skillsData} barSize={28}>
                                    <CartesianGrid strokeDasharray="3 3" stroke="#f1f5f9" />
                                    <XAxis
                                        dataKey="name"
                                        angle={-45}
                                        textAnchor="end"
                                        height={80}
                                        tick={{ fontSize: 11, fill: '#64748b' }}
                                    />
                                    <YAxis tick={{ fontSize: 11, fill: '#64748b' }} />
                                    <Tooltip content={<CustomTooltip />} />
                                    <Bar dataKey="value" radius={[6, 6, 0, 0]}>
                                        {skillsData.map((entry, index) => (
                                            <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                                        ))}
                                    </Bar>
                                </BarChart>
                            </ResponsiveContainer>
                        </Card>
                    </Col>

                    <Col span={24} md={12}>
                        <Card
                            title={<span style={{ fontSize: 15 }}>📊 Trạng Thái Công Việc</span>}
                            bordered={false}
                        >
                            <ResponsiveContainer width="100%" height={300}>
                                <PieChart>
                                    <Pie
                                        data={jobStatusData}
                                        cx="50%"
                                        cy="50%"
                                        innerRadius={60}
                                        outerRadius={100}
                                        paddingAngle={4}
                                        dataKey="value"
                                        label={({ name, value }) => `${name}: ${value}`}
                                        labelLine={false}
                                    >
                                        {jobStatusData.map((entry, index) => (
                                            <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} stroke="none" />
                                        ))}
                                    </Pie>
                                    <Tooltip content={<CustomTooltip />} />
                                </PieChart>
                            </ResponsiveContainer>
                        </Card>
                    </Col>
                </Row>

                {/* Charts Row 3 */}
                <Row gutter={[20, 20]} style={{ marginTop: 20 }}>
                    <Col span={24} md={16}>
                        <Card
                            title={<span style={{ fontSize: 15 }}>📈 Việc Làm Được Đăng (7 Ngày)</span>}
                            bordered={false}
                        >
                            <ResponsiveContainer width="100%" height={300}>
                                <AreaChart data={jobsOverTime}>
                                    <defs>
                                        <linearGradient id="colorJobs" x1="0" y1="0" x2="0" y2="1">
                                            <stop offset="5%" stopColor="#2563eb" stopOpacity={0.15} />
                                            <stop offset="95%" stopColor="#2563eb" stopOpacity={0} />
                                        </linearGradient>
                                    </defs>
                                    <CartesianGrid strokeDasharray="3 3" stroke="#f1f5f9" />
                                    <XAxis dataKey="date" tick={{ fontSize: 12, fill: '#64748b' }} />
                                    <YAxis tick={{ fontSize: 12, fill: '#64748b' }} />
                                    <Tooltip content={<CustomTooltip />} />
                                    <Area
                                        type="monotone"
                                        dataKey="jobs"
                                        stroke="#2563eb"
                                        strokeWidth={3}
                                        fill="url(#colorJobs)"
                                        dot={{ r: 5, fill: '#2563eb', strokeWidth: 2, stroke: '#fff' }}
                                        activeDot={{ r: 7, fill: '#2563eb', strokeWidth: 2, stroke: '#fff' }}
                                    />
                                </AreaChart>
                            </ResponsiveContainer>
                        </Card>
                    </Col>

                    <Col span={24} md={8}>
                        <Card
                            title={
                                <span style={{ fontSize: 15, display: 'flex', alignItems: 'center', gap: 6 }}>
                                    <TrophyOutlined style={{ color: '#f59e0b' }} /> Top 5 Công Ty
                                </span>
                            }
                            bordered={false}
                        >
                            <Table
                                columns={companyColumns}
                                dataSource={topCompanies}
                                pagination={false}
                                size="small"
                                rowKey="name"
                                showHeader={false}
                            />
                        </Card>
                    </Col>
                </Row>
            </div>
        </Spin>
    );
};

export default DashboardPage;