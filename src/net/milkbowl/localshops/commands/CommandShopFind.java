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

import java.util.List;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.milkbowl.localshops.Config;
import net.milkbowl.localshops.LocalShops;
import net.milkbowl.localshops.objects.MsgType;
import net.milkbowl.localshops.comparator.EntryValueComparator;
import net.milkbowl.localshops.objects.GlobalShop;
import net.milkbowl.localshops.objects.ShopRecord;
import net.milkbowl.localshops.objects.LocalShop;
import net.milkbowl.localshops.objects.Shop;
import net.milkbowl.localshops.objects.ShopLocation;
import net.milkbowl.localshops.util.GenericFunctions;
import net.milkbowl.vault.item.ItemInfo;
import net.milkbowl.vault.item.Items;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

@SuppressWarnings("deprecation")
public class CommandShopFind extends Command {

    public CommandShopFind(LocalShops plugin, String commandLabel, CommandSender sender, String command, boolean isGlobal) {
        super(plugin, commandLabel, sender, command);
    }

    public CommandShopFind(LocalShops plugin, String commandLabel, CommandSender sender, String[] command, boolean isGlobal) {
        super(plugin, commandLabel, sender, command);
    }

    public boolean process() {
        if (Config.getFindMaxDistance() == 0) {
            sender.sendMessage(String.format("[%s] Shop finding has been disabled on this server.", plugin.getDescription().getName()));
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getResourceManager().getString(MsgType.GEN_CONSOLE_NOT_IMPLEMENTED));
            return true;
        }

        Player player = (Player) sender;

        // search
        Pattern pattern = Pattern.compile("(?i)find$");
        Matcher matcher = pattern.matcher(command);
        if (matcher.find()) {
            ItemStack itemStack = player.getItemInHand();
            if (itemStack == null) {
                return true;
            }
            ItemInfo found = null;
            found = Items.itemByStack(itemStack);
            if (found == null) {
                sender.sendMessage(plugin.getResourceManager().getString(MsgType.GEN_ITEM_NOT_FOUND));
                return true;
            }
            return shopFind(player, found);
        }

        // search int
        matcher.reset();
        pattern = Pattern.compile("(?i)find\\s+(\\d+)$");
        matcher = pattern.matcher(command);
        if (matcher.find()) {
            int id = Integer.parseInt(matcher.group(1));
            ItemInfo found = Items.itemById(id);
            if (found == null) {
                sender.sendMessage(plugin.getResourceManager().getString(MsgType.GEN_ITEM_NOT_FOUND));
                return true;
            }
            return shopFind(player, found);
        }

        // search int:int
        matcher.reset();
        pattern = Pattern.compile("(?i)find\\s+(\\d+):(\\d+)$");
        matcher = pattern.matcher(command);
        if (matcher.find()) {
            int id = Integer.parseInt(matcher.group(1));
            short type = Short.parseShort(matcher.group(2));
            ItemInfo found = Items.itemById(id, type);
            if (found == null) {
                sender.sendMessage(plugin.getResourceManager().getString(MsgType.GEN_ITEM_NOT_FOUND));
                return true;
            }
            return shopFind(player, found);
        }

        // search name
        matcher.reset();
        pattern = Pattern.compile("(?i)find\\s+(.*)");
        matcher = pattern.matcher(command);
        if (matcher.find()) {
            String name = matcher.group(1);
            ItemInfo found = Items.itemByName(name);
            if (found == null) {
                sender.sendMessage(String.format("No item was not found matching \"%s\"", name));
                return true;
            } else {
                return shopFind(player, found);
            }
        }

