package com.mapgis.agent.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MemoryUtils {
    private static int staticPorts;

    public static Integer getPid(Integer port){
        staticPorts = port;
        MemoryUtils utils = new MemoryUtils();
        String pid = null;
        Runtime runtime = Runtime.getRuntime();
        try {
            //查找进程号
            Process p = runtime.exec("cmd /c netstat -ano | findstr \"" + port + "\"");
            InputStream inputStream = p.getInputStream();
            String read = utils.read(inputStream, "UTF-8", 1);
            if (read == null) {
                //System.out.println("找不到" + port + "端口的进程");
                return -1;
            } else {
                //System.out.println(read);
                pid = utils.format(read);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Integer.parseInt(pid);
    }

    private String read(InputStream in,String charset,int flag) throws IOException{
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, charset));
        String line;
        if(flag ==1){
            while ((line = reader.readLine()) != null) {
                boolean validPort = validPort(line);

                if (validPort) {
                    break;
                }
            }
        }
        else {
            while ((line = reader.readLine())!=null){
                if (line.contains("java"))
                    break;
            }
        }
        reader.close();
        return line;
    }

    private boolean validPort(String str) {
        Pattern pattern = Pattern.compile("^ *[a-zA-Z]+ +\\S+");
        Matcher matcher = pattern.matcher(str);

        matcher.find();
        String find = matcher.group();
        int spstart = find.lastIndexOf(":");
        find = find.substring(spstart + 1);

        int port;
        try {
            port = Integer.parseInt(find);
        } catch (NumberFormatException e) {
            System.out.println("查找到错误的端口:" + find);
            return false;
        }
        if (staticPorts==port) {
            return true;
        } else {
            return false;
        }
    }

    private String format(String read) {

        int end = read.lastIndexOf(" ");
        String spid = read.substring(end);
        spid = spid.replace(" ", "");
        return spid;
    }


    public static String getMemory(Integer pid){
        MemoryUtils utils = new MemoryUtils();
        String serverMemory = null;
        StringBuffer sb = new StringBuffer();
        Runtime runtime = Runtime.getRuntime();
        try {
            Process p = runtime.exec("cmd /c wmic process list brief | findstr \"" + pid + "\"");
            InputStream inputStream = p.getInputStream();
            String read = utils.read(inputStream, "GBK", 2);
            if (read==null)
                return null;
            else{
                serverMemory =utils.format(read.trim());
                //System.out.println("找到" + read + "进程，正在准备清理");
                float memory = Float.parseFloat(serverMemory) / 1024 /1024;
                int j = 0;
                serverMemory = String.format("%.1f",memory);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return serverMemory+" MB";
    }
}
