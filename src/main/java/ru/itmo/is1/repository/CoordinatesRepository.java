package ru.itmo.is1.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.itmo.is1.model.Coordinates;

public interface CoordinatesRepository extends JpaRepository<Coordinates, Integer> {
}
