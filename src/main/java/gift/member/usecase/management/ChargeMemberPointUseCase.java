package gift.member.usecase.management;

import gift.member.domain.Member;
import gift.member.vo.PointAmount;

public interface ChargeMemberPointUseCase {
    Member execute(Long id, PointAmount amount);
}
