package io.ryoung.bitwarden;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("bitwarden")
public interface BitwardenConfig extends Config
{
	@ConfigItem(
		keyName = "clearKeyOnLogin",
		name = "Clear Session Key on Login",
		description = "Clear Session Key on Login"
	)
	default boolean clearKeyOnLogin()
	{
		return false;
	}
}
