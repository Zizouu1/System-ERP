import { useEffect, useMemo, useState } from "react";
import editIcon from "@/assets/edit.png";
import { Card } from "@/components/Card";
import { DataTable } from "@/components/DataTable";
import { Modal } from "@/components/Modal";
import { Spinner } from "@/components/Spinner";
import {
  listProductsPaged,
  createProduct as createProductApi,
  updateProduct as updateProductApi,
  deleteProduct as deleteProductApi,
  importProducts,
} from "@/shared/api/endpoints";
import type {
  ProductDto,
  ProductType,
  ProductUpsertReq,
} from "@/shared/api/types";
import { TableToolbar } from "@/shared/ui/TableToolbar/TableToolbar";
import { useConfirmationDialog } from "@/shared/ui/ConfirmationDialog/useConfirmationDialog";
import { matchesExact, matchesText } from "@/utils/tableFilters";

const PRODUCT_TYPE_OPTIONS: Array<{ value: ProductType; label: string }> = [
  { value: "MATIERE_PREMIERE", label: "Matiere premiere" },
  { value: "SEMI_FINI", label: "Semi-fini" },
  { value: "PRODUIT_FINI", label: "Produit fini" },
];

function typeLabel(type: ProductType) {
  return PRODUCT_TYPE_OPTIONS.find((opt) => opt.value === type)?.label ?? type;
}

type ProductTableFilters = {
  reference: string;
  designation: string;
  productType: string;
  active: string;
};

const EMPTY_FILTERS: ProductTableFilters = {
  reference: "",
  designation: "",
  productType: "",
  active: "",
};

