package gift.member.usecase.auth;

import gift.member.auth.TokenResponse;
import gift.member.dto.LoginMemberCommand;

public interface LoginMemberUseCase {
    TokenResponse execute(LoginMemberCommand command);
}
