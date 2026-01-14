package me.thatonedevil.mageRPGParty

import me.thatonedevil.devilLib.utils.Utils.sendChat
import me.thatonedevil.devilLib.utils.Utils.toMiniMessage
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
){
    fun isMember(uuid: UUID): Boolean {
        return members.contains(uuid)
    }
    fun addMember(uuid: UUID): Boolean {
        if (isMember(uuid)) return false
        members.add(uuid)
        return true
    }
    fun removeMember(uuid: UUID): Boolean {
        members.remove(uuid)
        return true
    }
}

object PartyManager : Listener {

    private val parties: MutableMap<UUID, Party> = mutableMapOf()
    private val memberToParty: MutableMap<UUID, UUID> = mutableMapOf()
    val pendingInvites: MutableMap<UUID, PartyInvite> = mutableMapOf()

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val player = event.player
        val uuid = player.uniqueId

        // Remove pending invite if they have one
        removePendingInvite(uuid)

        // Check if they're in a party
        if (!isInParty(uuid)) return

        val leaderUUID = memberToParty[uuid]
        val party = parties[leaderUUID] ?: return

        // If the player is the leader, disband the party
        if (party.leader == uuid) {
            disbandParty(uuid)
        } else {
            // If they're a member, remove them from the party
            leaveParty(uuid)
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerDamageEvent(event: EntityDamageByEntityEvent) {
        val victim = event.entity
        val attacker = event.damager
        if (victim !is Player) return
        if (attacker !is Player) return

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

        if (party.leader != inviter.uniqueId) {
            return
        }

        if (party.isMember(clicked.uniqueId)) return
        if (isInParty(clicked.uniqueId)) return

        inviteToParty(party.leader, clicked.uniqueId)
    }


    fun isInParty(uuid: UUID): Boolean {
        return memberToParty.containsKey(uuid)
    }

    fun hasPendingInvite(uuid: UUID): Boolean {
        return pendingInvites.containsKey(uuid)
    }

    fun getParty(uuid: UUID): Party? {
        val leaderUUID = memberToParty[uuid] ?: return null
        return parties[leaderUUID]
    }

    fun removePendingInvite(uuid: UUID) {
        pendingInvites[uuid]?.cancel()
        pendingInvites.remove(uuid)
    }

    fun createParty(leader: UUID): Party? {
        if (memberToParty.containsKey(leader)) {
            return null
        }

        val party = Party(id = leader)
        parties[leader] = party
        memberToParty[leader] = leader
        return party
    }


    fun disbandParty(leader: UUID): Boolean {
        val party = parties[leader] ?: return false

        if (party.leader != leader) return false

        sendPartyChat(party, "<color:#FF5555>The party has been <color:#d45252>disbanded <color:#FF5555>by the leader!")

        party.members.forEach { memberToParty.remove(it) }

        parties.remove(leader)

        pendingInvites.entries.removeIf { it.value.party.leader == leader }

        return true
    }

    fun leaveParty(member: UUID): Boolean {
        val leaderUUID = memberToParty[member] ?: return false
        val party = parties[leaderUUID] ?: return false

        if (party.leader == member) {
            return false
        }

        Bukkit.getPlayer(party.leader)?.sendChat("<color:#d45252>${Bukkit.getPlayer(member)?.name} <color:#FF5555>has left the party.")
        party.removeMember(member)
        memberToParty.remove(member)
        return true
    }

    fun kickFromParty(leader: UUID, member: UUID): Boolean {
        val party = parties[leader] ?: return false

        // Only the leader can kick
        if (party.leader != leader) {
            return false
        }

        // Cannot kick yourself
        if (leader == member) {
            return false
        }

        // Check if the member is in the party
        if (!party.isMember(member)) {
            return false
        }

        // Remove the member
        party.removeMember(member)
        memberToParty.remove(member)

        sendPartyChat(party, "<color:#d45252>${Bukkit.getPlayer(member)?.name} <color:#FF5555>has been <color:#d45252>kicked <color:#FF5555>from the party.")

        return true
    }

    fun inviteToParty(leader: UUID, member: UUID): Boolean {
        val party = parties[leader] ?: return false

        if (isInParty(member)) {
            return false
        }

        if (hasPendingInvite(member)) {
            return false
        }

        val invite = PartyInvite(party, member)
        pendingInvites[member] = invite

        val leaderPlayer = Bukkit.getPlayer(party.leader)
        val memberPlayer = Bukkit.getPlayer(member)
        leaderPlayer?.sendChat("<color:#77DD77>Party invite sent to <color:#35cd35>${memberPlayer?.name}<color:#77DD77>!")

        val builder = Component.text()
            .append("<color:#77DD77>You have been invited to <color:#35cd35>${leaderPlayer?.name}<color:#77DD77>'s party! ".toMiniMessage()

            .append("<color:#77DD77>Use <color:#35cd35>/party accept".toMiniMessage())
                .clickEvent(ClickEvent.runCommand("/party accept")))
                .hoverEvent(HoverEvent.showText("<color:#35cd35>Click to accept the party invite".toMiniMessage()))

            .append(" <color:#77DD77>or ".toMiniMessage())

            .append("<color:#FF5555>/party decline".toMiniMessage()
                .clickEvent(ClickEvent.runCommand("/party decline"))
                .hoverEvent(HoverEvent.showText("<color:#FF5555>Click to decline the party invite".toMiniMessage())))

        memberPlayer?.sendMessage(builder.build())

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

    fun partyInfo(): Component {
        val builder = Component.text()
        parties.values.forEach { party ->
            val leader = Bukkit.getPlayer(party.leader)
            builder.append("\n<color:#77DD77>Party Leader: <color:#35cd35>${leader?.name}\n".toMiniMessage())
            builder.append("<color:#77DD77>Members:\n".toMiniMessage())
            party.members.forEach { memberUUID ->
                val member = Bukkit.getPlayer(memberUUID)
                builder.append("<white>- <color:#35cd35>${member?.name}\n".toMiniMessage())
            }
        }
        return builder.build()
    }

    fun sendPartyChat(party: Party, message: String) {
        party.members
            .filter { it != party.leader }
            .mapNotNull { Bukkit.getPlayer(it) }
            .forEach { it.sendChat(message) }
    }

    fun cleanup() {
        // Cancel all pending invites
        pendingInvites.values.forEach { invite ->
            invite.cancel()
        }
        pendingInvites.clear()

        // Clear all party data
        parties.clear()
        memberToParty.clear()
    }
}