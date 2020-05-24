package core.parsers.params;

import dao.entities.TimeFrameEnum;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.time.Year;
import java.time.temporal.ChronoUnit;

public class ChartYearRangeParameters extends ChartParameters {
    private final Year baseYear;
    private final int numberOfYears;


    public ChartYearRangeParameters(MessageReceivedEvent e, Year baseYear, String lastfmID, long discordId, TimeFrameEnum timeFrameEnum, int x, int y, int numberOfYears, boolean doAdditionalEmbed) {
        super(e, lastfmID, discordId, doAdditionalEmbed, timeFrameEnum, x, y);
        this.baseYear = baseYear;
        this.numberOfYears = numberOfYears;
    }

    public ChartYearRangeParameters(MessageReceivedEvent e, Year baseYear, String lastfmID, long discordId, TimeFrameEnum timeFrameEnum, int x, int y, boolean writeTitles, boolean writePlays, boolean isList, boolean pieFormat, int numberOfYears, boolean doAdditionalEmbed) {
        super(e, lastfmID, discordId, timeFrameEnum, x, y, writeTitles, writePlays, isList, pieFormat, doAdditionalEmbed);
        this.baseYear = baseYear;
        this.numberOfYears = numberOfYears;
    }

    public Year getBaseYear() {
        return baseYear;
    }

    public int getNumberOfYears() {
        return numberOfYears;
    }

    public int getLimitYear() {
        return getBaseYear().plus(numberOfYears, ChronoUnit.YEARS).getValue();
    }

    private int getDecade(int year) {
        return year < 2000 ? (year / 10 * 10 - 1900) : (year / 10 * 10);
    }

    public String getDisplayString() {
        if (numberOfYears == 10 && baseYear.isAfter(Year.now().minus(100, ChronoUnit.YEARS))) {
            return "the " + getDecade(baseYear.getValue()) + "s";
        } else {
            return baseYear.toString() + " to " + getLimitYear();
        }
    }

    public boolean isByTime() {
        return hasOptional("--time");
    }

    public boolean isCareAboutSized() {
        return !hasOptional("--nolimit");
    }
}
