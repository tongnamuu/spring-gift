package gift.member.usecase.management;

import gift.member.domain.Member;
import gift.member.vo.Password;

public interface CreateMemberUseCase {
    Member execute(String email, Password password);
}
