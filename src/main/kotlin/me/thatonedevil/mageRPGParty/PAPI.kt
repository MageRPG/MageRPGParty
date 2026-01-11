package me.thatonedevil.mageRPGParty

import me.clip.placeholderapi.expansion.PlaceholderExpansion
import me.thatonedevil.mageRPGParty.MageRPGParty.Companion.partyManager
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer

class PartyPlaceholder : PlaceholderExpansion() {

    override fun getIdentifier(): String = "party"

    override fun getAuthor(): String = "ThatOneDevil"

    override fun getVersion(): String = "1.0.0"

    override fun persist(): Boolean = true

    override fun onRequest(player: OfflinePlayer?, params: String): String? {
        if (player == null || !player.isOnline) return null

        val uuid = player.uniqueId

        return when (params.lowercase()) {
            "in_party" -> {
                if (!partyManager.isInParty(uuid)) "/party create" else ""
            }

            "member_1" -> {
                val party = partyManager.getPartyByMember(uuid) ?: return ""
                formatMember(party, 0)
            }

            "member_2" -> {
                val party = partyManager.getPartyByMember(uuid) ?: return ""
                formatMember(party, 1)
            }

            "member_3" -> {
                val party = partyManager.getPartyByMember(uuid) ?: return ""
                formatMember(party, 2)
            }

            "member_4" -> {
                val party = partyManager.getPartyByMember(uuid) ?: return ""
                formatMember(party, 3)
            }

            "member_5" -> {
                val party = partyManager.getPartyByMember(uuid) ?: return ""
                formatMember(party, 4)
            }

            "member_6" -> {
                val party = partyManager.getPartyByMember(uuid) ?: return ""
                formatMember(party, 5)
            }

            else -> null
        }
    }

    private fun formatMember(party: Party, index: Int): String {
        if (index >= party.members.size) return ""

        val memberUUID = party.members[index]
        val player = Bukkit.getPlayer(memberUUID)
        val name = player?.name ?: "Unknown"
        val isLeader = memberUUID == party.leader

        val status = "&a●"
        val role = if (isLeader) " &6[Leader]" else ""
        val health = player?.health?.div(2)?.toInt() ?: 0

        return "$status &f$name$role &c$health %animation:heart%"
    }

}


fun PartyManager.getPartyByMember(memberUUID: java.util.UUID): Party? {
    val memberToPartyMap = this::class.java.getDeclaredField("memberToParty")
        .apply { isAccessible = true }
        .get(this) as MutableMap<java.util.UUID, java.util.UUID>

    val leaderUUID = memberToPartyMap[memberUUID] ?: return null
    return getParty(leaderUUID)
}