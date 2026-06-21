import { useEffect, useState } from "react";

// Breakpoint mặc định khớp với các @media (max-width: 768px) trong SCSS.
export const MOBILE_BREAKPOINT = 768;

const getMatches = (query: string): boolean => {
  if (typeof window === "undefined" || !window.matchMedia) {
    return false;
  }
  return window.matchMedia(query).matches;
};

/**
 * Phát hiện mobile theo BỀ RỘNG VIEWPORT (responsive), không dựa vào user-agent.
 * Tự cập nhật khi người dùng đổi kích thước cửa sổ / xoay màn hình.
 */
export const useIsMobile = (breakpoint: number = MOBILE_BREAKPOINT): boolean => {
  const query = `(max-width: ${breakpoint}px)`;
  const [isMobile, setIsMobile] = useState<boolean>(() => getMatches(query));

  useEffect(() => {
    if (typeof window === "undefined" || !window.matchMedia) {
      return;
    }
    const mql = window.matchMedia(query);
    const handler = (event: MediaQueryListEvent) => setIsMobile(event.matches);

    // Đồng bộ lại ngay (phòng trường hợp đổi giữa lần render đầu và effect)
    setIsMobile(mql.matches);

    if (mql.addEventListener) {
      mql.addEventListener("change", handler);
      return () => mql.removeEventListener("change", handler);
    }
    // Fallback cho Safari < 14
    mql.addListener(handler);
    return () => mql.removeListener(handler);
  }, [query]);

  return isMobile;
};

export default useIsMobile;
