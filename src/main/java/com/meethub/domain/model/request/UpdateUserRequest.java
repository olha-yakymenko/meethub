//package com.meethub.domain.model.request;
//
//import lombok.AllArgsConstructor;
//import lombok.Builder;
//import lombok.Data;
//import lombok.NoArgsConstructor;
//
//@Data
//@Builder
//@NoArgsConstructor
//@AllArgsConstructor
//public class UpdateUserRequest {
//    private String firstName;
//    private String lastName;
//    private String phoneNumber;
//}


package com.meethub.domain.model.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRequest {

    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 50, message = "First name must be 2-50 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 50, message = "Last name must be 2-50 characters")
    private String lastName;

    @Pattern(regexp = "^\\+?[0-9\\s\\-()]{7,20}$",
            message = "Invalid phone number format")
    @Size(max = 20, message = "Phone number cannot exceed 20 characters")
    private String phoneNumber;
}