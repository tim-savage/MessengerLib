package com.winterhaven_mc.util;

import com.onarandombox.MultiverseCore.MultiverseCore;
import com.onarandombox.MultiverseCore.api.MultiverseWorld;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;


@SuppressWarnings("unused")
public abstract class AbstractMessage<MessageId extends Enum<MessageId>, Macro extends Enum<Macro>> {

	// reference to plugin main class
	private final JavaPlugin plugin = JavaPlugin.getProvidingPlugin(this.getClass());

	// required parameters
	private final CommandSender recipient;
	private final MessageId messageId;

	// optional parameters
	private final Map<Macro, Object> macroObjectMap = new HashMap<>();
	private int quantity = 1;


	/**
	 * Class constructor
	 * @param recipient message recipient
	 * @param messageId message identifier
	 */
	public AbstractMessage(CommandSender recipient, MessageId messageId) {
		this.recipient = recipient;
		this.messageId = messageId;
	}


	/**
	 * set macro for message replacements
	 * @param macro token for placeholder
	 * @param value object that contains value that will be substituted in message
	 * @return AbstractMessage to be used further in build process
	 */
	public final AbstractMessage<MessageId, Macro> setMacro(Macro macro, Object value) {
		macroObjectMap.put(macro, value);
		return this;
	}


	/**
	 * Final step of message builder, performs replacements and sends message to recipient
	 */
	public final void send() {

		// get language manager instance
		LanguageManager languageManager = LanguageManager.getInstance();

		// if message is not enabled in messages file, do nothing and return
		if (!languageManager.isEnabled(messageId)) {
			return;
		}

		// get cooldown instance
		MessageCooldown<MessageId> messageCooldown = MessageCooldown.getInstance(plugin);

		// if message is not cooled, do nothing and return
		if (messageCooldown.isCooling(recipient, messageId, languageManager.getRepeatDelay(messageId))) {
			return;
		}

		// get message string from file
		String messageString = languageManager.getMessage(messageId);

		// only process macro tokens if message string contains a token marker character
		if (messageString.contains("%")) {

			// iterate over macro map, giving special treatment to certain entries
			for (Map.Entry<Macro, Object> entry : macroObjectMap.entrySet()) {

				switch (entry.getKey().toString()) {
					case "ITEM_QUANTITY":
						// if quantity is an integer, copy value to class field
						if (entry.getValue() instanceof Integer) {
							quantity = (Integer) entry.getValue();
						}
						break;
					case "WORLD":
					case "WORLD_NAME":
						// if object is a world, attempt to replace with Multiverse alias as string
						if (entry.getValue() instanceof World) {
							entry.setValue(getWorldName((World) entry.getValue()));
						}
						// if object is an entity, attempt to replace with Multiverse alias for entity world name as string
						else if (entry.getValue() instanceof Entity) {
							entry.setValue(getWorldName((Entity) entry.getValue()));
						}
						// if object is a location, attempt to replace with Multiverse alias for location world as string
						else if (entry.getValue() instanceof Location) {
							entry.setValue(getWorldName((Location) entry.getValue()));
						}
						break;
					case "LOCATION":
						// if entry type is Entity, set value to entity's location
						if (entry.getValue() instanceof Entity) {
							entry.setValue(((Entity) entry.getValue()).getLocation());
						}
						// if entry type is location, set value to formatted location string and do message replacements
						if (entry.getValue() instanceof Location) {
							Location location = (Location) entry.getValue();
							String locWorld = getWorldName(location);
							String locX = String.valueOf(location.getBlockX());
							String locY = String.valueOf(location.getBlockY());
							String locZ = String.valueOf(location.getBlockZ());
							String locString = locWorld + " [" + locX + ", " + locY + ", " + locZ + "]";
							entry.setValue(locString);
							messageString = messageString.replace("%LOC_WORLD%", locWorld);
							messageString = messageString.replace("%LOC_X%", locX);
							messageString = messageString.replace("%LOC_Y%", locY);
							messageString = messageString.replace("%LOC_Z%", locZ);
						}
						break;
					case "PLAYER_LOCATION":
						// if entry type is Entity, set value to entity's location
						if (entry.getValue() instanceof Entity) {
							entry.setValue(((Entity) entry.getValue()).getLocation());
						}
						// if entry type is location, set value to formatted location string and do message replacements
						if (entry.getValue() instanceof Location) {
							Location location = (Location) entry.getValue();
							String locWorld = getWorldName(location);
							String locX = String.valueOf(location.getBlockX());
							String locY = String.valueOf(location.getBlockY());
							String locZ = String.valueOf(location.getBlockZ());
							String locString = locWorld + " [" + locX + ", " + locY + ", " + locZ + "]";
							entry.setValue(locString);
							messageString = messageString.replace("%PLAYER_LOC_WORLD%", locWorld);
							messageString = messageString.replace("%PLAYER_LOC_X%", locX);
							messageString = messageString.replace("%PLAYER_LOC_Y%", locY);
							messageString = messageString.replace("%PLAYER_LOC_Z%", locZ);
						}
						break;
					case "DURATION":
						// if entry type is Number, set value to time string
						if (entry.getValue() instanceof Number) {
							entry.setValue(LanguageManager.getInstance().getTimeString((Long) entry.getValue()));
						}
						break;
					default:
						// if entry is CommandSender, set value to name
						if (entry.getValue() instanceof CommandSender) {
							entry.setValue(((CommandSender) entry.getValue()).getName());
						}
						// if entry is OfflinePlayer, set value to name
						if (entry.getValue() instanceof OfflinePlayer) {
							entry.setValue(((OfflinePlayer) entry.getValue()).getName());
						}
						break;
				}

				// replace macro tokens in message string with values as string
				String macroToken = "%" + entry.getKey().toString() + "%";
				messageString = messageString.replace(macroToken, entry.getValue().toString());
			}

			// replace %ITEM_NAME% with value declared in language file
			String itemName = languageManager.getItemName();
			if (quantity != 1) {
				itemName = languageManager.getItemNamePlural();
			}
			messageString = messageString.replace("%ITEM%", itemName);
			messageString = messageString.replace("%ITEM_NAME%", itemName);

			// replace %WORLD_NAME% with recipient world name
			messageString = messageString.replace("%WORLD%", getWorldName(recipient));
			messageString = messageString.replace("%WORLD_NAME%", getWorldName(recipient));

			// replace %PLAYER_NAME% with recipient name
			messageString = messageString.replace("%PLAYER%", recipient.getName());
			messageString = messageString.replace("%PLAYER_NAME%", recipient.getName());
		}

		// send message to player
		recipient.sendMessage(ChatColor.translateAlternateColorCodes('&', messageString));

		// if message repeat delay value is greater than zero, add entry to messageCooldownMap
		if (languageManager.getRepeatDelay(messageId) > 0) {
			if (recipient instanceof Entity) {
				messageCooldown.put(messageId, (Entity) recipient);
			}
		}
	}


