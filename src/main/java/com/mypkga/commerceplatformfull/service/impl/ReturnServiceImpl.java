package com.mypkga.commerceplatformfull.service.impl;

import com.mypkga.commerceplatformfull.dto.CreateReturnRequestDto;
import com.mypkga.commerceplatformfull.dto.ghn.GHNCreateOrderResponse;
import com.mypkga.commerceplatformfull.dto.ghn.GHNFeeResponse;
import com.mypkga.commerceplatformfull.entity.*;
import com.mypkga.commerceplatformfull.exception.InvalidReturnStatusException;
import com.mypkga.commerceplatformfull.exception.ReturnNotEligibleException;
import com.mypkga.commerceplatformfull.exception.VideoUploadException;
import com.mypkga.commerceplatformfull.repository.OrderRepository;
import com.mypkga.commerceplatformfull.repository.ReturnRequestRepository;
import com.mypkga.commerceplatformfull.repository.ReturnRequestHistoryRepository;
import com.mypkga.commerceplatformfull.repository.UserRepository;
import com.mypkga.commerceplatformfull.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReturnServiceImpl implements ReturnService {

    private final ReturnRequestRepository returnRequestRepository;
    private final ReturnRequestHistoryRepository historyRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ReturnEligibilityService returnEligibilityService;
    private final FileService fileService;
    private final ReturnCodeService returnCodeService;
    private final NotificationService notificationService;
    private final GHNReturnService ghnReturnService;

    @Override
    @Transactional
    public ReturnRequest createReturnRequest(Long orderId, CreateReturnRequestDto dto) {
        log.info("Creating return request for order ID: {}", orderId);

        // Validate order eligibility
        if (!returnEligibilityService.isEligibleForReturn(orderId)) {
            ReturnEligibilityService.EligibilityResult result = returnEligibilityService.getEligibilityResult(orderId);
            log.warn("Order {} is not eligible for return: {}", orderId, result.getReason());
            throw new ReturnNotEligibleException(result.getReason());
        }

        // Fetch required entities
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ReturnNotEligibleException("Order not found"));

        // Upload evidence video
        String evidenceVideoUrl;
        try {
            FileService.FileUploadResult uploadResult = fileService.uploadVideo(
                    dto.getEvidenceVideo(),
                    "return-evidence/" + orderId);
            evidenceVideoUrl = uploadResult.getPublicUrl();
            log.info("Evidence video uploaded successfully for order {}: {}", orderId, evidenceVideoUrl);
        } catch (Exception e) {
            log.error("Failed to upload evidence video for order {}", orderId, e);
            throw new VideoUploadException("Failed to upload evidence video", e);
        }

        // Create return request entity
        ReturnRequest returnRequest = new ReturnRequest();
        returnRequest.setOrder(order);
        returnRequest.setReason(dto.getReason());
        returnRequest.setDetailedDescription(dto.getDetailedDescription());
        returnRequest.setEvidenceVideoUrl(evidenceVideoUrl);
        returnRequest.setStatus(ReturnStatus.REFUND_REQUESTED);

        // Set bank information
        RefundBankInfo bankInfo = new RefundBankInfo();
        bankInfo.setBankName(dto.getBankInfo().getBankName());
        bankInfo.setAccountNumber(dto.getBankInfo().getAccountNumber());
        bankInfo.setAccountHolderName(dto.getBankInfo().getAccountHolderName());
        returnRequest.setBankInfo(bankInfo);

        // Save return request first to get ID for return code generation
        returnRequest = returnRequestRepository.save(returnRequest);
        log.info("Return request created with ID: {}", returnRequest.getId());

        // Generate return code
        try {
            String returnCode = returnCodeService.generateReturnCode(returnRequest);
            returnRequest.setReturnCode(returnCode);
            returnRequest = returnRequestRepository.save(returnRequest);
            log.info("Return code generated for request {}: {}", returnRequest.getId(), returnCode);
        } catch (Exception e) {
            log.error("Failed to generate return code for request {}", returnRequest.getId(), e);
            // Don't fail the entire transaction for return code generation failure
            // The return request is still valid without the code
        }

        // Update order status to REFUND_REQUESTED
        order.updateCurrentStatus(OrderStatus.REFUND_REQUESTED);
        // IMPORTANT: Set bidirectional relationship explicitly
        // This ensures order.getReturnRequest() returns the created request
        order.setReturnRequest(returnRequest);
        orderRepository.save(order);
        log.info("Order {} status updated to REFUND_REQUESTED and linked to return request {}", orderId,
                returnRequest.getId());

        log.info("Return request created successfully for order {}", orderId);
        return returnRequest;
    }

    @Override
    public List<ReturnRequest> getPendingReturnRequests() {
        log.debug("Fetching pending return requests");
        return returnRequestRepository.findPendingReturnRequests();
    }

    @Override
    @Transactional
    public ReturnRequest approveReturnRequest(Long requestId, Long staffId) {
        log.info("Approving return request {} by staff {}", requestId, staffId);

        ReturnRequest returnRequest = returnRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Return request not found"));

        User staff = userRepository.findById(staffId)
                .orElseThrow(() -> new IllegalArgumentException("Staff user not found"));

        // Validate status transition
        if (!returnRequest.canUpdateStatus(ReturnStatus.RETURN_APPROVED)) {
            throw new InvalidReturnStatusException(
                    String.format("Cannot approve return request in status %s", returnRequest.getStatus()));
        }

        // Calculate GHN shipping fee first
        GHNFeeResponse feeResponse = null;
        GHNCreateOrderResponse ghnResponse = null;

        try {
            log.info("Calculating GHN return shipping fee for request: {}", requestId);
            feeResponse = ghnReturnService.calculateReturnShippingFee(returnRequest);

            if (feeResponse != null && feeResponse.getCode() == 200) {
                log.info("GHN shipping fee calculated: {} VND for request {}",
                        feeResponse.getData().getTotal(), requestId);

                // Create GHN return shipment order
                log.info("Creating GHN return order for return request: {}", requestId);
                ghnResponse = ghnReturnService.createReturnShippingOrder(returnRequest);

                if (ghnResponse != null && ghnResponse.getCode() == 200) {
                    // Update return request with GHN information
                    returnRequest.setGhnOrderCode(ghnResponse.getData().getOrderCode());
                    returnRequest.setGhnTrackingNumber(ghnResponse.getData().getSortCode());
                    returnRequest.setGhnFee(feeResponse.getData().getTotal());

                    log.info("Successfully created GHN order {} for return request {}",
                            ghnResponse.getData().getOrderCode(), requestId);
                } else {
                    log.error("Failed to create GHN order for return request {}: {}",
                            requestId, ghnResponse != null ? ghnResponse.getMessage() : "Unknown error");
                }
            } else {
                log.error("Failed to calculate GHN shipping fee for return request {}: {}",
                        requestId, feeResponse != null ? feeResponse.getMessage() : "Unknown error");
            }
        } catch (Exception e) {
            log.error("Error with GHN service for return request {}, continuing with manual processing", requestId, e);
            // Continue without GHN - manual processing
        }

        // Update return request status
        returnRequest.updateStatus(ReturnStatus.RETURN_APPROVED, staff);
        returnRequest = returnRequestRepository.save(returnRequest);

        // Update order status
        Order order = returnRequest.getOrder();
        order.updateCurrentStatus(OrderStatus.RETURN_APPROVED);
        orderRepository.save(order);

        // Save history record
        ReturnRequestHistory history = ReturnRequestHistory.createApprovalHistory(
                returnRequest,
                staff,
                ghnResponse != null ? ghnResponse.getData().getOrderCode() : null,
                feeResponse != null ? feeResponse.getData().getTotal() : null);
        historyRepository.save(history);

        // Send approval notification email - if this fails, rollback the entire
        // transaction
        boolean emailSent = notificationService.sendApprovalNotification(returnRequest);
        if (!emailSent) {
            log.error("Failed to send approval notification email for return request {}, rolling back transaction",
                    requestId);
            throw new RuntimeException("Failed to send approval notification email");
        }

        log.info("Return request {} approved successfully", requestId);
        return returnRequest;
    }

    @Override
    @Transactional
    public ReturnRequest rejectReturnRequest(Long requestId, String reason, Long staffId) {
        log.info("Rejecting return request {} by staff {} with reason: {}", requestId, staffId, reason);

        ReturnRequest returnRequest = returnRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Return request not found"));

        User staff = userRepository.findById(staffId)
                .orElseThrow(() -> new IllegalArgumentException("Staff user not found"));

        // Validate status transition
        if (!returnRequest.canUpdateStatus(ReturnStatus.REFUND_REJECTED)) {
            throw new InvalidReturnStatusException(
                    String.format("Cannot reject return request in status %s", returnRequest.getStatus()));
        }

        // Update return request status and rejection reason
        returnRequest.updateStatus(ReturnStatus.REFUND_REJECTED, staff);
        returnRequest.setRejectionReason(reason);
        returnRequest = returnRequestRepository.save(returnRequest);

        // Revert order status back to DELIVERED
        Order order = returnRequest.getOrder();
        order.updateCurrentStatus(OrderStatus.DELIVERED);
        orderRepository.save(order);

        // Save history record
        ReturnRequestHistory history = ReturnRequestHistory.createRejectionHistory(returnRequest, staff, reason);
        historyRepository.save(history);

        // Send rejection notification email - if this fails, rollback the entire
        // transaction
        boolean emailSent = notificationService.sendRejectionNotification(returnRequest);
        if (!emailSent) {
            log.error("Failed to send rejection notification email for return request {}, rolling back transaction",
                    requestId);
            throw new RuntimeException("Failed to send rejection notification email");
        }

        log.info("Return request {} rejected successfully", requestId);
        return returnRequest;
    }

    @Override
    @Transactional
    public ReturnRequest confirmShipping(String returnCode) {
        log.info("Confirming shipping for return code: {}", returnCode);

        if (returnCode == null || returnCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Return code cannot be null or empty");
        }

        ReturnRequest returnRequest = returnRequestRepository.findByReturnCode(returnCode)
                .orElseThrow(() -> new IllegalArgumentException("Return request not found for code: " + returnCode));

        // Validate status transition
        if (!returnRequest.canUpdateStatus(ReturnStatus.RETURNING)) {
            throw new InvalidReturnStatusException(
                    String.format("Cannot confirm shipping for return request in status %s",
                            returnRequest.getStatus()));
        }

        // Update return request status
        returnRequest.setStatus(ReturnStatus.RETURNING);
        returnRequest.setUpdatedAt(LocalDateTime.now());
        returnRequest = returnRequestRepository.save(returnRequest);

        // Update order status
        Order order = returnRequest.getOrder();
        order.updateCurrentStatus(OrderStatus.RETURNING);
        orderRepository.save(order);

        // Save history record
        ReturnRequestHistory history = ReturnRequestHistory.createShippingHistory(returnRequest,
                returnRequest.getGhnOrderCode());
        historyRepository.save(history);

        log.info("Shipping confirmed for return request {}", returnRequest.getId());
        return returnRequest;
    }

    @Override
    @Transactional
    public ReturnRequest confirmReceipt(Long requestId, Long staffId) {
        log.info("Confirming receipt for return request {} by staff {}", requestId, staffId);

        ReturnRequest returnRequest = returnRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Return request not found"));

        User staff = userRepository.findById(staffId)
                .orElseThrow(() -> new IllegalArgumentException("Staff user not found"));

        // Validate status transition
        if (!returnRequest.canUpdateStatus(ReturnStatus.RETURN_RECEIVED)) {
            throw new InvalidReturnStatusException(
                    String.format("Cannot confirm receipt for return request in status %s", returnRequest.getStatus()));
        }

        // Update return request status
        returnRequest.updateStatus(ReturnStatus.RETURN_RECEIVED, staff);
        returnRequest = returnRequestRepository.save(returnRequest);

        // Update order status
        Order order = returnRequest.getOrder();
        order.updateCurrentStatus(OrderStatus.RETURN_RECEIVED);
        orderRepository.save(order);

        // Save history record
        ReturnRequestHistory history = ReturnRequestHistory.createReceiptHistory(returnRequest, staff);
        historyRepository.save(history);

        log.info("Receipt confirmed for return request {}", requestId);
        return returnRequest;
    }

    @Override
    @Transactional
    public ReturnRequest completeRefund(Long requestId, Long staffId) {
        log.info("Completing refund for return request {} by staff {}", requestId, staffId);

        ReturnRequest returnRequest = returnRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Return request not found"));

        User staff = userRepository.findById(staffId)
                .orElseThrow(() -> new IllegalArgumentException("Staff user not found"));

        // Validate status transition
        if (!returnRequest.canUpdateStatus(ReturnStatus.REFUNDED)) {
            throw new InvalidReturnStatusException(
                    String.format("Cannot complete refund for return request in status %s", returnRequest.getStatus()));
        }

        // Update return request status
        returnRequest.updateStatus(ReturnStatus.REFUNDED, staff);
        returnRequest = returnRequestRepository.save(returnRequest);

        // Update order status
        Order order = returnRequest.getOrder();
        order.updateCurrentStatus(OrderStatus.REFUNDED);
        orderRepository.save(order);

        // Save history record
        ReturnRequestHistory history = ReturnRequestHistory.createRefundHistory(returnRequest, staff);
        historyRepository.save(history);

        // Send completion notification email - if this fails, rollback the entire
        // transaction
        boolean emailSent = notificationService.sendCompletionNotification(returnRequest);
        if (!emailSent) {
            log.error("Failed to send completion notification email for return request {}, rolling back transaction",
                    requestId);
            throw new RuntimeException("Failed to send completion notification email");
        }

        log.info("Refund completed for return request {}", requestId);
        return returnRequest;
    }

    @Override
    public boolean isEligibleForReturn(Long orderId) {
        return returnEligibilityService.isEligibleForReturn(orderId);
    }

    @Override
    public List<ReturnRequest> getReturningRequests() {
        log.debug("Fetching returning requests");
        return returnRequestRepository.findReturningRequests();
    }

    @Override
    public List<ReturnRequest> getApprovedReturnRequests() {
        log.debug("Fetching approved return requests");
        return returnRequestRepository.findApprovedRequests();
    }

    @Override
    public List<ReturnRequest> getReturnRequestsByCustomer(Long customerId) {
        log.debug("Fetching return requests for customer {}", customerId);
        return returnRequestRepository.findByCustomerId(customerId);
    }

    @Override
    public ReturnRequest findByReturnCode(String returnCode) {
        if (returnCode == null || returnCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Return code cannot be null or empty");
        }

        return returnRequestRepository.findByReturnCode(returnCode)
                .orElseThrow(() -> new IllegalArgumentException("Return request not found for code: " + returnCode));
    }

    @Override
    @Transactional
    public ReturnRequest confirmPackageReceiptWithPhoto(String returnCode,
            org.springframework.web.multipart.MultipartFile receiptPhoto) {
        if (returnCode == null || returnCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Return code cannot be null or empty");
        }

        ReturnRequest returnRequest = returnRequestRepository.findByReturnCode(returnCode)
                .orElseThrow(() -> new IllegalArgumentException("Return request not found for code: " + returnCode));

        // Kiểm tra trạng thái hợp lệ
        if (returnRequest.getStatus() != ReturnStatus.RETURNING) {
            throw new IllegalStateException("Return request must be in RETURNING status to confirm receipt");
        }

        try {
            // Cập nhật trạng thái - GHN sẽ tự động cập nhật qua webhook
            returnRequest.setStatus(ReturnStatus.RETURN_RECEIVED);
            returnRequest.setUpdatedAt(LocalDateTime.now());

            ReturnRequest savedRequest = returnRequestRepository.save(returnRequest);

            log.info("Confirmed package receipt for return code {}", returnCode);
            return savedRequest;

        } catch (Exception e) {
            log.error("Error confirming package receipt for return code {}: {}", returnCode, e.getMessage());
            throw new RuntimeException("Failed to confirm package receipt: " + e.getMessage(), e);
        }
    }

    @Override
    public ReturnRequest findById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Return request ID cannot be null");
        }

        return returnRequestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Return request not found with ID: " + id));
    }

    @Override
    public List<ReturnRequestHistory> getReturnRequestHistory(Long returnRequestId) {
        if (returnRequestId == null) {
            throw new IllegalArgumentException("Return request ID cannot be null");
        }

        return historyRepository.findByReturnRequestIdOrderByCreatedAtDesc(returnRequestId);
    }
}