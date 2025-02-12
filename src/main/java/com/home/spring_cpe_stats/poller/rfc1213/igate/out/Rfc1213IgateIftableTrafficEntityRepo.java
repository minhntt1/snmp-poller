package com.home.spring_cpe_stats.poller.rfc1213.igate.out;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface Rfc1213IgateIftableTrafficEntityRepo extends JpaRepository<Rfc1213IgateIftableTrafficEntity, Long> {
}
