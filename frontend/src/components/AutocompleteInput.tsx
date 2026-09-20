import { useState, useEffect, useRef } from "react";
import { listActiveProducts } from "@/shared/api/endpoints";
import { parseQrText, QrPayload } from "@/utils/qr";

interface Props {
  value: string;
  onChange: (val: string) => void;
  onScan?: (data: QrPayload) => void;
  placeholder?: string;
  className?: string;
  required?: boolean;
  productType?: string; // Optional filter: "MATIERE_PREMIERE", "SEMI_FINI", "PRODUIT_FINI"
  productTypes?: readonly string[]; // Optional filter for multiple product types
}

export function AutocompleteInput(props: Props) {
  const {
    value,
    onChange,
    onScan,
    placeholder,
    className,
    required,
    productType,
    productTypes,
  } = props;
  const [suggestions, setSuggestions] = useState<string[]>([]);
  const [filtered, setFiltered] = useState<string[]>([]);
  const [show, setShow] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);
  const scanTimerRef = useRef<any>(null);

  // Load data from Product master data (NOT nomenclature)
  useEffect(() => {
    listActiveProducts()
      .then((products) => {
        // Filter by product type(s) if specified
        let typeFiltered = products;
        if (productTypes && productTypes.length > 0) {
          const allowed = new Set(productTypes);
          typeFiltered = products.filter((p) => allowed.has(p.productType));
        } else if (productType) {
          typeFiltered = products.filter((p) => p.productType === productType);
        }
        // Extract refs from Product master data
        setSuggestions(typeFiltered.map((p) => p.ref));
      })
      .catch(console.error);
  }, [productType, productTypes]);

  useEffect(() => {
    const s = value.trim().toLowerCase();
    if (!s) {
      setFiltered([]);
      return;
    }
    const matches = suggestions.filter((x) => x.toLowerCase().includes(s));
    setFiltered(matches.slice(0, 10));
  }, [value, suggestions]);

  useEffect(() => {
    function handleClickOutside(e: MouseEvent) {
      if (
        containerRef.current &&
        !containerRef.current.contains(e.target as Node)
      ) {
        setShow(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  return (
    <div
      className="autocomplete-container"
      ref={containerRef}
      style={{ position: "relative", width: "100%" }}
    >
      <input
        className={className || "input"}
        value={value}
        onChange={(e) => {
          const val = e.target.value;

          // Wedge scanner detection: if it contains separators, it might be a scan.
          // We use a small delay to catch the whole string if it's being "typed" fast.
          if (
            onScan &&
            (val.includes("$") || val.includes("|") || val.includes(";"))
          ) {
            if (scanTimerRef.current) clearTimeout(scanTimerRef.current);
            scanTimerRef.current = setTimeout(() => {
              const p = parseQrText(val);
              if (p.reference && (p.quantity || p.lotNumber)) {
                onScan(p);
                setShow(false);
              } else {
                onChange(val);
              }
            }, 50); // Small delay to accumulate wedge scanner characters
          } else {
            onChange(val);
          }
          setShow(true);
        }}
        onFocus={() => setShow(true)}
        placeholder={placeholder}
        required={required}
      />
      {show && filtered.length > 0 && (
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
            maxHeight: 200,
            overflowY: "auto",
          }}
        >
          {filtered.map((f, i) => (
            <div
              key={i}
              className="autocomplete-item"
              onClick={() => {
                onChange(f);
                setShow(false);
              }}
              onMouseEnter={(e) =>
                (e.currentTarget.style.backgroundColor = "var(--primary-50)")
              }
              onMouseLeave={(e) =>
                (e.currentTarget.style.backgroundColor = "transparent")
              }
              style={{
                padding: "10px 14px",
                cursor: "pointer",
                fontSize: "0.9rem",
                transition: "background 0.2s",
                borderBottom:
                  i === filtered.length - 1
                    ? "none"
                    : "1px solid var(--border)",
              }}
            >
              {f}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
