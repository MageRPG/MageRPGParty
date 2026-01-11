package me.thatonedevil.mageRPGParty.commands

import me.thatonedevil.devilLib.utils.Utils.noMessage
import me.thatonedevil.devilLib.utils.Utils.yesMessage
import me.thatonedevil.devilLib.utils.Utils.sendChat
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
        val player = sender as Player
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
            player.noMessage("<color:#d45252>You <color:#FF5555>are already in a <color:#d45252>party!")
        } else {
            player.yesMessage("<color:#77DD77>Party created <color:#35cd35>successfully!")
        }
    }

    private fun handleInvite(player: Player, args: Array<out String>) {
        if (!requireInParty(player)) return

        if (args.size < 2) {
            player.noMessage("<color:#FF5555>Usage: <color:#d45252>/party invite <player>")
            return
        }

        val party = partyManager.getParty(player.uniqueId)
        if (party == null || party.leader != player.uniqueId) {
            player.noMessage("<color:#d45252>You <color:#FF5555>are not the <color:#d45252>leader <color:#FF5555>of the party!")
            return
        }

        val targetPlayer = Bukkit.getPlayer(args[1])
        if (targetPlayer == null) {
            player.noMessage("<color:#FF5555>Player not <color:#d45252>found!")
            return
        }

        if (targetPlayer.uniqueId == player.uniqueId) {
            player.noMessage("<color:#d45252>You <color:#FF5555>cannot invite yourself!")
            return
        }

        if (party.members.size >= TEAM_SIZE) {
            player.noMessage("<color:#FF5555>Your party is full! <color:#d45252>Maximum size is $TEAM_SIZE players.")
            return
        }

        if (!partyManager.inviteToParty(player.uniqueId, targetPlayer.uniqueId)) {
            player.noMessage("<color:#FF5555>Could not invite <color:#d45252>player! <color:#FF5555>They may already be in a <color:#d45252>party <color:#FF5555>or have a <color:#d45252>pending invite.")
        }
    }

    private fun handleAccept(player: Player) {
        if (!requirePendingInvite(player)) return

        if (partyManager.acceptInvite(player.uniqueId)) {
            player.yesMessage("<color:#77DD77>Party invite accepted <color:#35cd35>successfully!")
        } else {
            player.noMessage("<color:#FF5555>Failed to accept invite! It may have <color:#d45252>expired.")
        }
    }

    private fun handleDecline(player: Player) {
        if (!requirePendingInvite(player)) return

        if (partyManager.declineInvite(player.uniqueId)) {
            player.yesMessage("<color:#77DD77>Party invite declined <color:#35cd35>successfully!")
        } else {
            player.noMessage("<color:#FF5555>Failed to decline invite! It may have <color:#d45252>expired.")
        }
    }

    private fun handleLeave(player: Player) {
        if (!requireInParty(player)) return

        if (partyManager.leaveParty(player.uniqueId)) {
            player.yesMessage("<color:#35cd35>You <color:#77DD77>have left the party <color:#35cd35>successfully!")
        } else {
            player.noMessage("<color:#d45252>You <color:#FF5555>are the <color:#d45252>leader <color:#FF5555>of the party! Use <color:#d45252>/party disband <color:#FF5555>to disband the party.")
        }
    }

    private fun handleKick(player: Player, args: Array<out String>) {
        if (!requireInParty(player)) return

        if (args.size < 2) {
            player.noMessage("<color:#FF5555>Usage: <color:#d45252>/party kick <player>")
            return
        }

        val party = partyManager.getParty(player.uniqueId)
        if (party == null) {
            player.noMessage("<color:#d45252>You <color:#FF5555>are not in a party!")
            return
        }

        // Check if player is the leader
        if (party.leader != player.uniqueId) {
            player.noMessage("<color:#d45252>You <color:#FF5555>are not the <color:#d45252>leader <color:#FF5555>of the party!")
            return
        }

        // Check if party has only 2 members
        if (party.members.size <= 2) {
            player.noMessage("<color:#d45252>Cannot kick from a 2-person party! Use <color:#FF5555>/party disband <color:#d45252>instead.")
            return
        }

        val targetPlayer = Bukkit.getPlayer(args[1])
        if (targetPlayer == null) {
            player.noMessage("<color:#FF5555>Player not <color:#d45252>found!")
            return
        }

        if (targetPlayer.uniqueId == player.uniqueId) {
            player.noMessage("<color:#d45252>You <color:#FF5555>cannot kick yourself! Use <color:#d45252>/party leave <color:#FF5555>instead.")
            return
        }

        if (!party.isMember(targetPlayer.uniqueId)) {
            player.noMessage("<color:#FF5555>That player is not in your party!")
            return
        }

        // Kick the player
        if (partyManager.kickFromParty(player.uniqueId, targetPlayer.uniqueId)) {
            player.yesMessage("<color:#77DD77>Successfully kicked <color:#35cd35>${targetPlayer.name} <color:#77DD77>from the party!")
            targetPlayer.sendChat("<color:#d45252>You <color:#FF5555>have been kicked from the party!")
        } else {
            player.noMessage("<color:#FF5555>Failed to kick player!")
        }
    }

    private fun handleDisband(player: Player) {
        if (!requireInParty(player)) return

        if (partyManager.disbandParty(player.uniqueId)) {
            player.yesMessage("<color:#77DD77>Party disbanded <color:#35cd35>successfully!")
        } else {
            player.noMessage("<color:#d45252>You <color:#FF5555>are not the <color:#d45252>leader <color:#FF5555>of the party!")
        }
    }

    private fun handleInfo(player: Player) {
        if (!requireInParty(player)) return
        player.sendMessage(partyManager.partyInfo())
    }

    private fun requireInParty(player: Player): Boolean {
        if (!partyManager.isInParty(player.uniqueId)) {
            player.noMessage("<color:#d45252>You <color:#FF5555>are not in a party! Create one with /party create")
            return false
        }
        return true
    }

    private fun requirePendingInvite(player: Player): Boolean {
        if (!partyManager.hasPendingInvite(player.uniqueId)) {
            player.noMessage("<color:#d45252>You <color:#FF5555>don't have any pending <color:#d45252>party invites!")
            return false
        }
        return true
    }

    private fun showUsage(player: Player): Boolean {
        player.noMessage("Usage: /party <${subcommands.joinToString(", ")}>")
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
                "invite" -> Bukkit.getOnlinePlayers()
                    .map { it.name }
                    .filter { it.lowercase().startsWith(args[1].lowercase()) }
                "kick" -> {
                    val player = sender as? Player ?: return emptyList()
                    val party = partyManager.getParty(player.uniqueId) ?: return emptyList()
                    party.members
                        .filter { it != player.uniqueId }
                        .mapNotNull { Bukkit.getPlayer(it)?.name }
                        .filter { it.lowercase().startsWith(args[1].lowercase()) }
                }
                else -> emptyList()
            }
            else -> emptyList()
        }
    }
}