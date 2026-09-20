import { useEffect, useState } from "react";
import { Card } from "@/components/Card";
import { KpiCard } from "@/components/KpiCard";
import {
  fetchModelMetrics,
  predictProductionDelay,
} from "@/shared/api/endpoints";
import type {
  ProductionDelayPredictionReq,
  ProductionDelayPredictionResp,
  ModelMetricsResp,
} from "@/shared/api/types";

type FormState = {
  duree: string;
  quantiteCommandee: string;
  machinesDisponibles: string;
  bomDepth: string;
  totalOperations: string;
  totalBomComponents: string;
};

const INITIAL_FORM: FormState = {
  duree: "",
  quantiteCommandee: "",
  machinesDisponibles: "",
  bomDepth: "",
  totalOperations: "",
  totalBomComponents: "",
};

function parseNumericInput(value: string): number | null {
  const normalized = value.trim();
  if (!normalized) return null;

  const parsed = Number(normalized);
  if (Number.isNaN(parsed) || parsed < 0) {
    return null;
  }

  return parsed;
}

type FormErrors = {
  duree?: string;
  quantiteCommandee?: string;
  machinesDisponibles?: string;
  bomDepth?: string;
  totalOperations?: string;
  totalBomComponents?: string;
};

function mapModelUnavailableMessage(errorMessage: string): string {
  const normalized = errorMessage
    .toLowerCase()
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "");

  if (normalized.includes("pas encore") && normalized.includes("entraine")) {
    return "Le modele IA n'est pas encore disponible.";
  }
  if (normalized.includes("model not trained")) {
    return "Le modele IA n'est pas encore disponible.";
  }
  return errorMessage;
}

