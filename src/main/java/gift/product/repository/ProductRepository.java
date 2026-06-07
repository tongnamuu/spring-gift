package gift.product.repository;

import gift.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    @Query("select product from Product product where product.id = (select option.product.id from Option option where option.id = :optionId)")
    Optional<Product> findByOptionId(@Param("optionId") Long optionId);
}
