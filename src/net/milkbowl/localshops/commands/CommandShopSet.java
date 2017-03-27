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

import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.milkbowl.localshops.LocalShops;
import net.milkbowl.localshops.objects.MsgType;
import net.milkbowl.localshops.objects.PermType;
import net.milkbowl.localshops.objects.Shop;
import net.milkbowl.localshops.objects.ShopSign;
import net.milkbowl.localshops.util.GenericFunctions;
import net.milkbowl.vault.item.ItemInfo;
import net.milkbowl.vault.item.Items;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@SuppressWarnings("deprecation")
public class CommandShopSet extends Command {

    public CommandShopSet(LocalShops plugin, String commandLabel, CommandSender sender, String command, boolean isGlobal) {
        super(plugin, commandLabel, sender, command, isGlobal);
    }

    public CommandShopSet(LocalShops plugin, String commandLabel, CommandSender sender, String[] command, boolean isGlobal) {
        super(plugin, commandLabel, sender, command, isGlobal);
    }

    // TODO: Add set messages to the Messages.properties
    
    @Override
    public boolean process() {
        // Check Permissions
        if (!canUseCommand(PermType.SET)) {
            sender.sendMessage(plugin.getResourceManager().getString(MsgType.GEN_USER_ACCESS_DENIED));
            return true;
        }

        log.info(String.format("[%s] Command issued: %s", plugin.getDescription().getName(), command));

        // Parse Arguments
        if (command.matches("(?i)set\\s+sell.*")) {
            return shopSetSell();
        } else if (command.matches("(?i)set\\s+buy.*")) {
            return shopSetBuy();
        } else if (command.matches("(?i)set\\s+max.*")) {
            return shopSetMax();
        } else if (command.matches("(?i)set\\s+unlimited.*")) {
            return shopSetUnlimited();
        } else if (command.matches("(?i)set\\s+manager.*")) {
            return shopSetManager();
        } else if (command.matches("(?i)set\\s+minbalance.*")) {
            return shopSetMinBalance();
        } else if (command.matches("(?i)set\\s+notification.*")) {
            return shopSetNotification();
        } else if (command.matches("(?i)set\\s+owner.*")) {
            return shopSetOwner();
        } else if (command.matches("(?i)set\\s+user.*")) {
            return shopSetUser();
        } else if (command.matches("(?i)set\\s+group.*")) {
            return shopSetGroup();
        } else if (command.matches("(?i)set\\s+name.*")) {
            return shopSetName();
        } else if (command.matches("(?i)set\\s+dynamic.*")) {
            return shopSetDynamic();
        } else {
            return shopSetHelp();
        }
    }

    private boolean shopSetBuy() {
        Shop shop = null;

        // Get current shop
        if (sender instanceof Player) {
            // Get player & data
            Player player = (Player) sender;

            shop = getCurrentShop(player);

            if (shop == null) {
                sender.sendMessage(plugin.getResourceManager().getString(MsgType.GEN_NOT_IN_SHOP));
                return true;
            }

            // Check if Player can Modify
            if (!isShopController(shop)) {
                player.sendMessage(plugin.getResourceManager().getString(MsgType.GEN_MUST_BE_SHOP_OWNER));
                player.sendMessage(plugin.getResourceManager().getString(MsgType.GEN_CURR_OWNER_IS, new String[]{"%OWNER%"}, new String[]{shop.getOwner()}));
                return true;
            }
        } else {
            sender.sendMessage(plugin.getResourceManager().getString(MsgType.GEN_CONSOLE_NOT_IMPLEMENTED));
            return true;
        }

        // Command matching

        // set buy int int
        Pattern pattern = Pattern.compile("(?i)set\\s+buy\\s+(\\d+)\\s+(" + DECIMAL_REGEX + ")");
        Matcher matcher = pattern.matcher(command);
        if (matcher.find()) {
            int id = Integer.parseInt(matcher.group(1));
            ItemInfo item = Items.itemById(id);
            double price = Double.parseDouble(matcher.group(2));
            return shopSetBuy(shop, item, price);
        }

        // set buy int:int int
        matcher.reset();
        pattern = Pattern.compile("(?i)set\\s+buy\\s+(\\d+):(\\d+)\\s+(" + DECIMAL_REGEX + ")");
        matcher = pattern.matcher(command);
        if (matcher.find()) {
            int id = Integer.parseInt(matcher.group(1));
            short type = Short.parseShort(matcher.group(2));
            ItemInfo item = Items.itemById(id, type);
            double price = Double.parseDouble(matcher.group(3));
            return shopSetBuy(shop, item, price);
        }

        // set nuy (chars) int
        matcher.reset();
        pattern = Pattern.compile("(?i)set\\s+buy\\s+(.*)\\s+(" + DECIMAL_REGEX + ")");
        matcher = pattern.matcher(command);
        if (matcher.find()) {
            String name = matcher.group(1);
            ItemInfo item = Items.itemByName(name);
            double price = Double.parseDouble(matcher.group(2));
            return shopSetBuy(shop, item, price);
        }

        // show buy sell usage
        sender.sendMessage("   " + "/" + commandLabel + " set buy [item name] [price]");
        return true;
    }

