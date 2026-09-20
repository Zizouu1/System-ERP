import { Modal } from "@/components/Modal";
import type { OperationAuditEntryDto } from "@/shared/api/types";

function hasValues(data: Record<string, unknown> | undefined) {
  return !!data && Object.keys(data).length > 0;
}

function valueToText(value: unknown): string {
  if (value === null) return "null";
  if (value === undefined) return "-";
  if (typeof value === "string") return value;
  if (typeof value === "number" || typeof value === "boolean") {
    return String(value);
  }
  try {
    return JSON.stringify(value);
  } catch {
    return String(value);
  }
}

function isChanged(oldValue: unknown, newValue: unknown): boolean {
  return valueToText(oldValue) !== valueToText(newValue);
}

function formatDate(value?: string): string {
  if (!value) return "-";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "-";

  const dd = String(date.getDate()).padStart(2, "0");
  const mm = String(date.getMonth() + 1).padStart(2, "0");
  const yyyy = date.getFullYear();
  const hh = String(date.getHours()).padStart(2, "0");
  const min = String(date.getMinutes()).padStart(2, "0");

  return `${dd}/${mm}/${yyyy} ${hh}:${min}`;
}

const FIELD_LABELS: Record<string, string> = {
  id: "ID",
  reference: "Référence",
  quantity: "Quantité",
  quantityPerBatch: "Quantité par lot",
  batches: "Nombre de lots",
  scrapQuantity: "Rebuts",
  operatorMatricule: "Matricule opérateur",
  producedByCutMachine: "Machine de découpe",
  startTime: "Heure début",
  endTime: "Heure fin",
  lastModifiedAt: "Dernière modification",
  lastModifiedBy: "Modifié par",
};

function formatFieldValue(key: string, value: unknown): string {
  if (value === null || value === undefined || value === "")
    return "N'existe pas";
  if (typeof value === "boolean") return value ? "Oui" : "Non";

  if (key === "lastModifiedAt" || key === "startTime" || key === "endTime") {
    if (typeof value === "string") {
      if (/^\d{2}:\d{2}(:\d{2})?$/.test(value)) {
        return value.substring(0, 5);
      }
      const date = new Date(value);
      if (!Number.isNaN(date.getTime())) {
        return formatDate(value);
      }
    }
  }

  return String(value);
}

export function OperationAuditModal({
  open,
  title,
  loading,
  errorMessage,
  entries,
  onClose,
}: {
  open: boolean;
  title: string;
  loading: boolean;
  errorMessage?: string | null;
  entries: OperationAuditEntryDto[];
  onClose: () => void;
}) {
  return (
    <Modal
      open={open}
      title={title}
      onClose={onClose}
      actions={null}
      width={900}
    >
      {loading ? (
        <div className="muted">Chargement...</div>
      ) : errorMessage ? (
        <div className="notice">{errorMessage}</div>
      ) : entries.length === 0 ? (
        <div className="muted">Aucun historique de modification.</div>
      ) : (
        <div
          style={{
            maxHeight: "70vh",
            overflow: "auto",
            border: "1px solid var(--border)",
            borderRadius: 10,
          }}
        >
          <table style={{ width: "100%", borderCollapse: "collapse" }}>
            <thead>
              <tr style={{ background: "var(--surface-2)" }}>
                <th
                  style={{
                    textAlign: "left",
                    padding: "10px 12px",
                    borderBottom: "1px solid var(--border)",
                    whiteSpace: "nowrap",
                  }}
                >
                  Date de modification
                </th>
                <th
                  style={{
                    textAlign: "left",
                    padding: "10px 12px",
                    borderBottom: "1px solid var(--border)",
                    whiteSpace: "nowrap",
                  }}
                >
                  Modifié par
                </th>
                <th
                  style={{
                    textAlign: "left",
                    padding: "10px 12px",
                    borderBottom: "1px solid var(--border)",
                  }}
                >
                  Anciennes données
                </th>
              </tr>
            </thead>
            <tbody>
              {entries.map((entry, idx) => {
                const fallbackOldValues = hasValues(entry.oldValues)
                  ? entry.oldValues
                  : (entries[idx + 1]?.newValues ?? {});

                const oldData = fallbackOldValues ?? {};
                const fields = Object.keys(oldData).filter(
                  (field) => field in FIELD_LABELS,
                );

                return (
                  <tr key={entry.id}>
                    <td
                      style={{
                        padding: "10px 12px",
                        borderBottom: "1px solid var(--border)",
                        whiteSpace: "nowrap",
                        verticalAlign: "top",
                      }}
                    >
                      {formatDate(entry.changedAt)}
                    </td>
                    <td
                      style={{
                        padding: "10px 12px",
                        borderBottom: "1px solid var(--border)",
                        whiteSpace: "nowrap",
                        verticalAlign: "top",
                      }}
                    >
                      {entry.changedByUsername || "-"}
                    </td>
                    <td
                      style={{
                        padding: "10px 12px",
                        borderBottom: "1px solid var(--border)",
                        verticalAlign: "top",
                      }}
                    >
                      {fields.length === 0 ? (
                        <span className="muted">-</span>
                      ) : (
                        <div
                          style={{
                            display: "flex",
                            flexDirection: "column",
                            gap: 8,
                          }}
                        >
                          {fields.map((field) => {
                            const oldValue = oldData[field];
                            const newValue = entry.newValues?.[field];
                            const changed = isChanged(oldValue, newValue);

                            return (
                              <div
                                key={`${entry.id}-${field}`}
                                style={{
                                  padding: "6px 8px",
                                  borderRadius: 6,
                                  border: "1px solid var(--border)",
                                  background: changed
                                    ? "#FEECEC"
                                    : "transparent",
                                  color: changed ? "#B42318" : "inherit",
                                  wordBreak: "break-word",
                                }}
                              >
                                <span>{FIELD_LABELS[field] || field} :</span>{" "}
                                <strong>
                                  {formatFieldValue(field, oldValue)}
                                </strong>
                              </div>
                            );
                          })}
                        </div>
                      )}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}
    </Modal>
  );
}
