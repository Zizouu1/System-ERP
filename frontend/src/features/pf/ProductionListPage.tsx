import { useEffect, useMemo, useState } from "react";
import { useSearchParams } from "react-router-dom";
import editIcon from "@/assets/edit.png";
import { Card } from "@/components/Card";
import { DataTable } from "@/components/DataTable";
import { Spinner } from "@/components/Spinner";
import {
  dept2Delete,
  dept2List,
  dept2Update,
  operationAuditHistory,
} from "@/shared/api/endpoints";
import type {
  Dept2CreateProductionReq,
  Dept2ProductionDto,
  OperationAuditEntryDto,
} from "@/shared/api/types";
import { useAuth } from "@/app/providers/AuthProvider";
import { TableToolbar } from "@/shared/ui/TableToolbar/TableToolbar";
import { Modal } from "@/components/Modal";
import { OperationAuditModal } from "@/components/OperationAuditModal";
import { EmployeeAutocompleteInput } from "@/components/EmployeeAutocompleteInput";
import { useConfirmationDialog } from "@/shared/ui/ConfirmationDialog/useConfirmationDialog";
import { matchesDateRange, matchesText } from "@/utils/tableFilters";

function toTimeInput(value?: string) {
  if (!value) return "";
  const base = value.includes("T") ? value.split("T")[1] : value;
  return base.slice(0, 5);
}

function normalizeDateValue(value: unknown): string | undefined {
  if (!value) return undefined;
  if (typeof value === "string") return value;
  if (value instanceof Date) return value.toISOString();
  if (typeof value === "number" && !Number.isNaN(value)) {
    const parsed = new Date(value);
    return Number.isNaN(parsed.getTime()) ? undefined : parsed.toISOString();
  }
  if (Array.isArray(value) && value.length >= 3) {
    const [year, month, day, hour = 0, minute = 0, second = 0] = value;
    if (
      [year, month, day, hour, minute, second].every(
        (item) => typeof item === "number" && Number.isFinite(item),
      )
    ) {
      const parsed = new Date(
        year,
        Math.max(0, month - 1),
        day,
        hour,
        minute,
        second,
      );
      return Number.isNaN(parsed.getTime()) ? undefined : parsed.toISOString();
    }
  }
  return undefined;
}

function extractDateTimeParts(value?: string) {
  if (!value) return null;
  const match = value
    .trim()
    .match(/^(\d{4})-(\d{2})-(\d{2})[T\s](\d{2}):(\d{2})/);
  if (!match) return null;
  return {
    day: match[3],
    month: match[2],
    year: match[1],
    hour: match[4],
    minute: match[5],
  };
}

function extractDateParts(value?: string) {
  if (!value) return null;
  const match = value.trim().match(/^(\d{4})-(\d{2})-(\d{2})/);
  if (!match) return null;
  return {
    day: match[3],
    month: match[2],
    year: match[1],
  };
}

function extractTimeParts(value?: string) {
  if (!value) return null;
  const match = value.trim().match(/^(\d{2}):(\d{2})(?::\d{2})?$/);
  if (!match) return null;
  return { hour: match[1], minute: match[2] };
}

function formatDateTime(value?: string, fallbackDate?: string) {
  const normalizedValue = normalizeDateValue(value);
  const normalizedFallback = normalizeDateValue(fallbackDate);

  if (!normalizedValue && !normalizedFallback) return "—";

  const directDateTime = extractDateTimeParts(normalizedValue);
  if (directDateTime) {
    return `${directDateTime.day}/${directDateTime.month}/${directDateTime.year} ${directDateTime.hour}:${directDateTime.minute}`;
  }

  const fallbackDateParts = extractDateParts(normalizedFallback);
  const valueTimeParts = extractTimeParts(normalizedValue);
  if (fallbackDateParts && valueTimeParts) {
    return `${fallbackDateParts.day}/${fallbackDateParts.month}/${fallbackDateParts.year} ${valueTimeParts.hour}:${valueTimeParts.minute}`;
  }

  const fallbackDateTime = extractDateTimeParts(normalizedFallback);
  if (fallbackDateTime) {
    return `${fallbackDateTime.day}/${fallbackDateTime.month}/${fallbackDateTime.year} ${fallbackDateTime.hour}:${fallbackDateTime.minute}`;
  }

  const source = normalizedValue || normalizedFallback;
  if (source) {
    const parsed = new Date(source);
    if (!Number.isNaN(parsed.getTime())) {
      const dd = String(parsed.getDate()).padStart(2, "0");
      const mm = String(parsed.getMonth() + 1).padStart(2, "0");
      const yyyy = parsed.getFullYear();
      const hh = String(parsed.getHours()).padStart(2, "0");
      const min = String(parsed.getMinutes()).padStart(2, "0");
      return `${dd}/${mm}/${yyyy} ${hh}:${min}`;
    }
  }

  return "—";
}

