package com.healthplan.work.Controller;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.healthplan.work.service.MailService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/api/mail")
@RequiredArgsConstructor

public class MailController {

    @Autowired
    MailService service;

    @Autowired
    private static final Logger log = LoggerFactory.getLogger(MailController.class);

    @ResponseBody
    @PostMapping("/email")
    public ResponseEntity<?> MailSend(@RequestBody Map<String, String> request) throws Exception {

        String email = request.get("email");

        log.info("1. " + email);

        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("이메일은 필수입니다.");
        }

        int number = service.sendMail(email);
        log.info("2. " + email);

        // String num = "" + number;

        return ResponseEntity.ok(number);
    }

}
