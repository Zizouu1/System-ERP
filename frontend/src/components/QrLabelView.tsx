import { printQrLabel } from "@/utils/media";
import type { PersistedQrLabel } from "@/utils/qr";

type Props = {
  label: PersistedQrLabel;
  showPrintButton?: boolean;
  printButtonLabel?: string;
};

export function QrLabelView({
  label,
  showPrintButton = true,
  printButtonLabel = "Imprimer",
}: Props) {
  const localDate = new Date(label.display.generatedAt).toLocaleString("fr-FR");

  return (
    <div className="qrLabel">
      <img className="qrImg" src={label.imageDataUrl} alt="QR" />
      <div className="qrInfo">
        <div className="qrInfoRow">
          <span>Réf</span>
          <strong>{label.display.ref}</strong>
        </div>
        <div className="qrInfoRow">
          <span>Quantité</span>
          <strong>{label.display.quantity}</strong>
        </div>
        <div className="qrInfoRow">
          <span>Numéro de lot</span>
          <strong>{label.display.lotNumber}</strong>
        </div>
        <div className="qrInfoRow">
          <span>Date</span>
          <strong>{localDate}</strong>
        </div>
      </div>
      {showPrintButton && (
        <button
          className="btn"
          onClick={() => {
            if (!label.imageDataUrl || !label.imageDataUrl.trim()) {
              alert("QR non disponible");
              return;
            }
            printQrLabel(label.imageDataUrl, {
              ref: label.display.ref,
              quantity: label.display.quantity,
              lotNumber: label.display.lotNumber,
              generatedAt: label.display.generatedAt,
            });
          }}
        >
          {printButtonLabel}
        </button>
      )}
    </div>
  );
}
