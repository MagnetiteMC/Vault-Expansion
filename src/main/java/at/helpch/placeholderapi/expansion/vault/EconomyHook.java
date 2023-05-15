package at.helpch.placeholderapi.expansion.vault;

import com.google.common.primitives.Ints;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EconomyHook extends VaultHook {

    private static final Pattern BALANCE_DECIMAL_POINTS_PATTERN = Pattern.compile("balance_(?<points>\\d+)dp");
    private static final DecimalFormat COMMAS_FORMAT = new DecimalFormat("#,###");
    private static final DecimalFormat FIXED_FORMAT = new DecimalFormat("#");
    private static final Map<Integer, DecimalFormat> DECIMAL_FORMATS_CACHE = new HashMap<>();

    private final NavigableMap<BigInteger, String> suffixes = new TreeMap<>();
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#.#");
    private static final String[] SUFFIXES = new String[]{"", "K", "M", "B", "T", "q", "Q", "s", "S", "O"};
    private Economy economy;

    public EconomyHook(VaultExpansion expansion) {
        super(expansion);
        setup();
    }

    private double getBalance(@NotNull final OfflinePlayer player) {
        return economy.getBalance(player);
    }

    private @NotNull String setDecimalPoints(double balance, int points) {
        final DecimalFormat cachedFormat = DECIMAL_FORMATS_CACHE.get(points);

        if (cachedFormat != null) {
            return cachedFormat.format(balance);
        }

        final DecimalFormat decimalFormat = (DecimalFormat) DecimalFormat.getIntegerInstance();
        decimalFormat.setMaximumFractionDigits(points);
        decimalFormat.setGroupingUsed(false);
        DECIMAL_FORMATS_CACHE.put(points, decimalFormat);
        return decimalFormat.format(balance);
    }

    /**
     * Format player's balance, 1200 -> 1.2K
     *
     * @param balance balance to format
     * @return balance formatted
     * @author <a href="https://stackoverflow.com/users/829571/assylias">assylias</a> (<a href="https://stackoverflow.com/a/30661479/11496439">source</a>)
     */
    private @NotNull String formatBalance(double balance) {
        // Find the index of the suffix based on the number of digits
        int index = (int) Math.log10(balance) / 3;
        // Check if the index is valid
        if (index < 0 || index >= SUFFIXES.length) {
            // Return the original number as a string
            return String.valueOf(balance);
        }
        // Divide the number by the appropriate power of 1000
        double value = balance / Math.pow(1000, index);
        // Format the value and append the suffix
        return DECIMAL_FORMAT.format(value) + SUFFIXES[index];

    }

    @Override
    public void setup() {
        economy = getService(Economy.class);
    }

    @Override
    public boolean isReady() {
        return economy != null;
    }

    @SuppressWarnings("UnstableApiUsage")
    @Override
    public @Nullable String onRequest(@Nullable OfflinePlayer offlinePlayer, @NotNull String params) {
        if (offlinePlayer == null) {
            return "";
        }

        final double balance = getBalance(offlinePlayer);

        if (params.startsWith("balance_")) {
            final Matcher matcher = BALANCE_DECIMAL_POINTS_PATTERN.matcher(params);

            if (matcher.find()) {
                final Integer points = Ints.tryParse(matcher.group("points"));

                if (points == null) {
                    return matcher.group("points") + " is not a valid number";
                }

                return setDecimalPoints(balance, points);
            }
        }

        switch (params) {
            case "balance":
                return setDecimalPoints(balance, Math.max(2, economy.fractionalDigits()));
            case "balance_fixed":
                return FIXED_FORMAT.format(balance);
            case "balance_formatted":
                return formatBalance(balance);
            case "balance_commas":
                return COMMAS_FORMAT.format(balance);
            default:
                return null;
        }
    }

}
