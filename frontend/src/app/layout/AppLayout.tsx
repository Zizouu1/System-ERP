import { useEffect, useState } from "react";
import { Outlet, useLocation } from "react-router-dom";
import { useAuth } from "@/app/providers/AuthProvider";
import Sidebar from "@/shared/ui/Sidebar/Sidebar";
import Topbar from "@/shared/ui/Topbar/Topbar";

export default function AppLayout() {
    const { role } = useAuth();
    const location = useLocation();
    const [_pageReady, setPageReady] = useState(false);
    const [sidebarWidth, setSidebarWidth] = useState(260);

    // Re-trigger any animations on route change
    useEffect(() => {
        setPageReady(false);
        const t = setTimeout(() => setPageReady(true), 10);
        return () => clearTimeout(t);
    }, [location.pathname]);

    if (!role) return null;

    return (
        <div className="shell">
            <Sidebar role={role} onWidthChange={setSidebarWidth} />

            <div
                className="content"
                style={{
                    marginLeft: sidebarWidth,
                    transition: "margin-left 180ms ease",
                }}
            >
                <Topbar />
                <main className="appMain">
                    <Outlet />
                </main>
            </div>
        </div>
    );
}
