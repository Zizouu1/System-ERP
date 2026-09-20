package com.example.backend.modules.admin.productiondelay.service;

import com.example.backend.modules.admin.productiondelay.dto.ProductionDelayPredictionRequest;
import com.example.backend.modules.admin.productiondelay.dto.ProductionDelayPredictionResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ProductionDelayPredictionServiceTest {

    private ProductionDelayPredictionService service;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        ObjectMapper objectMapper = new ObjectMapper();

        service = new ProductionDelayPredictionService(restTemplate, objectMapper);
        ReflectionTestUtils.setField(service, "aiServiceBaseUrl", "http://localhost:8001");
        server = MockRestServiceServer.bindTo(restTemplate).build();
    }

    @Test
    void shouldMapNewAiResponseSchema() {
        server.expect(requestTo("http://localhost:8001/predict-delay"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json(
                        "{\"duration\":8,\"quantity_order\":300,\"machine_available\":2,"
                                + "\"bom_depth\":3,\"total_operations\":4,\"total_bom_components\":16}",
                        true
                ))
                .andRespond(withSuccess(
                        "{\"delay_probability\":0.82,"
                                + "\"message\":\"Le risque de retard de production est eleve.\"}",
                        MediaType.APPLICATION_JSON
                ));

        ProductionDelayPredictionRequest request = new ProductionDelayPredictionRequest(
                8, 300, 2, 3, 4, 16
        );

        ProductionDelayPredictionResponse response = service.predictDelay(request);
        server.verify();

        assertEquals(0.82, response.getDelayProbability(), 1e-9);
        assertEquals("Le risque de retard de production est eleve.", response.getMessage());
        assertNull(response.getError());
    }

    @Test
    void shouldSupportLegacyPredictionField() {
        server.expect(requestTo("http://localhost:8001/predict-delay"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(
                        "{\"prediction\":0.22}",
                        MediaType.APPLICATION_JSON
                ));

        ProductionDelayPredictionRequest request = new ProductionDelayPredictionRequest(
                3, 120, 5, 2, 1, 8
        );

        ProductionDelayPredictionResponse response = service.predictDelay(request);
        server.verify();

        assertEquals(0.22, response.getDelayProbability(), 1e-9);
        assertEquals("Le risque de retard de production est faible.", response.getMessage());
        assertNull(response.getError());
    }
}
