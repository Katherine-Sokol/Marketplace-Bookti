package com.teamchallenge.bookti.user;

import static com.teamchallenge.bookti.user.Role.ROLE_USER;
import static com.teamchallenge.bookti.utils.CloudinaryUtils.USERS_FOLDER_NAME;
import static java.text.MessageFormat.format;
import static org.springframework.transaction.annotation.Isolation.REPEATABLE_READ;

import com.teamchallenge.bookti.book.BookRepository;
import com.teamchallenge.bookti.exception.PasswordIsNotMatchesException;
import com.teamchallenge.bookti.exception.PasswordResetTokenNotFoundException;
import com.teamchallenge.bookti.exception.UserAlreadyExistsException;
import com.teamchallenge.bookti.exception.UserNotFoundException;
import com.teamchallenge.bookti.security.AuthorizedUser;
import com.teamchallenge.bookti.utils.AuthorizedUserMapper;
import com.teamchallenge.bookti.utils.CloudinaryUtils;
import com.teamchallenge.bookti.utils.MapperUtils;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * User service.
 *
 * @author Katherine Sokol and Maksym Reva
 */
@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
class UserService {

  private final UserRepository userRepository;
  private final BookRepository bookRepository;
  private final PasswordEncoder passwordEncoder;
  private final PasswordResetTokenRepository passwordTokenRepository;
  private final CloudinaryUtils cloudinaryUtils;
  private final MapperUtils mapperUtils;

  /**
   * Creates a new {@link UserEntity user} and save it into database.
   *
   * @param userDetails the {@link NewUserRegistrationRequest} DTO with basic user information
   * @return {@link AuthorizedUser}
   * @throws PasswordIsNotMatchesException if {@link NewUserRegistrationRequest#getPassword()
   *     password} not matches with {@link NewUserRegistrationRequest#getConfirmPassword()
   *     confirmPassword}
   * @throws UserAlreadyExistsException if user with provided email already exists
   */
  @Transactional
  public AuthorizedUser create(NewUserRegistrationRequest userDetails) {
    if (!userDetails.getPassword().equals(userDetails.getConfirmPassword())) {
      throw new PasswordIsNotMatchesException("Password is not matches");
    }
    if (userRepository.existsUserByEmail(userDetails.getEmail())) {
      throw new UserAlreadyExistsException(
          format("User with email <{0}> already exists", userDetails.getEmail()));
    }
    userDetails.setPassword(passwordEncoder.encode(userDetails.getPassword()));
    UserEntity user = build(userDetails);
    userRepository.save(user);
    return AuthorizedUserMapper.mapFrom(user);
  }

  /**
   * Return information about user.
   *
   * @param id user uuid
   * @return {@link UserFullInfo user} DTO
   * @throws UserNotFoundException if user with provided id not found
   */
  public UserFullInfo findById(UUID id) {
    var user =
        userRepository
            .findUserFullInfoById(id)
            .orElseThrow(
                () -> new UserNotFoundException(format("User with id <{0}> not found.", id)));
    var books = bookRepository.getAllUserBooks(id);
    user.setBooks(new ItemSet<>(books));
    return user;
  }

  /**
   * Return information about user.
   *
   * @param email user uuid
   * @return {@link UserFullInfo user} DTO
   * @throws UserNotFoundException if user with provided id not found
   */
  public UserFullInfo findUserByEmail(String email) {
    UserEntity user =
        userRepository
            .findByEmail(email)
            .orElseThrow(
                () -> new UserNotFoundException(format("User with email <{0}> not found.", email)));
    return UserFullInfo.mapFrom(user);
  }

  @Transactional
  public void changeUserPassword(UUID userId, String newPassword) {
    UserEntity user =
        userRepository
            .findById(userId)
            .orElseThrow(
                () -> new UserNotFoundException(format("User with id <{0}> not found.", userId)));
    user.setPassword(passwordEncoder.encode(newPassword));
    userRepository.save(user);
    passwordTokenRepository.deletePasswordResetTokenByUserId(user);
  }

  @Transactional
  public PasswordResetToken createPasswordResetTokenForUser(UserFullInfo user, String token) {
    UserEntity userEntity =
        userRepository
            .findById(user.getId())
            .orElseThrow(
                () ->
                    new UserNotFoundException(
                        format("User with id <{0}> not found.", user.getId())));
    if (passwordTokenRepository.findByUser(userEntity) != null) {
      passwordTokenRepository.deletePasswordResetTokenByUserId(userEntity);
    }
    PasswordResetToken passwordResetToken = new PasswordResetToken(userEntity, token);
    return passwordTokenRepository.save(passwordResetToken);
  }

  public PasswordResetToken getPasswordResetToken(String token) {
    return passwordTokenRepository
        .findByToken(token)
        .orElseThrow(
            () ->
                new PasswordResetTokenNotFoundException(
                    format("Password reset token <{0}> not found.", token)));
  }

  public UserFullInfo getUserByPasswordResetToken(String passwordResetToken) {
    UserEntity user = getPasswordResetToken(passwordResetToken).getUser();
    return UserFullInfo.mapFrom(user);
  }

  @Transactional(isolation = REPEATABLE_READ)
  public Object updateUserInfo(UUID id, UserUpdateReq userUpdate, MultipartFile file) {
    var user =
        userRepository
            .findById(id)
            .orElseThrow(
                () -> new UserNotFoundException(format("User with id <{0}> not found.", id)));
    if (file != null && !file.isEmpty()) {
      String avatarName = user.getAvatarName();
      if (avatarName == null || avatarName.isBlank()) {
        avatarName = UUID.randomUUID().toString();
        user.setAvatarName(avatarName);
      }
      var avatarUrl = cloudinaryUtils.uploadFile(file, avatarName, USERS_FOLDER_NAME);
      user.setAvatarUrl(avatarUrl);
    }
    mapperUtils.mapUserUpdateToUser(userUpdate, user);
    userRepository.save(user);
    return findById(id);
  }

  /**
   * Builds {@link UserEntity} from {@link NewUserRegistrationRequest}.
   *
   * @param userDetails {@link NewUserRegistrationRequest}
   * @return {@link UserEntity}
   */
  private UserEntity build(NewUserRegistrationRequest userDetails) {
    return UserEntity.builder()
        .fullName(userDetails.getFullName())
        .email(userDetails.getEmail())
        .password(userDetails.getPassword())
        .location(userDetails.getLocation())
        .role(ROLE_USER)
        .build();
  }
}
