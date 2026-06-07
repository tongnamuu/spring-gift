package gift.member.service.management;

import gift.member.domain.Member;
import gift.member.domain.MemberRepository;
import gift.member.usecase.management.ChargeMemberPointUseCase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChargeMemberPointService implements ChargeMemberPointUseCase {
    private static final String MEMBER_NOT_FOUND_MESSAGE = "회원이 존재하지 않습니다.";

    private final MemberRepository memberRepository;

    public ChargeMemberPointService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Override
    @Transactional
    public Member execute(Long id, int amount) {
        Member member = memberRepository.findByIdAndDeletedFalse(id)
            .orElseThrow(() -> new IllegalArgumentException(MEMBER_NOT_FOUND_MESSAGE));
        member.chargePoint(amount);
        return memberRepository.save(member);
    }
}
