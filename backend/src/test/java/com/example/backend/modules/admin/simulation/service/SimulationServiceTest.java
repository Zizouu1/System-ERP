package com.example.backend.modules.admin.simulation.service;

import com.example.backend.modules.admin.nomenclature.entity.Nomenclature;
import com.example.backend.modules.admin.nomenclature.repository.NomenclatureRepository;
import com.example.backend.modules.admin.product.entity.ProductTypeEnum;
import com.example.backend.modules.admin.simulation.dto.SimulationRequest;
import com.example.backend.modules.admin.simulation.dto.SimulationResponse;
import com.example.backend.modules.production.productionstock.entity.GlobalStock;
import com.example.backend.modules.production.productionstock.repository.GlobalStockRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SimulationServiceTest {

    @Mock
    private GlobalStockRepository globalStockRepository;

    @Mock
    private NomenclatureRepository nomenclatureRepository;

    @InjectMocks
    private SimulationService simulationService;

    @Test
    void multiLevelBomShouldIncludeOnlySelectedProductComponents() {
        mockBom("A", List.of(
                bom(1L, "A", "B", 2.0),
                bom(2L, "A", "C", 1.0)
        ));
        mockBom("B", List.of(bom(3L, "B", "D", 3.0)));
        mockBom("C", Collections.emptyList());
        mockBom("D", Collections.emptyList());

        mockStock("C", 15.0);
        mockStock("D", 50.0);

        SimulationResponse response = simulationService.checkFeasibility(
                SimulationRequest.builder().reference("A").quantity(10.0).build()
        );

        Assertions.assertFalse(response.isPossible());
        Assertions.assertEquals(2, response.getRequiredItems().size());
        Assertions.assertEquals(1, response.getMissingItems().size());

        SimulationResponse.RequiredItem requiredC = findRequired(response, "C");
        SimulationResponse.RequiredItem requiredD = findRequired(response, "D");

        Assertions.assertEquals(10.0, requiredC.getRequiredQty());
        Assertions.assertEquals(15.0, requiredC.getAvailableQty());
        Assertions.assertEquals(0.0, requiredC.getMissingQty());

        Assertions.assertEquals(60.0, requiredD.getRequiredQty());
        Assertions.assertEquals(50.0, requiredD.getAvailableQty());
        Assertions.assertEquals(10.0, requiredD.getMissingQty());

        Assertions.assertTrue(response.getMissingItems().stream().noneMatch(i -> "Y".equals(i.getReference())));
    }

    @Test
    void shouldBeFeasibleWhenAllRequiredComponentsAreAvailable() {
        mockBom("A", List.of(
                bom(1L, "A", "B", 2.0),
                bom(2L, "A", "C", 1.0)
        ));
        mockBom("B", List.of(bom(3L, "B", "D", 3.0)));
        mockBom("C", Collections.emptyList());
        mockBom("D", Collections.emptyList());

        mockStock("C", 10.0);
        mockStock("D", 80.0);

        SimulationResponse response = simulationService.checkFeasibility(
                SimulationRequest.builder().reference("A").quantity(10.0).build()
        );

        Assertions.assertTrue(response.isPossible());
        Assertions.assertTrue(response.getMissingItems().isEmpty());
        Assertions.assertEquals(2, response.getRequiredItems().size());
    }

    @Test
    void shouldAggregateSharedLeafRequirementsAcrossBranches() {
        mockBom("A", List.of(
                bom(1L, "A", "B", 1.0),
                bom(2L, "A", "C", 1.0)
        ));
        mockBom("B", List.of(bom(3L, "B", "D", 2.0)));
        mockBom("C", List.of(bom(4L, "C", "D", 4.0)));
        mockBom("D", Collections.emptyList());

        mockStock("D", 25.0);

        SimulationResponse response = simulationService.checkFeasibility(
                SimulationRequest.builder().reference("A").quantity(5.0).build()
        );

        Assertions.assertFalse(response.isPossible());
        Assertions.assertEquals(1, response.getRequiredItems().size());

        SimulationResponse.RequiredItem requiredD = findRequired(response, "D");
        Assertions.assertEquals(30.0, requiredD.getRequiredQty());
        Assertions.assertEquals(25.0, requiredD.getAvailableQty());
        Assertions.assertEquals(5.0, requiredD.getMissingQty());
    }

    @Test
    void shouldReturnNotFeasibleWithMessageWhenBomCycleDetected() {
        mockBom("A", List.of(bom(1L, "A", "B", 1.0)));
        mockBom("B", List.of(bom(2L, "B", "A", 1.0)));

        SimulationResponse response = simulationService.checkFeasibility(
                SimulationRequest.builder().reference("A").quantity(1.0).build()
        );

        Assertions.assertFalse(response.isPossible());
        Assertions.assertTrue(response.getMessage().contains("Cyclic BOM detected"));
        Assertions.assertTrue(response.getMissingItems().isEmpty());
        Assertions.assertTrue(response.getRequiredItems().isEmpty());
    }

    @Test
    void shouldRejectNonPositiveQuantity() {
        RuntimeException ex = Assertions.assertThrows(RuntimeException.class,
                () -> simulationService.checkFeasibility(
                        SimulationRequest.builder().reference("A").quantity(0.0).build()
                ));

        Assertions.assertTrue(ex.getMessage().contains("greater than 0"));
    }

    private void mockBom(String parentRef, List<Nomenclature> children) {
        when(nomenclatureRepository.findByParentRef(parentRef)).thenReturn(children);
    }

    private void mockStock(String reference, double availableQty) {
        when(globalStockRepository.findByRef(reference)).thenReturn(Optional.of(stock(reference, availableQty)));
    }

    private Nomenclature bom(Long id, String parent, String child, Double qty) {
        return Nomenclature.builder()
                .id(id)
                .parentRef(parent)
                .componentRef(child)
                .quantityRequired(qty)
                .build();
    }

    private GlobalStock stock(String ref, double availableQty) {
        return GlobalStock.builder()
                .id(1L)
                .ref(ref)
                .type(ProductTypeEnum.MATIERE_PREMIERE)
                .quantityTotal(availableQty)
                .quantityUsed(0.0)
                .build();
    }

    private SimulationResponse.RequiredItem findRequired(SimulationResponse response, String reference) {
        return response.getRequiredItems().stream()
                .filter(item -> reference.equals(item.getReference()))
                .findFirst()
                .orElseThrow();
    }
}
