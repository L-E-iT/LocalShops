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

package net.milkbowl.localshops.commands;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.milkbowl.localshops.LocalShops;
import net.milkbowl.localshops.Search;
import net.milkbowl.localshops.objects.ItemInfo;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;


public class CommandShopSearch extends Command {

    public CommandShopSearch(LocalShops plugin, String commandLabel, CommandSender sender, String command, boolean isGlobal) {
        super(plugin, commandLabel, sender, command);
    }
    
    public CommandShopSearch(LocalShops plugin, String commandLabel, CommandSender sender, String[] command, boolean isGlobal) {
        super(plugin, commandLabel, sender, command);
    }

    public boolean process() {
        Pattern pattern = Pattern.compile("(?i)search\\s+(.*)");
        Matcher matcher = pattern.matcher(command);
        if (matcher.find()) {
            String name = matcher.group(1);
            ItemInfo found = Search.itemByName(name);
            if (found == null) {
                sender.sendMessage(String.format("No item was not found matching \"%s\"", name));
            } else {
                sender.sendMessage(found.toString());
            }
            return true;            
        }

        // Show search stuff
        sender.sendMessage(ChatColor.WHITE + "   /" + commandLabel + " search [item name]" + ChatColor.DARK_AQUA + " - Searches for and displays information about an item.");
        return true;
    }
}
