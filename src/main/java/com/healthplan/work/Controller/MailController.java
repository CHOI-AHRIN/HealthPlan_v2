package com.healthplan.work.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.healthplan.work.service.MailService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/api/mail")
@RequiredArgsConstructor
public class MailController {
    private final MailService service;

    @ResponseBody
    @PostMapping("/email")
    public String MailSend(String email) {

        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Recipient email address is required.");
        }

        int number = service.sendMail(email);

        String num = "" + number;

        return num;
    }

}
