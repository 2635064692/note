package jerry.jsonbeans.utils;

import java.io.Closeable;
import java.io.IOException;

/**
 * @author Jerry Ou
 * @version 1.0 2016-05-03 13:59
 * @since JDK 1.6
 */
public class IOUtils {

    public static void close(Closeable closeable) throws IOException {
        if (closeable != null) {
            closeable.close();
        }
    }

}
