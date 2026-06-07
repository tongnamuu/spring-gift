package gift.member.usecase.auth;

import gift.member.auth.TokenResponse;

public interface LoginMemberUseCase {
    TokenResponse execute(MemberCredentialsCommand command);
}
