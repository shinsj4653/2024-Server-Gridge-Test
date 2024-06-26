package com.example.demo.common.oauth;

import com.example.demo.src.user.model.KakaoUser;
import com.example.demo.src.user.model.KakaoOAuthToken;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class KakaoOauth implements SocialOauth {

    //applications.yml 에서 value annotation을 통해서 값을 받아온다.
    @Value("${spring.OAuth2.kakao.url}")
    private String KAKAO_SNS_URL;

    @Value("${spring.OAuth2.kakao.client-id}")
    private String KAKAO_SNS_CLIENT_ID;

    @Value("${spring.OAuth2.kakao.callback-login-url}")
    private String KAKAO_SNS_CALLBACK_LOGIN_URL;

    @Value("${spring.OAuth2.kakao.client-secret}")
    private String KAKAO_SNS_CLIENT_SECRET;

    @Value("${spring.OAuth2.google.scope}")
    private String KAKAO_DATA_ACCESS_SCOPE;

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    @Override
    public String getOauthRedirectURL() {

        Map<String, Object> params = new HashMap<>();
        params.put("response_type", "code");
        params.put("client_id", KAKAO_SNS_CLIENT_ID);
        params.put("redirect_uri", KAKAO_SNS_CALLBACK_LOGIN_URL);


        //parameter를 형식에 맞춰 구성해주는 함수
        String parameterString = params.entrySet().stream()
                .map(x -> x.getKey() + "=" + x.getValue())
                .collect(Collectors.joining("&"));
        String redirectURL = KAKAO_SNS_URL + "?" + parameterString;
        log.info("redirectURL = {}", redirectURL);

        return redirectURL;
        /*
         * https://accounts.google.com/o/oauth2/v2/auth?scope=profile&response_type=code
         * &client_id="할당받은 id"&redirect_uri="access token 처리")
         * 로 Redirect URL을 생성하는 로직을 구성
         * */
    }


    public ResponseEntity<String> requestAccessToken(String code) {
        String KAKAO_TOKEN_REQUEST_URL = "https://kauth.kakao.com/oauth/token";

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("code", code);
        body.add("client_id", KAKAO_SNS_CLIENT_ID);
        body.add("client_secret", KAKAO_SNS_CLIENT_SECRET);
        body.add("redirect_uri", KAKAO_SNS_CALLBACK_LOGIN_URL);
        body.add("grant_type", "authorization_code");

        ResponseEntity<String> responseEntity = restTemplate.postForEntity(
                KAKAO_TOKEN_REQUEST_URL,
                new HttpEntity<>(body, headers),
                String.class
        );

        if (responseEntity.getStatusCode() == HttpStatus.OK) {
            log.info("HTTP status is OK");
            return responseEntity;
        }
        return null;
    }

    public KakaoOAuthToken getAccessToken(ResponseEntity<String> response) throws JsonProcessingException {
        log.info("response.getBody() = {}", response.getBody());

        KakaoOAuthToken kakaoOAuthToken = objectMapper.readValue(response.getBody(), KakaoOAuthToken.class);
        return kakaoOAuthToken;

    }

    public ResponseEntity<String> requestUserInfo(KakaoOAuthToken oAuthToken) {
        String KAKAO_USERINFO_REQUEST_URL="https://kapi.kakao.com/v2/user/me";

        //header에 accessToken을 담는다.
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization","Bearer "+oAuthToken.getAccess_token());

        //HttpEntity를 하나 생성해 헤더를 담아서 restTemplate으로 구글과 통신하게 된다.
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity(headers);
        ResponseEntity<String> response = restTemplate.exchange(KAKAO_USERINFO_REQUEST_URL, HttpMethod.GET,request,String.class);

        log.info("response.getBody() = {}", response.getBody());

        return response;
    }

    public KakaoUser getUserInfo(ResponseEntity<String> userInfoRes) throws JsonProcessingException{
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(userInfoRes.getBody());

        String id = rootNode.path("id").asText();
        String nickname = rootNode.path("properties").path("nickname").asText();
        String profileImage = rootNode.path("properties").path("profile_image").asText();

        KakaoUser kakaoUser = new KakaoUser();
        kakaoUser.setId(id);
        kakaoUser.setProfileNickname(nickname);
        kakaoUser.setProfileImage(profileImage);

        return kakaoUser;
    }

}
