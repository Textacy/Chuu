package core.parsers;

import core.apis.last.ConcurrentLastFM;
import core.exceptions.LastFmException;
import core.parsers.params.NowPlayingParameters;
import core.services.NPService;
import dao.ChuuService;
import dao.entities.LastFMData;
import dao.entities.NowPlayingArtist;
import dao.exceptions.InstanceNotFoundException;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class NpParser extends DaoParser<NowPlayingParameters> {
    private final ConcurrentLastFM lastFM;

    public NpParser(ChuuService dao, ConcurrentLastFM lastFM) {
        super(dao);
        this.lastFM = lastFM;
    }

    public NowPlayingParameters parseLogic(MessageReceivedEvent e, String[] subMessage) throws InstanceNotFoundException, LastFmException {
        LastFMData data = atTheEndOneUser(e, subMessage);
        NPService npService = new NPService(lastFM, data);
        NPService.NPUpdate nowPlayingBoth = npService.getNowPlayingBoth();
        NowPlayingArtist nowPlayingArtist = nowPlayingBoth.np();
        return new NowPlayingParameters(e, data, nowPlayingArtist, nowPlayingBoth.data());
    }

    @Override
    public String getUsageLogic(String commandName) {
        return "**" + commandName + " *username***\n" +
                "\t If the username is not specified it defaults to authors account\n";
    }
}
