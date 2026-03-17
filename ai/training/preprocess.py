import re
import unicodedata
from pathlib import Path
from typing import Tuple

import pandas as pd
from sklearn.compose import ColumnTransformer
from sklearn.impute import SimpleImputer
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import StandardScaler

try:
    from training.features import (
        RAW_FEATURE_COLUMNS,
        MODEL_FEATURE_COLUMNS,
        TARGET_COLUMN,
        build_model_features,
    )
except ModuleNotFoundError:
    from features import (  # type: ignore
        RAW_FEATURE_COLUMNS,
        MODEL_FEATURE_COLUMNS,
        TARGET_COLUMN,
        build_model_features,
    )


REQUIRED_COLUMNS = RAW_FEATURE_COLUMNS + [TARGET_COLUMN]

COLUMN_ALIASES = {
    "duration": "duration",
    "duree": "duration",
    "quantity_order": "quantity_order",
    "quantityordered": "quantity_order",
    "quantite_commandee": "quantity_order",
    "quantite_commande": "quantity_order",
    "machine_available": "machine_available",
    "machines_available": "machine_available",
    "machines_disponibles": "machine_available",
    "bom_depth": "bom_depth",
    "profondeur_bom": "bom_depth",
    "total_operations": "total_operations",
    "total_operation": "total_operations",
    "operations_total": "total_operations",
    "nombre_total_operations": "total_operations",
    "total_bom_components": "total_bom_components",
    "bom_total_components": "total_bom_components",
    "nombre_total_composants": "total_bom_components",
    "is_delayed": "is_delayed",
    "delay": "is_delayed",
    "retard": "is_delayed",
    "retard_production": "is_delayed",
}

TARGET_TRUE_VALUES = {"1", "true", "yes", "y", "oui", "retard", "delayed"}
TARGET_FALSE_VALUES = {"0", "false", "no", "n", "non", "on_time", "a_l_heure"}


def _normalize_column_name(name: str) -> str:
    normalized = (
        unicodedata.normalize("NFKD", str(name))
        .encode("ascii", "ignore")
        .decode("ascii")
    )
    normalized = normalized.strip().lower()
    normalized = re.sub(r"[^a-z0-9]+", "_", normalized).strip("_")
    return normalized


def _canonicalize_columns(df: pd.DataFrame) -> pd.DataFrame:
    rename_map: dict[str, str] = {}
    matched_columns: dict[str, str] = {}

    for original_name in df.columns:
        normalized_name = _normalize_column_name(original_name)
        canonical_name = COLUMN_ALIASES.get(normalized_name, normalized_name)

        if canonical_name in REQUIRED_COLUMNS:
            previous_match = matched_columns.get(canonical_name)
            if previous_match and previous_match != original_name:
                raise ValueError(
                    "Duplicate columns detected for "
                    f"'{canonical_name}': '{previous_match}' and '{original_name}'."
                )
            matched_columns[canonical_name] = original_name
            rename_map[original_name] = canonical_name

    return df.rename(columns=rename_map)


def _validate_and_convert_numeric_column(df: pd.DataFrame, column: str) -> pd.Series:
    converted = pd.to_numeric(df[column], errors="coerce")
    invalid_mask = converted.isna() & df[column].notna()

    if invalid_mask.any():
        invalid_samples = (
            df.loc[invalid_mask, column].astype(str).head(5).tolist()
        )
        raise ValueError(
            f"Column '{column}' contains non-numeric values: {invalid_samples}"
        )

    return converted


def _normalize_target_label(raw_value: object) -> str:
    normalized = (
        unicodedata.normalize("NFKD", str(raw_value))
        .encode("ascii", "ignore")
        .decode("ascii")
    )
    normalized = normalized.strip().lower()
    normalized = re.sub(r"[^a-z0-9]+", "_", normalized).strip("_")
    return normalized


def _convert_target_column(series: pd.Series) -> pd.Series:
    numeric_target = pd.to_numeric(series, errors="coerce")
    numeric_like_mask = numeric_target.notna()

    if numeric_like_mask.all():
        invalid_values = series[~numeric_target.isin([0, 1])]
        if not invalid_values.empty:
            sample_values = invalid_values.astype(str).head(5).tolist()
            raise ValueError(
                f"Target column '{TARGET_COLUMN}' must contain only 0/1 values. "
                f"Invalid samples: {sample_values}"
            )
        return numeric_target.astype(int)

    converted = series.map(
        lambda value: _map_target_value(value) if pd.notna(value) else None
    )
    if converted.isna().any():
        invalid_samples = series[converted.isna()].astype(str).head(5).tolist()
        raise ValueError(
            f"Target column '{TARGET_COLUMN}' contains unsupported values: "
            f"{invalid_samples}"
        )
    return converted.astype(int)


def _map_target_value(raw_value: object) -> int | None:
    normalized = _normalize_target_label(raw_value)
    if normalized in TARGET_TRUE_VALUES:
        return 1
    if normalized in TARGET_FALSE_VALUES:
        return 0
    return None


def load_dataset(dataset_path: Path) -> pd.DataFrame:
    if not dataset_path.exists():
        raise FileNotFoundError(
            "Dataset not found. Please add training data before running training."
        )

    return pd.read_csv(dataset_path, encoding="utf-8-sig")


def validate_dataset(df: pd.DataFrame) -> pd.DataFrame:
    canonical_df = _canonicalize_columns(df.copy())

    missing_columns = [
        column for column in REQUIRED_COLUMNS if column not in canonical_df.columns
    ]
    if missing_columns:
        missing_text = ", ".join(sorted(missing_columns))
        raise ValueError(f"Dataset is missing required columns: {missing_text}")

    for feature in RAW_FEATURE_COLUMNS:
        canonical_df[feature] = _validate_and_convert_numeric_column(
            canonical_df, feature
        )

        negative_values = canonical_df[feature] < 0
        if negative_values.any():
            sample_values = canonical_df.loc[negative_values, feature].head(5).tolist()
            raise ValueError(
                f"Column '{feature}' contains negative values: {sample_values}"
            )

    canonical_df[TARGET_COLUMN] = _convert_target_column(canonical_df[TARGET_COLUMN])
    if canonical_df[TARGET_COLUMN].isna().any():
        raise ValueError(f"Target column '{TARGET_COLUMN}' contains missing values.")

    return canonical_df


def prepare_features_and_target(df: pd.DataFrame) -> Tuple[pd.DataFrame, pd.Series]:
    validated_df = validate_dataset(df)
    features = build_model_features(validated_df)
    target = validated_df[TARGET_COLUMN].copy()
    return features, target


def build_preprocessing_pipeline() -> ColumnTransformer:
    numeric_pipeline = Pipeline(
        steps=[
            ("imputer", SimpleImputer(strategy="median")),
            ("scaler", StandardScaler()),
        ]
    )

    return ColumnTransformer(
        transformers=[("numeric", numeric_pipeline, MODEL_FEATURE_COLUMNS)],
        remainder="drop",
    )
