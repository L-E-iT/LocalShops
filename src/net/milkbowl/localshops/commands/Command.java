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

import java.util.logging.Logger;

import net.milkbowl.localshops.Config;
import net.milkbowl.localshops.LocalShops;
import net.milkbowl.localshops.objects.MsgType;
import net.milkbowl.localshops.objects.PermType;
import net.milkbowl.localshops.objects.PlayerData;
import net.milkbowl.localshops.objects.Shop;
import net.milkbowl.localshops.objects.ShopLocation;
import net.milkbowl.localshops.util.GenericFunctions;
import net.milkbowl.vault.item.ItemInfo;
import net.milkbowl.vault.item.Items;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public abstract class Command {

	// Attributes
	protected LocalShops plugin = null;
	protected String commandLabel = null;
	protected CommandSender sender = null;
	protected String command = null;
	protected static final String DECIMAL_REGEX = "(\\d+\\.\\d+)|(\\d+\\.)|(\\.\\d+)|(\\d+)";
	protected static final Logger log = Logger.getLogger("Minecraft");
	protected boolean isGlobal = false;

	public Command(LocalShops plugin, String commandLabel, CommandSender sender, String command) {
		this.plugin = plugin;
		this.commandLabel = commandLabel;
		this.sender = sender;
		this.command = command.trim();
	}

	public Command(LocalShops plugin, String commandLabel, CommandSender sender, String command, boolean isGlobal) {
		this.plugin = plugin;
		this.commandLabel = commandLabel;
		this.sender = sender;
		this.command = command.trim();
		this.isGlobal = isGlobal;
	}

	public Command(LocalShops plugin, String commandLabel, CommandSender sender, String[] args) {
		this(plugin, commandLabel, sender, GenericFunctions.join(args, " ").trim());
	}

	public Command(LocalShops plugin, String commandLabel, CommandSender sender, String[] args, boolean isGlobal) {
		this(plugin, commandLabel, sender, GenericFunctions.join(args, " ").trim(), isGlobal);
	}

	public String getCommand() {
		return command;
	}

	public boolean process() {
		// Does nothing and needs to be overloaded by subclasses
		return false;
	}

	protected boolean canUseCommand(PermType type) {
		if (sender instanceof Player) {
			//If this PermType is null then we should always allow useage
			if (type.get() == null) {
				return true;
			}

			Player player = (Player) sender;

			// check if admin first
			if (isGlobal) {
				if (plugin.getPerm().has(player, PermType.ADMIN_GLOBAL.get())) {
					return true;
				}
			} else {
				//Make sure this isn't a server command before allowing access.
				if (plugin.getPerm().has(player, PermType.ADMIN_LOCAL.get()) && !(this instanceof net.milkbowl.localshops.commands.CommandAdminSet)) {
					return true;
				}
			}
			// fail back to provided permissions second
			if (!plugin.getPerm().has(player, type.get())) {
				return false;
			}
			return true;
		} else {
			return true;
		}
	}

	protected boolean canCreateShop(String playerName) {
		if ((canUseCommand(PermType.ADMIN_LOCAL) && !isGlobal) || (canUseCommand(PermType.ADMIN_GLOBAL) && isGlobal)) {
			return true;
		} else if ((plugin.getShopManager().numOwnedShops(playerName) < Config.getPlayerMaxShops() || Config.getPlayerMaxShops() < 0) && canUseCommand(PermType.CREATE)) {
			return true;
		}

		return false;
	}

	protected boolean canModifyShop(Shop shop) {
		if (sender instanceof Player) {
			Player player = (Player) sender;
			// If owner, true
			if (shop.getOwner().equals(player.getName())) {
				return true;
			}
			// If manager, true
			if (shop.getManagers().contains(player.getName())) {
				return true;
			}
			// If admin, true
			if (canUseCommand(PermType.ADMIN_LOCAL) && shop instanceof net.milkbowl.localshops.objects.LocalShop) {
				return true;
			} else if (canUseCommand(PermType.ADMIN_GLOBAL) && shop instanceof net.milkbowl.localshops.objects.GlobalShop) {
				return true;
			}

			return false;
		} else {
			// Console, true
			return true;
		}
	}

	protected void givePlayerItem(ItemInfo item, int amount) {
		Player player = (Player) sender;

		ItemStack stack = item.toStack();
		int maxStackSize = item.getStackSize();
		// fill all the existing stacks first
		for (int i : player.getInventory().all(item.getType()).keySet()) {
			if (amount == 0) {
				continue;
			}
			ItemStack thisStack = player.getInventory().getItem(i);
			if (thisStack.getType().equals(item.getType()) && thisStack.getDurability() == stack.getDurability()) {
				if (thisStack.getAmount() < maxStackSize) {
					int remainder = maxStackSize - thisStack.getAmount();
					if (remainder <= amount) {
						amount -= remainder;
						thisStack.setAmount(maxStackSize);
					} else {
						thisStack.setAmount(maxStackSize - remainder + amount);
						amount = 0;
					}
				}
			}

		}

		for (int i = 0; i < 36; i++) {
			ItemStack thisSlot = player.getInventory().getItem(i);
			if (thisSlot == null || thisSlot.getType() == Material.AIR) {
				if (amount == 0) {
					continue;
				}
				if (amount >= maxStackSize) {
					stack.setAmount(maxStackSize);
					player.getInventory().setItem(i, stack);
					amount -= maxStackSize;
				} else {
					stack.setAmount(amount);
					player.getInventory().setItem(i, stack);
					amount = 0;
				}
			}
		}

		while (amount > 0) {
			if (amount >= maxStackSize) {
				stack.setAmount(maxStackSize);
				amount -= maxStackSize;
			} else {
				stack.setAmount(amount - maxStackSize);
				amount = 0;
			}
			player.getWorld().dropItemNaturally(player.getLocation(), stack);
		}

	}

	/**
	 * Returns true if the player is in the shop manager list or is the shop
	 * owner
	 *
	 * @param player
	 * @param shop
	 * @return
	 */
	protected boolean isShopController(Shop shop) {
		if (sender instanceof Player) {
			Player player = (Player) sender;
			if (shop.getOwner().equalsIgnoreCase(player.getName())) {
				return true;
			}
			if (shop.getManagers() != null) {
				for (String manager : shop.getManagers()) {
					if (player.getName().equalsIgnoreCase(manager)) {
						return true;
					}
				}
			}
			return false;
		} else {
			return true;
		}
	}

	protected int countItemsInInventory(PlayerInventory inventory, ItemStack item) {
		int totalAmount = 0;
		boolean isDurable = Items.itemByStack(item).isDurable();

		for (Integer i : inventory.all(item.getType()).keySet()) {
			ItemStack thisStack = inventory.getItem(i);
			if (isDurable) {
				int damage = calcDurabilityPercentage(thisStack);
				if (damage > Config.getItemMaxDamage() && Config.getItemMaxDamage() != 0) {
					continue;
				}
			} else if (thisStack.getDurability() != item.getDurability()) {
				continue;
			}
			totalAmount += thisStack.getAmount();
		}
		return totalAmount;
	}

	protected static int calcDurabilityPercentage(ItemStack item) {

		// calc durability prcnt
		short damage;
		if (item.getType() == Material.IRON_SWORD) {
			damage = (short) ((double) item.getDurability() / 250 * 100);
		} else {
			damage = (short) ((double) item.getDurability() / (double) item.getType().getMaxDurability() * 100);
		}

		return damage;
	}

	protected int removeItemsFromInventory(PlayerInventory inventory, ItemStack itemStack, int amount) {

		boolean isDurable = Items.itemByStack(itemStack).isDurable();

		// remove number of items from player adding stock
		for (int i : inventory.all(itemStack.getType()).keySet()) {
			if (amount == 0) {
				continue;
			}
			ItemStack thisStack = inventory.getItem(i);
			if (isDurable) {
				int damage = calcDurabilityPercentage(thisStack);
				if (damage > Config.getItemMaxDamage() && Config.getItemMaxDamage() != 0) {
					continue;
				}
			} else {
				if (thisStack.getDurability() != itemStack.getDurability()) {
					continue;
				}
			}

			int foundAmount = thisStack.getAmount();
			if (amount >= foundAmount) {
				amount -= foundAmount;
				inventory.setItem(i, null);
			} else {
				thisStack.setAmount(foundAmount - amount);
				inventory.setItem(i, thisStack);
				amount = 0;
			}
		}

		return amount;

	}

	protected int countAvailableSpaceForItemInInventory(PlayerInventory inventory, ItemInfo item) {
		int count = 0;
		for (ItemStack thisSlot : inventory.getContents()) {
			if (thisSlot == null || thisSlot.getType() == Material.AIR) {
				count += 64;
				continue;
			}
			if (thisSlot.getType().equals(item.getType()) && thisSlot.getDurability() == item.getSubTypeId()) {
				count += 64 - thisSlot.getAmount();
			}
		}

		return count;
	}

	protected boolean notifyPlayers(Shop shop, String...messages) {
		for(Player p : plugin.getServer().getOnlinePlayers()) {
			if(shop.containsPoint(p.getLocation())) {
				for (String message : messages) {
					p.sendMessage(message);
				}
			}
		}
		return true;
	}

	protected Shop getCurrentShop(Player player) {
		Shop shop = null;
		// Get Current Shop
		if (isGlobal) {
			shop = plugin.getShopManager().getGlobalShop(player.getWorld());
		} else if (!isGlobal) {
			shop = plugin.getShopManager().getLocalShop(player.getLocation());
		}

		return shop;
	}

	//Checks if a player has permission to access the shop
	public boolean hasShopAccess(Player player, Shop shop) {
		//Assume shop allows access to anyone if Sets are empty.
		if (shop.getUserSet().isEmpty() && shop.getGroupSet().isEmpty()) {
			return true;
		} else if (shop.getUserSet().contains(player.getName())) {
			return true;
		}

		for (String groupName : shop.getGroupSet()) {
			if (plugin.getPerm().playerInGroup(player.getWorld().getName(), player.getName(), groupName)) {
				return true;
			}
		}

		return false;
	}

	public ShopLocation getNewShopLoc(Player player) {
		PlayerData pData = plugin.getPlayerData().get(player.getName());
		//Null check our pData
		if (pData == null) {
			pData = new PlayerData(plugin, player.getName());
			plugin.getPlayerData().put(player.getName(), pData);
		}
		if (pData.isSelecting()) {
			if (GenericFunctions.calculateCuboidSize(pData.getPositionA(), pData.getPositionB(), Config.getShopSizeMaxWidth(), Config.getShopSizeMaxHeight()) == null) {
				String size = Config.getShopSizeMaxWidth() + "x" + Config.getShopSizeMaxHeight() + "x" + Config.getShopSizeMaxWidth();
				player.sendMessage(plugin.getResourceManager().getString(MsgType.CMD_SHP_CREATE_SELECTION_PROB_SIZE, new String[]{"%SIZE%"}, new Object[]{size}));
				return null;
			}


			if (pData.getPositionA() == null || pData.getPositionB() == null) {
				player.sendMessage(plugin.getResourceManager().getString(MsgType.CMD_SHP_CREATE_SELECTION_PROB_ONLY_ONE_POINT));
				return null;
			} else {
				return new ShopLocation(pData.getPositionA(), pData.getPositionB());
			}
		} else {
			int x1 = 0, x2 = 0, z1 = 0, z2 = 0;
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
			return new ShopLocation(x1, y - 1, z1, x2, y + Config.getShopSizeDefHeight() - 1, z2);
		}
	}
}
