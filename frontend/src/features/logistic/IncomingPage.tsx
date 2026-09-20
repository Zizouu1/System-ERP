import { useEffect, useMemo, useState } from "react";
import { useSearchParams } from "react-router-dom";
import editIcon from "@/assets/edit.png";
import qrCodeIcon from "@/assets/qr-code.png";
import { Card } from "@/components/Card";
import { AutocompleteInput } from "@/components/AutocompleteInput";
import {
  logisticIncoming,
  logisticIncomingDelete,
  logisticIncomingHistory,
  logisticIncomingUpdate,
  operationAuditHistory,
} from "@/shared/api/endpoints";
import { ensureDataUrlMaybeBase64 } from "@/utils/media";
import { useAuth } from "@/app/providers/AuthProvider";
import { DataTable } from "@/components/DataTable";
import { Spinner } from "@/components/Spinner";
import type {
  IncomingMaterialDto,
  OperationAuditEntryDto,
} from "@/shared/api/types";
import { TableToolbar } from "@/shared/ui/TableToolbar/TableToolbar";
import { useConfirmationDialog } from "@/shared/ui/ConfirmationDialog/useConfirmationDialog";
import { Modal } from "@/components/Modal";
import { OperationAuditModal } from "@/components/OperationAuditModal";
import { QrScannerModal } from "@/components/QrScannerModal";
import { QrLabelView } from "@/components/QrLabelView";
import {
  buildQrDisplayData,
  loadLatestQrLabel,
  parseQrTextToFormData,
  payloadToQrFormData,
  saveLatestQrLabel,
  type PersistedQrLabel,
} from "@/utils/qr";
import { matchesDateRange, matchesText } from "@/utils/tableFilters";

type IncomingTableFilters = {
  reference: string;
  lotNumber: string;
  startDate: string;
  endDate: string;
};

