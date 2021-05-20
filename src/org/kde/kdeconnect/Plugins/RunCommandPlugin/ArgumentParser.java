package org.kde.kdeconnect.Plugins.RunCommandPlugin;

import android.content.DialogInterface;
import android.content.Context;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.view.LayoutInflater;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;

import java.util.regex.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import org.kde.kdeconnect.Plugins.Plugin;
import org.kde.kdeconnect.Helpers.RandomHelper;
import org.kde.kdeconnect_tp.databinding.ArgumentGetterBinding;
import org.kde.kdeconnect_tp.R;

public class ArgumentParser {
	// private static final Pattern posArgPattern = Pattern.compile("(\\$(?<idx>[0-9]))|(\\$\\{(?<idx>[0-9]+)\\})");
	private static final Pattern posArgPattern = Pattern.compile("(\\$(?<idx>[0-9]))");
	private static final Pattern hasPosArgsPattern = Pattern.compile("(.*\\$[0-9].*)|(.*\\$\\{[0-9]+\\}.*)");

	private static final Pattern namedArgPattern = Pattern.compile("\\$\\$(?<name>[a-zA-Z][a-zA-Z0-9]*)");
	private static final Pattern hasNamedArgsPattern = Pattern.compile("(.*\\$\\$[a-zA-Z][a-zA-Z0-9]*.*)");

	// private static final Pattern digitPattern = Pattern.compile("[0-9]+");
	// private static final Pattern alphaNumericPattern = Pattern.compile("[a-zA-Z][a-zA-Z0-9]*");

	private static final String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

	private static ArgumentGetterBinding argBinding;


	private static ArrayList<Integer> getArgs(String cmd) {
		ArrayList<Integer> matches = new ArrayList<Integer>();
		Matcher matcher = posArgPattern.matcher(cmd);

		while (matcher.find()) {
			matches.add(Integer.parseInt(matcher.group("idx")));
		}

		return matches;
	}

	private static void doNamedArgs(Context context, String cmd) {
		final HashMap<String, HashSet<Pair<Integer, Integer>>> argsToIndicesMap = new HashMap<>();

		Matcher matcher = namedArgPattern.matcher(cmd);
		while (matcher.find()) {
			String arg = matcher.group("name");
			// Pair indices = new Pair(matcher.start(), matcher.end());
			//
			// if (argsToIndicesMap.containsKey(arg)) {
			// 	argsToIndicesMap.get(arg).add(indices);
			//
			// } else {
			// 	HashSet<Pair<Integer, Integer>> set = new HashSet<>();
			// 	set.add(indices);
			// 	argsToIndicesMap.put(arg, set);
			// }

			final EditText edittext = new EditText(context);
			edittext.setHint(arg);

			argBinding.argLayout.addView(edittext);
		}
	}

	private static boolean hasPosArgs(String cmd) {
		return hasPosArgsPattern.matcher(cmd).matches();
	}

	private static boolean hasNamedArgs(String cmd) {
		return hasNamedArgsPattern.matcher(cmd).matches();
	}

	private static void replaceNamedArg(StringBuilder cmd, String passedArg, String namedArg) {
		Pattern p = Pattern.compile("\\$\\$" + namedArg);
		String replaced = p.matcher(cmd.toString()).replaceAll(String.format("\"%s\"", passedArg));

		// yes, this is bizarre as fuck; ive done this due to inner classes requiring
		// references to local variables to be final
		cmd.replace(0, cmd.length(), replaced);
	}

	public static boolean hasArguments(String cmd) {
		return hasPosArgs(cmd) || hasNamedArgs(cmd);
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
		String funcName = RandomHelper.randomStringCharOnly(15);

		if (!cmd.endsWith(";"))
			cmd += ";";

		return String.format("%s() { %s }; %s ", funcName, cmd, funcName);
	}

	// run command with passed arguments
	final static class CommandRunner {
		public void runCommand(final RunCommandPlugin plugin, final String cmdKey, final String cmd, final String passedArgs) {
			String[] args = passedArgs.split("\n");
			String argString = "";
			for (String a : args) {
				argString += "\"" + a + "\" ";
			}

			final String finalCmd = wrapAsFunction(cmd) + argString;
			Log.d("RunCommand", "running " + finalCmd);
			plugin.runCommand(cmdKey, finalCmd);
		}
	}

	public static void getAndRunWithArgs(Context enclosingContext, RunCommandPlugin plugin, String cmdKey, String cmd, Runnable callback) {
		argBinding = ArgumentGetterBinding.inflate(LayoutInflater.from(enclosingContext));
		argBinding.argLayout.removeViews(1, argBinding.argLayout.getChildCount() - 1);

		if (!hasPosArgs(cmd)) {
			argBinding.posArgs.setVisibility(View.GONE);
		}

		Log.d("ArgumentParser", "argLayout child count " + argBinding.argLayout.getChildCount());

    doNamedArgs(enclosingContext, cmd);

		final StringBuilder sb = new StringBuilder(cmd);
		new AlertDialog.Builder(enclosingContext)
			.setTitle(R.string.rc_arguments)
			.setView(argBinding.getRoot())
			.setMessage(cmd)
			.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					for (int idx=1; idx < argBinding.argLayout.getChildCount(); idx++) {
						EditText t = ((EditText) argBinding.argLayout.getChildAt(idx));
						final String namedArg = t.getHint().toString();
						final String passedNamedArg = t.getText().toString();
						Log.d("ArgumentParser", String.format("replacing %s with %s", namedArg, passedNamedArg));
						replaceNamedArg(sb, passedNamedArg, namedArg);
						Log.d("ArgumentParser", "cmd is now " + sb.toString());

					}

					final String passedPosArgs = argBinding.posArgs.getText().toString();
					new CommandRunner().runCommand(plugin, cmdKey, sb.toString(), passedPosArgs);

					dialog.dismiss();
					if (callback != null) {
						callback.run();
					}
				}
			})
			.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.cancel();
					if (callback != null) {
						callback.run();
					}
				}
			})
			.show();
	}

}