function getRowDateFallback(row: Dept2ProductionDto): string | undefined {
  const raw = row as unknown as Record<string, unknown>;
  const candidates = [
    raw.startDateTime,
    raw.endDateTime,
    raw.timestamp,
    raw.operationDate,
    raw.productionDate,
    raw.createdAt,
    raw.createdDate,
    row.lastModifiedAt,
  ];
  for (const candidate of candidates) {
    const normalized = normalizeDateValue(candidate);
    if (normalized && normalized.trim().length > 0) return normalized;
  }
  return undefined;
}

type PfTableFilters = {
  reference: string;
  operatorMatricule: string;
  startDate: string;
  endDate: string;
};

const EMPTY_FILTERS: PfTableFilters = {
  reference: "",
  operatorMatricule: "",
  startDate: "",
  endDate: "",
};

export default function PfProductionListPage() {
  const { role } = useAuth();
  const isAdmin = role === "admin";
  const canUpdate = role === "pf" || isAdmin;
  const canDelete = isAdmin;
  const { askConfirmation, confirmationDialog } = useConfirmationDialog();

  const [loading, setLoading] = useState(true);
  const [rows, setRows] = useState<Dept2ProductionDto[]>([]);
  const [filters, setFilters] = useState<PfTableFilters>(EMPTY_FILTERS);
  const [draftFilters, setDraftFilters] =
    useState<PfTableFilters>(EMPTY_FILTERS);
  const [msg, setMsg] = useState<string | null>(null);
  const [selectedIds, setSelectedIds] = useState<(string | number)[]>([]);

  const [editOpen, setEditOpen] = useState(false);
  const [editRow, setEditRow] = useState<Dept2ProductionDto | null>(null);
  const [operatorMatricule, setOperatorMatricule] = useState("");
  const [reference, setReference] = useState("");
  const [quantity, setQuantity] = useState(1);
  const [scrapQuantity, setScrapQuantity] = useState(0);
  const [startTime, setStartTime] = useState("");
  const [endTime, setEndTime] = useState("");

  const [auditOpen, setAuditOpen] = useState(false);
  const [auditLoading, setAuditLoading] = useState(false);
  const [auditError, setAuditError] = useState<string | null>(null);
  const [auditEntries, setAuditEntries] = useState<OperationAuditEntryDto[]>(
    [],
  );
  const [searchParams] = useSearchParams();
  const focusedRowId = useMemo(() => {
    const raw = searchParams.get("focusId");
    if (!raw) return undefined;
    const parsed = Number(raw);
    return Number.isNaN(parsed) ? undefined : parsed;
  }, [searchParams]);

  async function refresh() {
    setLoading(true);
    try {
      const response = await dept2List();
      if (Array.isArray(response)) {
        setRows(response);
        setMsg(null);
      } else if (
        (response as any)?.content &&
        Array.isArray((response as any).content)
      ) {
        setRows((response as any).content as Dept2ProductionDto[]);
        setMsg(null);
      } else {
        setRows([]);
        setMsg("Aucune donnée disponible pour l'historique PF.");
      }
    } catch (e: any) {
      setRows([]);
      setMsg(e?.message ?? "Erreur lors du chargement de l'historique PF.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    refresh();
  }, []);

  const filtered = useMemo(() => {
    return rows.filter((r) => {
      const dateSource =
        (typeof r.startDateTime === "string" && r.startDateTime) ||
        (typeof r.createdAt === "string" && r.createdAt) ||
        (typeof r.startTime === "string" && r.startTime) ||
        getRowDateFallback(r);
      return (
        matchesText(r.reference, filters.reference) &&
        matchesText(r.operatorMatricule, filters.operatorMatricule) &&
        matchesDateRange(dateSource, filters.startDate, filters.endDate)
      );
    });
  }, [rows, filters]);

  function openEdit(row: Dept2ProductionDto) {
    setEditRow(row);
    setOperatorMatricule(row.operatorMatricule ?? "");
    setReference(row.reference ?? "");
    setQuantity(Number(row.quantity) || 0);
    setScrapQuantity(Number(row.scrapQuantity) || 0);
    setStartTime(toTimeInput(row.startTime));
    setEndTime(toTimeInput(row.endTime));
    setEditOpen(true);
  }

  async function saveEdit() {
    if (!editRow) return;
    const body: Dept2CreateProductionReq = {
      operatorMatricule: operatorMatricule.trim(),
      reference: reference.trim(),
      quantity: quantity || 0,
      scrapQuantity: scrapQuantity || 0,
      startTime,
      endTime,
    };

    try {
      await dept2Update(editRow.id, body);
      setEditOpen(false);
      await refresh();
    } catch (e: any) {
      setMsg(e?.message ?? "Erreur lors de la mise à jour.");
    }
  }

  async function remove(id: number) {
    const confirmed = await askConfirmation({
      title: "Supprimer la production PF",
      message: "Voulez-vous vraiment supprimer cette opération ?",
    });
    if (!confirmed) return;
    try {
      await dept2Delete(id);
      setMsg("Production supprimée avec succès.");
      await refresh();
    } catch (e: any) {
      setMsg(e?.message ?? "Erreur lors de la suppression.");
    }
  }

  async function handleGlobalDelete() {
    const confirmed = await askConfirmation({
      title: "Supprimer",
      message: `Voulez-vous vraiment supprimer ${selectedIds.length} opération(s) de production PF ?`,
    });
    if (!confirmed) return;
    setMsg(null);
    setLoading(true);
    try {
      for (const id of selectedIds) {
        await dept2Delete(Number(id));
      }
      setMsg("Supprimés avec succès.");
      setSelectedIds([]);
      await refresh();
    } catch (e: any) {
      setMsg(e?.message ?? "Erreur lors de la suppression.");
    } finally {
      setLoading(false);
    }
  }

  async function openAudit(row: Dept2ProductionDto) {
    if (!isAdmin || !row.id) return;
    setAuditOpen(true);
    setAuditLoading(true);
    setAuditError(null);
    setAuditEntries([]);
    try {
      const entries = await operationAuditHistory(
        "PF_PRODUCTION",
        Number(row.id),
      );
      setAuditEntries(Array.isArray(entries) ? entries : []);
    } catch (e: any) {
      setAuditError(
        e?.message ??
          "Impossible de charger l'historique pour cette opération.",
      );
      setAuditEntries([]);
    } finally {
      setAuditLoading(false);
    }
  }

  return (
    <div className="page">
      <Card title="Production PF - Historique">
        <TableToolbar
          exportFilename="pf_production"
          exportTitle="Historique Production PF"
          onRefresh={refresh}
          rows={
            selectedIds.length > 0
              ? filtered.filter((r) => r.id && selectedIds.includes(r.id))
              : filtered
          }
          selectedCount={selectedIds.length}
          onGlobalDelete={handleGlobalDelete}
          columns={[
            { header: "Produit", cell: (r) => r.reference },
            { header: "Opérateur", cell: (r) => r.operatorMatricule },
            { header: "Qté", cell: (r) => r.quantity },
            { header: "Rebuts", cell: (r) => r.scrapQuantity ?? 0 },
            {
              header: "Début",
              cell: (r) =>
                formatDateTime(
                  r.startDateTime ?? r.startTime,
                  getRowDateFallback(r),
                ),
            },
            {
              header: "Fin",
              cell: (r) =>
                formatDateTime(
                  r.endDateTime ?? r.endTime,
                  getRowDateFallback(r),
                ),
            },
          ]}
          filterContent={
            <>
              <div className="field">
                <div className="label">Réf</div>
                <input
                  className="input"
                  value={draftFilters.reference}
                  onChange={(e) =>
                    setDraftFilters((prev) => ({
                      ...prev,
                      reference: e.target.value,
                    }))
                  }
                  placeholder="Ex: PF-001"
                />
              </div>
              <div className="field">
                <div className="label">Opérateur</div>
                <input
                  className="input"
                  value={draftFilters.operatorMatricule}
                  onChange={(e) =>
                    setDraftFilters((prev) => ({
                      ...prev,
                      operatorMatricule: e.target.value,
                    }))
                  }
                  placeholder="Ex: OP200"
                />
              </div>
              <div className="grid2">
                <div className="field">
                  <div className="label">Date de début</div>
                  <input
                    className="input"
                    type="date"
                    value={draftFilters.startDate}
                    onChange={(e) =>
                      setDraftFilters((prev) => ({
                        ...prev,
                        startDate: e.target.value,
                      }))
                    }
                  />
                </div>
                <div className="field">
                  <div className="label">Date de fin</div>
                  <input
                    className="input"
                    type="date"
                    value={draftFilters.endDate}
                    onChange={(e) =>
                      setDraftFilters((prev) => ({
                        ...prev,
                        endDate: e.target.value,
                      }))
                    }
                  />
                </div>
              </div>
            </>
          }
          onApplyFilters={() => setFilters({ ...draftFilters })}
        />
        {msg && (
          <div className="notice" style={{ marginBottom: 12 }}>
            {msg}
          </div>
        )}
        {loading ? (
          <Spinner />
        ) : (
          <DataTable
            enableSelection
            selectedKeys={selectedIds}
            onSelectionChange={setSelectedIds}
            highlightedKey={focusedRowId}
            rows={filtered}
            rowKey={(r) => r.id}
            rowClassName={(r) =>
              r.lastModifiedAt !== null ? "rowEdited" : undefined
            }
            columns={[
              { header: "Produit", cell: (r) => r.reference },
              { header: "Opérateur", cell: (r) => r.operatorMatricule },
              {
                header: "Qté",
                cell: (r) => (
                  <span className="badge badgeBlue">{r.quantity}</span>
                ),
              },
              { header: "Rebuts", cell: (r) => r.scrapQuantity ?? "-" },
              {
                header: "Début",
                cell: (r) =>
                  formatDateTime(
                    r.startDateTime ?? r.startTime,
                    getRowDateFallback(r),
                  ),
              },
              {
                header: "Fin",
                cell: (r) =>
                  formatDateTime(
                    r.endDateTime ?? r.endTime,
                    getRowDateFallback(r),
                  ),
              },
              {
                header: "État",
                cell: (r) =>
                  r.lastModifiedAt !== null ? (
                    isAdmin ? (
                      <button
                        className="badge badgeEdited"
                        onClick={() => openAudit(r)}
                      >
                        Modifié
                      </button>
                    ) : (
                      <span className="badge badgeEdited">Modifié</span>
                    )
                  ) : (
                    "—"
                  ),
              },
              {
                header: "",
                cell: (r) => (
                  <div
                    className="actionsRow"
                    style={{ justifyContent: "flex-end" }}
                  >
                    {canUpdate && (
                      <button
                        className="btn btnGhost"
                        style={{ padding: 4 }}
                        onClick={() => openEdit(r)}
                        title="Modifier"
                      >
                        <img
                          src={editIcon}
                          alt="Edit"
                          style={{ width: 16, height: 16 }}
                        />
                      </button>
                    )}
                  </div>
                ),
              },
            ]}
          />
        )}
      </Card>

      <Modal
        open={editOpen}
        title="Modifier la production PF"
        onClose={() => setEditOpen(false)}
        actions={
          <button className="btn btnPrimary" onClick={saveEdit}>
            Enregistrer
          </button>
        }
      >
        <div className="form">
          <div className="field">
            <div className="label">Matricule opérateur</div>
            <EmployeeAutocompleteInput
              value={operatorMatricule}
              onChange={setOperatorMatricule}
              placeholder="Ex: OP123"
            />
          </div>
          <div className="field">
            <div className="label">Produit</div>
            <input
              className="input"
              value={reference}
              onChange={(e) => setReference(e.target.value)}
            />
          </div>
          <div className="grid2">
            <div className="field">
              <div className="label">Quantité</div>
              <input
                className="input"
                type="number"
                min={0}
                value={quantity}
                onChange={(e) => setQuantity(Number(e.target.value) || 0)}
              />
            </div>
            <div className="field">
              <div className="label">Rebuts</div>
              <input
                className="input"
                type="number"
                min={0}
                value={scrapQuantity}
                onChange={(e) => setScrapQuantity(Number(e.target.value) || 0)}
              />
            </div>
          </div>
          <div className="grid2">
            <div className="field">
              <div className="label">Début</div>
              <input
                className="input"
                type="time"
                value={startTime}
                onChange={(e) => setStartTime(e.target.value)}
              />
            </div>
            <div className="field">
              <div className="label">Fin</div>
              <input
                className="input"
                type="time"
                value={endTime}
                onChange={(e) => setEndTime(e.target.value)}
              />
            </div>
          </div>
        </div>
      </Modal>

      <OperationAuditModal
        open={auditOpen}
        title="Historique des modifications"
        loading={auditLoading}
        errorMessage={auditError}
        entries={auditEntries}
        onClose={() => setAuditOpen(false)}
      />
      {confirmationDialog}
    </div>
  );
}
