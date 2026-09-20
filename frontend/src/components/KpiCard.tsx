import React from "react";

export function KpiCard({
  label,
  value,
  hint,
}: {
  label: string;
  value: React.ReactNode;
  hint?: string;
}) {
  return (
    <div className="card">
      <div className="kpi">
        <div className="kpiContent">
          <div className="kpiLabel">{label}</div>
          <div className="kpiValue">{value}</div>
          {hint && <div className="kpiHint">{hint}</div>}
        </div>
      </div>
    </div>
  );
}
