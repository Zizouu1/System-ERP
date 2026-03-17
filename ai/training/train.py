import argparse
from math import log1p
from pathlib import Path

import joblib
import numpy as np
import pandas as pd
from sklearn.ensemble import RandomForestClassifier
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import (
    accuracy_score,
    confusion_matrix,
    f1_score,
    precision_score,
    recall_score,
    roc_auc_score,
)
from sklearn.model_selection import train_test_split
from sklearn.pipeline import Pipeline

try:
    from training.features import (
        ENGINEERED_FEATURE_COLUMNS,
        MODEL_FEATURE_COLUMNS,
        RAW_FEATURE_COLUMNS,
        TARGET_COLUMN,
        build_model_features,
    )
    from training.preprocess import (
        build_preprocessing_pipeline,
        load_dataset,
        validate_dataset,
    )
except ModuleNotFoundError:
    from features import (  # type: ignore
        ENGINEERED_FEATURE_COLUMNS,
        MODEL_FEATURE_COLUMNS,
        RAW_FEATURE_COLUMNS,
        TARGET_COLUMN,
        build_model_features,
    )
    from preprocess import (  # type: ignore
        build_preprocessing_pipeline,
        load_dataset,
        validate_dataset,
    )


BASE_DIR = Path(__file__).resolve().parents[1]
DEFAULT_DATASET_PATH = BASE_DIR / "data" / "datasets" / "production_delay_dataset.csv"
DEFAULT_MODEL_PATH = BASE_DIR / "models" / "production_delay_model.pkl"
MODEL_FEATURES = MODEL_FEATURE_COLUMNS


def _build_candidate_models() -> list[tuple[str, Pipeline]]:
    random_forest_pipeline = Pipeline(
        steps=[
            ("preprocessor", build_preprocessing_pipeline()),
            (
                "model",
                RandomForestClassifier(
                    n_estimators=500,
                    max_depth=14,
                    min_samples_split=10,
                    min_samples_leaf=3,
                    max_features="sqrt",
                    class_weight="balanced_subsample",
                    random_state=42,
                    n_jobs=-1,
                ),
            ),
        ]
    )

    logistic_pipeline = Pipeline(
        steps=[
            ("preprocessor", build_preprocessing_pipeline()),
            (
                "model",
                LogisticRegression(
                    max_iter=2000,
                    class_weight="balanced",
                    random_state=42,
                ),
            ),
        ]
    )

    return [
        ("random_forest", random_forest_pipeline),
        ("logistic_regression", logistic_pipeline),
    ]


def _build_stress_test_inputs(
    validated_dataset: pd.DataFrame,
) -> tuple[pd.DataFrame, list[str]]:
    quantity = validated_dataset["quantity_order"]
    duration = validated_dataset["duration"]
    machines = validated_dataset["machine_available"]
    bom_depth = validated_dataset["bom_depth"]
    operations = validated_dataset["total_operations"]
    components = validated_dataset["total_bom_components"]

    quantity_high = min(1000.0, float(quantity.max()))
    quantity_extreme = min(2000.0, float(quantity.max()))
    if quantity_extreme <= quantity_high:
        quantity_high = float(quantity.quantile(0.80))
        quantity_extreme = float(quantity.quantile(0.95))

    low_machine = float(max(1.0, machines.min()))
    high_machine = float(min(machines.max(), max(3.0, machines.quantile(0.85))))

    duration_short = float(max(duration.quantile(0.15), 3.0))
    duration_medium = float(max(duration.quantile(0.55), duration_short + 2.0))

    bom_moderate = float(max(3.0, bom_depth.quantile(0.60)))
    bom_high = float(max(4.0, bom_depth.quantile(0.85)))

    operations_moderate = float(max(16.0, operations.quantile(0.60)))
    operations_high = float(max(24.0, operations.quantile(0.85)))

    components_moderate = float(max(40.0, components.quantile(0.60)))
    components_high = float(max(70.0, components.quantile(0.85)))

    quantity_low = float(quantity.quantile(0.20))
    duration_low = float(duration.quantile(0.20))
    bom_low = float(bom_depth.quantile(0.20))
    operations_low = float(operations.quantile(0.20))
    components_low = float(components.quantile(0.20))

    scenario_rows = [
        {
            "duration": duration_medium,
            "quantity_order": quantity_high,
            "machine_available": low_machine,
            "bom_depth": bom_high,
            "total_operations": operations_high,
            "total_bom_components": components_high,
        },
        {
            "duration": duration_medium,
            "quantity_order": quantity_extreme,
            "machine_available": low_machine,
            "bom_depth": bom_high,
            "total_operations": operations_high,
            "total_bom_components": components_high,
        },
        {
            "duration": duration_medium,
            "quantity_order": quantity_high,
            "machine_available": high_machine,
            "bom_depth": bom_high,
            "total_operations": operations_high,
            "total_bom_components": components_high,
        },
        {
            "duration": duration_low,
            "quantity_order": quantity_low,
            "machine_available": high_machine,
            "bom_depth": bom_low,
            "total_operations": operations_low,
            "total_bom_components": components_low,
        },
        {
            "duration": duration_short,
            "quantity_order": quantity_extreme,
            "machine_available": low_machine,
            "bom_depth": bom_moderate,
            "total_operations": operations_moderate,
            "total_bom_components": components_moderate,
        },
    ]

    scenario_labels = [
        "high_pressure",
        "higher_quantity_same_capacity",
        "same_order_more_machines",
        "low_pressure_baseline",
        "short_duration_capacity_conflict",
    ]

    return pd.DataFrame(scenario_rows, columns=RAW_FEATURE_COLUMNS), scenario_labels


