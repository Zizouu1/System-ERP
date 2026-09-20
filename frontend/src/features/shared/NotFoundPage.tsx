import { Link } from "react-router-dom";

export default function NotFoundPage() {
    return (
        <div className="page" style={{ display: "grid", placeItems: "center", minHeight: "60vh" }}>
            <div style={{ textAlign: "center" }}>
                <div style={{ fontSize: 56, marginBottom: 12 }}>404</div>
                <h1 style={{ margin: "0 0 8px" }}>Page introuvable</h1>
                <p className="muted">Cette page n'existe pas ou a été déplacée.</p>
                <Link to="/" className="btn btnPrimary" style={{ marginTop: 16, display: "inline-flex" }}>
                    Retour à l'accueil
                </Link>
            </div>
        </div>
    );
}
