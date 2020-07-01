package com.winterhaven_mc.util;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


/**
 * Install localization files from language directory in jar archive to language subdirectory
 * of the plugin data directory. Any files with a .yml suffix that are stored as a resource
 * within a /language subdirectory in the plugin jar archive will be copied to the /lanuguage
 * subdirectory of the plugin data directory.
 */
public class FileInstaller {

	JavaPlugin plugin;

	FileInstaller(JavaPlugin plugin) {
		this.plugin = plugin;
	}


	/**
	 * Get list of resource filenames from language directory of plugin jar
	 *
	 * @return List of filename strings
	 */
	List<String> getFilenames() {

		// get the absolute path to this plugin as URL
		URL pluginURL = plugin.getClass().getProtectionDomain().getCodeSource().getLocation();

		List<String> returnList = new ArrayList<>();

		// read files contained in jar, adding language/*.yml files to fileList
		ZipInputStream zip;
		try {
			zip = new ZipInputStream(pluginURL.openStream());
			while (true) {
				ZipEntry e = zip.getNextEntry();
				if (e == null) {
					break;
				}
				String name = e.getName();
				if (name.startsWith("language" + '/') && name.endsWith(".yml")) {
					returnList.add(name);
				}
			}
		}
		catch (IOException e1) {
			plugin.getLogger().warning("Could not read language files from jar.");
		}

		return returnList;
	}


	/**
	 * Install files from plugin jar to plugin data directory
	 *
	 * @param fileList list of filenames to be installed
	 */
	void installFiles(List<String> fileList) {

		// iterate over list of language files and install from jar if not already present
		for (String filename : fileList) {
			// this check prevents a warning message when files are already installed
			if (new File(plugin.getDataFolder() + File.separator + filename).exists()) {
				continue;
			}
			plugin.saveResource(filename, false);
			plugin.getLogger().info("Installed localization file: " + filename);
		}
	}

}
