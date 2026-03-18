package com.example.backend.modules.admin.nomenclature.repository;

import com.example.backend.modules.admin.nomenclature.entity.Nomenclature;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NomenclatureRepository extends JpaRepository<Nomenclature, Long> {
    List<Nomenclature> findByParentRef(String parentRef);

    java.util.Optional<Nomenclature> findByParentRefAndComponentRef(String parentRef, String componentRef);

    boolean existsByParentRef(String parentRef);

    boolean existsByComponentRef(String componentRef);

    @org.springframework.data.jpa.repository.Query(value = "SELECT DISTINCT ref FROM (SELECT parent_ref as ref FROM nomenclature UNION SELECT component_ref as ref FROM nomenclature) AS all_refs", nativeQuery = true)
    List<String> findUniqueRefs();
}
