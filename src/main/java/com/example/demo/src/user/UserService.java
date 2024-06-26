package com.example.demo.src.user;



import com.example.demo.common.exceptions.BaseException;
import com.example.demo.src.admin.model.PostUserLogTimeReq;
import com.example.demo.src.user.entity.User;
import com.example.demo.src.user.model.*;
import com.example.demo.utils.JwtService;
import com.example.demo.utils.MessageUtils;
import com.example.demo.utils.SHA256;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.envers.AuditReader;
import org.springframework.data.history.Revision;
import org.springframework.data.history.Revisions;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.example.demo.common.entity.BaseEntity.*;
import static com.example.demo.common.entity.BaseEntity.State.ACTIVE;
import static com.example.demo.common.entity.BaseEntity.State.INACTIVE;
import static com.example.demo.common.response.BaseResponseStatus.*;
import static com.example.demo.src.user.entity.User.AccountState.*;

// Service Create, Update, Delete 의 로직 처리
@Transactional
@RequiredArgsConstructor
@Service
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final AuditReader auditReader;
    private final MessageUtils messageUtils;

    //POST
    public PostUserRes createUser(PostUserReq postUserReq) {

        // 소셜 로그인인지 구분
        Boolean oAuth = postUserReq.getIsOAuth();

        // 소셜 로그인을 사용하기로 메세지 넘기기
        if (oAuth) {
            throw new BaseException(INVALID_LOGIN_METHOD, messageUtils.getMessage("INVALID_LOGIN_METHOD"));
        }

        //중복 체크
        Optional<User> checkUser = userRepository.findByEmailAndState(postUserReq.getEmail(), ACTIVE);
        if (checkUser.isPresent()) {
            throw new BaseException(POST_USERS_EXISTS_EMAIL, messageUtils.getMessage("POST_USERS_EXISTS_EMAIL"));
        }

        String encryptPwd;
        try {
            encryptPwd = new SHA256().encrypt(postUserReq.getPassword());
            postUserReq.setPassword(encryptPwd);
        } catch (Exception exception) {
            throw new BaseException(PASSWORD_ENCRYPTION_ERROR, messageUtils.getMessage("PASSWORD_ENCRYPTION_ERROR"));
        }

        // 일반 로그인
        User saveUser = userRepository.save(postUserReq.toEntity());
        return new PostUserRes(saveUser.getId());

    }

    public PostUserRes createOAuthUser(User user) {

        User saveUser = userRepository.save(user);

        // JWT 발급
        String jwtToken = jwtService.createJwt(saveUser.getId());
        return new PostUserRes(saveUser.getId(), jwtToken);

    }




    public PostLoginRes logIn(PostLoginReq postLoginReq) {
        User user = userRepository.findByEmailAndState(postLoginReq.getEmail(), ACTIVE)
                .orElseThrow(() -> new BaseException(NOT_FIND_USER));

        if (user.getAccountState().equals(BLOCKED)) {
            throw new BaseException(USER_BLOCKED_ERROR, messageUtils.getMessage("USER_BLOCKED_ERROR"));
        }

        if (user.getState().equals(INACTIVE)) {
            throw new BaseException(USER_INACTIVE_ERROR, messageUtils.getMessage("USER_INACTIVE_ERROR"));
        }

        String encryptPwd;
        try {
            encryptPwd = new SHA256().encrypt(postLoginReq.getPassword());
        } catch (Exception exception) {
            throw new BaseException(PASSWORD_ENCRYPTION_ERROR, messageUtils.getMessage("PASSWORD_ENCRYPTION_ERROR"));
        }

        if(user.getPassword().equals(encryptPwd)){
            Long userId = user.getId();
            String jwt = jwtService.createJwt(userId);
            return new PostLoginRes(userId, jwt);
        } else{
            throw new BaseException(FAILED_TO_LOGIN, messageUtils.getMessage("FAILED_TO_LOGIN"));
        }

    }

    // PATCH
    public void modifyUserName(Long userId, PatchUserReq patchUserReq) {
        User user = userRepository.findByIdAndState(userId, ACTIVE)
                .orElseThrow(() -> new BaseException(NOT_FIND_USER , messageUtils.getMessage("NOT_FIND_USER")));
        user.updateName(patchUserReq.getName());
    }

    public void modifyBirthDate(Long userId, PatchUserBirthDateReq birthDateReq) {
        User user = userRepository.findByIdAndState(userId, ACTIVE)
                .orElseThrow(() -> new BaseException(NOT_FIND_USER, messageUtils.getMessage("NOT_FIND_USER")));
        user.updateBirthDate(birthDateReq.getBirthDate());
    }

    public void modifyPrivacy(Long userId, PatchUserPrivacyTermReq privacyTermReq) {
        User user = userRepository.findByIdAndState(userId, ACTIVE)
                .orElseThrow(() -> new BaseException(NOT_FIND_USER, messageUtils.getMessage("NOT_FIND_USER")));
        user.updatePrivacyTerm(privacyTermReq.getServiceTerm(), privacyTermReq.getDataTerm(),
                privacyTermReq.getLocationTerm());
    }

    public void modifyState(Long userId, State state) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BaseException(NOT_FIND_USER, messageUtils.getMessage("NOT_FIND_USER")));
        user.updateState(state);
    }

    // DELETE
    public void deleteUser(Long userId) {
        User user = userRepository.findByIdAndState(userId, ACTIVE)
                .orElseThrow(() -> new BaseException(NOT_FIND_USER, messageUtils.getMessage("NOT_FIND_USER")));
        userRepository.delete(user);
    }

    // GET
    @Transactional(readOnly = true)
    public List<GetUserRes> getUsers() {
        List<GetUserRes> getUserResList = userRepository.findAllByState(ACTIVE).stream()
                .map(GetUserRes::new)
                .collect(Collectors.toList());
        return getUserResList;
    }

    @Transactional(readOnly = true)
    public List<GetUserRes> getUsersByEmail(String email) {
        List<GetUserRes> getUserResList = userRepository.findAllByEmailAndState(email, ACTIVE).stream()
                .map(GetUserRes::new)
                .collect(Collectors.toList());
        return getUserResList;
    }


    @Transactional(readOnly = true)
    public GetUserRes getUser(Long userId) {
        User user = userRepository.findByIdAndState(userId, ACTIVE)
                .orElseThrow(() -> new BaseException(NOT_FIND_USER, messageUtils.getMessage("NOT_FIND_USER")));
        return new GetUserRes(user);
    }

    @Transactional(readOnly = true)
    public boolean checkUserByEmail(String email) {
        Optional<User> result = userRepository.findByEmailAndState(email, ACTIVE);
        if (result.isPresent()) return true;
        return false;
    }

    @Transactional(readOnly = true)
    public GetUserRes getUserByEmail(String email) {
        User user = userRepository.findByEmailAndState(email, ACTIVE)
                .orElseThrow(() -> new BaseException(NOT_FIND_USER, messageUtils.getMessage("NOT_FIND_USER")));
        return new GetUserRes(user);
    }

    @Transactional(readOnly = true)
    public List<GetUserLogRes> getUserHistoryByRevType(String revType) {

        if (!revType.equals("INSERT") && !revType.equals("UPDATE") && !revType.equals("DELETE")) {
            throw new BaseException(REVTYPE_ERROR, messageUtils.getMessage("REVTYPE_ERROR"));
        }

        List<Object> revs = getRevs();

        List<GetUserLogRes> userLogs = new ArrayList<>();

        revs.forEach(revision -> {
            Object[] revisionArray = (Object[]) revision;
            com.example.demo.src.revision.entity.Revision revObject = (com.example.demo.src.revision.entity.Revision) revisionArray[1];
            getUserLogResByType(userLogs, revObject.getId(), revType);
        });

        return userLogs;
    }

    @Transactional(readOnly = true)
    public List<GetUserLogRes> getUserHistory() {

        List<Object> revs = getRevs();
        List<GetUserLogRes> userLogs = new ArrayList<>();

        revs.forEach(revision -> {
            Object[] revisionArray = (Object[]) revision;
            com.example.demo.src.revision.entity.Revision revObject = (com.example.demo.src.revision.entity.Revision) revisionArray[1];
            getUserLogRes(userLogs, revObject.getId());
        });

        return userLogs;
    }

    @Transactional(readOnly = true)
    public List<GetUserLogRes> getUserHistoryByTime(PostUserLogTimeReq req) {

        LocalDateTime startTime = req.getStartTime();
        LocalDateTime endTime = req.getEndTime();

        List<Object> revs = getRevs();

        List<GetUserLogRes> userLogs = new ArrayList<>();

        revs.forEach(revision -> {
            Object[] revisionArray = (Object[]) revision;
            com.example.demo.src.revision.entity.Revision revObject = (com.example.demo.src.revision.entity.Revision) revisionArray[1];
            getUserLogResByTime(userLogs, revObject.getId(), startTime, endTime);
        });

        return userLogs;
    }

    private void getUserLogResByType(List<GetUserLogRes> userLogs, Long rev, String revType) {

        String rType = revType;

        Revisions<Long, User> revisions = userRepository.findRevisions(rev);

        for (Revision<Long, User> revision : revisions.getContent()) {
            if (String.valueOf(revision.getMetadata().getRevisionType()).equals(rType)) {
                userLogs.add(makeGetUserLogRes(revision));
            }
        }
    }

    private void getUserLogRes(List<GetUserLogRes> userLogs, Long revId) {

        Revisions<Long, User> revisions = userRepository.findRevisions(revId);
        for (Revision<Long, User> revision : revisions.getContent()) {
                userLogs.add(makeGetUserLogRes(revision));
        }
    }

    private void getUserLogResByTime(List<GetUserLogRes> userLogs, Long rev,
                                     LocalDateTime startTime, LocalDateTime endTime) {

        Revisions<Long, User> revisions = userRepository.findRevisions(rev);
        for (Revision<Long, User> revision : revisions.getContent()) {
            Instant requiredRevisionInstant = revision.getMetadata().getRequiredRevisionInstant();
            LocalDateTime localDateTime = LocalDateTime.ofInstant(requiredRevisionInstant, ZoneId.of("Asia/Seoul"));

            if (!localDateTime.isBefore(startTime) && !localDateTime.isAfter(endTime)) {
                GetUserLogRes getUserLogRes = makeGetUserLogRes(revision);
                userLogs.add(getUserLogRes);
            }

        }
    }

    private GetUserLogRes makeGetUserLogRes(Revision<Long, User> revision) {
        Long revisionNumber = revision.getMetadata().getRevisionNumber().get();
        String revisionType = String.valueOf(revision.getMetadata().getRevisionType());

        Instant requiredRevisionInstant = revision.getMetadata().getRequiredRevisionInstant();
        LocalDateTime localDateTime = LocalDateTime.ofInstant(requiredRevisionInstant, ZoneId.of("Asia/Seoul"));
        return new GetUserLogRes(revisionNumber, revisionType, localDateTime);
    }

    private List<Object> getRevs() {
        return auditReader.createQuery()
                .forRevisionsOfEntity(User.class, false, true)
                .getResultList();
    }
}
