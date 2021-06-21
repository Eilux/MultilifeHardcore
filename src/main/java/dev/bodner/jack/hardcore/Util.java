package dev.bodner.jack.hardcore;

import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;

public final class Util {
    public static UUID getPlayerID(String name){
        Gson gson = new Gson();
        JsonParser jsonParser = new JsonParser();
        UUID id = null;

        if (Bukkit.getPlayer(name) != null){
            id = Bukkit.getPlayer(name).getUniqueId();
        }
        else {
            try {
                URL request = new URL("https://api.mojang.com/users/profiles/minecraft/" + name);
                HttpURLConnection connection = (HttpURLConnection) request.openConnection();
                connection.setRequestMethod("GET");
                JsonObject jsonObject = (JsonObject)jsonParser.parse(new InputStreamReader(connection.getInputStream()));
                PlayerRequest playerRequest = gson.fromJson(jsonObject, PlayerRequest.class);
                id = UUID.fromString(playerRequest.getId().replaceFirst("(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)", "$1-$2-$3-$4-$5")); //https://stackoverflow.com/questions/18986712/creating-a-uuid-from-a-string-with-no-dashes/32594202
                connection.disconnect();
            }
            catch (Exception ignored){
            }
        }
        return id;
    }

    //From here: https://bukkit.org/threads/solved-player-direction.72789/
    public static String getCardinalDirection(Player player) {
        double rotation = (player.getLocation().getYaw() - 90) % 360;
        if (rotation < 0) {
            rotation += 360.0;
        }
        if (0 <= rotation && rotation < 22.5) {
            return "N";
        } else if (22.5 <= rotation && rotation < 67.5) {
            return "NE";
        } else if (67.5 <= rotation && rotation < 112.5) {
            return "E";
        } else if (112.5 <= rotation && rotation < 157.5) {
            return "SE";
        } else if (157.5 <= rotation && rotation < 202.5) {
            return "S";
        } else if (202.5 <= rotation && rotation < 247.5) {
            return "SW";
        } else if (247.5 <= rotation && rotation < 292.5) {
            return "W";
        } else if (292.5 <= rotation && rotation < 337.5) {
            return "NW";
        } else if (337.5 <= rotation && rotation < 360.0) {
            return "N";
        } else {
            return null;
        }
    }

    //Adapted from here: https://www.spigotmc.org/threads/remove-a-specific-amount-of-items-from-a-chest.388457/
    public static void removeItems(Inventory inventory, ItemStack item, int toRemove) {
        if (inventory == null || item == null || toRemove < 0){
            return;
        }
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack loopItem = inventory.getItem(i);
            if (loopItem == null || !item.isSimilar(loopItem)) {
                continue;
            }
            if (toRemove <= 0) {
                return;
            }
            if (toRemove < loopItem.getAmount()) {
                loopItem.setAmount(loopItem.getAmount() - toRemove);
                return;
            }
            inventory.clear(i);
            toRemove -= loopItem.getAmount();
        }
    }

}

class PlayerRequest {
    String id;
    String name;
    boolean legacy;
    boolean demo;

    public String getId() {
        return id;
    }
}

