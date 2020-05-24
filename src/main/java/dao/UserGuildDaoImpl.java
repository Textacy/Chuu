package dao;

import core.exceptions.ChuuServiceException;
import core.exceptions.InstanceNotFoundException;
import dao.entities.LastFMData;
import dao.entities.Role;
import dao.entities.UsersWrapper;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;
import org.apache.commons.lang3.tuple.Pair;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class UserGuildDaoImpl implements UserGuildDao {


    @Override
    public void createGuild(Connection con, long guildId) {
        String queryString = "INSERT IGNORE INTO  guild"
                + " (guild_id) " + " VALUES (?) ";

        try (PreparedStatement preparedStatement = con.prepareStatement(queryString)) {

            /* Fill "preparedStatement". */
            preparedStatement.setLong(1, guildId);
            /* Execute query. */
            preparedStatement.executeUpdate();

            /* Get generated identifier. */

        } catch (SQLException e) {
            throw new ChuuServiceException(e);
        }
    }

    @Override
    public void insertUserData(Connection con, LastFMData lastFMData) {
        /* Create "queryString". */
        String queryString = "INSERT INTO  user"
                + " (lastfm_id, discord_id) " + " VALUES (?, ?) ON DUPLICATE KEY UPDATE lastfm_id=" + "?";

        try (PreparedStatement preparedStatement = con.prepareStatement(queryString)) {

            /* Fill "preparedStatement". */
            int i = 1;
            preparedStatement.setString(i++, lastFMData.getName());
            preparedStatement.setLong(i++, lastFMData.getDiscordId());
            preparedStatement.setString(i, lastFMData.getName());


            /* Execute query. */
            preparedStatement.executeUpdate();

            /* Get generated identifier. */

        } catch (SQLException e) {
            throw new ChuuServiceException(e);
        }
    }

    @Override
    public LastFMData findLastFmData(Connection con, long discordId) throws InstanceNotFoundException {

        /* Create "queryString". */
        String queryString = "SELECT   discord_id, lastfm_id,role,private_update,notify_image,additional_embed FROM user WHERE discord_id = ?";

        try (PreparedStatement preparedStatement = con.prepareStatement(queryString)) {

            /* Fill "preparedStatement". */
            int i = 1;
            preparedStatement.setLong(i, discordId);


            /* Execute query. */
            ResultSet resultSet = preparedStatement.executeQuery();

            if (!resultSet.next()) {
                throw new core.exceptions.InstanceNotFoundException(discordId);
            }

            /* Get results. */
            i = 1;
            long resDiscordID = resultSet.getLong(i++);
            String lastFmID = resultSet.getString(i++);
            Role role = Role.valueOf(resultSet.getString(i++));
            boolean privateUpdate = resultSet.getBoolean(i++);
            boolean notify_image = resultSet.getBoolean(i++);
            boolean additional_embed = resultSet.getBoolean(i);


            return new LastFMData(lastFmID, resDiscordID, role, privateUpdate, notify_image, additional_embed);

        } catch (SQLException e) {
            throw new ChuuServiceException(e);
        }
    }

    @Override
    public List<Long> guildsFromUser(Connection connection, long userId) {
        @Language("MariaDB") String queryString = "SELECT discord_id,guild_id  FROM user_guild  WHERE discord_id = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(queryString)) {

            /* Fill "preparedStatement". */
            int i = 1;
            preparedStatement.setLong(i, userId);

            List<Long> returnList = new LinkedList<>();
            /* Execute query. */
            ResultSet resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {

                long guildId = resultSet.getLong("guild_Id");

                returnList.add(guildId);

            }
            return returnList;
        } catch (SQLException e) {
            throw new ChuuServiceException(e);
        }
    }

    @Override
    public MultiValuedMap<Long, Long> getWholeUserGuild(Connection connection) {
        @Language("MariaDB") String queryString = "SELECT discord_id,guild_id  FROM user_guild ";

        MultiValuedMap<Long, Long> map = new ArrayListValuedHashMap<>();
        try (PreparedStatement preparedStatement = connection.prepareStatement(queryString)) {

            ResultSet resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {

                long guildId = resultSet.getLong("guild_Id");
                long discordId = resultSet.getLong("discord_Id");
                map.put(guildId, discordId);


            }

        } catch (SQLException e) {
            throw new ChuuServiceException(e);
        }
        return map;
    }

    @Override
    public void updateLastFmData(Connection con, LastFMData lastFMData) {
        /* Create "queryString". */
        String queryString = "UPDATE user SET lastfm_id= ? WHERE discord_id = ?";

        try (PreparedStatement preparedStatement = con.prepareStatement(queryString)) {

            /* Fill "preparedStatement". */
            int i = 1;
            preparedStatement.setString(i++, lastFMData.getName());

            preparedStatement.setLong(i, lastFMData.getDiscordId());

            /* Execute query. */
            int updatedRows = preparedStatement.executeUpdate();

            if (updatedRows == 0) {
                throw new ChuuServiceException("E");
            }

        } catch (SQLException e) {
            throw new ChuuServiceException(e);
        }
    }

    @Override
    public void removeUser(Connection con, Long discordId) {
        /* Create "queryString". */
        @Language("MariaDB") String queryString = "DELETE FROM  user WHERE discord_id = ?";

        deleteIdLong(con, discordId, queryString);

    }

    private void deleteIdLong(Connection con, Long discordID, String queryString) {
        try (PreparedStatement preparedStatement = con.prepareStatement(queryString)) {

            /* Fill "preparedStatement". */
            int i = 1;
            preparedStatement.setLong(i, discordID);

            /* Execute query. */
            int removedRows = preparedStatement.executeUpdate();

            if (removedRows == 0) {
                System.err.println("No rows removed");
            }

        } catch (SQLException e) {
            throw new ChuuServiceException(e);
        }
    }

    @Override
    public void removeUserGuild(Connection con, long discordId, long guildId) {
        /* Create "queryString". */
        @Language("MariaDB") String queryString = "DELETE FROM  user_guild  WHERE discord_id = ? AND guild_id = ?";

        try (PreparedStatement preparedStatement = con.prepareStatement(queryString)) {

            /* Fill "preparedStatement". */
            int i = 1;
            preparedStatement.setLong(i++, discordId);
            preparedStatement.setLong(i, guildId);


            /* Execute query. */
            int removedRows = preparedStatement.executeUpdate();

            if (removedRows == 0) {
                System.err.println("No rows removed");
            }

        } catch (SQLException e) {
            throw new ChuuServiceException(e);
        }
    }

    @Override
    public List<UsersWrapper> getAll(Connection connection, long guildId) {
        String queryString = "SELECT a.discord_id, a.lastfm_id,a.role FROM user a JOIN (SELECT discord_id FROM user_guild WHERE guild_id = ? ) b ON a.discord_id = b.discord_id";
        try (PreparedStatement preparedStatement = connection.prepareStatement(queryString)) {

            /* Fill "preparedStatement". */

            /* Execute query. */
            preparedStatement.setLong(1, guildId);
            return getUsersWrappers(preparedStatement);
        } catch (SQLException e) {
            throw new ChuuServiceException(e);
        }
    }

    @NotNull
    private List<UsersWrapper> getUsersWrappers(PreparedStatement preparedStatement) throws SQLException {
        ResultSet resultSet = preparedStatement.executeQuery();
        List<UsersWrapper> returnList = new ArrayList<>();
        while (resultSet.next()) {

            String name = resultSet.getString("a.lastFm_Id");
            long discordID = resultSet.getLong("a.discord_ID");
            Role role = Role.valueOf(resultSet.getString(3));

            returnList.add(new UsersWrapper(discordID, name, role));
        }
        return returnList;
    }

    @Override
    public void addGuild(Connection con, long userId, long guildId) {
        /* Create "queryString". */
        String queryString = "INSERT IGNORE INTO  user_guild"
                + " ( discord_id,guild_id) " + " VALUES (?, ?) ";

        try (PreparedStatement preparedStatement = con.prepareStatement(queryString)) {

            /* Fill "preparedStatement". */
            int i = 1;
            preparedStatement.setLong(i++, userId);
            preparedStatement.setLong(i, guildId);


            /* Execute query. */
            preparedStatement.executeUpdate();

            /* Get generated identifier. */

            /* Return booking. */

        } catch (SQLException e) {
            throw new ChuuServiceException(e);
        }
    }

    @Override
    public void addLogo(Connection con, long guildID, BufferedImage image) {
        @Language("MariaDB") String queryString = "UPDATE  guild SET  logo = ? WHERE guild_id = ? ";
        try (PreparedStatement preparedStatement = con.prepareStatement(queryString)) {

            /* Fill "preparedStatement". */
            int i = 1;

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            InputStream is = new ByteArrayInputStream(baos.toByteArray());
            preparedStatement.setBlob(i++, new BufferedInputStream(is));
            preparedStatement.setLong(i, guildID);

            preparedStatement.executeUpdate();


        } catch (SQLException | IOException e) {
            throw new ChuuServiceException(e);
        }
    }

    @Override
    public void removeLogo(Connection connection, long guildId) {
        /* Create "queryString". */
        @Language("MariaDB") String queryString = "UPDATE  guild SET logo = NULL WHERE guild_id = ?";

        deleteIdLong(connection, guildId, queryString);
    }

    @Override
    public InputStream findLogo(Connection connection, long guildID) {
        /* Create "queryString". */
        String queryString = "SELECT logo FROM guild WHERE guild_id = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(queryString)) {

            /* Fill "preparedStatement". */
            int i = 1;
            preparedStatement.setLong(i, guildID);


            /* Execute query. */
            ResultSet resultSet = preparedStatement.executeQuery();

            if (!resultSet.next()) {
                return null;
            }

            /* Get results. */
            return resultSet.getBinaryStream("logo");


        } catch (SQLException e) {
            throw new ChuuServiceException(e);
        }
    }

    @Override
    public long getDiscordIdFromLastFm(Connection connection, String lastFmName, long guildId) throws InstanceNotFoundException {
        @Language("MariaDB") String queryString = "SELECT a.discord_id " +
                "FROM   user a" +
                " JOIN  user_guild  b " +
                "ON a.discord_id = b.discord_id " +
                " WHERE  a.lastfm_id = ? AND b.guild_id = ? ";

        try (PreparedStatement preparedStatement = connection.prepareStatement(queryString)) {

            /* Fill "preparedStatement". */
            int i = 1;
            preparedStatement.setString(i++, lastFmName);
            preparedStatement.setLong(i, guildId);

            /* Execute query. */
            ResultSet resultSet = preparedStatement.executeQuery();

            if (!resultSet.next()) {
                throw new InstanceNotFoundException("Not found ");
            }

            /* Get results. */

            return resultSet.getLong(1);

        } catch (SQLException e) {
            throw new ChuuServiceException(e);
        }

    }

    @Override
    public LastFMData findByLastFMId(Connection connection, String lastFmID) throws InstanceNotFoundException {
        @Language("MariaDB") String queryString = "SELECT a.discord_id, a.lastfm_id , a.role,a.private_update,a.notify_image,a.additional_embed  " +
                "FROM   user a" +
                " WHERE  a.lastfm_id = ? ";

        try (PreparedStatement preparedStatement = connection.prepareStatement(queryString)) {

            /* Fill "preparedStatement". */
            int i = 1;
            preparedStatement.setString(i, lastFmID);

            ResultSet resultSet = preparedStatement.executeQuery();

            if (!resultSet.next()) {
                throw new InstanceNotFoundException("Not found ");
            }
            long aLong = resultSet.getLong(1);
            String string = resultSet.getString(2);
            Role role = Role.valueOf(resultSet.getString(3));
            boolean privateUpdate = resultSet.getBoolean(4);
            boolean imageNOtify = resultSet.getBoolean(5);
            boolean additional_embed = resultSet.getBoolean(6);



            /* Get results. */

            return new LastFMData(string, aLong, role, privateUpdate, imageNOtify, additional_embed);

        } catch (SQLException e) {
            throw new ChuuServiceException(e);
        }
    }

    @Override
    public List<UsersWrapper> getAll(Connection connection) {
        String queryString = "SELECT a.discord_id, a.lastfm_id, a.role FROM user a ";
        try (PreparedStatement preparedStatement = connection.prepareStatement(queryString)) {

            /* Fill "preparedStatement". */

            /* Execute query. */
            return getUsersWrappers(preparedStatement);
        } catch (SQLException e) {
            throw new ChuuServiceException(e);
        }
    }

    @Override
    public void removeRateLimit(Connection connection, long discordId) {
        String queryString = "DELETE from rate_limited where discord_id = ? ";
        try (PreparedStatement preparedStatement = connection.prepareStatement(queryString)) {
            preparedStatement.setLong(1, discordId);
            /* Fill "preparedStatement". */

            /* Execute query. */
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            throw new ChuuServiceException(e);
        }
    }

    @Override
    public void upsertRateLimit(Connection connection, long discordId, float queriesPerSecond) {
        String queryString = "INSERT INTO  rate_limited"
                + " (discord_id,queries_second) " + " VALUES (?, ?) ON DUPLICATE KEY UPDATE queries_second = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(queryString)) {

            /* Fill "preparedStatement". */
            int i = 1;
            preparedStatement.setLong(i++, discordId);
            preparedStatement.setFloat(i++, queriesPerSecond);
            preparedStatement.setFloat(i, queriesPerSecond);


            /* Execute query. */
            preparedStatement.executeUpdate();

            /* Get generated identifier. */

        } catch (SQLException e) {
            throw new ChuuServiceException(e);
        }
    }


    @Override
    public void insertServerDisabled(Connection connection, long discordId, String commandName) {
        String queryString = "INSERT IGNORE INTO  command_guild_disabled"
                + " (guild_id,command_name) VALUES (?, ?) ";

        try (PreparedStatement preparedStatement = connection.prepareStatement(queryString)) {

            /* Fill "preparedStatement". */
            int i = 1;
            preparedStatement.setLong(i++, discordId);
            preparedStatement.setString(i, commandName);


            /* Execute query. */
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            throw new ChuuServiceException(e);
        }
    }

    @Override
    public void insertChannelCommandStatus(Connection connection, long discordId, long channelId, String commandName, boolean enabled) {
        String queryString = "INSERT ignore INTO  command_guild_channel_disabled"
                + " (guild_id,channel_id,command_name,enabled) VALUES (?, ?,? , ? ) ";

        try (PreparedStatement preparedStatement = connection.prepareStatement(queryString)) {

            /* Fill "preparedStatement". */
            int i = 1;
            preparedStatement.setLong(i++, discordId);
            preparedStatement.setLong(i++, channelId);
            preparedStatement.setString(i++, commandName);
            preparedStatement.setBoolean(i, enabled);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            throw new ChuuServiceException(e);
        }
    }

    @Override
    public void deleteChannelCommandStatus(Connection connection, long discordId, long channelId, String commandName) {
        String queryString = "DELETE FROM command_guild_channel_disabled"
                + " WHERE guild_id = ? and channel_id = ? and command_name = ? ";

        try (PreparedStatement preparedStatement = connection.prepareStatement(queryString)) {

            /* Fill "preparedStatement". */
            int i = 1;
            preparedStatement.setLong(i++, discordId);
            preparedStatement.setLong(i++, channelId);
            preparedStatement.setString(i, commandName);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            throw new ChuuServiceException(e);
        }
    }

    @Override
    public void deleteServerCommandStatus(Connection connection, long discordId, String commandName) {
        String queryString = "DELETE FROM command_guild_disabled"
                + " WHERE guild_id = ?  and command_name = ? ";

        try (PreparedStatement preparedStatement = connection.prepareStatement(queryString)) {

            /* Fill "preparedStatement". */
            int i = 1;
            preparedStatement.setLong(i++, discordId);
            preparedStatement.setString(i, commandName);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            throw new ChuuServiceException(e);
        }
    }

    @Override
    public MultiValuedMap<Long, String> initServerCommandStatuses(Connection connection) {
        String queryString = "SELECT guild_id,command_name FROM command_guild_disabled";
        MultiValuedMap<Long, String> map = new HashSetValuedHashMap<>();
        try (PreparedStatement preparedStatement = connection.prepareStatement(queryString)) {

            ResultSet resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {

                long guildId = resultSet.getLong("guild_id");
                String commandName = resultSet.getString("command_name");

                map.put(guildId, commandName);

            }
        } catch (SQLException e) {
            throw new ChuuServiceException(e);
        }
        return map;
    }

    @Override
    public MultiValuedMap<Pair<Long, Long>, String> initServerChannelsCommandStatuses(Connection connection, boolean enabled) {
        String queryString = "SELECT guild_id,channel_id,command_name FROM command_guild_channel_disabled where enabled = ? ";
        MultiValuedMap<Pair<Long, Long>, String> map = new HashSetValuedHashMap<>();
        try (PreparedStatement preparedStatement = connection.prepareStatement(queryString)) {

            preparedStatement.setBoolean(1, enabled);
            ResultSet resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {

                long guildId = resultSet.getLong("guild_id");
                long channel_id = resultSet.getLong("channel_id");

                String commandName = resultSet.getString("command_name");

                map.put(Pair.of(guildId, channel_id), commandName);

            }
        } catch (SQLException e) {
            throw new ChuuServiceException(e);
        }
        return map;
    }

    @Override
    public void setUserProperty(Connection connection, long discordId, String property, boolean chartEmbed) {
        @Language("MariaDB") String queryString = "UPDATE  user SET  " + property + " = ? WHERE discord_id = ? ";
        try (PreparedStatement preparedStatement = connection.prepareStatement(queryString)) {

            /* Fill "preparedStatement". */
            int i = 1;
            preparedStatement.setBoolean(i++, chartEmbed);
            preparedStatement.setLong(i, discordId);
            preparedStatement.executeUpdate();

        } catch (SQLException e) {
            throw new ChuuServiceException(e);
        }

    }

}
