package gift.member.controller;

import gift.member.auth.TokenResponse;
import gift.member.usecase.auth.LoginMemberUseCase;
import gift.member.usecase.auth.MemberCredentialsCommand;
import gift.member.usecase.auth.RegisterMemberUseCase;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Handles member registration and login.
 *
 * @author brian.kim
 * @since 1.0
 */
@RestController
@RequestMapping("/api/members")
public class MemberController {
    private final RegisterMemberUseCase registerMemberUseCase;
    private final LoginMemberUseCase loginMemberUseCase;

    @Autowired
    public MemberController(
        RegisterMemberUseCase registerMemberUseCase,
        LoginMemberUseCase loginMemberUseCase
    ) {
        this.registerMemberUseCase = registerMemberUseCase;
        this.loginMemberUseCase = loginMemberUseCase;
    }

    @PostMapping("/register")
    public ResponseEntity<TokenResponse> register(@Valid @RequestBody MemberRequest request) {
        return ResponseEntity.ok(registerMemberUseCase.execute(toCommand(request)));
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody MemberRequest request) {
        return ResponseEntity.ok(loginMemberUseCase.execute(toCommand(request)));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }

    private MemberCredentialsCommand toCommand(MemberRequest request) {
        return new MemberCredentialsCommand(request.email(), request.password());
    }
}
