package com.home.spring_cpe_stats.vendor;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VendorRepository extends JpaRepository<VendorEntity, Integer> {
    Optional<VendorEntity> findFirstByVendorPrefix(Integer vendorPrefix);
}
