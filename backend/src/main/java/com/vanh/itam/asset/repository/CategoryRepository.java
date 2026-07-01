package com.vanh.itam.asset.repository;

import com.vanh.itam.asset.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    @Query("SELECT c FROM Category c WHERE c.deletedAt IS NULL ORDER BY c.name")
    List<Category> findAllActive();

    @Query("SELECT c FROM Category c WHERE c.id = :id AND c.deletedAt IS NULL")
    Optional<Category> findActiveById(@Param("id") Long id);

    boolean existsByCode(String code);

    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Category c WHERE c.code = :code AND c.id != :excludeId")
    boolean existsByCodeAndIdNot(@Param("code") String code, @Param("excludeId") Long excludeId);
}
