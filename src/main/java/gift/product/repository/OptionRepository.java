package gift.product.repository;

import gift.product.entity.Option;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OptionRepository extends JpaRepository<Option, Long> {
    List<Option> findByProductId(Long productId);

    boolean existsByProductIdAndName(Long productId, String name);
}
