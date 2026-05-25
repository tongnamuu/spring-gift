package gift.member;

public interface CreateMemberUseCase {
    Member execute(String email, String password);
}
