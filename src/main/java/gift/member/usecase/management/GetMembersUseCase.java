package gift.member.usecase.management;

import gift.member.query.AdminMemberResponse;

import java.util.List;

public interface GetMembersUseCase {
    List<AdminMemberResponse> execute();
}
