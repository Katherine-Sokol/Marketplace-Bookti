package com.teamchallenge.bookti.security.jwt;

import com.teamchallenge.bookti.Application;
import com.teamchallenge.bookti.dto.authorization.TokenPair;
import com.teamchallenge.bookti.exception.RefreshTokenAlreadyRevokedException;
import com.teamchallenge.bookti.security.AuthorizedUser;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        classes = Application.class
)
class TokenManagerTest {

    private static AuthorizedUser user;
    private static JwtClaimsSet expiredClaimSet;
    private static JwtClaimsSet validClaimSet;
    @Autowired
    @Qualifier("jwtRefreshTokenEncoder")
    private JwtEncoder refreshTokenEncoder;
    @Autowired
    private TokenManager tokenManager;
    @MockBean
    private Authentication authentication;

    @BeforeAll
    static void setup() {
        user = AuthorizedUser
                .authorizedUserBuilder("username", "password", List.of())
                .id(UUID.randomUUID())
                .build();
        Instant now = Instant.now();
        expiredClaimSet = JwtClaimsSet
                .builder()
                .issuer("test-app")
                .issuedAt(now)
                .expiresAt(now.plus(2, ChronoUnit.DAYS))
                .subject(user.getId().toString())
                .build();
        validClaimSet = JwtClaimsSet
                .builder()
                .issuer("test-app")
                .issuedAt(now)
                .expiresAt(now.plus(7, ChronoUnit.DAYS))
                .subject(user.getId().toString())
                .build();
    }

    @BeforeEach
    void resetRevokedTokenMap() {
        tokenManager
                .getRevokedTokens()
                .clear();
    }

    @Test
    @DisplayName("When user authenticate via <UsernamePasswordAuthenticationToken>, then generate and return JWT key pair")
    void whenUserAuthenticateWithUsernamePasswordAuthenticationTokenThenGenerateTokenPair() {
        when(authentication.getPrincipal()).thenReturn(user);
        when(authentication.getCredentials()).thenReturn(String.class);

        TokenPair tokenPair = assertDoesNotThrow(() -> tokenManager.generateTokenPair(authentication));

        verify(authentication, times(3)).getPrincipal();
        verify(authentication, times(1)).getCredentials();
        assertAll("tokenPair",
                () -> assertNotNull(tokenPair),
                () -> assertEquals(user.getId().toString(), tokenPair.getUserId()),
                () -> assertNotNull(tokenPair.getAccessToken()),
                () -> assertNotNull(tokenPair.getRefreshToken()),
                () -> assertTrue(tokenManager.getRevokedTokens().isEmpty())
        );
    }

    @Test
    @DisplayName("When authentication principal is not of type <AuthorizedUser.class>, then throw BadCredentialsException")
    void whenAuthenticationPrincipalIsNotAuthorizedUserTypeThenThrowBadCredentialsException() {
        when(authentication.getPrincipal()).thenReturn(new Object());

        var e = assertThrows(BadCredentialsException.class, () -> tokenManager.generateTokenPair(authentication));

        verify(authentication, times(1)).getPrincipal();
        assertAll("exception",
                () -> assertNotNull(e),
                () -> assertTrue(e.getMessage().startsWith(String.format("Principal <%s>", Object.class))),
                () -> assertTrue(e.getMessage().endsWith(String.format("<%s> type", AuthorizedUser.class))),
                () -> assertTrue(tokenManager.getRevokedTokens().isEmpty())
        );
    }

    @Test
    @DisplayName("When user try to refresh access token with valid non-expired refresh token, then generate new access token and return it with old refresh token")
    void whenUserRefreshAccessTokenWithValidAndNonExpiredRefreshTokenThenReturnNewAccessTokenAndOldRefreshToken() {
        Jwt validToken = refreshTokenEncoder.encode(JwtEncoderParameters.from(validClaimSet));

        when(authentication.getPrincipal()).thenReturn(user);
        when(authentication.getCredentials()).thenReturn(validToken);

        TokenPair tokenPair = assertDoesNotThrow(() -> tokenManager.generateTokenPair(authentication));

        verify(authentication, times(2)).getPrincipal();
        verify(authentication, times(1)).getCredentials();
        assertAll("tokenPair",
                () -> assertNotNull(tokenPair),
                () -> assertEquals(user.getId().toString(), tokenPair.getUserId()),
                () -> assertNotNull(tokenPair.getAccessToken()),
                () -> assertNotNull(tokenPair.getRefreshToken()),
                () -> assertEquals(validToken.getTokenValue(), tokenPair.getRefreshToken()),
                () -> assertTrue(tokenManager.getRevokedTokens().isEmpty())
        );
    }

