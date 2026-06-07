package gift.member.usecase.management;

import gift.member.domain.Member;

public interface CreateMemberUseCase {
    Member execute(String email, String password);
}
