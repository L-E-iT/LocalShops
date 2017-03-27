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

import net.milkbowl.localshops.LocalShops;
import net.milkbowl.localshops.objects.MsgType;
import net.milkbowl.localshops.objects.PermType;
import net.milkbowl.localshops.objects.Shop;
import net.milkbowl.vault.item.ItemInfo;
import net.milkbowl.vault.item.Items;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

@SuppressWarnings("deprecation")
public class CommandShopRemove extends Command {

    public CommandShopRemove(LocalShops plugin, String commandLabel, CommandSender sender, String command, boolean isGlobal) {
        super(plugin, commandLabel, sender, command, isGlobal);
    }

    public CommandShopRemove(LocalShops plugin, String commandLabel, CommandSender sender, String[] command, boolean isGlobal) {
        super(plugin, commandLabel, sender, command, isGlobal);
    }

    public boolean process() {
        Shop shop = null;

        // Get current shop
        if (sender instanceof Player) {
            // Get player & data
            Player player = (Player) sender;

            shop = getCurrentShop(player);
            if (shop == null) {
                sender.sendMessage(plugin.getResourceManager().getString(MsgType.GEN_NOT_IN_SHOP));
                return false;
            }

            // Check Permissions
            if (!canUseCommand(PermType.REMOVE)) {
                sender.sendMessage(plugin.getResourceManager().getString(MsgType.GEN_USER_ACCESS_DENIED));
                return false;
            }

            // Check if Player can Modify
            if (!isShopController(shop)) {
                player.sendMessage(plugin.getResourceManager().getString(MsgType.GEN_MUST_BE_SHOP_OWNER));
                player.sendMessage(plugin.getResourceManager().getString(MsgType.GEN_CURR_OWNER_IS, new String[]{"%OWNER%"}, new String[]{shop.getOwner()}));
                return true;
            }

            // remove (player only command)
            Pattern pattern = Pattern.compile("(?i)remove$");
            Matcher matcher = pattern.matcher(command);
            if (matcher.find()) {
                ItemStack itemStack = player.getItemInHand();
                if (itemStack == null) {
                    return false;
                }
                ItemInfo item = Items.itemByStack(itemStack);
                if (item == null) {
                    sender.sendMessage(plugin.getResourceManager().getString(MsgType.GEN_ITEM_NOT_FOUND));
                    return false;
                }
                return shopRemove(shop, item);
            }

        } else {
            sender.sendMessage(plugin.getResourceManager().getString(MsgType.GEN_CONSOLE_NOT_IMPLEMENTED));
            return false;
        }

        // Command matching

        // remove int
        Pattern pattern = Pattern.compile("(?i)remove\\s+(\\d+)");
        Matcher matcher = pattern.matcher(command);
        if (matcher.find()) {
            int id = Integer.parseInt(matcher.group(1));
            ItemInfo item = Items.itemById(id);
            return shopRemove(shop, item);
        }

        // remove int:int
        matcher.reset();
        pattern = Pattern.compile("(?i)remove\\s+(\\d+):(\\d+)");
        matcher = pattern.matcher(command);
        if (matcher.find()) {
            int id = Integer.parseInt(matcher.group(1));
            short type = Short.parseShort(matcher.group(2));
            ItemInfo item = Items.itemById(id, type);
            return shopRemove(shop, item);
        }

        // remove name
        matcher.reset();
        pattern = Pattern.compile("(?i)remove\\s+(.*)");
        matcher = pattern.matcher(command);
        if (matcher.find()) {
            String itemName = matcher.group(1);
            ItemInfo item = Items.itemByName(itemName);
            return shopRemove(shop, item);
        }


        // Show usage
        sender.sendMessage(ChatColor.WHITE + "   /" + commandLabel + " remove [itemname]" + ChatColor.DARK_AQUA + " - Stop selling item in shop.");
        return true;
    }

    private boolean shopRemove(Shop shop, ItemInfo item) {
        if (item == null) {
            sender.sendMessage(ChatColor.DARK_AQUA + "Item not found.");
            return false;
        }

        if (!shop.containsItem(item)) {
            sender.sendMessage(ChatColor.DARK_AQUA + "The shop is not selling " + ChatColor.WHITE + item.getName());
            return true;
        }

        sender.sendMessage(ChatColor.WHITE + item.getName() + ChatColor.DARK_AQUA + " removed from the shop. ");
        if (!shop.isUnlimitedStock()) {
            int amount = shop.getItem(item).getStock();

            if (sender instanceof Player) {
                Player player = (Player) sender;
                // log the transaction
                plugin.getShopManager().logItems(player.getName(), shop.getName(), "remove-item", item.getName(), amount, amount, 0);

                givePlayerItem(item, amount);
                player.sendMessage("" + ChatColor.WHITE + amount + ChatColor.DARK_AQUA + " have been returned to your inventory");
            }
        }

        shop.removeItem(item);
        plugin.getShopManager().updateSigns(shop, item);
        plugin.getShopManager().saveShop(shop);

        return true;
    }
}
