import { Link } from "react-router-dom";

export default function AccessDeniedPage() {
    return (
        <div className="page" style={{ display: "grid", placeItems: "center", minHeight: "60vh" }}>
            <div style={{ textAlign: "center" }}>
                <div style={{ fontSize: 56, marginBottom: 12 }}>🚫</div>
                <h1 style={{ margin: "0 0 8px" }}>Accès refusé</h1>
                <p className="muted">Vous n'avez pas les permissions nécessaires pour accéder à cette page.</p>
                <Link to="/" className="btn btnPrimary" style={{ marginTop: 16, display: "inline-flex" }}>
                    Retour à l'accueil
                </Link>
            </div>
        </div>
    );
}
