package dev.bodner.jack.hardcore.command;

import dev.bodner.jack.hardcore.AdvancedHardcore;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class ResCost implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        AdvancedHardcore plugin = JavaPlugin.getPlugin(AdvancedHardcore.class);
        StringBuilder cost = new StringBuilder();
        for (ItemStack item : plugin.getResCost()){
            cost.append(ChatColor.GREEN + item.getType().name() + " x " + item.getAmount() + ChatColor.RESET + ", ");
        }
        cost.append(ChatColor.DARK_RED + "Health x " + plugin.getResHealthCost() + ChatColor.RESET);

        sender.sendMessage("Requires: " + cost);
        return true;
    }
}
