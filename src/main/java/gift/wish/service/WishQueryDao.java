package gift.wish.service;

import gift.wish.controller.WishResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class WishQueryDao {
    private static final Map<String, String> SORT_COLUMNS = Map.of(
        "id", "w.id",
        "productId", "w.product_id",
        "name", "p.name",
        "price", "p.price"
    );

    private final JdbcTemplate jdbcTemplate;

    public WishQueryDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Page<WishResponse> findResponsesByMemberId(Long memberId, Pageable pageable) {
        String query = """
            select w.id, w.product_id, p.name, p.price, p.image_url
            from wish w
            inner join product p on p.id = w.product_id
            where w.member_id = ?
            %s
            limit ? offset ?
            """.formatted(orderBy(pageable));

        List<WishResponse> content = jdbcTemplate.query(
            query,
            (resultSet, rowNumber) -> new WishResponse(
                resultSet.getLong("id"),
                resultSet.getLong("product_id"),
                resultSet.getString("name"),
                resultSet.getInt("price"),
                resultSet.getString("image_url")
            ),
            memberId,
            pageable.getPageSize(),
            pageable.getOffset()
        );

        Long total = jdbcTemplate.queryForObject(
            """
                select count(*)
                from wish w
                inner join product p on p.id = w.product_id
                where w.member_id = ?
                """,
            Long.class,
            memberId
        );

        return new PageImpl<>(content, pageable, total == null ? 0L : total);
    }

    private String orderBy(Pageable pageable) {
        if (pageable.getSort().isUnsorted()) {
            return "order by w.id asc";
        }

        String clauses = pageable.getSort().stream()
            .map(this::toOrderByClause)
            .reduce((left, right) -> left + ", " + right)
            .orElse("w.id asc");
        return "order by " + clauses;
    }

    private String toOrderByClause(Sort.Order order) {
        String column = SORT_COLUMNS.getOrDefault(order.getProperty(), "w.id");
        return column + (order.isDescending() ? " desc" : " asc");
    }
}
