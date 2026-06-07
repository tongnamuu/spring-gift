package gift.product.query;

import gift.product.dto.OptionResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class OptionQueryDao {
    private final JdbcTemplate jdbcTemplate;

    public OptionQueryDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<OptionResponse> findResponsesByProductId(Long productId) {
        return jdbcTemplate.query(
            """
                select id, name, quantity
                from options
                where product_id = ?
                order by id asc
                """,
            (resultSet, rowNumber) -> new OptionResponse(
                resultSet.getLong("id"),
                resultSet.getString("name"),
                resultSet.getInt("quantity")
            ),
            productId
        );
    }
}
