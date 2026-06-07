package gift.member.usecase.management;

import gift.member.domain.Member;
import gift.member.vo.Password;

public interface UpdateMemberUseCase {
    Member execute(Long id, String email, Password password);
}
