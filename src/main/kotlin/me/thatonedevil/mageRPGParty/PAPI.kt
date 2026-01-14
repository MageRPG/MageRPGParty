package me.thatonedevil.mageRPGParty

import me.clip.placeholderapi.expansion.PlaceholderExpansion
import me.thatonedevil.mageRPGParty.MageRPGParty.Companion.partyManager
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import java.util.UUID

class PartyPlaceholder : PlaceholderExpansion() {

    // Cache for reordered members to avoid recalculating on every placeholder call
    private data class CacheKey(val partyLeader: UUID, val clientUUID: UUID)
    private val reorderCache = mutableMapOf<CacheKey, List<UUID>>()

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
                val reorderedMembers = getReorderedMembers(party, uuid)
                formatMember(party, reorderedMembers, index, player)
            }

            else -> null
        }
    }

    private fun getReorderedMembers(party: Party, clientUUID: UUID): List<UUID> {
        val cacheKey = CacheKey(party.leader, clientUUID)

        // Return cached result if available and party hasn't changed
        reorderCache[cacheKey]?.let { cached ->
            // Verify cache is still valid (same members)
            if (cached.size == party.members.size && cached.all { it in party.members }) {
                return cached
            }
        }

        // Calculate and cache new ordering
        val reordered = calculateReorderedMembers(party, clientUUID)
        reorderCache[cacheKey] = reordered

        // Clean cache if it gets too large (keep only recent 50 entries)
        if (reorderCache.size > 50) {
            val keysToRemove = reorderCache.keys.take(reorderCache.size - 50)
            keysToRemove.forEach { reorderCache.remove(it) }
        }

        return reordered
    }

    private fun calculateReorderedMembers(party: Party, clientUUID: UUID): List<UUID> {
        val members = party.members
        val leaderUUID = party.leader

        // If client is the leader, no reordering needed
        if (clientUUID == leaderUUID) return members

        // Find the client in the members list
        if (clientUUID !in members) return members // Client not found, return original

        // Reorder: Leader first, then client, then others
        return buildList(members.size) {
            add(leaderUUID)
            add(clientUUID)
            members.forEach { memberUUID ->
                if (memberUUID != leaderUUID && memberUUID != clientUUID) {
                    add(memberUUID)
                }
            }
        }
    }

    private fun formatMember(party: Party, reorderedMembers: List<UUID>, index: Int, client: Player): String {
        if (index >= reorderedMembers.size) return ""

        val memberUUID = reorderedMembers[index]
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