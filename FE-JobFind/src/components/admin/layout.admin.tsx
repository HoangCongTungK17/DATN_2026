import React, { useState, useEffect } from 'react';
import {
    DashboardOutlined,
    BankOutlined,
    UserOutlined,
    ScheduleOutlined,
    FileSearchOutlined,
    KeyOutlined,
    SafetyCertificateOutlined,
    MenuFoldOutlined,
    MenuUnfoldOutlined,
    LogoutOutlined,
    HomeOutlined,
    ThunderboltFilled,
    BellOutlined,
    CaretDownOutlined,
    AppstoreOutlined,
    TeamOutlined,
    SolutionOutlined,
    LockOutlined,
} from '@ant-design/icons';
import { Layout, Menu, Dropdown, Space, message, Avatar, Button, Badge, Tooltip } from 'antd';
import { Outlet, useLocation, useNavigate } from "react-router-dom";
import { Link } from 'react-router-dom';
import { callLogout } from 'config/api';
import { useAppDispatch, useAppSelector } from '@/redux/hooks';
import { isMobile } from 'react-device-detect';
import type { MenuProps } from 'antd';
import { setLogoutAction } from '@/redux/slice/accountSlide';
import { ALL_PERMISSIONS } from '@/config/permissions';
import './admin.scss';

const { Content, Sider } = Layout;

