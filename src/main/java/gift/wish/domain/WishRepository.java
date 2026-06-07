package gift.wish.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WishRepository extends JpaRepository<Wish, Long> {
    Optional<Wish> findByMemberIdAndProductId(Long memberId, Long productId);
}
