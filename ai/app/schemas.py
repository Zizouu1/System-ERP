from typing import Optional

from pydantic import BaseModel, Field


class DelayPredictionRequest(BaseModel):
    duration: float = Field(..., ge=0)
    quantity_order: float = Field(..., ge=0)
    machine_available: float = Field(..., ge=0)
    bom_depth: float = Field(..., ge=0)
    total_operations: float = Field(..., ge=0)
    total_bom_components: float = Field(..., ge=0)


class DelayPredictionResponse(BaseModel):
    delay_probability: Optional[float] = Field(default=None, ge=0, le=1)
    message: Optional[str] = None
    error: Optional[str] = None
