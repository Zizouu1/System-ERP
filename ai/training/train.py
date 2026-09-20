import argparse
import json
from pathlib import Path

import joblib
import pandas as pd
from sklearn.ensemble import RandomForestClassifier
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
DEFAULT_METRICS_PATH = BASE_DIR / "models" / "production_delay_metrics.json"


def _build_model() -> Pipeline:
    return Pipeline(
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


def _evaluate_model(
    model: Pipeline, features: pd.DataFrame, target: pd.Series
) -> dict[str, float]:
    probabilities = model.predict_proba(features)[:, 1]
    predictions = (probabilities >= 0.5).astype(int)

    accuracy  = float(accuracy_score(target, predictions))
    precision = float(precision_score(target, predictions, zero_division=0))
    recall    = float(recall_score(target, predictions, zero_division=0))
    f1        = float(f1_score(target, predictions, zero_division=0))
    roc_auc   = float(roc_auc_score(target, probabilities))
    cm        = confusion_matrix(target, predictions, labels=[0, 1])

    print(
        f"accuracy={accuracy:.4f}, "
        f"precision={precision:.4f}, "
        f"recall={recall:.4f}, "
        f"f1={f1:.4f}, "
        f"roc_auc={roc_auc:.4f}"
    )
    print(f"confusion_matrix={cm.tolist()}")

    return {
        "accuracy": accuracy,
        "precision": precision,
        "recall": recall,
        "f1": f1,
        "roc_auc": roc_auc,
    }


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
        print("Échec de l'entraînement : le dataset est vide après le prétraitement.")
        return 1

    class_distribution = target.value_counts().sort_index().to_dict()
    if len(class_distribution) < 2:
        print(
            "Échec de l'entraînement : la colonne cible doit contenir les classes 0 et 1. "
            f"Valeurs trouvées : {class_distribution}"
        )
        return 1

    print(
        f"Dataset validated: rows={len(features)}, "
        f"class_distribution={class_distribution}"
    )

    try:
        x_train, x_test, y_train, y_test = train_test_split(
            features,
            target,
            test_size=0.2,
            random_state=42,
            stratify=target,
        )
        print(f"Split: train={len(x_train)}, test={len(x_test)}")

        pipeline = _build_model()
        pipeline.fit(x_train, y_train)
        metrics = _evaluate_model(pipeline, x_test, y_test)

        # Retrain on full dataset before saving
        pipeline.fit(features, target)

    except Exception as exc:
        print(f"Échec de l'entraînement : {exc}")
        return 1

    model_output_path.parent.mkdir(parents=True, exist_ok=True)
    joblib.dump(pipeline, model_output_path)
    DEFAULT_METRICS_PATH.parent.mkdir(parents=True, exist_ok=True)
    DEFAULT_METRICS_PATH.write_text(json.dumps(metrics, indent=2), encoding="utf-8")
    print(f"Model saved to {model_output_path}")
    return 0


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Train production delay prediction model"
    )
    parser.add_argument(
        "--dataset",
        type=Path,
        default=DEFAULT_DATASET_PATH,
    )
    parser.add_argument(
        "--output-model",
        type=Path,
        default=DEFAULT_MODEL_PATH,
    )
    return parser.parse_args()


if __name__ == "__main__":
    args = parse_args()
    raise SystemExit(train_model(args.dataset, args.output_model))