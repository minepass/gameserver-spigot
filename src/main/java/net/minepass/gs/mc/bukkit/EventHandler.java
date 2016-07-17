/*
 *  This file is part of MinePass, licensed under the MIT License (MIT).
 *
 *  Copyright (c) MinePass.net <http://www.minepass.net>
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */

package net.minepass.gs.mc.bukkit;

import net.minepass.api.gameserver.MPPlayer;
import net.minepass.api.gameserver.MPWorldServer;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class EventHandler implements Listener {

    private MP_BukkitPlugin plugin;

    public EventHandler(MP_BukkitPlugin plugin) {
        this.plugin = plugin;
    }

    @org.bukkit.event.EventHandler
    public void onLogin(PlayerJoinEvent event) {
        Player bukkitPlayer = (Player) event.getPlayer();
        MPPlayer player = plugin.getMinepass().getPlayer(bukkitPlayer.getUniqueId());

        if (player != null) {
            MPWorldServer server = plugin.getMinepass().getServer();
            GameMode minecraftGameMode = null;
            Pattern privPattern = Pattern.compile("mc:(?<name>[a-z]+)");

            Matcher pm;
            for (String p : player.privileges) {
                pm = privPattern.matcher(p);
                if (pm.find()) {
                    switch (pm.group("name")) {
                        case "survival":
                            minecraftGameMode = GameMode.SURVIVAL;
                            break;
                        case "creative":
                            minecraftGameMode = GameMode.CREATIVE;
                            break;
                        case "adventure":
                            minecraftGameMode = GameMode.ADVENTURE;
                            break;
                        case "spectator":
                            minecraftGameMode = GameMode.SPECTATOR;
                            break;
                    }
                }
            }

            if (minecraftGameMode != null) {
                if (!bukkitPlayer.getGameMode().equals(minecraftGameMode)) {
                    bukkitPlayer.setGameMode(minecraftGameMode);
                }
            } else {
                bukkitPlayer.kickPlayer("Your current MinePass does not permit access to this server.");
            }
        }
    }

}
