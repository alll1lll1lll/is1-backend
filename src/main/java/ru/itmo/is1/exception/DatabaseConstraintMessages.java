package ru.itmo.is1.exception;

import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DatabaseConstraintMessages {
    private static final String INTEGRITY_VIOLATION_STATE_PREFIX = "23";
    private static final Pattern CONSTRAINT_NAME = Pattern.compile("constraint \"([^\"]+)\"");
    private static final String DEFAULT_MESSAGE = "The operation violates database constraints";

    private DatabaseConstraintMessages() {
    }

    public static boolean isIntegrityViolation(SQLException exception) {
        String state = exception.getSQLState();
        return state != null && state.startsWith(INTEGRITY_VIOLATION_STATE_PREFIX);
    }

    public static String describe(SQLException exception) {
        String text = exception.getMessage();
        if (text == null) {
            return DEFAULT_MESSAGE;
        }
        Matcher matcher = CONSTRAINT_NAME.matcher(text);
        if (!matcher.find()) {
            return DEFAULT_MESSAGE;
        }
        return DatabaseConstraint.fromName(matcher.group(1))
                .map(DatabaseConstraint::getMessage)
                .orElse(DEFAULT_MESSAGE);
    }
}
