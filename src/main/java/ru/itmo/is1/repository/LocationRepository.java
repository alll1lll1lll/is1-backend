package ru.itmo.is1.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.itmo.is1.model.Location;

public interface LocationRepository extends JpaRepository<Location, Integer> {
}
