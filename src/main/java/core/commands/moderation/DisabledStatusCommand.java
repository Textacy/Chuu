package core.commands.moderation;

import core.Chuu;
import core.commands.abstracts.ConcurrentCommand;
import core.commands.abstracts.MyCommand;
import core.commands.utils.CommandCategory;
import core.commands.utils.CommandUtil;
import core.parsers.NoOpParser;
import core.parsers.Parser;
import core.parsers.params.CommandParameters;
import core.services.ColorService;
import core.services.MessageDisablingService;
import dao.ChuuService;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.lang3.tuple.Pair;

import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DisabledStatusCommand extends ConcurrentCommand<CommandParameters> {
    public DisabledStatusCommand(ChuuService dao) {
        super(dao);
        respondInPrivate = false;
    }

    @Override
    protected CommandCategory initCategory() {
        return CommandCategory.CONFIGURATION;
    }

    @Override
    public Parser<CommandParameters> initParser() {
        return new NoOpParser();
    }

    @Override
    public String getDescription() {
        return "A list of all the disabled commands in the server";
    }

    @Override
    public List<String> getAliases() {
        return List.of("disabled", "commandstatus", "enabled", "toggled");
    }

    @Override
    public String getName() {
        return "Command Statuses";
    }

    @Override
    protected void onCommand(MessageReceivedEvent e, @NotNull CommandParameters params) {
        MessageDisablingService messageDisablingService = Chuu.getMessageDisablingService();
        MultiValuedMap<Pair<Long, Long>, MyCommand<?>> disabledChannelsMap = messageDisablingService.disabledChannelsMap;
        MultiValuedMap<Pair<Long, Long>, MyCommand<?>> enabledChannelsMap = messageDisablingService.enabledChannelsMap;
        MultiValuedMap<Long, MyCommand<?>> disabledServersMap = messageDisablingService.disabledServersMap;
        Map<Long, GuildChannel> channelMap = e.getGuild().getChannels().stream().collect(Collectors.toMap(ISnowflake::getIdLong, x ->
                x));
        List<? extends MyCommand<?>>
                disabledServerCommands = disabledServersMap.entries().stream().filter(x -> x.getKey().equals(e.getGuild().getIdLong())).map(Map.Entry::getValue).collect(Collectors.toList());
        Map<GuildChannel, List<MyCommand<?>>> channelSpecificDisables = disabledChannelsMap.entries().stream()
                .filter(x -> x.getKey().getLeft().equals(e.getGuild().getIdLong()))
                .filter(x -> !disabledServerCommands.contains(x.getValue()))
                .collect(Collectors.groupingBy(x -> channelMap.get(x.getKey().getRight()),
                        Collectors.mapping(Map.Entry::getValue, Collectors.toList())));

        Map<GuildChannel, List<MyCommand<?>>> channelSpecificEnabled = enabledChannelsMap.entries().stream()
                .filter(x -> x.getKey().getLeft().equals(e.getGuild().getIdLong()))
                .filter(x -> disabledServerCommands.contains(x.getValue()))
                .collect(Collectors.groupingBy(x -> channelMap.get(x.getKey().getRight()),
                        Collectors.mapping(Map.Entry::getValue, Collectors.toList())));
        EmbedBuilder embedBuilder = new EmbedBuilder().setColor(ColorService.computeColor(e))
                .setThumbnail(e.getGuild().getIconUrl());
        StringBuilder a = new StringBuilder();

        Map<GuildChannel, StringBuilder> fieldBuilder = new HashMap<>();
        String globalDisabled = disabledServerCommands.stream().map(x -> x.getAliases().get(0)).collect(Collectors.joining(", "));
        if (!globalDisabled.isBlank()) {
            if (globalDisabled.length() > 1024) {
                String substring = globalDisabled.substring(0, 1024);
                int i = substring.lastIndexOf(",");
                embedBuilder.addField("**Commands disabled in the whole server**", substring.substring(0, i), false)
                        .addField(EmbedBuilder.ZERO_WIDTH_SPACE, globalDisabled.substring(i), false);
            } else {
                embedBuilder.addField("**...**", globalDisabled, false);
            }
        }
        for (Map.Entry<GuildChannel, List<MyCommand<?>>> guildChannelListEntry : channelSpecificDisables.entrySet()) {
            GuildChannel key = guildChannelListEntry.getKey();
            StringBuilder stringBuilder = fieldBuilder.get(key);
            if (stringBuilder == null) {
                stringBuilder = new StringBuilder();
                fieldBuilder.put(key, stringBuilder);
            }
            stringBuilder.append("Disabled:\t")
                    .append(guildChannelListEntry.getValue().stream().map(x -> x.getAliases().get(0)).collect(Collectors.joining(", ")))
                    .append("\n");

        }
        for (Map.Entry<GuildChannel, List<MyCommand<?>>> guildChannelListEntry : channelSpecificEnabled.entrySet()) {
            GuildChannel key = guildChannelListEntry.getKey();
            StringBuilder stringBuilder = fieldBuilder.get(key);
            if (stringBuilder == null) {
                stringBuilder = new StringBuilder();
                fieldBuilder.put(key, stringBuilder);
            }
            stringBuilder.append("Enabled:\t")
                    .append(guildChannelListEntry.getValue().stream().map(x -> x.getAliases().get(0)).collect(Collectors.joining(", ")))
                    .append("\n");
        }
        fieldBuilder.forEach((x, y) -> {
            String value = y.toString();
            if (value.length() > 1024) {
                String substring = value.substring(0, 1024);
                int i = substring.lastIndexOf(",");
                embedBuilder.addField("**Channel " + x.getName() + "**", value.substring(0, i), false)
                        .addField(EmbedBuilder.ZERO_WIDTH_SPACE, value.substring(i), false);
            } else {
                embedBuilder.addField("**Channel " + x.getName() + "**", value, false);
            }
        });

        if (embedBuilder.getFields().isEmpty()) {
            sendMessageQueue(e, " This server has not disabled any command");
            return;
        }

        embedBuilder.setDescription(a).setTitle(CommandUtil.cleanMarkdownCharacter(e.getGuild().getName()) + "'s commands status")
                .setThumbnail(e.getGuild().getIconUrl());
        e.getChannel().sendMessage(new MessageBuilder().setEmbed(embedBuilder.build()).build()).queue();
    }
}
