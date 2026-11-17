package com.greenproject.classway.global;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class TsidUtil {

    private static final char[] BASE32_ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * 'ORD' + '-' + 일자(YYYYMMDD) + '-' + TSID 형태의 주문번호 생성
     * 예: ORD-20261116-0H5P9Q8B2Z4X1
     */
    public static String generateOrderNo() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String tsid = generateTsidString();
        return "ORD-" + dateStr + "-" + tsid;
    }

    private static String generateTsidString() {
        long time = System.currentTimeMillis();
        long random = RANDOM.nextLong() & 0x3FFFFF; // 22-bit random
        long id = (time << 22) | random;

        char[] chars = new char[13];
        for (int i = 12; i >= 0; i--) {
            chars[i] = BASE32_ALPHABET[(int) (id & 0x1F)];
            id >>>= 5;
        }
        return new String(chars);
    }
}
