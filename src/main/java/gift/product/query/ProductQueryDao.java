package gift.product.query;

import gift.product.dto.OptionResponse;
import gift.product.dto.ProductResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class ProductQueryDao {
    private static final Map<String, String> SORT_COLUMNS = Map.of(
        "id", "p.id",
        "name", "p.name",
        "price", "p.price",
        "imageUrl", "p.image_url",
        "categoryId", "p.category_id"
    );

    private final JdbcTemplate jdbcTemplate;

    public ProductQueryDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<ProductResponse> findResponseById(Long id) {
        List<ProductRow> products = jdbcTemplate.query(
            """
                select p.id, p.name, p.price, p.image_url, p.category_id
                from product p
                where p.id = ?
                """,
            (resultSet, rowNumber) -> new ProductRow(
                resultSet.getLong("id"),
                resultSet.getString("name"),
                resultSet.getInt("price"),
                resultSet.getString("image_url"),
                resultSet.getLong("category_id")
            ),
            id
        );
        if (products.isEmpty()) {
            return Optional.empty();
        }
        Map<Long, List<OptionResponse>> optionsByProductId = findOptionsByProductIds(List.of(id));
        return Optional.of(toResponse(products.get(0), optionsByProductId));
    }

    public boolean existsById(Long id) {
        Boolean exists = jdbcTemplate.queryForObject(
            "select exists(select 1 from product where id = ?)",
            Boolean.class,
            id
        );
        return Boolean.TRUE.equals(exists);
    }

    public Page<ProductResponse> findResponses(Pageable pageable) {
        String query = """
            select p.id, p.name, p.price, p.image_url, p.category_id
            from product p
            %s
            %s
            """.formatted(orderBy(pageable), page(pageable));

        List<ProductRow> products = jdbcTemplate.query(
            query,
            (resultSet, rowNumber) -> new ProductRow(
                resultSet.getLong("id"),
                resultSet.getString("name"),
                resultSet.getInt("price"),
                resultSet.getString("image_url"),
                resultSet.getLong("category_id")
            ),
            queryArguments(pageable)
        );
        Map<Long, List<OptionResponse>> optionsByProductId = findOptionsByProductIds(
            products.stream()
                .map(ProductRow::id)
                .toList()
        );
        List<ProductResponse> content = products.stream()
            .map(product -> toResponse(product, optionsByProductId))
            .toList();
        Long total = jdbcTemplate.queryForObject("select count(*) from product", Long.class);

        return new PageImpl<>(content, pageable, total == null ? 0L : total);
    }

    private Map<Long, List<OptionResponse>> findOptionsByProductIds(List<Long> productIds) {
        if (productIds.isEmpty()) {
            return Collections.emptyMap();
        }
        String placeholders = productIds.stream()
            .map(ignored -> "?")
            .collect(Collectors.joining(", "));
        List<OptionRow> options = jdbcTemplate.query(
            """
                select o.product_id, o.id, o.name, o.quantity
                from options o
                where o.product_id in (%s)
                order by o.product_id asc, o.id asc
                """.formatted(placeholders),
            (resultSet, rowNumber) -> new OptionRow(
                resultSet.getLong("product_id"),
                new OptionResponse(
                    resultSet.getLong("id"),
                    resultSet.getString("name"),
                    resultSet.getInt("quantity")
                )
            ),
            productIds.toArray()
        );

        return options.stream()
            .collect(Collectors.groupingBy(
                OptionRow::productId,
                Collectors.mapping(OptionRow::response, Collectors.toCollection(ArrayList::new))
            ));
    }

    private ProductResponse toResponse(ProductRow product, Map<Long, List<OptionResponse>> optionsByProductId) {
        return new ProductResponse(
            product.id(),
            product.name(),
            product.price(),
            product.imageUrl(),
            product.categoryId(),
            optionsByProductId.getOrDefault(product.id(), List.of())
        );
    }

    private String page(Pageable pageable) {
        if (pageable.isUnpaged()) {
            return "";
        }
        return "limit ? offset ?";
    }

    private Object[] queryArguments(Pageable pageable) {
        if (pageable.isUnpaged()) {
            return new Object[] {};
        }
        return new Object[] {pageable.getPageSize(), pageable.getOffset()};
    }

    private String orderBy(Pageable pageable) {
        if (pageable.getSort().isUnsorted()) {
            return "order by p.id asc";
        }

        String clauses = pageable.getSort().stream()
            .map(this::toOrderByClause)
            .reduce((left, right) -> left + ", " + right)
            .orElse("p.id asc");
        return "order by " + clauses;
    }

    private String toOrderByClause(Sort.Order order) {
        String column = SORT_COLUMNS.getOrDefault(order.getProperty(), "p.id");
        return column + (order.isDescending() ? " desc" : " asc");
    }

    private record ProductRow(Long id, String name, int price, String imageUrl, Long categoryId) {
    }

    private record OptionRow(Long productId, OptionResponse response) {
    }
}
