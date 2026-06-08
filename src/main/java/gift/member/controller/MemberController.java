package gift.member.controller;

import gift.member.auth.TokenResponse;
import gift.member.dto.LoginMemberCommand;
import gift.member.dto.RegisterMemberCommand;
import gift.member.usecase.auth.LoginMemberUseCase;
import gift.member.usecase.auth.RegisterMemberUseCase;
import gift.member.vo.Password;
import jakarta.validation.Valid;
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

    public MemberController(
        RegisterMemberUseCase registerMemberUseCase,
        LoginMemberUseCase loginMemberUseCase
    ) {
        this.registerMemberUseCase = registerMemberUseCase;
        this.loginMemberUseCase = loginMemberUseCase;
    }

    @PostMapping("/register")
    public ResponseEntity<TokenResponse> register(@Valid @RequestBody MemberRequest request) {
        return ResponseEntity.ok(registerMemberUseCase.execute(toRegisterCommand(request)));
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody MemberRequest request) {
        return ResponseEntity.ok(loginMemberUseCase.execute(toLoginCommand(request)));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }

    private RegisterMemberCommand toRegisterCommand(MemberRequest request) {
        return new RegisterMemberCommand(request.email(), Password.encode(request.password()));
    }

    private LoginMemberCommand toLoginCommand(MemberRequest request) {
        return new LoginMemberCommand(request.email(), request.password());
    }
}
