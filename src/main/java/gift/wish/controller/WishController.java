package gift.wish.controller;

import gift.member.auth.AuthenticationResolver;
import gift.wish.service.WishAccessDeniedException;
import gift.wish.usecase.AddWishUseCase;
import gift.wish.usecase.GetWishesUseCase;
import gift.wish.usecase.RemoveWishUseCase;
import gift.wish.dto.WishCommand;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/wishes")
public class WishController {
    private final AuthenticationResolver authenticationResolver;
    private final GetWishesUseCase getWishesUseCase;
    private final AddWishUseCase addWishUseCase;
    private final RemoveWishUseCase removeWishUseCase;

    public WishController(
        AuthenticationResolver authenticationResolver,
        GetWishesUseCase getWishesUseCase,
        AddWishUseCase addWishUseCase,
        RemoveWishUseCase removeWishUseCase
    ) {
        this.authenticationResolver = authenticationResolver;
        this.getWishesUseCase = getWishesUseCase;
        this.addWishUseCase = addWishUseCase;
        this.removeWishUseCase = removeWishUseCase;
    }

    @GetMapping
    public ResponseEntity<Page<WishResponse>> getWishes(
        @RequestHeader(value = "Authorization", required = false) String authorization,
        Pageable pageable
    ) {
        var member = authenticationResolver.extractMember(authorization);
        if (member == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(getWishesUseCase.execute(member.id(), pageable));
    }

    @PostMapping
    public ResponseEntity<WishResponse> addWish(
        @RequestHeader(value = "Authorization", required = false) String authorization,
        @Valid @RequestBody WishRequest request
    ) {
        var member = authenticationResolver.extractMember(authorization);
        if (member == null) {
            return ResponseEntity.status(401).build();
        }

        var result = addWishUseCase.execute(member.id(), toCommand(request));
        return ResponseEntity.ok(result.response());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> removeWish(
        @RequestHeader(value = "Authorization", required = false) String authorization,
        @PathVariable Long id
    ) {
        var member = authenticationResolver.extractMember(authorization);
        if (member == null) {
            return ResponseEntity.status(401).build();
        }

        removeWishUseCase.execute(member.id(), id);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Void> handleNoSuchElement() {
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }

    @ExceptionHandler(WishAccessDeniedException.class)
    public ResponseEntity<Void> handleWishAccessDenied() {
        return ResponseEntity.status(403).build();
    }

    private WishCommand toCommand(WishRequest request) {
        return new WishCommand(request.productId());
    }
}
