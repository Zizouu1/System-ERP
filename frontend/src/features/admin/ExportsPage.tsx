import { useEffect, useMemo, useState } from "react";
import editIcon from "@/assets/edit.png";
import { Card } from "@/components/Card";
import { DataTable } from "@/components/DataTable";
import { Modal } from "@/components/Modal";
import { Spinner } from "@/components/Spinner";
import {
  createExport,
  deleteExport,
  listExports,
  listActiveProducts,
  updateExport,
} from "@/shared/api/endpoints";
import type { ExportDto, ProductDto } from "@/shared/api/types";
import { TableToolbar } from "@/shared/ui/TableToolbar/TableToolbar";
import { AutocompleteInput } from "@/components/AutocompleteInput";
import { useConfirmationDialog } from "@/shared/ui/ConfirmationDialog/useConfirmationDialog";
import {
  matchesDateRange,
  matchesNumberMax,
  matchesNumberMin,
  matchesText,
} from "@/utils/tableFilters";

type ExportTableFilters = {
  reference: string;
  startDate: string;
  endDate: string;
  quantityMin: string;
  quantityMax: string;
};

const EMPTY_FILTERS: ExportTableFilters = {
  reference: "",
  startDate: "",
  endDate: "",
  quantityMin: "",
  quantityMax: "",
};

