package gift.member.usecase.management;

import gift.member.domain.Member;

import java.util.List;

public interface GetMembersUseCase {
    List<Member> execute();
}
