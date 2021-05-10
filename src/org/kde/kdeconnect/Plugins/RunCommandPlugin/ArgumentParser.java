package org.kde.kdeconnect.Plugins.RunCommandPlugin;

import android.content.DialogInterface;
import android.content.Context;
import android.util.Log;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;

import java.util.regex.*;
import java.util.ArrayList;
import java.util.Collections;

import org.kde.kdeconnect.Plugins.Plugin;
import org.kde.kdeconnect.Helpers.RandomHelper;
import org.kde.kdeconnect_tp.R;

public class ArgumentParser {
	private static final Pattern argPattern = Pattern.compile("(\\$[0-9])|(\\$\\{[0-9]+\\})");
	private static final Pattern hasArgsPattern = Pattern.compile("(.*\\$[0-9].*)|(.*\\$\\{[0-9]+\\}.*)");
	private static final Pattern digitPattern = Pattern.compile("[0-9]+");

	private static final String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";


	private static ArrayList<Integer> getArgs(String cmd) {
		ArrayList<Integer> matches = new ArrayList<Integer>();
		Matcher matcher = argPattern.matcher(cmd);

		while (matcher.find()) {
			Matcher digits = digitPattern.matcher(matcher.group());
			digits.find();
			matches.add(Integer.parseInt(digits.group()));
		}

		return matches;
	}

  private static String getRandomString(int length) {
		return RandomHelper.randomString(length);

	}

	public static boolean hasArguments(String cmd) {
		return hasArgsPattern.matcher(cmd).matches();
	}

	public static Integer getMaxArg(String cmd) {
		ArrayList<Integer> args = getArgs(cmd);
		Collections.sort(args, Collections.reverseOrder());

		return args.get(0);
	}

	// wrap a given command in a function declaration, staging it for taking
	// user supplied arguments
	// eg. "echo $1"  ->  "kdsakjsad() { echo $1; }; kdsakjsad "
	public static String wrapAsFunction(String cmd) {
		String funcName = RandomHelper.randomString(15);

		if (!cmd.endsWith(";"))
			cmd += ";";

		return funcName + "() { " + cmd + " }; " + funcName + " ";
	}

	public static void getAndRunWithArgs(Context enclosingContext, RunCommandPlugin plugin, String cmdKey, String cmd, Runnable callback) {

		Log.d("RunCommand", "Running command with arguments");
		Log.d("RunCommand", "Max args: " + getMaxArg(cmd));
		Log.d("RunCommand", "As a function: " + wrapAsFunction(cmd));

		// run command with passed arguments
		final class CommandRunner {
			public void runCommand(final String passedArgs) {
				String[] args = passedArgs.split("\n");
				String argString = "";
				for (String a : args) {
					argString += "\"" + a + "\" ";
				}

				Log.d("RunCommand", "Received arguments: " + argString);
				plugin.runCommand(cmdKey, wrapAsFunction(cmd) + argString);
			}
		}

		// get arguments from user
		final EditText input = new EditText(enclosingContext);
		new AlertDialog.Builder(enclosingContext)
				.setTitle("Arguments..")
				.setView(input)
				.setMessage(cmd)
				.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						final String passedArgs = input.getText().toString();
						new CommandRunner().runCommand(passedArgs);
						dialog.dismiss();
						if (callback != null)
							callback.run();
					}
				})
				.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.cancel();
					}
				})
				.show();
	}

}
