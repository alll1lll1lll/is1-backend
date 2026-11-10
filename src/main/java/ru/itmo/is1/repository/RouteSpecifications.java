package ru.itmo.is1.repository;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import ru.itmo.is1.model.Route;

public final class RouteSpecifications {
    private static final char ESCAPE = '\\';

    private RouteSpecifications() {
    }

    public static Specification<Route> filter(String name, String fromName, String toName) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (hasText(name)) {
                predicates.add(contains(cb, root.get("name"), name));
            }
            if (hasText(fromName)) {
                predicates.add(contains(cb, root.join("from", JoinType.LEFT).get("name"), fromName));
            }
            if (hasText(toName)) {
                predicates.add(contains(cb, root.join("to", JoinType.LEFT).get("name"), toName));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static Predicate contains(CriteriaBuilder cb, Expression<String> field, String value) {
        String pattern = "%" + escapeLike(value.trim().toLowerCase()) + "%";
        return cb.like(cb.lower(field), pattern, ESCAPE);
    }

    private static String escapeLike(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }
}
