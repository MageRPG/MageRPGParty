package me.thatonedevil.mageRPGParty.commands

import me.thatonedevil.devilLib.utils.Utils.noMessage
import me.thatonedevil.devilLib.utils.Utils.yesMessage
import me.thatonedevil.mageRPGParty.MageRPGParty.Companion.partyManager
import org.bukkit.entity.Player
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.CommandDescription

@Command("party")
@CommandDescription("Party management commands")
object MainPartyCommand {

    private const val TEAM_SIZE = 6

    @Command("create")
    @CommandDescription("Create a new party")
    fun create(player: Player) {
        if (partyManager.createParty(player.uniqueId) == null) {
            player.noMessage("<color:#FF5555>You are already in a <color:#d45252>party!")
            return
        }
        player.yesMessage("<color:#77DD77>Party created <color:#35cd35>successfully!")
    }

    @Command("invite <target>")
    @CommandDescription("Invite a player to your party")
    fun invite(player: Player, @Argument("target") target: Player) {
        if (target.uniqueId == player.uniqueId) {
            player.noMessage("<color:#FF5555>You cannot invite <color:#d45252>yourself!")
            return
        }

        val party = partyManager.getParty(player.uniqueId)

        if (party == null) {
            partyManager.createParty(player.uniqueId) ?: run {
                player.noMessage("<color:#FF5555>Failed to create <color:#d45252>party!")
                return
            }
            player.yesMessage("<color:#77DD77>Party created <color:#35cd35>successfully!")
        } else {
            if (party.leader != player.uniqueId) {
                player.noMessage("<color:#FF5555>You are not the <color:#d45252>leader <color:#FF5555>of the party!")
                return
            }
            if (party.size >= TEAM_SIZE) {
                player.noMessage(
                    "<color:#FF5555>Your party is <color:#d45252>full<color:#FF5555>! " +
                            "Maximum size is <color:#d45252>$TEAM_SIZE <color:#FF5555>players."
                )
                return
            }
        }

        if (!partyManager.inviteToParty(player.uniqueId, target.uniqueId)) {
            player.noMessage(
                "<color:#FF5555>Could not invite player! They may already be in a <color:#d45252>party " +
                        "<color:#FF5555>or have a <color:#d45252>pending invite<color:#FF5555>."
            )
        }
    }

    @Command("accept")
    @CommandDescription("Accept a pending party invite")
    fun accept(player: Player) {
        if (!requirePendingInvite(player)) return

        if (!partyManager.acceptInvite(player.uniqueId)) {
            player.noMessage("<color:#FF5555>Failed to accept invite! It may have <color:#d45252>expired<color:#FF5555>.")
            return
        }
        player.yesMessage("<color:#77DD77>Party invite accepted <color:#35cd35>successfully!")
    }

    @Command("decline")
    @CommandDescription("Decline a pending party invite")
    fun decline(player: Player) {
        if (!requirePendingInvite(player)) return

        if (!partyManager.declineInvite(player.uniqueId)) {
            player.noMessage("<color:#FF5555>Failed to decline invite! It may have <color:#d45252>expired<color:#FF5555>.")
            return
        }
        player.yesMessage("<color:#77DD77>Party invite declined <color:#35cd35>successfully!")
    }

    @Command("leave")
    @CommandDescription("Leave your current party")
    fun leave(player: Player) {
        if (!requireInParty(player)) return

        val party = partyManager.getParty(player.uniqueId) ?: return

        if (party.leader == player.uniqueId) {
            if (!partyManager.disbandParty(player.uniqueId)) return
            player.yesMessage("<color:#77DD77>You have disbanded the party.")
        } else {
            if (!partyManager.leaveParty(player.uniqueId)) return
            player.yesMessage("<color:#77DD77>You have left the party <color:#35cd35>successfully!")
        }
    }

    @Command("kick <target>")
    @CommandDescription("Kick a player from your party")
    fun kick(player: Player, @Argument("target") target: Player) {
        if (!requireInParty(player)) return

        val party = partyManager.getParty(player.uniqueId) ?: return

        if (party.leader != player.uniqueId) {
            player.noMessage("<color:#FF5555>You are not the <color:#d45252>leader <color:#FF5555>of the party!")
            return
        }
        if (party.size <= 2) {
            player.noMessage(
                "<color:#FF5555>Cannot kick from a <color:#d45252>2-person party<color:#FF5555>! " +
                        "Use <color:#d45252>/party disband <color:#FF5555>instead."
            )
            return
        }
        if (target.uniqueId == player.uniqueId) {
            player.noMessage(
                "<color:#FF5555>You cannot kick <color:#d45252>yourself<color:#FF5555>! " +
                        "Use <color:#d45252>/party leave <color:#FF5555>instead."
            )
            return
        }
        if (!party.isMember(target.uniqueId)) {
            player.noMessage("<color:#FF5555>That player is not in your party!")
            return
        }
        if (!partyManager.kickFromParty(player.uniqueId, target.uniqueId)) {
            player.noMessage("<color:#FF5555>Failed to kick player!")
            return
        }

        player.yesMessage("<color:#77DD77>Successfully kicked <color:#35cd35>${target.name} <color:#77DD77>from the party!")
        target.noMessage("<color:#FF5555>You have been kicked from the party!")
    }

    @Command("disband")
    @CommandDescription("Disband your party")
    fun disband(player: Player) {
        if (!requireInParty(player)) return

        if (!partyManager.disbandParty(player.uniqueId)) {
            player.noMessage("<color:#FF5555>You are not the <color:#d45252>leader <color:#FF5555>of the party!")
            return
        }
        player.yesMessage("<color:#77DD77>Party disbanded <color:#35cd35>successfully!")
    }

    @Command("info")
    @CommandDescription("View your party info")
    fun info(player: Player) {
        if (!requireInParty(player)) return
        player.sendMessage(partyManager.partyInfo(player.uniqueId))
    }

    private fun requireInParty(player: Player): Boolean {
        if (!partyManager.isInParty(player.uniqueId)) {
            player.noMessage("<color:#FF5555>You are not in a party! Create one with <color:#d45252>/party create")
            return false
        }
        return true
    }

    private fun requirePendingInvite(player: Player): Boolean {
        if (!partyManager.hasPendingInvite(player.uniqueId)) {
            player.noMessage("<color:#FF5555>You don't have any pending <color:#d45252>party invites!")
            return false
        }
        return true
    }
}