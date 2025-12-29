package com.meethub.domain.model.request;

import lombok.Data;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Data
public class PageableRequest {

    @Min(value = 0, message = "Numer strony nie może być ujemny")
    private Integer page = 0;

    @Min(value = 1, message = "Rozmiar strony musi być co najmniej 1")
    @Max(value = 100, message = "Rozmiar strony nie może przekraczać 100")
    private Integer size = 20;

    private String sort; // np. "title,asc" lub "createdAt,desc"

    public Pageable toPageable() {
        if (sort != null && !sort.trim().isEmpty()) {
            String[] sortParams = sort.split(",");
            if (sortParams.length == 2) {
                Sort.Direction direction = Sort.Direction.fromString(sortParams[1]);
                return PageRequest.of(page, size, Sort.by(direction, sortParams[0]));
            }
        }
        return PageRequest.of(page, size);
    }
}