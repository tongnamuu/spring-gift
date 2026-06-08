package gift.member.service.auth;

import gift.member.auth.JwtProvider;
import gift.member.auth.TokenResponse;
import gift.member.domain.Member;
import gift.member.domain.MemberRepository;
import gift.member.dto.LoginMemberCommand;
import gift.member.usecase.auth.LoginMemberUseCase;
import gift.member.vo.Password;
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
    public TokenResponse execute(LoginMemberCommand command) {
        Member member = memberRepository.findByEmailAndDeletedFalse(command.email())
            .orElseThrow(() -> new IllegalArgumentException(INVALID_LOGIN_MESSAGE));

        if (member.getPassword() == null || !Password.matches(command.password(), member.getPassword())) {
            throw new IllegalArgumentException(INVALID_LOGIN_MESSAGE);
        }

        return new TokenResponse(jwtProvider.createToken(member.getEmail()));
    }
}
