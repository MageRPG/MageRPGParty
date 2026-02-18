package me.thatonedevil.mageRPGParty.commands

import me.thatonedevil.devilLib.utils.Utils.noMessage
import me.thatonedevil.devilLib.utils.Utils.yesMessage
import me.thatonedevil.mageRPGParty.MageRPGParty.Companion.partyManager
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class MainPartyCommand : CommandExecutor, TabCompleter {

    companion object {
        const val TEAM_SIZE = 6
    }

    private val subcommands = listOf("create", "invite", "accept", "decline", "leave", "kick", "disband", "info")

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        val player = sender as? Player ?: return false
        val subcommand = args.getOrNull(0)?.lowercase() ?: return showUsage(player)

        when (subcommand) {
            "create" -> handleCreate(player)
            "invite" -> handleInvite(player, args)
            "accept" -> handleAccept(player)
            "decline" -> handleDecline(player)
            "leave" -> handleLeave(player)
            "kick" -> handleKick(player, args)
            "disband" -> handleDisband(player)
            "info" -> handleInfo(player)
            else -> showUsage(player)
        }

        return true
    }

    private fun handleCreate(player: Player) {
        if (partyManager.createParty(player.uniqueId) == null) {
            player.noMessage("<color:#FF5555>You are already in a <color:#d45252>party!")
        } else {
            player.yesMessage("<color:#77DD77>Party created <color:#35cd35>successfully!")
        }
    }

    private fun handleInvite(player: Player, args: Array<out String>) {
        if (args.size < 2) {
            player.noMessage("<color:#FF5555>Usage: <color:#d45252>/party invite <player>")
            return
        }

        val targetPlayer = Bukkit.getPlayer(args[1]) ?: run {
            player.noMessage("<color:#FF5555>Player not <color:#d45252>found!")
            return
        }

        if (targetPlayer.uniqueId == player.uniqueId) {
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

            if (!partyManager.inviteToParty(player.uniqueId, targetPlayer.uniqueId)) {
                player.noMessage("<color:#FF5555>Could not invite player! They may already be in a <color:#d45252>party <color:#FF5555>or have a <color:#d45252>pending invite<color:#FF5555>.")
            }
            return
        }

        if (party.leader != player.uniqueId) {
            player.noMessage("<color:#FF5555>You are not the <color:#d45252>leader <color:#FF5555>of the party!")
            return
        }

        if (party.size >= TEAM_SIZE) {
            player.noMessage("<color:#FF5555>Your party is <color:#d45252>full<color:#FF5555>! Maximum size is <color:#d45252>$TEAM_SIZE <color:#FF5555>players.")
            return
        }

        if (!partyManager.inviteToParty(player.uniqueId, targetPlayer.uniqueId)) {
            player.noMessage("<color:#FF5555>Could not invite player! They may already be in a <color:#d45252>party <color:#FF5555>or have a <color:#d45252>pending invite<color:#FF5555>.")
        }
    }

    private fun handleAccept(player: Player) {
        if (!requirePendingInvite(player)) return

        if (partyManager.acceptInvite(player.uniqueId)) {
            player.yesMessage("<color:#77DD77>Party invite accepted <color:#35cd35>successfully!")
        } else {
            player.noMessage("<color:#FF5555>Failed to accept invite! It may have <color:#d45252>expired<color:#FF5555>.")
        }
    }

    private fun handleDecline(player: Player) {
        if (!requirePendingInvite(player)) return

        if (partyManager.declineInvite(player.uniqueId)) {
            player.yesMessage("<color:#77DD77>Party invite declined <color:#35cd35>successfully!")
        } else {
            player.noMessage("<color:#FF5555>Failed to decline invite! It may have <color:#d45252>expired<color:#FF5555>.")
        }
    }

    private fun handleLeave(player: Player) {
        if (!requireInParty(player)) return

        val party = partyManager.getParty(player.uniqueId) ?: return

        if (party.leader == player.uniqueId) {
            // Automatically disband if leader leaves
            if (partyManager.disbandParty(player.uniqueId)) {
                player.yesMessage("<color:#77DD77>You have disbanded the party.")
            }
        } else {
            // Regular member leaving
            if (partyManager.leaveParty(player.uniqueId)) {
                player.yesMessage("<color:#77DD77>You have left the party <color:#35cd35>successfully!")
            }
        }
    }

    private fun handleKick(player: Player, args: Array<out String>) {
        if (!requireInParty(player)) return

        if (args.size < 2) {
            player.noMessage("<color:#FF5555>Usage: <color:#d45252>/party kick <player>")
            return
        }

        val party = partyManager.getParty(player.uniqueId) ?: run {
            player.noMessage("<color:#FF5555>You are not in a party!")
            return
        }

        // Validation checks
        if (party.leader != player.uniqueId) {
            player.noMessage("<color:#FF5555>You are not the <color:#d45252>leader <color:#FF5555>of the party!")
            return
        }

        if (party.size <= 2) {
            player.noMessage("<color:#FF5555>Cannot kick from a <color:#d45252>2-person party<color:#FF5555>! Use <color:#d45252>/party disband <color:#FF5555>instead.")
            return
        }

        val targetPlayer = Bukkit.getPlayer(args[1]) ?: run {
            player.noMessage("<color:#FF5555>Player not <color:#d45252>found!")
            return
        }

        if (targetPlayer.uniqueId == player.uniqueId) {
            player.noMessage("<color:#FF5555>You cannot kick <color:#d45252>yourself<color:#FF5555>! Use <color:#d45252>/party leave <color:#FF5555>instead.")
            return
        }

        if (!party.isMember(targetPlayer.uniqueId)) {
            player.noMessage("<color:#FF5555>That player is not in your party!")
            return
        }

        // Kick the player
        if (partyManager.kickFromParty(player.uniqueId, targetPlayer.uniqueId)) {
            player.yesMessage("<color:#77DD77>Successfully kicked <color:#35cd35>${targetPlayer.name} <color:#77DD77>from the party!")
            targetPlayer.noMessage("<color:#FF5555>You have been kicked from the party!")
        } else {
            player.noMessage("<color:#FF5555>Failed to kick player!")
        }
    }

    private fun handleDisband(player: Player) {
        if (!requireInParty(player)) return

        if (partyManager.disbandParty(player.uniqueId)) {
            player.yesMessage("<color:#77DD77>Party disbanded <color:#35cd35>successfully!")
        } else {
            player.noMessage("<color:#FF5555>You are not the <color:#d45252>leader <color:#FF5555>of the party!")
        }
    }

    private fun handleInfo(player: Player) {
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

    private fun showUsage(player: Player): Boolean {
        player.noMessage("<color:#FF5555>Usage: <color:#d45252>/party <${subcommands.joinToString(", ")}>")
        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): List<String> {
        return when (args.size) {
            1 -> subcommands.filter { it.startsWith(args[0].lowercase()) }
            2 -> when (args[0].lowercase()) {
                "invite" -> getOnlinePlayerNames(args[1])
                "kick" -> getPartyMemberNames(sender as? Player, args[1])
                else -> emptyList()
            }
            else -> emptyList()
        }
    }

    private fun getOnlinePlayerNames(prefix: String): List<String> {
        return Bukkit.getOnlinePlayers()
            .map { it.name }
            .filter { it.lowercase().startsWith(prefix.lowercase()) }
    }

    private fun getPartyMemberNames(player: Player?, prefix: String): List<String> {
        player ?: return emptyList()
        val party = partyManager.getParty(player.uniqueId) ?: return emptyList()

        return party.members
            .filter { it != player.uniqueId }
            .mapNotNull { Bukkit.getPlayer(it)?.name }
            .filter { it.lowercase().startsWith(prefix.lowercase()) }
    }
}