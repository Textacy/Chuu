package core.commands.stats;

import core.commands.abstracts.ConcurrentCommand;
import core.commands.utils.CommandCategory;
import core.commands.utils.CommandUtil;
import core.otherlisteners.Reactionary;
import core.parsers.OnlyUsernameParser;
import core.parsers.Parser;
import core.parsers.params.ChuuDataParams;
import core.services.ColorService;
import dao.ChuuService;
import dao.entities.DiscordUserDisplay;
import dao.entities.ScoredAlbumRatings;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.stream.Collectors;

public class MyTopRatedRandomUrls extends ConcurrentCommand<ChuuDataParams> {
    public MyTopRatedRandomUrls(ChuuService dao) {
        super(dao);
    }

    static void RandomUrlDisplay(MessageReceivedEvent e, List<ScoredAlbumRatings> ratings, String title, String url) {
        List<String> list = ratings.stream().map(x ->
                ". ***[" + x.getUrl()
                        +
                        "](" + x.getUrl() +
                        ")***\n\t" + String.format("Average: **%s** | # of Ratings: **%d**", ScoredAlbumRatings.formatter.format(x.getAverage() / 2f), x.getNumberOfRatings()) +
                        "\n").collect(Collectors.toList());
        StringBuilder a = new StringBuilder();
        for (
                int i = 0;
                i < 10 && i < list.size(); i++) {
            a.append(i + 1).append(list.get(i));
        }

        EmbedBuilder embedBuilder = new EmbedBuilder()
                .setDescription(a).setTitle(title)
                .setThumbnail(url)
                .setColor(ColorService.computeColor(e));

        e.getChannel().sendMessage(new MessageBuilder().setEmbed(embedBuilder.build()).build()).
                queue(message ->
                        new Reactionary<>(list, message, embedBuilder));
    }

    @Override
    protected CommandCategory initCategory() {
        return CommandCategory.USER_STATS;
    }

    @Override
    public Parser<ChuuDataParams> initParser() {
        return new OnlyUsernameParser(db);
    }

    @Override
    public String getDescription() {
        return "The top rated random urls by yourself, this server or the bot";
    }

    @Override
    public List<String> getAliases() {
        return List.of("mytoprandoms", "mytopr", "myurls");
    }

    @Override
    public String getName() {
        return "My top random urls";
    }

    @Override
    protected void onCommand(MessageReceivedEvent e, @NotNull ChuuDataParams params) {


        long idLong = e.getAuthor().getIdLong();
        List<ScoredAlbumRatings> ratings = db.getUserTopRatedUrlsByEveryoneElse(idLong);
        DiscordUserDisplay userInfoConsideringGuildOrNot = CommandUtil.getUserInfoConsideringGuildOrNot(params.getE(), idLong);
        String title = userInfoConsideringGuildOrNot.getUsername();
        String url = userInfoConsideringGuildOrNot.getUrlImage();


        RandomUrlDisplay(e, ratings, title + "'s top rated urls by everyone else", url);
    }
}
