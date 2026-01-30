package me.thatonedevil.mageRPGParty

import org.bukkit.entity.Player
import java.util.UUID

object PartySkriptReflect {

    @JvmStatic
    val partyManager: PartyManager = MageRPGParty.partyManager

    @JvmStatic
    fun getParty(player: Player): Party? {
        return partyManager.getParty(player.uniqueId)
    }

    @JvmStatic
    fun getPartyMembers(player: Player): List<UUID>? {
        val party = getParty(player) ?: return null
        return party.members
    }
}