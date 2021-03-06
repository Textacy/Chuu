package core.commands.abstracts;

import core.Chuu;
import core.commands.utils.CommandCategory;
import core.commands.utils.CommandUtil;
import core.exceptions.LastFmException;
import core.parsers.NpParser;
import core.parsers.Parser;
import core.parsers.params.NowPlayingParameters;
import dao.ChuuService;
import dao.entities.LastFMData;
import dao.entities.NowPlayingArtist;
import dao.entities.ScrobbledArtist;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import javax.validation.constraints.NotNull;
import java.util.concurrent.CompletableFuture;

public abstract class NpCommand extends ConcurrentCommand<NowPlayingParameters> {


    protected NpCommand(ChuuService dao) {
        super(dao);
    }

    @Override
    protected CommandCategory initCategory() {
        return CommandCategory.NOW_PLAYING;
    }

    @Override
    public Parser<NowPlayingParameters> initParser() {
        return new NpParser(db, lastFM);
    }

    @Override
    protected void onCommand(MessageReceivedEvent e, @NotNull NowPlayingParameters params) {
        doSomethingWithArtist(params.getNowPlayingArtist(), e, params.getLastFMData().getDiscordId(), params.getLastFMData(), params);
        CompletableFuture.runAsync(() -> {
            if (params.getNowPlayingArtist().getUrl() != null && !params.getNowPlayingArtist().getUrl().isBlank()) {
                try {
                    ScrobbledArtist scrobbledArtist = CommandUtil.onlyCorrection(db, params.getNowPlayingArtist().getArtistName(), lastFM, true);
                    long trackValidate = CommandUtil.trackValidate(db, scrobbledArtist, lastFM, params.getNowPlayingArtist().getSongName());
                    db.updateTrackImage(trackValidate, params.getNowPlayingArtist().getUrl());
                } catch (LastFmException instanceNotFoundException) {
                    Chuu.getLogger().warn(instanceNotFoundException.getMessage(), instanceNotFoundException);
                }
                    }
                }
        );

    }

    protected abstract void doSomethingWithArtist(NowPlayingArtist artist, MessageReceivedEvent e, long discordId, LastFMData user, NowPlayingParameters parameters);
}
