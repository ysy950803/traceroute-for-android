package com.wandroid.traceroute;

import android.text.TextUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 通过ping实现traceroute
 */
public class TraceRouteByPing {

    private static final int PACKET_SIZE = 60;
    private String expectIP;
    private String realHostAndIP;
    private boolean reachedHost;
    private IOutputCallback outputCallback;

    public void execute(int maxHops, String host, IOutputCallback callback) {
        outputCallback = callback;
        String command = "ping -A -w 3 -W 5 -c 1 -s " + PACKET_SIZE + " -t %d " + host;
        String ip = "*";
        try {
            ip = InetAddress.getByName(host).getHostAddress();
            expectIP = ip;
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        if (outputCallback != null) {
            outputCallback.onAppend("traceroute to " + host + " (" + ip + "), " + maxHops + " hops max, " + PACKET_SIZE + " byte packets", false, false);
        }
        for (int ttl = 1; ttl <= maxHops; ttl++) {
            String result = getTraceResult(String.format(command, ttl));
            if (outputCallback != null) {
                outputCallback.onAppend(ttl + "  " + result, false, false);
            }
            if (outputCallback == null || reachedHost || ttl == maxHops) break;
        }
    }

    public void exit() {
        if (outputCallback != null) {
            outputCallback.onAppend("* * * finished! * * *\n", true, reachedHost);
            outputCallback = null;
        }
    }

    private String getTraceResult(String cmd) {
        final String[] result = {null};
        execCmd(cmd, line -> {
            result[0] = handleEachLine(line);
            return result[0] != null;
        });
        return result[0] == null ? "* * *" : result[0];
    }

    private String handleEachLine(String line) {
        String hostAndIP = null, handleFmt = null;
        try {
            if (line.contains("PING ") && TextUtils.isEmpty(realHostAndIP)) {
                realHostAndIP = extractBetween(line, "PING ", " " + PACKET_SIZE + "\\(");
            }
            if (line.contains("From ")) {
                hostAndIP = extractBetween(line, "From ", ": ");
            } else if (line.contains(" bytes from ")) {
                reachedHost = true;
                hostAndIP = extractBetween(line, " bytes from ", ": ");
                if (!TextUtils.equals(expectIP, extractIP(realHostAndIP))) {
                    hostAndIP = realHostAndIP;
                }
            }
            if (hostAndIP != null) handleFmt = hostAndIP + "%s";
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return handleFmt != null ? String.format(handleFmt, getPingTimes(hostAndIP)) : null;
    }

    private static String getPingTimes(String ip) {
        final StringBuilder times = new StringBuilder();
        ip = extractIP(ip);
        if (!TextUtils.isEmpty(ip)) {
            execCmd("ping -A -w 3 -W 5 -c 3 -s " + PACKET_SIZE + " " + ip, line -> {
                try {
                    if (line.contains(" bytes from ")) {
                        String timeInMs = line.split("time=")[1];
                        times.append("  ").append(timeInMs);
                    }
                } catch (Exception ignore) {
                }
                return false;
            });
        }
        String result = times.toString();
        return TextUtils.isEmpty(result) ? "  * * *" : result;
    }

    private static String extractBetween(String str, String head, String tail) {
        if (TextUtils.isEmpty(str)) return null;
        Pattern pattern = Pattern.compile(head + "(.+)" + tail);
        Matcher matcher = pattern.matcher(str);
        return matcher.find() ? matcher.group(1) : null;
    }

    private static String extractIP(String str) {
        if (TextUtils.isEmpty(str)) return null;
        Pattern pattern = Pattern.compile("\\b\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\b");
        Matcher matcher = pattern.matcher(str);
        String lastIP = null;
        while (matcher.find()) lastIP = matcher.group();
        return lastIP;
    }

    private interface ILineConsumer<T> {
        boolean onEach(T t);
    }

    private static void execCmd(String cmd, ILineConsumer<String> consumer) {
        Process process = null;
        BufferedReader reader = null;
        try {
            process = Runtime.getRuntime().exec(cmd);
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (consumer.onEach(line)) break;
            }
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        } finally {
            try {
                if (reader != null) reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (process != null) process.destroy();
        }
    }
}
