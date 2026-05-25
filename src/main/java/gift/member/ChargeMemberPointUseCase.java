package gift.member;

public interface ChargeMemberPointUseCase {
    Member execute(Long id, int amount);
}
