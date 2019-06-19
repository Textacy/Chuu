package main.ScheduledTasks;

import DAO.DaoImplementation;
import DAO.Entities.ArtistData;
import DAO.Entities.TimestampWrapper;
import DAO.Entities.UsersWrapper;
import main.APIs.Discogs.DiscogsApi;
import main.APIs.Spotify.Spotify;
import main.APIs.Spotify.SpotifySingleton;
import main.APIs.last.ConcurrentLastFM;
import main.Commands.CommandUtil;
import main.Exceptions.LastFMNoPlaysException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.Random;

public class UpdaterThread implements Runnable {

	private final DaoImplementation dao;
	private UsersWrapper username;
	private boolean isIncremental;
	private DiscogsApi discogsApi;
	private ConcurrentLastFM lastFM;
	private Spotify spotify;

	public UpdaterThread(DaoImplementation dao) {
		this.dao = dao;
		this.lastFM = new ConcurrentLastFM();
		this.spotify = SpotifySingleton.getInstanceUsingDoubleLocking();
	}

	public UpdaterThread(DaoImplementation dao, UsersWrapper username, boolean isIncremental) {
		this(dao);
		this.username = username;
		this.isIncremental = isIncremental;
	}


	public UpdaterThread(DaoImplementation dao, UsersWrapper username, boolean isIncremental, DiscogsApi discogsApi) {
		this(dao, username, isIncremental);
		this.discogsApi = discogsApi;
	}


	@Override
	public void run() {
		System.out.println("THREAD WORKING ) + " + LocalDateTime.now().toString());
		UsersWrapper usertoWork;
		Random r = new Random();
		float chance = r.nextFloat();

		if (this.username == null) {
			usertoWork = dao.getLessUpdated();
		} else
			usertoWork = this.username;

		try {
			if (isIncremental && chance <= 0.995f) {

				TimestampWrapper<LinkedList<ArtistData>> artistDataLinkedList = lastFM.getWhole(usertoWork.getLastFMName(), usertoWork.getTimestamp());

				for (ArtistData datum : artistDataLinkedList.getWrapped()) {
					String prevUrl = dao.getArtistUrl(datum.getArtist());
					if (prevUrl == null)
						prevUrl = CommandUtil.updateUrl(discogsApi, datum.getArtist(), dao, spotify);

					datum.setUrl(prevUrl);
				}

				dao.incrementalUpdate(artistDataLinkedList, usertoWork.getLastFMName());

				System.out.println("Updated Info Incremetally of " + usertoWork.getLastFMName() + LocalDateTime.now().format(DateTimeFormatter.ISO_DATE));
				System.out.println(" Number of rows updated :" + artistDataLinkedList.getWrapped().size());
			} else {

				LinkedList<ArtistData> artistDataLinkedList = lastFM.getLibrary(usertoWork.getLastFMName());
				dao.updateUserLibrary(artistDataLinkedList, usertoWork.getLastFMName());

				System.out.println("Updated Info Normally  of " + usertoWork.getLastFMName() + LocalDateTime.now().format(DateTimeFormatter.ISO_DATE));
				System.out.println(" Number of rows updated :" + artistDataLinkedList.size());
			}
		} catch (LastFMNoPlaysException e) {
			dao.updateUserTimeStamp(usertoWork.getLastFMName());
			System.out.println("No plays " + usertoWork.getLastFMName() + LocalDateTime.now().format(DateTimeFormatter.ISO_DATE));

		} catch (Throwable e) {
			System.out.println("Error while updating" + usertoWork.getLastFMName() + LocalDateTime.now().format(DateTimeFormatter.ISO_DATE));
			e.printStackTrace();
		}
	}
}