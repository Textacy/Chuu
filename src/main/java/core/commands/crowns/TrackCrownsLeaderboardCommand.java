package core.commands.crowns;

import core.commands.abstracts.LeaderboardCommand;
import core.commands.utils.CommandCategory;
import core.parsers.NoOpParser;
import core.parsers.NumberParser;
import core.parsers.Parser;
import core.parsers.params.CommandParameters;
import core.parsers.params.NumberParameters;
import dao.ChuuService;
import dao.entities.LbEntry;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static core.parsers.ExtraParser.LIMIT_ERROR;

public class TrackCrownsLeaderboardCommand extends LeaderboardCommand<NumberParameters<CommandParameters>> {

    public TrackCrownsLeaderboardCommand(ChuuService dao) {
        super(dao);
    }

    @Override
    protected CommandCategory initCategory() {
        return CommandCategory.CROWNS;
    }

    @Override
    public Parser<NumberParameters<CommandParameters>> initParser() {

        Map<Integer, String> map = new HashMap<>(2);
        map.put(LIMIT_ERROR, "The number introduced must be positive and not very big");
        String s = "You can also introduce a number to vary the number of plays to award a crown, " +
                "defaults to whatever the guild has configured (0 if not configured)";
        return new NumberParser<>(new NoOpParser(),
                null,
                Integer.MAX_VALUE,
                map, s, false, true, true);
    }

    @Override
    public String getEntryName(NumberParameters<CommandParameters> params) {
        return "Track crowns leaderboard";
    }

    @Override
    public String getDescription() {
        return ("List of users ordered by number of track crowns");
    }

    @Override
    public List<String> getAliases() {
        return Arrays.asList("crownstracklb", "crownstracklb", "ctrlb", "crownstrlb");
    }

    @Override
    public List<LbEntry> getList(NumberParameters<CommandParameters> params) {
        Long threshold = params.getExtraParam();
        long idLong = params.getE().getGuild().getIdLong();

        if (threshold == null) {
            threshold = (long) db.getGuildCrownThreshold(idLong);
        }
        return db.trackCrownsLeaderboard(params.getE().getGuild().getIdLong(), Math.toIntExact(threshold));
    }

    @Override
    public String getName() {
        return "Track crowns leaderboard";
    }
}
