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
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftPlayer;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
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
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Vector;

import com.valkutils.ValkUtils;
import com.valkutils.modules.BlockModule;
import com.valkutils.modules.PlayerModule;
import com.valkutils.modules.TextModule;
import com.vinka.Vinka;
import com.vinka.configs.LoadPlayerFiles;
import com.vinka.configs.PlayerFiles;
import com.vinka.events.CustomPlayerRespawnEvent;
import com.vinka.utils.Utils;
import com.vinkaitems.VinkaItems;

@SuppressWarnings("unused")
public class PlayerHandler implements Listener {
	@EventHandler
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
	}
	
	@EventHandler
	private void fishing(PlayerFishEvent e) {
		if (e.getCaught() != null) {
			e.setCancelled(true);
			Location loc = e.getPlayer().getLocation();
			loc.getWorld().dropItemNaturally(loc, VinkaItems.RAW_SALMON());
			e.getPlayer().sendMessage(TextModule.color("&7A salmon has given you its soul.. enjoy.."));
		}
	}

	@EventHandler
	private void playerDamage(EntityDamageEvent e) {
		if (e.getEntity() instanceof Player) {
			Player p = (Player) e.getEntity();
			
			if (e.getCause() == DamageCause.FALL) {
				e.setCancelled(true);
				return;
			}
			
			if ((p.getHealth() - e.getDamage()) <= 0) {
				// Player died.
				e.setCancelled(true);
				
				p.getWorld().playSound(p.getLocation(), Sound.AMBIENT_CAVE, 1f, 1f);
				
				Vinka.vinka.getServer().broadcastMessage(TextModule.color("&f" + p.getDisplayName() + " &7died from " + e.getCause().name().toLowerCase() + " damage."));
				
				PlayerModule.removeAllPotionEffects(p);
				
				p.setGameMode(GameMode.SPECTATOR);
				
				p.setFlySpeed(0.0f);
				PlayerModule.fillPlayerHealth(p);
				PlayerModule.fillPlayerFood(p);
				p.setLevel(0);
				p.setExp(0);
				p.sendTitle("", "You Died", 20, 80, 0);
				
				Utils.spawnMonster(p.getLocation(), EntityType.HUSK, p);
				
				int task = Bukkit.getScheduler().scheduleSyncRepeatingTask(Vinka.vinka, new Runnable() {
		            @Override
		            public void run() {
		            	p.setVelocity(new Vector(0, 0.03, 0));
		            }
		        }, 0L, 1L);
				
				new BukkitRunnable() {
					@Override
					public void run() {
						Bukkit.getScheduler().cancelTask(task);
						PlayerModule.addPotionEffect(p, PotionEffectType.BLINDNESS, 40, 1);
					}
				}.runTaskLater(Vinka.vinka, 80);
				
				new BukkitRunnable() {
					@Override
					public void run() {
						if (p.getBedSpawnLocation() != null) {
							p.teleport(p.getBedSpawnLocation());
						} else {
							p.teleport(p.getWorld().getSpawnLocation());
						}
						p.setGameMode(GameMode.SURVIVAL);
						p.setFlySpeed(0.1f);
						
						p.getWorld().playSound(p.getLocation(), Sound.MUSIC_DISC_13, 1f, 1f);
						
						// Call the custom player respawn event since the vanilla respawn event is never called.
						Bukkit.getServer().getPluginManager().callEvent(new CustomPlayerRespawnEvent(p));
					}
				}.runTaskLater(Vinka.vinka, 100);
			}
		}
	}
	
	@EventHandler
	private void playerInteractEntity(PlayerTeleportEvent e) {
		if (e.getPlayer().getGameMode() == GameMode.SPECTATOR && e.getCause() == TeleportCause.SPECTATE) {
			e.setCancelled(true);
			e.getPlayer().setSpectatorTarget(null);
			return;
		}
	}

	@EventHandler
	private void playerInteractEvent(PlayerInteractEvent e) {
		interactSouls(e);
		interactElytra(e);
	}
	
	private void interactElytra(PlayerInteractEvent e) {
		Player p = e.getPlayer();
		ItemStack chestplate = p.getEquipment().getChestplate();
		if (chestplate == null) return;
		if (chestplate.getType() != Material.ELYTRA) return;
		if (e.getHand() != EquipmentSlot.HAND) return;
		if (e.getAction() != Action.RIGHT_CLICK_AIR) return;
		if (!p.isOnGround()) return;
		
		p.setVelocity(new Vector(0, 10, 0));
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
		e.getPlayer().giveExp(num);
		Utils.updateHealth(p);
		p.sendMessage(TextModule.color("&7You absorbed &f" + num + " &7souls."));
		item.setAmount(0);
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

	@EventHandler
	private void joinEvent(PlayerJoinEvent e) {
		Player p = e.getPlayer();
		
		Scoreboard board = Bukkit.getServer().getScoreboardManager().getMainScoreboard();
		Team team;
		if (board.getTeam("Default") == null) {
			board.registerNewTeam("Default");
			team = board.getTeam("Default");
			team.setColor(ChatColor.GRAY);
		} else {
			team = board.getTeam("Default");
		}
		
		if (!team.hasEntry(e.getPlayer().getName())) {
			team.addEntry(e.getPlayer().getName());
		}
		
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
			inv.addItem(VinkaItems.WOODEN_PICKAXE());
			inv.addItem(VinkaItems.WOODEN_AXE());
			inv.addItem(VinkaItems.WOODEN_SHOVEL());
			inv.addItem(VinkaItems.WOODEN_HOE());
			inv.addItem(VinkaItems.WOODEN_SWORD());
			ItemStack ladder = VinkaItems.LADDER();
			ladder.setAmount(15);
			inv.addItem(ladder);
			inv.addItem(VinkaItems.BAKED_POTATO());
			ItemStack bookGuide = new ItemStack(Material.WRITTEN_BOOK);
			BookMeta bm = (BookMeta) bookGuide.getItemMeta();
			bm.setAuthor("valkyrienyanko");
			bm.setTitle("Server Guide");
			List<String> pages = new ArrayList<String>();
			pages.add(TextModule.color("&c&lPrologue\n&rWelcome to ValkMC, a unforgiving difficult survival server."));
			pages.add(TextModule.color("&c&lCrafting\n&rGather wood to make sticks and slabs.\n\nUsing these materials, you can make your first set of tools.\n\nTo see all the recipes, use the green book in your inventory."));
			pages.add(TextModule.color("&c&lBlocks\n&rBlocks can only be mined if you have the proper tools.\n\nBetter tools have more chance of harvesting more materials.\n\nMost blocks have gravity on place except iron blocks."));
			pages.add(TextModule.color("&c&lMonsters\n&rMonsters should be avoided at all costs, they drain your hunger, they split into silverfish on death.\n\nThough they drop souls on death which increase your hearts when used."));
			bm.setPages(pages);
			bookGuide.setItemMeta(bm);
			inv.addItem(bookGuide);
		} else {
			p.setInvulnerable(false);
			p.setFlySpeed(0.1f);
		}

		p.discoverRecipes(ValkUtils.plugin.recipes);
		
		p.getWorld().strikeLightningEffect(p.getLocation());
		p.getWorld().playSound(p.getLocation(), Sound.AMBIENT_CAVE, 1f, 1f);

		p.setHealth(6);
		Utils.updateHealth(p);

		new BukkitRunnable() {
			@Override
			public void run() {
				p.sendMessage(TextModule.color("&7Welcome, " + p.getDisplayName() + ", everything you see is in &fbeta &7and may dramatically change over the duration of one day."));
			}
		}.runTaskLater(Vinka.vinka, 200);

		new BukkitRunnable() {
			@Override
			public void run() {
				p.sendMessage(TextModule.color(
						"&7There is currently &fno solid tutorial, &7please &fask &7someone on the &fserver &7or &fdiscord &7for help."));
			}
		}.runTaskLater(Vinka.vinka, 400);

		new BukkitRunnable() {
			@Override
			public void run() {
				p.sendMessage(TextModule.color(
						"&7The discord can be found in &f/help&7."));
			}
		}.runTaskLater(Vinka.vinka, 600);

		new BukkitRunnable() {
			@Override
			public void run() {
				p.sendMessage(TextModule.color("&7Good luck and have fun."));
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

		droppedItem.setCustomName(TextModule.color("&7" + names[new Random().nextInt(names.length)]));
		droppedItem.setCustomNameVisible(true);
	}
	
	/*
	 * This event is very intensive on the server!
	 */
	
	@EventHandler
	private void moveEvent(PlayerMoveEvent e) {
		Player p = e.getPlayer();
	
		if (!PlayerModule.inSurvival(p))
			return;
		
		Block b = e.getFrom().getBlock();
	
		if (b.getLightFromBlocks() == 0 && b.getY() < 20) {
			PlayerModule.addPotionEffect(p, PotionEffectType.BLINDNESS, 100, 1);
			PlayerModule.addPotionEffect(p, PotionEffectType.WITHER, 20, 1);
		}
	
		/*if (b.getLightFromSky() == 15 && !b.isLiquid() && WorldModule.day()) {
			
		}*/
	
		/*if (b.isLiquid()) {
			PlayerModule.addPotionEffect(p, PotionEffectType.POISON, 40, 2);
		}*/
		
		/*if (BlockModule.isPlant(b.getType())) {
			b.setType(Material.AIR);
		}*/
		
		/*if (BlockModule.isLeaves(b.getRelative(BlockFace.DOWN).getType())) {
			b.getRelative(BlockFace.DOWN).setType(Material.AIR);
		}*/
	}
}
