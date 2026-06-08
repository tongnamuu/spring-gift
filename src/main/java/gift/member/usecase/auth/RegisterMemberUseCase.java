package gift.member.usecase.auth;

import gift.member.auth.TokenResponse;
import gift.member.dto.RegisterMemberCommand;

public interface RegisterMemberUseCase {
    TokenResponse execute(RegisterMemberCommand command);
}
