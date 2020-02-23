package com.massivecraft.factions.cmd;

import cn.nukkit.Server;
import com.massivecraft.factions.*;
import com.massivecraft.factions.event.FPlayerJoinEvent;
import com.massivecraft.factions.struct.Permission;

public class CmdJoin extends FCommand {
    public CmdJoin() {
        super();
        this.aliases.add("join");

        this.requiredArgs.add("league name");
        this.optionalArgs.put("player", "you");

        this.permission = Permission.JOIN.node;
        this.disableOnLock = true;

        senderMustBePlayer = true;
        senderMustBeMember = false;
        senderMustBeModerator = false;
        senderMustBeAdmin = false;
    }

    @Override
    public void perform() {
        Faction faction = this.argAsFaction(0);
        if (faction == null) return;

        FPlayer fplayer = this.argAsBestFPlayerMatch(1, fme, false);
        boolean samePlayer = fplayer == fme;

        if (!samePlayer && !Permission.JOIN_OTHERS.has(sender, false)) {
            msg("<b>You do not have permission to move other players into a league.");
            return;
        }

        if (!faction.isNormal()) {
            msg("<b>Players may only join normal league. This is a system faction.");
            return;
        }

        if (faction == fplayer.getFaction()) {
            msg("<b>%s %s already a member of %s", fplayer.describeTo(fme, true), (samePlayer ? "are" : "is"), faction.getTag(fme));
            return;
        }

        if (Conf.factionMemberLimit > 0 && faction.getFPlayers().size() >= Conf.factionMemberLimit) {
            msg(" <b>!<white> The league %s is at the limit of %d members, so %s cannot currently join.", faction.getTag(fme), Conf.factionMemberLimit, fplayer.describeTo(fme, false));
            return;
        }

        if (fplayer.hasFaction()) {
            msg("<b>%s must leave %s current league first.", fplayer.describeTo(fme, true), (samePlayer ? "your" : "their"));
            return;
        }

        if (!Conf.canLeaveWithNegativePower && fplayer.getPower() < 0) {
            msg("<b>%s cannot join a league with a negative power level.", fplayer.describeTo(fme, true));
            return;
        }

        if (!(faction.getOpen() || faction.isInvited(fplayer) || fme.isAdminBypassing() || Permission.JOIN_ANY.has(sender, false))) {
            msg("<i>This league requires invitation.");
            if (samePlayer)
                faction.msg("%s<i> tried to join your league.", fplayer.describeTo(faction, true));
            return;
        }

        // if economy is enabled, they're not on the bypass list, and this command has a cost set, make sure they can pay
        if (samePlayer && !canAffordCommand(Conf.econCostJoin, "to join a league")) return;

        // trigger the join event (cancellable)
        FPlayerJoinEvent joinEvent = new FPlayerJoinEvent(FPlayers.i.get(me), faction, FPlayerJoinEvent.PlayerJoinReason.COMMAND);
        Server.getInstance().getPluginManager().callEvent(joinEvent);
        if (joinEvent.isCancelled()) return;

        // then make 'em pay (if applicable)
        if (samePlayer && !payForCommand(Conf.econCostJoin, "to join a league", "for joining a league")) return;

        fme.msg("<i>%s successfully joined %s.", fplayer.describeTo(fme, true), faction.getTag(fme));

        if (!samePlayer)
            fplayer.msg("<i>%s moved you into the league.", fme.describeTo(fplayer, true), faction.getTag(fplayer));
        faction.msg("<i>%s joined your league.", fplayer.describeTo(faction, true));

        fplayer.resetFactionData();
        fplayer.setFaction(faction);
        faction.deinvite(fplayer);

        if (Conf.logFactionJoin) {
            if (samePlayer)
                P.p.log("%s joined the league %s.", fplayer.getName(), faction.getTag());
            else
                P.p.log("%s moved the player %s into the league %s.", fme.getName(), fplayer.getName(), faction.getTag());
        }
    }
}
