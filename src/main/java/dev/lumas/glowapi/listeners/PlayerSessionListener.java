package dev.lumas.glowapi.listeners;

import dev.lumas.core.annotation.Autowire;
import dev.lumas.core.annotation.Register;
import dev.lumas.glowapi.model.GlowColorManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

@Register(Autowire.LISTENER)
public class PlayerSessionListener implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        GlowColorManager glowColorManager = GlowColorManager.getInstance();
        Player player = event.getPlayer();
        glowColorManager.addPlayer(player);
        glowColorManager.update(player);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        GlowColorManager.getInstance().removePlayer(event.getPlayer());
    }

}
