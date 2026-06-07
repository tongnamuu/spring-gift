package gift.member.query;

import gift.member.auth.AuthenticatedMember;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class AuthenticationQueryDao {
    private final JdbcTemplate jdbcTemplate;

    public AuthenticationQueryDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<AuthenticatedMember> findAuthenticatedMemberByEmail(String email) {
        List<AuthenticatedMember> members = jdbcTemplate.query(
            """
                select id, email
                from member
                where email = ? and deleted = false
                """,
            (resultSet, rowNumber) -> new AuthenticatedMember(
                resultSet.getLong("id"),
                resultSet.getString("email")
            ),
            email
        );
        return members.stream().findFirst();
    }
}
