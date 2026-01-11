package com.home.network.statistic.poller.igate.gw240.out;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

public class TestStatusWifiStationRaw {
    @Test
    void testWebResponseOk() {
        // arrange
        var resp = "\t\t\t\t<tr><td width=&quot;96&quot; align=center class=tabdata><font color=&quot;#000000&quot;>1</font></td><td width=&quot;96&quot; align=center class=tabdata><font color=&quot;#000000&quot;>F2:18:FB:95:C7:5E</font></td><td width=&quot;96&quot; align=center class=tabdata><font color=&quot;#000000&quot;>Yes</font></td><td width=&quot;96&quot; align=center class=tabdata><font color=&quot;#000000&quot;>WPA2PSK</font></td><td width=&quot;96&quot; align=center class=tabdata><font color=&quot;#000000&quot;>TUANNGA&#45;1</font></td><td width=&quot;96&quot; align=center class=tabdata><font color=&quot;#000000&quot;>rai0</font></td><tr><td width=&quot;96&quot; align=center class=tabdata><font color=&quot;#000000&quot;>2</font></td><td width=&quot;96&quot; align=center class=tabdata><font color=&quot;#000000&quot;>52:B8:EF:AA:B2:41</font></td><td width=&quot;96&quot; align=center class=tabdata><font color=&quot;#000000&quot;>Yes</font></td><td width=&quot;96&quot; align=center class=tabdata><font color=&quot;#000000&quot;>WPA2PSK</font></td><td width=&quot;96&quot; align=center class=tabdata><font color=&quot;#000000&quot;>TUANNGA&#45;1</font></td><td width=&quot;96&quot; align=center class=tabdata><font color=&quot;#000000&quot;>rai0</font></td>\n";
        var raw = new StatusWifiStationRaw(LocalDateTime.now(), resp, List.of());

        // act & assert
        List<StatusWifiStationWebDataRaw> statusWifiStationWebDataRaws = raw.parseWebClientResponseToObjects();
        Assertions.assertEquals(2, statusWifiStationWebDataRaws.size());
    }

    @Test
    void testWebResponseEmpty() {
        // arrange
        var resp = "\t\t\t\t<tr><td width=&quot;96&quot; align=center class=tabdata><font color=&quot;#000000&quot;>1</font></td><td width=&quot;96&quot; align=center class=tabdata><font color=&quot;#000000&quot;>1E:AC:7F:C1:90:B3</font></td><td width=&quot;96&quot; align=center class=tabdata><font color=&quot;#000000&quot;>Yes</font></td><td width=&quot;96&quot; align=center class=tabdata><font color=&quot;#000000&quot;></font></td><td width=&quot;96&quot; align=center class=tabdata><font color=&quot;#000000&quot;>TUANNGA&#45;1</font></td><td width=&quot;96&quot; align=center class=tabdata><font color=&quot;#000000&quot;>rai0</font></td>\r";
        var raw = new StatusWifiStationRaw(LocalDateTime.now(), resp, List.of());

        // act & assert
        List<String> strings = raw.parseWebResponseToList();
        List<StatusWifiStationWebDataRaw> statusWifiStationWebDataRaws = raw.parseWebClientResponseToObjects();
        System.out.println(strings);

        Assertions.assertEquals(1, statusWifiStationWebDataRaws.size());
    }

    @Test
    void testWebResponseNoData() {
        // arrange
        var resp = "";
        var raw = new StatusWifiStationRaw(LocalDateTime.now(), resp, List.of());

        // act & assert
        List<StatusWifiStationWebDataRaw> statusWifiStationWebDataRaws = raw.parseWebClientResponseToObjects();
        Assertions.assertEquals(0, statusWifiStationWebDataRaws.size());
    }
}
