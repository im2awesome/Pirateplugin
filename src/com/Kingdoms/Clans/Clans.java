package com.Kingdoms.Clans;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.logging.Logger;
import org.bukkit.event.Event;
import org.bukkit.event.Event.Priority;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.yaml.snakeyaml.Yaml;



public class Clans extends JavaPlugin {

	//Clans Data
	public static HashMap<String, TeamPlayer> Users = new HashMap<String, TeamPlayer>();
	public static HashMap<String, Team> Teams = new HashMap<String, Team>(); 
	public static HashMap<String, TeamArea> TeamAreas = new HashMap<String, TeamArea>();

	//Files
	private File TeamsFile;
	private File PlayersFile;

	//Logger
	private Logger log = Logger.getLogger("Minecraft");//Define your logger
	
	//Listeners
	private final ClansPlayerListener playerListener = new ClansPlayerListener(this);
	
	


	public void onEnable() {
		
		PluginManager pm = this.getServer().getPluginManager();
        pm.registerEvent(Event.Type.PLAYER_JOIN, playerListener, Priority.Normal, this);
        
		//Team File
		TeamsFile = new File("plugins/Clans/Teams.yml");
		//Players File
		PlayersFile = new File("plugins/Clans/Players.yml");
		//Load Data From Files
		loadData();

	}
	public void onDisable() {
		log.info("Clans disabled.");
	}


	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args)
	{
		String commandName = cmd.getName().toLowerCase();
        if (sender instanceof Player) 
        {
            Player player = (Player) sender;
            String PlayerName = player.getDisplayName();
            TeamPlayer tPlayer = Users.get(PlayerName);
            
            if(commandName.equals("team") && args.length >= 1)
            {
            	switch(args[0].toUpperCase() )
            	{
            		/* ==============================================================================
            		 *	TEAM CREATE - Creates a team.
            		 * ============================================================================== */
            		case "CREATE": 
            			if(args.length < 2) {
            				player.sendMessage(ChatColor.RED + "Invalid number of arguments.");
            				return true;
            			}
            			else if(tPlayer.hasTeam()) {
            				player.sendMessage(ChatColor.RED + "You are already in a team.");
            				return true;
            			}
            			else{
            				int i;
            				String TeamName = args[1];
            				for(i=2;i<args.length;i++)
            					TeamName += " " + args[i];
            				//Set Player's Team to new Key
            				Users.get(PlayerName).setTeamKey(TeamName);
            				//Create New Team and Add to Teams
            				Teams.put(TeamName, new Team(PlayerName));
            				player.sendMessage(ChatColor.GREEN + "Team [" + TeamName +"] successfully created!");
            				player.sendMessage(ChatColor.GREEN + "Use /team tag <tag> to add a Team tag.");
            			}
            			break;
            		/* ==============================================================================
                     *	TEAM INVITE - Invites a player to the team
                     * ============================================================================== */   
            		case "INVITE": 
            			if(args.length != 2){ //NOT ENOUGH ARGS
            				player.sendMessage(ChatColor.RED + "You didn't invite anyone.");
            				return true;
            			}
            			else if(!tPlayer.hasTeam()){ //NO TEAM
            				player.sendMessage(ChatColor.RED + "Must have a team to be able to invite to one.");
            				return true;
            			}
            			else if (!getRank(PlayerName).canInvite()) { //NOT ALLOWED TO INVITE
            				player.sendMessage(ChatColor.RED + "You lack sufficient permissions to invite on this team.");
            				return true;
            			}
            			
            			else if(!Users.containsKey(args[1])){ // INVITED NAME DOESN'T EXIST
            				player.sendMessage(ChatColor.RED + "That player does not exist.");
            				return true;
            			}
            			else{
            				TeamPlayer invitedPlayer = Users.get(args[1]);
            				if(invitedPlayer.hasTeam()){ // INVITED PLAYER HAS A TEAM
            					player.sendMessage(ChatColor.RED + "Cannot invite: This player has a team already.");
            					return true;
            				}
            				else{ // GIVE INVITE TO INVITED PLAYER
            					Users.get(args[1]).setInvite(tPlayer.getTeamKey());
            					player.sendMessage(ChatColor.GREEN + "You have invited " + args[1] + " to your team.");
            					getServer().getPlayer(args[1]).sendMessage(ChatColor.RED + "You have been invited to " + tPlayer.getTeamKey() +". Type /team accept to or /team reject to accept or deny this offer.");
            				}
            			}
            			break;
                	/* ==============================================================================
                	 *	TEAM ACCEPT - Accepts an invite
                	 * ============================================================================== */           		
            		case "ACCEPT": 		
            			if(tPlayer.hasTeam()){ // PLAYER HAS A TEAM
            				player.sendMessage(ChatColor.RED + "You are already on a team.");
            				return true;
            			}
            			else if(tPlayer.getInvite() == ""){ //PLAYER HAS NO INVITATIONS
            				player.sendMessage(ChatColor.RED + "You have not been invited to a team.");
            				return true;
            			}
            			else {
            				player.sendMessage(ChatColor.GREEN + "You have accepted the invitation from " + tPlayer.getInvite() + ".");
            				teamAdd(PlayerName);
            			}
            			break;
                	/* ==============================================================================
                	 *	TEAM REJECT - Rejects an invite
                	 * ============================================================================== */            		
            		case "REJECT": 
            			//we will check to see if they have an invite, if they do, we will clear all invites and send message
            			if(!tPlayer.hasInvite()){
        					player.sendMessage(ChatColor.RED + "You do not have an invite to reject.");
        					return true;
        				}
        				else{
        					player.sendMessage(ChatColor.RED + "You have rejected the offer from '" + tPlayer.getInvite() + "'.");
        					Users.get(PlayerName).clearInvite();
        				}        				
            			break;
                	/* ==============================================================================
                	 *	TEAM LIST - Lists all teams
                	 * ============================================================================== */
            		case "LIST": 
            			if(args.length != 1){
            				player.sendMessage(ChatColor.RED + "Invalid use of command. Proper use is /team list");
            			}
            			else{
            				for(String key : Teams.keySet()){
            					Team team = Teams.get(key);
            					player.sendMessage(team.getColor() + "[" + team.getTeamTag() + "] " 
            							+ ChatColor.GRAY + key + "("+ team.getTeamSize() +")"); //add team size later
            		        }
            			}
            			break;
                	/* ==============================================================================
                	 *	TEAM INFO - Prints info about a team
                	 * ============================================================================== */
            		case "INFO": 
            			if(args.length == 1) {
            				//check to see if they have a team
            				 if(!tPlayer.hasTeam()){
            					 player.sendMessage(ChatColor.RED + "You are not in a team. Use /team info <TEAMNAME> to look up a team's info.");
            					 return true;
            				 }
            				 else {
            					 Team team = Teams.get(tPlayer.getTeamKey());
            					 player.sendMessage(team.getColor() + "[" + tPlayer.getTeamKey() + "]" + " Team Info" );
            					 ArrayList<String> teamInfo = team.getTeamInfo();
            					 for(String s : teamInfo)
            						 player.sendMessage(s);
            				 }	 
            			}
            			else {
            				//check to see if other teams exist
            				int i;
            				String TeamName = args[1];
            				for (i=2;i<args.length;i++)
            					TeamName += " " + args[i];
            				if(!Teams.containsKey(TeamName)) {
            					player.sendMessage(ChatColor.RED + "Team '"+TeamName+"' does not exist.");
            					return true;
            				}
            				else {
            					Team team = Teams.get(TeamName);
            					player.sendMessage(team.getColor() + "[" + TeamName + "]" + " Team Info" );
           					 	ArrayList<String> teamInfo = team.getTeamInfo();
           					 	for(String s : teamInfo)
           					 		player.sendMessage(s);
            				}
            			}
            			break;
            		/* ==============================================================================
                     *	TEAM ONLINE - Prints players in team that are online
                     * ============================================================================== */   
            		case "ONLINE": 
            			 if(!tPlayer.hasTeam()) {
             				player.sendMessage(ChatColor.RED + "You are not on a team.");
             				return true;
            			 }
             			 else {
             				 String teamKey = tPlayer.getTeamKey();
             				 Team team = Teams.get(tPlayer.getTeamKey());
             				 Player[] onlineList = getServer().getOnlinePlayers();
             				 
             				 int count = 0;
             				 String onlineMembers ="";
             				 
             				 for (Player p : onlineList)
             				 {
             					 String userTeamKey = Users.get(p.getDisplayName()).getTeamKey();
             					 if(userTeamKey.equals(teamKey))
             					 {
             						 count++;
             						 onlineMembers += p.getDisplayName() + ", ";
             					 }
             				 }
             				onlineMembers = onlineMembers.substring(0,onlineMembers.length()-2);
             				player.sendMessage(team.getColor() + "[" + teamKey + "] (" + count +"/"+ team.getTeamSize() + ") Online: ");
             				player.sendMessage(ChatColor.GRAY + onlineMembers);             				 
             			 }
            			break;
                	/* ==============================================================================
                	 *	TEAM LEAVE - Leave a team
                	 * ============================================================================== */
            		case "LEAVE":         			
            			if(!tPlayer.hasTeam()){ // PLAYER DOES NOT HAVE A TEAM
            				player.sendMessage(ChatColor.RED + "You are not in a team");
            				return true;
            			}
            			else if(getTeam(PlayerName).isLeader(PlayerName) && getTeam(PlayerName).getLeaderCount() == 1){	
            				player.sendMessage(ChatColor.RED + "Must promote someone else to leader before leaving.");
            				return true;
            			}
            			else {
            				player.sendMessage(ChatColor.GREEN + "You have left the team.");
            				teamRemove(PlayerName);		
            			}
            			break;
                	/* ==============================================================================
                	 *	TEAM TK - Toggles friendly fire
                	 * ============================================================================== */
            		case "TK": 
            			if(args.length != 2) {
            				player.sendMessage(ChatColor.RED + "Invalid number of arguments.");
            				return true;
            			}
            			else if(!tPlayer.hasTeam()){ // PLAYER DOES NOT HAVE A TEAM
            				player.sendMessage(ChatColor.RED + "You are not in a team");
            				return true;
            			}
            			else if (!args[1].equalsIgnoreCase("on") || !args[1].equalsIgnoreCase("off")) {
            				player.sendMessage(ChatColor.RED + "Invalid use. Proper usage is /team tk <on/off>.");
            				return true;
            			}
            			else {
            				Users.get(PlayerName).setCanTeamKill(args[1].equalsIgnoreCase("on"));
            				player.sendMessage(ChatColor.GREEN + "Team killing has been set to " + args[1] + ".");
            			}
            			break;
                	/* ==============================================================================
                	 *	TEAM TOPSCORELIST - Prints the top 5 teams based on score
                	 * ============================================================================== */
            		case "TOPSCORELIST": break;
                	/* ==============================================================================
                	 *	TEAM SCORE - Prints the score of the team
                	 * ============================================================================== */
            		case "SCORE": break;
                	/* ==============================================================================
                	 *	TEAM KICK - Kicks a player from a team
                	 * ============================================================================== */
            		case "KICK": 
            			if(args.length == 1){ //NOT ENOUGH ARGS
            				player.sendMessage(ChatColor.RED + "You didn't kick anyone");
            				return true;
            			}
            			else if(!tPlayer.hasTeam()){ //NO TEAM
            				player.sendMessage(ChatColor.RED + "Must have a team to be able to use that command");
            				return true;
            			}
            			else if (!getRank(PlayerName).canKick()) { //NOT ALLOWED TO KICK
            				player.sendMessage(ChatColor.RED + "You lack sufficient permissions to kick on this team");
            				return true;
            			}
            			
            			else if(!Users.containsKey(args[1])){ // KICKED NAME DOESN'T EXIST
            				player.sendMessage(ChatColor.RED + "That player does not exist");
            				return true;
            			}
            			else{
            				//kick out of team
            				teamRemove(args[1]);
            				player.sendMessage(ChatColor.GREEN + "You have kicked " + args[1] + " out of the team.");
        					getServer().getPlayer(args[1]).sendMessage(ChatColor.RED + "You have been kicked out of the team.");
            			}
            			break;
                	/* ==============================================================================
                	 *	TEAM RCREATE | RANKCREATE - Creates a new rank at the bottom of the team
                	 * ============================================================================== */
            		case "RCREATE": case "RANKCREATE": 
            			if(!tPlayer.hasTeam()){ //NO TEAM
            				player.sendMessage(ChatColor.RED + "You must be in a team first.");
            				return true;
            			}
            			else if(!getRank(PlayerName).canEditRanks()){ //CANT EDIT RANKS
            				player.sendMessage(ChatColor.RED + "You lack sufficient permissions to create a rank on this team");
            				return true;
            			}
            			else if(args.length < 2){ //NO RANK ADDED
            				player.sendMessage(ChatColor.RED + "There is no rank to add.");
            				return true;
            			}
            			else if(args.length > 2){//MUST BE ONE WORD
            				player.sendMessage(ChatColor.RED + "Ranks must be one word");
            				return true;
            			}
            			else{ //ADD RANK
            				Teams.get(tPlayer.getTeamKey()).addRank(new TeamRank(args[1]));
            				player.sendMessage(ChatColor.RED + "You have added rank " + args[1] + " to the team.");
            			}
            				
            			break;
                	/* ==============================================================================
                	 *	TEAM RSET | RANKSET - Sets a player's rank
                	 * ============================================================================== */
            		case "RSET": case "RANKSET": 
            			if(!tPlayer.hasTeam()){ //NO TEAM
            				player.sendMessage(ChatColor.RED + "You must be in a team first.");
            				return true;
            			}
            			else if(!getRank(PlayerName).canSetRanks()){ //CANT EDIT RANKS
            				player.sendMessage(ChatColor.RED + "You lack sufficient permissions to set ranks on this team");
            				return true;
            			}
            			else if(args.length != 3){ //NO RANK ADDED
            				player.sendMessage(ChatColor.RED + "Invalid use. Use /team rset <teammember> <ranknumber>.");
            				return true;
            			}
            			else if(args[2].length() > 1){
            				player.sendMessage(ChatColor.RED + "Rank Numbers must be one digit.");
            				return true;
            			}
            			else if(args[2].matches("\\d")){
            				player.sendMessage(ChatColor.RED + "Invalid use. <ranknumber> must be a digit.");
            				return true;
            			}
            			else{
            				Team team = Teams.get(tPlayer.getTeamKey());
            				if(!team.isLeader(PlayerName)){//PLAYER ISNT LEADER
            					if(team.isLeader(args[1])){//CANT ALTER LEADERS
            						player.sendMessage(ChatColor.RED + "Can not set rank of members in rank 1.");
            						return true;
            					}
            					else if(args[2] == "1"){//CANT SET LEADER AS A PLAYERS RANK
            						player.sendMessage(ChatColor.RED + "Can not set any members to rank 1.");
            						return true;
            					}
            					else{
            						Teams.get(tPlayer.getTeamKey()).changePlayerRank(args[1],Integer.parseInt(args[2]));
            						player.sendMessage(ChatColor.GREEN + "Rank Changed.");
            					}
            				}
            				else{
        						Teams.get(tPlayer.getTeamKey()).changePlayerRank(args[1],Integer.parseInt(args[2]));
        						player.sendMessage(ChatColor.GREEN + "Rank Changed.");
            				}
            				
            			}
            			break;
                	/* ==============================================================================
                	 *	TEAM RRENAME | RANKRENAME - Sets a rank's name
                	 * ============================================================================== */
            		case "RRENAME": case "RANKRENAME": 
            			if(!tPlayer.hasTeam()){ //NO TEAM
            				player.sendMessage(ChatColor.RED + "You must be in a team first.");
            				return true;
            			}
            			else if(!getRank(PlayerName).canEditRanks()){ //CANT EDIT RANKS
            				player.sendMessage(ChatColor.RED + "You lack sufficient permissions to rename ranks on this team");
            				return true;
            			}
            			else if(args.length != 3){
            				player.sendMessage(ChatColor.RED + "Invalid use. Use /team rrename <oldrankname> <newranknumber>.");
            				return true;
            			}
            			else{
            				Teams.get(tPlayer.getTeamKey()).changeRankName(args[1],args[2]);
            				player.sendMessage(ChatColor.RED + "Rank name changed.");
            			}
            			break;
                	/* ==============================================================================
                	 *	TEAM RMASSMOVE | RANKMASSMOVE - Moves all players of a rank to another
                	 * ============================================================================== */
            		case "RMASSMOVE": case "RANKMASSMOVE": 
            			if(!tPlayer.hasTeam()){ //NO TEAM
            				player.sendMessage(ChatColor.RED + "You must be in a team first.");
            			}
            			else if(!getTeam(PlayerName).isLeader(PlayerName)){
            				player.sendMessage(ChatColor.RED + "Must be team leader to mass move people to different ranks.");
            				return true;
            			}
            			else if(args.length != 3){
            				player.sendMessage(ChatColor.RED + "Invalid use. Use /team rmassmove <oldrankname> <newranknumber>.");
            				return true;
            			}
            			else{
            				//massmove 
            			}
            			break;
                	/* ==============================================================================
                	 *	TEAM RINFO | RANKINFO - Prints permissions of a rank
                	 * ============================================================================== */
            		case "RINFO": case "RANKINFO": break;
                	/* ==============================================================================
                	 *	TEAM RPERMISSION | RANKPERMISSION - Sets a permission of a rank
                	 * ============================================================================== */
            		case "RPERMISSION": case "RANKPERMISSION": break;
                	/* ==============================================================================
                	 *	TEAM RDELETE | RANKDELETE - Removes a rank and moves all players inside to bottom rank
                	 * ============================================================================== */
            		case "RDELETE": case "RANKDELETE": break;
                	/* ==============================================================================
                	 *	TEAM DISBAND - Disbands the entire team
                	 * ============================================================================== */
            		case "DISBAND": 
            			break;
                	/* ==============================================================================
                	 *	TEAM TAG - Sets a team's tag
                	 * ============================================================================== */
            		case "TAG": 
            			if(!tPlayer.hasTeam()){ //NO TEAM
            				player.sendMessage(ChatColor.RED + "You are not in a team.");
            				return true;
            			}
            			else if(!getTeam(PlayerName).isLeader(PlayerName)){
            				player.sendMessage(ChatColor.RED + "Must be team leader to edit tag.");
            				return true;
            			}
            			else if(args.length == 1){
            				player.sendMessage(ChatColor.GREEN + "Your current tag is [" + getTeam(PlayerName).getTeamTag() + "]. /team tag <NewTag> to change tag.");
            				return true;
            			}
            			else if(args.length > 2){
            				player.sendMessage(ChatColor.RED + "Tags must be at least three characters.");
            				return true;
            			}
            			else if(args[1].length() > 7){
            				player.sendMessage(ChatColor.RED + "Tags must be less than seven characters.");
            				return true;
            			}
            			else {
            				Teams.get(tPlayer.getTeamKey()).setTeamTag(args[1]);
            				player.sendMessage(ChatColor.GREEN +"Tag has been changed to [" + getTeam(PlayerName).getTeamTag() + "].");
            			}
            			break;
                	/* ==============================================================================
                	 *	TEAM COLOR | COLOUR - Sets a team's color
                	 * ============================================================================== */
            		case "COLOR": case "COLOUR": 
            			
            			break;
                	/* ==============================================================================
                	 *	TEAM MOTD - Set's a team's Message of the Day, prints if no argument 
                	 * ============================================================================== */
            		case "MOTD": 
            			if(!tPlayer.hasTeam()){ //NO TEAM
            				player.sendMessage(ChatColor.RED + "You are not in a team.");
            				return true;
            			}
            			else if(args.length == 1){ //DISPLAY MOTD
            				player.sendMessage(ChatColor.GREEN + getTeam(PlayerName).getMOTD());
            				return true;
            			}
            			else if(!getTeam(PlayerName).isLeader(PlayerName)){ //NOT TEAM LEADER
            				player.sendMessage(ChatColor.RED + "Must be the team leader to edit the Message of the Day.");
            				return true;
            			}
            			else {
            				String MOTD = args[1];
            				int i;
            				for(i=2;i<args.length;i++)
            					MOTD += " " + args[i];
            				Teams.get(tPlayer.getTeamKey()).setMOTD(MOTD);	
            				player.sendMessage(ChatColor.GREEN + "Team MOTD has been changed.");
            			}
            			break;
                	/* ==============================================================================
                	 *	TEAM HELP - Prints commands and how to use them
                	 * ============================================================================== */
            		case "HELP": 
            			if(args.length == 1){
                   			player.sendMessage(ChatColor.RED + "Use /team help [1-4] to view each page.");
                   			return true;
                   		}
            			else if(args[1].equalsIgnoreCase("1")) {
                   			player.sendMessage(ChatColor.RED + "General Team Commands:");
                   			player.sendMessage(ChatColor.RED + "/t <message>"+ChatColor.GRAY +" - Sends a message to your team.");
                   			player.sendMessage(ChatColor.RED + "/team create <teamname>"+ChatColor.GRAY +" - Creates a team.");
                   			player.sendMessage(ChatColor.RED + "/team invite <playername>"+ChatColor.GRAY +" - Invites a player to a team.");
                   			player.sendMessage(ChatColor.RED + "/team accept"+ChatColor.GRAY +" - Accepts recent team invite.");
                   			player.sendMessage(ChatColor.RED + "/team reject"+ChatColor.GRAY +" - Rejects recent team invite.");
                   			player.sendMessage(ChatColor.RED + "/team leave"+ChatColor.GRAY +" - Leave a team.");
                   			player.sendMessage(ChatColor.RED + "/team info"+ChatColor.GRAY +" - Lists players and rankings of your own team.");
                   		}
                   		else if(args[1].equalsIgnoreCase("2")) {
                   			player.sendMessage(ChatColor.RED + "General Team Commands Continued:");
                   			player.sendMessage(ChatColor.RED + "/team info <teamname>"+ChatColor.GRAY +" - Lists players and rankings of the specified team.");
                   			player.sendMessage(ChatColor.RED + "/team online"+ChatColor.GRAY +" - Lists online team members.");
                   			player.sendMessage(ChatColor.RED + "/team list"+ChatColor.GRAY +" - Lists all teams.");
                   			player.sendMessage(ChatColor.RED + "/team tag <teamtag>"+ChatColor.GRAY +" - Sets a team's tag.");
                   			player.sendMessage(ChatColor.RED + "/team color <color>"+ChatColor.GRAY +" - Sets a team's color.");
                   			player.sendMessage(ChatColor.RED + "/team motd |<message>"+ChatColor.GRAY +" - Displays or sets a team's message of the day.");
                   			player.sendMessage(ChatColor.RED + "/team kick <playername>"+ChatColor.GRAY +" - Kicks a player from the team.");
                   			player.sendMessage(ChatColor.RED + "/team tk <on/off>"+ChatColor.GRAY +" - Toggles friendly fire.");
                   		}
                   		else if(args[1].equalsIgnoreCase("3")) {
                   			player.sendMessage(ChatColor.RED + "Team Rank Commands:");
                   			player.sendMessage(ChatColor.RED + "/team rankcreate <rankname>"+ChatColor.GRAY +" - Creates new rank at the bottom of the rank structure.");
                   			player.sendMessage(ChatColor.RED + "/team rankname <ranknumber> <rankname>"+ChatColor.GRAY +" - Renames a specified rank.");
                   			player.sendMessage(ChatColor.RED + "/team setrank <playername> <ranknumber>"+ChatColor.GRAY +" - Sets the rank of a team member.");
                   			player.sendMessage(ChatColor.RED + "/team rankmoveall <oldranknumber> <newranknumber>"+ChatColor.GRAY +" - Moves all members of a rank to a new rank.");
                   			player.sendMessage(ChatColor.RED + "/team rankinfo <ranknumber>"+ChatColor.GRAY +" - Outputs a rank's permissions.");
                   			player.sendMessage(ChatColor.RED + "/team rankflag <ranknumber> <kick/teamchat/rankedit/invite/promote> <true/false>"+ChatColor.GRAY +" - Sets a rank's permissions.");
                   			player.sendMessage(ChatColor.RED + "/team rankdelete <ranknumber>"+ChatColor.GRAY +" - Deletes a rank.");
                   		}
                   		else if(args[1].equalsIgnoreCase("4")) {
                   			player.sendMessage(ChatColor.RED + "Team Area Commands:");
                   		}
                   		else
                   			player.sendMessage(ChatColor.RED + "Improper use of command, Usage is /team help [1-4] to view each page.");
            			
            			break;
                	/* ==============================================================================
                	 *	TEAM AREA - THIS ISNT SET UP CORRECTLY YET
                	 * ============================================================================== */
            		case "AREA": 
            			break;           			
            	}
        		return true;
            }
            else if(commandName.equals("t"))
            {
   			 	if(!tPlayer.hasTeam()) {
   			 		player.sendMessage(ChatColor.RED + "You are not on a team.");
   			 		return true;
   			 	}
   			 	else if(!getRank(PlayerName).canTeamChat()) {
   			 		player.sendMessage(ChatColor.RED + "You lack sufficient permissions to talk in team chat.");
   			 		return true;
   			 	}
   			 	else if (args.length > 1) {
   			 		player.sendMessage(ChatColor.RED + "You did not enter a message to send.");
   			 		return true;
   			 	}
   			 	else {
     				int i;
     				String message = args[1];
     				for(i=2;i<args.length;i++)
     					message += " " + args[i];
	  				String teamKey = tPlayer.getTeamKey();
	  				Team team = Teams.get(tPlayer.getTeamKey());
	  				Player[] onlineList = getServer().getOnlinePlayers();  				 
	  				 
	  				for (Player p : onlineList) {
	  					String userTeamKey = Users.get(p.getDisplayName()).getTeamKey();
	  					if(userTeamKey.equals(teamKey))
	  						p.sendMessage(ChatColor.GREEN + "[TEAM] " + message);
	  				 }         				 
   			 	}
            }
            else if(commandName.equals("elo"))
            {
            	if(args[0].toUpperCase() == "LIST")
            	{
            		
            	}
            	else
            	{
            		//Must be a player name
            	}
            }
            else if(commandName.equals("rules"))
            {
            	player.sendMessage(ChatColor.RED + "Rules:");
            	player.sendMessage(ChatColor.RED + "1. Do not use cheats or client modifications that provide you with an unfair advantage.");
            	player.sendMessage(ChatColor.RED + "2. Do not log out in order to avoid combat with another player.");
            	player.sendMessage(ChatColor.RED + "3. Do not spam chat.");
            	player.sendMessage(ChatColor.RED + "Allowed: Total destruction, looting, and killing.");
            }            


        }
        return true;
	}
	private void loadData()
	{
		/*
		 * LOAD PLAYERS FROM FILE
		 * 
		 */
		HashMap<String,HashMap<String,String>> pl = null;
		Yaml yamlPlayers = new Yaml();
		Reader reader = null;
        try {
            reader = new FileReader(PlayersFile);
        } catch (final FileNotFoundException fnfe) {
        	 System.out.println("Players.YML Not Found!");
        	   try{
	            	  String strManyDirectories="plugins/Clans";
	            	  boolean success = (new File(strManyDirectories)).mkdirs();
	            	  }catch (Exception e){//Catch exception if any
	            	  System.err.println("Error: " + e.getMessage());
	            	  }
        } finally {
            if (null != reader) {
                try {
                    pl = (HashMap<String,HashMap<String,String>>)yamlPlayers.load(reader);
                    reader.close();
                } catch (final IOException ioe) {
                    System.err.println("We got the following exception trying to clean up the reader: " + ioe);
                }
            }
        }
        if(pl != null)
        {
        	//TODO: Load Player data into Users
        	for(String key : pl.keySet())
        	{
        		HashMap<String,String> PlayerData = pl.get(key);
        		String[] sDate = PlayerData.get("LastOnline").split("/");
        		int month = Integer.parseInt(sDate[0]);
        		int day = Integer.parseInt(sDate[1]);
        		int year = Integer.parseInt(sDate[2]);
        		Calendar cal = Calendar.getInstance();
        		cal.set(year, month, day);
        		int elo = Integer.parseInt(PlayerData.get("ELO"));
        		Users.put(key, new TeamPlayer(elo, cal));
        	}
        }
		/*
		 * LOAD TEAMS FROM FILE
		 * 
		 */
		HashMap<String, HashMap<String,Object>> h = null;
		Yaml yaml = new Yaml();
        try {
            reader = new FileReader(TeamsFile);
            h = (HashMap<String, HashMap<String,Object>>)yaml.load(reader);
        } catch (final FileNotFoundException fnfe) {
        	 System.out.println("Teams.YML Not Found!");
        	   try{
	            	  String strManyDirectories="plugins/Clans";
	            	  boolean success = (new File(strManyDirectories)).mkdirs();
	            	  }catch (Exception e){//Catch exception if any
	            	  System.err.println("Error: " + e.getMessage());
	            	  }
        } finally {
            if (null != reader) {
                try {
                    reader.close();
                } catch (final IOException ioe) {
                    System.err.println("We got the following exception trying to clean up the reader: " + ioe);
                }
            }
            
        }
       //CREATE TEAMS ONE AT A TIME
       if(h != null)
       {  
    	   //System.out.println(h.toString());
    	   for(String key : h.keySet())
    	   {
    		  ///Get Hashmap containing all Team Data
    		   HashMap<String,Object> t = h.get(key);
    		   
    		   String MOTD = (String) t.get("Motd");
    		   String Tag = (String) t.get("Tag");
    		   String Color = (String) t.get("Color");
    		   int Score = Integer.parseInt(((String) t.get("Score")));
    		   
    		   //Create Tier Lists
    		   ArrayList<TierList> TeamList = new ArrayList<TierList>();
    		   HashMap<String,HashMap<String,Object>> List = (HashMap<String, HashMap<String, Object>>) t.get("List");
    		   for(String rankNumber : List.keySet())
    		   {
    			   HashMap<String,Object> Tier = List.get(rankNumber);
    			   //Create Rank
    			   TeamRank newRank = new TeamRank((String)Tier.get("Rank Name"),(HashMap<String,Boolean>)Tier.get("Permissions"));
    			   
    			   //Add TeamKeys to all Members
    			   for(String PlayerName : (HashSet<String>)Tier.get("Members"))
    				   Users.get(PlayerName).setTeamKey(key);
    			   
    			   //Add Tier to TeamList
    			   TeamList.add(new TierList(newRank, (HashSet<String>)Tier.get("Members")));
    		   }
    		   //Add to Teams
    		   Teams.put(key, new Team(TeamList, MOTD, Score, Tag, Color));
    		   
    		   //TODO: Add Team Area Info
    	   }
       }
	}
	private void saveTeams()
	{
		//Print Clans to File.
		try{
			FileWriter fstream = new FileWriter(TeamsFile, false);
			BufferedWriter out = new BufferedWriter(fstream);
			out.write("");
			for(String key : Teams.keySet())
			{
				out.write(key + ":\n");
				out.write(Teams.get(key).getSaveString());
			}
			out.close();
			fstream.close();
		}catch (Exception e){//Catch exception if any
			System.err.println("Error: " + e.getMessage());
		}
	}
	private void savePlayers()
	{
		try{
			FileWriter fstream = new FileWriter(PlayersFile, false);
			BufferedWriter out = new BufferedWriter(fstream);
			out.write("");
			for(String key : Users.keySet())
			{
				out.write(key + ": " + Users.get(key).getSaveString());
			}
			out.close();
			fstream.close();
		}catch (Exception e){//Catch exception if any
			System.err.println("Error: " + e.getMessage());
		}
	}
	private TeamRank getRank(String PlayerName)
	{
		TeamPlayer tPlayer = Users.get(PlayerName);
		return Teams.get(tPlayer.getTeamKey()).getRank(PlayerName);
	}
	private Team getTeam(String PlayerName)
	{
		TeamPlayer tPlayer = Users.get(PlayerName);
		return Teams.get(tPlayer.getTeamKey());
	}
	private void teamAdd(String PlayerName){
		TeamPlayer tPlayer = Users.get(PlayerName);
		Users.get(PlayerName).setTeamKey(tPlayer.getInvite());
		Teams.get(tPlayer.getTeamKey()).addMember(PlayerName);
		Users.get(PlayerName).clearInvite();
	}
	private void teamRemove(String PlayerName){
		TeamPlayer tPlayer = Users.get(PlayerName);
		Teams.get(tPlayer.getTeamKey()).removeMember(PlayerName);
		Users.get(PlayerName).clearTeamKey();
	}
	public boolean hasUser(String PlayerName)
	{
		return Users.containsKey(PlayerName);
	}
	public void makeUser(String PlayerName)
	{
		Users.put(PlayerName, new TeamPlayer());
	}
	public void updateUserDate(String PlayerName)
	{
		Users.get(PlayerName).updateLastSeen();
	}



}