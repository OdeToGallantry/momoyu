package com.example.bookmark.dish;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DishRepository extends JpaRepository<Dish, Long> {

    long countByFavoriteTrue();

    @Query("select lower(d.name) from Dish d")
    List<String> findAllNamesLowercase();

    Page<Dish> findByFavoriteTrue(Pageable pageable);

    @Query("""
            SELECT d FROM Dish d
            WHERE (:favoriteOnly = false OR d.favorite = true)
              AND (
                    :q = ''
                    OR lower(d.name) LIKE lower(concat('%', :q, '%'))
                    OR lower(coalesce(d.tags, '')) LIKE lower(concat('%', :q, '%'))
                  )
            """)
    Page<Dish> search(
            @Param("q") String q,
            @Param("favoriteOnly") boolean favoriteOnly,
            Pageable pageable);
}
