package gift.member.vo;

public record PointAmount(int value) {
    public PointAmount {
        if (value <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero.");
        }
    }
}
