package gift.category.query;

import gift.category.controller.CategoryResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class CategoryQueryDao {
    private final JdbcTemplate jdbcTemplate;

    public CategoryQueryDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<CategoryResponse> findResponses() {
        return jdbcTemplate.query(
            """
                select id, name, color, image_url, description
                from category
                order by id asc
                """,
            (resultSet, rowNumber) -> new CategoryResponse(
                resultSet.getLong("id"),
                resultSet.getString("name"),
                resultSet.getString("color"),
                resultSet.getString("image_url"),
                resultSet.getString("description")
            )
        );
    }
}
