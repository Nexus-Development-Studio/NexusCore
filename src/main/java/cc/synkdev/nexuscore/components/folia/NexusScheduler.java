package cc.synkdev.nexuscore.components.folia;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public final class NexusScheduler {
    private NexusScheduler() {
        /* This utility class should not be instantiated */
    }


    private static final boolean FOLIA;
    private static Method getGlobalScheduler;
    private static Method getAsyncScheduler;
    private static Method asyncRunNow;
    private static Method asyncRunLater;
    private static Method asyncRunTimer;
    private static Method globalRun;
    private static Method globalRunLater;
    private static Method globalRunTimer;
    private static final Set<Object> ACTIVE_TASKS =
            ConcurrentHashMap.newKeySet();

    static {
        boolean folia;
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            folia = true;
        } catch (ClassNotFoundException _) {
            folia = false;
        }
        FOLIA = folia;

        if (FOLIA) {
            try {
                Object scheduler = Bukkit.class
                        .getMethod("getGlobalRegionScheduler")
                        .invoke(null);

                Class<?> globalClass = Class.forName(
                        "io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler");
                Class<?> asyncClass = Class.forName(
                        "io.papermc.paper.threadedregions.scheduler.AsyncScheduler");
                Class<?> taskClass = Class.forName(
                        "io.papermc.paper.threadedregions.scheduler.ScheduledTask");

                getGlobalScheduler = Bukkit.class.getMethod("getGlobalRegionScheduler");
                getAsyncScheduler = Bukkit.class.getMethod("getAsyncScheduler");

                asyncRunNow = asyncClass.getMethod("runNow", Plugin.class, Consumer.class);
                asyncRunLater = asyncClass.getMethod("runDelayed", Plugin.class, Consumer.class, long.class, TimeUnit.class);
                asyncRunTimer = asyncClass.getMethod("runAtFixedRate", Plugin.class, Consumer.class, long.class, long.class, TimeUnit.class);
                globalRun = globalClass.getMethod("run", Plugin.class, Consumer.class);
                globalRunLater = globalClass.getMethod("runDelayed", Plugin.class, Consumer.class, long.class);
                globalRunTimer = globalClass.getMethod("runAtFixedRate", Plugin.class, Consumer.class, long.class, long.class);

            } catch (ReflectiveOperationException e) {
                throw new RuntimeException("Failed to initialize Folia scheduler", e);
            }
        }
    }

    private static Object global() throws ReflectiveOperationException {
        return getGlobalScheduler.invoke(null);
    }

    private static Object async() throws ReflectiveOperationException {
        return getAsyncScheduler.invoke(null);
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
                long safeDelay = Math.max(1L, delay);
                long safePeriod = Math.max(1L, period);
                Consumer<Object> consumer = t -> task.run();

                Object scheduledTask;
                if (async) {
                    scheduledTask = asyncRunTimer.invoke(async(), plugin, consumer,
                            getMs(delay), getMs(period), TimeUnit.MILLISECONDS);
                } else {
                    scheduledTask = globalRunTimer.invoke(global(), plugin, consumer,
                            safeDelay, safePeriod);
                }

                ACTIVE_TASKS.add(scheduledTask);

            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
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

    private static long getMs(long ticks) {
        return ticks*50;
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
                long safeDelay = Math.max(1L, delay);
                Consumer<Object> consumer = t -> task.run();

                if (async) {
                    asyncRunLater.invoke(async(), plugin, consumer, getMs(delay), TimeUnit.MILLISECONDS);
                } else {
                    globalRunLater.invoke(global(), plugin, consumer, safeDelay);
                }
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
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

    public static void runTask(Plugin plugin, Runnable task) {
        if (FOLIA) {
            try {
                Object scheduler = Bukkit.class
                        .getMethod("getGlobalRegionScheduler")
                        .invoke(null);

                globalRun.invoke(
                        global(),
                        plugin,
                        (Consumer<?>) t -> task.run());

            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    public static void runTaskAsync(Plugin plugin, Runnable task) {
        if (FOLIA) {
            try {
                Consumer<Object> consumer = t -> task.run();
                asyncRunNow.invoke(async(), plugin, consumer);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
        }
    }
}
