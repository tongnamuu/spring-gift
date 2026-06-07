package gift.member.service.auth;

import gift.member.auth.JwtProvider;
import gift.member.auth.KakaoLoginClient;
import gift.member.auth.TokenResponse;
import gift.member.domain.Member;
import gift.member.domain.MemberRepository;
import gift.member.dto.KakaoAuthorizationCodeCommand;
import gift.member.usecase.auth.LoginWithKakaoUseCase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoginWithKakaoService implements LoginWithKakaoUseCase {
    private static final String DELETED_MEMBER_MESSAGE = "회원이 존재하지 않습니다.";

    private final KakaoLoginClient kakaoLoginClient;
    private final MemberRepository memberRepository;
    private final JwtProvider jwtProvider;

    public LoginWithKakaoService(
        KakaoLoginClient kakaoLoginClient,
        MemberRepository memberRepository,
        JwtProvider jwtProvider
    ) {
        this.kakaoLoginClient = kakaoLoginClient;
        this.memberRepository = memberRepository;
        this.jwtProvider = jwtProvider;
    }

    @Override
    @Transactional
    public TokenResponse execute(KakaoAuthorizationCodeCommand command) {
        KakaoLoginClient.KakaoTokenResponse kakaoToken = kakaoLoginClient.requestAccessToken(command.code());
        KakaoLoginClient.KakaoUserResponse kakaoUser = kakaoLoginClient.requestUserInfo(kakaoToken.accessToken());
        String email = kakaoUser.email();

        Member member = memberRepository.findByEmail(email)
            .map(this::rejectDeletedMember)
            .orElseGet(() -> new Member(email));
        member.updateKakaoAccessToken(kakaoToken.accessToken());
        Member saved = memberRepository.save(member);

        return new TokenResponse(jwtProvider.createToken(saved.getEmail()));
    }

    private Member rejectDeletedMember(Member member) {
        if (member.isDeleted()) {
            throw new IllegalArgumentException(DELETED_MEMBER_MESSAGE);
        }
        return member;
    }
}