    @Test
    @DisplayName("When user try to refresh access token with valid almost expired refresh token, then revoke refresh token and return new JWT token pair")
    void whenUserRefreshAccessTokenWithValidAndAlmostExpiredRefreshTokenThenRevokeRefreshTokenAndGenerateNewTokenPair() {
        Jwt almostExpiredToken = refreshTokenEncoder.encode(JwtEncoderParameters.from(expiredClaimSet));
        String token = almostExpiredToken.getTokenValue();

        when(authentication.getPrincipal()).thenReturn(user);
        when(authentication.getCredentials()).thenReturn(almostExpiredToken);

        TokenPair tokenPair = assertDoesNotThrow(() -> tokenManager.generateTokenPair(authentication));

        verify(authentication, times(3)).getPrincipal();
        verify(authentication, times(1)).getCredentials();
        assertAll(
                () -> assertNotNull(tokenPair),
                () -> assertEquals(user.getId().toString(), tokenPair.getUserId()),
                () -> assertNotNull(tokenPair.getAccessToken()),
                () -> assertNotNull(tokenPair.getRefreshToken()),
                () -> assertNotEquals(token, tokenPair.getRefreshToken()),
                () -> assertTrue(tokenManager.getRevokedTokens().containsKey(token))
        );
    }

    @Test
    @DisplayName("When user try to revoke valid non-expired refresh token, then add this token to revokedToken list")
    void whenUserRevokeValidAndNonExpiredRefreshTokenThenAddTokenToRevokedList() {
        Jwt tokenToRevoke = refreshTokenEncoder.encode(JwtEncoderParameters.from(validClaimSet));

        when(authentication.getCredentials()).thenReturn(tokenToRevoke);

        assertDoesNotThrow(() -> tokenManager.revokeToken(authentication));

        verify(authentication, times(1)).getCredentials();
        assertTrue(tokenManager.getRevokedTokens().containsKey(tokenToRevoke.getTokenValue()));
    }

    @Test
    @DisplayName("When user try to revoke refresh token that not a Jwt.class, than throw BadCredentialsException")
    void whenUserRevokeRefreshTokenIsNotJwtTypeThenThrowBadCredentialsException() {
        when(authentication.getCredentials()).thenReturn("");

        var e = assertThrows(BadCredentialsException.class, () -> tokenManager.revokeToken(authentication));

        verify(authentication, times(1)).getCredentials();
        assertAll(
                () -> assertNotNull(e),
                () -> assertTrue(e.getMessage().startsWith(String.format("Credentials <%s>", String.class))),
                () -> assertTrue(e.getMessage().endsWith(String.format("<%s> type", Jwt.class))),
                () -> assertTrue(tokenManager.getRevokedTokens().isEmpty())
        );
    }

    @Test
    @DisplayName("When user try to revoke refresh token that already revoked, than throw RefreshTokenAlreadyRevokedException")
    void whenUserRevokeAlreadyRevokedRefreshTokenThenThrowRefreshTokenAlreadyRevokedException() {
        Jwt alreadyRevokedToken = refreshTokenEncoder.encode(JwtEncoderParameters.from(validClaimSet));
        String tokenValue = alreadyRevokedToken.getTokenValue();
        tokenManager.getRevokedTokens().put(tokenValue, UUID.randomUUID());

        when(authentication.getCredentials()).thenReturn(alreadyRevokedToken);

        var e = assertThrows(RefreshTokenAlreadyRevokedException.class, () -> tokenManager.revokeToken(authentication));
        verify(authentication, times(1)).getCredentials();
        assertAll(
                () -> assertNotNull(e),
                () -> assertTrue(e.getMessage().startsWith(String.format("Token <%s>", tokenValue))),
                () -> assertTrue(e.getMessage().endsWith("revoked")),
                () -> assertTrue(tokenManager.getRevokedTokens().containsKey(tokenValue))
        );
    }
}
