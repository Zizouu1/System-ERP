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
                        "{\"duration\":8.0,\"quantity_order\":300.0,\"machine_available\":2.0,"
                                + "\"bom_depth\":3.0,\"total_operations\":4.0,\"total_bom_components\":16.0}",
                        true
                ))
                .andRespond(withSuccess(
                        "{\"delay_probability\":0.82,"
                                + "\"message\":\"Le risque de retard de production est eleve.\"}",
                        MediaType.APPLICATION_JSON
                ));

        ProductionDelayPredictionRequest request = new ProductionDelayPredictionRequest(
                8.0, 300.0, 2.0, 3.0, 4.0, 16.0
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
                3.0, 120.0, 5.0, 2.0, 1.0, 8.0
        );

        ProductionDelayPredictionResponse response = service.predictDelay(request);
        server.verify();

        assertEquals(0.22, response.getDelayProbability(), 1e-9);
        assertEquals("Le risque de retard de production est faible.", response.getMessage());
        assertNull(response.getError());
    }
}
