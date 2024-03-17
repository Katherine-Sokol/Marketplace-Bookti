package com.teamchallenge.bookti.book;

import static jakarta.persistence.FetchType.LAZY;

import com.teamchallenge.bookti.user.UserEntity;
import com.teamchallenge.bookti.utils.YearConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Year;
import java.util.Objects;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.proxy.HibernateProxy;

/**
 * Class that describes Book entity.
 *
 * @author MinoUni
 * @version 1.0
 */
@Builder
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "books")
public class Book {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column
  private String title;

  @Column
  private String author;

  @Column
  private String genre;

  @Column(columnDefinition = "TEXT")
  private String description;

  @Column(name = "image_name")
  private String imageName;

  @Column(name = "image_url")
  private String imageUrl;

  @Column
  private String language;

  @Column(name = "publication_date", columnDefinition = "smallint")
  @Convert(converter = YearConverter.class)
  private Year publicationDate;

  @Column(name = "trade_format")
  private String tradeFormat;

  @ToString.Exclude
  @ManyToOne(fetch = LAZY, optional = false)
  private UserEntity owner;

  // todo: Add 'comments' relationship 1:n

  @Override
  public final boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null) {
      return false;
    }
    Class<?> objEffectiveClass =
        o instanceof HibernateProxy
            ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass()
            : o.getClass();
    Class<?> thisEffectiveClass =
        this instanceof HibernateProxy
            ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass()
            : this.getClass();
    if (thisEffectiveClass != objEffectiveClass) {
      return false;
    }
    Book book = (Book) o;
    return getId() != null && Objects.equals(getId(), book.getId());
  }

  @Override
  public final int hashCode() {
    return this instanceof HibernateProxy
        ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode()
        : getClass().hashCode();
  }
}
