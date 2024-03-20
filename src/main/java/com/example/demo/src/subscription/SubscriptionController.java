package com.example.demo.src.subscription;

import com.example.demo.common.entity.BaseEntity;
import com.example.demo.common.response.BaseResponse;
import com.example.demo.src.subscription.model.GetSubscriptionRes;
import com.example.demo.src.subscription.model.PatchSubscriptionReq;
import com.example.demo.src.subscription.model.PostSubscriptionReq;
import com.example.demo.src.subscription.model.PostSubscriptionRes;
import com.example.demo.utils.JwtService;
import com.example.demo.utils.MessageUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.parameters.P;
import org.springframework.web.bind.annotation.*;
import retrofit2.http.Path;

import java.util.List;

import static com.example.demo.common.entity.BaseEntity.*;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/app/subscriptions")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final JwtService jwtService;
    private final MessageUtils messageUtils;

    /**
     *  구독 추가 API
     * [POST] /app/subscriptions
     * @return BaseResponse<PostSubscriptionRes>
     */
    @PostMapping("")
    public BaseResponse<PostSubscriptionRes> createSubscription(@RequestBody PostSubscriptionReq req) {
        Long userId = jwtService.getUserId();
        PostSubscriptionRes subscriptionRes = subscriptionService.createSubscription(userId, req);
        return new BaseResponse<>(subscriptionRes, messageUtils.getMessage("SUCCESS"));
    }

    /**
     *  구독 내역 조회 API
     * [GET] /app/subscriptions
     * @return BaseResponse<List<GetSubscriptionRes>>
     */
    @GetMapping("")
    public BaseResponse<List<GetSubscriptionRes>> getSubscriptions() {
        Long userId = jwtService.getUserId();
        List<GetSubscriptionRes> subscriptions = subscriptionService.getSubscriptions(userId);
        return new BaseResponse<>(subscriptions, messageUtils.getMessage("SUCCESS"));
    }

    /**
     *  구독 내역 1개 조회 API
     * [GET] /app/subscriptions/:subscriptionId
     * @return BaseResponse<GetSubscriptionRes>
     */
    @GetMapping("/{subscriptionId}")
    public BaseResponse<GetSubscriptionRes> getSubscription(@PathVariable("subscriptionId") Long subscriptionId) {
        jwtService.getUserId();
        GetSubscriptionRes subscription = subscriptionService.getSubscription(subscriptionId);
        return new BaseResponse<>(subscription, messageUtils.getMessage("SUCCESS"));
    }

    /**
     *  구독 내역 수정 API
     * [PATCH] /app/subscriptions/:subscriptionId
     * ResponseBody: PatchSubscriptionReq
     * - nextPaymentDate: 다음 결제 일자
     *
     * @return BaseResponse<String>
     */
    @PatchMapping("/{subscriptionId}")
    public BaseResponse<String> modifySubscriptionNextPaymentDate(@PathVariable("subscriptionId") Long subscriptionId, @RequestBody PatchSubscriptionReq req) {
        jwtService.getUserId();
        subscriptionService.modifyNextSubscriptionDate(subscriptionId, req);
        return new BaseResponse<>(messageUtils.getMessage("MODIFY_SUBSCRIPTION_SUCCESS"), messageUtils.getMessage("SUCCESS"));
    }

    /**
     *  구독 내역 상태 수정 API
     * [PATCH] /app/subscriptions/:subscriptionId ?state=
     *
     * @return BaseResponse<String>
     */
    @PatchMapping("/{subscriptionId}/state")
    public BaseResponse<String> modifySubscriptionState(@PathVariable("subscriptionId") Long subscriptionId, @RequestParam("state") State state) {
        jwtService.getUserId();
        subscriptionService.modifySubscriptionState(subscriptionId, state);
        return new BaseResponse<>(messageUtils.getMessage("MODIFY_SUBSCRIPTION_SUCCESS"), messageUtils.getMessage("SUCCESS"));
    }

    /**
     *  구독 내역 삭제 API
     * [DELETE] /app/subscriptions
     *
     * @return BaseResponse<String>
     */
    @DeleteMapping("/{subscriptionId}")
    public BaseResponse<String> deleteSubscription(@PathVariable("subscriptionId") Long subscriptionId) {
        jwtService.getUserId();
        subscriptionService.deleteSubscription(subscriptionId);
        return new BaseResponse<>(messageUtils.getMessage("DELETE_SUBSCRIPTION_SUCCESS"), messageUtils.getMessage("SUCCESS"));
    }

}
