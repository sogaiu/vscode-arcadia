# vscode-arcadia

## Getting Started

### Enabling the Extension

To run the extension from source:

- Clone the repo somewhere
- Open the root folder in VSCode
- Run the extension in an Extension Development Host by pressing `F5`, or clicking `Debug` > `Start`

This runs the compiled extension.js file in the root folder. Instructions for rebuilding this file are below.

Note: the extension can be also be built as a .vsix file and installed manually.  See below for details.

### Usage

To connect to Arcadia's socket REPL:

- Ensure Unity is up & an Arcadia project is loaded
- In VSCode bring up the command palette (`ctrl-shift-p` or `⌘-shift-p`)
- Choose `Arcadia: REPL - Start`.

Once the REPL is active, you can:

- `Arcadia: REPL - Send Line` (`ctrl+, l`)
- `Arcadia: REPL - Send Selection` (`ctrl+, s`)
- `Arcadia: REPL - Send File` (`ctrl+, f`)
- `Arcadia: REPL - Send Region` (`ctrl+enter`)

to send the current line, selection, file, or detected region to the REPL respectively.

Note that the extension automatically activates when VSCode detects the current language as Clojure.  So if you have a Clojure file open but no REPL yet, you can simply send the line/selection/file/region and the REPL will be started for you.

There is currently no in-REPL editing available (greetings Stuart Halloway), but you can open an empty file & use it as a scratch buffer.  The REPL doesn't care what syntax the file is set to, but you would need to set it Clojure to get syntax highlighting.

To use Arcadia's debugger:

- Use VSCode's terminal to connect to Arcadia's socket REPL using something like nc (netcat) or telnet to:

    - host: IPv6 localhost aka ::1 aka 0:0:0:0:0:0:0:1
    - port: TCP port 32770

- Then adapt the steps mentioned at [Arcadia wiki's Debugging page](https://github.com/arcadia-unity/Arcadia/wiki/Debugging).

## Development

To build extension.js (heart of the extension):

- Run `lein cljsbuild once` (or `auto` for continuous compilation)

NOTE from the original author (@worrel):

> It took some fiddling to get ClojureScript working in a VSCode extension.

> I mostly followed the non-Figwheel parts of [this](https://github.com/bhauman/lein-figwheel/wiki/Node.js-development-with-figwheel), but I found that the Google Closure library didn't load properly (specifically, provided namespaces got attached to `goog.global` but not attached to the global `goog` var).

> The trick to get it working was `:optimizations :simple` combined with `:output-wrapper true`. The other requirements are specifying `:main` and calling `(set! *main-cli-fn* -main)` from that namespace as documented on the ClojureScript NodeJS wiki page.

To build the .vsix file:

- Install vsce via npm / yarn
- Invoke its 'package' subcommand

To install the .vsix file:

- Use VSCode's --install-extension command via the command line -OR-
- Use the 'Install from .VSIX...' menu item in the 'More Actions...' menu in the Extension panel of VSCode's GUI (it's cleverly hidden in a tribute to Douglas Adams)