export default function ProductPage() {
  const { askConfirmation, confirmationDialog } = useConfirmationDialog();
  const [filters, setFilters] = useState<ProductTableFilters>(EMPTY_FILTERS);
  const [draftFilters, setDraftFilters] =
    useState<ProductTableFilters>(EMPTY_FILTERS);
  const [selectedIds, setSelectedIds] = useState<(string | number)[]>([]);
  const [modalOpen, setModalOpen] = useState(false);
  const [editProduct, setEditProduct] = useState<ProductDto | null>(null);
  const [products, setProducts] = useState<ProductDto[]>([]);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [notice, setNotice] = useState<{
    type: "ok" | "bad";
    text: string;
  } | null>(null);

  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(true);
  const [isMoreLoading, setIsMoreLoading] = useState(false);

  async function loadProducts() {
    setLoading(true);
    setPage(0);
    try {
      const res = await listProductsPaged(0, 100);
      setProducts(res.content || []);
      setHasMore(!res.last);
    } catch (error) {
      console.error("Erreur lors du chargement des produits:", error);
      setNotice({
        type: "bad",
        text: "Impossible de charger les produits.",
      });
    } finally {
      setLoading(false);
    }
  }

  async function loadMore() {
    if (isMoreLoading || !hasMore) return;
    setIsMoreLoading(true);
    try {
      const nextPage = page + 1;
      const res = await listProductsPaged(nextPage, 100);
      setProducts((prev) => [...prev, ...res.content]);
      setPage(nextPage);
      setHasMore(!res.last);
    } catch (error: any) {
      console.error("Erreur chargement:", error);
      setNotice({
        type: "bad",
        text: error?.message ?? "Erreur de chargement.",
      });
    } finally {
      setIsMoreLoading(false);
    }
  }

  useEffect(() => {
    loadProducts();
  }, []);

  async function handleCreateProduct(data: ProductUpsertReq) {
    setSaving(true);
    try {
      const saved = await createProductApi(data);
      setProducts((prev) => [saved, ...prev]);
    } catch (error) {
      console.error("Erreur lors de la creation du produit:", error);
      setNotice({
        type: "bad",
        text: "Erreur lors de la creation du produit.",
      });
      throw error;
    } finally {
      setSaving(false);
    }
  }

  async function handleUpdateProduct(data: ProductUpsertReq) {
    if (!editProduct?.id) return;

    setSaving(true);
    try {
      const updated = await updateProductApi(editProduct.id, data);
      setProducts((prev) =>
        prev.map((item) => (item.id === updated.id ? updated : item)),
      );
    } catch (error) {
      console.error("Erreur lors de la modification du produit:", error);
      setNotice({
        type: "bad",
        text: "Erreur lors de la modification du produit.",
      });
      throw error;
    } finally {
      setSaving(false);
    }
  }

  async function handleImport(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    if (!file) return;

    setLoading(true);
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
      const idxRef = headers.findIndex((h) =>
        ["ref", "reference", "référence"].includes(h),
      );
      const idxDesignation = headers.findIndex((h) =>
        ["designation", "désignation", "nom"].includes(h),
      );
      const idxType = headers.findIndex((h) =>
        ["product_type", "type"].includes(h),
      );
      const idxActive = headers.findIndex((h) =>
        ["active", "actif"].includes(h),
      );

      let finalRows: string[][];

      if (idxRef !== -1 && idxDesignation !== -1 && idxType !== -1) {
        // Use header mapping
        finalRows = dataRows.map((row) => [
          row[idxRef] || "",
          row[idxDesignation] || "",
          row[idxType] || "",
          idxActive !== -1 ? row[idxActive] || "true" : "true",
        ]);
      } else {
        // Fallback to positional: ref, designation, type, active
        finalRows = dataRows.map((row) => [
          row[0] || "",
          row[1] || "",
          row[2] || "",
          row[3] || "true",
        ]);
      }

      await importProducts(finalRows);
      await loadProducts();
    } catch (error: any) {
      console.error("Erreur lors de l'import:", error);
      setNotice({
        type: "bad",
        text: error.message || "Erreur lors de l'import CSV.",
      });
    } finally {
      setLoading(false);
      if (e.target) e.target.value = "";
    }
  }

  async function handleDeleteProduct(id: number) {
    try {
      await deleteProductApi(id);
      setProducts((prev) => prev.filter((item) => item.id !== id));
    } catch (error) {
      console.error("Erreur lors de la suppression du produit:", error);
      setNotice({
        type: "bad",
        text: "Erreur lors de la suppression du produit.",
      });
    }
  }

  async function confirmDeleteProduct(row: ProductDto) {
    const confirmed = await askConfirmation({
      title: "Supprimer un produit",
      message: `Voulez-vous vraiment supprimer le produit ${row.ref} ?`,
    });
    if (!confirmed) return;
    await handleDeleteProduct(row.id);
  }

  async function handleGlobalDelete() {
    const confirmed = await askConfirmation({
      title: "Supprimer",
      message: `Voulez-vous vraiment supprimer ${selectedIds.length} produit(s) ?`,
    });
    if (!confirmed) return;
    setNotice(null);
    setLoading(true);
    try {
      for (const id of selectedIds) {
        await deleteProductApi(Number(id));
      }
      setNotice({ type: "ok", text: "Supprimés avec succès." });
      setSelectedIds([]);
      await loadProducts();
    } catch (error: any) {
      setNotice({ type: "bad", text: "Échec lors de la suppression." });
    } finally {
      setLoading(false);
    }
  }

  const filtered = useMemo(() => {
    return products.filter((product) => {
      const activeText = product.active ? "oui" : "non";
      return (
        matchesText(product.ref, filters.reference) &&
        matchesText(product.designation, filters.designation) &&
        matchesExact(product.productType, filters.productType) &&
        matchesExact(activeText, filters.active)
      );
    });
  }, [products, filters]);

  return (
    <div className="page">
      <div className="grid">
        <Card title="Produits">
          <TableToolbar
            exportFilename="produits"
            exportTitle="Liste des Produits"
            rows={
              selectedIds.length > 0
                ? filtered.filter((p) => selectedIds.includes(p.id))
                : filtered
            }
            selectedCount={selectedIds.length}
            onRefresh={loadProducts}
            onGlobalDelete={handleGlobalDelete}
            columns={[
              { header: "Reference", cell: (row) => row.ref },
              { header: "Designation", cell: (row) => row.designation },
              { header: "Type", cell: (row) => typeLabel(row.productType) },
              { header: "Actif", cell: (row) => (row.active ? "Oui" : "Non") },
            ]}
            onImport={() => {
              const input = document.createElement("input");
              input.type = "file";
              input.accept = ".csv";
              input.onchange = (e) => handleImport(e as any);
              input.click();
            }}
            importLabel="Importer CSV"
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
                  <div className="label">Designation</div>
                  <input
                    className="input"
                    value={draftFilters.designation}
                    onChange={(e) =>
                      setDraftFilters((prev) => ({
                        ...prev,
                        designation: e.target.value,
                      }))
                    }
                    placeholder="Ex: Produit A"
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
                    {PRODUCT_TYPE_OPTIONS.map((opt) => (
                      <option key={opt.value} value={opt.value}>
                        {opt.label}
                      </option>
                    ))}
                  </select>
                </div>
                <div className="field">
                  <div className="label">Actif</div>
                  <select
                    className="select"
                    value={draftFilters.active}
                    onChange={(e) =>
                      setDraftFilters((prev) => ({
                        ...prev,
                        active: e.target.value,
                      }))
                    }
                  >
                    <option value="">Tous</option>
                    <option value="oui">Oui</option>
                    <option value="non">Non</option>
                  </select>
                </div>
              </>
            }
            onApplyFilters={() => setFilters({ ...draftFilters })}
            extraActions={
              <button
                className="btn btnPrimary"
                onClick={() => {
                  setEditProduct(null);
                  setModalOpen(true);
                }}
              >
                Ajouter un produit
              </button>
            }
          />

          {notice && (
            <div
              className={`notice ${notice.type === "bad" ? "noticeBad" : "noticeOk"}`}
              style={{ marginBottom: 12 }}
            >
              {notice.text}
            </div>
          )}

          {loading ? (
            <Spinner />
          ) : (
            <DataTable
              enableSelection
              selectedKeys={selectedIds}
              onSelectionChange={setSelectedIds}
              rows={filtered}
              columns={[
                { header: "Reference", cell: (row: ProductDto) => row.ref },
                {
                  header: "Designation",
                  cell: (row: ProductDto) => row.designation,
                },
                {
                  header: "Type",
                  cell: (row: ProductDto) => typeLabel(row.productType),
                },
                {
                  header: "Actif",
                  cell: (row: ProductDto) => (
                    <span
                      className={
                        row.active ? "badge badgeGreen" : "badge badgeRed"
                      }
                    >
                      {row.active ? "Oui" : "Non"}
                    </span>
                  ),
                },
                {
                  header: "",
                  cell: (row: ProductDto) => (
                    <div
                      className="actionsRow"
                      style={{ justifyContent: "flex-end" }}
                    >
                      <button
                        className="btn btnGhost"
                        style={{ padding: 4 }}
                        onClick={() => {
                          setEditProduct(row);
                          setModalOpen(true);
                        }}
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
              rowKey={(row: ProductDto) => row.id}
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

          <Modal
            open={modalOpen}
            title={editProduct ? "Modifier le produit" : "Ajouter un produit"}
            onClose={() => {
              if (saving) return;
              setModalOpen(false);
              setEditProduct(null);
            }}
            actions={null}
          >
            <ProductForm
              product={editProduct ?? undefined}
              saving={saving}
              onSave={async (data) => {
                try {
                  if (editProduct) {
                    await handleUpdateProduct(data);
                  } else {
                    await handleCreateProduct(data);
                  }

                  setModalOpen(false);
                  setEditProduct(null);
                } catch {
                  // Error already handled in create/update
                }
              }}
            />
          </Modal>
          {confirmationDialog}
        </Card>
      </div>
    </div>
  );
}

function ProductForm({
  product,
  saving,
  onSave,
}: {
  product?: ProductDto;
  saving: boolean;
  onSave: (data: ProductUpsertReq) => Promise<void>;
}) {
  const [ref, setRef] = useState(product?.ref || "");
  const [designation, setDesignation] = useState(product?.designation || "");
  const [productType, setProductType] = useState<ProductType>(
    product?.productType || "MATIERE_PREMIERE",
  );
  const [active, setActive] = useState(product?.active ?? true);

  useEffect(() => {
    setRef(product?.ref || "");
    setDesignation(product?.designation || "");
    setProductType(product?.productType || "MATIERE_PREMIERE");
    setActive(product?.active ?? true);
  }, [product]);

  return (
    <form
      onSubmit={async (e) => {
        e.preventDefault();
        await onSave({
          ref: ref.trim(),
          designation: designation.trim(),
          productType,
          active,
        });
      }}
      className="form"
    >
      <div className="field">
        <label className="label">Reference</label>
        <input
          className="input"
          value={ref}
          onChange={(e) => setRef(e.target.value)}
          required
          disabled={saving}
        />
      </div>

      <div className="field">
        <label className="label">Designation</label>
        <input
          className="input"
          value={designation}
          onChange={(e) => setDesignation(e.target.value)}
          required
          disabled={saving}
        />
      </div>

      <div className="field">
        <label className="label">Type</label>
        <select
          className="select"
          value={productType}
          onChange={(e) => setProductType(e.target.value as ProductType)}
          disabled={saving}
        >
          {PRODUCT_TYPE_OPTIONS.map((opt) => (
            <option key={opt.value} value={opt.value}>
              {opt.label}
            </option>
          ))}
        </select>
      </div>

      <div
        className="field"
        style={{ display: "flex", alignItems: "center", gap: 8 }}
      >
        <input
          id="product-active"
          type="checkbox"
          checked={active}
          onChange={(e) => setActive(e.target.checked)}
          disabled={saving}
        />
        <label htmlFor="product-active">Actif</label>
      </div>

      <button className="btn btnPrimary" type="submit" disabled={saving}>
        {saving ? "Enregistrement..." : product ? "Modifier" : "Creer"}
      </button>
    </form>
  );
}
