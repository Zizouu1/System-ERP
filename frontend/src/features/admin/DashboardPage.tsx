import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import {
  ResponsiveContainer,
  LineChart,
  Line,
  XAxis,
  YAxis,
  Tooltip,
  CartesianGrid,
  BarChart,
  Bar,
  PieChart,
  Pie,
  Legend,
  Cell,
} from "recharts";

import { Card } from "@/components/Card";
import { KpiCard } from "@/components/KpiCard";
import { DataTable } from "@/components/DataTable";
import { Spinner } from "@/components/Spinner";
import { useAuth } from "@/app/providers/AuthProvider";
import {
  logisticStock,
  listProducts,
  listExports,
  listBom,
  countBom,
  countEmployees,
  dept2List,
  psfStocks,
  adminListEmployees,
} from "@/shared/api/endpoints";
import type {
  Dept2ProductionDto,
  ExportDto,
  StockDep1Dto,
  ProductDto,
  NomenclatureDto,
  EmployeeDto,
} from "@/shared/api/types";

function sum(nums: number[]) {
  return nums.reduce((a, b) => a + b, 0);
}

const PIE_COLORS = ["#4f46e5", "#10b981", "#f59e0b", "#ef4444", "#8b5cf6"];

type TimeScale = "hour" | "day" | "week";

function toValidDate(value: unknown): Date | null {
  if (!value) return null;
  const d = value instanceof Date ? value : new Date(String(value));
  return Number.isNaN(d.getTime()) ? null : d;
}

function parseDateLike(value: unknown): Date | null {
  if (typeof value !== "string") return toValidDate(value);
  const trimmed = value.trim();
  if (!trimmed) return null;

  const direct = toValidDate(trimmed);
  if (direct) return direct;

  // dd/MM/yyyy or dd/MM/yyyy HH:mm(:ss)
  const fr = trimmed.match(
    /^(\d{2})\/(\d{2})\/(\d{4})(?:[T\s](\d{2}):(\d{2})(?::(\d{2}))?)?$/,
  );
  if (!fr) return null;

  const [, day, month, year, hh = "00", mm = "00", ss = "00"] = fr;
  const d = new Date(
    Number(year),
    Number(month) - 1,
    Number(day),
    Number(hh),
    Number(mm),
    Number(ss),
  );

  return Number.isNaN(d.getTime()) ? null : d;
}

function parseTimeParts(value: unknown) {
  if (typeof value !== "string") return null;
  const match = value.trim().match(/^(\d{2}):(\d{2})(?::(\d{2}))?$/);
  if (!match) return null;
  return {
    hour: Number(match[1]),
    minute: Number(match[2]),
    second: Number(match[3] ?? "0"),
  };
}

function combineDateAndTime(
  dateBase: unknown,
  timeValue: unknown,
): Date | null {
  const base = parseDateLike(dateBase);
  const time = parseTimeParts(timeValue);
  if (!base || !time) return null;

  const merged = new Date(base);
  merged.setHours(time.hour, time.minute, time.second, 0);
  return Number.isNaN(merged.getTime()) ? null : merged;
}

function getRowDateFallback(row: Dept2ProductionDto): unknown {
  const raw = row as unknown as Record<string, unknown>;
  return (
    raw.createdAt ??
    raw.timestamp ??
    raw.operationDate ??
    raw.productionDate ??
    raw.dateProduction ??
    raw.producedAt ??
    raw.date ??
    row.lastModifiedAt
  );
}