const LayoutAdmin = () => {
    const location = useLocation();

    const [collapsed, setCollapsed] = useState(false);
    const [activeMenu, setActiveMenu] = useState('');
    const user = useAppSelector(state => state.account.user);

    const permissions = useAppSelector(state => state.account.user.role.permissions);
    const [menuItems, setMenuItems] = useState<MenuProps['items']>([]);

    const navigate = useNavigate();
    const dispatch = useAppDispatch();

    useEffect(() => {
        const ACL_ENABLE = import.meta.env.VITE_ACL_ENABLE;
        if (permissions?.length || ACL_ENABLE === 'false') {

            const viewCompany = permissions?.find(item =>
                item.apiPath === ALL_PERMISSIONS.COMPANIES.GET_PAGINATE.apiPath
                && item.method === ALL_PERMISSIONS.COMPANIES.GET_PAGINATE.method
            )

            const viewUser = permissions?.find(item =>
                item.apiPath === ALL_PERMISSIONS.USERS.GET_PAGINATE.apiPath
                && item.method === ALL_PERMISSIONS.USERS.GET_PAGINATE.method
            )

            const viewJob = permissions?.find(item =>
                item.apiPath === ALL_PERMISSIONS.JOBS.GET_PAGINATE.apiPath
                && item.method === ALL_PERMISSIONS.JOBS.GET_PAGINATE.method
            )

            const viewResume = permissions?.find(item =>
                item.apiPath === ALL_PERMISSIONS.RESUMES.GET_PAGINATE.apiPath
                && item.method === ALL_PERMISSIONS.RESUMES.GET_PAGINATE.method
            )

            const viewRole = permissions?.find(item =>
                item.apiPath === ALL_PERMISSIONS.ROLES.GET_PAGINATE.apiPath
                && item.method === ALL_PERMISSIONS.ROLES.GET_PAGINATE.method
            )

            const viewPermission = permissions?.find(item =>
                item.apiPath === ALL_PERMISSIONS.PERMISSIONS.GET_PAGINATE.apiPath
                && item.method === ALL_PERMISSIONS.USERS.GET_PAGINATE.method
            )

            const full = [
                {
                    label: <Link to='/admin'>Tổng Quan</Link>,
                    key: '/admin',
                    icon: <AppstoreOutlined />
                },

                // Group: Quản lý
                {
                    type: 'group' as const,
                    label: !collapsed ? 'QUẢN LÝ' : '—',
                    children: [
                        ...(viewCompany || ACL_ENABLE === 'false' ? [{
                            label: <Link to='/admin/company'>Công Ty</Link>,
                            key: '/admin/company',
                            icon: <BankOutlined />,
                        }] : []),
                        ...(viewUser || ACL_ENABLE === 'false' ? [{
                            label: <Link to='/admin/user'>Người Dùng</Link>,
                            key: '/admin/user',
                            icon: <TeamOutlined />
                        }] : []),
                        ...(viewJob || ACL_ENABLE === 'false' ? [{
                            label: <Link to='/admin/job'>Việc Làm</Link>,
                            key: '/admin/job',
                            icon: <SolutionOutlined />
                        }] : []),
                        ...(viewResume || ACL_ENABLE === 'false' ? [{
                            label: <Link to='/admin/resume'>Hồ Sơ CV</Link>,
                            key: '/admin/resume',
                            icon: <FileSearchOutlined />
                        }] : []),
                    ],
                },

                // Group: Hệ thống
                {
                    type: 'group' as const,
                    label: !collapsed ? 'HỆ THỐNG' : '—',
                    children: [
                        ...(viewPermission || ACL_ENABLE === 'false' ? [{
                            label: <Link to='/admin/permission'>Quyền Hạn</Link>,
                            key: '/admin/permission',
                            icon: <LockOutlined />
                        }] : []),
                        ...(viewRole || ACL_ENABLE === 'false' ? [{
                            label: <Link to='/admin/role'>Vai Trò</Link>,
                            key: '/admin/role',
                            icon: <SafetyCertificateOutlined />
                        }] : []),
                    ],
                },
            ];

            setMenuItems(full);
        }
    }, [permissions, collapsed])

    useEffect(() => {
        setActiveMenu(location.pathname)
    }, [location])

    const handleLogout = async () => {
        const res = await callLogout();
        if (res && +res.statusCode === 200) {
            dispatch(setLogoutAction({}));
            message.success('Đăng xuất thành công');
            navigate('/')
        }
    }

    const itemsDropdown = [
        {
            label: (
                <div style={{ display: 'flex', alignItems: 'center', gap: 8, padding: '4px 0' }}>
                    <HomeOutlined style={{ color: '#2563eb' }} />
                    <span>Về Trang Chủ</span>
                </div>
            ),
            key: 'home',
            onClick: () => navigate('/'),
        },
        {
            type: 'divider' as const,
        },
        {
            label: (
                <div style={{ display: 'flex', alignItems: 'center', gap: 8, padding: '4px 0', color: '#dc2626' }}>
                    <LogoutOutlined />
                    <span>Đăng xuất</span>
                </div>
            ),
            key: 'logout',
            onClick: () => handleLogout(),
        },
    ];

    // Get page config based on active menu
    const getPageConfig = () => {
        const configs: Record<string, {
            title: string;
            description: string;
            icon: React.ReactNode;
            gradient: string;
            iconBg: string;
        }> = {
            '/admin': {
                title: 'Tổng Quan',
                description: 'Thống kê & phân tích dữ liệu hệ thống',
                icon: <AppstoreOutlined />,
                gradient: 'linear-gradient(135deg, #6366f1, #818cf8)',
                iconBg: '#eef2ff',
            },
            '/admin/company': {
                title: 'Quản Lý Công Ty',
                description: 'Quản lý thông tin và danh sách đối tác tuyển dụng',
                icon: <BankOutlined />,
                gradient: 'linear-gradient(135deg, #ec4899, #f472b6)',
                iconBg: '#fdf2f8',
            },
            '/admin/user': {
                title: 'Quản Lý Người Dùng',
                description: 'Quản lý tài khoản, phân quyền người dùng',
                icon: <TeamOutlined />,
                gradient: 'linear-gradient(135deg, #0ea5e9, #38bdf8)',
                iconBg: '#e0f2fe',
            },
            '/admin/job': {
                title: 'Quản Lý Việc Làm',
                description: 'Quản lý tin tuyển dụng, vị trí đang mở',
                icon: <SolutionOutlined />,
                gradient: 'linear-gradient(135deg, #f59e0b, #fbbf24)',
                iconBg: '#fef3c7',
            },
            '/admin/resume': {
                title: 'Quản Lý Hồ Sơ CV',
                description: 'Theo dõi hồ sơ ứng viên, đánh giá & phân loại',
                icon: <FileSearchOutlined />,
                gradient: 'linear-gradient(135deg, #059669, #34d399)',
                iconBg: '#ecfdf5',
            },
            '/admin/permission': {
                title: 'Quản Lý Quyền Hạn',
                description: 'Cấu hình quyền truy cập API & tính năng',
                icon: <LockOutlined />,
                gradient: 'linear-gradient(135deg, #8b5cf6, #a78bfa)',
                iconBg: '#f5f3ff',
            },
            '/admin/role': {
                title: 'Quản Lý Vai Trò',
                description: 'Thiết lập vai trò & gán nhóm quyền hạn',
                icon: <SafetyCertificateOutlined />,
                gradient: 'linear-gradient(135deg, #dc2626, #f87171)',
                iconBg: '#fee2e2',
            },
        };
        return configs[activeMenu] || configs['/admin'];
    };

    return (
        <>
            <Layout
                style={{ minHeight: '100vh' }}
                className="admin-layout-premium"
            >
                {!isMobile ?
                    <Sider
                        theme='dark'
                        collapsible
                        collapsed={collapsed}
                        onCollapse={(value) => setCollapsed(value)}
                        className="admin-sidebar-dark"
                        width={264}
                        collapsedWidth={80}
                        trigger={null}
                    >
                        {/* Sidebar Logo */}
                        <div className="sidebar-logo-dark" onClick={() => navigate('/admin')}>
                            <div className="logo-icon-wrapper">
                                <ThunderboltFilled />
                            </div>
                            {!collapsed && (
                                <span className="logo-text">
                                    JOB<span className="logo-highlight">FIND</span>
                                    <span className="logo-badge">ADMIN</span>
                                </span>
                            )}
                        </div>

                        {/* Sidebar Menu */}
                        <div className="sidebar-menu-wrapper">
                            <Menu
                                selectedKeys={[activeMenu]}
                                mode="inline"
                                items={menuItems}
                                onClick={(e) => setActiveMenu(e.key)}
                                className="admin-menu-dark"
                                theme="dark"
                            />
                        </div>

                        {/* Sidebar Footer — User Info */}
                        <div className="sidebar-footer-dark">
                            {!collapsed ? (
                                <div className="sidebar-user-card-dark" onClick={() => navigate('/')}>
                                    <Avatar
                                        size={38}
                                        style={{
                                            background: 'linear-gradient(135deg, #6366f1, #a855f7)',
                                            fontWeight: 700,
                                            fontSize: 13,
                                            flexShrink: 0,
                                        }}
                                    >
                                        {user?.name?.substring(0, 2)?.toUpperCase()}
                                    </Avatar>
                                    <div className="user-meta">
                                        <div className="user-name">{user?.name}</div>
                                        <div className="user-role">{user?.role?.name || 'Administrator'}</div>
                                    </div>
                                    <Tooltip title="Đăng xuất">
                                        <LogoutOutlined
                                            className="logout-icon"
                                            onClick={(e) => {
                                                e.stopPropagation();
                                                handleLogout();
                                            }}
                                        />
                                    </Tooltip>
                                </div>
                            ) : (
                                <Tooltip title="Đăng xuất" placement="right">
                                    <div className="sidebar-collapsed-logout" onClick={() => handleLogout()}>
                                        <LogoutOutlined />
                                    </div>
                                </Tooltip>
                            )}
                        </div>
                    </Sider>
                    :
                    <Menu
                        selectedKeys={[activeMenu]}
                        items={menuItems}
                        onClick={(e) => setActiveMenu(e.key)}
                        mode="horizontal"
                    />
                }

                <Layout>
                    {!isMobile &&
                        <div className='admin-header-premium'>
                            <div className="admin-header-left">
                                <Button
                                    type="text"
                                    icon={collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
                                    onClick={() => setCollapsed(!collapsed)}
                                    className="collapse-btn"
                                />

                                {/* Page Banner */}
                                <div className="page-banner">
                                    <div
                                        className="page-banner-icon"
                                        style={{ background: getPageConfig().gradient }}
                                    >
                                        {getPageConfig().icon}
                                    </div>
                                    <div className="page-banner-text">
                                        <h2 className="page-banner-title">{getPageConfig().title}</h2>
                                        <p className="page-banner-desc">{getPageConfig().description}</p>
                                    </div>
                                </div>
                            </div>

                            <div className="admin-header-right">
                                <Tooltip title="Về trang chủ">
                                    <Button
                                        type="text"
                                        icon={<HomeOutlined />}
                                        onClick={() => navigate('/')}
                                        className="header-action-btn"
                                    />
                                </Tooltip>

                                <div className="header-divider" />

                                <Dropdown menu={{ items: itemsDropdown }} trigger={['click']} placement="bottomRight">
                                    <Space className="admin-user-dropdown" style={{ cursor: "pointer" }}>
                                        <Avatar
                                            size={36}
                                            style={{
                                                background: 'linear-gradient(135deg, #6366f1, #a855f7)',
                                                fontWeight: 700,
                                                fontSize: 13,
                                            }}
                                        >
                                            {user?.name?.substring(0, 2)?.toUpperCase()}
                                        </Avatar>
                                        <div className="admin-user-text">
                                            <span className="admin-user-name">{user?.name}</span>
                                            <span className="admin-user-role">{user?.role?.name || 'Admin'}</span>
                                        </div>
                                        <CaretDownOutlined style={{ fontSize: 10, color: '#94a3b8' }} />
                                    </Space>
                                </Dropdown>
                            </div>
                        </div>
                    }
                    <Content className="admin-content-premium">
                        <Outlet />
                    </Content>
                </Layout>
            </Layout>

        </>
    );
};

export default LayoutAdmin;