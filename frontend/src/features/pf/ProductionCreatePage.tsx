import { useEffect, useState } from "react";
import { Card } from "@/components/Card";
import { dept2Create, listActiveProducts } from "@/shared/api/endpoints";
import type { ProductDto } from "@/shared/api/types";
import { AutocompleteInput } from "@/components/AutocompleteInput";
import { EmployeeAutocompleteInput } from "@/components/EmployeeAutocompleteInput";

function toLocalDateTime(value: string): string {
  if (!value) return value;
  if (/Z|[+-]\d{2}:\d{2}$/.test(value)) return value;
  const match = value.match(/^(\d{4}-\d{2}-\d{2}T\d{2}:\d{2})(:\d{2})?$/);
  if (match) {
    return `${match[1]}${match[2] ?? ":00"}`;
  }
  return value;
}

export default function PfProductionCreatePage() {
  const [products, setProducts] = useState<ProductDto[]>([]);
  const [operatorMatricule, setOperatorMatricule] = useState("");
  const [reference, setReference] = useState("");
  const [quantity, setQuantity] = useState<number>(1);
  const [scrapQuantity, setScrapQuantity] = useState<number>(0);
  const [startTime, setStartTime] = useState("");
  const [endTime, setEndTime] = useState("");
  const [busy, setBusy] = useState(false);
  const [msg, setMsg] = useState<string | null>(null);

  useEffect(() => {
    (async () => {
      const p = await listActiveProducts().catch(() => [] as ProductDto[]);
      setProducts(p);
      const pf = p.find((x) => x.productType === "PRODUIT_FINI") ?? p[0];
      if (pf) setReference(pf.ref);
    })();
  }, []);

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setBusy(true);
    setMsg(null);
    try {
      await dept2Create({
        operatorMatricule: operatorMatricule.trim(),
        reference,
        quantity,
        startTime: toLocalDateTime(startTime),
        endTime: toLocalDateTime(endTime),
        scrapQuantity,
      });
      setQuantity(1);
      setScrapQuantity(0);
    } catch (e: any) {
      setMsg(e?.message ?? "Erreur.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="page mobilePage">
      <div className="grid">
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
              <div className="label">Produit</div>
              <AutocompleteInput
                value={reference}
                onChange={setReference}
                placeholder="Ex: REF-123"
                required
                productType="PRODUIT_FINI"
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
                />
              </div>
              <div className="field">
                <div className="label">Rebuts</div>
                <input
                  className="input"
                  type="text"
                  inputMode="numeric"
                  pattern="[0-9]*"
                  value={scrapQuantity}
                  onChange={(e) => {
                    const val = e.target.value.replace(/[^0-9]/g, "");
                    setScrapQuantity(Number(val) || 0);
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
                  value={startTime}
                  onChange={(e) => setStartTime(e.target.value)}
                  required
                />
              </div>
              <div className="field">
                <div className="label">Fin</div>
                <input
                  className="input"
                  type="datetime-local"
                  value={endTime}
                  onChange={(e) => setEndTime(e.target.value)}
                  required
                />
              </div>
            </div>
            <button className="btn btnPrimary" disabled={busy}>
              {busy ? "Enregistrement..." : "Enregistrer"}
            </button>
            {msg && <div className="notice">{msg}</div>}
          </form>
        </Card>
      </div>
    </div>
  );
}
