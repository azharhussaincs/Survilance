package com.cms.util;

import java.net.InetAddress;
import java.net.URL;
import java.util.regex.Pattern;

/**
 * Input validation utilities for network addresses, URLs, and form data.
 */
public class ValidationUtil {
    private static final Pattern IP_PATTERN = Pattern.compile(
        "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"
    );
    private static final Pattern HOSTNAME_PATTERN = Pattern.compile(
        "^([a-zA-Z0-9]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?\\.)*[a-zA-Z0-9]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?$"
    );

    public static boolean isValidIpAddress(String ip) {
        return ip != null && IP_PATTERN.matcher(ip.trim()).matches();
    }

    public static boolean isValidHostname(String hostname) {
        return hostname != null && HOSTNAME_PATTERN.matcher(hostname.trim()).matches();
    }

    public static boolean isValidIpOrHostname(String value) {
        if (value == null || value.isBlank()) return false;
        String trimmed = value.trim();
        return isValidIpAddress(trimmed) || isValidHostname(trimmed);
    }

    public static boolean isValidUrl(String urlStr) {
        try {
            new URL(urlStr);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isValidPort(int port) {
        return port > 0 && port <= 65535;
    }

    public static boolean isValidPort(String portStr) {
        try {
            int port = Integer.parseInt(portStr.trim());
            return isValidPort(port);
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static int extractPortFromUrl(String url) {
        try {
            URL parsed = new URL(url);
            int port = parsed.getPort();
            return port == -1 ? parsed.getDefaultPort() : port;
        } catch (Exception e) {
            return -1;
        }
    }

    public static String extractHostFromUrl(String url) {
        try {
            if (!url.startsWith("http") && !url.startsWith("rtsp")) {
                url = "http://" + url;
            }
            URL parsed = new URL(url);
            return parsed.getHost();
        } catch (Exception e) {
            return url;
        }
    }

    public static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    public record ValidationResult(boolean valid, String message) {
        public static ValidationResult ok() { return new ValidationResult(true, ""); }
        public static ValidationResult error(String msg) { return new ValidationResult(false, msg); }
    }
}
