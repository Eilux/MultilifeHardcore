package dev.bodner.jack.hardcore;

public enum DeathMode {
    BAN,
    SPECTATE,
    TELEPORT;

    public static DeathMode fromString(String str){
        switch (str){
            case "ban":
                return DeathMode.BAN;
            case "spectate":
                return DeathMode.SPECTATE;
            case "teleport":
                return DeathMode.TELEPORT;
            default:
                return null;
        }
    }
}
