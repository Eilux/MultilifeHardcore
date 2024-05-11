package dev.bodner.jack.hardcore.command;

import dev.bodner.jack.hardcore.AdvancedHardcore;
import dev.bodner.jack.hardcore.Util;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class Resurrect implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        AdvancedHardcore plugin = JavaPlugin.getPlugin(AdvancedHardcore.class);
        UUID uuid;
        AtomicBoolean hasItems = new AtomicBoolean(true);
        AtomicBoolean hasHealth = new AtomicBoolean(true);
        if (args.length != 1) {
            return false;
        }
        if (sender.hasPermission("hml.res") && sender instanceof Player){
            if (!plugin.isResurrection()){
                sender.sendMessage("§cResurrection must be enabled on this server");
                return true;
            }
            Player player = (Player)sender;
            uuid = Util.getPlayerID(args[0]);
            if (uuid == null || plugin.getPlayerLives().get(uuid) == null){
                sender.sendMessage("§cPlayer must have joined this server before");
                return true;
            }

            plugin.getResCost().forEach((v) -> {
                if (!player.getInventory().containsAtLeast(v,v.getAmount())){
                    hasItems.set(false);
                }
            });

            if (player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue() <= plugin.getResHealthMin()){
                sender.sendMessage("§cYou must have more than " + plugin.getResHealthMin() + " health to resurrect");
                hasHealth.set(false);
                return true;
            }

            if (plugin.getPlayerLives().get(uuid) >= 1){
                player.sendMessage("§cPlayer must be dead");
                return true;
            }

            if (hasItems.get() && hasHealth.get()){
                Location location = player.getLocation();

                double rotation = (player.getLocation().getYaw() - 90) % 360;
                if (rotation < 0) {
                    rotation += 360.0;
                }

                double x = Math.cos(Math.toRadians(rotation)) * 2.5;
                double z = Math.sin(Math.toRadians(rotation)) * 2.5;

                location.add(-x, 0, -z);
                location.setYaw((((float)(rotation + 90)) + 180F) % 360);

                Util.DrawParticleCircle(location, 2, Particle.PORTAL);
                Util.DrawParticleCircle(location, 1, Particle.PORTAL);
                Util.DrawParticleCircle(location, 0.5, Particle.PORTAL);

                location.getWorld().playSound(location, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 20, 1.2F);

                plugin.getResCost().forEach((v) -> {
                    Util.removeItems(player.getInventory(), v, v.getAmount());
                });
                player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue() - plugin.getResHealthCost());
                plugin.resurrect(uuid,location);
            }
            else {
                StringBuilder cost = new StringBuilder();
                for (ItemStack item : plugin.getResCost()){
                    cost.append(ChatColor.GREEN + item.getType().name() + " x " + item.getAmount() + ChatColor.RESET + ", ");
                }
                cost.append(ChatColor.DARK_RED + "Health x " + plugin.getResHealthCost() + ChatColor.RESET);

                sender.sendMessage("§cRequires: " + cost);
                return true;
            }

        }
        else {
            sender.sendMessage("§cSender must have permission and be a player");
        }
        return true;
    }
}
