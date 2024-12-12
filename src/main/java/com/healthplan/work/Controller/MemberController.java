package com.healthplan.work.Controller;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.WebUtils;

import com.healthplan.work.dao.MemberMapper;
import com.healthplan.work.dto.LoginDTO;
import com.healthplan.work.util.JwtUtils;
import com.healthplan.work.vo.MemberEntity;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/member")

public class MemberController {
    private final PasswordEncoder passwordEncoder;
    @Autowired
    MemberMapper mapper;

    @Autowired
    private JwtUtils jwtUtils = new JwtUtils();

    @Autowired
    private static final Logger logger = LoggerFactory.getLogger(MemberController.class);

    public MemberController(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    // 회원리스트
    @GetMapping("/list")
    public List<MemberEntity> selectList(Model model) throws Exception {

        logger.info("1. //****************************** /api/member/list");

        List<MemberEntity> list = mapper.selectMemberList();

        logger.info("2. // list.toString()=" + list.toString());

        return list;
    }

    // 회원번호로 아이디 조회
    @PostMapping("/getUuidByMno")
    public ResponseEntity<Map<String, String>> getUuidByMno(@RequestBody MemberEntity mem) throws Exception {
        logger.info("회원정보 조회 요청 - mno: " + mem.getMno());

        // mno로 uuid 조회
        String uuid = mapper.selectUuidByMno(mem.getMno());
        logger.info("/******************************* uuid 조회 당한 회원번호 보여줘 : " + uuid);

        if (uuid != null) {
            // 응답 데이터를 JSON 형식으로 반환
            Map<String, String> result = new HashMap<>();
            result.put("uuid", uuid);

            return new ResponseEntity<>(result, HttpStatus.OK);
        } else {
            // 해당 mno에 대한 uuid가 없을 경우
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    // 회원번호 조회
    @PostMapping("/readMno")
    public ResponseEntity<MemberEntity> selectMno(@RequestBody Map<String, String> requestData) throws Exception {

        // Map에서 "uuid" 값 추출
        String uuid = requestData.get("uuid");
        // 로그로 uuid 확인
        logger.info("조회할 아이디 : " + uuid);

        MemberEntity memberInfo = mapper.selectMno(uuid);

        logger.info("/************************** 일단 회원번호 정보 보여줘" + memberInfo);

        if (memberInfo != null) {
            logger.info("/************************** 성공한 회원번호 정보 보여줘" + memberInfo);
            return ResponseEntity.ok(memberInfo);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    // mtype 조회
    @PostMapping("/searchMtype")
    public ResponseEntity<String> searchMtype(@RequestBody Map<String, String> requestBody) {
        String uuid = requestBody.get("uuid");
        try {
            MemberEntity member = mapper.searchMtype(uuid);
            if (member != null) {
                return ResponseEntity.ok(member.getMtype());
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not Found");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // 회원가입
    @PostMapping("/register")
    public ResponseEntity<String> insertMemPOST(@RequestBody MemberEntity mem) throws Exception {

        logger.info("/*********************** 회원가입!! regist post ...........");
        logger.info(mem.toString());

        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        String hashedPassword = passwordEncoder.encode(mem.getUpw());

        System.out.println("해시된 비밀번호:" + hashedPassword);

        mem.setUpw(hashedPassword);

        mapper.insertMem(mem);
        int pmno = mapper.currval(); // 현재 회원번호 조회

        mapper.setpoint(pmno); // 기본 포인트 등록!

        // return "success";
        return ResponseEntity.ok("success");
    }

    // 로그인
    @PostMapping("loginPost")
    public ResponseEntity<?> loginPOST(@RequestBody LoginDTO dto) throws Exception {

        logger.info("/******************************************** loginPost");
        logger.info(dto.toString());

        try {
            final String token = jwtUtils.generateToken(dto.getUuid());
            logger.info("/*** 인코딩 !!!! token=" + token);

            // JWT 토큰 유효성 검사
            boolean isValidToken = jwtUtils.validateToken(token, dto.getUuid());
            logger.info("토큰 유효성: " + isValidToken);

            // 저장된 해시된 비밀번호 가져오기
            String storedHashedPassword = mapper.getHashedPasswordByUuid(dto.getUuid());
            logger.info("저장된 해시된 비밀번호 가져오기!!!! Stored Hashed Password: " + storedHashedPassword);

            // 비밀번호 비교
            BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
            boolean isPasswordMatch = passwordEncoder.matches(dto.getUpw(), storedHashedPassword);
            logger.info("비밀번호 일치 여부: " + isPasswordMatch);

            if (isPasswordMatch) {
                // 로그인 성공 시, 토큰과 사용자 정보를 응답 본문과 헤더에 포함하여 클라이언트에 전달
                MemberEntity loggedInMember = mapper.login(dto);

                // 응답 헤더에 토큰 포함
                HttpHeaders responseHeaders = new HttpHeaders();
                responseHeaders.set("Authorization", "Bearer " + token);

                // 응답 본문에도 JWT 토큰과 사용자 정보 포함
                Map<String, Object> responseBody = new HashMap<>();
                responseBody.put("member", loggedInMember);
                responseBody.put("token", token);

                return new ResponseEntity<>(responseBody, responseHeaders, HttpStatus.OK);

            } else {
                // 비밀번호 불일치 시
                return new ResponseEntity<>("비밀번호가 일치하지 않습니다.", HttpStatus.UNAUTHORIZED);
            }
        } catch (Exception e) {
            logger.error("로그인 처리 중 오류 발생", e);
            return new ResponseEntity<>("서버 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // 쿠키 관리
    @PostMapping("/loginCookie")
    public ResponseEntity<?> loginCookie(@RequestBody Map<String, String> requestData) throws Exception {

        logger.info("1. loginCookie Controller");

        String token = requestData.get("token"); // token 값 추출
        System.out.println("2. 받은 토큰 확인 : ㅣ" + token);

        if (token == null || token.isEmpty()) {
            return new ResponseEntity<>("토큰이 없습니다.", HttpStatus.UNAUTHORIZED);
        }

        try {
            // JWT 토큰에서 uuid 추출 (JwtUtils는 토큰을 처리하는 유틸리티 클래스라고 가정)
            String uuid = jwtUtils.getUuidFromToken(token);

            // 토큰이 유효한지 확인
            if (jwtUtils.validateToken(token, uuid)) {
                logger.info("3. token의 id : " + uuid);

                return ResponseEntity.ok(Map.of("uuid", uuid)); // 유효하면 uuid를 반환

            } else {
                return new ResponseEntity<>("유효하지 않은 토큰입니다.", HttpStatus.UNAUTHORIZED);
            }

        } catch (Exception e) {
            return new ResponseEntity<>("서버 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }

    // 로그아웃
    @GetMapping("/logout")
    public String logout(HttpServletRequest request, HttpServletResponse response, HttpSession session)
            throws Exception {

        Object obj = session.getAttribute("login");

        if (obj != null) {
            MemberEntity mem = (MemberEntity) obj;

            session.removeAttribute("login");
            session.invalidate();

            Cookie loginCookie = WebUtils.getCookie(request, "loginCookie");

            if (loginCookie != null) {
                loginCookie.setPath("/");
                loginCookie.setMaxAge(0);
                response.addCookie(loginCookie);
                mapper.keepLogin(mem.getUuid(), session.getId(), new Date());
            }
        }
        return "success";
    }

    // 아이디 중복체크
    @PostMapping("/uuidCk")
    public int uuidCk(@RequestBody Map<String, String> requestData) throws Exception {

        logger.info("1. uuidCk post");

        // Map에서 "uuid" 값 추출
        String uuid = requestData.get("uuid");

        // 로그로 uuid 확인
        logger.info("2. 조회할 아이디 : " + uuid);

        logger.info("3. " + uuid.toString());

        int result = mapper.uuidCk(uuid);

        logger.info("4. result 값 확인: " + result);

        return result;

    }

    // 이메일 중복체크
    @PostMapping("/emailCk")
    public String emailCk(@RequestBody Map<String, String> request) throws Exception {
        logger.info("1. emailCk post ...........");

        String email = request.get("inputEmail");

        logger.info("2. " + email);

        int result = mapper.emailCk(email);

        logger.info("3. Result: " + result); // int 값을 문자열로 출력

        String result2 = "" + result;

        logger.info("4. Result2: " + result); // int 값을 문자열로 출력

        return result2;
    }

    // 이름 조회
    @PostMapping("/readName")
    public ResponseEntity<MemberEntity> selectName(@RequestBody Map<String, String> requestData) throws Exception {

        // Map에서 "uuid" 값 추출
        String uuid = requestData.get("uuid");
        // 로그로 uuid 확인
        logger.info("조회할 아이디 : " + uuid);

        MemberEntity memberInfo = mapper.selectName(uuid);

        logger.info("/************************** 일단 이름 정보 보여줘" + memberInfo);

        if (memberInfo != null) {
            logger.info("/************************** 성공한 이름 정보 보여줘" + memberInfo);
            return ResponseEntity.ok(memberInfo);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    // 마이페이지 회원정보 조회
    @PostMapping("/read")
    public ResponseEntity<MemberEntity> selectMemId(@RequestBody Map<String, String> requestData) throws Exception {

        // Map에서 "uuid" 값 추출
        String uuid = requestData.get("uuid");
        // 로그로 uuid 확인
        logger.info("조회할 아이디 : " + uuid);

        MemberEntity memberInfo = mapper.selectUuid(uuid);

        logger.info("/************************** 일단 마이페이지 정보 보여줘" + memberInfo);

        if (memberInfo != null) {
            logger.info("/************************** 성공한 마이페이지 정보 보여줘" + memberInfo);
            return ResponseEntity.ok(memberInfo);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    // 특정 회원 정보 조회
    @GetMapping("/read/{uuid}")
    public ResponseEntity<MemberEntity> selectMemberByUuid(@PathVariable("uuid") String uuid) throws Exception {
        logger.info("1. 조회할 아이디 : " + uuid);

        MemberEntity memberInfo = mapper.selectUuid(uuid);

        if (memberInfo != null) {
            return ResponseEntity.ok(memberInfo);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    // 마이페이지 회원정보 수정
    @PostMapping("/modify")
    public String updatePOST(@RequestBody MemberEntity mem, RedirectAttributes rttr) throws Exception {

        logger.info(mem.toString());
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        String hashedPassword = passwordEncoder.encode(mem.getUpw());

        System.out.println("해시된 비밀번호:" + hashedPassword);

        mem.setUpw(hashedPassword);

        mapper.update(mem);

        rttr.addAttribute("name", mem.getName());
        rttr.addFlashAttribute("msg", "SUCCESS");

        logger.info(rttr.toString());

        return "SUCCESS";

    }

    // 이메일 수정
    @PostMapping("/modiEmail")
    public String modiEmail(@RequestBody Map<String, String> request) throws Exception {

        String uuid = request.get("uuid");
        String email = request.get("email");

        logger.info(email);
        mapper.modiEmail(email, uuid);

        return "success";
    }

    // 회원정보수정
    @PostMapping("/modifyMem")
    public String modifyMem(@RequestBody MemberEntity mem) throws Exception {
        logger.info("1. .modifyMem : " + mem);
        mapper.modifyMem(mem);
        return "SUCCESS";
    }

    // 비밀번호 확인
    @PostMapping("/upwCk")
    public ResponseEntity<String> upwCk(@RequestBody Map<String, String> request) throws Exception {

        String uuid = request.get("uuid");
        String upw = request.get("upw");

        logger.info("1. uuid: " + uuid);
        logger.info("2. upw: " + upw);

        // 저장된 해시된 비밀번호 가져오기
        String storedHashedPassword = mapper.getHashedPasswordByUuid(uuid);
        logger.info("3. Stored Hashed Password: " + storedHashedPassword);

        // 비밀번호 비교
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        boolean isPasswordMatch = passwordEncoder.matches(upw, storedHashedPassword);
        logger.info("4. 비밀번호 일치 여부: " + isPasswordMatch);

        if (isPasswordMatch) {
            return ResponseEntity.ok("success");
        }
        // 비밀번호 일치 하지 않을때 코드 변경하기
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("비밀번호가 일치하지 않습니다.");
    }

    // 비밀번호 변경
    @PostMapping("/modiUpw")
    public ResponseEntity<String> modiUpw(@RequestBody Map<String, String> request) throws Exception {

        String uuid = request.get("uuid");
        String upw = request.get("newUpw");

        logger.info("1. uuid: " + uuid);
        logger.info("2. upw: " + upw);

        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        String hashedPassword = passwordEncoder.encode(upw);

        logger.info("3. 해시된 비밀번호:" + hashedPassword);

        // upw = hashedPassword;
        Map<String, Object> params = new HashMap<>();
        params.put("uuid", uuid);
        params.put("upw", hashedPassword); 

        mapper.updateUpw(params);

        return ResponseEntity.ok("succes");
    }

    // 회원탈퇴
    @DeleteMapping("/remove")
    public ResponseEntity<String> delete(@RequestBody Map<String, String> request) throws Exception {
        String uuid = request.get("uuid");
        int rowsAffected = mapper.delete(uuid);
        if (rowsAffected > 0) { // 삭제후 반환되는 결과값에 따라 해당 문자열 반환
            return ResponseEntity.ok("success");
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("failure");
        }
    }

}
