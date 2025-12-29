package com.meethub.domain.model.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NotificationsListRequest {

    @Min(value = 0, message = "Numer strony nie może być ujemny")
    private Integer page = 0;

    @Min(value = 1, message = "Rozmiar strony musi być co najmniej 1")
    @Max(value = 100, message = "Rozmiar strony nie może przekraczać 100")
    private Integer size = 20;

    private String principalName;
}