    private boolean shopSetBuy(Shop shop, ItemInfo item, double price) {
        if (item == null) {
            sender.sendMessage(plugin.getResourceManager().getString(MsgType.GEN_ITEM_NOT_FOUND));
            return true;
        }

        // Check if Shop has item
        if (!shop.containsItem(item)) {
            // nicely message user
            sender.sendMessage(plugin.getResourceManager().getString(MsgType.GEN_ITEM_NOT_CARRIED, new String[]{"%SHOPNAME%", "ITEMNAME"}, new String[]{shop.getName(), item.getName()}));
            return true;
        }

        // Warn about negative prices
        if (price < 0) {
            sender.sendMessage(plugin.getResourceManager().getString(MsgType.CMD_SHP_SET_NEG_PRICE));
        }

        // Set new values
        shop.setItemBuyPrice(item, price);

        // Save Shop
        plugin.getShopManager().saveShop(shop);

        // Send Result
        sender.sendMessage(ChatColor.WHITE + shop.getName() + ChatColor.DARK_AQUA + " is now buying " + ChatColor.WHITE + item.getName() + ChatColor.DARK_AQUA + " for " + ChatColor.WHITE + plugin.getEcon().format(price));

        //update any sign in this shop with that value.
        plugin.getShopManager().updateSigns(shop, item);

        return true;
    }

    private boolean shopSetSell() {
        Shop shop = null;

        // Get current shop
        if (sender instanceof Player) {
            // Get player & data
            Player player = (Player) sender;

            shop = getCurrentShop(player);
            if (shop == null) {
                sender.sendMessage(plugin.getResourceManager().getString(MsgType.GEN_NOT_IN_SHOP));
                return true;
            }

            // Check if Player can Modify
            if (!isShopController(shop)) {
                player.sendMessage(plugin.getResourceManager().getString(MsgType.GEN_MUST_BE_SHOP_OWNER));
                player.sendMessage(plugin.getResourceManager().getString(MsgType.GEN_CURR_OWNER_IS, new String[]{"%OWNER%"}, new String[]{shop.getOwner()}));
                return true;
            }
        } else {
            sender.sendMessage(plugin.getResourceManager().getString(MsgType.GEN_CONSOLE_NOT_IMPLEMENTED));
            return true;
        }

        // Command matching

        // set sell int int
        Pattern pattern = Pattern.compile("(?i)set\\s+sell\\s+(\\d+)\\s+(" + DECIMAL_REGEX + ")");
        Matcher matcher = pattern.matcher(command);
        if (matcher.find()) {
            int id = Integer.parseInt(matcher.group(1));
            ItemInfo item = Items.itemById(id);
            double price = Double.parseDouble(matcher.group(2));
            return shopSetSell(shop, item, price);
        }

        // set sell int:int int
        matcher.reset();
        pattern = Pattern.compile("(?i)set\\s+sell\\s+(\\d+):(\\d+)\\s+(" + DECIMAL_REGEX + ")");
        matcher = pattern.matcher(command);
        if (matcher.find()) {
            int id = Integer.parseInt(matcher.group(1));
            short type = Short.parseShort(matcher.group(2));
            ItemInfo item = Items.itemById(id, type);
            double price = Double.parseDouble(matcher.group(3));
            return shopSetSell(shop, item, price);
        }

        // set sell (chars) int
        matcher.reset();
        pattern = Pattern.compile("(?i)set\\s+sell\\s+(.*)\\s+(" + DECIMAL_REGEX + ")");
        matcher = pattern.matcher(command);
        if (matcher.find()) {
            String name = matcher.group(1);
            ItemInfo item = Items.itemByName(name);
            double price = Double.parseDouble(matcher.group(2));
            return shopSetSell(shop, item, price);
        }

        // show set sell usage
        sender.sendMessage("   " + "/" + commandLabel + " set sell [item name] [price]");
        return true;
    }

