import { Navigate, Outlet } from "react-router-dom";
import { useAuth } from "@/app/providers/AuthProvider";
import type { Role } from "@/shared/ui/Sidebar/menu";

type Props = {
    /** If provided, also checks that the user has one of these roles */
    roles?: Role[];
};

/**
 * Combines RequireAuth + RequireRole:
 *  - Not logged in          → /login
 *  - Logged in, wrong role  → /unauthorized
 *  - Logged in, right role  → <Outlet />
 */
export default function ProtectedRoute({ roles }: Props) {
    const { token, role, isInitializing } = useAuth();

    if (isInitializing) {
        return null; // or a loading spinner
    }

    if (!token || !role) {
        return <Navigate to="/login" replace />;
    }

    if (roles && !roles.includes(role)) {
        return <Navigate to="/unauthorized" replace />;
    }


    return <Outlet />;
}
