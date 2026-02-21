package me.thatonedevil.mageRPGParty.api.party

import me.thatonedevil.devilLib.bridge.DevilExport
import me.thatonedevil.mageRPGParty.party.Party
import me.thatonedevil.mageRPGParty.party.PartyManager
import java.util.UUID

class PartyAPI {

    @DevilExport
    fun getParty(uuid: UUID): Party? {
        return PartyManager.getParty(uuid)
    }

    @DevilExport
    fun isInParty(uuid: UUID) = PartyManager.getParty(uuid) != null
}

