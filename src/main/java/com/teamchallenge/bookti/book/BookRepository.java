package com.teamchallenge.bookti.book;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Book repository.
 *
 * @author MinoUni
 * @version 1.0
 */
@Repository
public interface BookRepository extends JpaRepository<Book, UUID> {

  @Query(
      """
        SELECT new com.teamchallenge.bookti.book.BookDetails(
          b.id, b.title, b.author, b.genre, b.publicationDate,
          b. language, b.tradeFormat, b.imageUrl, b.description)
        FROM Book b
        WHERE b.id = :id
      """)
  Optional<BookDetails> findBookById(@Param("id") UUID id);

  @Query(
      """
        SELECT new com.teamchallenge.bookti.book.BookDetails(
          b.id, b.title, b.author, b.genre, b.publicationDate,
        b. language, b.tradeFormat, b.imageUrl, b.description)
        FROM Book b
      """)
  Page<BookDetails> findAllByPageable(Pageable pageable);

  @Query(
      """
        SELECT new com.teamchallenge.bookti.book.BookShortDetails(
          b.id, b.title, b.author, b.language, b.imageUrl)
        FROM Book b
        INNER JOIN b.owner u
        WHERE u.id = :user_id
      """)
  Set<BookShortDetails> getAllUserBooks(@Param("user_id") UUID userId);
}
