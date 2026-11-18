package fr.donner.web;

import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

final class ListingQuerySupport {
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "publishedAt", "title", "area", "state", "id");

    private ListingQuerySupport() {
    }

    static String blank(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    static Sort parseSort(String sortParam) {
        if (sortParam == null || sortParam.isBlank()) {
            return Sort.by(Sort.Direction.DESC, "publishedAt");
        }
        String[] parts = sortParam.split(",");
        String field = parts[0].trim();
        if (!ALLOWED_SORT_FIELDS.contains(field)) {
            field = "publishedAt";
        }
        Sort.Direction direction = Sort.Direction.DESC;
        if (parts.length > 1 && "asc".equalsIgnoreCase(parts[1].trim())) {
            direction = Sort.Direction.ASC;
        } else if (parts.length == 1 && ("title".equals(field) || "area".equals(field))) {
            direction = Sort.Direction.ASC;
        }
        return Sort.by(direction, field).and(Sort.by(Sort.Direction.DESC, "id"));
    }

    static Pageable pageRequest(int page, int size, String sort, int maxSize) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), maxSize);
        return PageRequest.of(safePage, safeSize, parseSort(sort));
    }
}
