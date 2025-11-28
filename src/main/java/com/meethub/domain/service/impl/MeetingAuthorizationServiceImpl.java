//// MeetingAuthorizationServiceImpl.java
//package com.meethub.domain.service.impl;
//
//import com.meethub.domain.model.entity.Meeting;
//import com.meethub.domain.model.entity.MeetingResource;
//import com.meethub.domain.model.enums.MeetingVisibility;
//import com.meethub.domain.model.response.MeetingParticipationInfo;
//import com.meethub.domain.repository.jpa.MeetingRepository;
//import com.meethub.domain.repository.jpa.MeetingResourceRepository;
//import com.meethub.domain.service.MeetingAuthorizationService;
//import com.meethub.domain.service.MeetingParticipantService;
//import com.meethub.exception.ResourceNotFoundException;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//import com.meethub.domain.model.enums.ResourceAccessLevel;
//import java.util.ArrayList;
//import java.util.List;
//
//@Slf4j
//@Service
//@RequiredArgsConstructor
//public class MeetingAuthorizationServiceImpl implements MeetingAuthorizationService {
//
//    private final MeetingRepository meetingRepository;
//    private final MeetingParticipantService meetingParticipantService;
//    private final MeetingResourceRepository meetingResourceRepository;
//    @Override
//    @Transactional(readOnly = true)
//    public MeetingParticipationInfo getUserMeetingPermissions(Long meetingId, Long userId) {
//        Meeting meeting = meetingRepository.findById(meetingId)
//                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found with id: " + meetingId));
//
//        return buildParticipationInfo(meeting, userId);
//    }
//
//    private MeetingParticipationInfo buildParticipationInfo(Meeting meeting, Long userId) {
//        // ✅ DOMYŚLNE WARTOŚCI DLA NIEZALOGOWANYCH
//        boolean isOrganizer = false;
//        boolean isDirectParticipant = false;
//        boolean isRelated = false;
//        String participantRole = "NONE";
//        List<String> permissions = new ArrayList<>();
//
//        if (userId != null) {
//            // ✅ SPRAWDŹ ORGANIZATORA
//            isOrganizer = meeting.getOrganizer() != null &&
//                    meeting.getOrganizer().getId().equals(userId);
//
//            // ✅ SPRAWDŹ BEZPOŚREDNIEGO UCZESTNICTWA
//            try {
//                isDirectParticipant = meetingParticipantService.isUserParticipant(userId, meeting.getId());
//            } catch (Exception e) {
//                log.warn("Could not check participant status for user {} in meeting {}: {}",
//                        userId, meeting.getId(), e.getMessage());
//            }
//
//            // ✅ LOGIKA BUSINESS - ORGANIZATOR JEST AUTOMATYCZNIE UCZESTNIKIEM
//            if (isOrganizer) {
//                isRelated = true;
//                participantRole = "ORGANIZER";
//                permissions.addAll(List.of("EDIT", "DELETE", "INVITE", "MANAGE_PARTICIPANTS", "VIEW_DETAILS"));
//            }
//            // ✅ ZWYKŁY UCZESTNIK
//            else if (isDirectParticipant) {
//                isRelated = true;
//                participantRole = "PARTICIPANT";
//                permissions.addAll(List.of("VIEW_DETAILS", "JOIN", "COMMENT"));
//            }
//        }
//
//        // ✅ UPRAWNIENIA DLA WSZYSTKICH (W TYM NIEZALOGOWANYCH)
//        if (meeting.getVisibility() == MeetingVisibility.PUBLIC) {
//            permissions.add("VIEW_DETAILS");
//        }
//
//        return MeetingParticipationInfo.builder()
//                .isOrganizer(isOrganizer)
//                .isParticipant(isOrganizer || isDirectParticipant) // ✅ ORGANIZATOR JEST UCZESTNIKIEM!
//                .isRelated(isRelated)
//                .participantRole(participantRole)
//                .permissions(permissions)
//                .canEdit(permissions.contains("EDIT"))
//                .canDelete(permissions.contains("DELETE"))
//                .canManageParticipants(permissions.contains("MANAGE_PARTICIPANTS"))
//                .canJoin(permissions.contains("JOIN"))
//                .canViewDetails(permissions.contains("VIEW_DETAILS"))
//                .build();
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public boolean canUserEditMeeting(Long meetingId, Long userId) {
//        MeetingParticipationInfo info = getUserMeetingPermissions(meetingId, userId);
//        return info.isCanEdit();
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public boolean canUserDeleteMeeting(Long meetingId, Long userId) {
//        MeetingParticipationInfo info = getUserMeetingPermissions(meetingId, userId);
//        return info.isCanDelete();
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public boolean canUserManageParticipants(Long meetingId, Long userId) {
//        MeetingParticipationInfo info = getUserMeetingPermissions(meetingId, userId);
//        return info.isCanManageParticipants();
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public boolean canUserJoinMeeting(Long meetingId, Long userId) {
//        MeetingParticipationInfo info = getUserMeetingPermissions(meetingId, userId);
//        return info.isCanJoin();
//    }
//
//
//    // MeetingAuthorizationServiceImpl.java - DODAJ TE METODY
//    @Override
//    @Transactional(readOnly = true)
//    public boolean canUserViewResource(Long meetingId, Long userId) {
//        MeetingParticipationInfo participationInfo = getUserMeetingPermissions(meetingId, userId);
//
//        // ✅ ORGANIZATOR WIDZI WSZYSTKO
//        if (participationInfo.isOrganizer()) {
//            return true;
//        }
//
//        // ✅ UCZESTNICY WIDZĄ ZASOBY DLA PARTICIPANTS I PUBLIC
//        if (participationInfo.isParticipant()) {
//            return true; // Uczestnicy widzą wszystkie zasoby spotkania
//        }
//
//        // ✅ SPRAWDŹ WIDOCZNOŚĆ SPOTKANIA
//        Meeting meeting = meetingRepository.findById(meetingId)
//                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));
//
//        return meeting.getVisibility() == MeetingVisibility.PUBLIC;
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public boolean canUserDownloadResource(Long meetingId, Long userId) {
//        MeetingParticipationInfo participationInfo = getUserMeetingPermissions(meetingId, userId);
//
//        // ✅ ORGANIZATOR MOŻE WSZYSTKO
//        if (participationInfo.isOrganizer()) {
//            return true;
//        }
//
//        // ✅ UCZESTNICY MOGĄ POBRAĆ
//        return participationInfo.isParticipant();
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public boolean canUserUploadResource(Long meetingId, Long userId) {
//        MeetingParticipationInfo participationInfo = getUserMeetingPermissions(meetingId, userId);
//
//        // ✅ ORGANIZATOR I CO-ORGANIZATOR MOGĄ DODAWAĆ
//        if (participationInfo.isOrganizer() || participationInfo.isCanManageParticipants()) {
//            return true;
//        }
//
//        // ✅ UCZESTNICY MOGĄ DODAWAĆ JEŚLI SPOTKANIE POZWALA
//        return participationInfo.isParticipant();
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public boolean canUserDeleteResource(Long meetingId, Long resourceId, Long userId) {
//        MeetingParticipationInfo participationInfo = getUserMeetingPermissions(meetingId, userId);
//
//        // ✅ TYLKO ORGANIZATOR I WŁAŚCICIEL ZASOBU MOŻE USUNĄĆ
//        if (participationInfo.isOrganizer()) {
//            return true;
//        }
//
//        // ✅ SPRAWDŹ CZY UŻYTKOWNIK JEST WŁAŚCICIELEM ZASOBU
//        try {
//            MeetingResource resource = meetingResourceRepository.findById(resourceId)
//                    .orElseThrow(() -> new ResourceNotFoundException("Resource not found"));
//            return resource.getUploadedBy().getId().equals(userId);
//        } catch (Exception e) {
//            return false;
//        }
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public ResourceAccessLevel getUserResourceAccessLevel(Long meetingId, Long userId) {
//        MeetingParticipationInfo participationInfo = getUserMeetingPermissions(meetingId, userId);
//
//        if (participationInfo.isOrganizer()) {
//            return ResourceAccessLevel.MANAGE;
//        }
//
//        if (participationInfo.isCanManageParticipants()) {
//            return ResourceAccessLevel.UPLOAD;
//        }
//
//        if (participationInfo.isParticipant()) {
//            return ResourceAccessLevel.DOWNLOAD;
//        }
//
//        // ✅ DLA PUBLICZNYCH SPOTKAŃ - TYLKO PODGLĄD
//        Meeting meeting = meetingRepository.findById(meetingId)
//                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));
//
//        if (meeting.getVisibility() == MeetingVisibility.PUBLIC) {
//            return ResourceAccessLevel.VIEW;
//        }
//
//        return ResourceAccessLevel.NONE;
//    }
//}







// MeetingAuthorizationServiceImpl.java
package com.meethub.domain.service.impl;

import com.meethub.domain.model.entity.Meeting;
import com.meethub.domain.model.entity.MeetingResource;
import com.meethub.domain.model.enums.MeetingVisibility;
import com.meethub.domain.model.enums.PermissionLevel;
import com.meethub.domain.model.response.MeetingParticipationInfo;
import com.meethub.domain.repository.jpa.MeetingRepository;
import com.meethub.domain.repository.jpa.MeetingResourceRepository;
import com.meethub.domain.service.MeetingAuthorizationService;
import com.meethub.domain.service.MeetingParticipantService;
import com.meethub.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.meethub.domain.model.enums.ResourceAccessLevel;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MeetingAuthorizationServiceImpl implements MeetingAuthorizationService {

    private final MeetingRepository meetingRepository;
    private final MeetingParticipantService meetingParticipantService;
    private final MeetingResourceRepository meetingResourceRepository;

    @Override
    @Transactional(readOnly = true)
    public MeetingParticipationInfo getUserMeetingPermissions(Long meetingId, Long userId) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found with id: " + meetingId));

        return buildParticipationInfo(meeting, userId);
    }

    private MeetingParticipationInfo buildParticipationInfo(Meeting meeting, Long userId) {
        // ✅ DOMYŚLNE WARTOŚCI DLA NIEZALOGOWANYCH
        boolean isOrganizer = false;
        boolean isDirectParticipant = false;
        boolean isRelated = false;
        boolean canManageParticipants = false;
        String participantRole = "NONE";
        List<String> permissions = new ArrayList<>();

        if (userId != null) {
            // ✅ SPRAWDŹ ORGANIZATORA
            isOrganizer = meeting.getOrganizer() != null &&
                    meeting.getOrganizer().getId().equals(userId);

            // ✅ SPRAWDŹ BEZPOŚREDNIEGO UCZESTNICTWA
            try {
                isDirectParticipant = meetingParticipantService.isUserParticipant(userId, meeting.getId());
            } catch (Exception e) {
                log.warn("Could not check participant status for user {} in meeting {}: {}",
                        userId, meeting.getId(), e.getMessage());
            }

            // ✅ SPRAWDŹ UPRAWNIENIA MODERATORA/CO-ORGANIZATORA
            PermissionLevel userPermissionLevel = getParticipantPermissionLevel(meeting.getId(), userId);
            canManageParticipants = isOrganizer ||
                    userPermissionLevel == PermissionLevel.MODERATOR ||
                    userPermissionLevel == PermissionLevel.CONTRIBUTOR;

            // ✅ LOGIKA BUSINESS - ORGANIZATOR JEST AUTOMATYCZNIE UCZESTNIKIEM
            if (isOrganizer) {
                isRelated = true;
                participantRole = "ORGANIZER";
                permissions.addAll(List.of("EDIT", "DELETE", "INVITE", "MANAGE_PARTICIPANTS", "VIEW_DETAILS", "JOIN", "COMMENT", "UPLOAD", "DOWNLOAD"));
            }
            // ✅ MODERATOR/CO-ORGANIZATOR
            else if (canManageParticipants && !isOrganizer) {
                isRelated = true;
                participantRole = "MODERATOR";
                permissions.addAll(List.of("EDIT", "INVITE", "MANAGE_PARTICIPANTS", "VIEW_DETAILS", "JOIN", "COMMENT", "UPLOAD", "DOWNLOAD"));
            }
            // ✅ ZWYKŁY UCZESTNIK
            else if (isDirectParticipant) {
                isRelated = true;
                participantRole = "PARTICIPANT";
                permissions.addAll(List.of("VIEW_DETAILS", "JOIN", "COMMENT", "DOWNLOAD"));
                // ✅ UCZESTNICY MOGĄ RÓWNIEŻ UPLOADOWAĆ JEŚLI MAJĄ ODPOWIEDNIE UPRAWNIENIA
                if (userPermissionLevel == PermissionLevel.CONTRIBUTOR) {
                    permissions.add("UPLOAD");
                }
            }
        }

        // ✅ UPRAWNIENIA DLA WSZYSTKICH (W TYM NIEZALOGOWANYCH)
        if (meeting.getVisibility() == MeetingVisibility.PUBLIC) {
            if (!permissions.contains("VIEW_DETAILS")) {
                permissions.add("VIEW_DETAILS");
            }
        }

        return MeetingParticipationInfo.builder()
                .isOrganizer(isOrganizer)
                .isParticipant(isOrganizer || isDirectParticipant) // ✅ ORGANIZATOR JEST UCZESTNIKIEM!
                .isRelated(isRelated)
                .participantRole(participantRole)
                .permissions(permissions)
                .canEdit(permissions.contains("EDIT"))
                .canDelete(permissions.contains("DELETE"))
                .canManageParticipants(permissions.contains("MANAGE_PARTICIPANTS"))
                .canJoin(permissions.contains("JOIN"))
                .canViewDetails(permissions.contains("VIEW_DETAILS"))
                .canUpload(permissions.contains("UPLOAD"))
                .canDownload(permissions.contains("DOWNLOAD"))
                .build();
    }

    private PermissionLevel getParticipantPermissionLevel(Long meetingId, Long userId) {
        try {
            return meetingParticipantService.getParticipantPermissionLevel(meetingId, userId);
        } catch (Exception e) {
            log.debug("Could not get participant permission level for user {} in meeting {}: {}",
                    userId, meetingId, e.getMessage());
            return PermissionLevel.PARTICIPANT; // domyślny poziom
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canUserEditMeeting(Long meetingId, Long userId) {
        MeetingParticipationInfo info = getUserMeetingPermissions(meetingId, userId);
        return info.isCanEdit();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canUserDeleteMeeting(Long meetingId, Long userId) {
        MeetingParticipationInfo info = getUserMeetingPermissions(meetingId, userId);
        return info.isCanDelete();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canUserManageParticipants(Long meetingId, Long userId) {
        MeetingParticipationInfo info = getUserMeetingPermissions(meetingId, userId);
        return info.isCanManageParticipants();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canUserJoinMeeting(Long meetingId, Long userId) {
        MeetingParticipationInfo info = getUserMeetingPermissions(meetingId, userId);
        return info.isCanJoin();
    }

//    @Override
//    @Transactional(readOnly = true)
//    public boolean canUserViewResource(Long meetingId, Long userId) {
//        MeetingParticipationInfo participationInfo = getUserMeetingPermissions(meetingId, userId);
//
//        // ✅ ORGANIZATOR, MODERATOR I UCZESTNICY WIDZĄ WSZYSTKIE ZASOBY
//        if (participationInfo.isOrganizer() || participationInfo.isCanManageParticipants() || participationInfo.isParticipant()) {
//            return true;
//        }
//
//        // ✅ SPRAWDŹ WIDOCZNOŚĆ SPOTKANIA DLA PUBLICZNYCH
//        Meeting meeting = meetingRepository.findById(meetingId)
//                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));
//
//        return meeting.getVisibility() == MeetingVisibility.PUBLIC;
//    }


    @Override
    @Transactional(readOnly = true)
    public boolean canUserViewResource(Long meetingId, Long userId) {
        MeetingParticipationInfo participationInfo = getUserMeetingPermissions(meetingId, userId);

        log.debug("Resource access check - Meeting: {}, User: {}, Organizer: {}, CanManage: {}, Participant: {}",
                meetingId, userId, participationInfo.isOrganizer(),
                participationInfo.isCanManageParticipants(), participationInfo.isParticipant());

        // ✅ ORGANIZATOR ZAWSZE WIDZI WSZYSTKIE ZASOBY
        if (participationInfo.isOrganizer()) {
            log.debug("✅ Access granted - User is organizer");
            return true;
        }

        // ✅ MODERATOR/CO-ORGANIZATOR ZAWSZE WIDZI WSZYSTKIE ZASOBY
        if (participationInfo.isCanManageParticipants()) {
            log.debug("✅ Access granted - User can manage participants");
            return true;
        }

        // ✅ UCZESTNICY Z UPRAWNIENIEM VIEW_DETAILS WIDZĄ ZASOBY
        if (participationInfo.isParticipant() && participationInfo.isCanViewDetails()) {
            log.debug("✅ Access granted - User is participant with view details permission");
            return true;
        }

        // ✅ SPRAWDŹ WIDOCZNOŚĆ SPOTKANIA DLA PUBLICZNYCH
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));

        boolean hasPublicAccess = meeting.getVisibility() == MeetingVisibility.PUBLIC && participationInfo.isCanViewDetails();
        log.debug("Public access check - Visibility: {}, CanViewDetails: {}, Result: {}",
                meeting.getVisibility(), participationInfo.isCanViewDetails(), hasPublicAccess);

        return hasPublicAccess;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canUserDownloadResource(Long meetingId, Long userId) {
        MeetingParticipationInfo participationInfo = getUserMeetingPermissions(meetingId, userId);

        // ✅ ORGANIZATOR, MODERATOR I UCZESTNICY MOGĄ POBRAĆ
        return participationInfo.isOrganizer() ||
                participationInfo.isCanManageParticipants() ||
                participationInfo.isCanDownload();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canUserUploadResource(Long meetingId, Long userId) {
        MeetingParticipationInfo participationInfo = getUserMeetingPermissions(meetingId, userId);

        // ✅ ORGANIZATOR, MODERATOR I UCZESTNICY Z UPRAWNIENIAMI UPLOAD MOGĄ DODAWAĆ
        return participationInfo.isOrganizer() ||
                participationInfo.isCanManageParticipants() ||
                participationInfo.isCanUpload();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canUserDeleteResource(Long meetingId, Long resourceId, Long userId) {
        MeetingParticipationInfo participationInfo = getUserMeetingPermissions(meetingId, userId);

        // ✅ ORGANIZATOR I MODERATOR MOGĄ USUNĄĆ KAŻDY ZASÓB
        if (participationInfo.isOrganizer() || participationInfo.isCanManageParticipants()) {
            return true;
        }

        // ✅ SPRAWDŹ CZY UŻYTKOWNIK JEST WŁAŚCICIELEM ZASOBU
        try {
            MeetingResource resource = meetingResourceRepository.findById(resourceId)
                    .orElseThrow(() -> new ResourceNotFoundException("Resource not found"));
            return resource.getUploadedBy().getId().equals(userId);
        } catch (Exception e) {
            log.warn("Error checking resource ownership for user {} and resource {}: {}",
                    userId, resourceId, e.getMessage());
            return false;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ResourceAccessLevel getUserResourceAccessLevel(Long meetingId, Long userId) {
        MeetingParticipationInfo participationInfo = getUserMeetingPermissions(meetingId, userId);

        // ✅ ORGANIZATOR - PEŁNE UPRAWNIENIA
        if (participationInfo.isOrganizer()) {
            return ResourceAccessLevel.MANAGE;
        }

        // ✅ MODERATOR/CO-ORGANIZATOR - PEŁNE UPRAWNIENIA (BEZ USUWANIA CUDZYCH ZASOBÓW)
        if (participationInfo.isCanManageParticipants()) {
            return ResourceAccessLevel.MANAGE;
        }

        // ✅ UCZESTNICY Z UPRAWNIENIAMI UPLOAD
        if (participationInfo.isCanUpload()) {
            return ResourceAccessLevel.UPLOAD;
        }

        // ✅ UCZESTNICY Z UPRAWNIENIAMI DOWNLOAD
        if (participationInfo.isCanDownload()) {
            return ResourceAccessLevel.DOWNLOAD;
        }

        // ✅ DLA PUBLICZNYCH SPOTKAŃ - TYLKO PODGLĄD
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));

        if (meeting.getVisibility() == MeetingVisibility.PUBLIC && participationInfo.isCanViewDetails()) {
            return ResourceAccessLevel.VIEW;
        }

        return ResourceAccessLevel.NONE;
    }

    @Transactional(readOnly = true)
    @Override
    public boolean hasResourceAccess(Long meetingId, Long userId, ResourceAccessLevel requiredLevel) {
        ResourceAccessLevel userLevel = getUserResourceAccessLevel(meetingId, userId);

        // ✅ HIERARCHIA DOSTĘPU: NONE < VIEW < DOWNLOAD < UPLOAD < MANAGE
        return userLevel.ordinal() >= requiredLevel.ordinal();
    }

    @Transactional(readOnly = true)
    @Override
    public boolean canUserComment(Long meetingId, Long userId) {
        MeetingParticipationInfo participationInfo = getUserMeetingPermissions(meetingId, userId);

        // ✅ ORGANIZATOR, MODERATOR I UCZESTNICY MOGĄ KOMENTOWAĆ
        return participationInfo.isOrganizer() ||
                participationInfo.isCanManageParticipants() ||
                participationInfo.isParticipant();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canUserViewParticipants(Long meetingId, Long userId) {
        MeetingParticipationInfo participationInfo = getUserMeetingPermissions(meetingId, userId);

        // ✅ ORGANIZATOR, MODERATOR I UCZESTNICY WIDZĄ LISTĘ UCZESTNIKÓW
        if (participationInfo.isOrganizer() || participationInfo.isCanManageParticipants() || participationInfo.isParticipant()) {
            return true;
        }

        // ✅ DLA PUBLICZNYCH SPOTKAŃ - TYLKO ORGANIZATOR JEST WIDOCZNY
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));

        return meeting.getVisibility() == MeetingVisibility.PUBLIC;
    }
}