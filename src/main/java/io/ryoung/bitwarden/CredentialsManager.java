package io.ryoung.bitwarden;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.SwingUtilities;
import net.runelite.api.Client;

final class CredentialsManager
{
	private static final Gson gson = new GsonBuilder().registerTypeAdapter(Credential.class, new Credential.Deserializer()).create();

	private final Client client;
	private CommandRunner commandRunner = null;

	private SecureString sessionKey = new SecureString("");
	private List<Credential> entries = new ArrayList<>();

	private boolean keepTrying = true;

	@Inject
	CredentialsManager(Client client)
	{
		this.client = client;
	}

	private void parseIssue(String result)
	{
		commandRunner = null;
		entries.clear();

		if (result.contains("Session key is invalid"))
		{
			askForKey();
			return;
		}

		if (result.startsWith("? Master password:"))
		{
			SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null, "Your vault might be locked.",
				"", JOptionPane.INFORMATION_MESSAGE));
			return;
		}

		setSessionKey(new SecureString(""));

		if (result.contains("You are not logged in"))
		{
			SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null, "You are not logged into Bitwarden CLI.",
				"", JOptionPane.INFORMATION_MESSAGE));
		}
		else if (result.contains("mac failed"))
		{
			SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null, "Error loading vault. Your session key might be wrong." +
					"\nYou can try logging out of Bitwarden CLI and logging back in.",
				"", JOptionPane.ERROR_MESSAGE));
		}
		else
		{
			SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null, "Error loading vault.\nTry logging out of Bitwarden CLI and logging back in.",
				"", JOptionPane.ERROR_MESSAGE));
		}

		askForKey();
	}

	private void consumeResult(String result)
	{
		System.out.println(result);
		if (!result.startsWith("["))
		{
			parseIssue(result);
			return;
		}

		try
		{
			entries = gson.fromJson(result, Credential.TYPE);

			setPassword();
		}
		catch (JsonParseException | NullPointerException e)
		{
			entries.clear();
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
				client.setPassword(credential.getPassword().asString());
				return;
			}
		}

		client.setPassword("");
	}

	void setSessionKey(SecureString key)
	{
		this.sessionKey.clear();
		this.sessionKey = key;
	}

	void clearEntries()
	{
		entries.clear();
	}

	void reset()
	{
		sessionKey.clear();
		entries.clear();
		commandRunner = null;
		keepTrying = true;
	}

	void injectPassword()
	{
		if (sessionKey.length() == 0)
		{
			askForKey();
		}
		else if (commandRunner == null && entries.isEmpty())
		{
			commandRunner = new CommandRunner(sessionKey, this::consumeResult);
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
			int option = JOptionPane.showOptionDialog(null, panel, "",
				JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE,
				null, null, "");
			if (option == 0) // pressing OK button
			{
				setSessionKey(new SecureString(pass.getPassword()));
				injectPassword();
			}
			else
			{
				keepTrying = false;
				setSessionKey(new SecureString(""));
			}
		});
	}
}
