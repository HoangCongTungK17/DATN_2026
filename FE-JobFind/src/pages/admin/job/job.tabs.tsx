import { Tabs } from 'antd';
import type { TabsProps } from 'antd';
import {
    AppstoreOutlined,
    ThunderboltOutlined,
} from '@ant-design/icons';
import JobPage from './job';
import SkillPage from './skill';
import Access from '@/components/share/access';
import { ALL_PERMISSIONS } from '@/config/permissions';

const JobTabs = () => {
    const onChange = (key: string) => {
        // console.log(key);
    };

    const items: TabsProps['items'] = [
        {
            key: '1',
            label: (
                <span style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                    <AppstoreOutlined style={{ fontSize: 16 }} />
                    <span>Quản Lý Việc Làm</span>
                </span>
            ),
            children: <JobPage />,
        },
        {
            key: '2',
            label: (
                <span style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                    <ThunderboltOutlined style={{ fontSize: 16 }} />
                    <span>Quản Lý Kỹ Năng</span>
                </span>
            ),
            children: <SkillPage />,
        },
    ];

    return (
        <div className="admin-job-tabs-wrapper">
            <Access
                permission={ALL_PERMISSIONS.JOBS.GET_PAGINATE}
            >
                <Tabs
                    defaultActiveKey="1"
                    items={items}
                    onChange={onChange}
                    className="admin-premium-tabs"
                    tabBarStyle={{
                        marginBottom: 0,
                        padding: '0 4px',
                    }}
                />
            </Access>
        </div>
    );
}

export default JobTabs;