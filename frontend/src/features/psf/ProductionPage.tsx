import { useEffect, useMemo, useState } from "react";
import { useSearchParams } from "react-router-dom";
import editIcon from "@/assets/edit.png";
import { Card } from "@/components/Card";
import {
  operationAuditHistory,
  psfProduction,
  psfProductionDelete,
  psfProductionHistory,
  psfProductionUpdate,
} from "@/shared/api/endpoints";
import { AutocompleteInput } from "@/components/AutocompleteInput";
import { ensureDataUrlMaybeBase64 } from "@/utils/media";
import { useAuth } from "@/app/providers/AuthProvider";
import { DataTable } from "@/components/DataTable";
import { Spinner } from "@/components/Spinner";
import { TableToolbar } from "@/shared/ui/TableToolbar/TableToolbar";
import type {
  OperationAuditEntryDto,
  PsfProductionDto,
  QrLabelDto,
} from "@/shared/api/types";
import { useConfirmationDialog } from "@/shared/ui/ConfirmationDialog/useConfirmationDialog";
import { Modal } from "@/components/Modal";
import { OperationAuditModal } from "@/components/OperationAuditModal";
import { EmployeeAutocompleteInput } from "@/components/EmployeeAutocompleteInput";
import { QrLabelView } from "@/components/QrLabelView";
import {
  buildQrDisplayData,
  loadLatestQrLabel,
  parseQrTextToFormData,
  saveLatestQrLabel,
  type PersistedQrLabel,
} from "@/utils/qr";
import { matchesDateRange, matchesText } from "@/utils/tableFilters";

function toLocalDateTime(value: string): string {
  if (!value) return value;
  if (/Z|[+-]\d{2}:\d{2}$/.test(value)) return value;
  const match = value.match(/^(\d{4}-\d{2}-\d{2}T\d{2}:\d{2})(:\d{2})?$/);
  if (match) {
    return `${match[1]}${match[2] ?? ":00"}`;
  }
  return value;
}

function toTimeInput(value?: string) {
  if (!value) return "";
  const base = value.includes("T") ? value.split("T")[1] : value;
  return base.slice(0, 5);
}

function formatDateTime(value?: string, fallbackDate?: string) {
  if (!value && !fallbackDate) return "-";

  if (value) {
    const parsed = new Date(value);
    if (!Number.isNaN(parsed.getTime())) {
      return parsed.toLocaleString("fr-FR");
    }

    const timeMatch = /^(\d{2}):(\d{2})(?::(\d{2}))?$/.exec(value);
    if (timeMatch && fallbackDate) {
      const baseDate = new Date(fallbackDate);
      if (!Number.isNaN(baseDate.getTime())) {
        baseDate.setHours(
          Number(timeMatch[1]),
          Number(timeMatch[2]),
          Number(timeMatch[3] ?? "0"),
          0,
        );
        return baseDate.toLocaleString("fr-FR");
      }
    }
    return value;
  }

  const fallbackParsed = new Date(fallbackDate as string);
  if (Number.isNaN(fallbackParsed.getTime())) return "-";
  return fallbackParsed.toLocaleString("fr-FR");
}

function mapLabelDtoToPersisted(item: QrLabelDto): PersistedQrLabel | null {
  if (!item.qrImageBase64 || !item.qrImageBase64.trim()) return null;
  return {
    imageDataUrl: ensureDataUrlMaybeBase64(item.qrImageBase64),
    display: buildQrDisplayData({
      reference: item.reference,
      quantity: item.quantity,
      lotNumber: item.lotNumber,
      generatedAt: item.generatedAt,
    }),
  };
}

type ProductionFilters = {
  reference: string;
  operatorMatricule: string;
  startDate: string;
  endDate: string;
};

const EMPTY_FILTERS: ProductionFilters = {
  reference: "",
  operatorMatricule: "",
  startDate: "",
  endDate: "",
};

