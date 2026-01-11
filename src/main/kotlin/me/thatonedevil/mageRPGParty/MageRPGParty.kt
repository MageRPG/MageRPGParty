package me.thatonedevil.mageRPGParty

import me.thatonedevil.devilLib.DevilLib
import me.thatonedevil.devilLib.DevilLib.plugin
import me.thatonedevil.devilLib.utils.Logging
import me.thatonedevil.mageRPGParty.commands.MainPartyCommand
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin

class MageRPGParty : JavaPlugin() {

    companion object {
        lateinit var instance: JavaPlugin
            private set
        var log = Logging("MageRPGParty", debug = true)
            private set
        var partyManager = PartyManager
    }

    override fun onEnable() {
        instance = this
        DevilLib.init(this, debug = true)
        log.log("MageRPGParty enabled!")

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            PartyPlaceholder().register()
        }

        // Register commands
        registerCommands()

        // Register listeners
        registerListeners()

    }

    override fun onDisable() {
        PartyManager.cleanup()
    }

    private fun registerCommands() {
        plugin.getCommand("party")?.setExecutor(MainPartyCommand())
    }
    private fun registerListeners() {
        Bukkit.getPluginManager().registerEvents(PartyManager, this)

    }
}
