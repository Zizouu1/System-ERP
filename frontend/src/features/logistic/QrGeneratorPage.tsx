import { useState } from "react";
import { Card } from "@/components/Card";
import { QrLabelView } from "@/components/QrLabelView";
import { logisticGenerateQr } from "@/shared/api/endpoints";

import {
  buildQrDisplayData,
  loadLatestQrLabel,
  saveLatestQrLabel,
  type PersistedQrLabel,
} from "@/utils/qr";

export default function QrGeneratorPage() {
  const [reference, setReference] = useState("");
  const [quantity, setQuantity] = useState<number>(1);
  const [lotNumber, setLotNumber] = useState("");
  const [busy, setBusy] = useState(false);
  const [latestQr, setLatestQr] = useState<PersistedQrLabel | null>(() =>
    loadLatestQrLabel(),
  );
  const [msg, setMsg] = useState<string | null>(null);

  async function generate(e: React.FormEvent) {
    e.preventDefault();
    setBusy(true);
    setMsg(null);
    try {
      const ref = reference.trim();
      const lot = lotNumber.trim();
      const blob = await logisticGenerateQr(ref, quantity, lot);

      // Log blob details for debugging
      console.log("[QR-generator] blob type:", blob.type, "size:", blob.size);

      if (!blob.type.startsWith("image/")) {
        console.error("[QR-generator] API returned non-image blob:", blob.type);
        const text = await blob.text();
        console.error("[QR-generator] blob content preview:", text.substring(0, 200));
        setMsg("Le serveur n'a pas retourné une image QR valide.");
        return;
      }

      // Use object URL instead of data URL for reliability
      const qrObjectUrl = URL.createObjectURL(blob);
      console.log("[QR-generator] object URL created:", qrObjectUrl);

      const entry: PersistedQrLabel = {
        imageDataUrl: qrObjectUrl,
        display: buildQrDisplayData({
          reference: ref,
          quantity,
          lotNumber: lot,
        }),
      };
      setLatestQr(entry);
      saveLatestQrLabel(entry);
    } catch (err: any) {
      setMsg(err?.message ?? "Erreur.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="page mobilePage">
      <div className="pageHeader">
        <div>
          <h1>Générateur QR</h1>
          <div className="muted">Générer une étiquette</div>
        </div>
      </div>
      <div className="grid">
        <Card title="Paramètres">
          <form className="form" onSubmit={generate}>
            <div className="field">
              <div className="label">Réf</div>
              <input
                className="input"
                value={reference}
                onChange={(e) => setReference(e.target.value)}
                required
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
                  required
                />
              </div>
            </div>
            <button className="btn btnPrimary" disabled={busy}>
              {busy ? "Génération..." : "Générer un QR code"}
            </button>
            {msg && <div className="notice">{msg}</div>}
          </form>
        </Card>
        {latestQr && (
          <Card title="Étiquette QR">
            <QrLabelView label={latestQr} />
          </Card>
        )}
      </div>
    </div>
  );
}
