package me.thatonedevil.mageRPGParty

import me.thatonedevil.devilLib.DevilLib
import me.thatonedevil.mageRPGParty.api.PartyAPI
import me.thatonedevil.mageRPGParty.api.PartyPlaceholder
import me.thatonedevil.mageRPGParty.commands.MainPartyCommand
import me.thatonedevil.mageRPGParty.party.PartyManager
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin

class MageRPGParty : JavaPlugin() {

    companion object {
        lateinit var instance: MageRPGParty
            private set
        var partyManager = PartyManager
    }

    override fun onEnable() {
        instance = this

        DevilLib.bridge.register("MageRPGParty", PartyAPI())

        DevilLib.logger.log("MageRPGParty enabled!")

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            PartyPlaceholder().register()
        }

        registerCommands()
        registerListeners()
    }

    override fun onDisable() {
        PartyManager.cleanup()
    }

    private fun registerCommands() {
        getCommand("party")?.setExecutor(MainPartyCommand())
    }

    private fun registerListeners() {
        Bukkit.getPluginManager().registerEvents(PartyManager, this)
    }
}
