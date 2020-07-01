package com.winterhaven_mc.util;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;


public class MessageCooldown<MessageId extends Enum<MessageId>> implements Listener {

	private static MessageCooldown<? extends Enum<?>> INSTANCE;

	// cooldown enum map
	private final Map<MessageId, Map<UUID, Long>> messageCooldownMap = new ConcurrentHashMap<>();


	/**
	 * private class constructor, registers class as bukkit event listener
	 */
	private MessageCooldown(JavaPlugin plugin) {
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}


	/**
	 * Static factory method
	 *
	 * @return instance of this class
	 */
	@SuppressWarnings("unchecked")
	static synchronized <MessageId extends Enum<MessageId>> MessageCooldown<MessageId> getInstance(JavaPlugin plugin) {
		if (INSTANCE == null) {
			INSTANCE = new MessageCooldown<>(plugin);
		}
		return (MessageCooldown<MessageId>) INSTANCE;
	}


	/**
	 * Add entry to message cooldown map
	 *
	 * @param messageId the message id to use as a key in the cooldown map
	 * @param entity    the entity whose uuid will be added as a key to the cooldown map
	 * @throws NullPointerException if parameter is null
	 */
	void put(final MessageId messageId, final Entity entity) {

		// check for null parameters
		Objects.requireNonNull(messageId);
		Objects.requireNonNull(entity);

		// create new HashMap with entity UUID as key
		Map<UUID, Long> tempMap = new ConcurrentHashMap<>();

		// put current time in HashMap with entity UUID as key
		tempMap.put(entity.getUniqueId(), System.currentTimeMillis());

		// put HashMap in cooldown map with messageId as key
		messageCooldownMap.put(messageId, tempMap);
	}


	/**
	 * get entry from message cooldown map
	 *
	 * @param messageId the message identifier for which retrieve cooldown time
	 * @param entity    the entity for whom to retrieve cooldown time
	 * @return cooldown expire time
	 * @throws NullPointerException if parameter is null
	 */
	long get(final MessageId messageId, final Entity entity) {

		// check for null parameters
		Objects.requireNonNull(messageId);
		Objects.requireNonNull(entity);

		// check if messageId is in message cooldown hashmap
		if (messageCooldownMap.containsKey(messageId)) {

			// check if messageID is in entity's cooldown hashmap
			if (messageCooldownMap.get(messageId).containsKey(entity.getUniqueId())) {

				// return cooldown time
				return messageCooldownMap.get(messageId).get(entity.getUniqueId());
			}
		}
		return 0L;
	}


	/**
	 * Remove player from message cooldown map
	 *
	 * @param entity the entity (player) to be removed from message cooldown map
	 */
	void remove(final Entity entity) {

		// if entity is null, do nothing and return
		if (entity == null) {
			return;
		}

		// iterate through all cooldown map keys
		for (MessageId messageId : messageCooldownMap.keySet()) {

			// remove entity UUID from cooldown map
			messageCooldownMap.get(messageId).remove(entity.getUniqueId());
		}
	}


	/**
	 * check if player message is in cooldown map
	 *
	 * @param recipient   player being sent message
	 * @param messageId   message id of message being sent
	 * @param repeatDelay time (in seconds) before message should be redisplayed
	 * @return true if player message is in cooldown map, false if it is not
	 * @throws NullPointerException if parameter is null
	 */
	boolean isCooling(final CommandSender recipient, final MessageId messageId, long repeatDelay) {

		// check for null parameters
		Objects.requireNonNull(recipient);
		Objects.requireNonNull(messageId);

		// if recipient is entity...
		if (recipient instanceof Entity) {

			// cast recipient to Entity
			Entity entity = (Entity) recipient;

			// get message cooldown time remaining
			long lastDisplayed = get(messageId, entity);

			// if message has repeat delay value and was displayed to player more recently, do nothing and return
			//noinspection RedundantIfStatement
			if (lastDisplayed > System.currentTimeMillis() - TimeUnit.SECONDS.toMillis(repeatDelay)) {
				return true;
			}
		}
		return false;
	}


	/**
	 * Event handler for PlayerQuitEvent;
	 * removes player from message cooldown map on logout
	 *
	 * @param event the event handled by this method
	 */
	@EventHandler
	public void onPlayerQuit(final PlayerQuitEvent event) {

		// remove player from message cooldown map
		remove(event.getPlayer());
	}

}
