package dev.bodner.jack.hardcore;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import dev.bodner.jack.hardcore.command.Lives;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;
import java.util.logging.Logger;

public final class AdvancedHardcore extends JavaPlugin implements Listener {
    final String DATA_PATH = getDataFolder().getAbsolutePath() + File.separator + "data";
    Logger logger = Bukkit.getLogger();
    Type type = new TypeToken<Map<UUID, Integer>>(){}.getType();

    Objective objective;
    Team green;
    Team yellow;
    Team red;
    Team single;
    Team dead;

    private HashMap<UUID, Integer> playerLives = new HashMap<>();

    int initialLives;
    DeathMode deathMode;
    Location location;
    boolean pvpOnLife1;
    boolean resurrection;
    ArrayList<ItemStack> resCost = new ArrayList<>();
    int resHealthCost;
    int resHealthMin;
    int resHealth;

    File dataPathFile = new File(DATA_PATH);
    File lifeCounter = new File(DATA_PATH + File.separator + "lives.json");

    @Override
    public void onEnable() {
        Gson gson = new GsonBuilder().serializeNulls().setPrettyPrinting().create();
        JsonParser parser = new JsonParser();

        this.saveDefaultConfig();

        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        if (scoreboard.getObjective("HML_lives") != null){
            scoreboard.getObjective("HML_lives").unregister();
        }
        objective = scoreboard.registerNewObjective("HML_lives", "dummy");
        objective.setDisplayName("❤");
        objective.setDisplaySlot(DisplaySlot.BELOW_NAME);

        if (scoreboard.getTeam("HML_green") != null){
            scoreboard.getTeam("HML_green").unregister();
        }
        green = scoreboard.registerNewTeam("HML_green");
        green.setColor(ChatColor.GREEN);

        if (scoreboard.getTeam("HML_yellow") != null){
            scoreboard.getTeam("HML_yellow").unregister();
        }
        yellow = scoreboard.registerNewTeam("HML_yellow");
        yellow.setColor(ChatColor.YELLOW);

        if (scoreboard.getTeam("HML_red") != null){
            scoreboard.getTeam("HML_red").unregister();
        }
        red = scoreboard.registerNewTeam("HML_red");
        red.setColor(ChatColor.RED);

        if (scoreboard.getTeam("HML_single") != null){
            scoreboard.getTeam("HML_single").unregister();
        }
        single = scoreboard.registerNewTeam("HML_single");
        single.setColor(ChatColor.DARK_RED);

        if (scoreboard.getTeam("HML_dead") != null){
            scoreboard.getTeam("HML_dead").unregister();
        }
        dead = scoreboard.registerNewTeam("HML_dead");
        dead.setColor(ChatColor.GRAY);

        initialLives = getConfig().getInt("lives");
        deathMode = DeathMode.fromString(getConfig().getString("mode"));
        pvpOnLife1 = getConfig().getBoolean("oneLifePvP");
        if (Bukkit.getWorld(getConfig().getString("teleportWorld")) == null){
            location = null;
            if (deathMode.equals(DeathMode.TELEPORT)){
                logger.warning("Invalid teleport location, changing mode from \"teleport\" to \"spectate\"");
                deathMode = DeathMode.SPECTATE;
            }
        }
        else {
            World world = Bukkit.getWorld(getConfig().getString("teleportWorld"));
            List<Double> coordinates = getConfig().getDoubleList("teleportCoordinates");
            location = new Location(world, coordinates.get(0), coordinates.get(1), coordinates.get(2), coordinates.get(3).floatValue(), coordinates.get(4).floatValue());
        }
        resurrection = getConfig().getBoolean("resurrection");
        List<Map<?, ?>> temp = getConfig().getMapList("resItemCost");
        for (Map<?, ?> map : temp) {
            map.forEach((v,k) -> resCost.add(new ItemStack(Material.valueOf((String) v), (int)k)));
        }
        resHealthCost = getConfig().getInt("resHealthCost");
        resHealthMin = getConfig().getInt("resHealthMin");
        resHealth = getConfig().getInt("resHealth");

        try {
            if (!dataPathFile.exists()){
                logger.info("data folder does not exist, creating...");
                boolean success = dataPathFile.mkdir();
                if (success){
                    logger.info("data folder successfully created.");
                }
            }
            if (!lifeCounter.exists()){
                logger.info("lives.json does not exist, creating...");
                lifeCounter.createNewFile();
                BufferedWriter writer = new BufferedWriter(new FileWriter(lifeCounter));
                writer.write(gson.toJson(playerLives, type));
                writer.close();
                logger.info("lives.json successfully created.");
            }
            try{
                FileInputStream stream = new FileInputStream(lifeCounter);
                JsonObject jsonObject = (JsonObject)parser.parse(new InputStreamReader(stream));
                playerLives = gson.fromJson(jsonObject, type);
            }
            catch (Exception e){
                logger.warning("there was an issue reading lives.json, creating empty array");
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }

        getServer().getPluginManager().registerEvents(this,this);

        BukkitScheduler scheduler = getServer().getScheduler();
        scheduler.scheduleSyncRepeatingTask(this, () -> {
            playerLives.forEach((v, k) -> {
                Player player = Bukkit.getPlayer(v);
                if (player != null){
                    ChatColor color;
                    objective.getScore(player.getName()).setScore(k);
                    if (k>1) {
                        float ratio = k / (float) initialLives;
                        if (ratio > 1 / 3F) {
                            if (ratio > 2 / 3F) {
                                green.addPlayer(player);
                                color = ChatColor.GREEN;
                            } else {
                                yellow.addPlayer(player);
                                color = ChatColor.YELLOW;
                            }
                        } else {
                            red.addPlayer(player);
                            color = ChatColor.RED;
                        }
                    }
                    else {
                        if (k == 1){
                            single.addPlayer(player);
                            color = ChatColor.DARK_RED;
                        }
                        else {
                            dead.addPlayer(player);
                            color = ChatColor.GRAY;
                        }
                    }
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(color.toString() + k + " Lives"));
                }
            });
        }, 0, 1);

        this.getCommand("lives").setExecutor(new Lives());
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        if (!playerLives.containsKey(id)){
            playerLives.replace(id,initialLives);
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event){
        UUID id = event.getEntity().getUniqueId();
        if (playerLives.get(id) > 0){
            playerLives.replace(id, playerLives.get(id) - 1);
        }
        if (deathMode.equals(DeathMode.BAN) && playerLives.get(id) <= 0){
            Bukkit.getBanList(BanList.Type.NAME).addBan(event.getEntity().getName(),"§fGame Over.",null,null);
            new KickPlayer(event.getEntity()).runTaskLater(this, 1);
        }
        if (deathMode.equals(DeathMode.SPECTATE) && playerLives.get(id) <= 0){
            event.getEntity().setGameMode(GameMode.SPECTATOR);
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event){
        UUID id = event.getPlayer().getUniqueId();
        if (deathMode.equals(DeathMode.TELEPORT) && playerLives.get(id) <= 0){
            event.setRespawnLocation(location);
        }
    }



    @Override
    public void onDisable() {
        Gson gson = new GsonBuilder().serializeNulls().setPrettyPrinting().create();
        objective.unregister();
        green.unregister();
        yellow.unregister();
        red.unregister();
        single.unregister();
        dead.unregister();
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(lifeCounter));
            writer.write(gson.toJson(playerLives, type));
            writer.close();
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    private boolean canPlayerHurt(Player target){
        return playerLives.get(target.getUniqueId()) == 1;
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent e){
        //I kind of "borrowed" most of this from this post: https://bukkit.org/threads/stopping-players-from-damaging-other-players-in-same-array.125550/

        if (!pvpOnLife1){
            return;
        }

        if(!(e.getEntity() instanceof Player)) {
            return;
        }

        Player victim = (Player) e.getEntity();

        Player attacker = null;

        if(e.getDamager() instanceof Player) {
            attacker = (Player) e.getDamager();
        } else if(e.getDamager() instanceof Arrow) {
            Arrow arrow = (Arrow) e.getDamager();
            if(!(arrow.getShooter() instanceof Player)) {
                return;
            }
            attacker = (Player) arrow.getShooter();
        } else if(e.getDamager() instanceof ThrownPotion) {
            return;
        } else if(e.getDamager() instanceof Trident){
            Trident trident = (Trident) e.getDamager();
            if (!(trident.getShooter() instanceof Player)){
                return;
            }
            attacker = (Player) trident.getShooter();
        }

        if(victim == attacker) {
            return;
        }

        if(attacker == null) {
            return;
        }

        if (!(canPlayerHurt(attacker)) && !(canPlayerHurt(victim))){
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onPotionSplash(PotionSplashEvent e) {
        if (!pvpOnLife1){
            return;
        }
        boolean cancel = true;
        for(PotionEffect effect : e.getEntity().getEffects()) {
            if(effect.getType().getName().equalsIgnoreCase("harm") || effect.getType().getName().equalsIgnoreCase("poison")) {
                cancel = false;
            }
        }

        if(cancel) return;

        if(!(e.getPotion().getShooter() instanceof Player)) {
            return;
        }

        Player attacker = (Player) e.getPotion().getShooter();

        Player victim;
        for(LivingEntity entity : e.getAffectedEntities()) {
            if(entity instanceof Player) {
                victim = (Player) entity;

                if(victim == attacker) {
                    continue;
                }

                if (!(canPlayerHurt(attacker)) && !(canPlayerHurt(victim))){
                    e.setIntensity(victim, 0);
                }
            }
        }
    }

    public boolean resurrect(UUID uuid){
        Player player = Bukkit.getPlayer(uuid);

        if (player != null){
            Location location = player.getBedSpawnLocation();
            if (location == null){
                location = Bukkit.getWorlds().get(0).getSpawnLocation();
            }
        }

        switch (deathMode){
            case SPECTATE:
                if (player == null){
                    return false;
                }
                player.teleport(location);
                player.setGameMode(GameMode.SURVIVAL);
                return true;
            case TELEPORT:
                if (player == null){
                    return false;
                }
                player.teleport(location);
                return true;
            case BAN:
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
                Bukkit.getBanList(BanList.Type.NAME).pardon(offlinePlayer.getName());
                return true;
            default:
                return false;
        }
    }

    public boolean resurrect(UUID uuid, Location location){
        Player player = Bukkit.getPlayer(uuid);

        switch (deathMode){
            case SPECTATE:
                if (player == null){
                    return false;
                }
                player.teleport(location);
                player.setGameMode(GameMode.SURVIVAL);
                return true;
            case TELEPORT:
                if (player == null){
                    return false;
                }
                player.teleport(location);
                return true;
            case BAN:
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
                Bukkit.getBanList(BanList.Type.NAME).pardon(offlinePlayer.getName());
                return true;
            default:
                return false;
        }
    }


    public HashMap<UUID, Integer> getPlayerLives() {
        return playerLives;
    }
}

class KickPlayer extends BukkitRunnable{
    private final Player player;

    public KickPlayer(Player player){
        this.player = player;
    }

    @Override
    public void run() {
        player.kickPlayer("§fGame Over.");
    }
}
