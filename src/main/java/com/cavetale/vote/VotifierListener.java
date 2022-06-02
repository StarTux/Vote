package com.cavetale.vote;

import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.model.VotifierEvent;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

@RequiredArgsConstructor
final class VotifierListener implements Listener {
    private final VotePlugin plugin;

    protected void enable() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    void onVotifier(VotifierEvent event) {
        Vote vote = event.getVote();
        plugin.onVote(vote.getServiceName(),
                      vote.getUsername(),
                      vote.getAddress(),
                      vote.getTimeStamp());
        plugin.getLogger().info("[Votifier] " + vote);
    }
}
