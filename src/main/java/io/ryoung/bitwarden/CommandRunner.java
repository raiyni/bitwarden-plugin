package io.ryoung.bitwarden;

import com.google.common.base.CharMatcher;
import com.google.common.io.ByteStreams;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.runelite.client.util.OSType;

class CommandRunner extends Thread
{

	private static final CharMatcher CHAR_MATCHER = CharMatcher.inRange('0', '9')
		.or(CharMatcher.inRange('a', 'z'))
		.or(CharMatcher.inRange('A', 'Z'))
		.or(CharMatcher.anyOf("=+/"));

	CommandRunner(char[] sessionKey, Consumer<String> consumer)
	{
		super(() -> {
			try
			{
				String filteredKey = CHAR_MATCHER.retainFrom(new String(sessionKey));
				ProcessBuilder pb = buildCommand(filteredKey);
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

	private static ProcessBuilder buildCommand(String sessionKey)
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
		params.add("bw list items --search runescape.com --session \"" + sessionKey + "\"" + redirect);

		return new ProcessBuilder(params);
	}
}
