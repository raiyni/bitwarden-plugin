package io.ryoung.bitwarden;

import com.google.common.io.ByteStreams;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.runelite.client.util.OSType;

class CommandRunner extends Thread
{
	CommandRunner(SecureString sessionKey, Consumer<String> consumer)
	{
		super(() -> {
			try
			{
				ProcessBuilder pb = buildCommand(sessionKey);
				pb.redirectErrorStream(true);
				Process p = pb.start();

				byte[] bytes = ByteStreams.toByteArray(p.getInputStream());
				p.waitFor();

				consumer.accept(new String(bytes));
			}
			catch (IOException | InterruptedException e)
			{
				// do nothing
			}
		});
	}

	private static ProcessBuilder buildCommand(SecureString sessionKey)
	{
		List<String> params = new ArrayList<>();
		if (OSType.getOSType() == OSType.Windows)
		{
			params.add("cmd");
			params.add("/c");
		}
		else
		{
			params.add("bash");
			params.add("-c");
		}

		String redirect = OSType.getOSType() == OSType.Windows ? " < NUL" : " < /dev/null";
		params.add("bw list items --search runescape.com --session \"" + sessionKey.asString() + "\"" + redirect);

		return new ProcessBuilder(params);
	}
}