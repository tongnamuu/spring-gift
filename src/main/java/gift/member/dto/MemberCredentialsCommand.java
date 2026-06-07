package gift.member.dto;

import gift.member.vo.Password;

public record MemberCredentialsCommand(String email, Password password) {
}
