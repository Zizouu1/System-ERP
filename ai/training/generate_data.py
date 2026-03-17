from pathlib import Path
from typing import Optional

import pandas as pd


BASE_DIR = Path(__file__).resolve().parents[1]
DEFAULT_SOURCE_PATH = BASE_DIR / "data" / "datasets" / "production_delay_dataset.csv"
DEFAULT_OUTPUT_PATH = (
    BASE_DIR / "data" / "processed" / "production_delay_dataset_prepared.csv"
)

REQUIRED_COLUMNS = [
    "duration",
    "quantity_order",
    "machine_available",
    "bom_depth",
    "total_operations",
    "total_bom_components",
    "is_delayed",
]


def prepare_dataset(
    source_path: Path = DEFAULT_SOURCE_PATH, output_path: Path = DEFAULT_OUTPUT_PATH
) -> Optional[Path]:
    if not source_path.exists():
        print("Dataset not found. Please add training data before running training.")
        return None

    output_path.parent.mkdir(parents=True, exist_ok=True)

    df = pd.read_csv(source_path)
    df = df.dropna(how="all")

    missing_columns = [col for col in REQUIRED_COLUMNS if col not in df.columns]
    if missing_columns:
        print(
            "Dataset is missing required columns: " + ", ".join(sorted(missing_columns))
        )
        return None

    df.to_csv(output_path, index=False)

    print(f"Dataset prepared successfully: {output_path}")
    return output_path


if __name__ == "__main__":
    prepare_dataset()
