import { Navigate, Route, Routes } from "react-router-dom";
import ProtectedRoute from "@/app/layout/ProtectedRoute";
import AppLayout from "@/app/layout/AppLayout";
import ScrollToTop from "@/app/layout/ScrollToTop";
import { useAuth } from "@/app/providers/AuthProvider";

// Auth
import LoginPage from "@/auth/LoginPage";

// Shared pages
import AccessDeniedPage from "@/features/shared/AccessDeniedPage";
import NotFoundPage from "@/features/shared/NotFoundPage";

// Admin
import DashboardPage from "@/features/admin/DashboardPage";
import UsersPage from "@/features/admin/UsersPage";
import EmployeesPage from "@/features/admin/EmployeesPage";
import BomPage from "@/features/admin/BomPage";
import GlobalStockPage from "@/features/admin/GlobalStockPage";
import SimulationPage from "@/features/admin/SimulationPage";
import ExportsPage from "@/features/admin/ExportsPage";
import ProductPage from "@/features/admin/ProductPage";
import ProductionDelayPredictionPage from "@/features/admin/ProductionDelayPredictionPage";

// Logistic
import LogisticIncomingPage from "@/features/logistic/IncomingPage";
import LogisticOutgoingPage from "@/features/logistic/OutgoingPage";
import LogisticStockPage from "@/features/logistic/StockPage";
import QrGeneratorPage from "@/features/logistic/QrGeneratorPage";

// PSF (Dept 1 Interne)
import PsfProductionPage from "@/features/psf/ProductionPage";
import PsfOutgoingPage from "@/features/psf/OutgoingPage";

// PF (Dept 2)
import PfProductionListPage from "@/features/pf/ProductionListPage";
import PfProductionCreatePage from "@/features/pf/ProductionCreatePage";

import { getHomePath } from "@/shared/utils/jwt";

function HomeRedirect() {
  const { role } = useAuth();
  return <Navigate to={getHomePath(role)} replace />;
}

function Dept1OutgoingRedirect() {
  const { role } = useAuth();
  if (role === "psf") return <Navigate to="/psf/outgoing" replace />;
  return <Navigate to="/logistic/outgoing" replace />;
}

export default function AppRouter() {
  return (
    <>
      <ScrollToTop />
      <Routes>
      {/* Public */}
      <Route path="/login" element={<LoginPage />} />
      <Route path="/unauthorized" element={<AccessDeniedPage />} />

      {/* Protected — any authenticated user */}
      <Route element={<ProtectedRoute />}>
        <Route element={<AppLayout />}>
          <Route index element={<HomeRedirect />} />
          <Route
            path="/departement-1/outgoing"
            element={<Dept1OutgoingRedirect />}
          />
          <Route path="/departement-2/stock" element={<Navigate to="/departement-2" replace />} />

          {/* No longer have profile page route */}

          {/* Admin only */}
          <Route element={<ProtectedRoute roles={["admin"]} />}>
            <Route path="/dashboard" element={<DashboardPage />} />
            <Route path="/products" element={<ProductPage />} />
            <Route path="/users" element={<UsersPage />} />
            <Route path="/employees" element={<EmployeesPage />} />
            <Route path="/nomenclature" element={<BomPage />} />
            <Route path="/stock" element={<GlobalStockPage />} />
            <Route path="/simulation" element={<SimulationPage />} />
            <Route
              path="/prediction-retard-production"
              element={<ProductionDelayPredictionPage />}
            />
            <Route path="/export" element={<ExportsPage />} />
          </Route>

          {/* Logistic & Admin shared */}
          <Route element={<ProtectedRoute roles={["logistic", "admin"]} />}>
            <Route path="/incoming" element={<LogisticIncomingPage />} />
            <Route
              path="/logistic/outgoing"
              element={<LogisticOutgoingPage />}
            />
            <Route
              path="/departement-1/incoming"
              element={<LogisticIncomingPage />}
            />
            <Route
              path="/departement-1/stock"
              element={<LogisticStockPage />}
            />
            <Route path="/qr" element={<QrGeneratorPage />} />
          </Route>

          {/* PSF (Dept 1) */}
          <Route element={<ProtectedRoute roles={["psf", "admin"]} />}>
            <Route
              path="/departement-1/production"
              element={<PsfProductionPage />}
            />
            <Route path="/psf/outgoing" element={<PsfOutgoingPage />} />
          </Route>

          {/* PF (Dept 2) */}
          <Route element={<ProtectedRoute roles={["pf", "admin"]} />}>
            <Route path="/production" element={<PfProductionCreatePage />} />
            <Route path="/departement-2" element={<PfProductionListPage />} />
          </Route>

          {/* 404 */}
          <Route path="*" element={<NotFoundPage />} />
        </Route>
      </Route>
      </Routes>
    </>
  );
}
