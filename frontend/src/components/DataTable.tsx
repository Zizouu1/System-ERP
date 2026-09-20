import React from "react";
import { cn } from "../utils/cn";
import { useAuth } from "@/app/providers/AuthProvider";
import { Modal } from "@/components/Modal";
import refreshIcon from "@/assets/refresh-page-option.png";

const TEXT_PREVIEW_LIMIT = 32;

function toTextPreview(value: string, limit = TEXT_PREVIEW_LIMIT) {
  const normalized = value.trim();
  if (normalized.length <= limit) return value;
  return `${normalized.slice(0, limit)}…`;
}

export type Column<T> = {
  header: string;
  cell: (row: T) => React.ReactNode;
  className?: string;
};

export function DataTable<T>({
  rows,
  columns,
  rowKey,
  rowClassName,
  emptyText = "No data",
  enableSelection = false,
  selectedKeys = [],
  onSelectionChange,
  onRefresh,
  highlightedKey,
}: {
  rows: T[];
  columns: Column<T>[];
  rowKey: (row: T) => string | number;
  rowClassName?: (row: T) => string | undefined;
  emptyText?: string;
  enableSelection?: boolean;
  selectedKeys?: (string | number)[];
  onSelectionChange?: (keys: (string | number)[]) => void;
  onRefresh?: () => void;
  highlightedKey?: string | number;
}) {
  const { role } = useAuth();
  const [expandedText, setExpandedText] = React.useState<string | null>(null);
  const tableWrapRef = React.useRef<HTMLDivElement | null>(null);
  const canSelect = enableSelection && role === "admin";
  const allSelected = rows.length > 0 && selectedKeys.length === rows.length;
  const someSelected =
    selectedKeys.length > 0 && selectedKeys.length < rows.length;

  const handleSelectAll = () => {
    if (allSelected) {
      onSelectionChange?.([]);
    } else {
      onSelectionChange?.(rows.map(rowKey));
    }
  };

  const renderCellValue = (value: React.ReactNode) => {
    if (typeof value === "string" && value.trim().length > TEXT_PREVIEW_LIMIT) {
      return (
        <button
          className="tableTextPreviewBtn"
          onClick={() => setExpandedText(value)}
          title={value}
          type="button"
        >
          {toTextPreview(value)}
        </button>
      );
    }
    return value;
  };

  React.useEffect(() => {
    if (highlightedKey === undefined || highlightedKey === null) {
      return;
    }

    const key = String(highlightedKey);
    const root = tableWrapRef.current;
    if (!root) return;

    const escaped =
      typeof CSS !== "undefined" && "escape" in CSS
        ? CSS.escape(key)
        : key.replace(/"/g, '\\"');

    const row = root.querySelector(
      `tr[data-row-key="${escaped}"]`,
    ) as HTMLTableRowElement | null;
    if (!row) return;

    row.scrollIntoView({ behavior: "smooth", block: "center" });
  }, [highlightedKey, rows]);

  return (
    <>
      {onRefresh && (
        <div className="tableTopActions printHide">
          <button
            className="btn"
            onClick={onRefresh}
            title="Rafraîchir"
            type="button"
          >
            <img
              src={refreshIcon}
              alt="Rafraîchir"
              style={{ width: 16, height: 16 }}
            />
          </button>
        </div>
      )}
      <div className="tableWrap" ref={tableWrapRef}>
        <table className="table">
          <thead>
            <tr>
              {canSelect && (
                <th style={{ width: 40, textAlign: "center" }}>
                  <input
                    type="checkbox"
                    checked={allSelected}
                    ref={(input) => {
                      if (input) input.indeterminate = someSelected;
                    }}
                    onChange={handleSelectAll}
                  />
                </th>
              )}
              {columns.map((c, i) => (
                <th key={i} className={c.className}>
                  {c.header}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {rows.length === 0 ? (
              <tr>
                <td
                  colSpan={columns.length + (canSelect ? 1 : 0)}
                  className="muted"
                  style={{ padding: 14 }}
                >
                  {emptyText}
                </td>
              </tr>
            ) : (
              rows.map((r) => {
                const key = rowKey(r);
                const isSelected = selectedKeys.includes(key);
                return (
                  <tr
                    key={key}
                    data-row-key={String(key)}
                    className={cn(
                      rowClassName?.(r),
                      highlightedKey !== undefined &&
                        highlightedKey !== null &&
                        String(key) === String(highlightedKey) &&
                        "rowHighlighted",
                      isSelected && "selectedRow",
                    )}
                  >
                    {canSelect && (
                      <td style={{ textAlign: "center", width: 40 }}>
                        <input
                          type="checkbox"
                          checked={isSelected}
                          onChange={(e) => {
                            if (e.target.checked) {
                              onSelectionChange?.([...selectedKeys, key]);
                            } else {
                              onSelectionChange?.(
                                selectedKeys.filter((k) => k !== key),
                              );
                            }
                          }}
                        />
                      </td>
                    )}
                    {columns.map((c, i) => (
                      <td key={i} className={cn(c.className)}>
                        {renderCellValue(c.cell(r))}
                      </td>
                    ))}
                  </tr>
                );
              })
            )}
          </tbody>
        </table>
      </div>

      <Modal
        open={Boolean(expandedText)}
        title="Texte complet"
        onClose={() => setExpandedText(null)}
        width={640}
      >
        <p className="tableTextFull">{expandedText}</p>
      </Modal>
    </>
  );
}
