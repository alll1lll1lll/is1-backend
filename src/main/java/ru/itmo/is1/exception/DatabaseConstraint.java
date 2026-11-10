package ru.itmo.is1.exception;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DatabaseConstraint {
    ROUTE_NAME_NOT_BLANK("route_name_not_blank", "Route name must not be empty"),
    ROUTE_DISTANCE_GT_ONE("route_distance_gt_one", "Distance must be greater than 1"),
    ROUTE_RATING_POSITIVE("route_rating_positive", "Rating must be greater than 0"),
    ROUTE_ID_POSITIVE("route_id_positive", "Route id must be greater than 0"),
    LOCATION_NAME_NOT_BLANK("location_name_not_blank", "Location name must not be empty"),
    ROUTE_COORDINATES_FK("route_coordinates_id_fkey", "Coordinates are referenced by a route"),
    ROUTE_FROM_LOCATION_FK("route_from_location_id_fkey", "Origin location is referenced by a route"),
    ROUTE_TO_LOCATION_FK("route_to_location_id_fkey", "Destination location is referenced by a route"),
    USERNAME_UNIQUE("app_user_username_unique", "Username is already taken"),
    USERNAME_NOT_BLANK("app_user_username_not_blank", "Username must not be empty"),
    REFRESH_TOKEN_HASH_UNIQUE("refresh_token_hash_unique", "Refresh token collision, please retry");

    private static final Map<String, DatabaseConstraint> BY_NAME = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(DatabaseConstraint::getConstraintName, Function.identity()));

    private final String constraintName;
    private final String message;

    public static Optional<DatabaseConstraint> fromName(String constraintName) {
        return Optional.ofNullable(BY_NAME.get(constraintName));
    }
}
