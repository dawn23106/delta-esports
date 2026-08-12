package com.delta.esports.payment;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YunGouOsSignerTest {
    @Test
    void shouldSortFieldsAndCreateUppercaseMd5Signature() {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("mch_id", "123456");
        fields.put("out_trade_no", "DE202608090001");
        fields.put("total_fee", "35.00");

        String first = YunGouOsSigner.sign(fields, "secret");
        Map<String, Object> reversed = new LinkedHashMap<>();
        reversed.put("total_fee", "35.00");
        reversed.put("out_trade_no", "DE202608090001");
        reversed.put("mch_id", "123456");

        assertEquals(first, YunGouOsSigner.sign(reversed, "secret"));
        assertTrue(first.matches("[0-9A-F]{32}"));
    }
}
