package com.teamchallenge.bookti.user;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teamchallenge.bookti.exception.PasswordIsNotMatchesException;
import com.teamchallenge.bookti.exception.RefreshTokenAlreadyRevokedException;
import com.teamchallenge.bookti.exception.UserAlreadyExistsException;
import com.teamchallenge.bookti.exception.UserNotFoundException;
import com.teamchallenge.bookti.security.AuthorizedUser;
import com.teamchallenge.bookti.security.jwt.TokenManager;
import com.teamchallenge.bookti.utils.EmailUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper jsonMapper;

  @MockBean private UserService userService;

  @MockBean private TokenManager tokenManager;

  @MockBean private AuthenticationManager authenticationManager;

  @MockBean
  @Qualifier("jwtRefreshTokenAuthenticationProvider")
  private JwtAuthenticationProvider refreshTokenProvider;

  @MockBean private EmailUtils emailUtils;

  @MockBean private UserRepository userRepository;

  @Test
  @Tag("signup")
  @DisplayName(
      "When calling /signup, expect that request is valid, then response with TokenPair.class and status code 201")
  void whenUserSignUpRequestIsValidThenResponseWithTokenPairAndStatusCode201() throws Exception {
    NewUserRegistrationRequest userDetails =
        new NewUserRegistrationRequest(
            "fullName", "abc@gmail.com", "Password1", "Password1", "city");
    var user =
        AuthorizedUser.authorizedUserBuilder(
                userDetails.getEmail(), userDetails.getPassword(), List.of())
            .id(UUID.randomUUID())
            .build();
    var tokenPair =
        TokenPair.builder()
            .userId(user.getId().toString())
            .accessToken("access_token")
            .refreshToken("refresh_token")
            .build();
    var authentication =
        new UsernamePasswordAuthenticationToken(user, user.getPassword(), user.getAuthorities());

    try (MockedStatic<UsernamePasswordAuthenticationToken> mock =
        mockStatic(UsernamePasswordAuthenticationToken.class)) {

      when(userService.create(eq(userDetails))).thenReturn(user);

      mock.when(
              () ->
                  UsernamePasswordAuthenticationToken.authenticated(
                      user, user.getPassword(), user.getAuthorities()))
          .thenReturn(authentication);

      when(tokenManager.generateTokenPair(eq(authentication))).thenReturn(tokenPair);

      mockMvc
          .perform(
              post("/api/v1/authorize/signup")
                  .contentType(APPLICATION_JSON)
                  .content(jsonMapper.writeValueAsString(userDetails)))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("user_id").value(user.getId().toString()))
          .andExpect(jsonPath("access_token").exists())
          .andExpect(jsonPath("refresh_token").exists());

      verify(userService, times(1)).create(eq(userDetails));
      mock.verify(
          () ->
              UsernamePasswordAuthenticationToken.authenticated(
                  user, user.getPassword(), user.getAuthorities()),
          times(1));
      verify(tokenManager, times(1)).generateTokenPair(eq(authentication));
    }
  }

  @Test
  @Tag("signup")
  @DisplayName(
      "When calling /signup, expect that passwords in request not matches, then response with ErrorResponse.class and status code 409")
  void whenUserSignUpRequestWithNotEqualsPasswordsThenResponseWithErrorResponseAndStatusCode409()
      throws Exception {
    NewUserRegistrationRequest userDetails =
        new NewUserRegistrationRequest(
            "FirstName", "abc@gmail.com", "Password1", "PassNoWord2", "city");
    var user =
        AuthorizedUser.authorizedUserBuilder(
                userDetails.getEmail(), userDetails.getPassword(), List.of())
            .id(UUID.randomUUID())
            .build();
    var authentication =
        new UsernamePasswordAuthenticationToken(user, user.getPassword(), user.getAuthorities());

    try (MockedStatic<UsernamePasswordAuthenticationToken> mock =
        mockStatic(UsernamePasswordAuthenticationToken.class)) {
      when(userService.create(userDetails))
          .thenThrow(new PasswordIsNotMatchesException("Password is not matches"));

      mockMvc
          .perform(
              post("/api/v1/authorize/signup")
                  .contentType(APPLICATION_JSON)
                  .content(jsonMapper.writeValueAsString(userDetails)))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("timestamp").exists())
          .andExpect(jsonPath("status_code").value("400"))
          .andExpect(jsonPath("message").value("Password is not matches"));

      verify(userService, times(1)).create(eq(userDetails));
      mock.verify(
          () ->
              UsernamePasswordAuthenticationToken.authenticated(
                  user, user.getPassword(), user.getAuthorities()),
          never());
      verify(tokenManager, never()).generateTokenPair(eq(authentication));
    }
  }

  @Test
  @Tag("signup")
  @DisplayName(
      "When calling /signup, expect that request fail validation, then response with ErrorResponse.class and status code 400")
  void whenUserSignupRequestThatIsInvalidThenResponseWithErrorResponseAndStatusCode400()
      throws Exception {
    NewUserRegistrationRequest userDetails =
        new NewUserRegistrationRequest("", "invalidEmail", "12345", "54321", "");
    var user =
        AuthorizedUser.authorizedUserBuilder(
                userDetails.getEmail(), userDetails.getPassword(), List.of())
            .id(UUID.randomUUID())
            .build();
    var authentication =
        new UsernamePasswordAuthenticationToken(user, user.getPassword(), user.getAuthorities());

    try (MockedStatic<UsernamePasswordAuthenticationToken> mock =
        mockStatic(UsernamePasswordAuthenticationToken.class)) {
      mockMvc
          .perform(
              post("/api/v1/authorize/signup")
                  .contentType(APPLICATION_JSON)
                  .content(jsonMapper.writeValueAsString(userDetails)))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("timestamp").exists())
          .andExpect(jsonPath("status_code").value("400"))
          .andExpect(jsonPath("message").value("Validation failed"));

      verify(userService, never()).create(eq(userDetails));
      mock.verify(
          () ->
              UsernamePasswordAuthenticationToken.authenticated(
                  user, user.getPassword(), user.getAuthorities()),
          never());
      verify(tokenManager, never()).generateTokenPair(eq(authentication));
    }
  }

  @Test
  @Tag("signup")
  @DisplayName(
      "when calling /signup, expect that user already exists, then response with ErrorResponse.class and status code 409")
  void whenUserSignUpRequestThatAlreadyExistsThenResponseWithErrorResponseAndStatusCode409()
      throws Exception {
    NewUserRegistrationRequest userDetails =
        new NewUserRegistrationRequest(
            "first_name", "abc@gmail.com", "Password1", "Password1", "city");
    try (MockedStatic<UsernamePasswordAuthenticationToken> mock =
        mockStatic(UsernamePasswordAuthenticationToken.class)) {
      when(userService.create(userDetails))
          .thenThrow(
              new UserAlreadyExistsException(
                  String.format("User <%s> already exists", userDetails.getEmail())));

      mockMvc
          .perform(
              post("/api/v1/authorize/signup")
                  .contentType(APPLICATION_JSON)
                  .content(jsonMapper.writeValueAsString(userDetails)))
          .andExpect(status().isConflict())
          .andExpect(jsonPath("timestamp").exists())
          .andExpect(jsonPath("status_code").value("409"))
          .andExpect(
              jsonPath("message")
                  .value(String.format("User <%s> already exists", userDetails.getEmail())));

      verify(userService, times(1)).create(userDetails);
      mock.verify(
          () ->
              UsernamePasswordAuthenticationToken.authenticated(
                  any(AuthorizedUser.class), any(String.class), anyList()),
          never());
      verify(tokenManager, never()).generateTokenPair(any(Authentication.class));
    }
  }

  @Test
  @Tag("login")
  @DisplayName(
      "when calling /login, expect that credentials are valid, then response with TokenPair.class and status code 200")
  void whenUserLoginRequestWithValidCredentialsThenResponseWithTokenPairAndStatusCode200()
      throws Exception {
    UserLoginRequest request = new UserLoginRequest("testmail@gmail.com", "PassWord110k");
    var user =
        AuthorizedUser.authorizedUserBuilder(request.getEmail(), request.getPassword(), List.of())
            .id(UUID.randomUUID())
            .build();
    var tokenPair =
        TokenPair.builder()
            .userId(user.getId().toString())
            .accessToken("access_token")
            .refreshToken("refresh_token")
            .build();
    var authentication =
        new UsernamePasswordAuthenticationToken(user, user.getPassword(), user.getAuthorities());

    when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authentication);

    when(tokenManager.generateTokenPair(any(Authentication.class))).thenReturn(tokenPair);

    mockMvc
        .perform(
            post("/api/v1/authorize/login")
                .contentType(APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("user_id").value(user.getId().toString()))
        .andExpect(jsonPath("access_token").exists())
        .andExpect(jsonPath("refresh_token").exists());

    verify(authenticationManager, times(1)).authenticate(any(Authentication.class));
    verify(tokenManager, times(1)).generateTokenPair(any(Authentication.class));
  }

  @Test
  @Tag("login")
  @DisplayName(
      "when calling /login, expect that credentials are invalid, then response with ErrorResponse.class and status code 401")
  void whenUserLoginRequestWithInvalidCredentialsThenResponseWithErrorResponseAndStatusCode401()
      throws Exception {
    UserLoginRequest request = new UserLoginRequest("testmail@gmail.com", "PassWord110k");

    when(authenticationManager.authenticate(any(Authentication.class)))
        .thenThrow(new BadCredentialsException("Bad Credentials"));

    mockMvc
        .perform(
            post("/api/v1/authorize/login")
                .contentType(APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request)))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("status_code").value(HttpStatus.UNAUTHORIZED.value()))
        .andExpect(jsonPath("message").value("Bad Credentials"));

    verify(authenticationManager, times(1)).authenticate(any(Authentication.class));
    verify(tokenManager, never()).generateTokenPair(any(Authentication.class));
  }

  @Test
  @Tag("login")
  @DisplayName(
      "when calling /login, expect that request is invalid, then response with ErrorResponse.class and status code 400")
  void whenUserLoginRequestIsInvalidThenResponseWithErrorResponseAndStatusCode400()
      throws Exception {
    UserLoginRequest request = new UserLoginRequest("", "1234");

    mockMvc
        .perform(
            post("/api/v1/authorize/login")
                .contentType(APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("status_code").value(HttpStatus.BAD_REQUEST.value()))
        .andExpect(jsonPath("message").value("Validation failed"));

    verify(authenticationManager, never()).authenticate(any(Authentication.class));
    verify(tokenManager, never()).generateTokenPair(any(Authentication.class));
  }

  @Test
  @Tag("refreshToken")
  @DisplayName(
      "when calling /token/refresh, expect that request is valid, then response with TokenPair.class and status code 200")
  void whenUserRefreshTokenRequestIsValidThenResponseWithTokenPairAndStatusCode200()
      throws Exception {
    UserTokenPair request = new UserTokenPair(UUID.randomUUID().toString(), "refresh_token");
    var user =
        AuthorizedUser.authorizedUserBuilder("email", "password", List.of())
            .id(UUID.fromString(request.getUserId()))
            .build();
    TokenPair tokenPair =
        TokenPair.builder()
            .userId(request.getUserId())
            .accessToken("access_token")
            .refreshToken("refresh_token")
            .build();
    Authentication authentication =
        new UsernamePasswordAuthenticationToken(user, user.getPassword(), user.getAuthorities());

    when(tokenManager.isRefreshTokenRevoked(eq(request.getRefreshToken())))
        .thenReturn(Boolean.FALSE);
    when(refreshTokenProvider.authenticate(any(Authentication.class))).thenReturn(authentication);
    when(tokenManager.generateTokenPair(any(Authentication.class))).thenReturn(tokenPair);

    mockMvc
        .perform(
            post("/api/v1/authorize/token/refresh")
                .contentType(APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("user_id").value(user.getId().toString()))
        .andExpect(jsonPath("access_token").exists())
        .andExpect(jsonPath("refresh_token").exists());

    verify(tokenManager, times(1)).isRefreshTokenRevoked(eq(request.getRefreshToken()));
    verify(refreshTokenProvider, times(1)).authenticate(any(Authentication.class));
    verify(tokenManager, times(1)).generateTokenPair(any(Authentication.class));
  }

  @Test
  @Tag("refreshToken")
  @DisplayName(
      "when calling /token/refresh, expect that refresh token invalid, then response with ErrorResponse.class and status code 401")
  void whenUserRefreshTokenRequestIsInvalidThenResponseWithErrorResponseAndStatusCode401()
      throws Exception {
    UserTokenPair request = new UserTokenPair(UUID.randomUUID().toString(), "refresh_token");

    when(tokenManager.isRefreshTokenRevoked(eq(request.getRefreshToken())))
        .thenReturn(Boolean.FALSE);
    when(refreshTokenProvider.authenticate(any(Authentication.class)))
        .thenThrow(new BadCredentialsException("Bad credentials"));

    mockMvc
        .perform(
            post("/api/v1/authorize/token/refresh")
                .contentType(APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request)))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("status_code").value(HttpStatus.UNAUTHORIZED.value()))
        .andExpect(jsonPath("message").value("Bad credentials"));

    verify(refreshTokenProvider, times(1)).authenticate(any(Authentication.class));
    verify(tokenManager, times(1)).isRefreshTokenRevoked(eq(request.getRefreshToken()));
    verify(tokenManager, never()).generateTokenPair(any(Authentication.class));
  }

  @Test
  @Tag("refreshToken")
  @DisplayName(
      "when calling /token/refresh, expect that refresh token revoked, then response with ErrorResponse.class and status code 409")
  void whenUserRefreshTokenRequestWithRevokedTokenThenResponseWIthErrorResponseAndStatusCode409()
      throws Exception {
    UserTokenPair request = new UserTokenPair(UUID.randomUUID().toString(), "refresh_token");

    when(tokenManager.isRefreshTokenRevoked(eq(request.getRefreshToken())))
        .thenThrow(new RefreshTokenAlreadyRevokedException("Token revoked"));

    mockMvc
        .perform(
            post("/api/v1/authorize/token/refresh")
                .contentType(APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("status_code").value(HttpStatus.CONFLICT.value()))
        .andExpect(jsonPath("message").value("Token revoked"));

    verify(refreshTokenProvider, never()).authenticate(any(Authentication.class));
    verify(tokenManager, times(1)).isRefreshTokenRevoked(eq(request.getRefreshToken()));
    verify(tokenManager, never()).generateTokenPair(any(Authentication.class));
  }

  @Test
  @Tag("revokeToken")
  @DisplayName(
      "when calling /token/revoke, expect that request is valid, then response with AppResponse.class and status code 200")
  void whenUserTokenRevokeRequestValidThenResponseWithAppResponseAndStatusCode200()
      throws Exception {
    UserTokenPair request = new UserTokenPair(UUID.randomUUID().toString(), "refresh_token");
    var user =
        AuthorizedUser.authorizedUserBuilder("email", "password", List.of())
            .id(UUID.fromString(request.getUserId()))
            .build();
    Authentication authentication =
        new UsernamePasswordAuthenticationToken(user, user.getPassword(), user.getAuthorities());

    when(refreshTokenProvider.authenticate(any(Authentication.class))).thenReturn(authentication);

    doNothing().when(tokenManager).revokeToken(eq(authentication));

    mockMvc
        .perform(
            post("/api/v1/authorize/token/revoke")
                .contentType(APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("status_code").value(HttpStatus.OK.value()))
        .andExpect(jsonPath("message").value("Token <refresh_token> successfully revoked"));

    verify(refreshTokenProvider, times(1)).authenticate(any(Authentication.class));
    verify(tokenManager, times(1)).revokeToken(eq(authentication));
  }

  @Test
  @Tag("revokeToken")
  @DisplayName(
      "when calling /token/revoke, expect that refresh token is invalid, then response with ErrorResponse.class and status code 401")
  void whenUserTokenRevokeRequestIsInvalidThenResponseWithErrorResponseAndStatusCode401()
      throws Exception {
    UserTokenPair request = new UserTokenPair(UUID.randomUUID().toString(), "refresh_token");

    when(refreshTokenProvider.authenticate(any(Authentication.class)))
        .thenThrow(new BadCredentialsException("Bad credentials"));

    mockMvc
        .perform(
            post("/api/v1/authorize/token/revoke")
                .contentType(APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request)))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("status_code").value(HttpStatus.UNAUTHORIZED.value()))
        .andExpect(jsonPath("message").value("Bad credentials"));

    verify(refreshTokenProvider, times(1)).authenticate(any(Authentication.class));
    verify(tokenManager, never()).revokeToken(any(Authentication.class));
  }

  @Test
  @Tag("revokeToken")
  @DisplayName(
      "when calling /token/revoke, expect that refresh token already revoked, then response with ErrorResponse.class and status code 409")
  void whenUserTokenRevokeRequestWithRevokedTokenThenResponseWithErrorResponseAndStatusCode409()
      throws Exception {
    UserTokenPair request = new UserTokenPair(UUID.randomUUID().toString(), "refresh_token");
    var user =
        AuthorizedUser.authorizedUserBuilder("email", "password", List.of())
            .id(UUID.fromString(request.getUserId()))
            .build();
    Authentication authentication =
        new UsernamePasswordAuthenticationToken(user, user.getPassword(), user.getAuthorities());

    when(refreshTokenProvider.authenticate(any(Authentication.class))).thenReturn(authentication);
    doThrow(new RefreshTokenAlreadyRevokedException("Token already revoked"))
        .when(tokenManager)
        .revokeToken(eq(authentication));

    mockMvc
        .perform(
            post("/api/v1/authorize/token/revoke")
                .contentType(APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("status_code").value(HttpStatus.CONFLICT.value()))
        .andExpect(jsonPath("message").value("Token already revoked"));

    verify(refreshTokenProvider, times(1)).authenticate(any(Authentication.class));
    verify(tokenManager, times(1)).revokeToken(eq(authentication));
  }

  @Test
  @DisplayName(
      "when calling /login/resetPassword, expect that user with email from request already exists and mail will be sent with status code 201")
  void shouldReturnMailResetPasswordResponseWithStatusCode200() throws Exception {
    String mail = "a@gmail.com";
    MailResetPasswordRequest mailResetPasswordRequest = new MailResetPasswordRequest(mail);
    UserFullInfo userInfo =
        new UserFullInfo(
            UUID.randomUUID(),
            mail,
            "fullName",
            "telegramId",
            LocalDate.now(),
            "location",
            "avatarUrl");
    when(userService.findUserByEmail(mailResetPasswordRequest.getEmail())).thenReturn(userInfo);

    String token = UUID.randomUUID().toString();
    UserEntity user =
        UserEntity.builder()
            .id(userInfo.getId())
            .email(userInfo.getEmail())
            .fullName(userInfo.getFullName())
            .password("Password1")
            .build();
    PasswordResetToken resetToken = new PasswordResetToken(user, token);
    when(userService.createPasswordResetTokenForUser(userInfo, token)).thenReturn(resetToken);

    mockMvc
        .perform(
            post("/api/v1/authorize/login/resetPassword")
                .contentType(APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(mailResetPasswordRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("user_id").value(userInfo.getId().toString()));
  }

  @Test
  @DisplayName(
      "when calling /login/resetPassword, expect that user with email from request does not exists and status code 404 will be thrown")
  void shouldReturnErrorResponseWithUserNotFoundExceptionWithStatusCode404() throws Exception {
    MailResetPasswordRequest mailResetPasswordRequest = new MailResetPasswordRequest("a@gmail.com");
    when(userService.findUserByEmail(mailResetPasswordRequest.getEmail()))
        .thenThrow(UserNotFoundException.class);

    mockMvc
        .perform(
            post("/api/v1/authorize/login/resetPassword")
                .contentType(APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(mailResetPasswordRequest)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("timestamp").exists())
        .andExpect(jsonPath("status_code").value("404"));
  }

  @Test
  @DisplayName(
      "when calling /login/resetPassword with invalid request body, expect validation failed with status code 400")
  void shouldReturnErrorResponseWithValidationExceptionWithStatusCode400() throws Exception {
    MailResetPasswordRequest mailResetPasswordRequest = new MailResetPasswordRequest("abc123");
    mockMvc
        .perform(
            post("/api/v1/authorize/login/resetPassword")
                .contentType(APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(mailResetPasswordRequest)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("timestamp").exists())
        .andExpect(jsonPath("status_code").value("400"))
        .andExpect(jsonPath("message").value("Validation failed"));

    verify(userService, never()).findUserByEmail(mailResetPasswordRequest.getEmail());
  }
}
