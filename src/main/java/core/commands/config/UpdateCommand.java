package core.commands.config;

import core.commands.abstracts.ConcurrentCommand;
import core.commands.utils.CommandCategory;
import core.exceptions.LastFMNoPlaysException;
import core.exceptions.LastFmException;
import core.parsers.OnlyUsernameParser;
import core.parsers.OptionalEntity;
import core.parsers.Parser;
import core.parsers.params.ChuuDataParams;
import core.parsers.utils.CustomTimeFrame;
import core.services.UpdaterHoarder;
import core.services.UpdaterService;
import dao.ChuuService;
import dao.entities.*;
import dao.exceptions.InstanceNotFoundException;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import javax.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class UpdateCommand extends ConcurrentCommand<ChuuDataParams> {


    public final AtomicInteger maxConcurrency = new AtomicInteger(5);

    public UpdateCommand(ChuuService dao) {
        super(dao);
    }

    @Override
    protected CommandCategory initCategory() {
        return CommandCategory.STARTING;
    }

    @Override
    public Parser<ChuuDataParams> initParser() {
        return new OnlyUsernameParser(db, new OptionalEntity("force", "Does a full heavy update"));
    }

    @Override
    public String getDescription() {
        return "Keeps you up to date";
    }

    @Override
    public List<String> getAliases() {
        return Collections.singletonList("update");
    }

    @Override
    protected void onCommand(MessageReceivedEvent e, @NotNull ChuuDataParams params) throws LastFmException, InstanceNotFoundException {

        LastFMData lastFMData = params.getLastFMData();
        String lastFmName = lastFMData.getName();
        long discordID = lastFMData.getDiscordId();
        if (lastFMData.isPrivateUpdate() && e.getAuthor().getIdLong() != discordID) {
            sendMessageQueue(e, "This user cannot be updated by other users");
            return;
        }
        db.findLastFMData(discordID);
        boolean force = params.hasOptional("force");
        String userString = getUserString(e, discordID, lastFmName);
        if (e.isFromGuild()) {
            if (db.getAll(e.getGuild().getIdLong()).stream()
                    .noneMatch(s -> s.getLastFMName().equals(lastFmName))) {
                sendMessageQueue(e, userString + " is not registered in this server");
                return;
            }
        } else if (!db.getMapGuildUsers().containsValue(e.getAuthor().getIdLong())) {
            sendMessageQueue(e, "You are not registered yet, go to any server and register there!");
            return;
        }
        boolean removeFlag = true;
        try {
            if (!UpdaterService.lockAndContinue(lastFmName)) {
                removeFlag = false;
                sendMessageQueue(e, "You were already being updated. Wait a few seconds");
                return;
            }
            if (force) {
                synchronized (this) {
                    if (maxConcurrency.decrementAndGet() == 0) {
                        sendMessageQueue(e, "There are a lot of people executing this type of update, try again later :(");
                        maxConcurrency.incrementAndGet();
                    }
                }
                try {
                    List<ScrobbledArtist> artistData = lastFM.getAllArtists(lastFMData, CustomTimeFrame.ofTimeFrameEnum(TimeFrameEnum.ALL));
                    db.insertArtistDataList(artistData, lastFmName);
                    List<ScrobbledAlbum> albumData = lastFM.getAllAlbums(lastFMData, CustomTimeFrame.ofTimeFrameEnum(TimeFrameEnum.ALL));
                    e.getChannel().sendTyping().queue();
                    db.albumUpdate(albumData, artistData, lastFmName);
                    List<ScrobbledTrack> trackData = lastFM.getAllTracks(lastFMData, CustomTimeFrame.ofTimeFrameEnum(TimeFrameEnum.ALL));
                    db.trackUpdate(trackData, artistData, lastFmName);
                } finally {
                    synchronized (this) {
                        maxConcurrency.incrementAndGet();
                    }
                }
            } else {
                UpdaterUserWrapper userUpdateStatus = db.getUserUpdateStatus(lastFMData.getDiscordId());
                try {
                    UpdaterHoarder updaterHoarder = new UpdaterHoarder(userUpdateStatus, db, lastFM);
                    updaterHoarder.updateUser();

                    //db.incrementalUpdate(artistDataLinkedList, userUpdateStatus.getLastFMName());
                } catch (LastFMNoPlaysException ex) {
                    db.updateUserTimeStamp(userUpdateStatus.getLastFMName(), userUpdateStatus.getTimestamp(),
                            (int) (Instant.now().getEpochSecond() + 4000));
                    sendMessageQueue(e, "You were already up to date!");
                    return;
                }
            }
            sendMessageQueue(e, "Successfully updated " + userString + " info!");


        } finally {
            if (removeFlag) {
                UpdaterService.remove(lastFmName);
            }
        }

    }

    @Override
    public String getName() {
        return "Update";
    }


}
