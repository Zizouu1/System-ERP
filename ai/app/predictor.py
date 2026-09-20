from typing import Any, Optional, Tuple

from math import log1p

import pandas as pd

from app.schemas import DelayPredictionRequest

try:
    from training.features import (
        MODEL_FEATURE_COLUMNS,
        RAW_FEATURE_COLUMNS,
        build_model_features,
    )
except ModuleNotFoundError:
    from features import (  # type: ignore
        MODEL_FEATURE_COLUMNS,
        RAW_FEATURE_COLUMNS,
        build_model_features,
    )


def _build_features(request: DelayPredictionRequest) -> pd.DataFrame:
    row = {
        "duration": request.duration,
        "quantity_order": request.quantity_order,
        "machine_available": request.machine_available,
        "bom_depth": request.bom_depth,
        "total_operations": request.total_operations,
        "total_bom_components": request.total_bom_components,
    }
    raw_features = pd.DataFrame([row], columns=RAW_FEATURE_COLUMNS)
    return build_model_features(raw_features)


def _extract_expected_feature_columns(model: Any) -> list[str]:
    if hasattr(model, "named_steps"):
        preprocessor = model.named_steps.get("preprocessor")
        if preprocessor is not None and hasattr(preprocessor, "feature_names_in_"):
            return list(preprocessor.feature_names_in_)

    if hasattr(model, "feature_names_in_"):
        return list(model.feature_names_in_)

    return MODEL_FEATURE_COLUMNS


def _resolve_risk_message(probability: float) -> str:

    if probability < 0.30:
        return "Le risque de retard de production est faible."
    if probability < 0.70:
        return "Le risque de retard de production est moyen."
    return "Le risque de retard de production est eleve."


def predict_delay(
    model: Any, request: DelayPredictionRequest
) -> Tuple[Optional[dict[str, Any]], Optional[str]]:

    # 1. Feature Engineering
    features = _build_features(request)
    
    # 2. Validation des colonnes attendues par le modèle
    expected_columns = _extract_expected_feature_columns(model)
    missing_columns = [column for column in expected_columns if column not in features.columns]
    
    if missing_columns:
        missing_text = ", ".join(sorted(missing_columns))
        return None, (
            "Incohérence entre le modèle entraîné et les données fournies. "
            f"Colonnes manquantes : {missing_text}"
        )

    features = features[expected_columns]

    # 3. Prédiction par le modèle ML
    try:
        probability: float
        if hasattr(model, "predict_proba"):
            probabilities = model.predict_proba(features)
            probability = float(probabilities[0][1])
        elif hasattr(model, "predict"):
            predictions = model.predict(features)
            probability = float(predictions[0])
        else:
            return None, "Loaded model is not compatible with prediction."

        # Garantie que la probabilité reste dans les bornes [0, 1]
        probability = max(0.0, min(1.0, probability))
        message = _resolve_risk_message(probability)
        
        return {
            "delay_probability": probability,
            "message": message,
        }, None
    except Exception as exc:  # pragma: no cover - defensive fallback
        return None, f"Échec de la prédiction : {exc}"

