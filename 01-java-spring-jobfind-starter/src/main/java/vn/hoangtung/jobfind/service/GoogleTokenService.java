package vn.hoangtung.jobfind.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Service;

@Service
public class GoogleTokenService {
    private static final String GOOGLE_JWK_SET_URI = "https://www.googleapis.com/oauth2/v3/certs";
    private static final String GOOGLE_ISSUER = "https://accounts.google.com";
    private static final String GOOGLE_ISSUER_SHORT = "accounts.google.com";

    private final JwtDecoder jwtDecoder;

    public GoogleTokenService(@Value("${google.oauth2.client-id}") String googleClientId) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(GOOGLE_JWK_SET_URI).build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefault(),
                validateIssuer(),
                validateAudience(googleClientId)));
        this.jwtDecoder = decoder;
    }

    public GoogleUserInfo verify(String credential) {
        Jwt jwt = this.jwtDecoder.decode(credential);
        String email = jwt.getClaimAsString("email");
        Object emailVerified = jwt.getClaims().get("email_verified");

        if (email == null || email.isBlank()) {
            throw new BadCredentialsException("Google token does not contain an email");
        }
        if (!Boolean.TRUE.equals(emailVerified) && !"true".equals(String.valueOf(emailVerified))) {
            throw new BadCredentialsException("Google email is not verified");
        }

        return new GoogleUserInfo(
                jwt.getSubject(),
                email,
                jwt.getClaimAsString("name"),
                jwt.getClaimAsString("picture"));
    }

    private OAuth2TokenValidator<Jwt> validateIssuer() {
        return jwt -> {
            String issuer = jwt.getIssuer() == null ? "" : jwt.getIssuer().toString();
            if (GOOGLE_ISSUER.equals(issuer) || GOOGLE_ISSUER_SHORT.equals(issuer)) {
                return OAuth2TokenValidatorResult.success();
            }
            return OAuth2TokenValidatorResult.failure(
                    new OAuth2Error("invalid_token", "Invalid Google token issuer", null));
        };
    }

    private OAuth2TokenValidator<Jwt> validateAudience(String googleClientId) {
        return jwt -> {
            if (jwt.getAudience().contains(googleClientId)) {
                return OAuth2TokenValidatorResult.success();
            }
            return OAuth2TokenValidatorResult.failure(
                    new OAuth2Error("invalid_token", "Invalid Google token audience", null));
        };
    }

    public record GoogleUserInfo(String subject, String email, String name, String picture) {
    }
}
