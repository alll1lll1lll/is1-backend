package ru.itmo.is1.dto.coordinates;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CoordinatesRequest {
    @NotNull(message = "x is required")
    private Long x;

    @NotNull(message = "y is required")
    private Integer y;
}
