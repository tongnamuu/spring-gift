package gift.member.auth;

import gift.member.query.AuthenticationQueryDao;
import org.springframework.stereotype.Component;

/**
 * Resolves the authenticated member from an Authorization header.
 *
 * @author brian.kim
 * @since 1.0
 */
@Component
public class AuthenticationResolver {
    private final JwtProvider jwtProvider;
    private final AuthenticationQueryDao authenticationQueryDao;

    public AuthenticationResolver(JwtProvider jwtProvider, AuthenticationQueryDao authenticationQueryDao) {
        this.jwtProvider = jwtProvider;
        this.authenticationQueryDao = authenticationQueryDao;
    }

    public AuthenticatedMember extractMember(String authorization) {
        try {
            final String token = authorization.replace("Bearer ", "");
            final String email = jwtProvider.getEmail(token);
            return authenticationQueryDao.findAuthenticatedMemberByEmail(email).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }
}
