// Script to restore the premium UI on Admin Tables
const fs = require('fs');
const path = require('path');

const files = [
    {
        path: 'FE-JobFind/src/pages/admin/job/job.tsx',
        title: 'Quản Lý Vị Trí Tuyển Dụng',
    },
    {
        path: 'FE-JobFind/src/pages/admin/resume.tsx',
        title: 'Quản Lý Hồ Sơ (CV)',
    },
    {
        path: 'FE-JobFind/src/pages/admin/role.tsx',
        title: 'Quản Lý Phân Quyền (Roles)',
    },
    {
        path: 'FE-JobFind/src/pages/admin/permission.tsx',
        title: 'Quản Lý Phân Quyền Chi Tiết (Permissions)',
    },
    {
        path: 'FE-JobFind/src/pages/admin/company.tsx',
        title: 'Cơ Sở Dữ Liệu Công Ty',
    },
    {
        path: 'FE-JobFind/src/pages/admin/user.tsx',
        title: 'Quản Trị Viên & Người Dùng',
    }
];

files.forEach(f => {
    const fullPath = path.resolve(__dirname, f.path);
    if (!fs.existsSync(fullPath)) return;
    
    let content = fs.readFileSync(fullPath, 'utf8');

    // 1. Add Card, Tooltip to antd imports
    if (content.includes('from "antd"')) {
        content = content.replace(/(import \{[^\}]+)(\} from "antd";)/, (match, p1, p2) => {
            if (!p1.includes('Card')) p1 += ', Card';
            if (!p1.includes('Tooltip')) p1 += ', Tooltip';
            return p1 + p2;
        });
    }

    // 2. Wrap DataTable in Card
    content = content.replace(/return \(\s*<div>/g, `return (\n        <Card bordered={false} style={{ borderRadius: 12, boxShadow: '0 2px 8px rgba(0,0,0,0.05)' }}>`);
    content = content.replace(/<\/\s*div\s*>\s*\n\s*\)\n\}/g, `</Card>\n    )\n}`);

    // Fix headerTitle
    content = content.replace(/headerTitle="[^"]+"/g, `headerTitle={<span style={{ fontSize: 18, fontWeight: 600 }}>${f.title}</span>}`);

    // Add gradient to Thêm mới button
    content = content.replace(/<Button\s+icon=\{<PlusOutlined \/>\}\s+type="primary"/g, `<Button\n                                icon={<PlusOutlined />}\n                                type="primary"\n                                style={{ borderRadius: 8, background: 'linear-gradient(135deg, #4f46e5, #7c3aed)', border: 'none' }}`);


    // Change Actions Column
    let inActions = false;
    let newContent = [];
    const lines = content.split('\n');
    for (let i = 0; i < lines.length; i++) {
        let line = lines[i];
        
        // Improve DeleteOutlined
        if (line.includes('<DeleteOutlined')) {
            line = `                                <Tooltip title="Xóa">\n                                    <Button type="text" icon={<DeleteOutlined style={{ color: '#ef4444', fontSize: 18 }} />} />\n                                </Tooltip>`;
            // Skip the next 6 lines of style for DeleteOutlined
            if (lines[i+1].includes('style=')) {
                i += 6;
            }
            if (lines[i+1].includes('fontSize: 20')) {
                 i+=4;
            }
        }

        // Improve EditOutlined
        if (line.includes('<EditOutlined') && !line.includes('button')) {
             // Just match the basic icon
        }

        newContent.push(line);
    }
    
    fs.writeFileSync(fullPath, content, 'utf8');
});

console.log("Restored standard Admin tables");
