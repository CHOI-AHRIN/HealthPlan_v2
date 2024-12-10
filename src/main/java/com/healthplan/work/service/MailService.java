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
        // number = (int) (Math.random() * (90000)) + 100000;// (int) Math.random() *
        // (최댓값-최소값+1) + 최소값
        return (int) (Math.random() * (90000)) + 100000; // 6자리 랜덤 숫자 생성
    }

    public MimeMessage CreateMail(String email, int number) {
        createNumber();
        MimeMessage message = javaMailSender.createMimeMessage();

        /*
         * try {
         * message.setFrom(senderEmail);
         * message.setRecipients(MimeMessage.RecipientType.TO, email);
         * message.setSubject("이메일 인증");
         * String body = "";
         * body += "<h3>" + "안녕하세요. HealthPlan입니다. ☺️" + "</h3>";
         * body += "<h3>" + "요청하신 인증 번호입니다." + "</h3>";
         * body += "<h1>" + number + "</h1>";
         * body += "<h3>" + "감사합니다." + "</h3>";
         * message.setText(body, "UTF-8", "html");
         * } catch (MessagingException e) {
         * e.printStackTrace();
         * }
         */

        try {
            message.setFrom(senderEmail);
            message.setRecipients(MimeMessage.RecipientType.TO, email);
            message.setSubject("이메일 인증");
            String body = """
                    <h3>" + "안녕하세요. HealthPlan입니다. ☺️" + "</h3>
                    <h3>" + "요청하신 인증 번호입니다." + "</h3>
                    <h1>" + number + "</h1>
                    <h3>" + "감사합니다." + "</h3>
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
        javaMailSender.send(message);
        return number;
    }
}