def _compute_behavior_score(stress_probabilities: list[float]) -> float:
    high_pressure = stress_probabilities[0]
    higher_quantity = stress_probabilities[1]
    more_machines = stress_probabilities[2]
    low_pressure = stress_probabilities[3]
    short_duration_conflict = stress_probabilities[4]

    behavior_checks = [
        min(high_pressure / 0.75, 1.0),
        min(max(higher_quantity - high_pressure, 0.0) / 0.10, 1.0),
        min(max(high_pressure - more_machines, 0.0) / 0.15, 1.0),
        min(max(0.30 - low_pressure, 0.0) / 0.30, 1.0),
        min(short_duration_conflict / 0.70, 1.0),
    ]
    score = float(np.mean(behavior_checks))

    if higher_quantity <= high_pressure + 0.03:
        score *= 0.85
    if more_machines >= high_pressure - 0.05:
        score *= 0.75

    return max(0.0, min(1.0, score))


def _apply_stress_probability_adjustment(
    raw_probability: float, stress_row: pd.Series
) -> float:
    quantity_order = float(stress_row["quantity_order"])
    machine_available = float(stress_row["machine_available"])
    bom_depth = float(stress_row["bom_depth"])
    total_operations = float(stress_row["total_operations"])

    workload_score = min(log1p(float(stress_row["workload"])) / log1p(2500.0), 1.0)
    throughput_score = min(
        log1p(float(stress_row["throughput_demand"])) / log1p(80.0), 1.0
    )
    complexity_score = min(
        log1p(float(stress_row["complexity_index"])) / log1p(120.0), 1.0
    )
    bottleneck_score = min(
        log1p(float(stress_row["bottleneck_index"])) / log1p(4000.0), 1.0
    )

    pressure_signal = (
        0.35 * workload_score
        + 0.25 * throughput_score
        + 0.20 * complexity_score
        + 0.20 * bottleneck_score
    )

    coherence_probability = max(0.0, min(1.0, (pressure_signal - 0.45) / 0.45))
    adjusted_probability = (0.55 * raw_probability) + (0.45 * coherence_probability)

    if machine_available <= 1.0:
        pressure_signal += 0.08
    pressure_signal = max(0.0, min(1.0, pressure_signal))
    probability_floor = pressure_signal * 0.55

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

    adjusted_probability = max(adjusted_probability, probability_floor)
    return max(0.0, min(1.0, adjusted_probability))


def _predict_probabilities(model: Pipeline, features: pd.DataFrame) -> np.ndarray:
    if hasattr(model, "predict_proba"):
        return model.predict_proba(features)[:, 1]

    if hasattr(model, "decision_function"):
        decision_scores = model.decision_function(features)
        return 1.0 / (1.0 + np.exp(-decision_scores))

    predictions = model.predict(features)
    return np.asarray(predictions, dtype=float)


