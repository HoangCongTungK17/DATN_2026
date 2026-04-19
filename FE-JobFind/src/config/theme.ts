import type { ThemeConfig } from 'antd';

export const themeConfig: ThemeConfig = {
  token: {
    colorPrimary: "#4f46e5",
    colorSuccess: "#10b981",
    colorWarning: "#f97316",
    colorError: "#ef4444",
    colorInfo: "#0ea5e9",

    fontFamily: "'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif",
    fontSize: 14,
    fontSizeHeading1: 38,
    fontSizeHeading2: 30,
    fontSizeHeading3: 24,
    fontSizeHeading4: 20,
    fontSizeHeading5: 16,

    borderRadius: 8,
    borderRadiusLG: 12,
    borderRadiusSM: 6,

    colorBgLayout: "#f8fafc",
    colorBgContainer: "#ffffff",
    colorBgElevated: "#ffffff",

    colorText: "#0f172a",
    colorTextSecondary: "#64748b",
    colorTextTertiary: "#94a3b8",

    colorBorder: "#e2e8f0",
    colorBorderSecondary: "#f1f5f9",

    boxShadow: "0 1px 3px rgba(0, 0, 0, 0.08)",
    boxShadowSecondary: "0 4px 12px rgba(0, 0, 0, 0.08)",

    colorLink: "#4f46e5",
    colorLinkHover: "#4338ca",
    colorLinkActive: "#3730a3",

    lineHeight: 1.6,
    lineHeightHeading1: 1.2,
    lineHeightHeading2: 1.2,
    lineHeightHeading3: 1.3,
  },
  components: {
    Button: {
      controlHeight: 44,
      controlHeightLG: 52,
      controlHeightSM: 36,
      fontWeight: 600,
      borderRadius: 8,
      borderRadiusLG: 10,
      borderRadiusSM: 6,
      primaryShadow: "0 4px 12px rgba(79, 70, 229, 0.25)",
    },
    Input: {
      controlHeight: 44,
      controlHeightLG: 52,
      controlHeightSM: 36,
      borderRadius: 8,
      paddingBlock: 10,
      paddingInline: 14,
    },
    Select: {
      controlHeight: 44,
      borderRadius: 8,
    },
    Card: {
      borderRadiusLG: 12,
      paddingLG: 24,
      boxShadowTertiary: "0 1px 2px 0 rgba(0, 0, 0, 0.05)",
    },
    Table: {
      borderRadius: 8,
      headerBg: "#F9FAFB",
      headerColor: "#374151",
      headerSplitColor: "#E5E7EB",
      rowHoverBg: "#F9FAFB",
      borderColor: "#E5E7EB",
    },
    Modal: {
      borderRadiusLG: 12,
      paddingContentHorizontalLG: 24,
    },
    Menu: {
      itemBorderRadius: 8,
      itemHeight: 44,
      borderRadius: 8,
    },
    Tag: {
      borderRadiusSM: 6,
    },
    Pagination: {
      borderRadius: 6,
    },
  },
};
