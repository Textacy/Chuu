package core.parsers;

import core.parsers.exceptions.InvalidChartValuesException;
import core.parsers.params.ChartParameters;
import core.parsers.utils.CustomTimeFrame;
import dao.ChuuService;
import dao.entities.LastFMData;
import dao.entities.TimeFrameEnum;
import dao.exceptions.InstanceNotFoundException;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.awt.*;

public class ChartNormalParser extends ChartableParser<ChartParameters> {


    public ChartNormalParser(ChuuService dao, TimeFrameEnum defaultTFE) {
        super(dao, defaultTFE);
    }

    public ChartNormalParser(ChuuService dao) {
        super(dao, TimeFrameEnum.WEEK);
    }

    @Override
    public ChartParameters parseLogic(MessageReceivedEvent e, String[] subMessage) throws InstanceNotFoundException {
        TimeFrameEnum timeFrame = this.defaultTFE;
        int x = 5;
        int y = 5;


        ChartParserAux chartParserAux = new ChartParserAux(subMessage);
        try {
            Point chartSize = chartParserAux.getChartSize();
            if (chartSize != null) {
                x = chartSize.x;
                y = chartSize.y;

            }
        } catch (InvalidChartValuesException ex) {
            this.sendError(getErrorMessage(6), e);
            return null;
        }
        timeFrame = chartParserAux.parseTimeframe(timeFrame);
        subMessage = chartParserAux.getMessage();
        LastFMData data = atTheEndOneUser(e, subMessage);
        return new ChartParameters(e, data, data.getDiscordId(), data.getChartMode(), data, CustomTimeFrame.ofTimeFrameEnum(timeFrame), x, y);
    }
}
