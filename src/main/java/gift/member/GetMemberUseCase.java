package gift.member;

import java.util.Optional;

public interface GetMemberUseCase {
    Optional<Member> execute(Long id);
}
