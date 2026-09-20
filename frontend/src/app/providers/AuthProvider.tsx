import React, {
  createContext,
  useContext,
  useEffect,
  useMemo,
  useState,
} from "react";
import { useNavigate } from "react-router-dom";
import {
  decodeJwtPayload,
  isExpired,
  toSidebarRole,
  getHomePath,
} from "@/shared/utils/jwt";
import { setAuthToken, setAuthFailureHandler } from "@/shared/api/http";
import type { JwtRole } from "@/shared/utils/jwt";
import type { Role } from "@/shared/ui/Sidebar/menu";
import { login as loginApi } from "@/shared/api/endpoints";
import { clearLatestQrLabel } from "@/utils/qr";

type User = {
  username: string;
  jwtRole: JwtRole;
  role: Role; // lowercase sidebar role
  firstname?: string;
  lastname?: string;
  matricule?: string;
};

type AuthState = {
  token: string | null;
  user: User | null;
  isInitializing: boolean;
};

type AuthContextValue = {
  token: string | null;
  user: User | null;
  role: Role | null;
  jwtRole: JwtRole | null;
  isInitializing: boolean;
  matricule: string | null;
  login: (username: string, password: string) => Promise<void>;
  logout: () => void;
  sessionExpired: () => void;
};

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const navigate = useNavigate();
  const [state, setState] = useState<AuthState>(() => {
    const token = localStorage.getItem("auth_token");
    return {
      token,
      user: null,
      isInitializing: !!token,
    };
  });

  // Register auth failure handler for HTTP layer
  useEffect(() => {
    setAuthFailureHandler(() => {
      handleLogout("Votre session a expiré, veuillez vous reconnecter.");
    });
  }, []);

  // Handle token changes and initialization
  useEffect(() => {
    const { token } = state;

    if (!token) {
      setAuthToken(null);
      setState(prev => ({ ...prev, user: null, isInitializing: false }));
      return;
    }

    if (isExpired(token)) {
      handleLogout();
      return;
    }

    const payload = decodeJwtPayload(token);
    const sidebarRole = payload?.role ? toSidebarRole(payload.role as string) : null;

    if (!payload?.sub || !sidebarRole) {
      handleLogout();
      return;
    }

    const user: User = {
      username: payload.sub,
      jwtRole: payload.role as JwtRole,
      role: sidebarRole,
      firstname: payload.firstname as string | undefined,
      lastname: payload.lastname as string | undefined,
      matricule: payload.matricule as string | undefined,
    };

    setAuthToken(token);
    localStorage.setItem("auth_token", token);
    setState(prev => ({ ...prev, user, isInitializing: false }));
  }, [state.token]);

  async function login(username: string, password: string) {
    const res = await loginApi(username, password);
    const token = res.token;
    const payload = decodeJwtPayload(token);
    const sidebarRole = payload?.role ? toSidebarRole(payload.role as string) : null;

    if (!payload?.sub || !sidebarRole) {
      throw new Error("Invalid token payload: missing username or role");
    }

    const user: User = {
      username: payload.sub,
      jwtRole: payload.role as JwtRole,
      role: sidebarRole,
      firstname: payload.firstname as string | undefined,
      lastname: payload.lastname as string | undefined,
      matricule: payload.matricule as string | undefined,
    };

    // Update everything immediately
    localStorage.setItem("auth_token", token);
    setAuthToken(token);
    setState({
      token,
      user,
      isInitializing: false
    });

    // Navigation depends on the target route
    navigate(getHomePath(sidebarRole), { replace: true });
  }

  function handleLogout(message?: string) {
    localStorage.removeItem("auth_token");
    clearLatestQrLabel();
    setAuthToken(null);
    setState({ token: null, user: null, isInitializing: false });
    navigate("/login", {
      replace: true,
      state: message ? { message } : undefined
    });
  }

  function sessionExpired() {
    handleLogout("Votre session a expiré, veuillez vous reconnecter.");
  }

  const value = useMemo<AuthContextValue>(
    () => ({
      token: state.token,
      user: state.user,
      role: state.user?.role ?? null,
      jwtRole: state.user?.jwtRole ?? null,
      isInitializing: state.isInitializing,
      matricule: state.user?.matricule ?? null,
      login,
      logout: () => handleLogout(),
      sessionExpired,
    }),
    [state],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}


export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
}