export default function ProductionDelayPredictionPage() {
  const [form, setForm] = useState<FormState>(INITIAL_FORM);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [result, setResult] = useState<ProductionDelayPredictionResp | null>(
    null,
  );
  const [modelMetrics, setModelMetrics] = useState<ModelMetricsResp | null>(
    null,
  );
  const [metricsLoading, setMetricsLoading] = useState(true);

  const [formErrors, setFormErrors] = useState<FormErrors>({});

  useEffect(() => {
    let active = true;
    setMetricsLoading(true);
    fetchModelMetrics()
      .then((response) => {
        if (!active) return;
        setModelMetrics(response);
      })
      .catch(() => {
        if (!active) return;
        setModelMetrics(null);
      })
      .finally(() => {
        if (!active) return;
        setMetricsLoading(false);
      });

    return () => {
      active = false;
    };
  }, []);

  function updateForm<K extends keyof FormState>(key: K, value: FormState[K]) {
    setForm((prev) => ({ ...prev, [key]: value }));
    setFormErrors((prev) => ({ ...prev, [key]: undefined }));
  }

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);
    setResult(null);
    setFormErrors({});

    const errors: FormErrors = {};
    let hasError = false;

    const duree = parseNumericInput(form.duree);
    if (duree === null || duree < 1 || duree > 80) {
      errors.duree = "La durée doit être entre 1 et 80 jours";
      hasError = true;
    }
    const quantiteCommandee = parseNumericInput(form.quantiteCommandee);
    if (
      quantiteCommandee === null ||
      quantiteCommandee < 1 ||
      quantiteCommandee > 1500
    ) {
      errors.quantiteCommandee =
        "La quantité commandée doit être entre 1 et 1500";
      hasError = true;
    }
    const machinesDisponibles = parseNumericInput(form.machinesDisponibles);
    if (
      machinesDisponibles === null ||
      machinesDisponibles < 1 ||
      machinesDisponibles > 10
    ) {
      errors.machinesDisponibles =
        "Le nombre de machines doit être entre 1 et 10";
      hasError = true;
    }
    const bomDepth = parseNumericInput(form.bomDepth);
    if (bomDepth === null || bomDepth < 1 || bomDepth > 7) {
      errors.bomDepth = "La profondeur BOM doit être entre 1 et 7";
      hasError = true;
    }
    const totalOperations = parseNumericInput(form.totalOperations);
    if (
      totalOperations === null ||
      totalOperations < 0 ||
      totalOperations > 50
    ) {
      errors.totalOperations = "Le nombre d'opérations doit être entre 0 et 50";
      hasError = true;
    }
    const totalBomComponents = parseNumericInput(form.totalBomComponents);
    if (
      totalBomComponents === null ||
      totalBomComponents < 2 ||
      totalBomComponents > 72
    ) {
      errors.totalBomComponents =
        "Le nombre de composants BOM doit être entre 2 et 72";
      hasError = true;
    }

    if (hasError) {
      setFormErrors(errors);
      return;
    }

    const payload = {
      duree: duree!,
      quantiteCommandee: quantiteCommandee!,
      machinesDisponibles: machinesDisponibles!,
      bomDepth: bomDepth!,
      totalOperations: totalOperations!,
      totalBomComponents: totalBomComponents!,
    };

    setBusy(true);
    try {
      const response = await predictProductionDelay(payload);
      if (response.error) {
        setError(mapModelUnavailableMessage(response.error));
        return;
      }
      const normalized = normalizePredictionResponse(response);
      if (normalized.delayProbability === undefined) {
        setError("Reponse de prediction incomplete.");
        return;
      }
      setResult(normalized);
    } catch (e: any) {
      const message = e?.message ?? "Erreur lors de la prediction de retard.";
      setError(mapModelUnavailableMessage(message));
    } finally {
      setBusy(false);
    }
  }

  function normalizePredictionResponse(
    response: ProductionDelayPredictionResp,
  ): ProductionDelayPredictionResp {
    return {
      ...response,
      delayProbability:
        response.delayProbability ??
        response.delay_probability ??
        response.prediction,
    };
  }

  function formatMetric(value?: number | null): string {
    if (
      metricsLoading ||
      value === undefined ||
      value === null ||
      Number.isNaN(value)
    ) {
      return "--";
    }
    return `${(value * 100).toFixed(2)}%`;
  }

  return (
    <div className="page">
      <div style={{ display: "grid", gap: 18, marginBottom: 18 }}>
        <div style={{ display: "flex", flexDirection: "column", gap: 6 }}>
          <div style={{ fontWeight: 700, fontSize: "16px" }}>Random Forest</div>
          <div className="muted">Performance du modèle</div>
        </div>

        <div>
          <div
            className="kpiGrid"
            style={{
              display: "grid",
              gridTemplateColumns: "repeat(auto-fit, minmax(180px, 1fr))",
              gap: "18px",
              width: "100%",
              overflow: "visible",
              padding: 0,
            }}
          >
            <KpiCard
              label="Accuracy"
              value={formatMetric(modelMetrics?.accuracy)}
            />
            <KpiCard
              label="Précision"
              value={formatMetric(modelMetrics?.precision)}
            />
            <KpiCard
              label="Rappel"
              value={formatMetric(modelMetrics?.recall)}
            />
            <KpiCard label="F1" value={formatMetric(modelMetrics?.f1)} />
            <KpiCard
              label="ROC-AUC"
              value={formatMetric(modelMetrics?.roc_auc)}
            />
          </div>
        </div>
      </div>

      <div className="grid" style={{ width: "100%", maxWidth: "100%" }}>
        <Card title="Prediction">
          <form className="form" onSubmit={handleSubmit}>
            <div className="grid2">
              <div className="field">
                <div className="label">Duree</div>
                <input
                  className="input"
                  type="number"
                  step="1"
                  min="1"
                  max="80"
                  value={form.duree}
                  onChange={(e) => updateForm("duree", e.target.value)}
                  placeholder="Ex: 12"
                  required
                />
                {formErrors.duree && (
                  <div
                    style={{
                      color: "red",
                      fontSize: "0.85rem",
                      marginTop: "4px",
                    }}
                  >
                    {formErrors.duree}
                  </div>
                )}
              </div>

              <div className="field">
                <div className="label">Quantite commandee</div>
                <input
                  className="input"
                  type="number"
                  step="1"
                  min="1"
                  max="1500"
                  value={form.quantiteCommandee}
                  onChange={(e) =>
                    updateForm("quantiteCommandee", e.target.value)
                  }
                  placeholder="Ex: 200"
                  required
                />
                {formErrors.quantiteCommandee && (
                  <div
                    style={{
                      color: "red",
                      fontSize: "0.85rem",
                      marginTop: "4px",
                    }}
                  >
                    {formErrors.quantiteCommandee}
                  </div>
                )}
              </div>
            </div>

            <div className="grid2">
              <div className="field">
                <div className="label">Machines disponibles</div>
                <input
                  className="input"
                  type="number"
                  step="1"
                  min="1"
                  max="10"
                  value={form.machinesDisponibles}
                  onChange={(e) =>
                    updateForm("machinesDisponibles", e.target.value)
                  }
                  placeholder="Ex: 4"
                  required
                />
                {formErrors.machinesDisponibles && (
                  <div
                    style={{
                      color: "red",
                      fontSize: "0.85rem",
                      marginTop: "4px",
                    }}
                  >
                    {formErrors.machinesDisponibles}
                  </div>
                )}
              </div>

              <div className="field">
                <div className="label">Profondeur du BOM</div>
                <input
                  className="input"
                  type="number"
                  step="1"
                  min="1"
                  max="7"
                  value={form.bomDepth}
                  onChange={(e) => updateForm("bomDepth", e.target.value)}
                  placeholder="Ex: 3"
                  required
                />
                {formErrors.bomDepth && (
                  <div
                    style={{
                      color: "red",
                      fontSize: "0.85rem",
                      marginTop: "4px",
                    }}
                  >
                    {formErrors.bomDepth}
                  </div>
                )}
              </div>
            </div>

            <div className="grid2">
              <div className="field">
                <div className="label">Nombre total d'operations</div>
                <input
                  className="input"
                  type="number"
                  step="1"
                  min="0"
                  max="50"
                  value={form.totalOperations}
                  onChange={(e) =>
                    updateForm("totalOperations", e.target.value)
                  }
                  placeholder="Ex: 6"
                  required
                />
                {formErrors.totalOperations && (
                  <div
                    style={{
                      color: "red",
                      fontSize: "0.85rem",
                      marginTop: "4px",
                    }}
                  >
                    {formErrors.totalOperations}
                  </div>
                )}
              </div>

              <div className="field">
                <div className="label">Nombre total de composants</div>
                <input
                  className="input"
                  type="number"
                  step="1"
                  min="2"
                  max="72"
                  value={form.totalBomComponents}
                  onChange={(e) =>
                    updateForm("totalBomComponents", e.target.value)
                  }
                  placeholder="Ex: 40"
                  required
                />
                {formErrors.totalBomComponents && (
                  <div
                    style={{
                      color: "red",
                      fontSize: "0.85rem",
                      marginTop: "4px",
                    }}
                  >
                    {formErrors.totalBomComponents}
                  </div>
                )}
              </div>
            </div>

            <button className="btn btnPrimary" type="submit" disabled={busy}>
              {busy ? "Analyse en cours..." : "Lancer la prediction"}
            </button>
          </form>
        </Card>

        {error && <div className="notice noticeBad">{error}</div>}

        {result?.delayProbability !== undefined && (
          <div className="notice noticeOk predictionResult">
            <div className="predictionResultValue">
              {(result.delayProbability * 100).toFixed(2)}%
            </div>
            {result.message && (
              <p className="predictionResultMessage">{result.message}</p>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