    private boolean shopSetSell(Shop shop, ItemInfo item, double price) {
        if (item == null) {
            sender.sendMessage(plugin.getResourceManager().getString(MsgType.GEN_ITEM_NOT_FOUND));
            return true;
        }

        // Check if Shop has item
        if (!shop.containsItem(item)) {
            // nicely message user
            sender.sendMessage(plugin.getResourceManager().getString(MsgType.GEN_ITEM_NOT_CARRIED, new String[]{"%SHOPNAME%", "%ITEMNAME%"}, new String[]{shop.getName(), item.getName()}));
            return true;
        }

        // Warn about negative items
        if (price < 0) {
            sender.sendMessage("[WARNING] This shop will loose money with negative values!");
        }

        // Set new values
        shop.setItemSellPrice(item, price);

        // Save Shop
        plugin.getShopManager().saveShop(shop);

        // Send Result
        sender.sendMessage(ChatColor.WHITE + item.getName() + ChatColor.DARK_AQUA + " now sells for " + ChatColor.WHITE + plugin.getEcon().format(price));

        //update any sign in this shop with that value.
        plugin.getShopManager().updateSigns(shop, item);
        return true;
    }

    private boolean shopSetMax() {
        Shop shop = null;

        // Get current shop
        if (sender instanceof Player) {
            // Get player & data
            Player player = (Player) sender;

            shop = getCurrentShop(player);
            if (shop == null) {
                sender.sendMessage(plugin.getResourceManager().getString(MsgType.GEN_NOT_IN_SHOP));
                return true;
            }

            // Check if Player can Modify
            if (!isShopController(shop)) {
                player.sendMessage(plugin.getResourceManager().getString(MsgType.GEN_MUST_BE_SHOP_OWNER));
                player.sendMessage(plugin.getResourceManager().getString(MsgType.GEN_CURR_OWNER_IS, new String[]{"%OWNER%"}, new String[]{shop.getOwner()}));
                return true;
            }
        } else {
            sender.sendMessage(plugin.getResourceManager().getString(MsgType.GEN_CONSOLE_NOT_IMPLEMENTED));
            return true;
        }

        // Command matching

        // shop set max int int
        Pattern pattern = Pattern.compile("(?i)set\\s+max\\s+(\\d+)\\s+(\\d+)");
        Matcher matcher = pattern.matcher(command);
        if (matcher.find()) {
            int id = Integer.parseInt(matcher.group(1));
            ItemInfo item = Items.itemById(id);
            int max = Integer.parseInt(matcher.group(2));
            return shopSetMax(shop, item, max);
        }

        // shop set max int:int int
        matcher.reset();
        pattern = Pattern.compile("(?i)set\\s+max\\s+(\\d+):(\\d+)\\s+(\\d+)");
        matcher = pattern.matcher(command);
        if (matcher.find()) {
            int id = Integer.parseInt(matcher.group(1));
            short type = Short.parseShort(matcher.group(2));
            ItemInfo item = Items.itemById(id, type);
            int max = Integer.parseInt(matcher.group(3));
            return shopSetMax(shop, item, max);
        }

        // shop set max chars int
        matcher.reset();
        pattern = Pattern.compile("(?i)set\\s+max\\s+(.*)\\s+(\\d+)");
        matcher = pattern.matcher(command);
        if (matcher.find()) {
            String name = matcher.group(1);
            ItemInfo item = Items.itemByName(name);
            int max = Integer.parseInt(matcher.group(2));
            return shopSetMax(shop, item, max);
        }

        // show set buy usage
        sender.sendMessage("   " + "/" + commandLabel + " set max [item name] [max number]");
        return true;
    }

    private boolean shopSetMax(Shop shop, ItemInfo item, int max) {
        if (item == null) {
            sender.sendMessage(plugin.getResourceManager().getString(MsgType.GEN_ITEM_NOT_FOUND));
            return true;
        }

        // Check if Shop has item
        if (!shop.containsItem(item)) {
            // nicely message user
            sender.sendMessage(plugin.getResourceManager().getString(MsgType.GEN_ITEM_NOT_CARRIED, new String[]{"%SHOPNAME%", "ITEMNAME"}, new String[]{shop.getName(), item.getName()}));
            return true;
        }

        // Check negative values
        if (max < 0) {
            sender.sendMessage("Only positive values allowed");
            return true;
        }

        // Set new values
        shop.setItemMaxStock(item, max);

        //Update our signs for this item
        plugin.getShopManager().updateSigns(shop, item);

        // Save Shop
        plugin.getShopManager().saveShop(shop);

        // Send Message
        sender.sendMessage(item.getName() + " maximum stock is now " + max);

        return true;
    }

