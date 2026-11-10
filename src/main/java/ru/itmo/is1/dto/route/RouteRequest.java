package ru.itmo.is1.dto.route;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.itmo.is1.dto.coordinates.CoordinatesRef;
import ru.itmo.is1.dto.location.LocationRef;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteRequest {
    @NotBlank(message = "name must not be empty")
    private String name;

    @NotNull(message = "coordinates are required")
    @Valid
    private CoordinatesRef coordinates;

    @Valid
    private LocationRef from;

    @Valid
    private LocationRef to;

    @NotNull(message = "distance is required")
    @Min(value = 2, message = "distance must be greater than 1")
    private Integer distance;

    @Positive(message = "rating must be greater than 0")
    private Float rating;
}
