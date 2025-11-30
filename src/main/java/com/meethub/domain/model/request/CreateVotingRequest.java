//package com.meethub.domain.model.request;
//
//import com.meethub.domain.model.enums.VotingType;
//import lombok.Data;
//import jakarta.validation.constraints.*;
//import java.time.LocalDateTime;
//import java.util.List;
//
//@Data
//public class CreateVotingRequest {
//    @NotBlank(message = "Tytuł głosowania jest wymagany")
//    private String title;
//
//    private String description;
//
//    @NotNull(message = "Typ głosowania jest wymagany")
//    private VotingType type;
//
//    private Integer maxChoices;
//    private Boolean allowSuggestions;
//    private LocalDateTime deadlineDate;
//    private Boolean autoClose;
//
//    @NotEmpty(message = "Musisz dodać przynajmniej jedną opcję")
//    private List<VotingOptionRequest> options;
//}





package com.meethub.domain.model.request;

import com.meethub.domain.model.enums.VotingType;
import lombok.Data;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class CreateVotingRequest {
    @NotBlank(message = "Tytuł głosowania jest wymagany")
    private String title;

    private String description;

    @NotNull(message = "Typ głosowania jest wymagany")
    private VotingType type;

    private Integer maxChoices;
    private Boolean allowSuggestions;
    private LocalDateTime deadlineDate;
    private Boolean autoClose;

    @NotEmpty(message = "Musisz dodać przynajmniej jedną opcję")
    private List<VotingOptionRequest> options;
}