package dao;

import dao.entities.*;

import java.sql.Connection;
import java.util.Map;
import java.util.Set;

interface UpdaterDao {
	void addArtist(Connection con, ArtistData artistData);


	UpdaterUserWrapper getLessUpdated(Connection connection);

	void addUrl(Connection con, ArtistData artistData);

	void setUpdatedTime(Connection connection, String id, Integer timestamp, Integer timestampControl);

	void upsertArtist(Connection con, ArtistData artistData);

	void upsertUrl(Connection con, ArtistInfo artistInfo);


	void upsertUrlBitMask(Connection con, ArtistInfo artistInfo, boolean bit);

	String getArtistUrl(Connection connection, String artist);

	Set<String> selectNullUrls(Connection connection, boolean spotifyNull);

	void upsertSpotify(Connection con, ArtistInfo artistInfo);

	UpdaterStatus getUpdaterStatus(Connection connection, String artist_id);

	void insertCorrection(Connection connection, String artist, String correction);

	void updateStatusBit(Connection connection, String artist_id);

	String findCorrection(Connection connection, String artist);

	void updateMetric(Connection connection, int metricId, long value);

	void deleteAllArtists(Connection con, String id);

	boolean insertRandomUrl(Connection con, String url, long discordId, Long guildId);

	RandomUrlEntity getRandomUrl(Connection con);

	RandomUrlEntity findRandomUrlById(Connection con, String url);

	void insertAlbumCrown(Connection connection, String artist, String album, long discordID, long guildId, int plays);

	Map<Long, Character> getGuildPrefixes(Connection connection);

	void upsertGuildPrefix(Connection connection, long guildID, Character prefix);
}