function getUserProductionDate(row: Dept2ProductionDto): Date | null {
  const raw = row as unknown as Record<string, unknown>;
  const dateFallback = getRowDateFallback(row);

  // prefer explicit date-like fields from backend payload
  const explicitDate =
    parseDateLike(raw.productionDate) ??
    parseDateLike(raw.dateProduction) ??
    parseDateLike(raw.production_time) ??
    parseDateLike(raw.producedAt) ??
    parseDateLike(raw.date) ??
    parseDateLike(raw.timestamp) ??
    parseDateLike(raw.operationDate) ??
    parseDateLike(raw.createdAt) ??
    parseDateLike(row.lastModifiedAt);

  if (explicitDate) return explicitDate;

  // when startTime is time-only (HH:mm:ss), attach a reliable date fallback
  const fromFallback = combineDateAndTime(dateFallback, row.startTime);
  if (fromFallback) return fromFallback;

  // last resort: use today with recorded time to avoid dropping events entirely
  const fromToday = combineDateAndTime(new Date(), row.startTime);
  if (fromToday) return fromToday;

  // or direct parse if startTime is full datetime
  return (
    parseDateLike(row.startTime) ??
    parseDateLike(row.endTime) ??
    parseDateLike(dateFallback)
  );
}

function startOfHour(d: Date) {
  const x = new Date(d);
  x.setMinutes(0, 0, 0);
  return x;
}
function startOfDay(d: Date) {
  const x = new Date(d);
  x.setHours(0, 0, 0, 0);
  return x;
}
function startOfWeekMonday(d: Date) {
  const x = startOfDay(d);
  const day = x.getDay(); // 0..6 (Sun..Sat)
  const diff = day === 0 ? -6 : 1 - day; // move to Monday
  x.setDate(x.getDate() + diff);
  return x;
}
function addHours(d: Date, h: number) {
  const x = new Date(d);
  x.setHours(x.getHours() + h);
  return x;
}
function addDays(d: Date, days: number) {
  const x = new Date(d);
  x.setDate(x.getDate() + days);
  return x;
}
function formatHour(d: Date) {
  return d.toLocaleTimeString("fr-FR", { hour: "2-digit", minute: "2-digit" });
}
function formatDay(d: Date) {
  return d.toLocaleDateString("fr-FR", { day: "2-digit", month: "2-digit" });
}
function formatWeek(d: Date) {
  const end = addDays(d, 6);
  return `${formatDay(d)} - ${formatDay(end)}`;
}