const EMPTY_FILTERS: IncomingTableFilters = {
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

export default function LogisticIncomingPage() {
  const { askConfirmation, confirmationDialog } = useConfirmationDialog();
  const { role } = useAuth();
  const isAdmin = role === "admin";

  const [reference, setReference] = useState("");
  const [quantity, setQuantity] = useState<number>(1);
  const [lotNumber, setLotNumber] = useState("");
  const [generateQr, setGenerateQr] = useState(false);
  const [busy, setBusy] = useState(false);
  const [msg, setMsg] = useState<string | null>(null);
  const [latestQr, setLatestQr] = useState<PersistedQrLabel | null>(() =>
    loadLatestQrLabel(),
  );
  const [scanOpen, setScanOpen] = useState(false);
  const [operationDate, setOperationDate] = useState("");

  const [history, setHistory] = useState<IncomingMaterialDto[]>([]);
  const [loading, setLoading] = useState(false);
  const [filters, setFilters] = useState<IncomingTableFilters>(EMPTY_FILTERS);
  const [draftFilters, setDraftFilters] =
    useState<IncomingTableFilters>(EMPTY_FILTERS);
  const [selectedIds, setSelectedIds] = useState<(string | number)[]>([]);

  const [editOpen, setEditOpen] = useState(false);
  const [editRow, setEditRow] = useState<IncomingMaterialDto | null>(null);
  const [eReference, setEReference] = useState("");
  const [eQuantity, setEQuantity] = useState<number>(1);
  const [eLotNumber, setELotNumber] = useState("");
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
      const rows = await logisticIncomingHistory();
      setHistory(rows);
      return rows;
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

  function applyScanToForm(text: string) {
    const parsed = parseQrTextToFormData(text);
    if (parsed.reference) setReference(parsed.reference);
    if (typeof parsed.quantity === "number" && !Number.isNaN(parsed.quantity)) {
      setQuantity(parsed.quantity);
    }
    if (parsed.lotNumber) setLotNumber(parsed.lotNumber);
  }

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setBusy(true);
    setMsg(null);

    try {
      const ref = reference.trim();
      const lot = lotNumber.trim();
      const blob = await logisticIncoming({
        reference: ref,
        quantity,
        lotNumber: lot,
        generateQr,
        operationDate: toLocalDateTime(operationDate),
      });

      const rows = await refresh();
      const resolvedLot = lot || rows[0]?.lotNumber || lot;

      if (generateQr && blob.size > 0) {
        // Step 5: Log blob details for debugging
        console.log("[QR-incoming] blob type:", blob.type, "size:", blob.size);

        // Validate the blob is actually an image
        if (!blob.type.startsWith("image/")) {
          console.error(
            "[QR-incoming] API returned non-image blob:",
            blob.type,
          );
          // Try to read as text to see what the server actually sent
          const text = await blob.text();
          console.error(
            "[QR-incoming] blob content preview:",
            text.substring(0, 200),
          );
          alert("Le serveur n'a pas retourné une image QR valide.");
        } else {
          // Step 6: Use object URL instead of data URL for reliability
          const qrObjectUrl = URL.createObjectURL(blob);
          console.log("[QR-incoming] object URL created:", qrObjectUrl);

          const entry: PersistedQrLabel = {
            imageDataUrl: qrObjectUrl,
            display: buildQrDisplayData({
              reference: ref,
              quantity,
              lotNumber: resolvedLot,
            }),
          };
          setLatestQr(entry);
          saveLatestQrLabel(entry);
        }
      }
    } catch (err: any) {
      setMsg(err?.message ?? "Erreur.");
    } finally {
      setBusy(false);
    }
  }

  function openEdit(row: IncomingMaterialDto) {
    setEditRow(row);
    setEReference(row.reference ?? "");
    setEQuantity(Number(row.quantity) || 0);
    setELotNumber(row.lotNumber ?? "");
    setEOperationDate(toDateTimeLocal(row.operationDate));
    setEditOpen(true);
  }

  async function saveEdit() {
    if (!editRow?.id) return;
    try {
      const updated = await logisticIncomingUpdate(editRow.id, {
        reference: eReference.trim(),
        quantity: eQuantity,
        lotNumber: eLotNumber.trim(),
        operationDate: toLocalDateTime(eOperationDate),
      });

      if (updated?.qrImageBase64 && updated.qrImageBase64.trim()) {
        const refreshedLabel: PersistedQrLabel = {
          imageDataUrl: ensureDataUrlMaybeBase64(updated.qrImageBase64),
          display: buildQrDisplayData({
            reference: updated.reference,
            quantity: updated.quantity,
            lotNumber: updated.lotNumber,
            generatedAt: updated.generatedAt,
          }),
        };
        setLatestQr(refreshedLabel);
        saveLatestQrLabel(refreshedLabel);
      }

      setEditOpen(false);
      await refresh();
    } catch (err: any) {
      setMsg(err?.message ?? "Erreur lors de la mise à jour.");
    }
  }

  async function remove(row: IncomingMaterialDto) {
    if (!row.id) return;
    const confirmed = await askConfirmation({
      title: "Supprimer l'entrée",
      message: "Voulez-vous vraiment supprimer cette entrée ?",
    });
    if (!confirmed) return;

    try {
      await logisticIncomingDelete(row.id);
      setMsg("Entrée supprimée avec succès.");
      await refresh();
    } catch (err: any) {
      setMsg(err?.message ?? "Erreur lors de la suppression.");
    }
  }

  async function handleGlobalDelete() {
    const confirmed = await askConfirmation({
      title: "Supprimer",
      message: `Voulez-vous vraiment supprimer ${selectedIds.length} entrée(s) ?`,
    });
    if (!confirmed) return;
    setMsg(null);
    setLoading(true);
    try {
      for (const id of selectedIds) {
        await logisticIncomingDelete(Number(id));
      }
      setMsg("Supprimés avec succès.");
      setSelectedIds([]);
      await refresh();
    } catch (err: any) {
      setMsg(err?.message ?? "Erreur lors de la suppression.");
    } finally {
      setLoading(false);
    }
  }

  async function openAudit(row: IncomingMaterialDto) {
    if (!isAdmin || !row.id) return;
    setAuditOpen(true);
    setAuditLoading(true);
    setAuditError(null);
    setAuditEntries([]);
    try {
      const entries = await operationAuditHistory("INCOMING", row.id);
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
          <div className="historyTopSplit">
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
                  <div className="label">Réf</div>
                  <AutocompleteInput
                    value={reference}
                    onChange={setReference}
                    onScan={(payload) => {
                      const parsed = payloadToQrFormData(payload);
                      if (parsed.reference) setReference(parsed.reference);
                      if (
                        typeof parsed.quantity === "number" &&
                        !Number.isNaN(parsed.quantity)
                      ) {
                        setQuantity(parsed.quantity);
                      }
                      if (parsed.lotNumber) setLotNumber(parsed.lotNumber);
                    }}
                    required
                    productType="MATIERE_PREMIERE"
                  />
                </div>
                <div className="grid2">
                  <div className="field">
                    <div className="label">Quantité</div>
                    <input
                      className="input"
                      type="text"
                      inputMode="numeric"
                      pattern="[0-9]*"
                      value={quantity}
                      onChange={(e) => {
                        const val = e.target.value.replace(/[^0-9]/g, "");
                        setQuantity(Number(val) || 0);
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
                      placeholder="Ex: LOT2025A"
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
                <label
                  className="field"
                  style={{ display: "flex", alignItems: "center", gap: 10 }}
                >
                  <input
                    type="checkbox"
                    checked={generateQr}
                    onChange={(e) => setGenerateQr(e.target.checked)}
                  />
                  <span>Générer un QR code</span>
                </label>
                <button className="btn btnPrimary" disabled={busy}>
                  {busy ? "Enregistrement..." : "Enregistrer"}
                </button>
                {msg && <div className="notice">{msg}</div>}
              </form>
            </Card>

            <Card title="Prévisualisation QR">
              {latestQr ? (
                <QrLabelView label={latestQr} />
              ) : (
                <div className="muted">Aucun QR généré pour le moment.</div>
              )}
            </Card>
          </div>
        )}

        <Card className="historyTableFull" title="Historique des entrées">
          <TableToolbar
            exportFilename="incoming"
            exportTitle="Historique des entrées matières"
            onRefresh={refresh}
            rows={
              selectedIds.length > 0
                ? filtered.filter((h) => h.id && selectedIds.includes(h.id))
                : filtered
            }
            selectedCount={selectedIds.length}
            onGlobalDelete={handleGlobalDelete}
            columns={[
              { header: "Réf", cell: (h) => h.reference },
              { header: "Numéro de lot", cell: (h) => h.lotNumber },
              { header: "Quantité", cell: (h) => h.quantity },
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
                    placeholder="Ex: LOT2025A"
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
                { header: "Réf", cell: (h) => h.reference },
                { header: "Numéro de lot", cell: (h) => h.lotNumber },
                {
                  header: "Quantité",
                  cell: (h) => (
                    <span className="badge badgeBlue">{h.quantity}</span>
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
        onScan={applyScanToForm}
      />

      <Modal
        open={editOpen}
        title="Modifier l'entrée"
        onClose={() => setEditOpen(false)}
        actions={
          <button className="btn btnPrimary" onClick={saveEdit}>
            Enregistrer
          </button>
        }
      >
        <div className="form">
          <div className="field">
            <div className="label">Réf</div>
            <input
              className="input"
              value={eReference}
              onChange={(e) => setEReference(e.target.value)}
            />
          </div>
          <div className="grid2">
            <div className="field">
              <div className="label">Quantité</div>
              <input
                className="input"
                type="number"
                min={0}
                value={eQuantity}
                onChange={(e) => setEQuantity(Number(e.target.value) || 0)}
              />
            </div>
            <div className="field">
              <div className="label">Numéro de lot</div>
              <input
                className="input"
                value={eLotNumber}
                onChange={(e) => setELotNumber(e.target.value)}
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
