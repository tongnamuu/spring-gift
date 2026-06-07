package gift.member;

import gift.auth.JwtProvider;
import gift.auth.TokenResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoginMemberService implements LoginMemberUseCase {
    private static final String INVALID_LOGIN_MESSAGE = "Invalid email or password.";

    private final MemberRepository memberRepository;
    private final JwtProvider jwtProvider;

    public LoginMemberService(MemberRepository memberRepository, JwtProvider jwtProvider) {
        this.memberRepository = memberRepository;
        this.jwtProvider = jwtProvider;
    }

    @Override
    @Transactional(readOnly = true)
    public TokenResponse execute(MemberRequest request) {
        Member member = memberRepository.findByEmail(request.email())
            .orElseThrow(() -> new IllegalArgumentException(INVALID_LOGIN_MESSAGE));

        if (member.getPassword() == null || !member.getPassword().equals(request.password())) {
            throw new IllegalArgumentException(INVALID_LOGIN_MESSAGE);
        }

        return new TokenResponse(jwtProvider.createToken(member.getEmail()));
    }
}
