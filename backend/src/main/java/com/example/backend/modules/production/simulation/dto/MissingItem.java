package com.example.backend.modules.production.simulation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MissingItem {

    private String productReference;
    private Double required;
    private Double available;
    private Double missing;

}
