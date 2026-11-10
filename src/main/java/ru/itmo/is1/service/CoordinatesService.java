package ru.itmo.is1.service;

import static ru.itmo.is1.event.ChangeEvent.Action.CREATED;
import static ru.itmo.is1.event.ChangeEvent.Action.DELETED;
import static ru.itmo.is1.event.ChangeEvent.Action.UPDATED;
import static ru.itmo.is1.event.ChangeEvent.EntityType.COORDINATES;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import ru.itmo.is1.dto.coordinates.CoordinatesDto;
import ru.itmo.is1.dto.coordinates.CoordinatesRef;
import ru.itmo.is1.dto.coordinates.CoordinatesRequest;
import ru.itmo.is1.event.ChangeNotifier;
import ru.itmo.is1.exception.ResourceInUseException;
import ru.itmo.is1.exception.ResourceNotFoundException;
import ru.itmo.is1.mapper.DtoMapper;
import ru.itmo.is1.model.Coordinates;
import ru.itmo.is1.repository.CoordinatesRepository;
import ru.itmo.is1.repository.RouteRepository;

@Service
@Transactional
@RequiredArgsConstructor
public class CoordinatesService {
    private final CoordinatesRepository coordinatesRepository;
    private final RouteRepository routeRepository;
    private final ChangeNotifier notifier;

    @Transactional(readOnly = true)
    public List<CoordinatesDto> findAll() {
        return coordinatesRepository.findAll(Sort.by("id")).stream()
                .map(DtoMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public CoordinatesDto findById(Integer id) {
        return DtoMapper.toDto(getExisting(id));
    }

    public CoordinatesDto create(CoordinatesRequest request) {
        return DtoMapper.toDto(createEntity(request.getX(), request.getY()));
    }

    public CoordinatesDto update(Integer id, CoordinatesRequest request) {
        Coordinates coordinates = getExisting(id);
        coordinates.setX(request.getX());
        coordinates.setY(request.getY());
        coordinatesRepository.saveAndFlush(coordinates);
        notifier.notify(COORDINATES, UPDATED, id);
        return DtoMapper.toDto(coordinates);
    }

    public void delete(Integer id) {
        Coordinates coordinates = getExisting(id);
        if (routeRepository.existsByCoordinatesId(id)) {
            throw ResourceInUseException.coordinates(id);
        }
        coordinatesRepository.delete(coordinates);
        notifier.notify(COORDINATES, DELETED, id);
    }

    Coordinates resolve(CoordinatesRef ref) {
        return ref.getId() != null
                ? getExisting(ref.getId())
                : createEntity(ref.getX(), ref.getY());
    }

    private Coordinates getExisting(Integer id) {
        return coordinatesRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.coordinates(id));
    }

    private Coordinates createEntity(long x, int y) {
        Coordinates coordinates = coordinatesRepository.saveAndFlush(new Coordinates(x, y));
        notifier.notify(COORDINATES, CREATED, coordinates.getId());
        return coordinates;
    }
}
