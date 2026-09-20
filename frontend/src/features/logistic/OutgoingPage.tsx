import { useEffect, useMemo, useState } from "react";
import { useSearchParams } from "react-router-dom";
import editIcon from "@/assets/edit.png";
import qrCodeIcon from "@/assets/qr-code.png";
import { Card } from "@/components/Card";
import { AutocompleteInput } from "@/components/AutocompleteInput";
import {
  createOutgoing,
  deleteOutgoing,
  listOutgoing,
  operationAuditHistory,
  updateOutgoing,
} from "@/shared/api/endpoints";
import { QrScannerModal } from "@/components/QrScannerModal";
import { parseQrTextToFormData } from "@/utils/qr";
import { useAuth } from "@/app/providers/AuthProvider";
import { DataTable } from "@/components/DataTable";
import { Spinner } from "@/components/Spinner";
import type { OperationAuditEntryDto, OutgoingDto } from "@/shared/api/types";
import { TableToolbar } from "@/shared/ui/TableToolbar/TableToolbar";
import { useConfirmationDialog } from "@/shared/ui/ConfirmationDialog/useConfirmationDialog";
import { Modal } from "@/components/Modal";
import { OperationAuditModal } from "@/components/OperationAuditModal";
import { matchesDateRange, matchesText } from "@/utils/tableFilters";

function toDateTimeLocal(value?: string) {
  if (!value) return "";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "";
  return new Date(date.getTime() - date.getTimezoneOffset() * 60000)
    .toISOString()
    .slice(0, 16);
}

function toLocalDateTime(value?: string) {
  if (!value) return undefined;
  if (/Z|[+-]\d{2}:\d{2}$/.test(value)) return value;
  const match = value.match(/^(\d{4}-\d{2}-\d{2}T\d{2}:\d{2})(:\d{2})?$/);
  if (match) {
    return `${match[1]}${match[2] ?? ":00"}`;
  }
  return value;
}

type OutgoingTableFilters = {
  reference: string;
  lotNumber: string;
  startDate: string;
  endDate: string;
};

const EMPTY_FILTERS: OutgoingTableFilters = {
  reference: "",
  lotNumber: "",
  startDate: "",
  endDate: "",
};

function formatDateTime(value?: string) {
  if (!value) return "—";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "—";
  const dd = String(date.getDate()).padStart(2, "0");
  const mm = String(date.getMonth() + 1).padStart(2, "0");
  const yyyy = date.getFullYear();
  const hh = String(date.getHours()).padStart(2, "0");
  const min = String(date.getMinutes()).padStart(2, "0");
  return `${dd}/${mm}/${yyyy} ${hh}:${min}`;
}

