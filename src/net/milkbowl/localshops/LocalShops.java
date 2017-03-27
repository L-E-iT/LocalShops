/**
 * 
 * Copyright 2011 MilkBowl (https://github.com/MilkBowl)
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 * 
 */
package net.milkbowl.localshops;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.milkbowl.localshops.commands.ShopCommandExecutor;
import net.milkbowl.localshops.listeners.ShopsBlockListener;
import net.milkbowl.localshops.listeners.ShopsEntityListener;
import net.milkbowl.localshops.listeners.ShopsPlayerListener;
import net.milkbowl.localshops.listeners.ShopsMoveListener;
import net.milkbowl.localshops.objects.MsgType;
import net.milkbowl.localshops.objects.PlayerData;
import net.milkbowl.localshops.objects.Shop;
import net.milkbowl.localshops.objects.ShopSign;
import net.milkbowl.localshops.threads.ThreadManager;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;

import org.bukkit.Bukkit;
import org.bukkit.Location;

import org.bukkit.command.CommandExecutor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Local Shops Plugin
 * 
 * @author Jonbas
 */
public class LocalShops extends JavaPlugin {
    // Listeners & Objects

    public ShopsPlayerListener playerListener = new ShopsPlayerListener(this);
    public ShopsBlockListener blockListener = new ShopsBlockListener(this);
    public ShopsEntityListener entityListener = new ShopsEntityListener(this);
    public ShopsMoveListener vehicleListener = new ShopsMoveListener(this);
    // Managers
    private ShopManager shopManager = new ShopManager(this);
    private DynamicManager dynamicManager = new DynamicManager();
    private ThreadManager threadManager = new ThreadManager(this);
    private ResourceManager resManager = null;
    // Services
    private Economy econ = null;
    private Permission perm = null;
    // Logging
    private static final Logger log = Logger.getLogger("Minecraft");
    private Map<String, PlayerData> playerData = Collections.synchronizedMap(new HashMap<String, PlayerData>());

    @Override
    public void onLoad() {
        Config.load();
    }

    public void onEnable() {
        resManager = new ResourceManager(getDescription(), new Locale(Config.getLocale()));
        log.info(resManager.getString(MsgType.MAIN_USING_LOCALE, new String[]{"%LOCALE%"}, new String[]{resManager.getLocale().getLanguage()}));
        // Get services
        if (!retrieveServices()) {
        	log.info("[LocalShops] - An Econ or Permission plugin was not detected - Check to make sure you have both!");
        	this.getServer().getPluginManager().disablePlugin(this);
        	return;
        }
        // add all the online users to the data trees
        for (Player player : Bukkit.getServer().getOnlinePlayers()) {
            getPlayerData().put(player.getName(), new PlayerData(this, player.getName()));
        }

        // Register our events
        PluginManager pm = getServer().getPluginManager();
        if(Config.getSrvMoveEvents()) {
            // Only register movement based events if move-events is enabled
            pm.registerEvents(vehicleListener, this);
        }
        pm.registerEvents(playerListener, this);
        pm.registerEvents(blockListener, this);
        pm.registerEvents(entityListener, this);

        // Register Commands
        CommandExecutor cmdExec = new ShopCommandExecutor(this);
        getCommand("lshop").setExecutor(cmdExec);
        getCommand("lsadmin").setExecutor(cmdExec);
        getCommand("gshop").setExecutor(cmdExec);
        getCommand("buy").setExecutor(cmdExec);
        getCommand("sell").setExecutor(cmdExec);
        getCommand("gbuy").setExecutor(cmdExec);
        getCommand("gsell").setExecutor(cmdExec);


        // setup the file IO
        File folderDir = new File(Config.getDirPath());
        File shopsDir = new File(Config.getDirShopsActivePath());
        if(folderDir.mkdir()) {
            log.log(Level.INFO, "Created directory ''{0}''", folderDir.getName());
        }
        if(shopsDir.mkdir()) {
            log.log(Level.INFO, "Created directory ''{0}''", shopsDir.getName());
        }

        // read the shops into memory
        getShopManager().loadShops(shopsDir);

        // update the console that we've started
        log.info(resManager.getString(MsgType.MAIN_LOAD, new String[]{"%NUM_SHOPS%"}, new Object[]{getShopManager().getNumShops()}));
        log.info(resManager.getString(MsgType.MAIN_ENABLE, new String[]{"%UUID%"}, new Object[]{Config.getSrvUuid().toString()}));

        // check which shops players are inside
        for (Player player : this.getServer().getOnlinePlayers()) {
            checkPlayerPosition(player);
        }

        // Start Notification thread
        if (Config.getShopTransactionNotice()) {
            threadManager.notificationStart();
        }

        // Start Scheduler thread
        threadManager.schedulerStart();

    }

