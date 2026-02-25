package com.mypkga.commerceplatformfull.repository;

import com.mypkga.commerceplatformfull.entity.WalletTransaction;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {
    List<WalletTransaction> findByWalletId(Long walletId, Sort sort);
}
