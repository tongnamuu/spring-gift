package gift.member.query;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class MemberQueryDao {
    private final JdbcTemplate jdbcTemplate;

    public MemberQueryDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<AdminMemberResponse> findAdminResponses() {
        return jdbcTemplate.query(
            """
                select id, email, point
                from member
                where deleted = false
                order by id asc
                """,
            (resultSet, rowNumber) -> new AdminMemberResponse(
                resultSet.getLong("id"),
                resultSet.getString("email"),
                resultSet.getInt("point")
            )
        );
    }

    public Optional<AdminMemberResponse> findAdminResponseById(Long id) {
        List<AdminMemberResponse> members = jdbcTemplate.query(
            """
                select id, email, point
                from member
                where id = ? and deleted = false
                """,
            (resultSet, rowNumber) -> new AdminMemberResponse(
                resultSet.getLong("id"),
                resultSet.getString("email"),
                resultSet.getInt("point")
            ),
            id
        );
        return members.stream().findFirst();
    }
}
