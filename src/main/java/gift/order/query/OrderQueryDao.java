package gift.order.query;

import gift.order.controller.OrderResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Repository
public class OrderQueryDao {
    private static final Map<String, String> SORT_COLUMNS = Map.of(
        "id", "o.id",
        "productId", "o.product_id",
        "optionId", "o.option_id",
        "productName", "o.product_name",
        "optionName", "o.option_name",
        "unitPrice", "o.unit_price",
        "totalPrice", "(o.unit_price * o.quantity)",
        "quantity", "o.quantity",
        "orderDateTime", "o.order_date_time"
    );

    private final JdbcTemplate jdbcTemplate;

    public OrderQueryDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Page<OrderResponse> findResponsesByMemberId(Long memberId, Pageable pageable) {
        String query = """
            select o.id,
                   o.product_id,
                   o.option_id,
                   o.product_name,
                   o.option_name,
                   o.unit_price,
                   (o.unit_price * o.quantity) as total_price,
                   o.product_image_url,
                   o.quantity,
                   o.order_date_time,
                   o.message
            from orders o
            where o.member_id = ?
            %s
            %s
            """.formatted(orderBy(pageable), page(pageable));

        List<OrderResponse> content = jdbcTemplate.query(
            query,
            this::toResponse,
            queryArguments(memberId, pageable)
        );

        Long total = jdbcTemplate.queryForObject(
            "select count(*) from orders where member_id = ?",
            Long.class,
            memberId
        );

        return new PageImpl<>(content, pageable, total == null ? 0L : total);
    }

    private OrderResponse toResponse(ResultSet resultSet, int rowNumber) throws SQLException {
        return new OrderResponse(
            resultSet.getLong("id"),
            resultSet.getLong("product_id"),
            resultSet.getLong("option_id"),
            resultSet.getString("product_name"),
            resultSet.getString("option_name"),
            resultSet.getInt("unit_price"),
            resultSet.getInt("total_price"),
            resultSet.getString("product_image_url"),
            resultSet.getInt("quantity"),
            resultSet.getObject("order_date_time", LocalDateTime.class),
            resultSet.getString("message")
        );
    }

    private String page(Pageable pageable) {
        if (pageable.isUnpaged()) {
            return "";
        }
        return "limit ? offset ?";
    }

    private Object[] queryArguments(Long memberId, Pageable pageable) {
        if (pageable.isUnpaged()) {
            return new Object[] {memberId};
        }
        return new Object[] {memberId, pageable.getPageSize(), pageable.getOffset()};
    }

    private String orderBy(Pageable pageable) {
        if (pageable.getSort().isUnsorted()) {
            return "order by o.id asc";
        }

        String clauses = pageable.getSort().stream()
            .map(this::toOrderByClause)
            .reduce((left, right) -> left + ", " + right)
            .orElse("o.id asc");
        return "order by " + clauses;
    }

    private String toOrderByClause(Sort.Order order) {
        String column = SORT_COLUMNS.getOrDefault(order.getProperty(), "o.id");
        return column + (order.isDescending() ? " desc" : " asc");
    }
}