    public boolean retrieveServices() {
        RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
        if (economyProvider != null) {
            this.econ = economyProvider.getProvider();
            log.info(String.format("[%s] Using Economy Provider %s", getDescription().getName(), econ.getName()));
        }
       
        RegisteredServiceProvider<Permission> permissionProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.permission.Permission.class);
        if (permissionProvider != null) {
            this.perm = permissionProvider.getProvider();
            log.info(String.format("[%s] Using Permission Provider %s", getDescription().getName(), perm.getName()));
        }
        
        return econ != null && perm != null;
    }

    public void onDisable() {
        // Save all shops
        getShopManager().saveAllShops();

        // Save config file
        Config.save();

        // Stop Scheduler thread
        threadManager.schedulerStop();

        // Stop Notification thread
        threadManager.notificationStop();

        // update the console that we've stopped
        log.info(resManager.getString(MsgType.MAIN_DISABLE, new String[]{}, new Object[]{}));
    }

    public Economy getEcon() {
        return econ;
    }

    public Permission getPerm() {
        return perm;
    }

    public ShopManager getShopManager() {
        return shopManager;
    }

    public Map<String, PlayerData> getPlayerData() {
        return playerData;
    }

    public ThreadManager getThreadManager() {
        return threadManager;
    }

    public DynamicManager getDynamicManager() {
        return dynamicManager;
    }

    public ResourceManager getResourceManager() {
        return resManager;
    }

    //Workaround for Bukkits inability to update multiple Signs in the same Tick
    public void scheduleUpdate(ShopSign sign, int delay) {
        getServer().getScheduler().scheduleSyncDelayedTask(this, new UpdateSignState(sign), delay);
    }

    private static class UpdateSignState implements Runnable {

        private ShopSign sign = null;

        public UpdateSignState(ShopSign sign) {
            this.sign = sign;
        }

        @Override
        public void run() {
            sign.getLoc().getBlock().getState().update(true);
        }
    }

    public void checkPlayerPosition(Player player, Location location) {
        PlayerData pData = getPlayerData().get(player.getName());

        Shop shop = getShopManager().getLocalShop(location);

        if (shop == null) {
            // not in a shop...
            for (UUID uuid : pData.shopList) {
                notifyPlayerLeftShop(player, uuid);
            }
            pData.shopList.clear();
            return;
        }

        if (!pData.shopList.contains(shop.getUuid())) {
            // Player was not in the shop, and now is...
            pData.shopList.add(shop.getUuid());
            notifyPlayerEnterShop(player, shop.getUuid());
        }
    }

    public void checkPlayerPosition(Player player) {
        checkPlayerPosition(player, player.getLocation());
    }

    private void notifyPlayerLeftShop(Player player, UUID shopUuid) {
        // TODO Add formatting
        Shop shop = getShopManager().getLocalShop(shopUuid);
        if(shop != null) {
        	player.sendMessage(getResourceManager().getString(MsgType.MV_LEAVE_SHOP, new String[] {"%SHOPNAME%"}, new String[] {shop.getName()}));
        } else {
        	player.sendMessage(getResourceManager().getString(MsgType.MV_NO_SHOP));
        }
    }

    private void notifyPlayerEnterShop(Player player, UUID shopUuid) {
        // TODO Add formatting
        Shop shop = getShopManager().getLocalShop(shopUuid);
        if(shop != null) {
        	player.sendMessage(getResourceManager().getString(MsgType.MV_ENTER_SHOP, new String[] {"%SHOPNAME%"}, new String[] {shop.getName()}));
        }
    }
}
