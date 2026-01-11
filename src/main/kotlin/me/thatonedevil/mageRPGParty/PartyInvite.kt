package me.thatonedevil.mageRPGParty

import me.thatonedevil.devilLib.utils.Utils.sendChat
import org.bukkit.Bukkit
import org.bukkit.scheduler.BukkitTask
import java.util.UUID

enum class InviteStatus {
    PENDING,
    ACCEPTED,
    DECLINED,
    EXPIRED
}

class PartyInvite(val party: Party, val invitedMember: UUID) {
    var status: InviteStatus = InviteStatus.PENDING
        private set

    private var expirationTask: BukkitTask? = null

    init {
        startExpirationTimer()
    }

    private fun startExpirationTimer() {
        expirationTask = Bukkit.getScheduler().runTaskLater(
            MageRPGParty.instance,
            Runnable {
                if (status == InviteStatus.PENDING) {
                    expire()
                }
            },
            600L // 30 seconds
        )
    }

    fun accept() {
        if (status != InviteStatus.PENDING) return

        status = InviteStatus.ACCEPTED
        expirationTask?.cancel()

        party.addMember(invitedMember)
        val leader = Bukkit.getPlayer(party.leader)
        val member = Bukkit.getPlayer(invitedMember)

        // Notify players
        member?.sendChat("<color:#35cd35>You <color:#77DD77>joined the party!")
        leader?.sendChat("<color:#35cd35>${member?.name} <color:#77DD77>joined the party!")

        PartyManager.removePendingInvite(invitedMember)
    }

    fun decline() {
        if (status != InviteStatus.PENDING) return

        status = InviteStatus.DECLINED
        expirationTask?.cancel()
        val leader = Bukkit.getPlayer(party.leader)
        val member = Bukkit.getPlayer(invitedMember)

        // Notify players
        leader?.sendChat("<color:#cd3535>${member?.name} <color:#DD7777>declined your party invite.")
        member?.sendChat("<color:#cd3535>You <color:#DD7777>declined the party invite.")

        PartyManager.removePendingInvite(invitedMember)
    }

    private fun expire() {
        if (status != InviteStatus.PENDING) return

        status = InviteStatus.EXPIRED
        val leader = Bukkit.getPlayer(party.leader)
        val member = Bukkit.getPlayer(invitedMember)

        // Notify players
        leader?.sendChat("<color:#DD7777>Party invite to <color:#cd3535>${member?.name} has <color:#cd3535>expired.")
        member?.sendChat("<color:#DD7777>Your party invite has <color:#cd3535>expired.")

        // Remove from pending invites
        PartyManager.removePendingInvite(invitedMember)
    }

    fun cancel() {
        expirationTask?.cancel()
    }

}