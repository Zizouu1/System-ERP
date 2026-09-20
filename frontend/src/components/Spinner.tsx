export function Spinner() {
  return (
    <div style={{ padding: 18, display: "grid", placeItems: "center" }}>
      <div style={{
        width: 26,
        height: 26,
        borderRadius: 999,
        border: "3px solid var(--border)",
        borderTopColor: "var(--primary)",
        animation: "spin 0.9s linear infinite",
      }} />
      <style>{`@keyframes spin { to { transform: rotate(360deg); } }`}</style>
    </div>
  );
}
