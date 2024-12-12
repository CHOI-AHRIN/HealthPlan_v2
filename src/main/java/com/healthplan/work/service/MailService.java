package com.healthplan.work.service;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender javaMailSender;
    private static final String senderEmail = "kottazi830@gmail.com";
    private static int number;

    public int createNumber() {
        // (int) Math.random() * (최댓값-최소값+1) + 최소값
        return (int) (Math.random() * (90000)) + 100000; // 6자리 랜덤 숫자 생성
    }

    public MimeMessage CreateMail(String email, int number) {
        createNumber();
        MimeMessage message = javaMailSender.createMimeMessage();

        try {
            message.setFrom(senderEmail);
            message.setRecipients(MimeMessage.RecipientType.TO, email);
            message.setSubject("[HealthPlan] 이메일 인증번호 발송드립니다");
            String body = """
                    <h3>안녕하세요. HealthPlan입니다. ☺️</h3>
                    <p>요청하신 인증 번호입니다.</p>
                    <h1>%d</h1>
                    <p>좋은하루 보내세요! 감사합니다.</p>
                    """.formatted(number);
            message.setText(body, "UTF-8", "html");
        } catch (MessagingException e) {
            e.printStackTrace();
        }

        return message;
    }

    public int sendMail(String email) {
        int number = createNumber(); // 번호 생성
        MimeMessage message = CreateMail(email, number);
        System.out.println("Received email: " + email); // 전달된 이메일 로그
        javaMailSender.send(message);
        return number;
    }
}
