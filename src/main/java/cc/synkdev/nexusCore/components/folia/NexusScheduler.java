package cc.synkdev.nexusCore.components.folia;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class NexusScheduler {

    private static final boolean FOLIA;
    private static Method foliaRunRepeating;
    private static Method foliaRunDelayed;
    private static final Set<Object> ACTIVE_TASKS =
            ConcurrentHashMap.newKeySet();

    static {
        boolean folia;
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            folia = true;
        } catch (ClassNotFoundException e) {
            folia = false;
        }
        FOLIA = folia;

        if (FOLIA) {
            try {
                Object scheduler = Bukkit.class
                        .getMethod("getGlobalRegionScheduler")
                        .invoke(null);

                Class<?> schedulerClass = scheduler.getClass();

                foliaRunRepeating = schedulerClass.getMethod(
                        "runAtFixedRate",
                        Plugin.class,
                        Consumer.class,
                        long.class,
                        long.class
                );

                foliaRunDelayed = schedulerClass.getMethod(
                        "runDelayed",
                        Plugin.class,
                        Consumer.class,
                        long.class
                );

            } catch (ReflectiveOperationException e) {
                throw new RuntimeException("Failed to initialize Folia scheduler", e);
            }
        }
    }

    public static void runTaskTimer(
            Plugin plugin,
            Runnable task,
            long delay,
            long period
    ) {
       runTaskTimer(plugin, task, delay, period, false);
    }

    public static void runTaskTimer(
            Plugin plugin,
            Runnable task,
            long delay,
            long period,
            boolean async
    ) {
        if (FOLIA) {
            try {
                Object scheduler = Bukkit.class
                        .getMethod("getGlobalRegionScheduler")
                        .invoke(null);

                long safeDelay = Math.max(1L, delay);
                long safePeriod = Math.max(1L, period);

                Object scheduledTask = foliaRunRepeating.invoke(
                        scheduler,
                        plugin,
                        (Consumer<?>) t -> task.run(),
                        safeDelay,
                        safePeriod
                );

                ACTIVE_TASKS.add(scheduledTask);

            } catch (ReflectiveOperationException e) {
                e.printStackTrace();
            }
        } else {
            if (async) {
                Bukkit.getScheduler().runTaskTimerAsynchronously(
                        plugin,
                        task,
                        delay,
                        period
                );
            } else {
                Bukkit.getScheduler().runTaskTimer(
                        plugin,
                        task,
                        delay,
                        period
                );
            }
        }
    }


    public static void runTaskLater(
            Plugin plugin,
            Runnable task,
            long delay
    ) {
        runTaskLater(plugin, task, delay, false);
    }

    public static void runTaskLater(
            Plugin plugin,
            Runnable task,
            long delay,
            boolean async
    ) {
        if (FOLIA) {
            try {
                Object scheduler = Bukkit.class
                        .getMethod("getGlobalRegionScheduler")
                        .invoke(null);

                long safeDelay = Math.max(1L, delay);

                foliaRunDelayed.invoke(
                        scheduler,
                        plugin,
                        (Consumer<?>) t -> task.run(),
                        safeDelay
                );

            } catch (ReflectiveOperationException e) {
                e.printStackTrace();
            }
        } else {
            if (async) {
                Bukkit.getScheduler().runTaskLaterAsynchronously(
                        plugin,
                        task,
                        delay
                );
            } else {
                Bukkit.getScheduler().runTaskLater(
                        plugin,
                        task,
                        delay
                );
            }
        }
    }
}
