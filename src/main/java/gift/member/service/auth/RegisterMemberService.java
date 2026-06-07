package gift.member.service.auth;

import gift.member.auth.JwtProvider;
import gift.member.auth.TokenResponse;
import gift.member.domain.Member;
import gift.member.usecase.management.CreateMemberUseCase;
import gift.member.dto.MemberCredentialsCommand;
import gift.member.usecase.auth.RegisterMemberUseCase;
import org.springframework.stereotype.Service;

@Service
public class RegisterMemberService implements RegisterMemberUseCase {
    private final CreateMemberUseCase createMemberUseCase;
    private final JwtProvider jwtProvider;

    public RegisterMemberService(CreateMemberUseCase createMemberUseCase, JwtProvider jwtProvider) {
        this.createMemberUseCase = createMemberUseCase;
        this.jwtProvider = jwtProvider;
    }

    @Override
    public TokenResponse execute(MemberCredentialsCommand command) {
        Member member = createMemberUseCase.execute(command.email(), command.password());
        return new TokenResponse(jwtProvider.createToken(member.getEmail()));
    }
}
