package com.mypkga.commerceplatformfull.service;

import com.mypkga.commerceplatformfull.entity.User;
import com.mypkga.commerceplatformfull.entity.Wallet;
import com.mypkga.commerceplatformfull.entity.WalletTransaction;
import com.mypkga.commerceplatformfull.repository.UserRepository;
import com.mypkga.commerceplatformfull.repository.WalletRepository;
import com.mypkga.commerceplatformfull.repository.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final UserRepository userRepository;

    /**
     * Get or create wallet for a user
     */
    @Transactional
    public Wallet getOrCreateWallet(Long userId) {
        return walletRepository.findByUserId(userId).orElseGet(() -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found: " + userId));
            Wallet wallet = new Wallet();
            wallet.setUser(user);
            wallet.setBalance(BigDecimal.ZERO);
            Wallet saved = walletRepository.save(wallet);
            log.info("Created new wallet for user {}", userId);
            return saved;
        });
    }

    /**
     * Credit money to a user's wallet (e.g., refund)
     */
    @Transactional
    public WalletTransaction credit(Long userId, BigDecimal amount, String description, Long returnRequestId) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Số tiền phải lớn hơn 0");
        }

        Wallet wallet = getOrCreateWallet(userId);
        wallet.setBalance(wallet.getBalance().add(amount));
        walletRepository.save(wallet);

        WalletTransaction tx = new WalletTransaction();
        tx.setWallet(wallet);
        tx.setAmount(amount);
        tx.setType(WalletTransaction.TransactionType.REFUND);
        tx.setDescription(description);
        tx.setRelatedReturnRequestId(returnRequestId);
        tx.setBalanceAfter(wallet.getBalance());

        WalletTransaction saved = walletTransactionRepository.save(tx);
        log.info("Credited {} to wallet of user {} (return request: {})", amount, userId, returnRequestId);
        return saved;
    }

    /**
     * Get wallet balance for a user
     */
    public BigDecimal getBalance(Long userId) {
        return walletRepository.findByUserId(userId)
                .map(Wallet::getBalance)
                .orElse(BigDecimal.ZERO);
    }

    /**
     * Get transaction history for a user (newest first)
     */
    public List<WalletTransaction> getTransactionHistory(Long userId) {
        Wallet wallet = walletRepository.findByUserId(userId).orElse(null);
        if (wallet == null)
            return List.of();
        return walletTransactionRepository.findByWalletId(
                wallet.getId(),
                Sort.by(Sort.Direction.DESC, "createdAt"));
    }
}
