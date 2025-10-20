package com.home.network.statistic.vendor;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VendorDTO {
    private static final Pattern searchVendorPattern = Pattern.compile("([a-zA-Z0-9]{6})\\s+\\(base 16\\)\\s+(.+)");
    private final String content;

    public VendorDTO(String content) {
        this.content = content;
    }

    public List<VendorEntity> extract() {
        Matcher matcher = searchVendorPattern.matcher(this.content);
        List<VendorEntity> vendors = new ArrayList<>();

        while (matcher.find()) {
            var prefix = matcher.group(1);
            var name = matcher.group(2);
            var vendor = new VendorEntity(prefix, name/*can be utf8*/);

            vendors.add(vendor);
        }

        return vendors;
    }
}