        // Show sell help
        sender.sendMessage(ChatColor.WHITE + "   /" + commandLabel + " find [itemname] " + ChatColor.DARK_AQUA + "- Find shops that buy or sell this item.");
        return true;
    }

    private boolean shopFind(Player player, ItemInfo found) {
        String playerWorld = player.getWorld().getName();
        Location playerLoc = player.getLocation();
        TreeMap<UUID, Double> foundShops = new TreeMap<UUID, Double>();
        List<Shop> shops = plugin.getShopManager().getAllShops();
        for (Shop shop : shops) {
            double distance = 0;

            // Check if global or local
            if (shop instanceof LocalShop) {
                LocalShop lShop = (LocalShop) shop;

                // Check that its the current world
                if (!lShop.getWorld().equals(playerWorld)) {
                    continue;
                }
                for (ShopLocation shopLoc : lShop.getShopLocations()) {
                    //If there's only 1 shop don't try to find the minimum distance.
                    if (lShop.getShopLocations().size() > 1) {
                        double tempDist = GenericFunctions.calculateDistance(playerLoc, shopLoc.getCenter(player.getWorld()));
                        if ((distance != 0 && distance > tempDist) || distance == 0) {
                            distance = tempDist;
                        }
                    } else {
                        distance = GenericFunctions.calculateDistance(playerLoc, shopLoc.getCenter(player.getWorld()));
                    }
                }
            } else if (shop instanceof GlobalShop) {
                GlobalShop gShop = (GlobalShop) shop;

                // Check that its the current world
                if (!gShop.containsWorld(playerWorld)) {
                    continue;
                }

                distance = 0;
            }

            // Determine if distance is too far away && ignore
            if (Config.getFindMaxDistance() > 0 && distance > Config.getFindMaxDistance()) {
                continue;
            } else if (!shop.containsItem(found)) {
                // Check shop has item & is either buying or selling it
                continue;
            } else // This shop is valid, add to list
            {
                foundShops.put(shop.getUuid(), distance);
            }
        }
        @SuppressWarnings("unchecked")
        SortedSet<Entry<UUID, Double>> entries = new TreeSet<Entry<UUID, Double>>(new EntryValueComparator());
        entries.addAll(foundShops.entrySet());

        if (entries.size() > 0) {
            int count = 0;
            sender.sendMessage(ChatColor.DARK_AQUA + "Showing " + ChatColor.WHITE + foundShops.size() + ChatColor.DARK_AQUA + " shops having " + ChatColor.WHITE + found.getName());
            for (Entry<UUID, Double> entry : entries) {
                UUID uuid = entry.getKey();
                double distance = entry.getValue();
                Shop shop = plugin.getShopManager().getLocalShop(uuid);
                ShopRecord shopRecord = shop.getItem(found);

                String sellPrice;
                if (shopRecord.getBuyPrice() <= 0 || (shopRecord.getStock() == 0 && !shop.isUnlimitedStock())) {
                    sellPrice = "--";
                } else {
                    sellPrice = String.format("%.2f", (shopRecord.getBuyPrice()));
                }

                String buyPrice;
                if (shopRecord.getSellPrice() <= 0 || (shopRecord.getStock() > 0 && shopRecord.getStock() > shopRecord.getMaxStock() && !shop.isUnlimitedStock())) {
                    buyPrice = "--";
                } else {
                    buyPrice = String.format("%.2f", (shopRecord.getSellPrice()));
                }

                if (buyPrice.equals("--") && sellPrice.equals("--")) {
                    continue;
                }

                sender.sendMessage(String.format(ChatColor.WHITE + "%s: " + ChatColor.GOLD + "selling for %s, " + ChatColor.GREEN + "buying for %s", shop.getName(), sellPrice, buyPrice));
                sender.sendMessage(String.format(ChatColor.WHITE + "  " + ChatColor.DARK_AQUA + "Currently " + ChatColor.WHITE + "%-2.0fm" + ChatColor.DARK_AQUA + " away with ID: " + ChatColor.WHITE + "%s", distance, shop.getShortUuidString()));

                count++;

                if (count == 4) {
                    break;
                }
            }
        } else {
            sender.sendMessage(ChatColor.DARK_AQUA + "No shops were found having " + ChatColor.WHITE + found.getName());
        }

        return true;
    }
}
