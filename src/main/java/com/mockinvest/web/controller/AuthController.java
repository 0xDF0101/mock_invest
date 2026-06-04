package com.mockinvest.web.controller;

import com.mockinvest.domain.user.PasswordResetService;
import com.mockinvest.domain.user.UserService;
import com.mockinvest.web.dto.RegisterRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final PasswordResetService passwordResetService;

    @GetMapping("/login")
    public String loginPage() {
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        RegisterRequest req = new RegisterRequest();
        model.addAttribute("registerRequest", req);
        model.addAttribute(BindingResult.MODEL_KEY_PREFIX + "registerRequest",
                new BeanPropertyBindingResult(req, "registerRequest"));
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute RegisterRequest req,
                           BindingResult bindingResult,
                           RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "auth/register";
        }
        if (!req.isPasswordMatch()) {
            bindingResult.rejectValue("passwordConfirm", "mismatch", "비밀번호가 일치하지 않습니다.");
            return "auth/register";
        }
        try {
            userService.register(req.getUsername(), req.getEmail(), req.getPassword());
            redirectAttributes.addFlashAttribute("successMessage",
                    "인증 이메일을 발송했습니다. 이메일을 확인하여 가입을 완료해주세요.");
            return "redirect:/auth/login";
        } catch (IllegalArgumentException e) {
            bindingResult.reject("duplicate", e.getMessage());
            return "auth/register";
        }
    }

    @GetMapping("/verify")
    public String verifyEmail(@RequestParam String token, RedirectAttributes redirectAttributes) {
        try {
            userService.verifyEmail(token);
            redirectAttributes.addFlashAttribute("successMessage", "이메일 인증이 완료되었습니다. 로그인해주세요.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/auth/login";
    }

    @GetMapping("/resend-verification")
    public String resendVerificationPage() {
        return "auth/resend-verification";
    }

    @PostMapping("/resend-verification")
    public String resendVerification(@RequestParam String email, RedirectAttributes redirectAttributes) {
        userService.resendVerification(email);
        redirectAttributes.addFlashAttribute("successMessage",
                "인증 이메일을 재발송했습니다. 이메일을 확인해주세요.");
        return "redirect:/auth/login";
    }

    @GetMapping("/forgot-password")
    public String forgotPasswordPage() {
        return "auth/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String forgotPassword(@RequestParam String email, RedirectAttributes redirectAttributes) {
        passwordResetService.requestReset(email);
        redirectAttributes.addFlashAttribute("successMessage",
                "비밀번호 재설정 이메일을 발송했습니다. 이메일을 확인해주세요.");
        return "redirect:/auth/login";
    }

    @GetMapping("/reset-password")
    public String resetPasswordPage(@RequestParam String token, Model model) {
        model.addAttribute("token", token);
        return "auth/reset-password";
    }

    @PostMapping("/reset-password")
    public String resetPassword(@RequestParam String token,
                                @RequestParam String password,
                                @RequestParam String passwordConfirm,
                                RedirectAttributes redirectAttributes) {
        if (!password.equals(passwordConfirm)) {
            redirectAttributes.addFlashAttribute("errorMessage", "비밀번호가 일치하지 않습니다.");
            return "redirect:/auth/reset-password?token=" + token;
        }
        if (password.length() < 8) {
            redirectAttributes.addFlashAttribute("errorMessage", "비밀번호는 8자 이상이어야 합니다.");
            return "redirect:/auth/reset-password?token=" + token;
        }
        try {
            passwordResetService.resetPassword(token, password);
            redirectAttributes.addFlashAttribute("successMessage",
                    "비밀번호가 변경되었습니다. 새 비밀번호로 로그인해주세요.");
            return "redirect:/auth/login";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/auth/reset-password?token=" + token;
        }
    }
}
