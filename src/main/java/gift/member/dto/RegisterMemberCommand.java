package gift.member.dto;

import gift.member.vo.Password;

public record RegisterMemberCommand(String email, Password password) {
}
