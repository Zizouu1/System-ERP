from pathlib import Path
from typing import Any, Optional, Tuple

import joblib


MODEL_PATH = (
    Path(__file__).resolve().parents[1] / "models" / "production_delay_model.pkl"
)


def load_model() -> Tuple[Optional[Any], Optional[str]]:
    if not MODEL_PATH.exists():
        return None, "Model not trained yet. Please run the training pipeline first."

    try:
        return joblib.load(MODEL_PATH), None
    except Exception as exc:  # pragma: no cover - defensive fallback
        return None, f"Failed to load model: {exc}"

