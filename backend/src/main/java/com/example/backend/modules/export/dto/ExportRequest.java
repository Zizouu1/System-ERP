package com.example.backend.modules.export.dto;

import lombok.Data;

@Data
public class ExportRequest {
    private String productRef;
    private Double quantity;
    private String reason;
}
