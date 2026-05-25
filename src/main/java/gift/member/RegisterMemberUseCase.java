package gift.member;

import gift.auth.TokenResponse;

public interface RegisterMemberUseCase {
    TokenResponse execute(MemberRequest request);
}
