package jerry.jsonbeans.utils;

import java.io.*;
import java.net.URL;
import java.util.Properties;

/**
 * @author Jerry Ou
 * @version 1.0 2016-05-03 13:59
 * @since JDK 1.6
 */
public class FileUtils {

    public static String filePath(String fileName) {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        URL url = loader.getResource(fileName);
        if (url == null) {
            url = FileUtils.class.getClassLoader().getResource(fileName);
            if (url == null) {
                url = FileUtils.class.getResource(fileName);
            }
        }
        return url.getFile();
    }

    public static void loadProperties(Properties properties, String path) {
        loadProperties(properties, new File(path));
    }

    public static void loadProperties(Properties properties, File file) {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            properties.load(fis);

        } catch (IOException e) {
            throw new RuntimeException("load properties error", e);
        } finally {
            try {
                IOUtils.close(fis);
            } catch (IOException e) {
                // ignore
            }
        }
    }

    public static String[] readFile(String path) {
        File file = new File(path);
        return readFile(file);
    }

    public static String[] readFile(File file) {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
            String firstLine = br.readLine();

            if (StringUtils.isNullOrEmpty(firstLine) || StringUtils.isNullOrEmpty(firstLine.trim())) {
                throw new RuntimeException("The File must be not empty");
            }

            String packageName = "json.bean";
            String className = "JsonBean";
            String json = "";

            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null)
                sb.append(line);

            if (firstLine.trim().equals("{")) {
                json = firstLine + sb.toString();
            } else {
                json = sb.toString();
                String[] arr = firstLine.split(" ");
                if (arr.length == 1) {
                    if (arr[0].contains(".")) {
                        packageName = arr[0];
                    } else {
                        className = arr[0];
                    }
                } else {
                    if (arr[1].contains(".")) {
                        packageName = arr[1];
                        className = arr[0];
                    } else {
                        packageName = arr[0];
                        className = arr[1];
                    }
                }
            }
            return new String[]{
                    packageName,
                    className,
                    json
            };
        } catch (IOException e) {
            throw new RuntimeException("read file error", e);
        } finally {
            try {
                IOUtils.close(br);
            } catch (IOException e) {
                //ignore
            }
        }
    }

    public static void write(String s, File file) {
        try {
            if (file.exists()) {
                file.delete();
            } else {
                file.getParentFile().mkdirs();
            }

            BufferedWriter wb = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)));
            wb.write(s);
            wb.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void writeFiles(String s, File file) {
        try {
            if (file.exists()) {
                file.delete();
            } else {
                file.getParentFile().mkdirs();
            }

            BufferedWriter wb = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)));
            wb.write(s);
            wb.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
