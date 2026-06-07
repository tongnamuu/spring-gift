package gift.member.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link Member} entities.
 *
 * @author brian.kim
 * @since 1.0
 */
public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByEmail(String email);

    Optional<Member> findByEmailAndDeletedFalse(String email);

    Optional<Member> findByIdAndDeletedFalse(Long id);

    List<Member> findByDeletedFalse();

    boolean existsByEmail(String email);
}
