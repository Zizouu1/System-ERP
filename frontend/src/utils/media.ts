export async function blobToDataUrl(blob: Blob): Promise<string> {
  return await new Promise((resolve, reject) => {
    const r = new FileReader();
    r.onload = () => resolve(String(r.result));
    r.onerror = reject;
    r.readAsDataURL(blob);
  });
}

export function ensureDataUrlMaybeBase64(value: string): string {
  // If already data URL, return it
  if (value.startsWith("data:image")) return value;
  // If raw base64, wrap it
  return `data:image/png;base64,${value}`;
}

export type PrintableQrData = {
  ref: string;
  quantity: number | string;
  lotNumber: string;
  generatedAt: string;
};

function escapeHtml(value: string): string {
  return value
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;");
}

export function printQrLabel(dataUrl: string, data: PrintableQrData) {
  // Step 1: Validate input
  if (!dataUrl || typeof dataUrl !== "string" || !dataUrl.trim()) {
    console.error("[QR-print] dataUrl is empty or not a string:", dataUrl);
    alert("Impossible d'imprimer : le QR code est vide.");
    return;
  }

  // Step 1: Debug logs
  console.log("[QR-print] dataUrl received, length:", dataUrl.length);
  console.log("[QR-print] dataUrl preview:", dataUrl.substring(0, 120));

  const localDate = new Date(data.generatedAt).toLocaleString("fr-FR");

  // Determine if this is a blob/object URL or a data URL
  const isBlobUrl = dataUrl.startsWith("blob:");
  let imgSrc: string;

  if (isBlobUrl) {
    // Blob URL – use directly, no need to validate base64
    console.log("[QR-print] Using blob URL directly");
    imgSrc = dataUrl;
  } else {
    // Data URL path – ensure proper format
    const safeDataUrl = ensureDataUrlMaybeBase64(dataUrl);
    console.log("[QR-print] safeDataUrl length:", safeDataUrl.length);
    console.log("[QR-print] safeDataUrl preview:", safeDataUrl.substring(0, 120));

    // Step 2: Validate data URL structure
    if (!safeDataUrl.startsWith("data:image/") || !safeDataUrl.includes("base64,")) {
      console.error("[QR-print] Invalid data URL format:", safeDataUrl.substring(0, 80));
      alert("Impossible d'imprimer : le QR code est vide.");
      return;
    }

    // Check the base64 body is not empty
    const base64Part = safeDataUrl.split("base64,")[1];
    if (!base64Part || !base64Part.trim()) {
      console.error("[QR-print] base64 body is empty");
      alert("Impossible d'imprimer : le QR code est vide.");
      return;
    }

    console.log("[QR-print] base64 body length:", base64Part.length);
    imgSrc = safeDataUrl;
  }

  // Pre-load the image to warm the browser cache
  const preload = new Image();
  preload.src = imgSrc;
  preload.onload = () => {
    console.log("[QR-print] Image preloaded successfully");
    openPrintWindow(imgSrc, localDate, data);
  };
  preload.onerror = () => {
    console.warn("[QR-print] Image preload failed, opening print window anyway");
    openPrintWindow(imgSrc, localDate, data);
  };
}

function openPrintWindow(
  imgSrc: string,
  localDate: string,
  data: PrintableQrData,
) {
  // Removed "noopener,noreferrer" – those flags prevent document.write on the child window
  const w = window.open("", "_blank", "width=520,height=700");
  if (!w) {
    console.error("[QR-print] window.open returned null (popup blocked?)");
    alert("Impossible d'ouvrir la fenêtre d'impression. Vérifiez les popups.");
    return;
  }

  const ref = escapeHtml(data.ref);
  const quantity = escapeHtml(String(data.quantity));
  const lot = escapeHtml(data.lotNumber);
  const date = escapeHtml(localDate);

  w.document.open();
  w.document.write(`
    <html>
      <head>
        <title>QR</title>
        <style>
          body { margin: 0; padding: 24px; font-family: Arial, sans-serif; color: #0f172a; }
          .wrapper { max-width: 360px; margin: 0 auto; display: grid; gap: 14px; }
          .qrBox { display: flex; justify-content: center; }
          #qr-image { width: 280px; height: 280px; max-width: 100%; border: 1px solid #d1d5db; border-radius: 12px; }
          .details { border: 1px solid #d1d5db; border-radius: 12px; padding: 12px 14px; display: grid; gap: 8px; }
          .row { display: flex; justify-content: space-between; gap: 10px; }
          .label { color: #475569; }
          .debug { font-size: 10px; color: #94a3b8; margin-top: 8px; word-break: break-all; }
        </style>
      </head>
      <body>
        <div class="wrapper">
          <div class="qrBox">
            <img id="qr-image" src="${imgSrc}" alt="QR" onerror="this.style.display='none'" />
          </div>
          <div class="details">
            <div class="row"><span class="label">Réf</span><strong>${ref}</strong></div>
            <div class="row"><span class="label">Quantité</span><strong>${quantity}</strong></div>
            <div class="row"><span class="label">Numéro de lot</span><strong>${lot}</strong></div>
            <div class="row"><span class="label">Date</span><strong>${date}</strong></div>
          </div>
        </div>
      </body>
    </html>
  `);
  w.document.close();

  // Step 4: Robust interval – prints after 4 seconds max even if image never loads
  let attempts = 0;
  const maxAttempts = 40; // 4 seconds
  const timer = window.setInterval(() => {
    const img = w.document.getElementById(
      "qr-image",
    ) as HTMLImageElement | null;
    const ready =
      img && img.complete && img.naturalWidth > 0 && img.naturalHeight > 0;
    if (ready) {
      console.log("[QR-print] Image ready, printing now");
      window.clearInterval(timer);
      w.focus();
      w.print();
      window.setTimeout(() => w.close(), 250);
    } else if (++attempts >= maxAttempts) {
      console.warn("[QR-print] Timeout waiting for image, printing anyway");
      window.clearInterval(timer);
      // Print anyway so at least the text is visible
      w.focus();
      w.print();
      window.setTimeout(() => w.close(), 250);
    }
  }, 100);
}
