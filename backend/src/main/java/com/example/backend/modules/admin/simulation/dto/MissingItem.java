package com.example.backend.modules.admin.simulation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MissingItem {

    private String reference;
    private Double required;
    private Double available;
    private Double missing;

}
