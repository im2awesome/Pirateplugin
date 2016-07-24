package com.Satrosity.Clans;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.getspout.spoutapi.SpoutManager;
import org.getspout.spoutapi.player.SpoutPlayer;
import org.yaml.snakeyaml.Yaml;


import java.util.Comparator;


public class Clans extends JavaPlugin {

	//Clans Data
	private HashMap<String, TeamPlayer> Users = new HashMap<String, TeamPlayer>();
	private HashMap<String, Team> Teams = new HashMap<String, Team>(); 
	

	//Files
	private File TeamsFile;
	private File PlayersFile;

	//Logger
	private Logger log = Logger.getLogger("Minecraft");//Define your logger
	
	//Config
	private ClansConfig config;
	
	//Listeners
	private final ClansPlayerListener playerListener = new ClansPlayerListener(this);
	private final ClansBlockListener blockListener = new ClansBlockListener(this);
	
	
	
	public void onEnable() {       
        //Config
        config = new ClansConfig();
        
        //Register Events
		PluginManager pm = Bukkit.getPluginManager();
        pm.registerEvents(playerListener, this);
        pm.registerEvents(blockListener, this);
        
        //if(config.UseTags())
        	//pm.registerEvent(Event.PLAYER_CHAT, playerListener, EventPriority.NORMAL, this);
        	//Fix this
        
		//Team File
		TeamsFile = new File("plugins/Clans/Teams.yml");
		//Players File
		PlayersFile = new File("plugins/Clans/Players.yml");
		loadData();
		
		//Count Online Team Players, Used during reloads
		countOnlineTeamPlayers();
		
		//Clears inactive players and teams from the files
		clearInactivity();
		
		if(config.isAllowCapes())
			addCapes();
		
		resistIDCount = 0;
		
        PluginDescriptionFile pdfFile = this.getDescription();
        System.out.println( pdfFile.getName() + " version " + pdfFile.getVersion() + " is enabled!" );
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
            
            if(commandName.equals("pirate") && args.length >= 1)
            {
                //Check Inputs, Cannot contain ' or "
                if(args.length > 0)
                {
                	for(String arg : args){
                		if(arg.contains("'") || arg.contains("'"))
                		{
                			player.sendMessage(ChatColor.RED + "Arguments may not contain characters \' or \".");
                			return true;
                		}
                	}
                	
                }
            	switch(args[0].toUpperCase())
            	{
            		/* ==============================================================================
            		 *	TEAM CREATE - Creates a team.
            		 * ============================================================================== */
            		case "CREATE": 
            			if(!player.hasPermission("pirate.create")) {
            				player.sendMessage(ChatColor.RED + "Arr you don't have access to create a pirate group.");
            				return true;
            			}
            			else if(args.length < 2) {//INVALID ARGUMENTS
            				player.sendMessage(ChatColor.RED + "Arr your group name needs to be longer.");
            				return true;
            			}
            			else if(tPlayer.hasTeam()) {//PLAYER HAS TEAM
            				player.sendMessage(ChatColor.RED + "Arr you are already in a pirate group.");
            				return true;
            			}
            			else if(args[1].length() > 30 ){//MORE THAN 30 CHARACTERS
            				player.sendMessage(ChatColor.RED + "Arr pirate group names must be less than 30 characters.");
            				return true;
            			}
            			else if(args[1].contains("@server") ){//MORE THAN 30 CHARACTERS
            				player.sendMessage(ChatColor.RED + "Arr pirate group names must not contain reserved words.");
            				return true;
            			}
            			else{ //CREATE TEAM
            				int i;
            				String TeamName = args[1];
            				for(i=2;i<args.length;i++)
            					TeamName += " " + args[i];
            				if(Teams.containsKey(TeamName)) {
            					player.sendMessage(ChatColor.RED + "Arr a pirate group with this name already exists, please choose another name.");
            					return true;
            				}
            				//Set Player's Team to new Key
            				Users.get(PlayerName).setTeamKey(TeamName);
            				//Create New Team and Add to Teams
            				Teams.put(TeamName, new Team(PlayerName));
            				player.sendMessage(ChatColor.GREEN + "Pirate group [" + TeamName +"] successfully created!");
            				player.sendMessage(ChatColor.GREEN + "Use /pirate tag <tag> to add a pirate tag.");
            				
            				saveTeams();
            			}
            			break;
            		/* ==============================================================================
                     *	TEAM INVITE - Invites a player to the team
                     * ============================================================================== */   
            		case "INVITE": 
            			if(!player.hasPermission("Pirate.invite")) {
            				player.sendMessage(ChatColor.RED + "Arr you do not have permission to use this command.");
            				return true;
            			}
            			else if(args.length != 2){ //NOT ENOUGH ARGS
            				player.sendMessage(ChatColor.RED + "Arr you didn't invite anyone.");
            				return true;
            			}
            			else if(!tPlayer.hasTeam()){ //NO TEAM
            				player.sendMessage(ChatColor.RED + "Arr must have a pirate group to be able to invite someone.");
            				return true;
            			}
            			else if (!getRank(PlayerName).canInvite()) { //NOT ALLOWED TO INVITE
            				player.sendMessage(ChatColor.RED + "Arr your captain doesn't allow you to invite someone to this pirate group.");
            				return true;
            			}
            			
            			else if(!Users.containsKey(args[1])){ // INVITED NAME DOESN'T EXIST
            				player.sendMessage(ChatColor.RED + "Arr that player does not exist.");
            				return true;
            			}
            			else{
            				TeamPlayer invitedPlayer = Users.get(args[1]);
            				if(invitedPlayer.hasTeam()){ // INVITED PLAYER HAS A TEAM
            					player.sendMessage(ChatColor.RED + "Arr cannot invite: This player is already in a pirate group.");
            					return true;
            				}
            				else{ // GIVE INVITE TO INVITED PLAYER
            					Users.get(args[1]).setInvite(tPlayer.getTeamKey());
            					player.sendMessage(ChatColor.GREEN + "Arr you have invited " + args[1] + " to your pirate group.");
            					getServer().getPlayer(args[1]).sendMessage(ChatColor.GREEN + "Arr you have been invited to " + tPlayer.getTeamKey() +". Type /pirate accept to or /pirate reject to accept or deny this offer.");
            				}
            			}
            			break;
                	/* ==============================================================================
                	 *	TEAM ACCEPT - Accepts an invite
                	 * ============================================================================== */           		
            		case "ACCEPT": 	
            			if(!player.hasPermission("Pirate.accept")) {
            				player.sendMessage(ChatColor.RED + "Arr you do not have permission to use this command.");
            				return true;
            			}
            			else if(tPlayer.hasTeam()){ // PLAYER HAS A TEAM
            				player.sendMessage(ChatColor.RED + "Arr you are already in a pirate group.");
            				return true;
            			}
            			else if(tPlayer.getInvite() == ""){ //PLAYER HAS NO INVITATIONS
            				player.sendMessage(ChatColor.RED + "Arr you have not been invited to a pirate group.");
            				return true;
            			}
            			else { //ACCEPT INVITATION
            				player.sendMessage(ChatColor.GREEN + "Arr you have accepted the invitation from " + tPlayer.getInvite() + ".");
            				teamAdd(PlayerName);
            				saveTeams();
            			}
            			break;
                	/* ==============================================================================
                	 *	TEAM REJECT - Rejects an invite
                	 * ============================================================================== */            		
            		case "REJECT": 
            			if(!player.hasPermission("Pirate.reject")) {
            				player.sendMessage(ChatColor.RED + "Arr you do not have permission to use this command.");
            				return true;
            			}
            			else if(!tPlayer.hasInvite()){
        					player.sendMessage(ChatColor.RED + "Arr you do not have an invite to reject.");
        					return true;
        				}
        				else{
        					player.sendMessage(ChatColor.RED + "Arr you have rejected the offer from '" + tPlayer.getInvite() + "'.");
        					Users.get(PlayerName).clearInvite();
        				}        				
            			break;
                	/* ==============================================================================
                	 *	TEAM LIST - Lists all teams
                	 * ============================================================================== */
            		case "LIST": 
            			if(!player.hasPermission("Pirate.list")) {
            				player.sendMessage(ChatColor.RED + "Arr you do not have permission to use this command.");
            				return true;
            			}
            			else if(args.length != 1){//INVALID ARGUMENTS
            				player.sendMessage(ChatColor.RED + "Arr invalid use of command. Proper use is /pirate list");
            			}
            			else{//GET TEAM LIST
            				for(String key : Teams.keySet()){
            					Team team = Teams.get(key);
            					player.sendMessage(team.getColor() + "[" + team.getTeamTag() + "] " 
            							+ ChatColor.GRAY + key + " ("+ team.getTeamSize() +")"); //add team size later
            		        }
            			}
            			break;
                	/* ==============================================================================
                	 *	TEAM INFO - Prints info about a team
                	 * ============================================================================== */
            		case "INFO": 
            			if(!player.hasPermission("Pirate.info")) {
            				player.sendMessage(ChatColor.RED + "Arr you do not have permission to use this command.");
            				return true;
            			}
            			else if(args.length == 1){//DISPLAY YOUR TEAM INFO
            				 if(!tPlayer.hasTeam()){//DOESNT HAVE TEAM
            					 player.sendMessage(ChatColor.RED + "Arr you are not in a team. Use /pirate info <NAME> to look up a pirate groups info.");
            					 return true;
            				 }
            				 else {//DISPLAY INFO
            					 Team team = Teams.get(tPlayer.getTeamKey());
            					 player.sendMessage(team.getColor() + "[" + tPlayer.getTeamKey() + "]" + " Pirate Group Info" );
            					 if(config.UseScore())
            						 player.sendMessage(team.getColor() + "Pirate group Score: " + team.getTeamScore() );
            					 ArrayList<String> teamInfo = team.getTeamInfo();
            					 for(String s : teamInfo)
            						 player.sendMessage(s);
            				 }	 
            			}
            			else {//DISPLAY OTHER TEAM INFO
            				int i;
            				String TeamName = args[1];
            				for (i=2;i<args.length;i++)
            					TeamName += " " + args[i];
            				if(!Teams.containsKey(TeamName)) {//NAME DOESNT EXIST
            					player.sendMessage(ChatColor.RED + "Arr pirate group '"+TeamName+"' does not exist.");
            					return true;
            				}
            				else {
            					Team team = Teams.get(TeamName);
            					player.sendMessage("Arr"+ team.getColor() + "[" + TeamName + "]" + " Pirate Group Info" );
           					 	if(config.UseScore())
           					 		player.sendMessage(team.getColor() + "Arr Pirate Group Score: " + team.getTeamScore() );
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
            			if(!player.hasPermission("Clans.online")) {
            				player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            				return true;
            			}
            			else if(!tPlayer.hasTeam()) {//NOT ON A TEAM
             				player.sendMessage(ChatColor.RED + "You are not on a team.");
             				return true;
            			}
             			else {//CHECK ONLINE TEAM MEMBERS
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
            			if(!player.hasPermission("Clans.leave")) {
            				player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            				return true;
            			}
            			else if(!tPlayer.hasTeam()){ // PLAYER DOES NOT HAVE A TEAM
            				player.sendMessage(ChatColor.RED + "You are not in a team");
            				return true;
            			}
            			else if(getTeam(PlayerName).isLeader(PlayerName) && getTeam(PlayerName).getLeaderCount() == 1){//CANT LEAVE AS LEADER	
            				player.sendMessage(ChatColor.RED + "Must promote someone else to leader before leaving. Do /team disband if you are trying to disband the team.");
            				return true;
            			}
            			else {//LEAVE TEAM
            				player.sendMessage(ChatColor.GREEN + "You have left the team.");
            				teamRemove(PlayerName);		
            				
            				saveTeams();
            			}
            			break;
                	/* ==============================================================================
                	 *	TEAM TK - Toggles friendly fire
                	 * ============================================================================== */
            		case "TK": 
            			if(!player.hasPermission("Clans.tktoggle") || !config.AllowTKToggle()) {
            				player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            				return true;
            			}
            			else if(args.length != 2) {//INVALID ARGUMENTS
            				player.sendMessage(ChatColor.RED + "Invalid number of arguments.");
            				return true;
            			}
            			else if(!tPlayer.hasTeam()){ // PLAYER DOES NOT HAVE A TEAM
            				player.sendMessage(ChatColor.RED + "You are not in a team");
            				return true;
            			}
            			else if (!(args[1].equalsIgnoreCase("on") || args[1].equalsIgnoreCase("off"))) {//INVALID USE
            				player.sendMessage(ChatColor.RED + "Invalid use. Proper usage is /team tk <on/off>.");
            				return true;
            			}
            			else {//TOGGLE SETTING
            				Users.get(PlayerName).setCanTeamKill(args[1].equalsIgnoreCase("on"));
            				player.sendMessage(ChatColor.GREEN + "Team killing has been set to " + args[1] + ".");
            			}
            			break;
                	/* ==============================================================================
                	 *	TEAM TOPSCORELIST - Prints the top 5 teams based on score
                	 * ============================================================================== */
            		case "TOPSCORES": case "TOP": case "SB": 
               			if(!player.hasPermission("Clans.topscores")) {
            				player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            				return true;
            			}
            			else if (!config.UseScore()) {
            				player.sendMessage(ChatColor.RED + "Team scores are disabled.");
            				return true;
            			}
            			else
            			{
            				//SORT LIST
            				int listSize = 5;
            				
            				ArrayList<TeamScoreNode> TopTeams = new ArrayList<TeamScoreNode>();
            				boolean start = false;
            				for(String teamName : Teams.keySet())
            				{
            					int ts = Teams.get(teamName).getTeamScore();
            					TopTeams.add(new TeamScoreNode(teamName,ts));
            					if(start) {
            						for(int i = TopTeams.size()-1; i >= 1; i--)
            						{
            							if(TopTeams.get(i-1).TeamScore < TopTeams.get(i).TeamScore)
            							{
            								TeamScoreNode temp = TopTeams.get(i);
            								TopTeams.set(i, TopTeams.get(i-1));
            								TopTeams.set(i-1, temp);
            							}
            							else
            							{
            								if(TopTeams.size() >= listSize+1)
            									TopTeams.remove(TopTeams.size()-1);
            								break;
            							}
            						}
            					}
            					start = true;;
            				}
            				
            				//PRINT TEAMS AND SCORES
            				player.sendMessage(ChatColor.GOLD + "Top " +listSize+ " Teams:");
            				int ranking = 1;
            				for(TeamScoreNode tsn : TopTeams)
            				{
            					player.sendMessage(ChatColor.GRAY +""+ ranking +". "  +tsn.TeamScore + "Pts  -  "+Teams.get(tsn.TeamName).getColor() + tsn.TeamName);
            					ranking++;
            				}
            			}
            			break;
                	/* ==============================================================================
                	 *	TEAM SCORE - Prints the score of the team
                	 * ============================================================================== */
            		case "SCORE": 
            			if(!player.hasPermission("Clans.score")) {
            				player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            				return true;
            			}
            			else if (!config.UseScore()) {
            				player.sendMessage(ChatColor.RED + "Team scores are disabled.");
            				return true;
            			}
            			else if(args.length == 1){//DISPLAY YOUR TEAM INFO
            				 if(!tPlayer.hasTeam()){//DOESNT HAVE TEAM
            					 player.sendMessage(ChatColor.RED + "You are not in a team. Use /team score <TEAMNAME> to look up a team's info.");
            					 return true;
            				 }
            				 else {//DISPLAY INFO
            					 Team team = Teams.get(tPlayer.getTeamKey());
            					 player.sendMessage(team.getColor() + "[" + tPlayer.getTeamKey() + "]" + " Team Score: " + team.getTeamScore() );
            				 }	 
            			}
            			else {//DISPLAY OTHER TEAM INFO
            				int i;
            				String TeamName = args[1];
            				for (i=2;i<args.length;i++)
            					TeamName += " " + args[i];
            				if(!Teams.containsKey(TeamName)) {//NAME DOESNT EXIST
            					player.sendMessage(ChatColor.RED + "Team '"+TeamName+"' does not exist.");
            					return true;
            				}
            				else {
            					Team team = Teams.get(TeamName);
           					 	player.sendMessage(team.getColor() + "[" + TeamName + "]" + " Team Score: " + team.getTeamScore() );
            				}
            			}	
            			break;
                	/* ==============================================================================
                	 *	TEAM KICK - Kicks a player from a team
                	 * ============================================================================== */
            		case "KICK": 
            			if(!player.hasPermission("Clans.kick")) {
            				player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            				return true;
            			}
            			else if(args.length == 1){ //NOT ENOUGH ARGS
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
            			else if(!Users.get(args[1]).getTeamKey().equalsIgnoreCase(tPlayer.getTeamKey())){ //MAKE SURE BOTH PLAYERS ARE IN THE SAME TEAM
            				player.sendMessage(ChatColor.RED + "You are not on the same team.");
            				return true;
            			}
            			else if(getTeam(PlayerName).getRankNumber(PlayerName) >= getTeam(PlayerName).getRankNumber((args[1]))){//CANT ALTER LEADERS
        					player.sendMessage(ChatColor.RED + "Can not kick players with a higher rank than your own.");
            			}
            			else{//KICK OUT OF TEAM
            				teamRemove(args[1]);
            				player.sendMessage(ChatColor.GREEN + "You have kicked " + args[1] + " out of the team.");
            				if(getServer().getPlayer(args[1]).isOnline())
            					getServer().getPlayer(args[1]).sendMessage(ChatColor.RED + "You have been kicked out of the team.");
            				saveTeams();
            			}
            			break;
                	/* ==============================================================================
                	 *	TEAM RCREATE | RANKCREATE - Creates a new rank at the bottom of the team
                	 * ============================================================================== */
            		case "RCREATE": case "RANKCREATE": 
            			if(!player.hasPermission("Clans.rankcreate")) {
            				player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            				return true;
            			}
            			else if(!tPlayer.hasTeam()){ //NO TEAM
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
            				player.sendMessage(ChatColor.GREEN + "You have added rank " + args[1] + " to the team.");
            				saveTeams();
            			}
            			break;
                	/* ==============================================================================
                	 *	TEAM RSET | RANKSET - Sets a player's rank
                	 * ============================================================================== */
            		case "RSET": case "RANKSET": 
            			if(!player.hasPermission("Clans.rankset")) {
            				player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            				return true;
            			}
            			else if(!tPlayer.hasTeam()){ //NO TEAM
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
            			else if(!isInteger(args[2])){ //IF NO INTEGER
            				player.sendMessage(ChatColor.RED + "Invalid use. Use /team rset <teammember> <ranknumber>.");
            				return true;
            			}
            			else if(1 > Integer.parseInt(args[2])|| Integer.parseInt(args[2]) > getTeam(PlayerName).getRankCount()){//RANK NUMBER DOESNT EXIST
            				player.sendMessage(ChatColor.RED + "Rank number does not exist.");
            				return true;	
            			}
            			else if(!Users.containsKey(args[1])){ //MAKE SURE BOTH PLAYERS ARE IN THE SAME TEAM
            				player.sendMessage(ChatColor.RED + "The specified user was not found.");
            				return true;
            			}
            			else if(!Users.get(args[1]).getTeamKey().equalsIgnoreCase(tPlayer.getTeamKey())){ //MAKE SURE BOTH PLAYERS ARE IN THE SAME TEAM
            				player.sendMessage(ChatColor.RED + "You are not on the same team.");
            				return true;
            			}
            			else if(getTeam(PlayerName).isLeader(PlayerName) && getTeam(PlayerName).getLeaderCount() == 1 && args[1].equalsIgnoreCase(PlayerName)){//CANT DEMOTE SELF WITH NO LEADERS	
            				player.sendMessage(ChatColor.RED + "Must promote someone else to leader before changing your own rank.");
            				return true;
            			}
            			else if(!getTeam(PlayerName).isLeader(PlayerName)){//PLAYER ISNT LEADER
            				if(getTeam(PlayerName).getRankNumber(PlayerName) < Integer.parseInt(args[1])){//CANT ALTER LEADERS
            					player.sendMessage(ChatColor.RED + "Can not set rank of members higher than your own.");
            					return true;
            				}
            				else if(getTeam(PlayerName).getRankNumber(PlayerName) < Integer.parseInt(args[2])){//CANT SET LEADER AS A PLAYERS RANK
            					player.sendMessage(ChatColor.RED + "Can not set any members to a rank higher than your own.");
            					return true;
            				}
            				else{
            					Teams.get(tPlayer.getTeamKey()).changePlayerRank(args[1],Integer.parseInt(args[2]));
            					player.sendMessage(ChatColor.GREEN + "Rank Changed.");
            					saveTeams();
            				}
            			}
            			else{
        					Teams.get(tPlayer.getTeamKey()).changePlayerRank(args[1],Integer.parseInt(args[2]));
        					player.sendMessage(ChatColor.GREEN + "Rank Changed.");
        					saveTeams();
            			}
            			break;
                	/* ==============================================================================
                	 *	TEAM RRENAME | RANKRENAME - Sets a rank's name
                	 * ============================================================================== */
            		case "RRENAME": case "RANKRENAME": 
            			if(!player.hasPermission("Clans.rankrename")) {
            				player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            				return true;
            			}
            			else if(!tPlayer.hasTeam()){ //NO TEAM
            				player.sendMessage(ChatColor.RED + "You must be in a team first.");
            				return true;
            			}
            			else if(!getRank(PlayerName).canEditRanks()){ //CANT EDIT RANKS
            				player.sendMessage(ChatColor.RED + "You lack sufficient permissions to rename ranks on this team");
            				return true;
            			}
            			else if(args.length != 3){
            				player.sendMessage(ChatColor.RED + "Invalid use. Use /team rrename <ranknumber> <newrankname>.");
            				return true;
            			}
            			else if(!isInteger(args[1])){
            				player.sendMessage(ChatColor.RED + "Rank Numbers must be digits.");
            				return true;
            			}
            			else if(1 > Integer.parseInt(args[1])|| Integer.parseInt(args[1]) > getTeam(PlayerName).getRankCount()){//RANK NUMBER DOESNT EXIST
            				player.sendMessage(ChatColor.RED + "Rank number does not exist.");
            				return true;	
            			}
            			else if(getTeam(PlayerName).getRankNumber(PlayerName) < Integer.parseInt(args[1]) && !getTeam(PlayerName).isLeader(PlayerName)){
            				player.sendMessage(ChatColor.RED + "Cannot edit ranks higher than your own.");
            				return true;
            			}
            			else{
            				Teams.get(tPlayer.getTeamKey()).changeRankName(Integer.parseInt(args[1])-1,args[2]);
            				player.sendMessage(ChatColor.GREEN + "Rank name changed.");
            				
            				saveTeams();
            			}
            			break;
                	/* ==============================================================================
                	 *	TEAM RMASSMOVE | RANKMASSMOVE - Moves all players of a rank to another
                	 * ============================================================================== */
            		case "RMASSMOVE": case "RANKMASSMOVE": 
            			if(!tPlayer.hasTeam()){ //NO TEAM
            				player.sendMessage(ChatColor.RED + "You must be in a team first.");
            				return true;
            			}
            			else if(!getTeam(PlayerName).isLeader(PlayerName)){
            				player.sendMessage(ChatColor.RED + "Must be team leader to mass move people to different ranks.");
            				return true;
            			}
            			else if(args.length != 3){
            				player.sendMessage(ChatColor.RED + "Invalid use. Use /team rmassmove <oldranknumber> <newranknumber>.");
            				return true;
            			}
            			else if(!isInteger(args[1]) || !isInteger(args[2])){
            				player.sendMessage(ChatColor.RED + "Rank Numbers must be digits.");
            				return true;
            			}
            			else if(1 > Integer.parseInt(args[1])|| Integer.parseInt(args[1]) > getTeam(PlayerName).getRankCount()){//RANK NUMBER DOESNT EXIST
            				player.sendMessage(ChatColor.RED + "Rank number does not exist.");
            				return true;	
            			}
            			else if(1 > Integer.parseInt(args[2])|| Integer.parseInt(args[2]) > getTeam(PlayerName).getRankCount()){//RANK NUMBER DOESNT EXIST
            				player.sendMessage(ChatColor.RED + "Rank number does not exist.");
            				return true;	
            			}
            			else{
            				Teams.get(tPlayer.getTeamKey()).massRankMove(Integer.parseInt(args[1]),Integer.parseInt(args[2]));
            				player.sendMessage(ChatColor.GREEN + "Ranks moved.");
            				saveTeams();
            			}
            			break;
                	/* ==============================================================================
                	 *	TEAM RINFO | RANKINFO - Prints permissions of a rank
                	 * ============================================================================== */
            		case "RINFO": case "RANKINFO": 
            			if(!player.hasPermission("Clans.rankinfo")) {
            				player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            				return true;
            			}
            			else if(!tPlayer.hasTeam()){//NOT IN TEAM
            				player.sendMessage(ChatColor.RED + "You are not in a team.");
            				return true;
            			}
            			else if(args.length != 2){//INVALID ARGUMENTS
            				player.sendMessage(ChatColor.RED + "Invalid use of command. Use /team rinfo <ranknumber> to get permissions.");
            				return true;
            			}
            			else if(!isInteger(args[1])){//MUST BE INTEGER
            				player.sendMessage(ChatColor.RED + "Rank Numbers must be digits.");
            				return true;
            			}
            			else if(1 > Integer.parseInt(args[1])|| Integer.parseInt(args[1]) > getTeam(PlayerName).getRankCount()){//RANK NUMBER DOESNT EXIST
            				player.sendMessage(ChatColor.RED + "Rank number does not exist.");
            				return true;	
            			}
            			else{
            				Team team = Teams.get(tPlayer.getTeamKey());
            				if(team.getRankCount() < Integer.parseInt(args[1])){//RANK NUMBER DOESNT EXIST
            					player.sendMessage(ChatColor.RED + "Rank number does not exist.");
            					return true;
            				}
            				else{//PRINT RANK INFO
            					
            					TeamRank rank = getTeam(PlayerName).getRank(Integer.parseInt(args[1]));
            					player.sendMessage(ChatColor.DARK_GREEN + rank.getRankName() + " Permissions:");
            					player.sendMessage(ChatColor.GREEN + "Set Ranks   : " + rank.canSetRanks());
            					player.sendMessage(ChatColor.GREEN + "Invite        : " + rank.canInvite());
            					player.sendMessage(ChatColor.GREEN + "Edit Ranks  : " + rank.canEditRanks());
            					player.sendMessage(ChatColor.GREEN + "Kick          : " + rank.canKick());
            					player.sendMessage(ChatColor.GREEN + "Team Chat  : " + rank.canTeamChat());
            					player.sendMessage(ChatColor.GREEN + "Area Info  : " + rank.canSeeAreaInfo());
            				}
            			}
            			break;
                	/* ==============================================================================
                	 *	TEAM RPERMISSION | RANKPERMISSION - Sets a permission of a rank
                	 * ============================================================================== */
            		case "RPERMISSION": case "RANKPERMISSION": case "RPERM":
            			if(!player.hasPermission("Clans.rankpermission")) {
            				player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            				return true;
            			}
            			else if(!tPlayer.hasTeam()){
            				player.sendMessage(ChatColor.RED + "You are not in a team.");
            				return true;
            			}
            			else if(!getRank(PlayerName).canEditRanks()){
            				player.sendMessage(ChatColor.RED + "You lack sufficent permission to edit rank permissions.");
            				return true;
            			}
            			else if(args.length != 4){
            				player.sendMessage(ChatColor.RED + "Invalid use of command. Use /team rpermission <ranknumber> <kick/teamchat/rankedit/invite/promote/areainfo> <true|false>.");
            				return true;
            			}
            			else if(!isInteger(args[1])){
            				player.sendMessage(ChatColor.RED + "Rank Numbers must be digits.");
            				return true;
            			}
            			else if(1 > Integer.parseInt(args[1])|| Integer.parseInt(args[1]) > getTeam(PlayerName).getRankCount()){//RANK NUMBER DOESNT EXIST
            				player.sendMessage(ChatColor.RED + "Rank number does not exist.");
            				return true;	
            			}
            			else if(getTeam(PlayerName).getRankNumber(PlayerName) < Integer.parseInt(args[1]) && !getTeam(PlayerName).isLeader(PlayerName)){
            				player.sendMessage(ChatColor.RED + "Cannot edit ranks higher than your own.");
            			}
            			else{
            				switch(args[2].toUpperCase()){
            				case "KICK":
            					Teams.get(tPlayer.getTeamKey()).getRank(Integer.parseInt(args[1])).setCanKick(Boolean.parseBoolean(args[3]));
            					saveTeams();
            					break;
            				case "TEAMCHAT":
            					Teams.get(tPlayer.getTeamKey()).getRank(Integer.parseInt(args[1])).setCanTeamChat(Boolean.parseBoolean(args[3]));
            					saveTeams();
            					break;
            				case "RANKEDIT":
            					Teams.get(tPlayer.getTeamKey()).getRank(Integer.parseInt(args[1])).setCanEditRanks(Boolean.parseBoolean(args[3]));
            					saveTeams();
            					break;
            				case "INVITE":
            					Teams.get(tPlayer.getTeamKey()).getRank(Integer.parseInt(args[1])).setCanInvite(Boolean.parseBoolean(args[3]));
            					saveTeams();
            					break;
            				case "RANKSET":
            					Teams.get(tPlayer.getTeamKey()).getRank(Integer.parseInt(args[1])).setCanSetRanks(Boolean.parseBoolean(args[3]));
            					saveTeams();
            					break;
            				case "AREAINFO":
            					Teams.get(tPlayer.getTeamKey()).getRank(Integer.parseInt(args[1])).setCanSeeAreaInfo(Boolean.parseBoolean(args[3]));
            					saveTeams();
            					break;
            				default: 
            					player.sendMessage(ChatColor.RED + "Invalid permission. Use /team rpermission <ranknumber> <kick/teamchat/rankedit/invite/promote/areainfo> <true|false>.");
            					return true;
            				}
            				player.sendMessage(ChatColor.GREEN + "Changed rank permission.");
            			}
            			break;
                	/* ==============================================================================
                	 *	TEAM RDELETE | RANKDELETE - Removes a rank and moves all players inside to bottom rank
                	 * ============================================================================== */
            		case "RDELETE": case "RANKDELETE": 
            			if(!player.hasPermission("Clans.rankdelete")) {
            				player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            				return true;
            			}
            			else if(!tPlayer.hasTeam()){
            				player.sendMessage(ChatColor.RED + "You are not in a team.");
            				return true;
            			}
            			Team team = Teams.get(tPlayer.getTeamKey());
            			if(getTeam(PlayerName).getRankNumber(PlayerName) >= Integer.parseInt(args[1])){
            				player.sendMessage(ChatColor.RED + "Unable to remove ranks above your own or your own.");
            				return true;
            			}
            			else if(!getRank(PlayerName).canEditRanks()){
            				player.sendMessage(ChatColor.RED + "You lack sufficent permission to delete ranks.");
            				return true;
            			}
            			else if(!isInteger(args[1])){
            				player.sendMessage(ChatColor.RED + "Rank number must be in digits.");
            				return true;
            			}
            			else if(1 > Integer.parseInt(args[1])|| Integer.parseInt(args[1]) > getTeam(PlayerName).getRankCount()){//RANK NUMBER DOESNT EXIST
            				player.sendMessage(ChatColor.RED + "Rank number does not exist.");
            				return true;	
            			}
            			else if(team.getRankCount() < Integer.parseInt(args[1])){//RANK NUMBER DOESNT EXIST
        					player.sendMessage(ChatColor.RED + "Rank number does not exist.");
        					return true;
        				}
            			else{
            				
            				Teams.get(tPlayer.getTeamKey()).massRankMove(Integer.parseInt(args[1]),team.getRankCount()-1);
            				Teams.get(tPlayer.getTeamKey()).removeRank(Integer.parseInt(args[1]));
            				player.sendMessage(ChatColor.GREEN + "Ranks moved.");
            				saveTeams();
            			}
            			break;
                	/* ==============================================================================
                	 *	TEAM DISBAND - Disbands the entire team
                	 * ============================================================================== */
            		case "DISBAND": 
            			if(!player.hasPermission("Clans.disband")) {
            				player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            				return true;
            			}
            			else if(!tPlayer.hasTeam()){//NO TEAM
            				player.sendMessage(ChatColor.RED + "You are not in a team.");
            				return true;
            			}
            			else if (!getTeam(PlayerName).isLeader(PlayerName)) {//MUST BE LEADER
            				player.sendMessage(ChatColor.RED + "You must be the leader to disband the team.");
            				return true;
            			}
            			else {//DISBAND TEAM
            				String TeamKey = tPlayer.getTeamKey();
            				disbandTeam(TeamKey);
            				player.sendMessage(ChatColor.GREEN + "Your team has been succesfully disbanded.");

            			}
            			break;
                	/* ==============================================================================
                	 *	TEAM TAG - Sets a team's tag
                	 * ============================================================================== */
            		case "TAG": 
            			if(!player.hasPermission("Clans.tag") || !config.UseTags()) {
            				player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            				return true;
            			}
            			else if(!tPlayer.hasTeam()){ //NO TEAM
            				player.sendMessage(ChatColor.RED + "You are not in a team.");
            				return true;
            			}
            			else if(!canAfford(PlayerName,config.getTagCost()))
            			{
            				player.sendMessage(ChatColor.RED + "Using this command costs " + config.getTagCost() + " of " + getCurrencyName() + " (Must have in Inventory).");
            				return true;
            			}
            			else if(!getTeam(PlayerName).isLeader(PlayerName)){
            				player.sendMessage(ChatColor.RED + "Must be team leader to edit tag.");
            				return true;
            			}
            			else if(args.length == 1){//PRINT CURRENT TAG
            				player.sendMessage(ChatColor.GREEN + "Your current tag is [" + getTeam(PlayerName).getTeamTag() + "]. /team tag <NewTag> to change tag.");
            				return true;
            			}
            			else if(args[1].length() < 2){//NOT ENOUGH CHARACTERS
            				player.sendMessage(ChatColor.RED + "Tags must be at least three characters.");
            				return true;
            			}
            			else if(args[1].length() > 7){//TOO MANY CHARACTERS
            				player.sendMessage(ChatColor.RED + "Tags must be less than seven characters.");
            				return true;
            			}
            			else {//CHANGE TAG
            				Teams.get(tPlayer.getTeamKey()).setTeamTag(args[1]);
            				player.sendMessage(ChatColor.GREEN +"Tag has been changed to [" + getTeam(PlayerName).getTeamTag() + "].");
            				spend(PlayerName, config.getTagCost());
            				saveTeams();
            			}
            			break;
                	/* ==============================================================================
                	 *	TEAM COLOR | COLOUR - Sets a team's color
                	 * ============================================================================== */
            		case "COLOR": case "COLOUR": 
            			if(!player.hasPermission("Clans.color")) {
            				player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            				return true;
            			}
            			else if(!tPlayer.hasTeam()){
            				player.sendMessage(ChatColor.RED + "You are not in a team.");
            				return true;
            			}
            			else if(getTeam(PlayerName).getTeamSize() < config.getReqMemColor()){
            				player.sendMessage(ChatColor.RED + "Your team must have " + config.getReqMemColor() + " members to set color.");
            				return true;
            			}
            			else if(config.UseScore() && !isMinScoreRank(tPlayer.getTeamKey(),config.getReqScoreColor())){
            				player.sendMessage(ChatColor.RED + "Your team must be rank " + config.getReqScoreColor() + " or higher to set color.");
            				return true;
            			}
            			else if(!getTeam(PlayerName).isLeader(PlayerName)){//ISNT LEADER
            				player.sendMessage(ChatColor.RED + "Must be the leader to change the team color.");
            				return true;
            			}
            			else if(args.length != 2){//INVALID ARGS
            				player.sendMessage(ChatColor.RED + "Invalid use of command. Use /team color <colorname>.");
            				return true;
            			}
            			else if(!Teams.get(tPlayer.getTeamKey()).validateColor(args[1].toUpperCase())){ //INVALID COLOR
            				player.sendMessage(ChatColor.RED + "Invalid color. Choose from this list of colors: DARK_RED, RED, DARK_AQUA," +
            						"AQUA, DARK_GREEN, GREEN, DARK_BLUE, BLUE, DARK_PURPLE, LIGHT_PURPLE, GOLD, YELLOW, BLACK, GRAY");
            				return true;
            			}
            			else{//SET COLOR
            				Teams.get(tPlayer.getTeamKey()).setColor(args[1]);
            				player.sendMessage(ChatColor.GREEN + "Color changed.");
            				saveTeams();
            			}
            			break;
                    	/* ==============================================================================
                    	 *	TEAM CAPE - Sets a team's cape
                    	 * ============================================================================== */
                		case "CAPE": 
                			if(!player.hasPermission("Clans.cape")) {
                				player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
                				return true;
                			}
                			else if(!config.isAllowCapes())
                			{
                				player.sendMessage(ChatColor.RED + "Capes are disabled on this server.");
                				return true;
                			}
                			else if(!tPlayer.hasTeam()){
                				player.sendMessage(ChatColor.RED + "You are not in a team.");
                				return true;
                			}
                			else if(config.UseScore() && !isMinScoreRank(tPlayer.getTeamKey(),config.getReqScoreCape())){
                				player.sendMessage(ChatColor.RED + "Your team must be rank " + config.getReqScoreCape() + " or higher to set rank.");
                				return true;
                			}
                			else if(!getTeam(PlayerName).isLeader(PlayerName)){//ISNT LEADER
                				player.sendMessage(ChatColor.RED + "Must be the leader to change the team cape.");
                				return true;
                			}
                			else if(args.length != 2){//INVALID ARGS
                				player.sendMessage(ChatColor.RED + "Invalid use of command. Use /team cape <url>.");
                				return true;
                			}
                			else{//SET CAPE
                				
                    			try {
                  				  URL url = new URL(args[1]);
                  				  if(url.toString().endsWith(".png")) {
    	                  			  Teams.get(tPlayer.getTeamKey()).setTeamCapeUrl(args[1]);
    	                  			  addCapes();
                  				  }
                  				  else {
                  					Teams.get(tPlayer.getTeamKey()).setTeamCapeUrl("");
                  					player.sendMessage(ChatColor.RED + "Invalid cape url, file must be a PNG.");
                    				return true;
                  				  }
                  				} catch (MalformedURLException e) {
                  					Teams.get(tPlayer.getTeamKey()).setTeamCapeUrl("");
                  					player.sendMessage(ChatColor.RED + "Invalid cape url, file must be a PNG.");
                    				return true;
                  				}
                				player.sendMessage(ChatColor.GREEN + "Cape url changed.");
                				saveTeams();
                			}
                			break;
                	/* ==============================================================================
                	 *	TEAM MOTD - Set's a team's Message of the Day, prints if no argument 
                	 * ============================================================================== */
            		case "MOTD": 
            			if(!player.hasPermission("Clans.motd")) {
            				player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            				return true;
            			}
            			else if(!tPlayer.hasTeam()){ //NO TEAM
            				player.sendMessage(ChatColor.RED + "You are not in a team.");
            				return true;
            			}
            			else if(args.length == 1){ //DISPLAY MOTD WITH TEAM COLOR
            				ChatColor color = Teams.get(tPlayer.getTeamKey()).getColor();
            				player.sendMessage(color + "[Team MOTD] " + getTeam(PlayerName).getMOTD());
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
            				saveTeams();
            			}
            			break;
                	/* ==============================================================================
                	 *	TEAM HELP - Prints commands and how to use them
                	 * ============================================================================== */
            		case "HELP": 
            			if(args.length == 1){
                   			player.sendMessage(ChatColor.RED + "Use /team help 1...5 to view each page.");
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
                   			player.sendMessage(ChatColor.RED + "/team rankrename <ranknumber> <rankname>"+ChatColor.GRAY +" - Renames a specified rank.");
                   			player.sendMessage(ChatColor.RED + "/team rankset <playername> <ranknumber>"+ChatColor.GRAY +" - Sets the rank of a team member.");
                   			player.sendMessage(ChatColor.RED + "/team rankmassmove <oldranknumber> <newranknumber>"+ChatColor.GRAY +" - Moves all members of a rank to a new rank.");
                   			player.sendMessage(ChatColor.RED + "/team rankinfo <ranknumber>"+ChatColor.GRAY +" - Outputs a rank's permissions.");
                   			player.sendMessage(ChatColor.RED + "/team rankpermission <ranknumber> <kick/teamchat/rankedit/invite/rankset/areainfo> <true/false>"+ChatColor.GRAY +" - Sets a rank's permissions.");
                   			player.sendMessage(ChatColor.RED + "/team rankdelete <ranknumber>"+ChatColor.GRAY +" - Deletes a rank.");
                   		}
                   		else if(args[1].equalsIgnoreCase("4")) {
                   			player.sendMessage(ChatColor.RED + "Team Area Commands:");
                   			player.sendMessage(ChatColor.RED + "/team area claim <area name>"+ChatColor.GRAY +" - Claims an area for the team, If you already have a area it will move it.");
                   			player.sendMessage(ChatColor.RED + "/team area info"+ChatColor.GRAY +" - Prints out detailed information about your team's area.");
                   			player.sendMessage(ChatColor.RED + "/team area upgrade <size/alerter/damager/resistance/cleanser>"+ChatColor.GRAY +" - Gives a team area upgrades that provide benefits.");
                   		}
                   		else if(args[1].equalsIgnoreCase("5")) {
                   			player.sendMessage(ChatColor.RED + "Team Area Upgrades:");
                   			player.sendMessage(ChatColor.RED + "Upgrades: Size"+ChatColor.GRAY +" - Increases team area by 10 blocks.");
                   			player.sendMessage(ChatColor.RED + "Upgrades: Alerter"+ChatColor.GRAY +" - Alerts your team when an outsider places/destroys blocks in your area.");
                   			player.sendMessage(ChatColor.RED + "Upgrades: Damager"+ChatColor.GRAY +" - Damages outsiders for placing/destroying blocks inside your area if your team is offline.");
                   			player.sendMessage(ChatColor.RED + "Upgrades: Resistence"+ChatColor.GRAY +" - Increases the time it takes to destroy blocks in your team area for outsiders.");
                   			player.sendMessage(ChatColor.RED + "Upgrades: Cleanse"+ChatColor.GRAY +" - Periodically cleanses blocks placed by outsiders from your area.");
                   		}
                   		else
                   			player.sendMessage(ChatColor.RED + "Improper use of command, Usage is /team help [1-4] to view each page.");
            			
            			break;
                	/* ==============================================================================
                	 *	TEAM AREA - THIS ISNT SET UP CORRECTLY YET
                	 * ============================================================================== */
            		case "AREA": 
            			if (args.length < 2) {
                   			player.sendMessage(ChatColor.RED + "Improper use of command.");
            				return true;
            			}
                    	switch(args[1].toUpperCase())
                    	{
	                		/* ==============================================================================
	                		 *	TEAM AREA CLAIM - Claims a team area for currency.
	                		 * ============================================================================== */
	                		case "CLAIM":
	                			if(!player.hasPermission("Clans.area.claim")) {
	                				player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
	                				return true;
	                			}
	                			else if(args.length < 2){
	                       			player.sendMessage(ChatColor.RED + "Improper use of command, Usage is /team area <Area Name>.");
	                       			return true;
	                       		}
	                			else if(!tPlayer.hasTeam()){//NO TEAM
	                				player.sendMessage(ChatColor.RED + "You are not in a team.");
	                				return true;
	                			}
	                			else if(getTeam(player.getDisplayName()).getTeamSize() < config.getReqMemArea()){//NO TEAM
	                				player.sendMessage(ChatColor.RED + "You must have " + config.getReqMemArea() + " members to claim an area.");
	                				return true;
	                			}
	                			else if (!getTeam(PlayerName).isLeader(PlayerName)) {//MUST BE LEADER
	                				player.sendMessage(ChatColor.RED + "You must be the leader to claim an area.");
	                				return true;
	                			}
	                			else if(!canAfford(PlayerName,config.getAreaCost()))
	                			{
	                				player.sendMessage(ChatColor.RED + "Using this command costs " + config.getAreaCost() + " of " + getCurrencyName() + " (Must have in Inventory).");
	                				return true;
	                			}
	                			else
	                			{
                    				int x = player.getLocation().getBlockX();
                    				int z = player.getLocation().getBlockZ();
                    				String world = player.getWorld().getName();
                    				String test = checkAreaMax(x,z,world,tPlayer.getTeamKey());
                    				if(!test.equalsIgnoreCase("") && !test.equalsIgnoreCase(tPlayer.getTeamKey()))
                    				{
    	                				player.sendMessage(ChatColor.RED + "You cannot claim an area here, it is to close to a nearby area.");
    	                				return true;
                    				}
                    				else
                    				{
	                    				if(Areas.containsKey(tPlayer.getTeamKey()))	{
	                    					if(!Areas.get(tPlayer.getTeamKey()).getHolder().equalsIgnoreCase(tPlayer.getTeamKey())) //if you do not hold the area
	                    					{
	                    						player.sendMessage(ChatColor.RED + "Your team must currently hold the area to move it.");
	        	                				return true;
	                    					}
	                    					else
	                    					{
			                					//Move Area
			                					spend(player.getDisplayName(),config.getAreaCost());
			                					Areas.get(tPlayer.getTeamKey()).setxLoc(x);
			                					Areas.get(tPlayer.getTeamKey()).setzLoc(z);
			                					player.sendMessage(ChatColor.GREEN + "Team Area has been moved.");
	                    					}
		                				}
		                				else {
		                					spend(player.getDisplayName(),config.getAreaCost());
		                    				int i;
		                    				String AreaName = args[2];
		                    				for(i=3;i<args.length;i++)
		                    					AreaName += " " + args[i];
		                    				
		                    				Areas.put(tPlayer.getTeamKey(),new TeamArea(AreaName, x, z,player.getWorld().getName(), 25, tPlayer.getTeamKey()));
		                    				player.sendMessage(ChatColor.GREEN + "Team area " + AreaName + " was sucessfully created.");
		                				}
	                    				saveAreas();
                    				}
	                			}
	                		break;
	                		/* ==============================================================================
	                		 *	TEAM AREA INFO - Prints a team's area info.
	                		 * ============================================================================== */
	                		case "INFO":     
	                			if(!player.hasPermission("Clans.area.info")) {
	                				player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
	                				return true;
	                			}
	                			else if(!tPlayer.hasTeam()){//NO TEAM
	                				player.sendMessage(ChatColor.RED + "You are not in a team.");
	                				return true;
	                			}
	                			else if(!getRank(PlayerName).canSeeAreaInfo()){ //CAN Area Info
	                    			player.sendMessage(ChatColor.RED + "You lack sufficient permissions to view area info on this team.");
	                    			return true;
	                    		}
	                			else if(!Areas.containsKey(tPlayer.getTeamKey())){ //CAN Area Info
	                    			player.sendMessage(ChatColor.RED + "Your team does not have a team area.");
	                    			return true;
	                    		}
	                			else
	                			{
	                				TeamArea a = Areas.get(tPlayer.getTeamKey());
	                				player.sendMessage(ChatColor.DARK_GREEN +"Areaname: " + ChatColor.GREEN + a.getAreaName());
	                				player.sendMessage(ChatColor.DARK_GREEN +"Owned By: " + ChatColor.GREEN + tPlayer.getTeamKey());
	                				player.sendMessage(ChatColor.DARK_GREEN +"Held By: " + ChatColor.GREEN + a.getHolder());
	                				
	                				int xMax = a.getxLoc()+a.getAreaRadius();
	                				int xMin = a.getxLoc()-a.getAreaRadius();
	                				int zMax = a.getzLoc()+a.getAreaRadius();
	                				int zMin = a.getzLoc()-a.getAreaRadius();
	                				player.sendMessage(ChatColor.DARK_GREEN +"Location: " + ChatColor.GREEN + "  MAX: {"+xMax + ", "+zMax +"}   MIN: {"+xMin + ", "+zMin +"}");
	                				player.sendMessage(ChatColor.DARK_GREEN +"Size: " + ChatColor.GREEN + a.getAreaRadius()*2 + "x"+ a.getAreaRadius()*2);
	                				
	                				String Upgrades = "";
	                				if(a.hasUpgradeAlerter())
	                					Upgrades += "Alerter";
	                				if(a.hasUpgradeDamager())
	                				{
	                					if(!Upgrades.equalsIgnoreCase(""))
	                						Upgrades += ", Damager";
	                					else
	                						Upgrades += "DestroyDamage";
	                				}
	                				if(a.hasUpgradeResistance())
	                				{
	                					if(!Upgrades.equalsIgnoreCase(""))
	                						Upgrades += ", Resistance";
	                					else
	                						Upgrades += "Resistance";
	                				}
	                				if(a.hasUpgradeCleanser())
	                				{
	                					if(!Upgrades.equalsIgnoreCase(""))
	                						Upgrades += ", Cleanser";
	                					else
	                						Upgrades += "Cleanse";
	                				}
                					if(Upgrades.equalsIgnoreCase(""))
                						Upgrades += "None";
	                				player.sendMessage(ChatColor.DARK_GREEN +"Upgrades: " + ChatColor.GREEN + Upgrades);
	                			}
	                		break;
                    		/* ==============================================================================
                    		 *	TEAM AREA UPGRADE - Upgrades an area for currency.
                    		 * ============================================================================== */
                    		case "UPGRADE":    
                    			if(!player.hasPermission("Clans.area.upgrade")) {
	                				player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
	                				return true;
	                			}
                    			else if(!config.AllowUpgrades()) {
	                				player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
	                				return true;
	                			}
	                			else if(args.length <= 2){
	                       			player.sendMessage(ChatColor.RED + "Improper use of command, Usage is /team upgrade <Type>.");
	                       			return true;
	                       		}
	                			else if(!tPlayer.hasTeam()){//NO TEAM
	                				player.sendMessage(ChatColor.RED + "You are not in a team.");
	                				return true;
	                			}
	                			else if(!Areas.containsKey(tPlayer.getTeamKey())){ //CAN Area Info
	                    			player.sendMessage(ChatColor.RED + "Your team does not have a team area.");
	                    			return true;
	                    		}
	                			else if (!getTeam(PlayerName).isLeader(PlayerName)) {//MUST BE LEADER
	                				player.sendMessage(ChatColor.RED + "You must be the leader to disband the team.");
	                				return true;
	                			}
	                			else
	                			{
	                				if(args[2].equalsIgnoreCase("size"))
	                				{
	                					if(Areas.get(tPlayer.getTeamKey()).getAreaRadius()*2 > config.getAreaMaxSize()) {
	    	                       			player.sendMessage(ChatColor.RED + "Your team area is already at max size.");
		                       				return true;
	                					}
	    	                			else if(!canAfford(PlayerName,config.getIncSizeCost()))	{
	    	                				player.sendMessage(ChatColor.RED + "Using this command costs " + config.getIncSizeCost() + " of " + getCurrencyName() + " (Must have in Inventory).");
	    	                				return true;
	    	                			}
	    	                			else {
	    	                				spend(player.getDisplayName(),config.getIncSizeCost());
	    	                				Areas.get(tPlayer.getTeamKey()).increaseRadius(5);
	    	                				player.sendMessage(ChatColor.GREEN + "Area radius has been increased to " + Areas.get(tPlayer.getTeamKey()).getAreaRadius() +".");
	    	                				saveAreas();
	    	                			}
	                					
	                				}
	                				else if(args[2].equalsIgnoreCase("resistance"))
	                				{
	                					if(!config.isUPBlockResist()) {
	    	                       			player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
		                       				return true;
	                					}
	                					if(Areas.get(tPlayer.getTeamKey()).hasUpgradeResistance()) {
	    	                       			player.sendMessage(ChatColor.RED + "Your team area is already has the resistance upgrade.");
		                       				return true;
	                					}
	    	                			else if(!canAfford(PlayerName,config.getUPResistCost()))	{
	    	                				player.sendMessage(ChatColor.RED + "Using this command costs " + config.getUPResistCost() + " of " + getCurrencyName() + " (Must have in Inventory).");
	    	                				return true;
	    	                			}
	    	                			else {
	    	                				spend(player.getDisplayName(),config.getUPResistCost());
	    	                				Areas.get(tPlayer.getTeamKey()).setUpgradeResistance(true);
	    	                				player.sendMessage(ChatColor.GREEN + "Your team area now has the resistance upgrade.");
	    	                				saveAreas();
	    	                			}
	                				}
	                				else if(args[2].equalsIgnoreCase("alerter"))
	                				{
	                					if(!config.isUPIntruderAlert()) {
	    	                       			player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
		                       				return true;
	                					}
	                					if(Areas.get(tPlayer.getTeamKey()).hasUpgradeAlerter()) {
	    	                       			player.sendMessage(ChatColor.RED + "Your team area is already has the alerter upgrade.");
		                       				return true;
	                					}
	    	                			else if(!canAfford(PlayerName,config.getUPAlertsCost()))	{
	    	                				player.sendMessage(ChatColor.RED + "Using this command costs " + config.getUPAlertsCost() + " of " + getCurrencyName() + " (Must have in Inventory).");
	    	                				return true;
	    	                			}
	    	                			else {
	    	                				spend(player.getDisplayName(),config.getUPAlertsCost());
	    	                				Areas.get(tPlayer.getTeamKey()).setUpgradeAlerter(true);
	    	                				player.sendMessage(ChatColor.GREEN + "Your team area now has the alerter upgrade.");
	    	                				saveAreas();
	    	                			}
	                				}
	                				else if(args[2].equalsIgnoreCase("damager"))
	                				{
	                					if(!config.isUPOfflineDamage()) {
	    	                       			player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
		                       				return true;
	                					}
	                					if(Areas.get(tPlayer.getTeamKey()).hasUpgradeDamager()) {
	    	                       			player.sendMessage(ChatColor.RED + "Your team area is already has the damager upgrade.");
		                       				return true;
	                					}
	    	                			else if(!canAfford(PlayerName,config.getUPDamageCost()))	{
	    	                				player.sendMessage(ChatColor.RED + "Using this command costs " + config.getUPDamageCost() + " of " + getCurrencyName() + " (Must have in Inventory).");
	    	                				return true;
	    	                			}
	    	                			else {
	    	                				spend(player.getDisplayName(),config.getUPDamageCost());
	    	                				Areas.get(tPlayer.getTeamKey()).setUpgradeDamager(true);
	    	                				player.sendMessage(ChatColor.GREEN + "Your team area now has the damager upgrade.");
	    	                				saveAreas();
	    	                			}
	                				}
	                				else if(args[2].equalsIgnoreCase("cleanser"))
	                				{
	                					if(!config.isUPCleanse()) {
	    	                       			player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
		                       				return true;
	                					}
	                					if(Areas.get(tPlayer.getTeamKey()).hasUpgradeCleanser()) {
	    	                       			player.sendMessage(ChatColor.RED + "Your team area is already has the cleanser upgrade.");
		                       				return true;
	                					}
	    	                			else if(!canAfford(PlayerName,config.getUPCleanseCost()))	{
	    	                				player.sendMessage(ChatColor.RED + "Using this command costs " + config.getUPCleanseCost() + " of " + getCurrencyName() + " (Must have in Inventory).");
	    	                				return true;
	    	                			}
	    	                			else {
	    	                				spend(player.getDisplayName(),config.getUPCleanseCost());
	    	                				Areas.get(tPlayer.getTeamKey()).setUpgradeCleanser(true);
	    	                				player.sendMessage(ChatColor.GREEN + "Your team area now has the cleanser upgrade.");
	    	                				saveAreas();
	    	                			}
	                					
	                				}
		                			else{
		                       			player.sendMessage(ChatColor.RED + "Improper upgrade type, Usage is /team upgrade <Size/Resistance/Alerter/Damager/Cleanser>.");
		                       			return true;
		                       		}
	                			}
                    		break;
                    		case "LIST":  
	                			if(!player.hasPermission("Clans.area.info")) {
	                				player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
	                				return true;
	                			}
	                			else if(!tPlayer.hasTeam()){//NO TEAM
	                				player.sendMessage(ChatColor.RED + "You are not in a team.");
	                				return true;
	                			}
	                			else if(!getRank(PlayerName).canSeeAreaInfo()){ //CAN Area Info
	                    			player.sendMessage(ChatColor.RED + "You lack sufficient permissions to view area list on this team.");
	                    			return true;
	                    		}
	                			else
	                			{
	                				String tkey = tPlayer.getTeamKey();
	                				player.sendMessage(ChatColor.DARK_GREEN + "Currently Captured Areas:");
	                				boolean any = false;
	                				for(String a : Areas.keySet())
	                				{
	                					if(tkey.equalsIgnoreCase(Areas.get(a).getHolder()))
	                					{
	                						player.sendMessage(ChatColor.GREEN + Teams.get(a).getTeamTag() + " - " + getArea(a).getAreaName());
	                						player.sendMessage(ChatColor.GREEN  + "     X:" + getArea(a).getxLoc() + " Z:" + getArea(a).getzLoc());
	                						any = true;
	                					}
	                				}
	                				if(!any)
	                					player.sendMessage(ChatColor.GREEN + "None.");
	                			
	                			}
                    		break;
                    	}
                    	
            	}
            	return true;
            }
            else if(commandName.equals("t"))
            {
    			if(!player.hasPermission("Clans.teamchat")) {
    				player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
    				return true;
    			}
   			 	if(!tPlayer.hasTeam()) {
   			 		player.sendMessage(ChatColor.RED + "You are not on a team.");
   			 		return true;
   			 	}
   			 	else if(!getRank(PlayerName).canTeamChat()) {
   			 		player.sendMessage(ChatColor.RED + "You lack sufficient permissions to talk in team chat.");
   			 		return true;
   			 	}
   			 	else if (args.length < 1) {
   			 		player.sendMessage(ChatColor.RED + "You did not enter a message to send.");
   			 		return true;
   			 	}
   			 	else if (args[0].equalsIgnoreCase("@loc"))
   			 	{
   			 		String message = "I am at X:"+ player.getLocation().getBlockX() + " Z:" + player.getLocation().getBlockZ() + " Y:" + player.getLocation().getBlockY() +".";
	  				messageTeam(tPlayer.getTeamKey(),ChatColor.DARK_GREEN + PlayerName + ": " + ChatColor.GREEN  + message);    	
   			 	}
   			 	else {
     				int i;
     				String message = args[0];
     				for(i=1;i<args.length;i++)
     					message += " " + args[i];
	  				String teamKey = tPlayer.getTeamKey();			 
	  				
	  				messageTeam(teamKey,ChatColor.DARK_GREEN + PlayerName + ": " + ChatColor.GREEN  + message);    				 
   			 	}
            }
            else if(commandName.equals("cap") || commandName.equals("capture"))
            {
   			 	String area = findArea(player.getLocation().getBlockX(), player.getLocation().getBlockZ(), player.getWorld().getName());
    			if(!player.hasPermission("Clans.area.capture")) {
    				player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
    				return true;
    			}
    			else if(!tPlayer.hasTeam()) {
   			 		player.sendMessage(ChatColor.RED + "You are not on a team.");
   			 		return true;
   			 	}
    			else if(area.equalsIgnoreCase(""))
   			 	{
   			 		player.sendMessage(ChatColor.RED + "You are not within the boundaries of a team's area.");
   			 		return true;
   			 	}
    			else if(getArea(area).getHolder().equalsIgnoreCase(tPlayer.getTeamKey()))
   			 	{
   			 		player.sendMessage(ChatColor.RED + "Your team already holds this area.");
   			 		return true;
   			 	}
   			 	//holders arent online or is team area is not your own
    			else if(Teams.get(getArea(area).getHolder()).getOnlineCount() == 0 && !area.equalsIgnoreCase(tPlayer.getTeamKey()))
   			 	{
   			 		player.sendMessage(ChatColor.RED + "You cannot besiege the areas of teams that are offline.");
   			 		return true;
   			 	}
    			else
    			{
	   			 	int[] res = countInCapturableArea(area, getArea(area).getHolder(), tPlayer.getTeamKey());
	   			 	if(res[0] < 0 )
	   			 	{
	   			 		//start new capture
	   			 		//area:defenders:attackers
	   			 		String key = area+":"+getArea(area).getHolder()+":"+tPlayer.getTeamKey();
	   			 		if(ContestedAreas.containsKey(key)) {
	   	   			 		player.sendMessage(ChatColor.RED + "Your team is already capturing this area.");
	   	   			 		return true;
	   			 		}
	   			 		else
	   			 		{
		   			 		ContestedAreas.put(key, new AreaContest(this, area, getArea(area).getHolder(), tPlayer.getTeamKey()));
		   			 		getServer().getScheduler().scheduleSyncDelayedTask(this, ContestedAreas.get(key), 200L);
		   			 		//send message
		   			 		getServer().broadcastMessage(ChatColor.DARK_RED + "[RAID] " + ChatColor.YELLOW + tPlayer.getTeamKey() + ChatColor.GOLD +" has besieged " + 
		   			 				getArea(area).getAreaName() + ", currently held by " + ChatColor.YELLOW +getArea(area).getHolder() + ChatColor.GOLD +".");
	   			 		}
	   			 	}
	   			 	else
	   			 	{
   	   			 		player.sendMessage(ChatColor.RED + "You do not have the population advantage in this area to contest it.");
   	   			 		return true;
	   			 	}
    			}
            }
            else
            {
            	return true;
            }
        return true;    
        }   
        else
        {
        	return true;
        }
	}
	private void disbandTeam(String TeamKey) {
		
		ArrayList<String> members = Teams.get(TeamKey).getAllMembers();
		for(String mem : members)
			Users.get(mem).clearTeamKey();
		Teams.remove(TeamKey);
		
		//Search through the areas, if this team holds any, return to original owner
		for(String areaTeam : Areas.keySet())
		{
			if(Areas.get(areaTeam).getHolder().equalsIgnoreCase(TeamKey))
				Areas.get(areaTeam).setHolder(areaTeam);
		}
		
		if(Areas.containsKey(TeamKey))
			Areas.remove(TeamKey);
		saveTeams();
		saveAreas();
		
	}
	public void startCleansers()
	{

		getServer().getScheduler().scheduleAsyncRepeatingTask(this, new Runnable() {
			public void run() {

				for(String area : Areas.keySet())
				{
					if(Areas.get(area).hasUpgradeCleanser()){ //CAN cleanse
						cleanseArea(area);
					}
				}
			}
		}, 36000L, 36000L);
	}
	public void startScoreKeeper()
	{
		getServer().getScheduler().scheduleAsyncRepeatingTask(this, new Runnable() {

			   public void run() {
				   
				   HashMap<String, Integer> rewardsArea = new HashMap<String, Integer>();
				   HashMap<String, Double> rewardsPop = new HashMap<String, Double>();
				   
				   //tally score from areas
				   HashMap<String, Integer> arPoints = new HashMap<String, Integer>();
				   for(String area : Areas.keySet())
				   {
					   String holder = Areas.get(area).getHolder();
					   if(Teams.containsKey(holder))
					   {
						   int score = Areas.get(area).getAreaRadius() * 2;
						   if(holder.equalsIgnoreCase(area))// score * 2 if online, score /4 if offline
						   {
							   Teams.get(holder).addScore(score);
							   messageTeam(holder, ChatColor.GOLD + "Your team gains " + score + "pts for holding your homeland.");
						   }
						   else if (Teams.get(holder).getOnlineCount() > 0)
						   {
							   Teams.get(holder).addScore(score*2);
							   Teams.get(area).addScore(-1*score/2);
							   if(!arPoints.containsKey(holder))
								   arPoints.put(holder, 0);
							   int addedPts = arPoints.get(holder) + (score*2);
							   arPoints.put(holder, addedPts);
							   messageTeam(area, ChatColor.RED + "Your team has lost " + score/2 + "pts because you do not hold your homeland.");
						   }
						   
						   //Add teams area count to rewardsArea
						   if(!rewardsArea.containsKey(holder))
							   rewardsArea.put(holder, 1);
						   else
							   rewardsArea.put(holder, rewardsArea.get(holder)+1);
					   }
				   }
				   for(String s : arPoints.keySet())
					   messageTeam(s, ChatColor.GOLD + "Your team gains " + arPoints.get(s) + "pts for holding enemy lands.");
				   HashMap<String, Integer> popPoints = new HashMap<String, Integer>();
				   for(Player p : getServer().getOnlinePlayers())
				   {
					   TeamPlayer t = Users.get(p.getDisplayName());
					   if(t.hasTeam())
					   {
						   if(!popPoints.containsKey(t.getTeamKey()))
						   {
							   int pts = Teams.get(t.getTeamKey()).getTeamSize() * 2;
							   popPoints.put(t.getTeamKey(), pts);
							   //if in team area 10 -2 
							   //if outside 5-2
						   }
						   if(Areas.containsKey(t.getTeamKey())) {
							   if(Areas.get(t.getTeamKey()).inArea(p) && t.getTeamKey().equalsIgnoreCase(Areas.get(t.getTeamKey()).getHolder())) {
								   popPoints.put(t.getTeamKey(), popPoints.get(t.getTeamKey())+8);
								   if(!rewardsPop.containsKey(t.getTeamKey()))
									   rewardsPop.put(t.getTeamKey(), 0.5);
								   else
									   rewardsPop.put(t.getTeamKey(), rewardsPop.get(t.getTeamKey())+(.5));
							   }
							   else {
								   popPoints.put(t.getTeamKey(), popPoints.get(t.getTeamKey())+3);
								   if(!rewardsPop.containsKey(t.getTeamKey()))
									   rewardsPop.put(t.getTeamKey(), .25);
								   else
									   rewardsPop.put(t.getTeamKey(), rewardsPop.get(t.getTeamKey())+(.25));
							   }
						   }
						   else
						   {
							   popPoints.put(t.getTeamKey(), popPoints.get(t.getTeamKey())+3);
							   if(!rewardsPop.containsKey(t.getTeamKey()))
								   rewardsPop.put(t.getTeamKey(), .25);
							   else
								   rewardsPop.put(t.getTeamKey(), rewardsPop.get(t.getTeamKey())+(.25));
						   }
					   }
				   }
				   for(String s : popPoints.keySet()) {
					   messageTeam(s, ChatColor.GOLD + "Your team gains " + popPoints.get(s)+ "pts from your teams population.");
					   Teams.get(s).addScore(popPoints.get(s));
				   }
				   for(Player p : getServer().getOnlinePlayers())
				   {
					   TeamPlayer tp = Users.get(p.getDisplayName());
					   if(tp.hasTeam())
					   {
						   if(rewardsArea.containsKey(tp.getTeamKey()))
						   {
								int areanum = rewardsArea.get(tp.getTeamKey());
								double rewardFunction = 1.5*Math.sqrt(areanum * rewardsPop.get(tp.getTeamKey()));
								int diamonds = (int)rewardFunction;
								int iron = (int)((rewardFunction - diamonds)*10);
								p.sendMessage(ChatColor.LIGHT_PURPLE + "[REWARD] " + ChatColor.GOLD + "You have received " + diamonds + " diamond and " + iron + " iron from the lands your team holds.");
								if(diamonds != 0)
									p.getInventory().addItem(new ItemStack(264,diamonds));
								if(iron != 0)
									p.getInventory().addItem(new ItemStack(265,iron));
						   }
					   }
				   }
				   //GIVE EXP
				   String topTeam = getTopRankedTeams(1).get(0).TeamName;
				   for(Player p : getServer().getOnlinePlayers())
				   {
					   if(Users.get(p.getDisplayName()).getTeamKey().equalsIgnoreCase(topTeam)) {
						   ExperienceUtils.changeExp(p, 100);
						   //p.setTotalExperience(p.getTotalExperience()+100);
						   p.sendMessage(ChatColor.LIGHT_PURPLE + "[REWARD] " + ChatColor.GOLD + "You have gained 100 exp for being on the top ranked team.");
					   }
				   }
				   
				   checkTeamColors();
				   checkTeamCapes();
				   outputToMySQL();
				   saveTeams();
			   }
			}, 18000L, 36000L);
	}
	private void outputToMySQL() {
	
		if(config.isOutputMySQL())
		{
		  String ip = config.getMySQLHost();
		  String db = config.getMySQLDB();
		  String username = config.getMySQLUser();
		  String password = config.getMySQLPassword();
		  
		  Connection con = null;
		  String url = "jdbc:mysql://"+ip+":3306/";
		  String driver = "com.mysql.jdbc.Driver";
		  ResultSet rs = null;
		  try{
			  Class.forName(driver);
			  con = DriverManager.getConnection(url+db,username,password);
			  Statement st = con.createStatement();
			  //Create table if not created already
			  try{
			  	  st.executeUpdate("CREATE TABLE teams(teamname varchar(90), tag varchar(50), tsize number(4), score number (8))");
			  }
			  catch (SQLException s){
				  System.out.println("SQL statement is not executed!");
			  }
			  //go through teams, if they exist update them, if not
			  for(String TeamName : Teams.keySet())
			  {
				  String tag = Teams.get(TeamName).getTeamTag();
				  int size = Teams.get(TeamName).getTeamSize();
				  int score = Teams.get(TeamName).getTeamScore();
				  rs = st.executeQuery("select * from teams where='"+TeamName+"'");
				  if(rs.next()) { //If tuple exists
					  st.executeUpdate("update teams set score='"+score+"', size='"+size+"', tag='"+tag+"' where teamname='"+TeamName+"'");
				  }
				  else //create tuple
				  {
					  st.executeUpdate("insert into teams values('"+TeamName+"','"+tag+"',"+size+","+score+")");
				  }
				  rs = null;
			  }
		  }
		  catch (Exception e){
			  	System.out.println("[MYSQL]" + e.getMessage());
			  }
		  if(con != null) {
			try {
				con.close();
			} catch (SQLException e) {System.out.println(e.getMessage());}
		  }
		}
	}
	public void checkTeamCapes()
	{
		ArrayList<TeamScoreNode> list = getTopRankedTeams(Teams.size());

		int i = Teams.size()-1;
		while (i > config.getReqScoreCape())
		{
			Teams.get(list.get(i).TeamName).setTeamCapeUrl("");
			i--;
		}
	}
	public void checkTeamColors()
	{
		ArrayList<TeamScoreNode> list = getTopRankedTeams(Teams.size());

		int i = Teams.size()-1;
		while (i > config.getReqScoreColor())
		{
			if(!Teams.get(list.get(i).TeamName).getColorName().equals("GRAY"))
				Teams.get(list.get(i).TeamName).setColor("GRAY");
			i--;
		}
	}
	public boolean isMinScoreRank(String TeamName, int rank)
	{
		ArrayList<TeamScoreNode> list = getTopRankedTeams(rank);
		for(TeamScoreNode tsn : list) {
			if(TeamName.equalsIgnoreCase(tsn.TeamName))
				return true;
		}
		return false;	
	}
	public ArrayList<TeamScoreNode> getTopRankedTeams (int listSize)
	{
		ArrayList<TeamScoreNode> TopTeams = new ArrayList<TeamScoreNode>();
		boolean start = false;
		for(String teamName : Teams.keySet())
		{
			int ts = Teams.get(teamName).getTeamScore();
			TopTeams.add(new TeamScoreNode(teamName,ts));
			if(start) {
				for(int i = TopTeams.size()-1; i >= 1; i--)
				{
					if(TopTeams.get(i-1).TeamScore < TopTeams.get(i).TeamScore)
					{
						TeamScoreNode temp = TopTeams.get(i);
						TopTeams.set(i, TopTeams.get(i-1));
						TopTeams.set(i-1, temp);
					}
					else
					{
						if(TopTeams.size() >= listSize+1)
							TopTeams.remove(TopTeams.size()-1);
						break;
					}
				}
			}
			start = true;;
		}
		return TopTeams;
	}
	public void continueSiege(String area, String defenders, String attackers)
	{
		String key = area+":"+defenders+":"+attackers;
		if(!getArea(area).getHolder().equalsIgnoreCase(defenders)) //cancel
		{
			ContestedAreas.remove(key);
			messageTeam(attackers, "The area you were attacking has been captured by another team. Use /cap to restart the siege against the new defenders." );
		}
		else
		{
			getServer().getScheduler().scheduleSyncDelayedTask(this, ContestedAreas.get(key), 200L);
		}
	}
	public void printSiegeProgress(String area, String defenders, String attackers,int numAtt, int numDef, int hp, int change)
	{
		
		int diffDef = numDef - numAtt;
		int diffAtt = numAtt - numDef;
		
		change = Math.abs(change);
		
		//print to attackers
		String msg = ChatColor.GOLD + " ["+numAtt+"v"+numDef+" | ";
		if(diffAtt < 0)
			msg += "-";
		msg += change + "pt] Capture Progress: ["+ChatColor.GREEN;
		int i = 0;
		for(i=0;i<(100-hp);i++) {
			if(i % 5 == 0)
				msg+="|";
		}
		msg += ChatColor.RED;
		for(i=0;i<hp;i++) {
			if(i % 5 == 0)
				msg+="|";
		}
		int pct = (int) (100 *((double)(100-hp)/100));
		msg += ChatColor.GOLD + "] " + pct + "%";
		messageTeam(attackers, msg);
		
		//print to defenders
		msg = ChatColor.GOLD + " ["+numDef+"v"+numAtt+" | ";
		if(diffDef < 0)
			msg += "-";
		msg += change + "pt] Defend Progress: ["+ChatColor.GREEN;
		i = 0;
		for(i=0;i<hp;i++) {
			if(i % 5 == 0)
				msg+="|";
		}
		msg += ChatColor.RED;
		for(i=0;i<(100-hp);i++) {
			if(i % 5 == 0)
				msg+="|";
		}
		pct = (int) (100 *((double)hp/100));
		msg += ChatColor.GOLD + "] " + pct + "%";
		messageTeam(defenders, msg);

	}
	public void declareWinner(String area, String defenders, String attackers, String winner)
	{
		Areas.get(area).setHolder(winner);
		if(winner.equalsIgnoreCase(attackers))
		{
			//attackers win message
		 	getServer().broadcastMessage(ChatColor.DARK_RED + "[RAID] " + ChatColor.GREEN + winner + ChatColor.GOLD +" has successfully captured " + getArea(area).getAreaName() + " from " + ChatColor.RED + defenders + ChatColor.GOLD +".");
		 	
		}
		else //defenders won
		{
			//defenders win message
		 	getServer().broadcastMessage(ChatColor.DARK_RED + "[RAID] " + ChatColor.GREEN + winner + ChatColor.GOLD +" has successfully defended " + getArea(area).getAreaName() + " against " + ChatColor.RED + attackers + ChatColor.GOLD +".");
		}
		String key = area+":"+defenders+":"+attackers;
		ContestedAreas.remove(key);
		saveAreas();
		//remove from contests
	}
	public int[] countInCapturableArea(String area, String defenders, String attackers)
	{
		int[] results = {0,0,0}; //first is difference, second is defenders in area, third is attackers in area

		Player[] onlineList = getServer().getOnlinePlayers();  				 
		 
		for (Player p : onlineList) {
			String userTeamKey = Users.get(p.getDisplayName()).getTeamKey();
			if(userTeamKey.equals(attackers)) {
				if(Areas.get(area).inAreaCapturable(p))
					results[2]++;
			}
			if(userTeamKey.equals(defenders)) {
				if(Areas.get(area).inAreaCapturable(p))
					results[1]++;
			}
		}
		results[0] = results[1] - results[2]; //defense - attack
		//System.out.println(results[0]);
		return results;
	}
	//Finds a team area given a point, returns the team name who owns the area if found
	public String findArea(int x, int z, String world)
	{
		String result = "";	
		for(String key : Areas.keySet()){
			if(Areas.get(key).inArea(x, z, world))
				return key;
		}
		return result;
	}
	public TeamArea getArea(String teamName)
	{
		return Areas.get(teamName);
	}
	//Used for creating and moving areas
	private String checkAreaMax(int x, int z, String world, String team)
	{
		String result = "";
		for(String key : Areas.keySet())
		{
			TeamArea a = Areas.get(key);
			int maxRadius = config.getAreaMaxSize()/2;
			if(inAreaMax(x+maxRadius,z+maxRadius,world,a)
					||inAreaMax(x+maxRadius,z-maxRadius,world,a)
					||inAreaMax(x-maxRadius,z+maxRadius,world,a)
					||inAreaMax(x-maxRadius,z-maxRadius,world,a)
					||inAreaMax(x,z,world,a))
			{
				if(!team.equalsIgnoreCase(key)) {
					result = key;
					break;
				}
				else if (result.equalsIgnoreCase("")) {
					result = key;
				}
			}
		}
		int maxR = config.getAreaMaxSize()/2;
		for(BlackArea ba : BlacklistedAreas)
		{
			if(ba.inArea(x+maxR, z+maxR, world) ||
					ba.inArea(x+maxR, z-maxR, world) ||
					ba.inArea(x-maxR, z+maxR, world) ||
					ba.inArea(x-maxR, z-maxR, world) ||
					ba.inArea(x, z, world)) {
				return "@Deny";
			}
		}
		return result;
	}
	private boolean inAreaMax(int x, int z, String world, TeamArea a)
	{
		boolean inArea = false;
		int maxRadius = config.getAreaMaxSize()/2;
		if(a.getWorld().equalsIgnoreCase(world)) {
			if(a.getxLoc()-maxRadius <= x && x <= a.getxLoc()+maxRadius) {
	    		if(a.getzLoc()-maxRadius <= z && z <= a.getzLoc()+maxRadius) {
	    			inArea = true;
	    		}
			}
		}
		return inArea;
	}
	private boolean isInteger( String input )  
 	{  
 		try  {  
 			Integer.parseInt( input );  
 			return true;  
 		}  
 		catch( Exception e )  {  
 			return false;  
 		}  
 	} 
 	private boolean canAfford(String PlayerName, int cost)
 	{
 		boolean canAfford = false;
 		if(cost == 0)
 			canAfford = true;
 		else if(getServer().getPlayer(PlayerName).getInventory().contains(config.getCurrency(),cost))
 			canAfford = true;

 		return canAfford;
 	}
 	private void spend(String PlayerName, int cost)
 	{
 		if(cost > 0)
 		{
 			getServer().getPlayer(PlayerName).getInventory().removeItem(new ItemStack(config.getCurrency(),cost));
 		}
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
        		int month = Integer.parseInt(sDate[0])-1;
        		int day = Integer.parseInt(sDate[1]);
        		int year = Integer.parseInt(sDate[2]);
        		Calendar cal = Calendar.getInstance();
        		cal.set(year, month, day);
        		Users.put(key, new TeamPlayer(cal, config.TeamTKDefault()));
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
			   String Cape = "";
    		   if(t.containsKey("Cape"))
    			   Cape = (String) t.get("Cape");

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
    			   if(Tier.get("Members") != null){
    				   HashSet<String> Mems = new HashSet<String>((ArrayList<String>)Tier.get("Members"));
    				   
    				   HashSet<String> MemsCopy = Mems;
    				   for(String PlayerName : MemsCopy) {
    					   if(Users.containsKey(PlayerName))
    						   Users.get(PlayerName).setTeamKey(key);
    					   else
    						   Mems.remove(PlayerName);
    				   }
    				   //Add Tier to TeamList
    				   TeamList.add(new TierList(newRank, Mems));
    			   }
    			   else
    				   TeamList.add(new TierList(newRank, new HashSet<String>()));
    		   }
    		   //Add to Teams
    		   Teams.put(key, new Team(TeamList, MOTD, Score, Tag, Color, Cape));
    		   
    		   if(Teams.get(key).getTeamSize() < config.getReqMemColor())
    		   {
    			   Teams.get(key).setColor("GRAY");
    		   }
    	   }
       }
		/*
		 * LOAD AREAS FROM FILE
		 * 
		 */
       if(config.UseAreas())
       {
    		HashMap<String,HashMap<String,Object>> al = null;
    		Yaml yamlAreas = new Yaml();
    		reader = null;
            try {
                reader = new FileReader(AreasFile);
            } catch (final FileNotFoundException fnfe) {
            	 System.out.println("Areas.YML Not Found!");
            	   try{
    	            	  String strManyDirectories="plugins/Clans";
    	            	  boolean success = (new File(strManyDirectories)).mkdirs();
    	            	  }catch (Exception e){//Catch exception if any
    	            	  System.err.println("Error: " + e.getMessage());
    	            	  }
            } finally {
                if (null != reader) {
                    try {
                        al = (HashMap<String,HashMap<String,Object>>)yamlPlayers.load(reader);
                        reader.close();
                    } catch (final IOException ioe) {
                        System.err.println("We got the following exception trying to clean up the reader: " + ioe);
                    }
                }
            }
            if(al != null)
            {
            	for(String key : al.keySet())
            	{
            		HashMap<String,Object> AreaData = al.get(key);
            		String areaType = (String) AreaData.get("Type");
            		if(areaType.equalsIgnoreCase("Clan"))
            		{
            			String ClanKey = key;
            			String areaName = (String) AreaData.get("Name");
            			String hold = (String) AreaData.get("Holder");
            			int xLoc = Integer.parseInt((String) AreaData.get("X"));
            			int zLoc = Integer.parseInt((String) AreaData.get("Z"));
            			String World = (String) AreaData.get("World");
            			int areaRadius = Integer.parseInt((String) AreaData.get("Radius"));
            			HashMap<String,Boolean> upgrades = (HashMap<String,Boolean>)AreaData.get("Upgrades");
            			boolean BlockDestroyDamage = upgrades.get("BlockDestroyDamage");
            			boolean BlockResistance = upgrades.get("BlockResistance");
            			boolean AreaCleanse = upgrades.get("AreaCleanse");
            			boolean IntruderAlert = upgrades.get("IntruderAlert");
            			
            			Areas.put(ClanKey, new TeamArea(areaName,xLoc, zLoc, World, areaRadius, hold, IntruderAlert,  BlockResistance, BlockDestroyDamage, AreaCleanse));
            		}
            	}
            }
            //BLACKLIST AREAS
            HashMap<String,HashMap<String,Object>> bA = null;
    		Yaml yamlBlacklist = new Yaml();
    		reader = null;
            try {
                reader = new FileReader(BlacklistFile);
            } catch (final FileNotFoundException fnfe) {
            	 System.out.println("BlacklistAreas.YML Not Found!");
            	   try{
    	            	  String strManyDirectories="plugins/Clans";
    	            	  boolean success = (new File(strManyDirectories)).mkdirs();
    	            	  }catch (Exception e){//Catch exception if any
    	            	  System.err.println("Error: " + e.getMessage());
    	            	  }
            } finally {
                if (null != reader) {
                    try {
                        bA = (HashMap<String, HashMap<String, Object>>) yamlPlayers.load(reader);
                        reader.close();
                    } catch (final IOException ioe) {
                        System.err.println("We got the following exception trying to clean up the reader: " + ioe);
                    }
                }
            }
            if(bA != null)
            {
            	for(String key : bA.keySet())
            	{
            		HashMap<String,Object> blacklistData = bA.get(key);
            		String world = (String) blacklistData.get("world");

            		HashMap<String, Integer> min = (HashMap<String, Integer>) blacklistData.get("min");
            		HashMap<String, Integer> max = (HashMap<String, Integer>) blacklistData.get("max");
            			
            		BlacklistedAreas.add(new BlackArea(min.get("x"),min.get("y"),min.get("z"),max.get("x"),max.get("y"),max.get("z"),world));
            	}
            }
    	   
       }
	}
	public void clearInactivity()
	{
		Set<String> users2 = new HashSet<String>(Users.keySet());
		for(String PName : users2)
		{
			if(isPlayerInactive(PName)) //delete from team and users
			{
				if(Users.get(PName).hasTeam())
				{
					if(getTeam(PName).getTeamSize() <= 1)//if last one in team, disband team
					{
        				String TeamKey = Users.get(PName).getTeamKey();
        				disbandTeam(TeamKey);
					}
					else
					{
						String TeamKey = Users.get(PName).getTeamKey();
						if(getTeam(PName).isLeader(PName)) //promote others in place
							Teams.get(TeamKey).leaderInactivePromotionCascade();
						Teams.get(TeamKey).removeMember(PName);
					}
				}
				Users.remove(PName);
			}
		}
		savePlayers();
		saveTeams();
		saveAreas();
	}
	public boolean isPlayerInactive(String PlayerName)
	{
		//add check that they arent on a special list
		if(config.isPlayerExempt(PlayerName) || config.getCleanPlayerDays() == 0)
			return false;

		Calendar lastseen = Users.get(PlayerName).getLastSeen();
		
		//get current date
		Calendar cur = Calendar.getInstance();
		cur.set(cur.get(Calendar.YEAR), cur.get(Calendar.MONTH), cur.get(Calendar.DATE));
		
		
		Calendar date = (Calendar) lastseen.clone();
		long daysBetween = 0;
		while (date.before(cur)) {
			      date.add(Calendar.DAY_OF_MONTH, 1);
			      daysBetween++;
		}
		  if(daysBetween > config.getCleanPlayerDays())
			  return true;
		  else
			  return false;
	}
	private void saveAreas()
	{
		//Print Areas to File.
		  if(config.UseAreas())
	      {
			try{
				FileWriter fstream = new FileWriter(AreasFile, false);
				BufferedWriter out = new BufferedWriter(fstream);
				out.write("");
				for(String key : Areas.keySet())
				{
					out.write("\'"+ key + "\':\n");
					out.write(Areas.get(key).getSaveString());
				}
				out.close();
				fstream.close();
			}catch (Exception e){//Catch exception if any
				System.err.println("Error: " + e.getMessage());
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
				out.write("\'"+ key + "\':\n");
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
				out.write("\'"+key + "\': " + Users.get(key).getSaveString()+"\n");
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
	public TeamPlayer getTeamPlayer(String PlayerName)
	{
		return Users.get(PlayerName);
	}
	public Team getTeam(String PlayerName)
	{
		TeamPlayer tPlayer = Users.get(PlayerName);
		return Teams.get(tPlayer.getTeamKey());
	}
	private void teamAdd(String PlayerName){
		TeamPlayer tPlayer = Users.get(PlayerName);
		Users.get(PlayerName).setTeamKey(tPlayer.getInvite());
		Teams.get(tPlayer.getTeamKey()).addMember(PlayerName);
		Users.get(PlayerName).clearInvite();
		Teams.get(tPlayer.getTeamKey()).IncreaseOnlineCount();
	}
	private void teamRemove(String PlayerName){
		TeamPlayer tPlayer = Users.get(PlayerName);
		Teams.get(tPlayer.getTeamKey()).removeMember(PlayerName);
		if(Teams.get(tPlayer.getTeamKey()).getTeamSize() < config.getReqMemColor())
			Teams.get(tPlayer.getTeamKey()).setColor("GRAY");
		
		if(getServer().getPlayer(PlayerName).isOnline())
		{
			Teams.get(tPlayer.getTeamKey()).DecreaseOnlineCount();
		}
		
		Users.get(PlayerName).clearTeamKey();
	}
	public boolean hasUser(String PlayerName)
	{
		return Users.containsKey(PlayerName);
	}
	public void makeUser(String PlayerName)
	{
		Users.put(PlayerName, new TeamPlayer(config.TeamTKDefault()));
		savePlayers();
	}
	public void updateUserDate(String PlayerName)
	{
		Users.get(PlayerName).updateLastSeen();
		savePlayers();
	}
	public String doTeamsMOTD(String PlayerName) {
		String motd = "";
		if(Users.get(PlayerName).hasTeam())
		{
			String tk = Users.get(PlayerName).getTeamKey();
			if(getTeam(PlayerName).hasMOTD())
			{
				motd = "" + ChatColor.DARK_GREEN + "[Team MOTD] " + ChatColor.GREEN + getTeam(PlayerName).getMOTD();
				getServer().getPlayer(PlayerName).sendMessage(motd);
			}
			if(Areas.containsKey(tk))
			{
				if(!Areas.get(tk).getHolder().equalsIgnoreCase(tk))
					getServer().getPlayer(PlayerName).sendMessage(ChatColor.GOLD + "[Team MOTD] Your homeland has been taken by " + Areas.get(tk).getHolder() 
							+  " use /capture in your area to take it back!");
			}
		}
		return motd;
	}
	public String getCurrencyName()
	{
		String curr = "Currency";
		if(config.getCurrency() == 41)
			curr = "Gold Block(s)";
		return curr;
	}
	public ClansConfig getClansConfig()
	{
		return config;
	}
	private int getTime()
	{
		Calendar calendar = new GregorianCalendar();
		int time = calendar.get(Calendar.HOUR)*1000 + calendar.get(Calendar.MINUTE)*100 + calendar.get(Calendar.SECOND);
		return time;
	}
	private void messageTeam(String teamName, String msg)
	{
		Team team = Teams.get(teamName);
		Player[] onlineList = getServer().getOnlinePlayers();  				 
			 
		for (Player p : onlineList) {
			String userTeamKey = Users.get(p.getDisplayName()).getTeamKey();
			if(userTeamKey.equals(teamName))
				p.sendMessage(ChatColor.GREEN + "[TEAM] " + msg);
		}  
	}
	public void TriggerAlerter(String teamName) {
		TeamArea a = Areas.get(teamName);
		int t = getTime();
		if(t < a.getLastAlertTime() || a.getLastAlertTime()+config.getAlertThreshold() < t) {
			messageTeam(teamName, ""+ChatColor.RED+"Alert! Intruder has been spotted near "+ a.getAreaName() +".");
			Areas.get(teamName).updateAlertTime();
		}
	}
	public void TriggerDamager(Player player, String teamName) 
	{
		//If team is offline
		if(Teams.get(teamName).getOnlineCount() <= 0)
		{
			TeamArea a = Areas.get(teamName);
			int t = getTime();
			
			//Check if keys have expired
			if(a.hasDamagerKey(player.getDisplayName()) && (t < a.getLastOnlineTime() || a.getLastOnlineTime()+config.getDamagerKeyCooldown() < t)) {
				Areas.get(teamName).removeAllDamagerKeys();
			}
			//Damage Player if they dont have a key
			if(!a.hasDamagerKey(player.getDisplayName())) {
				player.damage(config.getOfflineDamageAmount());
				player.sendMessage(ChatColor.RED + "You are in a team's area and they are all offline, you have taken "+config.getOfflineDamageAmount() +" damage.");
			}
		}
	}
	public void TriggerCleanserPlace(Block block, String teamName) {
		//record block to cleanse hashmap
		Areas.get(teamName).addCleanseLocation(block.getLocation());
	}
	public void TriggerCleanserBreak(Block block, String teamName) {
		//check if block is in cleanse hashmap, if so remove it
		if(Areas.get(teamName).hasCleanseLocation(block.getLocation()))
		   Areas.get(teamName).removeCleanseLocation(block.getLocation());
	}
	private void cleanseArea(String teamName)
	{
		String world = Areas.get(teamName).getWorld();
		HashSet<Location> cleanser = Areas.get(teamName).getCleanseData();
		if(cleanser.size() > 0)
			messageTeam(teamName, ""+ChatColor.GREEN + "Cleanser has cleansed " + cleanser.size() +" blocks from your area.");
		for(Location loc : cleanser)
			getServer().getWorld("world").getBlockAt(loc).setTypeId(0);
		Areas.get(teamName).clearCleanseData();
	}
	public boolean isTeamOnline(String TeamName)
	{
		return (Teams.get(TeamName).getOnlineCount() > 0);
	}
	private void cleanseAllAreas()
	{
		for(String key : Areas.keySet()) {
			if(Areas.get(key).hasUpgradeCleanser()) 
				cleanseArea(key);
		}
	}
	public void TriggerResistanceDamage(Block block) {
		if(ResistBlocks.containsKey(block.getLocation())) {
			ResistBlocks.get(block.getLocation()).IncreaseExtTime();
			getServer().getScheduler().scheduleSyncDelayedTask(this, ResistBlocks.get(block.getLocation()), 5100L);
		}
		else //if (getServer().getWorld("world").getBlockAt(block.getLocation()).getTypeId() != config.getResistanceBlock()) //if already obsidian do nothing
		{
				ResistBlocks.put(block.getLocation(), new ResistantBlock(this,block.getState(),resistIDCount));
				resistIDCount++;
				getServer().getWorld("world").getBlockAt(block.getLocation()).setTypeId(49);
				getServer().getScheduler().scheduleSyncDelayedTask(this, ResistBlocks.get(block.getLocation()), 5100L);
		}
	}
	public void TriggerResistanceBreak(Block block) {
		if(ResistBlocks.containsKey(block.getLocation())) {
			//Cancels Block
			getServer().getWorld("world").getBlockAt(block.getLocation()).setType(ResistBlocks.get(block.getLocation()).getState().getType());
			getServer().getWorld("world").getBlockAt(block.getLocation()).setData(ResistBlocks.get(block.getLocation()).getState().getRawData());
			if(getServer().getWorld("world").getBlockAt(block.getLocation()).getTypeId() != 49) //should fix double obsidian problem
				getServer().getWorld("world").getBlockAt(block.getLocation()).breakNaturally();
			else {
				getServer().getWorld("world").getBlockAt(block.getLocation()).setTypeId(0);
				getServer().getWorld("world").dropItem(block.getLocation(), new ItemStack(49));
			}
			ResistBlocks.remove(block.getLocation());
		}
	}
	public void IncreaseTeamOnlineCount(String teamName)
	{
		Teams.get(teamName).IncreaseOnlineCount();
	}
	public void DecreaseTeamOnlineCount(String teamName)
	{
		Teams.get(teamName).DecreaseOnlineCount();
		if(Teams.get(teamName).getOnlineCount() <= 0) {
			if(Areas.containsKey(teamName)) {
				if(Areas.get(teamName).hasUpgradeDamager()) {
					Areas.get(teamName).setLastOnlineTime();
					Player[] onlineList = getServer().getOnlinePlayers(); 
					for (Player p : onlineList) {
						int x = p.getLocation().getBlockX();
						int z = p.getLocation().getBlockZ();
						String worldname = p.getWorld().getName();
						if(Areas.get(teamName).inArea(x, z, worldname)) {
							Areas.get(teamName).addDamagerKey(p.getDisplayName());
						}
					}  
				}
			}
		}
	}
	private void countOnlineTeamPlayers()
	{
		Player[] onlineList = getServer().getOnlinePlayers();  	
		 
		for (Player p : onlineList) {
			if(Users.get(p.getDisplayName()).hasTeam()) {
				Teams.get(Users.get(p.getDisplayName()).getTeamKey()).IncreaseOnlineCount();
			}
		}  
	}
	//Called when a team member resets the block
	public void ResetResistBlock(Location location) {
		if(ResistBlocks.containsKey(location)) {
			//set back to normal
				getServer().getWorld("world").getBlockAt(location).setType(ResistBlocks.get(location).getState().getType());
				getServer().getWorld("world").getBlockAt(location).setData(ResistBlocks.get(location).getState().getRawData());
				ResistBlocks.remove(location);
		}
	}
	public void ResetResistBlock(Location location, int id) {
		//need to give blocks IDs for start so if you get rid of a block, then place it, the thread from the old block doesn't reset it
		if(ResistBlocks.containsKey(location)) {
			if(ResistBlocks.get(location).getExtTime() == 0) {
				//set back to normal
				if(ResistBlocks.get(location).getID() == id) { //might fix id problem
					getServer().getWorld("world").getBlockAt(location).setType(ResistBlocks.get(location).getState().getType());
					getServer().getWorld("world").getBlockAt(location).setData(ResistBlocks.get(location).getState().getRawData());
					ResistBlocks.remove(location);
				}
			}
		}
	}
	public void ResetAllResistBlocks()
	{
		ArrayList<Location> locs = new ArrayList<Location>(ResistBlocks.keySet());
		for(Location location : locs)
		{
			if(ResistBlocks.containsKey(location)) {
				getServer().getWorld("world").getBlockAt(location).setType(ResistBlocks.get(location).getState().getType());
				getServer().getWorld("world").getBlockAt(location).setData(ResistBlocks.get(location).getState().getRawData());
				ResistBlocks.remove(location);
			}
		}
	}
	public boolean isResistBlock(Location location) {
		return ResistBlocks.containsKey(location);
	}
	public void addCapes()
	{
		for(Player player : getServer().getOnlinePlayers())
		{
			if(Users.get(player.getDisplayName()).hasTeam())
			{
				if(!getTeam(player.getDisplayName()).getTeamCapeUrl().equalsIgnoreCase(""))
				{
					try {
						  URL url = new URL(getTeam(player.getDisplayName()).getTeamCapeUrl());
						  if(url.toString().endsWith(".png")) {
					        	SpoutPlayer sp = (SpoutPlayer)player;
					        	((SpoutPlayer)sp).setCape(getTeam(player.getDisplayName()).getTeamCapeUrl());
				        	//SpoutPlayer sp = (SpoutPlayer)p;
				        	//((SpoutPlayer)sp).set(getTeam(PlayerName).getTeamCapeUrl());
						  }
					}
					catch (MalformedURLException e) {
						Teams.get(Users.get(player.getDisplayName()).getTeamKey()).setTeamCapeUrl("");
					}
				}
			}

        	/*
			for(Player player2 : getServer().getOnlinePlayers())
			{
				if(Users.get(player2.getDisplayName()).hasTeam())
				{
					if(!getTeam(player2.getDisplayName()).getTeamCapeUrl().equalsIgnoreCase(""))
					{
						try {
							  URL url = new URL(getTeam(player2.getDisplayName()).getTeamCapeUrl());
							  if(url.toString().endsWith(".png")) {
								SpoutPlayer splayer = SpoutManager.getPlayer(player);
								SpoutPlayer splayerTo = SpoutManager.getPlayer(player2);
								splayer.setCapeFor(splayerTo, getTeam(player2.getDisplayName()).getTeamCapeUrl());
								System.out.println("Set player " + splayerTo.getDisplayName() + "");
								
					        	//SpoutPlayer sp = (SpoutPlayer)p;
					        	//((SpoutPlayer)sp).set(getTeam(PlayerName).getTeamCapeUrl());
							  }
						}
						catch (MalformedURLException e) {
							Teams.get(Users.get(player2.getDisplayName()).getTeamKey()).setTeamCapeUrl("");
						}
					}
				}
			}*/
		}
	}
	class TeamScoreNode {
		String TeamName;
		int TeamScore;
		public TeamScoreNode (String tn, int ts) {
			TeamName = tn;
			TeamScore = ts;
		}
	}
}
