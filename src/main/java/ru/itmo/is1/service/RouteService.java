package ru.itmo.is1.service;

import static ru.itmo.is1.event.ChangeEvent.Action.CREATED;
import static ru.itmo.is1.event.ChangeEvent.Action.DELETED;
import static ru.itmo.is1.event.ChangeEvent.Action.UPDATED;
import static ru.itmo.is1.event.ChangeEvent.EntityType.ROUTE;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import ru.itmo.is1.dto.common.PageResponse;
import ru.itmo.is1.dto.route.RouteDto;
import ru.itmo.is1.dto.route.RouteRequest;
import ru.itmo.is1.event.ChangeNotifier;
import ru.itmo.is1.exception.ResourceNotFoundException;
import ru.itmo.is1.mapper.DtoMapper;
import ru.itmo.is1.model.Route;
import ru.itmo.is1.repository.RouteRepository;
import ru.itmo.is1.repository.RouteSpecifications;

@Service
@Transactional
@RequiredArgsConstructor
public class RouteService {
    private final RouteRepository routeRepository;
    private final CoordinatesService coordinatesService;
    private final LocationService locationService;
    private final ChangeNotifier notifier;

    @Transactional(readOnly = true)
    public PageResponse<RouteDto> findPage(String name, String fromName, String toName, Pageable pageable) {
        return PageResponse.of(routeRepository
                .findAll(RouteSpecifications.filter(name, fromName, toName), pageable)
                .map(DtoMapper::toDto));
    }

    @Transactional(readOnly = true)
    public RouteDto findById(Integer id) {
        return DtoMapper.toDto(getExisting(id));
    }

    public RouteDto create(RouteRequest request) {
        Route route = new Route();
        applyRequest(route, request);
        Route saved = routeRepository.saveAndFlush(route);
        notifier.notify(ROUTE, CREATED, saved.getId());
        return DtoMapper.toDto(saved);
    }

    public RouteDto update(Integer id, RouteRequest request) {
        Route route = getExisting(id);
        applyRequest(route, request);
        routeRepository.saveAndFlush(route);
        notifier.notify(ROUTE, UPDATED, id);
        return DtoMapper.toDto(route);
    }

    public void delete(Integer id) {
        routeRepository.delete(getExisting(id));
        notifier.notify(ROUTE, DELETED, id);
    }

    private Route getExisting(Integer id) {
        return routeRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.route(id));
    }

    private void applyRequest(Route route, RouteRequest request) {
        route.setName(request.getName().trim());
        route.setCoordinates(coordinatesService.resolve(request.getCoordinates()));
        route.setFrom(locationService.resolve(request.getFrom()));
        route.setTo(locationService.resolve(request.getTo()));
        route.setDistance(request.getDistance());
        route.setRating(request.getRating());
    }
}
