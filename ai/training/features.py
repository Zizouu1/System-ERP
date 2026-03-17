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
    "inverse_machine_capacity",
    "workload",
    "operations_per_machine",
    "components_per_operation",
    "pressure_index",
    "complexity_index",
    "throughput_demand",
    "bottleneck_index",
    "workload_complexity_index",
]

MODEL_FEATURE_COLUMNS: Final[list[str]] = (
    RAW_FEATURE_COLUMNS + ENGINEERED_FEATURE_COLUMNS
)

TARGET_COLUMN: Final[str] = "is_delayed"


def build_model_features(df: pd.DataFrame) -> pd.DataFrame:
    missing_columns = [column for column in RAW_FEATURE_COLUMNS if column not in df.columns]
    if missing_columns:
        missing_text = ", ".join(sorted(missing_columns))
        raise ValueError(f"Missing raw feature columns: {missing_text}")

    features = df[RAW_FEATURE_COLUMNS].copy()

    # Keep division stable while preserving strong pressure effects for low capacity.
    machine_capacity = features["machine_available"].where(
        features["machine_available"] > 0, 0.5
    )
    operation_count = features["total_operations"].where(
        features["total_operations"] > 0, 1.0
    )
    duration_span = features["duration"].where(features["duration"] > 0, 1.0)

    features["inverse_machine_capacity"] = 1.0 / machine_capacity
    features["workload"] = features["quantity_order"] / machine_capacity
    features["operations_per_machine"] = (
        features["total_operations"] / machine_capacity
    )
    features["components_per_operation"] = (
        features["total_bom_components"] / operation_count
    )
    features["pressure_index"] = features["duration"] * features["workload"]
    features["complexity_index"] = (
        features["bom_depth"] * features["components_per_operation"]
    )
    features["throughput_demand"] = (
        features["quantity_order"] / (machine_capacity * (duration_span + 1.0))
    )
    features["bottleneck_index"] = features["workload"] * (
        1.0 + (features["bom_depth"] / 4.0)
    )
    features["workload_complexity_index"] = features["workload"] * (
        1.0 + (features["complexity_index"] / 40.0)
    )

    return features[MODEL_FEATURE_COLUMNS]
