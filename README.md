# Python path

## What does it plugin do?

Simply put, it allows management of source folders from within a text file ('source_dirs' located on the project's root folder).

## Why do we need this plugin ?

The idea is to have able to split your code into smaller self-sufficient packages. Each new service must have its own "source folder",
where your Python code resides in.
In Java Java platform, build tools such as Maven, Gradle and Sbt provides excellent plugins for integration. These
plugins also manages the IDEA (Intellij Platform) components state, such as source folders. In Python, on the other hand, there is
none. If you move your code some place else, you must mark it manually as "source code folder".

It's nice doing manualy if you're the only developer working on the code, but if you are part of a team and you would like this
change apply to all other team members automatically, you need a tool to do it automatically.

That where PyPath comes in handy. For every folder name you specify in a special file, this plugin will manage it and turn the
folder in "source folder". This way, PyCharm will add this folder path into PYTHONPATH during execution and will import lookups
in these folders.

## How to use it?

Usage is very simple: create a new file named 'source_dirs' in your workspace root and place list of paths you'd like to turn
into source folder, e.g.:

source_dirs:

```
# This is a comment line.
// This is a comment line as well.

# The following line marks '/monitor_apps/src' as source folder:
/monitor_apps/src
```

Note: # and // at the beginning of the line marks the line as comment. Empty lines are ignored. Reference to paths that does
not exist are skipped.

## Requirements

This plugin requires Java 8+ and runs within IDEA version 2019.1 and above.