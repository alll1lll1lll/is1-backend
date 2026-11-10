package ru.itmo.is1.dto.coordinates;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.validation.constraints.AssertTrue;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CoordinatesRef {
    private Integer id;
    private Long x;
    private Integer y;

    @JsonIgnore
    @AssertTrue(message = "specify existing coordinates id or both x and y")
    public boolean isValidRef() {
        return id != null || (x != null && y != null);
    }
}
