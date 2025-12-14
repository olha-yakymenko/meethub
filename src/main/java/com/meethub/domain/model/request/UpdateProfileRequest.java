//package com.meethub.domain.model.request;
//
//import lombok.Data;
//import jakarta.validation.constraints.Email;
//import jakarta.validation.constraints.NotBlank;
//import java.util.Set;
//
//@Data
//public class UpdateProfileRequest {
//    @NotBlank(message = "Imię jest wymagane")
//    private String firstName;
//
//    @NotBlank(message = "Nazwisko jest wymagane")
//    private String lastName;
//
//    @Email(message = "Nieprawidłowy format email")
//    private String email;
//
//    private String phoneNumber;
//    private String timezone;
//    private String language;
//}



package com.meethub.domain.model.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class UpdateProfileRequest {

    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 50, message = "First name must be 2-50 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 50, message = "Last name must be 2-50 characters")
    private String lastName;

    @Email(message = "Invalid email format")
    @NotBlank(message = "Email is required")
    private String email;

    @Pattern(regexp = "^\\+?[0-9\\s\\-()]{7,20}$",
            message = "Invalid phone number format")
    @Size(max = 20, message = "Phone number cannot exceed 20 characters")
    private String phoneNumber;

    @Size(max = 50, message = "Timezone cannot exceed 50 characters")
    private String timezone;

    @Pattern(regexp = "^[a-z]{2}(-[A-Z]{2})?$",
            message = "Language code must be like: pl or en-US")
    @Size(min = 2, max = 10, message = "Language code must be 2-10 characters")
    private String language;
}
