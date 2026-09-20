from __future__ import annotations

from typing import Final

import pandas as pd


RAW_FEATURE_COLUMNS: Final[list[str]] = [
    "duration",
    "quantity_order",
    "machine_available",
    "bom_depth",
    "total_operations",
    "total_bom_components",
]

ENGINEERED_FEATURE_COLUMNS: Final[list[str]] = [
    "workload",
    "complexity",
]

MODEL_FEATURE_COLUMNS: Final[list[str]] =  [
    "duration",
    "quantity_order",
    "machine_available",
    "bom_depth",
    "total_operations",
    "total_bom_components",
    "workload",
    "complexity",
]

TARGET_COLUMN: Final[str] = "is_delayed"


def build_model_features(df: pd.DataFrame) -> pd.DataFrame:

    missing_columns = [column for column in RAW_FEATURE_COLUMNS if column not in df.columns]
    if missing_columns:
        missing_text = ", ".join(sorted(missing_columns))
        raise ValueError(f"Colonnes de features manquantes : {missing_text}")

    features = df[RAW_FEATURE_COLUMNS].copy()


    # 1. Workload: ratio de la quantité commandée par machine disponible.
    features["workload"] = features["quantity_order"] / features["machine_available"]
    
    # 2. Complexity: interaction basique entre la profondeur de la nomenclature et le nombre de composants.
    features["complexity"] = features["bom_depth"] * features["total_bom_components"]

    return features[MODEL_FEATURE_COLUMNS]
