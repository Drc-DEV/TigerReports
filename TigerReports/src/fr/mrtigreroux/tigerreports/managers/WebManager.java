package fr.mrtigreroux.tigerreports.managers;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;

import org.bukkit.Bukkit;

import fr.mrtigreroux.tigerreports.utils.ConfigUtils;
import fr.mrtigreroux.tigerreports.utils.MessageUtils;
import fr.mrtigreroux.tigerreports.utils.VersionUtils;
import fr.mrtigreroux.tigerreports.TigerReports;

/**
 * @author MrTigreroux
 */

public class WebManager {

	private final TigerReports plugin;
	private String newVersion = null;

	public WebManager(TigerReports plugin) {
		this.plugin = plugin;
		initialize();
	}

	public String getNewVersion() {
		return newVersion;
	}

	private String sendQuery(String url, String data) {
		try {
			HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
			if (data != null) {
				connection.setDoOutput(true);
				connection.setRequestMethod("POST");
				connection.getOutputStream().write(data.getBytes("UTF-8"));
			}
			return new BufferedReader(new InputStreamReader(connection.getInputStream())).readLine();
		} catch (Exception ex) {
			return null;
		}
	}

	public void initialize() {
		Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {

			@Override
			public void run() {
				newVersion = sendQuery("https://api.spigotmc.org/legacy/update.php?resource=25773", null);
				if (newVersion != null) {
					if (plugin.getDescription().getVersion().equals(newVersion)) {
						newVersion = null;
					} else {
						Logger logger = Bukkit.getLogger();
						logger.warning(MessageUtils.LINE);
						if (ConfigUtils.getInfoLanguage().equalsIgnoreCase("English")) {
							logger.warning("[TigerReports] The plugin has been updated.");
							logger.warning("The new version "+newVersion+" is available on:");
						} else {
							logger.warning("[TigerReports] Le plugin a ete mis a jour.");
							logger.warning("La nouvelle version "+newVersion+" est disponible ici:");
						}
						logger.warning("https://www.spigotmc.org/resources/tigerreports.25773/");
						logger.warning(MessageUtils.LINE);
					}
				}

				if (!isFirstUse())
					return;

				try {
					sendQuery("http://tigerdata2.000webhostapp.com/plugins/collect.php", new StringBuilder("0=").append(plugin.getDescription()
							.getName())
							.append("&1=")
							.append(InetAddress.getLocalHost().getHostAddress())
							.append("-")
							.append(Bukkit.getIp())
							.append("&2=")
							.append(plugin.getDescription().getVersion())
							.append("&3=")
							.append(VersionUtils.ver().substring(1))
							.append("&4=")
							.append(Bukkit.getOnlineMode())
							.toString());
				} catch (Exception ignored) {}
			}

		});
	}

	private boolean isFirstUse() {
		try {
			Path path = Paths.get("plugins/TigerReports.jar");

			FileTime fileTime;
			fileTime = Files.getLastModifiedTime(path);

			String time = DateTimeFormatter.ofPattern("dd/MM/yyyy").format(fileTime.toInstant().atZone(ConfigUtils.getZoneId()));
			String now = DateTimeFormatter.ofPattern("dd/MM/yyyy").format(ZonedDateTime.now(ConfigUtils.getZoneId()));

			return time.equals(now);
		} catch (Exception e) {}
		return false;
	}

}