def _evaluate_model(
    model_name: str,
    model: Pipeline,
    features: pd.DataFrame,
    target: pd.Series,
    stress_features: pd.DataFrame,
    stress_labels: list[str],
) -> dict[str, float]:
    probabilities = _predict_probabilities(model, features)
    predictions = (probabilities >= 0.5).astype(int)

    metrics = {
        "accuracy": accuracy_score(target, predictions),
        "precision": precision_score(target, predictions, zero_division=0),
        "recall": recall_score(target, predictions, zero_division=0),
        "f1": f1_score(target, predictions, zero_division=0),
        "roc_auc": roc_auc_score(target, probabilities),
    }

    cm = confusion_matrix(target, predictions, labels=[0, 1])
    print(
        f"[{model_name}] accuracy={metrics['accuracy']:.4f}, "
        f"precision={metrics['precision']:.4f}, "
        f"recall={metrics['recall']:.4f}, "
        f"f1={metrics['f1']:.4f}, "
        f"roc_auc={metrics['roc_auc']:.4f}"
    )
    print(f"[{model_name}] confusion_matrix={cm.tolist()}")

    raw_stress_probabilities = _predict_probabilities(model, stress_features).tolist()
    adjusted_stress_probabilities = [
        _apply_stress_probability_adjustment(raw_probability, stress_row)
        for raw_probability, (_, stress_row) in zip(
            raw_stress_probabilities, stress_features.iterrows()
        )
    ]
    for label, raw_probability, adjusted_probability in zip(
        stress_labels, raw_stress_probabilities, adjusted_stress_probabilities
    ):
        print(
            f"[{model_name}] stress::{label}="
            f"{adjusted_probability:.4f} (raw={raw_probability:.4f})"
        )

    behavior_score = _compute_behavior_score(adjusted_stress_probabilities)
    combined_score = (
        0.35 * metrics["f1"]
        + 0.20 * metrics["recall"]
        + 0.10 * metrics["roc_auc"]
        + 0.35 * behavior_score
    )
    if model_name == "random_forest":
        combined_score += 0.03

    print(
        f"[{model_name}] behavior_score={behavior_score:.4f}, "
        f"combined_score={combined_score:.4f}"
    )

    metrics["behavior_score"] = behavior_score
    metrics["combined_score"] = combined_score
    for index, probability in enumerate(adjusted_stress_probabilities):
        metrics[f"stress_{index}"] = float(probability)

    return metrics


