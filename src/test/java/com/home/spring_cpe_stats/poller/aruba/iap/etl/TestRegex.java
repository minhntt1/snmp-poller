package com.home.spring_cpe_stats.poller.aruba.iap.etl;

import org.junit.jupiter.api.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestRegex {
    @Test
    void test() {
        String content = """
                OUI/MA-L                                                    Organization                                \s
                company_id                                                  Organization                                \s
                                                                            Address                                     \s
                
                10-E9-92   (hex)		INGRAM MICRO SERVICES
                10E992     (base 16)		INGRAM MICRO SERVICES
                				100 CHEMIN DE BAILLOT
                				MONTAUBAN    82000
                				FR
                
                78-F2-76   (hex)		Cyklop Fastjet Technologies (Shanghai) Inc.
                78F276     (base 16)		Cyklop Fastjet Technologies (Shanghai) Inc.
                				No 18?Lane 699, Zhang Wengmiao Rd,  Fengxian district, Shanghai China
                				Shanghai    201401
                				CN
                
                28-6F-B9   (hex)		Nokia Shanghai Bell Co., Ltd.
                286FB9     (base 16)		Nokia Shanghai Bell Co., Ltd.
                				No.388 Ning Qiao Road,Jin Qiao Pudong Shanghai
                				Shanghai     201206
                				CN
                
                """;

        Pattern regex = Pattern.compile("([a-zA-Z0-9]{6})\\s+\\(base 16\\)\\s+(.+)");
        Matcher matcher = regex.matcher(content);
        while(matcher.find()) {
            System.out.println(matcher.group(1) + " " + matcher.group(2));
        }
    }
}
