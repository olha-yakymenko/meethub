//// UpdateAssignmentRequest.java
//package com.meethub.domain.model.request;
//
//import com.meethub.domain.model.enums.AssignmentStatus;
//import jakarta.validation.constraints.NotNull;
//import lombok.AllArgsConstructor;
//import lombok.Builder;
//import lombok.Data;
//import lombok.NoArgsConstructor;
//
//
//@Data
//@Builder
//@NoArgsConstructor
//@AllArgsConstructor
//public class UpdateAssignmentRequest {
//
//    @NotNull(message = "Status jest wymagany")
//    private AssignmentStatus status;
//
//    private String comment; // Komentarz przy zmianie statusu
//}





package com.meethub.domain.model.request;

import com.meethub.domain.model.enums.AssignmentStatus;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAssignmentRequest {

    @NotNull(message = "Status jest wymagany")
    private AssignmentStatus status;

    @Size(max = 500, message = "Comment cannot exceed 500 characters")
    private String comment;
}