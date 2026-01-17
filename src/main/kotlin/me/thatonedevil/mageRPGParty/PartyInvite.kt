package me.thatonedevil.mageRPGParty

import me.thatonedevil.devilLib.utils.Utils.noMessage
import me.thatonedevil.devilLib.utils.Utils.yesMessage
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

        leader?.yesMessage("<color:#35cd35>${member?.name} <color:#77DD77>joined the party!")

        PartyManager.removePendingInvite(invitedMember)
    }

    fun decline() {
        if (status != InviteStatus.PENDING) return

        status = InviteStatus.DECLINED
        expirationTask?.cancel()
        val leader = Bukkit.getPlayer(party.leader)
        val member = Bukkit.getPlayer(invitedMember)

        leader?.noMessage("<color:#d45252>${member?.name} <color:#FF5555>declined your party invite.")

        PartyManager.removePendingInvite(invitedMember)
    }

    private fun expire() {
        if (status != InviteStatus.PENDING) return

        status = InviteStatus.EXPIRED
        val leader = Bukkit.getPlayer(party.leader)
        val member = Bukkit.getPlayer(invitedMember)

        leader?.noMessage("<color:#FF5555>Party invite to <color:#d45252>${member?.name} <color:#FF5555>has <color:#d45252>expired<color:#FF5555>.")
        member?.noMessage("<color:#FF5555>Your party invite has <color:#d45252>expired<color:#FF5555>.")

        PartyManager.removePendingInvite(invitedMember)
    }

    fun cancel() {
        expirationTask?.cancel()
    }

}