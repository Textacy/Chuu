package core.commands.stats;

import com.wrapper.spotify.model_objects.specification.AudioFeatures;
import com.wrapper.spotify.model_objects.specification.Track;
import core.apis.spotify.Spotify;
import core.apis.spotify.SpotifySingleton;
import core.commands.abstracts.ConcurrentCommand;
import core.commands.utils.CommandCategory;
import core.commands.utils.CommandUtil;
import core.commands.utils.PrivacyUtils;
import core.exceptions.LastFmException;
import core.parsers.ArtistSongParser;
import core.parsers.Parser;
import core.parsers.params.ArtistAlbumParameters;
import core.services.ColorService;
import core.services.SpotifyTrackService;
import dao.ChuuService;
import dao.entities.DiscordUserDisplay;
import dao.entities.LastFMData;
import dao.entities.ScrobbledArtist;
import dao.entities.ScrobbledTrack;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.apache.commons.lang3.tuple.Pair;

import javax.validation.constraints.NotNull;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class SongAudioFeaturesCommand extends ConcurrentCommand<ArtistAlbumParameters> {
    private final Spotify spotify;

    public SongAudioFeaturesCommand(ChuuService dao) {
        super(dao);
        this.spotify = SpotifySingleton.getInstance();
    }

    @Override
    protected CommandCategory initCategory() {
        return CommandCategory.INFO;
    }

    @Override
    public Parser<ArtistAlbumParameters> initParser() {
        return new ArtistSongParser(db, lastFM);
    }

    @Override
    public String getDescription() {
        return "Gets audio features of a specific song using Spotify data";
    }

    @Override
    public List<String> getAliases() {
        return List.of("trackfeatures", "songfeatures", "songft", "trackft");
    }

    @Override
    public String getName() {
        return "Specific song features";
    }

    @Override
    protected void onCommand(MessageReceivedEvent e, @NotNull ArtistAlbumParameters params) throws LastFmException {
        LastFMData lastFMData = params.getLastFMData();

        ScrobbledArtist scrobbledArtist = CommandUtil.onlyCorrection(db, params.getArtist(), lastFM, true);
        long trackId = CommandUtil.trackValidate(db, scrobbledArtist, lastFM, params.getAlbum());

        ScrobbledTrack scrobbledTrack = new ScrobbledTrack(
                params.getArtist(), params.getAlbum(), 1, false, 0, null, null, null);
        scrobbledTrack.setTrackId(trackId);


        SpotifyTrackService spotifyTrackService = new SpotifyTrackService(db, lastFMData.getName());
        List<Pair<ScrobbledTrack, com.wrapper.spotify.model_objects.specification.Track>> pairs = spotifyTrackService.searchTracks(List.of(scrobbledTrack));

        if (pairs.isEmpty() || pairs.get(0).getValue() == null) {
            sendMessageQueue(e, "Couldn't find any audio feature for **%s by %s**".formatted(scrobbledArtist.getArtist(), params.getAlbum()));
            return;
        }
        Track value = pairs.get(0).getValue();
        List<AudioFeatures> audioFeatures = spotify.getAudioFeatures(Set.of(value.getId()));
        CompletableFuture.runAsync(() -> {
            var audioFeaturesStream = audioFeatures.stream().map(t ->
                    new dao.entities.AudioFeatures(t.getAcousticness(), t.getAnalysisUrl(), t.getDanceability(), t.getDurationMs(), t.getEnergy(), t.getId(), t.getInstrumentalness(), t.getKey(), t.getLiveness(), t.getLoudness(), t.getSpeechiness(), t.getTempo(), t.getTimeSignature(), t.getTrackHref(), t.getUri(), t.getValence())).collect(Collectors.toList());
            db.insertAudioFeatures(audioFeaturesStream);
        });
        Optional<AudioFeatures> reduce = audioFeatures.stream().reduce((a, b) -> {
            if (b == null) {
                return a;
            }
            return a.builder().setAcousticness(a.getAcousticness() + b.getAcousticness())
                    .setDanceability(a.getDanceability() + b.getDanceability())
                    .setDurationMs(a.getDurationMs() + b.getDurationMs())
                    .setEnergy(a.getEnergy() + b.getEnergy())
                    .setInstrumentalness(a.getInstrumentalness() + b.getInstrumentalness())
                    .setKey(a.getKey() + b.getKey())
                    .setLiveness(a.getLiveness() + b.getLiveness())
                    .setLoudness((float) (Math.pow(10, (a.getLoudness() + 60) / 10.0) + Math.pow(10, (a.getLoudness() + 60) / 10.0)))
                    .setSpeechiness(a.getSpeechiness() + b.getSpeechiness())
                    .setTempo(a.getTempo() + b.getTempo())
                    .setValence(a.getValence() + b.getValence())
                    .build();
        });
        double[] doubles = audioFeatures.stream().mapToDouble(AudioFeatures::getLoudness).toArray();
        OptionalDouble average = Arrays.stream(doubles).map(x -> Math.pow(10, (x + 60) / 10.0)).average();
        if (reduce.isEmpty()) {
            sendMessageQueue(e, "Couldn't find any audio feature for **%s by %s**".formatted(scrobbledArtist.getArtist(), params.getAlbum()));
            return;
        }
        DecimalFormat df = new DecimalFormat("##.##%");
        DecimalFormat db = new DecimalFormat("##.# 'dB' ");

        int s = audioFeatures.size();
        DiscordUserDisplay userInfoNotStripped = CommandUtil.getUserInfoNotStripped(e, params.getLastFMData().getDiscordId());
        AudioFeatures audioFeature = reduce.get();
        EmbedBuilder embedBuilder = new EmbedBuilder()
                .setAuthor("Audio features for " + scrobbledTrack.getName() + " by " + scrobbledTrack.getArtist(), PrivacyUtils.getLastFmUser(lastFMData.getName()), userInfoNotStripped.getUrlImage())
                .setColor(ColorService.computeColor(e))
                .addField("Happiness:", df.format(audioFeature.getValence() / s), true)
                .addField("Acousticness:", df.format(audioFeature.getAcousticness() / s), true)
                .addField("Danceability:", df.format(audioFeature.getDanceability() / s), true)
                .addField("Instrumentalness:", df.format(audioFeature.getInstrumentalness() / s), true)
                .addField("Liveness:", df.format(audioFeature.getLiveness() / s), true);
        if (average.isPresent()) {
            embedBuilder.addField("Loudness:", db.format(10 * Math.log10(average.getAsDouble())), true);

        }
        embedBuilder.addField("Energy:", df.format(audioFeature.getEnergy() / s), true)
                .addField("Average Tempo:", (int) (audioFeature.getTempo() / s) + " BPM", true);

        e.getChannel().sendMessage(embedBuilder.build()).queue();
    }
}