// Note: reason field removed from ExportRequest (backend no longer has it)
export default function ExportsPage() {
  const { askConfirmation, confirmationDialog } = useConfirmationDialog();
  const [loading, setLoading] = useState(true);
  const [rows, setRows] = useState<ExportDto[]>([]);
  const [products, setProducts] = useState<ProductDto[]>([]);
  const [filters, setFilters] = useState<ExportTableFilters>(EMPTY_FILTERS);
  const [draftFilters, setDraftFilters] =
    useState<ExportTableFilters>(EMPTY_FILTERS);
  const [msg, setMsg] = useState<string | null>(null);
  const [selectedIds, setSelectedIds] = useState<(string | number)[]>([]);

  const [reference, setReference] = useState("");
  const [quantity, setQuantity] = useState<number | string>("");

  const [editOpen, setEditOpen] = useState(false);
  const [editRow, setEditRow] = useState<ExportDto | null>(null);
  const [eReference, setEReference] = useState("");
  const [eQuantity, setEQuantity] = useState<number | string>("");

  const [createOpen, setCreateOpen] = useState(false);

  async function refresh() {
    setLoading(true);
    try {
      const [e, p] = await Promise.all([listExports(), listActiveProducts()]);
      setRows(e);
      setProducts(p);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    refresh();
  }, []);

  const filtered = useMemo(() => {
    return rows.filter((r) => {
      return (
        matchesText(r.product?.ref ?? "", filters.reference) &&
        matchesDateRange(r.exportDate, filters.startDate, filters.endDate) &&
        matchesNumberMin(r.quantity, filters.quantityMin) &&
        matchesNumberMax(r.quantity, filters.quantityMax)
      );
    });
  }, [rows, filters]);

  async function create(e: React.FormEvent) {
    e.preventDefault();
    setMsg(null);
    try {
      await createExport({ reference, quantity: Number(quantity) });
      setQuantity("");
      setReference("");
      setCreateOpen(false);
      await refresh();
    } catch (e: any) {
      setMsg(e?.message ?? "Erreur.");
    }
  }

  function openEdit(r: ExportDto) {
    setEditRow(r);
    setEReference(r.product?.ref ?? "");
    setEQuantity(r.quantity);
    setEditOpen(true);
  }

  async function saveEdit() {
    if (!editRow) return;
    setMsg(null);
    try {
      await updateExport(editRow.id, {
        reference: eReference,
        quantity: Number(eQuantity),
      });
      setEditOpen(false);
      await refresh();
    } catch (e: any) {
      setMsg(e?.message ?? "Erreur.");
    }
  }

  async function remove(id: number) {
    const confirmed = await askConfirmation({
      title: "Supprimer un export",
      message: "Voulez-vous vraiment supprimer cet export ?",
    });
    if (!confirmed) return;
    setMsg(null);
    try {
      await deleteExport(id);
      setMsg("Supprimé avec succès.");
      await refresh();
    } catch (e: any) {
      setMsg(e?.message ?? "Erreur.");
    }
  }

  async function handleGlobalDelete() {
    const confirmed = await askConfirmation({
      title: "Supprimer",
      message: `Voulez-vous vraiment supprimer ${selectedIds.length} export(s) ?`,
    });
    if (!confirmed) return;
    setMsg(null);
    setLoading(true);
    try {
      for (const id of selectedIds) {
        await deleteExport(Number(id));
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
      <div className="grid">
        <Card title="Export">
          <TableToolbar
            exportFilename="exports"
            exportTitle="Historique des exportations"
            rows={
              selectedIds.length > 0
                ? filtered.filter((p) => selectedIds.includes(p.id))
                : filtered
            }
            selectedCount={selectedIds.length}
            onRefresh={refresh}
            onGlobalDelete={handleGlobalDelete}
            columns={[
              { header: "Produit", cell: (x) => x.product?.ref ?? "-" },
              { header: "Quantité", cell: (x) => x.quantity },
              {
                header: "Date",
                cell: (x) => (x.exportDate ? x.exportDate.split("T")[0] : "-"),
              },
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
                    placeholder="Ex: PF-001"
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
                <div className="grid2">
                  <div className="field">
                    <div className="label">Quantité min</div>
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
                    <div className="label">Quantité max</div>
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
                  setReference("");
                  setQuantity("");
                  setCreateOpen(true);
                }}
              >
                Nouvel export
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
              rowKey={(x) => x.id}
              columns={[
                { header: "Produit", cell: (x) => x.product?.ref ?? "-" },
                {
                  header: "Quantité",
                  cell: (x) => (
                    <span className="badge badgeBlue">{x.quantity}</span>
                  ),
                },
                {
                  header: "Date",
                  cell: (x) =>
                    x.exportDate ? x.exportDate.split("T")[0] : "-",
                },
                {
                  header: "",
                  cell: (x) => (
                    <div
                      className="actionsRow"
                      style={{ justifyContent: "flex-end" }}
                    >
                      <button
                        className="btn btnGhost"
                        style={{ padding: 4 }}
                        onClick={() => openEdit(x)}
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
        open={createOpen}
        title="Nouvel export"
        onClose={() => setCreateOpen(false)}
        actions={null}
      >
        <form className="form" onSubmit={create}>
          <div className="field">
            <div className="label">Référence du produit</div>
            <AutocompleteInput
              value={reference}
              onChange={setReference}
              placeholder="Ex: REF-123"
              required
              productType="PRODUIT_FINI"
            />
          </div>
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
          <button className="btn btnPrimary" type="submit">
            Enregistrer
          </button>
          {msg && <div className="notice">{msg}</div>}
        </form>
      </Modal>

      <Modal
        open={editOpen}
        title="Modifier l'export"
        onClose={() => setEditOpen(false)}
        actions={
          <button className="btn btnPrimary" onClick={saveEdit}>
            Enregistrer
          </button>
        }
      >
        <div className="form">
          <div className="field">
            <div className="label">Produit</div>
            <select
              className="select"
              value={eReference}
              onChange={(e) => setEReference(e.target.value)}
            >
              {products.map((p) => (
                <option key={p.id} value={p.ref}>
                  {p.ref} ({p.productType})
                </option>
              ))}
            </select>
          </div>
          <div className="field">
            <div className="label">Quantité</div>
            <input
              className="input"
              type="text"
              inputMode="numeric"
              pattern="[0-9]*"
              value={eQuantity}
              onChange={(e) => {
                const val = e.target.value.replace(/[^0-9]/g, "");
                setEQuantity(Number(val) || 0);
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
