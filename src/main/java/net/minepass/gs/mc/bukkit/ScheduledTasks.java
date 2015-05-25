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

import com.google.common.collect.ImmutableList;
import net.minepass.gs.GameserverTasks;
import net.minepass.gs.mc.MinePassMC;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ScheduledTasks extends BukkitRunnable {

    private final MP_BukkitPlugin plugin;
    private final MinePassMC minepass;
    private final GameserverTasks tasks;

    public ScheduledTasks(MP_BukkitPlugin bp) {
        this.plugin = bp;
        this.minepass = plugin.getMinepass();
        this.tasks = new GameserverTasks(plugin.getMinepass()) {
            @Override
            protected Map<UUID, String> getCurrentPlayers() {
                ImmutableList<Player> bukkitPlayers = ImmutableList.copyOf(plugin.getServer().getOnlinePlayers());
                HashMap<UUID, String> players = new HashMap<>(bukkitPlayers.size());
                for (Player p : bukkitPlayers) {
                    players.put(p.getUniqueId(), p.getName());
                }
                return players;
            }

            @Override
            protected void updateAndReloadLocalWhitelist() {
                minepass.updateLocalWhitelist();
                plugin.getServer().reloadWhitelist();
                plugin.getLogger().info("Whitelist updated");
            }

            @Override
            protected void kickPlayer(UUID playerId, String message) {
                Player p = plugin.getServer().getPlayer(playerId);
                if (p != null) p.kickPlayer(message);
            }

            @Override
            protected void warnPlayer(UUID playerId, String message) {
                Player p = plugin.getServer().getPlayer(playerId);
                if (p != null) {
                    p.sendRawMessage(String.format(
                            "[\"\",{\"text\":\"%s\",\"color\":\"gold\"}]",
                            message
                    ));
                }
            }

            @Override
            protected void warnPlayerPass(UUID playerId, String message) {
                Player p = plugin.getServer().getPlayer(playerId);
                if (p != null) {
                    p.sendRawMessage(String.format(
                            "[\"\",{\"text\":\"%s\",\"color\":\"aqua\",\"clickEvent\":{\"action\":\"open_url\",\"value\":\"%s\"}}]",
                            message.concat(" Click for your World Pass."),
                            minepass.getServer().join_url
                    ));
                }
            }
        };
    }

    @Override
    public void run() {
        tasks.runTasks();
    }
}
