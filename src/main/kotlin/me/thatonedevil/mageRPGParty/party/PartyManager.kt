package me.thatonedevil.mageRPGParty.party

import me.thatonedevil.devilLib.utils.Utils.noMessage
import me.thatonedevil.devilLib.utils.Utils.toMiniMessage
import me.thatonedevil.devilLib.utils.Utils.yesMessage
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.util.UUID

data class Party(
    val id: UUID,
    val leader: UUID = id,
    val members: MutableList<UUID> = mutableListOf(leader)
) {
    fun isMember(uuid: UUID): Boolean = members.contains(uuid)

    fun addMember(uuid: UUID): Boolean {
        if (isMember(uuid)) return false
        members.add(uuid)
        return true
    }

    fun removeMember(uuid: UUID): Boolean = members.remove(uuid)

    val size: Int get() = members.size
}

object PartyManager : Listener {

    private val parties: MutableMap<UUID, Party> = mutableMapOf()
    private val memberToParty: MutableMap<UUID, UUID> = mutableMapOf()
    val pendingInvites: MutableMap<UUID, PartyInvite> = mutableMapOf()

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val uuid = event.player.uniqueId

        removePendingInvite(uuid)

        val party = getParty(uuid) ?: return

        if (party.leader == uuid) {
            disbandParty(uuid)
        } else {
            leaveParty(uuid)
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerDamageEvent(event: EntityDamageByEntityEvent) {
        val victim = event.entity as? Player ?: return
        val attacker = event.damager as? Player ?: return

        val victimParty = getParty(victim.uniqueId) ?: return

        if (victimParty.isMember(attacker.uniqueId)) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onPlayerRightClick(event: PlayerInteractEntityEvent) {
        val inviter = event.player
        val clicked = event.rightClicked as? Player ?: return

        if (!inviter.isSneaking) return

        val party = getParty(inviter.uniqueId) ?: return

        if (party.leader != inviter.uniqueId) return
        if (party.isMember(clicked.uniqueId)) return
        if (isInParty(clicked.uniqueId)) return

        inviteToParty(party.leader, clicked.uniqueId)
    }

    fun isInParty(uuid: UUID): Boolean = memberToParty.containsKey(uuid)

    fun hasPendingInvite(uuid: UUID): Boolean = pendingInvites.containsKey(uuid)

    fun getParty(uuid: UUID): Party? {
        val leaderUUID = memberToParty[uuid] ?: return null
        return parties[leaderUUID]
    }

    fun removePendingInvite(uuid: UUID) {
        pendingInvites.remove(uuid)?.cancel()
    }

    fun createParty(leader: UUID): Party? {
        if (isInParty(leader)) return null

        val party = Party(id = leader)
        parties[leader] = party
        memberToParty[leader] = leader
        return party
    }

    fun disbandParty(leader: UUID): Boolean {
        val party = parties[leader] ?: return false
        if (party.leader != leader) return false

        sendPartyNoChat(party, "<color:#FF5555>The party has been <color:#d45252>disbanded <color:#FF5555>by the leader!")

        party.members.forEach { memberToParty.remove(it) }
        parties.remove(leader)

        // Clean up any pending invites for this party
        pendingInvites.entries.removeIf { it.value.party.leader == leader }

        return true
    }

    fun leaveParty(member: UUID): Boolean {
        val party = getParty(member) ?: return false
        if (party.leader == member) return false

        val memberName = Bukkit.getPlayer(member)?.name ?: "Unknown"
        Bukkit.getPlayer(party.leader)?.noMessage("<color:#d45252>$memberName <color:#FF5555>has left the party.")

        party.removeMember(member)
        memberToParty.remove(member)

        return true
    }

    fun kickFromParty(leader: UUID, member: UUID): Boolean {
        val party = parties[leader] ?: return false

        // Validation checks
        if (party.leader != leader) return false
        if (leader == member) return false
        if (!party.isMember(member)) return false

        val memberName = Bukkit.getPlayer(member)?.name ?: "Unknown"

        party.removeMember(member)
        memberToParty.remove(member)

        sendPartyNoChat(party, "<color:#d45252>$memberName <color:#FF5555>has been <color:#d45252>kicked <color:#FF5555>from the party.")

        return true
    }

    fun inviteToParty(leader: UUID, member: UUID): Boolean {
        val party = parties[leader] ?: return false

        // Validation checks
        if (isInParty(member)) return false
        if (hasPendingInvite(member)) return false

        val invite = PartyInvite(party, member)
        pendingInvites[member] = invite

        val leaderPlayer = Bukkit.getPlayer(party.leader)
        val memberPlayer = Bukkit.getPlayer(member)

        leaderPlayer?.yesMessage("<color:#77DD77>Party invite sent to <color:#35cd35>${memberPlayer?.name}<color:#77DD77>!")

        val inviteMessage = Component.text()
            .append("<color:#77DD77>You have been invited to <color:#35cd35>${leaderPlayer?.name}<color:#77DD77>'s party! ".toMiniMessage())
            .append(
                "<color:#35cd35>/party accept".toMiniMessage()
                    .clickEvent(ClickEvent.runCommand("/party accept"))
                    .hoverEvent(HoverEvent.showText("<color:#35cd35>Click to accept the party invite".toMiniMessage()))
            )
            .append(" <color:#77DD77>or ".toMiniMessage())
            .append(
                "<color:#FF5555>/party decline".toMiniMessage()
                    .clickEvent(ClickEvent.runCommand("/party decline"))
                    .hoverEvent(HoverEvent.showText("<color:#FF5555>Click to decline the party invite".toMiniMessage()))
            )
            .build()

        memberPlayer?.sendMessage(inviteMessage)

        return true
    }

    fun acceptInvite(member: UUID): Boolean {
        val invite = pendingInvites[member] ?: return false

        if (invite.status != InviteStatus.PENDING) {
            removePendingInvite(member)
            return false
        }

        invite.accept()
        memberToParty[member] = invite.party.leader
        removePendingInvite(member)

        return true
    }

    fun declineInvite(member: UUID): Boolean {
        val invite = pendingInvites[member] ?: return false

        if (invite.status != InviteStatus.PENDING) {
            removePendingInvite(member)
            return false
        }

        invite.decline()
        removePendingInvite(member)

        return true
    }

    fun partyInfo(playerUUID: UUID): Component {
        val builder = Component.text()
        val party = getParty(playerUUID) ?: return "<color:#FF5555>You are not in a party!".toMiniMessage()

        val leader = Bukkit.getPlayer(party.leader)
        builder.append("<color:#77DD77>Party Leader: <color:#35cd35>${leader?.name}\n".toMiniMessage())
        builder.append("<color:#77DD77>Members:\n".toMiniMessage())

        party.members.forEach { memberUUID ->
            val member = Bukkit.getPlayer(memberUUID)
            builder.append("<white>- <color:#35cd35>${member?.name}\n".toMiniMessage())
        }

        return builder.build()
    }

    private fun sendPartyNoChat(party: Party, message: String) {
        party.members
            .filter { it != party.leader }
            .mapNotNull { Bukkit.getPlayer(it) }
            .forEach { it.noMessage(message) }
    }

    fun cleanup() {
        pendingInvites.values.forEach { it.cancel() }
        pendingInvites.clear()
        parties.clear()
        memberToParty.clear()
    }
}