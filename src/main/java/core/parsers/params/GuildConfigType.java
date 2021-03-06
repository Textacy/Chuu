package core.parsers.params;

import dao.ChuuService;
import dao.entities.*;
import dao.exceptions.InstanceNotFoundException;
import org.apache.commons.text.WordUtils;

import java.util.EnumSet;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum GuildConfigType {
    CHART_MODE("chart"), CROWNS_THRESHOLD("crowns"),
    ALLOW_NP_REACTIONS("np-reactions"),
    OVERRIDE_NP_REACTIONS("override-reactions"),
    DELETE_MESSAGE("delete-message"), NP("np"), REMAINING_MODE("rest"), SHOW_DISABLED_WARNING("disabled-warning"),
    COLOR("color"),
    WHOKNOWS_MODE("whoknows");
    static final Pattern npMode = Pattern.compile("((" + "CLEAR|" +
            EnumSet.allOf(NPMode.class).stream().filter(x -> !x.equals(NPMode.UNKNOWN)).map(NPMode::toString).collect(Collectors.joining("|")) +
            ")[ |&,]*)+", Pattern.CASE_INSENSITIVE);
    static final Pattern overrideMode = Pattern.compile("(override|add|add[-_ ]end|empty)", Pattern.CASE_INSENSITIVE);
    static final Pattern colorMode = Pattern.compile("(random|role|.+)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private static final Map<String, GuildConfigType> ENUM_MAP;
    private static final Pattern number = Pattern.compile("\\d+");

    static {
        ENUM_MAP = Stream.of(GuildConfigType.values())
                .collect(Collectors.toMap(GuildConfigType::getCommandName, Function.identity()));
    }

    private final String commandName;

    GuildConfigType(String command) {
        commandName = command;
    }

    public static GuildConfigType get(String name) {
        return ENUM_MAP.get(name);
    }

    public static String list(ChuuService dao, long guildId) {
        GuildProperties guildProperties;
        try {
            guildProperties = dao.getGuildProperties(guildId);
        } catch (InstanceNotFoundException e) {
            return "This Server has no registered users?!";
        }
        return ENUM_MAP.entrySet().stream().map(
                x -> {
                    String key = x.getKey();
                    return switch (x.getValue()) {
                        case CROWNS_THRESHOLD -> String.format("**%s** -> %d", key, guildProperties.crown_threshold());
                        case CHART_MODE -> {
                            ChartMode chartMode = guildProperties.chartMode();
                            String mode;
                            if (chartMode == null) {
                                mode = "NOT SET";
                            } else {
                                mode = chartMode.toString();
                            }
                            yield String.format("**%s** -> %s", key, mode);
                        }
                        case COLOR -> String.format("**%s** -> %s", key, guildProperties.embedColor().toDisplayString());
                        case WHOKNOWS_MODE -> {
                            String whoknowsmode;
                            WhoKnowsMode modes = guildProperties.whoKnowsMode();

                            if (modes == null) {
                                whoknowsmode = "NOT SET";
                            } else {
                                whoknowsmode = modes.toString();
                            }
                            yield String.format("**%s** -> %s", key, whoknowsmode);
                        }
                        case REMAINING_MODE -> {
                            String whoknowsmode;
                            RemainingImagesMode modes2 = guildProperties.remainingImagesMode();

                            if (modes2 == null) {
                                whoknowsmode = "NOT SET";
                            } else {
                                whoknowsmode = modes2.toString();
                            }
                            yield String.format("**%s** -> %s", key, whoknowsmode);
                        }
                        case ALLOW_NP_REACTIONS -> String.format("**%s** -> %s", key, guildProperties.allowReactions());
                        case OVERRIDE_NP_REACTIONS -> String.format("**%s** -> %s", key, guildProperties.overrideReactions().toString());
                        case DELETE_MESSAGE -> String.format("**%s** -> %s", key, guildProperties.deleteMessages());
                        case SHOW_DISABLED_WARNING -> String.format("**%s** -> %s", key, guildProperties.showWarnings());
                        case NP -> {
                            EnumSet<NPMode> npModes = dao.getServerNPModes(guildId);
                            String strModes;
                            if (npModes.contains(NPMode.UNKNOWN)) {
                                strModes = "NOT SET";
                            } else {
                                strModes = NPMode.getListedName(npModes);
                            }
                            yield String.format("**%s** -> %s", key, strModes);
                        }
                    };
                }).collect(Collectors.joining("\n"));

    }

    public String getCommandName() {
        return commandName;
    }

    public Predicate<String> getParser() {
        return switch (this) {
            case CROWNS_THRESHOLD -> number.asMatchPredicate();
            case CHART_MODE -> UserConfigType.chartMode.asMatchPredicate();
            case COLOR -> colorMode.asMatchPredicate();
            case REMAINING_MODE, WHOKNOWS_MODE -> UserConfigType.whoknowsMode.asMatchPredicate();
            case ALLOW_NP_REACTIONS, DELETE_MESSAGE, SHOW_DISABLED_WARNING -> UserConfigType.bool.asMatchPredicate();
            case OVERRIDE_NP_REACTIONS -> overrideMode.asMatchPredicate();
            case NP -> GuildConfigType.npMode.asMatchPredicate();
        };
    }

    public String getExplanation() {
        switch (this) {
            case CROWNS_THRESHOLD:
                return "A positive number that represent the minimum number of scrobbles for a crown to count";
            case CHART_MODE:
                String explanation = EnumSet.allOf(ChartMode.class).stream().map(x -> "\n\t\t\t**" + WordUtils.capitalizeFully(x.toString()) + "**: " + x.getDescription()).collect(Collectors.joining(""));
                explanation += "\n\t\t\t**Clear**: Sets the default mode ";
                return "Set the mode for all charts of all users in this server. While this is set any user configuration will be overridden.\n" +
                        "\t\tThe possible values for the chart mode are the following:" + explanation;
            case COLOR:
                explanation = EnumSet.allOf(EmbedColor.EmbedColorType.class).stream().map(x -> "\n\t\t\t**" + WordUtils.capitalizeFully(x.toString()) + "**: " + x.getDescription()).collect(Collectors.joining(""));
                return "Set the color for all embed of all users in this server. While this is set any user configuration will be overridden.\n" +
                        "\t\tThe possible values for the embed colour are the following:" + explanation;
            case WHOKNOWS_MODE:
                explanation = EnumSet.allOf(WhoKnowsMode.class).stream().map(x -> "\n\t\t\t**" + WordUtils.capitalizeFully(x.toString()) + "**: " + x.getDescription()).collect(Collectors.joining(""));
                explanation += "\n\t\t\t**Clear**: Sets the default mode";
                return "Set the mode for all who knows of all users in this server. While this is set any user configuration will be overridden.\n" +
                        "\t\tThe possible values for the who knows mode are the following:" + explanation;
            case REMAINING_MODE:
                explanation = EnumSet.allOf(RemainingImagesMode.class).stream().map(x -> "\n\t\t\t**" + WordUtils.capitalizeFully(x.toString()) + "**: " + x.getDescription()).collect(Collectors.joining(""));
                explanation += "\n\t\t\t**Clear**: Sets the default mode";
                return "Set the mode for all charts of the remaining images of the users in this server. While this is set any user configuration will be overridden \n" +
                        "\t\tThe possible values for the rest of the commands are the following:" + explanation;
            case ALLOW_NP_REACTIONS:
                return "Whether you want the bot to add reactions to nps in this server.";
            case OVERRIDE_NP_REACTIONS:
                explanation = EnumSet.allOf(OverrideMode.class).stream().map(x -> "\n\t\t\t**" + WordUtils.capitalizeFully(x.toString().replaceAll("_", "-"), '-', ' ') + "**: " + x.getDescription()).collect(Collectors.joining(""));
                return "Whether you want the server reactions to override the users reactions, add to the user added or only use them when the user doesnt have any.\n"
                        + "\t\tThe possible values for the chart mode are the following:" + explanation;
            case DELETE_MESSAGE:
                return "Whether you want the bot to delete the original message the user wrote.";
            case SHOW_DISABLED_WARNING:
                return "Whether you want the bot to show a warning when you try to run a disabled command.";
            case NP:
                return "Setting this will alter the appearance of this server np commands. You can select up to 10 different from the following list and mix them up:\n" + "CLEAR | " + NPMode.getListedName(EnumSet.allOf(NPMode.class));
            default:
                return "";
        }
    }
}
