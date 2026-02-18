package me.thatonedevil.mageRPGParty.api

import me.thatonedevil.devilLib.bridge.DevilExport
import me.thatonedevil.mageRPGParty.party.Party
import me.thatonedevil.mageRPGParty.party.PartyManager
import java.util.UUID


class PartyAPI {

    @DevilExport
    fun getParty(uuid: UUID): Party? {
        return PartyManager.getParty(uuid)
    }

    @DevilExport(name = "isInParty") // explicit name
    fun isInParty(uuid: UUID): Boolean {
        return PartyManager.getParty(uuid) != null
    }
}