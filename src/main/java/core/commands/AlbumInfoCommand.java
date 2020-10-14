package core.commands;

import core.exceptions.LastFmException;
import core.parsers.params.ArtistAlbumParameters;
import core.services.TagAlbumService;
import dao.ChuuService;
import dao.entities.*;
import dao.musicbrainz.MusicBrainzService;
import dao.musicbrainz.MusicBrainzServiceSingleton;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.List;
import java.util.stream.Collectors;

public class AlbumInfoCommand extends AlbumPlaysCommand {
    private final MusicBrainzService mb;

    public AlbumInfoCommand(ChuuService dao) {
        super(dao);
        mb = MusicBrainzServiceSingleton.getInstance();
    }


    @Override
    protected CommandCategory getCategory() {
        return CommandCategory.INFO;
    }

    @Override
    public String getDescription() {
        return "Information about an album";
    }

    @Override
    public List<String> getAliases() {
        return List.of("albuminfo", "abi");
    }

    @Override
    public String getName() {
        return "Album Info";
    }

    @Override
    void doSomethingWithAlbumArtist(ScrobbledArtist artist, String album, MessageReceivedEvent e, long who, ArtistAlbumParameters params) throws LastFmException {
        LastFMData lastFMData = params.getLastFMData();
        FullAlbumEntityExtended albumSummary = lastFM.getAlbumSummary(lastFMData.getName(), artist.getArtist(), album);
        String username = getUserString(e, who, lastFMData.getName());

        EmbedBuilder embedBuilder = new EmbedBuilder();
        String tagsField = albumSummary.getTagList().isEmpty()
                ? ""
                : albumSummary.getTagList().stream()
                .map(tag -> "[" + CommandUtil.cleanMarkdownCharacter(tag) + "](" + CommandUtil.getLastFmTagUrl(tag) + ")")
                .collect(Collectors.joining(" - "));
        StringBuilder trackList = new StringBuilder();

        MusicbrainzFullAlbumEntity albumInfo = mb.getAlbumInfo(albumSummary);
        albumSummary.getTrackList().forEach(x ->
                trackList.append(x.getPosition()).append(". ")
                        .append(CommandUtil.cleanMarkdownCharacter(x.getName()))
                        .append(". ").append(
                        String
                                .format("%02d:%02d", x.getDuration() / 60, x.getDuration() % 60))
                        .append("\n"));
        MessageBuilder messageBuilder = new MessageBuilder();
        embedBuilder.setTitle(CommandUtil.cleanMarkdownCharacter(albumSummary.getAlbum()), CommandUtil.getLastFmArtistAlbumUrl(albumSummary.getArtist(), albumSummary.getAlbum()))
                .addField("Artist:", "[" + CommandUtil.cleanMarkdownCharacter(albumSummary.getArtist()) + "](" + CommandUtil.getLastFmArtistUrl(albumSummary.getArtist()) + ")", false)
                .addField(username + "'s plays:", String.valueOf(albumSummary.getTotalPlayNumber()), true)
                .addField("Listeners:", String.valueOf(albumSummary.getListeners()), true)
                .addField("Scrobbles:", String.valueOf(albumSummary.getTotalscrobbles()), true)
                .addField("Tags:", tagsField, false);
        if (!albumInfo.getTags().isEmpty()) {
            String collect = albumInfo.getTags().stream().limit(5)
                    .map(tag -> "[" + CommandUtil.cleanMarkdownCharacter(tag) + "](" + CommandUtil.getMusicbrainzTagUrl(tag) + ")")
                    .collect(Collectors.joining(" - "));
            embedBuilder.addField("MusicBrainz Tags: ", collect, false);
        }
        if (albumInfo.getYear() != null) {
            embedBuilder.addField("Year:", String.valueOf(albumInfo.getYear()), false);
        }

        if (!albumSummary.getTrackList().isEmpty()) {
            embedBuilder.addField("Track List:", trackList.toString().substring(0, Math.min(trackList.length(), 1000)), false)
                    .addField("Total Duration:",
                            (String.format("%02d:%02d minutes", albumSummary.getTotalDuration() / 60, albumSummary.getTotalDuration() % 60))
                            , true);
        }
        embedBuilder.setImage(albumSummary.getAlbumUrl())
                .setColor(CommandUtil.randomColor())
                .setThumbnail(artist.getUrl());
        e.getChannel().sendMessage(messageBuilder.setEmbed(embedBuilder.build()).build()).queue();
        if (!albumSummary.getTagList().isEmpty()) {
            executor.submit(new TagAlbumService(getService(), lastFM, albumSummary.getTagList(), new AlbumInfo(albumSummary.getMbid(), albumSummary.getAlbum(), albumSummary.getArtist())));

        }
    }
}
