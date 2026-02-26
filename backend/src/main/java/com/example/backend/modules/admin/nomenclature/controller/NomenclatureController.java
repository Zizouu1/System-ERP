package com.example.backend.modules.admin.nomenclature.controller;

import com.example.backend.modules.admin.nomenclature.dto.NomenclatureRequest;
import com.example.backend.modules.admin.nomenclature.entity.Nomenclature;
import com.example.backend.modules.admin.nomenclature.service.NomenclatureService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@RestController
@RequestMapping("/api/nomenclatures")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class NomenclatureController {

    private final NomenclatureService nomenclatureService;

    @PostMapping
    public ResponseEntity<Nomenclature> createNomenclature(@RequestBody NomenclatureRequest request) {
        return ResponseEntity.ok(nomenclatureService.createNomenclature(request));
    }

    @GetMapping
    public ResponseEntity<List<Nomenclature>> getAllNomenclatures() {
        return ResponseEntity.ok(nomenclatureService.getAllNomenclatures());
    }

    @GetMapping("/{parentRef}")
    public ResponseEntity<List<Nomenclature>> getNomenclatureByParentRef(@PathVariable String parentRef) {
        return ResponseEntity.ok(nomenclatureService.getNomenclatureByParentRef(parentRef));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Nomenclature> updateNomenclature(@PathVariable Long id,
            @RequestBody NomenclatureRequest request) {
        return ResponseEntity.ok(nomenclatureService.updateNomenclature(id, request.getQuantityRequired()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNomenclature(@PathVariable Long id) {
        nomenclatureService.deleteNomenclature(id);
        return ResponseEntity.ok().build();
    }
}
