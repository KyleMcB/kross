# Kross Project

## Overview

Kross is a cross platform shell that uses lua as its embedded scripting language.
If Fish is a shell for the 90's, Kross is a shell for the (20)00's. (except the mountain of work that still needs
to be done)
It is written in kotlin and runs on the JVM. It uses a lot more memory than most shells because of this. I don't think
this is an issue, but you have been warned. about a 100 MBs.
My goal is to make a shell for neovim lovers. I actually am just getting into the neovim scene. Also I can't stand any
shell scripting language. They are all so hard to read.

## Current status

Alpha build, the lua support barely exists and will be dramatically overhauled.

## Features

- **Live Updating Prompt**
  Experience a dynamic prompt that updates in real-time based on your environment and context.
  [![asciicast](https://asciinema.org/a/696476.svg)](https://asciinema.org/a/696476)

- **Logical AND (`&&`) Behavior**
  Execute commands conditionally using standard logical AND operations.
  [![asciicast](https://asciinema.org/a/696471.svg)](https://asciinema.org/a/696471)

- **Logical OR (`||`) Behavior**
  Implement conditional command execution with logical OR functionality.
  [![asciicast](https://asciinema.org/a/696478.svg)](https://asciinema.org/a/696478)

- **Pipe (`|`) Behavior**
  Utilize standard piping to pass outputs between commands seamlessly.
  [![asciicast](https://asciinema.org/a/696479.svg)](https://asciinema.org/a/696479)

- **Command Sequencing (`;`) Behavior**
  Execute multiple commands in sequence using standard command separators.
  [![asciicast](https://asciinema.org/a/696480.svg)](https://asciinema.org/a/696480)

- **Lua Command Creation**
  Create and manage commands using Lua scripts. *(Early prototype; subject to change.)*
  [![asciicast](https://asciinema.org/a/696477.svg)](https://asciinema.org/a/696477)

- **Fish-Style Command Substitution**
  Use the output of one command by using ()
  [![asciicast](https://asciinema.org/a/696472.svg)](https://asciinema.org/a/696472)

- **Environment Variable Substitution**
  Support for substituting environment variables within commands. *(Work in progress. Does not work in quotes)*
  [![asciicast](https://asciinema.org/a/696474.svg)](https://asciinema.org/a/696474)

- **Editor Integration for Input Editing**
  Edit command inputs using your preferred text editor for enhanced usability.
  [![asciicast](https://asciinema.org/a/696473.svg)](https://asciinema.org/a/696473)

- **File Search Integration with fzf**
  Easily find and select files using fzf as part of your command arguments.
  [![asciicast](https://asciinema.org/a/696475.svg)](https://asciinema.org/a/696475)

## Goals

* A plugin system
* Customizable prompt, and just about everything else too
* Windows support. While this program will technically run on windows right now, lacks some basic functionality that I
  get for free on *nix like ls. So I need to implement those basic commands in lua
* A proper tab completion system. I'm thinking of building something that can read fish completions.
* write lua on the repl part of the shell.

## Installation

For now, just homebrew.

```shell
brew tap KyleMcB/kross
brew install kross
```

Well you could also build from source. Note you need to download my fork of the kotter library and have it nested
in the kross repo. (Not ideal, will fix later.)

## License

This project is licensed under the MIT License. See the `LICENSE` file for details.

## Acknowledgements

Big thanks to bitspittle for https://github.com/varabyte/kotter
TJ Devries for making videos to help get into neovim. No one can look a blank black screen and see the hidden potential.
