package com.example.backend.modules.production.departement1.psfProduction.dto;

import com.example.backend.modules.production.departement1.psfProduction.entity.PsfProduction;
import lombok.Builder;

import java.util.List;

@Builder
public record PsfProductionUpdateResponse(
        PsfProduction production,
        List<PsfQrLabelDTO> qrLabels) {
}
