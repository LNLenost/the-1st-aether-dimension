package net.minecraft.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.Locale;

public record ColorRGBA(int rgba) {
    private static final String CUSTOM_COLOR_PREFIX = "#";
    public static final Codec<ColorRGBA> CODEC = Codec.STRING.comapFlatMap(p_304880_ -> {
        if (!p_304880_.startsWith("#")) {
            return DataResult.error(() -> "Not a color code: " + p_304880_);
        } else {
            try {
                int i = (int)Long.parseLong(p_304880_.substring(1), 16);
                return DataResult.success(new ColorRGBA(i));
            } catch (NumberFormatException numberformatexception) {
                return DataResult.error(() -> "Exception parsing color code: " + numberformatexception.getMessage());
            }
        }
    }, ColorRGBA::formatValue);

    private String formatValue() {
        return String.format(Locale.ROOT, "#%08X", this.rgba);
    }

    @Override
    public String toString() {
        return this.formatValue();
    }
}
