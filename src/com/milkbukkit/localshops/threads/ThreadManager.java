package com.milkbukkit.localshops.threads;

import java.util.logging.Logger;

import com.milkbukkit.localshops.Config;
import com.milkbukkit.localshops.LocalShops;

public class ThreadManager {
    LocalShops plugin = null;
    
    // Threads
    DynamicThread dt = null;
    NotificationThread nt = null;
    ReportThread rt = null;
    
    // Logging
    private final Logger log = Logger.getLogger("Minecraft");
    
    public ThreadManager(LocalShops plugin) {
        this.plugin = plugin;
    }
    
    public void dynamicStart() {
        if (dt == null || !dt.isAlive()) {
            dt = new DynamicThread(plugin);
            dt.start();
        }
    }
    
    public void dynamicStop() {
        if(dt != null && dt.isAlive()) {
            dt.setRun(false);
            try {
                dt.join(2000);
            } catch(InterruptedException e) {
                // ruh roh
                log.warning(String.format("[%s] %s", plugin.getDescription().getName(), "DynamicThread did not exit"));
            }
        }
    }
    
    public void notificationStart() {
        if (nt == null || !nt.isAlive()) {
            nt = new NotificationThread(plugin);
            nt.start();
        }
    }
    
    public void notificationStop() {
        if(nt != null && nt.isAlive()) {
            Config.setShopTransactionNotice(false);
            try {
                nt.join(2000);
            } catch(InterruptedException e) {
                // ruh roh
                log.warning(String.format("[%s] %s", plugin.getDescription().getName(), "NotificationThread did not exit"));
            }
        }
    }
    
    public void reportStart() {
        if (rt == null || !rt.isAlive()) {
            rt = new ReportThread(plugin);
            rt.start();
        }
    }
    
    public void reportStop() {
        if(rt != null && rt.isAlive()) {
            Config.setSrvReport(false);
            try {
                rt.join(2000);
            } catch(InterruptedException e) {
                // ruh roh
                log.warning(String.format("[%s] %s", plugin.getDescription().getName(), "ReportThread did not exit"));
            }
        }
    }
}