package gift.member.query;

import gift.member.usecase.management.GetMemberUseCase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class GetMemberService implements GetMemberUseCase {
    private final MemberQueryDao memberQueryDao;

    public GetMemberService(MemberQueryDao memberQueryDao) {
        this.memberQueryDao = memberQueryDao;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AdminMemberResponse> execute(Long id) {
        return memberQueryDao.findAdminResponseById(id);
    }
}
