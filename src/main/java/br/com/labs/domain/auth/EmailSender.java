package br.com.labs.domain.auth;

import br.com.labs.domain.user.Email;

public interface EmailSender {

    void sendMfaCode(Email to, MfaCode code);
}
