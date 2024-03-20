package com.example.demo.src.feed;

import com.example.demo.common.exceptions.BaseException;
import com.example.demo.src.feed.entity.Feed;
import com.example.demo.src.feed.model.*;
import com.example.demo.src.user.UserRepository;
import com.example.demo.src.user.entity.User;
import com.example.demo.src.user.model.*;
import lombok.RequiredArgsConstructor;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.query.AuditEntity;
import org.springframework.data.history.Revision;
import org.springframework.data.history.Revisions;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.example.demo.common.entity.BaseEntity.State.ACTIVE;
import static com.example.demo.common.response.BaseResponseStatus.*;

@Transactional
@RequiredArgsConstructor
@Service
public class FeedService {

    private final FeedRepository feedRepository;
    private final UserRepository userRepository;
    private final AuditReader auditReader;

    // POST
    public PostFeedRes createFeed(PostFeedReq req) {

        User user = userRepository.findByIdAndState(req.getUserId(), ACTIVE).
                orElseThrow(() -> new BaseException(INVALID_USER));

        Feed saveFeed = feedRepository.save(req.toEntity(user));
        return new PostFeedRes(saveFeed.getId(), saveFeed.getContent());
    }
    // GET
    @Transactional(readOnly = true)
    public List<GetFeedRes> getFeeds() {
        List<GetFeedRes> getPostsResList = feedRepository.findAllByState(ACTIVE).stream()
                .map(GetFeedRes::new)
                .collect(Collectors.toList());

        return getPostsResList;
    }

    @Transactional(readOnly = true)
    public List<GetFeedRes> getFeedsByUserId(Long userId) {
        User user = userRepository.findByIdAndState(userId, ACTIVE)
                .orElseThrow(() -> new BaseException(NOT_FIND_USER));

        List<GetFeedRes> getPostsResList =  user.getFeedList().stream()
                .filter(post -> post.getState() == ACTIVE)
                .map(GetFeedRes::new)
                .collect(Collectors.toList());

        return getPostsResList;
    }

    @Transactional(readOnly = true)
    public GetFeedRes getFeed(Long postId) {
        Feed feed = feedRepository.findByIdAndState(postId, ACTIVE)
                .orElseThrow(() -> new BaseException(NOT_FIND_FEED));
        return new GetFeedRes(feed);
    }

    @Transactional(readOnly = true)
    public List<GetFeedLogRes> getFeedHistoryByRevType(String revType) {

        if (!revType.equals("INSERT") && !revType.equals("UPDATE") && !revType.equals("DELETE")) {
            throw new BaseException(REVTYPE_ERROR);
        }

        List<Long> revIds = getRevIds();

        List<GetFeedLogRes> postLogs = new ArrayList<>();

        revIds.stream()
                .forEach((id) -> {
                    getFeedLogResByType(postLogs, id, revType);
                });

        return postLogs;
    }

    @Transactional(readOnly = true)
    public List<GetFeedLogRes> getFeedHistory() {

        List<Long> revIds = getRevIds();

        List<GetFeedLogRes> postLogs = new ArrayList<>();

        revIds.stream()
                .forEach((id) -> {
                    getFeedLogRes(postLogs, id);
                });

        return postLogs;
    }

    @Transactional(readOnly = true)
    public List<GetFeedLogRes> getFeedHistoryByTime(PostUserLogTimeReq req) {

        LocalDateTime startTime = req.getStartTime();
        LocalDateTime endTime = req.getEndTime();

        List<Long> revIds = getRevIds();

        List<GetFeedLogRes> postLogs = new ArrayList<>();

        revIds.stream()
                .forEach((id) -> {
                    getFeedLogResByTime(postLogs, id, startTime, endTime);
                });

        return postLogs;
    }

    private void getFeedLogResByType(List<GetFeedLogRes> postLogs, Long rev, String revType) {

        String rType = revType;

        Revisions<Long, Feed> revisions = feedRepository.findRevisions(rev);

        for (Revision<Long, Feed> revision : revisions.getContent()) {
            if (String.valueOf(revision.getMetadata().getRevisionType()).equals(rType)) {
                postLogs.add(makeGetPostLogRes(revision));
            }
        }
    }

    private void getFeedLogRes(List<GetFeedLogRes> postLogs, Long rev) {

        Revisions<Long, Feed> revisions = feedRepository.findRevisions(rev);
        for (Revision<Long, Feed> revision : revisions.getContent()) {
            postLogs.add(makeGetPostLogRes(revision));
        }
    }

    private void getFeedLogResByTime(List<GetFeedLogRes> postLogs, Long rev,
                                     LocalDateTime startTime, LocalDateTime endTime) {

        Revisions<Long, Feed> revisions = feedRepository.findRevisions(rev);
        for (Revision<Long, Feed> revision : revisions.getContent()) {
            Instant requiredRevisionInstant = revision.getMetadata().getRequiredRevisionInstant();
            LocalDateTime localDateTime = LocalDateTime.ofInstant(requiredRevisionInstant, ZoneId.of("Asia/Seoul"));

            if (!localDateTime.isBefore(startTime) && !localDateTime.isAfter(endTime)) {
                GetFeedLogRes getFeedLogRes = makeGetPostLogRes(revision);
                postLogs.add(getFeedLogRes);
            }

        }
    }

    private GetFeedLogRes makeGetPostLogRes(Revision<Long, Feed> revision) {
        Long revisionNumber = revision.getMetadata().getRevisionNumber().get();
        String revisionType = String.valueOf(revision.getMetadata().getRevisionType());

        Instant requiredRevisionInstant = revision.getMetadata().getRequiredRevisionInstant();
        LocalDateTime localDateTime = LocalDateTime.ofInstant(requiredRevisionInstant, ZoneId.of("Asia/Seoul"));
        return new GetFeedLogRes(revisionNumber, revisionType, localDateTime);
    }

    private List<Long> getRevIds() {
        return auditReader.createQuery()
                .forRevisionsOfEntity(Feed.class, false, false)
                .addProjection(AuditEntity.id())
                .getResultList();
    }

    // PATCH
    public void modifyPostContent(Long postId, PatchFeedReq patchPostReq) {
        Feed feed = feedRepository.findByIdAndState(postId, ACTIVE)
                .orElseThrow(() -> new BaseException(NOT_FIND_FEED));
        feed.updateContent(patchPostReq.getContent());
    }

    // DELETE
    public void deleteFeed(Long postId) {
        Feed feed = feedRepository.findByIdAndState(postId, ACTIVE)
                .orElseThrow(() -> new BaseException(NOT_FIND_FEED));
        feed.deleteFeed();
    }

}