package com.example.backend.modules.production.departement1.dto;

import com.example.backend.modules.production.departement1.entity.PsfStock;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PsfIncomingRequest {
    private String reference;
    private double quantity;
    private String lotNumber;
    private PsfStock.ProductType productType;
}
