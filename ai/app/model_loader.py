from pathlib import Path
from typing import Any, Optional, Tuple

import json

import joblib


MODEL_PATH = (
    Path(__file__).resolve().parents[1] / "models" / "production_delay_model.pkl"
)
METRICS_PATH = (
    Path(__file__).resolve().parents[1] / "models" / "production_delay_metrics.json"
)


def load_model() -> Tuple[Optional[Any], Optional[str]]:
    if not MODEL_PATH.exists():
        return None, "Model not trained yet. Please run the training pipeline first."

    try:
        return joblib.load(MODEL_PATH), None
    except Exception as exc:  # pragma: no cover - defensive fallback
        return None, f"Échec du chargement du modèle : {exc}"


def load_metrics() -> Optional[dict[str, float]]:
    if not METRICS_PATH.exists():
        return None

    try:
        metrics = json.loads(METRICS_PATH.read_text(encoding="utf-8"))
    except Exception:
        return None

    if isinstance(metrics, dict):
        return {
            key: float(value)
            for key, value in metrics.items()
            if isinstance(value, (int, float))
        }
    return None

