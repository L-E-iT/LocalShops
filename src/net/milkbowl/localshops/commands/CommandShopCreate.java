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
package net.milkbowl.localshops.commands;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.milkbowl.localshops.Config;
import net.milkbowl.localshops.LocalShops;
import net.milkbowl.localshops.objects.GlobalShop;
import net.milkbowl.localshops.objects.LocalShop;
import net.milkbowl.localshops.objects.MsgType;
import net.milkbowl.localshops.objects.PermType;
import net.milkbowl.localshops.objects.Shop;
import net.milkbowl.localshops.objects.ShopLocation;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@SuppressWarnings("deprecation")
public class CommandShopCreate extends Command {

    public CommandShopCreate(LocalShops plugin, String commandLabel, CommandSender sender, String command, boolean isGlobal) {
        super(plugin, commandLabel, sender, command, isGlobal);
    }

    public CommandShopCreate(LocalShops plugin, String commandLabel, CommandSender sender, String[] command, boolean isGlobal) {
        super(plugin, commandLabel, sender, command, isGlobal);
    }

    public boolean process() {
        String creator = null;
        String world = null;

        Player player = null;
        ShopLocation shopLoc = null;
        // Get current shop
        if (sender instanceof Player) {
            player = (Player) sender;

            creator = player.getName();
            world = player.getWorld().getName();
            shopLoc = getNewShopLoc(player);

            if (shopLoc == null) {
                return false;
            }

            //Check permissions
            if (!canCreateShop(creator)) {
                sender.sendMessage(plugin.getResourceManager().getString(MsgType.CMD_SHP_CREATE_MAX_NUM_SHOPS));
                return false;
            }

            if (!plugin.getShopManager().shopPositionOk(shopLoc.getLocation1(), shopLoc.getLocation2(), world) && !isGlobal) {
                sender.sendMessage(plugin.getResourceManager().getString(MsgType.CMD_SHP_CREATE_SHOP_EXISTS));
                return false;
            }
            
            if (!plugin.getShopManager().canBuildInPosition(player, shopLoc.getLocation1(), shopLoc.getLocation2(), player.getWorld())) {
                sender.sendMessage(plugin.getResourceManager().getString(MsgType.CMD_SHP_CREATE_ZONE_EXISTS));
                return false;
            }

            if (isGlobal && plugin.getShopManager().getGlobalShopByWorld(world) != null) {
                sender.sendMessage(plugin.getResourceManager().getString(MsgType.CMD_SHP_CREATE_WORLD_HAS_GLOBAL));
                return false;
            }

            if (Config.getShopChargeCreate()) {
                if (!canUseCommand(PermType.CREATE_FREE)) {
                    if (!plugin.getEcon().withdrawPlayer(player.getName(), Config.getShopChargeCreateCost()).transactionSuccess()) {
                        sender.sendMessage(plugin.getResourceManager().getString(MsgType.CMD_SHP_CREATE_INSUFFICIENT_FUNDS));
                        return false;
                    }
                }
            }

        } else {
            sender.sendMessage(plugin.getResourceManager().getString(MsgType.GEN_CONSOLE_NOT_IMPLEMENTED));
            return false;
        }

        // Command matching     
        String name = null;

        Pattern pattern = Pattern.compile("(?i)create$");
        Matcher matcher = pattern.matcher(command);
        if (matcher.find()) {
            if (isGlobal) {
                name = player.getWorld().getName() + " Shop";
            } else {
                name = player.getName() + " Shop";
            }
        }


        matcher.reset();
        pattern = Pattern.compile("(?i)create\\s+(.*)");
        matcher = pattern.matcher(command);
        if (matcher.find()) {
            name = matcher.group(1);
        }

        Shop shop = null;

        if (isGlobal) {
            GlobalShop gShop = new GlobalShop(UUID.randomUUID());
            gShop.setCreator(creator);
            gShop.setOwner(creator);
            gShop.setName(name);
            gShop.setUnlimitedMoney(true);
            gShop.setUnlimitedStock(true);
            gShop.addWorld(world);
            plugin.getShopManager().addShop(gShop);
            shop = gShop;
            log.info(plugin.getResourceManager().getString(MsgType.CMD_SHP_CREATE_LOG, new String[]{"%TYPE%", "%SHOP%"}, new Object[]{"Global", gShop}));
        } else {
            LocalShop lShop = new LocalShop(UUID.randomUUID());
            lShop.setCreator(creator);
            lShop.setOwner(creator);
            lShop.setName(name);
            lShop.setWorld(world);
            lShop.getShopLocations().add(shopLoc);
            plugin.getShopManager().addShop(lShop);
            shop = lShop;
            log.info(plugin.getResourceManager().getString(MsgType.CMD_SHP_CREATE_LOG, new String[]{"%TYPE%", "%SHOP%"}, new Object[]{"Local", lShop}));
            for (Player p : plugin.getServer().getOnlinePlayers()) {
                plugin.checkPlayerPosition(p);
            }
        }

        // Disable selecting for player (if player)
        if (sender instanceof Player) {
            plugin.getPlayerData().get(player.getName()).setSelecting(false);
        }

        // write the file
        if (plugin.getShopManager().saveShop(shop)) {
            //Command.Shop.Create.Success=%CHAT_PREFIX%%WHITE%%SHOPNAME%%DARK_AQUA% was created successfully.  CMD_SHP_CREATE_SUCCESS
            sender.sendMessage(plugin.getResourceManager().getString(MsgType.CMD_SHP_CREATE_SUCCESS, new String[]{"%SHOPNAME%"}, new Object[]{shop.getName()}));
            return true;
        } else {
            sender.sendMessage(plugin.getResourceManager().getString(MsgType.CMD_SHP_CREATE_FAIL));
            return false;
        }

    }
}
