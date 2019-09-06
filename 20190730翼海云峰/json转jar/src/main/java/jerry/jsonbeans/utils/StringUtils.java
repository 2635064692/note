package jerry.jsonbeans.utils;

/**
 * @author Jerry Ou
 * @version 1.0 2016-05-03 13:59
 * @since JDK 1.6
 */
public class StringUtils {

    /**
     * 首字母小写
     *
     * @param str 需要处理的字符串
     * @return 转换后的字符串
     */
    public static String firstCharToLowerCase(String str) {
        char firstChar = str.charAt(0);
        if (firstChar >= 'A' && firstChar <= 'Z') {
            char[] arr = str.toCharArray();
            arr[0] += ('a' - 'A');
            return new String(arr);
        }
        return str;
    }

    /**
     * 首字母变大写
     *
     * @param str 需要处理的字符串
     * @return 转换后的字符串
     */
    public static String firstCharToUpperCase(String str) {
        char firstChar = str.charAt(0);
        if (firstChar >= 'a' && firstChar <= 'z') {
            char[] arr = str.toCharArray();
            arr[0] -= ('a' - 'A');
            return new String(arr);
        }
        return str;
    }

    public static boolean isNullOrEmpty(String firstLine) {
        return firstLine == null || firstLine.length() == 0;
    }

}
