package ru.itmo.is1.web;

import java.util.Set;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ru.itmo.is1.dto.common.PageResponse;
import ru.itmo.is1.dto.route.RouteDto;
import ru.itmo.is1.dto.route.RouteRequest;
import ru.itmo.is1.exception.BadRequestException;
import ru.itmo.is1.service.RouteService;

@RestController
@RequestMapping("/api/routes")
@RequiredArgsConstructor
public class RouteController {
    private static final Set<String> SORTABLE_FIELDS = Set.of(
            "id", "name", "creationDate", "distance", "rating",
            "coordinates.x", "coordinates.y",
            "from.name", "from.x", "from.y",
            "to.name", "to.x", "to.y");

    private final RouteService routeService;

    @GetMapping
    public PageResponse<RouteDto> list(@RequestParam(required = false) String name,
                                       @RequestParam(required = false) String fromName,
                                       @RequestParam(required = false) String toName,
                                       @PageableDefault(sort = "id") Pageable pageable) {
        requireSortableFields(pageable.getSort());
        return routeService.findPage(name, fromName, toName, pageable);
    }

    @GetMapping("/{id}")
    public RouteDto get(@PathVariable Integer id) {
        return routeService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RouteDto create(@Valid @RequestBody RouteRequest request) {
        return routeService.create(request);
    }

    @PutMapping("/{id}")
    public RouteDto update(@PathVariable Integer id, @Valid @RequestBody RouteRequest request) {
        return routeService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        routeService.delete(id);
    }

    private static void requireSortableFields(Sort sort) {
        sort.stream()
                .map(Sort.Order::getProperty)
                .filter(property -> !SORTABLE_FIELDS.contains(property))
                .findFirst()
                .ifPresent(property -> {
                    throw new BadRequestException("Sorting by field '" + property + "' is not supported");
                });
    }
}
