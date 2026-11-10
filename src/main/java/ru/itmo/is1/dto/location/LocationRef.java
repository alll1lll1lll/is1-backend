package ru.itmo.is1.dto.location;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class LocationRef {
    private Integer id;
    private Long x;
    private Double y;

    @Pattern(regexp = "(?s).*\\S.*", message = "location name must not be empty")
    private String name;

    public static LocationRef ofId(Integer id) {
        LocationRef ref = new LocationRef();
        ref.setId(id);
        return ref;
    }

    @JsonIgnore
    @AssertTrue(message = "specify an existing location id or both x and y")
    public boolean isValidRef() {
        return id != null || (x != null && y != null);
    }
}
