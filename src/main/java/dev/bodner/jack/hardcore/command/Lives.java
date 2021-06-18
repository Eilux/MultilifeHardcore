package dev.bodner.jack.hardcore.command;

import dev.bodner.jack.hardcore.AdvancedHardcore;
import dev.bodner.jack.hardcore.Util;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

public class Lives implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        AdvancedHardcore plugin = JavaPlugin.getPlugin(AdvancedHardcore.class);
        UUID uuid;
        int num;
        boolean res = false;

        if (sender.hasPermission("hml.lives")){
            if(args.length < 3){
                sender.sendMessage("§cThis command requires 3 arguments");
                return false;
            }
            uuid = Util.getPlayerID(args[2]);
            if (uuid == null || plugin.getPlayerLives().get(uuid) == null){
                sender.sendMessage("§cPlayer must have joined this server before");
                return false;
            }
            try {
                num = Integer.parseInt(args[1]);
                if (num < 0){
                    throw new Exception();
                }
            }
            catch (Exception e){
                sender.sendMessage("§cSecond argument must be a number greater than or equal to 0");
                return false;
            }

            if (plugin.getPlayerLives().get(uuid) == 0 && num > 0 && !args[0].equals("remove")){
                res = true;
            }

            switch (args[0]){
                case "add":
                    plugin.getPlayerLives().replace(uuid, plugin.getPlayerLives().get(uuid) + num);
                    sender.sendMessage("Added lives, player now has " + plugin.getPlayerLives().get(uuid));
                    break;
                case "remove":
                    if (plugin.getPlayerLives().replace(uuid, plugin.getPlayerLives().get(uuid) - num) < 0){
                        sender.sendMessage("Life count cannot be negative");
                        return false;
                    }
                    plugin.getPlayerLives().replace(uuid, plugin.getPlayerLives().get(uuid) - num);
                    sender.sendMessage("Removed lives, player now has " + plugin.getPlayerLives().get(uuid));
                    break;
                case "set":
                    plugin.getPlayerLives().replace(uuid, num);
                    sender.sendMessage("Set lives, player now has " + plugin.getPlayerLives().get(uuid));
                    break;
                default:
                    sender.sendMessage("§cFirst argument must be either \"add\", \"remove\", or \"set\"");
                    return false;
            }

            if (res){
                plugin.resurrect(uuid);
            }
            return true;
        }
        else {
            return false;
        }
    }
}
