package gift.member.usecase.auth;

import gift.member.auth.TokenResponse;
import gift.member.dto.MemberCredentialsCommand;

public interface RegisterMemberUseCase {
    TokenResponse execute(MemberCredentialsCommand command);
}
