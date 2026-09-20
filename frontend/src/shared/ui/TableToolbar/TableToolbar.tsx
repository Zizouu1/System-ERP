import React, { useEffect, useRef, useState } from "react";
import { exportToCsv } from "@/utils/export";
import { exportToPdf, PdfColumn } from "@/utils/pdfExport";
import csvIcon from "@/assets/csv-ex.png";
import pdfIcon from "@/assets/pdf-ex.png";
import importIcon from "@/assets/import.png";
import filterIcon from "@/assets/filter.png";
import deleteIcon from "@/assets/delete.png";
import refreshIcon from "@/assets/refresh-page-option.png";
import { useAuth } from "@/app/providers/AuthProvider";

interface TableToolbarProps {
  exportFilename: string;
  exportTitle: string;
  rows: any[];
  columns: {
    header: string;
    cell: (row: any) => any;
    pdfCell?: (row: any) => string;
  }[];
  onImport?: () => void;
  importLabel?: string;
  extraActions?: React.ReactNode;
  filterContent?: React.ReactNode;
  onApplyFilters?: () => void;
  selectedCount?: number;
  onGlobalDelete?: () => void;
  onRefresh?: () => void;
}

export function TableToolbar({
  exportFilename,
  exportTitle,
  rows,
  columns,
  onImport,
  importLabel = "Importer CSV",
  extraActions,
  filterContent,
  onApplyFilters,
  selectedCount,
  onGlobalDelete,
  onRefresh,
}: TableToolbarProps) {
  const { role } = useAuth();
  const isAdmin = role === "admin";
  const [filterOpen, setFilterOpen] = useState(false);
  const filterRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    if (!filterOpen) return;

    function handleClickOutside(event: MouseEvent) {
      if (!filterRef.current) return;
      const target = event.target as Node;
      if (!filterRef.current.contains(target)) {
        setFilterOpen(false);
      }
    }

    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, [filterOpen]);

  const handleCsvExport = () => {
    exportToCsv(
      `${exportFilename}.csv`,
      rows,
      columns.map((c) => ({
        header: c.header,
        cell: c.cell,
      })),
    );
  };

  const handlePdfExport = () => {
    const pdfCols: PdfColumn[] = columns.map((c) => ({
      header: c.header,
      getData: (row: any) => {
        if (c.pdfCell) return c.pdfCell(row);
        const val = c.cell(row);
        if (typeof val === "string" || typeof val === "number") {
          return String(val);
        }
        if (React.isValidElement(val)) {
          const children = (val.props as any).children;
          if (typeof children === "string" || typeof children === "number") {
            return String(children);
          }
        }
        return "-";
      },
    }));
    exportToPdf(`${exportFilename}.pdf`, exportTitle, rows, pdfCols);
  };

  return (
    <div
      className="printHide"
      style={{
        display: "flex",
        flexDirection: "column",
        gap: 16,
        marginBottom: 16,
      }}
    >
      <div
        style={{
          display: "flex",
          justifyContent: "space-between",
          alignItems: "flex-start",
          flexWrap: "wrap",
          gap: 12,
        }}
      >
        <div
          style={{
            display: "flex",
            gap: 8,
            flexWrap: "wrap",
            alignItems: "center",
          }}
        >
          {isAdmin &&
            selectedCount !== undefined &&
            selectedCount > 0 &&
            onGlobalDelete && (
              <button
                className="btn btnDanger"
                style={{ display: "flex", alignItems: "center", gap: 6 }}
                onClick={onGlobalDelete}
                title={`Supprimer ${selectedCount} élément(s)`}
              >
                <img
                  src={deleteIcon}
                  alt="Delete"
                  style={{
                    width: 16,
                    height: 16,
                    filter: "brightness(0) invert(1)",
                  }}
                />
                <span>Supprimer ({selectedCount})</span>
              </button>
            )}

          {isAdmin && (
            <>
              <button
                className="btn"
                style={{ display: "flex", alignItems: "center", gap: 6 }}
                onClick={handleCsvExport}
                title="Export CSV"
              >
                <img
                  src={csvIcon}
                  alt="CSV"
                  style={{ width: 16, height: 16 }}
                />
              </button>
              <button
                className="btn"
                style={{ display: "flex", alignItems: "center", gap: 6 }}
                onClick={handlePdfExport}
                title="Export PDF"
              >
                <img
                  src={pdfIcon}
                  alt="PDF"
                  style={{ width: 16, height: 16 }}
                />
              </button>
            </>
          )}
          {isAdmin && onImport && (
            <button
              className="btn"
              style={{ display: "flex", alignItems: "center", gap: 6 }}
              onClick={onImport}
              title={importLabel}
            >
              <img
                src={importIcon}
                alt="Import"
                style={{ width: 16, height: 16 }}
              />
              <span>{importLabel}</span>
            </button>
          )}
        </div>

        {(filterContent || onRefresh) && (
          <div
            style={{
              display: "flex",
              gap: 8,
              flexWrap: "wrap",
              alignItems: "center",
            }}
          >
            {onRefresh && (
              <button
                className="btn"
                style={{ display: "flex", alignItems: "center", gap: 6 }}
                onClick={onRefresh}
                title="Rafraîchir"
              >
                <img
                  src={refreshIcon}
                  alt="Rafraîchir"
                  style={{ width: 16, height: 16 }}
                />
              </button>
            )}

            {filterContent && (
              <div style={{ position: "relative" }} ref={filterRef}>
                <button
                  className="btn"
                  style={{ display: "flex", alignItems: "center", gap: 6 }}
                  onClick={() => setFilterOpen((prev) => !prev)}
                >
                  <img
                    src={filterIcon}
                    alt="Filtrer"
                    style={{ width: 16, height: 16 }}
                  />
                  <span>Filtrer</span>
                </button>

                {filterOpen && (
                  <div className="filterPopover">
                    <div className="filterPopoverBody">{filterContent}</div>
                    <div className="filterPopoverActions">
                      <button
                        className="btn btnPrimary"
                        onClick={() => {
                          onApplyFilters?.();
                          setFilterOpen(false);
                        }}
                      >
                        Appliquer
                      </button>
                      <button
                        className="btn btnGhost"
                        onClick={() => setFilterOpen(false)}
                      >
                        Fermer
                      </button>
                    </div>
                  </div>
                )}
              </div>
            )}
          </div>
        )}
      </div>

      {extraActions && (
        <div style={{ display: "flex", justifyContent: "flex-end" }}>
          {extraActions}
        </div>
      )}
    </div>
  );
}
