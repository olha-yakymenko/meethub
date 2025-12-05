package com.meethub.controller.api;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/debug")
public class DebugController {

    @GetMapping("/auth")
    public Map<String, Object> getAuthInfo() {
        log.info("🔍 Debug endpoint called - checking authentication");

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> info = new HashMap<>();

        info.put("authenticated", auth != null && auth.isAuthenticated());
        info.put("principal", auth != null ? auth.getPrincipal() : null);
        info.put("principalClass", auth != null ? auth.getPrincipal().getClass().getName() : null);
        info.put("name", auth != null ? auth.getName() : null);
        info.put("authorities", auth != null ? auth.getAuthorities() : null);
        info.put("credentials", auth != null ? auth.getCredentials() : null);
        info.put("details", auth != null ? auth.getDetails() : null);

        log.info("📊 Auth debug info: {}", info);
        return info;
    }
}