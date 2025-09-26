package com.home.network.statistic.poller.aruba.iap.out;

import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Profile({"dev-executor","prd-executor"})
@Repository
public interface ArubaAiApInfoRepository extends JpaRepository<ArubaAiApInfoEntity, Long> {
}
