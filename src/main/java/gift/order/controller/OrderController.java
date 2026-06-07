package gift.order.controller;

import gift.auth.AuthenticationResolver;
import gift.order.usecase.CreateOrderUseCase;
import gift.order.usecase.GetOrdersUseCase;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final GetOrdersUseCase getOrdersUseCase;
    private final CreateOrderUseCase createOrderUseCase;
    private final AuthenticationResolver authenticationResolver;

    public OrderController(
        GetOrdersUseCase getOrdersUseCase,
        CreateOrderUseCase createOrderUseCase,
        AuthenticationResolver authenticationResolver
    ) {
        this.getOrdersUseCase = getOrdersUseCase;
        this.createOrderUseCase = createOrderUseCase;
        this.authenticationResolver = authenticationResolver;
    }

    @GetMapping
    public ResponseEntity<?> getOrders(
        @RequestHeader("Authorization") String authorization,
        Pageable pageable
    ) {
        // auth check
        var member = authenticationResolver.extractMember(authorization);
        if (member == null) {
            return ResponseEntity.status(401).build();
        }
        var orders = getOrdersUseCase.execute(member.getId(), pageable);
        return ResponseEntity.ok(orders);
    }

    @PostMapping
    public ResponseEntity<?> createOrder(
        @RequestHeader("Authorization") String authorization,
        @Valid @RequestBody OrderRequest request
    ) {
        // auth check
        var member = authenticationResolver.extractMember(authorization);
        if (member == null) {
            return ResponseEntity.status(401).build();
        }

        OrderResponse response = createOrderUseCase.execute(member.getId(), request);

        return ResponseEntity.created(URI.create("/api/orders/" + response.id()))
            .body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Void> handleNoSuchElement() {
        return ResponseEntity.notFound().build();
    }
}
