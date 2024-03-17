package com.teamchallenge.bookti.user;

import static com.teamchallenge.bookti.config.SwaggerConfig.USER_UPDATE_REQ_SCHEMA;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;

import com.teamchallenge.bookti.dto.AppResponse;
import com.teamchallenge.bookti.dto.ErrorResponse;
import com.teamchallenge.bookti.exception.PasswordIsNotMatchesException;
import com.teamchallenge.bookti.exception.RefreshTokenAlreadyRevokedException;
import com.teamchallenge.bookti.exception.UserAlreadyExistsException;
import com.teamchallenge.bookti.security.jwt.TokenManager;
import com.teamchallenge.bookti.utils.EmailUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * UserController is RestController witch works with user's information.
 *
 * @author Maksym Reva
 */
@RestController
@Tag(name = "User endpoints")
@RequestMapping("/api/v1")
class UserController {

  private final UserService userService;
  private final TokenManager tokenManager;
  private final AuthenticationManager authenticationManager;
  private final EmailUtils emailUtils;
  private final JwtAuthenticationProvider refreshTokenProvider;

  /**
   * AllArgs Constructor.
   *
   * @param userService {@link UserService}
   * @param tokenManager {@link TokenManager}
   * @param authenticationManager {@link AuthenticationManager}
   * @param emailUtils {@link EmailUtils}
   * @param refreshTokenProvider {@link JwtAuthenticationProvider}
   */
  public UserController(
      UserService userService,
      TokenManager tokenManager,
      AuthenticationManager authenticationManager,
      EmailUtils emailUtils,
      @Qualifier("jwtRefreshTokenAuthenticationProvider")
          JwtAuthenticationProvider refreshTokenProvider) {
    this.userService = userService;
    this.tokenManager = tokenManager;
    this.authenticationManager = authenticationManager;
    this.emailUtils = emailUtils;
    this.refreshTokenProvider = refreshTokenProvider;
  }

