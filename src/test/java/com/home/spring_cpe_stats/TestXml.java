package com.home.spring_cpe_stats;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

import java.util.Properties;

public class TestXml {
    @SneakyThrows
    @Test
    void test() {
        Properties props = new Properties();
//        props.loadFromXML(getClass().getResourceAsStream("/src/test/resources/test.xml"));
        System.out.println(getClass().getClassLoader().getResource("test.xml").getPath());
    }
}
