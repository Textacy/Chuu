package core.commands.music;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import core.Chuu;
import core.commands.abstracts.ConcurrentCommand;
import core.commands.utils.CommandCategory;
import core.commands.utils.CommandUtil;
import core.exceptions.LastFmException;
import core.music.MusicManager;
import core.music.utils.TrackContext;
import core.otherlisteners.Reactionary;
import core.parsers.NoOpParser;
import core.parsers.Parser;
import core.parsers.params.CommandParameters;
import dao.ChuuService;
import dao.exceptions.InstanceNotFoundException;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;

public class QueueCommand extends ConcurrentCommand<CommandParameters> {
    public QueueCommand(ChuuService dao) {
        super(dao);
    }

    @Override
    protected CommandCategory initCategory() {
        return CommandCategory.RYM_BETA;
    }

    @Override
    public Parser<CommandParameters> initParser() {
        return new NoOpParser();
    }

    @Override
    public String getDescription() {
        return "Queueu";
    }

    @Override
    public List<String> getAliases() {
        return List.of("q", "queue");
    }

    @Override
    public String getName() {
        return "Queue";
    }

    @Override
    protected void onCommand(MessageReceivedEvent e, @NotNull CommandParameters params) throws LastFmException, InstanceNotFoundException {
        MusicManager manager = Chuu.playerRegistry.getExisting(e.getGuild().getIdLong());
        if (manager == null) {
            sendMessageQueue(e, "There's no music manager in this server");
            return;
        }
        Queue<String> queue = manager.getQueue();
        long length = 0L;
        EmbedBuilder embedBuilder = new EmbedBuilder();
        StringBuilder stringBuilder = new StringBuilder();
        List<String> str = new ArrayList<>();
        for (String s : queue) {
            AudioTrack decodedTrack = Chuu.playerManager.decodeAudioTrack(s);
            StringBuilder a = new StringBuilder();
            decodedTrack.getUserData(TrackContext.class);
            a.append("`[").append(CommandUtil.getTimestamp(decodedTrack.getDuration()))
                    .append("]` __[")
                    .append(decodedTrack.getInfo().title)
                    .append("](").append(decodedTrack.getInfo().uri).append(")__\n");
            str.add(a.toString());
            length += decodedTrack.getDuration();
        }
        AudioTrack np = manager.getPlayer().getPlayingTrack();
        embedBuilder.addField("Now Playing", np == null ? "Nothing" : String.format("**[%s](%s)**", np.getInfo().title, np.getInfo().uri), false);

        if (manager.getRadio() != null) {
            String b = "Currently streaming music from radio station " + manager.getRadio().getSource().getName() +
                    ", requested by " + manager.getRadio().requester() +
                    ". When the queue is empty, random tracks from the station will be added.";
            embedBuilder.addField("Radio", b, false);
        }
        embedBuilder
                .addField("Entries", String.valueOf(queue.size()), true)
                .addField("Total Duration", CommandUtil.getTimestamp(length), true)
                .addField("Repeating", manager.getRepeatOption().name(), true);
        MessageChannel channel = e.getChannel();
//        DiscordUserDisplay uInfo = CommandUtil.getUserInfoNotStripped(e, e.getAuthor().getIdLong());
        embedBuilder
                .setDescription(str.stream().limit(10).collect(Collectors.joining()))
                .setAuthor("Music Queue")
                .setColor(CommandUtil.randomColor());
        channel.sendMessage(new MessageBuilder().setEmbed(embedBuilder.build()).build()).queue(m -> {
            new Reactionary<>(str, m, 10, embedBuilder, false, true);
        });
    }
}