export default function LogisticOutgoingPage() {
  const { askConfirmation, confirmationDialog } = useConfirmationDialog();
  const { role } = useAuth();
  const isAdmin = role === "admin";

  const [reference, setReference] = useState("");
  const [lotNumber, setLotNumber] = useState("");
  const [quantityOut, setQuantityOut] = useState<number>(1);
  const [busy, setBusy] = useState(false);
  const [msg, setMsg] = useState<string | null>(null);
  const [scanOpen, setScanOpen] = useState(false);
  const [operationDate, setOperationDate] = useState("");

  const [history, setHistory] = useState<OutgoingDto[]>([]);
  const [loading, setLoading] = useState(false);
  const [filters, setFilters] = useState<OutgoingTableFilters>(EMPTY_FILTERS);
  const [draftFilters, setDraftFilters] =
    useState<OutgoingTableFilters>(EMPTY_FILTERS);
  const [selectedIds, setSelectedIds] = useState<(string | number)[]>([]);

  const [editOpen, setEditOpen] = useState(false);
  const [editRow, setEditRow] = useState<OutgoingDto | null>(null);
  const [eReference, setEReference] = useState("");
  const [eLotNumber, setELotNumber] = useState("");
  const [eQuantityOut, setEQuantityOut] = useState(1);
  const [eOperationDate, setEOperationDate] = useState("");

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
      setHistory(await listOutgoing());
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    refresh();
  }, []);

  const filtered = useMemo(() => {
    return history.filter((h) => {
      return (
        matchesText(h.reference, filters.reference) &&
        matchesText(h.lotNumber, filters.lotNumber) &&
        matchesDateRange(h.operationDate, filters.startDate, filters.endDate)
      );
    });
  }, [history, filters]);

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setBusy(true);
    setMsg(null);
    try {
      await createOutgoing({
        reference: reference.trim(),
        lotNumber: lotNumber.trim() || "LOG",
        quantityOut,
        operationDate: toLocalDateTime(operationDate),
      });
      await refresh();
    } catch (e: any) {
      setMsg(e?.message ?? "Erreur.");
    } finally {
      setBusy(false);
    }
  }

  function onScan(text: string) {
    const parsed = parseQrTextToFormData(text);
    if (parsed.reference) setReference(parsed.reference);
    if (parsed.lotNumber) setLotNumber(parsed.lotNumber);
    if (typeof parsed.quantity === "number" && !Number.isNaN(parsed.quantity)) {
      setQuantityOut(parsed.quantity);
    }
  }

  function openEdit(row: OutgoingDto) {
    setEditRow(row);
    setEReference(row.reference ?? "");
    setELotNumber(row.lotNumber ?? "");
    setEQuantityOut(Number(row.quantityOut) || 0);
    setEOperationDate(toDateTimeLocal(row.operationDate));
    setEditOpen(true);
  }

  async function saveEdit() {
    if (!editRow?.id) return;
    try {
      await updateOutgoing(editRow.id, {
        reference: eReference.trim(),
        lotNumber: eLotNumber.trim(),
        quantityOut: eQuantityOut,
        operationDate: toLocalDateTime(eOperationDate),
      });
      setEditOpen(false);
      await refresh();
    } catch (e: any) {
      setMsg(e?.message ?? "Erreur lors de la mise à jour.");
    }
  }

  async function remove(row: OutgoingDto) {
    if (!row.id) return;
    const confirmed = await askConfirmation({
      title: "Supprimer la sortie",
      message: "Voulez-vous vraiment supprimer cette sortie ?",
    });
    if (!confirmed) return;
    try {
      await deleteOutgoing(row.id);
      setMsg("Sortie supprimée avec succès.");
      await refresh();
    } catch (e: any) {
      setMsg(e?.message ?? "Erreur lors de la suppression.");
    }
  }

  async function handleGlobalDelete() {
    const confirmed = await askConfirmation({
      title: "Supprimer",
      message: `Voulez-vous vraiment supprimer ${selectedIds.length} sortie(s) ?`,
    });
    if (!confirmed) return;
    setMsg(null);
    setLoading(true);
    try {
      for (const id of selectedIds) {
        await deleteOutgoing(Number(id));
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

  async function openAudit(row: OutgoingDto) {
    if (!isAdmin || !row.id) return;
    setAuditOpen(true);
    setAuditLoading(true);
    setAuditError(null);
    setAuditEntries([]);
    try {
      const entries = await operationAuditHistory("OUTGOING", row.id);
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
    <div className="page mobilePage">
      <div className="grid">
        {role === "logistic" && (
          <Card
            title="Enregistrer"
            actions={
              <button className="btn" onClick={() => setScanOpen(true)}>
                <img
                  src={qrCodeIcon}
                  alt="QR"
                  style={{ width: 16, height: 16 }}
                />
                Scanner
              </button>
            }
          >
            <form className="form" onSubmit={submit}>
              <div className="field">
                <div className="label">Référence</div>
                <AutocompleteInput
                  value={reference}
                  onChange={(val) => {
                    if (
                      val.includes("$") ||
                      val.includes("|") ||
                      val.includes(";")
                    ) {
                      onScan(val);
                    } else {
                      setReference(val);
                    }
                  }}
                  required
                  productType="MATIERE_PREMIERE"
                />
              </div>
              <div className="grid2">
                <div className="field">
                  <div className="label">Quantité sortie</div>
                  <input
                    className="input"
                    type="text"
                    inputMode="numeric"
                    pattern="[0-9]*"
                    value={quantityOut}
                    onChange={(e) => {
                      const val = e.target.value.replace(/[^0-9]/g, "");
                      setQuantityOut(Number(val) || 0);
                    }}
                    required
                  />
                </div>
                <div className="field">
                  <div className="label">Numéro de lot</div>
                  <input
                    className="input"
                    value={lotNumber}
                    onChange={(e) => setLotNumber(e.target.value)}
                    placeholder="Ex: LOT-123"
                    required
                  />
                </div>
              </div>
              <div className="grid2">
                <div className="field">
                  <div className="label">Date d'opération</div>
                  <input
                    className="input"
                    type="datetime-local"
                    value={operationDate}
                    onChange={(e) => setOperationDate(e.target.value)}
                  />
                </div>
              </div>
              <button className="btn btnPrimary" disabled={busy}>
                {busy ? "Enregistrement..." : "Enregistrer"}
              </button>
              {msg && <div className="notice">{msg}</div>}
            </form>
          </Card>
        )}

        <Card title="Historique des sorties">
          <TableToolbar
            exportFilename="outgoing"
            exportTitle="Historique des sorties"
            onRefresh={refresh}
            rows={
              selectedIds.length > 0
                ? filtered.filter((h) => h.id && selectedIds.includes(h.id))
                : filtered
            }
            selectedCount={selectedIds.length}
            onGlobalDelete={handleGlobalDelete}
            columns={[
              { header: "Référence", cell: (h) => h.reference },
              { header: "Numéro de lot", cell: (h) => h.lotNumber },
              { header: "Quantité", cell: (h) => h.quantityOut },
              {
                header: "Date",
                cell: (h) => formatDateTime(h.operationDate),
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
                    placeholder="Ex: REF-001"
                  />
                </div>
                <div className="field">
                  <div className="label">Numéro de lot</div>
                  <input
                    className="input"
                    value={draftFilters.lotNumber}
                    onChange={(e) =>
                      setDraftFilters((prev) => ({
                        ...prev,
                        lotNumber: e.target.value,
                      }))
                    }
                    placeholder="Ex: LOT-123"
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
              rowKey={(h) =>
                h.id || `${h.reference}-${h.lotNumber}-${h.operationDate}`
              }
              rowClassName={(h) =>
                h.lastModifiedAt !== null ? "rowEdited" : undefined
              }
              columns={[
                { header: "Référence", cell: (h) => h.reference },
                { header: "Numéro de lot", cell: (h) => h.lotNumber },
                {
                  header: "Quantité",
                  cell: (h) => (
                    <span className="badge badgeBlue">{h.quantityOut}</span>
                  ),
                },
                {
                  header: "Date",
                  cell: (h) => formatDateTime(h.operationDate),
                },
                {
                  header: "État",
                  cell: (h) =>
                    h.lastModifiedAt !== null ? (
                      isAdmin ? (
                        <button
                          className="badge badgeEdited"
                          onClick={() => openAudit(h)}
                        >
                          Modifié
                        </button>
                      ) : (
                        <span className="badge badgeEdited">Modifié</span>
                      )
                    ) : (
                      "-"
                    ),
                },
                {
                  header: "",
                  cell: (h) => (
                    <div
                      className="actionsRow"
                      style={{ justifyContent: "flex-end" }}
                    >
                      <button
                        className="btn btnGhost"
                        style={{ padding: 4 }}
                        onClick={() => openEdit(h)}
                        title="Modifier"
                      >
                        <img
                          src={editIcon}
                          alt="Edit"
                          style={{ width: 16, height: 16 }}
                        />
                      </button>
                    </div>
                  ),
                },
              ]}
            />
          )}
        </Card>
      </div>

      <QrScannerModal
        open={scanOpen}
        onClose={() => setScanOpen(false)}
        onScan={onScan}
      />

      <Modal
        open={editOpen}
        title="Modifier la sortie"
        onClose={() => setEditOpen(false)}
        actions={
          <button className="btn btnPrimary" onClick={saveEdit}>
            Enregistrer
          </button>
        }
      >
        <div className="form">
          <div className="field">
            <div className="label">Référence</div>
            <input
              className="input"
              value={eReference}
              onChange={(e) => setEReference(e.target.value)}
            />
          </div>
          <div className="grid2">
            <div className="field">
              <div className="label">Numéro de lot</div>
              <input
                className="input"
                value={eLotNumber}
                onChange={(e) => setELotNumber(e.target.value)}
              />
            </div>
            <div className="field">
              <div className="label">Quantité sortie</div>
              <input
                className="input"
                type="number"
                min={0}
                value={eQuantityOut}
                onChange={(e) => setEQuantityOut(Number(e.target.value) || 0)}
              />
            </div>
          </div>
          <div className="grid2">
            <div className="field">
              <div className="label">Date d'opération</div>
              <input
                className="input"
                type="datetime-local"
                value={eOperationDate}
                onChange={(e) => setEOperationDate(e.target.value)}
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
