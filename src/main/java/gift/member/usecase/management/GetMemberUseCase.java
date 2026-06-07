package gift.member.usecase.management;

import gift.member.domain.Member;

import java.util.Optional;

public interface GetMemberUseCase {
    Optional<Member> execute(Long id);
}
