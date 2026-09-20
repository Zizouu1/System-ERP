import { useEffect, useMemo, useState } from "react";
import { useSearchParams } from "react-router-dom";
import editIcon from "@/assets/edit.png";
import { Card } from "@/components/Card";
import { DataTable } from "@/components/DataTable";
import { Spinner } from "@/components/Spinner";
import {
  dep1StockDelete,
  dep1StockUpdate,
  logisticStock,
} from "@/shared/api/endpoints";
import type { StockDep1Dto } from "@/shared/api/types";
import { useAuth } from "@/app/providers/AuthProvider";
import { TableToolbar } from "@/shared/ui/TableToolbar/TableToolbar";
import { Modal } from "@/components/Modal";
import { useConfirmationDialog } from "@/shared/ui/ConfirmationDialog/useConfirmationDialog";
import { matchesExact, matchesText } from "@/utils/tableFilters";

const PRODUCT_TYPE_DISPLAY_NAMES: Record<string, string> = {
  MATIERE_PREMIERE: "matiere premiere",
  SEMI_FINI: "semi-fini",
  PRODUIT_FINI: "produit fini",
};

function getProductTypeDisplayName(type: string): string {
  return PRODUCT_TYPE_DISPLAY_NAMES[type] || type;
}

type AggregatedStockRow = {
  id: number;
  rowIds: number[];
  reference: string;
  productType: string;
  totalQuantity: number;
  storeQuantity: number;
  transferedQuantity: number;
};

function aggregateStockRows(rows: StockDep1Dto[]): AggregatedStockRow[] {
  const map = new Map<string, AggregatedStockRow>();

  for (const row of rows) {
    const key = String(row.reference ?? "").trim();
    if (!key) continue;

    const total = Number(row.totalQuantity) || 0;
    const store = Number(row.storeQuantity) || 0;
    const existing = map.get(key);

    if (existing) {
      existing.totalQuantity += total;
      existing.storeQuantity += store;
      existing.rowIds.push(row.id);
      continue;
    }

    map.set(key, {
      id: row.id,
      rowIds: [row.id],
      reference: key,
      productType: row.productType,
      totalQuantity: total,
      storeQuantity: store,
      transferedQuantity: 0,
    });
  }

  return Array.from(map.values())
    .map((row) => ({
      ...row,
      transferedQuantity: row.totalQuantity - row.storeQuantity,
    }))
    .sort((a, b) =>
      a.reference.localeCompare(b.reference, "fr", { sensitivity: "base" }),
    );
}

type StockTableFilters = {
  reference: string;
  productType: string;
};

const EMPTY_FILTERS: StockTableFilters = {
  reference: "",
  productType: "",
};

