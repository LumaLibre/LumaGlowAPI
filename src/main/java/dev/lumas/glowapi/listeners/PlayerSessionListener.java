package dev.lumas.glowapi.listeners;

import dev.lumas.glowapi.GlowColorManager;
import dev.lumas.lumacore.manager.modules.AutoRegister;
import dev.lumas.lumacore.manager.modules.RegisterType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

@AutoRegister(RegisterType.LISTENER)
public class PlayerSessionListener implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        GlowColorManager glowColorManager = GlowColorManager.getInstance();
        Player player = event.getPlayer();
        glowColorManager.playerJoinHook(player);
        glowColorManager.update(player);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        GlowColorManager.getInstance().playerQuitHook(event.getPlayer());
    }

}
