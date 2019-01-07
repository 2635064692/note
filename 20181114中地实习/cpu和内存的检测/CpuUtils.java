package com.mapgis.agent.utils;

import com.jezhumble.javasysmon.CpuTimes;
import com.jezhumble.javasysmon.JavaSysMon;
import com.jezhumble.javasysmon.ProcessInfo;

public class CpuUtils {
    private static JavaSysMon monitor = new JavaSysMon();
    private static String period = "1000";

    public static String getProcessUsage(Integer pid) {

        long cpuTotalTimeBegin = getCpuTotalTime(monitor);
        long processTotalTimeBegin = getProcessTotalTimeByPid(monitor, pid);

        try {
            Thread.sleep(Long.parseLong(period));
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        long cpuTotalTimeEnd = getCpuTotalTime(monitor);
        long processTotalTimeEnd = getProcessTotalTimeByPid(monitor, pid);

        float processUsage = (float) (processTotalTimeEnd - processTotalTimeBegin)
                / (float) (cpuTotalTimeEnd - cpuTotalTimeBegin);

        if(processUsage<0)
            return "0.0%";
        return String.format("%.1f", processUsage * 100)+"%";

    }

    private static long getCpuTotalTime(JavaSysMon monitor) {

        return monitor.cpuTimes().getTotalMillis();
    }

    private static long getProcessTotalTimeByPid(JavaSysMon monitor, int pid) {

        ProcessInfo processInfo = findProcessByPid(monitor, pid);

        if (processInfo == null) {
            return 0;
        }

        return processInfo.getSystemMillis() + processInfo.getUserMillis();

    }

    public static ProcessInfo findProcessByPid(JavaSysMon monitor, int pid) {

        ProcessInfo[] processInfos = monitor.processTable();

        for (ProcessInfo processInfo : processInfos) {

            if (pid == processInfo.getPid()) {
                return processInfo;
            }
        }

        return null;
    }

    public static void getCpuUsge() {

        JavaSysMon monitor = new JavaSysMon();

        while (true) {

            CpuTimes cpuTimes = monitor.cpuTimes();

            System.out.println("\n--------------------");

            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            CpuTimes cpuTimes2 = monitor.cpuTimes();

            String cpuUsage = String.format("%.5f", cpuTimes2.getCpuUsage(cpuTimes));

            System.out.println("cpuUsage :" + cpuUsage);

        }

    }

}
