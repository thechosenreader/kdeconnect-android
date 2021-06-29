# Hi, there!
This and its corresponding repository for the desktop application are my forks of KDE Connect. I've incorporated certain features for personal use like running commands with arguments, for example. You can find the breakdown of changes below. This is not the place for stability or reliability; these programs are guaranteed to work only in my environment. The official readme is below (except the official links to the desktop repository has been replaced with my forked version).

# Changes
Here you can find a high level overview of the changes I've made and the features I've added. For the most part, my changes have been localized to the specific plugins I've created or the specific behaviours I've modified. Here and there, there has been the need to change files beyond this, however.

## Features
- **Arguments in Run Command**
    * commands can be run with named arguments, e.g `echo $$msg`
    * named arguments can be given default values e.g `echo $$msg="hi"`
    * go to ARGS.md to learn more

- **File Manager**
    * browse files on your computer from your phone
    * easily download files and entire directories
    * run commands within the context of a directory
    * move, rename, and delete files
    * view and edit files as text

- **Remote Desktop**
    * coming soon

- **Misc**
    * RunCommandPlugin: have the option to view the full command (before it got cut off)
    * RunCommandPlugin: copy the command itself instead of the uri

## Consequences
As mentioned above, some of these changes required modifying code outside in unexpected places. Most are relatively insubstantial, like changing access modifiers. Some introduced new dependencies (libquazip). Most of the time it was just changing editing layouts, manifests, other xml files.

# KDE Connect - Android app

KDE Connect is a multi-platform app that allows your devices to communicate (eg: your phone and your computer).

## (Some) Features
- **Shared clipboard**: copy and paste between your phone and your computer (or any other device).
- **Notification sync**: Read and reply to your Android notifications from the desktop.
- **Share files and URLs** instantly from one device to another.
- **Multimedia remote control**: Use your phone as a remote for Linux media players.
- **Virtual touchpad**: Use your phone screen as your computer's touchpad and keyboard.

All this without wires, over the already existing WiFi network, and using TLS encryption.

## About this app

This is a native Android port of the KDE Connect Qt app. You will find a more complete readme about KDE Connect [here](https://github.com/thechosenreader/kdeconnect-kde).

## How to install this app

You can install this app from the [Play Store](https://play.google.com/store/apps/details?id=org.kde.kdeconnect_tp) as well as [F-Droid](https://f-droid.org/repository/browse/?fdid=org.kde.kdeconnect_tp). Note you will also need to install the [desktop app](https://github.com/thechosenreader/kdeconnect-kde) for it to work.

## Contributing

A lot of useful information, including how to get started working on KDE Connect and how to connect with the current developers, is on our [KDE Community Wiki page](https://community.kde.org/KDEConnect)

For bug reporting, please use [KDE's Bugzilla](https://bugs.kde.org). Please do not use the issue tracker in GitLab since we want to keep everything in one place.

To contribute patches, use [KDE Connect's Gitlab](https://invent.kde.org/kde/kdeconnect-android/).
On Gitlab (as well as on our [old Phabricator](https://phabricator.kde.org/tag/kde_connect/)) you can find a task list with stuff to do and links to other relevant resources.
It is a good idea to also subscribe to the [KDE Connect mailing list](https://mail.kde.org/mailman/listinfo/kdeconnect).

Please know that all translations for all KDE apps are handled by the [localization team](https://l10n.kde.org/). If you would like to submit a translation, that should be done by working with the proper team for that language.

## License
[GNU GPL v2](https://www.gnu.org/licenses/gpl-2.0.html) and [GNU GPL v3](https://www.gnu.org/licenses/gpl-3.0.html)

If you are reading this from Github, you should know that this is just a mirror of the [KDE Project repo](https://invent.kde.org/network/kdeconnect-android/).
