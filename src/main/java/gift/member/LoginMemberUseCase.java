package gift.member;

import gift.auth.TokenResponse;

public interface LoginMemberUseCase {
    TokenResponse execute(MemberRequest request);
}
