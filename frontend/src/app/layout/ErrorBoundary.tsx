import React, { Component, ErrorInfo, ReactNode } from "react";

interface Props {
    children: ReactNode;
}

interface State {
    hasError: boolean;
    error: Error | null;
}

export class ErrorBoundary extends Component<Props, State> {
    public state: State = {
        hasError: false,
        error: null,
    };

    public static getDerivedStateFromError(error: Error): State {
        return { hasError: true, error };
    }

    public componentDidCatch(error: Error, errorInfo: ErrorInfo) {
        console.error("Uncaught error:", error, errorInfo);
    }

    public render() {
        if (this.state.hasError) {
            return (
                <div
                    style={{
                        height: "100vh",
                        display: "flex",
                        flexDirection: "column",
                        alignItems: "center",
                        justifyContent: "center",
                        backgroundColor: "#f3f4f6",
                        color: "#111827",
                        fontFamily: "Inter, system-ui, -apple-system, sans-serif",
                        textAlign: "center",
                        padding: "20px",
                    }}
                >
                    <div style={{ fontSize: "64px", marginBottom: "24px" }}>!</div>
                    <h1 style={{ fontSize: "24px", marginBottom: "16px" }}>Une erreur inattendue est survenue</h1>
                    <p style={{ color: "#6b7280", marginBottom: "32px", maxWidth: "400px" }}>
                        L'application a rencontre un probleme. Veuillez rafraichir la page ou contacter le support si le probleme persiste.
                    </p>
                    <button
                        onClick={() => window.location.reload()}
                        style={{
                            backgroundColor: "#4c1d95",
                            color: "white",
                            border: "none",
                            padding: "12px 24px",
                            borderRadius: "8px",
                            fontWeight: "600",
                            cursor: "pointer",
                            transition: "background-color 0.2s",
                        }}
                        onMouseOver={(e) => (e.currentTarget.style.backgroundColor = "#5b21b6")}
                        onMouseOut={(e) => (e.currentTarget.style.backgroundColor = "#4c1d95")}
                    >
                        Recharger la page
                    </button>
                    {import.meta.env.DEV && this.state.error && (
                        <pre
                            style={{
                                marginTop: "40px",
                                padding: "16px",
                                backgroundColor: "#f9fafb",
                                borderRadius: "8px",
                                fontSize: "12px",
                                textAlign: "left",
                                maxWidth: "90vw",
                                overflow: "auto",
                                color: "#4b5563",
                            }}
                        >
                            {this.state.error.toString()}
                        </pre>
                    )}
                </div>
            );
        }

        return this.props.children;
    }
}
