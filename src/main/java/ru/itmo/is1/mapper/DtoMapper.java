package ru.itmo.is1.mapper;

import ru.itmo.is1.dto.coordinates.CoordinatesDto;
import ru.itmo.is1.dto.location.LocationDto;
import ru.itmo.is1.dto.route.RouteDto;
import ru.itmo.is1.model.Coordinates;
import ru.itmo.is1.model.Location;
import ru.itmo.is1.model.Route;

public final class DtoMapper {
    private DtoMapper() {
    }

    public static CoordinatesDto toDto(Coordinates coordinates) {
        if (coordinates == null) {
            return null;
        }
        return new CoordinatesDto(coordinates.getId(), coordinates.getX(), coordinates.getY());
    }

    public static LocationDto toDto(Location location) {
        if (location == null) {
            return null;
        }
        return new LocationDto(location.getId(), location.getX(), location.getY(), location.getName());
    }

    public static RouteDto toDto(Route route) {
        if (route == null) {
            return null;
        }
        return new RouteDto(
                route.getId(),
                route.getName(),
                toDto(route.getCoordinates()),
                route.getCreationDate(),
                toDto(route.getFrom()),
                toDto(route.getTo()),
                route.getDistance(),
                route.getRating());
    }
}
