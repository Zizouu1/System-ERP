import { useEffect, useMemo, useRef, useState } from "react";
import { useSearchParams } from "react-router-dom";
import editIcon from "@/assets/edit.png";
import { Card } from "@/components/Card";
import { DataTable } from "@/components/DataTable";
import { KpiCard } from "@/components/KpiCard";
import { Spinner } from "@/components/Spinner";
import {
  deleteGlobalStock,
  listGlobalStock,
  updateGlobalStock,
} from "@/shared/api/endpoints";
import type { GlobalStockDto, ProductType } from "@/shared/api/types";
import { TableToolbar } from "@/shared/ui/TableToolbar/TableToolbar";
import { Modal } from "@/components/Modal";
import { useConfirmationDialog } from "@/shared/ui/ConfirmationDialog/useConfirmationDialog";
import {
  matchesExact,
  matchesNumberMax,
  matchesNumberMin,
  matchesText,
} from "@/utils/tableFilters";

const PRODUCT_TYPE_DISPLAY_NAMES: Record<ProductType, string> = {
  MATIERE_PREMIERE: "matiere premiere",
  SEMI_FINI: "semi-fini",
  PRODUIT_FINI: "produit fini",
};

function getProductTypeDisplayName(type: ProductType): string {
  return PRODUCT_TYPE_DISPLAY_NAMES[type] ?? type;
}

type GlobalStockFilters = {
  reference: string;
  productType: string;
  availableMin: string;
  availableMax: string;
};

const EMPTY_FILTERS: GlobalStockFilters = {
  reference: "",
  productType: "",
  availableMin: "",
  availableMax: "",
};

