# Kross Project

## Overview

Kross is a cross platform shell that uses lua as its embedded scripting language.
If Fish is a shell for the 90's, Kross is a shell for the 2000's oughts. (except the mountain of work that still needs
to be done)
It is written in kotlin and runs on the JVM. It uses a lot more memory than most shells because of this. I don't think
this is an issue, but you have been warned. about a 100 MBs.
My goal is to make a shell for neovim lovers. I actually am just getting into the neovim scene. Also I can't stand any
shell scripting language. They are all so hard to read.

## Current status

Alpha build, the lua support barely exists and will be dramamtically overhualed.

## Features

* Live updating prompt
  [![asciicast](https://asciinema.org/a/696476.svg)](https://asciinema.org/a/696476)
* Standard && behavior
  [![asciicast](https://asciinema.org/a/696471.svg)](https://asciinema.org/a/696471)
* Standard || behavior
  [![asciicast](https://asciinema.org/a/696478.svg)](https://asciinema.org/a/696478)
* Standard | behavior
  [![asciicast](https://asciinema.org/a/696479.svg)](https://asciinema.org/a/696479)
* Standard ; behavior
  [![asciicast](https://asciinema.org/a/696480.svg)](https://asciinema.org/a/696480)
* Create commands in lua (early prototype, will change)
  [![asciicast](https://asciinema.org/a/696477.svg)](https://asciinema.org/a/696477)
* Fish style command substitution
  [![asciicast](https://asciinema.org/a/696472.svg)](https://asciinema.org/a/696472)
* Environment variable subsition (not fully completed)
  [![asciicast](https://asciinema.org/a/696474.svg)](https://asciinema.org/a/696474)
* Edit your input in your editor of choice
  [![asciicast](https://asciinema.org/a/696473.svg)](https://asciinema.org/a/696473)
* Find a file in fzf for an argument
  [![asciicast](https://asciinema.org/a/696475.svg)](https://asciinema.org/a/696475)

## Goals

1. Provide a powerful scripting language built on Lua.
2. Ensure compatibility with common shell utilities and scripting patterns.
3. Create a user-friendly interactive shell interface.

## License

This project is licensed under the MIT License. See the `LICENSE` file for details.
