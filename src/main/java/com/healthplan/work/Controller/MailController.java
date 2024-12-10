package com.healthplan.work.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.healthplan.work.service.MailService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class MailController {
    private final MailService service;

    @ResponseBody
    @PostMapping("/api/mail")
    public String MailSend(String email){

        int number = service.sendMail(email);

        String num = "" + number;

        return num;
    }

}
