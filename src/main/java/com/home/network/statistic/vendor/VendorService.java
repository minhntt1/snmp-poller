package com.home.network.statistic.vendor;

import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Profile({"dev-executor","prd-executor"})
public class VendorService {
    private static final String lookupUrl = "https://standards-oui.ieee.org/";
    private final RestClient restClient;
    private final VendorRepository vendorRepository;

    // use jpa tx instead of jdbc tx
    @Transactional(value = "appJpaTx")
    @Timed(value = "macvendor.poll")
    public void run() {
        log.info("fetch vendors from remote url");

        String response = restClient
                .get()
                .uri(lookupUrl)
                .retrieve()
                .body(String.class);

        log.info("fetch vendors from remote success");

        VendorDTO vendorDTO = new VendorDTO(response);

        // extract vendor entity from response
        // use map for better lookup
        List<VendorEntity> vendorListAPI = vendorDTO.extract();

        log.info("save vendors to database");

        // find all entity in db
        var vendorListDb = VendorEntity.toMap(vendorRepository.findAll());

        // loop over all data in API
        for (var vendor : vendorListAPI) {
            // get corresponding db record
            var vendorDb = vendorListDb.get(vendor.getVendorPrefix());

            // if no db record, save current to DB
            if (vendorDb == null) {
                vendorRepository.save(vendor);
                continue;
            }

            // otherwise, check if two have diffrent name, if yes, update db name
            vendorDb.updateName(vendor);
        }

        log.info("save vendors to database finished");
    }
}