export default function GlobalStockPage() {
  const { askConfirmation, confirmationDialog } = useConfirmationDialog();
  const [loading, setLoading] = useState(true);
  const [rows, setRows] = useState<GlobalStockDto[]>([]);
  const [filters, setFilters] = useState<GlobalStockFilters>(EMPTY_FILTERS);
  const [draftFilters, setDraftFilters] =
    useState<GlobalStockFilters>(EMPTY_FILTERS);
  const [msg, setMsg] = useState<string | null>(null);
  const [selectedIds, setSelectedIds] = useState<(string | number)[]>([]);

  const [editOpen, setEditOpen] = useState(false);
  const [editRow, setEditRow] = useState<GlobalStockDto | null>(null);
  const [eTotal, setETotal] = useState(0);
  const [eUsed, setEUsed] = useState(0);
  const [searchParams] = useSearchParams();
  const focusedRowId = useMemo(() => {
    const raw = searchParams.get("focusId");
    if (!raw) return undefined;
    const parsed = Number(raw);
    return Number.isNaN(parsed) ? undefined : parsed;
  }, [searchParams]);

  const pageRef = useRef<HTMLDivElement | null>(null);

  async function refresh() {
    setLoading(true);
    try {
      const data = await listGlobalStock();
      setRows(data);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    refresh();
  }, []);

  useEffect(() => {
    const node = pageRef.current;
    if (!node || typeof ResizeObserver === "undefined") return;

    let raf = 0;
    const observer = new ResizeObserver(() => {
      cancelAnimationFrame(raf);
      raf = requestAnimationFrame(() => {
        window.dispatchEvent(new Event("resize"));
      });
    });

    observer.observe(node);
    return () => {
      cancelAnimationFrame(raf);
      observer.disconnect();
    };
  }, []);

  const filtered = useMemo(() => {
    return rows.filter((r) => {
      return (
        matchesText(r.ref, filters.reference) &&
        matchesExact(r.type, filters.productType) &&
        matchesNumberMin(r.quantityAvailable, filters.availableMin) &&
        matchesNumberMax(r.quantityAvailable, filters.availableMax)
      );
    });
  }, [rows, filters]);

  const total = rows.reduce((a, c) => a + (c.quantityTotal ?? 0), 0);
  const used = rows.reduce((a, c) => a + (c.quantityUsed ?? 0), 0);
  const avail = rows.reduce((a, c) => a + (c.quantityAvailable ?? 0), 0);

  function openEdit(row: GlobalStockDto) {
    setEditRow(row);
    setETotal(Number(row.quantityTotal) || 0);
    setEUsed(Number(row.quantityUsed) || 0);
    setEditOpen(true);
  }

  async function saveEdit() {
    if (!editRow?.id) return;
    try {
      await updateGlobalStock(editRow.id, {
        quantityTotal: eTotal,
        quantityUsed: eUsed,
      });
      setEditOpen(false);
      await refresh();
    } catch (e: any) {
      setMsg(e?.message ?? "Erreur lors de la mise à jour.");
    }
  }

  async function remove(row: GlobalStockDto) {
    const confirmed = await askConfirmation({
      title: "Supprimer la ligne de stock",
      message: `Voulez-vous vraiment supprimer la référence ${row.ref} ?`,
    });
    if (!confirmed) return;
    try {
      await deleteGlobalStock(row.id);
      setMsg("Ligne supprimée avec succès.");
      await refresh();
    } catch (e: any) {
      setMsg(e?.message ?? "Erreur lors de la suppression.");
    }
  }

  async function handleGlobalDelete() {
    const confirmed = await askConfirmation({
      title: "Supprimer",
      message: `Voulez-vous vraiment supprimer ${selectedIds.length} ligne(s) de stock ?`,
    });
    if (!confirmed) return;
    setMsg(null);
    setLoading(true);
    try {
      for (const id of selectedIds) {
        await deleteGlobalStock(Number(id));
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
    <div className="page" ref={pageRef}>
      <div
        className="kpiGrid"
        style={{
          marginBottom: 16,
          display: "grid",
          gridTemplateColumns: "repeat(auto-fit, minmax(180px, 1fr))",
          gap: "24px",
          width: "100%",
        }}
      >
        <KpiCard
          label="Produits"
          value={rows.length}
          hint="References enregistrees"
        />
        <KpiCard label="Qte totale" value={total} hint="Toutes references" />
        <KpiCard label="Utilise" value={used} hint="En production" />
        <KpiCard label="Disponible" value={avail} hint="Pret a l'emploi" />
      </div>

      <Card title="Stock global">
        <TableToolbar
          exportFilename="stock_global"
          exportTitle="Stock Global"
          onRefresh={refresh}
          rows={
            selectedIds.length > 0
              ? filtered.filter((p) => selectedIds.includes(p.id))
              : filtered
          }
          selectedCount={selectedIds.length}
          onGlobalDelete={handleGlobalDelete}
          columns={[
            { header: "Reference", cell: (r) => r.ref },
            { header: "Type", cell: (r) => getProductTypeDisplayName(r.type) },
            { header: "Total", cell: (r) => r.quantityTotal ?? 0 },
            { header: "Utilise", cell: (r) => r.quantityUsed ?? 0 },
            { header: "Disponible", cell: (r) => r.quantityAvailable ?? 0 },
          ]}
          filterContent={
            <>
              <div className="field">
                <div className="label">Reference</div>
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
                      {getProductTypeDisplayName(type as ProductType)}
                    </option>
                  ))}
                </select>
              </div>
              <div className="grid2">
                <div className="field">
                  <div className="label">Disponible min</div>
                  <input
                    className="input"
                    type="number"
                    min={0}
                    value={draftFilters.availableMin}
                    onChange={(e) =>
                      setDraftFilters((prev) => ({
                        ...prev,
                        availableMin: e.target.value,
                      }))
                    }
                  />
                </div>
                <div className="field">
                  <div className="label">Disponible max</div>
                  <input
                    className="input"
                    type="number"
                    min={0}
                    value={draftFilters.availableMax}
                    onChange={(e) =>
                      setDraftFilters((prev) => ({
                        ...prev,
                        availableMax: e.target.value,
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
            columns={[
              { header: "Reference", cell: (r) => <b>{r.ref}</b> },
              {
                header: "Type",
                cell: (r) => (
                  <span className="badge badgeBlue">
                    {getProductTypeDisplayName(r.type)}
                  </span>
                ),
              },
              {
                header: "Total",
                cell: (r) => r.quantityTotal ?? "-",
              },
              { header: "Utilise", cell: (r) => r.quantityUsed ?? "-" },
              {
                header: "Disponible",
                cell: (r) => (
                  <span className="badge badgeGreen">
                    {r.quantityAvailable ?? "-"}
                  </span>
                ),
              },
              {
                header: "",
                cell: (r: GlobalStockDto) => (
                  <div
                    className="actionsRow"
                    style={{ justifyContent: "flex-end" }}
                  >
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
                  </div>
                ),
              },
            ]}
          />
        )}
      </Card>

      <Modal
        open={editOpen}
        title="Modifier le stock global"
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
            <input className="input" value={editRow?.ref ?? ""} disabled />
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
              <div className="label">Quantité utilisée</div>
              <input
                className="input"
                type="number"
                min={0}
                value={eUsed}
                onChange={(e) => setEUsed(Number(e.target.value) || 0)}
              />
            </div>
          </div>
        </div>
      </Modal>
      {confirmationDialog}
    </div>
  );
}