	/**
	 * get current world name of message recipient, using Multiverse alias if available
	 *
	 * @param recipient player to fetch world name
	 * @return String containing recipient world name
	 * @throws NullPointerException if parameter is null
	 */
	private static String getWorldName(final CommandSender recipient) {

		// check for null parameter
		Objects.requireNonNull(recipient);

		// get reference to plugin main class
		JavaPlugin plugin = JavaPlugin.getProvidingPlugin(AbstractMessage.class);

		// get reference to Multiverse-Core if installed
		MultiverseCore mvCore = (MultiverseCore) plugin.getServer().getPluginManager().getPlugin("Multiverse-Core");

		// declare recipient world
		World world;

		// if sender is entity, set worldName to entity world name
		if (recipient instanceof Entity) {
			world = ((Entity) recipient).getWorld();
		}
		else {
			// otherwise, use server first world
			world = recipient.getServer().getWorlds().get(0);
		}

		// set result string to world name
		String resultString = world.getName();

		// if Multiverse is enabled, use Mulitverse world alias if available
		if (mvCore != null && mvCore.isEnabled()) {

			// get Multiverse world object
			MultiverseWorld mvWorld = mvCore.getMVWorldManager().getMVWorld(world);

			// if Multiverse alias is not null or empty, set world name to alias if set
			if (mvWorld != null && mvWorld.getAlias() != null && !mvWorld.getAlias().isEmpty()) {
				resultString = mvWorld.getAlias();
			}
		}

		// return resultString
		return resultString;
	}


