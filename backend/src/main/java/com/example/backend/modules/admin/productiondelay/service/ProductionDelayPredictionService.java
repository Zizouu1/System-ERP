package com.example.backend.modules.admin.productiondelay.service;

import com.example.backend.modules.admin.productiondelay.dto.ModelMetricsResponse;
import com.example.backend.modules.admin.productiondelay.dto.ProductionDelayPredictionRequest;
import com.example.backend.modules.admin.productiondelay.dto.ProductionDelayPredictionResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProductionDelayPredictionService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${ai.service.base-url:http://localhost:8000}")
    private String aiServiceBaseUrl;

    public ProductionDelayPredictionResponse predictDelay(ProductionDelayPredictionRequest request) {
        if (request.getDuree() == null
                || request.getQuantiteCommandee() == null
                || request.getMachinesDisponibles() == null
                || request.getBomDepth() == null
                || request.getTotalOperations() == null
                || request.getTotalBomComponents() == null) {
            return error("Tous les champs numeriques sont obligatoires.");
        }

        Map<String, Integer> payload = new HashMap<>();
        payload.put("duration", request.getDuree());
        payload.put("quantity_order", request.getQuantiteCommandee());
        payload.put("machine_available", request.getMachinesDisponibles());
        payload.put("bom_depth", request.getBomDepth());
        payload.put("total_operations", request.getTotalOperations());
        payload.put("total_bom_components", request.getTotalBomComponents());

        try {
            ResponseEntity<Map> aiResponse = restTemplate.postForEntity(
                    resolvePredictUrl(),
                    payload,
                    Map.class
            );

            return parseAiResponse(aiResponse.getBody());
        } catch (RestClientResponseException ex) {
            String extractedError = extractErrorFromResponseBody(ex.getResponseBodyAsString());
            if (extractedError != null) {
                return error(extractedError);
            }
            return error("Le service IA a retourne une erreur: " + ex.getStatusCode().value());
        } catch (RestClientException ex) {
            return error("Service IA indisponible. Verifiez que le module IA est demarre.");
        }
    }

    public ModelMetricsResponse fetchModelMetrics() {
        try {
            ResponseEntity<Map> aiResponse = restTemplate.getForEntity(
                    resolveModelMetricsUrl(),
                    Map.class
            );

            return parseMetricsResponse(aiResponse.getBody());
        } catch (RestClientResponseException ex) {
            return emptyMetrics();
        } catch (RestClientException ex) {
            return emptyMetrics();
        }
    }

    private String resolvePredictUrl() {
        if (aiServiceBaseUrl.endsWith("/")) {
            return aiServiceBaseUrl + "predict-delay";
        }
        return aiServiceBaseUrl + "/predict-delay";
    }

    private String resolveModelMetricsUrl() {
        if (aiServiceBaseUrl.endsWith("/")) {
            return aiServiceBaseUrl + "model-metrics";
        }
        return aiServiceBaseUrl + "/model-metrics";
    }

    private ProductionDelayPredictionResponse parseAiResponse(Map<?, ?> body) {
        if (body == null) {
            return error("Reponse vide du service IA.");
        }

        Object errorValue = body.get("error");
        if (errorValue != null) {
            return error(String.valueOf(errorValue));
        }

        Object predictionValue = body.get("delay_probability");
        if (predictionValue == null) {
            // Backward compatibility with older AI response format.
            predictionValue = body.get("prediction");
        }

        if (predictionValue instanceof Number number) {
            String message = asText(body.get("message"));
            if (message == null) {
                message = inferMessage(number.doubleValue());
            }

            Map<?, ?> metrics = extractMetrics(body);
            Double accuracy = getMetric(body, metrics, "accuracy");
            Double precision = getMetric(body, metrics, "precision");
            Double recall = getMetric(body, metrics, "recall");
            Double f1 = getMetric(body, metrics, "f1");
            Double rocAuc = getMetric(body, metrics, "roc_auc");
            if (rocAuc == null) {
                rocAuc = getMetric(body, metrics, "rocAuc");
            }

            return ProductionDelayPredictionResponse.builder()
                    .delayProbability(number.doubleValue())
                    .message(message)
                    .accuracy(accuracy)
                    .precision(precision)
                    .recall(recall)
                    .f1(f1)
                    .rocAuc(rocAuc)
                    .build();
        }

        String fallbackMessage = asText(body.get("message"));
        if (fallbackMessage != null) {
            return error(fallbackMessage);
        }

        return error("Reponse invalide du service IA.");
    }

    private String extractErrorFromResponseBody(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return null;
        }

        try {
            JsonNode json = objectMapper.readTree(responseBody);
            if (json.hasNonNull("error")) {
                return json.get("error").asText();
            }
            if (json.hasNonNull("message")) {
                return json.get("message").asText();
            }
        } catch (Exception ignored) {
            return responseBody;
        }

        return null;
    }

    private ProductionDelayPredictionResponse error(String message) {
        return ProductionDelayPredictionResponse.builder()
                .error(message)
                .build();
    }

    private ModelMetricsResponse parseMetricsResponse(Map<?, ?> body) {
        if (body == null) {
            return emptyMetrics();
        }

        Double accuracy = toDouble(body.get("accuracy"));
        Double precision = toDouble(body.get("precision"));
        Double recall = toDouble(body.get("recall"));
        Double f1 = toDouble(body.get("f1"));
        Double rocAuc = toDouble(body.get("roc_auc"));
        if (rocAuc == null) {
            rocAuc = toDouble(body.get("rocAuc"));
        }

        return ModelMetricsResponse.builder()
                .accuracy(accuracy)
                .precision(precision)
                .recall(recall)
                .f1(f1)
                .rocAuc(rocAuc)
                .build();
    }

    private ModelMetricsResponse emptyMetrics() {
        return ModelMetricsResponse.builder()
                .accuracy(null)
                .precision(null)
                .recall(null)
                .f1(null)
                .rocAuc(null)
                .build();
    }

    private String asText(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    private String inferMessage(double probability) {
        if (probability < 0.30) {
            return "Le risque de retard de production est faible.";
        }
        if (probability < 0.70) {
            return "Le risque de retard de production est moyen.";
        }
        return "Le risque de retard de production est eleve.";
    }

    private Map<?, ?> extractMetrics(Map<?, ?> body) {
        Object metricsValue = body.get("metrics");
        if (metricsValue instanceof Map<?, ?> metricsMap) {
            return metricsMap;
        }
        return null;
    }

    private Double getMetric(Map<?, ?> body, Map<?, ?> metrics, String key) {
        Double fromBody = toDouble(body.get(key));
        if (fromBody != null) {
            return fromBody;
        }
        if (metrics != null) {
            return toDouble(metrics.get(key));
        }
        return null;
    }

    private Double toDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String text) {
            try {
                return Double.parseDouble(text);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}
