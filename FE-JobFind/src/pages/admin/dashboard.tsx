import { Card, Col, Row, Statistic, Spin, Table } from "antd";
import CountUp from 'react-countup';
import {
    AppstoreOutlined,
    ShopOutlined,
    TeamOutlined,
    FileTextOutlined,
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
    Legend,
    ResponsiveContainer,
    PieChart,
    Pie,
    Cell,
    LineChart,
    Line,
} from "recharts";

const COLORS = ["#0088FE", "#00C49F", "#FFBB28", "#FF8042", "#8884D8", "#FF6B9D", "#C44569"];

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
        console.log("Starting to fetch dashboard data...");
        try {
            // Fetch all data concurrently with reduced size to prevent timeout
            const [jobsRes, companiesRes, usersRes, resumesRes] = await Promise.all([
                callFetchJob("page=1&size=100"),
                callFetchCompany("page=1&size=100"),
                callFetchUser("page=1&size=100"),
                callFetchResume("page=1&size=100"),
            ]);
            console.log("Data fetched successfully:", { jobsRes, companiesRes, usersRes, resumesRes });

            // Set statistics
            setStats({
                totalJobs: jobsRes?.data?.meta?.total || 0,
                totalCompanies: companiesRes?.data?.meta?.total || 0,
                totalUsers: usersRes?.data?.meta?.total || 0,
                totalResumes: resumesRes?.data?.meta?.total || 0,
            });

            // Process jobs by location
            if (jobsRes?.data?.result) {
                const locationCount: Record<string, number> = {};
                jobsRes.data.result.forEach((job: any) => {
                    const location = job.location || "Others";
                    const key = location.includes("Hà Nội") ? "Hà Nội" :
                        location.includes("Hồ Chí Minh") ? "TP. HCM" :
                            location.includes("Đà Nẵng") ? "Đà Nẵng" : "Khác";
                    locationCount[key] = (locationCount[key] || 0) + 1;
                });

                const locData = Object.entries(locationCount).map(([name, value]) => ({
                    name,
                    value,
                }));
                setLocationData(locData);

                // Process jobs by skills
                const skillCount: Record<string, number> = {};
                jobsRes.data.result.forEach((job: any) => {
                    if (job.skills && Array.isArray(job.skills)) {
                        job.skills.forEach((skill: any) => {
                            const skillName = skill.name || skill;
                            skillCount[skillName] = (skillCount[skillName] || 0) + 1;
                        });
                    }
                });

                const topSkills = Object.entries(skillCount)
                    .sort(([, a], [, b]) => (b as number) - (a as number))
                    .slice(0, 8)
                    .map(([name, value]) => ({ name, value }));
                setSkillsData(topSkills);

                // Process job status (active/inactive)
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

            // Process top companies by job count
            if (companiesRes?.data?.result && jobsRes?.data?.result) {
                const companyJobCount: Record<string, number> = {};
                jobsRes.data.result.forEach((job: any) => {
                    const companyName = job.company?.name || "Unknown";
                    companyJobCount[companyName] = (companyJobCount[companyName] || 0) + 1;
                });

                const topComps = Object.entries(companyJobCount)
                    .sort(([, a], [, b]) => (b as number) - (a as number))
                    .slice(0, 5)
                    .map(([name, value]) => ({ name, jobs: value }));
                setTopCompanies(topComps);
            }

            // Process resume status
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

                const statusData = Object.entries(statusCount).map(([name, value]) => ({
                    name,
                    value,
                }));
                setResumeStatusData(statusData);
            }

            // Process jobs over time (last 7 days)
            if (jobsRes?.data?.result) {
                const dailyJobs: Record<string, number> = {};
                const today = new Date();

                // Initialize last 7 days
                for (let i = 6; i >= 0; i--) {
                    const date = new Date(today);
                    date.setDate(date.getDate() - i);
                    const dateStr = date.toLocaleDateString("vi-VN", { month: "short", day: "numeric" });
                    dailyJobs[dateStr] = 0;
                }

                // Count jobs created in last 7 days
                jobsRes.data.result.forEach((job: any) => {
                    const createdDate = new Date(job.createdAt);
                    const dateStr = createdDate.toLocaleDateString("vi-VN", { month: "short", day: "numeric" });
                    if (dailyJobs.hasOwnProperty(dateStr)) {
                        dailyJobs[dateStr]++;
                    }
                });

                const timeData = Object.entries(dailyJobs).map(([date, count]) => ({
                    date,
                    jobs: count,
                }));
                setJobsOverTime(timeData);
            }
        } catch (error) {
            console.error("Error fetching dashboard data:", error);
            // Set empty data on error to prevent infinite loading
            setStats({ totalJobs: 0, totalCompanies: 0, totalUsers: 0, totalResumes: 0 });
        } finally {
            console.log("Dashboard data fetch completed");
            setLoading(false);
        }
    };

    const companyColumns = [
        {
            title: "Công ty",
            dataIndex: "name",
            key: "name",
        },
        {
            title: "Số việc làm",
            dataIndex: "jobs",
            key: "jobs",
            sorter: (a: any, b: any) => b.jobs - a.jobs,
        },
    ];

    return (
        <Spin spinning={loading} tip="Đang tải dữ liệu...">
            <div style={{ padding: "20px" }}>
                {/* Statistics Cards */}
                <Row gutter={[20, 20]}>
                    <Col span={24} md={6}>
                        <Card bordered={false} style={{ background: "linear-gradient(135deg, #667eea 0%, #764ba2 100%)", color: "white" }}>
                            <Statistic
                                title={<span style={{ color: "rgba(255,255,255,0.9)", fontSize: 14 }}>Tổng Công Việc</span>}
                                value={stats.totalJobs}
                                formatter={formatter}
                                valueStyle={{ color: "white", fontSize: 28, fontWeight: "bold" }}
                                prefix={<AppstoreOutlined style={{ fontSize: 24, marginRight: 8 }} />}
                            />
                        </Card>
                    </Col>
                    <Col span={24} md={6}>
                        <Card bordered={false} style={{ background: "linear-gradient(135deg, #f093fb 0%, #f5576c 100%)", color: "white" }}>
                            <Statistic
                                title={<span style={{ color: "rgba(255,255,255,0.9)", fontSize: 14 }}>Tổng Công Ty</span>}
                                value={stats.totalCompanies}
                                formatter={formatter}
                                valueStyle={{ color: "white", fontSize: 28, fontWeight: "bold" }}
                                prefix={<ShopOutlined style={{ fontSize: 24, marginRight: 8 }} />}
                            />
                        </Card>
                    </Col>
                    <Col span={24} md={6}>
                        <Card bordered={false} style={{ background: "linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)", color: "white" }}>
                            <Statistic
                                title={<span style={{ color: "rgba(255,255,255,0.9)", fontSize: 14 }}>Tổng Người Dùng</span>}
                                value={stats.totalUsers}
                                formatter={formatter}
                                valueStyle={{ color: "white", fontSize: 28, fontWeight: "bold" }}
                                prefix={<TeamOutlined style={{ fontSize: 24, marginRight: 8 }} />}
                            />
                        </Card>
                    </Col>
                    <Col span={24} md={6}>
                        <Card bordered={false} style={{ background: "linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)", color: "white" }}>
                            <Statistic
                                title={<span style={{ color: "rgba(255,255,255,0.9)", fontSize: 14 }}>Tổng Hồ Sơ</span>}
                                value={stats.totalResumes}
                                formatter={formatter}
                                valueStyle={{ color: "white", fontSize: 28, fontWeight: "bold" }}
                                prefix={<FileTextOutlined style={{ fontSize: 24, marginRight: 8 }} />}
                            />
                        </Card>
                    </Col>
                </Row>

                {/* Charts Row 1 */}
                <Row gutter={[20, 20]} style={{ marginTop: 20 }}>
                    {/* Jobs by Location */}
                    <Col span={24} md={12}>
                        <Card title="Công Việc Theo Địa Điểm" bordered={false}>
                            <ResponsiveContainer width="100%" height={300}>
                                <PieChart>
                                    <Pie
                                        data={locationData}
                                        cx="50%"
                                        cy="50%"
                                        labelLine={false}
                                        label={(entry) => `${entry.name}: ${entry.value}`}
                                        outerRadius={100}
                                        fill="#8884d8"
                                        dataKey="value"
                                    >
                                        {locationData.map((entry, index) => (
                                            <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                                        ))}
                                    </Pie>
                                    <Tooltip />
                                </PieChart>
                            </ResponsiveContainer>
                        </Card>
                    </Col>

                    {/* Resume Status */}
                    <Col span={24} md={12}>
                        <Card title="Trạng Thái Hồ Sơ" bordered={false}>
                            <ResponsiveContainer width="100%" height={300}>
                                <PieChart>
                                    <Pie
                                        data={resumeStatusData}
                                        cx="50%"
                                        cy="50%"
                                        labelLine={false}
                                        label={(entry) => `${entry.name}: ${entry.value}`}
                                        outerRadius={100}
                                        fill="#8884d8"
                                        dataKey="value"
                                    >
                                        {resumeStatusData.map((entry, index) => (
                                            <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                                        ))}
                                    </Pie>
                                    <Tooltip />
                                </PieChart>
                            </ResponsiveContainer>
                        </Card>
                    </Col>
                </Row>

                {/* Charts Row 2 */}
                <Row gutter={[20, 20]} style={{ marginTop: 20 }}>
                    {/* Top Skills */}
                    <Col span={24} md={12}>
                        <Card title="Kỹ Năng Được Yêu Cầu Nhiều Nhất" bordered={false}>
                            <ResponsiveContainer width="100%" height={300}>
                                <BarChart data={skillsData}>
                                    <CartesianGrid strokeDasharray="3 3" />
                                    <XAxis dataKey="name" angle={-45} textAnchor="end" height={100} />
                                    <YAxis />
                                    <Tooltip />
                                    <Bar dataKey="value" fill="#8884d8">
                                        {skillsData.map((entry, index) => (
                                            <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                                        ))}
                                    </Bar>
                                </BarChart>
                            </ResponsiveContainer>
                        </Card>
                    </Col>

                    {/* Job Status */}
                    <Col span={24} md={12}>
                        <Card title="Trạng Thái Công Việc" bordered={false}>
                            <ResponsiveContainer width="100%" height={300}>
                                <PieChart>
                                    <Pie
                                        data={jobStatusData}
                                        cx="50%"
                                        cy="50%"
                                        labelLine={false}
                                        label={(entry) => `${entry.name}: ${entry.value}`}
                                        outerRadius={100}
                                        fill="#8884d8"
                                        dataKey="value"
                                    >
                                        {jobStatusData.map((entry, index) => (
                                            <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                                        ))}
                                    </Pie>
                                    <Tooltip />
                                </PieChart>
                            </ResponsiveContainer>
                        </Card>
                    </Col>
                </Row>

                {/* Charts Row 3 */}
                <Row gutter={[20, 20]} style={{ marginTop: 20 }}>
                    {/* Jobs Over Time */}
                    <Col span={24} md={16}>
                        <Card title="Công Việc Được Đăng (7 Ngày Gần Đây)" bordered={false}>
                            <ResponsiveContainer width="100%" height={300}>
                                <LineChart data={jobsOverTime}>
                                    <CartesianGrid strokeDasharray="3 3" />
                                    <XAxis dataKey="date" />
                                    <YAxis />
                                    <Tooltip />
                                    <Legend />
                                    <Line type="monotone" dataKey="jobs" stroke="#8884d8" strokeWidth={2} />
                                </LineChart>
                            </ResponsiveContainer>
                        </Card>
                    </Col>

                    {/* Top Companies Table */}
                    <Col span={24} md={8}>
                        <Card title="Top 5 Công Ty" bordered={false}>
                            <Table
                                columns={companyColumns}
                                dataSource={topCompanies}
                                pagination={false}
                                size="small"
                                rowKey="name"
                            />
                        </Card>
                    </Col>
                </Row>
            </div>
        </Spin>
    );
};

export default DashboardPage;