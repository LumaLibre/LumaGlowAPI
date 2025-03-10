package dev.jsinco.lumaglowapi;

import dev.jsinco.luma.lumacore.manager.modules.AutoRegister;
import dev.jsinco.luma.lumacore.manager.modules.RegisterType;
import dev.jsinco.lumaglowapi.colormanagers.ColorManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

@AutoRegister(RegisterType.LISTENER)
public class Listeners implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        ColorManager.updatePlayersColor(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        ColorManager.clearPlayerColor(event.getPlayer());
    }
}
