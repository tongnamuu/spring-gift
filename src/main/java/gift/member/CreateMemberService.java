package gift.member;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateMemberService implements CreateMemberUseCase {
    private static final String DUPLICATE_EMAIL_MESSAGE = "Email is already registered.";

    private final MemberRepository memberRepository;

    public CreateMemberService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Override
    @Transactional
    public Member execute(String email, String password) {
        if (memberRepository.existsByEmail(email)) {
            throw new IllegalArgumentException(DUPLICATE_EMAIL_MESSAGE);
        }

        try {
            return memberRepository.saveAndFlush(new Member(email, password));
        } catch (DataIntegrityViolationException e) {
            if (isDuplicateEmailViolation(e)) {
                throw new IllegalArgumentException(DUPLICATE_EMAIL_MESSAGE, e);
            }
            throw e;
        }
    }

    private boolean isDuplicateEmailViolation(DataIntegrityViolationException e) {
        Throwable cause = e.getMostSpecificCause();
        String message = cause.getMessage();
        return message != null && message.contains("Duplicate entry") && message.contains("email");
    }
}
