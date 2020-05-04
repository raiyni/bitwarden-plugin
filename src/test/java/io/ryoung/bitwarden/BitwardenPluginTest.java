package io.ryoung.bitwarden;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class BitwardenPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(BitwardenPlugin.class);
		RuneLite.main(args);
	}
}