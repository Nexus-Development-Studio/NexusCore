package cc.synkdev.nexuscore.components.folia;

public final class Platform {
    private Platform() {
        /* This utility class should not be instantiated */
    }

    private static final boolean FOLIA;

    static {
        boolean folia;
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            folia = true;
        } catch (ClassNotFoundException _) {
            folia = false;
        }
        FOLIA = folia;
    }

    public static boolean isFolia() {
        return FOLIA;
    }
}

