/**
 * 
 * Copyright 2011 MilkBowl (https://github.com/MilkBowl)
 * 
 * This work is licensed under the Creative Commons
 * Attribution-NonCommercial-ShareAlike 3.0 Unported License. To view a copy of
 * this license, visit http://creativecommons.org/licenses/by-nc-sa/3.0/ or send
 * a letter to Creative Commons, 444 Castro Street, Suite 900, Mountain View,
 * California, 94041, USA.
 * 
 */

package net.milkbowl.localshops.threads;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.logging.Logger;

import net.milkbowl.localshops.Config;

import org.bukkit.plugin.java.JavaPlugin;


public class ReportThread extends Thread {
    protected final Logger log = Logger.getLogger("Minecraft");
    private boolean run = true;
    private JavaPlugin plugin = null;

    public ReportThread(JavaPlugin plugin) {
        this.plugin = plugin;
    }
    
    public void setRun(boolean run) {
        this.run = run;
    }

    public void run() {
        // Obtain values
        String osName = System.getProperty("os.name");
        String osArch = System.getProperty("os.arch");
        String osVersion = System.getProperty("os.version");
        String java = System.getProperty("java.vendor") + " " + System.getProperty("java.version");
        String pluginName = plugin.getDescription().getName();
        String pluginVersion = plugin.getDescription().getVersion();
        String bukkitVersion = plugin.getServer().getVersion();

        if (Config.getSrvDebug()) {
            log.info(String.format("[%s] Start of ReportThread", pluginName));
        }

        while (true) {
            try {
                URL statsUrl = new URL(String.format(
                                                        "%s?uuid=%s&plugin=%s&version=%s&bVersion=%s&osName=%s&osArch=%s&osVersion=%s&java=%s",
                                                        Config.getSrvReportUrl(),
                                                        URLEncoder.encode(Config.getSrvUuid().toString(), "ISO-8859-1"),
                                                        URLEncoder.encode(pluginName, "ISO-8859-1"),
                                                        URLEncoder.encode(pluginVersion, "ISO-8859-1"),
                                                        URLEncoder.encode(bukkitVersion, "ISO-8859-1"),
                                                        URLEncoder.encode(osName, "ISO-8859-1"),
                                                        URLEncoder.encode(osArch, "ISO-8859-1"),
                                                        URLEncoder.encode(osVersion, "ISO-8859-1"),
                                                        URLEncoder.encode(java, "ISO-8859-1")
                                                    ));
                URLConnection conn = statsUrl.openConnection();
                conn.setRequestProperty("User-Agent", String.format("%s %s:%s", "BukkitReport", pluginName, pluginVersion));
                String inputLine;
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));

                if (Config.getSrvDebug()) {
                    // Output the contents -- wee
                    log.info(String.format("[%s] StatsURL: %s", pluginName, statsUrl.toString()));
                    while ((inputLine = in.readLine()) != null) {
                        if (inputLine.equals("<pre>") || inputLine.equals("</pre>")) {
                            continue;
                        }
                        log.info(String.format("[%s] %s", pluginName, inputLine));
                    }
                }

                in.close();

                // Sleep for 6 hours...
                for (int i = 0; i < Config.getSrvReportInterval(); i++) {
                    if (!Config.getSrvReport() || !run) {
                        if (Config.getSrvDebug()) {
                            log.info(String.format("[%s] Graceful quit of ReportThread", pluginName));
                        }
                        return;
                    }
                    Thread.sleep(1000);
                }
            } catch (Exception e) {
                // Ignore it...really its just not important
                return;
            }
        }
    }
}