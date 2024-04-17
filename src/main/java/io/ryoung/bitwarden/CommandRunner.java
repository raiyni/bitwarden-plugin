package io.ryoung.bitwarden;

import com.google.common.base.CharMatcher;
import com.google.common.io.CharStreams;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import net.runelite.client.util.OSType;

class CommandRunner extends Thread
{

	private static final CharMatcher CHAR_MATCHER = CharMatcher.inRange('0', '9')
		.or(CharMatcher.inRange('a', 'z'))
		.or(CharMatcher.inRange('A', 'Z'))
		.or(CharMatcher.anyOf("=+/"));

	CommandRunner(String bw, char[] sessionKey, Consumer<String> consumer)
	{
		super(() -> {
			try
			{
				ProcessBuilder pb = buildCommand(bw, new String(sessionKey));
				Process p = pb.start();

				CompletableFuture<String> sout = readOutStream(p.getInputStream());
				CompletableFuture<String> serror = readOutStream(p.getErrorStream());
				CompletableFuture<String> result = sout.thenCombine(serror, (stdout, stderr) ->
				{
					if (!stdout.startsWith("["))
					{
						return stdout + stderr;
					}

					return stdout;
				});

				p.waitFor();
				consumer.accept(result.get());
			}
			catch (IOException | InterruptedException | ExecutionException e)
			{
				// do nothing
			}
		});
	}

	static CompletableFuture<String> readOutStream(InputStream is)
	{
		return CompletableFuture.supplyAsync(() -> {
			String s = "";
			try
			{
				s = CharStreams.toString(new InputStreamReader(is));
			}
			catch (IOException e)
			{
				// do nothing
			}

			return s;
		});
	}

	private static ProcessBuilder buildCommand(String bw, String sessionKey)
	{
		String filteredKey = CHAR_MATCHER.retainFrom(sessionKey);
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
		params.add(bw + " list items --search runescape.com --session \"" + filteredKey + "\"" + redirect);

		return new ProcessBuilder(params);
	}
}
