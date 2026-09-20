import { useEffect, useMemo, useState } from "react";
import editIcon from "@/assets/edit.png";
import { Card } from "@/components/Card";
import { DataTable } from "@/components/DataTable";
import { Modal } from "@/components/Modal";
import { Spinner } from "@/components/Spinner";
import { AutocompleteInput } from "@/components/AutocompleteInput";
import {
  createBom,
  deleteBom,
  importNomenclature,
  listBom,
  listProducts,
  updateBom,
} from "@/shared/api/endpoints";
import type { NomenclatureDto, ProductDto } from "@/shared/api/types";
import { TableToolbar } from "@/shared/ui/TableToolbar/TableToolbar";
import { useConfirmationDialog } from "@/shared/ui/ConfirmationDialog/useConfirmationDialog";
import {
  matchesNumberMax,
  matchesNumberMin,
  matchesText,
} from "@/utils/tableFilters";

type BomTableFilters = {
  parentRef: string;
  componentRef: string;
  quantityMin: string;
  quantityMax: string;
};

const EMPTY_FILTERS: BomTableFilters = {
  parentRef: "",
  componentRef: "",
  quantityMin: "",
  quantityMax: "",
};

export default function BomPage() {
  const { askConfirmation, confirmationDialog } = useConfirmationDialog();
  const [loading, setLoading] = useState(true);
  const [rows, setRows] = useState<NomenclatureDto[]>([]);
  const [products, setProducts] = useState<ProductDto[]>([]);
  const [filters, setFilters] = useState<BomTableFilters>(EMPTY_FILTERS);
  const [draftFilters, setDraftFilters] =
    useState<BomTableFilters>(EMPTY_FILTERS);
  const [msg, setMsg] = useState<string | null>(null);
  const [selectedIds, setSelectedIds] = useState<(string | number)[]>([]);

  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(true);
  const [isMoreLoading, setIsMoreLoading] = useState(false);

  const [parentRef, setParentRef] = useState("");
  const [componentRef, setComponentRef] = useState("");
  const [qty, setQty] = useState<number>(1);

  const [editOpen, setEditOpen] = useState(false);
  const [editRow, setEditRow] = useState<NomenclatureDto | null>(null);
  const [eParentRef, setEParentRef] = useState("");
  const [eComponentRef, setEComponentRef] = useState("");
  const [eQty, setEQ] = useState<number>(1);

  const [createOpen, setCreateOpen] = useState(false);

  async function refresh() {
    setLoading(true);
    setPage(0);
    try {
      const [b, p] = await Promise.all([listBom(0, 100), listProducts()]);
      setRows(b.content);
      setHasMore(!b.last);
      setProducts(p);
      setParentRef((prev) => prev || (p.length ? p[0].ref : ""));
      setComponentRef((prev) => prev || (p.length ? p[0].ref : ""));
    } finally {
      setLoading(false);
    }
  }

  async function loadMore() {
    if (isMoreLoading || !hasMore) return;
    setIsMoreLoading(true);
    try {
      const nextPage = page + 1;
      const b = await listBom(nextPage, 100);
      setRows((prev) => [...prev, ...b.content]);
      setPage(nextPage);
      setHasMore(!b.last);
    } catch (e: any) {
      setMsg(e?.message ?? "Erreur chargement.");
    } finally {
      setIsMoreLoading(false);
    }
  }

  useEffect(() => {
    refresh();
  }, []);

  const allRefs = useMemo(
    () => Array.from(new Set(products.map((p) => p.ref))),
    [products],
  );

  const filtered = useMemo(() => {
    return rows.filter((r) => {
      return (
        matchesText(r.parentRef, filters.parentRef) &&
        matchesText(r.componentRef, filters.componentRef) &&
        matchesNumberMin(r.quantityRequired, filters.quantityMin) &&
        matchesNumberMax(r.quantityRequired, filters.quantityMax)
      );
    });
  }, [rows, filters]);

  async function create(e: React.FormEvent) {
    e.preventDefault();
    setMsg(null);
    try {
      await createBom({
        parentRef: parentRef,
        componentRef: componentRef,
        quantityRequired: qty,
      });
      setCreateOpen(false);
      await refresh();
    } catch (e: any) {
      setMsg(e?.message ?? "Erreur.");
    }
  }

  function openEdit(r: NomenclatureDto) {
    setEditRow(r);
    setEParentRef(r.parentRef ?? "");
    setEComponentRef(r.componentRef ?? "");
    setEQ(r.quantityRequired ?? 1);
    setEditOpen(true);
  }

  async function saveEdit() {
    if (!editRow) return;
    setMsg(null);
    try {
      await updateBom(editRow.id, {
        parentRef: eParentRef,
        componentRef: eComponentRef,
        quantityRequired: eQty,
      });
      setEditOpen(false);
      await refresh();
    } catch (e: any) {
      setMsg(e?.message ?? "Erreur.");
    }
  }

  async function remove(id: number) {
    const confirmed = await askConfirmation({
      title: "Supprimer une ligne BOM",
      message: "Voulez-vous vraiment supprimer cette ligne ?",
    });
    if (!confirmed) return;
    setMsg(null);
    try {
      await deleteBom(id);
      setMsg("Supprimé avec succès.");
      await refresh();
    } catch (e: any) {
      setMsg(e?.message ?? "Erreur.");
    }
  }

  async function handleGlobalDelete() {
    const confirmed = await askConfirmation({
      title: "Supprimer",
      message: `Voulez-vous vraiment supprimer ${selectedIds.length} ligne(s) BOM ?`,
    });
    if (!confirmed) return;
    setMsg(null);
    setLoading(true);
    try {
      for (const id of selectedIds) {
        await deleteBom(Number(id));
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

  async function handleCsv(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    if (!file) return;
    setLoading(true);
    setMsg(null);
    try {
      const text = await file.text();
      const lines = text.split(/\r?\n/).filter((l) => l.trim());
      if (lines.length < 2) throw new Error("Fichier vide ou mal formaté.");

      // Detect separator
      const firstLine = lines[0];
      const sep = firstLine.includes(";") ? ";" : ",";

      const headers = firstLine.split(sep).map((h) => h.trim().toLowerCase());
      const dataRows = lines
        .slice(1)
        .map((l) => l.split(sep).map((c) => c.trim()));

      // Map headers to indices
      const idxParent = headers.findIndex((h) =>
        ["parent_ref", "parent", "produit parent", "parentref"].includes(h),
      );
      const idxComponent = headers.findIndex((h) =>
        ["component_ref", "component", "composant", "componentref"].includes(h),
      );
      const idxQty = headers.findIndex((h) =>
        [
          "quantity_required",
          "quantityrequired",
          "qty",
          "quantité",
          "quantite",
          "quantité requise",
        ].includes(h),
      );

      let finalRows: string[][];

      if (idxParent !== -1 && idxComponent !== -1 && idxQty !== -1) {
        // Use header mapping
        finalRows = dataRows.map((row) => [
          row[idxParent] || "",
          row[idxComponent] || "",
          row[idxQty] || "0",
        ]);
      } else {
        // Fallback to positional (if headers don't match known patterns)
        finalRows = dataRows.map((row) => [
          row[0] || "",
          row[1] || "",
          row[2] || "0",
        ]);
      }

      await importNomenclature(finalRows);
      await refresh();
    } catch (e: any) {
      setMsg(e?.message ?? "Erreur import.");
    } finally {
      setLoading(false);
      if (e.target) e.target.value = "";
    }
  }

  return (
    <div className="page">
      <div className="grid">
        <Card title="Nomenclature">
          <TableToolbar
            exportFilename="nomenclature"
            exportTitle="Nomenclature (BOM)"
            rows={
              selectedIds.length > 0
                ? filtered.filter((p) => selectedIds.includes(p.id))
                : filtered
            }
            selectedCount={selectedIds.length}
            onRefresh={refresh}
            onGlobalDelete={handleGlobalDelete}
            columns={[
              { header: "Parent", cell: (r) => r.parentRef ?? "-" },
              { header: "Composant", cell: (r) => r.componentRef ?? "-" },
              { header: "Qté req.", cell: (r) => r.quantityRequired },
            ]}
            onImport={() => {
              const input = document.createElement("input");
              input.type = "file";
              input.accept = ".csv";
              input.onchange = (e) => handleCsv(e as any);
              input.click();
            }}
            importLabel="Importer CSV"
            filterContent={
              <>
                <div className="field">
                  <div className="label">Parent</div>
                  <input
                    className="input"
                    value={draftFilters.parentRef}
                    onChange={(e) =>
                      setDraftFilters((prev) => ({
                        ...prev,
                        parentRef: e.target.value,
                      }))
                    }
                    placeholder="Ex: REF-100"
                  />
                </div>
                <div className="field">
                  <div className="label">Composant</div>
                  <input
                    className="input"
                    value={draftFilters.componentRef}
                    onChange={(e) =>
                      setDraftFilters((prev) => ({
                        ...prev,
                        componentRef: e.target.value,
                      }))
                    }
                    placeholder="Ex: REF-200"
                  />
                </div>
                <div className="grid2">
                  <div className="field">
                    <div className="label">Qté min</div>
                    <input
                      className="input"
                      type="number"
                      min={0}
                      value={draftFilters.quantityMin}
                      onChange={(e) =>
                        setDraftFilters((prev) => ({
                          ...prev,
                          quantityMin: e.target.value,
                        }))
                      }
                    />
                  </div>
                  <div className="field">
                    <div className="label">Qté max</div>
                    <input
                      className="input"
                      type="number"
                      min={0}
                      value={draftFilters.quantityMax}
                      onChange={(e) =>
                        setDraftFilters((prev) => ({
                          ...prev,
                          quantityMax: e.target.value,
                        }))
                      }
                    />
                  </div>
                </div>
              </>
            }
            onApplyFilters={() => setFilters({ ...draftFilters })}
            extraActions={
              <button
                className="btn btnPrimary"
                onClick={() => {
                  setMsg(null);
                  setCreateOpen(true);
                }}
              >
                Ajouter ligne
              </button>
            }
          />

          {loading ? (
            <Spinner />
          ) : (
            <DataTable
              enableSelection
              selectedKeys={selectedIds}
              onSelectionChange={setSelectedIds}
              rows={filtered}
              rowKey={(r) => r.id}
              columns={[
                { header: "Parent", cell: (r) => r.parentRef ?? "-" },
                {
                  header: "Composant",
                  cell: (r) => r.componentRef ?? "-",
                },
                {
                  header: "Qté req.",
                  cell: (r) => (
                    <span className="badge badgeBlue">
                      {r.quantityRequired}
                    </span>
                  ),
                },
                {
                  header: "",
                  cell: (r: NomenclatureDto) => (
                    <div
                      className="actionsRow"
                      style={{ gap: 8, justifyContent: "flex-end" }}
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

          {hasMore && !loading && (
            <button
              className="btn"
              style={{ width: "100%", marginTop: 16, height: 44 }}
              onClick={loadMore}
              disabled={isMoreLoading}
            >
              {isMoreLoading ? <Spinner /> : "Charger plus"}
            </button>
          )}
        </Card>
      </div>

      <Modal
        open={createOpen}
        title="Ajouter une ligne"
        onClose={() => setCreateOpen(false)}
        actions={null}
      >
        <form className="form" onSubmit={create}>
          <div className="field">
            <div className="label">Produit parent</div>
            <AutocompleteInput
              value={parentRef}
              onChange={setParentRef}
              placeholder="Ex: REF-123"
              required
            />
          </div>
          <div className="field">
            <div className="label">Composant</div>
            <AutocompleteInput
              value={componentRef}
              onChange={setComponentRef}
              placeholder="Ex: COMP-456"
              required
            />
          </div>
          <div className="field">
            <div className="label">Quantité requise</div>
            <input
              className="input"
              type="text"
              inputMode="decimal"
              value={qty}
              onChange={(e) => {
                const val = e.target.value.replace(/[^0-9.]/g, "");
                if (val.split(".").length > 2) return;
                setQty(val as any);
              }}
              required
            />
          </div>
          <button className="btn btnPrimary" type="submit">
            Ajouter
          </button>
          {msg && <div className="notice">{msg}</div>}
        </form>
      </Modal>

      <Modal
        open={editOpen}
        title="Modifier la ligne"
        onClose={() => setEditOpen(false)}
        actions={
          <button className="btn btnPrimary" onClick={saveEdit}>
            Enregistrer
          </button>
        }
      >
        <div className="form">
          <div className="field">
            <div className="label">Parent</div>
            <AutocompleteInput
              value={eParentRef}
              onChange={setEParentRef}
              placeholder="Ex: REF-123"
              required
            />
          </div>
          <div className="field">
            <div className="label">Composant</div>
            <AutocompleteInput
              value={eComponentRef}
              onChange={setEComponentRef}
              placeholder="Ex: COMP-456"
              required
            />
          </div>
          <div className="field">
            <div className="label">Quantité</div>
            <input
              className="input"
              type="text"
              inputMode="decimal"
              value={eQty}
              onChange={(e) => {
                const val = e.target.value.replace(/[^0-9.]/g, "");
                if (val.split(".").length > 2) return;
                setEQ(val as any);
              }}
              required
            />
          </div>
        </div>
      </Modal>
      {confirmationDialog}
    </div>
  );
}
