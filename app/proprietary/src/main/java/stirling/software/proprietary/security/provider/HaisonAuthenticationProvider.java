package stirling.software.proprietary.security.provider;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import stirling.software.common.model.ApplicationProperties;
import stirling.software.proprietary.security.model.AuthenticationType;
import stirling.software.proprietary.security.service.CustomUserDetailsService;
import stirling.software.proprietary.security.service.UserService;

@Slf4j
@Component
@RequiredArgsConstructor
public class HaisonAuthenticationProvider implements AuthenticationProvider {

    private final ApplicationProperties applicationProperties;
    private final CustomUserDetailsService customUserDetailsService;
    private final UserService userService;
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public Authentication authenticate(Authentication authentication)
            throws AuthenticationException {
        if (applicationProperties.getSecurity().getHaisonMainAuth() == null
                || !applicationProperties.getSecurity().getHaisonMainAuth().isEnabled()) {
            return null; // Not enabled, let DaoAuthenticationProvider handle it
        }

        String username = authentication.getName();
        String password = authentication.getCredentials().toString();

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("username", username);
            requestBody.put("password", password);

            HttpEntity<Map<String, String>> request = new HttpEntity<>(requestBody, headers);
            String url = applicationProperties.getSecurity().getHaisonMainAuth().getUrl();

            log.debug("Authenticating user {} via haison-main at {}", username, url);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Boolean success = (Boolean) response.getBody().get("success");

                // haison-main app_mysql.py returns token on success, or success=true
                if (response.getBody().containsKey("token") || Boolean.TRUE.equals(success)) {
                    log.info("Successfully authenticated user {} via haison-main", username);

                    try {
                        // Ensure user exists locally for Stirling-PDF to issue its own JWT
                        userService.processSSOPostLogin(
                                username, username, "haison-main", true, AuthenticationType.OAUTH2);
                    } catch (Exception e) {
                        log.error("Failed to process auto-creation for user {}", username, e);
                    }

                    UserDetails userDetails = customUserDetailsService.loadUserByUsername(username);
                    return new UsernamePasswordAuthenticationToken(
                            userDetails, password, userDetails.getAuthorities());
                } else {
                    log.debug("haison-main auth failed for user {}", username);
                }
            }
        } catch (Exception e) {
            log.error("Error communicating with haison-main auth server: {}", e.getMessage());
        }

        // Return null to allow other providers (like local DB) to attempt authentication
        return null;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
