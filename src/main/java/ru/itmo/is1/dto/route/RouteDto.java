package ru.itmo.is1.dto.route;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Getter;
import ru.itmo.is1.dto.coordinates.CoordinatesDto;
import ru.itmo.is1.dto.location.LocationDto;

@Getter
@AllArgsConstructor
public class RouteDto {
    private Integer id;
    private String name;
    private CoordinatesDto coordinates;
    private Date creationDate;
    private LocationDto from;
    private LocationDto to;
    private int distance;
    private Float rating;
}
