import { ALL_PERMISSIONS } from "@/config/permissions";

type AdminPermission = {
  apiPath?: string;
  method?: string;
  module?: string;
};

const HR_ENTRY_PATHS = [
  {
    path: "/admin/company",
    permission: ALL_PERMISSIONS.COMPANIES.GET_PAGINATE,
  },
  {
    path: "/admin/job",
    permission: ALL_PERMISSIONS.JOBS.GET_PAGINATE,
  },
  {
    path: "/admin/resume",
    permission: ALL_PERMISSIONS.RESUMES.GET_PAGINATE,
  },
];

export const isHrRoleName = (roleName?: string) => {
  const normalizedRole = roleName?.trim().toUpperCase();
  return normalizedRole === "HR" || normalizedRole === "ROLE_HR";
};

export const hasAdminPermission = (
  permissions: AdminPermission[] | undefined,
  permission: AdminPermission,
) => {
  return permissions?.some(
    (item) =>
      item.apiPath === permission.apiPath &&
      item.method === permission.method &&
      item.module === permission.module,
  );
};

export const getFirstAllowedAdminPath = (
  permissions: AdminPermission[] | undefined,
  aclDisabled: boolean,
) => {
  if (aclDisabled) return HR_ENTRY_PATHS[0].path;

  return (
    HR_ENTRY_PATHS.find((item) =>
      hasAdminPermission(permissions, item.permission),
    )?.path ?? HR_ENTRY_PATHS[0].path
  );
};
