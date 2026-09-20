import React from "react";
import { Card } from "./Card";

export function Modal({
  open,
  title,
  onClose,
  children,
  actions,
  width = 520,
  showDefaultCloseAction = true,
  defaultCloseLabel = "Fermer",
}: React.PropsWithChildren<{
  open: boolean;
  title: string;
  onClose: () => void;
  actions?: React.ReactNode;
  width?: number;
  showDefaultCloseAction?: boolean;
  defaultCloseLabel?: string;
}>) {
  if (!open) return null;

  return (
    <>
      <div
        onClick={onClose}
        style={{
          position: "fixed",
          inset: 0,
          zIndex: 79,
          background: "rgba(15, 23, 42, 0.45)",
          backdropFilter: "blur(2px)",
        }}
      />
      <div
        style={{
          position: "fixed",
          inset: 0,
          zIndex: 80,
          display: "grid",
          placeItems: "center",
          padding: 18,
        }}
      >
        <div style={{ width: `min(${width}px, 100%)` }}>
          <Card
            title={title}
            actions={
              <div style={{ display: "flex", gap: 10, alignItems: "center" }}>
                {actions}
                {showDefaultCloseAction && (
                  <button className="btn btnGhost" onClick={onClose}>
                    {defaultCloseLabel}
                  </button>
                )}
              </div>
            }
          >
            {children}
          </Card>
        </div>
      </div>
    </>
  );
}
