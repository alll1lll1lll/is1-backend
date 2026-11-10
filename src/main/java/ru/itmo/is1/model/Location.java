package ru.itmo.is1.model;

import java.util.Comparator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Pattern;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "location")
@Getter
@Setter
@NoArgsConstructor
public class Location implements Comparable<Location> {
    public static final Comparator<Location> NATURAL_ORDER = Comparator
            .comparingLong(Location::getX)
            .thenComparingDouble(Location::getY)
            .thenComparing(Location::getName, Comparator.nullsFirst(Comparator.naturalOrder()));

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Integer id;

    @Column(name = "x", nullable = false)
    private long x;

    @Column(name = "y", nullable = false)
    private double y;

    @Pattern(regexp = "(?s).*\\S.*", message = "location name must not be empty")
    @Column(name = "name")
    private String name;

    public Location(long x, double y, String name) {
        this.x = x;
        this.y = y;
        this.name = name;
    }

    @Override
    public int compareTo(Location other) {
        return NATURAL_ORDER.compare(this, other);
    }
}
