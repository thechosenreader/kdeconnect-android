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
	// yes, this will not match all of the positional arguments. its fine, the function getArgs is not used meaningfully
	// private static final Pattern posArgPattern = Pattern.compile("(\\$(?<idx>[0-9]))|(\\$\\{(?<idx>[0-9]+)\\})");
	private static final Pattern posArgPattern = Pattern.compile("(\\$(?<idx>[0-9]))");
	private static final Pattern hasPosArgsPattern = Pattern.compile("(.*\\$[0-9].*)|(.*\\$\\{[0-9]+\\}.*)");

	// the below regex is complicated by java
	// without weird escaping: \$\$(?<name>[a-zA-Z_][_a-zA-Z0-9]*)(="(?<default>.*?)(?<![\\])")?
	private static final Pattern namedArgPattern = Pattern.compile("\\$\\$(?<name>[a-zA-Z_][_a-zA-Z0-9]*)(?<notname>=\"(?<default>.*?)(?<![\\\\])\")?");
	private static final Pattern hasNamedArgsPattern = Pattern.compile("(.*\\$\\$[a-zA-Z_][_a-zA-Z0-9]*.*)");
	private static final String namedArgDeclaration = "$$";

	private ArgumentGetterBinding argBinding;

	private Context enclosingContext;
	private RunCommandPlugin plugin;
	private String cmdKey;
	private String cmd;
	private Runnable callback;

	private final HashMap<String, String> argsToDefaultMap = new HashMap<>();

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

	final static class EditTextWithArgName extends EditText {
		String argname;
		String defaultValue;

		public EditTextWithArgName(Context context) {
			super(context);
		}

		public String getArgNameWithoutDefault() {
			return argname;
		}

		public String getDefaultValue() {
			return defaultValue;
		}

		public void setArgName(String arg) {
			this.argname = arg;
		}

		public void setDefaultValue(String def) {
			this.defaultValue = def;
		}

	}

	public ArgumentParser(Context enclosingContext, RunCommandPlugin plugin, String cmdKey, String cmd, Runnable callback) {
		this.enclosingContext = enclosingContext;
		this.plugin = plugin;
		this.cmdKey = cmdKey;
		this.cmd = cmd;
		this.callback = callback;

		argBinding = ArgumentGetterBinding.inflate(LayoutInflater.from(enclosingContext));
		argBinding.argLayout.removeViews(1, argBinding.argLayout.getChildCount() - 1);

		if (!hasPosArgs(cmd)) {
			argBinding.posArgs.setVisibility(View.GONE);
		}

		argsToDefaultMap.clear();
		argBinding.dialogMessage.setText(cmd);
	}

	private void doNamedArgs() {
		int start = 0;
		Matcher matcher = namedArgPattern.matcher(this.cmd);
		while (matcher.find(start)) {
			String appearance = matcher.group(0);
			String arg = matcher.group("name");
			String defaultValue = matcher.group("default");

			if (argsToDefaultMap.containsKey(arg)) {
				if (!(defaultValue == null || defaultValue.equals(""))) {
					argsToDefaultMap.put(arg, defaultValue);
				}
			} else {
				argsToDefaultMap.put(arg, defaultValue);
			}


			start = matcher.start("notname");
			if (start != -1) {
				this.cmd = new StringBuilder(this.cmd).replace(start, matcher.end("notname"), "").toString();
				matcher.reset(this.cmd);
			} else {
				start = matcher.end();
			}
		}

		Log.d("ArgumentParser", "finished stripping notnames, cmd is now " + this.cmd);
		argsToDefaultMap.forEach((name, defaultValue) -> {
			final EditTextWithArgName edittext = new EditTextWithArgName(this.enclosingContext);
			edittext.setHint(name + (!(defaultValue == null || defaultValue.equals("")) ? "=" + defaultValue : ""));
			edittext.setArgName(name);
			edittext.setDefaultValue((defaultValue == null ? "" : defaultValue));
			argBinding.argLayout.addView(edittext);
		});

	}

	private static boolean hasPosArgs(String cmd) {
		return hasPosArgsPattern.matcher(cmd).matches();
	}

	private static boolean hasNamedArgs(String cmd) {
		return hasNamedArgsPattern.matcher(cmd).matches();
	}

	public static boolean hasArguments(String cmd) {
		return hasPosArgs(cmd) || hasNamedArgs(cmd);
	}

	private static void replaceNamedArg(StringBuilder cmd, String passedArg, String namedArg) {
		Pattern p = Pattern.compile(namedArgDeclaration + namedArg, Pattern.LITERAL);
		// do not wrap supplied value in quotes if argument name starts with _
		String format = (namedArg.startsWith("_")) ? "%s" : "\"%s\"";
		cmd.replace(0, cmd.length(),
			p.matcher(cmd.toString()).replaceAll(String.format(format, passedArg)));
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

	public void getAndRunWithArgs() {
		// Log.d("ArgumentParser", "argLayout child count " + argBinding.argLayout.getChildCount());

    doNamedArgs();
		final StringBuilder sb = new StringBuilder(this.cmd);
		new AlertDialog.Builder(this.enclosingContext)
			.setTitle(R.string.rc_arguments)
			.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					for (int idx=1; idx < argBinding.argLayout.getChildCount(); idx++) {
						EditTextWithArgName t = ((EditTextWithArgName) argBinding.argLayout.getChildAt(idx));
						final String namedArg = t.getArgNameWithoutDefault();
						String passedNamedArg = t.getText().toString();
						if (passedNamedArg.equals("")) {
							passedNamedArg = t.getDefaultValue();
						}
						Log.d("ArgumentParser", String.format("replacing %s with %s", namedArg, passedNamedArg));
						replaceNamedArg(sb, passedNamedArg, namedArg);
						Log.d("ArgumentParser", "cmd is now " + sb.toString());

					}

					final String passedPosArgs = argBinding.posArgs.getText().toString();
					new CommandRunner().runCommand(ArgumentParser.this.plugin, ArgumentParser.this.cmdKey, sb.toString(), passedPosArgs);

					dialog.dismiss();
					if (ArgumentParser.this.callback != null) {
						ArgumentParser.this.callback.run();
					}
				}
			})
			.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.cancel();
					if (ArgumentParser.this.callback != null) {
						ArgumentParser.this.callback.run();
					}
				}
			})
			.setView(argBinding.getRoot())
			.show();
	}

}
