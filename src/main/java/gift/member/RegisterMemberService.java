package gift.member;

import gift.auth.JwtProvider;
import gift.auth.TokenResponse;
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
    public TokenResponse execute(MemberRequest request) {
        Member member = createMemberUseCase.execute(request.email(), request.password());
        return new TokenResponse(jwtProvider.createToken(member.getEmail()));
    }
}