  /**
   * Register new user.
   *
   * @param newUserRegistrationRequest {@link NewUserRegistrationRequest request} with information
   *     that user needs to register
   * @return {@link TokenPair}
   * @throws UserAlreadyExistsException if user with {@link NewUserRegistrationRequest#getEmail()
   *     given email} already exists
   * @throws PasswordIsNotMatchesException if {@link NewUserRegistrationRequest#getPassword()
   *     password} does not match {@link NewUserRegistrationRequest#getConfirmPassword()
   *     confirmPassword}
   */
  @Operation(
      summary = "User signup",
      responses = {
        @ApiResponse(
            responseCode = "201",
            description = "User created successfully",
            content = {
              @Content(
                  mediaType = APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = TokenPair.class))
            }),
        @ApiResponse(
            responseCode = "400",
            description = "Bad request or Validation failed",
            content = {
              @Content(
                  mediaType = APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = ErrorResponse.class))
            }),
        @ApiResponse(
            responseCode = "409",
            description = "User already exists",
            content = {
              @Content(
                  mediaType = APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = ErrorResponse.class))
            }),
        @ApiResponse(
            responseCode = "500",
            description = "INTERNAL_SERVER_ERROR",
            content = {
              @Content(
                  mediaType = APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = ErrorResponse.class))
            })
      })
  @PostMapping(path = "/authorize/signup")
  public ResponseEntity<TokenPair> signup(
      @Valid @RequestBody NewUserRegistrationRequest newUserRegistrationRequest)
      throws UserAlreadyExistsException, PasswordIsNotMatchesException {
    var createdUser = userService.create(newUserRegistrationRequest);
    Authentication authentication =
        UsernamePasswordAuthenticationToken.authenticated(
            createdUser, createdUser.getPassword(), createdUser.getAuthorities());
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(tokenManager.generateTokenPair(authentication));
  }

  /**
   * User login.
   *
   * @param userCredentials {@link UserLoginRequest} with information that user needs to log in
   * @return {@link AppResponse}
   * @throws BadCredentialsException if {@link Authentication} has bad credentials
   */
  @Operation(
      summary = "User login",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Login successful",
            content = {
              @Content(
                  mediaType = APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = TokenPair.class))
            }),
        @ApiResponse(
            responseCode = "401",
            description = "Invalid user credentials",
            content = {
              @Content(
                  mediaType = APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = ErrorResponse.class))
            }),
        @ApiResponse(
            responseCode = "500",
            description = "INTERNAL_SERVER_ERROR",
            content = {
              @Content(
                  mediaType = APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = ErrorResponse.class))
            })
      })
  @PostMapping(path = "/authorize/login")
  public ResponseEntity<TokenPair> login(@Valid @RequestBody UserLoginRequest userCredentials)
      throws BadCredentialsException {
    var authentication =
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                userCredentials.getEmail(), userCredentials.getPassword()));
    return ResponseEntity.status(HttpStatus.OK)
        .body(tokenManager.generateTokenPair(authentication));
  }

  /**
   * Gets new access JWT.
   *
   * @param refreshToken {@link UserTokenPair} that need to be refreshed
   * @return {@link TokenPair}
   * @throws BadCredentialsException if {@link Authentication} has bad credentials
   * @throws RefreshTokenAlreadyRevokedException if {@link UserTokenPair#getRefreshToken() refresh
   *     token} already revoked.
   */
  @Operation(
      summary = "Get new access JWT",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "New access and refresh JWT pair generated",
            content = {
              @Content(
                  mediaType = APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = TokenPair.class))
            }),
        @ApiResponse(
            responseCode = "401",
            description = "Provided refresh JWT is <Invalid> or <Expired>",
            content = {
              @Content(
                  mediaType = APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = ErrorResponse.class))
            }),
        @ApiResponse(
            responseCode = "409",
            description = "Provided refresh JWT is <Revoked>",
            content = {
              @Content(
                  mediaType = APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = ErrorResponse.class))
            }),
        @ApiResponse(
            responseCode = "500",
            description = "INTERNAL_SERVER_ERROR",
            content = {
              @Content(
                  mediaType = APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = ErrorResponse.class))
            })
      })
  @PostMapping("/authorize/token/refresh")
  public ResponseEntity<TokenPair> refreshToken(@RequestBody UserTokenPair refreshToken)
      throws BadCredentialsException, RefreshTokenAlreadyRevokedException {
    String token = refreshToken.getRefreshToken();
    if (tokenManager.isRefreshTokenRevoked(token)) {
      throw new RefreshTokenAlreadyRevokedException(
          MessageFormat.format("Token <{0} is revoked>", token));
    }
    var authentication =
        refreshTokenProvider.authenticate(new BearerTokenAuthenticationToken(token));
    return ResponseEntity.status(HttpStatus.OK)
        .body(tokenManager.generateTokenPair(authentication));
  }

  /**
   * Revokes current refresh JWT.
   *
   * @param refreshToken {@link UserTokenPair} that need to be revoked
   * @return {@link AppResponse}
   * @throws BadCredentialsException if {@link Authentication} has bad credentials
   * @throws RefreshTokenAlreadyRevokedException if {@link UserTokenPair#getRefreshToken() refresh
   *     token} already revoked.
   */
  @Operation(
      summary = "Revoke current refresh JWT",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully revoke refresh JWT",
            content = {
              @Content(
                  mediaType = APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = AppResponse.class))
            }),
        @ApiResponse(
            responseCode = "401",
            description = "Provided refresh JWT is <Invalid> or <Expired>",
            content = {
              @Content(
                  mediaType = APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = ErrorResponse.class))
            }),
        @ApiResponse(
            responseCode = "409",
            description = "JWT already revoked",
            content = {
              @Content(
                  mediaType = APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = ErrorResponse.class))
            }),
        @ApiResponse(
            responseCode = "500",
            description = "INTERNAL_SERVER_ERROR",
            content = {
              @Content(
                  mediaType = APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = ErrorResponse.class))
            })
      })
  @PostMapping("/authorize/token/revoke")
  public ResponseEntity<AppResponse> revokeToken(@RequestBody UserTokenPair refreshToken)
      throws BadCredentialsException, RefreshTokenAlreadyRevokedException {
    String token = refreshToken.getRefreshToken();
    var authentication =
        refreshTokenProvider.authenticate(new BearerTokenAuthenticationToken(token));
    tokenManager.revokeToken(authentication);
    var resp =
        new AppResponse(
            HttpStatus.OK.value(), String.format("Token <%s> successfully revoked", token));
    return ResponseEntity.status(HttpStatus.OK).body(resp);
  }

  /**
   * Checks if user with email from {@link MailResetPasswordRequest} exists. If true sends to user
   * email with reset password link.
   *
   * @param mailResetPasswordRequest {@link MailResetPasswordRequest}
   * @return {@link MailResetPasswordResponse}
   */
  @Operation(
      summary = "Send reset password link by email",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Email is sent successfully",
            content = {
              @Content(
                  mediaType = APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = MailResetPasswordResponse.class))
            }),
        @ApiResponse(
            responseCode = "400",
            description = "Bad request or Email was not sent",
            content = {
              @Content(
                  mediaType = APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = ErrorResponse.class))
            }),
        @ApiResponse(
            responseCode = "404",
            description = "User not found",
            content = {
              @Content(
                  mediaType = APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = ErrorResponse.class))
            }),
        @ApiResponse(
            responseCode = "500",
            description = "INTERNAL_SERVER_ERROR",
            content = {
              @Content(
                  mediaType = APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = ErrorResponse.class))
            })
      })
  @PostMapping(path = "/authorize/login/resetPassword")
  public ResponseEntity<MailResetPasswordResponse> sendResetPasswordEmail(
      @Valid @RequestBody MailResetPasswordRequest mailResetPasswordRequest) {
    var user = userService.findUserByEmail(mailResetPasswordRequest.getEmail());
    String token = UUID.randomUUID().toString();
    userService.createPasswordResetTokenForUser(user, token);
    emailUtils.sendResetPasswordEmail(token, user);
    return ResponseEntity.status(HttpStatus.OK)
        .body(new MailResetPasswordResponse(LocalDateTime.now(), String.valueOf(user.getId())));
  }

  /**
   * Checks {@link PasswordResetToken} and changes user's password.
   *
   * @param passwordResetRequest {@link PasswordResetRequest} with information about new password
   *     and user's reset token
   * @return {@link TokenPair}
   */
  @Operation(
      summary = "Reset user password",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Password updated",
            content = {
              @Content(
                  mediaType = APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = TokenPair.class))
            }),
        @ApiResponse(
            responseCode = "400",
            description = "Bad request or Validation failed",
            content = {
              @Content(
                  mediaType = APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = ErrorResponse.class))
            }),
        @ApiResponse(
            responseCode = "404",
            description = "User not found",
            content = {
              @Content(
                  mediaType = APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = ErrorResponse.class))
            }),
        @ApiResponse(
            responseCode = "500",
            description = "INTERNAL_SERVER_ERROR",
            content = {
              @Content(
                  mediaType = APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = ErrorResponse.class))
            })
      })
  @PostMapping(path = "/authorize/login/resetPassword/savePassword")
  public ResponseEntity<TokenPair> resetPassword(
      @Valid @RequestBody PasswordResetRequest passwordResetRequest) {
    var passwordResetToken =
        userService.getPasswordResetToken(passwordResetRequest.getResetToken());
    passwordResetToken.validate(passwordResetToken);
    var user = userService.getUserByPasswordResetToken(passwordResetRequest.getResetToken());
    userService.changeUserPassword(user.getId(), passwordResetRequest.getPassword());
    var authentication =
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                user.getEmail(), passwordResetRequest.getPassword()));
    return ResponseEntity.status(HttpStatus.OK)
        .body(tokenManager.generateTokenPair(authentication));
  }

  /**
   * Returns information about user.
   *
   * @param id user's uuid
   * @return {@link UserFullInfo}
   */
  @Operation(
      summary = "Find User information",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "User information found",
            content = {
              @Content(
                  mediaType = APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = UserFullInfo.class))
            }),
        @ApiResponse(
            responseCode = "401",
            description = "UNAUTHORIZED",
            content = {
              @Content(
                  mediaType = APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = ErrorResponse.class))
            }),
        @ApiResponse(
            responseCode = "403",
            description = "ACCESS_DENIED",
            content = {
              @Content(
                  mediaType = APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = ErrorResponse.class))
            }),
        @ApiResponse(
            responseCode = "404",
            description = "NOT_FOUND",
            content = {
              @Content(
                  mediaType = APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = ErrorResponse.class))
            }),
        @ApiResponse(
            responseCode = "500",
            description = "INTERNAL_SERVER_ERROR",
            content = {
              @Content(
                  mediaType = APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = ErrorResponse.class))
            })
      })
  @GetMapping(path = "/users/{id}")
  @PreAuthorize("isAuthenticated() and hasRole('ROLE_USER') and authentication.principal.id == #id")
  public ResponseEntity<UserFullInfo> getUserInfo(@PathVariable UUID id) {
    return ResponseEntity.status(HttpStatus.OK).body(userService.findById(id));
  }

  @Operation(
      summary = "Update user information",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "User information updated",
            content = {
              @Content(
                  mediaType = APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = UserFullInfo.class))
            }),
        @ApiResponse(
            responseCode = "401",
            description = "UNAUTHORIZED",
            content = {
              @Content(
                  mediaType = APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = ErrorResponse.class))
            }),
        @ApiResponse(
            responseCode = "403",
            description = "ACCESS_DENIED",
            content = {
              @Content(
                  mediaType = APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = ErrorResponse.class))
            }),
        @ApiResponse(
            responseCode = "404",
            description = "NOT_FOUND",
            content = {
              @Content(
                  mediaType = APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = ErrorResponse.class))
            }),
        @ApiResponse(
            responseCode = "500",
            description = "INTERNAL_SERVER_ERROR",
            content = {
              @Content(
                  mediaType = APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = ErrorResponse.class))
            })
      })
  @PatchMapping(
      value = "/users/{id}",
      consumes = {APPLICATION_JSON_VALUE, MULTIPART_FORM_DATA_VALUE},
      produces = APPLICATION_JSON_VALUE)
  @PreAuthorize("isAuthenticated() and hasRole('ROLE_USER') and authentication.principal.id == #id")
  public ResponseEntity<Object> updateUserInfo(
      @PathVariable UUID id,
      @Parameter(description = USER_UPDATE_REQ_SCHEMA, required = true) @RequestPart("user_update")
          UserUpdateReq userUpdateInfo,
      @RequestPart(value = "image", required = false) final MultipartFile image) {
    return ResponseEntity.ok(userService.updateUserInfo(id, userUpdateInfo, image));
  }
}
