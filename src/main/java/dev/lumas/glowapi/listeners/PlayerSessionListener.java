package dev.lumas.glowapi.listeners;

import dev.lumas.glowapi.GlowColorManager;
import dev.lumas.lumacore.manager.modules.AutoRegister;
import dev.lumas.lumacore.manager.modules.RegisterType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

@AutoRegister(RegisterType.LISTENER)
public class PlayerSessionListener implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        GlowColorManager.getInstance().playerJoinHook(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        GlowColorManager.getInstance().playerQuitHook(event.getPlayer());
    }

}
