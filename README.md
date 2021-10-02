# Bitwarden
Plugin that interfaces with the Bitwarden CLI to retrieve RuneScape passwords.

This plugin requires: 

* You have the [Bitwarden CLI](https://bitwarden.com/help/article/cli/) installed 
* `bw` is accessible in your PATH
* You are comfortable using a command line interface
* Your accounts are each under a website entry of `runescape.com`

It's recommended you walk through the documentation for [Bitwarden CLI](https://bitwarden.com/help/article/cli/), so you are aware of the features of the application and the differences between logout and lock.

**As long as you leave the CLI window open with the session key visible, or a browser window with Bitwarden logged in, someone can still access your account on your PC.**

No login information is saved between sessions.

# Usage

There are two ways to use this plugin. 

#### Active

With this method, you have to keep your session key on hand and enter it every time the plugin starts. This would keep your information more secure if multiple people use your computer. Combine this with the `Clear Key on Login` config option to have the most security.


1. Login to your Bitwarden vault through the CLI with `bw login --raw` and copy the resulting session key. Make sure not to copy the newline following it.
1. Load RuneLite with this plugin enabled or enable this plugin while on the login screen.
1. Paste your session key into the popup asking for it and press `Ok`.
    * Pressing cancel will suppress the popup until your next client start or plugin toggle.
1. Once you switch to the login page, your password should fill in when it matches your username.
    * It may take a few seconds for your information to load depending on your internet connection.
    * If an error occurs, you should receive a popup indicating that.
1. If you are using the AppImage, you will need to set the path to your executable in the config for the plugin.
    
#### Passive

With this method, you can manage your vault by locking it after you close RuneLite and unlocking it before you open it. It's a mildly faster way to get into the game, but it can be less secure if you have multiple users on your computer.

1. Login to your Bitwarden vault through the CLI with `bw login --raw`.
1. Set the environment variable `BW_SESSION` to your key.
    * On Windows, you can use the command `setx` or create an environment variable through the system menus.
    * On OSX/Linux, add the variable to your `.profile` file.
    * On OSX you might need to set it with `launchctl setenv BW_SESSION "YOUR_SESSION_KEY_HERE"`
1. Restart RuneLite if it is open.
1. Once you switch to the login page, your password should fill in when it matches your username.
    * It may take a few seconds for your information to load depending on your internet connection.
    * If an error occurs, you should receive a popup indicating that.
1. If the session key from `BW_SESSION` seems to be invalid, you will be prompted for a new key.
1. If you are using the AppImage, you will need to set the path to your executable in the config for the plugin.
