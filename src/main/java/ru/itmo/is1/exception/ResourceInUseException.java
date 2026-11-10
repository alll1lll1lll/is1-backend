package ru.itmo.is1.exception;

public class ResourceInUseException extends ApiException {
    private ResourceInUseException(String message) {
        super(ErrorCode.RESOURCE_IN_USE, message);
    }

    public static ResourceInUseException location(Integer id) {
        return new ResourceInUseException(
                "Cannot delete location #" + id + ": it is used by a route");
    }

    public static ResourceInUseException coordinates(Integer id) {
        return new ResourceInUseException(
                "Cannot delete coordinates #" + id + ": they are used by a route");
    }
}
