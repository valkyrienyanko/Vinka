package com.vinka.player;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Server;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLevelChangeEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Vector;

import com.valkcore.modules.PlayerModule;
import com.valkcore.modules.TextModule;
import com.vinka.Vinka;
import com.vinka.configs.LoadPlayerFiles;
import com.vinka.configs.PlayerFiles;
import com.vinka.events.AbsorbSoulsEvent;
import com.vinka.events.CustomPlayerDeathEvent;
import com.vinka.events.CustomPlayerRespawnEvent;
import com.vinka.utils.Utils;
import com.vinkaitems.VinkaItems;

@SuppressWarnings("unused")
public class PlayerHandler implements Listener {
	/*@EventHandler
	private void shoot(EntityShootBowEvent e) {
		e.setCancelled(true);
		Player p = (Player) e.getEntity();
		int diff = 15;
		for (int i = 0; i < 3; i++) {
			double pitch = ((p.getLocation().getPitch() + 90) * Math.PI) / 180;
			double yaw  = ((p.getLocation().getYaw() + ((90 - diff) + (i * diff)))  * Math.PI) / 180;
			double x = Math.sin(pitch) * Math.cos(yaw);
			double y = Math.sin(pitch) * Math.sin(yaw);
			double z = Math.cos(pitch);
			Vector vector = new Vector(x, z, y);
			final Arrow a = p.launchProjectile(Arrow.class, vector.multiply(e.getForce() * 3));
			new BukkitRunnable() {
				@Override
				public void run() {
					if (a.isDead() || a.isOnGround()) {
						cancel();
					}
					a.getWorld().spawnParticle(Particle.DRIP_LAVA, a.getLocation(), 1);
				}
			}.runTaskTimer(Vinka.vinka, 0, 1);
		}
	}*/
	
	@EventHandler
	private void fishing(PlayerFishEvent e) {
		if (e.getCaught() != null) {
			e.setCancelled(true);
			if (Math.random() < 0.5) {
				Location loc = e.getPlayer().getLocation();
				loc.getWorld().dropItemNaturally(loc, VinkaItems.SALMON().getItem());
				e.getPlayer().sendMessage(TextModule.color("A salmon has given you its soul.. enjoy.."));
			} else {
				e.getPlayer().sendMessage(TextModule.color("The salmon got away!"));
			}
		}
	}
	
	@EventHandler
	private void playerDmg(EntityDamageEvent e) {
		if (e.getEntity() instanceof Player) {
			if (e.getCause() == DamageCause.FALL) {
				e.setCancelled(true);
			}
		}
	}

	@EventHandler
	private void playerDamage(EntityDamageByEntityEvent e) {
		if (e.getEntity() instanceof Player) {
			Player p = (Player) e.getEntity();
			
			if ((p.getHealth() - e.getDamage()) <= 0) { // Player died.
				e.setCancelled(true);
				Bukkit.getPluginManager().callEvent(new CustomPlayerDeathEvent(p, e.getCause(), e.getDamager()));
			}
		}
	}
	
	@EventHandler
	private void playerInteractEntity(PlayerTeleportEvent e) {
		if (e.getPlayer().getGameMode() == GameMode.SPECTATOR && e.getCause() == TeleportCause.SPECTATE) {
			e.setCancelled(true);
			
			if (e.getPlayer().getSpectatorTarget() instanceof Player) {
				Player target = (Player) e.getPlayer().getSpectatorTarget();
				e.getPlayer().openInventory(target.getInventory());
			}
				
			e.getPlayer().setSpectatorTarget(null);
			return;
		}
	}

	@EventHandler
	private void playerInteractEvent(PlayerInteractEvent e) {
		interactSouls(e);
		interactElytra(e);
		interactBook(e);
	}
	
	private void interactBook(PlayerInteractEvent e) {
		if (e.getMaterial() != Material.WRITTEN_BOOK)
			return;
		switch (e.getItem().getItemMeta().getDisplayName().toLowerCase()) {
		case "recipes":
			//e.getPlayer().openInventory(arg0)
			break;
		case "shop":
			e.getPlayer().openInventory(shop());
			break;
		default:
			break;
		}
	}
	
	private Inventory shop() {
		Inventory inv = Bukkit.createInventory(null, 27, "Shop");
		return inv;
	}
	
	private void interactElytra(PlayerInteractEvent e) {
		final Player p = e.getPlayer();
		ItemStack chestplate = p.getEquipment().getChestplate();
		if (chestplate == null) return;
		if (chestplate.getType() != Material.ELYTRA) return;
		if (e.getHand() != EquipmentSlot.HAND) return;
		if (e.getAction() != Action.RIGHT_CLICK_AIR) return;
		if (!p.isOnGround()) return;
		
		Material handItemType = p.getEquipment().getItemInMainHand().getType();
		
		switch (handItemType) {
		case BOW:
		case WRITTEN_BOOK:
		case FLINT_AND_STEEL:
		case SUGAR:
		case FISHING_ROD:
		case BUCKET:
		case LAVA_BUCKET:
		case WATER_BUCKET:
		case POTION:
		case SPLASH_POTION:
		case SHEARS:
		case LEAD:
		case WHEAT:
			return;
		default:
			break;
		}
		
		p.setVelocity(new Vector(0, 10, 0));
		new BukkitRunnable() {
			@Override
			public void run() {
				p.setGliding(true);
			}
		}.runTaskLater(Vinka.vinka, 40);
	}
	
