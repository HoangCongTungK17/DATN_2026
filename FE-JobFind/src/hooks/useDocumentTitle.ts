import { useEffect } from 'react';

/**
 * Hook để set document title động cho mỗi trang.
 * SEO-friendly: mỗi trang có title riêng.
 * 
 * @param title - Tiêu đề trang, sẽ được append "| JobFind"
 * @param restoreOnUnmount - Có restore title cũ khi unmount không (default: true)
 */
export const useDocumentTitle = (title: string, restoreOnUnmount = true) => {
    useEffect(() => {
        const previousTitle = document.title;
        document.title = title ? `${title} | JobFind` : 'JobFind — Nền tảng tìm việc IT hàng đầu Việt Nam';

        return () => {
            if (restoreOnUnmount) {
                document.title = previousTitle;
            }
        };
    }, [title, restoreOnUnmount]);
};
