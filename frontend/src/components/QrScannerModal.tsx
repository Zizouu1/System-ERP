import React, { useEffect, useRef, useState } from "react";
import { BrowserQRCodeReader } from "@zxing/browser";
import { Card } from "./Card";

export function QrScannerModal({
  open,
  onClose,
  onScan,
}: {
  open: boolean;
  onClose: () => void;
  onScan: (text: string) => void;
}) {
  const videoRef = useRef<HTMLVideoElement | null>(null);
  const controlsRef = useRef<any>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!open) return;

    let cancelled = false;
    setError(null);

    const reader = new BrowserQRCodeReader();

    (async () => {
      try {
        if (!videoRef.current) return;
        const controls = await reader.decodeFromVideoDevice(undefined, videoRef.current, (result: any) => {
          if (!result) return;
          const text = result.getText();
          if (cancelled) return;
          onScan(text);
          onClose();
          try {
            controlsRef.current?.stop?.();
          } catch { }
        });
        controlsRef.current = controls;
      } catch (e: any) {
        setError(e?.message ?? "Caméra indisponible");
      }
    })();

    return () => {
      cancelled = true;
      try {
        controlsRef.current?.stop?.();
      } catch { }
      try {
        controlsRef.current?.stop?.();
      } catch { }
    };
  }, [open, onClose, onScan]);

  if (!open) return null;

  return (
    <>
      <div className="overlay" onClick={onClose} />
      <div
        style={{
          position: "fixed",
          inset: 0,
          zIndex: 70,
          display: "grid",
          placeItems: "center",
          padding: 18,
        }}
      >
        <div style={{ width: "min(560px, 100%)" }}>
          <Card
            title="Scanner le QR code"
            actions={<button className="btn btnGhost" onClick={onClose}>Fermer</button>}
          >
            <div className="muted" style={{ marginBottom: 10 }}>
              Alignez le QR code dans le cadre.
            </div>

            <div
              style={{
                borderRadius: 14,
                overflow: "hidden",
                border: "1px solid var(--border)",
                background: "black",
              }}
            >
              <video ref={videoRef} style={{ width: "100%", display: "block" }} />
            </div>

            {error && <div className="notice" style={{ marginTop: 12 }}>{error}</div>}
          </Card>
        </div>
      </div>
    </>
  );
}
