package gift.member;

public interface UpdateMemberUseCase {
    Member execute(Long id, String email, String password);
}
