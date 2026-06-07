package gift.member.service.management;

import gift.member.domain.Member;
import gift.member.domain.MemberRepository;
import gift.member.usecase.management.UpdateMemberUseCase;
import gift.member.vo.Password;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdateMemberService implements UpdateMemberUseCase {
    private static final String MEMBER_NOT_FOUND_MESSAGE = "회원이 존재하지 않습니다.";

    private final MemberRepository memberRepository;

    public UpdateMemberService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Override
    @Transactional
    public Member execute(Long id, String email, Password password) {
        Member member = memberRepository.findByIdAndDeletedFalse(id)
            .orElseThrow(() -> new IllegalArgumentException(MEMBER_NOT_FOUND_MESSAGE));
        member.update(email, password);
        return memberRepository.save(member);
    }
}