    private boolean shopSetUnlimited() {
        Shop shop = null;

        // Get current shop
        if (sender instanceof Player) {
            // Get player & data
            Player player = (Player) sender;

            shop = getCurrentShop(player);
            if (shop == null) {
                sender.sendMessage(plugin.getResourceManager().getString(MsgType.GEN_NOT_IN_SHOP));
                return true;
            }

            // Check Permissions
            if ((!canUseCommand(PermType.ADMIN_LOCAL) && !isGlobal) || (!canUseCommand(PermType.ADMIN_GLOBAL) && isGlobal)) {
                sender.sendMessage(plugin.getResourceManager().getString(MsgType.GEN_USER_ACCESS_DENIED));
                return true;
            }
        } else {
            sender.sendMessage(plugin.getResourceManager().getString(MsgType.GEN_CONSOLE_NOT_IMPLEMENTED));
            return true;
        }

        // Command matching

        // shop set max int int
        Pattern pattern = Pattern.compile("(?i)set\\s+max\\s+(\\d+)\\s+(\\d+)");
        Matcher matcher = pattern.matcher(command);
        if (matcher.find()) {
            int id = Integer.parseInt(matcher.group(1));
            ItemInfo item = Items.itemById(id);
            int max = Integer.parseInt(matcher.group(2));
            return shopSetMax(shop, item, max);
        }

        // shop set unlimited money
        matcher.reset();
        pattern = Pattern.compile("(?i)set\\s+unlimited\\s+money");
        matcher = pattern.matcher(command);
        if (matcher.find()) {
            shop.setUnlimitedMoney(!shop.isUnlimitedMoney());
            sender.sendMessage(plugin.getResourceManager().getChatPrefix() + ChatColor.DARK_AQUA + " Unlimited money was set to " + ChatColor.WHITE + shop.isUnlimitedMoney());
            plugin.getShopManager().saveShop(shop);
            return true;
        }

        // shop set unlimited stock
        matcher.reset();
        pattern = Pattern.compile("(?i)set\\s+unlimited\\s+stock");
        matcher = pattern.matcher(command);
        if (matcher.find()) {
            shop.setUnlimitedStock(!shop.isUnlimitedStock());
            sender.sendMessage(plugin.getResourceManager().getChatPrefix() + ChatColor.DARK_AQUA + " Unlimited stock was set to " + ChatColor.WHITE + shop.isUnlimitedStock());
            //Update signs after setting unlimited stock
            for (ShopSign sign : shop.getSigns()) {
                plugin.getShopManager().updateSign(shop, sign);
            }

            plugin.getShopManager().saveShop(shop);
            return true;
        }

        // show set buy usage
        sender.sendMessage("   " + "/" + commandLabel + " set unlimited money");
        sender.sendMessage("   " + "/" + commandLabel + " set unlimited stock");
        return true;
    }

    private boolean shopSetManager() {
        Shop shop = null;

        // Get current shop
        if (sender instanceof Player) {
            // Get player & data
            Player player = (Player) sender;

            shop = getCurrentShop(player);
            if (shop == null) {
                sender.sendMessage(plugin.getResourceManager().getString(MsgType.GEN_NOT_IN_SHOP));
                return true;
            }

            // Check if Player can Modify
            if (!isShopController(shop)) {
                player.sendMessage(plugin.getResourceManager().getString(MsgType.GEN_MUST_BE_SHOP_OWNER));
                player.sendMessage(plugin.getResourceManager().getString(MsgType.GEN_CURR_OWNER_IS, new String[]{"%OWNER%"}, new String[]{shop.getOwner()}));
                return true;
            }
        } else {
            sender.sendMessage(plugin.getResourceManager().getString(MsgType.GEN_CONSOLE_NOT_IMPLEMENTED));
            return true;
        }

        // set manager +name -name ...
        Pattern pattern = Pattern.compile("(?i)set\\s+manager\\s+(.*)");
        Matcher matcher = pattern.matcher(command);
        if (matcher.find()) {
            String names = matcher.group(1);
            String[] args = names.split(" ");

            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                if (arg.matches("\\+.*")) {
                    // add manager
                    shop.addManager(arg.replaceFirst("\\+", ""));
                } else if (arg.matches("\\-.*")) {
                    // remove manager
                    shop.removeManager(arg.replaceFirst("\\-", ""));
                }
            }

            // Save Shop
            plugin.getShopManager().saveShop(shop);

            notifyPlayers(shop, plugin.getResourceManager().getString(MsgType.CMD_SHP_SET_MANAGERS), GenericFunctions.join(shop.getManagers(), ", "));
            return true;
        }

