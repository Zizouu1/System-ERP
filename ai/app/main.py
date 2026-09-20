from fastapi import FastAPI

from app.model_loader import load_metrics, load_model
from app.predictor import predict_delay
from app.schemas import DelayPredictionRequest, DelayPredictionResponse


app = FastAPI()

model = None
load_error = None
metrics = None

@app.on_event("startup")
def startup():
    global model, load_error, metrics
    model, load_error = load_model()
    metrics = load_metrics()


@app.get("/")
def read_root():
    return {"message": "Welcome to the AI Prediction API"}


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok"}


@app.get("/model-metrics")
def model_metrics() -> dict[str, float | None]:
    if not isinstance(metrics, dict):
        return {
            "accuracy": None,
            "precision": None,
            "recall": None,
            "f1": None,
            "roc_auc": None,
        }

    return {
        "accuracy": metrics.get("accuracy"),
        "precision": metrics.get("precision"),
        "recall": metrics.get("recall"),
        "f1": metrics.get("f1"),
        "roc_auc": metrics.get("roc_auc"),
    }


@app.post("/predict-delay", response_model=DelayPredictionResponse)
def predict_delay_endpoint(payload: DelayPredictionRequest) -> DelayPredictionResponse:

    if model is None:
        return DelayPredictionResponse(
            error="Le modele IA n'est pas encore entraine."
        )

    prediction_result, prediction_error = predict_delay(model, payload)
    if prediction_error:
        return DelayPredictionResponse(error=prediction_error)

    if isinstance(prediction_result, DelayPredictionResponse):
        return prediction_result

    if isinstance(prediction_result, dict):
        safe_result = {str(k): v for k, v in prediction_result.items()}
        if isinstance(metrics, dict):
            safe_result.update(metrics)
        return DelayPredictionResponse(**safe_result)

    return DelayPredictionResponse(error="Le format du résultat de la prédiction est invalide.")
