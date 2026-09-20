import { useState } from "react";
import { Navigate, useLocation } from "react-router-dom";
import { useAuth } from "../app/providers/AuthProvider";
import { getHomePath } from "@/shared/utils/jwt";
import "./login.css";
import logoImg from "../assets/Xpng.png";

export default function LoginPage() {
  const { token, user, login } = useAuth();
  const location = useLocation() as any;

  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState<string | null>(
    location?.state?.message ?? null,
  );

  if (token && user) {
    return <Navigate to={location?.state?.from ?? getHomePath(user.role)} replace />;
  }

  async function onSubmit(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setMessage(null);
    setBusy(true);
    try {
      await login(username.trim(), password);
    } catch (err: any) {
      setMessage(err?.message ?? "Verifiez vos identifiants.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="authScreen">
      <main className="authMain">
        <div className="authBlob authBlobLeft" aria-hidden="true" />
        <div className="authBlob authBlobRight" aria-hidden="true" />

        <section className="authCard">
          <div className="authBrand">
            <span className="authLogoWrap" aria-hidden="true">
              <img src={logoImg} alt="" className="authLogo" />
            </span>
            <div>
              <h1 className="authTitle">Axone</h1>
              <p className="authSubtitle">Connexion</p>
            </div>
          </div>

          <form className="authForm" onSubmit={onSubmit}>
            <input
              className="authInput"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              autoComplete="username"
              placeholder="Nom d'utilisateur"
              required
            />

            <input
              className="authInput"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              autoComplete="current-password"
              placeholder="Mot de passe"
              required
            />

            <button className="authSubmitBtn" type="submit" disabled={busy}>
              {busy ? "Connexion..." : "Se connecter"}
            </button>

            {message && <div className="authError">{message}</div>}
          </form>

          <div className="authFooter">Axone - Tous droits reserves.</div>
        </section>
      </main>
    </div>
  );
}
