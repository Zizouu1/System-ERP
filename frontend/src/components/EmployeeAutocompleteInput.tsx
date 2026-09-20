import { useEffect, useRef, useState } from "react";
import { lookupEmployees } from "@/shared/api/endpoints";
import type { EmployeeLookupDto } from "@/shared/api/types";

type Props = {
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
  className?: string;
  required?: boolean;
  disabled?: boolean;
};

function employeeLabel(employee: EmployeeLookupDto): string {
  const fullName = [employee.nom, employee.prenom].filter(Boolean).join(" ");
  const details = [fullName, employee.poste, employee.departement]
    .filter(Boolean)
    .join(" - ");
  return details ? `${employee.matricule} - ${details}` : employee.matricule;
}

export function EmployeeAutocompleteInput({
  value,
  onChange,
  placeholder,
  className,
  required,
  disabled,
}: Props) {
  const [items, setItems] = useState<EmployeeLookupDto[]>([]);
  const [show, setShow] = useState(false);
  const [loading, setLoading] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    function onOutsideClick(event: MouseEvent) {
      if (
        containerRef.current &&
        !containerRef.current.contains(event.target as Node)
      ) {
        setShow(false);
      }
    }

    document.addEventListener("mousedown", onOutsideClick);
    return () => document.removeEventListener("mousedown", onOutsideClick);
  }, []);

  useEffect(() => {
    if (!show || disabled) return;

    let cancelled = false;
    const timeoutId = window.setTimeout(async () => {
      try {
        setLoading(true);
        const data = await lookupEmployees(value.trim());
        if (!cancelled) {
          setItems(data.slice(0, 10));
        }
      } catch {
        if (!cancelled) {
          setItems([]);
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    }, 200);

    return () => {
      cancelled = true;
      window.clearTimeout(timeoutId);
    };
  }, [value, show, disabled]);

  return (
    <div
      ref={containerRef}
      className="autocomplete-container"
      style={{ position: "relative", width: "100%" }}
    >
      <input
        className={className || "input"}
        value={value}
        onChange={(event) => {
          onChange(event.target.value);
          setShow(true);
        }}
        onFocus={() => setShow(true)}
        placeholder={placeholder}
        required={required}
        disabled={disabled}
      />

      {show && (items.length > 0 || loading) && (
        <div
          className="autocomplete-dropdown"
          style={{
            position: "absolute",
            top: "calc(100% + 4px)",
            left: 0,
            right: 0,
            zIndex: 1000,
            background: "var(--surface)",
            border: "1px solid var(--border)",
            borderRadius: "var(--radius)",
            boxShadow: "var(--shadow)",
            maxHeight: 240,
            overflowY: "auto",
          }}
        >
          {loading && (
            <div className="muted" style={{ padding: "10px 14px" }}>
              Chargement...
            </div>
          )}

          {!loading &&
            items.map((employee, index) => (
              <button
                key={`${employee.matricule}-${index}`}
                type="button"
                className="searchResultItem"
                style={{
                  width: "100%",
                  textAlign: "left",
                  border: "none",
                  borderBottom:
                    index === items.length - 1 ? "none" : "1px solid var(--border)",
                  borderRadius: 0,
                  background: "transparent",
                  padding: "10px 14px",
                  cursor: "pointer",
                }}
                onClick={() => {
                  onChange(employee.matricule);
                  setShow(false);
                }}
              >
                {employeeLabel(employee)}
              </button>
            ))}
        </div>
      )}
    </div>
  );
}
