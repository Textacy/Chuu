/*
 * MIT License
 *
 * Copyright (c) 2020 Melms Media LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 From Octave bot https://github.com/Stardust-Discord/Octave/ Modified for integrating with JAVA and the current bot
 */
package core.music;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioItem;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import core.Chuu;
import core.music.utils.TrackContext;
import core.services.ColorService;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.managers.AudioManager;

import java.util.stream.Collectors;

public class LoadResultHandler implements AudioLoadResultHandler {


    private final String query;
    private String identifier;
    private final MusicManager musicManager;
    private final TrackContext trackContext;
    private final MessageReceivedEvent e;
    private final Boolean isNext;
    private final String footnote;

    private Object settings;
    private final boolean premiumGuild = false;
    private int retryCount = 0;


    public LoadResultHandler(String query, MessageReceivedEvent e, MusicManager musicManager, TrackContext trackContext, Boolean isNext, String footnote) {

        this.query = query;
        this.e = e;
        this.musicManager = musicManager;
        this.trackContext = trackContext;
        this.isNext = isNext;
        this.footnote = footnote;
    }

    @Override
    public void trackLoaded(AudioTrack track) {
        cache(track);

        if (!checkVoiceState() || !checkTrack(track, false)) {
            return;
        }


        var isImmediatePlay = musicManager.getPlayer().getPlayingTrack() == null && musicManager.getQueue().isEmpty();
        track.setUserData(trackContext);
        musicManager.enqueue(track, isNext);

        if (!isImmediatePlay) {
            e.getChannel().sendMessage(" queued").queue();
        }
    }


    public void playlistLoaded(AudioPlaylist playlist) {
        cache(playlist);

        if (playlist.isSearchResult()) {
            trackLoaded(playlist.getTracks().get(0));
            return;
        }

        if (!checkVoiceState()) {
            return;
        }

        var queueLimit = queueLimit();
        var pendingEnqueue = playlist.getTracks().stream().filter(it -> checkTrack(it, true)).collect(Collectors.toList());
        var added = 0;
        for (AudioTrack track : pendingEnqueue) {

            if (musicManager.getQueue().size() + 1 >= queueLimit) {
                break;
            }
            track.setUserData(trackContext);
            musicManager.enqueue(track, isNext);
            added++;
        }

        var ignored = pendingEnqueue.size() - added;
        e.getChannel().sendMessage(new EmbedBuilder().setColor(ColorService.computeColor(e)).setTitle("Music Queue")
                .setDescription("Added **" + added + "** tracks to queue from playlist **" + playlist.getName() + "**")
                .setFooter(footnote).build()).queue();
    }

    public void loadFailed(Exception exception) {
        if (musicManager.isIdle()) {
            musicManager.destroy();
        }
        e.getChannel().sendMessage(new EmbedBuilder().setColor(ColorService.computeColor(e)).setTitle("Load Results").setDescription("Unable to load the track:\n`" + exception.getMessage() + "`").build()).queue();
    }


    public void noMatches() {

        if (retryCount < MAX_LOAD_RETRIES && identifier != null) {
            retryCount++;
            Chuu.playerManager.loadItemOrdered(e.getGuild().getIdLong(), identifier, this);
            return;
        }

        if (musicManager.isIdle()) {
            musicManager.destroy();
        }
        e.getChannel().sendMessage(new EmbedBuilder().setColor(ColorService.computeColor(e)).setTitle("Load Results").setDescription("Nothing found by **" + identifier + "**").build()).queue();
    }

    @Override
    public void loadFailed(FriendlyException exception) {
        if (musicManager.isIdle()) {
            musicManager.destroy();
        }
        e.getChannel().sendMessage(new EmbedBuilder()
                .setColor(ColorService.computeColor(e))
                .setTitle("Load Results")
                .setDescription("Unable to load the track:\n`" + exception.getMessage() + "`").build()).queue();
    }

    private boolean checkVoiceState() {
        AudioManager manager = e.getGuild().getAudioManager();
        if (manager.getConnectedChannel() == null) {
            assert e.getMember() != null && e.getMember().getVoiceState() != null : "Whatever";
            if (e.getMember().getVoiceState().getChannel() == null) {
                e.getChannel().sendMessage("You left the voice channel before the track was loaded.").queue();


                if (musicManager.isIdle()) {
                    musicManager.destroy();
                }

                return false;
            }

            return musicManager.openAudioConnection(e.getMember().getVoiceState().getChannel(), e);
        }

        return true;
    }

    private boolean checkTrack(AudioTrack track, Boolean isPlaylist) {
        if (!isPlaylist) {
            var queueLimit = queueLimit();
            String queueLimitDisplay;
            if (queueLimit == Integer.MAX_VALUE) {
                queueLimitDisplay = "unlimited";
            } else {
                queueLimitDisplay = String.valueOf(queueLimit);
            }


            if (musicManager.getQueue().size() + 1 >= queueLimit) {
                e.getChannel().sendMessage("The queue can not exceed  songs." + queueLimitDisplay).queue();
                return false;
            }
        }

        if (!track.getInfo().isStream) {
            var invalidDuration = false;

        }
        return true;

    }

    private int queueLimit() {
        return 500;
    }

    public void cache(AudioItem item) {

    }

    private static final int MAX_LOAD_RETRIES = 2;

    public static void loadItem(String query, MessageReceivedEvent e, MusicManager musicManager, TrackContext trackContext, Boolean isNext, String footnote) {
        var resultHandler = new LoadResultHandler(query, e, musicManager, trackContext, isNext, footnote);

        Chuu.playerManager.loadItemOrdered(e.getGuild().getIdLong(), query, resultHandler);
    }
}

