package ru.itmo.is1.dto.location;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LocationDto {
    private Integer id;
    private long x;
    private double y;
    private String name;
}
