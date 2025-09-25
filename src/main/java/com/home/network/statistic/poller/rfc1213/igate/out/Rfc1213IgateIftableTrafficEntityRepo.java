package com.home.network.statistic.poller.rfc1213.igate.out;


import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
@Profile({"dev-executor","prd-executor"})
public interface Rfc1213IgateIftableTrafficEntityRepo extends JpaRepository<Rfc1213IgateIftableTrafficEntity, Long> {
}
