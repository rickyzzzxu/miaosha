package com.miaoshaproject.util;

import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.math.BigInteger;

public class HexUtil {

    public static void main(String[] args) {
        System.out.println(hexStrToDouble("0x0000803f"));
    }
        /**
         * hexStr 字符串转换为    字符串
         * @param  hexStr
         * @return
         */
        public static String hexStrToString(String hexStr){
            String str = "0123456789ABCDEF";
            char[] hexs = hexStr.toCharArray();
            byte[] bytes = new byte[hexStr.length() / 2];
            int n;
            for (int i = 0; i < bytes.length; i++) {
                n = str.indexOf(hexs[2 * i]) * 16;
                n += str.indexOf(hexs[2 * i + 1]);
                bytes[i] = (byte) (n & 0xff);
            }
            String returnStr = new String(bytes);
            return returnStr;
        }


        /**
         * hexStr 转 UnsignedLong
         * @param hexStr
         * @return
         */
        public static Long hexStrToUnsignedLong(String hexStr){
            if(StringUtils.isEmpty(hexStr)){
                return null;
            }
            return Long.parseUnsignedLong(hexStr,16);
        }

        /**
         * hexStr 转 Double
         * @param hexStr
         * @return
         */
        public static Double hexStrToDouble(String hexStr){
            if(StringUtils.isEmpty(hexStr)){
                return null;
            }
            long longBits = Long.valueOf(hexStr,16).longValue();
            return Double.longBitsToDouble(longBits);
        }

        /**
         * hexStr 转 Float
         * @param hexStr
         * @return
         */
        public static Float hexStrToFloat(String hexStr){
            if(StringUtils.isEmpty(hexStr)){
                return null;
            }
            Integer integerBits = Integer.valueOf(hexStr,16);
            return Float.intBitsToFloat(integerBits);
        }

        /**
         * hexStr 转 BigDecimal
         * @param hexStr
         * @return
         */
        public static BigDecimal hexStrToBigDecimal(String hexStr){
            if(StringUtils.isEmpty(hexStr)){
                return null;
            }
            BigInteger bigInteger = new BigInteger(hexStr, 16);
            return new BigDecimal(bigInteger);
        }

        /**
         * hexStr 转 Byte
         * @param hexStr
         * @return
         */
        public static Byte hexStrToByte(String hexStr){
            if(StringUtils.isEmpty(hexStr)){
                return null;
            }
            return Byte.parseByte(hexStr,16);
        }
        /**
         * hexStr 转 Integer
         * @param hexStr
         * @return
         */
        public static Integer hexStrToInteger(String hexStr){
            if(StringUtils.isEmpty(hexStr)){
                return null;
            }
            return Integer.parseInt(hexStr,16);
        }

        /**
         * hexStr 转 BigInteger
         * @param hexStr
         * @return
         */
        public static BigInteger hexStrToBigInteger(String hexStr){
            if(StringUtils.isEmpty(hexStr)){
                return null;
            }
            return new BigInteger(hexStr,16);
        }

        /**
         * 将十六进制的字符串转换成字节数组
         *
         * @param hexString
         * @return
         */
        public static byte[] hexStrToByteArrs(String hexString) {
            if (StringUtils.isEmpty(hexString)) {
                return null;
            }

            hexString = hexString.replaceAll(" ", "");
            int len = hexString.length();
            int index = 0;

            byte[] bytes = new byte[len / 2];

            while (index < len) {
                String sub = hexString.substring(index, index + 2);
                bytes[index / 2] = (byte) Integer.parseInt(sub, 16);
                index += 2;
            }

            return bytes;
        }

        /**
         * 数组转换成十六进制字符串
         *
         * @param bArray
         * @return HexString
         */
        public static final String bytesToHexString(byte[] bArray) {
            StringBuffer sb = new StringBuffer(bArray.length);
            String sTemp;
            for (int i = 0; i < bArray.length; i++) {
                sTemp = Integer.toHexString(0xFF & bArray[i]);
                if (sTemp.length() < 2){
                    sb.append(0);
                }
                sb.append(sTemp.toUpperCase());
            }
            return sb.toString();
        }
}
