package ru.itmo.is1.web;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import ru.itmo.is1.dto.route.PathMode;
import ru.itmo.is1.dto.route.RouteBetweenRequest;
import ru.itmo.is1.dto.route.RouteDto;
import ru.itmo.is1.service.RouteSpecialService;

@RestController
@RequestMapping("/api/special")
@RequiredArgsConstructor
public class SpecialOperationsController {
    private final RouteSpecialService specialService;

    @GetMapping("/max-from")
    public ResponseEntity<RouteDto> maxFrom() {
        return okOrNoContent(specialService.findWithMaxFrom());
    }

    @GetMapping("/count-by-rating")
    public Map<String, Long> countByRating(
            @RequestParam @Positive(message = "rating must be greater than 0") float rating) {
        return Map.of("count", specialService.countByRating(rating));
    }

    @GetMapping("/name-contains")
    public List<RouteDto> nameContains(
            @RequestParam @NotEmpty(message = "substring must not be empty") String substring) {
        return specialService.findByNameContaining(substring);
    }

    @GetMapping("/between")
    public ResponseEntity<RouteDto> between(@RequestParam Integer fromId,
                                            @RequestParam Integer toId,
                                            @RequestParam(defaultValue = "SHORTEST") PathMode mode) {
        return okOrNoContent(specialService.findBetween(fromId, toId, mode));
    }

    @PostMapping("/between")
    @ResponseStatus(HttpStatus.CREATED)
    public RouteDto addBetween(@Valid @RequestBody RouteBetweenRequest request) {
        return specialService.addBetween(request);
    }

    private static <T> ResponseEntity<T> okOrNoContent(Optional<T> body) {
        return body.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
    }
}
