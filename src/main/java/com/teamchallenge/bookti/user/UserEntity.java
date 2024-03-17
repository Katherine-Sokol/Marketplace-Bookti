package com.teamchallenge.bookti.user;

import com.teamchallenge.bookti.book.Book;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.proxy.HibernateProxy;

/**
 * UserEntity is Entity Class witch contains information about user from database.
 *
 * @author Maksym Reva
 */
@Builder
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class UserEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "full_name", nullable = false)
  private String fullName;

  @Column(nullable = false, unique = true)
  private String email;

  @Column(nullable = false)
  private String password;

  @Builder.Default
  @Column(name = "creation_date", columnDefinition = "DATE")
  private LocalDate creationDate = LocalDate.now();

  @Column(name = "avatar_name")
  private String avatarName;

  @Column(name = "avatar_url")
  private String avatarUrl;

  @Column(nullable = false)
  private String location;

  @Column(name = "telegram_id", length = 32)
  private String telegramId;

  @Column
  @Enumerated(EnumType.STRING)
  private Role role;

  @ToString.Exclude
  @OneToMany(mappedBy = "owner")
  private Set<Book> books;

  @Override
  public final boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    Class<?> objEffectiveClass =
        obj instanceof HibernateProxy
            ? ((HibernateProxy) obj).getHibernateLazyInitializer().getPersistentClass()
            : obj.getClass();
    Class<?> thisEffectiveClass =
        this instanceof HibernateProxy
            ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass()
            : this.getClass();
    if (thisEffectiveClass != objEffectiveClass) {
      return false;
    }
    UserEntity that = (UserEntity) obj;
    return getId() != null && Objects.equals(getId(), that.getId());
  }

  @Override
  public final int hashCode() {
    return this instanceof HibernateProxy
        ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode()
        : getClass().hashCode();
  }
}
