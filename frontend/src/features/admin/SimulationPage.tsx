import { useEffect, useMemo, useState } from "react";
import RemoveIcon from "@/assets/delete.png";
import { Card } from "@/components/Card";
import { listProducts, simulationCheckBatch } from "@/shared/api/endpoints";
import type { ProductDto } from "@/shared/api/types";
import { AutocompleteInput } from "@/components/AutocompleteInput";

const SIMULATION_ALLOWED_TYPES = ["SEMI_FINI", "PRODUIT_FINI"] as const;

type MissingItemDetail = {
  componentRef: string;
  componentDesignation: string;
  requiredQty: number;
  availableQty: number;
  missingQty: number;
  productContributions: Array<{
    productRef: string;
    productDesignation?: string;
    producedQty?: number;
    requiredQty: number;
  }>;
};

type SimulationUiResult = {
  possible: boolean;
  missingItems: MissingItemDetail[];
};

function formatQty(value: number | undefined) {
  if (typeof value !== "number" || Number.isNaN(value)) return "-";
  if (Number.isInteger(value)) return String(value);
  return value.toFixed(2);
}

export default function SimulationPage() {
  const [loading, setLoading] = useState(true);
  const [products, setProducts] = useState<ProductDto[]>([]);
  const [searchTerm, setSearchTerm] = useState("");
  const [selectedItems, setSelectedItems] = useState<
    Array<{ product: ProductDto; quantity: number }>
  >([]);
  const [result, setResult] = useState<SimulationUiResult | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    (async () => {
      setLoading(true);
      try {
        const p = await listProducts();
        setProducts(p);
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  const simulationProducts = useMemo(
    () =>
      products.filter((p) =>
        SIMULATION_ALLOWED_TYPES.includes(
          p.productType as (typeof SIMULATION_ALLOWED_TYPES)[number],
        ),
      ),
    [products],
  );

  const productsByRef = useMemo(
    () => new Map(products.map((p) => [p.ref, p] as const)),
    [products],
  );

  const trimmedSearch = searchTerm.trim().toLowerCase();

  const exactMatch = useMemo(
    () =>
      simulationProducts.find((p) => p.ref.toLowerCase() === trimmedSearch) ??
      null,
    [simulationProducts, trimmedSearch],
  );

  const invalidTypeMatch = useMemo(() => {
    if (!trimmedSearch) return null;
    const match = products.find((p) => p.ref.toLowerCase() === trimmedSearch);
    if (!match) return null;
    return SIMULATION_ALLOWED_TYPES.includes(
      match.productType as (typeof SIMULATION_ALLOWED_TYPES)[number],
    )
      ? null
      : match;
  }, [products, trimmedSearch]);

  const addItem = (p: ProductDto) => {
    if (selectedItems.some((item) => item.product.id === p.id)) {
      setSearchTerm("");
      return;
    }
    setSelectedItems([...selectedItems, { product: p, quantity: 1 }]);
    setSearchTerm("");
  };

  useEffect(() => {
    if (invalidTypeMatch) {
      setError("Ce type de produit ne peut pas être simulé");
    } else if (error === "Ce type de produit ne peut pas être simulé") {
      setError(null);
    }
  }, [invalidTypeMatch, error]);

  const removeItem = (id: number) => {
    setSelectedItems(selectedItems.filter((item) => item.product.id !== id));
  };

  const updateQty = (id: number, qty: number) => {
    setSelectedItems(
      selectedItems.map((item) =>
        item.product.id === id ? { ...item, quantity: Math.max(1, qty) } : item,
      ),
    );
  };

  async function runSimulation() {
    setBusy(true);
    setError(null);
    setResult(null);

    if (selectedItems.length === 0) {
      setError("Veuillez selectionner au moins un produit pour la simulation.");
      setBusy(false);
      return;
    }

    try {
      const simulationResult = await simulationCheckBatch({
        items: selectedItems.map((item) => ({
          productId: item.product.id,
          quantity: item.quantity,
        })),
      });

      const detailedMissingItems = (simulationResult.missingItems ?? []).map(
        (missing) => {
          const componentProduct = productsByRef.get(missing.reference);
          return {
            componentRef: missing.reference,
            componentDesignation:
              componentProduct?.designation ?? "Designation inconnue",
            requiredQty: missing.requiredQty ?? 0,
            availableQty: missing.availableQty ?? 0,
            missingQty: missing.missingQty ?? 0,
            productContributions: missing.productContributions ?? [],
          };
        },
      );

      setResult({
        possible: simulationResult.possible,
        missingItems: detailedMissingItems,
      });
    } catch (e: any) {
      setError(e?.message ?? "Erreur lors de la simulation.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="page">
      <div className="pageHeader">
        <div>
          <div className="muted">Verification de faisabilite de production</div>
        </div>
      </div>

      <div className="grid" style={{ maxWidth: 800 }}>
        <Card title="Simulation">
          <div className="form">
            <div className="field" style={{ position: "relative" }}>
              <div className="label">Produit</div>
              <div className="actionsRow" style={{ alignItems: "center" }}>
                <div style={{ flex: 1 }}>
                  <AutocompleteInput
                    value={searchTerm}
                    onChange={setSearchTerm}
                    placeholder="Rechercher une reference produit..."
                    productTypes={SIMULATION_ALLOWED_TYPES}
                  />
                </div>
                <button
                  className="btn"
                  type="button"
                  disabled={!exactMatch}
                  onClick={() => {
                    if (invalidTypeMatch) {
                      setError("Ce type de produit ne peut pas être simulé");
                      return;
                    }
                    if (exactMatch) addItem(exactMatch);
                  }}
                >
                  Ajouter
                </button>
              </div>
            </div>

            <div style={{ marginTop: 20, display: "grid", gap: 12 }}>
              {selectedItems.map((item) => (
                <div
                  key={item.product.id}
                  style={{
                    display: "flex",
                    alignItems: "center",
                    gap: 16,
                    padding: "12px 16px",
                    background: "var(--surface-2)",
                    borderRadius: 12,
                    border: "1px solid var(--border)",
                  }}
                >
                  <div style={{ flex: 1 }}>
                    <div style={{ fontWeight: 700 }}>{item.product.ref}</div>
                    <div className="muted" style={{ fontSize: 12 }}>
                      {item.product.designation} - {item.product.productType}
                    </div>
                  </div>
                  <div
                    style={{ display: "flex", alignItems: "center", gap: 8 }}
                  >
                    <div className="label" style={{ marginBottom: 0 }}>
                      Qte:
                    </div>
                    <input
                      type="text"
                      inputMode="numeric"
                      pattern="[0-9]*"
                      className="input"
                      style={{ width: 80, height: 36 }}
                      value={item.quantity}
                      onChange={(e) => {
                        const val = e.target.value.replace(/[^0-9]/g, "");
                        updateQty(item.product.id, Number(val) || 1);
                      }}
                    />
                  </div>
                  <button
                    className="btn"
                    style={{ width: 36, height: 36, padding: 0 }}
                    onClick={() => removeItem(item.product.id)}
                  >
                    <img
                      src={RemoveIcon}
                      alt="Remove"
                      style={{ width: 16, height: 16 }}
                    />
                  </button>
                </div>
              ))}
            </div>

            <button
              className="btn btnPrimary"
              style={{ marginTop: 24, width: "100%", height: 48 }}
              onClick={runSimulation}
              disabled={busy || loading}
            >
              {busy ? "Simulation en cours..." : "Lancer la simulation"}
            </button>
          </div>
        </Card>

        {error && (
          <div className="notice noticeBad" style={{ marginTop: 16 }}>
            {error}
          </div>
        )}

        {result && (
          <div
            className={result.possible ? "notice noticeOk" : "notice noticeBad"}
            style={{ marginTop: 16, padding: 20 }}
          >
            <div
              style={{
                display: "flex",
                alignItems: "center",
                gap: 12,
                marginBottom: result.missingItems.length ? 16 : 0,
              }}
            >
              <div style={{ fontWeight: 900, fontSize: 18 }}>
                {result.possible ? "Faisable" : "Non faisable"}
              </div>
            </div>

            {result.missingItems.length > 0 && (
              <div style={{ marginTop: 16 }}>
                <div style={{ display: "grid", gap: 10 }}>
                  {result.missingItems.map((m, idx) => (
                    <div
                      key={`${m.componentRef}-${idx}`}
                      style={{
                        padding: "12px 16px",
                        background: "rgba(0,0,0,0.05)",
                        borderRadius: 10,
                      }}
                    >
                      <div style={{ fontSize: 13, lineHeight: 1.5 }}>
                        Production impossible : le composant{" "}
                        <b style={{ color: "var(--text)" }}>{m.componentRef}</b>{" "}
                        ({m.componentDesignation}) requiert{" "}
                        <b>{formatQty(m.requiredQty)}</b>, disponible:{" "}
                        <b style={{ color: "var(--danger)" }}>
                          {formatQty(m.availableQty)}
                        </b>
                        , manque: <b>{formatQty(m.missingQty)}</b>.
                        {m.productContributions.length > 0 && (
                          <div style={{ marginTop: 6 }}>
                            Produits concernés :{" "}
                            {m.productContributions.map((contribution, idx) => (
                              <span key={`${contribution.productRef}-${idx}`}>
                                {contribution.productRef} (x
                                {formatQty(contribution.producedQty)}) nécessite{" "}
                                {formatQty(contribution.requiredQty)}
                                {idx < m.productContributions.length - 1
                                  ? ", "
                                  : "."}
                              </span>
                            ))}
                          </div>
                        )}
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
