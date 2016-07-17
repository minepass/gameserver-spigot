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

import net.minepass.api.gameserver.MPAsciiArt;
import net.minepass.api.gameserver.MPConfig;
import net.minepass.api.gameserver.MPConfigException;
import net.minepass.api.gameserver.MPStartupException;
import net.minepass.api.gameserver.MPUtil;
import net.minepass.api.gameserver.MPWorldServerDetails;
import net.minepass.api.gameserver.embed.solidtx.TxStack;
import net.minepass.api.gameserver.embed.solidtx.TxSync;
import net.minepass.gs.mc.MinePassMC;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class MP_BukkitPlugin extends JavaPlugin {

    private MinePassMC minepass;
    private Thread syncThread;
    private EventHandler eventHandler;
    private BukkitTask scheduledTasks;

    public MP_BukkitPlugin() {
        super();
        this.eventHandler = new EventHandler(this);
    }

    public MinePassMC getMinepass() {
        return minepass;
    }

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        getLogger().info("Loading MinePass configuration");

        Boolean debug = getConfig().getBoolean("debug_enabled");
        String version = getPlugin(MP_BukkitPlugin.class).getDescription().getVersion();

        try {
            if (debug) {
                TxStack.debug = true;
            }

            MPConfig mtc = new MPConfig();
            mtc.variant = "BukkitPlugin ".concat(version);
            mtc.api_host = getConfig().getString("setup_api_host");
            mtc.server_uuid = getConfig().getString("setup_server_id");
            mtc.server_secret = getConfig().getString("setup_server_secret");

            /**
             * The MinePass network stack is built upon SolidTX, an MIT licensed project
             * developed in collaboration with BinaryBabel OSS.
             *
             * The source code for the MinePass game server stack is available at:
             *   https://github.com/minepass/gameserver-core
             *
             * The source code and documentation for SolidTX is available at:
             *   https://github.com/org-binbab/solid-tx
             *
             */
            this.minepass = new MinePassMC(mtc);
            minepass.setContext(this);

            getLogger().info("MinePass Core Version: " + minepass.getVersion());
            getLogger().info("MinePass API Endpoint: " + mtc.api_host);
            getLogger().info("MinePass World Server UUID: " + minepass.getServerUUID());
        } catch (MPConfigException e) {
            e.printStackTrace();
            for (String x : MPAsciiArt.getNotice("Configuration Update Required")) {
                getLogger().info(x);
            }
            getLogger().warning("Run the server configuration wizard at http://minepass.net");
            getLogger().warning("Then paste the configuration into plugins/MinePass/config.yml");
            getServer().shutdown();
            return;
        } catch (MPStartupException e) {
            e.printStackTrace();
            return;
        }

        // This is the post-start separation in Forge.
        // ----------------------------------------------------------------------------------------------------- //

        getLogger().info("Requiring whitelist enabled.");
        getServer().reloadWhitelist();
        getServer().setWhitelist(true);

        // Register event handler.
        getServer().getPluginManager().registerEvents(eventHandler, this);

        // Start sync thread.
        syncThread = new Thread(new TxSync(minepass, 10));
        syncThread.setDaemon(false);  // ensure any disk writing finishes
        syncThread.start();
        for (String x : MPAsciiArt.getLogo("System Ready")) {
            getLogger().info(x);
        }

        // Start scheduled tasks.
        int refreshTicks = 20 * 2; // 2 seconds.
        this.scheduledTasks = new ScheduledTasks(this).runTaskTimer(this, refreshTicks, refreshTicks);

        // Send server details.
        MPWorldServerDetails details = new MPWorldServerDetails();
        details.plugin_type = "mc-bukkit";
        details.plugin_version = version;
        details.game_realm = "mc";
        details.game_version = MPUtil.parseVersion(getServer().getVersion());
        details.game_version_raw = getServer().getVersion();
        for (Plugin p : getServer().getPluginManager().getPlugins()) {
            details.addPlugin(p.getName(), p.getDescription().getVersion(), p.getDescription().getMain());
        }
        if (!minepass.getServer().whitelist_imported) {
            details.importWhitelist(MinePassMC.whitelistBackupFile);
        }
        minepass.sendObject(details, null);
    }

    @Override
    public void onDisable() {
        if (syncThread != null) {
            syncThread.interrupt();
            syncThread = null;
        }

        if (scheduledTasks != null) {
            scheduledTasks.cancel();
            scheduledTasks = null;
        }

        this.minepass = null;
    }

}
