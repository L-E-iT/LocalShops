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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.milkbowl.localshops.Config;
import net.milkbowl.localshops.LocalShops;
import net.milkbowl.localshops.objects.LocalShop;
import net.milkbowl.localshops.objects.MsgType;
import net.milkbowl.localshops.objects.PermType;
import net.milkbowl.localshops.objects.PlayerData;
import net.milkbowl.localshops.objects.ShopLocation;
import net.milkbowl.localshops.util.GenericFunctions;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@SuppressWarnings("deprecation")
public class CommandShopMove extends Command {

    public CommandShopMove(LocalShops plugin, String commandLabel, CommandSender sender, String command, boolean isGlobal) {
        super(plugin, commandLabel, sender, command);
    }

    public CommandShopMove(LocalShops plugin, String commandLabel, CommandSender sender, String[] command, boolean isGlobal) {
        super(plugin, commandLabel, sender, command);
    }

    public boolean process() {

        if (isGlobal) {
            sender.sendMessage(plugin.getResourceManager().getChatPrefix() + " " + ChatColor.DARK_AQUA + "You cannot move a global shop!");
            return false;
        } else if (!canUseCommand(PermType.MOVE)) {
            sender.sendMessage(plugin.getResourceManager().getString(MsgType.GEN_USER_ACCESS_DENIED));
            return false;
        } else if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getResourceManager().getString(MsgType.GEN_CONSOLE_NOT_IMPLEMENTED));
            return false;
        }

        Pattern pattern = Pattern.compile("(?i)move\\s+(.*)");
        Matcher matcher = pattern.matcher(command);
        if (matcher.find()) {
            String id = matcher.group(1);

            Player player = (Player) sender;
            String world = player.getWorld().getName();

            PlayerData pData = plugin.getPlayerData().get(player.getName());

            // check to see if that shop exists
            LocalShop thisShop = plugin.getShopManager().getLocalShop(id);
            if (thisShop == null) {
                sender.sendMessage(plugin.getResourceManager().getChatPrefix() + " " + ChatColor.DARK_AQUA + "Could not find shop: " + ChatColor.WHITE + id);
                return false;
            } else if (!thisShop.getOwner().equalsIgnoreCase(player.getName())) {
                // check if player has access
                player.sendMessage(plugin.getResourceManager().getString(MsgType.GEN_MUST_BE_SHOP_OWNER));
                return false;
            } else if (thisShop.getShopLocations().size() > 1) {
                //Can't move a shop that has more than 1 location
                player.sendMessage(plugin.getResourceManager().getChatPrefix() + " " + ChatColor.DARK_AQUA + "You can't move a shop that has more than 1 location!");
                return false;
            }

            //Shop location variables
            int x1 = 0, x2 = 0, z1 = 0, z2 = 0;
            ShopLocation shopLoc = null;

            if (pData.isSelecting()) {
                if (GenericFunctions.calculateCuboidSize(pData.getPositionA(), pData.getPositionB(), Config.getShopSizeMaxWidth(), Config.getShopSizeMaxHeight()) == null) {
                    String size = Config.getShopSizeMaxWidth() + "x" + Config.getShopSizeMaxHeight() + "x" + Config.getShopSizeMaxWidth();
                    player.sendMessage(plugin.getResourceManager().getString(MsgType.CMD_SHP_CREATE_SELECTION_PROB_SIZE, new String[]{"%SIZE%"}, new Object[]{size}));
                    return false;
                }


                if (pData.getPositionA() == null || pData.getPositionB() == null) {
                    player.sendMessage(plugin.getResourceManager().getString(MsgType.CMD_SHP_CREATE_SELECTION_PROB_ONLY_ONE_POINT));
                    return false;
                } else {
                    shopLoc = new ShopLocation(pData.getPositionA(), pData.getPositionB());
                }

            } else {
                // otherwise calculate the shop from the player's location
                // get current position
                Location loc = player.getLocation();
                int x = loc.getBlockX();
                int y = loc.getBlockY();
                int z = loc.getBlockZ();

                if (Config.getShopSizeDefWidth() % 2 == 0) {
                    x1 = x - (Config.getShopSizeDefWidth() / 2);
                    x2 = x + (Config.getShopSizeDefWidth() / 2);
                    z1 = z - (Config.getShopSizeDefWidth() / 2);
                    z2 = z + (Config.getShopSizeDefWidth() / 2);
                } else {
                    x1 = x - (Config.getShopSizeDefWidth() / 2) + 1;
                    x2 = x + (Config.getShopSizeDefWidth() / 2);
                    z1 = z - (Config.getShopSizeDefWidth() / 2) + 1;
                    z2 = z + (Config.getShopSizeDefWidth() / 2);
                }
                //generate the new shopLocation
                shopLoc = new ShopLocation(x1, y - 1, z1, x2, y + Config.getShopSizeDefHeight() - 1, z2);

            }

            if (!plugin.getShopManager().shopPositionOk(shopLoc.getLocation1(), shopLoc.getLocation2(), world)) {
                sender.sendMessage(plugin.getResourceManager().getString(MsgType.CMD_SHP_CREATE_SHOP_EXISTS));
                return false;
            } else if (Config.getShopChargeMove() && !canUseCommand(PermType.MOVE_FREE)) {
                if (!plugin.getEcon().withdrawPlayer(player.getName(), Config.getShopChargeMoveCost()).transactionSuccess()) {
                    // return, this player did not have enough money
                    player.sendMessage(plugin.getResourceManager().getChatPrefix() + " " + ChatColor.DARK_AQUA + "You need " + plugin.getEcon().format(Config.getShopChargeMoveCost()) + " to move a shop.");
                    return false;
                }
            }
            // update the shop
            thisShop.setWorld(world);
            thisShop.getShopLocations().clear();
            thisShop.getShopLocations().add(shopLoc);

            log.info(thisShop.getUuid().toString());

            pData.setSelecting(false);

            // write the file
            if (plugin.getShopManager().saveShop(thisShop)) {
                player.sendMessage(plugin.getResourceManager().getChatPrefix() + " " + ChatColor.WHITE + thisShop.getName() + ChatColor.DARK_AQUA + " was moved successfully.");
                return true;
            } else {
                player.sendMessage(plugin.getResourceManager().getChatPrefix() + " " + ChatColor.DARK_AQUA + "There was an error, could not move shop.");
                return false;
            }
        }


        // Show usage
        sender.sendMessage(plugin.getResourceManager().getChatPrefix() + " " + ChatColor.DARK_AQUA + "The command format is " + ChatColor.WHITE + "/" + commandLabel + " move [id]");
        sender.sendMessage(plugin.getResourceManager().getChatPrefix() + " " + ChatColor.DARK_AQUA + "Use " + ChatColor.WHITE + "/" + commandLabel + " info" + ChatColor.DARK_AQUA + " to obtain the id.");
        return true;
    }
}
