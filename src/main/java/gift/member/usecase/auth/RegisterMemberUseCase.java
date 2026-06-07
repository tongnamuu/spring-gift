package gift.member.usecase.auth;

import gift.member.auth.TokenResponse;

public interface RegisterMemberUseCase {
    TokenResponse execute(MemberCredentialsCommand command);
}
