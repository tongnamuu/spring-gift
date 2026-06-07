package gift.member.usecase.management;

import gift.member.domain.Member;

public interface UpdateMemberUseCase {
    Member execute(Long id, String email, String password);
}