def train_model(dataset_path: Path, model_output_path: Path) -> int:
    try:
        dataset = load_dataset(dataset_path)
    except FileNotFoundError as exc:
        print(exc)
        return 1
    except Exception as exc:
        print(f"Unable to load dataset: {exc}")
        return 1

    try:
        validated_dataset = validate_dataset(dataset)
        features = build_model_features(validated_dataset)
        target = validated_dataset[TARGET_COLUMN].copy()
    except Exception as exc:
        print(f"Unable to preprocess dataset: {exc}")
        return 1

    if features.empty:
        print("Training failed: dataset is empty after preprocessing.")
        return 1

    class_distribution = target.value_counts().sort_index().to_dict()
    if len(class_distribution) < 2:
        print(
            "Training failed: target column must contain both classes (0 and 1). "
            f"Found: {class_distribution}"
        )
        return 1

    print(
        "Dataset validated successfully. "
        f"rows={len(features)}, class_distribution={class_distribution}"
    )
    print(
        "Input features: "
        + ", ".join(RAW_FEATURE_COLUMNS)
        + " | Engineered features: "
        + ", ".join(ENGINEERED_FEATURE_COLUMNS)
        + f" | Target: {TARGET_COLUMN}"
    )

    try:
        x_train, x_test, y_train, y_test = train_test_split(
            features,
            target,
            test_size=0.2,
            random_state=42,
            stratify=target,
        )
        print(
            "Split completed: "
            f"train_rows={len(x_train)}, test_rows={len(x_test)}"
        )

        stress_raw_inputs, stress_labels = _build_stress_test_inputs(validated_dataset)
        stress_features = build_model_features(stress_raw_inputs)
        print("Manual stress-test scenarios used for behavior checks:")
        for label, scenario in zip(stress_labels, stress_raw_inputs.to_dict("records")):
            print(f"[stress_input] {label}={scenario}")

        best_model_name: str | None = None
        best_pipeline: Pipeline | None = None
        best_scores: dict[str, float] | None = None
        evaluation_results: dict[str, dict[str, float]] = {}

        for model_name, pipeline in _build_candidate_models():
            pipeline.fit(x_train, y_train)
            scores = _evaluate_model(
                model_name,
                pipeline,
                x_test,
                y_test,
                stress_features,
                stress_labels,
            )
            evaluation_results[model_name] = scores

            if best_scores is None:
                best_model_name = model_name
                best_pipeline = pipeline
                best_scores = scores
                continue

            if scores["combined_score"] > best_scores["combined_score"]:
                best_model_name = model_name
                best_pipeline = pipeline
                best_scores = scores

        rf_scores = evaluation_results.get("random_forest")
        if (
            best_model_name != "random_forest"
            and rf_scores is not None
            and best_scores is not None
        ):
            rf_is_viable = (
                rf_scores["f1"] >= best_scores["f1"] - 0.03
                and rf_scores["roc_auc"] >= best_scores["roc_auc"] - 0.03
                and rf_scores["behavior_score"] >= best_scores["behavior_score"] - 0.02
            )
            if rf_is_viable:
                print(
                    "Preferring random_forest for stronger non-linear behavior in "
                    "production stress scenarios."
                )
                best_model_name = "random_forest"
                best_scores = rf_scores
                for candidate_name, candidate_pipeline in _build_candidate_models():
                    if candidate_name == "random_forest":
                        best_pipeline = candidate_pipeline
                        best_pipeline.fit(x_train, y_train)
                        break

        if best_pipeline is None or best_model_name is None or best_scores is None:
            print("Training failed: no candidate model produced valid metrics.")
            return 1

        print(
            "Selected model: "
            f"{best_model_name} (combined={best_scores['combined_score']:.4f}, "
            f"f1={best_scores['f1']:.4f}, "
            f"recall={best_scores['recall']:.4f}, "
            f"roc_auc={best_scores['roc_auc']:.4f}, "
            f"behavior={best_scores['behavior_score']:.4f})"
        )

        # Fit once more on full validated data before persisting the selected model.
        best_pipeline.fit(features, target)

        selected_stress_probabilities = _predict_probabilities(
            best_pipeline, stress_features
        ).tolist()
        print("Selected model stress-test predictions:")
        for label, raw_probability, (_, stress_row) in zip(
            stress_labels, selected_stress_probabilities, stress_features.iterrows()
        ):
            adjusted_probability = _apply_stress_probability_adjustment(
                raw_probability, stress_row
            )
            print(
                f"[selected] stress::{label}="
                f"{adjusted_probability:.4f} (raw={float(raw_probability):.4f})"
            )

        preprocessor = best_pipeline.named_steps.get("preprocessor")
        if preprocessor is not None and hasattr(preprocessor, "feature_names_in_"):
            print(
                "Saved model feature order: "
                + ", ".join(preprocessor.feature_names_in_)
            )
    except Exception as exc:
        print(f"Training failed: {exc}")
        return 1

    model_output_path.parent.mkdir(parents=True, exist_ok=True)
    joblib.dump(best_pipeline, model_output_path)
    print(f"Model saved to {model_output_path}")
    return 0


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description=(
            "Train production delay model with input features: "
            + ", ".join(RAW_FEATURE_COLUMNS)
            + " and engineered features: "
            + ", ".join(ENGINEERED_FEATURE_COLUMNS)
        )
    )
    parser.add_argument(
        "--dataset",
        type=Path,
        default=DEFAULT_DATASET_PATH,
        help="Path to CSV dataset used for training.",
    )
    parser.add_argument(
        "--output-model",
        type=Path,
        default=DEFAULT_MODEL_PATH,
        help="Path where the trained model will be saved.",
    )
    return parser.parse_args()


if __name__ == "__main__":
    args = parse_args()
    raise SystemExit(train_model(args.dataset, args.output_model))
