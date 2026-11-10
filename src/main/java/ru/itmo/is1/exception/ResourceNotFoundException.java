package ru.itmo.is1.exception;

public class ResourceNotFoundException extends ApiException {
    private ResourceNotFoundException(String message) {
        super(ErrorCode.NOT_FOUND, message);
    }

    public static ResourceNotFoundException route(Integer id) {
        return new ResourceNotFoundException("Route #" + id + " not found");
    }

    public static ResourceNotFoundException location(Integer id) {
        return new ResourceNotFoundException("Location #" + id + " not found");
    }

    public static ResourceNotFoundException coordinates(Integer id) {
        return new ResourceNotFoundException("Coordinates #" + id + " not found");
    }
}
