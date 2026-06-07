package gift.member;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class GetMemberService implements GetMemberUseCase {
    private final MemberRepository memberRepository;

    public GetMemberService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Member> execute(Long id) {
        return memberRepository.findById(id);
    }
}
