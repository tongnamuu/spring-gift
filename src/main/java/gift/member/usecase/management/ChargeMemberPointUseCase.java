package gift.member.usecase.management;

import gift.member.domain.Member;

public interface ChargeMemberPointUseCase {
    Member execute(Long id, int amount);
}
