package com.winterhaven_mc.util;

import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.nio.charset.StandardCharsets;


public class FileLoader {

	JavaPlugin plugin;

	private final String directoryName = "language";


	/**
	 * Class constructor
	 * @param plugin reference to plugin main class
	 */
	FileLoader(JavaPlugin plugin) {
		this.plugin = plugin;
	}


	/**
	 * Get configuration object containing message settings and strings
	 * @return Configuration - message configuration object
	 */
	Configuration getMessages() {

		// check that file exists for language
		String confirmedLanguage = languageFileExists(plugin, getConfiguredLanguage(plugin));

		// get file object for configured language file
		File languageFile = new File(getCurrentFilename(plugin, confirmedLanguage));

		// create new YamlConfiguration object
		YamlConfiguration newMessagesConfig = new YamlConfiguration();

		// try to load specified language file into new YamlConfiguration object
		try {
			newMessagesConfig.load(languageFile);
			plugin.getLogger().info("Language file " + confirmedLanguage + ".yml successfully loaded.");
		}
		catch (FileNotFoundException e) {
			plugin.getLogger().severe("Language file " + confirmedLanguage + ".yml does not exist.");
		}
		catch (IOException e) {
			plugin.getLogger().severe("Language file " + confirmedLanguage + ".yml could not be read.");
		}
		catch (InvalidConfigurationException e) {
			plugin.getLogger().severe("Language file " + confirmedLanguage + ".yml is not valid yaml.");
		}

		// Set defaults to embedded resource file

		// get embedded resource file name; note that forward slash (/) is always used, regardless of platform
		String resourceName = directoryName + "/" + confirmedLanguage + ".yml";

		// check if specified language resource exists, otherwise use en-US
		if (plugin.getResource(resourceName) == null) {
			resourceName = directoryName + "/en-US.yml";
		}

		// get input stream reader for embedded resource file
		//noinspection ConstantConditions
		Reader defaultConfigStream = new InputStreamReader(plugin.getResource(resourceName), StandardCharsets.UTF_8);

		// load embedded resource stream into Configuration object
		Configuration defaultConfig = YamlConfiguration.loadConfiguration(defaultConfigStream);

		// set Configuration object as defaults for messages configuration
		newMessagesConfig.setDefaults(defaultConfig);

		return newMessagesConfig;
	}


	/**
	 * get language specified in config.yml
	 *
	 * @param plugin reference to plugin main class
	 * @return IETF language string from config.yml
	 */
	private String getConfiguredLanguage(JavaPlugin plugin) {
		return plugin.getConfig().getString("language");
	}


	/**
	 * Check if a file exists for the provided IETF language tag (ex: en-US)
	 *
	 * @param language the IETF language tag
	 * @return if file exists for language tag, return the language tag; else return the default tag (en-US)
	 */
	private String languageFileExists(JavaPlugin plugin, final String language) {

		// get file object for passed language tag by adding prefixing for directory name and .yml suffix
		File languageFile = new File(getCurrentFilename(plugin, language));

		// if a language file exists for the language tag, return the language tag
		if (languageFile.exists()) {
			return language;
		}

		// output language file not found message to log
		plugin.getLogger().info("Language file " + language + ".yml does not exist. Defaulting to en-US.");

		// return default language tag (en-US)
		return "en-US";
	}


	/**
	 * Get the file name for the currently selected language
	 *
	 * @param plugin reference to plugin main class
	 * @param language IETF language tag
	 * @return current language file name as String
	 */
	private String getCurrentFilename(JavaPlugin plugin, String language) {
		return plugin.getDataFolder() + File.separator + directoryName + File.separator + language + ".yml";
	}

}
