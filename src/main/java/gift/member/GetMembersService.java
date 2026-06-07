package gift.member;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class GetMembersService implements GetMembersUseCase {
    private final MemberRepository memberRepository;

    public GetMembersService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Member> execute() {
        return memberRepository.findAll();
    }
}
