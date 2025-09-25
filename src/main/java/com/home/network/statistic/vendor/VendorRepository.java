package com.home.network.statistic.vendor;

import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Profile({"dev-executor","prd-executor"})
@Repository
public interface VendorRepository extends JpaRepository<VendorEntity, Integer> {
    Optional<VendorEntity> findFirstByVendorPrefix(Integer vendorPrefix);
}
