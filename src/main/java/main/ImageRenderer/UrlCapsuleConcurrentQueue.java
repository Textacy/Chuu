package main.ImageRenderer;

import DAO.DaoImplementation;
import DAO.Entities.UrlCapsule;
import main.APIs.Discogs.DiscogsApi;
import main.APIs.Spotify.Spotify;
import main.Chuu;
import main.Commands.CommandUtil;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;

public class UrlCapsuleConcurrentQueue extends LinkedBlockingQueue<UrlCapsule> {
	private static final long serialVersionUID = 1L;
	private transient final DaoImplementation dao;
	private transient final DiscogsApi discogsApi;
	private transient final Spotify spotifyApi;
	private transient final LinkedBlockingQueue<CompletableFuture<UrlCapsule>> wrapper;

	public UrlCapsuleConcurrentQueue(DaoImplementation dao, DiscogsApi discogsApi, Spotify spotify) {
		super();
		this.dao = dao;
		this.discogsApi = discogsApi;
		this.spotifyApi = spotify;
		this.wrapper = new LinkedBlockingQueue<>();
	}

	@Override
	public int size() {
		return this.wrapper.size();
	}

	public boolean offer(@NotNull UrlCapsule item) {
		CompletableFuture<UrlCapsule> future = CompletableFuture.supplyAsync(() -> {
			item.setUrl(null);
			String url = dao.getArtistUrl(item.getArtistName());
			if (url == null) {
				url = CommandUtil.updateUrl(discogsApi, item.getArtistName(), dao, spotifyApi);
			}
			item.setUrl(url);
			return item;
		}).toCompletableFuture();
		return wrapper.offer(future);
	}

	@NotNull
	public UrlCapsule take() throws InterruptedException {
		try {
			return wrapper.take().get();
		} catch (ExecutionException e) {
			Chuu.getLogger().warn(e.getMessage(), e);
			throw new InterruptedException();
		}
	}

}
