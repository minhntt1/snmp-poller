package com.home.network.statistic.vendor;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VendorDTO {
    private final Pattern searchVendorPattern = Pattern.compile("([a-zA-Z0-9]{6})\\s+\\(base 16\\)\\s+(.+)");
    private String content;

    public VendorDTO(String content) {
        this.content = content;
    }

    public List<VendorEntity> extract() {
        Matcher matcher = searchVendorPattern.matcher(this.content);
        List<VendorEntity> vendors = new ArrayList<>();

        while (matcher.find()) {
            vendors.add(new VendorEntity(
                matcher.group(1),
                matcher.group(2) // can be utf8
            ));
        }

        return vendors;
    }
}
