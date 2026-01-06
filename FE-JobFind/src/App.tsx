import { useEffect, useRef, useState } from "react";
import {
  createBrowserRouter,
  Outlet,
  RouterProvider,
  useLocation,
} from "react-router-dom";
import { useAppDispatch, useAppSelector } from "@/redux/hooks";
import NotFound from "components/share/not.found";
import Loading from "components/share/loading";
import LoginPage from "pages/auth/login";
import RegisterPage from "pages/auth/register";
import LayoutAdmin from "components/admin/layout.admin";
import ProtectedRoute from "components/share/protected-route.ts";
import Header from "components/client/header.client";
import Footer from "components/client/footer.client";
import HomePage from "pages/home";
import styles from "styles/app.module.scss";
import DashboardPage from "./pages/admin/dashboard";
import CompanyPage from "./pages/admin/company";
import PermissionPage from "./pages/admin/permission";
import ResumePage from "./pages/admin/resume";
import RolePage from "./pages/admin/role";
import UserPage from "./pages/admin/user";
import { fetchAccount } from "./redux/slice/accountSlide";
import LayoutApp from "./components/share/layout.app";
import ViewUpsertJob from "./components/admin/job/upsert.job";
import ClientJobPage from "./pages/job";
import ClientJobDetailPage from "./pages/job/detail";
import ClientCompanyPage from "./pages/company";
import ClientCompanyDetailPage from "./pages/company/detail";
import JobTabs from "./pages/admin/job/job.tabs";
import { ConfigProvider, App as AntdApp } from "antd";
const THEME_CONFIG = {
  token: {
    // === COLOR SYSTEM ===
    colorPrimary: "#0A65CC", // Primary blue - professional & trustworthy
    colorSuccess: "#0BA02C", // Green for salary & success states
    colorWarning: "#F97316", // Orange for highlights & warnings
    colorError: "#DC2626", // Red for errors
    colorInfo: "#0EA5E9", // Sky blue for info

    // === TYPOGRAPHY ===
    fontFamily: "'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif",
    fontSize: 14,
    fontSizeHeading1: 38,
    fontSizeHeading2: 30,
    fontSizeHeading3: 24,
    fontSizeHeading4: 20,
    fontSizeHeading5: 16,

    // === SPACING & LAYOUT ===
    borderRadius: 8, // Default border radius
    borderRadiusLG: 12, // Large elements (cards, modals)
    borderRadiusSM: 6, // Small elements (tags, badges)

    // === COLORS - BACKGROUNDS ===
    colorBgLayout: "#F8F9FA", // Main app background (lighter gray)
    colorBgContainer: "#FFFFFF", // Card/container backgrounds
    colorBgElevated: "#FFFFFF", // Elevated elements (dropdowns, modals)

    // === COLORS - TEXT ===
    colorText: "#1F2937", // Primary text color (darker for better contrast)
    colorTextSecondary: "#6B7280", // Secondary text
    colorTextTertiary: "#9CA3AF", // Tertiary/placeholder text

    // === COLORS - BORDERS ===
    colorBorder: "#E5E7EB", // Default border
    colorBorderSecondary: "#F3F4F6", // Lighter borders

    // === SHADOWS ===
    boxShadow: "0 1px 2px 0 rgba(0, 0, 0, 0.05)",
    boxShadowSecondary: "0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06)",

    // === LINKS ===
    colorLink: "#0A65CC",
    colorLinkHover: "#084B94",
    colorLinkActive: "#063A75",

    // === LINE HEIGHT ===
    lineHeight: 1.5715,
    lineHeightHeading: 1.2,
  },
  components: {
    // === BUTTON COMPONENT ===
    Button: {
      controlHeight: 44,
      controlHeightLG: 52,
      controlHeightSM: 36,
      fontWeight: 600,
      borderRadius: 8,
      borderRadiusLG: 10,
      borderRadiusSM: 6,
      primaryShadow: "0 4px 12px rgba(10, 101, 204, 0.25)",
      // Gradient for primary button (applied via custom styles in components)
    },

    // === INPUT COMPONENT ===
    Input: {
      controlHeight: 44,
      controlHeightLG: 52,
      controlHeightSM: 36,
      borderRadius: 8,
      paddingBlock: 10,
      paddingInline: 14,
    },

    // === SELECT COMPONENT ===
    Select: {
      controlHeight: 44,
      borderRadius: 8,
    },

    // === CARD COMPONENT ===
    Card: {
      borderRadiusLG: 12,
      paddingLG: 24,
      boxShadowTertiary: "0 1px 2px 0 rgba(0, 0, 0, 0.05)",
    },

    // === TABLE COMPONENT ===
    Table: {
      borderRadius: 8,
      headerBg: "#F9FAFB",
      headerColor: "#374151",
      headerSplitColor: "#E5E7EB",
      rowHoverBg: "#F9FAFB",
      borderColor: "#E5E7EB",
    },

    // === MODAL COMPONENT ===
    Modal: {
      borderRadiusLG: 12,
      paddingContentHorizontalLG: 24,
    },

    // === MENU COMPONENT ===
    Menu: {
      itemBorderRadius: 8,
      itemHeight: 44,
      borderRadius: 8,
    },

    // === TAG COMPONENT ===
    Tag: {
      borderRadiusSM: 6,
    },

    // === PAGINATION ===
    Pagination: {
      borderRadius: 6,
    },
  },
};

