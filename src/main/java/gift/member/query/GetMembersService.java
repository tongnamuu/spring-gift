package gift.member.query;

import gift.member.usecase.management.GetMembersUseCase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class GetMembersService implements GetMembersUseCase {
    private final MemberQueryDao memberQueryDao;

    public GetMembersService(MemberQueryDao memberQueryDao) {
        this.memberQueryDao = memberQueryDao;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminMemberResponse> execute() {
        return memberQueryDao.findAdminResponses();
    }
}
