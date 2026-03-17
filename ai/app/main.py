from fastapi import FastAPI

from app.model_loader import load_model
from app.predictor import predict_delay
from app.schemas import DelayPredictionRequest, DelayPredictionResponse


app = FastAPI(title="Production Delay Prediction API")


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok"}


@app.post("/predict-delay", response_model=DelayPredictionResponse)
def predict_delay_endpoint(payload: DelayPredictionRequest) -> DelayPredictionResponse:
    model, _load_error = load_model()

    if model is None:
        return DelayPredictionResponse(
            error="Le modele IA n'est pas encore entraine."
        )

    prediction_result, prediction_error = predict_delay(model, payload)
    if prediction_error:
        return DelayPredictionResponse(error=prediction_error)

    return DelayPredictionResponse(**prediction_result)
