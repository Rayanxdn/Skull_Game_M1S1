package fr.miage.skull.frontend.form;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegisterFormTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private RegisterForm form(String pseudo, String email, String password, String confirm) {
        RegisterForm f = new RegisterForm();
        f.setPseudo(pseudo);
        f.setEmail(email);
        f.setPassword(password);
        f.setConfirmPassword(confirm);
        return f;
    }

    @Test
    void validForm_hasNoViolation() {
        assertTrue(validator.validate(form("Bob", "bob@test.fr", "secret1", "secret1")).isEmpty());
    }

    @Test
    void emptyForm_hasViolationsOnEveryField() {
        assertEquals(4, validator.validate(form("", "", "", "")).size()); // un seul message par champ vide
    }

    @Test
    void invalidEmail_isRejected() {
        assertEquals(1, validator.validate(form("Bob", "abc", "secret1", "secret1")).size());
    }

    @Test
    void shortPassword_isRejected() {
        assertEquals(1, validator.validate(form("Bob", "bob@test.fr", "123", "123")).size());
    }
}
