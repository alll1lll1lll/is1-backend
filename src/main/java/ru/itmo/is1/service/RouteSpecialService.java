package ru.itmo.is1.service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import ru.itmo.is1.dto.location.LocationRef;
import ru.itmo.is1.dto.route.PathMode;
import ru.itmo.is1.dto.route.RouteBetweenRequest;
import ru.itmo.is1.dto.route.RouteDto;
import ru.itmo.is1.dto.route.RouteRequest;
import ru.itmo.is1.mapper.DtoMapper;
import ru.itmo.is1.model.Location;
import ru.itmo.is1.model.Route;
import ru.itmo.is1.repository.RouteRepository;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RouteSpecialService {
    private static final Comparator<Route> BY_FROM_LOCATION =
            Comparator.comparing(Route::getFrom, Location.NATURAL_ORDER);

    private final RouteRepository routeRepository;
    private final RouteService routeService;
    private final LocationService locationService;

    public Optional<RouteDto> findWithMaxFrom() {
        return routeRepository.findByFromIsNotNull().stream()
                .max(BY_FROM_LOCATION)
                .map(DtoMapper::toDto);
    }

    public long countByRating(float rating) {
        return routeRepository.countByRating(rating);
    }

    public List<RouteDto> findByNameContaining(String substring) {
        return routeRepository.findByNameContainingOrderByIdAsc(substring).stream()
                .map(DtoMapper::toDto)
                .toList();
    }

    public Optional<RouteDto> findBetween(Integer fromId, Integer toId, PathMode mode) {
        locationService.getExisting(fromId);
        locationService.getExisting(toId);
        Optional<Route> route = switch (mode) {
            case SHORTEST -> routeRepository.findFirstByFromIdAndToIdOrderByDistanceAsc(fromId, toId);
            case LONGEST -> routeRepository.findFirstByFromIdAndToIdOrderByDistanceDesc(fromId, toId);
        };
        return route.map(DtoMapper::toDto);
    }

    @Transactional
    public RouteDto addBetween(RouteBetweenRequest request) {
        RouteRequest route = RouteRequest.builder()
                .name(request.getName())
                .coordinates(request.getCoordinates())
                .from(LocationRef.ofId(request.getFromId()))
                .to(LocationRef.ofId(request.getToId()))
                .distance(request.getDistance())
                .rating(request.getRating())
                .build();
        return routeService.create(route);
    }
}
