package core.commands;

import core.Chuu;
import core.exceptions.InstanceNotFoundException;
import core.exceptions.LastFmException;
import core.parsers.ArtistUrlParser;
import core.parsers.Parser;
import core.parsers.params.ArtistUrlParameters;
import dao.ChuuService;
import dao.entities.LastFMData;
import dao.entities.Role;
import dao.entities.ScrobbledArtist;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.OptionalLong;

public class ArtistUrlCommand extends ConcurrentCommand<ArtistUrlParameters> {
    public ArtistUrlCommand(ChuuService dao) {
        super(dao);
    }


    @Override
    protected CommandCategory getCategory() {
        return CommandCategory.ARTIST_IMAGES;
    }

    @Override
    public Parser<ArtistUrlParameters> getParser() {
        return new ArtistUrlParser();
    }

    @Override
    public String getDescription() {
        return "Changes artist image that is displayed on some bot functionalities";
    }

    @Override
    public List<String> getAliases() {
        return Collections.singletonList("url");
    }

    @Override
    public void onCommand(MessageReceivedEvent e) throws LastFmException, InstanceNotFoundException {
        String urlParsed;
        String artist;
        LastFMData lastFMData = getService().findLastFMData(e.getAuthor().getIdLong());
        if (lastFMData.getRole().equals(Role.IMAGE_BLOCKED)) {
            sendMessageQueue(e, "You don't have enough permissions to add an image!");
            return;
        }
        ArtistUrlParameters params = parser.parse(e);
        if (params == null)
            return;
        urlParsed = params.getUrl();

        artist = params.getArtist();
        try (InputStream in = new URL(urlParsed).openStream()) {
            BufferedImage image = ImageIO.read(in);
            if (image == null) {
                parser.sendError(parser.getErrorMessage(2), e);
                return;
            }
            ScrobbledArtist scrobbledArtist = CommandUtil.onlyCorrection(getService(), artist, lastFM);
            OptionalLong persistedId = getService().checkArtistUrlExists(scrobbledArtist.getArtistId(), urlParsed);
            OptionalLong queuedId = getService().checkQueuedUrlExists(scrobbledArtist.getArtistId(), urlParsed);

            if (persistedId.isPresent()) {
                sendMessageQueue(e, "That image already existed for artist: " + CommandUtil.cleanMarkdownCharacter(scrobbledArtist.getArtist()) + "\n Added a vote to that image instead");
                getService().castVote(persistedId.getAsLong(), e.getAuthor().getIdLong(), true);
                return;
            } else if (queuedId.isPresent()) {
                sendMessageQueue(e, "That image for **" + CommandUtil.cleanMarkdownCharacter(scrobbledArtist.getArtist()) + "** is already on the review queue.");
                return;
            }
            getService().userInsertQueueUrl(urlParsed, scrobbledArtist.getArtistId(), e.getAuthor().getIdLong());
            sendMessageQueue(e, "Submitted an image for " + CommandUtil.cleanMarkdownCharacter(scrobbledArtist.getArtist()) + ".\nIt will be reviewed by a bot moderator.");

        } catch (IOException exception) {
            parser.sendError(parser.getErrorMessage(2), e);
            Chuu.getLogger().warn(exception.getMessage(), exception);
        }

    }

    @Override
    public String getName() {
        return "Artist Url ";
    }


}
