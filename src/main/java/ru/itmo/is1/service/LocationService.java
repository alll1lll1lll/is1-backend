package ru.itmo.is1.service;

import static ru.itmo.is1.event.ChangeEvent.Action.CREATED;
import static ru.itmo.is1.event.ChangeEvent.Action.DELETED;
import static ru.itmo.is1.event.ChangeEvent.Action.UPDATED;
import static ru.itmo.is1.event.ChangeEvent.EntityType.LOCATION;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import ru.itmo.is1.dto.location.LocationDto;
import ru.itmo.is1.dto.location.LocationRef;
import ru.itmo.is1.dto.location.LocationRequest;
import ru.itmo.is1.event.ChangeNotifier;
import ru.itmo.is1.exception.ResourceInUseException;
import ru.itmo.is1.exception.ResourceNotFoundException;
import ru.itmo.is1.mapper.DtoMapper;
import ru.itmo.is1.model.Location;
import ru.itmo.is1.repository.LocationRepository;
import ru.itmo.is1.repository.RouteRepository;

@Service
@Transactional
@RequiredArgsConstructor
public class LocationService {
    private final LocationRepository locationRepository;
    private final RouteRepository routeRepository;
    private final ChangeNotifier notifier;

    @Transactional(readOnly = true)
    public List<LocationDto> findAll() {
        return locationRepository.findAll(Sort.by("id")).stream()
                .map(DtoMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public LocationDto findById(Integer id) {
        return DtoMapper.toDto(getExisting(id));
    }

    public LocationDto create(LocationRequest request) {
        return DtoMapper.toDto(createEntity(request.getX(), request.getY(), request.getName()));
    }

    public LocationDto update(Integer id, LocationRequest request) {
        Location location = getExisting(id);
        location.setX(request.getX());
        location.setY(request.getY());
        location.setName(trim(request.getName()));
        locationRepository.saveAndFlush(location);
        notifier.notify(LOCATION, UPDATED, id);
        return DtoMapper.toDto(location);
    }

    public void delete(Integer id) {
        Location location = getExisting(id);
        if (routeRepository.isLocationUsed(id)) {
            throw ResourceInUseException.location(id);
        }
        locationRepository.delete(location);
        notifier.notify(LOCATION, DELETED, id);
    }

    Location resolve(LocationRef ref) {
        if (ref == null) {
            return null;
        }
        return ref.getId() != null
                ? getExisting(ref.getId())
                : createEntity(ref.getX(), ref.getY(), ref.getName());
    }

    Location getExisting(Integer id) {
        return locationRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.location(id));
    }

    private Location createEntity(long x, double y, String name) {
        Location location = locationRepository.saveAndFlush(new Location(x, y, trim(name)));
        notifier.notify(LOCATION, CREATED, location.getId());
        return location;
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }
}
