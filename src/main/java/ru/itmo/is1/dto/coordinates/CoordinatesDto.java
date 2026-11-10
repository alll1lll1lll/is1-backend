package ru.itmo.is1.dto.coordinates;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CoordinatesDto {
    private Integer id;
    private long x;
    private int y;
}
