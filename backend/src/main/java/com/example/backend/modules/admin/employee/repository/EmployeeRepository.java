package com.example.backend.modules.admin.employee.repository;

import com.example.backend.modules.admin.employee.entity.Employee;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    List<Employee> findAllByOrderByMatriculeAsc();

    Optional<Employee> findByMatriculeIgnoreCase(String matricule);

    boolean existsByMatriculeIgnoreCase(String matricule);

    boolean existsByMatriculeIgnoreCaseAndIdNot(String matricule, Long id);

    @Query("""
            select e from Employee e
            where (:query = ''
                   or lower(e.matricule) like concat('%', :query, '%')
                   or lower(e.nom) like concat('%', :query, '%')
                   or lower(e.prenom) like concat('%', :query, '%')
                   or lower(e.poste) like concat('%', :query, '%')
                   or lower(e.departement) like concat('%', :query, '%'))
            order by e.matricule asc
            """)
    List<Employee> searchForLookup(@Param("query") String query, Pageable pageable);
}