        // show set manager usage
        sender.sendMessage("   " + "/" + commandLabel + " set manager +[playername] -[playername2]");
        return true;
    }

    private boolean shopSetUser() {
        Shop shop = null;

        // Get current shop
        if (sender instanceof Player) {
            // Get player & data
            Player player = (Player) sender;

            shop = getCurrentShop(player);
            if (shop == null) {
                sender.sendMessage(plugin.getResourceManager().getString(MsgType.GEN_NOT_IN_SHOP));
                return true;
            }

            // Check if Player can Modify
            if (!isShopController(shop)) {
                player.sendMessage(plugin.getResourceManager().getString(MsgType.GEN_MUST_BE_SHOP_OWNER));
                player.sendMessage(plugin.getResourceManager().getString(MsgType.GEN_CURR_OWNER_IS, new String[]{"%OWNER%"}, new String[]{shop.getOwner()}));
                return true;
            }
        } else {
            sender.sendMessage(plugin.getResourceManager().getString(MsgType.GEN_CONSOLE_NOT_IMPLEMENTED));
            return true;
        }

        // set user +name -name ...
        Pattern pattern = Pattern.compile("(?i)set\\s+user\\s+(.*)");
        Matcher matcher = pattern.matcher(command);
        if (matcher.find()) {
            String names = matcher.group(1);
            String[] args = names.split(" ");

            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                if (arg.matches("\\+.*")) {
                    // add user
                    shop.addUser(arg.replaceFirst("\\+", ""));
                } else if (arg.matches("\\-.*")) {
                    // remove user
                    shop.removeUser(arg.replaceFirst("\\-", ""));
                }
            }

            // Save Shop
            plugin.getShopManager().saveShop(shop);

            notifyPlayers(shop, plugin.getResourceManager().getString(MsgType.CMD_SHP_SET_ALLOWED_USERS));
            return true;
        }

        // show set user usage
        sender.sendMessage("   " + "/" + commandLabel + " set user +[playername] -[playername2]");
        return true;
    }

    private boolean shopSetGroup() {
        Shop shop = null;

        // Get current shop
        if (sender instanceof Player) {
            // Get player & data
            Player player = (Player) sender;

            shop = getCurrentShop(player);
            if (shop == null) {
                sender.sendMessage(plugin.getResourceManager().getString(MsgType.GEN_NOT_IN_SHOP));
                return true;
            }

            // Check if Player can Modify
            if (!isShopController(shop)) {
                player.sendMessage(plugin.getResourceManager().getString(MsgType.GEN_MUST_BE_SHOP_OWNER));
                player.sendMessage(plugin.getResourceManager().getString(MsgType.GEN_CURR_OWNER_IS, new String[]{"%OWNER%"}, new String[]{shop.getOwner()}));
                return true;
            }
        } else {
            sender.sendMessage(plugin.getResourceManager().getString(MsgType.GEN_CONSOLE_NOT_IMPLEMENTED));
            return true;
        }

        // set group +name -name ...
        Pattern pattern = Pattern.compile("(?i)set\\s+group\\s+(.*)");
        Matcher matcher = pattern.matcher(command);
        if (matcher.find()) {
            String names = matcher.group(1);
            String[] args = names.split(" ");

            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                if (arg.matches("\\+.*")) {
                    // add group
                    shop.addGroup(arg.replaceFirst("\\+", ""));
                } else if (arg.matches("\\-.*")) {
                    // remove group
                    shop.removeGroup(arg.replaceFirst("\\-", ""));
                }
            }

            // Save Shop
            plugin.getShopManager().saveShop(shop);

            notifyPlayers(shop, new String[]{ChatColor.DARK_AQUA + "This shop's allowed groups have been updated. "});
            return true;
        }

        // show set group usage
        sender.sendMessage("   " + "/" + commandLabel + " set group +[playername] -[playername2]");
        return true;
    }

    private boolean shopSetNotification() {
        Shop shop = null;

        // Get current shop
        if (sender instanceof Player) {
            // Get player & data
            Player player = (Player) sender;

            shop = getCurrentShop(player);
            if (shop == null) {
                sender.sendMessage(plugin.getResourceManager().getString(MsgType.GEN_NOT_IN_SHOP));
                return true;
            }

            // Check if Player can Modify
            if (!shop.getOwner().equalsIgnoreCase(player.getName())) {
                player.sendMessage(plugin.getResourceManager().getString(MsgType.GEN_MUST_BE_SHOP_OWNER));
                player.sendMessage(plugin.getResourceManager().getString(MsgType.GEN_CURR_OWNER_IS, new String[]{"%OWNER%"}, new String[]{shop.getOwner()}));
                return true;
            }
        } else {
            sender.sendMessage(plugin.getResourceManager().getString(MsgType.GEN_CONSOLE_NOT_IMPLEMENTED));
            return true;
        }

        // set notification
        shop.setNotification(!shop.getNotification());

        // Save Shop
        plugin.getShopManager().saveShop(shop);

        // Output
        sender.sendMessage(String.format(ChatColor.DARK_AQUA + "Notices for " + ChatColor.WHITE + "%s" + ChatColor.DARK_AQUA + " are now " + ChatColor.WHITE + "%s", shop.getName(), shop.getNotification() ? "on" : "off"));
        return true;
    }

    private boolean shopSetMinBalance() {
        Shop shop = null;

        // Get current shop
        if (sender instanceof Player) {
            // Get player & data
            Player player = (Player) sender;

            shop = getCurrentShop(player);
            if (shop == null) {
                sender.sendMessage(plugin.getResourceManager().getString(MsgType.GEN_NOT_IN_SHOP));
                return true;
            }

            // Check if Player can Modify
            if (!shop.getOwner().equalsIgnoreCase(player.getName())) {
                player.sendMessage(plugin.getResourceManager().getString(MsgType.GEN_MUST_BE_SHOP_OWNER));
                player.sendMessage(plugin.getResourceManager().getString(MsgType.GEN_CURR_OWNER_IS, new String[]{"%OWNER%"}, new String[]{shop.getOwner()}));
                return true;
            }
        } else {
            sender.sendMessage(plugin.getResourceManager().getString(MsgType.GEN_CONSOLE_NOT_IMPLEMENTED));
            return true;
        }

        // set minbalance amount
        Pattern pattern = Pattern.compile("(?i)set\\s+minbalance\\s+(\\d+)");
        Matcher matcher = pattern.matcher(command);
        if (matcher.find()) {
            double min = Double.parseDouble(matcher.group(1));
            shop.setMinBalance(min);
            // Save Shop
            plugin.getShopManager().saveShop(shop);

            sender.sendMessage(ChatColor.WHITE + shop.getName() + ChatColor.DARK_AQUA + " now has a minimum balance of " + ChatColor.WHITE + plugin.getEcon().format(min));
            return true;
        }

        sender.sendMessage(" " + "/" + commandLabel + " set minbalance [amount]");
        return true;
    }

    private boolean shopSetOwner() {
        Shop shop = null;
        boolean reset = false;

        // Get current shop
        if (sender instanceof Player) {
            // Get player & data
            Player player = (Player) sender;

            shop = getCurrentShop(player);
            if (shop == null) {
                sender.sendMessage(plugin.getResourceManager().getString(MsgType.GEN_NOT_IN_SHOP));
                return true;
            }

            // Check if Player can Modify
            if (((!canUseCommand(PermType.ADMIN_LOCAL) && !isGlobal) || (!canUseCommand(PermType.ADMIN_GLOBAL) && isGlobal)) && !shop.getOwner().equalsIgnoreCase(player.getName())) {
                player.sendMessage(plugin.getResourceManager().getString(MsgType.GEN_MUST_BE_SHOP_OWNER));
                sender.sendMessage(ChatColor.DARK_AQUA + "  The current shop owner is " + ChatColor.WHITE + shop.getOwner());
                return true;
            }

            if (!canUseCommand(PermType.SET_OWNER) && ((!canUseCommand(PermType.ADMIN_LOCAL) && !isGlobal) || (!canUseCommand(PermType.ADMIN_GLOBAL) && isGlobal))) {
                sender.sendMessage(plugin.getResourceManager().getString(MsgType.GEN_USER_ACCESS_DENIED));
                return true;
            }

            if ((!canUseCommand(PermType.ADMIN_LOCAL) && !isGlobal) || (!canUseCommand(PermType.ADMIN_GLOBAL) && isGlobal)) {
                sender.sendMessage(plugin.getResourceManager().getChatPrefix() + ChatColor.DARK_AQUA + " " + shop.getName() + " is no longer buying items.");
                reset = true;
            }
        } else {
            sender.sendMessage(plugin.getResourceManager().getString(MsgType.GEN_CONSOLE_NOT_IMPLEMENTED));
            return true;
        }

        // set owner name
        Pattern pattern = Pattern.compile("(?i)set\\s+owner\\s+(.*)");
        Matcher matcher = pattern.matcher(command);
        if (matcher.find()) {
            String name = matcher.group(1);
            if (!canUseCommand(PermType.SET_OWNER)) {
                sender.sendMessage(plugin.getResourceManager().getString(MsgType.GEN_USER_ACCESS_DENIED));
                return true;
            } else if (!canCreateShop(name)) {
                sender.sendMessage(plugin.getResourceManager().getChatPrefix() + ChatColor.DARK_AQUA + " that player already has the maximum number of shops!");
                return true;
            } else {
                shop.setOwner(name);

                // Save Shop
                plugin.getShopManager().saveShop(shop);

                // Reset buy prices (0)
                if (reset) {
                    Iterator<ItemInfo> it = shop.getItems().iterator();
                    while (it.hasNext()) {
                        ItemInfo item = it.next();
                        shop.getItem(item).setSellPrice(0);
                    }
                }

                notifyPlayers(shop, new String[]{plugin.getResourceManager().getChatPrefix() + " " + ChatColor.DARK_AQUA + shop.getName() + " is now under new management!  The new owner is " + ChatColor.WHITE + shop.getOwner()});
                return true;
            }
        }

        sender.sendMessage("   " + "/" + commandLabel + " set owner [player name]");
        return true;
    }

    private boolean shopSetName() {
        Shop shop = null;

        // Get current shop
        if (sender instanceof Player) {
            // Get player & data
            Player player = (Player) sender;

            shop = getCurrentShop(player);
            if (shop == null) {
                sender.sendMessage(plugin.getResourceManager().getString(MsgType.GEN_NOT_IN_SHOP));
                return true;
            }

            // Check if Player can Modify
            if (!canModifyShop(shop)) {
                player.sendMessage(plugin.getResourceManager().getString(MsgType.GEN_MUST_BE_SHOP_OWNER));
                player.sendMessage(plugin.getResourceManager().getString(MsgType.GEN_CURR_OWNER_IS, new String[]{"%OWNER%"}, new String[]{shop.getOwner()}));
                return true;
            }
        } else {
            sender.sendMessage(plugin.getResourceManager().getString(MsgType.GEN_CONSOLE_NOT_IMPLEMENTED));
            return true;
        }

        Pattern pattern = Pattern.compile("(?i)set\\s+name\\s+(.*)");
        Matcher matcher = pattern.matcher(command);
        if (matcher.find()) {
            String name = matcher.group(1).trim();
            shop.setName(name);
            plugin.getShopManager().saveShop(shop);
            notifyPlayers(shop, new String[]{plugin.getResourceManager().getChatPrefix() + " " + ChatColor.DARK_AQUA + "Shop name is now " + ChatColor.WHITE + shop.getName()});
            return true;
        }

        sender.sendMessage("   " + "/" + commandLabel + " set name [shop name]");
        return true;
    }

    private boolean shopSetDynamic(Shop shop, ItemInfo item) {
        if (item == null) {
            sender.sendMessage(plugin.getResourceManager().getString(MsgType.GEN_ITEM_NOT_FOUND));
            return true;
        }

        // Check if Shop has item
        if (!shop.containsItem(item)) {
            // nicely message user
            sender.sendMessage(plugin.getResourceManager().getString(MsgType.GEN_ITEM_NOT_CARRIED, new String[]{"%SHOPNAME%", "ITEMNAME"}, new String[]{shop.getName(), item.getName()}));
            return true;
        }

        //Set new value
        shop.setItemDynamic(item);

        // Save Shop
        plugin.getShopManager().saveShop(shop);

        // Send Result
        sender.sendMessage(plugin.getResourceManager().getChatPrefix() + " " + ChatColor.DARK_AQUA + "Dynamic pricing for " + ChatColor.WHITE + item.getName() + ChatColor.DARK_AQUA + " is now " + shop.isItemDynamic(item));

        //update any sign in this shop with that value.
        plugin.getShopManager().updateSigns(shop, item);
        return true;
    }

    private boolean shopSetDynamic() {
        log.info("shopSetDynamic");
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
            if (!canUseCommand(PermType.ADMIN_SERVER)) {
                sender.sendMessage(plugin.getResourceManager().getString(MsgType.GEN_USER_ACCESS_DENIED));
                return false;
            }
        } else {
            sender.sendMessage(plugin.getResourceManager().getString(MsgType.GEN_CONSOLE_NOT_IMPLEMENTED));
            return false;
        }

        // Command matching


        //shop set dynamic int
        Pattern pattern = Pattern.compile("(?i)set\\s+dynamic\\s+(\\d+)$");
        Matcher matcher = pattern.matcher(command);
        if (matcher.find()) {
            int id = Integer.parseInt(matcher.group(1));
            ItemInfo item = Items.itemById(id);
            return shopSetDynamic(shop, item);
        }
        // set dynamic int:int
        matcher.reset();
        pattern = Pattern.compile("(?i)set\\s+dynamic\\s+(\\d+):(\\d+)");
        matcher = pattern.matcher(command);
        if (matcher.find()) {
            int id = Integer.parseInt(matcher.group(1));
            short type = Short.parseShort(matcher.group(2));
            ItemInfo item = Items.itemById(id, type);
            return shopSetDynamic(shop, item);
        }
        matcher.reset();
        //shop set dynamic (char)
        pattern = Pattern.compile("(?i)set\\s+dynamic\\s+(.*)");
        matcher = pattern.matcher(command);
        if (matcher.find()) {
            String name = matcher.group(1);
            ItemInfo item = Items.itemByName(name);
            return shopSetDynamic(shop, item);
        }

        // shop set dynamic
        matcher.reset();
        pattern = Pattern.compile("(?i)set\\s+dynamic");
        matcher = pattern.matcher(command);
        if (matcher.find()) {
            shop.setDynamicPrices(!shop.isDynamicPrices());
            sender.sendMessage(ChatColor.DARK_AQUA + "Dynamic pricing for " + ChatColor.WHITE + shop.getName() + ChatColor.DARK_AQUA + " was set to " + ChatColor.WHITE + shop.isDynamicPrices());
            plugin.getShopManager().saveShop(shop);
            return true;
        }


        // show set dynamic usage
        sender.sendMessage("   " + "/" + commandLabel + " set dynamic");
        sender.sendMessage("   " + "/" + commandLabel + " set dynamic item");
        sender.sendMessage("   " + "/" + commandLabel + " set dynamic id");
        sender.sendMessage("   " + "/" + commandLabel + " set dynamic id:id");
        return true;
    }

    private boolean shopSetHelp() {
        // Display list of set commands & return
        sender.sendMessage(plugin.getResourceManager().getChatPrefix() + " " + ChatColor.DARK_AQUA + "The following set commands are available: ");
        sender.sendMessage("   " + "/" + commandLabel + " set buy [item name] [price] <bundle size>");
        sender.sendMessage("   " + "/" + commandLabel + " set sell [item name] [price] <bundle size>");
        sender.sendMessage("   " + "/" + commandLabel + " set max [item name] [max number]");
        sender.sendMessage("   " + "/" + commandLabel + " set manager +[playername] -[playername2]");
        sender.sendMessage("   " + "/" + commandLabel + " set minbalance [amount]");
        sender.sendMessage("   " + "/" + commandLabel + " set name [shop name]");
        sender.sendMessage("   " + "/" + commandLabel + " set owner [player name]");
        if (((canUseCommand(PermType.ADMIN_LOCAL) && !isGlobal) || (canUseCommand(PermType.ADMIN_GLOBAL) && isGlobal))) {
            sender.sendMessage("   " + "/" + commandLabel + " set unlimited money");
            sender.sendMessage("   " + "/" + commandLabel + " set unlimited stock");
            sender.sendMessage("   " + "/" + commandLabel + " set dynamic <id>");
        }
        return true;
    }
}
