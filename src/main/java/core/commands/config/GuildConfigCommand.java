package core.commands.config;

import com.google.common.collect.Sets;
import core.Chuu;
import core.commands.abstracts.ConcurrentCommand;
import core.commands.utils.CommandCategory;
import core.commands.utils.CommandUtil;
import core.exceptions.LastFmException;
import core.parsers.GuildConfigParser;
import core.parsers.Parser;
import core.parsers.params.GuildConfigParams;
import core.parsers.params.GuildConfigType;
import core.services.ColorService;
import dao.ChuuService;
import dao.entities.*;
import dao.exceptions.InstanceNotFoundException;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.apache.commons.text.WordUtils;

import javax.validation.constraints.NotNull;
import java.util.EnumSet;
import java.util.List;

public class GuildConfigCommand extends ConcurrentCommand<GuildConfigParams> {
    public GuildConfigCommand(ChuuService dao) {
        super(dao);
        this.respondInPrivate = false;
    }

    @Override
    protected CommandCategory initCategory() {
        return CommandCategory.CONFIGURATION;
    }

    @Override
    public Parser<GuildConfigParams> initParser() {
        return new GuildConfigParser(db);
    }

    @Override
    public String getDescription() {
        return "Configuration per server";
    }

    @Override
    public List<String> getAliases() {
        return List.of("serverconfiguration", "serverconfig", "sconfig");
    }

    @Override
    public String getName() {
        return "Server Configuration";
    }

    @Override
    protected void onCommand(MessageReceivedEvent e, @NotNull GuildConfigParams params) throws LastFmException, InstanceNotFoundException {
        GuildConfigParams parse = this.parser.parse(e);

        GuildConfigType config = parse.getConfig();
        String value = parse.getValue();
        boolean cleansing = value.equalsIgnoreCase("clear");
        long guildId = e.getGuild().getIdLong();
        switch (config) {
            case CROWNS_THRESHOLD:
                int threshold = Integer.parseInt(value);
                db.updateGuildCrownThreshold(guildId, threshold);
                sendMessageQueue(e, "Successfully updated the crown threshold to " + threshold);
                break;
            case CHART_MODE:
                ChartMode chartMode;
                if (cleansing) {
                    chartMode = null;
                } else {
                    chartMode = ChartMode.valueOf(value.replace("-", "_").toUpperCase());
                }
                db.setServerChartMode(guildId, chartMode);
                if (cleansing) {
                    sendMessageQueue(e, "Now all charts are back to the default");
                } else {
                    sendMessageQueue(e, "Server chart mode set to: **" + WordUtils.capitalizeFully(chartMode.toString()) + "**");
                }
                break;
            case COLOR:
                EmbedColor embedColor = EmbedColor.fromString(value);
                if (embedColor.type() == EmbedColor.EmbedColorType.COLOURS && embedColor.colorList().isEmpty()) {
                    sendMessageQueue(e, "Couldn't read any colour :(\nTry with different values.");
                    return;
                }
                if (!embedColor.isValid()) {
                    parser.sendError("Too many colours were introduced. Pls reduce your input a bit", e);
                    return;
                }
                db.setServerColorMode(guildId, embedColor);
                ColorService.handleServerChange(guildId, embedColor);
                sendMessageQueue(e, "Guild color mode set to: **" + WordUtils.capitalizeFully(embedColor.toDisplayString()) + "**");

                break;
            case WHOKNOWS_MODE:
                WhoKnowsMode whoKnowsMode;
                if (cleansing) {
                    whoKnowsMode = null;
                } else {
                    whoKnowsMode = WhoKnowsMode.valueOf(value.replace("-", "_").toUpperCase());
                }
                db.setServerWhoknowMode(guildId, whoKnowsMode);
                if (cleansing) {
                    sendMessageQueue(e, "Now your who knows are back to the default");
                } else {
                    sendMessageQueue(e, "Who Knows mode set to: **" + WordUtils.capitalizeFully(whoKnowsMode.toString()) + "**");
                }
                break;
            case REMAINING_MODE:
                RemainingImagesMode remainingImagesMode;
                if (cleansing) {
                    remainingImagesMode = null;
                } else {
                    remainingImagesMode = RemainingImagesMode.valueOf(value.replace("-", "_").toUpperCase());
                }
                db.setRemainingImagesModeServer(guildId, remainingImagesMode);
                if (!cleansing) {
                    sendMessageQueue(e, "The mode of the remaining image commands was set to: **" + WordUtils.capitalizeFully(remainingImagesMode.toString()) + "**");
                } else {
                    sendMessageQueue(e, "The mode of the remaining image commands to the default");
                }
                break;
            case ALLOW_NP_REACTIONS:
                boolean b = Boolean.parseBoolean(value);
                db.setServerAllowReactions(guildId, b);
                if (b) {
                    sendMessageQueue(e, "Np reactions are now allowed");
                } else {
                    sendMessageQueue(e, "Np reactions are not allowed anymore");
                }
                break;
            case OVERRIDE_NP_REACTIONS:
                OverrideMode overrideMode = OverrideMode.valueOf(value.trim().replaceAll("\s+|-", "_").toUpperCase());
                db.setServerOverrideReactions(guildId, overrideMode);
                sendMessageQueue(e, "Set the override mode to: " + WordUtils.capitalizeFully(overrideMode.toString().replaceAll("_", " ")));
                break;
            case DELETE_MESSAGE:
                b = Boolean.parseBoolean(value);
                if (b) {
                    if (!e.getGuild().getSelfMember().hasPermission(Permission.MESSAGE_MANAGE)) {
                        sendMessageQueue(e, "Don't have MESSAGE_MANAGE permissions so can't delete messages :(");
                    }
                }
                db.setServerDeleteMessage(guildId, b);
                if (b) {
                    Chuu.getMessageDeletionService().addServerToDelete(guildId);
                    sendMessageQueue(e, "The commands will be deleted by the bot.");

                } else {
                    Chuu.getMessageDeletionService().removeServerToDelete(guildId);
                    sendMessageQueue(e, "The commands won't be deleted by the bot.");
                }
                break;
            case SHOW_DISABLED_WARNING:
                b = Boolean.parseBoolean(value);
                db.setServerShowDisabledWarning(guildId, b);
                Chuu.getMessageDisablingService().setDontRespondOnError(b, guildId);
                if (b) {
                    sendMessageQueue(e, "You will be notified when you run a disabled command");

                } else {
                    sendMessageQueue(e, "The bot won't say anything when you run a disabled command");
                }
                break;
            case NP:
                String[] split = value.trim().replaceAll("\s+", " ").split("[|,& ]+");
                EnumSet<NPMode> modes = EnumSet.noneOf(NPMode.class);
                for (String mode : split) {
                    if (mode.equalsIgnoreCase("CLEAR")) {
                        modes = EnumSet.of(NPMode.UNKNOWN);
                        break;
                    }
                    NPMode npMode = NPMode.valueOf(mode.replace("-", "_").toUpperCase());
                    modes.add(npMode);
                }
                if (modes.size() > 15) {
                    sendMessageQueue(e, "You can't set more than 15 as a default for the server");
                } else {
                    db.setServerNPModes(guildId, modes);
                    String strModes = NPMode.getListedName(modes);
                    if (Sets.difference(modes, EnumSet.of(NPMode.UNKNOWN)).isEmpty()) {
                        sendMessageQueue(e, "Successfully cleared the server config");
                    } else {
                        sendMessageQueue(e, String.format("Successfully changed the server config to the following %s: %s", CommandUtil.singlePlural(modes.size(), "mode", "modes"), strModes));
                    }
                }
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + config);
        }
    }
}