	/**
	 * Get world name from world object, using Multiverse alias if available
	 *
	 * @param world the world object to retrieve name
	 * @return bukkit world name or multiverse alias as String
	 * @throws NullPointerException if passed world is null
	 */
	private static String getWorldName(final World world) {

		// world must be non-null
		Objects.requireNonNull(world);

		// get reference to plugin main class
		JavaPlugin plugin = JavaPlugin.getProvidingPlugin(AbstractMessage.class);

		// get reference to Multiverse-Core if installed
		MultiverseCore mvCore = (MultiverseCore) plugin.getServer().getPluginManager().getPlugin("Multiverse-Core");

		// get bukkit world name
		String worldName = world.getName();

		// if Multiverse is enabled, get MultiverseWorld object
		if (mvCore != null && mvCore.isEnabled()) {

			MultiverseWorld mvWorld = mvCore.getMVWorldManager().getMVWorld(world);

			// if Multiverse alias is not null or empty, set worldName to alias
			if (mvWorld != null && mvWorld.getAlias() != null && !mvWorld.getAlias().isEmpty()) {
				worldName = mvWorld.getAlias();
			}
		}

		// return the bukkit world name or Multiverse world alias
		return worldName;
	}


	/**
	 * Get world name from world name string, using Multiverse alias if available
	 *
	 * @param passedName the bukkit world name as string
	 * @return bukkit world name or multiverse alias as String
	 */
	private static String getWorldName(final String passedName) {

		// if passedName is null or empty, return empty string
		if (passedName == null || passedName.isEmpty()) {
			return "";
		}

		// get reference to plugin main class
		JavaPlugin plugin = JavaPlugin.getProvidingPlugin(AbstractMessage.class);

		// get reference to Multiverse-Core if installed
		MultiverseCore mvCore = (MultiverseCore) plugin.getServer().getPluginManager().getPlugin("Multiverse-Core");

		// get world
		World world = plugin.getServer().getWorld(passedName);

		// if world is null, return empty string
		if (world == null) {
			return "";
		}

		// get bukkit world name
		String worldName = world.getName();

		// if Multiverse is enabled, get MultiverseWorld object
		if (mvCore != null && mvCore.isEnabled()) {

			// get MultiverseWorld object
			MultiverseWorld mvWorld = mvCore.getMVWorldManager().getMVWorld(world);

			// if Multiverse alias is not null or empty, set worldName to alias
			if (mvWorld != null && mvWorld.getAlias() != null && !mvWorld.getAlias().isEmpty()) {
				worldName = mvWorld.getAlias();
			}
		}

		// return the bukkit world name or Multiverse world alias
		return worldName;
	}


	/**
	 * Get world name for location, using Multiverse alias if available
	 *
	 * @param location the location used to retrieve world name
	 * @return bukkit world name or multiverse alias as String
	 * @throws NullPointerException if passed location is null
	 */
	private static String getWorldName(final Location location) {

		// check for null parameter
		Objects.requireNonNull(location);

		// get reference to plugin main class
		JavaPlugin plugin = JavaPlugin.getProvidingPlugin(AbstractMessage.class);

		// get reference to Multiverse-Core if installed
		MultiverseCore mvCore = (MultiverseCore) plugin.getServer().getPluginManager().getPlugin("Multiverse-Core");

		// declare resultString with world name for location
		String resultString;
		if (location.getWorld() != null) {
			resultString = location.getWorld().getName();
		}
		else {
			// get name of first world
			resultString = plugin.getServer().getWorlds().get(0).getName();
		}

		// if Multiverse is enabled, use Mulitverse world alias if available
		if (mvCore != null && mvCore.isEnabled()) {

			// get Multiverse world object
			MultiverseWorld mvWorld = mvCore.getMVWorldManager().getMVWorld(location.getWorld());

			// if Multiverse alias is not null or empty, set world name to alias if set
			if (mvWorld != null && mvWorld.getAlias() != null && !mvWorld.getAlias().isEmpty()) {
				resultString = mvWorld.getAlias();
			}
		}

		// return resultString
		return resultString;
	}

}
