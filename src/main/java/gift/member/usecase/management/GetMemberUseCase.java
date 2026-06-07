package gift.member.usecase.management;

import gift.member.query.AdminMemberResponse;

import java.util.Optional;

public interface GetMemberUseCase {
    Optional<AdminMemberResponse> execute(Long id);
}
