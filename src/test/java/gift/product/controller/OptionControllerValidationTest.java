package gift.product.controller;

import gift.product.dto.OptionRequest;
import gift.product.dto.OptionResponse;
import gift.product.usecase.CreateOptionUseCase;
import gift.product.dto.OptionCommand;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OptionControllerValidationTest {
    @Test
    void createOptionDoesNotCallUseCaseWhenOptionNameIsInvalid() {
        RecordingCreateOptionUseCase createOptionUseCase = new RecordingCreateOptionUseCase();
        OptionController controller = new OptionController(
            productId -> Optional.of(List.of()),
            createOptionUseCase,
            (productId, optionId) -> {
            }
        );
        OptionRequest request = new OptionRequest("옵션!", 10);

        assertThatThrownBy(() -> controller.createOption(1L, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("옵션 이름에 허용되지 않는 특수 문자가 포함되어 있습니다. 사용 가능: ( ), [ ], +, -, &, /, _");

        assertThat(createOptionUseCase.called()).isFalse();
    }

    private static class RecordingCreateOptionUseCase implements CreateOptionUseCase {
        private boolean called;

        @Override
        public OptionResponse execute(Long productId, OptionCommand command) {
            called = true;
            return new OptionResponse(1L, command.name().value(), command.quantity());
        }

        private boolean called() {
            return called;
        }
    }
}
