package core.commands;

import core.Chuu;
import core.exceptions.InstanceNotFoundException;
import core.exceptions.LastFmException;
import core.parsers.ArtistUrlParser;
import dao.ChuuService;
import dao.entities.ArtistInfo;
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

public class ArtistUrlCommand extends ConcurrentCommand {
    public ArtistUrlCommand(ChuuService dao) {
        super(dao);
        this.parser = new ArtistUrlParser();
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
        String[] message = parser.parse(e);
        if (message == null)
            return;
        urlParsed = message[1];

        artist = message[0];
        try (InputStream in = new URL(urlParsed).openStream()) {
            BufferedImage image = ImageIO.read(in);
            if (image == null) {
                parser.sendError(parser.getErrorMessage(2), e);
                return;
            }
            ScrobbledArtist scrobbledArtist = CommandUtil.onlyCorrection(getService(), artist, lastFM);
            getService().upsertUrl(new ArtistInfo(urlParsed, scrobbledArtist.getArtist()));
            sendMessageQueue(e, "Image of " + scrobbledArtist.getArtist() + " updated");

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