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


def _compute_pressure_floor(features: pd.DataFrame) -> float:
    row = features.iloc[0]

    quantity_order = float(row["quantity_order"])
    machine_available = float(row["machine_available"])
    bom_depth = float(row["bom_depth"])
    total_operations = float(row["total_operations"])

    workload_score = min(log1p(float(row["workload"])) / log1p(2500.0), 1.0)
    throughput_score = min(
        log1p(float(row["throughput_demand"])) / log1p(80.0), 1.0
    )
    complexity_score = min(
        log1p(float(row["complexity_index"])) / log1p(120.0), 1.0
    )
    bottleneck_score = min(
        log1p(float(row["bottleneck_index"])) / log1p(4000.0), 1.0
    )

    pressure_signal = (
        0.35 * workload_score
        + 0.25 * throughput_score
        + 0.20 * complexity_score
        + 0.20 * bottleneck_score
    )

    if machine_available <= 1.0:
        pressure_signal += 0.08

    pressure_signal = max(0.0, min(1.0, pressure_signal))
    probability_floor = pressure_signal * 0.55

    # Lightweight business coherence rule for extreme overload scenarios.
    if (
        quantity_order >= 1000.0
        and machine_available <= 1.0
        and (bom_depth >= 3.0 or total_operations >= 16.0)
    ):
        quantity_growth = min((quantity_order - 1000.0) / 1500.0, 1.0)
        probability_floor = max(probability_floor, 0.70 + (0.20 * quantity_growth))
    elif quantity_order >= 700.0 and machine_available <= 1.0:
        probability_floor = max(probability_floor, 0.58)
    elif quantity_order >= 1500.0 and machine_available <= 2.0:
        probability_floor = max(probability_floor, 0.62)

    return max(0.0, min(0.95, probability_floor))


def _compute_coherence_probability(features: pd.DataFrame) -> float:
    row = features.iloc[0]

    workload_score = min(log1p(float(row["workload"])) / log1p(2500.0), 1.0)
    throughput_score = min(
        log1p(float(row["throughput_demand"])) / log1p(80.0), 1.0
    )
    complexity_score = min(
        log1p(float(row["complexity_index"])) / log1p(120.0), 1.0
    )
    bottleneck_score = min(
        log1p(float(row["bottleneck_index"])) / log1p(4000.0), 1.0
    )

    pressure_signal = (
        0.35 * workload_score
        + 0.25 * throughput_score
        + 0.20 * complexity_score
        + 0.20 * bottleneck_score
    )
    coherence_probability = (pressure_signal - 0.45) / 0.45
    return max(0.0, min(1.0, coherence_probability))


def predict_delay(
    model: Any, request: DelayPredictionRequest
) -> Tuple[Optional[dict[str, Any]], Optional[str]]:
    features = _build_features(request)
    expected_columns = _extract_expected_feature_columns(model)

    missing_columns = [column for column in expected_columns if column not in features.columns]
    if missing_columns:
        missing_text = ", ".join(sorted(missing_columns))
        return None, f"Feature mismatch with trained model. Missing columns: {missing_text}"

    features = features[expected_columns]

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

        coherence_probability = _compute_coherence_probability(features)
        probability = (0.55 * probability) + (0.45 * coherence_probability)
        probability = max(probability, _compute_pressure_floor(features))
        probability = max(0.0, min(1.0, probability))
        message = _resolve_risk_message(probability)
        return {
            "delay_probability": probability,
            "message": message,
        }, None
    except Exception as exc:  # pragma: no cover - defensive fallback
        return None, f"Prediction failed: {exc}"
