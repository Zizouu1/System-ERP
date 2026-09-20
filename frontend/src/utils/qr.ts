export type QrPayload = Partial<{
  reference: string;
  ref: string;
  lotNumber: string;
  lot: string;
  quantity: number;
  quantityOut: number;
}>;

export type QrFormData = {
  reference?: string;
  quantity?: number;
  lotNumber?: string;
};

export type QrDisplayData = {
  ref: string;
  quantity: number;
  lotNumber: string;
  generatedAt: string;
  payload: string;
};

export type PersistedQrLabel = {
  imageDataUrl: string;
  display: QrDisplayData;
};

const LATEST_QR_STORAGE_KEY = "latest_generated_qr";

export function parseQrText(text: string): QrPayload {
  const raw = text.trim();

  // JSON payload
  try {
    const obj = JSON.parse(raw);
    if (obj && typeof obj === "object") return obj as QrPayload;
  } catch {
    // Ignore invalid JSON and continue with other formats
  }

  // key=value;key=value
  if (raw.includes("=")) {
    const out: QrPayload = {};
    const parts = raw
      .split(/[;,&]/g)
      .map((s) => s.trim())
      .filter(Boolean);
    for (const p of parts) {
      const [k, v] = p.split("=").map((s) => s.trim());
      if (!k) continue;
      if (k === "quantity" || k === "quantityOut" || k === "qty") {
        const val = Number(v);
        if (!Number.isNaN(val)) {
          out.quantity = val;
          out.quantityOut = val;
        }
      } else {
        (out as Record<string, unknown>)[k] = v;
      }
    }
    return out;
  }

  // ref$qty$lot (supports | and ; for backward compatibility)
  if (raw.includes("$") || raw.includes("|") || raw.includes(";")) {
    const parts = raw.split(/[|;$]/g).map((s) => s.trim());
    const [reference, quantity, lotNumber] = parts;
    const out: QrPayload = {};
    if (reference) out.reference = reference;
    if (quantity && !Number.isNaN(Number(quantity))) {
      const q = Number(quantity);
      out.quantity = q;
      out.quantityOut = q;
    }
    if (lotNumber) out.lotNumber = lotNumber;
    return out;
  }

  // fallback: treat text as reference only
  return { reference: raw };
}

export function payloadToQrFormData(payload: QrPayload): QrFormData {
  const reference = (payload.reference ?? payload.ref ?? "").trim();
  const lotNumber = (payload.lotNumber ?? payload.lot ?? "").trim();
  const quantityCandidate =
    typeof payload.quantity === "number" ? payload.quantity : payload.quantityOut;

  const out: QrFormData = {};
  if (reference) out.reference = reference;
  if (typeof quantityCandidate === "number" && !Number.isNaN(quantityCandidate)) {
    out.quantity = quantityCandidate;
  }
  if (lotNumber) out.lotNumber = lotNumber;
  return out;
}

export function parseQrTextToFormData(text: string): QrFormData {
  return payloadToQrFormData(parseQrText(text));
}

export function buildQrContent(reference: string, quantity: number, lotNumber: string): string {
  return [reference.trim(), String(quantity), lotNumber.trim()].join("$");
}

export function buildQrDisplayData(args: {
  reference: string;
  quantity: number;
  lotNumber: string;
  generatedAt?: string;
}): QrDisplayData {
  return {
    ref: args.reference.trim(),
    quantity: args.quantity,
    lotNumber: args.lotNumber.trim(),
    generatedAt: args.generatedAt ?? new Date().toISOString(),
    payload: buildQrContent(args.reference, args.quantity, args.lotNumber),
  };
}

export function saveLatestQrLabel(entry: PersistedQrLabel): void {
  localStorage.setItem(LATEST_QR_STORAGE_KEY, JSON.stringify(entry));
}

export function loadLatestQrLabel(): PersistedQrLabel | null {
  const raw = localStorage.getItem(LATEST_QR_STORAGE_KEY);
  if (!raw) return null;
  try {
    const parsed = JSON.parse(raw) as PersistedQrLabel;
    if (
      parsed &&
      typeof parsed.imageDataUrl === "string" &&
      parsed.display &&
      typeof parsed.display.ref === "string" &&
      typeof parsed.display.quantity === "number" &&
      typeof parsed.display.lotNumber === "string" &&
      typeof parsed.display.generatedAt === "string"
    ) {
      if (typeof parsed.display.payload !== "string") {
        parsed.display.payload = buildQrContent(
          parsed.display.ref,
          parsed.display.quantity,
          parsed.display.lotNumber,
        );
      }
      return parsed;
    }
    return null;
  } catch {
    return null;
  }
}

export function clearLatestQrLabel(): void {
  localStorage.removeItem(LATEST_QR_STORAGE_KEY);
}
