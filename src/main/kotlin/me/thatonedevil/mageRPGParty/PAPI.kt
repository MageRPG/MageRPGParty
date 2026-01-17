package me.thatonedevil.mageRPGParty

import me.clip.placeholderapi.expansion.PlaceholderExpansion
import me.thatonedevil.mageRPGParty.MageRPGParty.Companion.partyManager
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import java.util.UUID

class PartyPlaceholder : PlaceholderExpansion() {

    override fun getIdentifier(): String = "party"

    override fun getAuthor(): String = "ThatOneDevil"

    override fun getVersion(): String = "1.0.0"

    override fun persist(): Boolean = true

    override fun onRequest(player: OfflinePlayer?, params: String): String? {
        if (player == null || !player.isOnline) return null

        val uuid = player.uniqueId
        val player = player as Player

        return when (params.lowercase()) {
            "in_party" -> {
                if (!partyManager.isInParty(uuid)) "/party create" else ""
            }

            "member_1", "member_2", "member_3", "member_4", "member_5", "member_6" -> {
                val party = partyManager.getPartyByMember(uuid) ?: return ""
                val index = params.last().digitToInt() - 1
                formatMember(party, index, player)
            }

            else -> null
        }
    }

    private fun formatMember(party: Party, index: Int, client: Player): String {
        if (index >= party.members.size) return ""

        val memberUUID = party.members[index]
        val memberPlayer = Bukkit.getPlayer(memberUUID)
        val isClient = memberPlayer?.name == client.name
        val isLeader = memberUUID == party.leader

        val name = if (isClient) {
            "<#FF6961><shadow:black:0.75>${memberPlayer.name}"
        } else {
            "<#FFC067><shadow:black:0.75>${memberPlayer?.name}"
        }

        val role = if (isLeader) " &6[Leader]" else ""
        val health = memberPlayer?.health?.div(2)?.toInt() ?: 0

        return "&a● $name$role &c$health %animation:heart%"
    }

}


fun PartyManager.getPartyByMember(memberUUID: UUID): Party? {
    val memberToPartyMap = this::class.java.getDeclaredField("memberToParty")
        .apply { isAccessible = true }
        .get(this) as MutableMap<UUID, UUID>

    val leaderUUID = memberToPartyMap[memberUUID] ?: return null
    return getParty(leaderUUID)
}