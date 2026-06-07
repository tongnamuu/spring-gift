package gift.member.service.management;

import gift.member.domain.Member;
import gift.member.domain.MemberRepository;
import gift.member.usecase.management.DeleteMemberUseCase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeleteMemberService implements DeleteMemberUseCase {
    private static final String MEMBER_NOT_FOUND_MESSAGE = "회원이 존재하지 않습니다.";

    private final MemberRepository memberRepository;

    public DeleteMemberService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Override
    @Transactional
    public void execute(Long id) {
        Member member = memberRepository.findByIdAndDeletedFalse(id)
            .orElseThrow(() -> new IllegalArgumentException(MEMBER_NOT_FOUND_MESSAGE));
        member.markDeleted();
        memberRepository.save(member);
    }
}