	private void interactSouls(PlayerInteractEvent e) {
		Player p = e.getPlayer();
		if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK)
			return;
		if (e.getMaterial() != Material.SUGAR)
			return;
		if (!PlayerModule.inSurvival(p))
			return;
		ItemStack item = e.getItem();
		int num = (int) (1 + Math.random() * 10) * item.getAmount();
		Bukkit.getServer().getPluginManager().callEvent(new AbsorbSoulsEvent(e.getPlayer(), num, item));
	}

	@EventHandler
	private void foodChangeEvent(FoodLevelChangeEvent e) {
		if (e.getEntity() instanceof Player) {
			Player p = (Player) e.getEntity();
			if (!PlayerModule.inSurvival(p))
				return;
			if (e.getFoodLevel() > 0)
				return;
			if (!e.getEntity().hasPotionEffect(PotionEffectType.HARM)) {
				e.getEntity().addPotionEffect(new PotionEffect(PotionEffectType.HARM, 20, 1));
			}
		}
	}

	@SuppressWarnings("deprecation")
	@EventHandler
	private void joinEvent(PlayerJoinEvent e) {
		final Player p = e.getPlayer();
		
		p.setGameMode(GameMode.SURVIVAL);
		
		PlayerFiles cm = PlayerFiles.getConfig(e.getPlayer());
		if (!cm.exists()) {
			try {
				cm.createFile();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
		
		if (!p.hasPlayedBefore()) {
			p.setInvulnerable(true);
			
			new BukkitRunnable() {
				@Override
				public void run() {
					p.sendMessage(TextModule.color("&cYou're invunrable to any damage for &410 &cminutes."));
				}
			}.runTaskLater(Vinka.vinka, 1000);
			
			new BukkitRunnable() {
				@Override
				public void run() {
					p.setInvulnerable(false);
					p.sendMessage(TextModule.color("&cYou're no longer invunrable."));
				}
			}.runTaskLater(Vinka.vinka, 12000);
			
			PlayerInventory inv = p.getInventory();
			p.getEquipment().setChestplate(VinkaItems.ELYTRA().getItem());
			inv.addItem(VinkaItems.WOODEN_SWORD().getItem());
			ItemStack bookGuide = new ItemStack(Material.WRITTEN_BOOK);
			BookMeta bm = (BookMeta) bookGuide.getItemMeta();
			bm.setAuthor("valkyrienyanko");
			bm.setTitle("Server Guide");
			List<String> pages = new ArrayList<String>();
			pages.add(TextModule.color("&c&lHelp\n&r"
					+ "\n/recipes"
					+ "\n/help"
					+ "\n/info"
					+ "\n/cmds"));
			bm.setPages(pages);
			bookGuide.setItemMeta(bm);
			inv.addItem(bookGuide);
		} else {
			p.setInvulnerable(false);
			p.setFlySpeed(0.1f);
		}

		p.discoverRecipes(VinkaItems.recipes);
		
		p.getWorld().playSound(p.getLocation(), Sound.AMBIENT_CAVE, 1f, 1f);

		p.setHealth(p.getMaxHealth());
		Utils.updateHealth(p);

		new BukkitRunnable() {
			@Override
			public void run() {
				p.sendMessage(TextModule.color("Welcome, " + p.getDisplayName() + ", everything you see is in &qbeta &wand may dramatically change over the duration of one day."));
			}
		}.runTaskLater(Vinka.vinka, 200);

		new BukkitRunnable() {
			@Override
			public void run() {
				p.sendMessage(TextModule.color(
						"There is currently &qno solid tutorial, &wplease &qask &wsomeone on the &qserver &wor &qdiscord &wfor help."));
			}
		}.runTaskLater(Vinka.vinka, 400);

		new BukkitRunnable() {
			@Override
			public void run() {
				p.sendMessage(TextModule.color(
						"The discord can be found in &q/help&w."));
			}
		}.runTaskLater(Vinka.vinka, 600);

		new BukkitRunnable() {
			@Override
			public void run() {
				p.sendMessage(TextModule.color("&wGood luck and have fun."));
			}
		}.runTaskLater(Vinka.vinka, 800);
	}

	/*
	 * Update player level at a cap of 100.
	 */
	@EventHandler
	private void levelChange(PlayerLevelChangeEvent e) {
		if (e.getNewLevel() > 100) {
			e.getPlayer().setLevel(100);
		} else {
			if (e.getNewLevel() == 0)
				return;
			e.getPlayer().sendTitle("", "Lv" + e.getNewLevel(), 50, 100, 50);
		}
	}

	private boolean monstersNearby(PlayerMoveEvent e, int radius) {
		for (Entity entity : e.getPlayer().getNearbyEntities(radius, radius, radius)) {
			if (entity instanceof Monster) {
				return true;
			}
		}
		return false;
	}

	@EventHandler
	private void customName(PlayerDropItemEvent e) {
		Player p = e.getPlayer();

		if (!PlayerModule.inSurvival(p))
			return;

		String[] names = new String[] { "die", "hunted", "run", "death", "hunger", "darkness", "evil", "nothing",
				"alone", "secrets", "unknown", "whispering", "?", "suffering", "error" };

		Item droppedItem = e.getItemDrop();

		droppedItem.setCustomName(TextModule.color(names[new Random().nextInt(names.length)]));
		droppedItem.setCustomNameVisible(true);
	}
}
