package vn.vnpay.common.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ResponseTimeUtil {
    public static String getCurrentTime() {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
            return LocalDateTime.now().format(formatter);
        } catch (Exception e) {
            return null;
        }
    }
}
