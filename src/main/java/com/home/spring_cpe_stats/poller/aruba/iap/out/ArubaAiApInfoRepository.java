package com.home.spring_cpe_stats.poller.aruba.iap.out;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ArubaAiApInfoRepository extends JpaRepository<ArubaAiApInfoEntity, Long> {
}
