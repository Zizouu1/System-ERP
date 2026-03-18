package com.example.backend.modules.admin.nomenclature.controller;

import com.example.backend.modules.admin.nomenclature.dto.NomenclatureRequest;
import com.example.backend.modules.admin.nomenclature.entity.Nomenclature;
import com.example.backend.modules.admin.nomenclature.service.NomenclatureService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@RestController
@RequestMapping("/api/nomenclatures")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'PSF', 'LOGISTIC')")
public class NomenclatureController {

    private final NomenclatureService nomenclatureService;

    @PostMapping
    public ResponseEntity<Nomenclature> createNomenclature(@RequestBody NomenclatureRequest request) {
        return ResponseEntity.ok(nomenclatureService.createNomenclature(request));
    }

    @GetMapping
    public ResponseEntity<Page<Nomenclature>> getAllNomenclatures(Pageable pageable) {
        return ResponseEntity.ok(nomenclatureService.getAllNomenclatures(pageable));
    }

    @GetMapping("/{parentRef}")
    public ResponseEntity<List<Nomenclature>> getNomenclatureByParentRef(@PathVariable String parentRef) {
        return ResponseEntity.ok(nomenclatureService.getNomenclatureByParentRef(parentRef));
    }

    @GetMapping("/count")
    public ResponseEntity<Long> countAllNomenclatures() {
        return ResponseEntity.ok(nomenclatureService.countAllNomenclatures());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Nomenclature> updateNomenclature(@PathVariable Long id,
            @RequestBody NomenclatureRequest request) {
        return ResponseEntity.ok(nomenclatureService.updateNomenclature(id, request.getQuantityRequired()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteNomenclature(@PathVariable Long id) {
        nomenclatureService.deleteNomenclature(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/unique-refs")
    public ResponseEntity<List<String>> getUniqueRefs() {
        return ResponseEntity.ok(nomenclatureService.getUniqueRefs());
    }

    @PostMapping("/import")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> importCSV(@RequestBody List<String[]> rows) {
        return ResponseEntity.ok(nomenclatureService.importCSV(rows));
    }
}
