package com.home.network.statistic.vendor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Profile({"dev-executor","prd-executor"})
public class VendorService {
    private final String lookupUrl = "https://standards-oui.ieee.org/";
    private final RestClient restClient;
    private final VendorRepository vendorRepository;

//    @Scheduled(fixedRate = 1, timeUnit = TimeUnit.DAYS)
    @Transactional(value = "appTx")
    public void run() {
        log.info("fetch vendors from remote url");

        String response = restClient
                .get()
                .uri(this.lookupUrl)
                .retrieve()
                .body(String.class);

        log.info("fetch vendors from remote success");

        VendorDTO vendorDTO = new VendorDTO(response);

        List<VendorEntity> vendorEntityList = vendorDTO.extract();

        log.info("save vendors to database");

        for (VendorEntity vendorEntity : vendorEntityList) {
            Optional<VendorEntity> vendorEntity1 = vendorRepository
                    .findFirstByVendorPrefix(vendorEntity.getVendorPrefix());

            if (vendorEntity1.isPresent() && vendorEntity1.get().diffName(vendorEntity)) {
                vendorEntity1.get().setVendorName(vendorEntity.getVendorName());// update latest
            } else if (vendorEntity1.isEmpty()) {
                vendorRepository.save(vendorEntity);
            }
        }

        log.info("save vendors to database finished");
    }
}
