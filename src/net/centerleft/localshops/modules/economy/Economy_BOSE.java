package net.centerleft.localshops.modules.economy;

import net.centerleft.localshops.LocalShops;
import net.centerleft.localshops.Shop;

import org.bukkit.event.Event.Priority;
import org.bukkit.event.Event.Type;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.event.server.ServerListener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

import cosine.boseconomy.BOSEconomy;

public class Economy_BOSE implements Economy {
    private String name = "BOSEconomy";
    private LocalShops plugin = null;
    private PluginManager pluginManager = null;
    private BOSEconomy economy = null;
    private EconomyServerListener economyServerListener = null;

    public Economy_BOSE(LocalShops plugin) {
        this.plugin = plugin;
        pluginManager = this.plugin.getServer().getPluginManager();

        economyServerListener = new EconomyServerListener(this);

        this.pluginManager.registerEvent(Type.PLUGIN_ENABLE, economyServerListener, Priority.Monitor, plugin);
        this.pluginManager.registerEvent(Type.PLUGIN_DISABLE, economyServerListener, Priority.Monitor, plugin);

        // Load Plugin in case it was loaded before
        if (economy == null) {
            Plugin bose = plugin.getServer().getPluginManager().getPlugin("BOSEconomy");
            if (bose != null && bose.isEnabled()) {
                economy = (BOSEconomy) bose;
                log.info(String.format("[%s] %s hooked.", plugin.getDescription().getName(), name));
            }
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isEnabled() {
        if(economy == null) {
            return false;
        } else {
            return economy.isEnabled();
        }
    }

    @Override
    public double getBalance(String playerName) {
        return (double) economy.getPlayerMoney(playerName);
    }

    @Override
    public double withdrawPlayer(String playerName, double amount) {
        if(amount < 0) {
            return -1;
        }
        amount = Math.ceil(amount);
        double balance = getBalance(playerName);
        if(balance - amount < 0) {
            return -1;
        }
        if(economy.setPlayerMoney(playerName, (int) (balance - amount), false)) {
            return amount;
        } else {
            return -1;
        }
    }

    @Override
    public double depositPlayer(String playerName, double amount) {
        if(amount < 0) {
            return -1;
        }
        amount = Math.ceil(amount);
        double balance = getBalance(playerName);
        if(economy.setPlayerMoney(playerName, (int) (balance + amount), false)) {
            return amount;
        } else {
            return -1;
        }
    }

    @Override
    public double depositShop(Shop shop, double amount) {
        if(amount < 0) {
            return -1;
        }
        amount = Math.ceil(amount);
        // Currently not supported
        return -1;
    }

    @Override
    public double withdrawShop(Shop shop, double amount) {
        if(amount < 0) {
            return -1;
        }
        amount = Math.ceil(amount);
        // Currently not supported
        return -1;
    }

    public String getMoneyNamePlural() {
        return economy.getMoneyNamePlural();
    }

    public String getMoneyNameSingular() {
        return economy.getMoneyName();
    }
    
    private class EconomyServerListener extends ServerListener {
        Economy_BOSE economy = null;
        
        public EconomyServerListener(Economy_BOSE economy) {
            this.economy = economy;
        }
        
        public void onPluginEnable(PluginEnableEvent event) {
            if (economy.economy == null) {
                Plugin bose = plugin.getServer().getPluginManager().getPlugin("BOSEconomy");

                if (bose != null && bose.isEnabled()) {
                    economy.economy = (BOSEconomy) bose;
                    log.info(String.format("[%s] %s hooked.", plugin.getDescription().getName(), economy.name));
                }
            }
        }
        
        public void onPluginDisable(PluginDisableEvent event) {
            if (economy.economy != null) {
                if (event.getPlugin().getDescription().getName().equals("Essentials")) {
                    economy.economy = null;
                    log.info(String.format("[%s] %s un-hooked.", plugin.getDescription().getName(), economy.name));
                }
            }
        }
    }

    @Override
    public String format(double amount) {
        if (amount == 1) {
            return String.format("%f %s", amount, getMoneyNameSingular());
        } else {
            return String.format("%f %s", amount, getMoneyNamePlural());
        }
    }
}