export default function DashboardPage() {
  const { role } = useAuth();

  const [loading, setLoading] = useState(true);
  const [stock, setStock] = useState<StockDep1Dto[]>([]);
  const [products, setProducts] = useState<ProductDto[]>([]);
  const [exportsRows, setExports] = useState<ExportDto[]>([]);
  const [bom, setBom] = useState<NomenclatureDto[]>([]);
  const [bomTotal, setBomTotal] = useState<number>(0);
  const [employeeTotal, setEmployeeTotal] = useState<number>(0);
  const [employees, setEmployees] = useState<EmployeeDto[]>([]);
  const [pf, setPf] = useState<Dept2ProductionDto[]>([]);
  const [psf, setPsf] = useState<StockDep1Dto[]>([]);
  const [timeScale, setTimeScale] = useState<TimeScale>("day");

  async function refreshDashboard() {
    setLoading(true);
    try {
      if (role === "admin") {
        const [s, p, e, b, bt, ce, d2, ps, el] = await Promise.all([
          logisticStock().catch(() => [] as any),
          listProducts().catch(() => [] as any),
          listExports().catch(() => [] as any),
          listBom(0, 100000).catch(() => ({ content: [] }) as any),
          countBom().catch(() => 0 as any),
          countEmployees().catch(() => 0 as any),
          dept2List().catch(() => [] as any),
          psfStocks().catch(() => [] as any),
          adminListEmployees().catch(() => [] as any),
        ]);
        setStock(s);
        setProducts(p);
        setExports(e);
        setBom(b.content);
        setBomTotal(bt);
        setEmployeeTotal(ce);
        setPf(d2);
        setPsf(ps);
        setEmployees(el);
      } else if (role === "psf") {
        const ps = await psfStocks().catch(() => [] as any);
        setPsf(ps);
      } else if (role === "pf") {
        const d2 = await dept2List().catch(() => [] as any);
        setPf(d2);
      }
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    refreshDashboard();
  }, [role]);

  const stockTotals = useMemo(
    () => ({
      total: sum(stock.map((r) => Number(r.totalQuantity) || 0)),
    }),
    [stock],
  );

  const productTypePie = useMemo(() => {
    const map = new Map<string, number>();
    for (const p of products) {
      const key = p.productType ?? "INCONNU";
      map.set(key, (map.get(key) ?? 0) + 1);
    }
    return Array.from(map.entries()).map(([name, value]) => ({ name, value }));
  }, [products]);

  const recentExports = useMemo(
    () =>
      [...exportsRows]
        .sort(
          (a, b) =>
            new Date(b.exportDate ?? 0).getTime() -
            new Date(a.exportDate ?? 0).getTime(),
        )
        .slice(0, 6),
    [exportsRows],
  );

  const recentEmployees = useMemo(
    () => [...employees].reverse().slice(0, 6),
    [employees],
  );

  const productionChartData = useMemo(() => {
    if (!pf.length) return [];

    const events = pf
      .map((r) => ({
        date: getUserProductionDate(r),
        qty: Number(r.quantity) || 0,
      }))
      .filter((e): e is { date: Date; qty: number } => !!e.date);

    if (!events.length) return [];

    // anchor on latest production event date (handles delayed input correctly)
    const anchor = new Date(Math.max(...events.map((e) => e.date.getTime())));

    if (timeScale === "hour") {
      const end = startOfHour(anchor);
      const start = addHours(end, -23);
      const buckets = Array.from({ length: 24 }, (_, i) => addHours(start, i));
      const map = new Map<number, number>(buckets.map((d) => [d.getTime(), 0]));

      for (const e of events) {
        const b = startOfHour(e.date).getTime();
        if (b >= start.getTime() && b <= end.getTime()) {
          map.set(b, (map.get(b) ?? 0) + e.qty);
        }
      }

      return buckets.map((d) => ({
        label: formatHour(d),
        qty: map.get(d.getTime()) ?? 0,
      }));
    }

    if (timeScale === "day") {
      const end = startOfDay(anchor);
      const start = addDays(end, -13);
      const buckets = Array.from({ length: 14 }, (_, i) => addDays(start, i));
      const map = new Map<number, number>(buckets.map((d) => [d.getTime(), 0]));

      for (const e of events) {
        const b = startOfDay(e.date).getTime();
        if (b >= start.getTime() && b <= end.getTime()) {
          map.set(b, (map.get(b) ?? 0) + e.qty);
        }
      }

      return buckets.map((d) => ({
        label: formatDay(d),
        qty: map.get(d.getTime()) ?? 0,
      }));
    }

    const end = startOfWeekMonday(anchor);
    const start = addDays(end, -7 * 7); // 8 weeks total
    const buckets = Array.from({ length: 8 }, (_, i) => addDays(start, i * 7));
    const map = new Map<number, number>(buckets.map((d) => [d.getTime(), 0]));

    for (const e of events) {
      const b = startOfWeekMonday(e.date).getTime();
      if (b >= start.getTime() && b <= end.getTime()) {
        map.set(b, (map.get(b) ?? 0) + e.qty);
      }
    }

    return buckets.map((d) => ({
      label: formatWeek(d),
      qty: map.get(d.getTime()) ?? 0,
    }));
  }, [pf, timeScale]);

  if (loading)
    return (
      <div className="page">
        <Spinner />
      </div>
    );

  // PSF dashboard
  if (role === "psf") {
    return (
      <div className="page mobilePage">
        <div className="pageHeader">
          <div>
            <h1>PSF</h1>
            <div className="muted">Opérations du jour</div>
          </div>
        </div>
        <div className="grid">
          <Card title="Actions">
            <div className="actionsRow">
              <Link className="btn btnPrimary" to="/departement-1/production">
                Production
              </Link>
              <Link className="btn btnPrimary" to="/psf/outgoing">
                Sortie
              </Link>
            </div>
          </Card>
          <Card title="Stock">
            <DataTable
              rows={psf.slice(0, 10)}
              rowKey={(r) => r.id}
              columns={[
                { header: "Réf", cell: (r) => r.reference },
                { header: "Lot", cell: (r) => r.lotNumber || "-" },
                {
                  header: "Qté",
                  cell: (r) => (
                    <span className="badge badgeBlue">{r.totalQuantity}</span>
                  ),
                },
              ]}
              emptyText="Aucun lot"
            />
          </Card>
        </div>
      </div>
    );
  }

  // PF dashboard
  if (role === "pf") {
    return (
      <div className="page mobilePage">
        <div className="pageHeader">
          <div>
            <h1>PF</h1>
            <div className="muted">Opérations du jour</div>
          </div>
        </div>
        <div className="grid">
          <Card title="Actions">
            <div className="actionsRow">
              <Link className="btn btnPrimary" to="/production">
                Nouvelle production
              </Link>
              <Link className="btn" to="/departement-2">
                Historique
              </Link>
            </div>
          </Card>
          <Card title="Productions récentes">
            <DataTable
              rows={[...pf]
                .sort(
                  (a, b) =>
                    new Date(b.startTime).getTime() -
                    new Date(a.startTime).getTime(),
                )
                .slice(0, 8)}
              rowKey={(r) => r.id}
              columns={[
                { header: "Produit", cell: (r) => r.reference },
                {
                  header: "Qté",
                  cell: (r) => (
                    <span className="badge badgeBlue">{r.quantity}</span>
                  ),
                },
                {
                  header: "Début",
                  cell: (r) => {
                    const d = new Date(r.startTime);
                    return isNaN(d.getTime()) ? "-" : d.toLocaleDateString();
                  },
                },
              ]}
              emptyText="Aucune production"
            />
          </Card>
        </div>
      </div>
    );
  }

  // Admin dashboard
  return (
    <div
      className="page"
      style={{ background: "#f9fafb", minHeight: "100vh", padding: "24px" }}
    >
      <div className="pageHeader" style={{ marginBottom: "24px" }}>
        <div>
          <h1 style={{ fontSize: "24px", fontWeight: "700", color: "#111827" }}>
            Dashboard Administrateur
          </h1>
          <p className="muted">Bienvenue sur votre espace de gestion.</p>
        </div>
      </div>

      {/* TOP SECTION: KPIs */}
      <div
        className="kpiGrid"
        style={{
          display: "grid",
          gridTemplateColumns: "repeat(auto-fit, minmax(220px, 1fr))",
          gap: "24px",
          marginBottom: "32px",
        }}
      >
        <KpiCard
          label="Stock total"
          value={stockTotals.total}
          hint="Unités disponibles"
        />
        <KpiCard label="Employés" value={employeeTotal} hint="Total effectif" />
        <KpiCard
          label="Produits"
          value={products.length}
          hint="Références actives"
        />
        <KpiCard
          label="Nomenclatures"
          value={bomTotal}
          hint="BOM enregistrées"
        />
      </div>

      {/* MIDDLE SECTION: Production Chart & Pie Chart */}
      <div
        style={{
          display: "grid",
          gridTemplateColumns: "repeat(auto-fit, minmax(340px, 1fr))",
          gap: "24px",
          marginBottom: "32px",
        }}
      >
        <Card
          title="Évolution de la production"
          actions={
            <div className="actionsRow" style={{ gap: "8px" }}>
              <button
                className={`btn ${timeScale === "hour" ? "btnPrimary" : ""}`}
                onClick={() => setTimeScale("hour")}
                style={{ fontSize: "12px", padding: "4px 12px" }}
              >
                Heures
              </button>
              <button
                className={`btn ${timeScale === "day" ? "btnPrimary" : ""}`}
                onClick={() => setTimeScale("day")}
                style={{ fontSize: "12px", padding: "4px 12px" }}
              >
                Jours
              </button>
              <button
                className={`btn ${timeScale === "week" ? "btnPrimary" : ""}`}
                onClick={() => setTimeScale("week")}
                style={{ fontSize: "12px", padding: "4px 12px" }}
              >
                Semaines
              </button>
            </div>
          }
        >
          <div style={{ height: 350, padding: "16px 0" }}>
            <ResponsiveContainer width="100%" height="100%">
              <LineChart data={productionChartData}>
                <CartesianGrid
                  strokeDasharray="3 3"
                  stroke="#f3f4f6"
                  vertical={false}
                />
                <XAxis
                  dataKey="label"
                  axisLine={false}
                  tickLine={false}
                  tick={{ fill: "#6b7280", fontSize: 12 }}
                  dy={10}
                />
                <YAxis
                  axisLine={false}
                  tickLine={false}
                  tick={{ fill: "#6b7280", fontSize: 12 }}
                />
                <Tooltip
                  contentStyle={{
                    borderRadius: "8px",
                    border: "none",
                    boxShadow: "0 4px 6px -1px rgb(0 0 0 / 0.1)",
                  }}
                />
                <Line
                  type="monotone"
                  dataKey="qty"
                  stroke="#4f46e5"
                  strokeWidth={3}
                  dot={{
                    r: 4,
                    fill: "#4f46e5",
                    strokeWidth: 2,
                    stroke: "#fff",
                  }}
                  activeDot={{ r: 6, strokeWidth: 0 }}
                  name="Quantité"
                />
              </LineChart>
            </ResponsiveContainer>
          </div>
        </Card>

        <Card title="Répartition par type">
          <div
            style={{ height: 350, display: "flex", flexDirection: "column" }}
          >
            <ResponsiveContainer width="100%" height="70%">
              <PieChart>
                <Pie
                  data={productTypePie}
                  dataKey="value"
                  nameKey="name"
                  innerRadius={60}
                  outerRadius={100}
                  paddingAngle={5}
                >
                  {productTypePie.map((entry, index) => (
                    <Cell
                      key={`cell-${index}`}
                      fill={PIE_COLORS[index % PIE_COLORS.length]}
                    />
                  ))}
                </Pie>
                <Tooltip />
              </PieChart>
            </ResponsiveContainer>
            <div style={{ padding: "0 16px", marginTop: "auto" }}>
              <div
                style={{
                  display: "grid",
                  gridTemplateColumns: "1fr",
                  gap: "8px",
                }}
              >
                {productTypePie.map((item, idx) => (
                  <div
                    key={item.name}
                    style={{
                      display: "flex",
                      alignItems: "center",
                      gap: "12px",
                      fontSize: "13px",
                    }}
                  >
                    <div
                      style={{
                        width: 12,
                        height: 12,
                        borderRadius: "50%",
                        background: PIE_COLORS[idx % PIE_COLORS.length],
                      }}
                    />
                    <span style={{ color: "#374151", fontWeight: "500" }}>
                      {item.name}
                    </span>
                    <span style={{ marginLeft: "auto", color: "#6b7280" }}>
                      {item.value}
                    </span>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </Card>
      </div>

      {/* BOTTOM SECTION: Employees & Exports */}
      <div
        style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "24px" }}
      >
        <Card title="Derniers employés recrutés">
          <DataTable
            rows={recentEmployees}
            rowKey={(r) => r.id}
            columns={[
              {
                header: "Matricule",
                cell: (r) => (
                  <span style={{ fontWeight: "600" }}>{r.matricule}</span>
                ),
              },
              { header: "Nom", cell: (r) => `${r.nom} ${r.prenom}` },
              {
                header: "Poste",
                cell: (r) => <span className="badge">{r.poste}</span>,
              },
            ]}
            emptyText="Aucun employé"
          />
        </Card>

        <Card title="Derniers exports enregistrés">
          <DataTable
            rows={recentExports}
            rowKey={(r) => r.id}
            columns={[
              { header: "Référence", cell: (r) => r.product?.ref ?? "-" },
              {
                header: "Quantité",
                cell: (r) => (
                  <span className="badge badgeBlue">{r.quantity}</span>
                ),
              },
              {
                header: "Date",
                cell: (r) =>
                  r.exportDate
                    ? new Date(r.exportDate).toLocaleDateString("fr-FR")
                    : "-",
              },
            ]}
            emptyText="Aucun export"
          />
        </Card>
      </div>
    </div>
  );
}
