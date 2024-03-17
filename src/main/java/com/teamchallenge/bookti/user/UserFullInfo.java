package com.teamchallenge.bookti.user;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.teamchallenge.bookti.book.BookShortDetails;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * UserInfo is DTO class that contains information about user.
 *
 * @author Maksym Reva
 */
@Data
@AllArgsConstructor
@Builder
public class UserFullInfo {

  private UUID id;

  private String email;

  @JsonProperty("full_name")
  private String fullName;

  @JsonProperty("telegram_id")
  private String telegramId;

  @JsonProperty("creation_date")
  @JsonFormat(shape = STRING, pattern = "yyyy-MM-dd")
  private LocalDate creationDate;

  private String location;

  @JsonProperty("avatar_url")
  private String avatarUrl;

  private ItemSet<BookShortDetails> books;

  private ItemSet<BookShortDetails> wishlist;

  private ItemSet<?> reviews;

  /**
   * Constructor to map {@link UserEntity} through {@link UserRepository} using jpql.
   *
   * @param id identifier
   * @param email email address
   * @param fullName name and surname
   * @param telegramId telegram id
   * @param creationDate user account creation date
   * @param location location(city)
   * @param avatarUrl avatar profile image url
   */
  public UserFullInfo(
      UUID id,
      String email,
      String fullName,
      String telegramId,
      LocalDate creationDate,
      String location,
      String avatarUrl) {
    this.id = id;
    this.email = email;
    this.fullName = fullName;
    this.telegramId = telegramId;
    this.creationDate = creationDate;
    this.location = location;
    this.avatarUrl = avatarUrl;
  }

  /**
   * Maps {@link UserFullInfo} from {@link UserEntity}.
   *
   * @param user {@link UserEntity}
   * @return {@link UserFullInfo}
   */
  public static UserFullInfo mapFrom(UserEntity user) {
    return UserFullInfo.builder()
        .id(user.getId())
        .email(user.getEmail())
        .fullName(user.getFullName())
        .telegramId(user.getTelegramId())
        .creationDate(user.getCreationDate())
        .location(user.getLocation())
        .avatarUrl(user.getAvatarUrl())
        .build();
  }
}
