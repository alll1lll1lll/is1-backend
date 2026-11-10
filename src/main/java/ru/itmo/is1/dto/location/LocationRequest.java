package ru.itmo.is1.dto.location;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class LocationRequest {
    @NotNull(message = "x is required")
    private Long x;

    @NotNull(message = "y is required")
    private Double y;

    @Pattern(regexp = "(?s).*\\S.*", message = "name must not be blank (use null instead)")
    private String name;
}
