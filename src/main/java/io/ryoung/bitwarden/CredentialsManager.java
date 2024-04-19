package io.ryoung.bitwarden;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;

@Slf4j
final class CredentialsManager
{
	private final Gson GSON;

	private final Client client;
	private CommandRunner commandRunner = null;

	private char[] sessionKey = new char[0];
	private List<Credential> entries = new ArrayList<>();

	private boolean keepTrying = true;
	private BitwardenConfig config;

	private boolean developerMode;

	@Inject
	CredentialsManager(Client client, BitwardenConfig config, @Named("developerMode") boolean developerMode, Gson gson)
	{
		this.client = client;
		this.config = config;
		this.developerMode = developerMode;
		this.GSON = gson.newBuilder().registerTypeAdapter(Credential.class, new Credential.Deserializer()).create();
	}

	private void parseIssue(String result)
	{
		commandRunner = null;
		entries.clear();

		if (developerMode)
		{
			log.error(result);
		}

		if (result.contains("Session key is invalid"))
		{
			SwingUtilities.invokeLater(() -> {
				JOptionPane.showMessageDialog(null, "Your session key is invalid.",
					"", JOptionPane.INFORMATION_MESSAGE);
				askForKey();
			});
			return;
		}

		if (result.startsWith("? Master password:"))
		{
			SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null, "Your vault is locked.",
				"", JOptionPane.INFORMATION_MESSAGE));
			return;
		}

		clearSessionKey();

		if (result.contains("You are not logged in"))
		{
			SwingUtilities.invokeLater(() -> {
				JOptionPane.showMessageDialog(null, "You are not logged into Bitwarden CLI.",
					"", JOptionPane.INFORMATION_MESSAGE);
				askForKey();
			});
		}
		else if (result.contains("mac failed"))
		{
			SwingUtilities.invokeLater(() -> {
				JOptionPane.showMessageDialog(null, "Error loading vault. Your session key might be wrong." +
						"\nTry: " +
						"\n1. Closing RuneLite" +
						"\n2. Locking your vault" +
						"\n3. Deleting your session key" +
						"\n4. Closing your terminal" +
						"\n5. Then unlock your vault and save your session key",
					"", JOptionPane.ERROR_MESSAGE);
				askForKey();
			});
		}
		else
		{
			SwingUtilities.invokeLater(() -> {
				JOptionPane.showMessageDialog(null, "Unknown error: " + result,
					"", JOptionPane.ERROR_MESSAGE);
				askForKey();
			});
		}
	}

	private void consumeResult(String result)
	{
		if (!result.startsWith("["))
		{
			parseIssue(result);
			return;
		}

		try
		{
			entries = GSON.fromJson(result, Credential.TYPE);

			setPassword();
		}
		catch (JsonParseException | NullPointerException e)
		{
			entries.clear();

			SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null, "Error parsing credential results.",
				"", JOptionPane.ERROR_MESSAGE));
		}
		finally
		{
			commandRunner = null;
		}
	}

	private void setPassword()
	{
		String username = client.getUsername();
		for (Credential credential : entries)
		{
			if (credential != null && username.equalsIgnoreCase(credential.getUsername()))
			{
				client.setPassword(new String(credential.getPassword()));
				return;
			}
		}
	}

	void clearSessionKey()
	{
		this.sessionKey = new char[0];
	}

	void setSessionKey(char[] key)
	{
		this.sessionKey = Arrays.copyOf(key, key.length);
	}

	void clearEntries()
	{
		entries.clear();
	}

	void reset()
	{
		clearSessionKey();
		entries.clear();
		commandRunner = null;
		keepTrying = true;
	}

	void injectPassword()
	{
		if (sessionKey.length == 0)
		{
			askForKey();
		}
		else if (commandRunner == null && entries.isEmpty())
		{
			commandRunner = new CommandRunner(config.bwLocation(), sessionKey, this::consumeResult);
			commandRunner.start();
		}
		else
		{
			setPassword();
		}
	}

	private void askForKey()
	{
		if (!keepTrying)
		{
			return;
		}

		SwingUtilities.invokeLater(() -> {
			JPanel panel = new JPanel();
			JLabel label = new JLabel("Session Key:");
			JPasswordField pass = new JPasswordField(128);
			panel.add(label);
			panel.add(pass);
			int option = JOptionPane.showOptionDialog(null, panel, "Bitwarden",
				JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE,
				null, null, "");
			if (option == 0) // pressing OK button
			{
				setSessionKey(pass.getPassword());
				injectPassword();
			}
			else
			{
				keepTrying = false;
				clearSessionKey();
			}
		});
	}
}
