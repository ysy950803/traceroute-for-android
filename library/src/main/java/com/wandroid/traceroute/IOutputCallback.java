package com.wandroid.traceroute;

public interface IOutputCallback {
    void onAppend(String output, boolean finished, boolean reachedHost);
}
