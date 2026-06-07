package gift.member.service.management;

import gift.member.domain.MemberRepository;
import gift.member.usecase.management.DeleteMemberUseCase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeleteMemberService implements DeleteMemberUseCase {
    private final MemberRepository memberRepository;

    public DeleteMemberService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Override
    @Transactional
    public void execute(Long id) {
        memberRepository.deleteById(id);
    }
}
