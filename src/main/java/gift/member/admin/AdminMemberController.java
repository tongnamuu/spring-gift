package gift.member.admin;

import gift.member.query.AdminMemberResponse;
import gift.member.usecase.management.ChargeMemberPointUseCase;
import gift.member.usecase.management.CreateMemberUseCase;
import gift.member.usecase.management.DeleteMemberUseCase;
import gift.member.usecase.management.GetMemberUseCase;
import gift.member.usecase.management.GetMembersUseCase;
import gift.member.usecase.management.UpdateMemberUseCase;
import gift.member.vo.Password;
import gift.member.vo.PointAmount;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Admin controller for managing members.
 *
 * @author brian.kim
 * @since 1.0
 */
@Controller
@RequestMapping("/admin/members")
public class AdminMemberController {
    private static final String MEMBER_NOT_FOUND_MESSAGE = "회원이 존재하지 않습니다.";

    private final GetMembersUseCase getMembersUseCase;
    private final GetMemberUseCase getMemberUseCase;
    private final CreateMemberUseCase createMemberUseCase;
    private final UpdateMemberUseCase updateMemberUseCase;
    private final DeleteMemberUseCase deleteMemberUseCase;
    private final ChargeMemberPointUseCase chargeMemberPointUseCase;

    public AdminMemberController(
        GetMembersUseCase getMembersUseCase,
        GetMemberUseCase getMemberUseCase,
        CreateMemberUseCase createMemberUseCase,
        UpdateMemberUseCase updateMemberUseCase,
        DeleteMemberUseCase deleteMemberUseCase,
        ChargeMemberPointUseCase chargeMemberPointUseCase
    ) {
        this.getMembersUseCase = getMembersUseCase;
        this.getMemberUseCase = getMemberUseCase;
        this.createMemberUseCase = createMemberUseCase;
        this.updateMemberUseCase = updateMemberUseCase;
        this.deleteMemberUseCase = deleteMemberUseCase;
        this.chargeMemberPointUseCase = chargeMemberPointUseCase;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("members", getMembersUseCase.execute());
        return "member/list";
    }

    @GetMapping("/new")
    public String newForm() {
        return "member/new";
    }

    @PostMapping
    public String create(
        @RequestParam String email,
        @RequestParam String password,
        Model model
    ) {
        try {
            createMemberUseCase.execute(email, Password.encode(password));
        } catch (IllegalArgumentException e) {
            populateNewFormError(model, email, e.getMessage());
            return "member/new";
        }

        return "redirect:/admin/members";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        final AdminMemberResponse member = getMemberUseCase.execute(id)
            .orElseThrow(() -> new IllegalArgumentException(MEMBER_NOT_FOUND_MESSAGE));
        model.addAttribute("member", member);
        return "member/edit";
    }

    @PostMapping("/{id}/edit")
    public String update(
        @PathVariable Long id,
        @RequestParam String email,
        @RequestParam String password,
        Model model
    ) {
        try {
            updateMemberUseCase.execute(id, email, Password.encode(password));
        } catch (IllegalArgumentException e) {
            populateEditFormError(model, id, email, e.getMessage());
            return "member/edit";
        }

        return "redirect:/admin/members";
    }

    @PostMapping("/{id}/charge-point")
    public String chargePoint(
        @PathVariable Long id,
        @RequestParam int amount,
        Model model
    ) {
        try {
            chargeMemberPointUseCase.execute(id, new PointAmount(amount));
        } catch (IllegalArgumentException e) {
            populateListError(model, e.getMessage());
            return "member/list";
        }
        return "redirect:/admin/members";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        deleteMemberUseCase.execute(id);
        return "redirect:/admin/members";
    }

    private void populateNewFormError(Model model, String email, String error) {
        model.addAttribute("error", error);
        model.addAttribute("email", email);
    }

    private void populateListError(Model model, String error) {
        model.addAttribute("members", getMembersUseCase.execute());
        model.addAttribute("error", error);
    }

    private void populateEditFormError(Model model, Long id, String email, String error) {
        final AdminMemberResponse member = getMemberUseCase.execute(id)
            .orElseThrow(() -> new IllegalArgumentException(MEMBER_NOT_FOUND_MESSAGE));
        model.addAttribute("member", member);
        model.addAttribute("email", email);
        model.addAttribute("error", error);
    }
}
