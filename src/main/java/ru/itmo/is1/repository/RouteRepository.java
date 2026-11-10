package ru.itmo.is1.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import ru.itmo.is1.model.Route;

public interface RouteRepository extends JpaRepository<Route, Integer>, JpaSpecificationExecutor<Route> {
    boolean existsByCoordinatesId(Integer coordinatesId);

    boolean existsByFromIdOrToId(Integer fromId, Integer toId);

    default boolean isLocationUsed(Integer locationId) {
        return existsByFromIdOrToId(locationId, locationId);
    }

    List<Route> findByFromIsNotNull();

    long countByRating(Float rating);

    List<Route> findByNameContainingOrderByIdAsc(String substring);

    Optional<Route> findFirstByFromIdAndToIdOrderByDistanceAsc(Integer fromId, Integer toId);

    Optional<Route> findFirstByFromIdAndToIdOrderByDistanceDesc(Integer fromId, Integer toId);
}
