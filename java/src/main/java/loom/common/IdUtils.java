package loom.common;

import javax.annotation.Nullable;
import java.util.UUID;

public class IdUtils {
    public static UUID coerceUUID(@Nullable UUID id) {
        if (id == null) {
            return UUID.randomUUID();
        }
        return id;
    }

    public static UUID coerceUUID(String string) {
        if (string == null || string.isEmpty()) {
            return UUID.randomUUID();
        }
        return UUID.fromString(string);
    }
}
