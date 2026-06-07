package gift.member.usecase.auth;

import gift.member.auth.TokenResponse;
import gift.member.dto.MemberCredentialsCommand;

public interface LoginMemberUseCase {
    TokenResponse execute(MemberCredentialsCommand command);
}
