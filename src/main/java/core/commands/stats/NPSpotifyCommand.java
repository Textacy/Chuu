package core.commands.stats;

import core.apis.spotify.Spotify;
import core.apis.spotify.SpotifySingleton;
import core.commands.abstracts.NpCommand;
import core.commands.utils.CommandUtil;
import core.parsers.params.NowPlayingParameters;
import dao.ChuuService;
import dao.entities.LastFMData;
import dao.entities.NowPlayingArtist;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.Arrays;
import java.util.List;

public class NPSpotifyCommand extends NpCommand {
    private final Spotify spotify;

    public NPSpotifyCommand(ChuuService dao) {
        super(dao);
        this.spotify = SpotifySingleton.getInstance();


    }

    @Override
    public void doSomethingWithArtist(NowPlayingArtist nowPlayingArtist, MessageReceivedEvent e, long discordId, LastFMData user, NowPlayingParameters parameters) {
        MessageBuilder messageBuilder = new MessageBuilder();
        String uri = spotify
                .searchItems(nowPlayingArtist.getSongName(), nowPlayingArtist.getArtistName(), nowPlayingArtist
                        .getAlbumName());

        if (uri.isBlank()) {
            sendMessageQueue(e, String.format("Was not able to find %s - %s on spotify", CommandUtil.cleanMarkdownCharacter(nowPlayingArtist.getArtistName()), CommandUtil.cleanMarkdownCharacter(nowPlayingArtist
                    .getSongName())));
            return;
        }
        e.getChannel().sendMessage(messageBuilder.setContent(uri).build()).queue();
    }

    @Override
    public String getDescription() {
        return "Returns a link to your current song via Spotify";
    }

    @Override
    public List<String> getAliases() {
        return Arrays.asList("npspotify", "spotify", "nps", "npspo");
    }

    @Override
    public String getName() {
        return "Now Playing Spotify";
    }


}
