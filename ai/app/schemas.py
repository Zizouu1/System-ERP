from typing import Optional

from pydantic import BaseModel, Field


class DelayPredictionRequest(BaseModel):
    duration: int = Field(...)
    quantity_order: int = Field(...)
    machine_available: int = Field(...)
    bom_depth: int = Field(...)
    total_operations: int = Field(...)
    total_bom_components: int = Field(...)


class DelayPredictionResponse(BaseModel):
    delay_probability: Optional[float] = Field(default=None)
    message: Optional[str] = Field(default=None)
    error: Optional[str] = Field(default=None)
    accuracy: Optional[float] = Field(default=None)
    precision: Optional[float] = Field(default=None)
    recall: Optional[float] = Field(default=None)
    f1: Optional[float] = Field(default=None)
    roc_auc: Optional[float] = Field(default=None)
