package core.services;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import core.Chuu;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class HeavyCommandRateLimiter {

    private final static long MAX_SERVER = 4L;
    private final static long MAX_GLOBAL = 12L;
    private final static Map<Long, LocalDateTime> accesibleAgain = new HashMap<>();
    private static LocalDateTime globalAccesibleAgain = LocalDateTime.now();
    private static final LoadingCache<Long, AtomicInteger> serverCache = CacheBuilder.newBuilder()
            .maximumSize(10000)

            .expireAfterWrite(10, TimeUnit.MINUTES)
            .removalListener(notification -> {
                Object key = notification.getKey();
                if (key instanceof Long k)
                    accesibleAgain.remove(k);
            })
            .build(
                    new CacheLoader<>() {
                        public AtomicInteger load(@NotNull Long key) {
                            accesibleAgain.put(key, LocalDateTime.now().plus(10, ChronoUnit.MINUTES));
                            return new AtomicInteger();
                        }
                    });
    private static final LoadingCache<Boolean, AtomicInteger> globalCache = CacheBuilder.newBuilder()
            .maximumSize(10000)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .removalListener(notification -> {
                Object key = notification.getKey();
                if (key instanceof Long k)
                    globalAccesibleAgain = null;
            })
            .build(
                    new CacheLoader<>() {
                        public AtomicInteger load(@NotNull Boolean key) {
                            globalAccesibleAgain = LocalDateTime.now().plus(10, ChronoUnit.MINUTES);
                            return new AtomicInteger();
                        }
                    });

    public static RateLimited checkRateLimit(MessageReceivedEvent e) {
        try {

            if (e.isFromGuild()) {
                AtomicInteger longAdder = serverCache.get(e.getGuild().getIdLong());
                if (longAdder.incrementAndGet() >= MAX_SERVER) {
                    return RateLimited.SERVER;
                }
            }

            AtomicInteger globalAdder = globalCache.get(true);
            if (globalAdder.incrementAndGet() >= MAX_GLOBAL) {
                return RateLimited.GLOBAL;
            }

            return RateLimited.NONE;
        } catch (Exception exception) {
            // Impossible Exception
            Chuu.getLogger().warn(exception.getMessage(), exception);
            return RateLimited.NONE;
        }
    }

    public enum RateLimited {

        SERVER, GLOBAL, NONE;

        private static String formate(LocalDateTime time) {
            LocalDateTime now = LocalDateTime.now();
            long hours = now.until(time, ChronoUnit.MINUTES);
            now = now.plus(hours, ChronoUnit.MINUTES);
            long minutes = now.until(time, ChronoUnit.SECONDS);
            return "%d minutes and %d seconds".formatted(hours, minutes);
        }

        public String remainingTime(MessageReceivedEvent e) {
            return switch (this) {
                case SERVER -> {
                    LocalDateTime localDateTime = accesibleAgain.get(e.getGuild().getIdLong());
                    yield formate(localDateTime);
                }
                case GLOBAL -> formate(globalAccesibleAgain);
                case NONE -> null;
            };
        }
    }
}