const LayoutClient = () => {
  const [searchTerm, setSearchTerm] = useState("");
  const location = useLocation();
  const rootRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (rootRef && rootRef.current) {
      rootRef.current.scrollIntoView({ behavior: "smooth" });
    }
  }, [location]);

  return (
    <div className="layout-app" ref={rootRef}>
      <Header searchTerm={searchTerm} setSearchTerm={setSearchTerm} />
      <div className={styles["content-app"]}>
        <Outlet context={[searchTerm, setSearchTerm]} />
      </div>
      <Footer />
    </div>
  );
};

export default function App() {
  const dispatch = useAppDispatch();
  const isLoading = useAppSelector((state) => state.account.isLoading);

  useEffect(() => {
    if (
      window.location.pathname === "/login" ||
      window.location.pathname === "/register"
    )
      return;
    dispatch(fetchAccount());
  }, []);

  const router = createBrowserRouter([
    {
      path: "/",
      element: (
        <LayoutApp>
          <LayoutClient />
        </LayoutApp>
      ),
      errorElement: <NotFound />,
      children: [
        { index: true, element: <HomePage /> },
        { path: "job", element: <ClientJobPage /> },
        { path: "job/:id", element: <ClientJobDetailPage /> },
        { path: "company", element: <ClientCompanyPage /> },
        { path: "company/:id", element: <ClientCompanyDetailPage /> },
      ],
    },

    {
      path: "/admin",
      element: (
        <LayoutApp>
          <LayoutAdmin />{" "}
        </LayoutApp>
      ),
      errorElement: <NotFound />,
      children: [
        {
          index: true,
          element: (
            <ProtectedRoute>
              <DashboardPage />
            </ProtectedRoute>
          ),
        },
        {
          path: "company",
          element: (
            <ProtectedRoute>
              <CompanyPage />
            </ProtectedRoute>
          ),
        },
        {
          path: "user",
          element: (
            <ProtectedRoute>
              <UserPage />
            </ProtectedRoute>
          ),
        },

        {
          path: "job",
          children: [
            {
              index: true,
              element: (
                <ProtectedRoute>
                  <JobTabs />
                </ProtectedRoute>
              ),
            },
            {
              path: "upsert",
              element: (
                <ProtectedRoute>
                  <ViewUpsertJob />
                </ProtectedRoute>
              ),
            },
          ],
        },

        {
          path: "resume",
          element: (
            <ProtectedRoute>
              <ResumePage />
            </ProtectedRoute>
          ),
        },
        {
          path: "permission",
          element: (
            <ProtectedRoute>
              <PermissionPage />
            </ProtectedRoute>
          ),
        },
        {
          path: "role",
          element: (
            <ProtectedRoute>
              <RolePage />
            </ProtectedRoute>
          ),
        },
      ],
    },

    {
      path: "/login",
      element: <LoginPage />,
    },

    {
      path: "/register",
      element: <RegisterPage />,
    },
  ]);

  return (
    <ConfigProvider theme={THEME_CONFIG}>
      <RouterProvider router={router} />
    </ConfigProvider>
  );
}
