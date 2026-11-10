package ru.itmo.is1.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ru.itmo.is1.dto.coordinates.CoordinatesDto;
import ru.itmo.is1.dto.coordinates.CoordinatesRequest;
import ru.itmo.is1.service.CoordinatesService;

@RestController
@RequestMapping("/api/coordinates")
@RequiredArgsConstructor
public class CoordinatesController {
    private final CoordinatesService service;

    @GetMapping
    public List<CoordinatesDto> list() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public CoordinatesDto get(@PathVariable Integer id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CoordinatesDto create(@Valid @RequestBody CoordinatesRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public CoordinatesDto update(@PathVariable Integer id, @Valid @RequestBody CoordinatesRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        service.delete(id);
    }
}