export default function PsfProductionPage() {
  const { askConfirmation, confirmationDialog } = useConfirmationDialog();
  const { role } = useAuth();
  const isAdmin = role === "admin";

  const [reference, setReference] = useState("");
  const [qty, setQty] = useState<number>(1);
  const [operatorMatricule, setOperatorMatricule] = useState("");
  const [start, setStart] = useState("");
  const [end, setEnd] = useState("");
  const [scrap, setScrap] = useState<number>(0);
  const [busy, setBusy] = useState(false);
  const [msg, setMsg] = useState<string | null>(null);
  const [qrs, setQrs] = useState<PersistedQrLabel[]>([]);
  const [latestQr, setLatestQr] = useState<PersistedQrLabel | null>(() =>
    loadLatestQrLabel(),
  );
  const [quantityPerBatch, setQuantityPerBatch] = useState<number>(1);
  const [producedByCutMachine, setProducedByCutMachine] = useState(false);

  const [history, setHistory] = useState<PsfProductionDto[]>([]);
  const [loading, setLoading] = useState(false);
  const [filters, setFilters] = useState<ProductionFilters>(EMPTY_FILTERS);
  const [draftFilters, setDraftFilters] =
    useState<ProductionFilters>(EMPTY_FILTERS);
  const [selectedIds, setSelectedIds] = useState<(string | number)[]>([]);

  const [editOpen, setEditOpen] = useState(false);
  const [editRow, setEditRow] = useState<PsfProductionDto | null>(null);
  const [eReference, setEReference] = useState("");
  const [eQty, setEQty] = useState<number>(1);
  const [eBatchQty, setEBatchQty] = useState<number>(1);
  const [eOperatorMatricule, setEOperatorMatricule] = useState("");
  const [eScrap, setEScrap] = useState<number>(0);
  const [eProducedByCutMachine, setEProducedByCutMachine] = useState(false);
  const [eStart, setEStart] = useState("");
  const [eEnd, setEEnd] = useState("");

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

  async function refreshHistory() {
    setLoading(true);
    try {
      setHistory(await psfProductionHistory());
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    refreshHistory();
  }, []);

  const filteredHistory = useMemo(() => {
    return history.filter(
      (h) =>
        matchesText(h.reference, filters.reference) &&
        matchesText(h.operatorMatricule, filters.operatorMatricule) &&
        matchesDateRange(
          h.timestamp || h.startTime,
          filters.startDate,
          filters.endDate,
        ),
    );
  }, [history, filters]);

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setBusy(true);
    setMsg(null);
    setQrs([]);
    try {
      const res = await psfProduction({
        reference: reference.trim(),
        quantity: qty,
        quantityPerBatch,
        operatorMatricule: operatorMatricule.trim(),
        scrapQuantity: scrap,
        producedByCutMachine,
        startTime: toLocalDateTime(start),
        endTime: toLocalDateTime(end),
      });

      const labels: PersistedQrLabel[] = [];
      if (Array.isArray(res)) {
        for (const item of res) {
          if (item && typeof item === "object" && "qrImageBase64" in item) {
            const mapped = mapLabelDtoToPersisted(item as QrLabelDto);
            if (mapped) labels.push(mapped);
          }
        }
      }

      setQrs(labels);
      if (labels.length > 0) {
        const latest = labels[labels.length - 1];
        setLatestQr(latest);
        saveLatestQrLabel(latest);
      }

      await refreshHistory();
    } catch (err: any) {
      setMsg(err?.message ?? "Erreur.");
    } finally {
      setBusy(false);
    }
  }

  function handleAutoSplit(text: string) {
    if (text.includes("$") || text.includes("|") || text.includes(";")) {
      const parsed = parseQrTextToFormData(text);
      if (parsed.reference) setReference(parsed.reference);
      if (
        typeof parsed.quantity === "number" &&
        !Number.isNaN(parsed.quantity)
      ) {
        setQty(parsed.quantity);
      }
      return;
    }
    setReference(text);
  }

  function openEdit(row: PsfProductionDto) {
    setEditRow(row);
    setEReference(row.reference ?? "");
    setEQty(Number(row.quantity) || 0);
    setEBatchQty(Number(row.quantityPerBatch) || 1);
    setEOperatorMatricule(row.operatorMatricule ?? "");
    setEScrap(Number(row.scrapQuantity) || 0);
    setEProducedByCutMachine(Boolean(row.producedByCutMachine));
    setEStart(toTimeInput(row.startTime));
    setEEnd(toTimeInput(row.endTime));
    setEditOpen(true);
  }

  async function saveEdit() {
    if (!editRow?.id) return;
    try {
      const updated = await psfProductionUpdate(editRow.id, {
        reference: eReference.trim(),
        quantity: eQty,
        quantityPerBatch: eBatchQty,
        operatorMatricule: eOperatorMatricule.trim(),
        scrapQuantity: eScrap,
        producedByCutMachine: eProducedByCutMachine,
        startTime: eStart,
        endTime: eEnd,
      });

      const labels = (updated?.qrLabels ?? [])
        .map(mapLabelDtoToPersisted)
        .filter((l): l is PersistedQrLabel => l !== null);
      setQrs(labels);
      if (labels.length > 0) {
        const latest = labels[labels.length - 1];
        setLatestQr(latest);
        saveLatestQrLabel(latest);
      }

      setEditOpen(false);
      await refreshHistory();
    } catch (err: any) {
      setMsg(err?.message ?? "Erreur lors de la mise à jour.");
    }
  }

  async function remove(row: PsfProductionDto) {
    if (!row.id) return;
    const confirmed = await askConfirmation({
      title: "Supprimer la production PSF",
      message: "Voulez-vous vraiment supprimer cette production ?",
    });
    if (!confirmed) return;
    try {
      await psfProductionDelete(row.id);
      setMsg("Production supprimée avec succès.");
      await refreshHistory();
    } catch (err: any) {
      setMsg(err?.message ?? "Erreur lors de la suppression.");
    }
  }

  async function handleGlobalDelete() {
    const confirmed = await askConfirmation({
      title: "Supprimer",
      message: `Voulez-vous vraiment supprimer ${selectedIds.length} production(s) PSF ?`,
    });
    if (!confirmed) return;
    setMsg(null);
    setLoading(true);
    try {
      for (const id of selectedIds) {
        await psfProductionDelete(Number(id));
      }
      setMsg("Supprimés avec succès.");
      setSelectedIds([]);
      await refreshHistory();
    } catch (err: any) {
      setMsg(err?.message ?? "Erreur lors de la suppression.");
    } finally {
      setLoading(false);
    }
  }

  async function openAudit(row: PsfProductionDto) {
    if (!isAdmin || !row.id) return;
    setAuditOpen(true);
    setAuditLoading(true);
    setAuditError(null);
    setAuditEntries([]);
    try {
      const entries = await operationAuditHistory("PSF_PRODUCTION", row.id);
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

  const visibleQrs = qrs.length > 0 ? qrs : latestQr ? [latestQr] : [];
  const showGrid = visibleQrs.length > 1;

  return (
    <div className="page mobilePage">
      <div className="grid">
        {role === "psf" && (
          <div className="historyTopSplit">
            <Card title="Déclarer production">
              <form className="form" onSubmit={submit}>
                <div className="field">
                  <div className="label">Matricule opérateur</div>
                  <EmployeeAutocompleteInput
                    value={operatorMatricule}
                    onChange={setOperatorMatricule}
                    placeholder="Ex: OP123"
                    required
                  />
                </div>
                <div className="field">
                  <div className="label">Réf</div>
                  <AutocompleteInput
                    value={reference}
                    onChange={(val) => handleAutoSplit(val)}
                    placeholder="Référence produit"
                    required
                    productType="SEMI_FINI"
                  />
                </div>
                <div className="grid2">
                  <div className="field">
                    <div className="label">Quantité produite</div>
                    <input
                      className="input"
                      type="text"
                      inputMode="numeric"
                      pattern="[0-9]*"
                      value={qty}
                      onChange={(e) => {
                        const val = e.target.value.replace(/[^0-9]/g, "");
                        setQty(Number(val) || 0);
                      }}
                      required
                    />
                  </div>
                  <div className="field">
                    <div className="label">Rebuts</div>
                    <input
                      className="input"
                      type="text"
                      inputMode="numeric"
                      pattern="[0-9]*"
                      value={scrap}
                      onChange={(e) => {
                        const val = e.target.value.replace(/[^0-9]/g, "");
                        setScrap(Number(val) || 0);
                      }}
                    />
                  </div>
                </div>
                <div className="grid2">
                  <div className="field">
                    <div className="label">Début</div>
                    <input
                      className="input"
                      type="datetime-local"
                      value={start}
                      onChange={(e) => setStart(e.target.value)}
                      required
                    />
                  </div>
                  <div className="field">
                    <div className="label">Fin</div>
                    <input
                      className="input"
                      type="datetime-local"
                      value={end}
                      onChange={(e) => setEnd(e.target.value)}
                      required
                    />
                  </div>
                </div>
                <div className="grid2">
                  <div className="field">
                    <div className="label">Quantité par lot</div>
                    <input
                      className="input"
                      type="text"
                      inputMode="numeric"
                      pattern="[0-9]*"
                      value={quantityPerBatch}
                      onChange={(e) => {
                        const val = e.target.value.replace(/[^0-9]/g, "");
                        setQuantityPerBatch(Number(val) || 1);
                      }}
                      required
                    />
                  </div>
                  <div
                    className="field"
                    style={{
                      display: "flex",
                      alignItems: "center",
                      gap: 8,
                      marginTop: 24,
                    }}
                  >
                    <input
                      type="checkbox"
                      id="cutMachine"
                      checked={producedByCutMachine}
                      onChange={(e) =>
                        setProducedByCutMachine(e.target.checked)
                      }
                    />
                    <label htmlFor="cutMachine">
                      Produit par machine de découpe
                    </label>
                  </div>
                </div>
                <button className="btn btnPrimary" disabled={busy}>
                  {busy ? "Enregistrement..." : "Enregistrer"}
                </button>
                {msg && <div className="notice">{msg}</div>}
              </form>
            </Card>

            <Card title={showGrid ? "Étiquettes QR" : "Étiquette QR"}>
              {visibleQrs.length > 0 ? (
                <div className={showGrid ? "qrGrid" : "grid"}>
                  {visibleQrs.map((label, idx) => (
                    <QrLabelView
                      key={`${label.display.lotNumber}-${idx}`}
                      label={label}
                      printButtonLabel="Imprimer"
                    />
                  ))}
                </div>
              ) : (
                <div className="muted">
                  Aucune étiquette QR générée pour le moment.
                </div>
              )}
            </Card>
          </div>
        )}

        <Card className="historyTableFull" title="Productions PSF - Historique">
          <TableToolbar
            exportFilename="psf_production"
            exportTitle="Historique des productions PSF"
            onRefresh={refreshHistory}
            rows={
              selectedIds.length > 0
                ? filteredHistory.filter(
                    (h) => h.id && selectedIds.includes(h.id),
                  )
                : filteredHistory
            }
            selectedCount={selectedIds.length}
            onGlobalDelete={handleGlobalDelete}
            columns={[
              {
                header: "Début",
                cell: (h) => formatDateTime(h.startTime, h.timestamp),
              },
              {
                header: "Fin",
                cell: (h) => formatDateTime(h.endTime, h.timestamp),
              },
              { header: "Réf", cell: (h) => h.reference },
              { header: "Qté produite", cell: (h) => h.quantity },
              { header: "Qté batch", cell: (h) => h.quantityPerBatch },
              { header: "Rebuts", cell: (h) => h.scrapQuantity },
              { header: "Opérateur", cell: (h) => h.operatorMatricule },
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
                    placeholder="Ex: SF-001"
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
                    placeholder="Ex: OP100"
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
              rows={filteredHistory}
              rowKey={(h) => h.id}
              rowClassName={(h) =>
                h.lastModifiedAt !== null ? "rowEdited" : undefined
              }
              columns={[
                {
                  header: "Début",
                  cell: (h) => formatDateTime(h.startTime, h.timestamp),
                },
                {
                  header: "Fin",
                  cell: (h) => formatDateTime(h.endTime, h.timestamp),
                },
                { header: "Réf", cell: (h) => h.reference },
                {
                  header: "Qté produite",
                  cell: (h) => (
                    <span className="badge badgeBlue">{h.quantity}</span>
                  ),
                },
                { header: "Qté batch", cell: (h) => h.quantityPerBatch },
                {
                  header: "Rebuts",
                  cell: (h) => (
                    <span className="badge badgeRed">{h.scrapQuantity}</span>
                  ),
                },
                {
                  header: "Coupe",
                  cell: (h) => (h.producedByCutMachine ? "Oui" : "Non"),
                  className: "textCenter",
                },
                { header: "Opérateur", cell: (h) => h.operatorMatricule },
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

      <Modal
        open={editOpen}
        title="Modifier la production PSF"
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
              <div className="label">Quantité produite</div>
              <input
                className="input"
                type="number"
                min={0}
                value={eQty}
                onChange={(e) => setEQty(Number(e.target.value) || 0)}
              />
            </div>
            <div className="field">
              <div className="label">Quantité par lot</div>
              <input
                className="input"
                type="number"
                min={1}
                value={eBatchQty}
                onChange={(e) => setEBatchQty(Number(e.target.value) || 1)}
              />
            </div>
          </div>
          <div className="grid2">
            <div className="field">
              <div className="label">Rebuts</div>
              <input
                className="input"
                type="number"
                min={0}
                value={eScrap}
                onChange={(e) => setEScrap(Number(e.target.value) || 0)}
              />
            </div>
            <div className="field">
              <div className="label">Matricule opérateur</div>
              <EmployeeAutocompleteInput
                value={eOperatorMatricule}
                onChange={setEOperatorMatricule}
                placeholder="Ex: OP123"
              />
            </div>
          </div>
          <div className="grid2">
            <div className="field">
              <div className="label">Début</div>
              <input
                className="input"
                type="time"
                value={eStart}
                onChange={(e) => setEStart(e.target.value)}
              />
            </div>
            <div className="field">
              <div className="label">Fin</div>
              <input
                className="input"
                type="time"
                value={eEnd}
                onChange={(e) => setEEnd(e.target.value)}
              />
            </div>
          </div>
          <div
            className="field"
            style={{ display: "flex", alignItems: "center", gap: 8 }}
          >
            <input
              id="edit-cut-machine"
              type="checkbox"
              checked={eProducedByCutMachine}
              onChange={(e) => setEProducedByCutMachine(e.target.checked)}
            />
            <label htmlFor="edit-cut-machine">
              Produit par machine de découpe
            </label>
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