export default function LogisticStockPage() {
  const { role } = useAuth();
  const isAdmin = role === "admin";
  const { askConfirmation, confirmationDialog } = useConfirmationDialog();

  const [loading, setLoading] = useState(true);
  const [rawRows, setRawRows] = useState<StockDep1Dto[]>([]);
  const [filters, setFilters] = useState<StockTableFilters>(EMPTY_FILTERS);
  const [draftFilters, setDraftFilters] =
    useState<StockTableFilters>(EMPTY_FILTERS);
  const [msg, setMsg] = useState<string | null>(null);
  const [selectedIds, setSelectedIds] = useState<(string | number)[]>([]);

  const [editOpen, setEditOpen] = useState(false);
  const [editRow, setEditRow] = useState<AggregatedStockRow | null>(null);
  const [eTotal, setETotal] = useState(0);
  const [eStore, setEStore] = useState(0);
  const [searchParams] = useSearchParams();
  const focusedReference = useMemo(() => {
    const ref = searchParams.get("focusRef");
    return ref?.trim() ? ref.trim() : undefined;
  }, [searchParams]);

  async function refresh() {
    setLoading(true);
    try {
      setRawRows(await logisticStock());
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    refresh();
  }, []);

  const rows = useMemo(() => aggregateStockRows(rawRows), [rawRows]);

  const filtered = useMemo(() => {
    return rows.filter((r) => {
      return (
        matchesText(r.reference, filters.reference) &&
        matchesExact(r.productType, filters.productType)
      );
    });
  }, [rows, filters]);

  function openEdit(row: AggregatedStockRow) {
    if (row.rowIds.length !== 1) {
      setMsg(
        "Modification indisponible: cette référence contient plusieurs entrées.",
      );
      return;
    }
    setEditRow(row);
    setETotal(Number(row.totalQuantity) || 0);
    setEStore(Number(row.storeQuantity) || 0);
    setEditOpen(true);
  }

  async function saveEdit() {
    if (!editRow?.id || editRow.rowIds.length !== 1) return;
    try {
      await dep1StockUpdate(editRow.rowIds[0], {
        totalQuantity: eTotal,
        storeQuantity: eStore,
      });
      setEditOpen(false);
      await refresh();
    } catch (e: any) {
      setMsg(e?.message ?? "Erreur lors de la mise à jour.");
    }
  }

  async function remove(row: AggregatedStockRow) {
    if (row.rowIds.length !== 1) {
      setMsg(
        "Suppression indisponible: cette référence contient plusieurs entrées.",
      );
      return;
    }
    const confirmed = await askConfirmation({
      title: "Supprimer la ligne de stock",
      message: "Voulez-vous vraiment supprimer cette ligne ?",
    });
    if (!confirmed) return;
    try {
      await dep1StockDelete(row.rowIds[0]);
      setMsg("Ligne supprimée avec succès.");
      await refresh();
    } catch (e: any) {
      setMsg(e?.message ?? "Erreur lors de la suppression.");
    }
  }

  async function handleGlobalDelete() {
    const selectedRows = rows.filter((r) => selectedIds.includes(r.reference));
    const allRowIds = selectedRows.flatMap((r) => r.rowIds);

    const confirmed = await askConfirmation({
      title: "Supprimer",
      message: `Voulez-vous vraiment supprimer ${selectedIds.length} référence(s) (${allRowIds.length} entrées) de stock ?`,
    });
    if (!confirmed) return;
    setMsg(null);
    setLoading(true);
    try {
      for (const id of allRowIds) {
        await dep1StockDelete(id);
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

  return (
    <div className="page">
      <Card title="Stock Département 1">
        <TableToolbar
          exportFilename="stock_dep1"
          exportTitle="Stock Département 1"
          onRefresh={refresh}
          rows={
            selectedIds.length > 0
              ? filtered.filter((r) => selectedIds.includes(r.reference))
              : filtered
          }
          selectedCount={selectedIds.length}
          onGlobalDelete={handleGlobalDelete}
          columns={[
            { header: "Référence", cell: (s) => s.reference },
            {
              header: "Type",
              cell: (s) => getProductTypeDisplayName(s.productType),
            },
            { header: "Qté Totale", cell: (s) => s.totalQuantity },
            { header: "Qté Magasin", cell: (s) => s.storeQuantity },
            { header: "Qté Transférée", cell: (s) => s.transferedQuantity },
          ]}
          filterContent={
            <>
              <div className="field">
                <div className="label">Référence</div>
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
                <div className="label">Type</div>
                <select
                  className="select"
                  value={draftFilters.productType}
                  onChange={(e) =>
                    setDraftFilters((prev) => ({
                      ...prev,
                      productType: e.target.value,
                    }))
                  }
                >
                  <option value="">Tous</option>
                  {Object.keys(PRODUCT_TYPE_DISPLAY_NAMES).map((type) => (
                    <option key={type} value={type}>
                      {getProductTypeDisplayName(type)}
                    </option>
                  ))}
                </select>
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
            highlightedKey={focusedReference}
            rows={filtered}
            rowKey={(s) => s.reference}
            columns={[
              { header: "Référence", cell: (s) => s.reference },
              {
                header: "Type",
                cell: (s) => (
                  <span className="badge">
                    {getProductTypeDisplayName(s.productType)}
                  </span>
                ),
              },
              {
                header: "Qté Totale",
                cell: (s) => (
                  <span className="badge badgeBlue">{s.totalQuantity}</span>
                ),
              },
              {
                header: "Qté Magasin",
                cell: (s) => (
                  <span className="badge badgeGreen">{s.storeQuantity}</span>
                ),
              },
              {
                header: "Qté Transférée",
                cell: (s) => (
                  <span className="badge badgeRed">{s.transferedQuantity}</span>
                ),
              },
              {
                header: "",
                cell: (s) =>
                  isAdmin ? (
                    <div
                      className="actionsRow"
                      style={{ justifyContent: "flex-end" }}
                    >
                      {s.rowIds.length === 1 ? (
                        <button
                          className="btn btnGhost"
                          style={{ padding: 4 }}
                          onClick={() => openEdit(s)}
                          title="Modifier"
                        >
                          <img
                            src={editIcon}
                            alt="Edit"
                            style={{ width: 16, height: 16 }}
                          />
                        </button>
                      ) : (
                        <span className="muted">Non modifiable</span>
                      )}
                    </div>
                  ) : null,
              },
            ]}
          />
        )}
      </Card>

      <Modal
        open={editOpen}
        title="Modifier le stock département 1"
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
              value={editRow?.reference ?? ""}
              disabled
            />
          </div>
          <div className="grid2">
            <div className="field">
              <div className="label">Quantité totale</div>
              <input
                className="input"
                type="number"
                min={0}
                value={eTotal}
                onChange={(e) => setETotal(Number(e.target.value) || 0)}
              />
            </div>
            <div className="field">
              <div className="label">Quantité magasin</div>
              <input
                className="input"
                type="number"
                min={0}
                value={eStore}
                onChange={(e) => setEStore(Number(e.target.value) || 0)}
              />
            </div>
          </div>
        </div>
      </Modal>
      {confirmationDialog}
    </div>
  );
}
