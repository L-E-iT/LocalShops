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

package net.milkbowl.localshops.comparator;

import java.util.Comparator;

import net.milkbowl.localshops.objects.InventoryItem;



public class InventoryItemSortByName implements Comparator<InventoryItem> {

    @Override
    public int compare(InventoryItem o1, InventoryItem o2) {
        return o1.getInfo().name.compareTo(o2.getInfo().name);
    }
    
}