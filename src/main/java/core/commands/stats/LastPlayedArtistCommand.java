package core.commands.stats;

import core.commands.abstracts.ConcurrentCommand;
import core.commands.utils.CommandCategory;
import core.commands.utils.CommandUtil;
import core.exceptions.LastFmException;
import core.parsers.ArtistParser;
import core.parsers.OptionalEntity;
import core.parsers.Parser;
import core.parsers.params.ArtistParameters;
import dao.ChuuService;
import dao.entities.LastFMData;
import dao.entities.ScrobbledArtist;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import javax.validation.constraints.NotNull;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public class LastPlayedArtistCommand extends ConcurrentCommand<ArtistParameters> {
    public LastPlayedArtistCommand(ChuuService dao) {
        super(dao);
    }

    @Override
    protected CommandCategory initCategory() {
        return CommandCategory.USER_STATS;
    }

    @Override
    public Parser<ArtistParameters> initParser() {
        ArtistParser artistParser = new ArtistParser(db, lastFM);
        artistParser.addOptional(new OptionalEntity("today", "to not include the current day"));
        return artistParser;
    }

    @Override
    public String getDescription() {
        return "Last time you scrobbled an artist";
    }

    @Override
    public List<String> getAliases() {
        return List.of("last", "lastscrobbled");
    }

    @Override
    public String getName() {
        return "Last scrobble of an artist";
    }

    @Override
    protected void onCommand(MessageReceivedEvent e, @NotNull ArtistParameters params) throws LastFmException {
        ScrobbledArtist artist = CommandUtil.onlyCorrection(db, params.getArtist(), lastFM, !params.isNoredirect());
        long whom = params.getLastFMData().getDiscordId();
        int a;
        LastFMData data = params.getLastFMData();
        Optional<Instant> instant = db.getLastScrobbledArtist(artist.getArtistId(), params.getLastFMData().getName(), false);
        if (instant.isEmpty()) {
            sendMessageQueue(e, "Couldn't get the last time you scrobbled **" + artist.getArtist() + "**");
            return;
        }
        String usernameString = getUserString(e, params.getLastFMData().getDiscordId(), data.getName());
        OffsetDateTime offsetDateTime = OffsetDateTime.ofInstant(instant.get(), data.getTimeZone().toZoneId());
        String date = CommandUtil.getAmericanizedDate(offsetDateTime);
        sendMessageQueue(e, String.format("Last time that **%s** scrobbled **%s** was at %s", usernameString, CommandUtil.cleanMarkdownCharacter(artist.getArtist()), date));
    }


}

