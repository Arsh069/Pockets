package com.marsh.pockets.transaction.util;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public final class UpiQrParser {

    private UpiQrParser() {
    }

    public record UpiQrPayload(
        String payeeUpiId,
        BigDecimal amount,
        String payeeName,
        String note
    ) {
    }

    public static UpiQrPayload parse(String rawPayload) {
        if (rawPayload == null || rawPayload.trim().isEmpty()) {
            throw new IllegalArgumentException("UPI QR payload cannot be null or empty");
        }

        URI uri;
        try {
            uri = URI.create(rawPayload.trim());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid UPI QR URI syntax: " + e.getMessage(), e);
        }

        if (uri.getScheme() == null || !uri.getScheme().equalsIgnoreCase("upi")) {
            throw new IllegalArgumentException("Invalid UPI QR scheme: expected 'upi' URI");
        }

        String query = uri.getRawQuery();
        if (query == null) {
            String ssp = uri.getRawSchemeSpecificPart();
            if (ssp != null && ssp.contains("?")) {
                query = ssp.substring(ssp.indexOf('?') + 1);
            }
        }

        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("Invalid UPI QR: query parameters missing");
        }

        Map<String, String> params = new HashMap<>();
        for (String pair : query.split("&")) {
            if (pair.trim().isEmpty()) {
                continue;
            }
            int eqIdx = pair.indexOf('=');
            String key;
            String val;
            if (eqIdx > 0) {
                key = URLDecoder.decode(pair.substring(0, eqIdx), StandardCharsets.UTF_8).trim().toLowerCase();
                val = eqIdx < pair.length() - 1 ? URLDecoder.decode(pair.substring(eqIdx + 1), StandardCharsets.UTF_8).trim() : "";
            } else {
                key = URLDecoder.decode(pair, StandardCharsets.UTF_8).trim().toLowerCase();
                val = "";
            }
            params.put(key, val);
        }

        String pa = params.get("pa");
        if (pa == null || pa.isEmpty()) {
            throw new IllegalArgumentException("Invalid UPI QR: missing required payee VPA ('pa' parameter)");
        }

        BigDecimal amount = null;
        String amStr = params.get("am");
        if (amStr != null && !amStr.isEmpty()) {
            try {
                amount = new BigDecimal(amStr);
                if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new IllegalArgumentException("Invalid UPI QR: amount must be greater than zero");
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid UPI QR: malformed amount value '" + amStr + "'", e);
            }
        }

        String pn = params.get("pn");
        if (pn != null && pn.isEmpty()) {
            pn = null;
        }

        String tn = params.get("tn");
        if (tn != null && tn.isEmpty()) {
            tn = null;
        }

        return new UpiQrPayload(pa, amount, pn, tn);
    }
}
