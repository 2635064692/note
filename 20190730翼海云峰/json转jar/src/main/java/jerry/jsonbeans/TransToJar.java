package jerry.jsonbeans;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

/**
 * @author zhanghai by 2019/8/19
 */
public class TransToJar {
    private String javaSourcePath;
    private String javaClassPath;
    private String targetPath;
    private String libPath;
    private List<String> filePaths = new ArrayList<>();
    private List<String> libPathsList = new ArrayList<String>() {
        {
            add("lombok-1.16.20.jar;");
            add("spring-context-5.1.5.RELEASE.jar;");
            add("swagger-annotations-1.5.20.jar;");
            add("validation-api-2.0.1.Final.jar");

        }
    };

    public TransToJar(String javaSourcePath, String javaClassPath, String targetPath, String libPath) {
        String absolutePath = new File("").getAbsolutePath();
        String replace = absolutePath.replace("\\", "/");
        this.javaSourcePath = replace +"/"+ javaSourcePath;
        this.javaClassPath = replace +"/"+ javaClassPath;
        this.targetPath = replace + "/"+targetPath;
        this.libPath = replace + "/"+libPath;
        File file = new File(javaClassPath);
        if (!file.exists())
            file.mkdirs();

    }

    public void compile() {
        File libFile = new File(libPath);
        StringBuilder libPathBuilder = new StringBuilder();
        StringBuilder javaFilesBuilder = new StringBuilder();
        if (libFile.exists()) {
            for (String path : libPathsList) {
                libPathBuilder.append(libFile.getAbsolutePath() + "\\" + path);
            }
        }
        getFiles(new File(javaSourcePath).getAbsolutePath());
        for (String path : filePaths) {
            javaFilesBuilder.append(path + " ");
        }
        String content = "javac -cp " + libPathBuilder.toString() + " -d " + new File(javaClassPath).getAbsolutePath() + " " + javaFilesBuilder;
        System.out.println(content);

        CommandLine cmdLine = CommandLine.parse(content);

        DefaultExecutor exec = new DefaultExecutor();

        ExecuteWatchdog watchdog = new ExecuteWatchdog(ExecuteWatchdog.INFINITE_TIMEOUT);
        DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler();

        exec.setWatchdog(watchdog);

        try {
            exec.execute(cmdLine, resultHandler);
            System.out.println("*** --> java源代码编译完成。");
            resultHandler.waitFor(10_000);
            this.generateJar();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("************* --> java源代码编译失败。");
        }
    }

    private void getFiles(String path) {

        File file = new File(path);
        File[] files = file.listFiles();
        if (files == null)
            return;
        for (int i = 0; i < files.length; i++) {
            if (files[i].isDirectory())
                getFiles(files[i].getAbsolutePath());
            else
                filePaths.add(files[i].getAbsolutePath());
        }
    }

    public void generateJar() throws IOException {

        System.out.println("*** --> 开始生成jar包...");
        String targetDirPath = targetPath.substring(0, targetPath.lastIndexOf("/"));
        File targetDir = new File(targetDirPath);
        if (!targetDir.exists()) {
            targetDir.mkdirs();
        }

        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");

        JarOutputStream target = new JarOutputStream(new FileOutputStream(targetPath), manifest);
        writeClassFile(new File(javaClassPath), target);
        target.close();
        System.out.println("*** --> jar包生成完毕。");
    }

    private void writeClassFile(File source, JarOutputStream target) throws IOException {
        BufferedInputStream in = null;
        try {
            if (source.isDirectory()) {
                String name = source.getPath().replace("\\", "/");
                if (!name.isEmpty()) {
                    if (!name.endsWith("/")) {
                        name += "/";
                    }
                    name = name.substring(javaClassPath.length() + 1);
                    if (!name.equals("")) {
                        JarEntry entry = new JarEntry(name);
                        entry.setTime(source.lastModified());
                        target.putNextEntry(entry);
                        target.closeEntry();
                    }
                }
                for (File nestedFile : source.listFiles())
                    writeClassFile(nestedFile, target);
                return;
            }

            String middleName = source.getPath().replace("\\", "/").substring(javaClassPath.length() + 1);
            JarEntry entry = new JarEntry(middleName);
            entry.setTime(source.lastModified());
            target.putNextEntry(entry);
            in = new BufferedInputStream(new FileInputStream(source));

            byte[] buffer = new byte[1024];
            while (true) {
                int count = in.read(buffer);
                if (count == -1)
                    break;
                target.write(buffer, 0, count);
            }
            target.closeEntry();
        } finally {
            if (in != null)
                in.close();
        }
    }

}
